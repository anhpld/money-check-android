package com.example.moneycheck.notification

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.StatusBarNotification
import com.example.moneycheck.data.CapturedNotificationEntity
import org.json.JSONArray
import org.json.JSONObject

internal object NotificationContentExtractor {
    @Suppress("DEPRECATION")
    fun extract(context: Context, sbn: StatusBarNotification): CapturedNotificationEntity? {
        val notification = sbn.notification ?: return null
        val extras = notification.extras
        val appName = applicationLabel(context, sbn.packageName)
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.map { it.toString().trim() }
            ?.filter(String::isNotEmpty)
            .orEmpty()
        val messages = runCatching {
            Notification.MessagingStyle.Message.getMessagesFromBundleArray(
                extras.getParcelableArray(Notification.EXTRA_MESSAGES),
            )
        }.getOrDefault(emptyList()).mapNotNull { message ->
            val messageText = message.text?.toString()?.trim().orEmpty()
            if (messageText.isEmpty()) null else {
                val sender = message.sender?.toString()?.trim().orEmpty()
                if (sender.isEmpty()) messageText else "$sender\n$messageText"
            }
        }
        val expanded = when {
            bigText.isNotEmpty() -> bigText
            messages.isNotEmpty() -> messages.joinToString("\n\n")
            textLines.isNotEmpty() -> textLines.joinToString("\n")
            else -> text
        }

        val rawPayload = JSONObject().apply {
            put("packageName", sbn.packageName)
            put("appName", appName)
            put("title", title)
            put("text", text)
            put("bigText", bigText)
            put("textLines", JSONArray(textLines))
            put("messages", JSONArray(messages))
            put("expandedContent", expanded)
            put("postedAt", sbn.postTime)
            put("category", notification.category ?: JSONObject.NULL)
            put("channelId", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) notification.channelId else JSONObject.NULL)
            put("group", notification.group ?: JSONObject.NULL)
            put("isOngoing", sbn.isOngoing)
            put("isClearable", sbn.isClearable)
        }.toString()

        return CapturedNotificationEntity(
            eventId = "${sbn.key}:${sbn.postTime}",
            notificationKey = sbn.key,
            packageName = sbn.packageName,
            appName = appName,
            title = title.ifBlank { appName },
            text = text,
            expandedContent = expanded,
            rawPayload = rawPayload,
            postedAt = sbn.postTime,
        )
    }

    private fun applicationLabel(context: Context, packageName: String): String = try {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(info).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        packageName
    }
}
