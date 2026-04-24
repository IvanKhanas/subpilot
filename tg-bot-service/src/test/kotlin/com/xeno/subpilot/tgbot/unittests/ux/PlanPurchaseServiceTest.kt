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

    private lateinit var service: PlanPurchaseService

    @BeforeEach
    fun setUp() {
        service = PlanPurchaseService(paymentClient, telegramClient)
    }

    @Test
    fun `startPayment sends confirmation link message when payment is created`() {
        coEvery { paymentClient.createPayment(42L, "openai-basic", 50) } returns
            "https://pay.example/link"
        val textSlot = io.mockk.slot<String>()
        everySendMessageCapture(textSlot)

        runBlocking {
            service.startPayment(
                chatId = 100L,
                userId = 42L,
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
            service.startPayment(chatId = 100L, userId = 42L, planId = "openai-basic")
        }

        verify {
            telegramClient.sendMessage(
                100L,
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
        } returns
            1L
    }
}
