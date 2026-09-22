package com.tracky.app.data.sms

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.tracky.app.data.local.entity.TransactionEntity

/**
 * Reads SMS messages from the device's ContentResolver and parses them
 * into mobile money transaction entities using [SmsParser].
 */
class SmsReader(private val context: Context) {

    /**
     * Reads existing SMS inbox messages from the last [days] days,
     * parses each one through [SmsParser], and returns the list of
     * successfully parsed [TransactionEntity] objects.
     *
     * Messages that don't match mobile money patterns are filtered out.
     */
    fun readExistingSms(days: Int = DEFAULT_HISTORY_DAYS): List<TransactionEntity> {
        val transactions = mutableListOf<TransactionEntity>()
        val uri = Uri.parse("content://sms/inbox")

        val cutoff = System.currentTimeMillis() - (days.toLong() * MILLIS_PER_DAY)

        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val selection = "${Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(cutoff.toString())

        val cursor = context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "${Telephony.Sms.DATE} DESC"
        )

        cursor?.use { c ->
            val addressIndex = c.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = c.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = c.getColumnIndex(Telephony.Sms.DATE)

            if (addressIndex < 0 || bodyIndex < 0 || dateIndex < 0) {
                return@use
            }

            while (c.moveToNext()) {
                val address = c.getString(addressIndex) ?: ""
                val body = c.getString(bodyIndex) ?: ""
                val date = c.getLong(dateIndex)

                if (body.isBlank()) continue

                val transaction = SmsParser.parseSms(body, address, date)
                if (transaction != null) {
                    transactions.add(transaction)
                }
            }
        }

        return transactions
    }

    companion object {
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

        /** How far back the first-install history import reaches. */
        const val DEFAULT_HISTORY_DAYS = 90
    }
}
