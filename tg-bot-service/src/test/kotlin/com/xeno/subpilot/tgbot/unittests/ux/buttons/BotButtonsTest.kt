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
    fun `main menu has choose model and help buttons in second row`() {
        val keyboard = BotButtons.mainMenu.keyboard

        assertEquals(BotButtons.BTN_CHOOSE_MODEL, keyboard[1][0].text)
        assertEquals(BotButtons.BTN_HELP, keyboard[1][1].text)
    }

    @Test
    fun `main menu has two rows`() {
        assertEquals(2, BotButtons.mainMenu.keyboard.size)
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
