package com.example.moneycheck.accessibility

import android.content.Context

data class ScreenCaptureSession(
    val startedAt: Long,
)

class ScreenCaptureSessionStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun start() {
        preferences.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putLong(KEY_STARTED_AT, System.currentTimeMillis())
            .apply()
    }

    fun active(): ScreenCaptureSession? {
        if (!preferences.getBoolean(KEY_ACTIVE, false)) return null
        val startedAt = preferences.getLong(KEY_STARTED_AT, 0L)
        if (startedAt <= 0L) return null
        return ScreenCaptureSession(startedAt = startedAt)
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        const val EVENT_PREFIX = "manual-screen:"
        const val SOURCE = "manual_screen"
        private const val PREFERENCES = "manual_screen_capture_session"
        private const val KEY_ACTIVE = "active"
        private const val KEY_STARTED_AT = "started_at"
        fun isManualScreenEvent(eventId: String): Boolean = eventId.startsWith(EVENT_PREFIX)
    }
}
