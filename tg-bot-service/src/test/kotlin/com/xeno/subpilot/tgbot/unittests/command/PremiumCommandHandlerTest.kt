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

import com.xeno.subpilot.tgbot.command.PremiumCommandHandler
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.ux.BotScreen
import com.xeno.subpilot.tgbot.ux.NavigationService
import com.xeno.subpilot.tgbot.ux.ScreenRenderer
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class PremiumCommandHandlerTest {

    @MockK
    lateinit var navigationService: NavigationService

    @MockK
    lateinit var screenRenderer: ScreenRenderer

    private val faker = Faker()

    private lateinit var handler: PremiumCommandHandler
    private var chatId: Long = 0L

    @BeforeEach
    fun setUp() {
        chatId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        handler = PremiumCommandHandler(navigationService, screenRenderer)
        justRun { navigationService.clear(any()) }
        justRun { screenRenderer.render(any(), any()) }
    }

    @Test
    fun `handle clears navigation stack before rendering premium menu`() =
        runTest {
            handler.handle(Message(chat = Chat(id = chatId), text = "/premium"))

            verify { navigationService.clear(chatId) }
        }

    @Test
    fun `handle renders premium menu screen`() =
        runTest {
            handler.handle(Message(chat = Chat(id = chatId), text = "/premium"))

            verify { screenRenderer.render(chatId, BotScreen.PREMIUM_MENU) }
        }
}
