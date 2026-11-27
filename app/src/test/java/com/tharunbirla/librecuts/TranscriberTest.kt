package com.tharunbirla.librecuts

import com.tharunbirla.librecuts.utils.Transcriber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TranscriberTest {
    @Test
    fun buildsHttpsEndpointOnly() {
        assertEquals("https://api.openai.com/v1/audio/transcriptions", Transcriber.endpoint("https://api.openai.com/v1/").toString())
        assertEquals("https://api.groq.com/openai/v1/audio/transcriptions", Transcriber.endpoint(" https://api.groq.com/openai/v1 ").toString())
        assertThrows(IllegalArgumentException::class.java) { Transcriber.endpoint("http://example.com/v1") }
        assertThrows(IllegalArgumentException::class.java) { Transcriber.endpoint("not a url") }
        assertThrows(IllegalArgumentException::class.java) { Transcriber.endpoint("") }
    }
}
