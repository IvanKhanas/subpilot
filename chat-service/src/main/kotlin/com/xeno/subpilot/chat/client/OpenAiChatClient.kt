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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

@Component
class OpenAiChatClient(
    private val openAiClient: OpenAIClient,
    private val openAiProperties: OpenAiProperties,
) {

    suspend fun chat(
        history: List<ChatTurn>,
        userMessage: String,
        model: String,
    ): String =
        withContext(Dispatchers.IO) {
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
