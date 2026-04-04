package com.xeno.subpilot.chat.repository

import com.xeno.subpilot.chat.entity.ChatModelPreference
import com.xeno.subpilot.chat.properties.OpenAiProperties
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaChatModelPreferenceRepository(
    private val repository: ChatModelPreferenceJpaRepository,
    private val openAiProperties: OpenAiProperties,
) : ChatModelPreferenceRepository {

    @Transactional(readOnly = true)
    override fun getModel(chatId: Long): String =
        repository.findByIdOrNull(chatId)?.model ?: openAiProperties.defaultModel

    @Transactional
    override fun setModel(
        chatId: Long,
        model: String,
    ) {
        val preference =
            repository
                .findByIdOrNull(chatId)
                ?.also { it.model = model }
                ?: ChatModelPreference(chatId = chatId, model = model)
        repository.save(preference)
    }
}
