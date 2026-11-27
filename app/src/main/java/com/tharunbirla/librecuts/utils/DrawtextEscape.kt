package com.tharunbirla.librecuts.utils

/**
 * Escape user text for an unquoted `text=` value of an FFmpeg drawtext filter inside a
 * double-quoted FFmpegKit command string. Pair it with `:expansion=none` so `%` stays literal.
 * Three layers, inner to outer: filter option, filtergraph, FFmpegKit argument parser.
 */
fun escapeDrawtext(text: String): String {
    var t = text
    for (c in "\\':") t = t.replace("$c", "\\$c")
    for (c in "\\'[],;") t = t.replace("$c", "\\$c")
    return t.replace("\"", "\\\"")
}
