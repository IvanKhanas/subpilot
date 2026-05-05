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
package com.xeno.subpilot.loyalty.repository

import org.springframework.stereotype.Repository

@Repository
class JpaUserLoyaltyBalanceRepository(
    private val repository: UserLoyaltyBalanceJpaRepository,
) : UserLoyaltyBalanceRepository {

    override fun upsertAdd(
        userId: Long,
        amount: Long,
    ) = repository.upsertAdd(userId, amount)

    override fun deductIfSufficient(
        userId: Long,
        amount: Long,
    ): Int = repository.deductIfSufficient(userId, amount)

    override fun findPointsByUserId(userId: Long): Long? = repository.findPointsByUserId(userId)

    override fun subtractCappedAtZero(
        userId: Long,
        amount: Long,
    ) = repository.subtractCappedAtZero(userId, amount)
}
