package com.xeno.subpilot.tgbot.ux

import com.xeno.subpilot.tgbot.dto.BalanceInfo
import com.xeno.subpilot.tgbot.dto.FreeProviderBalance
import com.xeno.subpilot.tgbot.dto.PaidProviderBalance
import com.xeno.subpilot.tgbot.message.BotResponses
import org.springframework.stereotype.Component

import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class BalanceFormatter {

    fun format(balance: BalanceInfo): String {
        val freeSection = "\n" + balance.freeBalances.joinToString("\n") { it.format() }
        val paidSection = "\n" + buildPaidSection(balance.paidBalances)
        return BotResponses.BALANCE_RESPONSE.format(freeSection, paidSection)
    }

    private fun buildPaidSection(paidBalances: List<PaidProviderBalance>): String {
        val balanceByKey = paidBalances.associateBy { it.provider }
        val lines =
            AiProvider.entries.map { provider ->
                val remaining = balanceByKey[provider.providerKey]?.requestsRemaining ?: 0
                if (remaining == 0) {
                    BotResponses.BALANCE_PAID_EMPTY_ENTRY_RESPONSE.format(provider.displayName)
                } else {
                    BotResponses.BALANCE_PAID_ENTRY_RESPONSE.format(provider.displayName, remaining)
                }
            }
        return (lines + "\n" + BotResponses.BALANCE_TOP_UP_RESPONSE.text).joinToString("\n")
    }

    private fun FreeProviderBalance.format(): String {
        val name = providerDisplayName(provider)
        return if (requestsRemaining == 0) {
            val resetDate = nextResetAt.atZone(ZoneId.of("UTC")).format(DATE_FORMATTER)
            BotResponses.BALANCE_FREE_RESET_ENTRY_RESPONSE.format(name, resetDate)
        } else {
            BotResponses.BALANCE_FREE_ENTRY_RESPONSE.format(name, requestsRemaining)
        }
    }

    private fun providerDisplayName(key: String): String = AiProvider.displayNameByKey(key)

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
