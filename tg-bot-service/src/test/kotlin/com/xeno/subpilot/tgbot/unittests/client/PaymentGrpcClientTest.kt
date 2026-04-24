/*
 * Copyright 2024 Ivan Khanas
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
package com.xeno.subpilot.tgbot.unittests.client

import com.xeno.subpilot.proto.payment.v1.CreatePaymentResponse
import com.xeno.subpilot.proto.payment.v1.PaymentServiceGrpcKt
import com.xeno.subpilot.tgbot.client.GrpcRetry
import com.xeno.subpilot.tgbot.client.PaymentGrpcClient
import com.xeno.subpilot.tgbot.config.GrpcRetryProperties
import com.xeno.subpilot.tgbot.exception.PaymentServiceException
import io.grpc.Status
import io.grpc.StatusException
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

import kotlin.test.assertEquals

import kotlinx.coroutines.runBlocking

@ExtendWith(MockKExtension::class)
class PaymentGrpcClientTest {

    @MockK(relaxed = true)
    lateinit var stub: PaymentServiceGrpcKt.PaymentServiceCoroutineStub

    private lateinit var client: PaymentGrpcClient

    @BeforeEach
    fun setUp() {
        client =
            PaymentGrpcClient(
                stub = stub,
                grpcRetry =
                    GrpcRetry(
                        GrpcRetryProperties(
                            maxAttempts = 1,
                            initialBackoffMs = 0,
                            backoffMultiplier = 1.0,
                        ),
                    ),
            )
    }

    @Test
    fun `createPayment returns confirmation URL from gRPC response`() {
        coEvery { stub.createPayment(any(), any()) } returns
            CreatePaymentResponse
                .newBuilder()
                .setPaymentId("payment-1")
                .setConfirmationUrl("https://pay.example/confirm")
                .build()

        val result =
            runBlocking {
                client.createPayment(
                    userId = 1L,
                    planId = "openai-basic",
                    bonusPointsToApply = 50,
                )
            }

        assertEquals("https://pay.example/confirm", result)
    }

    @Test
    fun `createPayment wraps StatusException into PaymentServiceException`() {
        coEvery { stub.createPayment(any(), any()) } throws StatusException(Status.UNAVAILABLE)

        assertThrows<PaymentServiceException> {
            runBlocking {
                client.createPayment(
                    userId = 1L,
                    planId = "openai-basic",
                    bonusPointsToApply = 0,
                )
            }
        }
    }
}
