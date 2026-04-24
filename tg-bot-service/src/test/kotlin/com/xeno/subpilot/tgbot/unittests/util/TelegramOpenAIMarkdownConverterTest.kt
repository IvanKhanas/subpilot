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
package com.xeno.subpilot.tgbot.unittests.util

import com.xeno.subpilot.tgbot.util.TelegramOpenAIMarkdownConverter
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TelegramOpenAIMarkdownConverterTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        "**bold**,       <b>bold</b>",
        "*italic*,       <i>italic</i>",
        "# H1,           <b>H1</b>",
        "## H2,          <b>H2</b>",
        "### H3,         <b>H3</b>",
        "#### H4,        <b>H4</b>",
        "##### H5,       <b>H5</b>",
        "###### H6,      <b>H6</b>",
    )
    fun `toHtml converts inline markdown to correct HTML tag`(
        markdown: String,
        expected: String,
    ) {
        assertEquals(
            expected.trim(),
            TelegramOpenAIMarkdownConverter.toHtml(markdown.trim()).trim(),
        )
    }

    @ParameterizedTest(name = "escapes {0}")
    @CsvSource(
        "cats & dogs,   cats &amp; dogs",
        "a < b,         a &lt; b",
        "a > b,         a &gt; b",
    )
    fun `toHtml escapes special HTML characters`(
        markdown: String,
        expectedSubstring: String,
    ) {
        val result = TelegramOpenAIMarkdownConverter.toHtml(markdown.trim())

        assertTrue(result.contains(expectedSubstring.trim()))
    }

    @Test
    fun `toHtml converts inline code to code tag`() {
        val result = TelegramOpenAIMarkdownConverter.toHtml("use `foo()` here")

        assertTrue(result.contains("<code>foo()</code>"))
    }

    @Test
    fun `toHtml converts code block to pre tag`() {
        val result = TelegramOpenAIMarkdownConverter.toHtml("```\nval x = 1\n```")

        assertTrue(result.contains("<pre>"))
        assertTrue(result.contains("val x = 1"))
        assertTrue(result.contains("</pre>"))
    }

    @Test
    fun `toHtml converts unordered list items to bullet points`() {
        val result = TelegramOpenAIMarkdownConverter.toHtml("- first\n- second")

        assertTrue(result.contains("• first"))
        assertTrue(result.contains("• second"))
    }

    @Test
    fun `toHtml converts ordered list items with numbers`() {
        val result = TelegramOpenAIMarkdownConverter.toHtml("1. first\n2. second")

        assertTrue(result.contains("1. first"))
        assertTrue(result.contains("2. second"))
    }

    @Test
    fun `toHtml converts hyperlink to anchor tag`() {
        val result = TelegramOpenAIMarkdownConverter.toHtml("[click](https://example.com)")

        assertEquals("<a href=\"https://example.com\">click</a>", result.trim())
    }

    @Test
    fun `toHtml collapses three or more blank lines to two`() {
        val result = TelegramOpenAIMarkdownConverter.toHtml("line one\n\n\n\nline two")

        assertFalse(result.contains("\n\n\n"))
    }

    @Test
    fun `toHtml returns blank string on empty input`() {
        val result = TelegramOpenAIMarkdownConverter.toHtml("")

        assertTrue(result.isBlank())
    }

    @Test
    fun `toHtml indents nested list items`() {
        val result = TelegramOpenAIMarkdownConverter.toHtml("- outer\n  - inner")

        assertTrue(result.contains("• outer"))
        assertTrue(result.contains("    • inner"))
    }

    @Test
    fun `toHtml handles paragraph inside list item without extra blank lines`() {
        // A loose list (blank line between items) generates <p> inside <li>
        val result = TelegramOpenAIMarkdownConverter.toHtml("- first\n\n- second")

        assertTrue(result.contains("• first"))
        assertTrue(result.contains("• second"))
        // <p> inside <li> must use a single newline, not double
        assertFalse(result.contains("first\n\n• second"))
    }
}
