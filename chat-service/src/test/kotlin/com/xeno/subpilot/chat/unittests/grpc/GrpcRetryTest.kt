package com.xeno.subpilot.chat.unittests.grpc

import com.xeno.subpilot.chat.client.GrpcRetry
import com.xeno.subpilot.chat.config.GrpcRetryProperties
import io.grpc.Status
import io.grpc.StatusException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

import kotlin.test.assertEquals

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class GrpcRetryTest {

    private val retry =
        GrpcRetry(
            GrpcRetryProperties(maxAttempts = 3, initialBackoffMs = 200, backoffMultiplier = 3.0),
        )

    @Test
    fun `returns result on first successful attempt`() =
        runTest {
            val result = retry.retryOnUnavailable { "ok" }

            assertEquals("ok", result)
        }

    @Test
    fun `retries on UNAVAILABLE and returns result on second attempt`() =
        runTest {
            var attempts = 0
            val result =
                retry.retryOnUnavailable {
                    if (++attempts == 1) throw StatusException(Status.UNAVAILABLE)
                    "ok"
                }

            assertEquals("ok", result)
            assertEquals(2, attempts)
        }

    @ParameterizedTest
    @EnumSource(
        value = Status.Code::class,
        names = ["NOT_FOUND", "PERMISSION_DENIED", "INTERNAL", "ALREADY_EXISTS"],
    )
    fun `throws immediately on non-UNAVAILABLE status`(code: Status.Code) =
        runTest {
            var attempts = 0

            assertThrows<StatusException> {
                retry.retryOnUnavailable {
                    attempts++
                    throw StatusException(Status.fromCode(code))
                }
            }

            assertEquals(1, attempts)
        }

    @Test
    fun `exhausts all attempts and rethrows last exception`() =
        runTest {
            var attempts = 0

            assertThrows<StatusException> {
                retry.retryOnUnavailable {
                    attempts++
                    throw StatusException(Status.UNAVAILABLE)
                }
            }

            assertEquals(3, attempts)
        }

    @Test
    fun `applies exponential backoff between attempts`() =
        runTest {
            assertThrows<StatusException> {
                retry.retryOnUnavailable { throw StatusException(Status.UNAVAILABLE) }
            }

            assertEquals(800, testScheduler.currentTime)
        }
}
