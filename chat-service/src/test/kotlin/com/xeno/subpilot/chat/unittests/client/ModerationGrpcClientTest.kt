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
package com.xeno.subpilot.chat.unittests.client

import com.xeno.subpilot.chat.client.ModerationGrpcClient
import com.xeno.subpilot.proto.moderation.v1.FlaggedPromptNotification
import com.xeno.subpilot.proto.moderation.v1.ModerationServiceGrpcKt
import com.xeno.subpilot.proto.moderation.v1.NotifyResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import kotlin.test.assertEquals

import kotlinx.coroutines.runBlocking

@ExtendWith(MockKExtension::class)
class ModerationGrpcClientTest {

    @MockK
    lateinit var stub: ModerationServiceGrpcKt.ModerationServiceCoroutineStub

    private lateinit var client: ModerationGrpcClient

    companion object {
        const val USER_ID = 42L
        const val CHAT_ID = 99L
        const val PROMPT = "unsafe prompt"
        val CATEGORIES = listOf("violence", "self-harm")
    }

    @BeforeEach
    fun setUp() {
        client = ModerationGrpcClient(stub)
    }

    @Test
    fun `notifyFlagged sends mapped notification request to gRPC stub`() {
        val requestSlot = slot<FlaggedPromptNotification>()
        coEvery { stub.notifyFlaggedPrompt(capture(requestSlot), any()) } returns
            NotifyResponse.getDefaultInstance()

        runBlocking {
            client.notifyFlagged(
                userId = USER_ID,
                chatId = CHAT_ID,
                promptText = PROMPT,
                categories = CATEGORIES,
            )
        }

        assertEquals(USER_ID, requestSlot.captured.userId)
        assertEquals(CHAT_ID, requestSlot.captured.chatId)
        assertEquals(PROMPT, requestSlot.captured.promptText)
        assertEquals(CATEGORIES, requestSlot.captured.flaggedCategoriesList)
        coVerify(exactly = 1) { stub.notifyFlaggedPrompt(any(), any()) }
    }
}
