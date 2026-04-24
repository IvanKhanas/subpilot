package com.xeno.subpilot.chat.testcontainers

import com.xeno.subpilot.chat.properties.ChatHistoryProperties
import com.xeno.subpilot.chat.service.ChatHistoryService
import com.xeno.subpilot.chat.service.ChatTurn
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper

import java.time.Duration

import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatHistoryServiceContainerTest {

    private lateinit var service: ChatHistoryService
    private lateinit var template: StringRedisTemplate

    companion object {
        private val faker = Faker()
        private val redis = TestContainersConfiguration.redis
        private val objectMapper =
            JsonMapper
                .builder()
                .findAndAddModules()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .build()
    }

    @BeforeEach
    fun setUp() {
        val factory = LettuceConnectionFactory(redis.host, redis.getMappedPort(6379))
        factory.afterPropertiesSet()
        template = StringRedisTemplate()
        template.connectionFactory = factory
        template.afterPropertiesSet()
        service =
            ChatHistoryService(
                template,
                objectMapper,
                ChatHistoryProperties(maxMessages = 6, ttl = Duration.ofSeconds(2)),
            )
    }

    private fun randomChatId() = faker.number().numberBetween(1L, Long.MAX_VALUE)

    private fun randomMessage() = faker.lorem().sentence()

    @Test
    fun `getHistory returns empty list for new chat`() {
        val chatId = randomChatId()

        assertTrue(service.getHistory(chatId).isEmpty())
    }

    @Test
    fun `append and getHistory returns stored turns in order`() {
        val chatId = randomChatId()
        val userMsg = randomMessage()
        val aiMsg = randomMessage()

        service.append(chatId, userMsg, aiMsg)
        val history = service.getHistory(chatId)

        assertEquals(2, history.size)
        assertEquals(ChatTurn(ChatTurn.Role.USER, userMsg), history[0])
        assertEquals(ChatTurn(ChatTurn.Role.ASSISTANT, aiMsg), history[1])
    }

    @Test
    fun `append multiple times accumulates history`() {
        val chatId = randomChatId()

        repeat(2) { service.append(chatId, randomMessage(), randomMessage()) }

        assertEquals(4, service.getHistory(chatId).size)
    }

    @Test
    fun `history is trimmed to maxMessages`() {
        val chatId = randomChatId()

        repeat(5) { service.append(chatId, randomMessage(), randomMessage()) }

        assertEquals(6, service.getHistory(chatId).size)
    }

    @Test
    fun `history expires after TTL`() {
        val chatId = randomChatId()
        service.append(chatId, randomMessage(), randomMessage())

        awaitCondition(timeoutMs = 5000) { service.getHistory(chatId).isEmpty() }
    }

    @Test
    fun `different chats have independent histories`() {
        val chatId1 = randomChatId()
        val chatId2 = randomChatId()
        val msg1 = randomMessage()
        val msg2 = randomMessage()

        service.append(chatId1, msg1, randomMessage())
        service.append(chatId2, msg2, randomMessage())

        assertEquals(msg1, service.getHistory(chatId1)[0].content)
        assertEquals(msg2, service.getHistory(chatId2)[0].content)
    }
}
