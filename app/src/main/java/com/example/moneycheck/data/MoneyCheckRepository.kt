package com.example.moneycheck.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class MoneyCheckRepository private constructor(context: Context) {
    private val helper = MoneyCheckOpenHelper(context.applicationContext)
    private val lock = Any()
    private val _notifications = MutableStateFlow<List<NotificationWithDraft>>(emptyList())
    private val _transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())

    val notifications = _notifications.asStateFlow()
    val transactions = _transactions.asStateFlow()

    init {
        synchronized(lock) { refreshAll() }
    }

    suspend fun capture(notification: CapturedNotificationEntity): Long = io {
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
            updateState(
                id,
                if (draft.isTransaction) AnalysisStatus.READY else AnalysisStatus.IGNORED,
                handled = !draft.isTransaction,
                error = null,
            )
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

    suspend fun confirm(
        notification: CapturedNotificationEntity,
        direction: String,
        amount: Long,
        currency: String,
        purpose: String,
        sender: String,
        recipient: String,
        reference: String,
    ) = io {
        helper.writableDatabase.inTransaction {
            insertWithOnConflict(
                "transactions",
                null,
                ContentValues().apply {
                    put("sourceNotificationId", notification.id)
                    put("direction", direction)
                    put("amount", amount)
                    put("currency", currency)
                    put("purpose", purpose)
                    put("sender", sender)
                    put("recipient", recipient)
                    put("reference", reference)
                    put("transactionTime", notification.postedAt)
                    put("confirmedAt", System.currentTimeMillis())
                },
                SQLiteDatabase.CONFLICT_REPLACE,
            )
            updateState(notification.id, AnalysisStatus.CONFIRMED, handled = true, error = null)
        }
        refreshAll()
    }

    suspend fun clearNotifications() = io {
        helper.writableDatabase.delete("captured_notifications", null, null)
        refreshNotifications()
    }

    suspend fun deleteTransaction(id: Long) = io {
        helper.writableDatabase.delete("transactions", "id = ?", arrayOf(id.toString()))
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
        val items = helper.readableDatabase.rawQuery(
            "SELECT * FROM captured_notifications ORDER BY capturedAt DESC LIMIT 500",
            null,
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val notification = cursor.toNotification()
                    add(NotificationWithDraft(notification, loadDraft(notification.id)))
                }
            }
        }
        _notifications.value = items
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
        put("analysisStatus", analysisStatus)
        put("handled", if (handled) 1 else 0)
        put("errorMessage", errorMessage)
    }

    private fun ExtractedDraftEntity.toValues() = ContentValues().apply {
        put("notificationId", notificationId)
        put("isTransaction", if (isTransaction) 1 else 0)
        put("direction", direction)
        if (amount == null) putNull("amount") else put("amount", amount)
        put("currency", currency)
        put("purpose", purpose)
        put("sender", sender)
        put("recipient", recipient)
        put("reference", reference)
        put("confidence", confidence)
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
        analysisStatus = string("analysisStatus"),
        handled = int("handled") == 1,
        errorMessage = nullableString("errorMessage"),
    )

    private fun Cursor.toDraft() = ExtractedDraftEntity(
        notificationId = long("notificationId"),
        isTransaction = int("isTransaction") == 1,
        direction = string("direction"),
        amount = if (isNull(column("amount"))) null else long("amount"),
        currency = string("currency"),
        purpose = nullableString("purpose"),
        sender = nullableString("sender"),
        recipient = nullableString("recipient"),
        reference = nullableString("reference"),
        confidence = getDouble(column("confidence")),
        rawModelJson = string("rawModelJson"),
        analyzedAt = long("analyzedAt"),
    )

    private fun Cursor.toTransaction() = TransactionEntity(
        id = long("id"),
        sourceNotificationId = if (isNull(column("sourceNotificationId"))) null else long("sourceNotificationId"),
        direction = string("direction"),
        amount = long("amount"),
        currency = string("currency"),
        purpose = string("purpose"),
        sender = string("sender"),
        recipient = string("recipient"),
        reference = string("reference"),
        transactionTime = long("transactionTime"),
        confirmedAt = long("confirmedAt"),
    )

    private fun Cursor.column(name: String) = getColumnIndexOrThrow(name)
    private fun Cursor.string(name: String) = getString(column(name))
    private fun Cursor.nullableString(name: String) = if (isNull(column(name))) null else string(name)
    private fun Cursor.long(name: String) = getLong(column(name))
    private fun Cursor.int(name: String) = getInt(column(name))

    companion object {
        @Volatile private var instance: MoneyCheckRepository? = null

        fun get(context: Context): MoneyCheckRepository = instance ?: synchronized(this) {
            instance ?: MoneyCheckRepository(context.applicationContext).also { instance = it }
        }
    }
}

private class MoneyCheckOpenHelper(context: Context) : SQLiteOpenHelper(context, "money-check.db", null, 1) {
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
                isTransaction INTEGER NOT NULL,
                direction TEXT NOT NULL,
                amount INTEGER,
                currency TEXT NOT NULL,
                purpose TEXT,
                sender TEXT,
                recipient TEXT,
                reference TEXT,
                confidence REAL NOT NULL,
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
                sourceNotificationId INTEGER UNIQUE,
                direction TEXT NOT NULL,
                amount INTEGER NOT NULL,
                currency TEXT NOT NULL,
                purpose TEXT NOT NULL,
                sender TEXT NOT NULL,
                recipient TEXT NOT NULL,
                reference TEXT NOT NULL,
                transactionTime INTEGER NOT NULL,
                confirmedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
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
