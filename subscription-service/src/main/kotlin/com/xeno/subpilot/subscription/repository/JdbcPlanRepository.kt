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

import com.xeno.subpilot.subscription.properties.PlanProperties
import com.xeno.subpilot.subscription.properties.ProviderAllocation
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

import java.math.BigDecimal
import java.sql.ResultSet

@Repository
class JdbcPlanRepository(
    private val jdbcTemplate: JdbcTemplate,
) : PlanRepository {

    override fun findById(planId: String): PlanProperties? {
        val rows =
            jdbcTemplate.query(
                PLAN_WITH_ALLOCATIONS_SQL + " WHERE p.plan_id = ? AND p.active = true",
                ::mapRow,
                planId,
            )
        return rows.toPlanProperties().values.firstOrNull()
    }

    override fun findAllActive(): Map<String, PlanProperties> {
        val rows =
            jdbcTemplate.query(
                PLAN_WITH_ALLOCATIONS_SQL + " WHERE p.active = true ORDER BY p.plan_id",
                ::mapRow,
            )
        return rows.toPlanProperties()
    }

    private fun mapRow(
        rs: ResultSet,
        @Suppress("UNUSED_PARAMETER") rowNum: Int,
    ) = PlanRow(
        planId = rs.getString("plan_id"),
        provider = rs.getString("provider"),
        displayName = rs.getString("display_name"),
        price = rs.getBigDecimal("price"),
        currency = rs.getString("currency"),
        allocProvider = rs.getString("alloc_provider"),
        requests = rs.getInt("requests"),
    )

    private fun List<PlanRow>.toPlanProperties(): Map<String, PlanProperties> =
        groupBy { it.planId }.mapValues { (_, rows) ->
            val first = rows.first()
            PlanProperties(
                provider = first.provider,
                displayName = first.displayName,
                price = first.price,
                currency = first.currency,
                allocations = rows.map { ProviderAllocation(it.allocProvider, it.requests) },
            )
        }

    private data class PlanRow(
        val planId: String,
        val provider: String,
        val displayName: String,
        val price: BigDecimal,
        val currency: String,
        val allocProvider: String,
        val requests: Int,
    )

    private companion object {
        const val PLAN_WITH_ALLOCATIONS_SQL =
            """
            SELECT p.plan_id, p.provider, p.display_name, p.price, p.currency,
                   a.provider AS alloc_provider, a.requests
            FROM subscription_plan p
            JOIN subscription_plan_allocation a ON p.plan_id = a.plan_id
            """
    }
}
