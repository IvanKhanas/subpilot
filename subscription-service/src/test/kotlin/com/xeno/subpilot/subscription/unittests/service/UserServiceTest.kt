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
package com.xeno.subpilot.subscription.unittests.service

import com.xeno.subpilot.subscription.metrics.SubscriptionMetrics
import com.xeno.subpilot.subscription.properties.SubscriptionProperties
import com.xeno.subpilot.subscription.repository.SubscriptionUserRepository
import com.xeno.subpilot.subscription.repository.UserFreeQuotaRepository
import com.xeno.subpilot.subscription.repository.UserModelPreferenceRepository
import com.xeno.subpilot.subscription.service.UserService
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

import java.time.Duration

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class UserServiceTest {

    @MockK
    lateinit var subscriptionUserRepository: SubscriptionUserRepository

    @MockK
    lateinit var freeQuotaRepository: UserFreeQuotaRepository

    @MockK
    lateinit var modelPreferenceRepository: UserModelPreferenceRepository

    private lateinit var service: UserService

    private val faker = Faker()

    private val properties =
        SubscriptionProperties(
            freeQuota = 10,
            freeQuotaResetPeriod = Duration.ofDays(7),
            defaultModel = "gpt-4o-mini",
            modelProviders = mapOf("gpt-4o" to "openai", "gpt-4o-mini" to "openai"),
            modelCosts = mapOf("gpt-4o" to 3, "gpt-4o-mini" to 1),
        )

    private val meterRegistry = SimpleMeterRegistry()
    private val metrics = SubscriptionMetrics(meterRegistry)
    private var userId: Long = 0L

    @BeforeEach
    fun setUp() {
        userId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        service =
            UserService(
                subscriptionUserRepository,
                freeQuotaRepository,
                modelPreferenceRepository,
                properties,
                metrics,
            )
    }

    @Test
    fun `registerUser returns false and skips provisioning when user already exists`() {
        every { subscriptionUserRepository.insertIfAbsent(userId) } returns false

        val result = service.registerUser(userId)

        assertFalse(result)
        verify(exactly = 0) { freeQuotaRepository.createAll(any(), any(), any(), any()) }
        verify(exactly = 0) { modelPreferenceRepository.upsert(any(), any()) }
    }

    @Test
    fun `registerUser returns true and provisions free quota and default model for new user`() {
        every { subscriptionUserRepository.insertIfAbsent(userId) } returns true
        justRun { freeQuotaRepository.createAll(any(), any(), any(), any()) }
        justRun { modelPreferenceRepository.upsert(any(), any()) }

        val result = service.registerUser(userId)

        assertTrue(result)
        verify { freeQuotaRepository.createAll(userId, setOf("openai"), 10, any()) }
        verify { modelPreferenceRepository.upsert(userId, "gpt-4o-mini") }
    }

    @ParameterizedTest(name = "isNew={0} -> registrations_total={1}")
    @CsvSource(
        "true, 1.0",
        "false, 0.0",
    )
    fun `registerUser updates user_registrations_total depending on user existence`(
        isNew: Boolean,
        expectedCounterValue: Double,
    ) {
        every { subscriptionUserRepository.insertIfAbsent(userId) } returns isNew
        if (isNew) {
            justRun { freeQuotaRepository.createAll(any(), any(), any(), any()) }
            justRun { modelPreferenceRepository.upsert(any(), any()) }
        }

        service.registerUser(userId)

        assertEquals(
            expectedCounterValue,
            meterRegistry.counter("user_registrations_total").count(),
        )
    }
}
