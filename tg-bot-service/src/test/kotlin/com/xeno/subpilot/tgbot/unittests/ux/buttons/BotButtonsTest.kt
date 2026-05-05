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

import com.xeno.subpilot.tgbot.dto.KeyboardButton
import com.xeno.subpilot.tgbot.ux.AiProvider
import com.xeno.subpilot.tgbot.ux.buttons.BotButtons
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

import java.util.stream.Stream

class BotButtonsTest {

    @ParameterizedTest(name = "mainMenu[{0}][{1}] = {2}")
    @MethodSource("mainMenuButtonCases")
    fun `main menu keeps expected button positions`(
        rowIndex: Int,
        columnIndex: Int,
        expectedButtonText: String,
    ) {
        val keyboard = BotButtons.mainMenu.keyboard

        assertEquals(expectedButtonText, keyboard[rowIndex][columnIndex].text)
    }

    @Test
    fun `main menu has five rows`() {
        assertEquals(5, BotButtons.mainMenu.keyboard.size)
    }

    @Test
    fun `main menu has resize keyboard enabled`() {
        assertTrue(BotButtons.mainMenu.resizeKeyboard)
    }

    @Test
    fun `provider menu contains all AI providers`() {
        val providerRow = BotButtons.providerMenu.keyboard[0]

        assertEquals(AiProvider.entries.size, providerRow.size)
        AiProvider.entries.forEachIndexed { i, provider ->
            assertEquals(provider.displayName, providerRow[i].text)
        }
    }

    @Test
    fun `model menu contains all models for given provider`() {
        val provider = AiProvider.OPENAI
        val modelRow = BotButtons.modelMenu(provider).keyboard[0]

        assertEquals(provider.models.size, modelRow.size)
        provider.models.forEachIndexed { i, model ->
            assertEquals(model.displayName, modelRow[i].text)
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("navigationRowCases")
    fun `menus keep back and main menu buttons in last row`(
        caseName: String,
        navRow: List<KeyboardButton>,
    ) {
        assertEquals(BotButtons.BTN_BACK, navRow.first().text, "Case: $caseName")
        assertEquals(BotButtons.BTN_MAIN_MENU, navRow.last().text, "Case: $caseName")
    }

    companion object {

        @JvmStatic
        fun mainMenuButtonCases(): Stream<Arguments> =
            Stream.of(
                arguments(0, 0, BotButtons.BTN_START_CHAT),
                arguments(1, 0, BotButtons.BTN_CHOOSE_MODEL),
                arguments(1, 1, BotButtons.CLEAR_CONTEXT),
                arguments(2, 0, BotButtons.PREMIUM),
                arguments(2, 1, BotButtons.BALANCE),
                arguments(3, 0, BotButtons.BONUS),
                arguments(3, 1, BotButtons.BTN_HELP),
                arguments(4, 0, BotButtons.SUPPORT),
            )

        @JvmStatic
        fun navigationRowCases(): Stream<Arguments> =
            Stream.of(
                arguments("provider menu", BotButtons.providerMenu.keyboard.last()),
                arguments("model menu", BotButtons.modelMenu(AiProvider.OPENAI).keyboard.last()),
            )
    }
}
