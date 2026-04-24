package com.xeno.subpilot.tgbot.unittests.ux.buttons

import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.ux.BotScreen
import com.xeno.subpilot.tgbot.ux.NavigationService
import com.xeno.subpilot.tgbot.ux.ScreenRenderer
import com.xeno.subpilot.tgbot.ux.buttons.BotButtons
import com.xeno.subpilot.tgbot.ux.buttons.ChooseProviderTextButtonHandler
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import kotlin.test.assertFalse
import kotlin.test.assertTrue

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class ChooseProviderTextButtonHandlerTest {

    @MockK
    lateinit var navigationService: NavigationService

    @MockK
    lateinit var screenRenderer: ScreenRenderer

    private lateinit var handler: ChooseProviderTextButtonHandler

    private val chatId = 42L

    @BeforeEach
    fun setUp() {
        justRun { navigationService.push(any(), any()) }
        justRun { screenRenderer.render(any(), any()) }
        handler = ChooseProviderTextButtonHandler(navigationService, screenRenderer)
    }

    @Test
    fun `supports returns true for choose model button text`() {
        assertTrue(handler.supports(BotButtons.BTN_CHOOSE_MODEL))
    }

    @Test
    fun `supports returns false for other text`() {
        assertFalse(handler.supports("something else"))
    }

    @Test
    fun `handle pushes MAIN_MENU to navigation stack`() =
        runTest {
            handler.handle(Message(chat = Chat(id = chatId)))

            verify { navigationService.push(chatId, BotScreen.MAIN_MENU) }
        }

    @Test
    fun `handle renders PROVIDER_MENU screen`() =
        runTest {
            handler.handle(Message(chat = Chat(id = chatId)))

            verify { screenRenderer.render(chatId, BotScreen.PROVIDER_MENU) }
        }
}
