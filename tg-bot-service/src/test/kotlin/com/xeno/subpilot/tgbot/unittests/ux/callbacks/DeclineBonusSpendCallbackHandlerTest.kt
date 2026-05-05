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
package com.xeno.subpilot.tgbot.unittests.ux.callbacks

import com.xeno.subpilot.tgbot.dto.CallbackQuery
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.User
import com.xeno.subpilot.tgbot.ux.BonusPurchaseService
import com.xeno.subpilot.tgbot.ux.callbacks.DeclineBonusSpendCallbackHandler
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import kotlin.test.assertFalse
import kotlin.test.assertTrue

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class DeclineBonusSpendCallbackHandlerTest {

    @MockK
    lateinit var bonusPurchaseService: BonusPurchaseService

    private val faker = Faker()

    private lateinit var handler: DeclineBonusSpendCallbackHandler
    private var chatId: Long = 0L
    private var userId: Long = 0L
    private var messageId: Long = 0L
    private lateinit var callbackId: String

    @BeforeEach
    fun setUp() {
        chatId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        userId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        messageId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        callbackId = "cb-${faker.number().digits(8)}"
        handler = DeclineBonusSpendCallbackHandler(bonusPurchaseService)
        coJustRun { bonusPurchaseService.declineBonusSpend(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `supports matches bonus no callback prefix`() {
        assertTrue(handler.supports("bonus_no:openai-basic"))
        assertFalse(handler.supports("bonus_yes:openai-basic:uuid"))
    }

    @Test
    fun `handle delegates parsed values to bonus purchase service`() =
        runTest {
            val callback =
                CallbackQuery(
                    id = callbackId,
                    from = User(id = userId),
                    message = Message(messageId = messageId, chat = Chat(id = chatId), text = "prompt-text"),
                    data = "bonus_no:openai-basic",
                )

            handler.handle(callback)

            coVerify {
                bonusPurchaseService.declineBonusSpend(
                    chatId = chatId,
                    userId = userId,
                    planId = "openai-basic",
                    promptMessageId = messageId,
                    promptText = "prompt-text",
                )
            }
        }

    @Test
    fun `handle does nothing when callback misses required fields`() =
        runTest {
            handler.handle(CallbackQuery(id = callbackId, data = "bonus_no:openai-basic"))

            coVerify(
                exactly = 0,
            ) { bonusPurchaseService.declineBonusSpend(any(), any(), any(), any(), any()) }
        }
}
