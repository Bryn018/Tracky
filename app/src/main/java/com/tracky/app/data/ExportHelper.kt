package com.tracky.app.data

import android.content.Context
import android.net.Uri
import com.tracky.app.data.local.entity.TransactionEntity
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * Exports transactions to CSV and writes to the given URI.
     * Returns the number of rows exported (excluding header).
     */
    fun exportTransactionsToCsv(
        context: Context,
        transactions: List<TransactionEntity>,
        uri: Uri
    ): Int {
        val outputStream: OutputStream = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Cannot open output stream for $uri")

        return try {
            outputStream.bufferedWriter().use { writer ->
                // CSV header
                writer.write("ID,Type,Amount,Channel,Contact,Sender,Timestamp,Balance,Notes,Category,MessageBody\n")

                // Data rows
                for (tx in transactions) {
                    val timestamp = dateFormat.format(Date(tx.timestamp))
                    val line = buildString {
                        append(tx.id); append(',')
                        append(escapeCsv(tx.type)); append(',')
                        append(tx.amount); append(',')
                        append(escapeCsv(tx.channel)); append(',')
                        append(escapeCsv(tx.contact ?: "")); append(',')
                        append(escapeCsv(tx.senderName ?: "")); append(',')
                        append(escapeCsv(timestamp)); append(',')
                        append(tx.balance?.toString() ?: ""); append(',')
                        append(escapeCsv(tx.notes ?: "")); append(',')
                        append(escapeCsv(tx.category ?: "")); append(',')
                        append(escapeCsv(tx.messageBody))
                    }
                    writer.write(line)
                    writer.newLine()
                }
            }
            transactions.size
        } catch (e: Exception) {
            throw e
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
