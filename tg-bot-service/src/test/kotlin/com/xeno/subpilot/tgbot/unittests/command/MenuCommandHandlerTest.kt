package com.xeno.subpilot.tgbot.unittests.command

import com.xeno.subpilot.tgbot.command.MenuCommandHandler
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.ux.BotScreen
import com.xeno.subpilot.tgbot.ux.NavigationService
import com.xeno.subpilot.tgbot.ux.ScreenRenderer
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class MenuCommandHandlerTest {

    @MockK
    lateinit var navigationService: NavigationService

    @MockK
    lateinit var screenRenderer: ScreenRenderer

    private lateinit var handler: MenuCommandHandler

    private val chatId = 42L

    @BeforeEach
    fun setUp() {
        justRun { navigationService.clear(any()) }
        justRun { screenRenderer.render(any(), any()) }
        handler = MenuCommandHandler(navigationService, screenRenderer)
    }

    @Test
    fun `handle clears navigation stack for the message chat`() {
        handler.handle(Message(chat = Chat(id = chatId)))

        verify { navigationService.clear(chatId) }
    }

    @Test
    fun `handle renders MAIN_MENU for the message chat`() {
        handler.handle(Message(chat = Chat(id = chatId)))

        verify { screenRenderer.render(chatId, BotScreen.MAIN_MENU) }
    }

    @Test
    fun `exposes correct command and description`() {
        assertEquals("/menu", handler.command)
        assertEquals("Show main menu", handler.description)
    }
}
