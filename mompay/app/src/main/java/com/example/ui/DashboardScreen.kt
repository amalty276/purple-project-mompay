package com.example.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.FairSplitUiState
import com.example.components.ExpenseCard
import com.example.components.FinancialMetricTile
import com.example.components.PrimaryButton
import com.example.components.SecondaryOutlinedButton
import com.example.components.StatusPillBadge
import com.example.logic.ExpenseCalculator
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.FintechBlue
import com.example.ui.theme.IndigoDark
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MomPayCardSurface
import com.example.ui.theme.MomPayCoral
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    uiState: FairSplitUiState,
    onNavigateToSettlement: () -> Unit,
    onNavigateToMomPay: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    modifier: Modifier = Modifier
) {
    val membersById = uiState.members.associateBy { it.id }
    val currentUserName = membersById[uiState.currentUserId]?.name ?: "Abhay"

    val netLabel = when {
        uiState.userNetPaise > 0L -> "You receive"
        uiState.userNetPaise < 0L -> "You owe"
        else -> "Settled"
    }
    val netColor = when {
        uiState.userNetPaise > 0L -> PositiveGreen
        uiState.userNetPaise < 0L -> CoralRed
        else -> PendingAmber
    }
    val netDisplay = ExpenseCalculator.formatRupees(
        if (uiState.userNetPaise < 0L) -uiState.userNetPaise else uiState.userNetPaise
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .testTag("dashboard_lazy_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(listOf(ElectricPurple, FintechBlue))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Wallet,
                                contentDescription = "FairSplit logo",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "FAIRSPLIT",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Your squad. Your expenses. Sorted.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SoftLavender
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Hey $currentUserName, ready to settle things?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("dashboard_greeting")
                    )
                }
            }

            // Prominent Group Card: GOA TRIP – SQUAD
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goa_trip_group_card"),
                    shape = RoundedCornerShape(24.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.45f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        CardSurfaceElevated,
                                        IndigoDark
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "GOA TRIP – SQUAD",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SoftLavender,
                                    fontWeight = FontWeight.Bold
                                )
                                StatusPillBadge(
                                    label = "ACTIVE TRIP",
                                    accentColor = FintechBlue
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Total group expenditure",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = ExpenseCalculator.formatRupees(uiState.totalExpenditurePaise),
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = TextPrimary,
                                    modifier = Modifier.testTag("dashboard_total_spend")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Group,
                                        contentDescription = null,
                                        tint = SoftLavender,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${uiState.members.size} friends",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ReceiptLong,
                                        contentDescription = null,
                                        tint = FintechBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${uiState.expenses.size} recorded expenses",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Financial Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FinancialMetricTile(
                        label = "You paid",
                        amountText = ExpenseCalculator.formatRupees(uiState.userPaidPaise),
                        accentColor = FintechBlue,
                        modifier = Modifier.weight(1f),
                        testTag = "metric_you_paid"
                    )
                    FinancialMetricTile(
                        label = "Your share",
                        amountText = ExpenseCalculator.formatRupees(uiState.userSharePaise),
                        accentColor = TextPrimary,
                        modifier = Modifier.weight(1f),
                        testTag = "metric_your_share"
                    )
                    FinancialMetricTile(
                        label = netLabel,
                        amountText = netDisplay,
                        accentColor = netColor,
                        modifier = Modifier.weight(1f),
                        testTag = "metric_you_receive"
                    )
                }
            }

            // Prominent Action: View Smart Settlement
            item {
                PrimaryButton(
                    text = "View Smart Settlement",
                    icon = Icons.Rounded.AutoGraph,
                    onClick = onNavigateToSettlement,
                    testTag = "dashboard_view_settlement_button"
                )
            }

            // MomPay Promo Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_mompay_promo_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = MomPayCardSurface,
                    border = BorderStroke(1.dp, MomPayCoral.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MomPayCoral.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = "MomPay alert",
                                    tint = MomPayCoral,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Someone forgot to pay?",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Escalate politely via Aunty.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SoftLavender
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        SecondaryOutlinedButton(
                            text = "Check MomPay",
                            onClick = onNavigateToMomPay,
                            accentColor = MomPayCoral,
                            contentColor = MomPayCoral,
                            testTag = "dashboard_check_mompay_button"
                        )
                    }
                }
            }

            // Recent Activity Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    TextButton(
                        onClick = onNavigateToExpenses,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("dashboard_see_all_expenses")
                    ) {
                        Text(
                            text = "Manage",
                            color = SoftLavender
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = SoftLavender,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (uiState.expenses.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = CardSurface,
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No expenses recorded yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Head to Expenses to log your first Goa Trip spend.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(uiState.expenses, key = { it.id }) { expense ->
                    ExpenseCard(
                        expense = expense,
                        membersById = membersById
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}
