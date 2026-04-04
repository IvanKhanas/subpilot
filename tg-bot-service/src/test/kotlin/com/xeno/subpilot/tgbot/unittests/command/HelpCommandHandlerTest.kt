package com.xeno.subpilot.tgbot.unittests.command

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.command.HelpCommandHandler
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.message.BotResponses
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class HelpCommandHandlerTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    private lateinit var helpCommandHandler: HelpCommandHandler

    @BeforeEach
    fun setUp() {
        helpCommandHandler = HelpCommandHandler(telegramClient)
        every { telegramClient.sendMessage(any(), any(), any(), any()) } returns null
    }

    @Test
    fun `sends help response text`() {
        val message = Message(chat = Chat(id = 1), text = "/help")

        helpCommandHandler.handle(message)

        verify {
            telegramClient.sendMessage(
                chatId = 1,
                text = BotResponses.HELP_RESPONSE.text,
            )
        }
    }

    @Test
    fun `sends message to message chat id`() {
        val message = Message(chat = Chat(id = 123456789L), text = "/help")

        helpCommandHandler.handle(message)

        verify {
            telegramClient.sendMessage(
                chatId = 123456789L,
                text = any(),
            )
        }
    }

    @Test
    fun `calls telegramClient sendMessage exactly once`() {
        val message = Message(chat = Chat(id = 1), text = "/help")

        helpCommandHandler.handle(message)

        verify(exactly = 1) { telegramClient.sendMessage(any(), any(), any(), any()) }
    }

    @Test
    fun `exposes expected command and description`() {
        assertEquals("/help", helpCommandHandler.command)
        assertEquals("Show available commands", helpCommandHandler.description)
    }
}
