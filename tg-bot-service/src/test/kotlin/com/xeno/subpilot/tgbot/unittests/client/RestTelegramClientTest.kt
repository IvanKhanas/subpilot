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

import com.xeno.subpilot.tgbot.client.RestTelegramClient
import com.xeno.subpilot.tgbot.dto.AnswerCallbackQueryRequest
import com.xeno.subpilot.tgbot.dto.BotCommandInfo
import com.xeno.subpilot.tgbot.dto.DeleteMessageRequest
import com.xeno.subpilot.tgbot.dto.EditMessageTextRequest
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.ReplyKeyboardMarkup
import com.xeno.subpilot.tgbot.dto.SendMessageRequest
import com.xeno.subpilot.tgbot.dto.SetMyCommandsRequest
import com.xeno.subpilot.tgbot.dto.TelegramResponse
import com.xeno.subpilot.tgbot.dto.Update
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

import java.util.stream.Stream

@ExtendWith(MockKExtension::class)
class RestTelegramClientTest {

    @MockK
    lateinit var restClient: RestClient

    @MockK
    lateinit var requestBodyUriSpec: RestClient.RequestBodyUriSpec

    @MockK
    lateinit var requestBodySpec: RestClient.RequestBodySpec

    @MockK
    lateinit var responseSpec: RestClient.ResponseSpec

    private lateinit var client: RestTelegramClient

    private enum class ApiFailureCase {
        SEND_MESSAGE,
        ANSWER_CALLBACK,
        SET_MY_COMMANDS,
        EDIT_MESSAGE,
        DELETE_MESSAGE,
    }

    companion object {
        const val BOOM_MESSAGE = "boom"
        const val CHAT_ID = 99L
        const val MESSAGE_ID = 7L
        const val UPDATE_OFFSET = 5
        const val UPDATE_TIMEOUT = 30
        const val TEXT_HELLO = "hello"
        const val TEXT_UPDATED = "updated"
        const val CALLBACK_ID = "callback-id"

        @JvmStatic
        fun apiFailureCases(): Stream<Arguments> =
            Stream.of(
                Arguments.of(ApiFailureCase.SEND_MESSAGE),
                Arguments.of(ApiFailureCase.ANSWER_CALLBACK),
                Arguments.of(ApiFailureCase.SET_MY_COMMANDS),
                Arguments.of(ApiFailureCase.EDIT_MESSAGE),
                Arguments.of(ApiFailureCase.DELETE_MESSAGE),
            )
    }

    @BeforeEach
    fun setUp() {
        client = RestTelegramClient(restClient)

        every { restClient.post() } returns requestBodyUriSpec
        every { requestBodyUriSpec.uri(any<String>()) } returns requestBodySpec
        every { requestBodySpec.contentType(MediaType.APPLICATION_JSON) } returns requestBodySpec
        every { requestBodySpec.body(any<Map<String, Any>>()) } returns requestBodySpec
        every { requestBodySpec.body(any<SendMessageRequest>()) } returns requestBodySpec
        every { requestBodySpec.body(any<AnswerCallbackQueryRequest>()) } returns requestBodySpec
        every { requestBodySpec.body(any<SetMyCommandsRequest>()) } returns requestBodySpec
        every { requestBodySpec.body(any<EditMessageTextRequest>()) } returns requestBodySpec
        every { requestBodySpec.body(any<DeleteMessageRequest>()) } returns requestBodySpec
        every { requestBodySpec.retrieve() } returns responseSpec
        every { responseSpec.toBodilessEntity() } returns ResponseEntity.ok().build()
        every {
            responseSpec.body(any<ParameterizedTypeReference<TelegramResponse<Message>>>())
        } returns null
    }

    @Test
    fun `getUpdates returns update list when telegram response ok`() {
        val updates = listOf(Update(updateId = 10))
        val bodySlot: CapturingSlot<Map<String, Any>> = slot()
        every {
            responseSpec.body(any<ParameterizedTypeReference<TelegramResponse<List<Update>>>>())
        } returns TelegramResponse(ok = true, result = updates)
        every { requestBodySpec.body(capture(bodySlot)) } returns requestBodySpec

        val result = client.getUpdates(offset = UPDATE_OFFSET, timeout = UPDATE_TIMEOUT)

        assertEquals(updates, result)
        assertEquals(UPDATE_OFFSET.toLong(), bodySlot.captured["offset"])
        assertEquals(UPDATE_TIMEOUT, bodySlot.captured["timeout"])
        assertEquals(listOf("message", "callback_query"), bodySlot.captured["allowed_updates"])
        verify { requestBodyUriSpec.uri("/getUpdates") }
    }

