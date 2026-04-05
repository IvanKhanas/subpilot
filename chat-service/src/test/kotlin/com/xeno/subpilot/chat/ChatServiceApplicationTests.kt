package com.xeno.subpilot.chat

import com.xeno.subpilot.chat.config.H2TestConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest(
    properties = [
        "spring.liquibase.enabled=false",
        "openai.api-key=test-api-key",
    ],
)
@Import(H2TestConfig::class)
class ChatServiceApplicationTests {
    @Test
    fun contextLoads() = Unit
}
