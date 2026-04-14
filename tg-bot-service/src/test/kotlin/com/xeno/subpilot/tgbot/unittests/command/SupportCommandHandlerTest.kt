package com.xeno.subpilot.tgbot.unittests.command

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.command.SupportCommandHandler
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.properties.SupportProperties
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SupportCommandHandlerTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    private lateinit var handler: SupportCommandHandler

    private val chatId = 100L
    private val operatorTag = "@support_operator"

    @BeforeEach
    fun setUp() {
        handler = SupportCommandHandler(telegramClient, SupportProperties(operatorTag))
        every { telegramClient.sendMessage(any(), any()) } returns null
    }

    @Test
    fun `handle sends message containing operator tag`() {
        val message = Message(messageId = 1L, chat = Chat(id = chatId), text = "/support")

        handler.handle(message)

        verify { telegramClient.sendMessage(chatId, match { it.contains(operatorTag) }) }
    }
}
