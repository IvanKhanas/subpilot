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

import com.xeno.subpilot.tgbot.ux.AiProvider
import com.xeno.subpilot.tgbot.ux.buttons.BotButtons
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BotButtonsTest {

    @Test
    fun `main menu has start chat button in first row`() {
        val keyboard = BotButtons.mainMenu.keyboard

        assertEquals(BotButtons.BTN_START_CHAT, keyboard[0][0].text)
    }

    @Test
    fun `main menu has choose model and clear context in second row`() {
        val keyboard = BotButtons.mainMenu.keyboard

        assertEquals(BotButtons.BTN_CHOOSE_MODEL, keyboard[1][0].text)
        assertEquals(BotButtons.CLEAR_CONTEXT, keyboard[1][1].text)
    }

    @Test
    fun `main menu has premium and balance in third row`() {
        val keyboard = BotButtons.mainMenu.keyboard

        assertEquals(BotButtons.PREMIUM, keyboard[2][0].text)
        assertEquals(BotButtons.BALANCE, keyboard[2][1].text)
    }

    @Test
    fun `main menu has bonus and help in fourth row`() {
        val keyboard = BotButtons.mainMenu.keyboard

        assertEquals(BotButtons.BONUS, keyboard[3][0].text)
        assertEquals(BotButtons.BTN_HELP, keyboard[3][1].text)
    }

    @Test
    fun `main menu has support alone in fifth row`() {
        val keyboard = BotButtons.mainMenu.keyboard

        assertEquals(BotButtons.SUPPORT, keyboard[4][0].text)
        assertEquals(1, keyboard[4].size)
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
    fun `provider menu has back and main menu buttons in last row`() {
        val navRow = BotButtons.providerMenu.keyboard.last()

        assertEquals(BotButtons.BTN_BACK, navRow[0].text)
        assertEquals(BotButtons.BTN_MAIN_MENU, navRow[1].text)
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

    @Test
    fun `model menu has back and main menu buttons in last row`() {
        val navRow = BotButtons.modelMenu(AiProvider.OPENAI).keyboard.last()

        assertEquals(BotButtons.BTN_BACK, navRow[0].text)
        assertEquals(BotButtons.BTN_MAIN_MENU, navRow[1].text)
    }
}
