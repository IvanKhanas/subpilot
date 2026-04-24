package com.xeno.subpilot.payment.dto

import java.math.BigDecimal

data class PlanDetails(
    val price: BigDecimal,
    val currency: String,
)
