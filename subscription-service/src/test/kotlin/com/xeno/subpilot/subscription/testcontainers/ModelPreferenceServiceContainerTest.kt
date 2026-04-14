package com.xeno.subpilot.subscription.testcontainers

import com.xeno.subpilot.subscription.service.ModelPreferenceService
import com.xeno.subpilot.subscription.service.UserService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@SpringBootTest
@Transactional
class ModelPreferenceServiceContainerTest {

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

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var modelPreferenceService: ModelPreferenceService

    private var nextUserId = 90_000L

    private fun newUserId(): Long = nextUserId++

    @Test
    fun `getModelPreference returns default model after registration`() {
        val userId = newUserId()
        userService.registerUser(userId)

        val preference = modelPreferenceService.getModelPreference(userId)

        assertEquals("gpt-4o-mini", preference)
    }

    @Test
    fun `getModelPreference returns null for unknown user`() {
        val preference = modelPreferenceService.getModelPreference(newUserId())

        assertNull(preference)
    }

    @Test
    fun `setModelPreference updates stored model`() {
        val userId = newUserId()
        userService.registerUser(userId)

        modelPreferenceService.setModelPreference(userId, "gpt-4o")

        assertEquals("gpt-4o", modelPreferenceService.getModelPreference(userId))
    }

    @Test
    fun `setModelPreference returns false when staying within same provider`() {
        val userId = newUserId()
        userService.registerUser(userId)

        val providerChanged = modelPreferenceService.setModelPreference(userId, "gpt-4o")

        assertFalse(providerChanged)
    }
}
