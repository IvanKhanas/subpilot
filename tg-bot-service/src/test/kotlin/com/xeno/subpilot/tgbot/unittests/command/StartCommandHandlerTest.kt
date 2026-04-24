package com.xeno.subpilot.tgbot.unittests.command

import com.xeno.subpilot.tgbot.client.SubscriptionClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.command.StartCommandHandler
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.User
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.ux.BotScreen
import com.xeno.subpilot.tgbot.ux.NavigationService
import com.xeno.subpilot.tgbot.ux.ScreenRenderer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class StartCommandHandlerTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    @MockK
    lateinit var navigationService: NavigationService

    @MockK
    lateinit var screenRenderer: ScreenRenderer

    @MockK
    lateinit var subscriptionClient: SubscriptionClient

    private lateinit var startCommandHandler: StartCommandHandler

    @BeforeEach
    fun setUp() {
        startCommandHandler =
            StartCommandHandler(
                telegramClient,
                navigationService,
                screenRenderer,
                subscriptionClient,
            )
        coEvery { subscriptionClient.registerUser(any()) } returns null
    }

    @Test
    fun `uses default username when from is null`() =
        runTest {
            val message = Message(chat = Chat(id = 1), from = null, text = "/start")
            justRun { navigationService.clear(any()) }
            every { telegramClient.sendMessage(any(), any(), any(), any()) } returns null
            justRun { screenRenderer.render(any(), any()) }

            startCommandHandler.handle(message)

            verify {
                telegramClient.sendMessage(
                    chatId = 1,
                    text =
                        BotResponses.START_ALREADY_REGISTERED_USER_RESPONSE.format(
                            StartCommandHandler.DEFAULT_USERNAME,
                        ),
                )
            }
        }

    @Test
    fun `uses firstName if present`() =
        runTest {
            val message =
                Message(
                    chat = Chat(id = 1),
                    from = User(id = 1, firstName = "Mike"),
                    text = "/start",
                )
            justRun { navigationService.clear(any()) }
            every { telegramClient.sendMessage(any(), any(), any(), any()) } returns null
            justRun { screenRenderer.render(any(), any()) }

            startCommandHandler.handle(message)

            verify {
                telegramClient.sendMessage(
                    chatId = 1,
                    text = BotResponses.START_ALREADY_REGISTERED_USER_RESPONSE.format("Mike"),
                )
            }
        }

    @Test
    fun `uses default username when from exists but firstName is null`() =
        runTest {
            val message =
                Message(chat = Chat(id = 1), from = User(id = 1, firstName = null), text = "/start")
            justRun { navigationService.clear(any()) }
            every { telegramClient.sendMessage(any(), any(), any(), any()) } returns null
            justRun { screenRenderer.render(any(), any()) }

            startCommandHandler.handle(message)

            verify {
                telegramClient.sendMessage(
                    chatId = 1,
                    text =
                        BotResponses.START_ALREADY_REGISTERED_USER_RESPONSE.format(
                            StartCommandHandler.DEFAULT_USERNAME,
                        ),
                )
            }
        }

    @Test
    fun `clears navigation stack on start`() =
        runTest {
            val message = Message(chat = Chat(id = 42), from = User(id = 1), text = "/start")
            justRun { navigationService.clear(any()) }
            every { telegramClient.sendMessage(any(), any(), any(), any()) } returns null
            justRun { screenRenderer.render(any(), any()) }

            startCommandHandler.handle(message)

            verify { navigationService.clear(42) }
        }

    @Test
    fun `renders main menu after greeting`() =
        runTest {
            val message = Message(chat = Chat(id = 42), from = User(id = 1), text = "/start")
            justRun { navigationService.clear(any()) }
            every { telegramClient.sendMessage(any(), any(), any(), any()) } returns null
            justRun { screenRenderer.render(any(), any()) }

            startCommandHandler.handle(message)

            verify { screenRenderer.render(42, BotScreen.MAIN_MENU) }
        }

    @Test
    fun `sends message to correct chat id`() =
        runTest {
            val message =
                Message(
                    chat = Chat(id = 987654321L),
                    from = User(id = 1, firstName = "Mike"),
                    text = "/start",
                )
            justRun { navigationService.clear(any()) }
            every { telegramClient.sendMessage(any(), any(), any(), any()) } returns null
            justRun { screenRenderer.render(any(), any()) }

            startCommandHandler.handle(message)

            verify { telegramClient.sendMessage(chatId = 987654321L, text = any()) }
        }

    @Test
    fun `exposes expected command and description`() {
        assertEquals("/start", startCommandHandler.command)
        assertEquals("Start the bot", startCommandHandler.description)
    }
}
