package com.xeno.subpilot.tgbot.dto

import com.xeno.subpilot.tgbot.ux.AiProvider

data class RegistrationResult(
    val isNew: Boolean,
    val freeProvider: AiProvider,
    val freeQuota: Int,
)
