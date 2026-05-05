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
package com.xeno.subpilot.admin.auth

import com.xeno.subpilot.admin.exception.AuthException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val adminAccountRepository: AdminAccountRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authJwtTokenFactory: AuthJwtTokenFactory,

    @Qualifier("refreshJwtDecoder")
    private val refreshJwtDecoder: JwtDecoder,
) {

    fun login(
        username: String,
        password: String,
    ): TokenPair {
        val normalizedUsername = username.trim()

        val account =
            adminAccountRepository.findByUsername(normalizedUsername)
                ?: throw AuthException(HttpStatus.UNAUTHORIZED, "invalid_credentials")

        if (!account.enabled) {
            throw AuthException(HttpStatus.FORBIDDEN, "admin_account_disabled")
        }

        if (!passwordEncoder.matches(password, account.passwordHash)) {
            throw AuthException(HttpStatus.UNAUTHORIZED, "invalid_credentials")
        }

        return authJwtTokenFactory.issueTokens(account.username, account.scopes)
    }

    fun refresh(refreshToken: String): TokenPair {
        val jwt =
            try {
                refreshJwtDecoder.decode(refreshToken)
            } catch (_: JwtException) {
                throw AuthException(HttpStatus.UNAUTHORIZED, "refresh_token_invalid")
            }

        val subject =
            jwt.subject?.takeIf { it.isNotBlank() }
                ?: throw AuthException(HttpStatus.UNAUTHORIZED, "refresh_token_invalid")

        val account =
            adminAccountRepository.findByUsername(subject)
                ?: throw AuthException(HttpStatus.UNAUTHORIZED, "refresh_token_invalid")

        if (!account.enabled) {
            throw AuthException(HttpStatus.FORBIDDEN, "admin_account_disabled")
        }

        return authJwtTokenFactory.issueTokens(account.username, account.scopes)
    }
}
