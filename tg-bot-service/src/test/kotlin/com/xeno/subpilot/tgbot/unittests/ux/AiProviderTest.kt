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
package com.xeno.subpilot.tgbot.unittests.ux

import com.xeno.subpilot.tgbot.ux.AiModel
import com.xeno.subpilot.tgbot.ux.AiProvider
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

import kotlin.test.assertEquals

import java.util.stream.Stream

class AiProviderTest {

    @ParameterizedTest(name = "displayName={0}")
    @MethodSource("providerDisplayNameCases")
    fun `findByDisplayName returns provider for known names`(
        displayName: String,
        expectedProvider: AiProvider,
    ) {
        assertEquals(expectedProvider, AiProvider.findByDisplayName(displayName))
    }

    @Test
    fun `findByDisplayName returns null for unknown name`() {
        assertNull(AiProvider.findByDisplayName(UNKNOWN_NAME))
    }

    @ParameterizedTest(name = "displayName={0}")
    @MethodSource("modelCases")
    fun `findModelByDisplayName returns model for known display names`(
        modelDisplayName: String,
        modelId: String,
    ) {
        assertEquals(
            AiModel(modelId, modelDisplayName),
            AiProvider.findModelByDisplayName(modelDisplayName),
        )
    }

    @ParameterizedTest(name = "id={1}")
    @MethodSource("modelCases")
    fun `findModelById returns model for known ids`(
        modelDisplayName: String,
        modelId: String,
    ) {
        assertEquals(
            AiModel(modelId, modelDisplayName),
            AiProvider.findModelById(modelId),
        )
    }

    @Test
    fun `findModelByDisplayName returns null for unknown name`() {
        assertNull(AiProvider.findModelByDisplayName(UNKNOWN_NAME))
    }

    @Test
    fun `findModelById returns null for unknown id`() {
        assertNull(AiProvider.findModelById(UNKNOWN_NAME))
    }

    companion object {
        private const val UNKNOWN_NAME = "unknown"

        @JvmStatic
        fun providerDisplayNameCases(): Stream<Arguments> =
            AiProvider.entries.stream().map { provider ->
                Arguments.of(provider.displayName, provider)
            }

        @JvmStatic
        fun modelCases(): Stream<Arguments> =
            AiProvider.entries
                .flatMap { provider -> provider.models }
                .stream()
                .map { model -> Arguments.of(model.displayName, model.id) }
    }
}
