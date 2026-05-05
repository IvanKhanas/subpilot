/*
 * Copyright 2026 Ivan Khanas
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
package com.xeno.subpilot.tgbot.unittests.ux

import com.xeno.subpilot.tgbot.client.PaymentClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.exception.PaymentServiceException
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.ux.PlanPurchaseService
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import kotlin.test.assertContains

import kotlinx.coroutines.runBlocking

@ExtendWith(MockKExtension::class)
class PlanPurchaseServiceTest {

    @MockK
    lateinit var paymentClient: PaymentClient

    @MockK
    lateinit var telegramClient: TelegramClient

    private val faker = Faker()

    private lateinit var service: PlanPurchaseService
    private var chatId: Long = 0L
    private var userId: Long = 0L
    private var sentMessageId: Long = 0L

    @BeforeEach
    fun setUp() {
        chatId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        userId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        sentMessageId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        service = PlanPurchaseService(paymentClient, telegramClient)
    }

    @Test
    fun `startPayment sends confirmation link message when payment is created`() {
        coEvery { paymentClient.createPayment(userId, "openai-basic", 50) } returns
            "https://pay.example/link"
        val textSlot = io.mockk.slot<String>()
        everySendMessageCapture(textSlot)

        runBlocking {
            service.startPayment(
                chatId = chatId,
                userId = userId,
                planId = "openai-basic",
                bonusPointsToApply = 50,
            )
        }

        assertContains(textSlot.captured, "https://pay.example/link")
    }

    @Test
    fun `startPayment sends failure message when payment service throws`() {
        coEvery { paymentClient.createPayment(any(), any(), any()) } throws
            PaymentServiceException("down", RuntimeException())
        val textSlot = io.mockk.slot<String>()
        everySendMessageCapture(textSlot)

        runBlocking {
            service.startPayment(chatId = chatId, userId = userId, planId = "openai-basic")
        }

        verify {
            telegramClient.sendMessage(
                chatId,
                BotResponses.PAYMENT_FAILED_RESPONSE.text,
                any(),
                any(),
            )
        }
    }

    private fun everySendMessageCapture(textSlot: io.mockk.CapturingSlot<String>) {
        io.mockk.every {
            telegramClient.sendMessage(
                any(),
                capture(textSlot),
                any(),
                any(),
            )
        } returns sentMessageId
    }
}
