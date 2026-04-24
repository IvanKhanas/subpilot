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

import com.xeno.subpilot.tgbot.client.ChatClient
import com.xeno.subpilot.tgbot.client.SubscriptionClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.exception.SubscriptionServiceException
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.ux.AiProvider
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(11)
class SelectModelTextButtonHandler(
    private val telegramClient: TelegramClient,
    private val subscriptionClient: SubscriptionClient,
    private val chatClient: ChatClient,
) : TextButtonHandler {

    override fun supports(text: String) = AiProvider.findModelByDisplayName(text) != null

    override suspend fun handle(message: Message) {
        val userId = message.from?.id ?: return
        val model = AiProvider.findModelByDisplayName(message.text ?: return) ?: return
        try {
            val result = subscriptionClient.setModelPreference(userId, model.id)
            if (result.providerChanged) {
                chatClient.clearContext(message.chat.id)
            }
            val providerName = AiProvider.displayNameByKey(result.provider)
            telegramClient.sendMessage(
                message.chat.id,
                BotResponses.MODEL_SET_RESPONSE.format(
                    model.displayName,
                    model.displayName,
                    result.modelCost,
                    providerName,
                ),
                BotButtons.mainMenu,
            )
        } catch (ex: SubscriptionServiceException) {
            telegramClient.sendMessage(
                message.chat.id,
                BotResponses.MODEL_SET_FAILED_RESPONSE.text,
                BotButtons.mainMenu,
            )
        }
    }
}
