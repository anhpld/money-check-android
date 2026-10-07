package com.example.moneycheck.llm

import java.net.URI

internal object OpenAiCompatibleEndpoint {
    fun normalize(baseUrl: String): String {
        val normalized = baseUrl.trim().trimEnd('/')
        require(normalized.isNotEmpty()) { "Hãy nhập API base URL" }
        val uri = runCatching { URI(normalized) }
            .getOrElse { throw IllegalArgumentException("API base URL không hợp lệ") }
        require(uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) {
            "API base URL phải bắt đầu bằng http:// hoặc https://"
        }
        require(!uri.host.isNullOrBlank() && uri.rawQuery == null && uri.rawFragment == null) {
            "API base URL không hợp lệ"
        }
        return normalized
    }

    fun url(baseUrl: String, endpoint: String): String {
        val normalized = normalize(baseUrl)
        val path = URI(normalized).path.trimEnd('/')
        val apiBaseUrl = if (path.endsWith("/v1", ignoreCase = true)) normalized else "$normalized/v1"
        return "$apiBaseUrl/${endpoint.trimStart('/')}"
    }
}
