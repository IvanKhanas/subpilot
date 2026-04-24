package com.xeno.subpilot.tgbot.client

import com.xeno.subpilot.tgbot.dto.SpendResult

import java.util.UUID

interface LoyaltyClient {
    suspend fun getBalance(userId: Long): Long

    suspend fun spend(
        userId: Long,
        planId: String,
        idempotencyKey: UUID,
    ): SpendResult
}
