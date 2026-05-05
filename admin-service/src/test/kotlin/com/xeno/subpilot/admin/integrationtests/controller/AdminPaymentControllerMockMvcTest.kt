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

import com.xeno.subpilot.admin.controller.AdminPaymentController
import com.xeno.subpilot.admin.integrationtests.MockMvcTestSupport
import com.xeno.subpilot.admin.service.AdminPaymentService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ExtendWith(MockKExtension::class)
class AdminPaymentControllerMockMvcTest {

    @MockK
    lateinit var adminPaymentService: AdminPaymentService

    private lateinit var mockMvc: MockMvc

    private val faker = Faker()

    private lateinit var operator: String
    private var flushedCount: Int = 0

    @BeforeEach
    fun setUp() {
        operator = "admin-${faker.number().digits(6)}"
        flushedCount = faker.number().numberBetween(1, 500)
        coEvery { adminPaymentService.triggerOutboxFlush(operator) } returns flushedCount
        mockMvc =
            MockMvcTestSupport.buildMockMvc(operator, AdminPaymentController(adminPaymentService))
    }

    @Test
    fun `POST flush outbox returns flushed count and delegates`() {
        MockMvcTestSupport
            .performSuspend(mockMvc, post("/admin/payment/outbox/flush"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.flushed_count").value(flushedCount))

        coVerify(exactly = 1) { adminPaymentService.triggerOutboxFlush(operator) }
    }
}
