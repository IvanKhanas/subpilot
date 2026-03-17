package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.dto.InlineKeyboardButton
import com.xeno.subpilot.tgbot.dto.InlineKeyboardMarkup
import com.xeno.subpilot.tgbot.dto.KeyboardButton
import com.xeno.subpilot.tgbot.dto.ReplyKeyboardMarkup

object BotButtons {

    const val START_CHAT = "start_chat"
    const val HELP = "help"

    const val BTN_CHAT = "Start chat"
    const val BTN_HELP = "Help"

    val startChatButton = InlineKeyboardButton(text = "Start chat", callbackData = START_CHAT)
    val helpButton = InlineKeyboardButton(text = "Help", callbackData = HELP)

    val welcomeInlineKeyboard =
        InlineKeyboardMarkup(
            inlineKeyboard =
                listOf(
                    listOf(startChatButton),
                    listOf(helpButton),
                ),
        )

    val mainMenu =
        ReplyKeyboardMarkup(
            keyboard =
                listOf(
                    listOf(KeyboardButton(text = BTN_CHAT)),
                    listOf(KeyboardButton(text = BTN_HELP)),
                ),
        )
}
