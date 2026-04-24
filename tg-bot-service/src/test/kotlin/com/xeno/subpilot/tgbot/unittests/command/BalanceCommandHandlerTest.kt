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
import com.xeno.subpilot.tgbot.command.BalanceCommandHandler
import com.xeno.subpilot.tgbot.dto.BalanceInfo
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.User
import com.xeno.subpilot.tgbot.ux.BalanceFormatter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import kotlin.test.assertEquals

import kotlinx.coroutines.runBlocking

@ExtendWith(MockKExtension::class)
class BalanceCommandHandlerTest {

    @MockK
    lateinit var subscriptionClient: SubscriptionClient

    @MockK
    lateinit var telegramClient: TelegramClient

    @MockK
    lateinit var balanceFormatter: BalanceFormatter

    private lateinit var handler: BalanceCommandHandler

    @BeforeEach
    fun setUp() {
        handler = BalanceCommandHandler(subscriptionClient, telegramClient, balanceFormatter)
        every { telegramClient.sendMessage(any(), any(), any(), any()) } returns 1L
    }

    @Test
    fun `handle sends formatted balance for valid user`() {
        val balance = BalanceInfo(emptyList(), emptyList())
        coEvery { subscriptionClient.getBalance(42L) } returns balance
        every { balanceFormatter.format(balance) } returns "formatted-balance"
        val textSlot = io.mockk.slot<String>()
        every { telegramClient.sendMessage(100L, capture(textSlot), any(), any()) } returns 1L

        runBlocking {
            handler.handle(
                Message(chat = Chat(id = 100L), from = User(id = 42L), text = "/balance"),
            )
        }

        assertEquals("formatted-balance", textSlot.captured)
        coVerify { subscriptionClient.getBalance(42L) }
    }

    @Test
    fun `handle ignores command when user is missing`() {
        runBlocking {
            handler.handle(Message(chat = Chat(id = 100L), from = null, text = "/balance"))
        }

        coVerify(exactly = 0) { subscriptionClient.getBalance(any()) }
        verify(exactly = 0) { telegramClient.sendMessage(any(), any(), any(), any()) }
    }
}
