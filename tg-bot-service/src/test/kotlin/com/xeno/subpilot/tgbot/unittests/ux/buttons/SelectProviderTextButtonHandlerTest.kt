package com.xeno.subpilot.tgbot.unittests.ux.buttons

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.ux.AiProvider
import com.xeno.subpilot.tgbot.ux.BotScreen
import com.xeno.subpilot.tgbot.ux.NavigationService
import com.xeno.subpilot.tgbot.ux.buttons.SelectProviderTextButtonHandler
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
class SelectProviderTextButtonHandlerTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    @MockK
    lateinit var navigationService: NavigationService

    private lateinit var handler: SelectProviderTextButtonHandler

    private val chatId = 42L
    private val provider = AiProvider.OPENAI

    @BeforeEach
    fun setUp() {
        justRun { navigationService.push(any(), any()) }
        every { telegramClient.sendMessage(any(), any(), any(), any()) } returns null
        handler = SelectProviderTextButtonHandler(telegramClient, navigationService)
    }

    @Test
    fun `supports returns true for known provider display name`() {
        assertTrue(handler.supports(provider.displayName))
    }

    @Test
    fun `supports returns false for unknown text`() {
        assertFalse(handler.supports("UnknownProvider"))
    }

    @Test
    fun `handle pushes PROVIDER_MENU to navigation stack`() {
        handler.handle(Message(chat = Chat(id = chatId), text = provider.displayName))

        verify { navigationService.push(chatId, BotScreen.PROVIDER_MENU) }
    }

    @Test
    fun `handle sends model selection message for the provider`() {
        handler.handle(Message(chat = Chat(id = chatId), text = provider.displayName))

        verify { telegramClient.sendMessage(chatId, any(), any(), any()) }
    }

    @Test
    fun `handle does nothing when message text is null`() {
        handler.handle(Message(chat = Chat(id = chatId), text = null))

        verify(exactly = 0) { telegramClient.sendMessage(any(), any(), any(), any()) }
        verify(exactly = 0) { navigationService.push(any(), any()) }
    }
}
