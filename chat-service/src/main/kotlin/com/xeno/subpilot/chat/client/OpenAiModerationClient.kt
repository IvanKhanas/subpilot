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
import com.openai.models.moderations.Moderation
import com.openai.models.moderations.ModerationCreateParams
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

import kotlin.coroutines.CoroutineContext
import kotlin.jvm.optionals.getOrNull

import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

@Component
class OpenAiModerationClient(
    private val openAiClient: OpenAIClient,
    private val ioDispatcher: CoroutineContext,
) {

    suspend fun flaggedCategories(text: String): List<String> =
        withContext(ioDispatcher) {
            val response =
                openAiClient
                    .moderations()
                    .create(ModerationCreateParams.builder().input(text).build())
            val result = response.results().firstOrNull() ?: return@withContext emptyList()
            if (!result.flagged()) return@withContext emptyList<String>()
            result.categories().toFlaggedList()
        }

    private fun Moderation.Categories.toFlaggedList(): List<String> =
        listOfNotNull(
            "harassment".takeIf { harassment() },
            "harassment/threatening".takeIf { harassmentThreatening() },
            "hate".takeIf { hate() },
            "hate/threatening".takeIf { hateThreatening() },
            "illicit".takeIf { illicit().getOrNull() == true },
            "illicit/violent".takeIf { illicitViolent().getOrNull() == true },
            "self-harm".takeIf { selfHarm() },
            "self-harm/instructions".takeIf { selfHarmInstructions() },
            "self-harm/intent".takeIf { selfHarmIntent() },
            "sexual".takeIf { sexual() },
            "sexual/minors".takeIf { sexualMinors() },
            "violence".takeIf { violence() },
            "violence/graphic".takeIf { violenceGraphic() },
        )
}
