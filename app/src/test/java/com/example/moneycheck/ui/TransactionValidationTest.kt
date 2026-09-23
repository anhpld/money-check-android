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

    @Test
    fun `unchanged draft incorporates inbox title and new app`() {
        val original = mapOf("bank" to "Old")
        val persisted = mapOf("bank" to "Old\nNew", "wallet" to "Payment")
        assertEquals(persisted, reconcileDraftMap(original, original, persisted))
    }

    @Test
    fun `local deletion and edits survive unrelated external additions`() {
        val original = mapOf("bank" to "Old", "removed" to "Title")
        val draft = mapOf("bank" to "Edited")
        val persisted = original + ("wallet" to "Payment")
        assertEquals(draft + ("wallet" to "Payment"), reconcileDraftMap(draft, original, persisted))
    }

    @Test
    fun `external removal is applied to an unchanged field`() {
        val original = mapOf("bank" to "Old")
        assertEquals(emptyMap<String, String>(), reconcileDraftMap(original, original, emptyMap()))
    }

    @Test
    fun `save advances baseline for later external edits`() {
        val saved = mapOf("bank" to "Edited")
        val persisted = saved + ("bank" to "Edited\nNew")
        assertEquals(persisted, reconcileDraftMap(saved, saved, persisted))
    }

    @Test
    fun `reconcile draft imports external changes without overwriting dirty fields`() {
        val merged = reconcileDraftMap(
            draft = mapOf("bank" to "Đã sửa cục bộ", "cash" to ""),
            lastPersisted = mapOf("bank" to "Cũ", "cash" to ""),
            persisted = mapOf("bank" to "Cập nhật ngoài", "cash" to "", "wallet" to "Ví điện tử"),
        )

        assertEquals(
            mapOf("bank" to "Đã sửa cục bộ", "cash" to "", "wallet" to "Ví điện tử"),
            merged,
        )
    }
}
