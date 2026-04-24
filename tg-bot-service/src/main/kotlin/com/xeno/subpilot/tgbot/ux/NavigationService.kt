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
package com.xeno.subpilot.tgbot.ux

import com.xeno.subpilot.tgbot.properties.NavigationProperties
import org.springframework.data.redis.core.RedisOperations
import org.springframework.data.redis.core.SessionCallback
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
        redis.executePipelined(
            object : SessionCallback<Any?> {
                override fun <K : Any, V : Any> execute(ops: RedisOperations<K, V>): Any? {
                    @Suppress("UNCHECKED_CAST")
                    val stringOps = ops as RedisOperations<String, String>
                    stringOps.opsForList().rightPush(key, screen.name)
                    stringOps.expire(key, properties.stackTtl)
                    return null
                }
            },
        )
    }

    fun peek(chatId: Long): BotScreen? {
        val name = redis.opsForList().index(redisKey(chatId), -1) ?: return null
        return BotScreen.entries.find { it.name == name }
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
