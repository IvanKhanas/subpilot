package com.xeno.subpilot.chat.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Status
import io.grpc.StatusException
import org.springframework.grpc.server.exception.GrpcExceptionHandler
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class ChatGrpcExceptionHandler : GrpcExceptionHandler {

    override fun handleException(e: Throwable): StatusException =
        when (e) {
            is ChatException -> {
                logger.atError {
                    message = "grpc_chat_exception"
                    cause = e
                }
                e.status
                    .withDescription(e.message)
                    .withCause(e)
                    .asException()
            }
            else -> {
                logger.atError {
                    message = "grpc_unhandled_exception"
                    cause = e
                }
                Status.INTERNAL
                    .withDescription(e.message)
                    .withCause(e)
                    .asException()
            }
        }
}
