package com.xeno.subpilot.chat.service

data class ChatTurn(
    val role: Role,
    val content: String,
) {
    enum class Role { USER, ASSISTANT }
}
