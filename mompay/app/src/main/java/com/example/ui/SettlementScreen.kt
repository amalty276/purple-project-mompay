package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.FairSplitUiState
import com.example.components.BalanceCard
import com.example.components.PrimaryButton
import com.example.components.SecondaryOutlinedButton
import com.example.components.SettlementCard
import com.example.components.StatusPillBadge
import com.example.logic.ExpenseCalculator
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.FintechBlue
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettlementScreen(
    uiState: FairSplitUiState,
    onCalculateSettlement: () -> Unit,
    onResetDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val result = uiState.settlementResult
    val txCount = result.transactions.size
    val rawCount = result.rawRepaymentCount
    val savedTransfers = (rawCount - txCount).coerceAtLeast(0)
    val resolutionProgress = if (result.remainingUnsettledPaise == 0L) 1f else 0.5f

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .testTag("settlement_lazy_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Smart Settlement",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Less transfers. Less confusion.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftLavender
                    )
                }
            }

            // Before vs After Comparison Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("before_after_comparison_card"),
                    shape = RoundedCornerShape(22.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(listOf(CardSurfaceElevated, CardSurface))
                            )
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Minimum Cash Flow Engine",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            StatusPillBadge(
                                label = if (savedTransfers > 0) "-$savedTransfers STEPS" else "OPTIMIZED",
                                accentColor = PositiveGreen
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = CoralRed.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, CoralRed.copy(alpha = 0.35f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "BEFORE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CoralRed
                                    )
                                    Text(
                                        text = "$rawCount repayments",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Multiple confusing repayments.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = PositiveGreen.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, PositiveGreen.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "AFTER",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PositiveGreen
                                    )
                                    Text(
                                        text = "$txCount optimized transfers",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = TextPrimary,
                                        modifier = Modifier.testTag("after_transfer_count")
                                    )
                                    Text(
                                        text = "Exact balance zeroed out.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        // Settlement Progress
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Settlement progress",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Remaining unsettled: ${ExpenseCalculator.formatRupees(result.remainingUnsettledPaise)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PositiveGreen,
                                    modifier = Modifier.testTag("remaining_unsettled_text")
                                )
                            }
                            LinearProgressIndicator(
                                progress = { resolutionProgress },
                                color = PositiveGreen,
                                trackColor = BorderSubtle,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                        }
                    }
                }
            }

            // Action Buttons: Calculate Settlement + Reset Demo
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PrimaryButton(
                        text = "Calculate Settlement",
                        icon = Icons.Rounded.AutoAwesome,
                        onClick = onCalculateSettlement,
                        modifier = Modifier.weight(1.4f),
                        testTag = "calculate_settlement_button"
                    )
                    SecondaryOutlinedButton(
                        text = "Reset Demo",
                        icon = Icons.Rounded.Refresh,
                        onClick = onResetDemo,
                        modifier = Modifier.weight(1f),
                        testTag = "reset_demo_button"
                    )
                }
            }

            item {
                AnimatedVisibility(visible = uiState.settlementStatusBanner != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PositiveGreen.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, PositiveGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.testTag("settlement_status_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = PositiveGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = uiState.settlementStatusBanner ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = PositiveGreen
                            )
                        }
                    }
                }
            }

            // Optimized Transfers Section
            item {
                Text(
                    text = "Optimized Transfers ($txCount)",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }

            if (result.transactions.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("no_settlements_needed_card"),
                        shape = RoundedCornerShape(18.dp),
                        color = CardSurface,
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "No transfers required",
                                style = MaterialTheme.typography.titleMedium,
                                color = PositiveGreen
                            )
                            Text(
                                text = if (uiState.expenses.isEmpty()) {
                                    "Add expenses or tap Reset Demo to calculate squad settlements."
                                } else {
                                    "All squad balances are completely even."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(result.transactions, key = { _, tx -> tx.id }) { idx, tx ->
                    SettlementCard(
                        stepNumber = idx + 1,
                        transaction = tx
                    )
                }
            }

            // Member Balances Breakdown
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Member Balances",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }

            items(result.balances, key = { it.member.id }) { balance ->
                BalanceCard(
                    balance = balance,
                    isCurrentUser = balance.member.id == uiState.currentUserId
                )
            }

            if (uiState.settlementHistory.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Recent Settlement Calculations",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                }
                items(uiState.settlementHistory, key = { it.id }) { record ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = CardSurface,
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = record.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                            StatusPillBadge(label = "SAVED", accentColor = FintechBlue)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}
