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
package com.xeno.subpilot.admin.unittests.controller

import com.xeno.subpilot.admin.auth.AuthService
import com.xeno.subpilot.admin.auth.TokenPair
import com.xeno.subpilot.admin.controller.AuthController
import com.xeno.subpilot.admin.exception.AuthException
import com.xeno.subpilot.admin.exception.GlobalExceptionHandler
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

@ExtendWith(MockKExtension::class)
class AuthControllerTest {

    private val authService = mockk<AuthService>()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val validator = LocalValidatorFactoryBean().apply { afterPropertiesSet() }
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(AuthController(authService))
                .setControllerAdvice(GlobalExceptionHandler())
                .setValidator(validator)
                .build()
    }

    @Test
    fun `POST login returns tokens for valid credentials`() {
        val tokenPair = TokenPair(accessToken = "access", refreshToken = "refresh", expiresIn = 900)
        every { authService.login("ivan", "pass") } returns tokenPair

        mockMvc
            .perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"username":"ivan","password":"pass"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("access"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
    }

    @Test
    fun `POST refresh returns new tokens`() {
        val tokenPair =
            TokenPair(accessToken = "new-access", refreshToken = "new-refresh", expiresIn = 900)
        every { authService.refresh("old-refresh") } returns tokenPair

        mockMvc
            .perform(
                post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"refreshToken":"old-refresh"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("new-access"))
    }

    @Test
    fun `POST login returns 401 when credentials are invalid`() {
        every { authService.login(any(), any()) } throws
            AuthException(HttpStatus.UNAUTHORIZED, "invalid_credentials")

        mockMvc
            .perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"username":"ivan","password":"wrong"}"""),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST login returns 400 for blank username`() {
        mockMvc
            .perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"username":"","password":"pass"}"""),
            ).andExpect(status().isBadRequest)
    }
}
