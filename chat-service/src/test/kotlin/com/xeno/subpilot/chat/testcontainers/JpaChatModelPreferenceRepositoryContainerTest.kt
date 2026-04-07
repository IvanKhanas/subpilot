package com.xeno.subpilot.chat.testcontainers

import com.xeno.subpilot.chat.properties.OpenAiProperties
import com.xeno.subpilot.chat.repository.ChatModelPreferenceJpaRepository
import com.xeno.subpilot.chat.repository.JpaChatModelPreferenceRepository
import net.datafaker.Faker
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

import kotlin.test.assertEquals

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = [
        "openai.api-key=test-key",
        "spring.grpc.server.port=0",
    ],
)
class JpaChatModelPreferenceRepositoryContainerTest {

    companion object {
        private val faker = Faker()
        private val postgres = TestContainersConfiguration.postgres
        private val redis = TestContainersConfiguration.redis

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
        }
    }

    @Autowired
    private lateinit var jpaRepository: ChatModelPreferenceJpaRepository

    @Autowired
    private lateinit var openAiProperties: OpenAiProperties

    private fun repository() = JpaChatModelPreferenceRepository(jpaRepository, openAiProperties)

    private fun randomChatId() = faker.number().numberBetween(1L, Long.MAX_VALUE)

    @Test
    fun `getModel returns defaultModel when no preference exists`() {
        val chatId = randomChatId()

        assertEquals(openAiProperties.defaultModel, repository().getModel(chatId))
    }

    @Test
    fun `setModel creates preference and getModel returns it`() {
        val chatId = randomChatId()

        repository().setModel(chatId, "gpt-4o-mini")

        assertEquals("gpt-4o-mini", repository().getModel(chatId))
    }

    @Test
    fun `setModel updates existing preference`() {
        val chatId = randomChatId()
        repository().setModel(chatId, "gpt-4o")

        repository().setModel(chatId, "gpt-4o-mini")

        assertEquals("gpt-4o-mini", repository().getModel(chatId))
    }

    @Test
    fun `different chats have independent preferences`() {
        val chatId1 = randomChatId()
        val chatId2 = randomChatId()

        repository().setModel(chatId1, "gpt-4o")
        repository().setModel(chatId2, "gpt-4o-mini")

        assertEquals("gpt-4o", repository().getModel(chatId1))
        assertEquals("gpt-4o-mini", repository().getModel(chatId2))
    }
}
