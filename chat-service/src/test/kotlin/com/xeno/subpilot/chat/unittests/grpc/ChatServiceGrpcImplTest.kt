package com.xeno.subpilot.chat.unittests.grpc

import com.xeno.subpilot.chat.client.OpenAiChatClient
import com.xeno.subpilot.chat.grpc.ChatServiceGrpcImpl
import com.xeno.subpilot.chat.repository.ChatModelPreferenceRepository
import com.xeno.subpilot.chat.service.ChatHistoryService
import com.xeno.subpilot.chat.service.ChatTurn
import com.xeno.subpilot.proto.chat.v1.processMessageRequest
import com.xeno.subpilot.proto.chat.v1.setModelRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import kotlin.test.assertEquals

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class ChatServiceGrpcImplTest {

    @MockK
    lateinit var chatModelPreferenceRepository: ChatModelPreferenceRepository

    @MockK
    lateinit var openAiChatClient: OpenAiChatClient

    @MockK
    lateinit var chatHistoryService: ChatHistoryService

    private lateinit var grpcImpl: ChatServiceGrpcImpl

    // captured as local val to avoid proto builder DSL shadowing
    private val testChatId = 42L
    private val testUserId = 1L

    @BeforeEach
    fun setUp() {
        grpcImpl =
            ChatServiceGrpcImpl(chatModelPreferenceRepository, openAiChatClient, chatHistoryService)
    }

    @Test
    fun `processMessage returns AI text in response`() =
        runTest {
            every { chatModelPreferenceRepository.getModel(any()) } returns "gpt-4o"
            every { chatHistoryService.getHistory(any()) } returns emptyList()
            coEvery { openAiChatClient.chat(any(), any(), any()) } returns "AI response"
            justRun { chatHistoryService.append(any(), any(), any()) }

            val chatId = testChatId
            val response =
                grpcImpl.processMessage(
                    processMessageRequest {
                        this.chatId = chatId
                        this.userId = testUserId
                        this.text = "hello"
                    },
                )

            assertEquals("AI response", response.text)
        }

    @Test
    fun `processMessage loads model from repository for the correct chat`() =
        runTest {
            every { chatModelPreferenceRepository.getModel(any()) } returns "gpt-4o-mini"
            every { chatHistoryService.getHistory(any()) } returns emptyList()
            coEvery { openAiChatClient.chat(any(), any(), any()) } returns "response"
            justRun { chatHistoryService.append(any(), any(), any()) }

            val chatId = testChatId
            grpcImpl.processMessage(
                processMessageRequest {
                    this.chatId = chatId
                    this.userId = testUserId
                    this.text = "hello"
                },
            )

            coVerify { openAiChatClient.chat(any(), any(), "gpt-4o-mini") }
        }

    @Test
    fun `processMessage passes chat history to AI client`() =
        runTest {
            val history = listOf(ChatTurn(ChatTurn.Role.USER, "previous message"))
            every { chatModelPreferenceRepository.getModel(any()) } returns "gpt-4o"
            every { chatHistoryService.getHistory(any()) } returns history
            coEvery { openAiChatClient.chat(history, any(), any()) } returns "response"
            justRun { chatHistoryService.append(any(), any(), any()) }

            val chatId = testChatId
            grpcImpl.processMessage(
                processMessageRequest {
                    this.chatId = chatId
                    this.userId = testUserId
                    this.text = "hello"
                },
            )

            coVerify { openAiChatClient.chat(history, any(), any()) }
        }

    @Test
    fun `processMessage saves user message and AI response to history`() =
        runTest {
            every { chatModelPreferenceRepository.getModel(any()) } returns "gpt-4o"
            every { chatHistoryService.getHistory(any()) } returns emptyList()
            coEvery { openAiChatClient.chat(any(), any(), any()) } returns "AI response"
            justRun { chatHistoryService.append(any(), any(), any()) }

            val chatId = testChatId
            grpcImpl.processMessage(
                processMessageRequest {
                    this.chatId = chatId
                    this.userId = testUserId
                    this.text = "user message"
                },
            )

            coVerify { chatHistoryService.append(any(), "user message", "AI response") }
        }

    @Test
    fun `setModel saves model via repository`() =
        runTest {
            justRun { chatModelPreferenceRepository.setModel(any(), any()) }

            val chatId = testChatId
            grpcImpl.setModel(
                setModelRequest {
                    this.chatId = chatId
                    this.model = "gpt-4o-mini"
                },
            )

            coVerify { chatModelPreferenceRepository.setModel(any(), "gpt-4o-mini") }
        }
}
