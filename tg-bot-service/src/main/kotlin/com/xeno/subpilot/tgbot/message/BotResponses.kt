package com.xeno.subpilot.tgbot.message

enum class BotResponses(
    val text: String,
) {
    START_NEW_USER_RESPONSE(
        """
        Hey, %s! I'm SubPilot — your AI assistant.
        You've got %d free requests for %s provider to start.
        Just send me a message and I'll reply!
        """.trimIndent(),
    ),

    START_ALREADY_REGISTERED_USER_RESPONSE(
        """
        Welcome back, %s! Just send me a message and I'll reply!
        """.trimIndent(),
    ),

    HELP_RESPONSE(
        """
        Available commands:
        /start — start the bot
        /help — show this message
        /support - write to a support specialist

        Just send a message to start an AI chat.
        """.trimIndent(),
    ),

    SUPPORT_RESPONSE(
        """
        Have some problem?
        Please write to a support specialist: %s
        """.trimIndent(),
    ),

    UNKNOWN_COMMAND_RESPONSE(
        """
        Unknown command.
        Use /help to list all supported commands.
        """.trimIndent(),
    ),

    MAIN_MENU_RESPONSE("Main menu:"),

    CHOOSE_PROVIDER_RESPONSE("Choose a provider:"),

    CHOOSE_MODEL_RESPONSE("Choose a %s model:"),

    MODEL_SET_RESPONSE("Model set to %s."),

    WAITING_RESPONSE("Please wait while AI processes your request."),

    AI_UNAVAILABLE_RESPONSE(
        "Sorry, the AI service is currently unavailable. Please try again later.",
    ),

    QUOTA_EXCEEDED_RESPONSE(
        "You've used all your free requests. Subscribe to continue using the bot.",
    ),

    NO_SUBSCRIPTION_RESPONSE(
        "You have %d requests on your %s balance. %s costs %d requests per message. To top up: /premium",
    ),

    ACCESS_BLOCKED_RESPONSE(
        "Your access has been restricted. Please contact support.",
    ),

    FREE_QUOTA_EXHAUSTED_RESPONSE(
        "You have 0 free requests left for %s. Renewal on %s.",
    ),

    MODEL_SET_FAILED_RESPONSE(
        "Failed to set model. Please try again later.",
    ),

    MODEL_COMMAND_USAGE_RESPONSE(
        "Usage: /model <model_id>\n\nAvailable models:\n\n%s",
    ),

    MODEL_NOT_FOUND_RESPONSE(
        "Unknown model: %s\n\nAvailable models:\n%s",
    ),
    ;

    fun format(vararg args: Any): String = text.format(*args)
}
