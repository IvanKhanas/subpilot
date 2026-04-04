package com.xeno.subpilot.tgbot.message

enum class BotResponses(
    val text: String,
) {
    START_RESPONSE(
        """
        Hey, %s! I'm SubPilot — your AI assistant.

        Just send me a message and I'll reply!
        """.trimIndent(),
    ),

    HELP_RESPONSE(
        """
        Available commands:
        /start — start the bot
        /help — show this message

        Just send a message to start an AI chat.
        """.trimIndent(),
    ),

    MAIN_MENU_RESPONSE("Main menu:"),

    CHOOSE_PROVIDER_RESPONSE("Choose a provider:"),

    CHOOSE_MODEL_RESPONSE("Choose a %s model:"),

    MODEL_SET_RESPONSE("Model set to %s."),

    CHAT_PROMPT_RESPONSE("Send me any message and I'll reply!"),

    WAITING_RESPONSE("Please wait while AI processes your request."),

    AI_UNAVAILABLE_RESPONSE(
        "Sorry, the AI service is currently unavailable. Please try again later.",
    ),
    ;

    fun format(vararg args: Any): String = text.format(*args)
}
