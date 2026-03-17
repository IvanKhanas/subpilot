package com.xeno.subpilot.tgbot.runtime

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.command.BotCommand
import com.xeno.subpilot.tgbot.dto.BotCommandInfo
import com.xeno.subpilot.tgbot.properties.TelegramBotProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class TelegramLongPollingService(
    private val telegramClient: TelegramClient,
    private val messageHandler: TelegramMessageHandler,
    private val botCommands: List<BotCommand>,
    private val properties: TelegramBotProperties,
) {

    private val running = AtomicBoolean(false)
    private val offset = AtomicLong(0)
    private var pollingThread: Thread? = null

    @EventListener(ApplicationReadyEvent::class)
    fun start() {
        registerCommands()
        startPolling()
    }

    @PreDestroy
    fun stop() {
        logger.info { "Stopping Telegram long-polling" }
        running.set(false)
        pollingThread?.interrupt()
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
            logger.info { "Registered ${commands.size} bot commands: ${commands.map { it.command }}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to register bot commands" }
        }
    }

    private fun startPolling() {
        if (!running.compareAndSet(false, true)) return
        logger.info { "Starting Telegram long-polling" }

        pollingThread =
            Thread.ofVirtual().name("tg-polling").start {
                while (running.get()) {
                    try {
                        val currentOffset = offset.get().takeIf { it > 0 }
                        val updates = telegramClient.getUpdates(currentOffset, properties.pollingTimeout)

                        for (update in updates) {
                            offset.set(update.updateId + 1)
                            try {
                                messageHandler.onUpdate(update)
                            } catch (e: Exception) {
                                logger.error(e) { "Error handling update ${update.updateId}" }
                            }
                        }
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    } catch (e: Exception) {
                        logger.error(e) { "Polling error, retrying in 5s" }
                        Thread.sleep(5_000)
                    }
                }
            }
    }
}
