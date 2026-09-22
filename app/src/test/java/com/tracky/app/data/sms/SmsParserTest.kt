package com.tracky.app.data.sms

import org.junit.Assert.*
import org.junit.Test

class SmsParserTest {

    private val fixedTimestamp = 1700000000000L

    @Test
    fun `test mpesa received`() {
        val body = "Ksh 1,500.00 received from John Doe on 25/5/2026"
        val sender = "MPESA"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals(1500.0, result!!.amount, 0.001)
        assertEquals("INCOMING", result.type)
        assertEquals("M-Pesa", result.channel)
        assertEquals("John Doe", result.contact)
    }

    @Test
    fun `test mpesa sent`() {
        val body = "Ksh 500.00 sent to Jane Smith on 25/5/2026"
        val sender = "MPESA"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals(500.0, result!!.amount, 0.001)
        assertEquals("OUTGOING", result.type)
        assertEquals("M-Pesa", result.channel)
        assertEquals("Jane Smith", result.contact)
    }

    @Test
    fun `test mpesa paid to till`() {
        val body = "Ksh 100.00 paid to TILL NUMBER 123456 on 25/5/2026 at 11:00 AM. New M-Pesa balance is Ksh 4,000.00"
        val sender = "MPESA"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals(100.0, result!!.amount, 0.001)
        assertEquals("OUTGOING", result.type)
        assertEquals("M-Pesa", result.channel)
        assertTrue(result.contact?.contains("TILL") == true)
    }

    @Test
    fun `test airtel money received`() {
        val body = "Received Ksh 2,000.00 from Alice"
        val sender = "AIRTEL"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals(2000.0, result!!.amount, 0.001)
        assertEquals("INCOMING", result.type)
        assertEquals("Airtel Money", result.channel)
    }

    @Test
    fun `test airtel money sent`() {
        val body = "Sent Ksh 300.00 to Bob"
        val sender = "AIRTEL"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals(300.0, result!!.amount, 0.001)
        assertEquals("OUTGOING", result.type)
        assertEquals("Airtel Money", result.channel)
    }

    @Test
    fun `test balance extraction`() {
        val body = "Ksh 1,500.00 received from John Doe on 25/5/2026. New M-Pesa balance is Ksh 4,500.00"
        val sender = "MPESA"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals("INCOMING", result!!.type)
        assertEquals(1500.0, result.amount, 0.001)
        assertNotNull(result.balance)
        assertEquals(4500.0, result.balance!!, 0.001)
    }

    @Test
    fun `test non financial sms returns null`() {
        val body = "Hello, how are you? Let's meet for lunch."
        val sender = "UNKNOWN"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNull(result)
    }

    @Test
    fun `test amount with decimals`() {
        val body = "Ksh 100.50 received from Test"
        val sender = "MPESA"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals(100.5, result!!.amount, 0.001)
        assertEquals("INCOMING", result.type)
        assertEquals("Test", result.contact)
    }

    @Test
    fun `test amount without decimals`() {
        val body = "Ksh 1000 sent to Person"
        val sender = "MPESA"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals(1000.0, result!!.amount, 0.001)
        assertEquals("OUTGOING", result.type)
        assertEquals("Person", result.contact)
    }

    @Test
    fun `test contact number extraction`() {
        val body = "Ksh 500.00 sent to 0722 000 000 on 25/5/2026"
        val sender = "MPESA"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals(500.0, result!!.amount, 0.001)
        assertEquals("OUTGOING", result.type)
        assertEquals("0722000000", result.contact)
    }

    @Test
    fun `test generic financial message with unknown sender returns null`() {
        val body = "Ksh 500.00 received from Someone"
        val sender = "UNKNOWN"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNull(result)
    }

    @Test
    fun `test empty message returns null`() {
        val body = ""
        val sender = "MPESA"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNull(result)
    }

    @Test
    fun `test mpesa short format`() {
        val body = "Ksh 100.00 received"
        val sender = "MPESA"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals(100.0, result!!.amount, 0.001)
        assertEquals("INCOMING", result.type)
    }

