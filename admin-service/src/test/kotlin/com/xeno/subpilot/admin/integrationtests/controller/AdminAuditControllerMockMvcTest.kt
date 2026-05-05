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

import com.xeno.subpilot.admin.controller.AdminAuditController
import com.xeno.subpilot.admin.entity.AdminAction
import com.xeno.subpilot.admin.entity.AdminTargetType
import com.xeno.subpilot.admin.entity.AuditLog
import com.xeno.subpilot.admin.integrationtests.MockMvcTestSupport
import com.xeno.subpilot.admin.service.AuditService
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.time.LocalDateTime

import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class AdminAuditControllerMockMvcTest {

    @MockK
    lateinit var auditService: AuditService

    private lateinit var mockMvc: MockMvc

    private val faker = Faker()

    private lateinit var operator: String
    private var logId: Long = 0L
    private lateinit var targetId: String
    private lateinit var payload: String
    private val pageSize = 10

    @BeforeEach
    fun setUp() {
        operator = "admin-${faker.number().digits(6)}"
        logId = faker.number().numberBetween(100L, 10000L)
        targetId = faker.regexify("[0-9]{6}")
        payload = """{"flushed_count":${faker.number().numberBetween(1, 50)}}"""
        every { auditService.getPage(any()) } returns
            PageImpl(
                listOf(
                    AuditLog(
                        id = logId,
                        operator = operator,
                        action = AdminAction.TRIGGER_OUTBOX_FLUSH,
                        targetType = AdminTargetType.PAYMENT,
                        targetId = targetId,
                        payload = payload,
                        createdAt = LocalDateTime.of(2026, 4, 26, 10, 15, 0),
                    ),
                ),
                PageRequest.of(0, pageSize),
                1,
            )
        mockMvc = MockMvcTestSupport.buildMockMvc(operator, AdminAuditController(auditService))
    }

    @Test
    fun `GET audit returns page payload and forwards pageable`() {
        val pageableSlot = slot<Pageable>()

        mockMvc
            .perform(get("/admin/audit").param("page", "0").param("size", pageSize.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].id").value(logId))
            .andExpect(jsonPath("$.content[0].operator").value(operator))
            .andExpect(jsonPath("$.content[0].action").value(AdminAction.TRIGGER_OUTBOX_FLUSH.name))
            .andExpect(jsonPath("$.content[0].targetType").value(AdminTargetType.PAYMENT.name))
            .andExpect(jsonPath("$.content[0].targetId").value(targetId))

        verify(exactly = 1) { auditService.getPage(capture(pageableSlot)) }
        assertEquals(0, pageableSlot.captured.pageNumber)
        assertEquals(pageSize, pageableSlot.captured.pageSize)
    }
}
