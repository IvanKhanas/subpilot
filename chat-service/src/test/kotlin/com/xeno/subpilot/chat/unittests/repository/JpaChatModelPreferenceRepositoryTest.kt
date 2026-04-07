package com.xeno.subpilot.chat.unittests.repository

import com.xeno.subpilot.chat.entity.ChatModelPreference
import com.xeno.subpilot.chat.properties.OpenAiProperties
import com.xeno.subpilot.chat.repository.ChatModelPreferenceJpaRepository
import com.xeno.subpilot.chat.repository.JpaChatModelPreferenceRepository
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull

import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class JpaChatModelPreferenceRepositoryTest {

    @MockK
    lateinit var jpaRepository: ChatModelPreferenceJpaRepository

    private val openAiProperties = OpenAiProperties(apiKey = "test-key", defaultModel = "gpt-4o")

    private lateinit var repository: JpaChatModelPreferenceRepository

    private val chatId = 42L

    @BeforeEach
    fun setUp() {
        repository = JpaChatModelPreferenceRepository(jpaRepository, openAiProperties)
    }

    @Test
    fun `getModel returns model when preference exists`() {
        every { jpaRepository.findByIdOrNull(chatId) } returns
            ChatModelPreference(chatId, "gpt-4o-mini")

        val result = repository.getModel(chatId)

        assertEquals("gpt-4o-mini", result)
    }

    @Test
    fun `getModel returns defaultModel when preference not found`() {
        every { jpaRepository.findByIdOrNull(chatId) } returns null

        val result = repository.getModel(chatId)

        assertEquals(openAiProperties.defaultModel, result)
    }

    @Test
    fun `setModel updates model on existing preference`() {
        val existing = ChatModelPreference(chatId, "gpt-4o")
        every { jpaRepository.findByIdOrNull(chatId) } returns existing
        every { jpaRepository.save(any()) } answers { firstArg() }

        repository.setModel(chatId, "gpt-4o-mini")

        verify { jpaRepository.save(match { it.model == "gpt-4o-mini" && it.chatId == chatId }) }
    }

    @Test
    fun `setModel creates new preference when none exists`() {
        val savedSlot = slot<ChatModelPreference>()
        every { jpaRepository.findByIdOrNull(chatId) } returns null
        every { jpaRepository.save(capture(savedSlot)) } answers { firstArg() }

        repository.setModel(chatId, "gpt-4o-mini")

        assertEquals(chatId, savedSlot.captured.chatId)
        assertEquals("gpt-4o-mini", savedSlot.captured.model)
    }
}
