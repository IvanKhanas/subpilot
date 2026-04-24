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

import com.xeno.subpilot.payment.entity.OutboxPaymentEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

import java.time.LocalDateTime

interface OutboxPaymentEventJpaRepository : JpaRepository<OutboxPaymentEvent, Long> {

    @Query(
        value =
            "SELECT * FROM outbox_payment_event WHERE published_at IS NULL ORDER BY created_at LIMIT :limit",
        nativeQuery = true,
    )
    fun findUnpublished(
        @Param("limit") limit: Int,
    ): List<OutboxPaymentEvent>

    @Modifying
    @Query(
        "UPDATE OutboxPaymentEvent event" +
            " SET event.publishedAt = :now WHERE event.id IN :ids",
    )
    fun markPublished(
        @Param("ids") ids: List<Long>,
        @Param("now") now: LocalDateTime,
    )
}
