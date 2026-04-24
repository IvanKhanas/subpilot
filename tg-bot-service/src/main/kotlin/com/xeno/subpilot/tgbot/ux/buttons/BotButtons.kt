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
package com.xeno.subpilot.tgbot.ux.buttons

import com.xeno.subpilot.tgbot.dto.InlineKeyboardButton
import com.xeno.subpilot.tgbot.dto.InlineKeyboardMarkup
import com.xeno.subpilot.tgbot.dto.KeyboardButton
import com.xeno.subpilot.tgbot.dto.PlanInfo
import com.xeno.subpilot.tgbot.dto.ReplyKeyboardMarkup
import com.xeno.subpilot.tgbot.ux.AiProvider
import com.xeno.subpilot.tgbot.ux.PremiumProvider

object BotButtons {

    const val BTN_START_CHAT = "🚀 Start chat"
    const val BTN_CHOOSE_MODEL = "🤖 Choose model"
    const val CLEAR_CONTEXT = "🧹 Clear context"
    const val PREMIUM = "🤩 Premium"
    const val BALANCE = "👛 Balance"
    const val BONUS = "🎁 Bonus"
    const val BTN_HELP = "ℹ️ Help"
    const val SUPPORT = "💬 Support"

    const val BTN_BACK = "⏪ Back"
    const val BTN_MAIN_MENU = "🏠 Main menu"

    val mainMenu =
        ReplyKeyboardMarkup(
            keyboard =
                listOf(
                    listOf(KeyboardButton(BTN_START_CHAT)),
                    listOf(KeyboardButton(BTN_CHOOSE_MODEL), KeyboardButton(CLEAR_CONTEXT)),
                    listOf(KeyboardButton(PREMIUM), KeyboardButton(BALANCE)),
                    listOf(KeyboardButton(BONUS), KeyboardButton(BTN_HELP)),
                    listOf(KeyboardButton(SUPPORT)),
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

    val premiumProviderMenu =
        ReplyKeyboardMarkup(
            keyboard =
                listOf(
                    PremiumProvider.entries.map { KeyboardButton(it.displayName) },
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

    fun planPurchaseKeyboard(plans: List<PlanInfo>) =
        InlineKeyboardMarkup(
            inlineKeyboard =
                plans.map { plan ->
                    listOf(
                        InlineKeyboardButton(
                            text = "Purchase ${plan.displayName}",
                            callbackData = "purchase:${plan.planId}",
                        ),
                    )
                },
        )

    fun bonusConfirmKeyboard(
        planId: String,
        idempotencyKey: java.util.UUID,
    ) = InlineKeyboardMarkup(
        inlineKeyboard =
            listOf(
                listOf(
                    InlineKeyboardButton(
                        text = "Yes",
                        callbackData = "bonus_yes:$planId:$idempotencyKey",
                    ),
                    InlineKeyboardButton(text = "No", callbackData = "bonus_no:$planId"),
                ),
            ),
    )
}
