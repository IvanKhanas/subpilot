package com.xeno.subpilot.chat.service

import com.xeno.subpilot.chat.properties.ChatHistoryProperties
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
        val ops = redis.opsForList()
        ops.rightPush(
            key,
            objectMapper.writeValueAsString(ChatTurn(ChatTurn.Role.USER, userMessage)),
        )
        ops.rightPush(
            key,
            objectMapper.writeValueAsString(ChatTurn(ChatTurn.Role.ASSISTANT, assistantMessage)),
        )
        ops.trim(key, -properties.maxMessages.toLong(), -1)
        redis.expire(key, properties.ttl)
    }

    private fun redisKey(chatId: Long) = "chat:history:$chatId"
}
