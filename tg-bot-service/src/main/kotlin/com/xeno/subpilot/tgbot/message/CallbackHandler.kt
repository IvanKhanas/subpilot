package com.xeno.subpilot.tgbot.message

import com.xeno.subpilot.tgbot.dto.CallbackQuery

interface CallbackHandler {
    fun supports(data: String): Boolean

    fun handle(callbackQuery: CallbackQuery)
}
