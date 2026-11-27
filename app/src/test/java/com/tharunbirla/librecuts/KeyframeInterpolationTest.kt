package com.tharunbirla.librecuts

import com.tharunbirla.librecuts.models.EditOperation.KeyframePoint
import com.tharunbirla.librecuts.models.keyframePairAt
import com.tharunbirla.librecuts.models.keyframeValueAt
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyframeInterpolationTest {
    private val eps = 1e-4f

    @Test
    fun followsEasingAndHoldsOutsideRange() {
        val linear = listOf(KeyframePoint(1000, 0f), KeyframePoint(2000, 10f))
        assertEquals(7f, keyframeValueAt(emptyList(), 1500, 7f), eps)
        assertEquals(0f, keyframeValueAt(linear, 0, 7f), eps)
        assertEquals(10f, keyframeValueAt(linear, 5000, 7f), eps)
        assertEquals(5f, keyframeValueAt(linear, 1500, 7f), eps)

        val easeIn = listOf(KeyframePoint(1000, 0f, interpolationType = "ease_in"), KeyframePoint(2000, 10f))
        assertEquals(2.5f, keyframeValueAt(easeIn, 1500, 7f), eps)

        val hold = listOf(KeyframePoint(1000, 0f, interpolationType = "hold"), KeyframePoint(2000, 10f))
        assertEquals(0f, keyframeValueAt(hold, 1999, 7f), eps)
    }

    @Test
    fun interpolatesBothAxesAndToleratesDuplicateTimes() {
        val keys = listOf(KeyframePoint(0, 0f, 1f), KeyframePoint(1000, 1f, 0f))
        val p = keyframePairAt(keys, 500, 9f, 9f)
        assertEquals(0.5f, p.first, eps)
        assertEquals(0.5f, p.second, eps)

        val dup = listOf(KeyframePoint(0, 0f), KeyframePoint(500, 3f), KeyframePoint(500, 4f), KeyframePoint(1000, 5f))
        assertEquals(false, keyframeValueAt(dup, 500, 0f).isNaN())
        assertEquals(false, keyframeValueAt(dup, 250, 0f).isNaN())
    }
}
