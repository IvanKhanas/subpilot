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
