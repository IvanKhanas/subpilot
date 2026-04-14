package com.xeno.subpilot.tgbot.client

import com.xeno.subpilot.tgbot.config.GrpcRetryProperties
import io.grpc.Status
import io.grpc.StatusException
import org.springframework.stereotype.Component

import kotlinx.coroutines.delay

@Component
class GrpcRetry(
    private val props: GrpcRetryProperties,
) {

    suspend fun <T> retryOnUnavailable(block: suspend () -> T): T {
        var backoffMs = props.initialBackoffMs
        repeat(props.maxAttempts - 1) {
            try {
                return block()
            } catch (ex: StatusException) {
                if (ex.status.code != Status.Code.UNAVAILABLE) throw ex
            }
            delay(backoffMs)
            backoffMs = (backoffMs * props.backoffMultiplier).toLong()
        }
        return block()
    }
}
