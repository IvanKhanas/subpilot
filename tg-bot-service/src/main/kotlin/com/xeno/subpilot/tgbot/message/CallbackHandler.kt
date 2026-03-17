package com.xeno.subpilot.tgbot.message

import com.xeno.subpilot.tgbot.dto.CallbackQuery

interface CallbackHandler {
    fun handle(callbackQuery: CallbackQuery)
}
