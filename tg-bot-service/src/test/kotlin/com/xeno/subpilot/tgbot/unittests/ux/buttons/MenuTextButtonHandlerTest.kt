package com.xeno.subpilot.tgbot.unittests.ux.buttons

import com.xeno.subpilot.tgbot.command.MenuCommandHandler
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.ux.buttons.BotButtons
import com.xeno.subpilot.tgbot.ux.buttons.MenuTextButtonHandler
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class MenuTextButtonHandlerTest {

    @MockK
    lateinit var menuCommandHandler: MenuCommandHandler

    private lateinit var handler: MenuTextButtonHandler

    @BeforeEach
    fun setUp() {
        justRun { menuCommandHandler.handle(any()) }
        handler = MenuTextButtonHandler(menuCommandHandler)
    }

    @Test
    fun `supports returns true for main menu button text`() {
        assertTrue(handler.supports(BotButtons.BTN_MAIN_MENU))
    }

    @Test
    fun `supports returns false for other text`() {
        assertFalse(handler.supports("something else"))
    }

    @Test
    fun `handle delegates to menuCommandHandler`() {
        val message = Message(chat = Chat(id = 1L))

        handler.handle(message)

        verify { menuCommandHandler.handle(message) }
    }
}
