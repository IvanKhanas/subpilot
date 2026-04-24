package com.xeno.subpilot.tgbot.unittests.ux.callbacks

import com.xeno.subpilot.tgbot.dto.CallbackQuery
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.User
import com.xeno.subpilot.tgbot.ux.BonusPurchaseService
import com.xeno.subpilot.tgbot.ux.callbacks.DeclineBonusSpendCallbackHandler
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import kotlin.test.assertFalse
import kotlin.test.assertTrue

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class DeclineBonusSpendCallbackHandlerTest {

    @MockK
    lateinit var bonusPurchaseService: BonusPurchaseService

    private lateinit var handler: DeclineBonusSpendCallbackHandler

    @BeforeEach
    fun setUp() {
        handler = DeclineBonusSpendCallbackHandler(bonusPurchaseService)
        coJustRun { bonusPurchaseService.declineBonusSpend(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `supports matches bonus no callback prefix`() {
        assertTrue(handler.supports("bonus_no:openai-basic"))
        assertFalse(handler.supports("bonus_yes:openai-basic:uuid"))
    }

    @Test
    fun `handle delegates parsed values to bonus purchase service`() =
        runTest {
            val callback =
                CallbackQuery(
                    id = "cb-1",
                    from = User(id = 42L),
                    message = Message(messageId = 7L, chat = Chat(id = 100L), text = "prompt-text"),
                    data = "bonus_no:openai-basic",
                )

            handler.handle(callback)

            coVerify {
                bonusPurchaseService.declineBonusSpend(
                    chatId = 100L,
                    userId = 42L,
                    planId = "openai-basic",
                    promptMessageId = 7L,
                    promptText = "prompt-text",
                )
            }
        }

    @Test
    fun `handle does nothing when callback misses required fields`() =
        runTest {
            handler.handle(CallbackQuery(id = "cb-1", data = "bonus_no:openai-basic"))

            coVerify(
                exactly = 0,
            ) { bonusPurchaseService.declineBonusSpend(any(), any(), any(), any(), any()) }
        }
}
