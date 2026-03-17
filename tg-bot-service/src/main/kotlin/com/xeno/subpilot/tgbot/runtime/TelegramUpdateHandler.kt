package com.xeno.subpilot.tgbot.runtime

import com.xeno.subpilot.tgbot.dto.Update

fun interface TelegramUpdateHandler {
    fun onUpdate(update: Update)
}
