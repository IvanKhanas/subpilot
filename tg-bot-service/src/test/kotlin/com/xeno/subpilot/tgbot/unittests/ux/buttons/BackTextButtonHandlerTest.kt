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
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import kotlinx.coroutines.test.runTest

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

    @ParameterizedTest(name = "supports(''{0}'')={1}")
    @MethodSource("supportsCases")
    fun `supports handles back button text`(
        text: String,
        expected: Boolean,
    ) {
        assertEquals(expected, handler.supports(text))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("renderCases")
    fun `handle renders screen from navigation stack or fallback`(
        caseName: String,
        poppedScreen: BotScreen?,
        expectedScreen: BotScreen,
    ) =
        runTest {
            assertTrue(caseName.isNotBlank())
            every { navigationService.pop(chatId) } returns poppedScreen

            handler.handle(Message(chat = Chat(id = chatId)))

            verify { screenRenderer.render(chatId, expectedScreen) }
        }

    companion object {
        @JvmStatic
        fun supportsCases(): Stream<Arguments> =
            Stream.of(
                Arguments.of(BotButtons.BTN_BACK, true),
                Arguments.of("something else", false),
            )

        @JvmStatic
        fun renderCases(): Stream<Arguments> =
            Stream.of(
                Arguments.of("pop returns previous screen", BotScreen.PROVIDER_MENU, BotScreen.PROVIDER_MENU),
                Arguments.of("pop returns null", null, BotScreen.MAIN_MENU),
            )
    }
}
