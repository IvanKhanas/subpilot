package com.xeno.subpilot.subscription.repository

interface UserModelPreferenceRepository {
    fun findById(userId: Long): String?

    fun upsert(
        userId: Long,
        modelId: String,
    )
}
