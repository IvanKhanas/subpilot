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
import com.xeno.subpilot.tgbot.ux.callbacks.PurchasePlanCallbackHandler
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import kotlin.test.assertFalse
import kotlin.test.assertTrue

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class PurchasePlanCallbackHandlerTest {

    @MockK
    lateinit var bonusPurchaseService: BonusPurchaseService

    private lateinit var handler: PurchasePlanCallbackHandler

    @BeforeEach
    fun setUp() {
        handler = PurchasePlanCallbackHandler(bonusPurchaseService)
        coJustRun { bonusPurchaseService.startBonusPurchase(any(), any(), any()) }
    }

    @Test
    fun `supports matches purchase callback prefix`() {
        assertTrue(handler.supports("purchase:openai-basic"))
        assertFalse(handler.supports("bonus_yes:openai-basic:uuid"))
    }

    @Test
    fun `handle delegates plan purchase callback to bonus service`() =
        runTest {
            val callback =
                CallbackQuery(
                    id = "cb-1",
                    from = User(id = 42L),
                    message = Message(chat = Chat(id = 100L)),
                    data = "purchase:openai-basic",
                )

            handler.handle(callback)

            coVerify { bonusPurchaseService.startBonusPurchase(100L, 42L, "openai-basic") }
        }

    @Test
    fun `handle does nothing when callback misses chat or user`() =
        runTest {
            handler.handle(CallbackQuery(id = "cb-1", data = "purchase:openai-basic"))

            coVerify(exactly = 0) { bonusPurchaseService.startBonusPurchase(any(), any(), any()) }
        }
}
