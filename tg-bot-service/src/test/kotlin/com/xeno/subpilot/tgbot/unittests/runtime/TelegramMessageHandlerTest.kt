package com.xeno.subpilot.tgbot.unittests.runtime

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.command.BotCommand
import com.xeno.subpilot.tgbot.dto.CallbackQuery
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.Update
import com.xeno.subpilot.tgbot.dto.User
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.message.CallbackHandler
import com.xeno.subpilot.tgbot.message.MessageHandler
import com.xeno.subpilot.tgbot.runtime.TelegramMessageHandler
import com.xeno.subpilot.tgbot.ux.buttons.TextButtonHandler
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class TelegramMessageHandlerTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    @MockK
    lateinit var messageHandler: MessageHandler

    @MockK
    lateinit var startCommand: BotCommand

    @MockK
    lateinit var helpCommand: BotCommand

    @MockK
    lateinit var startChatCallback: CallbackHandler

    @MockK
    lateinit var helpCallback: CallbackHandler

    @MockK
    lateinit var chatTextButton: TextButtonHandler

    @MockK
    lateinit var helpTextButton: TextButtonHandler

    private lateinit var handler: TelegramMessageHandler

    @BeforeEach
    fun setUp() {
        every { startCommand.command } returns "/start"
        every { helpCommand.command } returns "/help"

        handler =
            TelegramMessageHandler(
                botCommands = listOf(startCommand, helpCommand),
                callbackHandlers = listOf(startChatCallback, helpCallback),
                textButtonHandlers = listOf(chatTextButton, helpTextButton),
                messageHandler = messageHandler,
                telegramClient = telegramClient,
            )
    }

    @Test
    fun `dispatches callback query to matching handler and answers callback`() {
        val callback = CallbackQuery(id = "cb-1", from = User(id = 42), data = "start_chat")
        val update = Update(updateId = 1, callbackQuery = callback)

        every { startChatCallback.supports("start_chat") } returns true
        justRun { startChatCallback.handle(callback) }
        justRun { telegramClient.answerCallbackQuery("cb-1") }

        handler.onUpdate(update)

        verify { startChatCallback.handle(callback) }
        verify { telegramClient.answerCallbackQuery("cb-1") }
    }

    @Test
    fun `answers callback even when no matching handler found`() {
        val callback = CallbackQuery(id = "cb-2", from = User(id = 42), data = "unknown_action")
        val update = Update(updateId = 2, callbackQuery = callback)

        every { startChatCallback.supports("unknown_action") } returns false
        every { helpCallback.supports("unknown_action") } returns false
        justRun { telegramClient.answerCallbackQuery("cb-2") }

        handler.onUpdate(update)

        verify(exactly = 0) { startChatCallback.handle(any()) }
        verify(exactly = 0) { helpCallback.handle(any()) }
        verify { telegramClient.answerCallbackQuery("cb-2") }
    }

    @Test
    fun `routes slash command to matching BotCommand handler`() {
        val message = message("/start")
        val update = Update(updateId = 3, message = message)

        justRun { startCommand.handle(message) }

        handler.onUpdate(update)

        verify { startCommand.handle(message) }
    }

    @Test
    fun `strips bot mention from command before matching`() {
        val message = message("/help@MyBot")
        val update = Update(updateId = 4, message = message)

        justRun { helpCommand.handle(message) }

        handler.onUpdate(update)

        verify { helpCommand.handle(message) }
    }

    @Test
    fun `sends unknown command response for unrecognized command`() {
        val message = message("/unknown")
        val update = Update(updateId = 5, message = message)
        every { telegramClient.sendMessage(any(), any(), any(), any()) } returns null

        handler.onUpdate(update)

        verify(exactly = 0) { startCommand.handle(any()) }
        verify(exactly = 0) { helpCommand.handle(any()) }
        verify {
            telegramClient.sendMessage(
                chatId = 100,
                text = BotResponses.UNKNOWN_COMMAND_RESPONSE.text,
            )
        }
    }

    @Test
    fun `routes text to matching TextButtonHandler`() {
        val message = message("Start chat")
        val update = Update(updateId = 6, message = message)

        every { chatTextButton.supports("Start chat") } returns true
        justRun { chatTextButton.handle(message) }

        handler.onUpdate(update)

        verify { chatTextButton.handle(message) }
        verify(exactly = 0) { messageHandler.handle(any()) }
    }

    @Test
    fun `falls through to default MessageHandler when no text button matches`() {
        val message = message("Hello, bot!")
        val update = Update(updateId = 7, message = message)

        every { chatTextButton.supports("Hello, bot!") } returns false
        every { helpTextButton.supports("Hello, bot!") } returns false
        justRun { messageHandler.handle(message) }

        handler.onUpdate(update)

        verify { messageHandler.handle(message) }
    }

    @Test
    fun `ignores update with neither message nor callback`() {
        val update = Update(updateId = 8)

        handler.onUpdate(update)

        verify(exactly = 0) { telegramClient.answerCallbackQuery(any()) }
        verify(exactly = 0) { messageHandler.handle(any()) }
    }

    @Test
    fun `ignores message with null text`() {
        val message = Message(messageId = 1, chat = Chat(id = 100), text = null)
        val update = Update(updateId = 9, message = message)

        handler.onUpdate(update)

        verify(exactly = 0) { messageHandler.handle(any()) }
    }

    private fun message(text: String) =
        Message(
            messageId = 1,
            chat = Chat(id = 100),
            text = text,
            from = User(id = 42, firstName = "Test"),
        )
}
