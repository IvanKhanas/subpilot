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
import org.springframework.stereotype.Repository

import java.time.LocalDateTime

@Repository
class JpaOutboxPaymentEventRepository(
    private val repository: OutboxPaymentEventJpaRepository,
) : OutboxPaymentEventRepository {

    override fun save(event: OutboxPaymentEvent): OutboxPaymentEvent = repository.save(event)

    override fun findUnpublished(limit: Int): List<OutboxPaymentEvent> =
        repository.findUnpublished(limit)

    override fun countByPublishedAtIsNull(): Long = repository.countByPublishedAtIsNull()

    override fun markPublished(
        ids: List<Long>,
        now: LocalDateTime,
    ) = repository.markPublished(ids, now)
}
