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
package com.xeno.subpilot.subscription.testcontainers

import com.xeno.subpilot.subscription.properties.ProviderAllocation
import com.xeno.subpilot.subscription.repository.JpaUserSubscriptionActivationRepository
import com.xeno.subpilot.subscription.repository.SubscriptionUserJpaRepository
import com.xeno.subpilot.subscription.repository.UserRequestBalanceJpaRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional

import java.time.Instant
import java.util.UUID

import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@Transactional
class JpaUserSubscriptionActivationRepositoryContainerTest {

    @Autowired
    lateinit var repository: JpaUserSubscriptionActivationRepository

    @Autowired
    lateinit var requestBalanceJpaRepository: UserRequestBalanceJpaRepository

    @Autowired
    lateinit var subscriptionUserJpaRepository: SubscriptionUserJpaRepository

    companion object {
        private val postgres = TestContainersConfiguration.postgres

        @JvmStatic
        @DynamicPropertySource
        fun configure(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
        }
    }

    @Test
    fun `batchInsertUserSubscriptionIfAbsent returns inserted providers and is idempotent`() {
        val paymentId = UUID.randomUUID()
        subscriptionUserJpaRepository.insertIfAbsent(42L)
        val allocations =
            listOf(
                ProviderAllocation(provider = "openai", requests = 100),
                ProviderAllocation(provider = "anthropic", requests = 50),
            )

        val insertedFirst =
            repository.batchInsertUserSubscriptionIfAbsent(
                paymentId = paymentId,
                userId = 42L,
                planId = "combo-basic",
                allocations = allocations,
                activatedAt = Instant.parse("2026-04-24T12:00:00Z"),
            )
        val insertedSecond =
            repository.batchInsertUserSubscriptionIfAbsent(
                paymentId = paymentId,
                userId = 42L,
                planId = "combo-basic",
                allocations = allocations,
                activatedAt = Instant.parse("2026-04-24T12:00:00Z"),
            )

        assertEquals(setOf("openai", "anthropic"), insertedFirst.toSet())
        assertTrue(insertedSecond.isEmpty())
    }

    @Test
    fun `batchUpsertRequestBalance inserts and accumulates requests per provider`() {
        val userId = 77L
        subscriptionUserJpaRepository.insertIfAbsent(userId)

        repository.batchUpsertRequestBalance(
            userId = userId,
            allocations =
                listOf(
                    ProviderAllocation(provider = "openai", requests = 10),
                    ProviderAllocation(provider = "anthropic", requests = 5),
                ),
        )
        repository.batchUpsertRequestBalance(
            userId = userId,
            allocations =
                listOf(
                    ProviderAllocation(provider = "openai", requests = 3),
                ),
        )

        val openAi = requestBalanceJpaRepository.findByUserIdAndProvider(userId, "openai")
        val anthropic = requestBalanceJpaRepository.findByUserIdAndProvider(userId, "anthropic")
        assertEquals(13, openAi?.requestsRemaining)
        assertEquals(5, anthropic?.requestsRemaining)
    }
}
