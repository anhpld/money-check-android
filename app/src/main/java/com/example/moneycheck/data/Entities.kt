package com.example.moneycheck.data

object AnalysisStatus {
    const val PENDING = "pending"
    const val PROCESSING = "processing"
    const val READY = "ready"
    const val IGNORED = "ignored"
    const val CONFIRMED = "confirmed"
    const val ERROR = "error"
}

data class CapturedNotificationEntity(
    val id: Long = 0,
    val eventId: String,
    val notificationKey: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val expandedContent: String,
    val rawPayload: String,
    val postedAt: Long,
    val capturedAt: Long = System.currentTimeMillis(),
    val isSaved: Boolean = false,
    val analysisStatus: String = AnalysisStatus.PENDING,
    val handled: Boolean = false,
    val errorMessage: String? = null,
)

data class ExtractedDraftEntity(
    val notificationId: Long,
    val direction: String,
    val amount: Long?,
    val purpose: String,
    val recipient: String,
    val rawModelJson: String,
    val analyzedAt: Long = System.currentTimeMillis(),
)

data class TransactionEntity(
    val id: Long = 0,
    val sourceNotificationId: Long?,
    val direction: String,
    val amount: Long,
    val recipient: String,
    val purpose: String,
    val appName: String,
    val transactionTime: Long,
    val llmInputJson: String,
    val confirmedAt: Long = System.currentTimeMillis(),
)

data class NotificationWithDraft(
    val notification: CapturedNotificationEntity,
    val draft: ExtractedDraftEntity?,
)
