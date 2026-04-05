package com.xeno.subpilot.chat.repository

interface ChatModelPreferenceRepository {
    fun getModel(chatId: Long): String

    fun setModel(
        chatId: Long,
        model: String,
    )
}
