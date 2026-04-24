package com.xeno.subpilot.tgbot.util

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.message.BotResponses
import org.springframework.stereotype.Component

import java.util.concurrent.atomic.AtomicBoolean

private val ANIMATION_FRAMES = listOf("⣾", "⣽", "⣻", "⢿", "⡿", "⣟", "⣯", "⣷")
private const val FRAME_DELAY_MS = 200L

@Component
class AIResponseWaitingIndicator(
    private val telegramClient: TelegramClient,
) {
    suspend fun <T> wrap(
        chatId: Long,
        block: suspend () -> T,
    ): T {
        val messageId =
            telegramClient.sendMessage(chatId, BotResponses.WAITING_RESPONSE.text)
                ?: return block()

        val stopped = AtomicBoolean(false)
        val animationThread = startAnimationThread(chatId, messageId, stopped)
        return try {
            block()
        } finally {
            stopAnimation(animationThread, stopped, chatId, messageId)
        }
    }

    private fun startAnimationThread(
        chatId: Long,
        messageId: Long,
        stopped: AtomicBoolean,
    ): Thread =
        Thread.ofVirtual().start {
            var frameIndex = 0
            try {
                while (!stopped.get()) {
                    Thread.sleep(FRAME_DELAY_MS)
                    if (stopped.get()) break
                    frameIndex = (frameIndex + 1) % ANIMATION_FRAMES.size
                    telegramClient.editMessage(
                        chatId,
                        messageId,
                        "${ANIMATION_FRAMES[frameIndex]} ${BotResponses.WAITING_RESPONSE.text}",
                    )
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

    private fun stopAnimation(
        thread: Thread,
        stopped: AtomicBoolean,
        chatId: Long,
        messageId: Long,
    ) {
        stopped.set(true)
        thread.join()
        telegramClient.deleteMessage(chatId, messageId)
    }
}
