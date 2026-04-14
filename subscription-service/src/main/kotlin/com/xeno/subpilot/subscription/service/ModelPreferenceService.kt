package com.xeno.subpilot.subscription.service

import com.xeno.subpilot.subscription.properties.SubscriptionProperties
import com.xeno.subpilot.subscription.repository.UserModelPreferenceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ModelPreferenceService(
    private val modelPreferenceRepository: UserModelPreferenceRepository,
    private val properties: SubscriptionProperties,
) {

    @Transactional(readOnly = true)
    fun getModelPreference(userId: Long): String? = modelPreferenceRepository.findById(userId)

    @Transactional
    fun setModelPreference(
        userId: Long,
        modelId: String,
    ): Boolean {
        val previousModelId = modelPreferenceRepository.findById(userId)
        modelPreferenceRepository.upsert(userId, modelId)
        val previousProvider = previousModelId?.let { properties.modelProviders[it] }
        val newProvider = properties.modelProviders[modelId]
        return previousProvider != null && previousProvider != newProvider
    }
}
