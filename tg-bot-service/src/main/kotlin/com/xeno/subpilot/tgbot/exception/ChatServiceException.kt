package com.xeno.subpilot.tgbot.exception

class ChatServiceException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
