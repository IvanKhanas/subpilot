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
