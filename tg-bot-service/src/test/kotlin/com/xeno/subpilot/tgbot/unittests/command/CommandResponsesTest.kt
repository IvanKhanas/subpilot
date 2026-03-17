package com.xeno.subpilot.tgbot.unittests.command

import com.xeno.subpilot.tgbot.command.CommandResponses
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommandResponsesTest {

    @Test
    fun `format substitutes user name in start response`() {
        val result = CommandResponses.START_RESPONSE.format("Ivan")

        assertTrue(result.contains("Hey, Ivan!"))
    }

    @Test
    fun `help response contains available commands`() {
        val result = CommandResponses.HELP_RESPONSE.text

        assertTrue(result.contains("/start"))
        assertTrue(result.contains("/help"))
    }
}
