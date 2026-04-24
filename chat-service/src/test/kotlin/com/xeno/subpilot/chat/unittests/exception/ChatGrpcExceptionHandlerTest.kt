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
package com.xeno.subpilot.chat.unittests.exception

import com.xeno.subpilot.chat.exception.ChatException
import com.xeno.subpilot.chat.exception.ChatGrpcExceptionHandler
import com.xeno.subpilot.chat.exception.OpenAiException
import io.grpc.Status
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import kotlin.test.assertEquals

class ChatGrpcExceptionHandlerTest {

    private lateinit var handler: ChatGrpcExceptionHandler

    @BeforeEach
    fun setUp() {
        handler = ChatGrpcExceptionHandler()
    }

    @Test
    fun `handleException returns UNAVAILABLE status for OpenAiException`() {
        val result = handler.handleException(OpenAiException("openai failed", RuntimeException()))

        assertEquals(Status.Code.UNAVAILABLE, result.status.code)
    }

    @Test
    fun `handleException uses the status from ChatException`() {
        val result = handler.handleException(ChatException(Status.NOT_FOUND, "not found"))

        assertEquals(Status.Code.NOT_FOUND, result.status.code)
    }

    @Test
    fun `handleException returns INTERNAL for unknown exception`() {
        val result = handler.handleException(RuntimeException("unexpected"))

        assertEquals(Status.Code.INTERNAL, result.status.code)
    }
}
