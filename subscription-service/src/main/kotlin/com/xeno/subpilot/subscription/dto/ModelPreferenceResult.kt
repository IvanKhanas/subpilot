package com.xeno.subpilot.subscription.dto

data class ModelPreferenceResult(
    val providerChanged: Boolean,
    val modelCost: Int,
    val provider: String,
)
