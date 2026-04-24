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
package com.xeno.subpilot.payment.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "payment")
class Payment(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "plan_id", nullable = false)
    val planId: String,

    @Column(name = "yookassa_payment_id")
    var yooKassaPaymentId: UUID? = null,

    @Column(name = "amount", nullable = false)
    val amount: BigDecimal,

    @Column(name = "currency", nullable = false)
    val currency: String,

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: PaymentStatus = PaymentStatus.PENDING,

    @Column(name = "bonus_points_used", nullable = false)
    val bonusPointsUsed: Long = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,
)
