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
package com.xeno.subpilot.tgbot.unittests.grpc

import com.xeno.subpilot.proto.moderation.v1.FlaggedPromptNotification
import com.xeno.subpilot.proto.moderation.v1.NotifyResponse
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.grpc.ModerationGrpcService
import com.xeno.subpilot.tgbot.properties.ModerationProperties
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import kotlin.test.assertEquals
import kotlin.test.assertTrue

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class ModerationGrpcServiceTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    private val faker = Faker()

    private lateinit var service: ModerationGrpcService
    private var userId: Long = 0L
    private var sourceChatId: Long = 0L
    private var sentMessageId: Long = 0L

    companion object {
        const val MODERATION_CHAT_ID = -1_000_000_001L
        const val SHORT_PROMPT = "unsafe text"
        const val MAX_PREVIEW_LENGTH = 200
    }

    @BeforeEach
    fun setUp() {
        userId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        sourceChatId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        sentMessageId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        service =
            ModerationGrpcService(
                telegramClient = telegramClient,
                properties = ModerationProperties(chatId = MODERATION_CHAT_ID),
                ioDispatcher = UnconfinedTestDispatcher(),
            )
        every { telegramClient.sendMessage(any(), any(), any(), any()) } returns sentMessageId
    }

    @Test
    fun `notifyFlaggedPrompt sends formatted moderation alert to configured chat`() =
        runTest {
            val request = request(promptText = SHORT_PROMPT)

            val response = service.notifyFlaggedPrompt(request)

            assertEquals(NotifyResponse.getDefaultInstance(), response)
            verify {
                telegramClient.sendMessage(
                    MODERATION_CHAT_ID,
                    """
                    ⚠️ Flagged prompt detected
                    User: $userId  Chat: $sourceChatId
                    Categories: hate, violence
                    Text: "$SHORT_PROMPT"
                    """.trimIndent(),
                )
            }
        }

    @Test
    fun `notifyFlaggedPrompt truncates prompt preview to max length with ellipsis`() =
        runTest {
            val longPrompt = "x".repeat(MAX_PREVIEW_LENGTH + 25)
            val textSlot = slot<String>()
            every {
                telegramClient.sendMessage(MODERATION_CHAT_ID, capture(textSlot), any(), any())
            } returns sentMessageId

            service.notifyFlaggedPrompt(request(promptText = longPrompt))

            val expectedPreview = "${"x".repeat(MAX_PREVIEW_LENGTH)}…"
            assertTrue(textSlot.captured.contains("Text: \"$expectedPreview\""))
            verify(
                exactly = 1,
            ) { telegramClient.sendMessage(MODERATION_CHAT_ID, any(), any(), any()) }
        }

    private fun request(promptText: String): FlaggedPromptNotification =
        FlaggedPromptNotification
            .newBuilder()
            .setUserId(userId)
            .setChatId(sourceChatId)
            .setPromptText(promptText)
            .addAllFlaggedCategories(listOf("hate", "violence"))
            .build()
}
