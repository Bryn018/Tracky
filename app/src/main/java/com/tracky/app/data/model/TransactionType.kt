package com.tracky.app.data.model

/**
 * What kind of movement a transaction represents.
 *
 * Direction and kind are deliberately separate concepts. The previous
 * schema had only INCOMING/OUTGOING, which forced reversals and refunds to
 * be recorded as fresh outgoing spending. A reversal is money coming back,
 * so booking it as an outflow overstated what the user had spent — and the
 * error compounded on every subsequent total, because nothing in the app
 * could later mark the original as void.
 */
enum class TransactionType(val displayName: String) {
    INCOMING("Money In"),
    OUTGOING("Money Out"),
    REVERSAL("Reversal"),
    REFUND("Refund");

    /** True when this type adds to the user's balance. */
    val isCredit: Boolean
        get() = this == INCOMING || this == REVERSAL || this == REFUND

    /** True when this type subtracts from the user's balance. */
    val isDebit: Boolean
        get() = this == OUTGOING

    /**
     * How this type should offset a spending total. A reversal undoes an
     * earlier outflow, so it counts as negative spending; a refund is money
     * returned on a purchase and does the same. Both therefore reduce the
     * amount the user is treated as having spent.
     */
    val spendingMultiplier: Int
        get() = when (this) {
            OUTGOING -> 1
            REVERSAL, REFUND -> -1
            INCOMING -> 0
        }

    val balanceMultiplier: Int
        get() = if (isCredit) 1 else -1

    companion object {
        fun fromString(value: String?): TransactionType? {
            if (value.isNullOrBlank()) return null
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
        }

        /** Lenient lookup for old rows and user-entered filters. */
        fun fromStringOrNull(value: String?): TransactionType? = fromString(value)
    }
}
