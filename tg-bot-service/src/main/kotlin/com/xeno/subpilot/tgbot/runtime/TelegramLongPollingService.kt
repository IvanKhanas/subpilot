package com.xeno.subpilot.tgbot.runtime

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.command.BotCommand
import com.xeno.subpilot.tgbot.dto.BotCommandInfo
import com.xeno.subpilot.tgbot.dto.Update
import com.xeno.subpilot.tgbot.properties.TelegramBotProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

import java.util.concurrent.atomic.AtomicLong

import kotlin.coroutines.CoroutineContext

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger {}

@Service
class TelegramLongPollingService(
    private val telegramClient: TelegramClient,
    private val messageHandler: TelegramMessageHandler,
    private val botCommands: List<BotCommand>,
    private val properties: TelegramBotProperties,
    private val ioDispatcher: CoroutineContext,
) {
    private val scope = CoroutineScope(ioDispatcher)
    private val offset = AtomicLong(0)

    @EventListener(ApplicationReadyEvent::class)
    fun start() {
        registerCommands()
        startPolling()
    }

    @PreDestroy
    fun stop() {
        logger.info { "Stopping Telegram long-polling" }
        scope.cancel()
    }

    private fun registerCommands() {
        try {
            val commands =
                botCommands.map {
                    BotCommandInfo(
                        command = it.command.removePrefix("/"),
                        description = it.description,
                    )
                }
            telegramClient.setMyCommands(commands)
            logger.info {
                "Registered ${commands.size} bot commands: ${commands.map { it.command }}"
            }
        } catch (ex: Exception) {
            logger.atError {
                message = "telegram_register_commands_failed"
                cause = ex
            }
        }
    }

    private fun startPolling() {
        logger.info { "Starting Telegram long-polling" }
        scope.launch {
            while (isActive) {
                try {
                    pollOnce()
                } catch (ex: CancellationException) {
                    throw ex
                } catch (ex: Exception) {
                    logger.atWarn {
                        message = "telegram_polling_error"
                        cause = ex
                        payload = mapOf("retry_delay_ms" to properties.pollingRetryDelayMs)
                    }
                    delay(properties.pollingRetryDelayMs)
                }
            }
        }
    }

    private suspend fun pollOnce() {
        val updates =
            telegramClient.getUpdates(
                offset.get().takeIf { it > 0 },
                properties.pollingTimeout,
            )

        updates.forEach { update ->
            offset.set(update.updateId + 1)
            scope.launch { handleUpdate(update) }
        }
    }

    private fun handleUpdate(update: Update) {
        try {
            messageHandler.onUpdate(update)
        } catch (ex: Exception) {
            logger.atError {
                message = "telegram_update_handling_failed"
                cause = ex
                payload = mapOf("update_id" to update.updateId)
            }
        }
    }
}
