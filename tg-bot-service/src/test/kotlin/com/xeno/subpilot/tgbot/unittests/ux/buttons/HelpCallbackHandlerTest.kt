package com.xeno.subpilot.tgbot.unittests.ux.buttons

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.command.CommandResponses
import com.xeno.subpilot.tgbot.dto.CallbackQuery
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.ux.buttons.HelpCallbackHandler
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class HelpCallbackHandlerTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    private lateinit var handler: HelpCallbackHandler

    @BeforeEach
    fun setUp() {
        handler = HelpCallbackHandler(telegramClient)
    }

    @Test
    fun `sends help response when callback contains chat id`() {
        val callback = CallbackQuery(id = "cb-1", message = Message(chat = Chat(id = 99)))
        justRun { telegramClient.sendMessage(any(), any(), any()) }

        handler.handle(callback)

        verify(exactly = 1) {
            telegramClient.sendMessage(
                chatId = 99,
                text = CommandResponses.HELP_RESPONSE.text,
                replyMarkup = null,
            )
        }
    }

    @Test
    fun `does nothing when callback message is missing`() {
        handler.handle(CallbackQuery(id = "cb-2", message = null))

        verify(exactly = 0) { telegramClient.sendMessage(any(), any(), any()) }
    }
}
