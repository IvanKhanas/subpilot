package com.xeno.subpilot.tgbot.command

enum class CommandResponses(
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
    ;

    fun format(vararg args: Any): String = text.format(*args)
}
