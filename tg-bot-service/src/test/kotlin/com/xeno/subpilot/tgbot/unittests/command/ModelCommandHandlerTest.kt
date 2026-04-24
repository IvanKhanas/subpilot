package com.xeno.subpilot.tgbot.unittests.command

import com.xeno.subpilot.tgbot.client.ChatClient
import com.xeno.subpilot.tgbot.client.SubscriptionClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.command.ModelCommandHandler
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.ModelPreferenceResult
import com.xeno.subpilot.tgbot.dto.User
import com.xeno.subpilot.tgbot.exception.SubscriptionServiceException
import com.xeno.subpilot.tgbot.message.BotResponses
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import kotlinx.coroutines.runBlocking

@ExtendWith(MockKExtension::class)
class ModelCommandHandlerTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    @MockK
    lateinit var subscriptionClient: SubscriptionClient

    @MockK
    lateinit var chatClient: ChatClient

    private lateinit var handler: ModelCommandHandler

    private val chatId = 100L
    private val userId = 42L
    private val noChange =
        ModelPreferenceResult(providerChanged = false, modelCost = 10, provider = "openai")
    private val providerChanged =
        ModelPreferenceResult(providerChanged = true, modelCost = 10, provider = "openai")

    @BeforeEach
    fun setUp() {
        handler = ModelCommandHandler(telegramClient, subscriptionClient, chatClient)
        every { telegramClient.sendMessage(any(), any()) } returns null
    }

    private fun message(text: String) =
        Message(
            messageId = 1L,
            from = User(id = userId),
            chat = Chat(id = chatId),
            text = text,
        )

    @Test
    fun `handle sends usage when no model arg provided`() {
        runBlocking { handler.handle(message("/model")) }

        verify { telegramClient.sendMessage(chatId, match { it.startsWith("Usage:") }) }
    }

    @Test
    fun `handle sends usage when too many args provided`() {
        runBlocking { handler.handle(message("/model gpt-4o extra")) }

        verify { telegramClient.sendMessage(chatId, match { it.startsWith("Usage:") }) }
    }

    @Test
    fun `handle sends not found when model id is unknown`() {
        runBlocking { handler.handle(message("/model unknown-model")) }

        verify { telegramClient.sendMessage(chatId, match { it.contains("Unknown model") }) }
    }

    @Test
    fun `handle sends confirmation after setting model`() {
        coEvery { subscriptionClient.setModelPreference(userId, "gpt-4o") } returns noChange

        runBlocking { handler.handle(message("/model gpt-4o")) }

        verify { telegramClient.sendMessage(chatId, match { it.contains("GPT-4o") }) }
    }

    @Test
    fun `handle clears context when provider changes`() {
        coEvery { subscriptionClient.setModelPreference(userId, "gpt-4o") } returns providerChanged
        coJustRun { chatClient.clearContext(chatId) }

        runBlocking { handler.handle(message("/model gpt-4o")) }

        coVerify { chatClient.clearContext(chatId) }
    }

    @Test
    fun `handle does not clear context when provider is unchanged`() {
        coEvery { subscriptionClient.setModelPreference(userId, "gpt-4o") } returns noChange

        runBlocking { handler.handle(message("/model gpt-4o")) }

        coVerify(exactly = 0) { chatClient.clearContext(any()) }
    }

    @Test
    fun `handle sends failure message when subscription service throws`() {
        coEvery { subscriptionClient.setModelPreference(any(), any()) } throws
            SubscriptionServiceException("failed")

        runBlocking { handler.handle(message("/model gpt-4o")) }

        verify { telegramClient.sendMessage(chatId, BotResponses.MODEL_SET_FAILED_RESPONSE.text) }
    }
}
