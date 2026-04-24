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
package com.xeno.subpilot.subscription.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table

import java.io.Serializable
import java.time.LocalDateTime

data class UserFreeQuotaId(
    val userId: Long = 0,
    val provider: String = "",
) : Serializable

@Entity
@IdClass(UserFreeQuotaId::class)
@Table(name = "user_free_quota")
class UserFreeQuota(
    @Id
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Id
    @Column(name = "provider", nullable = false)
    val provider: String,

    @Column(name = "requests_remaining", nullable = false)
    var requestsRemaining: Int,

    @Column(name = "next_reset_at", nullable = false)
    var nextResetAt: LocalDateTime,
)
