package com.xeno.subpilot.tgbot.message

import com.xeno.subpilot.tgbot.dto.Message

interface MessageHandler {
    suspend fun handle(message: Message)
}
