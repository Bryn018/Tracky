package com.tracky.app.data.sms

import com.tracky.app.data.model.Money
import com.tracky.app.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parser tests.
 *
 * The `regression` block at the end pins defects that were live in the
 * previous implementation. Each of those tests failed against the old code;
 * they exist so the same bug cannot return unnoticed.
 */
class SmsParserTest {

    private val ts = 1_700_000_000_000L

    private fun parse(body: String, sender: String = "MPESA") =
        SmsParser.parseSms(body, sender, ts)

    private fun shillings(value: String): Long =
        Money.parseOrNull(value)!!.cents

    // ── M-Pesa ─────────────────────────────────────────────────────────

    @Test
    fun `mpesa received from person`() {
        val r = parse("Ksh 1,500.00 received from John Doe on 25/5/2026")
        assertNotNull(r)
        assertEquals(150_000L, r!!.amountCents)
        assertEquals(TransactionType.INCOMING, type(r))
        assertEquals("M-Pesa", r.channel)
        assertEquals("John Doe", r.contact)
    }

    @Test
    fun `mpesa sent to person`() {
        val r = parse("Ksh 500.00 sent to Jane Smith on 25/5/2026")
        assertEquals(50_000L, r!!.amountCents)
        assertEquals(TransactionType.OUTGOING, type(r))
        assertEquals("Jane Smith", r.contact)
    }

    @Test
    fun `mpesa paid to till`() {
        val r = parse("Ksh 100.00 paid to TILL NUMBER 123456 on 25/5/2026 at 11:00 AM. New M-Pesa balance is Ksh 4,000.00")
        assertEquals(10_000L, r!!.amountCents)
        assertEquals(TransactionType.OUTGOING, type(r))
        assertEquals(400_000L, r.balanceCents)
    }

    @Test
    fun `mpesa withdrawn from agent`() {
        val r = parse("Ksh 2,000.00 withdrawn from 0712 345 678 on 25/5/2026 at 3:00 PM. New M-Pesa balance is Ksh 8,000.00")
        assertEquals(200_000L, r!!.amountCents)
        assertEquals(TransactionType.OUTGOING, type(r))
    }

    @Test
    fun `balance is captured separately from the amount`() {
        val r = parse("Ksh 1,500.00 received from John Doe on 25/5/2026. New M-Pesa balance is Ksh 4,500.00")
        assertEquals(150_000L, r!!.amountCents)
        assertEquals(450_000L, r.balanceCents)
    }

    @Test
    fun `sent amount to person without ksh prefix`() {
        val r = parse("Sent 3,200.00 to John Doe on 25/5/2026")
        assertEquals(320_000L, r!!.amountCents)
        assertEquals(TransactionType.OUTGOING, type(r))
    }

    @Test
    fun `contact number is normalised`() {
        val r = parse("Ksh 500.00 sent to 0722 000 000 on 25/5/2026")
        assertEquals("0722000000", r!!.contact)
    }

    // ── Airtel ────────────────────────────────────────────────────────

    @Test
    fun `airtel received`() {
        val r = parse("Received Ksh 2,000.00 from Alice", "AIRTEL")
        assertEquals(200_000L, r!!.amountCents)
        assertEquals(TransactionType.INCOMING, type(r))
        assertEquals("Airtel Money", r.channel)
    }

    @Test
    fun `airtel sent`() {
        val r = parse("Sent Ksh 300.00 to Bob", "AIRTEL")
        assertEquals(30_000L, r!!.amountCents)
        assertEquals(TransactionType.OUTGOING, type(r))
    }

    // ── Bank ──────────────────────────────────────────────────────────

    @Test
    fun `bank credit`() {
        val r = parse(
            "Your account EQ1234567 has been credited with Ksh 5,000.00 on 25/5/2026 at 14:30. Available balance: Ksh 45,000.00",
            "EquityBank"
        )
        assertEquals(500_000L, r!!.amountCents)
        assertEquals(TransactionType.INCOMING, type(r))
        assertEquals("Bank", r.channel)
    }

    @Test
    fun `bank debit`() {
        val r = parse(
            "Ksh 2,500.00 debited from account KCB123456 on 25/5/2026. Balance: Ksh 12,000.00",
            "KCB"
        )
        assertEquals(250_000L, r!!.amountCents)
        assertEquals(TransactionType.OUTGOING, type(r))
    }

    // ── Rejection ─────────────────────────────────────────────────────

