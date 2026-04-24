package com.xeno.subpilot.loyalty.exception

import io.grpc.Status

class SubscriptionServiceException(
    message: String,
    cause: Throwable,
) : LoyaltyException(
        status = Status.UNAVAILABLE,
        message = message,
        cause = cause,
    )
