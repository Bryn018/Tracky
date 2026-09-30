package com.tracky.app.data.sms

import com.tracky.app.data.local.entity.SmsIdentity
import com.tracky.app.data.local.entity.TransactionEntity
import com.tracky.app.data.model.AutoCategoryManager
import com.tracky.app.data.model.Money
import com.tracky.app.data.model.TransactionType

/**
 * Parses SMS bodies from Kenyan mobile-money and bank providers into
 * transactions.
 *
 * Structural rules that the previous implementation got wrong, and that the
 * tests in this package now pin down:
 *
 *  - A **balance** is not a transaction. "Available balance Ksh 50,000. You
 *    received Ksh 2,000" must record 2,000, not 50,000. Balances are located
 *    by an explicit label and blanked out before the amount is chosen.
 *  - A **reversal or refund** is not spending. Booking it as an outflow is
 *    what made totals permanently wrong, because nothing could later mark the
 *    original as void.
 *  - The **scam filter is not a sender allowlist**. SMS sender addresses are
 *    forgeable, so trusting "MPESA" as sender let a Ksh 500,000 lottery scam
 *    be recorded as a real payment. Scam detection is now purely about what
 *    the message *says*.
 *  - **Zero is not a transaction.** Unparseable amounts are rejected rather
 *    than becoming 0-value rows.
 */
object SmsParser {

    // ── Providers ──────────────────────────────────────────────────────

    private val bankNames = listOf(
        "EQUITY", "KCB", "CO-OP", "COOPERATIVE", "NCBA", "ABSA", "BARCLAYS",
        "STANCHART", "FAMILYBANK", "DTB", "IMBANK", "SBM", "HABIB", "UNITY",
        "ECOBANK", "WABISABU"
    )

    // ── Scam detection ─────────────────────────────────────────────────
    //
    // These describe the *content* of a scam. The sender address is
    // deliberately not consulted: any device can present a sender of "MPESA",
    // so a sender-based allowlist is not a security control. Real M-Pesa
    // confirmations state a transaction verb (sent, received, paid,
    // withdrawn, reversed) and are matched by none of the patterns below.

    private val scamPatterns = listOf(
        // Prize / lottery
        Regex("""\byou have won\b""", RegexOption.IGNORE_CASE),
        Regex("""\bcongratulations\b""", RegexOption.IGNORE_CASE),
        Regex("""\bprize\b""", RegexOption.IGNORE_CASE),
        Regex("""\bwinner\b""", RegexOption.IGNORE_CASE),
        Regex("""\bjac?kpot\b""", RegexOption.IGNORE_CASE),
        Regex("""\blottery\b""", RegexOption.IGNORE_CASE),
        Regex("""\bclaim\s+(?:your|prize|reward|gift)\b""", RegexOption.IGNORE_CASE),
        // Credential harvesting. The negative lookbehind matters: genuine
        // Safaricom and bank footers say "Never share your PIN", and a naive
        // match on "share ... pin" flags every real confirmation as a scam.
        Regex(
            """(?<!never )(?<!do not )(?<!don't )(?<!not to )\b(?:send|share|confirm|provide|enter)\s+(?:your\s+)?(?:pin|password|passcode|otp)\b""",
            RegexOption.IGNORE_CASE
        ),
        Regex("""\bverify\s+(?:your\s+)?account\b""", RegexOption.IGNORE_CASE),
        Regex("""\baccount\s+(?:will\s+be\s+)?(?:suspended|blocked|frozen|deactivated)\b""", RegexOption.IGNORE_CASE),
        // Lure mechanics
        Regex("""\bwin\s+(?:mpesa|cash|ksh|kes)\b""", RegexOption.IGNORE_CASE),
        Regex("""\bclaim\s+.*\breward\b""", RegexOption.IGNORE_CASE),
        Regex("""\bfree\s+(?:airtime|data|ksh|cash)\b""", RegexOption.IGNORE_CASE),
        Regex("""\bmpesa\s+promo(?!tion)""", RegexOption.IGNORE_CASE),
        Regex("""\bsafaricom\s+promo(?!tion)""", RegexOption.IGNORE_CASE),
        Regex("""\bwithdraw\s+(?:your\s+)?(?:mpesa\s+)?winnings\b""", RegexOption.IGNORE_CASE),
    )

