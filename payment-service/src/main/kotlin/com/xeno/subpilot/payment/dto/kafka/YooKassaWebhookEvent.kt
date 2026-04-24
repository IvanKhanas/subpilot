package com.xeno.subpilot.payment.dto.kafka

import com.fasterxml.jackson.annotation.JsonProperty

data class YooKassaWebhookEvent(
    val event: String,

    @JsonProperty("object")
    val payment: YooKassaWebhookPayment,
)
