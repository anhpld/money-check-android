package com.example.moneycheck.llm

import com.example.moneycheck.ui.TransactionInitialDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisionExtractionTest {
    @Test
    fun defaultVisionPromptContainsEssentialFields() {
        val prompt = OpenAiClient.DEFAULT_VISION_PROMPT
        assertNotNull(prompt)
        assertTrue(prompt.contains("direction"))
        assertTrue(prompt.contains("amount"))
        assertTrue(prompt.contains("recipient"))
        assertTrue(prompt.contains("purpose"))
        assertTrue(prompt.contains("app_name"))
        assertTrue(prompt.contains("transaction_time"))
    }

    @Test
    fun visionExtractedTransactionDefaults() {
        val extracted = VisionExtractedTransaction()
        assertEquals("unknown", extracted.direction)
        assertEquals(null, extracted.amount)
        assertEquals("", extracted.recipient)
        assertEquals("", extracted.purpose)
        assertEquals("", extracted.appName)
        assertEquals(null, extracted.transactionTime)
    }

    @Test
    fun transactionInitialDraftMapping() {
        val draft = TransactionInitialDraft(
            direction = "expense",
            amount = 150000L,
            recipient = "Nguyen Van A",
            purpose = "Tien an trua",
            appName = "Vietcombank",
            packageName = "com.VCB",
            transactionTime = 1700000000000L,
        )
        assertEquals("expense", draft.direction)
        assertEquals(150000L, draft.amount)
        assertEquals("Nguyen Van A", draft.recipient)
        assertEquals("Tien an trua", draft.purpose)
        assertEquals("Vietcombank", draft.appName)
        assertEquals("com.VCB", draft.packageName)
        assertEquals(1700000000000L, draft.transactionTime)
    }
}
