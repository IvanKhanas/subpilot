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
