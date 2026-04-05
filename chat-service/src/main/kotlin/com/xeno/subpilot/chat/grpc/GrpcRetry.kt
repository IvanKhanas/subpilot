package com.xeno.subpilot.chat.grpc

import io.grpc.Status
import io.grpc.StatusRuntimeException

import kotlinx.coroutines.delay

private val RETRYABLE_STATUS_CODES =
    setOf(
        Status.Code.UNAVAILABLE,
        Status.Code.RESOURCE_EXHAUSTED,
        Status.Code.DEADLINE_EXCEEDED,
    )

suspend fun <T> retryGrpc(
    attempts: Int,
    delayMs: Long,
    block: suspend () -> T,
): T {
    repeat(attempts - 1) {
        try {
            return block()
        } catch (ex: StatusRuntimeException) {
            if (ex.status.code !in RETRYABLE_STATUS_CODES) throw ex
            delay(delayMs)
        }
    }
    return block()
}
