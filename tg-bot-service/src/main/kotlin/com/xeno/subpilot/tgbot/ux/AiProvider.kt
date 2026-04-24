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
package com.xeno.subpilot.tgbot.ux

enum class AiProvider(
    val displayName: String,
    val providerKey: String,
    val models: List<AiModel>,
) {
    OPENAI(
        "֎ OpenAI",
        "openai",
        listOf(
            AiModel("gpt-4o", "GPT-4o"),
            AiModel("gpt-4o-mini", "GPT-4o mini"),
            AiModel("gpt-4-turbo", "GPT-4 Turbo"),
        ),
    ),
    ;

    companion object {
        private val byDisplayName = entries.associateBy { it.displayName }
        private val modelByDisplayName =
            entries
                .flatMap {
                    it.models
                }.associateBy { it.displayName }
        private val modelById = entries.flatMap { it.models }.associateBy { it.id }
        private val providerByModelId = entries.flatMap { p -> p.models.map { it.id to p } }.toMap()

        fun findByDisplayName(name: String): AiProvider? = byDisplayName[name]

        fun findModelByDisplayName(name: String): AiModel? = modelByDisplayName[name]

        fun findModelById(id: String): AiModel? = modelById[id]

        fun findProviderByModelId(id: String): AiProvider? = providerByModelId[id]

        fun displayNameByKey(key: String): String =
            entries.find { it.providerKey == key }?.displayName
                ?: key.replaceFirstChar { it.uppercase() }
    }
}

data class AiModel(
    val id: String,
    val displayName: String,
)
