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
package com.xeno.subpilot.chat.service

import com.xeno.subpilot.chat.properties.ChatHistoryProperties
import org.springframework.data.redis.core.RedisOperations
import org.springframework.data.redis.core.SessionCallback
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class ChatHistoryService(
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val properties: ChatHistoryProperties,
) {

    fun getHistory(chatId: Long): List<ChatTurn> {
        val raw = redis.opsForList().range(redisKey(chatId), 0, -1) ?: return emptyList()
        return raw.map { objectMapper.readValue(it, ChatTurn::class.java) }
    }

    fun append(
        chatId: Long,
        userMessage: String,
        assistantMessage: String,
    ) {
        val key = redisKey(chatId)
        val userJson = objectMapper.writeValueAsString(ChatTurn(ChatTurn.Role.USER, userMessage))
        val assistantJson =
            objectMapper.writeValueAsString(
                ChatTurn(ChatTurn.Role.ASSISTANT, assistantMessage),
            )
        redis.executePipelined(
            object : SessionCallback<Any?> {
                override fun <K : Any, V : Any> execute(ops: RedisOperations<K, V>): Any? {
                    @Suppress("UNCHECKED_CAST")
                    val stringOps = ops as RedisOperations<String, String>
                    stringOps.opsForList().rightPush(key, userJson)
                    stringOps.opsForList().rightPush(key, assistantJson)
                    stringOps.opsForList().trim(key, -properties.maxMessages.toLong(), -1)
                    stringOps.expire(key, properties.ttl)
                    return null
                }
            },
        )
    }

    fun clear(chatId: Long) {
        redis.delete(redisKey(chatId))
    }

    private fun redisKey(chatId: Long) = "chat:history:$chatId"
}
