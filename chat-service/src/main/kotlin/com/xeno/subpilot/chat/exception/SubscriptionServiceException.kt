package com.xeno.subpilot.chat.exception

import io.grpc.Status

class SubscriptionServiceException(
    message: String,
    cause: Throwable,
) : ChatException(
        status = Status.UNAVAILABLE,
        message = message,
        cause = cause,
    )
