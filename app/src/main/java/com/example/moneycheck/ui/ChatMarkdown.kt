package com.example.moneycheck.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class MarkdownKind { PARAGRAPH, HEADING, QUOTE, BULLET, NUMBERED, CODE, RULE }
private data class MarkdownBlock(val kind: MarkdownKind, val text: String, val level: Int = 0, val ordinal: Int = 1)

@Composable
internal fun ChatMarkdown(content: String, modifier: Modifier = Modifier) {
    val blocks = parseMarkdownBlocks(content)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        blocks.forEach { block ->
            when (block.kind) {
                MarkdownKind.PARAGRAPH -> Text(
                    inlineMarkdown(block.text),
                    fontSize = 12.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                MarkdownKind.HEADING -> Text(
                    inlineMarkdown(block.text),
                    fontSize = when (block.level) { 1 -> 20.sp; 2 -> 18.sp; 3 -> 16.sp; else -> 14.sp },
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                MarkdownKind.QUOTE -> Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Row(Modifier.padding(9.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.foundation.layout.Box(
                            Modifier.width(3.dp).height(24.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
                        )
                        Text(inlineMarkdown(block.text), fontSize = 11.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                MarkdownKind.BULLET, MarkdownKind.NUMBERED -> Row(
                    Modifier.fillMaxWidth().padding(start = (block.level * 12).dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(if (block.kind == MarkdownKind.BULLET) "•" else "${block.ordinal}.", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    Text(inlineMarkdown(block.text), Modifier.weight(1f), fontSize = 12.sp, lineHeight = 21.sp)
                }
                MarkdownKind.CODE -> Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        block.text,
                        Modifier.fillMaxWidth().padding(10.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                MarkdownKind.RULE -> androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val result = mutableListOf<MarkdownBlock>()
    val paragraph = mutableListOf<String>()
    val code = mutableListOf<String>()
    var inCode = false
    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            result += MarkdownBlock(MarkdownKind.PARAGRAPH, paragraph.joinToString(" ").trim())
            paragraph.clear()
        }
    }
    markdown.replace("\r\n", "\n").lines().forEach { raw ->
        val line = raw.trimEnd()
        if (line.trimStart().startsWith("```")) {
            if (inCode) {
                result += MarkdownBlock(MarkdownKind.CODE, code.joinToString("\n"))
                code.clear()
                inCode = false
            } else {
                flushParagraph()
                inCode = true
            }
            return@forEach
        }
        if (inCode) {
            code += line
            return@forEach
        }
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            flushParagraph()
            return@forEach
        }
        val heading = Regex("^(#{1,6})\\s+(.+)$").matchEntire(trimmed)
        val bullet = Regex("^(\\s*)[-+*]\\s+(.+)$").matchEntire(line)
        val numbered = Regex("^(\\s*)(\\d+)[.)]\\s+(.+)$").matchEntire(line)
        when {
            heading != null -> {
                flushParagraph()
                result += MarkdownBlock(MarkdownKind.HEADING, heading.groupValues[2], heading.groupValues[1].length)
            }
            trimmed.matches(Regex("^(---+|___+|\\*\\*\\*+)$")) -> {
                flushParagraph()
                result += MarkdownBlock(MarkdownKind.RULE, "")
            }
            trimmed.startsWith(">") -> {
                flushParagraph()
                result += MarkdownBlock(MarkdownKind.QUOTE, trimmed.removePrefix(">").trimStart())
            }
            bullet != null -> {
                flushParagraph()
                result += MarkdownBlock(MarkdownKind.BULLET, bullet.groupValues[2], bullet.groupValues[1].length / 2)
            }
            numbered != null -> {
                flushParagraph()
                result += MarkdownBlock(MarkdownKind.NUMBERED, numbered.groupValues[3], numbered.groupValues[1].length / 2, numbered.groupValues[2].toIntOrNull() ?: 1)
            }
            else -> paragraph += trimmed
        }
    }
    if (inCode) result += MarkdownBlock(MarkdownKind.CODE, code.joinToString("\n"))
    flushParagraph()
    return result
}

private val inlinePattern = Regex("""(\*\*.+?\*\*|__.+?__|~~.+?~~|`[^`]+`|\[[^\]]+]\(https?://[^)\s]+\)|(?<!\*)\*(?!\s).+?(?<!\s)\*(?!\*)|(?<!_)_(?!\s).+?(?<!\s)_(?!_))""")

@Composable
private fun inlineMarkdown(source: String) = buildAnnotatedString {
    var cursor = 0
    inlinePattern.findAll(source).forEach { match ->
        append(source.substring(cursor, match.range.first))
        val token = match.value
        when {
            token.startsWith("**") || token.startsWith("__") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(token.substring(2, token.length - 2)) }
            token.startsWith("~~") -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(token.substring(2, token.length - 2)) }
            token.startsWith("`") -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)) { append(token.substring(1, token.length - 1)) }
            token.startsWith("[") -> {
                val link = Regex("^\\[([^]]+)]\\((https?://[^)\\s]+)\\)$").matchEntire(token)
                if (link != null) withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) { append(link.groupValues[1]) }
                else append(token)
            }
            else -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(token.substring(1, token.length - 1)) }
        }
        cursor = match.range.last + 1
    }
    append(source.substring(cursor))
}
