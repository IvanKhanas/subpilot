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
package com.xeno.subpilot.tgbot.grpc

import com.xeno.subpilot.proto.moderation.v1.FlaggedPromptNotification
import com.xeno.subpilot.proto.moderation.v1.ModerationServiceGrpcKt
import com.xeno.subpilot.proto.moderation.v1.NotifyResponse
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.properties.ModerationProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.grpc.server.service.GrpcService

import kotlin.coroutines.CoroutineContext

import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

private const val MAX_PROMPT_PREVIEW_LENGTH = 200

@GrpcService
class ModerationGrpcService(
    private val telegramClient: TelegramClient,
    private val properties: ModerationProperties,
    private val ioDispatcher: CoroutineContext,
) : ModerationServiceGrpcKt.ModerationServiceCoroutineImplBase() {

    override suspend fun notifyFlaggedPrompt(request: FlaggedPromptNotification): NotifyResponse {
        logger.atInfo {
            message = "moderation_flagged_prompt"
            payload =
                mapOf(
                    "user_id" to request.userId,
                    "categories" to request.flaggedCategoriesList,
                )
        }
        withContext(ioDispatcher) {
            telegramClient.sendMessage(properties.chatId, buildMessage(request))
        }
        return NotifyResponse.getDefaultInstance()
    }

    private fun buildMessage(request: FlaggedPromptNotification): String {
        val preview =
            request.promptText.take(MAX_PROMPT_PREVIEW_LENGTH).let {
                if (request.promptText.length > MAX_PROMPT_PREVIEW_LENGTH) "$it…" else it
            }
        val categories = request.flaggedCategoriesList.joinToString(", ")
        return """
            ⚠️ Flagged prompt detected
            User: ${request.userId}  Chat: ${request.chatId}
            Categories: $categories
            Text: "$preview"
            """.trimIndent()
    }
}
