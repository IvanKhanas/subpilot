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
package com.xeno.subpilot.payment.repository

import com.xeno.subpilot.payment.entity.Payment
import com.xeno.subpilot.payment.entity.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

import java.time.LocalDateTime
import java.util.UUID

interface PaymentJpaRepository : JpaRepository<Payment, UUID> {

    @Modifying
    @Query(
        "UPDATE Payment p SET p.status = :status," +
            " p.updatedAt = :now WHERE p.id = :id AND p.status = :pending",
    )
    fun updateStatusIfPending(
        id: UUID,
        status: PaymentStatus,
        now: LocalDateTime,
        pending: PaymentStatus = PaymentStatus.PENDING,
    ): Int

    @Query("SELECT p FROM Payment p WHERE p.yooKassaPaymentId = :yooKassaPaymentId")
    fun findByYooKassaPaymentId(yooKassaPaymentId: UUID): Payment?
}
