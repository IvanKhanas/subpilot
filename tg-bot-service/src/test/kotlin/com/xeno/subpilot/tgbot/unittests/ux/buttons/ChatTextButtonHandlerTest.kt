/*
 * Copyright 2026 Ivan Khanas
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

import com.xeno.subpilot.tgbot.command.StartCommandHandler
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.ux.buttons.BotButtons
import com.xeno.subpilot.tgbot.ux.buttons.ChatTextButtonHandler
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
class ChatTextButtonHandlerTest {

    @MockK
    lateinit var startCommandHandler: StartCommandHandler

    private lateinit var handler: ChatTextButtonHandler

    @BeforeEach
    fun setUp() {
        handler = ChatTextButtonHandler(startCommandHandler)
        coJustRun { startCommandHandler.registerAndGreet(any()) }
    }

    @ParameterizedTest(name = "supports(''{0}'')={1}")
    @MethodSource("supportsCases")
    fun `supports handles chat button text`(
        text: String,
        expected: Boolean,
    ) {
        assertEquals(expected, handler.supports(text))
    }

    @Test
    fun `handle delegates to registerAndGreet`() =
        runTest {
            val message = Message(chat = Chat(id = 42L), text = BotButtons.BTN_START_CHAT)

            handler.handle(message)

            coVerify { startCommandHandler.registerAndGreet(message) }
        }

    companion object {
        @JvmStatic
        fun supportsCases(): Stream<Arguments> =
            Stream.of(
                Arguments.of(BotButtons.BTN_START_CHAT, true),
                Arguments.of("random text", false),
                Arguments.of("", false),
            )
    }
}
