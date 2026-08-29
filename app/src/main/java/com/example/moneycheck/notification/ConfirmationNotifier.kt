package com.example.moneycheck.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.moneycheck.MainActivity
import java.text.NumberFormat
import java.util.Locale

object ConfirmationNotifier {
    private const val CHANNEL_ID = "transaction_confirmation"

    fun show(context: Context, notificationId: Long, direction: String, amount: Long?, purpose: String): Boolean {
        ensureChannel(context)
        if (!canNotify(context)) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_CONFIRM_NOTIFICATION_ID, notificationId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (notificationId and 0x7fffffff).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val directionText = when (direction) {
            "income" -> "Tiền vào"
            "expense" -> "Tiền ra"
            else -> "Giao dịch"
        }
        val amountText = amount?.let {
            NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(it) + " đ"
        }
        val summary = listOfNotNull(directionText, amountText, purpose.takeIf(String::isNotBlank))
            .joinToString(" · ")
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Xác nhận giao dịch")
            .setContentText(summary.take(120))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId.hashCode(), notification)
        return true
    }

    fun cancel(context: Context, notificationId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId.hashCode())
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Xác nhận giao dịch",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Thông báo khi Money check cần bạn xác nhận một giao dịch"
            },
        )
    }

    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = manager.getNotificationChannel(CHANNEL_ID)
            if (channel?.importance == NotificationManager.IMPORTANCE_NONE) return false
        }
        return true
    }
}
