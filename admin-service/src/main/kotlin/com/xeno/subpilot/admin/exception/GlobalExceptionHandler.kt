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
package com.xeno.subpilot.admin.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Status
import io.grpc.StatusException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

private val logger = KotlinLogging.logger {}

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(AuthException::class)
    fun handleAuth(ex: AuthException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(ex.status, ex.message)

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(ex: UserNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "User not found")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ProblemDetail {
        val detail =
            ex.bindingResult.fieldErrors.joinToString(
                "; ",
            ) { "${it.field}: ${it.defaultMessage}" }
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail)
    }

    @ExceptionHandler(StatusException::class)
    fun handleGrpc(ex: StatusException): ProblemDetail {
        logger.atError {
            message = "admin_grpc_call_failed"
            cause = ex
        }
        val httpStatus =
            when (ex.status.code) {
                Status.Code.NOT_FOUND -> HttpStatus.NOT_FOUND
                Status.Code.INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST
                Status.Code.ALREADY_EXISTS -> HttpStatus.CONFLICT
                else -> HttpStatus.BAD_GATEWAY
            }
        return ProblemDetail.forStatusAndDetail(
            httpStatus,
            ex.status.description ?: "Upstream service error",
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ProblemDetail {
        logger.atError {
            message = "admin_unexpected_error"
            cause = ex
        }
        return ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
