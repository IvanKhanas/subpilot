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
package com.xeno.subpilot.chat.client

import com.openai.client.OpenAIClient
import com.openai.models.ChatModel
import com.openai.models.chat.completions.ChatCompletion
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.xeno.subpilot.chat.exception.OpenAiException
import com.xeno.subpilot.chat.properties.OpenAiProperties
import com.xeno.subpilot.chat.service.ChatTurn
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

import kotlin.coroutines.CoroutineContext

import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

@Component
class OpenAiChatClient(
    private val openAiClient: OpenAIClient,
    private val openAiProperties: OpenAiProperties,
    private val ioDispatcher: CoroutineContext,
) {

    suspend fun chat(
        history: List<ChatTurn>,
        userMessage: String,
        model: String,
    ): String =
        withContext(ioDispatcher) {
            logger.atDebug {
                message = "openai_request_sending"
                payload = mapOf("model" to model, "history_size" to history.size)
            }
            val params = buildParams(history, userMessage, model)
            val completion = complete(params, model)
            extractText(completion, model)
        }

    private fun buildParams(
        history: List<ChatTurn>,
        userMessage: String,
        model: String,
    ): ChatCompletionCreateParams =
        ChatCompletionCreateParams
            .builder()
            .model(ChatModel.of(model))
            .maxCompletionTokens(openAiProperties.maxCompletionTokens)
            .apply {
                history.forEach { turn ->
                    when (turn.role) {
                        ChatTurn.Role.USER -> addUserMessage(turn.content)
                        ChatTurn.Role.ASSISTANT -> addAssistantMessage(turn.content)
                    }
                }
                addUserMessage(userMessage)
            }.build()

    private fun complete(
        params: ChatCompletionCreateParams,
        model: String,
    ) = try {
        openAiClient.chat().completions().create(params).also {
            logger.atInfo {
                message = "openai_response_received"
                payload = mapOf("requested_model" to model, "actual_model" to it.model())
            }
        }
    } catch (ex: Exception) {
        throw OpenAiException("OpenAI API call failed", ex)
    }

    private fun extractText(
        completion: ChatCompletion,
        model: String,
    ): String =
        completion
            .choices()
            .first()
            .message()
            .content()
            .orElseGet {
                logger.atWarn {
                    message = "openai_empty_response"
                    payload = mapOf("model" to model)
                }
                ""
            }
}
