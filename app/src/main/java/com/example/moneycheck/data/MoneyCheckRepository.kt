package com.example.moneycheck.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.moneycheck.accessibility.ScreenCaptureSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

class MoneyCheckRepository private constructor(context: Context) {
    private val helper = MoneyCheckOpenHelper(context.applicationContext)
    private val lock = Any()
    private val _notifications = MutableStateFlow<List<NotificationWithDraft>>(emptyList())
    private val _inboxNotifications = MutableStateFlow<List<NotificationWithDraft>>(emptyList())
    private val _savedNotifications = MutableStateFlow<List<NotificationWithDraft>>(emptyList())
    private val _transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())

    val notifications = _notifications.asStateFlow()
    val inboxNotifications = _inboxNotifications.asStateFlow()
    val savedNotifications = _savedNotifications.asStateFlow()
    val transactions = _transactions.asStateFlow()

    init {
        synchronized(lock) {
            backfillMissingTransactionTraces()
            purgeExpiredInbox()
            refreshAll()
        }
    }

    suspend fun capture(notification: CapturedNotificationEntity): Long = io {
        purgeExpiredInbox()
        val id = helper.writableDatabase.insertWithOnConflict(
            "captured_notifications",
            null,
            notification.toValues(),
            SQLiteDatabase.CONFLICT_IGNORE,
        )
        if (id > 0) refreshNotifications()
        id
    }

    suspend fun getNotification(id: Long): NotificationWithDraft? = io { loadNotification(id) }

    /** Creates a fresh temporary row so a saved sample remains immutable and reusable. */
    suspend fun createTestRun(savedNotificationId: Long): Long? = io {
        val sample = loadNotification(savedNotificationId)?.notification
            ?.takeIf { it.isSaved }
            ?: return@io null
        val now = System.currentTimeMillis()
        val runToken = UUID.randomUUID().toString()
        val run = sample.copy(
            id = 0,
            eventId = "test:$runToken",
            notificationKey = "test:${sample.notificationKey}:$runToken",
            postedAt = now,
            capturedAt = now,
            isSaved = false,
            analysisStatus = AnalysisStatus.PENDING,
            handled = false,
            errorMessage = null,
        )
        val id = helper.writableDatabase.insertOrThrow(
            "captured_notifications",
            null,
            run.toValues(),
        )
        refreshNotifications()
        id
    }

    suspend fun saveNotification(id: Long) = io {
        helper.writableDatabase.update(
            "captured_notifications",
            ContentValues().apply { put("isSaved", 1) },
            "id = ?",
            arrayOf(id.toString()),
        )
        refreshNotifications()
    }

    suspend fun deleteNotification(id: Long) = io {
        val notification = loadNotification(id)?.notification
        if (notification?.isSaved == true && notification.analysisStatus == AnalysisStatus.READY) {
            helper.writableDatabase.update(
                "captured_notifications",
                ContentValues().apply { put("isSaved", 0) },
                "id = ?",
                arrayOf(id.toString()),
            )
        } else {
            helper.writableDatabase.delete("captured_notifications", "id = ?", arrayOf(id.toString()))
        }
        refreshNotifications()
    }

    suspend fun markProcessing(id: Long) = io {
        helper.writableDatabase.inTransaction {
            delete("extracted_drafts", "notificationId = ?", arrayOf(id.toString()))
            updateState(id, AnalysisStatus.PROCESSING, handled = false, error = null)
        }
        refreshNotifications()
    }

    suspend fun saveAnalysis(id: Long, draft: ExtractedDraftEntity) = io {
        helper.writableDatabase.inTransaction {
            insertWithOnConflict("extracted_drafts", null, draft.toValues(), SQLiteDatabase.CONFLICT_REPLACE)
            updateState(id, AnalysisStatus.READY, handled = false, error = null)
        }
        refreshNotifications()
    }

    suspend fun saveError(id: Long, message: String) = io {
        helper.writableDatabase.updateState(id, AnalysisStatus.ERROR, handled = false, error = message)
        refreshNotifications()
    }

    suspend fun markHandled(id: Long) = io {
        helper.writableDatabase.update(
            "captured_notifications",
            ContentValues().apply { put("handled", 1) },
            "id = ?",
            arrayOf(id.toString()),
        )
        refreshNotifications()
    }

    suspend fun cancelPendingConfirmation(id: Long) = io {
        helper.writableDatabase.inTransaction {
            delete("extracted_drafts", "notificationId = ?", arrayOf(id.toString()))
            updateState(id, AnalysisStatus.IGNORED, handled = true, error = null)
        }
        refreshNotifications()
    }

    suspend fun cancelPendingConfirmations(ids: Collection<Long>) = io {
        if (ids.isEmpty()) return@io
        helper.writableDatabase.inTransaction {
            ids.distinct().forEach { id ->
                delete("extracted_drafts", "notificationId = ?", arrayOf(id.toString()))
                updateState(id, AnalysisStatus.IGNORED, handled = true, error = null)
            }
        }
        refreshNotifications()
    }

    suspend fun confirm(
        notification: CapturedNotificationEntity,
        direction: String,
        amount: Long,
        recipient: String,
        purpose: String,
        transactionTime: Long,
    ) = io {
        val draft = loadDraft(notification.id)
        val isManualScreen = ScreenCaptureSessionStore.isManualScreenEvent(notification.eventId)
        helper.writableDatabase.inTransaction {
            insertOrThrow(
                "transactions",
                null,
                ContentValues().apply {
                    put("sourceNotificationId", notification.id)
                    put("direction", direction)
                    put("amount", amount)
                    put("recipient", recipient)
                    put("purpose", purpose)
                    put("appName", notification.appName)
                    put("packageName", notification.packageName)
                    put("sourceType", if (isManualScreen) TransactionSource.MANUAL else TransactionSource.AUTOMATIC)
                    put("transactionTime", transactionTime)
                    put(
                        "llmInputJson",
                        if (isManualScreen) {
                            JSONObject(notification.rawPayload).apply {
                                put("model_output", draft?.rawModelJson.orEmpty())
                            }.toString()
                        } else {
                            JSONObject().apply {
                                put("title", notification.title)
                                put("text", notification.text)
                                put("expanded_content", notification.expandedContent)
                            }.toString()
                        },
                    )
                    put("confirmedAt", System.currentTimeMillis())
                },
            )
            updateState(notification.id, AnalysisStatus.CONFIRMED, handled = true, error = null)
        }
        refreshAll()
    }

    suspend fun clearInbox() = io {
        helper.writableDatabase.delete(
            "captured_notifications",
            "isSaved = 0 AND analysisStatus != ?",
            arrayOf(AnalysisStatus.READY),
        )
        refreshNotifications()
    }

    suspend fun clearSavedNotifications() = io {
        helper.writableDatabase.inTransaction {
            update(
                "captured_notifications",
                ContentValues().apply { put("isSaved", 0) },
                "isSaved = 1 AND analysisStatus = ?",
                arrayOf(AnalysisStatus.READY),
            )
            delete("captured_notifications", "isSaved = 1", null)
        }
        refreshNotifications()
    }

    suspend fun deleteTransaction(id: Long) = io {
        helper.writableDatabase.delete("transactions", "id = ?", arrayOf(id.toString()))
        refreshTransactions()
    }

    suspend fun addManualTransaction(
        appName: String,
        packageName: String,
        direction: String,
        amount: Long,
        recipient: String,
        purpose: String,
        transactionTime: Long,
    ) = io {
        helper.writableDatabase.insertOrThrow(
            "transactions",
            null,
            ContentValues().apply {
                putNull("sourceNotificationId")
                put("direction", direction)
                put("amount", amount)
                put("recipient", recipient)
                put("purpose", purpose)
                put("appName", appName)
                put("packageName", packageName)
                put("sourceType", TransactionSource.MANUAL)
                put("transactionTime", transactionTime)
                put("llmInputJson", "")
                put("confirmedAt", System.currentTimeMillis())
            },
        )
        refreshTransactions()
    }

    suspend fun updateTransaction(
        id: Long,
        appName: String,
        packageName: String,
        direction: String,
        amount: Long,
        recipient: String,
        purpose: String,
        transactionTime: Long,
    ) = io {
        helper.writableDatabase.update(
            "transactions",
            ContentValues().apply {
                put("appName", appName)
                put("packageName", packageName)
                put("direction", direction)
                put("amount", amount)
                put("recipient", recipient)
                put("purpose", purpose)
                put("transactionTime", transactionTime)
            },
            "id = ?",
            arrayOf(id.toString()),
        )
        refreshTransactions()
    }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) {
        synchronized(lock) { block() }
    }

    private fun refreshAll() {
        refreshNotifications()
        refreshTransactions()
    }

    private fun refreshNotifications() {
        val cutoff = System.currentTimeMillis() - INBOX_RETENTION_MILLIS
        _inboxNotifications.value = loadNotifications(
            "isSaved = 0 AND capturedAt >= ? AND eventId NOT LIKE ?",
            arrayOf(cutoff.toString(), "${ScreenCaptureSessionStore.EVENT_PREFIX}%"),
        )
        _savedNotifications.value = loadNotifications("isSaved = 1", null)
        val pendingConfirmations = loadNotifications(
            "analysisStatus = ?",
            arrayOf(AnalysisStatus.READY),
        )
        _notifications.value = (_savedNotifications.value + _inboxNotifications.value + pendingConfirmations)
            .distinctBy { it.notification.id }
            .sortedByDescending { it.notification.capturedAt }
    }

    private fun loadNotifications(where: String, args: Array<String>?): List<NotificationWithDraft> =
        helper.readableDatabase.rawQuery(
            "SELECT * FROM captured_notifications WHERE $where ORDER BY capturedAt DESC LIMIT 500",
            args,
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val notification = cursor.toNotification()
                    add(NotificationWithDraft(notification, loadDraft(notification.id)))
                }
            }
        }

    private fun purgeExpiredInbox() {
        val cutoff = System.currentTimeMillis() - INBOX_RETENTION_MILLIS
        helper.writableDatabase.delete(
            "captured_notifications",
            "isSaved = 0 AND capturedAt < ? AND analysisStatus != ?",
            arrayOf(cutoff.toString(), AnalysisStatus.READY),
        )
    }

    private fun backfillMissingTransactionTraces() {
        val database = helper.writableDatabase
        val traces = database.rawQuery(
            """
            SELECT t.id, c.title, c.text, c.expandedContent
            FROM transactions t
            JOIN captured_notifications c ON c.id = t.sourceNotificationId
            WHERE t.llmInputJson = ''
            """.trimIndent(),
            null,
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        cursor.long("id") to JSONObject().apply {
                            put("title", cursor.string("title"))
                            put("text", cursor.string("text"))
                            put("expanded_content", cursor.string("expandedContent"))
                        }.toString(),
                    )
                }
            }
        }
        traces.forEach { (transactionId, inputJson) ->
            database.update(
                "transactions",
                ContentValues().apply { put("llmInputJson", inputJson) },
                "id = ?",
                arrayOf(transactionId.toString()),
            )
        }
        database.execSQL(
            """
            UPDATE transactions
            SET packageName = COALESCE(
                (SELECT c.packageName FROM captured_notifications c WHERE c.id = transactions.sourceNotificationId),
                ''
            )
            WHERE packageName = ''
            """.trimIndent(),
        )
    }

    private fun refreshTransactions() {
        _transactions.value = helper.readableDatabase.rawQuery(
            "SELECT * FROM transactions ORDER BY transactionTime DESC, id DESC",
            null,
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toTransaction()) }
        }
    }

    private fun loadNotification(id: Long): NotificationWithDraft? =
        helper.readableDatabase.query(
            "captured_notifications", null, "id = ?", arrayOf(id.toString()),
            null, null, null, "1",
        ).use { cursor ->
            if (!cursor.moveToFirst()) null else {
                val notification = cursor.toNotification()
                NotificationWithDraft(notification, loadDraft(id))
            }
        }

    private fun loadDraft(notificationId: Long): ExtractedDraftEntity? =
        helper.readableDatabase.query(
            "extracted_drafts", null, "notificationId = ?", arrayOf(notificationId.toString()),
            null, null, null, "1",
        ).use { cursor -> if (cursor.moveToFirst()) cursor.toDraft() else null }

    private fun CapturedNotificationEntity.toValues() = ContentValues().apply {
        put("eventId", eventId)
        put("notificationKey", notificationKey)
        put("packageName", packageName)
        put("appName", appName)
        put("title", title)
        put("text", text)
        put("expandedContent", expandedContent)
        put("rawPayload", rawPayload)
        put("postedAt", postedAt)
        put("capturedAt", capturedAt)
        put("isSaved", if (isSaved) 1 else 0)
        put("analysisStatus", analysisStatus)
        put("handled", if (handled) 1 else 0)
        put("errorMessage", errorMessage)
    }

    private fun ExtractedDraftEntity.toValues() = ContentValues().apply {
        put("notificationId", notificationId)
        put("direction", direction)
        if (amount == null) putNull("amount") else put("amount", amount)
        put("purpose", purpose)
        put("recipient", recipient)
        if (transactionTime == null) putNull("transactionTime") else put("transactionTime", transactionTime)
        put("rawModelJson", rawModelJson)
        put("analyzedAt", analyzedAt)
    }

    private fun SQLiteDatabase.updateState(id: Long, status: String, handled: Boolean, error: String?) {
        update(
            "captured_notifications",
            ContentValues().apply {
                put("analysisStatus", status)
                put("handled", if (handled) 1 else 0)
                put("errorMessage", error)
            },
            "id = ?",
            arrayOf(id.toString()),
        )
    }

    private fun Cursor.toNotification() = CapturedNotificationEntity(
        id = long("id"),
        eventId = string("eventId"),
        notificationKey = string("notificationKey"),
        packageName = string("packageName"),
        appName = string("appName"),
        title = string("title"),
        text = string("text"),
        expandedContent = string("expandedContent"),
        rawPayload = string("rawPayload"),
        postedAt = long("postedAt"),
        capturedAt = long("capturedAt"),
        isSaved = int("isSaved") == 1,
        analysisStatus = string("analysisStatus"),
        handled = int("handled") == 1,
        errorMessage = nullableString("errorMessage"),
    )

    private fun Cursor.toDraft() = ExtractedDraftEntity(
        notificationId = long("notificationId"),
        direction = string("direction"),
        amount = if (isNull(column("amount"))) null else long("amount"),
        purpose = string("purpose"),
        recipient = string("recipient"),
        transactionTime = if (isNull(column("transactionTime"))) null else long("transactionTime"),
        rawModelJson = string("rawModelJson"),
        analyzedAt = long("analyzedAt"),
    )

    private fun Cursor.toTransaction() = TransactionEntity(
        id = long("id"),
        sourceNotificationId = if (isNull(column("sourceNotificationId"))) null else long("sourceNotificationId"),
        direction = string("direction"),
        amount = long("amount"),
        recipient = string("recipient"),
        purpose = string("purpose"),
        appName = string("appName"),
        packageName = string("packageName"),
        sourceType = string("sourceType"),
        transactionTime = long("transactionTime"),
        llmInputJson = string("llmInputJson"),
        confirmedAt = long("confirmedAt"),
    )

    private fun Cursor.column(name: String) = getColumnIndexOrThrow(name)
    private fun Cursor.string(name: String) = getString(column(name))
    private fun Cursor.nullableString(name: String) = if (isNull(column(name))) null else string(name)
    private fun Cursor.long(name: String) = getLong(column(name))
    private fun Cursor.int(name: String) = getInt(column(name))

    companion object {
        const val INBOX_RETENTION_MILLIS = 24L * 60L * 60L * 1_000L

        @Volatile private var instance: MoneyCheckRepository? = null

        fun get(context: Context): MoneyCheckRepository = instance ?: synchronized(this) {
            instance ?: MoneyCheckRepository(context.applicationContext).also { instance = it }
        }
    }
}

