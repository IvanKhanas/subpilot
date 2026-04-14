package com.xeno.subpilot.tgbot.client

import com.xeno.subpilot.tgbot.dto.RegistrationResult

interface SubscriptionClient {
    fun registerUser(userId: Long): RegistrationResult?

    fun setModelPreference(
        userId: Long,
        modelId: String,
    ): Boolean
}
