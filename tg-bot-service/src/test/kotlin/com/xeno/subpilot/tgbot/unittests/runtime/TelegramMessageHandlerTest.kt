/*
 * Copyright 2024 Ivan Khanas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xeno.subpilot.tgbot.unittests.runtime

import com.xeno.subpilot.tgbot.client.SubscriptionClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.command.AdminBotCommand
import com.xeno.subpilot.tgbot.command.BotCommand
import com.xeno.subpilot.tgbot.dto.CallbackQuery
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.Update
import com.xeno.subpilot.tgbot.dto.User
import com.xeno.subpilot.tgbot.dto.UserInfoResult
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.message.CallbackHandler
import com.xeno.subpilot.tgbot.message.MessageHandler
import com.xeno.subpilot.tgbot.runtime.TelegramMessageHandler
import com.xeno.subpilot.tgbot.ux.buttons.TextButtonHandler
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

import java.util.stream.Stream

import kotlinx.coroutines.test.runTest

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
    lateinit var banCommand: AdminBotCommand

    @MockK
    lateinit var startChatCallback: CallbackHandler

    @MockK
    lateinit var helpCallback: CallbackHandler

    @MockK
    lateinit var chatTextButton: TextButtonHandler

    @MockK
    lateinit var helpTextButton: TextButtonHandler

    @MockK
    lateinit var subscriptionClient: SubscriptionClient

    private val faker = Faker()

    private lateinit var handler: TelegramMessageHandler
    private var chatId: Long = 0L
    private var regularUserId: Long = 0L
    private var adminUserId: Long = 0L
    private lateinit var firstCallbackId: String
    private lateinit var secondCallbackId: String

    @BeforeEach
    fun setUp() {
        chatId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        regularUserId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        adminUserId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        firstCallbackId = "cb-${faker.number().digits(8)}"
        secondCallbackId = "cb-${faker.number().digits(8)}"
        every { startCommand.command } returns "/start"
        every { helpCommand.command } returns "/help"
        every { banCommand.command } returns "/ban"

        handler =
            TelegramMessageHandler(
                botCommands = listOf(startCommand, helpCommand, banCommand),
                callbackHandlers = listOf(startChatCallback, helpCallback),
                textButtonHandlers = listOf(chatTextButton, helpTextButton),
                messageHandler = messageHandler,
                telegramClient = telegramClient,
                subscriptionClient = subscriptionClient,
            )
    }

    @Test
    fun `dispatches callback query to matching handler and answers callback`() =
        runTest {
            val callback = CallbackQuery(id = firstCallbackId, from = User(id = regularUserId), data = "start_chat")
            val update = Update(updateId = 1, callbackQuery = callback)

            every { startChatCallback.supports("start_chat") } returns true
            coJustRun { startChatCallback.handle(callback) }
            justRun { telegramClient.answerCallbackQuery(firstCallbackId) }

            handler.onUpdate(update)

            coVerify { startChatCallback.handle(callback) }
            verify { telegramClient.answerCallbackQuery(firstCallbackId) }
        }

    @Test
    fun `answers callback even when no matching handler found`() =
        runTest {
            val callback = CallbackQuery(id = secondCallbackId, from = User(id = regularUserId), data = "unknown_action")
            val update = Update(updateId = 2, callbackQuery = callback)

            every { startChatCallback.supports("unknown_action") } returns false
            every { helpCallback.supports("unknown_action") } returns false
            justRun { telegramClient.answerCallbackQuery(secondCallbackId) }

            handler.onUpdate(update)

            coVerify(exactly = 0) { startChatCallback.handle(any()) }
            coVerify(exactly = 0) { helpCallback.handle(any()) }
            verify { telegramClient.answerCallbackQuery(secondCallbackId) }
        }

    @Test
    fun `routes slash command to matching BotCommand handler`() =
        runTest {
            val message = message("/start", regularUserId)
            val update = Update(updateId = 3, message = message)

            coJustRun { startCommand.handle(message) }

            handler.onUpdate(update)

            coVerify { startCommand.handle(message) }
        }

    @Test
    fun `strips bot mention from command before matching`() =
        runTest {
            val message = message("/help@MyBot", regularUserId)
            val update = Update(updateId = 4, message = message)

            coJustRun { helpCommand.handle(message) }

            handler.onUpdate(update)

            coVerify { helpCommand.handle(message) }
        }

    @Test
    fun `sends unknown command response for unrecognized command`() =
        runTest {
            val message = message("/unknown", regularUserId)
            val update = Update(updateId = 5, message = message)
            every { telegramClient.sendMessage(any(), any(), any(), any()) } returns null

            handler.onUpdate(update)

            coVerify(exactly = 0) { startCommand.handle(any()) }
            coVerify(exactly = 0) { helpCommand.handle(any()) }
            verify {
                telegramClient.sendMessage(
                    chatId = chatId,
                    text = BotResponses.UNKNOWN_COMMAND_RESPONSE.text,
                )
            }
        }

    @Test
    fun `routes admin command when user has admin role`() =
        runTest {
            val message = message("/ban 123", adminUserId)
            val update = Update(updateId = 10, message = message)
            coEvery { subscriptionClient.getUserInfo(adminUserId) } returns
                UserInfoResult(
                    blocked = false,
                    role = "ADMIN",
                    registeredAtEpoch = 0,
                )
            coJustRun { banCommand.handle(message) }

            handler.onUpdate(update)

            coVerify(exactly = 1) { banCommand.handle(message) }
            verify(exactly = 0) { telegramClient.sendMessage(any(), BotResponses.UNKNOWN_COMMAND_RESPONSE.text, any(), any()) }
        }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nonAdminCases")
    fun `rejects admin command for unknown or non-admin users`(
        caseName: String,
        userInfo: UserInfoResult?,
    ) =
        runTest {
            kotlin.test.assertTrue(caseName.isNotBlank())
            val message = message(ADMIN_COMMAND_TEXT, regularUserId)
            val update = Update(updateId = 11, message = message)
            coEvery { subscriptionClient.getUserInfo(regularUserId) } returns userInfo
            every { telegramClient.sendMessage(any(), any(), any(), any()) } returns null

            handler.onUpdate(update)

            coVerify(exactly = 0) { banCommand.handle(any()) }
            verify {
                telegramClient.sendMessage(
                    chatId = chatId,
                    text = BotResponses.UNKNOWN_COMMAND_RESPONSE.text,
                )
            }
        }

    @Test
    fun `routes text to matching TextButtonHandler`() =
        runTest {
            val message = message("Start chat", regularUserId)
            val update = Update(updateId = 6, message = message)

            every { chatTextButton.supports("Start chat") } returns true
            coJustRun { chatTextButton.handle(message) }
            justRun { telegramClient.deleteMessage(any(), any()) }

            handler.onUpdate(update)

            coVerify { chatTextButton.handle(message) }
            coVerify(exactly = 0) { messageHandler.handle(any()) }
        }

    @Test
    fun `falls through to default MessageHandler when no text button matches`() =
        runTest {
            val message = message("Hello, bot!", regularUserId)
            val update = Update(updateId = 7, message = message)

            every { chatTextButton.supports("Hello, bot!") } returns false
            every { helpTextButton.supports("Hello, bot!") } returns false
            coJustRun { messageHandler.handle(message) }

            handler.onUpdate(update)

            coVerify { messageHandler.handle(message) }
        }

    @Test
    fun `ignores update with neither message nor callback`() =
        runTest {
            val update = Update(updateId = 8)

            handler.onUpdate(update)

            verify(exactly = 0) { telegramClient.answerCallbackQuery(any()) }
            coVerify(exactly = 0) { messageHandler.handle(any()) }
        }

    @Test
    fun `ignores message with null text`() =
        runTest {
            val message = Message(messageId = 1, chat = Chat(id = chatId), text = null)
            val update = Update(updateId = 9, message = message)

            handler.onUpdate(update)

            coVerify(exactly = 0) { messageHandler.handle(any()) }
        }

    private fun message(
        text: String,
        userId: Long,
    ) =
        Message(
            messageId = 1,
            chat = Chat(id = chatId),
            text = text,
            from = User(id = userId, firstName = "Test"),
        )

    companion object {
        private const val ADMIN_COMMAND_TEXT = "/ban 123"

        @JvmStatic
        fun nonAdminCases(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    "non-admin role",
                    UserInfoResult(
                        blocked = false,
                        role = "USER",
                        registeredAtEpoch = 0,
                    ),
                ),
                Arguments.of("unknown user", null),
            )
    }
}
