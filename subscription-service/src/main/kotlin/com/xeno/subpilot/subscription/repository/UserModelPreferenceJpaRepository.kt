package com.xeno.subpilot.subscription.repository

import com.xeno.subpilot.subscription.entity.UserModelPreference
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface UserModelPreferenceJpaRepository : JpaRepository<UserModelPreference, Long> {

    @Modifying(clearAutomatically = true)
    @Query(
        value =
            "INSERT INTO user_model_preference (user_id, model_id)" +
                " VALUES (:userId, :modelId)" +
                " ON CONFLICT (user_id) DO UPDATE SET model_id = :modelId",
        nativeQuery = true,
    )
    fun upsert(
        userId: Long,
        modelId: String,
    )
}
