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
package com.xeno.subpilot.tgbot.unittests.util

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.util.AIResponseWaitingIndicator
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

import kotlin.test.assertEquals
import kotlin.test.assertTrue

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class AIResponseWaitingIndicatorTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    private val faker = Faker()

    private lateinit var indicator: AIResponseWaitingIndicator
    private var chatId: Long = 0L
    private var waitingMessageId: Long = 0L

    @BeforeEach
    fun setUp() {
        chatId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        waitingMessageId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        indicator = AIResponseWaitingIndicator(telegramClient)
    }

    @Test
    fun `wrap returns block result when sendMessage returns null`() =
        runTest {
            every { telegramClient.sendMessage(any(), any()) } returns null

            val result = indicator.wrap(chatId = chatId) { "ai response" }

            assertEquals("ai response", result)
        }

    @Test
    fun `wrap sends waiting message with correct chat id`() =
        runTest {
            every { telegramClient.sendMessage(any(), any()) } returns null

            indicator.wrap(chatId = chatId) { "result" }

            verify { telegramClient.sendMessage(chatId, BotResponses.WAITING_RESPONSE.text) }
        }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        "success path, false",
        "exception path, true",
    )
    fun `wrap deletes waiting message when waiting message exists`(
        caseName: String,
        blockThrows: Boolean,
    ) = runTest {
        assertTrue(caseName.isNotBlank())
        every { telegramClient.sendMessage(any(), any()) } returns waitingMessageId
        justRun { telegramClient.editMessage(any(), any(), any()) }
        justRun { telegramClient.deleteMessage(any(), any()) }

        if (blockThrows) {
            assertThrows<RuntimeException> {
                indicator.wrap(chatId = chatId) { throw RuntimeException("boom") }
            }
        } else {
            val result = indicator.wrap(chatId = chatId) { "ai response" }
            assertEquals("ai response", result)
        }

        verify { telegramClient.deleteMessage(chatId, waitingMessageId) }
    }
}
