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
package com.xeno.subpilot.tgbot.unittests.ux

import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.ux.BotScreen
import com.xeno.subpilot.tgbot.ux.ScreenRenderer
import com.xeno.subpilot.tgbot.ux.buttons.BotButtons
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class ScreenRendererTest {

    @MockK
    private lateinit var telegramClient: TelegramClient

    private lateinit var screenRenderer: ScreenRenderer

    private val chatId = 42L

    @BeforeEach
    fun setUp() {
        every {
            telegramClient.sendMessage(
                any(),
                any(),
                any(),
                any(),
            )
        } returns null

        screenRenderer = ScreenRenderer(telegramClient)
    }

    @Test
    fun `render MAIN_MENU sends main menu message with keyboard`() {
        screenRenderer.render(chatId, BotScreen.MAIN_MENU)

        verify {
            telegramClient.sendMessage(
                chatId = chatId,
                text = BotResponses.MAIN_MENU_RESPONSE.text,
                replyMarkup = BotButtons.mainMenu,
                parseMode = null,
            )
        }
    }

    @Test
    fun `render PROVIDER_MENU sends provider menu message with keyboard`() {
        screenRenderer.render(chatId, BotScreen.PROVIDER_MENU)

        verify {
            telegramClient.sendMessage(
                chatId = chatId,
                text = BotResponses.CHOOSE_PROVIDER_RESPONSE.text,
                replyMarkup = BotButtons.providerMenu,
                parseMode = null,
            )
        }
    }
}
