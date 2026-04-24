package com.xeno.subpilot.loyalty.properties

import org.springframework.boot.context.properties.ConfigurationProperties

import java.math.BigDecimal

@ConfigurationProperties(prefix = "loyalty")
data class LoyaltyProperties(
    val cashbackRate: BigDecimal,
)
