package com.xeno.subpilot.tgbot.util

import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

object TelegramOpenAIMarkdownConverter {

    private val parser = Parser.builder().build()
    private val renderer = HtmlRenderer.builder().build()

    /**
     * Converts standard Markdown (as returned by OpenAI) into
     * Telegram-compatible HTML for parse_mode=HTML.
     */
    fun toHtml(markdown: String): String {
        val rawHtml = renderer.render(parser.parse(markdown))
        val body = Jsoup.parseBodyFragment(rawHtml).body()
        return renderElement(body)
            .trim()
            .replace(Regex("\n{3,}"), "\n\n")
    }

    private fun renderElement(el: Element): String =
        buildString {
            for (node in el.childNodes()) {
                when (node) {
                    is TextNode -> {
                        val text = node.wholeText
                        if (text.isNotBlank()) append(text.escapeHtml())
                    }
                    is Element -> append(renderTag(node))
                }
            }
        }

    private fun renderTag(el: Element): String =
        when (el.tagName()) {
            "p" -> renderElement(el) + if (el.parent()?.tagName() == "li") "\n" else "\n\n"
            "br" -> "\n"
            "strong", "b" -> "<b>${renderElement(el)}</b>"
            "em", "i" -> "<i>${renderElement(el)}</i>"
            "code" ->
                if (el.parent()?.tagName() == "pre") {
                    renderElement(el)
                } else {
                    "<code>${renderElement(el)}</code>"
                }
            "pre" -> "<pre>${renderElement(el)}</pre>\n\n"
            "h1", "h2", "h3", "h4", "h5", "h6" -> "<b>${renderElement(el)}</b>\n\n"
            "ul" -> {
                val indent = if (el.parent()?.tagName() == "li") "    " else ""
                el.children().joinToString("") { li ->
                    "$indent• ${renderElement(li).trim()}\n"
                } + "\n"
            }
            "ol" ->
                el
                    .children()
                    .mapIndexed { i, li ->
                        "${i + 1}. ${renderElement(li).trim()}\n"
                    }.joinToString("") + "\n"
            "li" -> renderElement(el)
            "a" -> "<a href=\"${el.attr("href").escapeHtml()}\">${renderElement(el)}</a>"
            else -> renderElement(el)
        }

    private fun String.escapeHtml(): String =
        replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
