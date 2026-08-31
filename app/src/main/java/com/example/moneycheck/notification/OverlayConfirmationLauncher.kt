package com.example.moneycheck.notification

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.moneycheck.OverlayConfirmationActivity

object OverlayConfirmationLauncher {
    fun show(context: Context, notificationId: Long): Boolean {
        if (!Settings.canDrawOverlays(context)) return false
        return runCatching {
            context.startActivity(
                Intent(context, OverlayConfirmationActivity::class.java).apply {
                    putExtra(OverlayConfirmationActivity.EXTRA_NOTIFICATION_ID, notificationId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                },
            )
            true
        }.getOrDefault(false)
    }
}
