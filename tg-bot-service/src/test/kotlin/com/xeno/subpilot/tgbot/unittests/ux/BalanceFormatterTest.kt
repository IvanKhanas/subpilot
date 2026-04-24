package com.xeno.subpilot.tgbot.unittests.ux

import com.xeno.subpilot.tgbot.dto.BalanceInfo
import com.xeno.subpilot.tgbot.dto.FreeProviderBalance
import com.xeno.subpilot.tgbot.dto.PaidProviderBalance
import com.xeno.subpilot.tgbot.message.BotResponses
import com.xeno.subpilot.tgbot.ux.AiProvider
import com.xeno.subpilot.tgbot.ux.BalanceFormatter
import org.junit.jupiter.api.Test

import java.time.Instant

import kotlin.test.assertContains

class BalanceFormatterTest {

    private val formatter = BalanceFormatter()

    @Test
    fun `format includes free and paid sections with provider names`() {
        val balance =
            BalanceInfo(
                freeBalances =
                    listOf(
                        FreeProviderBalance(
                            provider = "openai",
                            requestsRemaining = 7,
                            nextResetAt = Instant.parse("2026-04-25T10:00:00Z"),
                        ),
                    ),
                paidBalances =
                    listOf(
                        PaidProviderBalance(provider = "openai", requestsRemaining = 120),
                    ),
            )

        val text = formatter.format(balance)

        assertContains(
            text,
            BotResponses.BALANCE_RESPONSE.text
                .substringBefore("%s")
                .trim(),
        )
        assertContains(text, AiProvider.OPENAI.displayName)
        assertContains(text, "7")
        assertContains(text, "120")
        assertContains(text, BotResponses.BALANCE_TOP_UP_RESPONSE.text)
    }

    @Test
    fun `format renders reset timestamp when free quota is exhausted`() {
        val balance =
            BalanceInfo(
                freeBalances =
                    listOf(
                        FreeProviderBalance(
                            provider = "openai",
                            requestsRemaining = 0,
                            nextResetAt = Instant.parse("2026-04-26T09:15:00Z"),
                        ),
                    ),
                paidBalances = emptyList(),
            )

        val text = formatter.format(balance)

        assertContains(text, "resets at 2026-04-26 09:15")
        assertContains(
            text,
            BotResponses.BALANCE_PAID_EMPTY_ENTRY_RESPONSE.format(AiProvider.OPENAI.displayName),
        )
    }
}
