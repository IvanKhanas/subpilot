package com.xeno.subpilot.tgbot.unittests.dto

import com.xeno.subpilot.tgbot.dto.AnswerCallbackQueryRequest
import com.xeno.subpilot.tgbot.dto.BotCommandInfo
import com.xeno.subpilot.tgbot.dto.InlineKeyboardButton
import com.xeno.subpilot.tgbot.dto.InlineKeyboardMarkup
import com.xeno.subpilot.tgbot.dto.KeyboardButton
import com.xeno.subpilot.tgbot.dto.ReplyKeyboardMarkup
import com.xeno.subpilot.tgbot.dto.SendMessageRequest
import com.xeno.subpilot.tgbot.dto.SetMyCommandsRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TelegramRequestModelsTest {

    @Test
    fun `send message request stores chat text and markup`() {
        val markup = ReplyKeyboardMarkup(keyboard = listOf(listOf(KeyboardButton("Help"))))
        val request = SendMessageRequest(chatId = 1, text = "hi", replyMarkup = markup)

        assertEquals(1L, request.chatId)
        assertEquals("hi", request.text)
        assertEquals(markup, request.replyMarkup)
    }

    @Test
    fun `inline keyboard markup stores rows and callback data`() {
        val button = InlineKeyboardButton(text = "Start", callbackData = "start_chat")
        val markup = InlineKeyboardMarkup(inlineKeyboard = listOf(listOf(button)))

        assertEquals(1, markup.inlineKeyboard.size)
        assertEquals("Start", markup.inlineKeyboard[0][0].text)
        assertEquals("start_chat", markup.inlineKeyboard[0][0].callbackData)
    }

    @Test
    fun `callback and command requests hold provided values`() {
        val callbackRequest = AnswerCallbackQueryRequest(callbackQueryId = "cb")
        val commandsRequest =
            SetMyCommandsRequest(
                commands = listOf(BotCommandInfo(command = "help", description = "Show help")),
            )

        assertEquals("cb", callbackRequest.callbackQueryId)
        assertEquals(1, commandsRequest.commands.size)
        assertEquals("help", commandsRequest.commands[0].command)
        assertEquals("Show help", commandsRequest.commands[0].description)
    }
}
