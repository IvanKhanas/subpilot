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
package com.xeno.subpilot.tgbot.unittests.ux

import com.xeno.subpilot.tgbot.client.LoyaltyClient
import com.xeno.subpilot.tgbot.client.SubscriptionClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.InlineKeyboardMarkup
import com.xeno.subpilot.tgbot.dto.PlanInfo
import com.xeno.subpilot.tgbot.dto.SpendDenialReason
import com.xeno.subpilot.tgbot.dto.SpendResult
import com.xeno.subpilot.tgbot.exception.LoyaltyServiceException
import com.xeno.subpilot.tgbot.ux.BonusPurchaseService
import com.xeno.subpilot.tgbot.ux.PlanPurchaseService
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import java.util.UUID

import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs

import kotlinx.coroutines.runBlocking

@ExtendWith(MockKExtension::class)
class BonusPurchaseServiceTest {

    @MockK
    lateinit var loyaltyClient: LoyaltyClient

    @MockK
    lateinit var subscriptionClient: SubscriptionClient

    @MockK
    lateinit var planPurchaseService: PlanPurchaseService

    @MockK
    lateinit var telegramClient: TelegramClient

    private lateinit var service: BonusPurchaseService

    private val chatId = 100L
    private val userId = 42L
    private val planId = "openai-basic"

    @BeforeEach
    fun setUp() {
        service =
            BonusPurchaseService(
                loyaltyClient,
                subscriptionClient,
                planPurchaseService,
                telegramClient,
            )
        every { telegramClient.sendMessage(any(), any(), any(), any()) } returns 1L
        every { telegramClient.editMessage(any(), any(), any()) } returns Unit
        coJustRun { planPurchaseService.startPayment(any(), any(), any(), any()) }
    }

    @Test
    fun `startBonusPurchase falls back to normal payment when loyalty balance is unavailable`() {
        coEvery { loyaltyClient.getBalance(userId) } throws
            LoyaltyServiceException("boom", RuntimeException())
        coEvery { subscriptionClient.getPlanInfo(planId) } returns plan(price = "199")

        runBlocking { service.startBonusPurchase(chatId, userId, planId) }

        coVerify { planPurchaseService.startPayment(chatId, userId, planId, 0) }
        verify(exactly = 0) { telegramClient.sendMessage(chatId, any(), any(), any()) }
    }

    @Test
    fun `startBonusPurchase falls back to normal payment when plan is unknown`() {
        coEvery { loyaltyClient.getBalance(userId) } returns 50
        coEvery { subscriptionClient.getPlanInfo(planId) } returns null

        runBlocking { service.startBonusPurchase(chatId, userId, planId) }

        coVerify { planPurchaseService.startPayment(chatId, userId, planId, 0) }
    }

    @Test
    fun `startBonusPurchase sends full bonus prompt when balance covers plan price`() {
        coEvery { loyaltyClient.getBalance(userId) } returns 300
        coEvery { subscriptionClient.getPlanInfo(planId) } returns plan(price = "199")
        val textSlot = slot<String>()
        val markupSlot = slot<com.xeno.subpilot.tgbot.dto.ReplyMarkup?>()
        every {
            telegramClient.sendMessage(
                chatId,
                capture(textSlot),
                captureNullable(markupSlot),
                any(),
            )
        } returns 1L

        runBlocking { service.startBonusPurchase(chatId, userId, planId) }

        assertContains(textSlot.captured, "Use them to get this subscription for free")
        val markup = assertIs<InlineKeyboardMarkup>(markupSlot.captured)
        val yesCallback = markup.inlineKeyboard[0][0].callbackData
        val noCallback = markup.inlineKeyboard[0][1].callbackData
        assertContains(yesCallback, "bonus_yes:$planId:")
        assertEquals("bonus_no:$planId", noCallback)
    }

    @Test
    fun `startBonusPurchase sends partial prompt when bonus covers only part of price`() {
        coEvery { loyaltyClient.getBalance(userId) } returns 50
        coEvery { subscriptionClient.getPlanInfo(planId) } returns plan(price = "199")
        val textSlot = slot<String>()
        every { telegramClient.sendMessage(chatId, capture(textSlot), any(), any()) } returns 1L

        runBlocking { service.startBonusPurchase(chatId, userId, planId) }

        assertContains(textSlot.captured, "Apply a 50")
        assertContains(textSlot.captured, "pay 149")
    }

