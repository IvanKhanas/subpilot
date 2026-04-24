package com.xeno.subpilot.payment.exception

class YooKassaException(
    message: String,
    cause: Throwable,
) : RuntimeException(message, cause)
