package com.tracky.app.data.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Money as an exact integer count of minor units (Kenyan shilling cents).
 *
 * Every monetary value in Tracky is stored and computed as [Long] cents.
 * Floating point is never used for money: summing Doubles drifts, and the
 * drift lands exactly where it hurts most — budget limit comparisons and
 * daily totals shown to the user as "your spending".
 *
 * KES has two minor-unit digits, so 1 shilling = 100 cents and
 * [centsPerUnit] is fixed. If a currency with a different minor unit is ever
 * added, that constant and this class are the only things that change.
 */
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {

    init {
        // A negative amount is never legitimate; direction is carried by
        // TransactionType. This catches sign errors at construction.
        require(cents >= 0) { "Money cannot be negative: $cents" }
    }

    operator fun plus(other: Money): Money = Money(cents + other.cents)

    operator fun minus(other: Money): Money = Money((cents - other.cents).coerceAtLeast(0))

    operator fun times(factor: Int): Money = Money(cents * factor)

    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    val isZero: Boolean get() = cents == 0L

    /** Whole shillings, e.g. 150000 cents -> 1500. */
    val wholeShillings: Long get() = cents / CENTS_PER_UNIT

    /** True when the value has a non-zero cent remainder, e.g. 150050. */
    val hasFraction: Boolean get() = cents % CENTS_PER_UNIT != 0L

    /** e.g. 150050 -> "1,500.50" */
    fun toPlainString(): String = formatPlain(cents)

    /** e.g. 150050 -> "Ksh 1,500.50" */
    fun toDisplayString(): String = "Ksh ${formatPlain(cents)}"

    /** Compact form for tight UI: exact below 100k, abbreviated above. */
    fun toCompactString(): String = when {
        cents == 0L -> "Ksh 0"
        cents >= 100_000_000L -> "Ksh ${trimZeros(cents / 1_000_000_000.0)}B"
        cents >= 100_000_000L -> "Ksh ${trimZeros(cents / 1_000_000.0)}M"
        cents >= 100_000L -> "Ksh ${trimZeros(cents / 100_000.0)}K"
        else -> toDisplayString()
    }

    companion object {
        const val CENTS_PER_UNIT = 100L

        val ZERO = Money(0)

        /**
         * Parse a user/SMS-supplied amount string into exact cents.
         *
         * Returns null when the text is not a well-formed non-negative
         * amount. Returning null rather than 0.0 is deliberate: the old
         * parser turned unparseable amounts into zero-value transactions,
         * which silently inflated a user's real transaction list with
         * rows worth nothing. A rejected parse is a missing transaction,
         * which is a far smaller and visible failure.
         */
        fun parseOrNull(raw: String?): Money? {
            if (raw.isNullOrBlank()) return null
            val cleaned = raw.trim().replace(",", "").replace(" ", "")
            if (cleaned.isEmpty()) return null
            if (!cleaned.matches(NUMERIC)) return null

            // Reject more precision than a cent can represent, rather than
            // silently rounding someone's balance.
            val dotIndex = cleaned.indexOf('.')
            if (dotIndex >= 0 && cleaned.length - dotIndex - 1 > 2) return null

            val cents = try {
                java.math.BigDecimal(cleaned)
                    .movePointRight(2)
                    .setScale(0, java.math.RoundingMode.HALF_UP)
                    .longValueExact()
            } catch (_: ArithmeticException) {
                return null
            } catch (_: NumberFormatException) {
                return null
            }
            if (cents < 0) return null
            return Money(cents)
        }

        /** Build from whole shillings, e.g. 1500 -> Money(150000). */
        fun ofShillings(shillings: Long): Money = Money(shillings * CENTS_PER_UNIT)

        private val NUMERIC = Regex("""\d+(\.\d+)?""")

        private fun formatPlain(cents: Long): String {
            val whole = cents / CENTS_PER_UNIT
            val fraction = cents % CENTS_PER_UNIT
            val grouped = GROUPING.format(whole)
            return if (fraction == 0L) grouped else "$grouped.${fraction.toString().padStart(2, '0')}"
        }

        private fun trimZeros(value: Double): String {
            val symbols = DecimalFormatSymbols(Locale.US)
            return DecimalFormat("#,##0.##", symbols).format(value)
        }

        private val GROUPING = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
    }
}
