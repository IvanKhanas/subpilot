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
