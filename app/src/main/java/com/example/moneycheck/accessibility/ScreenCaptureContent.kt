package com.example.moneycheck.accessibility

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal enum class ScreenExtractionMode(val wireValue: String) {
    ACCESSIBILITY("accessibility"),
    OCR("ocr"),
    ACCESSIBILITY_OCR("accessibility_ocr"),
}

internal data class OcrScreenLine(
    val order: Int,
    val text: String,
    val bounds: String? = null,
)

internal class OnDeviceScreenTextRecognizer {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap): List<OcrScreenLine> = suspendCancellableCoroutine { continuation ->
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { result ->
                if (!continuation.isActive) return@addOnSuccessListener
                val lines = result.textBlocks
                    .flatMap { block -> block.lines }
                    .mapIndexedNotNull { index, line ->
                        val text = ScreenXmlSanitizer.cleanText(line.text)
                        text.takeIf(String::isNotEmpty)?.let {
                            OcrScreenLine(
                                order = index,
                                text = it,
                                bounds = line.boundingBox?.toBoundsString(),
                            )
                        }
                    }
                continuation.resume(lines)
            }
            .addOnFailureListener { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            }
    }

    fun close() = recognizer.close()

    private fun Rect.toBoundsString(): String = "[$left,$top][$right,$bottom]"
}

internal object ScreenCaptureXmlComposer {
    fun compose(
        packageName: String,
        appName: String,
        mode: ScreenExtractionMode,
        accessibilityXml: String? = null,
        ocrLines: List<OcrScreenLine> = emptyList(),
    ): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<screen_capture package=\"").append(ScreenXmlSanitizer.escape(packageName)).append("\"")
        append(" app_name=\"").append(ScreenXmlSanitizer.escape(appName)).append("\"")
        append(" extraction_mode=\"").append(mode.wireValue).append("\">\n")
        accessibilityXml?.takeIf(String::isNotBlank)?.let { xml ->
            append("  <accessibility>\n")
            append(indent(stripDeclaration(xml), 4)).append('\n')
            append("  </accessibility>\n")
        }
        if (ocrLines.isNotEmpty()) {
            append("  <ocr line_count=\"").append(ocrLines.size).append("\">\n")
            ocrLines.forEach { line ->
                append("    <line order=\"").append(line.order).append("\"")
                line.bounds?.let { append(" bounds=\"").append(ScreenXmlSanitizer.escape(it)).append("\"") }
                append(" text=\"").append(ScreenXmlSanitizer.escape(line.text)).append("\" />\n")
            }
            append("  </ocr>\n")
        }
        append("</screen_capture>")
    }

    private fun stripDeclaration(xml: String): String = xml.trim()
        .replaceFirst(Regex("""^<\?xml[^>]*>\s*"""), "")

    private fun indent(value: String, spaces: Int): String {
        val prefix = " ".repeat(spaces)
        return value.lineSequence().joinToString("\n") { line -> prefix + line }
    }
}
