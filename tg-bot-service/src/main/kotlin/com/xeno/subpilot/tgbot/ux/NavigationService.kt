package com.xeno.subpilot.tgbot.ux

import com.xeno.subpilot.tgbot.properties.NavigationProperties
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

@Service
class NavigationService(
    private val redis: StringRedisTemplate,
    private val properties: NavigationProperties,
) {
    fun push(
        chatId: Long,
        screen: BotScreen,
    ) {
        val key = redisKey(chatId)
        redis.opsForList().rightPush(key, screen.name)
        redis.expire(key, properties.stackTtl)
    }

    fun pop(chatId: Long): BotScreen? {
        val name = redis.opsForList().rightPop(redisKey(chatId)) ?: return null
        return BotScreen.entries.find { it.name == name }
    }

    fun clear(chatId: Long) {
        redis.delete(redisKey(chatId))
    }

    private fun redisKey(chatId: Long) = "nav:stack:$chatId"
}
