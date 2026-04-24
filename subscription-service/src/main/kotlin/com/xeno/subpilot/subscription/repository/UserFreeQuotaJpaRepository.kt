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
import com.xeno.subpilot.subscription.entity.UserFreeQuotaId
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface UserFreeQuotaJpaRepository : JpaRepository<UserFreeQuota, UserFreeQuotaId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM UserFreeQuota q WHERE q.userId = :userId AND q.provider = :provider")
    fun findByUserIdAndProviderForUpdate(
        userId: Long,
        provider: String,
    ): UserFreeQuota?

    @Modifying(clearAutomatically = true)
    @Query(
        """
        UPDATE UserFreeQuota q
        SET q.requestsRemaining = q.requestsRemaining + :amount
        WHERE q.userId = :userId AND q.provider = :provider
        """,
    )
    fun addRequests(
        userId: Long,
        provider: String,
        amount: Int,
    )

    @Modifying(clearAutomatically = true)
    @Query(
        """
        UPDATE UserFreeQuota q
        SET q.requestsRemaining = q.requestsRemaining - :amount
        WHERE q.userId = :userId AND q.provider = :provider
        """,
    )
    fun deductRequests(
        userId: Long,
        provider: String,
        amount: Int,
    )

    fun findAllByUserId(userId: Long): List<UserFreeQuota>
}
