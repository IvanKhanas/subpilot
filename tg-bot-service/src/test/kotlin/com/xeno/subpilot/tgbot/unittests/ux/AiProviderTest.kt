package com.xeno.subpilot.tgbot.unittests.ux

import com.xeno.subpilot.tgbot.ux.AiModel
import com.xeno.subpilot.tgbot.ux.AiProvider
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

import kotlin.test.assertEquals

class AiProviderTest {

    @Test
    fun `findByDisplayName returns OPENAI for its display name`() {
        assertEquals(
            AiProvider.OPENAI,
            AiProvider.findByDisplayName(AiProvider.OPENAI.displayName),
        )
    }

    @Test
    fun `findByDisplayName returns null for unknown name`() {
        assertNull(AiProvider.findByDisplayName("unknown"))
    }

    @Test
    fun `findModelByDisplayName returns model for GPT-4o`() {
        assertEquals(
            AiModel("gpt-4o", "GPT-4o"),
            AiProvider.findModelByDisplayName("GPT-4o"),
        )
    }

    @Test
    fun `findModelByDisplayName returns model for GPT-4o mini`() {
        assertEquals(
            AiModel("gpt-4o-mini", "GPT-4o mini"),
            AiProvider.findModelByDisplayName("GPT-4o mini"),
        )
    }

    @Test
    fun `findModelByDisplayName returns null for unknown name`() {
        assertNull(AiProvider.findModelByDisplayName("unknown"))
    }

    @Test
    fun `findModelById returns model for gpt-4o id`() {
        assertEquals(
            AiModel("gpt-4o", "GPT-4o"),
            AiProvider.findModelById("gpt-4o"),
        )
    }

    @Test
    fun `findModelById returns null for unknown id`() {
        assertNull(AiProvider.findModelById("unknown"))
    }
}
