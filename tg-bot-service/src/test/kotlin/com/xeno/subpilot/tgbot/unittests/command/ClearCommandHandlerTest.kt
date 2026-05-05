/*
 * Copyright 2026 Ivan Khanas
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
import net.datafaker.Faker
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

    private val faker = Faker()

    private lateinit var handler: ClearCommandHandler
    private var chatId: Long = 0L
    private var sentMessageId: Long = 0L

    @BeforeEach
    fun setUp() {
        chatId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        sentMessageId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        handler = ClearCommandHandler(chatClient, telegramClient)
        coJustRun { chatClient.clearContext(any()) }
        every { telegramClient.sendMessage(any(), any(), any(), any()) } returns sentMessageId
    }

    @Test
    fun `handle clears context for chat id`() =
        runTest {
            handler.handle(Message(chat = Chat(id = chatId), text = "/clear"))

            coVerify { chatClient.clearContext(chatId) }
        }

    @Test
    fun `handle sends context cleared confirmation`() =
        runTest {
            handler.handle(Message(chat = Chat(id = chatId), text = "/clear"))

            verify {
                telegramClient.sendMessage(
                    chatId,
                    BotResponses.CONTEXT_CLEARED_RESPONSE.text,
                    any(),
                    any(),
                )
            }
        }
}
