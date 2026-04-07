package com.xeno.subpilot.chat.unittests.grpc

import com.xeno.subpilot.chat.grpc.retryGrpc
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

import kotlin.test.assertEquals

import kotlinx.coroutines.test.runTest

class GrpcRetryTest {

    @Test
    fun `retryGrpc returns result on first successful attempt`() =
        runTest {
            val result = retryGrpc(attempts = 3, delayMs = 0) { "ok" }

            assertEquals("ok", result)
        }

    @ParameterizedTest
    @EnumSource(
        value = Status.Code::class,
        names = ["UNAVAILABLE", "RESOURCE_EXHAUSTED", "DEADLINE_EXCEEDED"],
    )
    fun `retryGrpc retries on retryable status and returns result on second attempt`(
        code: Status.Code,
    ) = runTest {
        var attempts = 0
        val result =
            retryGrpc(attempts = 3, delayMs = 0) {
                if (++attempts == 1) throw StatusRuntimeException(Status.fromCode(code))
                "ok"
            }

        assertEquals("ok", result)
        assertEquals(2, attempts)
    }

    @Test
    fun `retryGrpc throws immediately on non-retryable status`() =
        runTest {
            var attempts = 0

            assertThrows<StatusRuntimeException> {
                retryGrpc(attempts = 3, delayMs = 0) {
                    attempts++
                    throw StatusRuntimeException(Status.NOT_FOUND)
                }
            }

            assertEquals(1, attempts)
        }

    @Test
    fun `retryGrpc throws after all attempts exhausted`() =
        runTest {
            var attempts = 0

            assertThrows<StatusRuntimeException> {
                retryGrpc(attempts = 3, delayMs = 0) {
                    attempts++
                    throw StatusRuntimeException(Status.UNAVAILABLE)
                }
            }

            assertEquals(3, attempts)
        }
}
