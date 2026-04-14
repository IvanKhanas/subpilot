package com.xeno.subpilot.chat.exception

import io.grpc.Status

class ChatHistoryException(
    message: String,
    cause: Throwable? = null,
) : ChatException(Status.INTERNAL, message, cause)
