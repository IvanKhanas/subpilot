package com.xeno.subpilot.payment.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "yookassa")
class Yookassaproperties {
    data class Yookassaproperties(
        val shopId: String,
        val secretKey: String,
        val returnUrl: String,
    )
}
