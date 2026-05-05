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
package com.xeno.subpilot.admin.unittests.client

import com.xeno.subpilot.admin.client.GrpcRetry
import com.xeno.subpilot.admin.client.PaymentAdminGrpcClient
import com.xeno.subpilot.admin.config.GrpcRetryProperties
import com.xeno.subpilot.proto.payment.v1.PaymentServiceGrpcKt
import com.xeno.subpilot.proto.payment.v1.TriggerOutboxFlushResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import kotlin.test.assertEquals

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class PaymentAdminGrpcClientTest {

    @MockK
    lateinit var stub: PaymentServiceGrpcKt.PaymentServiceCoroutineStub

    private lateinit var client: PaymentAdminGrpcClient

    companion object {
        const val FLUSHED_COUNT = 14
    }

    private val grpcRetry =
        GrpcRetry(
            GrpcRetryProperties(maxAttempts = 1, initialBackoffMs = 0, backoffMultiplier = 1.0),
        )

    @BeforeEach
    fun setUp() {
        client = PaymentAdminGrpcClient(stub, grpcRetry)
    }

    @Test
    fun `triggerOutboxFlush returns grpc flushed count`() =
        runTest {
            coEvery { stub.triggerOutboxFlush(any(), any()) } returns
                TriggerOutboxFlushResponse
                    .newBuilder()
                    .setFlushedCount(FLUSHED_COUNT)
                    .build()

            val result = client.triggerOutboxFlush()

            assertEquals(FLUSHED_COUNT, result)
            coVerify(exactly = 1) { stub.triggerOutboxFlush(any(), any()) }
        }
}
