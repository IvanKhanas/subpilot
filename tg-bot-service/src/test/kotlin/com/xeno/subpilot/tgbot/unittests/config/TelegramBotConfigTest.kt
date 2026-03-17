package com.xeno.subpilot.tgbot.unittests.config

import com.xeno.subpilot.tgbot.config.TelegramBotConfig
import com.xeno.subpilot.tgbot.properties.TelegramBotProperties
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class TelegramBotConfigTest {

    @Test
    fun `creates telegram rest client bean`() {
        val config = TelegramBotConfig()

        val restClient =
            config.telegramRestClient(
                properties = TelegramBotProperties(token = "token", pollingTimeout = 10),
                jsonMapper = JsonMapper.builder().build(),
            )

        assertNotNull(restClient)
    }
}
