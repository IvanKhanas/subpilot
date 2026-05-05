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

import com.xeno.subpilot.tgbot.command.MenuCommandHandler
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.ux.buttons.BotButtons
import com.xeno.subpilot.tgbot.ux.buttons.MenuTextButtonHandler
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

import java.util.stream.Stream

import kotlin.test.assertEquals

import kotlinx.coroutines.test.runTest

@ExtendWith(MockKExtension::class)
class MenuTextButtonHandlerTest {

    @MockK
    lateinit var menuCommandHandler: MenuCommandHandler

    private lateinit var handler: MenuTextButtonHandler

    @BeforeEach
    fun setUp() {
        coJustRun { menuCommandHandler.handle(any()) }
        handler = MenuTextButtonHandler(menuCommandHandler)
    }

    @ParameterizedTest(name = "supports(''{0}'')={1}")
    @MethodSource("supportsCases")
    fun `supports handles main menu button text`(
        text: String,
        expected: Boolean,
    ) {
        assertEquals(expected, handler.supports(text))
    }

    @Test
    fun `handle delegates to menuCommandHandler`() =
        runTest {
            val message = Message(chat = Chat(id = 1L))

            handler.handle(message)

            coVerify { menuCommandHandler.handle(message) }
        }

    companion object {
        @JvmStatic
        fun supportsCases(): Stream<Arguments> =
            Stream.of(
                Arguments.of(BotButtons.BTN_MAIN_MENU, true),
                Arguments.of("something else", false),
            )
    }
}
