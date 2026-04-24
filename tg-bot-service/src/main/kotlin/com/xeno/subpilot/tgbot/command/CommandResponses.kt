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
package com.xeno.subpilot.tgbot.command

enum class CommandResponses(
    val text: String,
) {

    START_RESPONSE(
        """
        Hey, %s! I'm SubPilot — your AI assistant.

        Just send me a message and I'll reply!
        """.trimIndent(),
    ),

    HELP_RESPONSE(
        """
        Available commands:
        /start — start the bot
        /help — show this message

        Just send a message to start an AI chat.
        """.trimIndent(),
    ),
    ;

    fun format(vararg args: Any): String = text.format(*args)
}
