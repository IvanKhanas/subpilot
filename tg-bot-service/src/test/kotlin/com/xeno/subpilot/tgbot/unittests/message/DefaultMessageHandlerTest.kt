package com.xeno.subpilot.tgbot.unittests.message

import com.xeno.subpilot.proto.chat.v1.ProcessMessageResponse
import com.xeno.subpilot.proto.subscription.v1.DenialReason
import com.xeno.subpilot.tgbot.client.ChatClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.User
import com.xeno.subpilot.tgbot.exception.ChatServiceException
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.message.DefaultMessageHandler
import com.xeno.subpilot.tgbot.util.AIResponseWaitingIndicator
import com.xeno.subpilot.tgbot.ux.AiProvider
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class DefaultMessageHandlerTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    @MockK
    lateinit var chatClient: ChatClient

    @MockK
    lateinit var waitingIndicator: AIResponseWaitingIndicator

    private lateinit var handler: DefaultMessageHandler

    private val blockSlot = slot<() -> ProcessMessageResponse>()

    @BeforeEach
    fun setUp() {
        handler = DefaultMessageHandler(telegramClient, chatClient, waitingIndicator)
    }

    private fun successResponse(text: String): ProcessMessageResponse =
        ProcessMessageResponse.newBuilder().setText(text).build()

    private fun deniedResponse(
        reason: DenialReason,
        modelId: String = "",
    ): ProcessMessageResponse =
        ProcessMessageResponse
            .newBuilder()
            .setDenialReason(reason)
            .setModelId(modelId)
            .build()

    @Test
    fun `forwards message to chat service and sends response to user`() {
        val message = Message(chat = Chat(id = 42), from = User(id = 7), text = "Hello")
        every { waitingIndicator.wrap(any(), capture(blockSlot)) } answers { blockSlot.captured() }
        every { chatClient.processMessage(7L, 42L, "Hello") } returns successResponse("AI response")
        every { telegramClient.sendMessage(any(), any(), any(), any()) } returns null

        handler.handle(message)

        verify(exactly = 1) {
            telegramClient.sendMessage(
                chatId = 42,
                text = "AI response",
                replyMarkup = null,
                parseMode = "HTML",
            )
        }
    }

    @Test
    fun `sends AI unavailable response when chat service throws ChatServiceException`() {
        val message = Message(chat = Chat(id = 42), from = User(id = 7), text = "Hello")
        every { waitingIndicator.wrap(any(), capture(blockSlot)) } answers { blockSlot.captured() }
        every { chatClient.processMessage(7L, 42L, "Hello") } throws
            ChatServiceException("unavailable", RuntimeException())
        every { telegramClient.sendMessage(any(), any(), any(), any()) } returns null

        handler.handle(message)

        verify(exactly = 1) {
            telegramClient.sendMessage(
                chatId = 42,
                text = BotResponses.AI_UNAVAILABLE_RESPONSE.text,
            )
        }
    }

    @Test
    fun `sends quota exceeded response when access denied with QUOTA_EXHAUSTED`() {
        val message = Message(chat = Chat(id = 42), from = User(id = 7), text = "Hello")
        every { waitingIndicator.wrap(any(), capture(blockSlot)) } answers { blockSlot.captured() }
        every { chatClient.processMessage(7L, 42L, "Hello") } returns
            deniedResponse(DenialReason.QUOTA_EXHAUSTED)
        every { telegramClient.sendMessage(any(), any(), any(), any()) } returns null

        handler.handle(message)

        verify(exactly = 1) {
            telegramClient.sendMessage(
                chatId = 42,
                text = BotResponses.QUOTA_EXCEEDED_RESPONSE.text,
            )
        }
    }

    @Test
    fun `sends no subscription response with model name when access denied with NO_SUBSCRIPTION`() {
        val message = Message(chat = Chat(id = 42), from = User(id = 7), text = "Hello")
        every { waitingIndicator.wrap(any(), capture(blockSlot)) } answers { blockSlot.captured() }
        every { chatClient.processMessage(7L, 42L, "Hello") } returns
            deniedResponse(DenialReason.NO_SUBSCRIPTION, modelId = "gpt-4o-mini")
        every { telegramClient.sendMessage(any(), any(), any(), any()) } returns null

        handler.handle(message)

        val model = AiProvider.findModelById("gpt-4o-mini")!!
        val provider = AiProvider.findProviderByModelId("gpt-4o-mini")!!
        verify(exactly = 1) {
            telegramClient.sendMessage(
                chatId = 42,
                text =
                    BotResponses.NO_SUBSCRIPTION_RESPONSE.format(
                        0,
                        provider.displayName,
                        model.displayName,
                        0,
                    ),
            )
        }
    }

    @Test
    fun `sends access blocked response when access denied with BLOCKED`() {
        val message = Message(chat = Chat(id = 42), from = User(id = 7), text = "Hello")
        every { waitingIndicator.wrap(any(), capture(blockSlot)) } answers { blockSlot.captured() }
        every { chatClient.processMessage(7L, 42L, "Hello") } returns
            deniedResponse(DenialReason.BLOCKED)
        every { telegramClient.sendMessage(any(), any(), any(), any()) } returns null

        handler.handle(message)

        verify(exactly = 1) {
            telegramClient.sendMessage(
                chatId = 42,
                text = BotResponses.ACCESS_BLOCKED_RESPONSE.text,
            )
        }
    }

    @Test
    fun `ignores message without text`() {
        val message = Message(chat = Chat(id = 42), from = User(id = 7), text = null)

        handler.handle(message)

        verify(exactly = 0) { telegramClient.sendMessage(any(), any(), any(), any()) }
        verify(exactly = 0) { chatClient.processMessage(any(), any(), any()) }
    }

    @Test
    fun `ignores message without from user`() {
        val message = Message(chat = Chat(id = 42), from = null, text = "Hello")

        handler.handle(message)

        verify(exactly = 0) { telegramClient.sendMessage(any(), any(), any(), any()) }
        verify(exactly = 0) { chatClient.processMessage(any(), any(), any()) }
    }
}
