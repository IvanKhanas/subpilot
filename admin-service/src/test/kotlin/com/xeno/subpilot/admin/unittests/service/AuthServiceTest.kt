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
package com.xeno.subpilot.admin.unittests.service

import com.xeno.subpilot.admin.auth.AdminAccount
import com.xeno.subpilot.admin.auth.AdminAccountRepository
import com.xeno.subpilot.admin.auth.AuthJwtTokenFactory
import com.xeno.subpilot.admin.auth.AuthService
import com.xeno.subpilot.admin.auth.TokenPair
import com.xeno.subpilot.admin.exception.AuthException
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException

import java.time.Instant

@ExtendWith(MockKExtension::class)
class AuthServiceTest {

    private val repository = mockk<AdminAccountRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val tokenFactory = mockk<AuthJwtTokenFactory>()
    private val refreshJwtDecoder = mockk<JwtDecoder>()

    private val authService =
        AuthService(repository, passwordEncoder, tokenFactory, refreshJwtDecoder)

    private val tokenPair =
        TokenPair(accessToken = "access", refreshToken = "refresh", expiresIn = 900)

    @Test
    fun `login returns tokens for valid credentials`() {
        every { repository.findByUsername("ivan") } returns account(enabled = true)
        every { passwordEncoder.matches("pass", "hash") } returns true
        every { tokenFactory.issueTokens("ivan", "admin.read") } returns tokenPair

        val result = authService.login("ivan", "pass")

        assertEquals(tokenPair, result)
    }

    @Test
    fun `login trims whitespace from username`() {
        every { repository.findByUsername("ivan") } returns account(enabled = true)
        every { passwordEncoder.matches("pass", "hash") } returns true
        every { tokenFactory.issueTokens("ivan", "admin.read") } returns tokenPair

        val result = authService.login("  ivan  ", "pass")

        assertEquals(tokenPair, result)
    }

    @Test
    fun `login throws 401 when account not found`() {
        every { repository.findByUsername(any()) } returns null

        val ex = assertThrows<AuthException> { authService.login("unknown", "pass") }
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status)
    }

    @Test
    fun `login throws 403 when account is disabled`() {
        every { repository.findByUsername("ivan") } returns account(enabled = false)

        val ex = assertThrows<AuthException> { authService.login("ivan", "pass") }
        assertEquals(HttpStatus.FORBIDDEN, ex.status)
    }

    @Test
    fun `login throws 401 when password does not match`() {
        every { repository.findByUsername("ivan") } returns account(enabled = true)
        every { passwordEncoder.matches("wrong", "hash") } returns false

        val ex = assertThrows<AuthException> { authService.login("ivan", "wrong") }
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status)
    }

    @Test
    fun `refresh returns new tokens for valid refresh token`() {
        val jwt =
            Jwt
                .withTokenValue("refresh")
                .header("alg", "none")
                .subject("ivan")
                .build()
        every { refreshJwtDecoder.decode("refresh") } returns jwt
        every { repository.findByUsername("ivan") } returns account(enabled = true)
        every { tokenFactory.issueTokens("ivan", "admin.read") } returns tokenPair

        val result = authService.refresh("refresh")

        assertEquals(tokenPair, result)
    }

    @Test
    fun `refresh throws 401 when refresh token is invalid`() {
        every { refreshJwtDecoder.decode(any()) } throws JwtException("bad token")

        val ex = assertThrows<AuthException> { authService.refresh("bad-token") }
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status)
    }

    @Test
    fun `refresh throws 401 when account not found after token decode`() {
        val jwt =
            Jwt
                .withTokenValue("refresh")
                .header("alg", "none")
                .subject("ghost")
                .build()
        every { refreshJwtDecoder.decode("refresh") } returns jwt
        every { repository.findByUsername("ghost") } returns null

        val ex = assertThrows<AuthException> { authService.refresh("refresh") }
        assertEquals(HttpStatus.UNAUTHORIZED, ex.status)
    }

    @Test
    fun `refresh throws 403 when account is disabled`() {
        val jwt =
            Jwt
                .withTokenValue("refresh")
                .header("alg", "none")
                .subject("ivan")
                .build()
        every { refreshJwtDecoder.decode("refresh") } returns jwt
        every { repository.findByUsername("ivan") } returns account(enabled = false)

        val ex = assertThrows<AuthException> { authService.refresh("refresh") }
        assertEquals(HttpStatus.FORBIDDEN, ex.status)
    }

    private fun account(enabled: Boolean) =
        AdminAccount(
            id = 1L,
            username = "ivan",
            passwordHash = "hash",
            scopes = "admin.read",
            enabled = enabled,
            createdAt = Instant.now(),
        )
}
