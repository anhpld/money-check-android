package com.example.moneycheck.llm

import com.example.moneycheck.data.MoneyCheckRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionChatToolTest {
    @Test
    fun allowsSafeSelectStatements() {
        assertNull(MoneyCheckRepository.validateReadOnlySql("SELECT * FROM v_transactions"))
        assertNull(MoneyCheckRepository.validateReadOnlySql("SELECT recipient, sum(amount) FROM v_transactions WHERE direction = 'expense' GROUP BY recipient"))
        assertNull(MoneyCheckRepository.validateReadOnlySql("WITH totals AS (SELECT amount FROM v_transactions) SELECT sum(amount) FROM totals"))
    }

    @Test
    fun blocksDestructiveStatements() {
        assertNotNull(MoneyCheckRepository.validateReadOnlySql("DELETE FROM transactions"))
        assertNotNull(MoneyCheckRepository.validateReadOnlySql("DROP TABLE transactions"))
        assertNotNull(MoneyCheckRepository.validateReadOnlySql("UPDATE transactions SET amount = 0"))
        assertNotNull(MoneyCheckRepository.validateReadOnlySql("INSERT INTO transactions VALUES (1, 2)"))
        assertNotNull(MoneyCheckRepository.validateReadOnlySql("SELECT * FROM v_transactions; DROP TABLE transactions"))
    }
}
