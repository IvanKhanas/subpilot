package com.xeno.subpilot.tgbot.client

interface ChatClient {
    fun processMessage(
        userId: Long,
        chatId: Long,
        text: String,
    ): String

    fun setModel(
        chatId: Long,
        model: String,
    )
}
