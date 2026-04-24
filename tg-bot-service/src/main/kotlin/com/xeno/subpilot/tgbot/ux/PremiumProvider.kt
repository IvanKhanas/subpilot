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

enum class PremiumProvider(
    val displayName: String,
    val planProviderKey: String,
) {
    OPENAI("֎ OpenAI", "openai"),
    ;

    companion object {
        fun findByDisplayName(text: String) = entries.find { it.displayName == text }

        fun findByKey(key: String) = entries.find { it.planProviderKey == key }
    }
}
