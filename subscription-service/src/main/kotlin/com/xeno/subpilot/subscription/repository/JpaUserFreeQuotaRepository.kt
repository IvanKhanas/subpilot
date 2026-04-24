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
package com.xeno.subpilot.subscription.repository

import com.xeno.subpilot.subscription.entity.UserFreeQuota
import org.springframework.stereotype.Repository

import java.time.LocalDateTime

@Repository
class JpaUserFreeQuotaRepository(
    private val repository: UserFreeQuotaJpaRepository,
) : UserFreeQuotaRepository {

    override fun findByUserIdAndProviderForUpdate(
        userId: Long,
        provider: String,
    ): UserFreeQuota? = repository.findByUserIdAndProviderForUpdate(userId, provider)

    override fun addRequests(
        userId: Long,
        provider: String,
        amount: Int,
    ) = repository.addRequests(userId, provider, amount)

    override fun deductRequests(
        userId: Long,
        provider: String,
        amount: Int,
    ) = repository.deductRequests(userId, provider, amount)

    override fun createAll(
        userId: Long,
        providers: Set<String>,
        initialAmount: Int,
        nextResetAt: LocalDateTime,
    ) {
        repository.saveAll(
            providers.map { provider ->
                UserFreeQuota(userId, provider, initialAmount, nextResetAt)
            },
        )
    }

    override fun findAllByUserId(userId: Long): List<UserFreeQuota> =
        repository.findAllByUserId(userId)
}
