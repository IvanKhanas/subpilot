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
package com.xeno.subpilot.tgbot.unittests.ux.buttons

import com.xeno.subpilot.tgbot.client.ChatClient
import com.xeno.subpilot.tgbot.client.SubscriptionClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.ModelPreferenceResult
import com.xeno.subpilot.tgbot.dto.User
import com.xeno.subpilot.tgbot.ux.AiProvider
import com.xeno.subpilot.tgbot.ux.buttons.SelectModelTextButtonHandler
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

import java.util.stream.Stream

import kotlin.test.assertEquals
import kotlin.test.assertTrue

import kotlinx.coroutines.runBlocking

@ExtendWith(MockKExtension::class)
class SelectModelTextButtonHandlerTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    @MockK
    lateinit var subscriptionClient: SubscriptionClient

    @MockK
    lateinit var chatClient: ChatClient

    private lateinit var handler: SelectModelTextButtonHandler

    private val userId = TEST_USER_ID
    private val chatId = TEST_CHAT_ID
    private val model = AiProvider.OPENAI.models.first()
    private val resultNoChange =
        ModelPreferenceResult(providerChanged = false, modelCost = 10, provider = "openai")
    private val resultProviderChanged =
        ModelPreferenceResult(providerChanged = true, modelCost = 10, provider = "openai")

    @BeforeEach
    fun setUp() {
        every { telegramClient.sendMessage(any(), any(), any(), any()) } returns null
        handler = SelectModelTextButtonHandler(telegramClient, subscriptionClient, chatClient)
    }

    @ParameterizedTest(name = "text={0} -> supports={1}")
    @MethodSource("supportsCases")
    fun `supports handles known and unknown texts`(
        text: String,
        expectedSupports: Boolean,
    ) {
        assertEquals(expectedSupports, handler.supports(text))
    }

    @Test
    fun `handle sets model preference via subscriptionClient`() {
        coEvery { subscriptionClient.setModelPreference(userId, model.id) } returns resultNoChange

        runBlocking {
            handler.handle(
                Message(
                    chat = Chat(id = chatId),
                    from = User(id = userId),
                    text = model.displayName,
                ),
            )
        }

        coVerify { subscriptionClient.setModelPreference(userId, model.id) }
    }

    @Test
    fun `handle clears context when provider changed`() {
        coEvery { subscriptionClient.setModelPreference(userId, model.id) } returns
            resultProviderChanged
        coJustRun { chatClient.clearContext(chatId) }

        runBlocking {
            handler.handle(
                Message(
                    chat = Chat(id = chatId),
                    from = User(id = userId),
                    text = model.displayName,
                ),
            )
        }

        coVerify { chatClient.clearContext(chatId) }
    }

    @Test
    fun `handle does not clear context when provider did not change`() {
        coEvery { subscriptionClient.setModelPreference(userId, model.id) } returns resultNoChange

        runBlocking {
            handler.handle(
                Message(
                    chat = Chat(id = chatId),
                    from = User(id = userId),
                    text = model.displayName,
                ),
            )
        }

        coVerify(exactly = 0) { chatClient.clearContext(any()) }
    }

    @Test
    fun `handle sends confirmation message to chat`() {
        coEvery { subscriptionClient.setModelPreference(userId, model.id) } returns resultNoChange

        runBlocking {
            handler.handle(
                Message(
                    chat = Chat(id = chatId),
                    from = User(id = userId),
                    text = model.displayName,
                ),
            )
        }

        verify { telegramClient.sendMessage(chatId, any(), any(), any()) }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidMessageCases")
    fun `handle does nothing for invalid incoming messages`(
        caseName: String,
        message: Message,
    ) {
        assertTrue(caseName.isNotBlank())
        runBlocking { handler.handle(message) }
        coVerify(exactly = 0) { subscriptionClient.setModelPreference(any(), any()) }
        verify(exactly = 0) { telegramClient.sendMessage(any(), any(), any(), any()) }
    }

    companion object {
        private const val TEST_USER_ID = 1L
        private const val TEST_CHAT_ID = 42L
        private const val UNKNOWN_MODEL_TEXT = "UnknownModel"
        private val KNOWN_MODEL_TEXT =
            AiProvider.OPENAI.models
                .first()
                .displayName

        @JvmStatic
        fun supportsCases(): Stream<Arguments> =
            Stream.of(
                Arguments.of(KNOWN_MODEL_TEXT, true),
                Arguments.of(UNKNOWN_MODEL_TEXT, false),
            )

        @JvmStatic
        fun invalidMessageCases(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    "null text",
                    Message(
                        chat = Chat(id = TEST_CHAT_ID),
                        from = User(id = TEST_USER_ID),
                        text = null,
                    ),
                ),
                Arguments.of(
                    "null from",
                    Message(
                        chat = Chat(id = TEST_CHAT_ID),
                        from = null,
                        text = KNOWN_MODEL_TEXT,
                    ),
                ),
            )
    }
}
