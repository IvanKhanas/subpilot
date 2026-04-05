package com.xeno.subpilot.chat.exception

import io.grpc.Status

class OpenAiException(
    message: String,
    cause: Throwable? = null,
) : ChatException(Status.UNAVAILABLE, message, cause)
