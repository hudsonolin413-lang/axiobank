package org.dals.project.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shimmer effect modifier for skeleton loading
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.3f),
        Color.LightGray.copy(alpha = 0.5f),
        Color.LightGray.copy(alpha = 0.3f),
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim - 1000f, translateAnim - 1000f),
            end = Offset(translateAnim, translateAnim)
        )
    )
}

/**
 * Skeleton box with shimmer effect
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    width: Dp? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp)
) {
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(shape)
            .shimmerEffect()
    )
}

/**
 * Skeleton card for transaction loading
 */
@Composable
fun TransactionCardSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon skeleton
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .shimmerEffect()
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Title skeleton
                ShimmerBox(height = 16.dp, width = 120.dp)
                Spacer(modifier = Modifier.height(8.dp))
                // Subtitle skeleton
                ShimmerBox(height = 12.dp, width = 80.dp)
            }

            // Amount skeleton
            ShimmerBox(height = 18.dp, width = 60.dp)
        }
    }
}

/**
 * Skeleton card for balance/account card
 */
@Composable
fun BalanceCardSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Label skeleton
            ShimmerBox(height = 14.dp, width = 100.dp)
            Spacer(modifier = Modifier.height(16.dp))
            // Balance skeleton
            ShimmerBox(height = 32.dp, width = 180.dp)
            Spacer(modifier = Modifier.height(12.dp))
            // Account number skeleton
            ShimmerBox(height = 12.dp, width = 140.dp)
        }
    }
}

/**
 * Skeleton card for loan card
 */
@Composable
fun LoanCardSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Loan type skeleton
                    ShimmerBox(height = 18.dp, width = 100.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    // Loan ID skeleton
                    ShimmerBox(height = 12.dp, width = 80.dp)
                }
                // Status badge skeleton
                ShimmerBox(
                    height = 24.dp,
                    width = 60.dp,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount row skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerBox(height = 14.dp, width = 80.dp)
                ShimmerBox(height = 14.dp, width = 100.dp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar skeleton
            ShimmerBox(height = 8.dp, shape = RoundedCornerShape(4.dp))
        }
    }
}

/**
 * Skeleton card for credit/debit card
 */
@Composable
fun BankCardSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Card type skeleton
            ShimmerBox(height = 16.dp, width = 80.dp)

            Spacer(modifier = Modifier.height(24.dp))

            // Card number skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerBox(height = 20.dp, width = 50.dp)
                ShimmerBox(height = 20.dp, width = 50.dp)
                ShimmerBox(height = 20.dp, width = 50.dp)
                ShimmerBox(height = 20.dp, width = 50.dp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Cardholder name label
                    ShimmerBox(height = 10.dp, width = 70.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    // Cardholder name
                    ShimmerBox(height = 14.dp, width = 120.dp)
                }
                Column {
                    // Expiry label
                    ShimmerBox(height = 10.dp, width = 40.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    // Expiry date
                    ShimmerBox(height = 14.dp, width = 50.dp)
                }
            }
        }
    }
}

/**
 * Skeleton for list items
 */
@Composable
fun ListItemSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon skeleton
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .shimmerEffect()
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(height = 16.dp, width = 150.dp)
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBox(height = 12.dp, width = 100.dp)
        }

        ShimmerBox(height = 14.dp, width = 40.dp)
    }
}
