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
package com.xeno.subpilot.admin.unittests.service

import com.xeno.subpilot.admin.entity.AdminAction
import com.xeno.subpilot.admin.entity.AdminTargetType
import com.xeno.subpilot.admin.entity.AuditLog
import com.xeno.subpilot.admin.repository.AuditLogRepository
import com.xeno.subpilot.admin.service.AuditService
import com.xeno.subpilot.admin.unittests.AdminTestFixtures
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

import kotlin.test.assertEquals
import kotlin.test.assertSame

@ExtendWith(MockKExtension::class)
class AuditServiceTest {

    @MockK
    lateinit var auditLogRepository: AuditLogRepository

    private lateinit var service: AuditService

    companion object {
        const val TARGET_ID = "1000"
        const val PAYLOAD = """{"key":"value"}"""
    }

    @BeforeEach
    fun setUp() {
        service = AuditService(auditLogRepository)
    }

    @Test
    fun `record stores audit entity with passed fields`() {
        val auditLogSlot = slot<AuditLog>()
        every { auditLogRepository.save(capture(auditLogSlot)) } answers { auditLogSlot.captured }

        service.record(
            operator = AdminTestFixtures.OPERATOR,
            action = AdminAction.BAN_USER,
            targetType = AdminTargetType.USER,
            targetId = TARGET_ID,
            payload = PAYLOAD,
        )

        assertEquals(AdminTestFixtures.OPERATOR, auditLogSlot.captured.operator)
        assertEquals(AdminAction.BAN_USER, auditLogSlot.captured.action)
        assertEquals(AdminTargetType.USER, auditLogSlot.captured.targetType)
        assertEquals(TARGET_ID, auditLogSlot.captured.targetId)
        assertEquals(PAYLOAD, auditLogSlot.captured.payload)
    }

    @Test
    fun `getPage delegates to repository ordered query`() {
        val pageable: Pageable = PageRequest.of(0, 20)
        val expectedPage = PageImpl(listOf(auditLog()))
        every { auditLogRepository.findAllByOrderByCreatedAtDesc(pageable) } returns expectedPage

        val page = service.getPage(pageable)

        assertSame(expectedPage, page)
        verify(exactly = 1) { auditLogRepository.findAllByOrderByCreatedAtDesc(pageable) }
    }

    private fun auditLog(): AuditLog =
        AuditLog(
            id = 1L,
            operator = AdminTestFixtures.OPERATOR,
            action = AdminAction.BAN_USER,
            targetType = AdminTargetType.USER,
            targetId = TARGET_ID,
            payload = PAYLOAD,
        )
}
