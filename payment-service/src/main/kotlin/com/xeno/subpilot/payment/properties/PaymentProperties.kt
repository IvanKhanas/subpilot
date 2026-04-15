package com.xeno.subpilot.payment.properties

import org.springframework.boot.context.properties.ConfigurationProperties

import java.math.BigDecimal

@ConfigurationProperties(prefix = "payment")
class PaymentProperties {

    data class PaymentPlanProperties(
        val displayName: String,
        val price: BigDecimal,
        val currency: String,
    )

    data class PaymentProperties(
        val plans: Map<String, PaymentPlanProperties>,
    )
}
