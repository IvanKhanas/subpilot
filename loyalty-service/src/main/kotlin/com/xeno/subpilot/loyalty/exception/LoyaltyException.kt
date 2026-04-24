package com.xeno.subpilot.loyalty.exception

import io.grpc.Status

open class LoyaltyException(
    val status: Status,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
