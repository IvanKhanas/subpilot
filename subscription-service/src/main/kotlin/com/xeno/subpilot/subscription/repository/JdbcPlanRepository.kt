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
            jdbcTemplate.query(PLAN_WITH_ALLOCATIONS_SQL + " WHERE p.plan_id = ? AND p.active = true", ::mapRow, planId)
        return rows.toPlanProperties().values.firstOrNull()
    }

    override fun findAllActive(): Map<String, PlanProperties> {
        val rows = jdbcTemplate.query(PLAN_WITH_ALLOCATIONS_SQL + " WHERE p.active = true ORDER BY p.plan_id", ::mapRow)
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
