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
package com.xeno.subpilot.gateway.unittests.controller

import com.xeno.subpilot.gateway.auth.InternalJwtTokenFactory
import com.xeno.subpilot.gateway.config.GatewayProperties
import com.xeno.subpilot.gateway.controller.GatewayProxyController
import com.xeno.subpilot.gateway.exception.GlobalExceptionHandler
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.core.MethodParameter
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.client.RestTemplate
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

import java.net.URI

@ExtendWith(MockKExtension::class)
class GatewayProxyControllerTest {

    private val tokenFactory = mockk<InternalJwtTokenFactory>()
    private val restTemplate = mockk<RestTemplate>()

    private lateinit var mockMvc: MockMvc
    private lateinit var mockMvcWithJwt: MockMvc
    private lateinit var mockMvcWithJwtNoScope: MockMvc

    @BeforeEach
    fun setUp() {
        val properties = properties(adminRoute())

        mockMvc =
            MockMvcBuilders
                .standaloneSetup(GatewayProxyController(properties, tokenFactory, restTemplate))
                .setControllerAdvice(GlobalExceptionHandler())
                .setCustomArgumentResolvers(FixedJwtResolver(null))
                .build()

        val jwtWithScope =
            Jwt
                .withTokenValue("test-token")
                .header("alg", "none")
                .subject("ivan")
                .claim("scope", "admin.read admin.write")
                .build()

        mockMvcWithJwt =
            MockMvcBuilders
                .standaloneSetup(GatewayProxyController(properties, tokenFactory, restTemplate))
                .setControllerAdvice(GlobalExceptionHandler())
                .setCustomArgumentResolvers(FixedJwtResolver(jwtWithScope))
                .build()

        val jwtNoScope =
            Jwt
                .withTokenValue("test-token")
                .header("alg", "none")
                .subject("ivan")
                .build()

        mockMvcWithJwtNoScope =
            MockMvcBuilders
                .standaloneSetup(GatewayProxyController(properties, tokenFactory, restTemplate))
                .setControllerAdvice(GlobalExceptionHandler())
                .setCustomArgumentResolvers(FixedJwtResolver(jwtNoScope))
                .build()
    }

    @Test
    fun `returns 404 when route not found`() {
        val properties = properties()

        MockMvcBuilders
            .standaloneSetup(GatewayProxyController(properties, tokenFactory, restTemplate))
            .setControllerAdvice(GlobalExceptionHandler())
            .setCustomArgumentResolvers(FixedJwtResolver(null))
            .build()
            .perform(get("/api/v1/unknown"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `proxies request without JWT for public endpoints`() {
        val properties = properties(authRoute(), adminRoute())

        every {
            restTemplate.exchange(any<URI>(), HttpMethod.POST, any(), ByteArray::class.java)
        } returns ResponseEntity.ok(ByteArray(0))

        MockMvcBuilders
            .standaloneSetup(GatewayProxyController(properties, tokenFactory, restTemplate))
            .setControllerAdvice(GlobalExceptionHandler())
            .setCustomArgumentResolvers(FixedJwtResolver(null))
            .build()
            .perform(
                post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"),
            ).andExpect(status().isOk)
    }

    @Test
    fun `proxies request with JWT and injects internal bearer`() {
        every { tokenFactory.issue("ivan", "admin.read admin.write") } returns "internal-token"
        every {
            restTemplate.exchange(any<URI>(), HttpMethod.GET, any(), ByteArray::class.java)
        } returns ResponseEntity.ok(ByteArray(0))

        mockMvcWithJwt
            .perform(get("/api/v1/admin/users"))
            .andExpect(status().isOk)

        verify { tokenFactory.issue("ivan", "admin.read admin.write") }
    }

    @Test
    fun `extracts scope from scp claim when scope claim is absent`() {
        val jwtWithScp =
            Jwt
                .withTokenValue("test-token")
                .header("alg", "none")
                .subject("ivan")
                .claim("scp", listOf("admin.read"))
                .build()

        every { tokenFactory.issue("ivan", "admin.read") } returns "internal-token"
        every {
            restTemplate.exchange(any<URI>(), HttpMethod.GET, any(), ByteArray::class.java)
        } returns ResponseEntity.ok(ByteArray(0))

        MockMvcBuilders
            .standaloneSetup(
                GatewayProxyController(properties(adminRoute()), tokenFactory, restTemplate),
            ).setControllerAdvice(GlobalExceptionHandler())
            .setCustomArgumentResolvers(FixedJwtResolver(jwtWithScp))
            .build()
            .perform(get("/api/v1/admin/users"))
            .andExpect(status().isOk)

        verify { tokenFactory.issue("ivan", "admin.read") }
    }

    @Test
    fun `returns 401 when JWT has neither scope nor scp claim`() {
        mockMvcWithJwtNoScope
            .perform(get("/api/v1/admin/users"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `forwards upstream status code to caller`() {
        every { tokenFactory.issue(any(), any()) } returns "internal-token"
        every {
            restTemplate.exchange(any<URI>(), HttpMethod.GET, any(), ByteArray::class.java)
        } returns ResponseEntity.notFound().build()

        mockMvcWithJwt
            .perform(get("/api/v1/admin/users/999"))
            .andExpect(status().isNotFound)
    }

    private fun adminRoute() =
        GatewayProperties.RouteProperties(
            id = "admin-service",
            pathPrefix = "/api/v1/admin",
            targetBaseUrl = "http://admin-service:8086",
        )

    private fun authRoute() =
        GatewayProperties.RouteProperties(
            id = "admin-auth",
            pathPrefix = "/api/v1/auth",
            targetBaseUrl = "http://admin-service:8086",
        )

    private fun properties(vararg routes: GatewayProperties.RouteProperties) =
        GatewayProperties(
            auth =
                GatewayProperties.AuthProperties(
                    jwt =
                        GatewayProperties.AuthJwtProperties(
                            secret = "secret",
                            issuer = "subpilot-admin",
                            audience = "subpilot-api",
                            accessTtlSeconds = 900,
                            refreshTtlSeconds = 1209600,
                        ),
                ),
            internalJwt =
                GatewayProperties.InternalJwtProperties(
                    secret = "internal-secret",
                    ttlSeconds = 60,
                ),
            routes = routes.toList(),
        )

    private class FixedJwtResolver(
        private val jwt: Jwt?,
    ) : HandlerMethodArgumentResolver {

        override fun supportsParameter(parameter: MethodParameter): Boolean =
            parameter.parameterType == Jwt::class.java &&
                parameter.hasParameterAnnotation(AuthenticationPrincipal::class.java)

        override fun resolveArgument(
            parameter: MethodParameter,
            mavContainer: ModelAndViewContainer?,
            webRequest: NativeWebRequest,
            binderFactory: WebDataBinderFactory?,
        ): Any? = jwt
    }
}
