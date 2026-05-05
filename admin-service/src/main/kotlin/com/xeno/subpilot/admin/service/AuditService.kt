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
package com.xeno.subpilot.admin.service

import com.xeno.subpilot.admin.entity.AdminAction
import com.xeno.subpilot.admin.entity.AdminTargetType
import com.xeno.subpilot.admin.entity.AuditLog
import com.xeno.subpilot.admin.repository.AuditLogRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuditService(
    private val auditLogRepository: AuditLogRepository,
) {

    @Transactional
    fun record(
        operator: String,
        action: AdminAction,
        targetType: AdminTargetType,
        targetId: String? = null,
        payload: String? = null,
    ) {
        auditLogRepository.save(
            AuditLog(
                operator = operator,
                action = action,
                targetType = targetType,
                targetId = targetId,
                payload = payload,
            ),
        )
    }

    fun getPage(pageable: Pageable): Page<AuditLog> =
        auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
}
