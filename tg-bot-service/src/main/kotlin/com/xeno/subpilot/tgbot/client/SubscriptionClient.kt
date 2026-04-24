package com.xeno.subpilot.tgbot.client

import com.xeno.subpilot.tgbot.dto.BalanceInfo
import com.xeno.subpilot.tgbot.dto.ModelPreferenceResult
import com.xeno.subpilot.tgbot.dto.PlanInfo
import com.xeno.subpilot.tgbot.dto.RegistrationResult

interface SubscriptionClient {
    suspend fun registerUser(userId: Long): RegistrationResult?

    suspend fun setModelPreference(
        userId: Long,
        modelId: String,
    ): ModelPreferenceResult

    suspend fun getPlans(): List<PlanInfo>

    suspend fun getPlanInfo(planId: String): PlanInfo?

    suspend fun getBalance(userId: Long): BalanceInfo
}