    @Test
    fun `test bank credit transaction from equity`() {
        val body = "Your account EQ1234567 has been credited with Ksh 5,000.00 on 25/5/2026 at 14:30. Available balance: Ksh 45,000.00"
        val sender = "EquityBank"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals(5000.0, result!!.amount, 0.001)
        assertEquals("INCOMING", result.type)
        assertEquals("Bank", result.channel)
    }

    @Test
    fun `test bank debit transaction from kcb`() {
        val body = "Ksh 2,500.00 debited from account KCB123456 on 25/5/2026. Balance: Ksh 12,000.00"
        val sender = "KCB"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals(2500.0, result!!.amount, 0.001)
        assertEquals("OUTGOING", result.type)
        assertEquals("Bank", result.channel)
    }

    @Test
    fun `test scam message returns null`() {
        val body = "Congratulations! You have won Ksh 500,000 in the M-Pesa lottery! Call 0712345678 to claim your prize now!!!"
        val sender = "MPESA"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNull(result)
    }

    @Test
    fun `test mpesa withdrawn from`() {
        val body = "Ksh 2,000.00 withdrawn from 0712 345 678 on 25/5/2026 at 3:00 PM. New M-Pesa balance is Ksh 8,000.00"
        val sender = "MPESA"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals(2000.0, result!!.amount, 0.001)
        assertEquals("OUTGOING", result.type)
        assertEquals("M-Pesa", result.channel)
    }

    @Test
    fun `test auto categorization for airtime`() {
        val body = "Ksh 100.00 sent to AIRTIME PURCHASE on 25/5/2026"
        val sender = "MPESA"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals("AIRTIME", result!!.category)
    }

    @Test
    fun `test auto categorization for food`() {
        val body = "Ksh 500.00 paid to TILL NUMBER 123456 on 25/5/2026"
        val sender = "MPESA"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals("FOOD", result!!.category)
    }

    @Test
    fun `test auto categorization for transport`() {
        val body = "Ksh 300.00 sent to BODA on 25/5/2026"
        val sender = "MPESA"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals("TRANSPORT", result!!.category)
    }

    @Test
    fun `test auto categorization for bills`() {
        val body = "Ksh 2,000.00 paid to PAYBILL 123456 on 25/5/2026"
        val sender = "MPESA"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals("BILLS", result!!.category)
    }

    @Test
    fun `test auto categorization for business`() {
        val body = "Ksh 5,000.00 paid to BUY GOODS on 25/5/2026"
        val sender = "MPESA"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals("BUSINESS", result!!.category)
    }

    @Test
    fun `test auto categorization for entertainment`() {
        val body = "Ksh 1,500.00 sent to BET on 25/5/2026"
        val sender = "MPESA"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals("ENTERTAINMENT", result!!.category)
    }

    @Test
    fun `test auto categorization for health`() {
        val body = "Ksh 3,000.00 sent to HOSPITAL on 25/5/2026"
        val sender = "MPESA"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals("HEALTH", result!!.category)
    }

    @Test
    fun `test auto categorization for education`() {
        val body = "Ksh 10,000.00 sent to SCHOOL on 25/5/2026"
        val sender = "MPESA"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals("EDUCATION", result!!.category)
    }

    @Test
    fun `test auto categorization for shopping`() {
        val body = "Ksh 2,500.00 sent to SHOP on 25/5/2026"
        val sender = "MPESA"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals("SHOPPING", result!!.category)
    }

    @Test
    fun `test auto categorization for savings`() {
        val body = "Ksh 5,000.00 sent to DEPOSIT on 25/5/2026"
        val sender = "MPESA"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals("SAVINGS", result!!.category)
    }

    @Test
    fun `test auto categorization for uncategorized`() {
        val body = "Ksh 500.00 sent to John on 25/5/2026"
        val sender = "MPESA"
        val result = SmsParser.parseSms(body, sender, fixedTimestamp)
        assertNotNull(result)
        assertEquals("UNCATEGORIZED", result!!.category)
    }
}