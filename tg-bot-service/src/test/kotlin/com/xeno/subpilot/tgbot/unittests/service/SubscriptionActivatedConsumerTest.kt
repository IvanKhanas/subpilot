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
package com.xeno.subpilot.tgbot.unittests.service

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.kafka.SubscriptionActivatedEvent
import com.xeno.subpilot.tgbot.service.SubscriptionActivatedConsumer
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tools.jackson.databind.ObjectMapper

import kotlin.test.assertContains
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class SubscriptionActivatedConsumerTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    @MockK
    lateinit var objectMapper: ObjectMapper

    private lateinit var consumer: SubscriptionActivatedConsumer

    @BeforeEach
    fun setUp() {
        consumer = SubscriptionActivatedConsumer(telegramClient, objectMapper)
        every { telegramClient.sendMessage(any(), any(), any(), any()) } returns 1L
    }

    @Test
    fun `consume sends activation message with plan and allocations`() {
        val event =
            SubscriptionActivatedEvent(
                userId = 42L,
                planDisplayName = "Basic - 100 requests for OpenAI",
                allocations =
                    listOf(
                        SubscriptionActivatedEvent.ProviderAllocation(
                            provider = "openai",
                            requests = 100,
                        ),
                        SubscriptionActivatedEvent.ProviderAllocation(
                            provider = "anthropic",
                            requests = 50,
                        ),
                    ),
            )
        every {
            objectMapper.readValue(
                "event-json",
                SubscriptionActivatedEvent::class.java,
            )
        } returns
            event
        val textSlot = slot<String>()
        every { telegramClient.sendMessage(42L, capture(textSlot), any(), any()) } returns 1L

        consumer.consume("event-json")

        assertContains(textSlot.captured, "Subscription activated!")
        assertContains(textSlot.captured, "Basic - 100 requests for OpenAI")
        assertContains(textSlot.captured, "OpenAI")
        assertContains(textSlot.captured, "Anthropic")
        assertContains(textSlot.captured, "requests credited")
    }

    @Test
    fun `consume uses user id as chat id`() {
        val event =
            SubscriptionActivatedEvent(
                userId = 77L,
                planDisplayName = "Plan",
                allocations = emptyList(),
            )
        every {
            objectMapper.readValue(
                any<String>(),
                SubscriptionActivatedEvent::class.java,
            )
        } returns
            event
        val chatIdSlot = slot<Long>()
        every { telegramClient.sendMessage(capture(chatIdSlot), any(), any(), any()) } returns 1L

        consumer.consume("event-json")

        assertEquals(77L, chatIdSlot.captured)
    }
}
