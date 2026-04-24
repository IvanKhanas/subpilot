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

import kotlinx.coroutines.test.runTest

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
    fun `handle sends message containing operator tag`() =
        runTest {
            val message = Message(messageId = 1L, chat = Chat(id = chatId), text = "/support")

            handler.handle(message)

            verify { telegramClient.sendMessage(chatId, match { it.contains(operatorTag) }) }
        }
}
