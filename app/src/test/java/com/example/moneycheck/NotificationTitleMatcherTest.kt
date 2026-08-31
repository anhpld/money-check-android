package com.example.moneycheck.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationTitleMatcherTest {
    @Test
    fun emptyConfiguration_matchesEveryTitle() {
        assertTrue(notificationTitleMatches(emptySet(), "Biến động số dư"))
    }

    @Test
    fun comparison_ignoresCaseAndOuterWhitespace() {
        assertTrue(
            notificationTitleMatches(
                setOf("  Biến động số dư  ", "Chuyển tiền thành công"),
                "biẾn ĐỘng SỐ DƯ ",
            ),
        )
    }

    @Test
    fun multipleTitles_matchWhenAnyTitleIsEqual() {
        assertTrue(
            notificationTitleMatches(
                setOf("Tiền vào", "Giao dịch thành công"),
                "GIAO DỊCH THÀNH CÔNG",
            ),
        )
    }

    @Test
    fun partialTitle_doesNotMatch() {
        assertFalse(notificationTitleMatches(setOf("Biến động số dư"), "Thông báo biến động số dư"))
    }
}
