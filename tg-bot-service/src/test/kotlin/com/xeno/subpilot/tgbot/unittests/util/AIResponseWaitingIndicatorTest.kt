package com.xeno.subpilot.tgbot.unittests.util

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.util.AIResponseWaitingIndicator
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

import kotlin.test.assertEquals

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class AIResponseWaitingIndicatorTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    private lateinit var indicator: AIResponseWaitingIndicator

    @BeforeEach
    fun setUp() {
        indicator = AIResponseWaitingIndicator(telegramClient)
    }

    @Test
    fun `wrap returns block result when sendMessage returns null`() =
        runTest {
            every { telegramClient.sendMessage(any(), any()) } returns null

            val result = indicator.wrap(chatId = 1L) { "ai response" }

            assertEquals("ai response", result)
        }

    @Test
    fun `wrap sends waiting message with correct chat id`() =
        runTest {
            every { telegramClient.sendMessage(any(), any()) } returns null

            indicator.wrap(chatId = 77L) { "result" }

            verify { telegramClient.sendMessage(77L, BotResponses.WAITING_RESPONSE.text) }
        }

    @Test
    fun `wrap calls block and deletes waiting message on success`() =
        runTest {
            every { telegramClient.sendMessage(any(), any()) } returns 99L
            justRun { telegramClient.editMessage(any(), any(), any()) }
            justRun { telegramClient.deleteMessage(any(), any()) }

            val result = indicator.wrap(chatId = 42L) { "ai response" }

            assertEquals("ai response", result)
            verify { telegramClient.deleteMessage(42L, 99L) }
        }

    @Test
    fun `wrap deletes waiting message even when block throws`() =
        runTest {
            every { telegramClient.sendMessage(any(), any()) } returns 99L
            justRun { telegramClient.editMessage(any(), any(), any()) }
            justRun { telegramClient.deleteMessage(any(), any()) }

            assertThrows<RuntimeException> {
                indicator.wrap(chatId = 42L) { throw RuntimeException("boom") }
            }

            verify { telegramClient.deleteMessage(42L, 99L) }
        }
}
