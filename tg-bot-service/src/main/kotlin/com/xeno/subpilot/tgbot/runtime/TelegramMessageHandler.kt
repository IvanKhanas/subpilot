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
package com.xeno.subpilot.tgbot.runtime

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.command.BotCommand
import com.xeno.subpilot.tgbot.dto.CallbackQuery
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.Update
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.message.CallbackHandler
import com.xeno.subpilot.tgbot.message.MessageHandler
import com.xeno.subpilot.tgbot.ux.buttons.TextButtonHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class TelegramMessageHandler(
    botCommands: List<BotCommand>,
    private val callbackHandlers: List<CallbackHandler>,
    private val textButtonHandlers: List<TextButtonHandler>,
    private val messageHandler: MessageHandler,
    private val telegramClient: TelegramClient,
) : TelegramUpdateHandler {

    private val commandHandlers = botCommands.associateBy { it.command }

    override suspend fun onUpdate(update: Update) {
        when {
            update.callbackQuery != null -> handleCallback(update.callbackQuery)
            update.message?.text != null -> handleMessage(update.message)
        }
    }

    private suspend fun handleCallback(callback: CallbackQuery) {
        val data = callback.data ?: return
        logger.atDebug {
            message = "telegram_callback_received"
            payload = mapOf("user_id" to callback.from?.id, "data" to data)
        }

        val handler = callbackHandlers.find { it.supports(data) }
        if (handler != null) {
            handler.handle(callback)
        } else {
            logger.atDebug {
                message = "telegram_callback_unhandled"
                payload = mapOf("data" to data)
            }
        }

        telegramClient.answerCallbackQuery(callback.id)
    }

    private suspend fun handleMessage(message: Message) {
        val text = message.text ?: return
        when {
            text.startsWith("/") -> handleCommand(message, text)
            else -> {
                val buttonHandler = textButtonHandlers.find { it.supports(text) }
                if (buttonHandler != null) {
                    buttonHandler.handle(message)
                    telegramClient.deleteMessage(message.chat.id, message.messageId)
                } else {
                    messageHandler.handle(message)
                }
            }
        }
    }

    private suspend fun handleCommand(
        message: Message,
        text: String,
    ) {
        val command = text.split(" ", "@").first().lowercase()
        val handler = commandHandlers[command]

        if (handler != null) {
            logger.atDebug {
                this.message = "telegram_command_received"
                payload = mapOf("command" to command, "user_id" to message.from?.id)
            }
            handler.handle(message)
        } else {
            telegramClient.sendMessage(
                chatId = message.chat.id,
                BotResponses.UNKNOWN_COMMAND_RESPONSE.text,
            )

            logger.atDebug {
                this.message = "unknown_telegram_command_handled"
                payload = mapOf("command" to command, "user_id" to message.from?.id)
            }
        }
    }
}
