package com.example.moneycheck.llm

import com.example.moneycheck.ChatMessage
import com.example.moneycheck.data.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal class TransactionChatClient {
    suspend fun chat(
        apiBaseUrl: String,
        authBearerToken: String,
        model: String,
        transactionCount: Int,
        conversation: List<ChatMessage>,
        onDelta: (String) -> Unit,
        executeQuery: suspend (String) -> String,
    ): String {
        val messages = JSONArray().apply {
            put(
                JSONObject()
                    .put("role", "system")
                    .put("content", systemPrompt(transactionCount)),
            )
            conversation.takeLast(MAX_HISTORY_MESSAGES).forEach { message ->
                put(
                    JSONObject()
                        .put("role", message.role)
                        .put("content", message.content),
                )
            }
        }

        val tools = createTools()
        val maxTurns = 6
        var currentTurn = 0

        while (currentTurn < maxTurns) {
            currentTurn++

            val isFinalTurn = currentTurn == maxTurns
            val requestPayload = JSONObject().apply {
                put("model", model)
                put("stream", false)
                put("messages", messages)
                if (!isFinalTurn) {
                    put("tools", tools)
                }
            }

            val responseJson = postJson(apiBaseUrl, authBearerToken, requestPayload)
            val firstChoice = responseJson
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?: throw IOException("API không trả về lựa chọn trả lời hợp lệ")

            val assistantMessage = firstChoice.optJSONObject("message")
                ?: throw IOException("API không trả về nội dung tin nhắn")

            val toolCalls = assistantMessage.optJSONArray("tool_calls")
            if (toolCalls == null || toolCalls.length() == 0 || isFinalTurn) {
                val directAnswer = extractText(assistantMessage.opt("content"))
                    ?: extractText(assistantMessage.opt("reasoning_content"))
                    ?: extractText(assistantMessage.opt("thought"))
                    ?: throw IOException("API không trả về nội dung trả lời")
                onDelta(directAnswer)
                return directAnswer
            }

            if (!assistantMessage.has("content") || assistantMessage.isNull("content")) {
                assistantMessage.put("content", JSONObject.NULL)
            }
            messages.put(assistantMessage)

            for (i in 0 until toolCalls.length()) {
                val call = toolCalls.getJSONObject(i)
                val callId = call.optString("id", "call_${System.currentTimeMillis()}_$i")
                val func = call.getJSONObject("function")
                val funcName = func.getString("name")
                val argsStr = func.optString("arguments")

                val resultJson = if (funcName == "execute_read_only_sql") {
                    val args = runCatching { JSONObject(argsStr) }.getOrNull()
                    val sql = args?.optString("query")?.ifBlank { null }
                        ?: args?.optString("sql")?.ifBlank { null }
                    if (!sql.isNullOrBlank()) {
                        executeQuery(sql)
                    } else {
                        JSONObject().put("error", "Tham số 'query' không được để trống").toString()
                    }
                } else {
                    JSONObject().put("error", "Không hỗ trợ công cụ: $funcName").toString()
                }

                messages.put(
                    JSONObject().apply {
                        put("role", "tool")
                        put("tool_call_id", callId)
                        put("content", resultJson)
                    },
                )
            }
        }

        throw IOException("Đã vượt quá số lượt truy vấn dữ liệu cho phép")
    }

    private fun systemPrompt(totalCount: Int): String {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val todayStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val currentMonthStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val dayOfWeek = now.dayOfWeek.name

        return """
            Bạn là trợ lý tài chính thông minh của ứng dụng Moneycheck.
            Thời điểm hiện tại: $todayStr ($dayOfWeek), tháng hiện tại: $currentMonthStr.
            Tổng số giao dịch đã lưu trong máy: $totalCount giao dịch.

            BẠN CÓ CÔNG CỤ: `execute_read_only_sql` để truy vấn trực tiếp cơ sở dữ liệu SQLite cục bộ.
            Khi người dùng hỏi về thu chi, số tiền, phân tích, thống kê, tìm kiếm giao dịch hay thói quen chi tiêu, hãy LUÔN dùng tool `execute_read_only_sql` để lấy số liệu chính xác thay vì tự đoán.

            SCHEMA VIEW `v_transactions` (chuẩn hóa ngày giờ theo giờ Việt Nam):
            - id: INTEGER (Khóa chính)
            - direction: TEXT ('income' là tiền vào / thu, 'expense' là tiền ra / chi)
            - amount: INTEGER (Số tiền VNĐ, ví dụ: 50000, 1200000)
            - recipient: TEXT (Tên người nhận, bên thụ hưởng, người gửi hoặc cửa hàng)
            - purpose: TEXT (Nội dung chi tiết, mục đích giao dịch, tên món hàng)
            - appName: TEXT (Tên ứng dụng: Vietcombank, Shopee, MoMo, Tiền mặt...)
            - transaction_date: TEXT (Ngày dạng 'YYYY-MM-DD', ví dụ: '$todayStr')
            - transaction_clock: TEXT (Giờ dạng 'HH:MM:SS')
            - transactionTime: INTEGER (Timestamp epoch mili-giây)

            HƯỚNG DẪN TRUY VẤN SQLITE:
            - LUÔN truy vấn trực tiếp view `v_transactions` (không cần kiểm tra sqlite_master).
            - Chi tiêu / Tiền ra: `direction = 'expense'`
            - Thu nhập / Tiền vào: `direction = 'income'`
            - Hôm nay: `transaction_date = '$todayStr'`
            - Tháng này: `transaction_date LIKE '$currentMonthStr%'`
            - Tính tổng tiền: Dùng `SUM(amount)`
            - Đếm số giao dịch: Dùng `COUNT(*)`
            - Phân tích dòng tiền (Cash flow): Truy vấn tổng thu, tổng chi `GROUP BY direction`, hoặc theo tháng `strftime('%Y-%m', transaction_date)`.
            - Thói quen (ví dụ "hay ăn gì", "hay mua ở đâu"): Hãy gom nhóm `GROUP BY recipient, purpose`, đếm `COUNT(*)` và sắp xếp `ORDER BY COUNT(*) DESC, SUM(amount) DESC LIMIT 20`.

            QUY TẮC PHẢN HỒI:
            - Trả lời bằng tiếng Việt thân thiện, rõ ràng, có cấu trúc (gạch đầu dòng, định dạng tiền tệ như 50.000 đ, emoji trực quan).
            - Sau khi nhận dữ liệu từ các lệnh SQL, hãy tổng hợp câu trả lời chi tiết và đầy đủ cho người dùng.
            - Nếu cơ sở dữ liệu không có giao dịch phù hợp, hãy thông báo rõ ràng cho người dùng.
            - Không bịa đặt số liệu không có trong kết quả truy vấn.
        """.trimIndent()
    }

    private fun createTools(): JSONArray = JSONArray().apply {
        put(
            JSONObject().apply {
                put("type", "function")
                put(
                    "function",
                    JSONObject().apply {
                        put("name", "execute_read_only_sql")
                        put(
                            "description",
                            "Thực thi câu lệnh SQL SELECT trên SQLite cục bộ để truy vấn, thống kê, tính tổng tiền (SUM), đếm (COUNT), tìm kiếm, lọc theo ngày/tháng, gom nhóm theo mục đích/người nhận.",
                        )
                        put(
                            "parameters",
                            JSONObject().apply {
                                put("type", "object")
                                put(
                                    "properties",
                                    JSONObject().apply {
                                        put(
                                            "query",
                                            JSONObject().apply {
                                                put("type", "string")
                                                put(
                                                    "description",
                                                    "Câu lệnh SQL SELECT hợp lệ cho SQLite. Bảng view: v_transactions(id, direction, amount, recipient, purpose, appName, transaction_date, transaction_clock, transactionTime).",
                                                )
                                            },
                                        )
                                    },
                                )
                                put("required", JSONArray().put("query"))
                            },
                        )
                    },
                )
            },
        )
    }

    private fun postJson(
        apiBaseUrl: String,
        authBearerToken: String,
        request: JSONObject,
    ): JSONObject {
        val connection = (URL(OpenAiCompatibleEndpoint.url(apiBaseUrl, "chat/completions"))
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${authBearerToken.trim()}")
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
                val apiMessage = extractApiError(responseBody)
                throw IOException(apiMessage?.takeIf(String::isNotBlank) ?: "API HTTP $statusCode")
            }

            return parseJsonValue(responseBody) as? JSONObject
                ?: throw IOException("API không trả về định dạng JSON hợp lệ")
        } finally {
            connection.disconnect()
        }
    }

    private fun streamResponse(
        apiBaseUrl: String,
        authBearerToken: String,
        request: JSONObject,
        onDelta: (String) -> Unit,
    ): String {
        val connection = (URL(OpenAiCompatibleEndpoint.url(apiBaseUrl, "chat/completions"))
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${authBearerToken.trim()}")
            setRequestProperty("Content-Type", "application/json")
        }

        try {
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(request.toString()) }
            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                val responseBody = connection.errorStream
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    .orEmpty()
                val apiMessage = extractApiError(responseBody)
                throw IOException(apiMessage?.takeIf(String::isNotBlank) ?: "API HTTP $statusCode")
            }

            val streamedText = StringBuilder()
            val fallbackBody = StringBuilder()
            var receivedSse = false

            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("data:")) {
                        if (!receivedSse && trimmed.isNotEmpty()) fallbackBody.appendLine(line)
                        continue
                    }

                    receivedSse = true
                    val payload = trimmed.removePrefix("data:").trim()
                    if (payload.isEmpty() || payload == "[DONE]") continue
                    val delta = parseJsonValue(payload)?.let(::extractDeltaText).orEmpty()
                    if (delta.isNotEmpty()) {
                        streamedText.append(delta)
                        onDelta(delta)
                    }
                }
            }

            if (receivedSse) {
                return streamedText.toString().takeIf(String::isNotBlank)
                    ?: throw IOException("API không trả về nội dung trả lời")
            }
            return extractResponseText(fallbackBody.toString())
                ?: throw IOException("API không trả về nội dung trả lời")
        } finally {
            connection.disconnect()
        }
    }

    private fun extractDeltaText(value: Any?): String? = when (value) {
        is JSONObject -> {
            val choices = value.opt("choices") as? JSONArray
            val firstChoice = choices?.opt(0) as? JSONObject
            val delta = firstChoice?.opt("delta") as? JSONObject
            extractText(delta?.opt("content")) ?: extractText(firstChoice?.opt("text"))
        }
        else -> extractText(value)
    }

    private fun extractResponseText(body: String): String? {
        val trimmed = body.trim()
        if (trimmed.startsWith("data:")) {
            return trimmed.lineSequence()
                .map(String::trim)
                .filter { it.startsWith("data:") }
                .map { it.removePrefix("data:").trim() }
                .filter { it.isNotEmpty() && it != "[DONE]" }
                .mapNotNull { parseJsonValue(it)?.let(::extractText) }
                .joinToString("")
                .takeIf(String::isNotEmpty)
        }
        return parseJsonValue(trimmed)?.let(::extractText)
    }

    private fun extractText(value: Any?): String? = when (value) {
        is String -> {
            val text = value.trim()
            if (text.startsWith("{") || text.startsWith("[")) {
                parseJsonValue(text)?.let(::extractText) ?: text
            } else {
                text.takeIf(String::isNotEmpty)
            }
        }
        is JSONObject -> {
            val choices = value.opt("choices") as? JSONArray
            val firstChoice = choices?.opt(0) as? JSONObject
            val message = firstChoice?.opt("message")
            val messageContent = when (message) {
                is JSONObject -> message.opt("content") ?: message.opt("reasoning_content") ?: message.opt("thought")
                else -> message
            }
            extractText(messageContent)
                ?: extractText((firstChoice?.opt("delta") as? JSONObject)?.opt("content"))
                ?: extractText((firstChoice?.opt("delta") as? JSONObject)?.opt("reasoning_content"))
                ?: extractText((firstChoice?.opt("delta") as? JSONObject)?.opt("thought"))
                ?: extractText(firstChoice?.opt("text"))
                ?: extractText(value.opt("content"))
                ?: extractText(value.opt("reasoning_content"))
                ?: extractText(value.opt("thought"))
                ?: extractText(value.opt("output_text"))
                ?: extractText(value.opt("content"))
                ?: extractText(value.opt("answer"))
                ?: extractText(value.opt("response"))
                ?: extractText(value.opt("data"))
                ?: extractText(value.opt("text"))
        }
        is JSONArray -> buildString {
            for (index in 0 until value.length()) {
                val text = extractText(value.opt(index)) ?: continue
                if (isNotEmpty()) append('\n')
                append(text)
            }
        }.takeIf(String::isNotEmpty)
        else -> null
    }

    private fun extractApiError(body: String): String? {
        val root = parseJsonValue(body) as? JSONObject ?: return null
        return when (val error = root.opt("error")) {
            is JSONObject -> extractText(error.opt("message"))
            else -> extractText(error)
        }
    }

    private fun parseJsonValue(value: String): Any? = runCatching {
        JSONTokener(value).nextValue()
    }.getOrNull()

    companion object {
        private const val MAX_TRANSACTIONS = 500
        private const val MAX_HISTORY_MESSAGES = 12
    }
}
