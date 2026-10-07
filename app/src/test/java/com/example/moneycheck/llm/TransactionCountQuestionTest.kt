package com.example.moneycheck.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionCountQuestionTest {
    @Test
    fun generalTransactionCountQuestionUsesExactLocalCount() {
        assertEquals(
            "Hiện có 169 giao dịch đã lưu.",
            exactTransactionCountAnswer("Tôi có bao nhiêu giao dịch?", 169),
        )
    }

    @Test
    fun scopedCountQuestionStillGoesToAssistant() {
        assertNull(exactTransactionCountAnswer("Có bao nhiêu giao dịch trong tháng này?", 169))
    }
}
