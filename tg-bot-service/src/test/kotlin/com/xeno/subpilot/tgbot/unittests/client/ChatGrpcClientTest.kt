package com.xeno.subpilot.tgbot.unittests.client

import com.xeno.subpilot.proto.chat.v1.ChatServiceGrpcKt
import com.xeno.subpilot.proto.chat.v1.ProcessMessageResponse
import com.xeno.subpilot.proto.chat.v1.SetModelResponse
import com.xeno.subpilot.tgbot.client.ChatGrpcClient
import com.xeno.subpilot.tgbot.exception.ChatServiceException
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

@ExtendWith(MockKExtension::class)
class ChatGrpcClientTest {

    @MockK(relaxed = true)
    lateinit var stub: ChatServiceGrpcKt.ChatServiceCoroutineStub

    private lateinit var client: ChatGrpcClient

    @BeforeEach
    fun setUp() {
        client = ChatGrpcClient(stub)
    }

    @Test
    fun `processMessage returns text from gRPC response`() {
        coEvery { stub.processMessage(any(), any()) } returns
            ProcessMessageResponse
                .newBuilder()
                .setText("AI response")
                .build()

        val result = client.processMessage(userId = 1L, chatId = 2L, text = "hello")

        assertEquals("AI response", result)
    }

    @Test
    fun `processMessage throws ChatServiceException on StatusException`() {
        coEvery { stub.processMessage(any(), any()) } throws StatusException(Status.UNAVAILABLE)

        assertThrows<ChatServiceException> {
            client.processMessage(userId = 1L, chatId = 2L, text = "hello")
        }
    }

    @Test
    fun `setModel completes without exception on success`() {
        coEvery { stub.setModel(any(), any()) } returns SetModelResponse.newBuilder().build()

        client.setModel(chatId = 1L, model = "gpt-4o")
    }

    @Test
    fun `setModel throws ChatServiceException on StatusException`() {
        coEvery { stub.setModel(any(), any()) } throws StatusException(Status.UNAVAILABLE)

        assertThrows<ChatServiceException> {
            client.setModel(chatId = 1L, model = "gpt-4o")
        }
    }
}
