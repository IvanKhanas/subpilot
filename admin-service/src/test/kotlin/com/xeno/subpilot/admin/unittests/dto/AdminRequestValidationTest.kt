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
package com.xeno.subpilot.admin.unittests.dto

import com.xeno.subpilot.admin.dto.AddSubscriptionRequest
import com.xeno.subpilot.admin.dto.AllocationRequest
import com.xeno.subpilot.admin.dto.CreatePlanRequest
import com.xeno.subpilot.admin.unittests.AdminTestFixtures
import jakarta.validation.Validation
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

import java.math.BigDecimal
import java.util.stream.Stream

import kotlin.test.assertEquals

class AdminRequestValidationTest {

    companion object {
        @JvmStatic
        fun invalidAddSubscriptionCases(): Stream<Arguments> =
            Stream.of(
                arguments(AddSubscriptionRequest("", AdminTestFixtures.IDEMPOTENCY_KEY), "planId"),
                arguments(AddSubscriptionRequest(AdminTestFixtures.PLAN_ID, ""), "idempotencyKey"),
            )

        @JvmStatic
        fun invalidAllocationCases(): Stream<Arguments> =
            Stream.of(
                arguments(AllocationRequest("", 10), "provider"),
                arguments(AllocationRequest(AdminTestFixtures.PROVIDER, 0), "requests"),
            )
    }

    private val validator = Validation.buildDefaultValidatorFactory().validator

    @ParameterizedTest(name = "{1} must be validated")
    @MethodSource("invalidAddSubscriptionCases")
    fun `AddSubscriptionRequest enforces not blank constraints`(
        request: AddSubscriptionRequest,
        expectedField: String,
    ) {
        val violations = validator.validate(request)

        assertEquals(setOf(expectedField), violations.map { it.propertyPath.toString() }.toSet())
    }

    @ParameterizedTest(name = "{1} must be validated")
    @MethodSource("invalidAllocationCases")
    fun `AllocationRequest enforces provider and requests constraints`(
        request: AllocationRequest,
        expectedField: String,
    ) {
        val violations = validator.validate(request)

        assertEquals(setOf(expectedField), violations.map { it.propertyPath.toString() }.toSet())
    }

    @Test
    fun `CreatePlanRequest validates minimum price and non-empty allocations`() {
        val request =
            CreatePlanRequest(
                planId = AdminTestFixtures.PLAN_ID,
                provider = AdminTestFixtures.PROVIDER,
                displayName = "Plan",
                price = BigDecimal.ZERO,
                currency = AdminTestFixtures.CURRENCY,
                allocations = emptyList(),
            )

        val violations = validator.validate(request)

        assertEquals(
            setOf("price", "allocations"),
            violations.map { it.propertyPath.toString() }.toSet(),
        )
    }
}
