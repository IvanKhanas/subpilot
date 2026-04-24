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

import com.xeno.subpilot.subscription.properties.SubscriptionProperties
import com.xeno.subpilot.subscription.repository.SubscriptionUserRepository
import com.xeno.subpilot.subscription.repository.UserFreeQuotaRepository
import com.xeno.subpilot.subscription.repository.UserModelPreferenceRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

@Service
class UserService(
    private val subscriptionUserRepository: SubscriptionUserRepository,
    private val freeQuotaRepository: UserFreeQuotaRepository,
    private val modelPreferenceRepository: UserModelPreferenceRepository,
    private val properties: SubscriptionProperties,
) {

    @Transactional
    fun registerUser(userId: Long): Boolean {
        val isNew = subscriptionUserRepository.insertIfAbsent(userId)
        if (!isNew) return false

        freeQuotaRepository.createAll(
            userId,
            properties.modelProviders.values.toSet(),
            properties.freeQuota,
            LocalDateTime.now().plus(properties.freeQuotaResetPeriod),
        )
        modelPreferenceRepository.upsert(userId, properties.defaultModel)
        return true
    }
}
