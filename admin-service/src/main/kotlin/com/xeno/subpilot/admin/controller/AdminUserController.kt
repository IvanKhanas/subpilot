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

import com.xeno.subpilot.admin.dto.AddSubscriptionRequest
import com.xeno.subpilot.admin.dto.AdjustLoyaltyRequest
import com.xeno.subpilot.admin.dto.UserInfoResponse
import com.xeno.subpilot.admin.service.AdminUserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/users")
class AdminUserController(
    private val adminUserService: AdminUserService,
) {

    @GetMapping("/{userId}")
    suspend fun getUser(
        @PathVariable userId: Long,
    ): UserInfoResponse = adminUserService.getUserInfo(userId)

    @PostMapping("/{userId}/ban")
    suspend fun banUser(
        @PathVariable userId: Long,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Unit> {
        adminUserService.banUser(operator = jwt.subject, userId = userId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{userId}/unban")
    suspend fun unbanUser(
        @PathVariable userId: Long,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Unit> {
        adminUserService.unbanUser(operator = jwt.subject, userId = userId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{userId}/subscription")
    suspend fun addSubscription(
        @PathVariable userId: Long,
        @Valid @RequestBody request: AddSubscriptionRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Unit> {
        adminUserService.addSubscription(operator = jwt.subject, userId = userId, request = request)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{userId}/loyalty/adjust")
    suspend fun adjustLoyalty(
        @PathVariable userId: Long,
        @Valid @RequestBody request: AdjustLoyaltyRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Unit> {
        adminUserService.adjustLoyalty(operator = jwt.subject, userId = userId, request = request)
        return ResponseEntity.ok().build()
    }
}
