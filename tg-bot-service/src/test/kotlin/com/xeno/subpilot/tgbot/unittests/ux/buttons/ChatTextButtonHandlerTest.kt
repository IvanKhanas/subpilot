package com.xeno.subpilot.tgbot.unittests.ux.buttons

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.ux.buttons.BotButtons
import com.xeno.subpilot.tgbot.ux.buttons.ChatTextButtonHandler
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class ChatTextButtonHandlerTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    private lateinit var handler: ChatTextButtonHandler

    @BeforeEach
    fun setUp() {
        handler = ChatTextButtonHandler(telegramClient)
    }

    @Test
    fun `supports returns true for BTN_CHAT text`() {
        Assertions.assertTrue(handler.supports(BotButtons.BTN_CHAT))
    }

    @Test
    fun `supports returns false for arbitrary text`() {
        Assertions.assertFalse(handler.supports("random text"))
    }

    @Test
    fun `supports returns false for empty string`() {
        Assertions.assertFalse(handler.supports(""))
    }

    @Test
    fun `handle sends default message to correct chat id`() {
        val message =
            Message(
                chat = Chat(id = 42L),
                text = BotButtons.BTN_CHAT,
            )

        justRun { telegramClient.sendMessage(any(), any(), any()) }

        handler.handle(message)

        verify {
            telegramClient.sendMessage(
                chatId = 42L,
                text = ChatTextButtonHandler.Companion.DEFAULT_MESSAGE,
            )
        }
    }

    @Test
    fun `handle calls sendMessage exactly once`() {
        val message =
            Message(
                chat = Chat(id = 1L),
                text = BotButtons.BTN_CHAT,
            )

        justRun { telegramClient.sendMessage(any(), any(), any()) }

        handler.handle(message)

        verify(exactly = 1) { telegramClient.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `handle uses chat id from message`() {
        val message =
            Message(
                chat = Chat(id = 999999L),
                text = BotButtons.BTN_CHAT,
            )

        justRun { telegramClient.sendMessage(any(), any(), any()) }

        handler.handle(message)

        verify {
            telegramClient.sendMessage(
                chatId = 999999L,
                text = any(),
            )
        }
    }
}
