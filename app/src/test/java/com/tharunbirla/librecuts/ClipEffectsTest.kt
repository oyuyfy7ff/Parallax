package com.tharunbirla.librecuts

import com.tharunbirla.librecuts.models.ClipEffects
import com.tharunbirla.librecuts.models.EditOperation.ClipTransform
import com.tharunbirla.librecuts.models.EditOperation.KeyframePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipEffectsTest {
    @Test
    fun applyShiftsEffectAndReplacesKeysInsideItsSpan() {
        val base = ClipTransform(scaleKeyframes = listOf(KeyframePoint(0, 1f), KeyframePoint(1200, 2f), KeyframePoint(5000, 3f)))
        val effect = ClipTransform(scaleKeyframes = listOf(KeyframePoint(0, 0.5f), KeyframePoint(400, 1f)))
        val out = ClipEffects.apply(base, effect, 1000)
        assertEquals(listOf(0L, 1000L, 1400L, 5000L), out.scaleKeyframes.map { it.timeMs })
        assertEquals(base.positionKeyframes, out.positionKeyframes)
    }

    @Test
    fun builtInsRoundTripAndAreNotEmpty() {
        for (e in ClipEffects.builtIn) {
            assertTrue(e.name, !e.effect.isIdentity)
            val back = ClipEffects.fromJson(ClipEffects.toJson(e))
            assertNotNull(e.name, back)
            assertEquals(e.name, back!!.name)
        }
    }

    @Test
    fun rejectsOrSanitizesBadFiles() {
        assertNull(ClipEffects.fromJson("not json"))
        assertNull(ClipEffects.fromJson("{}"))
        assertNull(ClipEffects.fromJson("""{"name":"x","effect":{"scaleKeyframes":[{"timeMs":-5,"valueX":2}]}}"""))
        val e = ClipEffects.fromJson("""{"name":"","effect":{"opacityKeyframes":[{"timeMs":0,"valueX":7}],"tiltXKeyframes":[{"timeMs":10,"valueX":500}]}}""")!!
        assertEquals("Imported effect", e.name)
        assertEquals(1f, e.effect.opacityKeyframes[0].valueX, 0f)
        assertEquals(89f, e.effect.tiltXKeyframes[0].valueX, 0f)
    }

    @Test
    fun fromClipRetimesToZero() {
        val t = ClipTransform(rotationKeyframes = listOf(KeyframePoint(2000, 0f), KeyframePoint(2500, 90f)))
        val e = ClipEffects.fromClip("mine", t)!!
        assertEquals(listOf(0L, 500L), e.effect.rotationKeyframes.map { it.timeMs })
        assertNull(ClipEffects.fromClip("none", ClipTransform()))
    }
}
