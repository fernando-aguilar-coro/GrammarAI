package com.app.grammarai.ui.lesson.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.app.grammarai.ui.theme.NeonCyan
import com.app.grammarai.ui.theme.TextPrimary

fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        val boldRegex = Regex("""\*\*(.*?)\*\*|`([^`]+)`""")
        val matches = boldRegex.findAll(text)

        for (match in matches) {
            if (match.range.first > currentIndex) {
                append(text.substring(currentIndex, match.range.first))
            }
            val boldContent = match.groups[1]?.value
            val codeContent = match.groups[2]?.value

            if (boldContent != null) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                    append(boldContent)
                }
            } else if (codeContent != null) {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan,
                        background = Color(0x3322D3EE)
                    )
                ) {
                    append(" $codeContent ")
                }
            }
            currentIndex = match.range.last + 1
        }

        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}
