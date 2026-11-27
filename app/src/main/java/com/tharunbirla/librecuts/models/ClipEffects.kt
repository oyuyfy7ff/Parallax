package com.tharunbirla.librecuts.models

import com.google.gson.Gson
import com.tharunbirla.librecuts.models.EditOperation.ClipTransform
import com.tharunbirla.librecuts.models.EditOperation.KeyframePoint

/**
 * A clip effect is a short ClipTransform keyframe recipe (times relative to where it is applied).
 * Applying it just writes keyframes, so preview and export already support every effect.
 */
data class ClipEffect(val name: String = "", val effect: ClipTransform = ClipTransform())

object ClipEffects {
    private fun k(t: Long, x: Float, y: Float = 0f, ease: String = "linear") = KeyframePoint(t, x, y, ease)

    val builtIn: List<ClipEffect> = listOf(
        ClipEffect("Fade In", ClipTransform(opacityKeyframes = listOf(k(0, 0f), k(500, 1f)))),
        ClipEffect("Fade Out", ClipTransform(opacityKeyframes = listOf(k(0, 1f), k(500, 0f)))),
        ClipEffect("Pop In", ClipTransform(scaleKeyframes = listOf(k(0, 0.01f, ease = "ease_out"), k(250, 1.1f, ease = "ease_in_out"), k(400, 1f)))),
        ClipEffect("Zoom Pulse", ClipTransform(scaleKeyframes = listOf(
            k(0, 1f, ease = "ease_out"), k(200, 1.15f, ease = "ease_in"), k(400, 1f, ease = "ease_out"),
            k(600, 1.15f, ease = "ease_in"), k(800, 1f)))),
        ClipEffect("Ken Burns", ClipTransform(scaleKeyframes = listOf(k(0, 1f), k(4000, 1.2f)))),
        ClipEffect("Shake", ClipTransform(positionKeyframes = listOf(
            k(0, 0f, 0f), k(50, 0.02f, -0.015f), k(100, -0.02f, 0.01f), k(150, 0.015f, 0.02f),
            k(200, -0.015f, -0.02f), k(250, 0.01f, 0.015f), k(300, -0.01f, -0.01f), k(400, 0f, 0f)))),
        ClipEffect("Slide In Left", ClipTransform(positionKeyframes = listOf(k(0, -1f, 0f, "ease_out"), k(500, 0f, 0f)))),
        ClipEffect("Slide In Right", ClipTransform(positionKeyframes = listOf(k(0, 1f, 0f, "ease_out"), k(500, 0f, 0f)))),
        ClipEffect("Spin In", ClipTransform(
            rotationKeyframes = listOf(k(0, -180f, ease = "ease_out"), k(600, 0f)),
            scaleKeyframes = listOf(k(0, 0.2f, ease = "ease_out"), k(600, 1f)))),
        ClipEffect("3D Flip In", ClipTransform(tiltYKeyframes = listOf(k(0, 89f, ease = "ease_out"), k(600, 0f)))),
        ClipEffect("3D Flip Out", ClipTransform(tiltYKeyframes = listOf(k(0, 0f, ease = "ease_in"), k(600, -89f)))),
        ClipEffect("3D Swing", ClipTransform(tiltYKeyframes = listOf(
            k(0, 0f, ease = "ease_in_out"), k(300, 25f, ease = "ease_in_out"), k(900, -25f, ease = "ease_in_out"), k(1200, 0f)))),
        ClipEffect("3D Tilt Back", ClipTransform(tiltXKeyframes = listOf(k(0, 0f, ease = "ease_in_out"), k(600, 35f)))),
        ClipEffect("3D Fall In", ClipTransform(
            tiltXKeyframes = listOf(k(0, -80f, ease = "ease_out"), k(700, 0f)),
            opacityKeyframes = listOf(k(0, 0f), k(300, 1f))))
    )

    private val gson = Gson()

    fun toJson(effect: ClipEffect): String = gson.toJson(effect)

