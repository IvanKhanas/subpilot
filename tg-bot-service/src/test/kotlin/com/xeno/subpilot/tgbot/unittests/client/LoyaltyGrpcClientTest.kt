package com.xeno.subpilot.tgbot.unittests.client

import com.xeno.subpilot.proto.loyalty.v1.LoyaltyServiceGrpcKt
import com.xeno.subpilot.proto.loyalty.v1.SpendDenialReason
import com.xeno.subpilot.proto.loyalty.v1.SpendPointsResponse
import com.xeno.subpilot.proto.loyalty.v1.getBalanceResponse
import com.xeno.subpilot.tgbot.client.GrpcRetry
import com.xeno.subpilot.tgbot.client.LoyaltyGrpcClient
import com.xeno.subpilot.tgbot.config.GrpcRetryProperties
import com.xeno.subpilot.tgbot.dto.SpendResult
import com.xeno.subpilot.tgbot.exception.LoyaltyServiceException
import io.grpc.Status
import io.grpc.StatusException
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

import java.util.UUID

import kotlin.test.assertEquals
import kotlin.test.assertIs

import kotlinx.coroutines.runBlocking

@ExtendWith(MockKExtension::class)
class LoyaltyGrpcClientTest {

    @MockK(relaxed = true)
    lateinit var stub: LoyaltyServiceGrpcKt.LoyaltyServiceCoroutineStub

    private lateinit var client: LoyaltyGrpcClient

    @BeforeEach
    fun setUp() {
        client =
            LoyaltyGrpcClient(
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
    fun `getBalance returns points from grpc response`() {
        coEvery { stub.getBalance(any(), any()) } returns getBalanceResponse { points = 345 }

        val result = runBlocking { client.getBalance(1L) }

        assertEquals(345L, result)
    }

    @Test
    fun `getBalance wraps grpc failure into LoyaltyServiceException`() {
        coEvery { stub.getBalance(any(), any()) } throws StatusException(Status.UNAVAILABLE)

        assertThrows<LoyaltyServiceException> {
            runBlocking { client.getBalance(1L) }
        }
    }

    @Test
    fun `spend returns Success when grpc responds with success`() {
        coEvery { stub.spendPoints(any(), any()) } returns
            SpendPointsResponse.newBuilder().setSuccess(true).build()

        val result = runBlocking { client.spend(1L, "openai-basic", UUID.randomUUID()) }

        assertIs<SpendResult.Success>(result)
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        "SPEND_DENIAL_REASON_INSUFFICIENT_POINTS, INSUFFICIENT_POINTS",
        "SPEND_DENIAL_REASON_UNKNOWN_PLAN, UNKNOWN_PLAN",
        "SPEND_DENIAL_REASON_UNSPECIFIED, UNSPECIFIED",
    )
    fun `spend maps denial reasons to dto`(
        protoReason: SpendDenialReason,
        expectedReasonName: String,
    ) {
        coEvery { stub.spendPoints(any(), any()) } returns
            SpendPointsResponse
                .newBuilder()
                .setSuccess(false)
                .setDenialReason(protoReason)
                .build()

        val result = runBlocking { client.spend(1L, "openai-basic", UUID.randomUUID()) }

        val denied = assertIs<SpendResult.Denied>(result)
        assertEquals(expectedReasonName, denied.reason.name)
    }

    @Test
    fun `spend wraps grpc status exception`() {
        coEvery { stub.spendPoints(any(), any()) } throws StatusException(Status.UNAVAILABLE)

        assertThrows<LoyaltyServiceException> {
            runBlocking { client.spend(1L, "openai-basic", UUID.randomUUID()) }
        }
    }
}
