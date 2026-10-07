package com.example.moneycheck.llm

import java.text.Normalizer
import java.util.Locale

internal fun exactTransactionCountAnswer(question: String, count: Int): String? {
    val normalized = Normalizer.normalize(question, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9 ]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    val asksForCount = listOf("bao nhieu", "tong so", "so luong", "dem").any { it in normalized }
    if (!asksForCount || !Regex("\\bgiao dich\\b").containsMatchIn(normalized)) return null

    val scopedRequest = listOf(
        "thang", "tuan", "ngay", "nam", "quy", "hom nay", "hom qua", "gan day",
        "tu ngay", "den ngay", "chi", "thu", "tien vao", "tien ra", "theo app",
        "cua app", "nguoi nhan", "khoan",
    ).any { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(normalized) }
    if (scopedRequest) return null

    return "Hiện có $count giao dịch đã lưu."
}
