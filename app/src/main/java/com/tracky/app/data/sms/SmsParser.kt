package com.tracky.app.data.sms

import com.tracky.app.data.local.entity.TransactionEntity
import com.tracky.app.data.model.AutoCategoryManager
import com.tracky.app.data.model.Category

/**
 * Parses SMS message bodies for mobile money transactions.
 * Supports M-Pesa, Airtel Money, and Kenyan bank SMS transactions.
 *
 * Designed to handle real-world SMS formats from Safaricom/M-Pesa,
 * Airtel Kenya/Airtel Money, and major Kenyan banks.
 */
object SmsParser {

    // ── Scam / Fraud Patterns ──────────────────────────────────────────
    private val scamPatterns = listOf(
        // Prize/lottery wins
        Regex("""\byou have won\b""", RegexOption.IGNORE_CASE),
        Regex("""\bcongratulations\b""", RegexOption.IGNORE_CASE),
        Regex("""\bprize\b""", RegexOption.IGNORE_CASE),
        Regex("""\bwinner\b""", RegexOption.IGNORE_CASE),
        Regex("""\bjac?kpot\b""", RegexOption.IGNORE_CASE),
        Regex("""\blucky\b.*\bwinner\b""", RegexOption.IGNORE_CASE),
        Regex("""\blottery\b""", RegexOption.IGNORE_CASE),
        // Account threats
        Regex("""account.*\bsuspended\b""", RegexOption.IGNORE_CASE),
        Regex("""account.*\bblocked\b""", RegexOption.IGNORE_CASE),
        Regex("""account.*\bfrozen\b""", RegexOption.IGNORE_CASE),
        Regex("""account.*\bdeactivated\b""", RegexOption.IGNORE_CASE),
        // Call-to number scams
        Regex("""call\s+(?:0[71]|254|\+254)""", RegexOption.IGNORE_CASE),
        Regex("""call\s+(?:customer|help|support)\s*(?:care|line|number)""", RegexOption.IGNORE_CASE),
        // PIN/password requests
        Regex("""(?:send|share|confirm|provide)\s.*\bpin\b""", RegexOption.IGNORE_CASE),
        Regex("""\bpin\b.*(?:send|share|confirm|provide)""", RegexOption.IGNORE_CASE),
        Regex("""(?:send|share|confirm|provide)\s.*\bpassword\b""", RegexOption.IGNORE_CASE),
        Regex("""verify\s.*\b(details|account)""", RegexOption.IGNORE_CASE),
        // URLs
        Regex("""https?://""", RegexOption.IGNORE_CASE),
        Regex("""www\.""", RegexOption.IGNORE_CASE),
        // Free promo scams
        Regex("""free\s*(?:data|airtime|promo)""", RegexOption.IGNORE_CASE),
        Regex("""claim\s.*\breward\b""", RegexOption.IGNORE_CASE),
        Regex("""\bcash\s*(?:prize|reward|gift)\b""", RegexOption.IGNORE_CASE),
        // Urgency tactics
        Regex("""!!!"""),
        Regex("""\burgent\b""", RegexOption.IGNORE_CASE),
        Regex("""immediate\s+action""", RegexOption.IGNORE_CASE),
        Regex("""act\s+now""", RegexOption.IGNORE_CASE),
        // Fake M-Pesa/Safaricom promos
        Regex("""mpesa\s+promo(?!tion)""", RegexOption.IGNORE_CASE),
        Regex("""safaricom\s+promo(?!tion)""", RegexOption.IGNORE_CASE),
        Regex("""win\s+mpesa""", RegexOption.IGNORE_CASE),
        // Fake bank security alerts
        Regex("""bank.*alert.*login""", RegexOption.IGNORE_CASE),
        Regex("""verify.*account.*now""", RegexOption.IGNORE_CASE),
        Regex("""security.*breach""", RegexOption.IGNORE_CASE),
        // Fake refunds
        Regex("""refund.*due to""", RegexOption.IGNORE_CASE),
        Regex("""overcharged""", RegexOption.IGNORE_CASE),
        Regex("""overpayment.*refund""", RegexOption.IGNORE_CASE),
    ).filterNotNull()

