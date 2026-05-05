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
package com.xeno.subpilot.admin.dto

import com.xeno.subpilot.admin.entity.AuditLog

import java.time.format.DateTimeFormatter

data class AuditLogResponse(
    val id: Long,
    val operator: String,
    val action: String,
    val targetType: String,
    val targetId: String?,
    val payload: String?,
    val createdAt: String,
) {
    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        fun from(entity: AuditLog) =
            AuditLogResponse(
                id = entity.id!!,
                operator = entity.operator,
                action = entity.action.name,
                targetType = entity.targetType.name,
                targetId = entity.targetId,
                payload = entity.payload,
                createdAt = entity.createdAt.format(formatter),
            )
    }
}
