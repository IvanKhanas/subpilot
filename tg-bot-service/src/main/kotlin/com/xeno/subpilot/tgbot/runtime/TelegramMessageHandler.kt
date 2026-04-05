package com.xeno.subpilot.tgbot.runtime

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.command.BotCommand
import com.xeno.subpilot.tgbot.dto.CallbackQuery
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.Update
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

    override fun onUpdate(update: Update) {
        when {
            update.callbackQuery != null -> handleCallback(update.callbackQuery)
            update.message?.text != null -> handleMessage(update.message)
        }
    }

    private fun handleCallback(callback: CallbackQuery) {
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

    private fun handleMessage(message: Message) {
        val text = message.text ?: return
        when {
            text.startsWith("/") -> handleCommand(message, text)
            else -> {
                val buttonHandler = textButtonHandlers.find { it.supports(text) }
                if (buttonHandler != null) {
                    buttonHandler.handle(message)
                } else {
                    messageHandler.handle(message)
                }
            }
        }
    }

    private fun handleCommand(
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
            logger.atDebug {
                this.message = "telegram_command_unhandled"
                payload = mapOf("command" to command, "user_id" to message.from?.id)
            }
        }
    }
}
