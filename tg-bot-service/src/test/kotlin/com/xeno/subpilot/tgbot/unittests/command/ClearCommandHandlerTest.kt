package com.xeno.subpilot.tgbot.unittests.command

import com.xeno.subpilot.tgbot.client.ChatClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.command.ClearCommandHandler
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.message.BotResponses
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class ClearCommandHandlerTest {

    @MockK
    lateinit var chatClient: ChatClient

    @MockK
    lateinit var telegramClient: TelegramClient

    private lateinit var handler: ClearCommandHandler

    @BeforeEach
    fun setUp() {
        handler = ClearCommandHandler(chatClient, telegramClient)
        coJustRun { chatClient.clearContext(any()) }
        every { telegramClient.sendMessage(any(), any(), any(), any()) } returns 1L
    }

    @Test
    fun `handle clears context for chat id`() =
        runTest {
            handler.handle(Message(chat = Chat(id = 100L), text = "/clear"))

            coVerify { chatClient.clearContext(100L) }
        }

    @Test
    fun `handle sends context cleared confirmation`() =
        runTest {
            handler.handle(Message(chat = Chat(id = 100L), text = "/clear"))

            verify {
                telegramClient.sendMessage(
                    100L,
                    BotResponses.CONTEXT_CLEARED_RESPONSE.text,
                    any(),
                    any(),
                )
            }
        }
}
