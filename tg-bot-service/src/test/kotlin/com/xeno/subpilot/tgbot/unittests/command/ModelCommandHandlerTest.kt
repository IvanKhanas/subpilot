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
package com.xeno.subpilot.tgbot.unittests.command

import com.xeno.subpilot.tgbot.client.ChatClient
import com.xeno.subpilot.tgbot.client.SubscriptionClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.command.ModelCommandHandler
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.ModelPreferenceResult
import com.xeno.subpilot.tgbot.dto.User
import com.xeno.subpilot.tgbot.exception.SubscriptionServiceException
import com.xeno.subpilot.tgbot.message.BotResponses
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

import kotlinx.coroutines.runBlocking

@ExtendWith(MockKExtension::class)
class ModelCommandHandlerTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    @MockK
    lateinit var subscriptionClient: SubscriptionClient

    @MockK
    lateinit var chatClient: ChatClient

    private val faker = Faker()

    private lateinit var handler: ModelCommandHandler

    private var chatId: Long = 0L
    private var userId: Long = 0L
    private var messageId: Long = 0L

    companion object {
        const val MODEL_COMMAND = "/model gpt-4o"
        const val UNKNOWN_MODEL_COMMAND = "/model unknown-model"
        const val INVALID_EMPTY_ARGS_COMMAND = "/model"
        const val INVALID_EXTRA_ARGS_COMMAND = "/model gpt-4o extra"
        const val TARGET_MODEL_ID = "gpt-4o"
    }

    private val noChange =
        ModelPreferenceResult(providerChanged = false, modelCost = 10, provider = "openai")
    private val providerChanged =
        ModelPreferenceResult(providerChanged = true, modelCost = 10, provider = "openai")

    @BeforeEach
    fun setUp() {
        chatId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        userId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        messageId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        handler = ModelCommandHandler(telegramClient, subscriptionClient, chatClient)
        every { telegramClient.sendMessage(any(), any()) } returns null
    }

    private fun message(text: String) =
        Message(
            messageId = messageId,
            from = User(id = userId),
            chat = Chat(id = chatId),
            text = text,
        )

    @ParameterizedTest(name = "command=''{0}''")
    @ValueSource(
        strings = [
            INVALID_EMPTY_ARGS_COMMAND,
            INVALID_EXTRA_ARGS_COMMAND,
        ],
    )
    fun `handle sends usage for invalid model command format`(command: String) {
        runBlocking { handler.handle(message(command)) }

        verify { telegramClient.sendMessage(chatId, match { it.startsWith("Usage:") }) }
    }

    @Test
    fun `handle sends not found when model id is unknown`() {
        runBlocking { handler.handle(message(UNKNOWN_MODEL_COMMAND)) }

        verify { telegramClient.sendMessage(chatId, match { it.contains("Unknown model") }) }
    }

    @Test
    fun `handle sends confirmation after setting model`() {
        coEvery { subscriptionClient.setModelPreference(userId, TARGET_MODEL_ID) } returns noChange

        runBlocking { handler.handle(message(MODEL_COMMAND)) }

        verify { telegramClient.sendMessage(chatId, match { it.contains("GPT-4o") }) }
    }

    @Test
    fun `handle clears context when provider changes`() {
        coEvery { subscriptionClient.setModelPreference(userId, TARGET_MODEL_ID) } returns providerChanged
        coJustRun { chatClient.clearContext(chatId) }

        runBlocking { handler.handle(message(MODEL_COMMAND)) }

        coVerify { chatClient.clearContext(chatId) }
    }

    @Test
    fun `handle does not clear context when provider is unchanged`() {
        coEvery { subscriptionClient.setModelPreference(userId, TARGET_MODEL_ID) } returns noChange

        runBlocking { handler.handle(message(MODEL_COMMAND)) }

        coVerify(exactly = 0) { chatClient.clearContext(any()) }
    }

    @Test
    fun `handle sends failure message when subscription service throws`() {
        coEvery { subscriptionClient.setModelPreference(any(), any()) } throws
            SubscriptionServiceException("failed")

        runBlocking { handler.handle(message(MODEL_COMMAND)) }

        verify { telegramClient.sendMessage(chatId, BotResponses.MODEL_SET_FAILED_RESPONSE.text) }
    }
}