    @Test
    fun `non financial message is rejected`() {
        assertNull(parse("Hello, how are you? Let's meet for lunch.", "UNKNOWN"))
    }

    @Test
    fun `empty message is rejected`() {
        assertNull(parse("", "MPESA"))
    }

    @Test
    fun `balance only message is not a transaction`() {
        assertNull(parse("New M-Pesa balance is Ksh 4,000.00"))
    }

    @Test
    fun `promotional message is not a transaction`() {
        assertNull(parse("Buy airtime and win. Dial *334# today. Terms apply."))
    }

    @Test
    fun `unknown sender with no provider hint is rejected`() {
        assertNull(parse("Ksh 500.00 received from Someone", "UNKNOWN"))
    }

    // ── Scam detection ────────────────────────────────────────────────

    @Test
    fun `lottery scam from a spoofed MPESA sender is rejected`() {
        // The old code trusted the sender address, so this was recorded as a
        // real Ksh 500,000 outflow.
        val r = parse(
            "Congratulations! You have won Ksh 500,000 in the M-Pesa lottery! Call 0712345678 to claim your prize now!!!",
            "MPESA"
        )
        assertNull(r)
    }

    @Test
    fun `lottery scam from an arbitrary sender is rejected`() {
        assertNull(parse(
            "Congratulations! You have won Ksh 500,000. Call 0712345678 to claim now!!!",
            "MPESA"
        ))
    }

    @Test
    fun `pin harvesting phishing is rejected`() {
        assertNull(parse(
            "URGENT: your KCB account is suspended. Send your PIN to 0712345678 to verify your account now",
            "KCB"
        ))
    }

    @Test
    fun `legitimate mpesa message with a security footer is still captured`() {
        // The reason the sender allowlist was added in the first place — real
        // confirmations do carry PIN/URL/urgent wording in footers.
        val r = parse("Ksh 500.00 sent to John on 25/5/2026. Never share your PIN. Visit www.safaricom.co.ke")
        assertNotNull(r)
        assertEquals(50_000L, r!!.amountCents)
    }

    // ── Regression: balance must not be booked as a transaction ───────

    @Test
    fun `REGRESSION leading balance is not the transaction amount`() {
        // Previously recorded as INCOMING 50,000 instead of 2,000.
        val r = parse(
            "Available balance Ksh 50,000.00. You have received Ksh 2,000.00 from John",
            "KCB"
        )
        assertNotNull(r)
        assertEquals(200_000L, r!!.amountCents)
        assertEquals(TransactionType.INCOMING, type(r))
    }

    @Test
    fun `REGRESSION trailing balance does not become the contact name`() {
        val r = parse(
            "You have received Ksh 2,000.00 from John. Available balance Ksh 50,000.00",
            "KCB"
        )
        assertNotNull(r)
        assertEquals(200_000L, r!!.amountCents)
        assertEquals("John", r!!.contact)
    }

    // ── Regression: reversals and refunds are not spending ────────────

    @Test
    fun `REGRESSION reversal is typed as REVERSAL not OUTGOING`() {
        val r = parse("Ksh 1,200.00 reversed. Transaction reversed for M-Pesa. New M-Pesa balance is Ksh 3,000.00")
        assertNotNull(r)
        assertEquals(TransactionType.REVERSAL, type(r!!))
        assertEquals(120_000L, r.amountCents)
        // A reversal must not inflate spending.
        assertEquals(-1, TransactionType.REVERSAL.spendingMultiplier)
    }

    @Test
    fun `REGRESSION reversed sent transaction is typed as REVERSAL`() {
        val r = parse("You have reversed Ksh 1,200.00 sent to JOHN. New M-Pesa balance is Ksh 3,000.00")
        assertNotNull(r)
        assertEquals(TransactionType.REVERSAL, type(r!!))
    }

    @Test
    fun `REGRESSION refund is typed as REFUND`() {
        val r = parse("Ksh 300.00 refund due to cancelled transaction. New M-Pesa balance is Ksh 2,000.00")
        assertEquals(TransactionType.REFUND, type(r!!))
        assertEquals(-1, TransactionType.REFUND.spendingMultiplier)
    }

    // ── Regression: no zero-value rows ─────────────────────────────────

    @Test
    fun `REGRESSION zero amount is not stored as a transaction`() {
        assertNull(parse("Ksh 0.00 sent to John on 25/5/2026"))
    }

