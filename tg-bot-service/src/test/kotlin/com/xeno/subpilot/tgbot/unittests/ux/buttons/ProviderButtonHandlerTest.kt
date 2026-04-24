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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import kotlin.test.assertContains
import kotlin.test.assertFalse
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

    private lateinit var handler: ProviderButtonHandler

    @BeforeEach
    fun setUp() {
        handler = ProviderButtonHandler(telegramClient, subscriptionClient, navigationService)
        every { telegramClient.sendMessage(any(), any(), any(), any()) } returns 1L
        justRun { navigationService.push(any(), any()) }
    }

    @Test
    fun `supports returns true for ai and premium provider buttons`() {
        assertTrue(handler.supports(AiProvider.OPENAI.displayName))
        assertTrue(handler.supports(PremiumProvider.OPENAI.displayName))
        assertFalse(handler.supports("unsupported"))
    }

    @Test
    fun `handle in main menu shows model selection for chosen provider`() {
        every { navigationService.peek(100L) } returns BotScreen.MAIN_MENU
        val textSlot = slot<String>()
        every { telegramClient.sendMessage(100L, capture(textSlot), any(), any()) } returns 1L

        runBlocking {
            handler.handle(
                Message(
                    chat = Chat(id = 100L),
                    text = AiProvider.OPENAI.displayName,
                ),
            )
        }

        verify { navigationService.push(100L, BotScreen.PROVIDER_MENU) }
        assertContains(textSlot.captured, "Choose a")
        assertContains(textSlot.captured, AiProvider.OPENAI.displayName)
    }

    @Test
    fun `handle in premium menu sends plan list for selected provider`() {
        every { navigationService.peek(100L) } returns BotScreen.PREMIUM_MENU
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
        every { telegramClient.sendMessage(100L, capture(textSlot), any(), any()) } returns 1L

        runBlocking {
            handler.handle(
                Message(
                    chat = Chat(id = 100L),
                    text = PremiumProvider.OPENAI.displayName,
                ),
            )
        }

        assertContains(textSlot.captured, "plans:")
        assertContains(textSlot.captured, "Basic")
        assertContains(textSlot.captured, "199.00")
    }

    @Test
    fun `handle sends unavailable message when plan fetch fails`() {
        every { navigationService.peek(100L) } returns BotScreen.PREMIUM_MENU
        coEvery { subscriptionClient.getPlans() } throws SubscriptionServiceException("down")

        runBlocking {
            handler.handle(
                Message(chat = Chat(id = 100L), text = PremiumProvider.OPENAI.displayName),
            )
        }

        verify {
            telegramClient.sendMessage(
                100L,
                BotResponses.AI_UNAVAILABLE_RESPONSE.text,
                any(),
                any(),
            )
        }
    }

    @Test
    fun `handle sends unavailable message when provider has no plans`() {
        every { navigationService.peek(100L) } returns BotScreen.PREMIUM_MENU
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

        runBlocking {
            handler.handle(
                Message(chat = Chat(id = 100L), text = PremiumProvider.OPENAI.displayName),
            )
        }

        verify {
            telegramClient.sendMessage(
                100L,
                BotResponses.AI_UNAVAILABLE_RESPONSE.text,
                any(),
                any(),
            )
        }
        coVerify(exactly = 1) { subscriptionClient.getPlans() }
    }
}
