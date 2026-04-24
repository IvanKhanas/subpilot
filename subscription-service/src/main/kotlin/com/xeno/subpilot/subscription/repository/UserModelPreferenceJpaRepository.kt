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
