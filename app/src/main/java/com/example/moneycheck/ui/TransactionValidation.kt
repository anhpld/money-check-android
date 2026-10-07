package com.example.moneycheck.ui

enum class ManualTransactionField { AMOUNT, DIRECTION, PURPOSE }

data class ManualTransactionValidation(
    val errors: Map<ManualTransactionField, String> = emptyMap(),
) {
    val isValid: Boolean get() = errors.isEmpty()
}

/**
 * Merges persisted settings changes into a locally edited draft.
 * A key is considered dirty when the draft differs from the last persisted snapshot.
 */
internal fun reconcileDraftMap(
    draft: Map<String, String>,
    lastPersisted: Map<String, String>,
    persisted: Map<String, String>,
): Map<String, String> {
    val keys = draft.keys + lastPersisted.keys + persisted.keys
    return keys.mapNotNull { key ->
        val isDirty = draft[key] != lastPersisted[key]
        val value = if (isDirty) draft[key] else persisted[key]
        value?.let { key to it }
    }.toMap()
}

/** Pure validation for the manual transaction form. Cash entries intentionally have no app/recipient requirement. */
fun validateManualTransaction(
    amountInput: String,
    direction: String,
    recipient: String,
    purpose: String,
    appName: String = "",
    packageName: String = "",
): ManualTransactionValidation {
    val errors = linkedMapOf<ManualTransactionField, String>()
    val amount = amountInput.filter(Char::isDigit).toLongOrNull()
    if (amount == null || amount <= 0L) {
        errors[ManualTransactionField.AMOUNT] = "Nhập số tiền lớn hơn 0"
    }
    if (direction !in setOf("income", "expense")) {
        errors[ManualTransactionField.DIRECTION] = "Chọn chiều giao dịch"
    }
    if (purpose.isBlank()) {
        errors[ManualTransactionField.PURPOSE] = "Nhập nội dung giao dịch"
    }
    return ManualTransactionValidation(errors)
}
