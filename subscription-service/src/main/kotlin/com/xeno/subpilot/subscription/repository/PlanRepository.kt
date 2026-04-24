package com.xeno.subpilot.subscription.repository

import com.xeno.subpilot.subscription.properties.PlanProperties

interface PlanRepository {

    fun findById(planId: String): PlanProperties?

    fun findAllActive(): Map<String, PlanProperties>
}
