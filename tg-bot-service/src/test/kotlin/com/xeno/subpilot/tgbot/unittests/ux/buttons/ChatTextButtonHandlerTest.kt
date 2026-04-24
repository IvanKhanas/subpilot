package com.xeno.subpilot.tgbot.unittests.ux.buttons

import com.xeno.subpilot.tgbot.command.StartCommandHandler
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.ux.buttons.BotButtons
import com.xeno.subpilot.tgbot.ux.buttons.ChatTextButtonHandler
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class ChatTextButtonHandlerTest {

    @MockK
    lateinit var startCommandHandler: StartCommandHandler

    private lateinit var handler: ChatTextButtonHandler

    @BeforeEach
    fun setUp() {
        handler = ChatTextButtonHandler(startCommandHandler)
        coJustRun { startCommandHandler.registerAndGreet(any()) }
    }

    @Test
    fun `supports returns true for BTN_START_CHAT text`() {
        assertTrue(handler.supports(BotButtons.BTN_START_CHAT))
    }

    @Test
    fun `supports returns false for arbitrary text`() {
        assertFalse(handler.supports("random text"))
    }

    @Test
    fun `supports returns false for empty string`() {
        assertFalse(handler.supports(""))
    }

    @Test
    fun `handle delegates to registerAndGreet`() =
        runTest {
            val message = Message(chat = Chat(id = 42L), text = BotButtons.BTN_START_CHAT)

            handler.handle(message)

            coVerify { startCommandHandler.registerAndGreet(message) }
        }
}
