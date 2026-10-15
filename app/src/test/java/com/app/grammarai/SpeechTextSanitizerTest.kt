package com.app.grammarai

import com.app.grammarai.ui.practice.audio.SpeechTextSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechTextSanitizerTest {

    @Test
    fun testCleanWordPunctuationAndWhitespace() {
        assertEquals("hello", SpeechTextSanitizer.cleanWord("  Hello,  "))
        assertEquals("world", SpeechTextSanitizer.cleanWord("\"world!\""))
        assertEquals("hola", SpeechTextSanitizer.cleanWord("¿Hola?"))
        assertEquals("bien", SpeechTextSanitizer.cleanWord("¡bien!"))
        assertEquals("test", SpeechTextSanitizer.cleanWord("test..."))
    }

    @Test
    fun testPrepareTtsTextUnderscoreSubstitution() {
        assertEquals("Fill in the blank.", SpeechTextSanitizer.prepareTtsText("Fill in the ___."))
        assertEquals("I have blank apple.", SpeechTextSanitizer.prepareTtsText("I have _ apple."))
        assertEquals("No change here.", SpeechTextSanitizer.prepareTtsText("No change here."))
    }

    @Test
    fun testIsLikelySpanish() {
        assertTrue(SpeechTextSanitizer.isLikelySpanish("¿Cómo estás?"))
        assertTrue(SpeechTextSanitizer.isLikelySpanish("El carro rojo"))
        assertTrue(SpeechTextSanitizer.isLikelySpanish("una manzana"))
        assertFalse(SpeechTextSanitizer.isLikelySpanish("The red car"))
        assertFalse(SpeechTextSanitizer.isLikelySpanish("Good morning, how are you?"))
    }

    @Test
    fun testCleanMarkdownForSpeech() {
        val markdown = """
            # Header Title
            This is **bold** text and *italic* text with `inline code`.
            
            ---
            
            | Concept | Explanation |
            | --- | --- |
            | Past Simple | Completed past actions |
            | Present Perfect | Connected to now |
            
            * Bullet 1
            * Bullet 2
            > A famous quote
        """.trimIndent()

        val cleaned = SpeechTextSanitizer.cleanMarkdownForSpeech(markdown)

        assertFalse(cleaned.contains("# Header"))
        assertFalse(cleaned.contains("**bold**"))
        assertFalse(cleaned.contains("*italic*"))
        assertFalse(cleaned.contains("`inline code`"))
        assertFalse(cleaned.contains("---"))
        assertFalse(cleaned.contains("> A famous"))
        assertTrue(cleaned.contains("bold text and italic text with inline code"))
        assertTrue(cleaned.contains("Past Simple. Completed past actions"))
        assertTrue(cleaned.contains("Present Perfect. Connected to now"))
    }
}
