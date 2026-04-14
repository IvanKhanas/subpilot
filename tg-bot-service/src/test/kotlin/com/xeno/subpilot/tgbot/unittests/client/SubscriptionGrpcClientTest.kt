package com.xeno.subpilot.tgbot.unittests.client

import com.xeno.subpilot.proto.subscription.v1.RegisterUserResponse
import com.xeno.subpilot.proto.subscription.v1.SetModelPreferenceResponse
import com.xeno.subpilot.proto.subscription.v1.SubscriptionServiceGrpcKt
import com.xeno.subpilot.tgbot.client.GrpcRetry
import com.xeno.subpilot.tgbot.client.SubscriptionGrpcClient
import com.xeno.subpilot.tgbot.config.GrpcRetryProperties
import com.xeno.subpilot.tgbot.exception.SubscriptionServiceException
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    fun `registerUser returns RegistrationResult for new user`() {
        coEvery { stub.registerUser(any(), any()) } returns
            RegisterUserResponse
                .newBuilder()
                .setIsNew(true)
                .setFreeQuota(10)
                .setFreeModelId("gpt-4o-mini")
                .build()

        val result = client.registerUser(1L)

        assertTrue(result!!.isNew)
        assertEquals(10, result.freeQuota)
    }

    @Test
    fun `registerUser returns null on StatusException`() {
        coEvery { stub.registerUser(any(), any()) } throws StatusException(Status.UNAVAILABLE)

        val result = client.registerUser(1L)

        assertNull(result)
    }

    @Test
    fun `setModelPreference returns providerChanged from stub`() {
        coEvery { stub.setModelPreference(any(), any()) } returns
            SetModelPreferenceResponse.newBuilder().setProviderChanged(true).build()

        val result = client.setModelPreference(1L, "gpt-4o")

        assertTrue(result)
    }

    @Test
    fun `setModelPreference throws SubscriptionServiceException on StatusException`() {
        coEvery { stub.setModelPreference(any(), any()) } throws StatusException(Status.UNAVAILABLE)

        assertThrows<SubscriptionServiceException> {
            client.setModelPreference(1L, "gpt-4o")
        }
    }
}
