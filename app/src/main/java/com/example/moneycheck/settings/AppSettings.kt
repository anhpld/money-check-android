package com.example.moneycheck.settings

import android.content.Context

data class SettingsSnapshot(
    val model: String,
    val hasApiKey: Boolean,
    val enabledPackages: Set<String>,
)

class AppSettings private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val apiKeyStore = ApiKeyStore(appContext)

    fun snapshot(): SettingsSnapshot = SettingsSnapshot(
        model = model(),
        hasApiKey = !apiKey().isNullOrBlank(),
        enabledPackages = enabledPackages(),
    )

    fun model(): String = preferences.getString(KEY_MODEL, DEFAULT_MODEL)
        ?.trim()
        ?.ifBlank { DEFAULT_MODEL }
        ?: DEFAULT_MODEL

    fun saveModel(model: String) {
        preferences.edit().putString(KEY_MODEL, model.trim().ifBlank { DEFAULT_MODEL }).apply()
    }

    fun apiKey(): String? = apiKeyStore.get()

    fun saveApiKey(apiKey: String) = apiKeyStore.save(apiKey)

    fun enabledPackages(): Set<String> =
        preferences.getStringSet(KEY_PACKAGES, emptySet())?.toSet().orEmpty()

    fun saveEnabledPackages(packages: Set<String>) {
        preferences.edit().putStringSet(KEY_PACKAGES, packages.toSet()).apply()
    }

    companion object {
        const val DEFAULT_MODEL = "gpt-5.6-luna"

        private const val PREFS = "money_check_settings"
        private const val KEY_MODEL = "openai_model"
        private const val KEY_PACKAGES = "enabled_notification_packages"

        @Volatile
        private var instance: AppSettings? = null

        fun get(context: Context): AppSettings = instance ?: synchronized(this) {
            instance ?: AppSettings(context.applicationContext).also { instance = it }
        }
    }
}
