package com.xeno.subpilot.tgbot.unittests.runtime

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.command.BotCommand
import com.xeno.subpilot.tgbot.dto.BotCommandInfo
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.Update
import com.xeno.subpilot.tgbot.properties.TelegramBotProperties
import com.xeno.subpilot.tgbot.runtime.TelegramLongPollingService
import com.xeno.subpilot.tgbot.runtime.TelegramMessageHandler
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import java.util.concurrent.Executors

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher

@ExtendWith(MockKExtension::class)
class TelegramLongPollingServiceTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    @MockK
    lateinit var messageHandler: TelegramMessageHandler

    @MockK
    lateinit var startCommand: BotCommand

    @MockK
    lateinit var helpCommand: BotCommand

    private lateinit var service: TelegramLongPollingService
    private lateinit var ioDispatcher: ExecutorCoroutineDispatcher

    @BeforeEach
    fun setUp() {
        ioDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        every { startCommand.command } returns "/start"
        every { startCommand.description } returns "Start the bot"
        every { helpCommand.command } returns "/help"
        every { helpCommand.description } returns "Show available commands"
        justRun { telegramClient.setMyCommands(any()) }
        justRun { messageHandler.onUpdate(any()) }

        service =
            TelegramLongPollingService(
                telegramClient = telegramClient,
                messageHandler = messageHandler,
                botCommands = listOf(startCommand, helpCommand),
                properties = TelegramBotProperties(token = "token", pollingTimeout = 15),
                ioDispatcher = ioDispatcher,
            )
    }

    @AfterEach
    fun tearDown() {
        service.stop()
        ioDispatcher.close()
    }

    @Test
    fun `start registers commands without slash prefix`() {
        val commandsSlot = slot<List<BotCommandInfo>>()
        every { telegramClient.getUpdates(any(), 15) } throws InterruptedException()

        service.start()

        verify(timeout = 1500) { telegramClient.setMyCommands(capture(commandsSlot)) }
        assertEquals(2, commandsSlot.captured.size)
        assertEquals("start", commandsSlot.captured[0].command)
        assertEquals("Start the bot", commandsSlot.captured[0].description)
        assertEquals("help", commandsSlot.captured[1].command)
        assertEquals("Show available commands", commandsSlot.captured[1].description)
    }

    @Test
    fun `polling passes updates to message handler and increments offset`() {
        val update = Update(updateId = 5, message = Message(chat = Chat(id = 1), text = "hello"))
        every { telegramClient.getUpdates(null, 15) } returns listOf(update)
        every { telegramClient.getUpdates(6, 15) } throws InterruptedException()

        service.start()

        verify(timeout = 1500) { messageHandler.onUpdate(update) }
        verify(timeout = 1500) { telegramClient.getUpdates(6, 15) }
    }

    @Test
    fun `polling continues after exception thrown by message handler`() {
        val badUpdate = Update(updateId = 1, message = Message(chat = Chat(id = 1), text = "bad"))
        val goodUpdate = Update(updateId = 2, message = Message(chat = Chat(id = 1), text = "good"))
        every { telegramClient.getUpdates(null, 15) } returns listOf(badUpdate)
        every { telegramClient.getUpdates(2, 15) } returns listOf(goodUpdate)
        every { telegramClient.getUpdates(3, 15) } throws InterruptedException()
        every { messageHandler.onUpdate(badUpdate) } throws RuntimeException("crash!")

        service.start()

        verify(timeout = 2000) { messageHandler.onUpdate(goodUpdate) }
    }
}
