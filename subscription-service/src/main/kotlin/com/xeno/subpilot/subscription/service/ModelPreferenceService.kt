package com.xeno.subpilot.subscription.service

import com.xeno.subpilot.subscription.dto.ModelPreferenceResult
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
    ): ModelPreferenceResult {
        val previousModelId = modelPreferenceRepository.findById(userId)
        modelPreferenceRepository.upsert(userId, modelId)
        val previousProvider = previousModelId?.let { properties.modelProviders[it] }
        val newProvider = properties.modelProviders[modelId] ?: ""
        val modelCost = properties.modelCosts[modelId] ?: 1
        return ModelPreferenceResult(
            providerChanged = previousProvider != null && previousProvider != newProvider,
            modelCost = modelCost,
            provider = newProvider,
        )
    }
}
