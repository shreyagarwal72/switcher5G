package com.app.switcher5g.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Utility to parse markdown bold syntax (`**text**`) into Compose AnnotatedString,
 * and sanitize pipe `|` characters or unwanted formatting artifacts.
 */
object MarkdownUtils {

    fun parseMarkdown(input: String): AnnotatedString {
        // Replace unwanted pipe characters with bullet dots
        val sanitized = input.replace("|", " • ")

        return buildAnnotatedString {
            val parts = sanitized.split("**")
            parts.forEachIndexed { index, part ->
                if (index % 2 == 1) {
                    // Inside **bold**
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(part)
                    }
                } else {
                    append(part)
                }
            }
        }
    }
}
