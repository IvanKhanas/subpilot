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

import kotlin.test.assertFalse
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

    private val userId = 1L
    private val chatId = 42L
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

    @Test
    fun `supports returns true for known model display name`() {
        assertTrue(handler.supports(model.displayName))
    }

    @Test
    fun `supports returns false for unknown text`() {
        assertFalse(handler.supports("UnknownModel"))
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

    @Test
    fun `handle does nothing when message text is null`() {
        runBlocking {
            handler.handle(Message(chat = Chat(id = chatId), from = User(id = userId), text = null))
        }

        coVerify(exactly = 0) { subscriptionClient.setModelPreference(any(), any()) }
        verify(exactly = 0) { telegramClient.sendMessage(any(), any(), any(), any()) }
    }

    @Test
    fun `handle does nothing when from is null`() {
        runBlocking {
            handler.handle(Message(chat = Chat(id = chatId), from = null, text = model.displayName))
        }

        coVerify(exactly = 0) { subscriptionClient.setModelPreference(any(), any()) }
        verify(exactly = 0) { telegramClient.sendMessage(any(), any(), any(), any()) }
    }
}
