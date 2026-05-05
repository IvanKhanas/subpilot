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
package com.xeno.subpilot.admin.controller

import com.xeno.subpilot.admin.dto.CreatePlanRequest
import com.xeno.subpilot.admin.service.AdminSubscriptionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/subscription")
class AdminSubscriptionController(
    private val adminSubscriptionService: AdminSubscriptionService,
) {

    @PostMapping("/plans")
    suspend fun createPlan(
        @Valid @RequestBody request: CreatePlanRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Unit> {
        adminSubscriptionService.createPlan(operator = jwt.subject, request = request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