    @Test
    fun `confirmBonusSpend sends success message when loyalty spend succeeds`() {
        val idempotencyKey = UUID.randomUUID()
        coEvery { loyaltyClient.getBalance(userId) } returns 300
        coEvery { subscriptionClient.getPlanInfo(planId) } returns plan(price = "199")
        coEvery { loyaltyClient.spend(userId, planId, idempotencyKey) } returns SpendResult.Success

        runBlocking {
            service.confirmBonusSpend(
                chatId = chatId,
                userId = userId,
                planId = planId,
                idempotencyKey = idempotencyKey,
                promptMessageId = 55L,
                promptText = "prompt",
            )
        }

        verify { telegramClient.editMessage(chatId, 55L, "prompt") }
        verify {
            telegramClient.sendMessage(
                chatId,
                match {
                    it.contains("activated with bonus points")
                },
                any(),
                any(),
            )
        }
        coVerify(exactly = 0) { planPurchaseService.startPayment(any(), any(), any(), any()) }
    }

    @Test
    fun `confirmBonusSpend falls back to regular payment when loyalty denies spend`() {
        val idempotencyKey = UUID.randomUUID()
        coEvery { loyaltyClient.getBalance(userId) } returns 300
        coEvery { subscriptionClient.getPlanInfo(planId) } returns plan(price = "199")
        coEvery { loyaltyClient.spend(userId, planId, idempotencyKey) } returns
            SpendResult.Denied(SpendDenialReason.INSUFFICIENT_POINTS)

        runBlocking {
            service.confirmBonusSpend(chatId, userId, planId, idempotencyKey, 10L, "prompt")
        }

        coVerify { planPurchaseService.startPayment(chatId, userId, planId, 0) }
    }

    @Test
    fun `confirmBonusSpend sends failure message when loyalty call throws`() {
        val idempotencyKey = UUID.randomUUID()
        coEvery { loyaltyClient.getBalance(userId) } returns 300
        coEvery { subscriptionClient.getPlanInfo(planId) } returns plan(price = "199")
        coEvery { loyaltyClient.spend(userId, planId, idempotencyKey) } throws
            LoyaltyServiceException("down", RuntimeException())

        runBlocking {
            service.confirmBonusSpend(chatId, userId, planId, idempotencyKey, 10L, "prompt")
        }

        verify {
            telegramClient.sendMessage(
                chatId,
                match {
                    it.contains("Failed to apply bonus points")
                },
                any(),
                any(),
            )
        }
        coVerify(exactly = 0) { planPurchaseService.startPayment(any(), any(), any(), any()) }
    }

    @Test
    fun `confirmBonusSpend starts payment with partial bonus when balance is lower than price`() {
        coEvery { loyaltyClient.getBalance(userId) } returns 50
        coEvery { subscriptionClient.getPlanInfo(planId) } returns plan(price = "199")

        runBlocking {
            service.confirmBonusSpend(chatId, userId, planId, UUID.randomUUID(), 10L, "prompt")
        }

        coVerify { planPurchaseService.startPayment(chatId, userId, planId, 50) }
    }

    @Test
    fun `confirmBonusSpend starts regular payment when plan lookup fails`() {
        coEvery { loyaltyClient.getBalance(userId) } returns 50
        coEvery { subscriptionClient.getPlanInfo(planId) } returns null

        runBlocking {
            service.confirmBonusSpend(chatId, userId, planId, UUID.randomUUID(), 10L, "prompt")
        }

        coVerify { planPurchaseService.startPayment(chatId, userId, planId, 0) }
    }

    @Test
    fun `declineBonusSpend edits prompt and starts normal payment`() {
        runBlocking {
            service.declineBonusSpend(
                chatId = chatId,
                userId = userId,
                planId = planId,
                promptMessageId = 99L,
                promptText = "declined",
            )
        }

        verify { telegramClient.editMessage(chatId, 99L, "declined") }
        coVerify { planPurchaseService.startPayment(chatId, userId, planId, 0) }
    }

    private fun plan(price: String) =
        PlanInfo(
            planId = planId,
            provider = "openai",
            displayName = "Basic",
            price = price,
            currency = "RUB",
            allocations = emptyList(),
        )
}
