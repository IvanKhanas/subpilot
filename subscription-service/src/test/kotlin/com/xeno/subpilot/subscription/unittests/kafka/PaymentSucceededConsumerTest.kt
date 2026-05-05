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
package com.xeno.subpilot.subscription.unittests.kafka

import com.xeno.subpilot.subscription.dto.kafka.PaymentSucceededEvent
import com.xeno.subpilot.subscription.dto.kafka.SubscriptionActivatedEvent
import com.xeno.subpilot.subscription.metrics.SubscriptionMetrics
import com.xeno.subpilot.subscription.properties.PlanProperties
import com.xeno.subpilot.subscription.properties.ProviderAllocation
import com.xeno.subpilot.subscription.repository.PlanRepository
import com.xeno.subpilot.subscription.service.SubscriptionActivationService
import com.xeno.subpilot.subscription.service.kafka.PaymentSucceededConsumer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.kafka.core.KafkaTemplate
import tools.jackson.databind.ObjectMapper

import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.CompletableFuture

import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class PaymentSucceededConsumerTest {

    @MockK
    lateinit var activationService: SubscriptionActivationService

    @MockK
    lateinit var planRepository: PlanRepository

    @MockK
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @MockK
    lateinit var objectMapper: ObjectMapper

    private val faker = Faker()

    private val openaiBasicPlan =
        PlanProperties(
            provider = "openai",
            displayName = "Basic - 100 requests for OpenAI",
            price = BigDecimal("199.00"),
            currency = "RUB",
            allocations = listOf(ProviderAllocation(provider = "openai", requests = 100)),
        )

    private val meterRegistry = SimpleMeterRegistry()
    private val metrics = SubscriptionMetrics(meterRegistry)

    private lateinit var consumer: PaymentSucceededConsumer
    private var userId: Long = 0L

    @BeforeEach
    fun setUp() {
        userId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        consumer =
            PaymentSucceededConsumer(
                activationService,
                planRepository,
                kafkaTemplate,
                objectMapper,
                metrics,
            )
        every { kafkaTemplate.send(any(), any()) } returns CompletableFuture.completedFuture(null)
        every { planRepository.findById("openai-basic") } returns openaiBasicPlan
        every { planRepository.findById("unknown-plan") } returns null
    }

    @Test
    fun `consume delegates activation and publishes subscription_activated event when activated`() {
        val paymentEvent =
            PaymentSucceededEvent(
                paymentId = UUID.randomUUID(),
                userId = userId,
                planId = "openai-basic",
                amount = BigDecimal("199.00"),
            )
        every {
            objectMapper.readValue(
                "event-json",
                PaymentSucceededEvent::class.java,
            )
        } returns paymentEvent
        every { activationService.activate(paymentEvent) } returns true
        val publishedEvent = slot<Any>()
        every { objectMapper.writeValueAsString(capture(publishedEvent)) } returns """{"ok":true}"""

        consumer.consume("event-json")

        verify { activationService.activate(paymentEvent) }
        verify { kafkaTemplate.send("subscription_activated", """{"ok":true}""") }

        val notification = publishedEvent.captured as SubscriptionActivatedEvent
        assertEquals(userId, notification.userId)
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
                userId = userId,
                planId = "openai-basic",
                amount = BigDecimal("199.00"),
            )
        every {
            objectMapper.readValue(
                "event-json",
                PaymentSucceededEvent::class.java,
            )
        } returns paymentEvent
        every { activationService.activate(paymentEvent) } returns false

        consumer.consume("event-json")

        verify(exactly = 0) { objectMapper.writeValueAsString(any()) }
        verify(exactly = 0) { kafkaTemplate.send(any(), any()) }
    }

    @Test
    fun `consume does not publish when plan is not found`() {
        val paymentEvent =
            PaymentSucceededEvent(
                paymentId = UUID.randomUUID(),
                userId = userId,
                planId = "unknown-plan",
                amount = BigDecimal("199.00"),
            )
        every {
            objectMapper.readValue(
                "event-json",
                PaymentSucceededEvent::class.java,
            )
        } returns paymentEvent
        every { activationService.activate(paymentEvent) } returns true

        consumer.consume("event-json")

        verify(exactly = 0) { objectMapper.writeValueAsString(any()) }
        verify(exactly = 0) { kafkaTemplate.send(any(), any()) }
    }

    @Test
    fun `consume increments subscription_activations_total when activated`() {
        val paymentEvent =
            PaymentSucceededEvent(
                paymentId = UUID.randomUUID(),
                userId = userId,
                planId = "openai-basic",
                amount = BigDecimal("199.00"),
            )
        every { objectMapper.readValue("event-json", PaymentSucceededEvent::class.java) } returns
            paymentEvent
        every { activationService.activate(paymentEvent) } returns true
        every { objectMapper.writeValueAsString(any()) } returns """{"ok":true}"""

        consumer.consume("event-json")

        assertEquals(1.0, meterRegistry.counter("subscription_activations_total").count())
    }

    @Test
    fun `consume does not increment subscription_activations_total when not activated`() {
        val paymentEvent =
            PaymentSucceededEvent(
                paymentId = UUID.randomUUID(),
                userId = userId,
                planId = "openai-basic",
                amount = BigDecimal("199.00"),
            )
        every { objectMapper.readValue("event-json", PaymentSucceededEvent::class.java) } returns
            paymentEvent
        every { activationService.activate(paymentEvent) } returns false

        consumer.consume("event-json")

        assertEquals(0.0, meterRegistry.counter("subscription_activations_total").count())
    }
}
