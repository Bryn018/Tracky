package com.tracky.app.data

import android.content.Context
import android.net.Uri
import com.tracky.app.data.local.entity.TransactionEntity
import com.tracky.app.data.model.Money
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    /**
     * Exports transactions to CSV and writes to the given URI.
     * Returns the number of rows exported (excluding header).
     *
     * Amounts are written as exact decimal strings from integer cents, so a
     * spreadsheet receives "1500.50" rather than a float artefact.
     */
    fun exportTransactionsToCsv(
        context: Context,
        transactions: List<TransactionEntity>,
        uri: Uri
    ): Int {
        val outputStream: OutputStream = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Cannot open output stream for $uri")

        outputStream.bufferedWriter().use { writer ->
            writer.write("ID,Type,Amount,Channel,Contact,Sender,Timestamp,Balance,Notes,Category,MessageBody\n")

            for (tx in transactions) {
                val line = buildString {
                    append(tx.id); append(',')
                    append(escapeCsv(tx.type)); append(',')
                    append(Money(tx.amountCents).toPlainString()); append(',')
                    append(escapeCsv(tx.channel)); append(',')
                    append(escapeCsv(tx.contact ?: "")); append(',')
                    append(escapeCsv(tx.senderName ?: "")); append(',')
                    append(escapeCsv(formatTimestamp(tx.timestamp))); append(',')
                    append(tx.balanceCents?.let { Money(it).toPlainString() } ?: ""); append(',')
                    append(escapeCsv(tx.notes ?: "")); append(',')
                    append(escapeCsv(tx.category ?: "")); append(',')
                    append(escapeCsv(tx.messageBody))
                }
                writer.write(line)
                writer.newLine()
            }
        }

        return transactions.size
    }

    // SimpleDateFormat is not thread-safe and export runs on a coroutine
    // dispatcher, so a formatter is created per call rather than shared.
    private fun formatTimestamp(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(millis))

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
