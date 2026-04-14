package com.xeno.subpilot.tgbot.ux

import com.xeno.subpilot.tgbot.dto.ReplyMarkup
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.ux.buttons.BotButtons

enum class BotScreen(
    val responseText: String,
    val replyMarkup: ReplyMarkup,
) {
    MAIN_MENU(BotResponses.MAIN_MENU_RESPONSE.text, BotButtons.mainMenu),
    PROVIDER_MENU(BotResponses.CHOOSE_PROVIDER_RESPONSE.text, BotButtons.providerMenu),
}
