package com.example.moneycheck.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionValidationTest {
    @Test
    fun `manual cash entry does not require app or recipient`() {
        val result = validateManualTransaction(
            amountInput = "125000",
            direction = "expense",
            recipient = "",
            purpose = "Ăn trưa",
            appName = "",
            packageName = "",
        )

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `amount and purpose errors are exposed inline`() {
        val result = validateManualTransaction(
            amountInput = "0",
            direction = "expense",
            recipient = "",
            purpose = " ",
        )

        assertEquals("Nhập số tiền lớn hơn 0", result.errors[ManualTransactionField.AMOUNT])
        assertEquals("Nhập nội dung giao dịch", result.errors[ManualTransactionField.PURPOSE])
    }

    @Test
    fun `direction must be income or expense`() {
        val result = validateManualTransaction(
            amountInput = "1000",
            direction = "unknown",
            recipient = "",
            purpose = "Hoàn tiền",
        )

        assertEquals("Chọn chiều giao dịch", result.errors[ManualTransactionField.DIRECTION])
    }
}
