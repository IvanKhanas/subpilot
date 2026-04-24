package com.xeno.subpilot.tgbot.service

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.kafka.SubscriptionActivatedEvent
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.ux.AiProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

private val logger = KotlinLogging.logger {}

@Component
class SubscriptionActivatedConsumer(
    private val telegramClient: TelegramClient,
    private val objectMapper: ObjectMapper,
) {

    @KafkaListener(topics = ["subscription_activated"])
    fun consume(message: String) {
        val event = objectMapper.readValue(message, SubscriptionActivatedEvent::class.java)
        val allocationsText =
            event.allocations.joinToString("\n") { alloc ->
                "• ${AiProvider.displayNameByKey(
                    alloc.provider,
                )}: ${alloc.requests} requests credited"
            }
        telegramClient.sendMessage(
            chatId = event.userId,
            text =
                BotResponses.SUBSCRIPTION_ACTIVATED_RESPONSE.format(
                    event.planDisplayName,
                    allocationsText,
                ),
        )
    }
}
