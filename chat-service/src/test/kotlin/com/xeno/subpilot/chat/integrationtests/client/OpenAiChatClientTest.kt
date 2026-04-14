package com.xeno.subpilot.chat.integrationtests.client

import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.xeno.subpilot.chat.client.OpenAiChatClient
import com.xeno.subpilot.chat.exception.OpenAiException
import com.xeno.subpilot.chat.properties.OpenAiProperties
import com.xeno.subpilot.chat.service.ChatTurn
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension

import kotlin.test.assertEquals
import kotlin.test.assertTrue

import kotlinx.coroutines.test.runTest

class OpenAiChatClientTest {

    companion object {
        @RegisterExtension
        @JvmField
        val wireMock: WireMockExtension =
            WireMockExtension
                .newInstance()
                .options(wireMockConfig().dynamicPort())
                .build()

        private const val COMPLETIONS_PATH = "/chat/completions"
        private const val TEST_MODEL = "gpt-4o"

        private fun completionResponse(content: String) =
            """
            {
              "id": "chatcmpl-test",
              "object": "chat.completion",
              "created": 1234567890,
              "model": "$TEST_MODEL",
              "choices": [{
                "index": 0,
                "message": { "role": "assistant", "content": "$content" },
                "finish_reason": "stop"
              }],
              "usage": { "prompt_tokens": 10, "completion_tokens": 5, "total_tokens": 15 }
            }
            """.trimIndent()
    }

    private lateinit var client: OpenAiChatClient

    @BeforeEach
    fun setUp() {
        wireMock.resetAll()
        val openAiClient =
            OpenAIOkHttpClient
                .builder()
                .apiKey("test-key")
                .baseUrl("http://localhost:${wireMock.port}")
                .maxRetries(0)
                .build()
        client =
            OpenAiChatClient(
                openAiClient,
                OpenAiProperties(apiKey = "test-key", maxCompletionTokens = 1000),
            )
    }

    @Test
    fun `chat returns text from completion response`() =
        runTest {
            wireMock.stubFor(
                post(urlPathEqualTo(COMPLETIONS_PATH))
                    .willReturn(okJson(completionResponse("Hello from AI"))),
            )

            val result = client.chat(emptyList(), "Hello", TEST_MODEL)

            assertEquals("Hello from AI", result)
        }

    @Test
    fun `chat sends user message in request body`() =
        runTest {
            wireMock.stubFor(
                post(urlPathEqualTo(COMPLETIONS_PATH))
                    .willReturn(okJson(completionResponse("ok"))),
            )

            client.chat(emptyList(), "user question", TEST_MODEL)

            wireMock.verify(
                postRequestedFor(urlPathEqualTo(COMPLETIONS_PATH))
                    .withRequestBody(
                        matchingJsonPath(
                            "$.messages[?(@.role == 'user' && @.content == 'user question')]",
                        ),
                    ),
            )
        }

    @Test
    fun `chat sends history messages before current user message`() =
        runTest {
            val history =
                listOf(
                    ChatTurn(ChatTurn.Role.USER, "previous question"),
                    ChatTurn(ChatTurn.Role.ASSISTANT, "previous answer"),
                )
            wireMock.stubFor(
                post(urlPathEqualTo(COMPLETIONS_PATH))
                    .willReturn(okJson(completionResponse("ok"))),
            )

            client.chat(history, "new question", TEST_MODEL)

            wireMock.verify(
                postRequestedFor(urlPathEqualTo(COMPLETIONS_PATH))
                    .withRequestBody(
                        matchingJsonPath(
                            "$.messages[?(@.role == 'user' && @.content == 'previous question')]",
                        ),
                    ).withRequestBody(
                        matchingJsonPath(
                            "$.messages[?(@.role == 'assistant' && @.content == 'previous answer')]",
                        ),
                    ),
            )
        }

    @Test
    fun `chat sends correct model in request body`() =
        runTest {
            wireMock.stubFor(
                post(urlPathEqualTo(COMPLETIONS_PATH))
                    .willReturn(okJson(completionResponse("ok"))),
            )

            client.chat(emptyList(), "hello", TEST_MODEL)

            wireMock.verify(
                postRequestedFor(urlPathEqualTo(COMPLETIONS_PATH))
                    .withRequestBody(matchingJsonPath("$.model", equalTo(TEST_MODEL))),
            )
        }

    @Test
    fun `chat throws OpenAiException on server error`() =
        runTest {
            wireMock.stubFor(
                post(urlPathEqualTo(COMPLETIONS_PATH))
                    .willReturn(serverError()),
            )

            assertThrows<OpenAiException> {
                client.chat(emptyList(), "hello", TEST_MODEL)
            }
        }

    @Test
    fun `chat returns empty string when response content is absent`() =
        runTest {
            val emptyContentResponse =
                """
                {
                  "id": "chatcmpl-test",
                  "object": "chat.completion",
                  "created": 1234567890,
                  "model": "$TEST_MODEL",
                  "choices": [{
                    "index": 0,
                    "message": { "role": "assistant" },
                    "finish_reason": "stop"
                  }],
                  "usage": { "prompt_tokens": 5, "completion_tokens": 0, "total_tokens": 5 }
                }
                """.trimIndent()
            wireMock.stubFor(
                post(urlPathEqualTo(COMPLETIONS_PATH))
                    .willReturn(okJson(emptyContentResponse)),
            )

            val result = client.chat(emptyList(), "hello", TEST_MODEL)

            assertTrue(result.isEmpty())
        }
}
