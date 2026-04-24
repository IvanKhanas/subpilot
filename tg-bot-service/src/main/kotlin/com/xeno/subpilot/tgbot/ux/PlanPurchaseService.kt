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

import com.xeno.subpilot.tgbot.client.PaymentClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.exception.PaymentServiceException
import com.xeno.subpilot.tgbot.message.BotResponses
import org.springframework.stereotype.Component

@Component
class PlanPurchaseService(
    private val paymentClient: PaymentClient,
    private val telegramClient: TelegramClient,
) {

    suspend fun startPayment(
        chatId: Long,
        userId: Long,
        planId: String,
        bonusPointsToApply: Long = 0,
    ) {
        val confirmationUrl =
            try {
                paymentClient.createPayment(userId, planId, bonusPointsToApply)
            } catch (ex: PaymentServiceException) {
                telegramClient.sendMessage(chatId, BotResponses.PAYMENT_FAILED_RESPONSE.text)
                return
            }
        telegramClient.sendMessage(
            chatId,
            BotResponses.PAYMENT_LINK_RESPONSE.format(confirmationUrl),
        )
    }
}
