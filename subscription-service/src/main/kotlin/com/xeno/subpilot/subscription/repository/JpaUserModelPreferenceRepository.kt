package com.xeno.subpilot.subscription.repository

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class JpaUserModelPreferenceRepository(
    private val repository: UserModelPreferenceJpaRepository,
) : UserModelPreferenceRepository {

    override fun findById(userId: Long): String? = repository.findByIdOrNull(userId)?.modelId

    override fun upsert(
        userId: Long,
        modelId: String,
    ) {
        repository.upsert(userId, modelId)
    }
}
