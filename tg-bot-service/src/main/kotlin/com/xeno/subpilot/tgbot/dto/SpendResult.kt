package com.xeno.subpilot.tgbot.dto

sealed class SpendResult {
    object Success : SpendResult()

    data class Denied(
        val reason: SpendDenialReason,
    ) : SpendResult()
}
