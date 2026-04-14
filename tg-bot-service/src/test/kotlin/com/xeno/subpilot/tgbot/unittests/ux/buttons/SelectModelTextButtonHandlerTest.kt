package com.xeno.subpilot.tgbot.unittests.ux.buttons

import com.xeno.subpilot.tgbot.client.ChatClient
import com.xeno.subpilot.tgbot.client.SubscriptionClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.User
import com.xeno.subpilot.tgbot.ux.AiProvider
import com.xeno.subpilot.tgbot.ux.buttons.SelectModelTextButtonHandler
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        every { subscriptionClient.setModelPreference(userId, model.id) } returns false

        handler.handle(
            Message(chat = Chat(id = chatId), from = User(id = userId), text = model.displayName),
        )

        verify { subscriptionClient.setModelPreference(userId, model.id) }
    }

    @Test
    fun `handle clears history when provider changed`() {
        every { subscriptionClient.setModelPreference(userId, model.id) } returns true
        justRun { chatClient.clearHistory(chatId) }

        handler.handle(
            Message(chat = Chat(id = chatId), from = User(id = userId), text = model.displayName),
        )

        verify { chatClient.clearHistory(chatId) }
    }

    @Test
    fun `handle does not clear history when provider did not change`() {
        every { subscriptionClient.setModelPreference(userId, model.id) } returns false

        handler.handle(
            Message(chat = Chat(id = chatId), from = User(id = userId), text = model.displayName),
        )

        verify(exactly = 0) { chatClient.clearHistory(any()) }
    }

    @Test
    fun `handle sends confirmation message to chat`() {
        every { subscriptionClient.setModelPreference(userId, model.id) } returns false

        handler.handle(
            Message(chat = Chat(id = chatId), from = User(id = userId), text = model.displayName),
        )

        verify { telegramClient.sendMessage(chatId, any(), any(), any()) }
    }

    @Test
    fun `handle does nothing when message text is null`() {
        handler.handle(Message(chat = Chat(id = chatId), from = User(id = userId), text = null))

        verify(exactly = 0) { subscriptionClient.setModelPreference(any(), any()) }
        verify(exactly = 0) { telegramClient.sendMessage(any(), any(), any(), any()) }
    }

    @Test
    fun `handle does nothing when from is null`() {
        handler.handle(Message(chat = Chat(id = chatId), from = null, text = model.displayName))

        verify(exactly = 0) { subscriptionClient.setModelPreference(any(), any()) }
        verify(exactly = 0) { telegramClient.sendMessage(any(), any(), any(), any()) }
    }
}