private class MoneyCheckOpenHelper(context: Context) : SQLiteOpenHelper(context, "money-check.db", null, 9) {
    override fun onConfigure(database: SQLiteDatabase) {
        super.onConfigure(database)
        database.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE captured_notifications (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                eventId TEXT NOT NULL UNIQUE,
                notificationKey TEXT NOT NULL,
                packageName TEXT NOT NULL,
                appName TEXT NOT NULL,
                title TEXT NOT NULL,
                text TEXT NOT NULL,
                expandedContent TEXT NOT NULL,
                rawPayload TEXT NOT NULL,
                postedAt INTEGER NOT NULL,
                capturedAt INTEGER NOT NULL,
                isSaved INTEGER NOT NULL DEFAULT 0,
                analysisStatus TEXT NOT NULL,
                handled INTEGER NOT NULL,
                errorMessage TEXT
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE extracted_drafts (
                notificationId INTEGER PRIMARY KEY,
                direction TEXT NOT NULL,
                amount INTEGER,
                purpose TEXT NOT NULL,
                recipient TEXT NOT NULL,
                transactionTime INTEGER,
                rawModelJson TEXT NOT NULL,
                analyzedAt INTEGER NOT NULL,
                FOREIGN KEY(notificationId) REFERENCES captured_notifications(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sourceNotificationId INTEGER,
                direction TEXT NOT NULL,
                amount INTEGER NOT NULL,
                recipient TEXT NOT NULL,
                purpose TEXT NOT NULL,
                appName TEXT NOT NULL,
                packageName TEXT NOT NULL,
                sourceType TEXT NOT NULL,
                transactionTime INTEGER NOT NULL,
                llmInputJson TEXT NOT NULL,
                confirmedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            database.execSQL(
                """
                CREATE TABLE extracted_drafts_new (
                    notificationId INTEGER PRIMARY KEY,
                    direction TEXT NOT NULL,
                    amount INTEGER,
                    purpose TEXT NOT NULL,
                    recipient TEXT NOT NULL,
                    rawModelJson TEXT NOT NULL,
                    analyzedAt INTEGER NOT NULL,
                    FOREIGN KEY(notificationId) REFERENCES captured_notifications(id) ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO extracted_drafts_new (notificationId, direction, amount, purpose, recipient, rawModelJson, analyzedAt)
                SELECT notificationId, direction, amount, COALESCE(purpose, ''), COALESCE(recipient, ''), rawModelJson, analyzedAt FROM extracted_drafts
                """.trimIndent(),
            )
            database.execSQL("DROP TABLE extracted_drafts")
            database.execSQL("ALTER TABLE extracted_drafts_new RENAME TO extracted_drafts")

            database.execSQL(
                """
                CREATE TABLE transactions_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sourceNotificationId INTEGER UNIQUE,
                    direction TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    recipient TEXT NOT NULL,
                    purpose TEXT NOT NULL,
                    appName TEXT NOT NULL,
                    transactionTime INTEGER NOT NULL,
                    confirmedAt INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO transactions_new (
                    id, sourceNotificationId, direction, amount, recipient, purpose, appName, transactionTime, confirmedAt
                )
                SELECT
                    t.id,
                    t.sourceNotificationId,
                    t.direction,
                    t.amount,
                    t.recipient,
                    t.purpose,
                    COALESCE((SELECT c.appName FROM captured_notifications c WHERE c.id = t.sourceNotificationId), ''),
                    t.transactionTime,
                    t.confirmedAt
                FROM transactions t
                """.trimIndent(),
            )
            database.execSQL("DROP TABLE transactions")
            database.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
        }
        if (oldVersion == 2) {
            database.execSQL(
                """
                CREATE TABLE extracted_drafts_v3 (
                    notificationId INTEGER PRIMARY KEY,
                    direction TEXT NOT NULL,
                    amount INTEGER,
                    purpose TEXT NOT NULL,
                    recipient TEXT NOT NULL,
                    rawModelJson TEXT NOT NULL,
                    analyzedAt INTEGER NOT NULL,
                    FOREIGN KEY(notificationId) REFERENCES captured_notifications(id) ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO extracted_drafts_v3 (notificationId, direction, amount, purpose, recipient, rawModelJson, analyzedAt)
                SELECT notificationId, direction, amount, purpose, '', rawModelJson, analyzedAt FROM extracted_drafts
                """.trimIndent(),
            )
            database.execSQL("DROP TABLE extracted_drafts")
            database.execSQL("ALTER TABLE extracted_drafts_v3 RENAME TO extracted_drafts")
        }
        if (oldVersion < 4) {
            // Preserve notifications from older app versions. New notifications are temporary by default.
            database.execSQL("ALTER TABLE captured_notifications ADD COLUMN isSaved INTEGER NOT NULL DEFAULT 1")
        }
        if (oldVersion < 5) {
            database.execSQL("ALTER TABLE transactions ADD COLUMN llmInputJson TEXT NOT NULL DEFAULT ''")
        }
        if (oldVersion < 6) {
            database.execSQL("ALTER TABLE transactions ADD COLUMN packageName TEXT NOT NULL DEFAULT ''")
        }
        if (oldVersion < 7) {
            database.execSQL(
                """
                CREATE TABLE transactions_v7 (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sourceNotificationId INTEGER,
                    direction TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    recipient TEXT NOT NULL,
                    purpose TEXT NOT NULL,
                    appName TEXT NOT NULL,
                    packageName TEXT NOT NULL,
                    transactionTime INTEGER NOT NULL,
                    llmInputJson TEXT NOT NULL,
                    confirmedAt INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO transactions_v7 (
                    id, sourceNotificationId, direction, amount, recipient, purpose,
                    appName, packageName, transactionTime, llmInputJson, confirmedAt
                )
                SELECT
                    id, sourceNotificationId, direction, amount, recipient, purpose,
                    appName, packageName, transactionTime, llmInputJson, confirmedAt
                FROM transactions
                """.trimIndent(),
            )
            database.execSQL("DROP TABLE transactions")
            database.execSQL("ALTER TABLE transactions_v7 RENAME TO transactions")
        }
        if (oldVersion < 8) {
            database.execSQL(
                "ALTER TABLE transactions ADD COLUMN sourceType TEXT NOT NULL DEFAULT '${TransactionSource.AUTOMATIC}'",
            )
        }
        if (oldVersion < 9) {
            database.execSQL("ALTER TABLE extracted_drafts ADD COLUMN transactionTime INTEGER")
        }
    }
}

private inline fun <T> SQLiteDatabase.inTransaction(block: SQLiteDatabase.() -> T): T {
    beginTransaction()
    return try {
        val result = block()
        setTransactionSuccessful()
        result
    } finally {
        endTransaction()
    }
}
