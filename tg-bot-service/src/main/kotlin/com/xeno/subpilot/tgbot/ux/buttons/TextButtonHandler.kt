package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.dto.Message

interface TextButtonHandler {
    fun supports(text: String): Boolean

    fun handle(message: Message)
}