    /** A full 10-digit Kenyan mobile number presented as somewhere to call. */
    private val callOutNumber = Regex(
        """\b(?:call|contact|dial)\s+(?:us\s+)?(?:on\s+)?(?:\+?254|0)\s?[71]\d{8}\b""",
        RegexOption.IGNORE_CASE
    )

    // ── Amount extraction ──────────────────────────────────────────────

    private const val AMOUNT = """(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)"""

    /** A monetary amount introduced by an explicit currency token. */
    private val currencyAmount = Regex("""(?:ksh|kes|kshs)\.?\s*$AMOUNT""", RegexOption.IGNORE_CASE)

    // ── Balance extraction ──────────────────────────────────────────────
    //
    // A balance is only ever read from an explicitly labelled position, and
    // the matched span is blanked out of the segment before the transaction
    // amount is chosen. That is what stops "Available balance Ksh 50,000"
    // from being mistaken for the amount.

    private val balanceLabels = listOf(
        "new m-pesa balance", "new mpesa balance", "mpesa balance",
        "new airtel money balance", "airtel money balance", "new balance",
        "account balance", "available balance", "current balance",
        "balance is", "bal:", "balance:", "bal", "balance"
    )

    private val balanceLabelGroup =
        balanceLabels.joinToString("|") { Regex.escape(it) }

    private val balancePattern = Regex(
        """(?:$balanceLabelGroup)\s*(?:is|:)?\s*(?:ksh\.?|kes\.?)?\s*$AMOUNT""",
        RegexOption.IGNORE_CASE
    )

    /** A whole leading "Available balance Ksh 50,000.00." clause. */
    private val leadingBalanceClause = Regex(
        """^\s*(?:$balanceLabelGroup)\s*(?:is|:)?\s*(?:ksh\.?|kes\.?)?\s*$AMOUNT\s*[.;,-]?\s*""",
        RegexOption.IGNORE_CASE
    )

    // ── Contact / name extraction ──────────────────────────────────────

    private val contactNumber = Regex("""(0\d{2,3}\s?\d{3}\s?\d{3})|(\+?254\d{9})""")

    // A name is letters and spaces, terminated by a date, time, balance
    // label, sentence end, or end of string. "Balance" being a terminator is
    // what stops the previous version capturing "John. Available" as a name.
    private val nameTerminator =
        """(?:\s+\d{1,2}[/-]\d{1,2}|\s+on\s+\d|\s+at\s+\d|\s+\d{1,2}:\d{2}|\s+Ksh\b|\s+KES\b|\s+[Bb]alance\b|\s+[Bb]al\b|\s+available\b|\s+is\b|\s+ID\s|[.;,]|\s+$)"""

    private val contactNamePatterns = listOf(
        Regex("""\bfrom\s+([A-Za-z][A-Za-z\s.'-]*?)$nameTerminator"""),
        Regex("""\bto\s+([A-Za-z][A-Za-z\s.'-]*?)$nameTerminator"""),
    )

    /** Words meaning the captured span is boilerplate, not a name or till. */
    private val nonNameWords = setOf(
        "balance", "mpesa", "pesa", "airtel", "your", "the", "a", "an", "is",
        "available", "current", "new", "account", "bank", "usd", "kes", "ksh",
        "mobile", "money", "withdrawal", "deposit", "transaction", "id", "till"
    )

    // ── Reversal / refund detection ────────────────────────────────────

    private val reversalPattern = Regex(
        """\b(?:reversed|reversal|transaction\s+reversed)\b""",
        RegexOption.IGNORE_CASE
    )

    private val refundPattern = Regex(
        """\b(?:refund|refunded|refund\s+due|cashback)\b""",
        RegexOption.IGNORE_CASE
    )

    // ── Transaction verbs ──────────────────────────────────────────────

    private val outgoingVerbs = Regex(
        """\b(?:sent|paid|withdrawn|withdrawal|transferred|debited|purchased)\b""",
        RegexOption.IGNORE_CASE
    )