    // ── Regression: every movement in a message is captured ───────────

    @Test
    fun `REGRESSION concatenated two-transaction message yields both`() {
        val results = SmsParser.parseAll(
            "Ksh 100.00 sent to AIRTIME on 01/01/2026 at 09:00 AM. New M-Pesa balance is Ksh 900.00 " +
                "Ksh 200.00 sent to BODA on 01/01/2026 at 09:05 AM. New M-Pesa balance is Ksh 700.00",
            "MPESA", ts
        )
        assertEquals(2, results.size)
        val amounts = results.map { it.amountCents }.sorted()
        assertEquals(listOf(10_000L, 20_000L), amounts)
        // Distinct keys, so both survive dedup.
        assertEquals(2, results.map { it.smsKey }.toSet().size)
    }

    // ── Money arithmetic ──────────────────────────────────────────────

    @Test
    fun `money parses thousand separators exactly`() {
        // 123,456,789 KES is 12,345,678,900 cents.
        assertEquals(12_345_678_900L, shillings("123,456,789"))
    }

    @Test
    fun `money keeps cents precision`() {
        assertEquals(150_050L, shillings("1,500.50"))
        assertEquals(150_000L, shillings("1500"))
    }

    @Test
    fun `money rejects garbage instead of returning zero`() {
        assertNull(Money.parseOrNull("not a number"))
        assertNull(Money.parseOrNull(""))
        assertNull(Money.parseOrNull(null))
    }

    @Test
    fun `money rejects sub-cent precision rather than rounding silently`() {
        assertNull(Money.parseOrNull("10.999"))
    }

    @Test
    fun `money arithmetic is exact where floating point is not`() {
        // 0.1 + 0.2 != 0.3 in binary floating point; in cents it is exact.
        val total = listOf(Money(10), Money(20)).fold(Money.ZERO) { acc, m -> acc + m }
        assertEquals(30L, total.cents)
    }

    @Test
    fun `money formats with grouping and two decimals`() {
        assertEquals("1,500.50", Money(150_050).toPlainString())
        assertEquals("1,500", Money(150_000).toPlainString())
        assertEquals("Ksh 1,500.50", Money(150_050).toDisplayString())
    }

    // ── Auto-categorisation ───────────────────────────────────────────

    @Test
    fun `categorises airtime`() {
        assertEquals("AIRTIME", parse("Ksh 100.00 sent to AIRTIME PURCHASE on 25/5/2026")!!.category)
    }

    @Test
    fun `categorises food`() {
        assertEquals("FOOD", parse("Ksh 500.00 paid to TILL NUMBER 123456 on 25/5/2026")!!.category)
    }

    @Test
    fun `categorises transport`() {
        assertEquals("TRANSPORT", parse("Ksh 300.00 sent to BODA on 25/5/2026")!!.category)
    }

    @Test
    fun `categorises bills`() {
        assertEquals("BILLS", parse("Ksh 2,000.00 paid to PAYBILL 123456 on 25/5/2026")!!.category)
    }

    @Test
    fun `categorises business`() {
        assertEquals("BUSINESS", parse("Ksh 5,000.00 paid to BUY GOODS on 25/5/2026")!!.category)
    }

    @Test
    fun `categorises entertainment`() {
        assertEquals("ENTERTAINMENT", parse("Ksh 1,500.00 sent to BET on 25/5/2026")!!.category)
    }

    @Test
    fun `categorises health`() {
        assertEquals("HEALTH", parse("Ksh 3,000.00 sent to HOSPITAL on 25/5/2026")!!.category)
    }

    @Test
    fun `categorises education`() {
        assertEquals("EDUCATION", parse("Ksh 10,000.00 sent to SCHOOL on 25/5/2026")!!.category)
    }

    @Test
    fun `categorises shopping`() {
        assertEquals("SHOPPING", parse("Ksh 2,500.00 sent to SHOP on 25/5/2026")!!.category)
    }

    @Test
    fun `categorises savings`() {
        assertEquals("SAVINGS", parse("Ksh 5,000.00 sent to DEPOSIT on 25/5/2026")!!.category)
    }

    @Test
    fun `falls back to uncategorized`() {
        assertEquals("UNCATEGORIZED", parse("Ksh 500.00 sent to John on 25/5/2026")!!.category)
    }

    private fun type(tx: com.tracky.app.data.local.entity.TransactionEntity) =
        TransactionType.fromString(tx.type)
}