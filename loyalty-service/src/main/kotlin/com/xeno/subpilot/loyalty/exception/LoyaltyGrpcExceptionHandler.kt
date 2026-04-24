package com.xeno.subpilot.loyalty.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Status
import io.grpc.StatusException
import org.springframework.grpc.server.exception.GrpcExceptionHandler
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class LoyaltyGrpcExceptionHandler : GrpcExceptionHandler {

    override fun handleException(e: Throwable): StatusException =
        when (e) {
            is LoyaltyException -> {
                logger.atError {
                    message = "grpc_loyalty_exception"
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
