package com.xeno.subpilot.tgbot.unittests.command

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.command.CommandResponses
import com.xeno.subpilot.tgbot.command.StartCommandHandler
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.User
import com.xeno.subpilot.tgbot.ux.buttons.BotButtons
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class StartCommandHandlerTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    private lateinit var startCommandHandler: StartCommandHandler

    @BeforeEach
    fun setUp() {
        startCommandHandler = StartCommandHandler(telegramClient)
    }

    @Test
    fun `return standard user name when username is null`() {
        val message =
            Message(
                chat = Chat(id = 1),
                from = null,
                text = "/start",
            )

        justRun { telegramClient.sendMessage(any(), any(), any()) }

        startCommandHandler.handle(message)

        verify {
            telegramClient.sendMessage(
                chatId = 1,
                text = CommandResponses.START_RESPONSE.format(StartCommandHandler.DEFAULT_USERNAME),
                replyMarkup = BotButtons.mainMenu,
            )
        }
    }

    @Test
    fun `uses firstName if present`() {
        val user = User(id = 1, firstName = "Mike")

        val message =
            Message(
                chat = Chat(id = 1),
                from = user,
                text = "/start",
            )

        justRun { telegramClient.sendMessage(any(), any(), any()) }

        startCommandHandler.handle(message)

        verify {
            telegramClient.sendMessage(
                chatId = 1,
                text = CommandResponses.START_RESPONSE.format("Mike"),
                replyMarkup = BotButtons.mainMenu,
            )
        }
    }

    @Test
    fun `uses default username when from exists but firstName is null`() {
        val user = User(id = 1, firstName = null)

        val message =
            Message(
                chat = Chat(id = 1),
                from = user,
                text = "/start",
            )

        justRun { telegramClient.sendMessage(any(), any(), any()) }

        startCommandHandler.handle(message)

        verify {
            telegramClient.sendMessage(
                chatId = 1,
                text = CommandResponses.START_RESPONSE.format(StartCommandHandler.DEFAULT_USERNAME),
                replyMarkup = BotButtons.mainMenu,
            )
        }
    }

    @Test
    fun `sends message to message chat id`() {
        val message =
            Message(
                chat = Chat(id = 987654321L),
                from = User(id = 1, firstName = "Mike"),
                text = "/start",
            )

        justRun { telegramClient.sendMessage(any(), any(), any()) }

        startCommandHandler.handle(message)

        verify {
            telegramClient.sendMessage(
                chatId = 987654321L,
                text = any(),
                replyMarkup = any(),
            )
        }
    }

    @Test
    fun `calls telegramClient sendMessage exactly once`() {
        val message =
            Message(
                chat = Chat(id = 1),
                from = User(id = 1, firstName = "Mike"),
                text = "/start",
            )

        justRun { telegramClient.sendMessage(any(), any(), any()) }

        startCommandHandler.handle(message)

        verify(exactly = 1) { telegramClient.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `exposes expected command and description`() {
        assertEquals("/start", startCommandHandler.command)
        assertEquals("Start the bot", startCommandHandler.description)
    }
}
