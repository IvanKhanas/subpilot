package com.xeno.subpilot.subscription.unittests.service

import com.xeno.subpilot.subscription.entity.UserFreeQuota
import com.xeno.subpilot.subscription.entity.UserRequestBalance
import com.xeno.subpilot.subscription.repository.UserFreeQuotaRepository
import com.xeno.subpilot.subscription.repository.UserRequestBalanceRepository
import com.xeno.subpilot.subscription.service.BalanceService
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
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

    private lateinit var service: BalanceService

    @BeforeEach
    fun setUp() {
        service = BalanceService(userFreeQuotaRepository, userRequestBalanceRepository)
    }

    @Test
    fun `getBalance returns empty sections when no balances exist`() {
        every { userFreeQuotaRepository.findAllByUserId(42L) } returns emptyList()
        every { userRequestBalanceRepository.findAllByUserId(42L) } returns emptyList()

        val result = service.getBalance(42L)

        assertTrue(result.freeBalances.isEmpty())
        assertTrue(result.paidBalances.isEmpty())
    }

    @Test
    fun `getBalance maps free and paid entities to dto`() {
        val resetAt = LocalDateTime.of(2026, 4, 24, 10, 30, 0)
        every { userFreeQuotaRepository.findAllByUserId(42L) } returns
            listOf(
                UserFreeQuota(
                    userId = 42L,
                    provider = "openai",
                    requestsRemaining = 7,
                    nextResetAt = resetAt,
                ),
            )
        every { userRequestBalanceRepository.findAllByUserId(42L) } returns
            listOf(
                UserRequestBalance(
                    userId = 42L,
                    provider = "openai",
                    requestsRemaining = 120,
                ),
            )

        val result = service.getBalance(42L)

        assertEquals(1, result.freeBalances.size)
        assertEquals("openai", result.freeBalances[0].provider)
        assertEquals(7, result.freeBalances[0].requestsRemaining)
        assertEquals(resetAt, result.freeBalances[0].nextResetAt)

        assertEquals(1, result.paidBalances.size)
        assertEquals("openai", result.paidBalances[0].provider)
        assertEquals(120, result.paidBalances[0].requestsRemaining)
    }
}
