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
package com.xeno.subpilot.admin.unittests.controller

import com.xeno.subpilot.admin.controller.AdminPaymentController
import com.xeno.subpilot.admin.service.AdminPaymentService
import com.xeno.subpilot.admin.unittests.AdminTestFixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.oauth2.jwt.Jwt

import kotlin.test.assertEquals

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class AdminPaymentControllerTest {

    @MockK
    lateinit var adminPaymentService: AdminPaymentService

    @MockK
    lateinit var jwt: Jwt

    private lateinit var controller: AdminPaymentController

    companion object {
        const val FLUSHED_COUNT = 11
    }

    @BeforeEach
    fun setUp() {
        controller = AdminPaymentController(adminPaymentService)
        every { jwt.subject } returns AdminTestFixtures.OPERATOR
    }

    @Test
    fun `flushOutbox delegates to service and returns flushed_count map`() =
        runTest {
            coEvery { adminPaymentService.triggerOutboxFlush(AdminTestFixtures.OPERATOR) } returns
                FLUSHED_COUNT

            val result = controller.flushOutbox(jwt)

            assertEquals(mapOf("flushed_count" to FLUSHED_COUNT), result)
            coVerify(
                exactly = 1,
            ) { adminPaymentService.triggerOutboxFlush(AdminTestFixtures.OPERATOR) }
        }
}
