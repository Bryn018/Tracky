package com.tracky.app.data.local.entity

import com.tracky.app.data.model.Money
import com.tracky.app.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the SMS-identity dedup that replaced amount/contact proximity.
 *
 * The old rule was "same amount, same contact, same type, within 60 seconds".
 * These tests pin the two behaviours that rule got wrong: it deleted genuine
 * repeated purchases, and it collided on unparsed contacts.
 */
class SmsIdentityTest {

    private fun tx(smsKey: String, amountCents: Long, contact: String?, type: String, ts: Long) =
        TransactionEntity(
            type = type,
            amountCents = amountCents,
            channel = "M-Pesa",
            contact = contact,
            senderName = contact,
            messageBody = "body",
            timestamp = ts,
            smsKey = smsKey
        )

    @Test
    fun `the same message always produces the same key`() {
        val a = SmsIdentity.key("MPESA", 1_700_000_000_000L, "Ksh 100.00 sent to John")
        val b = SmsIdentity.key("MPESA", 1_700_000_000_000L, "Ksh 100.00 sent to John")
        assertEquals(a, b)
    }

    @Test
    fun `key is stable across trivial whitespace and case differences`() {
        // SMS re-reads can differ in surrounding whitespace; that must not
        // make the same message look new.
        val a = SmsIdentity.key("MPESA", 1L, "Ksh 100.00 sent to John")
        val b = SmsIdentity.key("mpesa", 1L, "  KSH 100.00 SENT TO JOHN  ")
        assertEquals(a, b)
    }

    @Test
    fun `REGRESSION two identical purchases minutes apart get distinct keys`() {
        // Splitting a bill, or topping up twice. The old 60-second rule
        // deleted the second one.
        val first = SmsIdentity.key("MPESA", 1_000L, "Ksh 100.00 sent to SUPERMARKET")
        val second = SmsIdentity.key("MPESA", 61_000L, "Ksh 100.00 sent to SUPERMARKET")
        assertNotEquals(first, second)
    }

    @Test
    fun `REGRESSION identical amounts with an unparsed contact stay distinct`() {
        // Two unrelated Ksh 500 credits 10 seconds apart. Same amount, and the
        // contact is null for both, so the old rule treated them as one.
        val first = SmsIdentity.key("MPESA", 1_000L, "Ksh 500.00 received from A")
        val second = SmsIdentity.key("MPESA", 11_000L, "Ksh 500.00 received from B")
        assertNotEquals(first, second)
    }

    @Test
    fun `different senders of the same text are distinct messages`() {
        val a = SmsIdentity.key("MPESA", 1L, "Ksh 100.00 sent to John")
        val b = SmsIdentity.key("AIRTEL", 1L, "Ksh 100.00 sent to John")
        assertNotEquals(a, b)
    }

    @Test
    fun `segments of one message get distinct keys`() {
        // A concatenated two-transaction SMS must not collide with itself.
        val first = SmsIdentity.key("MPESA", 1L, "Ksh 100.00 sent to A ... Ksh 200.00 sent to B", segment = 0)
        val second = SmsIdentity.key("MPESA", 1L, "Ksh 100.00 sent to A ... Ksh 200.00 sent to B", segment = 1)
        assertNotEquals(first, second)
    }

    @Test
    fun `keys are short enough to index comfortably`() {
        val key = SmsIdentity.key("MPESA", 1L, "Ksh 100.00 sent to John")
        assertEquals(16, key.length)
        assertTrue(key.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `money is never negative and the constructor rejects it`() {
        // A negative amount would mean direction was encoded in the sign,
        // which is what the old Double model allowed.
        val threw = runCatching { Money(-1L) }.isFailure
        assertTrue(threw)
        assertEquals(Money.ZERO, Money(0L))
    }

    @Test
    fun `transaction type multipliers drive correct net spending`() {
        val outgoing = tx("a", 100_00L, "John", TransactionType.OUTGOING.name, 1L)
        val reversal = tx("b", 100_00L, "John", TransactionType.REVERSAL.name, 2L)

        val typeOf = { t: String -> TransactionType.fromString(t)!! }
        val net = listOf(outgoing, reversal)
            .fold(0L) { acc, t -> acc + (t.amountCents * typeOf(t.type).spendingMultiplier) }

        // A reversed purchase nets to zero rather than remaining as spending.
        assertEquals(0L, net)
    }
}
