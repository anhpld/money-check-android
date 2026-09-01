package com.example.moneycheck

import com.example.moneycheck.accessibility.OcrScreenLine
import com.example.moneycheck.accessibility.ScreenCaptureXmlComposer
import com.example.moneycheck.accessibility.ScreenExtractionMode
import com.example.moneycheck.settings.AppSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenCaptureContentTest {
    @Test
    fun `combined capture keeps accessibility and OCR in one XML document`() {
        val xml = ScreenCaptureXmlComposer.compose(
            packageName = "vn.com.vng.zalopay",
            appName = "ZaloPay & Wallet",
            mode = ScreenExtractionMode.ACCESSIBILITY_OCR,
            accessibilityXml = """<?xml version="1.0"?><screen><node text="Chi tiết giao dịch" /></screen>""",
            ocrLines = listOf(
                OcrScreenLine(0, "Grab - Thanh toán", "[80,400][900,460]"),
                OcrScreenLine(1, "-48.000đ", "[350,500][700,560]"),
            ),
        )

        assertTrue(xml.contains("extraction_mode=\"accessibility_ocr\""))
        assertTrue(xml.contains("<accessibility>"))
        assertTrue(xml.contains("<ocr line_count=\"2\">"))
        assertTrue(xml.contains("text=\"-48.000đ\""))
        assertTrue(xml.contains("app_name=\"ZaloPay &amp; Wallet\""))
        assertFalse(xml.substringAfter("<screen_capture").contains("<?xml"))
    }

    @Test
    fun `ZaloPay package resolves its dedicated prompt`() {
        val prompt = AppSettings.defaultScreenPrompt("vn.com.vng.zalopay")

        assertTrue(prompt.contains("ZaloPay"))
        assertTrue(prompt.contains("Thời gian"))
    }

    @Test
    fun `unknown package uses generic OCR aware prompt`() {
        val prompt = AppSettings.defaultScreenPrompt("com.example.unknown")

        assertTrue(prompt.contains("OCR"))
    }
}
