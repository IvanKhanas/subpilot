package com.xeno.subpilot.chat.exception

import io.grpc.Status

open class ChatException(
    val status: Status,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
