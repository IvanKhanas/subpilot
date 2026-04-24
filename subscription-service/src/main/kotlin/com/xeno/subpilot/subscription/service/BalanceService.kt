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

import com.xeno.subpilot.subscription.dto.BalanceInfo
import com.xeno.subpilot.subscription.dto.FreeProviderBalance
import com.xeno.subpilot.subscription.dto.PaidProviderBalance
import com.xeno.subpilot.subscription.repository.UserFreeQuotaRepository
import com.xeno.subpilot.subscription.repository.UserRequestBalanceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BalanceService(
    private val userFreeQuotaRepository: UserFreeQuotaRepository,
    private val userRequestBalanceRepository: UserRequestBalanceRepository,
) {

    @Transactional(readOnly = true)
    fun getBalance(userId: Long): BalanceInfo {
        val freeBalances =
            userFreeQuotaRepository
                .findAllByUserId(userId)
                .map { FreeProviderBalance(it.provider, it.requestsRemaining, it.nextResetAt) }
        val paidBalances =
            userRequestBalanceRepository
                .findAllByUserId(userId)
                .map { PaidProviderBalance(it.provider, it.requestsRemaining) }
        return BalanceInfo(freeBalances, paidBalances)
    }
}