    /** Parse an imported effect file. Returns null when it is not a usable effect. */
    fun fromJson(json: String): ClipEffect? {
        val parsed = try { gson.fromJson(json, ClipEffect::class.java) } catch (e: Exception) { null } ?: return null
        val e = parsed.effect ?: return null
        // Untrusted file: drop broken keyframes and cap the size so a bad file cannot stall preview/export.
        fun clean(keys: List<KeyframePoint>?) = keys.orEmpty()
            .filter { it.timeMs in 0..600_000L && it.valueX.isFinite() && it.valueY.isFinite() }
            .take(500)
        val cleaned = ClipTransform(
            positionKeyframes = clean(e.positionKeyframes).map { it.copy(valueX = it.valueX.coerceIn(-2f, 2f), valueY = it.valueY.coerceIn(-2f, 2f)) },
            scaleKeyframes = clean(e.scaleKeyframes).map { it.copy(valueX = it.valueX.coerceIn(0.01f, 10f)) },
            rotationKeyframes = clean(e.rotationKeyframes).map { it.copy(valueX = it.valueX.coerceIn(-3600f, 3600f)) },
            opacityKeyframes = clean(e.opacityKeyframes).map { it.copy(valueX = it.valueX.coerceIn(0f, 1f)) },
            tiltXKeyframes = clean(e.tiltXKeyframes).map { it.copy(valueX = it.valueX.coerceIn(-89f, 89f)) },
            tiltYKeyframes = clean(e.tiltYKeyframes).map { it.copy(valueX = it.valueX.coerceIn(-89f, 89f)) }
        )
        if (cleaned.isIdentity) return null
        return ClipEffect(parsed.name?.take(60)?.ifBlank { null } ?: "Imported effect", cleaned)
    }

    /** Keyframes of [t] that start at [atMs], replacing [t]'s keyframes inside the effect's time span. */
    fun apply(t: ClipTransform, effect: ClipTransform, atMs: Long): ClipTransform {
        fun merge(base: List<KeyframePoint>, add: List<KeyframePoint>): List<KeyframePoint> {
            if (add.isEmpty()) return base
            val shifted = add.map { it.copy(timeMs = it.timeMs + atMs) }
            val from = shifted.minOf { it.timeMs }
            val to = shifted.maxOf { it.timeMs }
            return (base.filter { it.timeMs < from || it.timeMs > to } + shifted).sortedBy { it.timeMs }
        }
        return t.copy(
            positionKeyframes = merge(t.positionKeyframes, effect.positionKeyframes),
            scaleKeyframes = merge(t.scaleKeyframes, effect.scaleKeyframes),
            rotationKeyframes = merge(t.rotationKeyframes, effect.rotationKeyframes),
            opacityKeyframes = merge(t.opacityKeyframes, effect.opacityKeyframes),
            tiltXKeyframes = merge(t.tiltXKeyframes, effect.tiltXKeyframes),
            tiltYKeyframes = merge(t.tiltYKeyframes, effect.tiltYKeyframes)
        )
    }

    /** The current clip's keyframes as a shareable effect, re-timed to start at 0. */
    fun fromClip(name: String, t: ClipTransform): ClipEffect? {
        val all = listOf(t.positionKeyframes, t.scaleKeyframes, t.rotationKeyframes, t.opacityKeyframes, t.tiltXKeyframes, t.tiltYKeyframes).flatten()
        if (all.isEmpty()) return null
        val start = all.minOf { it.timeMs }
        fun shift(keys: List<KeyframePoint>) = keys.map { it.copy(timeMs = it.timeMs - start) }
        return ClipEffect(name, ClipTransform(
            positionKeyframes = shift(t.positionKeyframes), scaleKeyframes = shift(t.scaleKeyframes),
            rotationKeyframes = shift(t.rotationKeyframes), opacityKeyframes = shift(t.opacityKeyframes),
            tiltXKeyframes = shift(t.tiltXKeyframes), tiltYKeyframes = shift(t.tiltYKeyframes)
        ))
    }
}
