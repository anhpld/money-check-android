package com.example.moneycheck.llm

import com.example.moneycheck.data.CapturedNotificationEntity
import com.example.moneycheck.data.ExtractedDraftEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal class OpenAiClient {
    fun analyze(
        notification: CapturedNotificationEntity,
        apiBaseUrl: String,
        apiKey: String,
        model: String,
        prompt: String,
    ): ExtractedDraftEntity = analyzeInput(
        notification = notification,
        apiBaseUrl = apiBaseUrl,
        apiKey = apiKey,
        model = model,
        prompt = prompt,
        input = JSONObject().apply {
            put("title", notification.title)
            put("text", notification.text)
            put("expanded_content", notification.expandedContent)
        }.toString(),
        includeTransactionTime = false,
    )

    fun analyzeScreen(
        notification: CapturedNotificationEntity,
        apiBaseUrl: String,
        apiKey: String,
        model: String,
        prompt: String,
        cleanedXml: String,
    ): ExtractedDraftEntity = analyzeInput(
        notification = notification,
        apiBaseUrl = apiBaseUrl,
        apiKey = apiKey,
        model = model,
        prompt = prompt,
        input = cleanedXml,
        includeTransactionTime = true,
    )

    private fun analyzeInput(
        notification: CapturedNotificationEntity,
        apiBaseUrl: String,
        apiKey: String,
        model: String,
        prompt: String,
        input: String,
        includeTransactionTime: Boolean,
    ): ExtractedDraftEntity {
        val request = JSONObject().apply {
            put("model", model)
            put("stream", false)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", prompt))
                put(JSONObject().put("role", "user").put("content", input))
            })
            put(
                "response_format",
                JSONObject()
                    .put("type", "json_schema")
                    .put("json_schema", responseFormat(includeTransactionTime)),
            )
        }

        val connection = (URL(OpenAiCompatibleEndpoint.url(apiBaseUrl, "chat/completions")).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
        }

        try {
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(request.toString()) }
            val statusCode = connection.responseCode
            val responseBody = (if (statusCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()
            if (statusCode !in 200..299) {
                val apiMessage = runCatching {
                    JSONObject(responseBody).optJSONObject("error")?.optString("message")
                }.getOrNull()
                throw IOException(apiMessage?.takeIf(String::isNotBlank) ?: "API HTTP $statusCode")
            }

            val modelOutput = JSONObject(responseBody)
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.takeIf(String::isNotBlank)
                ?: throw IOException("API không trả về nội dung phân tích")
            val modelJson = normalizeJsonObject(modelOutput)
            val parsed = JSONObject(modelJson)
            val transactionTimeText = if (includeTransactionTime && !parsed.isNull("transaction_time")) {
                parsed.optString("transaction_time").trim().takeIf(String::isNotEmpty)
            } else {
                null
            }
            return ExtractedDraftEntity(
                notificationId = notification.id,
                direction = parsed.optString("direction")
                    .takeIf { it in setOf("income", "expense", "unknown") }
                    ?: "unknown",
                amount = if (parsed.isNull("amount")) null else parsed.getLong("amount"),
                purpose = parsed.optString("purpose").trim(),
                recipient = parsed.optString("recipient")
                    .ifBlank { parsed.optString("recipent") }
                    .trim(),
                transactionTime = transactionTimeText?.let(::parseTransactionTime),
                rawModelJson = modelJson,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun responseFormat(includeTransactionTime: Boolean): JSONObject = JSONObject().apply {
        put("name", if (includeTransactionTime) "screen_transaction_extraction" else "transaction_extraction")
        put("strict", true)
        put("schema", JSONObject().apply {
            put("type", "object")
            put("additionalProperties", false)
            put(
                "properties",
                JSONObject().apply {
                    put("direction", enum("income", "expense", "unknown"))
                    put("amount", nullableType("integer"))
                    put("purpose", type("string"))
                    put(
                        "recipient",
                        type("string").put(
                            "description",
                            "Tên người nhận, bên thụ hưởng, cửa hàng hoặc đơn vị nhận tiền; chuỗi rỗng nếu không xác định được",
                        ),
                    )
                    if (includeTransactionTime) {
                        put(
                            "transaction_time",
                            nullableType("string").put(
                                "description",
                                "Ngày hoặc thời điểm thanh toán thực tế theo ISO 8601; ưu tiên trường có nhãn Ngày thanh toán/Thời gian thanh toán/Thời gian giao dịch, null nếu màn hình không thể hiện rõ",
                            ),
                        )
                    }
                },
            )
            val required = mutableListOf("direction", "amount", "purpose", "recipient")
            if (includeTransactionTime) required += "transaction_time"
            put("required", JSONArray(required))
        })
    }

    private fun normalizeJsonObject(output: String): String {
        val trimmed = output.trim()
        val withoutFence = if (trimmed.startsWith("```")) {
            trimmed
                .substringAfter('\n', missingDelimiterValue = trimmed)
                .substringBeforeLast("```", missingDelimiterValue = trimmed)
                .trim()
        } else {
            trimmed
        }
        val candidate = withoutFence
            .substring(withoutFence.indexOf('{').coerceAtLeast(0))
            .let { value ->
                val closingBrace = value.lastIndexOf('}')
                if (closingBrace >= 0) value.substring(0, closingBrace + 1) else value
            }
        return runCatching { JSONObject(candidate).toString() }
            .getOrElse { throw IOException("Model không trả về JSON hợp lệ") }
    }

    private fun parseTransactionTime(value: String): Long? {
        runCatching { return Instant.parse(value).toEpochMilli() }
        runCatching { return OffsetDateTime.parse(value).toInstant().toEpochMilli() }
        val zone = ZoneId.systemDefault()
        val dateTimeFormats = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        )
        dateTimeFormats.forEach { formatter ->
            runCatching {
                return LocalDateTime.parse(value, formatter).atZone(zone).toInstant().toEpochMilli()
            }
        }
        val dateFormats = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        )
        dateFormats.forEach { formatter ->
            runCatching {
                return LocalDate.parse(value, formatter).atStartOfDay(zone).toInstant().toEpochMilli()
            }
        }
        return null
    }

    private fun type(type: String) = JSONObject().put("type", type)

    private fun nullableType(type: String) =
        JSONObject().put("type", JSONArray(listOf(type, "null")))

    private fun enum(vararg values: String) =
        JSONObject().put("type", "string").put("enum", JSONArray(values.toList()))
}
