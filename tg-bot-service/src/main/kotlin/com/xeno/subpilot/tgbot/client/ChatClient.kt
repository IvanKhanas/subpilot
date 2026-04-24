package com.xeno.subpilot.tgbot.client

import com.xeno.subpilot.proto.chat.v1.ProcessMessageResponse

interface ChatClient {
    suspend fun processMessage(
        userId: Long,
        chatId: Long,
        text: String,
    ): ProcessMessageResponse

    suspend fun clearContext(chatId: Long)
}
