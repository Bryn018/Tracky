package com.tracky.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SummaryCard(
    title: String,
    value: String,
    color: Color,
    icon: ImageVector = Icons.Default.AccountBalance,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val animAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "fade"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .alpha(animAlpha)
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Icon at top-right
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.TopEnd)
            )
            // Content stacked bottom-left
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    ),
                    color = color
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
