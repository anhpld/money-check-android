package com.example.moneycheck.ui

enum class ManualTransactionField { AMOUNT, DIRECTION, PURPOSE }

data class ManualTransactionValidation(
    val errors: Map<ManualTransactionField, String> = emptyMap(),
) {
    val isValid: Boolean get() = errors.isEmpty()
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
