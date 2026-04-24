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
package com.xeno.subpilot.tgbot.command

import com.xeno.subpilot.tgbot.client.SubscriptionClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.ux.BotScreen
import com.xeno.subpilot.tgbot.ux.NavigationService
import com.xeno.subpilot.tgbot.ux.ScreenRenderer
import org.springframework.stereotype.Component

@Component
class StartCommandHandler(
    private val telegramClient: TelegramClient,
    private val navigationService: NavigationService,
    private val screenRenderer: ScreenRenderer,
    private val subscriptionClient: SubscriptionClient,
) : BotCommand {

    override val command = "/start"
    override val description = "Start the bot"

    override suspend fun handle(message: Message) {
        navigationService.clear(message.chat.id)
        registerAndGreet(message)
        screenRenderer.render(message.chat.id, BotScreen.MAIN_MENU)
    }

    internal suspend fun registerAndGreet(message: Message) {
        val chatId = message.chat.id
        val userName = message.from?.firstName ?: DEFAULT_USERNAME
        val result = message.from?.id?.let { subscriptionClient.registerUser(it) }
        val text =
            if (result?.isNew == true) {
                BotResponses.START_NEW_USER_RESPONSE.format(
                    userName,
                    result.freeQuota,
                    result.freeProvider.displayName,
                )
            } else {
                BotResponses.START_ALREADY_REGISTERED_USER_RESPONSE.format(userName)
            }
        telegramClient.sendMessage(chatId = chatId, text = text)
    }

    companion object {
        internal const val DEFAULT_USERNAME = "friend"
    }
}
