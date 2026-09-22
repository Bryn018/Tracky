package com.tracky.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tracky.app.data.local.entity.TransactionEntity
import com.tracky.app.data.model.Category
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionCard(
    transaction: TransactionEntity,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isIncoming = transaction.type == "INCOMING"
    val amountPrefix = if (isIncoming) "+" else "-"
    val amountColor = if (isIncoming) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
    val badgeColor = if (isIncoming) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
    val badgeTextColor = if (isIncoming) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer

    val category = Category.fromString(transaction.category)

    val formattedDate = remember(transaction.timestamp) {
        SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp))
    }
    val contactName = remember(transaction.contact, transaction.senderName) {
        transaction.contact ?: transaction.senderName ?: "Unknown"
    }
    val formattedAmount = remember(transaction.amount) {
        String.format("%.2f", transaction.amount)
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val animAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "cardFade"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .alpha(animAlpha)
            .animateContentSize()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = if (isIncoming) "IN" else "OUT",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = transaction.channel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (category != Category.UNCATEGORIZED) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = category.color.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = category.color
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = category.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = category.color
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = contactName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                if (transaction.notes != null && transaction.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = transaction.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "$amountPrefixKsh $formattedAmount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}