package com.xeno.subpilot.tgbot.dto

data class ModelPreferenceResult(
    val providerChanged: Boolean,
    val modelCost: Int,
    val provider: String,
)
