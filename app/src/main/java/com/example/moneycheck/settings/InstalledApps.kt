package com.example.moneycheck.settings

import android.content.Context
import android.content.Intent

data class InstalledApp(
    val packageName: String,
    val label: String,
)

fun loadLaunchableApps(context: Context): List<InstalledApp> {
    val packageManager = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return packageManager.queryIntentActivities(intent, 0)
        .map { info ->
            InstalledApp(
                packageName = info.activityInfo.packageName,
                label = info.loadLabel(packageManager).toString(),
            )
        }
        .filterNot { it.packageName == context.packageName }
        .distinctBy(InstalledApp::packageName)
        .sortedBy { it.label.lowercase() }
}
