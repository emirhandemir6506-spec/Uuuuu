package com.example.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class CodeSyntaxHighlighter(private val language: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val transformed = highlightCode(text.text, language)
        return TransformedText(transformed, OffsetMapping.Identity)
    }
}

fun highlightCode(code: String, language: String): AnnotatedString {
    return buildAnnotatedString {
        append(code)
        
        val listKeywords = when (language.lowercase()) {
            "python" -> listOf(
                "def", "class", "import", "as", "from", "return", "if", "else", "elif", "for", "while", 
                "in", "is", "not", "and", "or", "try", "except", "lambda", "None", "True", "False", "print"
            )
            "javascript", "js" -> listOf(
                "function", "const", "let", "var", "return", "if", "else", "for", "while", "class",
                "export", "import", "from", "extends", "new", "this", "console", "log", "true", "false", "null"
            )
            "sql" -> listOf(
                "SELECT", "FROM", "WHERE", "GROUP", "BY", "ORDER", "LIMIT", "AND", "OR", "JOIN", "ON",
                "select", "from", "where", "group", "by", "order", "limit", "and", "or", "join", "on", 
                "SUM", "AVG", "COUNT", "ROUND", "INTO", "INSERT", "TABLE"
            )
            "sh", "bash" -> listOf(
                "echo", "if", "then", "else", "fi", "for", "in", "do", "done", "exit", "sleep", "nproc", "sudo", "apt", "git"
            )
            else -> emptyList()
        }
        
        val commentRegex = when (language.lowercase()) {
            "python", "sh", "bash" -> Regex("#.*")
            "javascript", "js" -> Regex("//.*")
            "sql" -> Regex("--.*")
            else -> Regex("$^") 
        }

        val functionRegex = when (language.lowercase()) {
            "python" -> Regex("def\\s+([a-zA-Z_][a-zA-Z0-9_]*)")
            "javascript", "js" -> Regex("function\\s+([a-zA-Z_][a-zA-Z0-9_]*)")
            else -> Regex("$^")
        }
        
        val stringRegex = Regex("\".*?\"|'.*?'")
        val numberRegex = Regex("\\b\\d+\\b")

        // Colors
        val keywordColor = Color(0xFFFF79C6) // Soft Pink
        val commentColor = Color(0xFF6272A4) // Slate Purple Gray
        val stringColor = Color(0xFF50FA7B)  // Vivid Green
        val numberColor = Color(0xFFBD93F9)  // Lavender Purple
        val functionColor = Color(0xFF8BE9FD)// Light Cyan
        
        // 1. Keywords
        listKeywords.forEach { keyword ->
            val matches = Regex("\\b$keyword\\b").findAll(code)
            matches.forEach { match ->
                addStyle(
                    style = SpanStyle(
                        color = keywordColor,
                        fontWeight = FontWeight.Bold
                    ),
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }
        }

        // 2. Numbers
        numberRegex.findAll(code).forEach { match ->
            addStyle(
                style = SpanStyle(color = numberColor),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        // 3. Functions
        functionRegex.findAll(code).forEach { match ->
            // The first group contains the full function name, group 1 is just the name.
            // Let's color the function name
            val nameGroup = match.groups[1]
            if (nameGroup != null) {
                addStyle(
                    style = SpanStyle(color = functionColor, fontWeight = FontWeight.Bold),
                    start = nameGroup.range.first,
                    end = nameGroup.range.last + 1
                )
            }
        }

        // 4. Strings
        stringRegex.findAll(code).forEach { match ->
            addStyle(
                style = SpanStyle(color = stringColor),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        // 5. Comments
        commentRegex.findAll(code).forEach { match ->
            addStyle(
                style = SpanStyle(color = commentColor),
                start = match.range.first,
                end = match.range.last + 1
            )
        }
    }
}
