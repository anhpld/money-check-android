package com.example.moneycheck.llm

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal class OpenAiModelsClient {
    fun validateAndList(apiKey: String): List<String> {
        val connection = (URL(MODELS_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
            setRequestProperty("Accept", "application/json")
        }

        try {
            val statusCode = connection.responseCode
            val body = (if (statusCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()
            if (statusCode !in 200..299) {
                val message = runCatching {
                    JSONObject(body).optJSONObject("error")?.optString("message")
                }.getOrNull()
                throw IOException(message?.takeIf(String::isNotBlank) ?: "OpenAI HTTP $statusCode")
            }

            val data = JSONObject(body).optJSONArray("data")
                ?: throw IOException("OpenAI không trả về danh sách model")
            val models = buildList {
                for (index in 0 until data.length()) {
                    val model = data.optJSONObject(index) ?: continue
                    val id = model.optString("id")
                    if (id.isSupportedTextModel()) add(id)
                }
            }.distinct().sortedWith(compareBy<String> { it.modelPriority() }.thenByDescending { it })

            if (models.isEmpty()) {
                throw IOException("API key hợp lệ nhưng không có model text phù hợp")
            }
            return models
        } finally {
            connection.disconnect()
        }
    }

    private fun String.isSupportedTextModel(): Boolean {
        if (!startsWith("gpt-", ignoreCase = true)) return false
        val unsupportedMarkers = listOf(
            "audio", "realtime", "transcribe", "tts", "image", "search", "chat", "codex",
        )
        return unsupportedMarkers.none { contains(it, ignoreCase = true) }
    }

    private fun String.modelPriority(): Int = when {
        contains("luna", ignoreCase = true) || contains("mini", ignoreCase = true) -> 0
        contains("terra", ignoreCase = true) -> 1
        contains("sol", ignoreCase = true) -> 2
        else -> 3
    }

    companion object {
        private const val MODELS_URL = "https://api.openai.com/v1/models"
    }
}