    private val incomingVerbs = Regex(
        """\b(?:received|credited|deposited)\b""",
        RegexOption.IGNORE_CASE
    )

    /** A message must state a movement to be a transaction at all. */
    private val movementVerb = Regex(
        """\b(?:sent|paid|received|credited|debited|withdrawn|withdrawal|deposited|transferred|refunded|reversed|refund)\b""",
        RegexOption.IGNORE_CASE
    )

    // ── Public API ─────────────────────────────────────────────────────

    /**
     * Parse one SMS into zero or more transactions.
     *
     * Returns a list because a single message can carry more than one
     * movement. Concatenated confirmations are common, and the previous
     * implementation recorded only the first and silently dropped the rest.
     */
    fun parseAll(messageBody: String, senderAddress: String, timestamp: Long): List<TransactionEntity> {
        val body = messageBody.trim()
        if (body.isBlank()) return emptyList()

        val channel = detectChannel(senderAddress, body) ?: return emptyList()

        // Scam check is content-based and runs before any parsing, so a scam
        // can never be partially captured as a real transaction.
        if (isScamMessage(body)) return emptyList()

        // No movement verb means this is promotional or an informational
        // balance check, not a transaction.
        if (!movementVerb.containsMatchIn(body)) return emptyList()

        val segments = splitSegments(body)
        return segments.mapIndexedNotNull { index, segment ->
            parseSegment(segment, body, senderAddress, channel, timestamp, index)
        }
    }

    /** Single-transaction convenience wrapper. */
    fun parseSms(messageBody: String, senderAddress: String, timestamp: Long): TransactionEntity? =
        parseAll(messageBody, senderAddress, timestamp).firstOrNull()

    // ── Segmentation ───────────────────────────────────────────────────

    /**
     * Split a concatenated multi-transaction message into segments, each
     * carrying its own amount.
     *
     * Two things this has to get right, both of which were wrong in the first
     * draft of this pass:
     *
     *  - Amounts that belong to a labelled balance are not transaction
     *    amounts. "… paid to TILL 123. New M-Pesa balance is Ksh 4,000"
     *    contains two currency amounts but only one movement; treating the
     *    balance as a second transaction invented a Ksh 4,000 row.
     *  - A segment must start *before* its own amount, not after it.
     *
     * Segments are cut from the original text so each one can still carry its
     * own balance.
     */
    private fun splitSegments(body: String): List<String> {
        val balanceSpans = balancePattern.findAll(body).map { it.range }.toList()
        val allAmounts = currencyAmount.findAll(body).toList()

        // An amount is a balance if it sits inside a labelled balance phrase.
        val isBalanceAmount = { range: IntRange ->
            balanceSpans.any { span -> range.first >= span.first && range.last <= span.last }
        }

        val anchors = allAmounts.filter { !isBalanceAmount(it.range) }
        if (anchors.size <= 1) return listOf(body)

        val segments = mutableListOf<String>()
        for (i in anchors.indices) {
            // Start after the previous segment's trailing balance, so this
            // segment contains its own amount, contact and balance rather
            // than inheriting the previous one's.
            val trailingBalance = balanceSpans
                .filter { it.last < anchors[i].range.first }
                .maxByOrNull { it.last }
            val start = when {
                trailingBalance != null -> trailingBalance.last + 1
                i == 0 -> 0
                else -> anchors[i - 1].range.last + 1
            }
            val end = if (i == anchors.size - 1) body.length else anchors[i + 1].range.first
            val from = start.coerceIn(0, body.length)
            val to = end.coerceIn(from, body.length)
            if (from < to) segments.add(body.substring(from, to))
        }
        return segments.ifEmpty { listOf(body) }
    }

    // ── Single segment parsing ─────────────────────────────────────────

