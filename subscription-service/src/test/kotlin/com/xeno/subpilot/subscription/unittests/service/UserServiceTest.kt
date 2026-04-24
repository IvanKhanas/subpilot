package com.xeno.subpilot.subscription.unittests.service

import com.xeno.subpilot.subscription.properties.SubscriptionProperties
import com.xeno.subpilot.subscription.repository.SubscriptionUserRepository
import com.xeno.subpilot.subscription.repository.UserFreeQuotaRepository
import com.xeno.subpilot.subscription.repository.UserModelPreferenceRepository
import com.xeno.subpilot.subscription.service.UserService
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import java.time.Duration

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

    private val properties =
        SubscriptionProperties(
            freeQuota = 10,
            freeQuotaResetPeriod = Duration.ofDays(7),
            defaultModel = "gpt-4o-mini",
            modelProviders = mapOf("gpt-4o" to "openai", "gpt-4o-mini" to "openai"),
            modelCosts = mapOf("gpt-4o" to 3, "gpt-4o-mini" to 1),
        )

    private val userId = 42L

    @BeforeEach
    fun setUp() {
        service =
            UserService(
                subscriptionUserRepository,
                freeQuotaRepository,
                modelPreferenceRepository,
                properties,
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
}
