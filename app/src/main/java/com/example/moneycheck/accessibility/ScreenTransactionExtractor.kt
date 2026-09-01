package com.example.moneycheck.accessibility

internal data class ScreenTransactionMatch(
    val title: String,
    val text: String,
    val expandedContent: String,
)

internal object ScreenTransactionExtractor {
    private val shopeePaymentPattern = Regex(
        pattern = """^Bạn\s+vừa\s+thanh\s+toán\s+[₫đ]?\s*([\d.,]+)\s*[₫đ]?\s*$""",
        option = RegexOption.IGNORE_CASE,
    )

    fun extract(packageName: String, visibleTexts: List<String>): ScreenTransactionMatch? = when (packageName) {
        SHOPEE_PACKAGE -> extractShopee(visibleTexts)
        else -> null
    }

    private fun extractShopee(visibleTexts: List<String>): ScreenTransactionMatch? {
        val normalized = visibleTexts.map(String::trim).filter(String::isNotEmpty)
        val paymentText = normalized.firstOrNull { shopeePaymentPattern.matches(it) } ?: return null
        val confirmationText = normalized.firstOrNull {
            it.contains("Xác nhận thanh toán thành công", ignoreCase = true) &&
                it.contains("SPayLater", ignoreCase = true)
        } ?: return null

        return ScreenTransactionMatch(
            title = paymentText,
            text = confirmationText,
            expandedContent = "$paymentText\n$confirmationText",
        )
    }

    const val SHOPEE_PACKAGE = "com.shopee.vn"
}
