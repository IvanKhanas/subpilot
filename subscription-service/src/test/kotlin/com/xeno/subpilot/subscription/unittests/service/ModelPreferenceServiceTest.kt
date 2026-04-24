package com.xeno.subpilot.subscription.unittests.service

import com.xeno.subpilot.subscription.properties.SubscriptionProperties
import com.xeno.subpilot.subscription.repository.UserModelPreferenceRepository
import com.xeno.subpilot.subscription.service.ModelPreferenceService
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

import java.time.Duration

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class ModelPreferenceServiceTest {

    @MockK
    lateinit var modelPreferenceRepository: UserModelPreferenceRepository

    private lateinit var service: ModelPreferenceService

    private val properties =
        SubscriptionProperties(
            freeQuota = 10,
            freeQuotaResetPeriod = Duration.ofDays(7),
            defaultModel = "gpt-4o-mini",
            modelProviders =
                mapOf(
                    "gpt-4o" to "openai",
                    "gpt-4o-mini" to "openai",
                    "claude-3-5-sonnet" to "anthropic",
                ),
            modelCosts = mapOf("gpt-4o" to 3, "gpt-4o-mini" to 1, "claude-3-5-sonnet" to 2),
        )

    private val userId = 7L

    @BeforeEach
    fun setUp() {
        service = ModelPreferenceService(modelPreferenceRepository, properties)
    }

    @Test
    fun `getModelPreference returns model from repository`() {
        every { modelPreferenceRepository.findById(userId) } returns "gpt-4o"

        assertEquals("gpt-4o", service.getModelPreference(userId))
    }

    @Test
    fun `getModelPreference returns null when no preference set`() {
        every { modelPreferenceRepository.findById(userId) } returns null

        assertNull(service.getModelPreference(userId))
    }

    @ParameterizedTest(
        name = "switching from {0} to {1} within same provider -> providerChanged=false",
    )
    @CsvSource(
        "gpt-4o, gpt-4o-mini",
        "gpt-4o-mini, gpt-4o",
    )
    fun `setModelPreference returns false when switching models within same provider`(
        previousModel: String,
        newModel: String,
    ) {
        every { modelPreferenceRepository.findById(userId) } returns previousModel
        justRun { modelPreferenceRepository.upsert(userId, newModel) }

        val result = service.setModelPreference(userId, newModel)

        assertFalse(result.providerChanged)
        assertEquals(properties.modelCosts[newModel], result.modelCost)
        assertEquals(properties.modelProviders[newModel], result.provider)
        verify { modelPreferenceRepository.upsert(userId, newModel) }
    }

    @ParameterizedTest(name = "switching from {0} to {1} across providers -> providerChanged=true")
    @CsvSource(
        "gpt-4o, claude-3-5-sonnet",
        "claude-3-5-sonnet, gpt-4o-mini",
    )
    fun `setModelPreference returns true when switching to a different provider`(
        previousModel: String,
        newModel: String,
    ) {
        every { modelPreferenceRepository.findById(userId) } returns previousModel
        justRun { modelPreferenceRepository.upsert(userId, newModel) }

        val result = service.setModelPreference(userId, newModel)

        assertTrue(result.providerChanged)
        assertEquals(properties.modelCosts[newModel], result.modelCost)
        assertEquals(properties.modelProviders[newModel], result.provider)
        verify { modelPreferenceRepository.upsert(userId, newModel) }
    }

    @Test
    fun `setModelPreference returns false when no previous preference exists`() {
        every { modelPreferenceRepository.findById(userId) } returns null
        justRun { modelPreferenceRepository.upsert(userId, "gpt-4o") }

        val result = service.setModelPreference(userId, "gpt-4o")

        assertFalse(result.providerChanged)
        assertEquals(3, result.modelCost)
        assertEquals("openai", result.provider)
    }
}
