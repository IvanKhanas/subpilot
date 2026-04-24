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
import com.xeno.subpilot.tgbot.ux.callbacks.ConfirmBonusSpendCallbackHandler
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import java.util.UUID

import kotlin.test.assertFalse
import kotlin.test.assertTrue

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class ConfirmBonusSpendCallbackHandlerTest {

    @MockK
    lateinit var bonusPurchaseService: BonusPurchaseService

    private lateinit var handler: ConfirmBonusSpendCallbackHandler

    @BeforeEach
    fun setUp() {
        handler = ConfirmBonusSpendCallbackHandler(bonusPurchaseService)
        coJustRun {
            bonusPurchaseService.confirmBonusSpend(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `supports matches bonus yes callback prefix`() {
        assertTrue(handler.supports("bonus_yes:openai-basic:uuid"))
        assertFalse(handler.supports("bonus_no:openai-basic"))
    }

    @Test
    fun `handle parses callback data and delegates to bonus purchase service`() =
        runTest {
            val idempotencyKey = UUID.randomUUID()
            val callback =
                callbackQuery(
                    data = "bonus_yes:openai-basic:$idempotencyKey",
                    chatId = 100L,
                    userId = 42L,
                    messageId = 7L,
                    text = "prompt-text",
                )

            handler.handle(callback)

            coVerify {
                bonusPurchaseService.confirmBonusSpend(
                    chatId = 100L,
                    userId = 42L,
                    planId = "openai-basic",
                    idempotencyKey = idempotencyKey,
                    promptMessageId = 7L,
                    promptText = "prompt-text",
                )
            }
        }

    @Test
    fun `handle does nothing for malformed callback data`() =
        runTest {
            val callback =
                callbackQuery(
                    data = "bonus_yes:malformed",
                    chatId = 100L,
                    userId = 42L,
                    messageId = 7L,
                    text = "prompt",
                )

            handler.handle(callback)

            coVerify(exactly = 0) {
                bonusPurchaseService.confirmBonusSpend(any(), any(), any(), any(), any(), any())
            }
        }

    private fun callbackQuery(
        data: String,
        chatId: Long,
        userId: Long,
        messageId: Long,
        text: String,
    ): CallbackQuery =
        CallbackQuery(
            id = "cb-1",
            from = User(id = userId),
            message = Message(messageId = messageId, chat = Chat(id = chatId), text = text),
            data = data,
        )
}
