package com.example.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.MemberBalance
import com.example.logic.ExpenseCalculator
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun FinancialMetricTile(
    label: String,
    amountText: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Surface(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BalanceCard(
    balance: MemberBalance,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    val net = balance.netBalancePaise
    val statusColor = when {
        net > 0L -> PositiveGreen
        net < 0L -> CoralRed
        else -> PendingAmber
    }
    val statusLabel = when {
        net > 0L -> "Receives"
        net < 0L -> "Owes"
        else -> "Settled"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("member_balance_card_${balance.member.id}"),
        shape = RoundedCornerShape(18.dp),
        color = CardSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isCurrentUser) ElectricPurple.copy(alpha = 0.55f) else BorderSubtle
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = balance.member.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isCurrentUser) "${balance.member.name} (You)" else balance.member.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Paid ${ExpenseCalculator.formatRupees(balance.totalPaidPaise)} • Share ${ExpenseCalculator.formatRupees(balance.totalSharePaise)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = ExpenseCalculator.formatRupees(net, includePlusSign = true),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = statusColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusPillBadge(label = statusLabel, accentColor = statusColor)
            }
        }
    }
}
