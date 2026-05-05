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
package com.xeno.subpilot.tgbot.unittests.command

import com.xeno.subpilot.tgbot.client.SubscriptionClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.command.AdminBotCommand
import com.xeno.subpilot.tgbot.command.BanCommandHandler
import com.xeno.subpilot.tgbot.command.UnbanCommandHandler
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.exception.SubscriptionServiceException
import com.xeno.subpilot.tgbot.message.BotResponses
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class AdminUserActionCommandHandlerTest {

    enum class ActionCase(
        val command: String,
        val successResponse: BotResponses,
    ) {
        BAN("/ban", BotResponses.ADMIN_BAN_SUCCESS_RESPONSE),
        UNBAN("/unban", BotResponses.ADMIN_UNBAN_SUCCESS_RESPONSE),
    }

    @MockK
    lateinit var telegramClient: TelegramClient

    @MockK
    lateinit var subscriptionClient: SubscriptionClient

    private val faker = Faker()
    private var chatId: Long = 0L
    private var targetUserId: Long = 0L
    private var sentMessageId: Long = 0L

    @BeforeEach
    fun setUp() {
        chatId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        targetUserId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        sentMessageId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        every { telegramClient.sendMessage(any(), any(), any(), any()) } returns sentMessageId
    }

    @ParameterizedTest(name = "{0} sends invalid usage response for malformed command")
    @EnumSource(ActionCase::class)
    fun `handle sends invalid usage response when user id is missing`(actionCase: ActionCase) =
        runTest {
            val handler = createHandler(actionCase)

            handler.handle(message(actionCase.command))

            verify {
                telegramClient.sendMessage(
                    chatId,
                    BotResponses.ADMIN_INVALID_USAGE_RESPONSE.format(actionCase.command),
                )
            }
            coVerify(exactly = 0) { subscriptionClient.blockUser(any()) }
            coVerify(exactly = 0) { subscriptionClient.unblockUser(any()) }
        }

    @ParameterizedTest(name = "{0} executes state change and sends success message")
    @EnumSource(ActionCase::class)
    fun `handle performs action and sends success response`(actionCase: ActionCase) =
        runTest {
            val handler = createHandler(actionCase)
            stubStateChange(actionCase)

            handler.handle(message("${actionCase.command} $targetUserId"))

            verifyStateChange(actionCase)
            verify {
                telegramClient.sendMessage(
                    chatId,
                    actionCase.successResponse.format(targetUserId),
                )
            }
        }

    @ParameterizedTest(name = "{0} sends failure response when subscription service fails")
    @EnumSource(ActionCase::class)
    fun `handle sends failure response on subscription error`(actionCase: ActionCase) =
        runTest {
            val handler = createHandler(actionCase)
            stubStateChangeError(actionCase)

            handler.handle(message("${actionCase.command} $targetUserId"))

            verify {
                telegramClient.sendMessage(
                    chatId,
                    BotResponses.ADMIN_ACTION_FAILED_RESPONSE.text,
                )
            }
        }

    private fun createHandler(actionCase: ActionCase): AdminBotCommand =
        when (actionCase) {
            ActionCase.BAN -> BanCommandHandler(telegramClient, subscriptionClient)
            ActionCase.UNBAN -> UnbanCommandHandler(telegramClient, subscriptionClient)
        }

    private fun stubStateChange(actionCase: ActionCase) {
        when (actionCase) {
            ActionCase.BAN -> coEvery { subscriptionClient.blockUser(targetUserId) } returns Unit
            ActionCase.UNBAN ->
                coEvery { subscriptionClient.unblockUser(targetUserId) } returns
                    Unit
        }
    }

    private fun stubStateChangeError(actionCase: ActionCase) {
        val error = SubscriptionServiceException("failed")
        when (actionCase) {
            ActionCase.BAN -> coEvery { subscriptionClient.blockUser(targetUserId) } throws error
            ActionCase.UNBAN ->
                coEvery { subscriptionClient.unblockUser(targetUserId) } throws
                    error
        }
    }

    private fun verifyStateChange(actionCase: ActionCase) {
        when (actionCase) {
            ActionCase.BAN -> coVerify(exactly = 1) { subscriptionClient.blockUser(targetUserId) }
            ActionCase.UNBAN ->
                coVerify(
                    exactly = 1,
                ) { subscriptionClient.unblockUser(targetUserId) }
        }
    }

    private fun message(text: String) =
        Message(
            chat = Chat(id = chatId),
            text = text,
        )
}
