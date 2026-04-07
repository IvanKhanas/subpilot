package com.xeno.subpilot.tgbot.testcontainers

import com.xeno.subpilot.tgbot.properties.NavigationProperties
import com.xeno.subpilot.tgbot.ux.BotScreen
import com.xeno.subpilot.tgbot.ux.NavigationService
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate

import java.time.Duration

import kotlin.test.assertEquals
import kotlin.test.assertNull

class NavigationServiceContainerTest {

    private lateinit var service: NavigationService
    private lateinit var template: StringRedisTemplate

    companion object {
        private val faker = Faker()
        private val redis = TestContainersConfiguration.redis
    }

    @BeforeEach
    fun setUp() {
        val factory = LettuceConnectionFactory(redis.host, redis.getMappedPort(6379))
        factory.afterPropertiesSet()
        template = StringRedisTemplate()
        template.connectionFactory = factory
        template.afterPropertiesSet()
        service =
            NavigationService(template, NavigationProperties(stackTtl = Duration.ofSeconds(2)))
    }

    private fun randomChatId() = faker.number().numberBetween(1L, Long.MAX_VALUE)

    @Test
    fun `push and pop returns the pushed screen`() {
        val chatId = randomChatId()

        service.push(chatId, BotScreen.PROVIDER_MENU)

        assertEquals(BotScreen.PROVIDER_MENU, service.pop(chatId))
    }

    @Test
    fun `push multiple screens and pop returns in LIFO order`() {
        val chatId = randomChatId()

        service.push(chatId, BotScreen.MAIN_MENU)
        service.push(chatId, BotScreen.PROVIDER_MENU)

        assertEquals(BotScreen.PROVIDER_MENU, service.pop(chatId))
        assertEquals(BotScreen.MAIN_MENU, service.pop(chatId))
    }

    @Test
    fun `pop returns null when stack is empty`() {
        val chatId = randomChatId()

        assertNull(service.pop(chatId))
    }

    @Test
    fun `clear removes all screens from stack`() {
        val chatId = randomChatId()
        service.push(chatId, BotScreen.MAIN_MENU)
        service.push(chatId, BotScreen.PROVIDER_MENU)

        service.clear(chatId)

        assertNull(service.pop(chatId))
    }

    @Test
    fun `different chats have independent stacks`() {
        val chatId1 = randomChatId()
        val chatId2 = randomChatId()

        service.push(chatId1, BotScreen.MAIN_MENU)
        service.push(chatId2, BotScreen.PROVIDER_MENU)

        assertEquals(BotScreen.MAIN_MENU, service.pop(chatId1))
        assertEquals(BotScreen.PROVIDER_MENU, service.pop(chatId2))
    }

    @Test
    fun `stack key expires after TTL`() {
        val chatId = randomChatId()
        service.push(chatId, BotScreen.MAIN_MENU)

        Thread.sleep(2500)

        assertNull(service.pop(chatId))
    }
}
