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

import com.xeno.subpilot.proto.chat.v1.ChatServiceGrpcKt
import com.xeno.subpilot.proto.chat.v1.ProcessMessageResponse
import com.xeno.subpilot.tgbot.client.ChatGrpcClient
import com.xeno.subpilot.tgbot.client.GrpcRetry
import com.xeno.subpilot.tgbot.config.GrpcRetryProperties
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

import kotlinx.coroutines.runBlocking

@ExtendWith(MockKExtension::class)
class ChatGrpcClientTest {

    @MockK(relaxed = true)
    lateinit var stub: ChatServiceGrpcKt.ChatServiceCoroutineStub

    private lateinit var client: ChatGrpcClient

    @BeforeEach
    fun setUp() {
        client =
            ChatGrpcClient(
                stub,
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
    fun `processMessage returns response from gRPC`() {
        coEvery { stub.processMessage(any(), any()) } returns
            ProcessMessageResponse
                .newBuilder()
                .setText("AI response")
                .build()

        val result = runBlocking { client.processMessage(userId = 1L, chatId = 2L, text = "hello") }

        assertEquals("AI response", result.text)
    }

    @Test
    fun `processMessage throws ChatServiceException on StatusException`() {
        coEvery { stub.processMessage(any(), any()) } throws StatusException(Status.UNAVAILABLE)

        assertThrows<ChatServiceException> {
            runBlocking { client.processMessage(userId = 1L, chatId = 2L, text = "hello") }
        }
    }
}
