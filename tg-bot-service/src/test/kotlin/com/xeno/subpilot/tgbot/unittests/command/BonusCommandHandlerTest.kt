package com.xeno.subpilot.tgbot.unittests.command

import com.xeno.subpilot.tgbot.client.LoyaltyClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.command.BonusCommandHandler
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.User
import com.xeno.subpilot.tgbot.exception.LoyaltyServiceException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import kotlin.test.assertEquals

import kotlinx.coroutines.runBlocking

@ExtendWith(MockKExtension::class)
class BonusCommandHandlerTest {

    @MockK
    lateinit var loyaltyClient: LoyaltyClient

    @MockK
    lateinit var telegramClient: TelegramClient

    private lateinit var handler: BonusCommandHandler

    @BeforeEach
    fun setUp() {
        handler = BonusCommandHandler(loyaltyClient, telegramClient)
        every { telegramClient.sendMessage(any(), any(), any(), any()) } returns 1L
    }

    @Test
    fun `handle sends bonus balance when loyalty client succeeds`() {
        coEvery { loyaltyClient.getBalance(42L) } returns 150
        val textSlot = io.mockk.slot<String>()
        every { telegramClient.sendMessage(100L, capture(textSlot), any(), any()) } returns 1L

        runBlocking {
            handler.handle(
                Message(
                    chat = Chat(id = 100L),
                    from = User(id = 42L),
                    text = "/bonus",
                ),
            )
        }

        assertEquals("You have 150 bonus points.", textSlot.captured)
        coVerify { loyaltyClient.getBalance(42L) }
    }

    @Test
    fun `handle sends failure message when loyalty lookup fails`() {
        coEvery { loyaltyClient.getBalance(42L) } throws
            LoyaltyServiceException("down", RuntimeException())

        runBlocking {
            handler.handle(
                Message(
                    chat = Chat(id = 100L),
                    from = User(id = 42L),
                    text = "/bonus",
                ),
            )
        }

        verify {
            telegramClient.sendMessage(
                100L,
                match {
                    it.contains("Failed to get bonus balance")
                },
                any(),
                any(),
            )
        }
    }

    @Test
    fun `handle ignores command when message has no user`() {
        runBlocking {
            handler.handle(
                Message(
                    chat = Chat(id = 100L),
                    from = null,
                    text = "/bonus",
                ),
            )
        }

        coVerify(exactly = 0) { loyaltyClient.getBalance(any()) }
        verify(exactly = 0) { telegramClient.sendMessage(any(), any(), any(), any()) }
    }
}
