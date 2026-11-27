package com.tharunbirla.librecuts.utils

import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Speech-to-text through an OpenAI-compatible `/audio/transcriptions` endpoint
 * (OpenAI, Groq and most self-hosted Whisper servers). Returns SRT text.
 */
object Transcriber {
    const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
    const val DEFAULT_MODEL = "whisper-1"
    /** Most providers reject uploads above 25 MB. */
    const val MAX_UPLOAD_BYTES = 25L * 1024 * 1024

    /** The transcription URL for [baseUrl]; only https, so the API key never travels in clear text. */
    fun endpoint(baseUrl: String): URL {
        val url = try { URL(baseUrl.trim().trimEnd('/') + "/audio/transcriptions") } catch (e: Exception) {
            throw IllegalArgumentException("Invalid API URL")
        }
        require(url.protocol.equals("https", ignoreCase = true) && !url.host.isNullOrBlank()) { "API URL must start with https://" }
        return url
    }

    fun transcribeToSrt(audio: File, baseUrl: String, apiKey: String, model: String, language: String?): String {
        if (audio.length() > MAX_UPLOAD_BYTES) throw IOException("Audio is too long to upload (over 25 MB)")
        val boundary = "LibreCuts${System.nanoTime()}"
        val conn = endpoint(baseUrl).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 30_000
            conn.readTimeout = 600_000
            conn.setChunkedStreamingMode(0)
            conn.setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

            DataOutputStream(conn.outputStream).use { out ->
                fun field(name: String, value: String) {
                    out.writeBytes("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n")
                    out.write(value.toByteArray(Charsets.UTF_8))
                    out.writeBytes("\r\n")
                }
                field("model", model.trim().ifBlank { DEFAULT_MODEL })
                field("response_format", "srt")
                language?.trim()?.takeIf { it.isNotEmpty() }?.let { field("language", it) }
                out.writeBytes("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"audio.mp3\"\r\nContent-Type: audio/mpeg\r\n\r\n")
                audio.inputStream().use { it.copyTo(out) }
                out.writeBytes("\r\n--$boundary--\r\n")
            }

            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val hint = when (code) {
                    401, 403 -> "API key was rejected"
                    413 -> "Audio is too large for this provider"
                    429 -> "Rate limit or quota reached"
                    else -> "Request failed"
                }
                throw IOException("$hint (HTTP $code): ${body.take(200)}")
            }
            return body
        } finally {
            conn.disconnect()
        }
    }
}
