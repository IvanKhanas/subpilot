/*
 * Copyright 2024 Ivan Khanas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xeno.subpilot.tgbot.unittests.command

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.command.HelpCommandHandler
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.message.BotResponses
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

import kotlinx.coroutines.runBlocking

@ExtendWith(MockKExtension::class)
class HelpCommandHandlerTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    private lateinit var helpCommandHandler: HelpCommandHandler

    @BeforeEach
    fun setUp() {
        helpCommandHandler = HelpCommandHandler(telegramClient)
        every { telegramClient.sendMessage(any(), any(), any(), any()) } returns null
    }

    @ParameterizedTest(name = "chatId={0}")
    @CsvSource(
        "1",
        "123456789",
    )
    fun `sends help response to message chat id`(chatId: Long) {
        val message = Message(chat = Chat(id = chatId), text = "/help")

        runBlocking { helpCommandHandler.handle(message) }

        verify {
            telegramClient.sendMessage(
                chatId = chatId,
                text = BotResponses.HELP_RESPONSE.text,
            )
        }
    }

    @Test
    fun `calls telegramClient sendMessage exactly once`() {
        val message = Message(chat = Chat(id = 1), text = "/help")

        runBlocking { helpCommandHandler.handle(message) }

        verify(exactly = 1) { telegramClient.sendMessage(any(), any(), any(), any()) }
    }

    @Test
    fun `exposes expected command and description`() {
        assertEquals("/help", helpCommandHandler.command)
        assertEquals("Show available commands", helpCommandHandler.description)
    }
}
