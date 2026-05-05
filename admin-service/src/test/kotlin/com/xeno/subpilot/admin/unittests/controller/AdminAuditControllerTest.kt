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

import com.xeno.subpilot.admin.controller.AdminAuditController
import com.xeno.subpilot.admin.entity.AdminAction
import com.xeno.subpilot.admin.entity.AdminTargetType
import com.xeno.subpilot.admin.entity.AuditLog
import com.xeno.subpilot.admin.service.AuditService
import com.xeno.subpilot.admin.unittests.AdminTestFixtures
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

import java.time.LocalDateTime

import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class AdminAuditControllerTest {

    @MockK
    lateinit var auditService: AuditService

    private lateinit var controller: AdminAuditController

    companion object {
        const val LOG_ID = 101L
        const val TARGET_ID = "1000"
        const val PAYLOAD = """{"flushed_count":11}"""
    }

    @BeforeEach
    fun setUp() {
        controller = AdminAuditController(auditService)
    }

    @Test
    fun `getAuditLog maps entity page to response page`() {
        val pageable: Pageable = PageRequest.of(0, 10)
        val createdAt = LocalDateTime.of(2026, 4, 25, 12, 30, 0)
        val page =
            PageImpl(
                listOf(
                    AuditLog(
                        id = LOG_ID,
                        operator = AdminTestFixtures.OPERATOR,
                        action = AdminAction.TRIGGER_OUTBOX_FLUSH,
                        targetType = AdminTargetType.PAYMENT,
                        targetId = TARGET_ID,
                        payload = PAYLOAD,
                        createdAt = createdAt,
                    ),
                ),
            )
        every { auditService.getPage(pageable) } returns page

        val response = controller.getAuditLog(pageable)

        assertEquals(1, response.totalElements)
        assertEquals(LOG_ID, response.content.first().id)
        assertEquals(AdminAction.TRIGGER_OUTBOX_FLUSH.name, response.content.first().action)
        assertEquals(AdminTargetType.PAYMENT.name, response.content.first().targetType)
        assertEquals("2026-04-25 12:30:00", response.content.first().createdAt)
        verify(exactly = 1) { auditService.getPage(pageable) }
    }
}
