package com.xeno.subpilot.tgbot.exception

class PaymentServiceException(
    message: String,
    cause: Throwable,
) : RuntimeException(message, cause)
