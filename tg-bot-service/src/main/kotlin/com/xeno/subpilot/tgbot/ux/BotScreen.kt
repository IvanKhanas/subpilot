/*
 * Copyright 2024 Ivan Khanas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    PREMIUM_MENU(
        BotResponses.CHOOSE_PREMIUM_PROVIDER_RESPONSE.text,
        BotButtons.premiumProviderMenu,
    ),
}
