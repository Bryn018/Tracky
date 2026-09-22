package com.tracky.app.data.local.entity

/**
 * Utility to detect duplicate transactions.
 */
object DuplicateDetector {

    /**
     * Check if a transaction is a duplicate of an existing one.
     * Duplicates have same amount, same contact, and timestamps within 60 seconds.
     */
    fun isDuplicate(candidate: TransactionEntity, existing: List<TransactionEntity>): Boolean {
        return existing.any { tx ->
            tx.amount == candidate.amount &&
                tx.contact == candidate.contact &&
                tx.type == candidate.type &&
                kotlin.math.abs(tx.timestamp - candidate.timestamp) < 60_000
        }
    }

    /**
     * Find potential duplicates in a list of transactions.
     */
    fun findDuplicates(transactions: List<TransactionEntity>): List<Pair<TransactionEntity, TransactionEntity>> {
        val duplicates = mutableListOf<Pair<TransactionEntity, TransactionEntity>>()
        for (i in transactions.indices) {
            for (j in i + 1 until transactions.size) {
                val a = transactions[i]
                val b = transactions[j]
                if (a.amount == b.amount &&
                    a.contact == b.contact &&
                    a.type == b.type &&
                    kotlin.math.abs(a.timestamp - b.timestamp) < 60_000
                ) {
                    duplicates.add(a to b)
                }
            }
        }
        return duplicates
    }
}