package com.xeno.subpilot.loyalty.unittests.kafka

import com.xeno.subpilot.loyalty.dto.kafka.PaymentSucceededEvent
import com.xeno.subpilot.loyalty.service.LoyaltyService
import com.xeno.subpilot.loyalty.service.kafka.PaymentSucceededConsumer
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tools.jackson.databind.ObjectMapper

import java.math.BigDecimal
import java.util.UUID

import kotlin.test.assertFailsWith

@ExtendWith(MockKExtension::class)
class PaymentSucceededConsumerTest {

    @MockK
    lateinit var loyaltyService: LoyaltyService

    @MockK
    lateinit var objectMapper: ObjectMapper

    private lateinit var consumer: PaymentSucceededConsumer

    @BeforeEach
    fun setUp() {
        consumer =
            PaymentSucceededConsumer(
                loyaltyService = loyaltyService,
                objectMapper = objectMapper,
            )
    }

    @Test
    fun `consume deserializes event and delegates to loyalty service`() {
        val message = """{"payment_id":"ignored-in-test"}"""
        val event =
            PaymentSucceededEvent(
                paymentId = UUID.randomUUID(),
                userId = 42L,
                planId = "openai-basic",
                amount = BigDecimal("199.00"),
                bonusPointsUsed = 50,
            )
        every { objectMapper.readValue(message, PaymentSucceededEvent::class.java) } returns event
        justRun { loyaltyService.earn(event) }

        consumer.consume(message)

        verify { objectMapper.readValue(message, PaymentSucceededEvent::class.java) }
        verify { loyaltyService.earn(event) }
    }

    @Test
    fun `consume does not swallow deserialization exceptions`() {
        val message = "{invalid-json}"
        every { objectMapper.readValue(message, PaymentSucceededEvent::class.java) } throws
            IllegalArgumentException("invalid payload")

        assertFailsWith<IllegalArgumentException> {
            consumer.consume(message)
        }

        verify(exactly = 0) { loyaltyService.earn(any()) }
    }
}
