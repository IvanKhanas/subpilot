package com.xeno.subpilot.chat.unittests.client

import com.xeno.subpilot.chat.client.GrpcRetry
import com.xeno.subpilot.chat.client.SubscriptionGrpcClient
import com.xeno.subpilot.chat.config.GrpcRetryProperties
import com.xeno.subpilot.chat.exception.SubscriptionServiceException
import com.xeno.subpilot.proto.subscription.v1.CheckAccessResponse
import com.xeno.subpilot.proto.subscription.v1.GetModelPreferenceResponse
import com.xeno.subpilot.proto.subscription.v1.RefundAccessResponse
import com.xeno.subpilot.proto.subscription.v1.SubscriptionServiceGrpcKt
import io.grpc.Status
import io.grpc.StatusException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

import kotlin.test.assertEquals
import kotlin.test.assertTrue

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class SubscriptionGrpcClientTest {

    @MockK(relaxed = true)
    lateinit var stub: SubscriptionServiceGrpcKt.SubscriptionServiceCoroutineStub

    private lateinit var client: SubscriptionGrpcClient

    @BeforeEach
    fun setUp() {
        client = SubscriptionGrpcClient(stub, GrpcRetry(GrpcRetryProperties(maxAttempts = 1)))
    }

    @Test
    fun `checkAccess returns response from stub`() =
        runTest {
            val response = CheckAccessResponse.newBuilder().setAllowed(true).build()
            coEvery { stub.checkAccess(any(), any()) } returns response

            val result = client.checkAccess(userId = 1L, modelId = "gpt-4o")

            assertTrue(result.allowed)
        }

    @Test
    fun `checkAccess throws SubscriptionServiceException on StatusException`() =
        runTest {
            coEvery { stub.checkAccess(any(), any()) } throws StatusException(Status.UNAVAILABLE)

            assertThrows<SubscriptionServiceException> {
                client.checkAccess(userId = 1L, modelId = "gpt-4o")
            }
        }

    @Test
    fun `getModelPreference returns modelId from stub`() =
        runTest {
            coEvery { stub.getModelPreference(any(), any()) } returns
                GetModelPreferenceResponse.newBuilder().setModelId("gpt-4o-mini").build()

            val result = client.getModelPreference(userId = 1L)

            assertEquals("gpt-4o-mini", result)
        }

    @Test
    fun `getModelPreference throws SubscriptionServiceException on StatusException`() =
        runTest {
            coEvery { stub.getModelPreference(any(), any()) } throws
                StatusException(Status.UNAVAILABLE)

            assertThrows<SubscriptionServiceException> {
                client.getModelPreference(userId = 1L)
            }
        }

    @Test
    fun `refundAccess delegates to stub`() =
        runTest {
            coEvery { stub.refundAccess(any(), any()) } returns
                RefundAccessResponse.getDefaultInstance()

            client.refundAccess(userId = 1L, modelId = "gpt-4o", freeConsumed = 1, paidConsumed = 2)

            coVerify { stub.refundAccess(any(), any()) }
        }

    @Test
    fun `refundAccess swallows StatusException`() =
        runTest {
            coEvery { stub.refundAccess(any(), any()) } throws StatusException(Status.UNAVAILABLE)

            client.refundAccess(userId = 1L, modelId = "gpt-4o", freeConsumed = 1, paidConsumed = 2)
        }
}
