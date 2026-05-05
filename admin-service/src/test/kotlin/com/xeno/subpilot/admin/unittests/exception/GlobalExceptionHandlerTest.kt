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
package com.xeno.subpilot.admin.unittests.exception

import com.xeno.subpilot.admin.dto.AdjustLoyaltyRequest
import com.xeno.subpilot.admin.exception.GlobalExceptionHandler
import com.xeno.subpilot.admin.exception.UserNotFoundException
import com.xeno.subpilot.admin.unittests.AdminTestFixtures
import io.grpc.Status
import io.grpc.StatusException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException

import java.util.stream.Stream

import kotlin.test.assertEquals

class GlobalExceptionHandlerTest {

    private lateinit var handler: GlobalExceptionHandler

    companion object {
        const val GRPC_DESCRIPTION = "upstream failed"
        const val VALIDATION_OBJECT_NAME = "request"
        const val REASON_FIELD = "reason"
        const val KEY_FIELD = "idempotencyKey"
        const val REASON_ERROR = "must not be blank"
        const val KEY_ERROR = "must not be blank"

        private val validationMethod =
            ValidationMethodSource::class.java.getDeclaredMethod(
                "validationMethod",
                AdjustLoyaltyRequest::class.java,
            )
        private val validationParameter = MethodParameter(validationMethod, 0)

        @JvmStatic
        fun grpcStatusCases(): Stream<Arguments> =
            Stream.of(
                arguments(Status.NOT_FOUND, HttpStatus.NOT_FOUND),
                arguments(Status.INVALID_ARGUMENT, HttpStatus.BAD_REQUEST),
                arguments(Status.ALREADY_EXISTS, HttpStatus.CONFLICT),
                arguments(Status.UNAVAILABLE, HttpStatus.BAD_GATEWAY),
            )
    }

    @BeforeEach
    fun setUp() {
        handler = GlobalExceptionHandler()
    }

    @Test
    fun `handleUserNotFound returns NOT_FOUND with user message`() {
        val problem = handler.handleUserNotFound(UserNotFoundException(AdminTestFixtures.USER_ID))

        assertEquals(HttpStatus.NOT_FOUND.value(), problem.status)
        assertEquals("User ${AdminTestFixtures.USER_ID} not found", problem.detail)
    }

    @ParameterizedTest(name = "{0} maps to {1}")
    @MethodSource("grpcStatusCases")
    fun `handleGrpc maps grpc status to expected http status`(
        status: Status,
        expectedHttpStatus: HttpStatus,
    ) {
        val exception = status.withDescription(GRPC_DESCRIPTION).asException()

        val problem = handler.handleGrpc(exception)

        assertEquals(expectedHttpStatus.value(), problem.status)
        assertEquals(GRPC_DESCRIPTION, problem.detail)
    }

    @Test
    fun `handleGrpc uses default detail when grpc description is missing`() {
        val exception: StatusException = Status.INTERNAL.asException()

        val problem = handler.handleGrpc(exception)

        assertEquals(HttpStatus.BAD_GATEWAY.value(), problem.status)
        assertEquals("Upstream service error", problem.detail)
    }

    @Test
    fun `handleValidation joins all field errors in one detail string`() {
        val bindingResult =
            BeanPropertyBindingResult(
                AdjustLoyaltyRequest(
                    delta = 1L,
                    reason = "",
                    idempotencyKey = AdminTestFixtures.IDEMPOTENCY_KEY,
                ),
                VALIDATION_OBJECT_NAME,
            )
        bindingResult.addError(FieldError(VALIDATION_OBJECT_NAME, REASON_FIELD, REASON_ERROR))
        bindingResult.addError(FieldError(VALIDATION_OBJECT_NAME, KEY_FIELD, KEY_ERROR))
        val exception = MethodArgumentNotValidException(validationParameter, bindingResult)

        val problem = handler.handleValidation(exception)

        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.status)
        assertEquals("$REASON_FIELD: $REASON_ERROR; $KEY_FIELD: $KEY_ERROR", problem.detail)
    }

    @Test
    fun `handleUnexpected returns INTERNAL_SERVER_ERROR`() {
        val problem = handler.handleUnexpected(IllegalStateException("unexpected"))

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problem.status)
    }

    class ValidationMethodSource {
        @Suppress("unused")
        fun validationMethod(request: AdjustLoyaltyRequest) {
            request.copy()
        }
    }
}
