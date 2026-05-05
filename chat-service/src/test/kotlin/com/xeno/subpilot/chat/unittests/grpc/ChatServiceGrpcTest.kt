/*
 * Copyright 2024 Ivan Khanas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xeno.subpilot.chat.unittests.grpc

import com.xeno.subpilot.chat.client.ModerationGrpcClient
import com.xeno.subpilot.chat.client.OpenAiChatClient
import com.xeno.subpilot.chat.client.OpenAiModerationClient
import com.xeno.subpilot.chat.client.SubscriptionGrpcClient
import com.xeno.subpilot.chat.exception.ChatHistoryException
import com.xeno.subpilot.chat.exception.OpenAiException
import com.xeno.subpilot.chat.grpc.ChatServiceGrpc
import com.xeno.subpilot.chat.metrics.ChatMetrics
import com.xeno.subpilot.chat.service.ChatHistoryService
import com.xeno.subpilot.chat.service.ChatTurn
import com.xeno.subpilot.proto.chat.v1.clearContextRequest
import com.xeno.subpilot.proto.chat.v1.processMessageRequest
import com.xeno.subpilot.proto.subscription.v1.CheckAccessResponse
import com.xeno.subpilot.proto.subscription.v1.DenialReason
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

import java.util.stream.Stream

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.reflect.KClass

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

    @MockK(relaxed = true)
    lateinit var moderationClient: OpenAiModerationClient

    @MockK(relaxed = true)
    lateinit var moderationGrpcClient: ModerationGrpcClient

    private val meterRegistry = SimpleMeterRegistry()
    private val metrics = ChatMetrics(meterRegistry)

    private lateinit var grpc: ChatServiceGrpc

    private val testChatId = 42L
    private val testUserId = 1L
    private val requestText = "hello"

    @BeforeEach
    fun setUp() {
        grpc =
            ChatServiceGrpc(
                openAiChatClient,
                chatHistoryService,
                subscriptionGrpcClient,
                moderationClient,
                moderationGrpcClient,
                UnconfinedTestDispatcher(),
                metrics,
            )
        coEvery { moderationClient.flaggedCategories(any()) } returns emptyList()
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
                        this.text = requestText
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
                    this.text = requestText
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
                    this.text = requestText
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
                        this.text = requestText
                    },
                )

            assertEquals(DenialReason.QUOTA_EXHAUSTED, response.denialReason)
            assertEquals("", response.text)
        }

    @ParameterizedTest(name = "{0}")
    @MethodSource("refundFailureCases")
    fun `processMessage refunds quota and rethrows for pipeline failures`(
        caseName: String,
        failureStage: FailureStage,
        expectedExceptionType: KClass<out Throwable>,
    ) =
        runTest {
            assertTrue(caseName.isNotBlank())
            accessAllowed()
            coJustRun { subscriptionGrpcClient.refundAccess(any(), any(), any(), any()) }

            when (failureStage) {
                FailureStage.OPENAI -> {
                    every { chatHistoryService.getHistory(any()) } returns emptyList()
                    coEvery { openAiChatClient.chat(any(), any(), any()) } throws OpenAiException("timeout")
                }
                FailureStage.GET_HISTORY -> {
                    every { chatHistoryService.getHistory(any()) } throws ChatHistoryException("redis down")
                }
                FailureStage.APPEND -> {
                    every { chatHistoryService.getHistory(any()) } returns emptyList()
                    coEvery { openAiChatClient.chat(any(), any(), any()) } returns "AI response"
                    every { chatHistoryService.append(any(), any(), any()) } throws
                        ChatHistoryException("redis down")
                }
            }

            val thrown =
                assertThrows<Throwable> {
                    grpc.processMessage(
                        processMessageRequest {
                            this.chatId = testChatId
                            this.userId = testUserId
                            this.text = requestText
                        },
                    )
                }

            assertTrue(expectedExceptionType.isInstance(thrown))
            coVerify { subscriptionGrpcClient.refundAccess(testUserId, any(), any(), any()) }
        }

    @Test
    fun `clearHistory delegates to chatHistoryService`() =
        runTest {
            justRun { chatHistoryService.clear(any()) }

            grpc.clearHistory(clearContextRequest { chatId = testChatId })

            verify { chatHistoryService.clear(testChatId) }
        }

    @Test
    fun `processMessage increments prompts_total when prompt reaches OpenAI`() =
        runTest {
            accessAllowed()
            every { chatHistoryService.getHistory(any()) } returns emptyList()
            coEvery { openAiChatClient.chat(any(), any(), any()) } returns "AI response"
            justRun { chatHistoryService.append(any(), any(), any()) }

            grpc.processMessage(
                processMessageRequest {
                    this.chatId = testChatId
                    this.userId = testUserId
                    this.text = requestText
                },
            )

            assertEquals(1.0, meterRegistry.counter("prompts_total").count())
        }

    @Test
    fun `processMessage notifies moderation service when prompt is flagged`() =
        runTest {
            accessAllowed()
            coEvery { moderationClient.flaggedCategories(requestText) } returns
                listOf("violence", "hate")
            every { chatHistoryService.getHistory(any()) } returns emptyList()
            coEvery { openAiChatClient.chat(any(), any(), any()) } returns "AI response"
            justRun { chatHistoryService.append(any(), any(), any()) }

            grpc.processMessage(
                processMessageRequest {
                    this.chatId = testChatId
                    this.userId = testUserId
                    this.text = requestText
                },
            )

            coVerify(timeout = 1000) {
                moderationGrpcClient.notifyFlagged(
                    userId = testUserId,
                    chatId = testChatId,
                    promptText = requestText,
                    categories = listOf("violence", "hate"),
                )
            }
        }

    @Test
    fun `processMessage skips moderation notification when prompt is clean`() =
        runTest {
            accessAllowed()
            coEvery { moderationClient.flaggedCategories(requestText) } returns emptyList()
            every { chatHistoryService.getHistory(any()) } returns emptyList()
            coEvery { openAiChatClient.chat(any(), any(), any()) } returns "AI response"
            justRun { chatHistoryService.append(any(), any(), any()) }

            grpc.processMessage(
                processMessageRequest {
                    this.chatId = testChatId
                    this.userId = testUserId
                    this.text = requestText
                },
            )

            coVerify(exactly = 0) { moderationGrpcClient.notifyFlagged(any(), any(), any(), any()) }
        }

    @Test
    fun `processMessage continues when moderation check throws`() =
        runTest {
            accessAllowed()
            coEvery { moderationClient.flaggedCategories(any()) } throws RuntimeException("moderation down")
            every { chatHistoryService.getHistory(any()) } returns emptyList()
            coEvery { openAiChatClient.chat(any(), any(), any()) } returns "AI response"
            justRun { chatHistoryService.append(any(), any(), any()) }

            val response =
                grpc.processMessage(
                    processMessageRequest {
                        this.chatId = testChatId
                        this.userId = testUserId
                        this.text = requestText
                    },
                )

            assertEquals("AI response", response.text)
            coVerify(exactly = 0) { moderationGrpcClient.notifyFlagged(any(), any(), any(), any()) }
        }

    @Test
    fun `processMessage does not increment prompts_total when access is denied`() =
        runTest {
            coEvery { subscriptionGrpcClient.getModelPreference(any()) } returns "gpt-4o"
            coEvery { subscriptionGrpcClient.checkAccess(any(), any()) } returns
                CheckAccessResponse
                    .newBuilder()
                    .setAllowed(false)
                    .setDenialReason(DenialReason.QUOTA_EXHAUSTED)
                    .build()

            grpc.processMessage(
                processMessageRequest {
                    this.chatId = testChatId
                    this.userId = testUserId
                    this.text = requestText
                },
            )

            assertEquals(0.0, meterRegistry.counter("prompts_total").count())
        }

    companion object {
        @JvmStatic
        fun refundFailureCases(): Stream<Arguments> =
            Stream.of(
                Arguments.of("openai chat failure", FailureStage.OPENAI, OpenAiException::class),
                Arguments.of("history retrieval failure", FailureStage.GET_HISTORY, ChatHistoryException::class),
                Arguments.of("history append failure", FailureStage.APPEND, ChatHistoryException::class),
            )
    }

    enum class FailureStage {
        OPENAI,
        GET_HISTORY,
        APPEND,
    }
}