    private fun parseSegment(
        segment: String,
        fullBody: String,
        senderAddress: String,
        channel: String,
        timestamp: Long,
        index: Int
    ): TransactionEntity? {
        // Blank out balance clauses so a balance can never be chosen as the
        // transaction amount.
        val scannable = balancePattern.replace(
            leadingBalanceClause.replace(segment, " "),
            " "
        )

        val amount = extractAmount(scannable) ?: return null
        if (amount.isZero) return null

        val type = detectType(segment)
        val contact = extractContact(segment)
        val balance = extractBalance(segment)
        val category = AutoCategoryManager.categorize(segment, channel, contact)

        return TransactionEntity(
            type = type.name,
            amountCents = amount.cents,
            channel = channel,
            contact = contact,
            senderName = contact,
            messageBody = fullBody,
            timestamp = timestamp,
            balanceCents = balance?.cents,
            category = category.name,
            smsKey = SmsIdentity.key(senderAddress, timestamp, fullBody, index)
        )
    }

    // ── Type detection ─────────────────────────────────────────────────

    private fun detectType(segment: String): TransactionType {
        // Reversal wins over refund: it is the more specific claim, and both
        // mean money came back rather than being spent.
        if (reversalPattern.containsMatchIn(segment)) return TransactionType.REVERSAL
        if (refundPattern.containsMatchIn(segment)) return TransactionType.REFUND

        // "credited" beats "sent": "sent to bank, credited to your account"
        // is money arriving.
        if (segment.contains("credited", ignoreCase = true)) return TransactionType.INCOMING
        if (outgoingVerbs.containsMatchIn(segment)) return TransactionType.OUTGOING
        if (incomingVerbs.containsMatchIn(segment)) return TransactionType.INCOMING

        return TransactionType.OUTGOING
    }

    // ── Scam detection ─────────────────────────────────────────────────

    internal fun isScamMessage(body: String): Boolean {
        if (scamPatterns.any { it.containsMatchIn(body) }) return true
        if (callOutNumber.containsMatchIn(body)) return true
        return false
    }

    // ── Channel detection ──────────────────────────────────────────────

    private fun detectChannel(senderAddress: String, body: String): String? {
        val sender = senderAddress.trim().uppercase()
        val bodyUpper = body.uppercase()

        if (sender.contains("MPESA")) return "M-Pesa"
        if (sender.contains("AIRTEL")) return "Airtel Money"
        if (bankNames.any { sender.contains(it) }) return "Bank"

        if (bodyUpper.contains("MPESA") || bodyUpper.contains("M-PESA")) return "M-Pesa"
        if (bodyUpper.contains("AIRTEL")) return "Airtel Money"
        if (bankNames.any { bodyUpper.contains(it) }) return "Bank"
        return null
    }

    // ── Shared extraction helpers ──────────────────────────────────────

    /**
     * First currency-denominated amount in [text], or the first bare number
     * when the provider omits the currency token. The bare-number fallback is
     * only reached when the message also states a movement verb, which keeps
     * dates and reference numbers out of the running.
     */
    private fun extractAmount(text: String): Money? {
        currencyAmount.find(text)?.let { return Money.parseOrNull(it.groupValues[1]) }

        if (!movementVerb.containsMatchIn(text)) return null

        val bare = Regex(AMOUNT).find(text) ?: return null
        return Money.parseOrNull(bare.value)
    }

    private fun extractBalance(text: String): Money? {
        val match = balancePattern.find(text) ?: return null
        return Money.parseOrNull(match.groupValues[1])
    }

    private fun extractContact(text: String): String? {
        contactNumber.find(text)?.let { return it.value.replace(" ", "") }

        for (pattern in contactNamePatterns) {
            val match = pattern.find(text) ?: continue
            val candidate = match.groupValues[1]
                .trim()
                .replace(Regex("""\s+"""), " ")
                .trimEnd('.', ',', ';', '-')
                .trim()
            if (isPlausibleName(candidate)) return candidate
        }
        return null
    }

    /**
     * A captured span is a usable contact only if it looks like a name, till,
     * or paybill label rather than a sentence fragment the regex overran.
     */
    private fun isPlausibleName(candidate: String): Boolean {
        if (candidate.isBlank()) return false
        if (candidate.length > 40) return false
        if (!candidate.first().isLetter()) return false
        val words = candidate.lowercase().split(" ").filter { it.isNotBlank() }
        if (words.isEmpty()) return false
        // Every word being a known non-name term means the regex captured
        // boilerplate such as "your account" or "new balance".
        if (words.all { it in nonNameWords }) return false
        return true
    }
}