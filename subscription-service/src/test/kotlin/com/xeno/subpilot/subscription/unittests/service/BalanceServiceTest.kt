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

import com.xeno.subpilot.subscription.entity.UserFreeQuota
import com.xeno.subpilot.subscription.entity.UserRequestBalance
import com.xeno.subpilot.subscription.repository.UserFreeQuotaRepository
import com.xeno.subpilot.subscription.repository.UserRequestBalanceRepository
import com.xeno.subpilot.subscription.service.BalanceService
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import java.time.LocalDateTime

import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class BalanceServiceTest {

    @MockK
    lateinit var userFreeQuotaRepository: UserFreeQuotaRepository

    @MockK
    lateinit var userRequestBalanceRepository: UserRequestBalanceRepository

    private val faker = Faker()

    private lateinit var service: BalanceService
    private var userId: Long = 0L

    @BeforeEach
    fun setUp() {
        userId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        service = BalanceService(userFreeQuotaRepository, userRequestBalanceRepository)
    }

    @Test
    fun `getBalance returns empty sections when no balances exist`() {
        every { userFreeQuotaRepository.findAllByUserId(userId) } returns emptyList()
        every { userRequestBalanceRepository.findAllByUserId(userId) } returns emptyList()

        val result = service.getBalance(userId)

        assertTrue(result.freeBalances.isEmpty())
        assertTrue(result.paidBalances.isEmpty())
    }

    @Test
    fun `getBalance maps free and paid entities to dto`() {
        val resetAt = LocalDateTime.of(2026, 4, 24, 10, 30, 0)
        every { userFreeQuotaRepository.findAllByUserId(userId) } returns
            listOf(
                UserFreeQuota(
                    userId = userId,
                    provider = "openai",
                    requestsRemaining = 7,
                    nextResetAt = resetAt,
                ),
            )
        every { userRequestBalanceRepository.findAllByUserId(userId) } returns
            listOf(
                UserRequestBalance(
                    userId = userId,
                    provider = "openai",
                    requestsRemaining = 120,
                ),
            )

        val result = service.getBalance(userId)

        assertEquals(1, result.freeBalances.size)
        assertEquals("openai", result.freeBalances[0].provider)
        assertEquals(7, result.freeBalances[0].requestsRemaining)
        assertEquals(resetAt, result.freeBalances[0].nextResetAt)

        assertEquals(1, result.paidBalances.size)
        assertEquals("openai", result.paidBalances[0].provider)
        assertEquals(120, result.paidBalances[0].requestsRemaining)
    }
}
