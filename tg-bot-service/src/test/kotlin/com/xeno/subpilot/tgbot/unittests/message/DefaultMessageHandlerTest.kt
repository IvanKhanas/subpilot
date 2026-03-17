package com.xeno.subpilot.tgbot.unittests.message

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.message.DefaultMessageHandler
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class DefaultMessageHandlerTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    private lateinit var handler: DefaultMessageHandler

    @BeforeEach
    fun setUp() {
        handler = DefaultMessageHandler(telegramClient)
    }

    @Test
    fun `sends fallback response to message chat`() {
        val message = Message(chat = Chat(id = 42), text = "Hello")
        justRun { telegramClient.sendMessage(any(), any(), any()) }

        handler.handle(message)

        verify(exactly = 1) {
            telegramClient.sendMessage(
                chatId = 42,
                text = "I got your message. AI integration is still in progress.",
                replyMarkup = null,
            )
        }
    }
}
