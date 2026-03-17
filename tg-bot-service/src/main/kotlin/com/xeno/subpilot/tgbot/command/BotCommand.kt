package com.xeno.subpilot.tgbot.command

import com.xeno.subpilot.tgbot.dto.Message

interface BotCommand {
    val command: String
    val description: String

    fun handle(message: Message)
}
