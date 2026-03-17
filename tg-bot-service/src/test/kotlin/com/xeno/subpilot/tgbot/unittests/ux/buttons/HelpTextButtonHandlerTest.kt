package com.xeno.subpilot.tgbot.unittests.ux.buttons

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.command.CommandResponses
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.ux.buttons.BotButtons
import com.xeno.subpilot.tgbot.ux.buttons.HelpTextButtonHandler
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class HelpTextButtonHandlerTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    private lateinit var handler: HelpTextButtonHandler

    @BeforeEach
    fun setUp() {
        handler = HelpTextButtonHandler(telegramClient)
    }

    @Test
    fun `supports returns true for help button text`() {
        assertTrue(handler.supports(BotButtons.BTN_HELP))
    }

    @Test
    fun `supports returns false for unknown text`() {
        assertFalse(handler.supports("unknown"))
    }

    @Test
    fun `handle sends help response to chat`() {
        val message = Message(chat = Chat(id = 111), text = BotButtons.BTN_HELP)
        justRun { telegramClient.sendMessage(any(), any(), any()) }

        handler.handle(message)

        verify(exactly = 1) {
            telegramClient.sendMessage(
                chatId = 111,
                text = CommandResponses.HELP_RESPONSE.text,
                replyMarkup = null,
            )
        }
    }
}
