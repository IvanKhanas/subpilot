package com.xeno.subpilot.tgbot.exception

class SubscriptionServiceException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
