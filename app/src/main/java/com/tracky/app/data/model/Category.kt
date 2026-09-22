package com.tracky.app.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.tracky.app.ui.theme.*

/**
 * Pre-defined transaction categories.
 * Each has a display name, icon, and accent color.
 */
enum class Category(
    val displayName: String,
    val icon: ImageVector,
    val color: Color
) {
    FOOD("Food & Groceries", Icons.Default.Restaurant, Color(0xFFE67E22)),
    TRANSPORT("Transport", Icons.Default.DirectionsBus, Color(0xFF3498DB)),
    BILLS("Bills & Utilities", Icons.Default.Receipt, Color(0xFF9B59B6)),
    AIRTIME("Airtime & Data", Icons.Default.PhoneAndroid, Color(0xFF1ABC9C)),
    BUSINESS("Business", Icons.Default.BusinessCenter, Color(0xFF2C3E50)),
    SAVINGS("Savings", Icons.Default.Savings, Color(0xFF27AE60)),
    PERSONAL("Personal", Icons.Default.Person, Color(0xFFE74C3C)),
    ENTERTAINMENT("Entertainment", Icons.Default.SportsEsports, Color(0xFFF39C12)),
    HEALTH("Health", Icons.Default.LocalHospital, Color(0xFFE91E63)),
    EDUCATION("Education", Icons.Default.School, Color(0xFF3F51B5)),
    FAMILY("Family", Icons.Default.FamilyRestroom, Color(0xFF607D8B)),
    SHOPPING("Shopping", Icons.Default.ShoppingCart, Color(0xFFFF5722)),
    UNCATEGORIZED("Uncategorized", Icons.Default.LabelOff, Color(0xFF9E9E9E));

    companion object {
        /**
         * Resolve a stored category string (from DB) to a Category enum.
         * Returns UNCATEGORIZED for null or unknown values.
         */
        fun fromString(value: String?): Category {
            if (value == null) return UNCATEGORIZED
            return entries.firstOrNull { it.name == value || it.displayName == value } ?: UNCATEGORIZED
        }

        /**
         * All categories suitable for display in a picker (excludes UNCATEGORIZED
         * as a selectable option since it represents the default state).
         */
        val selectable: List<Category>
            get() = entries.filter { it != UNCATEGORIZED }
    }
}