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
package com.xeno.subpilot.admin.integrationtests.controller

import com.xeno.subpilot.admin.controller.AdminSubscriptionController
import com.xeno.subpilot.admin.dto.CreatePlanRequest
import com.xeno.subpilot.admin.integrationtests.MockMvcTestSupport
import com.xeno.subpilot.admin.service.AdminSubscriptionService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.math.BigDecimal

import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class AdminSubscriptionControllerMockMvcTest {

    @MockK
    lateinit var adminSubscriptionService: AdminSubscriptionService

    private lateinit var mockMvc: MockMvc

    private val faker = Faker()

    private lateinit var operator: String
    private lateinit var planId: String
    private lateinit var provider: String
    private lateinit var displayName: String
    private lateinit var currency: String
    private var requests: Int = 0

    @BeforeEach
    fun setUp() {
        operator = "admin-${faker.number().digits(6)}"
        planId = faker.regexify("[a-z]{6}") + "-pro"
        provider = faker.options().option("openai", "anthropic")
        displayName = faker.commerce().productName()
        currency = faker.options().option("RUB", "USD")
        requests = faker.number().numberBetween(1, 500)

        coEvery { adminSubscriptionService.createPlan(operator, any()) } returns Unit
        mockMvc =
            MockMvcTestSupport.buildMockMvc(
                operator,
                AdminSubscriptionController(adminSubscriptionService),
            )
    }

    @Test
    fun `POST create plan returns 201 and maps request body`() {
        val price = faker.number().randomDouble(2, 1, 999)
        val body =
            """
            {
              "planId":"$planId",
              "provider":"$provider",
              "displayName":"$displayName",
              "price":$price,
              "currency":"$currency",
              "allocations":[{"provider":"$provider","requests":$requests}]
            }
            """.trimIndent()
        val requestSlot = slot<CreatePlanRequest>()

        MockMvcTestSupport
            .performSuspend(
                mockMvc,
                post("/admin/subscription/plans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            ).andExpect(status().isCreated)

        coVerify(
            exactly = 1,
        ) { adminSubscriptionService.createPlan(operator, capture(requestSlot)) }
        assertEquals(planId, requestSlot.captured.planId)
        assertEquals(provider, requestSlot.captured.provider)
        assertEquals(displayName, requestSlot.captured.displayName)
        assertEquals(BigDecimal(price.toString()), requestSlot.captured.price)
        assertEquals(currency, requestSlot.captured.currency)
        assertEquals(
            requests,
            requestSlot.captured.allocations
                .single()
                .requests,
        )
    }

    @Test
    fun `POST create plan returns 400 on invalid body`() {
        val body =
            """
            {
              "planId":"$planId",
              "provider":"$provider",
              "displayName":"$displayName",
              "price":0,
              "currency":"$currency",
              "allocations":[]
            }
            """.trimIndent()

        mockMvc
            .perform(
                post("/admin/subscription/plans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            ).andExpect(status().isBadRequest)

        coVerify(exactly = 0) { adminSubscriptionService.createPlan(any(), any()) }
    }
}
