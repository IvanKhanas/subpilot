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
package com.xeno.subpilot.tgbot.dto

data class TelegramResponse<T>(
    val ok: Boolean = false,
    val result: T? = null,
)

data class Update(
    val updateId: Long = 0,
    val message: Message? = null,
    val callbackQuery: CallbackQuery? = null,
)

data class Message(
    val messageId: Long = 0,
    val chat: Chat = Chat(),
    val text: String? = null,
    val from: User? = null,
)

data class Chat(
    val id: Long = 0,
)

data class User(
    val id: Long = 0,
    val firstName: String? = null,
)

data class CallbackQuery(
    val id: String = "",
    val from: User? = null,
    val message: Message? = null,
    val data: String? = null,
)
