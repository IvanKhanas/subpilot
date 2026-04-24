package com.xeno.subpilot.subscription.unittests.kafka

import com.xeno.subpilot.subscription.dto.kafka.PaymentSucceededEvent
import com.xeno.subpilot.subscription.dto.kafka.SubscriptionActivatedEvent
import com.xeno.subpilot.subscription.properties.PlanProperties
import com.xeno.subpilot.subscription.properties.ProviderAllocation
import com.xeno.subpilot.subscription.properties.SubscriptionProperties
import com.xeno.subpilot.subscription.service.SubscriptionActivationService
import com.xeno.subpilot.subscription.service.kafka.PaymentSucceededConsumer
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.kafka.core.KafkaTemplate
import tools.jackson.databind.ObjectMapper

import java.math.BigDecimal
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture

import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class PaymentSucceededConsumerTest {

    @MockK
    lateinit var activationService: SubscriptionActivationService

    @MockK
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @MockK
    lateinit var objectMapper: ObjectMapper

    private lateinit var consumer: PaymentSucceededConsumer

    private val properties =
        SubscriptionProperties(
            freeQuota = 10,
            freeQuotaResetPeriod = Duration.ofDays(7),
            defaultModel = "gpt-4o-mini",
            modelProviders = mapOf("gpt-4o-mini" to "openai"),
            modelCosts = mapOf("gpt-4o-mini" to 1),
            plans =
                mapOf(
                    "openai-basic" to
                        PlanProperties(
                            provider = "openai",
                            displayName = "Basic - 100 requests for OpenAI",
                            price = BigDecimal("199.00"),
                            currency = "RUB",
                            allocations =
                                listOf(
                                    ProviderAllocation(provider = "openai", requests = 100),
                                ),
                        ),
                ),
        )

    @BeforeEach
    fun setUp() {
        consumer =
            PaymentSucceededConsumer(activationService, properties, kafkaTemplate, objectMapper)
        every { kafkaTemplate.send(any(), any()) } returns CompletableFuture.completedFuture(null)
    }

    @Test
    fun `consume delegates activation and publishes subscription_activated event when activated`() {
        val paymentEvent =
            PaymentSucceededEvent(
                paymentId = UUID.randomUUID(),
                userId = 42L,
                planId = "openai-basic",
                amount = BigDecimal("199.00"),
            )
        every { objectMapper.readValue("event-json", PaymentSucceededEvent::class.java) } returns
            paymentEvent
        every { activationService.activate(paymentEvent) } returns true
        val publishedEvent = slot<Any>()
        every { objectMapper.writeValueAsString(capture(publishedEvent)) } returns """{"ok":true}"""

        consumer.consume("event-json")

        verify { activationService.activate(paymentEvent) }
        verify { kafkaTemplate.send("subscription_activated", """{"ok":true}""") }

        val notification = publishedEvent.captured as SubscriptionActivatedEvent
        assertEquals(42L, notification.userId)
        assertEquals("Basic - 100 requests for OpenAI", notification.planDisplayName)
        assertEquals(1, notification.allocations.size)
        assertEquals("openai", notification.allocations[0].provider)
        assertEquals(100, notification.allocations[0].requests)
    }

    @Test
    fun `consume does not publish when activation was not performed`() {
        val paymentEvent =
            PaymentSucceededEvent(
                paymentId = UUID.randomUUID(),
                userId = 42L,
                planId = "openai-basic",
                amount = BigDecimal("199.00"),
            )
        every { objectMapper.readValue("event-json", PaymentSucceededEvent::class.java) } returns
            paymentEvent
        every { activationService.activate(paymentEvent) } returns false

        consumer.consume("event-json")

        verify(exactly = 0) { objectMapper.writeValueAsString(any()) }
        verify(exactly = 0) { kafkaTemplate.send(any(), any()) }
    }

    @Test
    fun `consume does not publish when plan is no longer configured`() {
        val paymentEvent =
            PaymentSucceededEvent(
                paymentId = UUID.randomUUID(),
                userId = 42L,
                planId = "unknown-plan",
                amount = BigDecimal("199.00"),
            )
        every { objectMapper.readValue("event-json", PaymentSucceededEvent::class.java) } returns
            paymentEvent
        every { activationService.activate(paymentEvent) } returns true

        consumer.consume("event-json")

        verify(exactly = 0) { objectMapper.writeValueAsString(any()) }
        verify(exactly = 0) { kafkaTemplate.send(any(), any()) }
    }
}
