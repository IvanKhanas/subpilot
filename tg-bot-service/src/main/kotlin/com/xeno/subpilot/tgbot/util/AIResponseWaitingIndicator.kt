package com.xeno.subpilot.tgbot.util

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.message.BotResponses
import org.springframework.stereotype.Component

private val ANIMATION_FRAMES = listOf("⣾", "⣽", "⣻", "⢿", "⡿", "⣟", "⣯", "⣷")
private const val FRAME_DELAY_MS = 200L

@Component
class AIResponseWaitingIndicator(
    private val telegramClient: TelegramClient,
) {
    fun <T> wrap(
        chatId: Long,
        block: () -> T,
    ): T {
        val messageId =
            telegramClient.sendMessage(chatId, BotResponses.WAITING_RESPONSE.text)
                ?: return block()

        val animationThread = startAnimationThread(chatId, messageId)
        return try {
            block()
        } finally {
            stopAnimation(animationThread, chatId, messageId)
        }
    }

    private fun startAnimationThread(
        chatId: Long,
        messageId: Long,
    ): Thread =
        Thread.ofVirtual().start {
            var frameIndex = 0
            try {
                while (!Thread.currentThread().isInterrupted) {
                    Thread.sleep(FRAME_DELAY_MS)
                    if (Thread.currentThread().isInterrupted) break
                    frameIndex = (frameIndex + 1) % ANIMATION_FRAMES.size
                    telegramClient.editMessage(
                        chatId,
                        messageId,
                        "${ANIMATION_FRAMES[frameIndex]} ${BotResponses.WAITING_RESPONSE.text}",
                    )
                }
            } catch (_: InterruptedException) {
                // animation stopped normally
            }
        }

    private fun stopAnimation(
        thread: Thread,
        chatId: Long,
        messageId: Long,
    ) {
        thread.interrupt()
        thread.join()
        telegramClient.deleteMessage(chatId, messageId)
    }
}
