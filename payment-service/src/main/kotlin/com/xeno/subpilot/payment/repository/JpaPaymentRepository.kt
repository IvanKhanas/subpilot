/*
 * Copyright 2026 Ivan Khanas
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
import org.springframework.stereotype.Repository

import java.time.LocalDateTime
import java.util.UUID

@Repository
class JpaPaymentRepository(
    private val repository: PaymentJpaRepository,
) : PaymentRepository {

    override fun save(payment: Payment): Payment = repository.save(payment)

    override fun findByYooKassaPaymentId(yooKassaPaymentId: UUID): Payment? =
        repository.findByYooKassaPaymentId(yooKassaPaymentId)

    override fun updateStatusIfPending(
        id: UUID,
        status: PaymentStatus,
        now: LocalDateTime,
    ): Int = repository.updateStatusIfPending(id, status, now)
}
