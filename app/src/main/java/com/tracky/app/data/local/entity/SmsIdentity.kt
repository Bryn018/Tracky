package com.tracky.app.data.local.entity

import java.security.MessageDigest

/**
 * Identity of a source SMS, used to decide whether a transaction has already
 * been imported.
 *
 * The previous approach compared amount + contact + type within a 60-second
 * window. That rule could not tell a re-delivered message apart from two
 * genuine identical purchases, so it deleted real transactions: buying Ksh 100
 * twice at the same till 30 seconds apart lost the second one, and any two
 * Ksh 500 credits with an unparsed contact collided.
 *
 * Keying on the message itself inverts the problem correctly. The same
 * message — however many times it is read by the receiver, the backfill, or
 * the periodic scan — always produces the same key, and two distinct
 * messages never collide no matter how similar their contents.
 */
object SmsIdentity {

    /**
     * Stable key for a message. Sender and timestamp pin the delivery;
     * the body covers the case where one SMS carries several transactions
     * (those get distinct [segment] values instead).
     */
    fun key(sender: String, timestamp: Long, body: String, segment: Int = 0): String {
        val canonical = buildString {
            append(sender.trim().lowercase())
            append('|')
            append(timestamp)
            append('|')
            append(segment)
            append('|')
            append(body.trim().lowercase())
        }
        return sha256(canonical)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        // 16 hex chars is ample: these keys only need to be unique within one
        // user's database, and a shorter key keeps the unique index small.
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }
}
