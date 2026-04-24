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

import kotlinx.coroutines.test.runTest

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
    fun `handle clears navigation stack for the message chat`() =
        runTest {
            handler.handle(Message(chat = Chat(id = chatId)))

            verify { navigationService.clear(chatId) }
        }

    @Test
    fun `handle renders MAIN_MENU for the message chat`() =
        runTest {
            handler.handle(Message(chat = Chat(id = chatId)))

            verify { screenRenderer.render(chatId, BotScreen.MAIN_MENU) }
        }

    @Test
    fun `exposes correct command and description`() {
        assertEquals("/menu", handler.command)
        assertEquals("Show main menu", handler.description)
    }
}
