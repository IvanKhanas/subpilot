package com.xeno.subpilot.tgbot.runtime

import com.xeno.subpilot.tgbot.dto.Update

fun interface TelegramUpdateHandler {
    suspend fun onUpdate(update: Update)
}
