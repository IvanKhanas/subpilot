package com.xeno.subpilot.tgbot.exception

class LoyaltyServiceException(
    message: String,
    cause: Throwable,
) : RuntimeException(message, cause)
