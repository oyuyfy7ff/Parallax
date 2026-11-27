package com.tharunbirla.librecuts

import com.tharunbirla.librecuts.models.EditOperation.KeyframePoint
import com.tharunbirla.librecuts.models.bezierEasing
import com.tharunbirla.librecuts.models.cubicBezierEase
import com.tharunbirla.librecuts.models.easeProgress
import com.tharunbirla.librecuts.models.easingHandles
import com.tharunbirla.librecuts.models.expandCurvedKeyframes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EasingCurveTest {
    @Test
    fun builtInTypesAreExactBezierCurves() {
        for (type in listOf("linear", "ease_in", "ease_out", "ease_in_out")) {
            val h = easingHandles(type)!!
            for (i in 0..20) {
                val p = i / 20f
                assertEquals("$type at $p", easeProgress(type, p), cubicBezierEase(h, p), 1e-4f)
            }
        }
        assertNull(easingHandles("hold"))
    }

    @Test
    fun customCurvesRoundTripAndOvershoot() {
        val back = bezierEasing(0.34f, 1.56f, 0.64f, 1f)
        assertEquals("bezier:0.340,1.560,0.640,1.000", back)
        assertTrue(easeProgress(back, 0.6f) > 1f) // overshoots before settling
        assertEquals(1f, easeProgress(back, 1f), 0f)
        assertEquals(0.5f, easeProgress("bezier:garbage", 0.5f), 1e-4f) // unreadable -> linear
    }

    @Test
    fun exportExpansionFollowsTheCurve() {
        val type = bezierEasing(0.2f, 0f, 0f, 1f)
        val keys = listOf(KeyframePoint(0, 0f, 0f, type), KeyframePoint(1000, 10f, 20f))
        val out = expandCurvedKeyframes(keys)
        assertEquals(25, out.size)
        assertEquals(1000L, out.last().timeMs)
        for (k in out.dropLast(1)) {
            val p = k.timeMs / 1000f
            assertEquals(10f * easeProgress(type, p), k.valueX, 0.05f)
            assertEquals("linear", k.interpolationType)
        }
        val plain = listOf(KeyframePoint(0, 0f), KeyframePoint(1000, 1f, interpolationType = "ease_in"))
        assertTrue(expandCurvedKeyframes(plain) === plain)
    }
}
