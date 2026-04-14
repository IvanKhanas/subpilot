package com.xeno.subpilot.chat.unittests.service

import com.xeno.subpilot.chat.properties.ChatHistoryProperties
import com.xeno.subpilot.chat.service.ChatHistoryService
import com.xeno.subpilot.chat.service.ChatTurn
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.redis.core.ListOperations
import org.springframework.data.redis.core.StringRedisTemplate
import tools.jackson.databind.ObjectMapper

import java.time.Duration

import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class ChatHistoryServiceTest {

    @MockK(relaxed = true)
    lateinit var redis: StringRedisTemplate

    @MockK(relaxed = true)
    lateinit var listOperations: ListOperations<String, String>

    @MockK
    lateinit var objectMapper: ObjectMapper

    private val properties = ChatHistoryProperties(maxMessages = 40, ttl = Duration.ofMinutes(20))

    private lateinit var service: ChatHistoryService

    private val chatId = 42L
    private val redisKey = "chat:history:$chatId"

    @BeforeEach
    fun setUp() {
        every { redis.opsForList() } returns listOperations
        service = ChatHistoryService(redis, objectMapper, properties)
    }

    @Test
    fun `getHistory returns empty list when Redis has no data`() {
        every { listOperations.range(redisKey, 0, -1) } returns null

        val result = service.getHistory(chatId)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getHistory deserializes each entry from Redis`() {
        val userTurn = ChatTurn(ChatTurn.Role.USER, "hello")
        val assistantTurn = ChatTurn(ChatTurn.Role.ASSISTANT, "hi")
        every { listOperations.range(redisKey, 0, -1) } returns listOf("json1", "json2")
        every { objectMapper.readValue("json1", ChatTurn::class.java) } returns userTurn
        every { objectMapper.readValue("json2", ChatTurn::class.java) } returns assistantTurn

        val result = service.getHistory(chatId)

        assertEquals(listOf(userTurn, assistantTurn), result)
    }
}
