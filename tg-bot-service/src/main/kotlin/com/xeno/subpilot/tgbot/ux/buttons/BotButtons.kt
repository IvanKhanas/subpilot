package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.dto.KeyboardButton
import com.xeno.subpilot.tgbot.dto.ReplyKeyboardMarkup
import com.xeno.subpilot.tgbot.ux.AiProvider

object BotButtons {

    const val BTN_START_CHAT = "\uD83D\uDE80Start chat"
    const val BTN_CHOOSE_MODEL = "\uD83E\uDD16Choose model"
    const val BTN_HELP = "ℹ\uFE0FHelp"
    const val BTN_BACK = "⏪Back"
    const val BTN_MAIN_MENU = "\uD83C\uDFE0Main menu"

    val mainMenu =
        ReplyKeyboardMarkup(
            keyboard =
                listOf(
                    listOf(KeyboardButton(BTN_START_CHAT)),
                    listOf(KeyboardButton(BTN_CHOOSE_MODEL), KeyboardButton(BTN_HELP)),
                ),
        )

    val providerMenu =
        ReplyKeyboardMarkup(
            keyboard =
                listOf(
                    AiProvider.entries.map { KeyboardButton(it.displayName) },
                    listOf(KeyboardButton(BTN_BACK), KeyboardButton(BTN_MAIN_MENU)),
                ),
        )

    fun modelMenu(provider: AiProvider) =
        ReplyKeyboardMarkup(
            keyboard =
                listOf(
                    provider.models.map { KeyboardButton(it.displayName) },
                    listOf(KeyboardButton(BTN_BACK), KeyboardButton(BTN_MAIN_MENU)),
                ),
        )
}
