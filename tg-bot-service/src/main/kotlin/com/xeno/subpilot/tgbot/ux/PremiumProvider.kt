package com.xeno.subpilot.tgbot.ux

enum class PremiumProvider(
    val displayName: String,
    val planProviderKey: String,
) {
    OPENAI("֎ OpenAI", "openai"),
    ;

    companion object {
        fun findByDisplayName(text: String) = entries.find { it.displayName == text }

        fun findByKey(key: String) = entries.find { it.planProviderKey == key }
    }
}
