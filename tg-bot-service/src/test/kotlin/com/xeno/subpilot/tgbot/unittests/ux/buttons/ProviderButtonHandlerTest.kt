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

import com.xeno.subpilot.tgbot.client.SubscriptionClient
import com.xeno.subpilot.tgbot.client.TelegramClient
import com.xeno.subpilot.tgbot.dto.Chat
import com.xeno.subpilot.tgbot.dto.Message
import com.xeno.subpilot.tgbot.dto.PlanInfo
import com.xeno.subpilot.tgbot.exception.SubscriptionServiceException
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.ux.AiProvider
import com.xeno.subpilot.tgbot.ux.BotScreen
import com.xeno.subpilot.tgbot.ux.NavigationService
import com.xeno.subpilot.tgbot.ux.PremiumProvider
import com.xeno.subpilot.tgbot.ux.buttons.ProviderButtonHandler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.slot
import io.mockk.verify
import net.datafaker.Faker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import kotlinx.coroutines.runBlocking

@ExtendWith(MockKExtension::class)
class ProviderButtonHandlerTest {

    @MockK
    lateinit var telegramClient: TelegramClient

    @MockK
    lateinit var subscriptionClient: SubscriptionClient

    @MockK
    lateinit var navigationService: NavigationService

    private val faker = Faker()

    private lateinit var handler: ProviderButtonHandler
    private var chatId: Long = 0L
    private var sentMessageId: Long = 0L

    @BeforeEach
    fun setUp() {
        chatId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        sentMessageId = faker.number().numberBetween(1_000_000L, Long.MAX_VALUE)
        handler = ProviderButtonHandler(telegramClient, subscriptionClient, navigationService)
        every { telegramClient.sendMessage(any(), any(), any(), any()) } returns sentMessageId
        justRun { navigationService.push(any(), any()) }
    }

    @ParameterizedTest(name = "supports(''{0}'') = {1}")
    @CsvSource(
        "'֎ OpenAI', true",
        "'💎 OpenAI', false",
        "'unsupported', false",
    )
    fun `supports handles provider button texts`(
        input: String,
        expected: Boolean,
    ) {
        assertEquals(expected, handler.supports(input))
    }

    @Test
    fun `handle in main menu shows model selection for chosen provider`() {
        every { navigationService.peek(chatId) } returns BotScreen.MAIN_MENU
        val textSlot = slot<String>()
        every { telegramClient.sendMessage(chatId, capture(textSlot), any(), any()) } returns
            sentMessageId

        runBlocking {
            handler.handle(
                Message(
                    chat = Chat(id = chatId),
                    text = AiProvider.OPENAI.displayName,
                ),
            )
        }

        verify { navigationService.push(chatId, BotScreen.PROVIDER_MENU) }
        assertContains(textSlot.captured, "Choose a")
        assertContains(textSlot.captured, AiProvider.OPENAI.displayName)
    }

    @Test
    fun `handle in premium menu sends plan list for selected provider`() {
        every { navigationService.peek(chatId) } returns BotScreen.PREMIUM_MENU
        coEvery { subscriptionClient.getPlans() } returns
            listOf(
                PlanInfo(
                    planId = "openai-basic",
                    provider = "openai",
                    displayName = "Basic",
                    price = "199.00",
                    currency = "RUB",
                    allocations = emptyList(),
                ),
                PlanInfo(
                    planId = "anthropic-basic",
                    provider = "anthropic",
                    displayName = "Anthropic Basic",
                    price = "299.00",
                    currency = "RUB",
                    allocations = emptyList(),
                ),
            )
        val textSlot = slot<String>()
        every { telegramClient.sendMessage(chatId, capture(textSlot), any(), any()) } returns
            sentMessageId

        runBlocking {
            handler.handle(
                Message(
                    chat = Chat(id = chatId),
                    text = PremiumProvider.OPENAI.displayName,
                ),
            )
        }

        assertContains(textSlot.captured, "plans:")
        assertContains(textSlot.captured, "Basic")
        assertContains(textSlot.captured, "199.00")
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        "plan fetch fails, true",
        "provider has no plans, false",
    )
    fun `handle sends unavailable message for invalid premium plans`(
        caseName: String,
        fetchFails: Boolean,
    ) {
        assertTrue(caseName.isNotBlank())
        every { navigationService.peek(chatId) } returns BotScreen.PREMIUM_MENU
        if (fetchFails) {
            coEvery { subscriptionClient.getPlans() } throws SubscriptionServiceException("down")
        } else {
            coEvery { subscriptionClient.getPlans() } returns
                listOf(
                    PlanInfo(
                        planId = "anthropic-basic",
                        provider = "anthropic",
                        displayName = "Anthropic Basic",
                        price = "299.00",
                        currency = "RUB",
                        allocations = emptyList(),
                    ),
                )
        }

        runBlocking {
            handler.handle(
                Message(chat = Chat(id = chatId), text = PremiumProvider.OPENAI.displayName),
            )
        }

        verify {
            telegramClient.sendMessage(
                chatId,
                BotResponses.AI_UNAVAILABLE_RESPONSE.text,
                any(),
                any(),
            )
        }
        if (!fetchFails) {
            coVerify(exactly = 1) { subscriptionClient.getPlans() }
        }
    }
}
