package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.repository.NavTab
import com.example.ui.DashboardScreen
import com.example.ui.ExpensesScreen
import com.example.ui.IncomingCallScreen
import com.example.ui.MomPayScreen
import com.example.ui.MomReceiverScreen
import com.example.ui.SettlementScreen
import com.example.ui.theme.CardSurface
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.FairSplitTheme
import com.example.ui.theme.IndigoDark
import com.example.ui.theme.MomPayBgTop
import com.example.ui.theme.MomPayCoral
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            FairSplitTheme {
                FairSplitApp()
            }
        }
    }
}

private data class BottomNavItem(
    val tab: NavTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val bottomNavItems = listOf(
    BottomNavItem(
        tab = NavTab.HOME,
        label = "Home",
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_home"
    ),
    BottomNavItem(
        tab = NavTab.EXPENSES,
        label = "Expenses",
        selectedIcon = Icons.Rounded.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong,
        testTag = "nav_expenses"
    ),
    BottomNavItem(
        tab = NavTab.SETTLEMENT,
        label = "Settlement",
        selectedIcon = Icons.Rounded.AutoGraph,
        unselectedIcon = Icons.Outlined.AutoGraph,
        testTag = "nav_settlement"
    ),
    BottomNavItem(
        tab = NavTab.MOMPAY,
        label = "MomPay",
        selectedIcon = Icons.Rounded.NotificationsActive,
        unselectedIcon = Icons.Outlined.NotificationsActive,
        testTag = "nav_mompay"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FairSplitApp(
    viewModel: FairSplitViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState.overlayScreen) {
        OverlayScreen.CALL_FLOW -> {
            val invitation = uiState.validatedInvitation
            if (invitation != null) {
                IncomingCallScreen(
                    uiState = uiState,
                    invitation = invitation,
                    onAnswer = { viewModel.answerSimulatedCall() },
                    onDecline = { viewModel.declineSimulatedCall() },
                    onToggleMute = { viewModel.toggleMute() },
                    onReplay = { viewModel.replayAnnouncement() },
                    onEndCall = { viewModel.endSimulatedCall() },
                    onCloseSummary = { viewModel.closeOverlay() },
                    onBackgroundPause = { viewModel.pauseIncomingAlertOnBackground() }
                )
            } else {
                LaunchedEffect(Unit) {
                    viewModel.closeOverlay()
                }
            }
        }

        OverlayScreen.MOM_RECEIVER -> {
            Scaffold(
                containerColor = NavyDeep,
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                MomReceiverScreen(
                    uiState = uiState,
                    onInputChange = { viewModel.updateReceiverInput(it) },
                    onLoadExample = { viewModel.loadExampleInvitation() },
                    onValidate = { viewModel.validateInvitation() },
                    onAcceptCall = { viewModel.startIncomingCallFromValidatedInvitation() },
                    onDeclineInvitation = { viewModel.declineInvitationPreview() },
                    onBack = { viewModel.closeOverlay() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        OverlayScreen.NONE -> {
            val isMomPay = uiState.activeTab == NavTab.MOMPAY
            val topBarBg = if (isMomPay) MomPayBgTop else NavyDeep

            Scaffold(
                containerColor = NavyDeep,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (isMomPay) "MomPay Escalation" else "FairSplit",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (isMomPay) MomPayCoral else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        actions = {
                            OutlinedButton(
                                onClick = { viewModel.openReceiverMode() },
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.65f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = CardSurface.copy(alpha = 0.85f),
                                    contentColor = SoftLavender
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("topbar_receive_invite_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CallReceived,
                                    contentDescription = "Receive Invitation",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Receive Invite",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = topBarBg
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = IndigoDark,
                        contentColor = TextPrimary,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = uiState.activeTab == item.tab
                            val accent = if (item.tab == NavTab.MOMPAY) MomPayCoral else ElectricPurple
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TextPrimary,
                                    selectedTextColor = if (item.tab == NavTab.MOMPAY) MomPayCoral else SoftLavender,
                                    indicatorColor = accent.copy(alpha = 0.25f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NavyDeep)
                        .padding(innerPadding)
                ) {
                    Crossfade(
                        targetState = uiState.activeTab,
                        label = "tab_transition"
                    ) { tab ->
                        when (tab) {
                            NavTab.HOME -> DashboardScreen(
                                uiState = uiState,
                                onNavigateToSettlement = { viewModel.selectTab(NavTab.SETTLEMENT) },
                                onNavigateToMomPay = { viewModel.selectTab(NavTab.MOMPAY) },
                                onNavigateToExpenses = { viewModel.selectTab(NavTab.EXPENSES) }
                            )

                            NavTab.EXPENSES -> ExpensesScreen(
                                uiState = uiState,
                                onAddExpense = { title, amountText, paidById, participantIds ->
                                    viewModel.addExpense(title, amountText, paidById, participantIds)
                                },
                                onDeleteExpense = { expenseId -> viewModel.deleteExpense(expenseId) },
                                onResetDemo = { viewModel.resetDemo() }
                            )

                            NavTab.SETTLEMENT -> SettlementScreen(
                                uiState = uiState,
                                onCalculateSettlement = { viewModel.calculateSettlement() },
                                onResetDemo = { viewModel.resetDemo() }
                            )

                            NavTab.MOMPAY -> MomPayScreen(
                                uiState = uiState,
                                onSelectDebtor = { viewModel.selectDebtor(it) },
                                onToggleDemoModeOverdue = { viewModel.toggleDemoModeOverdue(it) },
                                onUpdateMotherName = { viewModel.updateMotherName(it) },
                                onUpdateMotherPhone = { viewModel.updateMotherPhone(it) },
                                onSaveMotherContact = { viewModel.saveMotherContact() },
                                onPrepareSmsPayload = { requirePhone ->
                                    viewModel.prepareSmsForActiveDebtor(requirePhone)
                                },
                                onConfirmSmsSent = { viewModel.confirmSmsSentBySender() },
                                onReportSmsError = { viewModel.reportSmsLaunchError(it) },
                                onOpenReceiverMode = { viewModel.openReceiverMode() }
                            )
                        }
                    }
                }
            }
        }
    }
}
