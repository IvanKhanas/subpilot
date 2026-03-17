package com.xeno.subpilot.tgbot.unittests.properties

import com.xeno.subpilot.tgbot.properties.TelegramBotProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TelegramBotPropertiesTest {

    @Test
    fun `stores token and polling timeout`() {
        val properties = TelegramBotProperties(token = "abc", pollingTimeout = 25)

        assertEquals("abc", properties.token)
        assertEquals(25, properties.pollingTimeout)
    }
}
