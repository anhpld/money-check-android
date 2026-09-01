package com.example.moneycheck.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTransactionExtractorTest {
    @Test
    fun shopeePayment_extractsOnlyPaymentAndConfirmation() {
        val result = ScreenTransactionExtractor.extract(
            ScreenTransactionExtractor.SHOPEE_PACKAGE,
            listOf(
                "Bạn vừa thanh toán ₫332.000",
                "Xác nhận thanh toán thành công bằng SPayLater!",
                "Số dư khả dụng hiện tại ₫24.404.663",
                "102.000",
                "₫",
            ),
        )

        requireNotNull(result)
        assertEquals("Bạn vừa thanh toán ₫332.000", result.title)
        assertTrue(result.expandedContent.contains("SPayLater"))
        assertTrue(!result.expandedContent.contains("24.404.663"))
        assertTrue(!result.expandedContent.contains("102.000"))
    }

    @Test
    fun shopeeScreen_withoutConfirmation_isIgnored() {
        assertNull(
            ScreenTransactionExtractor.extract(
                ScreenTransactionExtractor.SHOPEE_PACKAGE,
                listOf("Bạn vừa thanh toán ₫15.950", "Tiếp tục mua sắm"),
            ),
        )
    }

    @Test
    fun unsupportedPackage_isIgnored() {
        assertNull(
            ScreenTransactionExtractor.extract(
                "com.example.other",
                listOf("Bạn vừa thanh toán ₫15.950", "Xác nhận thanh toán thành công bằng SPayLater!"),
            ),
        )
    }
}
