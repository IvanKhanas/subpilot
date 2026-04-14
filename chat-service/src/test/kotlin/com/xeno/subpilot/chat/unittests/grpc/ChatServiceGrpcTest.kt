package com.xeno.subpilot.chat.unittests.grpc

import com.xeno.subpilot.chat.client.OpenAiChatClient
import com.xeno.subpilot.chat.client.SubscriptionGrpcClient
import com.xeno.subpilot.chat.exception.ChatHistoryException
import com.xeno.subpilot.chat.exception.OpenAiException
import com.xeno.subpilot.chat.grpc.ChatServiceGrpc
import com.xeno.subpilot.chat.service.ChatHistoryService
import com.xeno.subpilot.chat.service.ChatTurn
import com.xeno.subpilot.proto.chat.v1.clearHistoryRequest
import com.xeno.subpilot.proto.chat.v1.processMessageRequest
import com.xeno.subpilot.proto.subscription.v1.CheckAccessResponse
import com.xeno.subpilot.proto.subscription.v1.DenialReason
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

import kotlin.test.assertEquals

import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class ChatServiceGrpcTest {

    @MockK
    lateinit var openAiChatClient: OpenAiChatClient

    @MockK
    lateinit var chatHistoryService: ChatHistoryService

    @MockK
    lateinit var subscriptionGrpcClient: SubscriptionGrpcClient

    private lateinit var grpc: ChatServiceGrpc

    private val testChatId = 42L
    private val testUserId = 1L

    @BeforeEach
    fun setUp() {
        grpc =
            ChatServiceGrpc(
                openAiChatClient,
                chatHistoryService,
                subscriptionGrpcClient,
                UnconfinedTestDispatcher(),
            )
    }

    private fun accessAllowed(model: String = "gpt-4o") {
        coEvery { subscriptionGrpcClient.getModelPreference(any()) } returns model
        coEvery { subscriptionGrpcClient.checkAccess(any(), any()) } returns
            CheckAccessResponse.newBuilder().setAllowed(true).build()
    }

    @Test
    fun `processMessage returns AI text in response`() =
        runTest {
            accessAllowed()
            every { chatHistoryService.getHistory(any()) } returns emptyList()
            coEvery { openAiChatClient.chat(any(), any(), any()) } returns "AI response"
            justRun { chatHistoryService.append(any(), any(), any()) }

            val response =
                grpc.processMessage(
                    processMessageRequest {
                        this.chatId = testChatId
                        this.userId = testUserId
                        this.text = "hello"
                    },
                )

            assertEquals("AI response", response.text)
        }

    @Test
    fun `processMessage uses model from subscription service`() =
        runTest {
            accessAllowed(model = "gpt-4o-mini")
            every { chatHistoryService.getHistory(any()) } returns emptyList()
            coEvery { openAiChatClient.chat(any(), any(), any()) } returns "response"
            justRun { chatHistoryService.append(any(), any(), any()) }

            grpc.processMessage(
                processMessageRequest {
                    this.chatId = testChatId
                    this.userId = testUserId
                    this.text = "hello"
                },
            )

            coVerify { openAiChatClient.chat(any(), any(), "gpt-4o-mini") }
        }

    @Test
    fun `processMessage passes chat history to AI client`() =
        runTest {
            accessAllowed()
            val history = listOf(ChatTurn(ChatTurn.Role.USER, "previous message"))
            every { chatHistoryService.getHistory(any()) } returns history
            coEvery { openAiChatClient.chat(history, any(), any()) } returns "response"
            justRun { chatHistoryService.append(any(), any(), any()) }

            grpc.processMessage(
                processMessageRequest {
                    this.chatId = testChatId
                    this.userId = testUserId
                    this.text = "hello"
                },
            )

            coVerify { openAiChatClient.chat(history, any(), any()) }
        }

    @Test
    fun `processMessage saves user message and AI response to history`() =
        runTest {
            accessAllowed()
            every { chatHistoryService.getHistory(any()) } returns emptyList()
            coEvery { openAiChatClient.chat(any(), any(), any()) } returns "AI response"
            justRun { chatHistoryService.append(any(), any(), any()) }

            grpc.processMessage(
                processMessageRequest {
                    this.chatId = testChatId
                    this.userId = testUserId
                    this.text = "user message"
                },
            )

            coVerify { chatHistoryService.append(any(), "user message", "AI response") }
        }

    @Test
    fun `processMessage returns denial reason when access is denied`() =
        runTest {
            coEvery { subscriptionGrpcClient.getModelPreference(any()) } returns "gpt-4o"
            coEvery { subscriptionGrpcClient.checkAccess(any(), any()) } returns
                CheckAccessResponse
                    .newBuilder()
                    .setAllowed(false)
                    .setDenialReason(DenialReason.QUOTA_EXHAUSTED)
                    .build()

            val response =
                grpc.processMessage(
                    processMessageRequest {
                        this.chatId = testChatId
                        this.userId = testUserId
                        this.text = "hello"
                    },
                )

            assertEquals(DenialReason.QUOTA_EXHAUSTED, response.denialReason)
            assertEquals("", response.text)
        }

    @Test
    fun `processMessage refunds quota and rethrows when OpenAI fails`() =
        runTest {
            accessAllowed()
            every { chatHistoryService.getHistory(any()) } returns emptyList()
            coEvery { openAiChatClient.chat(any(), any(), any()) } throws OpenAiException("timeout")
            coJustRun { subscriptionGrpcClient.refundAccess(any(), any(), any(), any()) }

            assertThrows<OpenAiException> {
                grpc.processMessage(
                    processMessageRequest {
                        this.chatId = testChatId
                        this.userId = testUserId
                        this.text = "hello"
                    },
                )
            }

            coVerify { subscriptionGrpcClient.refundAccess(testUserId, any(), any(), any()) }
        }

    @Test
    fun `processMessage refunds quota and rethrows when getHistory fails`() =
        runTest {
            accessAllowed()
            every { chatHistoryService.getHistory(any()) } throws ChatHistoryException("redis down")
            coJustRun { subscriptionGrpcClient.refundAccess(any(), any(), any(), any()) }

            assertThrows<ChatHistoryException> {
                grpc.processMessage(
                    processMessageRequest {
                        this.chatId = testChatId
                        this.userId = testUserId
                        this.text = "hello"
                    },
                )
            }

            coVerify { subscriptionGrpcClient.refundAccess(testUserId, any(), any(), any()) }
        }

    @Test
    fun `clearHistory delegates to chatHistoryService`() =
        runTest {
            justRun { chatHistoryService.clear(any()) }

            grpc.clearHistory(clearHistoryRequest { chatId = testChatId })

            verify { chatHistoryService.clear(testChatId) }
        }

    @Test
    fun `processMessage refunds quota and rethrows when append fails`() =
        runTest {
            accessAllowed()
            every { chatHistoryService.getHistory(any()) } returns emptyList()
            coEvery { openAiChatClient.chat(any(), any(), any()) } returns "AI response"
            every { chatHistoryService.append(any(), any(), any()) } throws
                ChatHistoryException("redis down")
            coJustRun { subscriptionGrpcClient.refundAccess(any(), any(), any(), any()) }

            assertThrows<ChatHistoryException> {
                grpc.processMessage(
                    processMessageRequest {
                        this.chatId = testChatId
                        this.userId = testUserId
                        this.text = "hello"
                    },
                )
            }

            coVerify { subscriptionGrpcClient.refundAccess(testUserId, any(), any(), any()) }
        }
}
