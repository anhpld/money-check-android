package com.example.moneycheck

import com.example.moneycheck.accessibility.CleanScreenNode
import com.example.moneycheck.accessibility.ScreenXmlSanitizer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenXmlSanitizerTest {
    @Test
    fun `compact xml keeps semantic fields and escapes content`() {
        val xml = ScreenXmlSanitizer.toXml(
            packageName = "com.example.shop",
            appName = "Shop & Pay",
            originalNodeCount = 120,
            nodes = listOf(
                CleanScreenNode(0, "Thành tiền:", depth = 4, bounds = "[10,20][200,80]"),
                CleanScreenNode(1, "248.498₫", parentOrder = 0, depth = 5, resourceId = "labelTotal"),
            ),
        )

        assertTrue(xml.contains("app_name=\"Shop &amp; Pay\""))
        assertTrue(xml.contains("resource_id=\"labelTotal\""))
        assertTrue(xml.contains("text=\"248.498₫\""))
        assertTrue(xml.contains("parent_order=\"0\""))
        assertTrue(xml.contains("depth=\"5\""))
        assertFalse(xml.contains("clickable="))
    }

    @Test
    fun `sensitive contact data is redacted`() {
        val cleaned = ScreenXmlSanitizer.cleanText("Đức Anh (+84) 978 618 991")

        assertTrue(cleaned.contains("[PHONE_REDACTED]"))
        assertFalse(cleaned.contains("978 618 991"))
    }

    @Test
    fun `meaningless punctuation is removed`() {
        assertTrue(ScreenXmlSanitizer.cleanText(" : ").isEmpty())
    }
}
