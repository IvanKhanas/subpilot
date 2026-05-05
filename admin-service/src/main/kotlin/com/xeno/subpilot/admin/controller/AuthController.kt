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

import com.xeno.subpilot.admin.auth.AuthService
import com.xeno.subpilot.admin.dto.AuthTokensResponse
import com.xeno.subpilot.admin.dto.LoginRequest
import com.xeno.subpilot.admin.dto.RefreshRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
) {

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
    ): AuthTokensResponse {
        val tokenPair = authService.login(request.username, request.password)
        return AuthTokensResponse.from(tokenPair)
    }

    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshRequest,
    ): AuthTokensResponse {
        val tokenPair = authService.refresh(request.refreshToken)
        return AuthTokensResponse.from(tokenPair)
    }
}
