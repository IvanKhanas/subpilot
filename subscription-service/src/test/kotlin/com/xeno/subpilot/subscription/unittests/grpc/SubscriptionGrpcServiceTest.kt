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
package com.xeno.subpilot.subscription.unittests.grpc

import com.xeno.subpilot.proto.subscription.v1.CreatePlanRequest
import com.xeno.subpilot.proto.subscription.v1.DenialReason
import com.xeno.subpilot.proto.subscription.v1.PlanAllocation as ProtoPlanAllocation
import com.xeno.subpilot.proto.subscription.v1.activateSubscriptionRequest
import com.xeno.subpilot.proto.subscription.v1.blockUserRequest
import com.xeno.subpilot.proto.subscription.v1.checkAccessRequest
import com.xeno.subpilot.proto.subscription.v1.getBalanceRequest
import com.xeno.subpilot.proto.subscription.v1.getModelPreferenceRequest
import com.xeno.subpilot.proto.subscription.v1.getPlanInfoRequest
import com.xeno.subpilot.proto.subscription.v1.getPlansRequest
import com.xeno.subpilot.proto.subscription.v1.getUserInfoRequest
import com.xeno.subpilot.proto.subscription.v1.refundAccessRequest
import com.xeno.subpilot.proto.subscription.v1.registerUserRequest
import com.xeno.subpilot.proto.subscription.v1.setModelPreferenceRequest
import com.xeno.subpilot.proto.subscription.v1.unblockUserRequest
import com.xeno.subpilot.subscription.dto.AccessResult
import com.xeno.subpilot.subscription.dto.BalanceInfo
import com.xeno.subpilot.subscription.dto.DenialReason as ServiceDenialReason
import com.xeno.subpilot.subscription.dto.FreeProviderBalance
import com.xeno.subpilot.subscription.dto.ModelPreferenceResult
import com.xeno.subpilot.subscription.dto.PaidProviderBalance
import com.xeno.subpilot.subscription.entity.SubscriptionUser
import com.xeno.subpilot.subscription.entity.UserRole
import com.xeno.subpilot.subscription.grpc.SubscriptionGrpcService
import com.xeno.subpilot.subscription.properties.PlanProperties
import com.xeno.subpilot.subscription.properties.ProviderAllocation
import com.xeno.subpilot.subscription.properties.SubscriptionProperties
import com.xeno.subpilot.subscription.repository.PlanRepository
import com.xeno.subpilot.subscription.service.AccessService
import com.xeno.subpilot.subscription.service.BalanceService
import com.xeno.subpilot.subscription.service.ModelPreferenceService
import com.xeno.subpilot.subscription.service.SubscriptionActivationService
import com.xeno.subpilot.subscription.service.UserAdminService
import com.xeno.subpilot.subscription.service.UserService
import io.grpc.Status
import io.grpc.StatusException
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class SubscriptionGrpcServiceTest {

    @MockK
    lateinit var accessService: AccessService

    @MockK
    lateinit var userService: UserService

    @MockK
    lateinit var modelPreferenceService: ModelPreferenceService

    @MockK
    lateinit var balanceService: BalanceService

    @MockK
    lateinit var activationService: SubscriptionActivationService

    @MockK(relaxed = true)
    lateinit var userAdminService: UserAdminService

    @MockK
    lateinit var planRepository: PlanRepository

    private val faker = Faker()

    private val properties =
        SubscriptionProperties(
            freeQuota = 10,
            freeQuotaResetPeriod = Duration.ofDays(7),
            defaultModel = "gpt-4o-mini",
            modelProviders = mapOf("gpt-4o" to "openai", "gpt-4o-mini" to "openai"),
            modelCosts = mapOf("gpt-4o" to 3, "gpt-4o-mini" to 1),
        )

    private val openaiBasicPlan =
        PlanProperties(
            provider = "openai",
            displayName = "Basic - 100 requests for OpenAI",
            price = BigDecimal("199.00"),
            currency = "RUB",
            allocations = listOf(ProviderAllocation(provider = "openai", requests = 100)),
        )

    private val comboPlan =
        PlanProperties(
            provider = "openai",
            displayName = "Combo Basic - 50 OpenAI + 50 Anthropic",
            price = BigDecimal("299.00"),
            currency = "RUB",
            allocations =
                listOf(
                    ProviderAllocation(provider = "openai", requests = 50),
                    ProviderAllocation(provider = "anthropic", requests = 50),
                ),
        )

    private lateinit var service: SubscriptionGrpcService
    private var testUserId: Long = 0L

    @BeforeEach
    fun setUp() {
        testUserId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        service =
            SubscriptionGrpcService(
                accessService,
                userService,
                modelPreferenceService,
                balanceService,
                activationService,
                userAdminService,
                properties,
                planRepository,
                UnconfinedTestDispatcher(),
            )
        every { planRepository.findAllActive() } returns
            mapOf(
                "openai-basic" to openaiBasicPlan,
                "combo-basic" to comboPlan,
            )
        every { planRepository.findById("openai-basic") } returns openaiBasicPlan
        every { planRepository.findById("combo-basic") } returns comboPlan
        every { planRepository.findById("unknown-plan") } returns null
    }

    @Test
    fun `checkAccess returns allowed when access granted`() =
        runTest {
            every { accessService.checkAndConsume(testUserId, "gpt-4o-mini") } returns
                AccessResult(allowed = true, freeConsumed = 1)

            val response =
                service.checkAccess(
                    checkAccessRequest {
                        userId = testUserId
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
            every { accessService.checkAndConsume(testUserId, "gpt-4o-mini") } returns
                AccessResult(allowed = false, denialReason = ServiceDenialReason.QUOTA_EXHAUSTED)

            val response =
                service.checkAccess(
                    checkAccessRequest {
                        userId = testUserId
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
                    userId = testUserId
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
                        userId = testUserId
                        modelId =
                            "gpt-4o-mini"
                    },
                )

            assertTrue(response.resetAtEpoch > 0)
        }

    @Test
    fun `registerUser returns isNew and freeQuota from service and properties`() =
        runTest {
            every { userService.registerUser(testUserId) } returns true

            val response = service.registerUser(registerUserRequest { userId = testUserId })

            assertTrue(response.isNew)
            assertEquals(10, response.freeQuota)
            assertEquals("gpt-4o-mini", response.freeModelId)
        }

    @Test
    fun `registerUser returns isNew=false for existing user`() =
        runTest {
            every { userService.registerUser(testUserId) } returns false

            val response = service.registerUser(registerUserRequest { userId = testUserId })

            assertFalse(response.isNew)
        }

    @Test
    fun `getModelPreference returns modelId when preference is set`() =
        runTest {
            every { modelPreferenceService.getModelPreference(testUserId) } returns "gpt-4o"

            val response =
                service.getModelPreference(
                    getModelPreferenceRequest {
                        userId =
                            testUserId
                    },
                )

            assertEquals("gpt-4o", response.modelId)
        }

    @Test
    fun `getModelPreference returns empty modelId when preference is absent`() =
        runTest {
            every { modelPreferenceService.getModelPreference(testUserId) } returns null

            val response =
                service.getModelPreference(
                    getModelPreferenceRequest {
                        userId =
                            testUserId
                    },
                )

            assertEquals("", response.modelId)
        }

    @Test
    fun `setModelPreference returns providerChanged from service`() =
        runTest {
            every { modelPreferenceService.setModelPreference(testUserId, "gpt-4o") } returns
                ModelPreferenceResult(
                    providerChanged = true,
                    modelCost = 3,
                    provider = "openai",
                )

            val response =
                service.setModelPreference(
                    setModelPreferenceRequest {
                        userId = testUserId
                        modelId =
                            "gpt-4o"
                    },
                )

            assertTrue(response.providerChanged)
            assertEquals(3, response.modelCost)
            assertEquals("openai", response.provider)
        }

    @Test
    fun `refundAccess delegates to accessService`() =
        runTest {
            justRun { accessService.refund(testUserId, "gpt-4o-mini", 1, 0) }

            service.refundAccess(
                refundAccessRequest {
                    userId = testUserId
                    modelId = "gpt-4o-mini"
                    freeConsumed = 1
                    paidConsumed = 0
                },
            )

            verify { accessService.refund(testUserId, "gpt-4o-mini", 1, 0) }
        }

    @Test
    fun `getPlans returns all configured plans with allocations`() =
        runTest {
            val response = service.getPlans(getPlansRequest { })

            assertEquals(2, response.plansCount)
            val openAiPlan = response.plansList.single { it.planId == "openai-basic" }
            assertEquals("openai", openAiPlan.provider)
            assertEquals("199.00", openAiPlan.price)
            assertEquals("RUB", openAiPlan.currency)
            assertEquals(1, openAiPlan.allocationsCount)
            assertEquals("openai", openAiPlan.allocationsList.first().provider)
            assertEquals(100, openAiPlan.allocationsList.first().requests)
        }

    @Test
    fun `getPlanInfo returns requested plan details`() =
        runTest {
            val response = service.getPlanInfo(getPlanInfoRequest { planId = "combo-basic" })

            val plan = response.plan
            assertEquals("combo-basic", plan.planId)
            assertEquals("openai", plan.provider)
            assertEquals("299.00", plan.price)
            assertEquals(2, plan.allocationsCount)
            assertEquals("openai", plan.allocationsList[0].provider)
            assertEquals("anthropic", plan.allocationsList[1].provider)
        }

    @Test
    fun `getPlanInfo throws NOT_FOUND for unknown plan`() =
        runTest {
            val ex =
                assertThrows<StatusException> {
                    service.getPlanInfo(getPlanInfoRequest { planId = "unknown-plan" })
                }

            assertEquals(Status.Code.NOT_FOUND, ex.status.code)
        }

    @Test
    fun `getBalance maps free and paid balances to proto response`() =
        runTest {
            val resetAt = LocalDateTime.of(2026, 4, 21, 10, 0, 0)
            every { balanceService.getBalance(testUserId) } returns
                BalanceInfo(
                    freeBalances =
                        listOf(
                            FreeProviderBalance(
                                provider = "openai",
                                requestsRemaining = 7,
                                nextResetAt = resetAt,
                            ),
                        ),
                    paidBalances =
                        listOf(
                            PaidProviderBalance(provider = "openai", requestsRemaining = 120),
                        ),
                )

            val response = service.getBalance(getBalanceRequest { userId = testUserId })

            assertEquals(1, response.freeBalancesCount)
            assertEquals("openai", response.freeBalancesList[0].provider)
            assertEquals(7, response.freeBalancesList[0].requestsRemaining)
            assertTrue(response.freeBalancesList[0].nextResetAtEpoch > 0)

            assertEquals(1, response.paidBalancesCount)
            assertEquals("openai", response.paidBalancesList[0].provider)
            assertEquals(120, response.paidBalancesList[0].requestsRemaining)
        }

    @Test
    fun `activateSubscription delegates to activation service with parsed UUID`() =
        runTest {
            val idempotencyKey = UUID.randomUUID()
            every {
                activationService.activateDirect(
                    testUserId,
                    "openai-basic",
                    idempotencyKey,
                )
            } returns
                true

            val response =
                service.activateSubscription(
                    activateSubscriptionRequest {
                        userId = testUserId
                        planId = "openai-basic"
                        this.idempotencyKey = idempotencyKey.toString()
                    },
                )

            assertNotNull(response)
            verify { activationService.activateDirect(testUserId, "openai-basic", idempotencyKey) }
        }

    @Test
    fun `getUserInfo returns found false when user does not exist`() =
        runTest {
            every { userAdminService.getUserInfo(testUserId) } returns null

            val response = service.getUserInfo(getUserInfoRequest { userId = testUserId })

            assertFalse(response.found)
        }

    @Test
    fun `getUserInfo maps user fields when user exists`() =
        runTest {
            val user =
                SubscriptionUser(
                    userId = testUserId,
                    registeredAt = java.time.Instant.ofEpochSecond(1_700_000_000L),
                    blocked = true,
                    role = UserRole.ADMIN,
                )
            every { userAdminService.getUserInfo(testUserId) } returns user

            val response = service.getUserInfo(getUserInfoRequest { userId = testUserId })

            assertTrue(response.found)
            assertTrue(response.blocked)
            assertEquals("ADMIN", response.role)
            assertEquals(1_700_000_000L, response.registeredAtEpoch)
        }

    @Test
    fun `blockUser delegates to UserAdminService`() =
        runTest {
            every { userAdminService.blockUser(testUserId) } returns Unit

            service.blockUser(blockUserRequest { userId = testUserId })

            verify(exactly = 1) { userAdminService.blockUser(testUserId) }
        }

    @Test
    fun `blockUser maps service failure to NOT_FOUND status`() =
        runTest {
            every { userAdminService.blockUser(testUserId) } throws
                IllegalArgumentException("missing")

            val ex =
                assertThrows<StatusException> {
                    service.blockUser(blockUserRequest { userId = testUserId })
                }

            assertEquals(Status.Code.NOT_FOUND, ex.status.code)
        }

    @Test
    fun `unblockUser delegates to UserAdminService`() =
        runTest {
            every { userAdminService.unblockUser(testUserId) } returns Unit

            service.unblockUser(unblockUserRequest { userId = testUserId })

            verify(exactly = 1) { userAdminService.unblockUser(testUserId) }
        }

    @Test
    fun `unblockUser maps service failure to NOT_FOUND status`() =
        runTest {
            every { userAdminService.unblockUser(testUserId) } throws
                IllegalArgumentException("missing")

            val ex =
                assertThrows<StatusException> {
                    service.unblockUser(unblockUserRequest { userId = testUserId })
                }

            assertEquals(Status.Code.NOT_FOUND, ex.status.code)
        }

    @Test
    fun `createPlan maps request and delegates to PlanRepository`() =
        runTest {
            every { planRepository.create(any(), any()) } returns Unit

            service.createPlan(
                CreatePlanRequest
                    .newBuilder()
                    .setPlanId("openai-pro")
                    .setProvider("openai")
                    .setDisplayName("OpenAI PRO")
                    .setPrice("599.00")
                    .setCurrency("RUB")
                    .addAllocations(
                        ProtoPlanAllocation
                            .newBuilder()
                            .setProvider("openai")
                            .setRequests(300)
                            .build(),
                    ).addAllocations(
                        ProtoPlanAllocation
                            .newBuilder()
                            .setProvider("anthropic")
                            .setRequests(100)
                            .build(),
                    ).build(),
            )

            verify(exactly = 1) {
                planRepository.create(
                    "openai-pro",
                    PlanProperties(
                        provider = "openai",
                        displayName = "OpenAI PRO",
                        price = BigDecimal("599.00"),
                        currency = "RUB",
                        allocations =
                            listOf(
                                ProviderAllocation(provider = "openai", requests = 300),
                                ProviderAllocation(provider = "anthropic", requests = 100),
                            ),
                    ),
                )
            }
        }
}
