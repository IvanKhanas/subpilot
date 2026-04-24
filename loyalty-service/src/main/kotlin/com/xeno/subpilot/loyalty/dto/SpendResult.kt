package com.xeno.subpilot.loyalty.dto

sealed class SpendResult {
    object Success : SpendResult()

    data class Denied(
        val reason: SpendDenialReason,
    ) : SpendResult()
}
