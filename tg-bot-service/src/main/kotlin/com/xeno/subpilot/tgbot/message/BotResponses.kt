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
        /balance — show your request balance
        /bonus — show your bonus points
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

    MODEL_SET_RESPONSE("Model set to %s.\n%s costs %d requests to %s provider per message."),

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

    CHOOSE_PREMIUM_PROVIDER_RESPONSE("Choose subscription provider:"),

    PLAN_LIST_RESPONSE("%s plans:\n\n%s"),

    PAYMENT_LINK_RESPONSE("Pay here to activate your subscription:\n%s"),

    PAYMENT_FAILED_RESPONSE("Failed to create payment. Please try again later."),

    BALANCE_RESPONSE(
        """
        Free quota requests:
        %s

        Paid quota requests:
        %s
        """.trimIndent(),
    ),

    BALANCE_FREE_ENTRY_RESPONSE("• %s: %d"),

    BALANCE_FREE_RESET_ENTRY_RESPONSE("• %s: 0 (resets at %s)"),

    BALANCE_PAID_ENTRY_RESPONSE("• %s: %d"),

    BALANCE_PAID_EMPTY_ENTRY_RESPONSE("• %s: 0"),

    BALANCE_TOP_UP_RESPONSE("Top up your balance: /premium"),

    CONTEXT_CLEARED_RESPONSE("Context cleared. Starting a fresh conversation."),

    SUBSCRIPTION_ACTIVATED_RESPONSE(
        """
        Subscription activated!
        Plan: %s
        %s
        """.trimIndent(),
    ),

    BONUS_BALANCE_RESPONSE("You have %d bonus points."),

    BONUS_PROMPT_RESPONSE(
        "You have %d bonus points. Use them to get this subscription for free?",
    ),

    BONUS_PARTIAL_PROMPT_RESPONSE(
        "You have %d bonus points. Apply a %d₽ discount and pay %d₽?",
    ),

    BONUS_SPEND_SUCCESS_RESPONSE("Subscription activated with bonus points!"),

    BONUS_SPEND_FAILED_RESPONSE(
        "Failed to apply bonus points. Please try again later.",
    ),
    ;

    fun format(vararg args: Any): String = text.format(*args)
}
