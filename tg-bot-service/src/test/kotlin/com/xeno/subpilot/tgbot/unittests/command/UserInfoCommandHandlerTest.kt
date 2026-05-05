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
import com.xeno.subpilot.tgbot.command.UserInfoCommandHandler
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.UserInfoResult
import com.xeno.subpilot.tgbot.message.BotResponses
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class UserInfoCommandHandlerTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    @MockK
    lateinit var subscriptionClient: SubscriptionClient

    private val faker = Faker()

    private lateinit var handler: UserInfoCommandHandler
    private var chatId: Long = 0L
    private var targetUserId: Long = 0L
    private var sentMessageId: Long = 0L

    companion object {
        const val REGISTERED_AT_EPOCH = 0L
        const val REGISTERED_AT_UTC = "1970-01-01 00:00"
    }

    @BeforeEach
    fun setUp() {
        chatId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        targetUserId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        sentMessageId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        handler = UserInfoCommandHandler(telegramClient, subscriptionClient)
        every { telegramClient.sendMessage(any(), any(), any(), any()) } returns sentMessageId
    }

    @Test
    fun `handle sends invalid usage response when user id is missing`() =
        runTest {
            handler.handle(message("/userinfo"))

            verify {
                telegramClient.sendMessage(
                    chatId,
                    BotResponses.ADMIN_INVALID_USAGE_RESPONSE.format("/userinfo"),
                )
            }
            coVerify(exactly = 0) { subscriptionClient.getUserInfo(any()) }
        }

    @Test
    fun `handle sends not found response when user does not exist`() =
        runTest {
            coEvery { subscriptionClient.getUserInfo(targetUserId) } returns null

            handler.handle(message("/userinfo $targetUserId"))

            verify {
                telegramClient.sendMessage(
                    chatId,
                    BotResponses.ADMIN_USER_NOT_FOUND_RESPONSE.format(targetUserId),
                )
            }
        }

    @ParameterizedTest(name = "blocked={0} maps to status={1}")
    @CsvSource(
        "true, BANNED",
        "false, ACTIVE",
    )
    fun `handle sends formatted user info with status mapping`(
        blocked: Boolean,
        expectedStatus: String,
    ) = runTest {
        coEvery { subscriptionClient.getUserInfo(targetUserId) } returns
            UserInfoResult(
                blocked = blocked,
                role = "ADMIN",
                registeredAtEpoch = REGISTERED_AT_EPOCH,
            )

        handler.handle(message("/userinfo $targetUserId"))

        verify {
            telegramClient.sendMessage(
                chatId,
                BotResponses.ADMIN_USER_INFO_RESPONSE.format(
                    targetUserId,
                    REGISTERED_AT_UTC,
                    "ADMIN",
                    expectedStatus,
                ),
            )
        }
    }

    private fun message(text: String) =
        Message(
            chat = Chat(id = chatId),
            text = text,
        )
}
