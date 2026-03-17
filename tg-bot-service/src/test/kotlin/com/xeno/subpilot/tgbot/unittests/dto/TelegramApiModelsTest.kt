package com.xeno.subpilot.tgbot.unittests.dto

import com.xeno.subpilot.tgbot.dto.CallbackQuery
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.TelegramResponse
import com.xeno.subpilot.tgbot.dto.Update
import com.xeno.subpilot.tgbot.dto.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TelegramApiModelsTest {

    @Test
    fun `telegram response defaults to not ok and null result`() {
        val response = TelegramResponse<List<Update>>()

        assertEquals(false, response.ok)
        assertNull(response.result)
    }

    @Test
    fun `update defaults are initialized`() {
        val update = Update()

        assertEquals(0L, update.updateId)
        assertNull(update.message)
        assertNull(update.callbackQuery)
    }

    @Test
    fun `message and nested chat user fields are assigned`() {
        val message =
            Message(
                messageId = 10,
                chat = Chat(id = 77),
                text = "hello",
                from = User(id = 5, firstName = "Ivan"),
            )

        assertEquals(10L, message.messageId)
        assertEquals(77L, message.chat.id)
        assertEquals("hello", message.text)
        assertEquals(5L, message.from?.id)
        assertEquals("Ivan", message.from?.firstName)
    }

    @Test
    fun `callback query keeps data and linked message`() {
        val callback =
            CallbackQuery(
                id = "cb-1",
                from = User(id = 3),
                message = Message(chat = Chat(id = 9)),
                data = "start_chat",
            )

        assertEquals("cb-1", callback.id)
        assertEquals(3L, callback.from?.id)
        assertEquals(9L, callback.message?.chat?.id)
        assertEquals("start_chat", callback.data)
    }
}