    // ── Known sender patterns ──────────────────────────────────────────
    private val bankSenderPatterns = listOf(
        "EQUITY", "KCB", "CO-OP", "COOPERATIVE", "NCBA", "ABSA", "BARCLAYS",
        "STANCHART", "FAMILYBANK", "DTB", "I&M", "IMBANK"
    )

    private val bankBodyKeywords = listOf(
        "EQUITY", "KCB", "CO-OP", "COOPERATIVE", "NCBA", "ABSA", "BARCLAYS",
        "STANCHART", "FAMILYBANK", "DTB", "I&M", "IMBANK",
        "CREDITED", "DEBITED", "WITHDRAWN", "DEPOSIT", "WITHDRAWAL",
        "ACCOUNT BALANCE", "AVAILABLE BALANCE"
    )

    // ── Amount extraction ──────────────────────────────────────────────
    private val amountRegex = Regex(
        """(?:Ksh|KES|KSh|ksh|KSH|kes)\s*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // ── M-Pesa specific recognition ────────────────────────────────────
    private val mpesaKeywords = listOf(
        "MPESA", "M-PESA", "M PESA",
        "SENT TO", "RECEIVED FROM", "PAID TO", "TRANSFERRED TO",
        "WITHDRAWN FROM", "WITHDRAWN BY",
        "TILL NUMBER", "TILL NO", "PAYBILL", "BUY GOODS",
        "AIRTIME", "FULIZA",
        "NEW M-PESA BALANCE", "NEW BALANCE"
    )

    private val airtelKeywords = listOf(
        "AIRTEL", "AIRTEL MONEY",
        "SENT TO", "RECEIVED FROM",
        "AIRTEL MONEY BALANCE", "NEW BALANCE"
    )

    // ── Balance extraction ─────────────────────────────────────────────
    private val balanceRegex = Regex(
        """(?:New\s+M[-\s]?Pesa\s+balance|Account\s+Balance|Balance\s+is|New\s+balance|Bal:?|Available\s+balance)\s*(?:Ksh|KES|KSh|ksh|KSH)?\s*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // ── Contact/number extraction ──────────────────────────────────────
    private val contactNumberRegex = Regex(
        """(0\d{2,3}\s?\d{3}\s?\d{3})|(\+?254\d{9})""",
        RegexOption.IGNORE_CASE
    )

    private val contactNameRegex = listOf(
        Regex("""(?:from|From|to|To)\s+([A-Za-z\s.]+?)(?:\s+(?:on|at|via)\s|\s+\d|\s*$|\s+Ksh|\s+-)"""),
        Regex("""(?:sent\s+to|Sent\s+to|received\s+from|Received\s+from)\s+([A-Za-z\s.]+?)(?:\s+(?:on|at|via)\s|\s+\d|\s*$)"""),
    )

    // ── Public API ─────────────────────────────────────────────────────

    fun parseSms(
        messageBody: String,
        senderAddress: String,
        timestamp: Long
    ): TransactionEntity? {
        val body = messageBody.trim()
        if (body.isBlank()) return null

        // Scam/fraud detection
        if (isScamMessage(body, senderAddress)) {
            return null
        }

        // Identify the channel
        val channel = detectChannel(senderAddress, body) ?: return null

        // Parse the transaction
        val parsed = parseTransaction(body, channel) ?: return null
        val (amount, type, contact) = parsed

        val balance = extractBalance(body)
        val category = AutoCategoryManager.categorize(body, channel, contact)

        return TransactionEntity(
            type = type,
            amount = amount,
            channel = channel,
            contact = contact,
            senderName = contact,
            messageBody = body,
            timestamp = timestamp,
            balance = balance,
            category = category.name
        )
    }

    // ── Scam detection ─────────────────────────────────────────────────

    private fun isScamMessage(body: String, senderAddress: String): Boolean {
        for (pattern in scamPatterns) {
            if (pattern.containsMatchIn(body)) {
                return true
            }
        }
        if (Regex("""call\s+(0[71]\d{8}|\+254[71]\d{8}|254[71]\d{8})""", RegexOption.IGNORE_CASE)
                .containsMatchIn(body)) {
            return true
        }
        return false
    }

    // ── Channel detection ──────────────────────────────────────────────

    private fun looksLikeFinancialMessage(body: String, senderAddress: String): Boolean {
        val senderUpper = senderAddress.uppercase()
        val bodyUpper = body.uppercase()

        if (senderUpper.contains("MPESA") || senderUpper.contains("AIRTEL")) return true
        for (bank in bankSenderPatterns) {
            if (senderUpper.contains(bank)) return true
        }
        for (keyword in mpesaKeywords) {
            if (bodyUpper.contains(keyword)) return true
        }
        for (keyword in airtelKeywords) {
            if (bodyUpper.contains(keyword)) return true
        }
        for (keyword in bankBodyKeywords) {
            if (bodyUpper.contains(keyword)) return true
        }
        return false
    }

    private fun detectChannel(senderAddress: String, body: String): String? {
        val combined = "$senderAddress $body".uppercase()

        return when {
            combined.contains("MPESA") || combined.contains("M-PESA") || combined.contains("M PESA") -> "M-Pesa"
            combined.contains("AIRTEL") -> "Airtel Money"
            bankSenderPatterns.any { combined.contains(it) } -> "Bank"
            bankBodyKeywords.any { combined.contains(it) } -> "Bank"
            else -> null
        }
    }

    // ── Main parsing logic ─────────────────────────────────────────────

    private fun parseTransaction(
        body: String,
        channel: String
    ): Triple<Double, String, String?>? {
        return when (channel) {
            "M-Pesa" -> parseMpesa(body)
            "Airtel Money" -> parseAirtel(body)
            "Bank" -> parseBank(body)
            else -> null
        }
    }

    // ── M-Pesa parsing ─────────────────────────────────────────────────

    private fun parseMpesa(body: String): Triple<Double, String, String?>? {
        val bodyUpper = body.uppercase()

        // 1. M-Pesa received from someone
        val receivedMatch = Regex(
            """(?:received|Received)\s+(?:Ksh|KES|KSh|ksh)?\s*([\d,]+(?:\.\d{1,2})?)\s*(?:from|From)\s+(.+?)(?:\s+\d|\s+on|\s+at|$)""",
            RegexOption.IGNORE_CASE
        ).find(body)

        if (receivedMatch != null) {
            val amount = parseAmount(receivedMatch.groupValues[1])
            val contactInfo = extractContact(receivedMatch.groupValues[2].trim())
            return Triple(amount, "INCOMING", contactInfo)
        }

        val receivedKshFirst = Regex(
            """(?:Ksh|KES|KSh|ksh)\s*([\d,]+(?:\.\d{1,2})?)\s*(?:received|Received)\s*(?:from|From)?\s*(.*?)(?:\s+on|\s+at|\s+\d|$)""",
            RegexOption.IGNORE_CASE
        ).find(body)

        if (receivedKshFirst != null) {
            val amount = parseAmount(receivedKshFirst.groupValues[1])
            val contactInfo = extractContact(receivedKshFirst.groupValues[2].trim())
            return Triple(amount, "INCOMING", contactInfo)
        }

        // 2. M-Pesa sent to someone
        val sentMatch = Regex(
            """(?:sent|Sent)\s+(?:to|To)\s+(.+?)(?:\s+(?:on|at)\b|$)""",
            RegexOption.IGNORE_CASE
        ).find(body)

        if (sentMatch != null) {
            val contactInfo = extractContact(sentMatch.groupValues[1].trim())
            val amount = extractAmount(body) ?: return null
            return Triple(amount, "OUTGOING", contactInfo)
        }

        // 3. Paid to (Till Number, Paybill)
        val paidMatch = Regex(
            """(?:paid|Paid)\s+(?:to|To)\s+(.+?)(?:\s+on|\s+at|\s+\d|$)""",
            RegexOption.IGNORE_CASE
        ).find(body)

        if (paidMatch != null) {
            val contactInfo = paidMatch.groupValues[1].trim().take(40)
            val amount = extractAmount(body) ?: return null
            return Triple(amount, "OUTGOING", contactInfo)
        }

        // 4. Withdrawn from / by
        val withdrawnMatch = Regex(
            """(?:withdrawn|Withdrawn)\s+(?:from|by)\s+(.+?)(?:\s+on|\s+at|\s+\d|$)""",
            RegexOption.IGNORE_CASE
        ).find(body)

        if (withdrawnMatch != null) {
            val contactInfo = extractContact(withdrawnMatch.groupValues[1].trim())
            val amount = extractAmount(body) ?: return null
            return Triple(amount, "OUTGOING", contactInfo)
        }

        // 5. Simple outgoing keywords
        val outgoingKeyword = Regex(
            """(?:sent|Sent|paid|Paid|withdrawn|Withdrawn|transferred|Transferred|debited|Debited)""",
            RegexOption.IGNORE_CASE
        ).find(body)

        if (outgoingKeyword != null) {
            val amount = extractAmount(body) ?: return null
            val contactInfo = extractAnyContact(body)
            return Triple(amount, "OUTGOING", contactInfo)
        }

        // 6. Simple incoming keywords
        val incomingKeyword = Regex(
            """(?:received|Received|credited|Credited)""",
            RegexOption.IGNORE_CASE
        ).find(body)

        if (incomingKeyword != null) {
            val amount = extractAmount(body) ?: return null
            val contactInfo = extractAnyContact(body)
            return Triple(amount, "INCOMING", contactInfo)
        }

        // 7. Last resort
        val amount = extractAmount(body) ?: return null
        return Triple(amount, "OUTGOING", null)
    }

    // ── Airtel Money parsing ───────────────────────────────────────────

    private fun parseAirtel(body: String): Triple<Double, String, String?>? {
        val bodyUpper = body.uppercase()

        val receivedMatch = Regex(
            """(?:received|Received)\s+(?:Ksh|KES|KSh|ksh)?\s*([\d,]+(?:\.\d{1,2})?)\s*(?:from|From)\s+(.+?)(?:\s+\d|\s+on|\s+at|$)""",
            RegexOption.IGNORE_CASE
        ).find(body)

        if (receivedMatch != null) {
            val amount = parseAmount(receivedMatch.groupValues[1])
            val contactInfo = extractContact(receivedMatch.groupValues[2].trim())
            return Triple(amount, "INCOMING", contactInfo)
        }

        val sentMatch = Regex(
            """(?:sent|Sent|paid|Paid)\s+(?:Ksh|KES|KSh|ksh)?\s*([\d,]+(?:\.\d{1,2})?)\s*(?:to|To)\s+(.+?)(?:\s+(?:on|at)\b|$)""",
            RegexOption.IGNORE_CASE
        ).find(body)

        if (sentMatch != null) {
            val amount = parseAmount(sentMatch.groupValues[1])
            val contactInfo = extractContact(sentMatch.groupValues[2].trim())
            return Triple(amount, "OUTGOING", contactInfo)
        }

        val kshFirstReceived = Regex(
            """(?:Ksh|KES|KSh|ksh)\s*([\d,]+(?:\.\d{1,2})?)\s*(?:received|Received)\s*(?:from|From)?\s*(.*?)(?:\s+\d|\s+on|\s+at|$)""",
            RegexOption.IGNORE_CASE
        ).find(body)

        if (kshFirstReceived != null) {
            val amount = parseAmount(kshFirstReceived.groupValues[1])
            val contactInfo = extractContact(kshFirstReceived.groupValues[2].trim())
            return Triple(amount, "INCOMING", contactInfo)
        }

        val kshFirstSent = Regex(
            """(?:Ksh|KES|KSh|ksh)\s*([\d,]+(?:\.\d{1,2})?)\s*(?:sent|Sent|paid|Paid)\s*(?:to|To)?\s*(.*?)(?:\s+(?:on|at)\b|$)""",
            RegexOption.IGNORE_CASE
        ).find(body)

        if (kshFirstSent != null) {
            val amount = parseAmount(kshFirstSent.groupValues[1])
            val contactInfo = extractContact(kshFirstSent.groupValues[2].trim())
            return Triple(amount, "OUTGOING", contactInfo)
        }

        val amount = extractAmount(body) ?: return null
        val type = when {
            body.contains("received", ignoreCase = true) ||
            body.contains("credited", ignoreCase = true) -> "INCOMING"
            else -> "OUTGOING"
        }
        val contactInfo = extractAnyContact(body)
        return Triple(amount, type, contactInfo)
    }

    // ── Bank SMS parsing ───────────────────────────────────────────────

    private fun parseBank(body: String): Triple<Double, String, String?>? {
        val creditMatch = Regex(
            """(?:credited|Credited|deposit|Deposit|deposited|Deposited)\s*(?:with|of)?\s*(?:Ksh|KES|KSh|ksh)?\s*([\d,]+(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        ).find(body)

        if (creditMatch != null) {
            val amount = parseAmount(creditMatch.groupValues[1])
            val contactInfo = extractAnyContact(body)
            return Triple(amount, "INCOMING", contactInfo)
        }

        val debitMatch = Regex(
            """(?:debited|Debited|withdrawn|Withdrawn|withdrawal|Withdrawal|payment|Payment)\s*(?:with|of)?\s*(?:Ksh|KES|KSh|ksh)?\s*([\d,]+(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        ).find(body)

        if (debitMatch != null) {
            val amount = parseAmount(debitMatch.groupValues[1])
            val contactInfo = extractAnyContact(body)
            return Triple(amount, "OUTGOING", contactInfo)
        }

        val transferMatch = Regex(
            """(?:transferred|Transferred)\s*(?:Ksh|KES|KSh|ksh)?\s*([\d,]+(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        ).find(body)

        if (transferMatch != null) {
            val amount = parseAmount(transferMatch.groupValues[1])
            val direction = if (body.contains("to", ignoreCase = true)) "OUTGOING" else "INCOMING"
            val contactInfo = extractAnyContact(body)
            return Triple(amount, direction, contactInfo)
        }

        val amount = extractAmount(body) ?: return null
        val type = when {
            body.contains("credited", ignoreCase = true) ||
            body.contains("deposit", ignoreCase = true) ||
            body.contains("received", ignoreCase = true) -> "INCOMING"
            body.contains("debited", ignoreCase = true) ||
            body.contains("withdrawn", ignoreCase = true) ||
            body.contains("withdrawal", ignoreCase = true) ||
            body.contains("payment", ignoreCase = true) ||
            body.contains("sent", ignoreCase = true) ||
            body.contains("paid", ignoreCase = true) -> "OUTGOING"
            else -> "OUTGOING"
        }
        val contactInfo = extractAnyContact(body)
        return Triple(amount, type, contactInfo)
    }

    // ── Shared helpers ─────────────────────────────────────────────────

    private fun extractAmount(body: String): Double? {
        val match = amountRegex.find(body)
        return match?.let { parseAmount(it.groupValues[1]) }
    }

    private fun extractBalance(body: String): Double? {
        val match = balanceRegex.find(body)
        return match?.let { parseAmount(it.groupValues[1]) }
    }

    private fun extractContact(text: String): String? {
        val number = contactNumberRegex.find(text)
        if (number != null) {
            return number.value.replace(" ", "")
        }
        val cleaned = text.trim().replace(Regex("""\s+"""), " ")
        return cleaned.ifBlank { null }
    }

    private fun extractAnyContact(body: String): String? {
        val number = contactNumberRegex.find(body)
        if (number != null) return number.value.replace(" ", "")

        for (pattern in contactNameRegex) {
            val match = pattern.find(body)
            if (match != null) {
                val name = match.groupValues[1].trim().replace(Regex("""\s+"""), " ")
                if (name.isNotBlank()) return name
            }
        }
        return null
    }

    private fun parseAmount(amountStr: String): Double {
        return amountStr.replace(",", "").trim().toDoubleOrNull() ?: 0.0
    }
}