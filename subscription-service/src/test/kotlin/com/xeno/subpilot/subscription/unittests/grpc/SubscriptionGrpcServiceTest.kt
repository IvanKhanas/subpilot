package com.xeno.subpilot.subscription.unittests.grpc

import com.xeno.subpilot.proto.subscription.v1.DenialReason
import com.xeno.subpilot.proto.subscription.v1.checkAccessRequest
import com.xeno.subpilot.proto.subscription.v1.getModelPreferenceRequest
import com.xeno.subpilot.proto.subscription.v1.refundAccessRequest
import com.xeno.subpilot.proto.subscription.v1.registerUserRequest
import com.xeno.subpilot.proto.subscription.v1.setModelPreferenceRequest
import com.xeno.subpilot.subscription.dto.AccessResult
import com.xeno.subpilot.subscription.dto.DenialReason as ServiceDenialReason
import com.xeno.subpilot.subscription.grpc.SubscriptionGrpcService
import com.xeno.subpilot.subscription.properties.SubscriptionProperties
import com.xeno.subpilot.subscription.service.AccessService
import com.xeno.subpilot.subscription.service.ModelPreferenceService
import com.xeno.subpilot.subscription.service.UserService
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
import java.time.LocalDateTime

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class SubscriptionGrpcServiceTest {

    @MockK
    lateinit var accessService: AccessService

    @MockK
    lateinit var userService: UserService

    @MockK
    lateinit var modelPreferenceService: ModelPreferenceService

    private val properties =
        SubscriptionProperties(
            freeQuota = 10,
            freeQuotaResetPeriod = Duration.ofDays(7),
            defaultModel = "gpt-4o-mini",
            modelProviders = mapOf("gpt-4o" to "openai", "gpt-4o-mini" to "openai"),
            modelCosts = mapOf("gpt-4o" to 3, "gpt-4o-mini" to 1),
            plans = emptyMap(),
        )

    private lateinit var service: SubscriptionGrpcService

    @BeforeEach
    fun setUp() {
        service =
            SubscriptionGrpcService(
                accessService,
                userService,
                modelPreferenceService,
                properties,
                UnconfinedTestDispatcher(),
            )
    }

    @Test
    fun `checkAccess returns allowed when access granted`() =
        runTest {
            every { accessService.checkAndConsume(1L, "gpt-4o-mini") } returns
                AccessResult(allowed = true, freeConsumed = 1)

            val response =
                service.checkAccess(
                    checkAccessRequest {
                        userId = 1L
                        modelId =
                            "gpt-4o-mini"
                    },
                )

            assertTrue(response.allowed)
            assertEquals(1, response.freeConsumed)
        }

    @Test
    fun `checkAccess returns denied with correct reason`() =
        runTest {
            every { accessService.checkAndConsume(1L, "gpt-4o-mini") } returns
                AccessResult(allowed = false, denialReason = ServiceDenialReason.QUOTA_EXHAUSTED)

            val response =
                service.checkAccess(
                    checkAccessRequest {
                        userId = 1L
                        modelId =
                            "gpt-4o-mini"
                    },
                )

            assertFalse(response.allowed)
            assertEquals(DenialReason.QUOTA_EXHAUSTED, response.denialReason)
        }

    @ParameterizedTest(name = "ServiceDenialReason.{0} maps to proto DenialReason.{1}")
    @CsvSource(
        "QUOTA_EXHAUSTED, QUOTA_EXHAUSTED",
        "NO_SUBSCRIPTION, NO_SUBSCRIPTION",
        "BLOCKED, BLOCKED",
        "UNSPECIFIED, DENIAL_REASON_UNSPECIFIED",
    )
    fun `checkAccess maps all denial reasons to proto`(
        serviceDenial: ServiceDenialReason,
        protoDenial: DenialReason,
    ) = runTest {
        every { accessService.checkAndConsume(any(), any()) } returns
            AccessResult(allowed = false, denialReason = serviceDenial)

        val response =
            service.checkAccess(
                checkAccessRequest {
                    userId = 1L
                    modelId = "gpt-4o-mini"
                },
            )

        assertEquals(protoDenial, response.denialReason)
    }

    @Test
    fun `checkAccess includes resetAtEpoch when resetAt is present`() =
        runTest {
            val resetAt = LocalDateTime.of(2026, 4, 20, 12, 0, 0)
            every { accessService.checkAndConsume(any(), any()) } returns
                AccessResult(allowed = true, resetAt = resetAt)

            val response =
                service.checkAccess(
                    checkAccessRequest {
                        userId = 1L
                        modelId =
                            "gpt-4o-mini"
                    },
                )

            assertTrue(response.resetAtEpoch > 0)
        }

    @Test
    fun `registerUser returns isNew and freeQuota from service and properties`() =
        runTest {
            every { userService.registerUser(1L) } returns true

            val response = service.registerUser(registerUserRequest { userId = 1L })

            assertTrue(response.isNew)
            assertEquals(10, response.freeQuota)
            assertEquals("gpt-4o-mini", response.freeModelId)
        }

    @Test
    fun `registerUser returns isNew=false for existing user`() =
        runTest {
            every { userService.registerUser(1L) } returns false

            val response = service.registerUser(registerUserRequest { userId = 1L })

            assertFalse(response.isNew)
        }

    @Test
    fun `getModelPreference returns modelId when preference is set`() =
        runTest {
            every { modelPreferenceService.getModelPreference(1L) } returns "gpt-4o"

            val response = service.getModelPreference(getModelPreferenceRequest { userId = 1L })

            assertEquals("gpt-4o", response.modelId)
        }

    @Test
    fun `getModelPreference returns empty modelId when preference is absent`() =
        runTest {
            every { modelPreferenceService.getModelPreference(1L) } returns null

            val response = service.getModelPreference(getModelPreferenceRequest { userId = 1L })

            assertEquals("", response.modelId)
        }

    @Test
    fun `setModelPreference returns providerChanged from service`() =
        runTest {
            every { modelPreferenceService.setModelPreference(1L, "gpt-4o") } returns true

            val response =
                service.setModelPreference(
                    setModelPreferenceRequest {
                        userId = 1L
                        modelId =
                            "gpt-4o"
                    },
                )

            assertTrue(response.providerChanged)
        }

    @Test
    fun `refundAccess delegates to accessService`() =
        runTest {
            justRun { accessService.refund(1L, "gpt-4o-mini", 1, 0) }

            service.refundAccess(
                refundAccessRequest {
                    userId = 1L
                    modelId = "gpt-4o-mini"
                    freeConsumed = 1
                    paidConsumed = 0
                },
            )

            verify { accessService.refund(1L, "gpt-4o-mini", 1, 0) }
        }
}
