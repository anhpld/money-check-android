package com.example.moneycheck.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

internal data class CleanScreenNode(
    val order: Int,
    val text: String,
    val parentOrder: Int? = null,
    val depth: Int = 0,
    val contentDescription: String = "",
    val resourceId: String = "",
    val className: String = "",
    val hint: String = "",
    val stateDescription: String = "",
    val paneTitle: String = "",
    val tooltip: String = "",
    val bounds: String? = null,
)

internal data class CleanedScreenXml(
    val xml: String,
    val retainedNodeCount: Int,
)

internal object AccessibilityScreenXmlCleaner {
    private data class PendingNode(
        val node: AccessibilityNodeInfo,
        val parentRetainedOrder: Int?,
        val depth: Int,
    )

    fun clean(root: AccessibilityNodeInfo, appName: String): CleanedScreenXml {
        val packageName = root.packageName?.toString().orEmpty()
        val pending = ArrayDeque<PendingNode>()
        val snapshots = ArrayList<CleanScreenNode>()
        val seen = HashSet<String>()
        pending.add(PendingNode(root, parentRetainedOrder = null, depth = 0))
        var visited = 0
        var retainedCharacters = 0

        while (pending.isNotEmpty() && visited < MAX_VISITED_NODES && snapshots.size < MAX_TEXT_NODES) {
            val current = pending.removeFirst()
            val node = current.node
            visited++

            val text = ScreenXmlSanitizer.cleanText(node.text?.toString().orEmpty())
            val description = ScreenXmlSanitizer.cleanText(node.contentDescription?.toString().orEmpty())
            val resourceId = node.viewIdResourceName.orEmpty()
            val className = node.className?.toString().orEmpty()
            val hint = ScreenXmlSanitizer.cleanText(node.hintText?.toString().orEmpty())
            val stateDescription = ScreenXmlSanitizer.cleanText(node.stateDescription?.toString().orEmpty())
            val paneTitle = ScreenXmlSanitizer.cleanText(node.paneTitle?.toString().orEmpty())
            val tooltip = ScreenXmlSanitizer.cleanText(node.tooltipText?.toString().orEmpty())
            val hasSemanticContent = text.isNotEmpty() || description.isNotEmpty() ||
                resourceId.isNotEmpty() || hint.isNotEmpty() || stateDescription.isNotEmpty() ||
                paneTitle.isNotEmpty() || tooltip.isNotEmpty()
            var retainedOrder = current.parentRetainedOrder
            if (hasSemanticContent) {
                val rect = Rect().also(node::getBoundsInScreen)
                val bounds = rect.takeIf { it.right > it.left && it.bottom > it.top }
                    ?.let { "[${it.left},${it.top}][${it.right},${it.bottom}]" }
                val dedupKey = listOf(
                    text,
                    description,
                    resourceId,
                    className,
                    hint,
                    stateDescription,
                    paneTitle,
                    tooltip,
                    bounds.orEmpty(),
                    current.parentRetainedOrder?.toString().orEmpty(),
                ).joinToString("\u0000")
                val estimatedCharacters = text.length + description.length + resourceId.length +
                    className.length + hint.length + stateDescription.length + paneTitle.length +
                    tooltip.length + 160
                if (retainedCharacters + estimatedCharacters <= MAX_XML_CONTENT_CHARACTERS && seen.add(dedupKey)) {
                    retainedOrder = snapshots.size
                    snapshots += CleanScreenNode(
                        order = retainedOrder,
                        parentOrder = current.parentRetainedOrder,
                        depth = current.depth,
                        text = text,
                        contentDescription = description.takeUnless { it == text }.orEmpty(),
                        resourceId = resourceId,
                        className = className,
                        hint = hint,
                        stateDescription = stateDescription,
                        paneTitle = paneTitle,
                        tooltip = tooltip,
                        bounds = bounds,
                    )
                    retainedCharacters += estimatedCharacters
                }
            }

            for (index in 0 until node.childCount) {
                node.getChild(index)?.let { child ->
                    pending.addLast(
                        PendingNode(
                            node = child,
                            parentRetainedOrder = retainedOrder,
                            depth = current.depth + 1,
                        ),
                    )
                }
            }
        }

        val xml = ScreenXmlSanitizer.toXml(
            packageName = packageName,
            appName = appName,
            originalNodeCount = visited,
            nodes = snapshots,
        )
        return CleanedScreenXml(xml = xml, retainedNodeCount = snapshots.size)
    }

    private const val MAX_VISITED_NODES = 2_000
    private const val MAX_TEXT_NODES = 600
    private const val MAX_XML_CONTENT_CHARACTERS = 40_000
}

internal object ScreenXmlSanitizer {
    private val phonePattern = Regex("""(?<!\d)\(?(?:\+?84|0)\)?(?:[\s.\-]?\d){8,10}(?!\d)""")
    private val emailPattern = Regex("""[A-Z0-9._%+\-]+@[A-Z0-9.\-]+\.[A-Z]{2,}""", RegexOption.IGNORE_CASE)
    private val whitespacePattern = Regex("""\s+""")
    private val meaninglessPattern = Regex("""^[\s:|,.;\-–—]+$""")

    fun cleanText(value: String): String {
        if (value.isBlank()) return ""
        val normalized = value.replace(whitespacePattern, " ").trim()
        if (normalized.isEmpty() || meaninglessPattern.matches(normalized)) return ""
        return normalized
            .replace(phonePattern, "[PHONE_REDACTED]")
            .replace(emailPattern, "[EMAIL_REDACTED]")
            .take(MAX_TEXT_LENGTH)
    }

    fun toXml(
        packageName: String,
        appName: String,
        originalNodeCount: Int,
        nodes: List<CleanScreenNode>,
    ): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<screen package=\"").append(escape(packageName)).append("\"")
        append(" app_name=\"").append(escape(appName)).append("\"")
        append(" source=\"accessibility\" sanitized=\"true\"")
        append(" original_node_count=\"").append(originalNodeCount).append("\"")
        append(" retained_node_count=\"").append(nodes.size).append("\">\n")
        nodes.forEach { node ->
            append("  <node order=\"").append(node.order).append("\"")
            node.parentOrder?.let { append(" parent_order=\"").append(it).append("\"") }
            append(" depth=\"").append(node.depth).append("\"")
            node.resourceId.takeIf(String::isNotEmpty)?.let {
                append(" resource_id=\"").append(escape(it)).append("\"")
            }
            node.className.takeIf(String::isNotEmpty)?.let {
                append(" class=\"").append(escape(it)).append("\"")
            }
            append(" text=\"").append(escape(node.text)).append("\"")
            node.contentDescription.takeIf(String::isNotEmpty)?.let {
                append(" content_description=\"").append(escape(it)).append("\"")
            }
            node.hint.takeIf(String::isNotEmpty)?.let {
                append(" hint=\"").append(escape(it)).append("\"")
            }
            node.stateDescription.takeIf(String::isNotEmpty)?.let {
                append(" state_description=\"").append(escape(it)).append("\"")
            }
            node.paneTitle.takeIf(String::isNotEmpty)?.let {
                append(" pane_title=\"").append(escape(it)).append("\"")
            }
            node.tooltip.takeIf(String::isNotEmpty)?.let {
                append(" tooltip=\"").append(escape(it)).append("\"")
            }
            node.bounds?.let { append(" bounds=\"").append(escape(it)).append("\"") }
            append(" />\n")
        }
        append("</screen>")
    }

    internal fun escape(value: String): String = value
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("'", "&apos;")

    private const val MAX_TEXT_LENGTH = 1_500
}
