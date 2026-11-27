package com.tharunbirla.librecuts

import com.tharunbirla.librecuts.utils.escapeDrawtext
import org.junit.Assert.assertEquals
import org.junit.Test

// Expected values were checked by rendering them through real ffmpeg drawtext.
class DrawtextEscapeTest {
    @Test
    fun escapesEveryLayer() {
        assertEquals("plain text", escapeDrawtext("plain text"))
        assertEquals("50% off", escapeDrawtext("50% off"))
        assertEquals("a\\\\:b", escapeDrawtext("a:b"))
        assertEquals("it\\\\\\'s", escapeDrawtext("it's"))
        assertEquals("back\\\\\\\\slash", escapeDrawtext("back\\slash"))
        assertEquals("say \\\"hi\\\"", escapeDrawtext("say \"hi\""))
        assertEquals("\\[x\\]\\,y\;z", escapeDrawtext("[x],y;z"))
    }
}
