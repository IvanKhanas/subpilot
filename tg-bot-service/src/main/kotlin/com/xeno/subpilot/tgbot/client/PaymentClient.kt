package com.xeno.subpilot.tgbot.client

interface PaymentClient {
    suspend fun createPayment(
        userId: Long,
        planId: String,
        bonusPointsToApply: Long = 0,
    ): String
}
