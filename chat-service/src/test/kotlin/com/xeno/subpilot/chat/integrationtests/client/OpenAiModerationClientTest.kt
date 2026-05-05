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
package com.xeno.subpilot.chat.integrationtests.client

import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.xeno.subpilot.chat.client.OpenAiModerationClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

import kotlin.test.assertEquals
import kotlin.test.assertTrue

import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class OpenAiModerationClientTest {

    companion object {
        @RegisterExtension
        @JvmField
        val wireMock: WireMockExtension =
            WireMockExtension
                .newInstance()
                .options(wireMockConfig().dynamicPort())
                .build()

        private const val MODERATIONS_PATH = "/moderations"
        private const val INPUT_TEXT = "moderate this text"
        private val EXPECTED_CATEGORIES = listOf("harassment", "violence/graphic")

        private val flaggedResponse =
            """
            {
              "id": "modr-test",
              "model": "omni-moderation-latest",
              "results": [
                {
                  "flagged": true,
                  "categories": {
                    "harassment": true,
                    "harassment/threatening": false,
                    "hate": false,
                    "hate/threatening": false,
                    "illicit": false,
                    "illicit/violent": false,
                    "self-harm": false,
                    "self-harm/instructions": false,
                    "self-harm/intent": false,
                    "sexual": false,
                    "sexual/minors": false,
                    "violence": false,
                    "violence/graphic": true
                  },
                  "category_scores": {
                    "harassment": 0.9,
                    "harassment/threatening": 0.0,
                    "hate": 0.0,
                    "hate/threatening": 0.0,
                    "illicit": 0.0,
                    "illicit/violent": 0.0,
                    "self-harm": 0.0,
                    "self-harm/instructions": 0.0,
                    "self-harm/intent": 0.0,
                    "sexual": 0.0,
                    "sexual/minors": 0.0,
                    "violence": 0.1,
                    "violence/graphic": 0.8
                  },
                  "category_applied_input_types": {
                    "harassment": ["text"],
                    "harassment/threatening": ["text"],
                    "hate": ["text"],
                    "hate/threatening": ["text"],
                    "illicit": ["text"],
                    "illicit/violent": ["text"],
                    "self-harm": ["text"],
                    "self-harm/instructions": ["text"],
                    "self-harm/intent": ["text"],
                    "sexual": ["text"],
                    "sexual/minors": ["text"],
                    "violence": ["text"],
                    "violence/graphic": ["text"]
                  }
                }
              ]
            }
            """.trimIndent()

        private val cleanResponse =
            """
            {
              "id": "modr-test",
              "model": "omni-moderation-latest",
              "results": [
                {
                  "flagged": false,
                  "categories": {
                    "harassment": false,
                    "harassment/threatening": false,
                    "hate": false,
                    "hate/threatening": false,
                    "illicit": false,
                    "illicit/violent": false,
                    "self-harm": false,
                    "self-harm/instructions": false,
                    "self-harm/intent": false,
                    "sexual": false,
                    "sexual/minors": false,
                    "violence": false,
                    "violence/graphic": false
                  },
                  "category_scores": {
                    "harassment": 0.0,
                    "harassment/threatening": 0.0,
                    "hate": 0.0,
                    "hate/threatening": 0.0,
                    "illicit": 0.0,
                    "illicit/violent": 0.0,
                    "self-harm": 0.0,
                    "self-harm/instructions": 0.0,
                    "self-harm/intent": 0.0,
                    "sexual": 0.0,
                    "sexual/minors": 0.0,
                    "violence": 0.0,
                    "violence/graphic": 0.0
                  },
                  "category_applied_input_types": {
                    "harassment": ["text"],
                    "harassment/threatening": ["text"],
                    "hate": ["text"],
                    "hate/threatening": ["text"],
                    "illicit": ["text"],
                    "illicit/violent": ["text"],
                    "self-harm": ["text"],
                    "self-harm/instructions": ["text"],
                    "self-harm/intent": ["text"],
                    "sexual": ["text"],
                    "sexual/minors": ["text"],
                    "violence": ["text"],
                    "violence/graphic": ["text"]
                  }
                }
              ]
            }
            """.trimIndent()
    }

    private lateinit var client: OpenAiModerationClient

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
            OpenAiModerationClient(
                openAiClient,
                UnconfinedTestDispatcher(),
            )
    }

    @Test
    fun `flaggedCategories returns only flagged categories in stable order`() =
        runTest {
            wireMock.stubFor(
                post(urlPathEqualTo(MODERATIONS_PATH))
                    .willReturn(okJson(flaggedResponse)),
            )

            val categories = client.flaggedCategories(INPUT_TEXT)

            assertEquals(EXPECTED_CATEGORIES, categories)
        }

    @Test
    fun `flaggedCategories returns empty list when prompt is not flagged`() =
        runTest {
            wireMock.stubFor(
                post(urlPathEqualTo(MODERATIONS_PATH))
                    .willReturn(okJson(cleanResponse)),
            )

            val categories = client.flaggedCategories(INPUT_TEXT)

            assertTrue(categories.isEmpty())
        }

    @Test
    fun `flaggedCategories sends input text to moderation endpoint`() =
        runTest {
            wireMock.stubFor(
                post(urlPathEqualTo(MODERATIONS_PATH))
                    .willReturn(okJson(cleanResponse)),
            )

            client.flaggedCategories(INPUT_TEXT)

            wireMock.verify(
                postRequestedFor(urlPathEqualTo(MODERATIONS_PATH))
                    .withRequestBody(matchingJsonPath("$.input", equalTo(INPUT_TEXT))),
            )
        }
}
