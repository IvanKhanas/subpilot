package com.xeno.subpilot.tgbot.unittests

import com.xeno.subpilot.tgbot.TgBotServiceApplication
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class TgBotServiceApplicationTest {

    @Test
    fun `application class can be instantiated`() {
        assertNotNull(TgBotServiceApplication())
    }
}
