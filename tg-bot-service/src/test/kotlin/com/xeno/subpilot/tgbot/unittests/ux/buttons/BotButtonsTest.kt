package com.xeno.subpilot.tgbot.unittests.ux.buttons

import com.xeno.subpilot.tgbot.ux.buttons.BotButtons
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BotButtonsTest {

    @Test
    fun `inline keyboard contains start chat and help callbacks`() {
        val keyboard = BotButtons.welcomeInlineKeyboard.inlineKeyboard

        assertEquals(2, keyboard.size)
        assertEquals(BotButtons.START_CHAT, keyboard[0][0].callbackData)
        assertEquals(BotButtons.HELP, keyboard[1][0].callbackData)
    }

    @Test
    fun `main menu contains chat and help text buttons`() {
        val keyboard = BotButtons.mainMenu.keyboard

        assertEquals(2, keyboard.size)
        assertEquals(BotButtons.BTN_CHAT, keyboard[0][0].text)
        assertEquals(BotButtons.BTN_HELP, keyboard[1][0].text)
        assertTrue(BotButtons.mainMenu.resizeKeyboard)
    }
}
