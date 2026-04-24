package com.xeno.subpilot.tgbot.message

import com.xeno.subpilot.tgbot.dto.CallbackQuery

interface CallbackHandler {
    fun supports(data: String): Boolean

    suspend fun handle(callbackQuery: CallbackQuery)
}
