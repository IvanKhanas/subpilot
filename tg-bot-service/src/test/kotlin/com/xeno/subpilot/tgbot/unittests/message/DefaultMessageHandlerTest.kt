/*
 * Copyright 2026 Ivan Khanas
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
package com.xeno.subpilot.tgbot.unittests.message

import com.xeno.subpilot.proto.chat.v1.ProcessMessageResponse
import com.xeno.subpilot.proto.subscription.v1.DenialReason
import com.xeno.subpilot.tgbot.client.ChatClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.User
import com.xeno.subpilot.tgbot.exception.ChatServiceException
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.message.DefaultMessageHandler
import com.xeno.subpilot.tgbot.util.AIResponseWaitingIndicator
import com.xeno.subpilot.tgbot.ux.AiProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

import java.util.stream.Stream

import kotlin.test.assertTrue

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class DefaultMessageHandlerTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    @MockK
    lateinit var chatClient: ChatClient

    @MockK
    lateinit var waitingIndicator: AIResponseWaitingIndicator

    private val faker = Faker()

    private lateinit var handler: DefaultMessageHandler
    private var chatId: Long = 0L
    private var userId: Long = 0L

    private val blockSlot = slot<suspend () -> ProcessMessageResponse>()
    private val inputText = "Hello"

    @BeforeEach
    fun setUp() {
        chatId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        userId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        handler = DefaultMessageHandler(telegramClient, chatClient, waitingIndicator)
    }

    private fun successResponse(text: String): ProcessMessageResponse =
        ProcessMessageResponse.newBuilder().setText(text).build()

    private fun deniedResponse(
        reason: DenialReason,
        modelId: String = "",
    ): ProcessMessageResponse =
        ProcessMessageResponse
            .newBuilder()
            .setDenialReason(reason)
            .setModelId(modelId)
            .build()

    @Test
    fun `forwards message to chat service and sends response to user`() =
        runTest {
            val message =
                Message(chat = Chat(id = chatId), from = User(id = userId), text = inputText)
            coEvery { waitingIndicator.wrap(any(), capture(blockSlot)) } coAnswers
                { blockSlot.captured() }
            coEvery { chatClient.processMessage(userId, chatId, inputText) } returns
                successResponse("AI response")
            every { telegramClient.sendMessage(any(), any(), any(), any()) } returns null

            handler.handle(message)

            verify(exactly = 1) {
                telegramClient.sendMessage(
                    chatId = chatId,
                    text = "AI response",
                    replyMarkup = null,
                    parseMode = "HTML",
                )
            }
        }

    @Test
    fun `sends AI unavailable response when chat service throws ChatServiceException`() =
        runTest {
            val message =
                Message(chat = Chat(id = chatId), from = User(id = userId), text = inputText)
            coEvery { waitingIndicator.wrap(any(), capture(blockSlot)) } coAnswers
                { blockSlot.captured() }
            coEvery { chatClient.processMessage(userId, chatId, inputText) } throws
                ChatServiceException("unavailable", RuntimeException())
            every { telegramClient.sendMessage(any(), any(), any(), any()) } returns null

            handler.handle(message)

            verify(exactly = 1) {
                telegramClient.sendMessage(
                    chatId = chatId,
                    text = BotResponses.AI_UNAVAILABLE_RESPONSE.text,
                )
            }
        }

    @ParameterizedTest(name = "{0}")
    @MethodSource("simpleDeniedCases")
    fun `sends mapped response for simple denial reasons`(
        caseName: String,
        denialReason: DenialReason,
        expectedText: String,
    ) = runTest {
        assertTrue(caseName.isNotBlank())
        val message =
            Message(chat = Chat(id = chatId), from = User(id = userId), text = inputText)
        coEvery { waitingIndicator.wrap(any(), capture(blockSlot)) } coAnswers
            { blockSlot.captured() }
        coEvery { chatClient.processMessage(userId, chatId, inputText) } returns
            deniedResponse(denialReason)
        every { telegramClient.sendMessage(any(), any(), any(), any()) } returns null

        handler.handle(message)

        verify(exactly = 1) {
            telegramClient.sendMessage(
                chatId = chatId,
                text = expectedText,
            )
        }
    }

    @Test
    fun `sends no subscription response with model name when access denied with NO_SUBSCRIPTION`() =
        runTest {
            val message =
                Message(chat = Chat(id = chatId), from = User(id = userId), text = inputText)
            coEvery { waitingIndicator.wrap(any(), capture(blockSlot)) } coAnswers
                { blockSlot.captured() }
            coEvery { chatClient.processMessage(userId, chatId, inputText) } returns
                deniedResponse(DenialReason.NO_SUBSCRIPTION, modelId = "gpt-4o-mini")
            every { telegramClient.sendMessage(any(), any(), any(), any()) } returns null

            handler.handle(message)

            val model = AiProvider.findModelById("gpt-4o-mini")!!
            val provider = AiProvider.findProviderByModelId("gpt-4o-mini")!!
            verify(exactly = 1) {
                telegramClient.sendMessage(
                    chatId = chatId,
                    text =
                        BotResponses.NO_SUBSCRIPTION_RESPONSE.format(
                            0,
                            provider.displayName,
                            model.displayName,
                            0,
                        ),
                )
            }
        }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidMessageCases")
    fun `ignores invalid messages`(
        caseName: String,
        message: Message,
    ) = runTest {
        assertTrue(caseName.isNotBlank())

        handler.handle(message)

        verify(exactly = 0) { telegramClient.sendMessage(any(), any(), any(), any()) }
        coVerify(exactly = 0) { chatClient.processMessage(any(), any(), any()) }
    }

    companion object {
        @JvmStatic
        fun simpleDeniedCases(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    "quota exhausted",
                    DenialReason.QUOTA_EXHAUSTED,
                    BotResponses.QUOTA_EXCEEDED_RESPONSE.text,
                ),
                Arguments.of(
                    "access blocked",
                    DenialReason.BLOCKED,
                    BotResponses.ACCESS_BLOCKED_RESPONSE.text,
                ),
            )

        @JvmStatic
        fun invalidMessageCases(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    "message without text",
                    Message(chat = Chat(id = 1L), from = User(id = 2L), text = null),
                ),
                Arguments.of(
                    "message without from",
                    Message(chat = Chat(id = 1L), from = null, text = "Hello"),
                ),
            )
    }
}
