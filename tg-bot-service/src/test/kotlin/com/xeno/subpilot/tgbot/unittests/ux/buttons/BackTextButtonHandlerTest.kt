package com.xeno.subpilot.tgbot.unittests.ux.buttons

import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.ux.BotScreen
import com.xeno.subpilot.tgbot.ux.NavigationService
import com.xeno.subpilot.tgbot.ux.ScreenRenderer
import com.xeno.subpilot.tgbot.ux.buttons.BackTextButtonHandler
import com.xeno.subpilot.tgbot.ux.buttons.BotButtons
import io.mockk.every
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
class BackTextButtonHandlerTest {

    @MockK
    lateinit var navigationService: NavigationService

    @MockK
    lateinit var screenRenderer: ScreenRenderer

    private lateinit var handler: BackTextButtonHandler

    private val chatId = 42L

    @BeforeEach
    fun setUp() {
        justRun { screenRenderer.render(any(), any()) }
        handler = BackTextButtonHandler(navigationService, screenRenderer)
    }

    @Test
    fun `supports returns true for back button text`() {
        assertTrue(handler.supports(BotButtons.BTN_BACK))
    }

    @Test
    fun `supports returns false for other text`() {
        assertFalse(handler.supports("something else"))
    }

    @Test
    fun `handle renders the screen popped from navigation stack`() {
        every { navigationService.pop(chatId) } returns BotScreen.PROVIDER_MENU

        handler.handle(Message(chat = Chat(id = chatId)))

        verify { screenRenderer.render(chatId, BotScreen.PROVIDER_MENU) }
    }

    @Test
    fun `handle renders MAIN_MENU when navigation stack is empty`() {
        every { navigationService.pop(chatId) } returns null

        handler.handle(Message(chat = Chat(id = chatId)))

        verify { screenRenderer.render(chatId, BotScreen.MAIN_MENU) }
    }
}
