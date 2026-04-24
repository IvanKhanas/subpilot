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
