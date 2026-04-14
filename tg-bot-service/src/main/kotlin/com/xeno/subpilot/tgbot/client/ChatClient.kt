package com.xeno.subpilot.tgbot.client

import com.xeno.subpilot.proto.chat.v1.ProcessMessageResponse

interface ChatClient {
    fun processMessage(
        userId: Long,
        chatId: Long,
        text: String,
    ): ProcessMessageResponse

    fun clearHistory(chatId: Long)
}
