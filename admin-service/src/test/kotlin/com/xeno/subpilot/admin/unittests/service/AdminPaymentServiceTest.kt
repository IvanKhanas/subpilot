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
package com.xeno.subpilot.admin.unittests.service

import com.xeno.subpilot.admin.client.PaymentAdminClient
import com.xeno.subpilot.admin.entity.AdminAction
import com.xeno.subpilot.admin.entity.AdminTargetType
import com.xeno.subpilot.admin.service.AdminPaymentService
import com.xeno.subpilot.admin.service.AuditService
import com.xeno.subpilot.admin.unittests.AdminTestFixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import kotlin.test.assertEquals

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class AdminPaymentServiceTest {

    @MockK
    lateinit var paymentClient: PaymentAdminClient

    @MockK
    lateinit var auditService: AuditService

    private lateinit var service: AdminPaymentService

    companion object {
        const val FLUSHED_COUNT = 17
        const val PAYLOAD = """{"flushed_count":17}"""
    }

    @BeforeEach
    fun setUp() {
        service =
            AdminPaymentService(
                paymentClient = paymentClient,
                auditService = auditService,
            )
    }

    @Test
    fun `triggerOutboxFlush returns flushed count and records audit`() =
        runTest {
            coEvery { paymentClient.triggerOutboxFlush() } returns FLUSHED_COUNT
            justRun {
                auditService.record(
                    operator = AdminTestFixtures.OPERATOR,
                    action = AdminAction.TRIGGER_OUTBOX_FLUSH,
                    targetType = AdminTargetType.PAYMENT,
                    payload = PAYLOAD,
                )
            }

            val flushed = service.triggerOutboxFlush(AdminTestFixtures.OPERATOR)

            assertEquals(FLUSHED_COUNT, flushed)
            coVerify(exactly = 1) { paymentClient.triggerOutboxFlush() }
            verify(exactly = 1) {
                auditService.record(
                    operator = AdminTestFixtures.OPERATOR,
                    action = AdminAction.TRIGGER_OUTBOX_FLUSH,
                    targetType = AdminTargetType.PAYMENT,
                    payload = PAYLOAD,
                )
            }
        }
}
