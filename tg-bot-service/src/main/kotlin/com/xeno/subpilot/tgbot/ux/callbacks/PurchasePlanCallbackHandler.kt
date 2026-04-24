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
package com.xeno.subpilot.tgbot.ux.callbacks

import com.xeno.subpilot.tgbot.dto.CallbackQuery
import com.xeno.subpilot.tgbot.message.CallbackHandler
import com.xeno.subpilot.tgbot.ux.BonusPurchaseService
import org.springframework.stereotype.Component

@Component
class PurchasePlanCallbackHandler(
    private val bonusPurchaseService: BonusPurchaseService,
) : CallbackHandler {

    override fun supports(data: String) = data.startsWith(CALLBACK_PREFIX)

    override suspend fun handle(callbackQuery: CallbackQuery) {
        val chatId = callbackQuery.message?.chat?.id ?: return
        val userId = callbackQuery.from?.id ?: return
        val planId = callbackQuery.data?.removePrefix(CALLBACK_PREFIX) ?: return
        bonusPurchaseService.startBonusPurchase(chatId, userId, planId)
    }

    companion object {
        const val CALLBACK_PREFIX = "purchase:"
    }
}
