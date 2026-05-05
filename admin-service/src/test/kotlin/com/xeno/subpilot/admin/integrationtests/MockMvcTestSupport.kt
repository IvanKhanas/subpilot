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
package com.xeno.subpilot.admin.integrationtests

import com.xeno.subpilot.admin.exception.GlobalExceptionHandler
import org.springframework.core.MethodParameter
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

object MockMvcTestSupport {

    fun buildMockMvc(
        operator: String,
        vararg controllers: Any,
    ): MockMvc {
        val validator = LocalValidatorFactoryBean().apply { afterPropertiesSet() }
        val jwt =
            Jwt
                .withTokenValue("test-token")
                .header("alg", "none")
                .subject(operator)
                .build()
        return MockMvcBuilders
            .standaloneSetup(*controllers)
            .setControllerAdvice(GlobalExceptionHandler())
            .setCustomArgumentResolvers(
                FixedJwtArgumentResolver(jwt),
                PageableHandlerMethodArgumentResolver(),
            ).setValidator(validator)
            .build()
    }

    fun performSuspend(
        mockMvc: MockMvc,
        requestBuilder: RequestBuilder,
    ): ResultActions {
        val initialResult =
            mockMvc
                .perform(requestBuilder)
                .andExpect(request().asyncStarted())
                .andReturn()
        return mockMvc.perform(asyncDispatch(initialResult))
    }

    private class FixedJwtArgumentResolver(
        private val jwt: Jwt,
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
