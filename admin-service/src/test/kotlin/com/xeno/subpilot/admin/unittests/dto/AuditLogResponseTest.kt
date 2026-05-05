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
package com.xeno.subpilot.admin.unittests.dto

import com.xeno.subpilot.admin.dto.AuditLogResponse
import com.xeno.subpilot.admin.entity.AdminAction
import com.xeno.subpilot.admin.entity.AdminTargetType
import com.xeno.subpilot.admin.entity.AuditLog
import com.xeno.subpilot.admin.unittests.AdminTestFixtures
import org.junit.jupiter.api.Test

import java.time.LocalDateTime

import kotlin.test.assertEquals

class AuditLogResponseTest {

    companion object {
        const val LOG_ID = 10L
        const val TARGET_ID = "1000"
        const val PAYLOAD = """{"flushed_count":11}"""
    }

    @Test
    fun `from maps entity fields and formats createdAt`() {
        val entity =
            AuditLog(
                id = LOG_ID,
                operator = AdminTestFixtures.OPERATOR,
                action = AdminAction.TRIGGER_OUTBOX_FLUSH,
                targetType = AdminTargetType.PAYMENT,
                targetId = TARGET_ID,
                payload = PAYLOAD,
                createdAt = LocalDateTime.of(2026, 4, 25, 12, 30, 0),
            )

        val response = AuditLogResponse.from(entity)

        assertEquals(LOG_ID, response.id)
        assertEquals(AdminTestFixtures.OPERATOR, response.operator)
        assertEquals(AdminAction.TRIGGER_OUTBOX_FLUSH.name, response.action)
        assertEquals(AdminTargetType.PAYMENT.name, response.targetType)
        assertEquals(TARGET_ID, response.targetId)
        assertEquals(PAYLOAD, response.payload)
        assertEquals("2026-04-25 12:30:00", response.createdAt)
    }
}
