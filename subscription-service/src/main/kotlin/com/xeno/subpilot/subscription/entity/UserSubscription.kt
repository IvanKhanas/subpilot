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
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_subscription")
class UserSubscription(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "payment_id", nullable = false)
    val paymentId: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "plan_id", nullable = false)
    val planId: String,

    @Column(name = "provider", nullable = false)
    val provider: String,

    @Column(name = "earned_requests", nullable = false)
    val earnedRequests: Int,

    @Column(name = "activated_at", nullable = false)
    val activatedAt: Instant,
)
