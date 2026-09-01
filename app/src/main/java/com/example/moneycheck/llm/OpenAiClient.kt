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
        apiKey: String,
        model: String,
        prompt: String,
    ): ExtractedDraftEntity = analyzeInput(
        notification = notification,
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
        apiKey: String,
        model: String,
        prompt: String,
        cleanedXml: String,
    ): ExtractedDraftEntity = analyzeInput(
        notification = notification,
        apiKey = apiKey,
        model = model,
        prompt = prompt,
        input = cleanedXml,
        includeTransactionTime = true,
    )

    private fun analyzeInput(
        notification: CapturedNotificationEntity,
        apiKey: String,
        model: String,
        prompt: String,
        input: String,
        includeTransactionTime: Boolean,
    ): ExtractedDraftEntity {
        val request = JSONObject().apply {
            put("model", model)
            put("store", false)
            put("instructions", prompt)
            put("input", input)
            put("text", JSONObject().put("format", responseFormat(includeTransactionTime)))
        }

        val connection = (URL(RESPONSES_URL).openConnection() as HttpURLConnection).apply {
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
                throw IOException(apiMessage?.takeIf(String::isNotBlank) ?: "OpenAI HTTP $statusCode")
            }

            val modelJson = extractOutputText(JSONObject(responseBody))
                ?: throw IOException("OpenAI không trả về output_text")
            val parsed = JSONObject(modelJson)
            val transactionTimeText = if (includeTransactionTime && !parsed.isNull("transaction_time")) {
                parsed.optString("transaction_time").trim().takeIf(String::isNotEmpty)
            } else {
                null
            }
            return ExtractedDraftEntity(
                notificationId = notification.id,
                direction = parsed.getString("direction"),
                amount = if (parsed.isNull("amount")) null else parsed.getLong("amount"),
                purpose = parsed.getString("purpose").trim(),
                recipient = parsed.getString("recipient").trim(),
                transactionTime = transactionTimeText?.let(::parseTransactionTime),
                rawModelJson = modelJson,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun extractOutputText(response: JSONObject): String? {
        val output = response.optJSONArray("output") ?: return null
        for (outputIndex in 0 until output.length()) {
            val item = output.optJSONObject(outputIndex) ?: continue
            val content = item.optJSONArray("content") ?: continue
            for (contentIndex in 0 until content.length()) {
                val block = content.optJSONObject(contentIndex) ?: continue
                if (block.optString("type") == "output_text") {
                    return block.optString("text").takeIf(String::isNotBlank)
                }
            }
        }
        return null
    }

    private fun responseFormat(includeTransactionTime: Boolean): JSONObject = JSONObject().apply {
        put("type", "json_schema")
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

    companion object {
        private const val RESPONSES_URL = "https://api.openai.com/v1/responses"
    }
}
