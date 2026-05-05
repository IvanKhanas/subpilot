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
package com.xeno.subpilot.loyalty.unittests.service

import com.xeno.subpilot.loyalty.repository.LoyaltyTransactionRepository
import com.xeno.subpilot.loyalty.repository.UserLoyaltyBalanceRepository
import com.xeno.subpilot.loyalty.service.LoyaltyAdminService
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

import kotlin.test.assertFailsWith

@ExtendWith(MockKExtension::class)
class LoyaltyAdminServiceTest {

    @MockK
    lateinit var loyaltyTransactionJpaRepository: LoyaltyTransactionRepository

    @MockK
    lateinit var userLoyaltyBalanceJpaRepository: UserLoyaltyBalanceRepository

    private val fixedClock: Clock =
        Clock.fixed(
            Instant.parse("2026-01-10T10:00:00Z"),
            ZoneOffset.UTC,
        )
    private val now: LocalDateTime = LocalDateTime.ofInstant(fixedClock.instant(), ZoneOffset.UTC)

    private lateinit var service: LoyaltyAdminService

    @BeforeEach
    fun setUp() {
        service =
            LoyaltyAdminService(
                loyaltyTransactionJpaRepository = loyaltyTransactionJpaRepository,
                userLoyaltyBalanceJpaRepository = userLoyaltyBalanceJpaRepository,
                clock = fixedClock,
            )
        justRun { userLoyaltyBalanceJpaRepository.upsertAdd(any(), any()) }
        justRun { userLoyaltyBalanceJpaRepository.subtractCappedAtZero(any(), any()) }
    }

    @Test
    fun `adjustPoints throws when delta is zero`() {
        assertFailsWith<IllegalArgumentException> {
            service.adjustPoints(
                userId = 42,
                delta = 0,
                reason = "manual adjustment",
                idempotencyKey = UUID.randomUUID(),
            )
        }
    }

    @Test
    fun `adjustPoints skips balance update when adjustment is duplicate`() {
        val idempotencyKey = UUID.randomUUID()
        every {
            loyaltyTransactionJpaRepository.insertAdjustedIfAbsent(
                userId = 42,
                amount = 50,
                idempotencyKey = idempotencyKey,
                reason = "manual grant",
                createdAt = now,
            )
        } returns 0

        service.adjustPoints(
            userId = 42,
            delta = 50,
            reason = "manual grant",
            idempotencyKey = idempotencyKey,
        )

        verify(exactly = 0) { userLoyaltyBalanceJpaRepository.upsertAdd(any(), any()) }
        verify(exactly = 0) { userLoyaltyBalanceJpaRepository.subtractCappedAtZero(any(), any()) }
    }

    @Test
    fun `adjustPoints adds points when delta is positive`() {
        val idempotencyKey = UUID.randomUUID()
        every {
            loyaltyTransactionJpaRepository.insertAdjustedIfAbsent(
                userId = 42,
                amount = 70,
                idempotencyKey = idempotencyKey,
                reason = "admin reward",
                createdAt = now,
            )
        } returns 1

        service.adjustPoints(
            userId = 42,
            delta = 70,
            reason = "admin reward",
            idempotencyKey = idempotencyKey,
        )

        verify { userLoyaltyBalanceJpaRepository.upsertAdd(42, 70) }
        verify(exactly = 0) { userLoyaltyBalanceJpaRepository.subtractCappedAtZero(any(), any()) }
    }

    @Test
    fun `adjustPoints subtracts points when delta is negative`() {
        val idempotencyKey = UUID.randomUUID()
        every {
            loyaltyTransactionJpaRepository.insertAdjustedIfAbsent(
                userId = 42,
                amount = -30,
                idempotencyKey = idempotencyKey,
                reason = "admin correction",
                createdAt = now,
            )
        } returns 1

        service.adjustPoints(
            userId = 42,
            delta = -30,
            reason = "admin correction",
            idempotencyKey = idempotencyKey,
        )

        verify { userLoyaltyBalanceJpaRepository.subtractCappedAtZero(42, 30) }
        verify(exactly = 0) { userLoyaltyBalanceJpaRepository.upsertAdd(any(), any()) }
    }
}