    @Test
    fun `getUpdates returns empty when response is not ok`() {
        every {
            responseSpec.body(any<ParameterizedTypeReference<TelegramResponse<List<Update>>>>())
        } returns TelegramResponse(ok = false, result = listOf(Update(updateId = 1)))

        val result = client.getUpdates(offset = null, timeout = 10)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getUpdates returns empty when api call throws exception`() {
        every { restClient.post() } throws RestClientException("boom")

        val result = client.getUpdates(offset = 1, timeout = 10)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `sendMessage posts payload to sendMessage endpoint`() {
        val bodySlot: CapturingSlot<SendMessageRequest> = slot()
        every { requestBodySpec.body(capture(bodySlot)) } returns requestBodySpec

        val markup = ReplyKeyboardMarkup(emptyList())
        client.sendMessage(chatId = CHAT_ID, text = TEXT_HELLO, replyMarkup = markup)

        assertEquals(CHAT_ID, bodySlot.captured.chatId)
        assertEquals(TEXT_HELLO, bodySlot.captured.text)
        assertEquals(markup, bodySlot.captured.replyMarkup)
        verify { requestBodyUriSpec.uri("/sendMessage") }
    }

    @Test
    fun `answerCallbackQuery calls telegram endpoint`() {
        client.answerCallbackQuery(CALLBACK_ID)

        verify { requestBodyUriSpec.uri("/answerCallbackQuery") }
        verify { requestBodySpec.retrieve() }
    }

    @Test
    fun `setMyCommands posts command list`() {
        val commands = listOf(BotCommandInfo(command = "start", description = "Start"))

        client.setMyCommands(commands)

        verify { requestBodyUriSpec.uri("/setMyCommands") }
        verify { requestBodySpec.retrieve() }
    }

    @ParameterizedTest(name = "case={0}")
    @MethodSource("apiFailureCases")
    fun `api methods do not throw when telegram api call fails`(failureCase: ApiFailureCase) {
        every { requestBodySpec.retrieve() } throws RestClientException(BOOM_MESSAGE)

        when (failureCase) {
            ApiFailureCase.SEND_MESSAGE ->
                client.sendMessage(chatId = CHAT_ID, text = TEXT_HELLO)
            ApiFailureCase.ANSWER_CALLBACK ->
                client.answerCallbackQuery(CALLBACK_ID)
            ApiFailureCase.SET_MY_COMMANDS ->
                client.setMyCommands(listOf(BotCommandInfo(command = "help", description = "Help")))
            ApiFailureCase.EDIT_MESSAGE ->
                client.editMessage(chatId = CHAT_ID, messageId = MESSAGE_ID, text = TEXT_UPDATED)
            ApiFailureCase.DELETE_MESSAGE ->
                client.deleteMessage(chatId = CHAT_ID, messageId = MESSAGE_ID)
        }
    }

    @Test
    fun `editMessage posts payload to editMessageText endpoint`() {
        val bodySlot: CapturingSlot<EditMessageTextRequest> = slot()
        every { requestBodySpec.body(capture(bodySlot)) } returns requestBodySpec

        client.editMessage(chatId = 42, messageId = MESSAGE_ID, text = TEXT_UPDATED)

        assertEquals(42L, bodySlot.captured.chatId)
        assertEquals(MESSAGE_ID, bodySlot.captured.messageId)
        assertEquals(TEXT_UPDATED, bodySlot.captured.text)
        verify { requestBodyUriSpec.uri("/editMessageText") }
    }

    @Test
    fun `deleteMessage posts payload to deleteMessage endpoint`() {
        val bodySlot: CapturingSlot<DeleteMessageRequest> = slot()
        every { requestBodySpec.body(capture(bodySlot)) } returns requestBodySpec

        client.deleteMessage(chatId = 55, messageId = 3)

        assertEquals(55L, bodySlot.captured.chatId)
        assertEquals(3L, bodySlot.captured.messageId)
        verify { requestBodyUriSpec.uri("/deleteMessage") }
    }
}
