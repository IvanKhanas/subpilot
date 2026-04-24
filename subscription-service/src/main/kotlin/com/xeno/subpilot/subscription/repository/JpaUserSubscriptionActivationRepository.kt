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

import com.xeno.subpilot.subscription.properties.ProviderAllocation
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Repository
class JpaUserSubscriptionActivationRepository(
    private val jdbcTemplate: JdbcTemplate,
) : UserSubscriptionActivationRepository {

    override fun batchInsertUserSubscriptionIfAbsent(
        paymentId: UUID,
        userId: Long,
        planId: String,
        allocations: List<ProviderAllocation>,
        activatedAt: Instant,
    ): List<String> =
        jdbcTemplate.execute { conn: java.sql.Connection ->
            conn
                .prepareStatement(
                    """
                    INSERT INTO user_subscription (payment_id, user_id, plan_id, provider, earned_requests, activated_at)
                    SELECT ?, ?, ?, unnest(?::text[]), unnest(?::int[]), ?
                    ON CONFLICT (payment_id, provider) DO NOTHING
                    RETURNING provider
                    """.trimIndent(),
                ).use { ps ->
                    ps.setObject(1, paymentId)
                    ps.setLong(2, userId)
                    ps.setString(3, planId)
                    ps.setArray(
                        4,
                        conn.createArrayOf("text", allocations.map { it.provider }.toTypedArray()),
                    )
                    ps.setArray(
                        5,
                        conn.createArrayOf("int", allocations.map { it.requests }.toTypedArray()),
                    )
                    ps.setTimestamp(6, Timestamp.from(activatedAt))
                    ps.executeQuery().use { rs ->
                        generateSequence {
                            if (rs.next()) {
                                rs.getString(
                                    "provider",
                                )
                            } else {
                                null
                            }
                        }.toList()
                    }
                }
        } ?: emptyList()

    override fun batchUpsertRequestBalance(
        userId: Long,
        allocations: List<ProviderAllocation>,
    ) {
        jdbcTemplate.batchUpdate(
            """
            INSERT INTO user_request_balance (user_id, provider, requests_remaining)
            VALUES (?, ?, ?)
            ON CONFLICT (user_id, provider)
            DO UPDATE SET requests_remaining = user_request_balance.requests_remaining + EXCLUDED.requests_remaining
            """.trimIndent(),
            allocations.map { allocation ->
                arrayOf(userId, allocation.provider, allocation.requests)
            },
        )
    }
}
