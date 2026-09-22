package com.tracky.app.data.model

/**
 * Rule-based auto-categorization engine.
 * Matches patterns in SMS message body to assign categories.
 */
object AutoCategoryManager {

    private data class Rule(
        val category: Category,
        val keywords: List<String>,
        val channels: List<String>? = null, // null = any channel
        val contactPatterns: List<String>? = null
    )

    // Define rules — order matters, first match wins
    private val rules = listOf(
        Rule(Category.AIRTIME, keywords = listOf("AIRTIME", "BUNDLES", "DATA BUNDLE", "AIRTIME PURCHASE", "BUNDLE")),
        Rule(Category.FOOD, keywords = listOf("TILL", "RESTAURANT", "HOTEL", "CAFE", "KFC", "JAVA", "FOOD", "GROCERY", "SUPERMARKET")),
        Rule(Category.TRANSPORT, keywords = listOf("MATATU", "BODA", "UBER", "BOLT", "LITTLE", "FUEL", "PETROL", "TRANSPORT")),
        Rule(Category.BILLS, keywords = listOf("PAYBILL", "KPLC", "NSSF", "NHIF", "WATER", "RENT", "BILL")),
        Rule(Category.BUSINESS, keywords = listOf("B2C", "C2B", "LIPA NA", "BUY GOODS", "MERCHANT", "TILL")),
        Rule(Category.SAVINGS, keywords = listOf("DEPOSIT", "SAVE", "FULIZA REPAYMENT")),
        Rule(Category.ENTERTAINMENT, keywords = listOf("BET", "GAMING", "MOVIE", "SPORT", "NETFLIX", "SHOWMAX")),
        Rule(Category.HEALTH, keywords = listOf("HOSPITAL", "CLINIC", "PHARMACY", "DR.", "DOCTOR", "NHIF")),
        Rule(Category.EDUCATION, keywords = listOf("SCHOOL", "UNIVERSITY", "TUITION", "ACADEMY", "INSTITUTE")),
        Rule(Category.SHOPPING, keywords = listOf("SHOP", "STORE", "MALL", "JUMIA", "ONLINE"))
    )

    /**
     * Auto-categorize a transaction based on SMS body and channel.
     */
    fun categorize(messageBody: String, channel: String, contact: String?): Category {
        val bodyUpper = messageBody.uppercase()
        val channelUpper = channel.uppercase()
        val contactUpper = contact?.uppercase() ?: ""

        for (rule in rules) {
            if (rule.channels != null && channelUpper !in rule.channels) continue

            val keywordMatch = rule.keywords.any { bodyUpper.contains(it) }
            val contactMatch = rule.contactPatterns?.any { contactUpper.contains(it) } ?: false

            if (keywordMatch || contactMatch) return rule.category
        }

        return Category.UNCATEGORIZED
    }
}