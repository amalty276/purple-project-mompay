package com.example.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.FairSplitUiState
import com.example.PreparedSmsPayload
import com.example.components.PrimaryButton
import com.example.components.SecondaryOutlinedButton
import com.example.components.StatusPillBadge
import com.example.logic.ExpenseCalculator
import com.example.logic.MomPayInvitationCodec
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.FintechBlue
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MomPayBgBottom
import com.example.ui.theme.MomPayBgTop
import com.example.ui.theme.MomPayCardSurface
import com.example.ui.theme.MomPayCoral
import com.example.ui.theme.MomPayOrange
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MOMPAY_SCREEN_TAG = "MomPayScreen"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MomPayScreen(
    uiState: FairSplitUiState,
    onSelectDebtor: (String) -> Unit,
    onToggleDemoModeOverdue: (Boolean) -> Unit,
    onUpdateMotherName: (String) -> Unit,
    onUpdateMotherPhone: (String) -> Unit,
    onSaveMotherContact: () -> Boolean,
    onPrepareSmsPayload: (requirePhoneNumber: Boolean) -> PreparedSmsPayload?,
    onConfirmSmsSent: () -> Unit,
    onReportSmsError: (String) -> Unit,
    onOpenReceiverMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDetailsDialog by remember { mutableStateOf(false) }
    var copiedFeedback by remember { mutableStateOf(false) }

    val pulseTransition = rememberInfiniteTransition(label = "mompay_pulse")
    val ctaScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cta_scale"
    )
    val badgeAlpha by pulseTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "warning_alpha"
    )

    val activeDebtor = uiState.activeDebtor
    val isExpired = uiState.isGraceExpired

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MomPayBgTop, MomPayBgBottom))),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .testTag("mompay_lazy_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dramatic MomPay Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "MOMPAY",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MomPayCoral,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = "Escalation alert",
                            tint = MomPayOrange.copy(alpha = badgeAlpha),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "When your friends forget, Aunty remembers.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftLavender
                    )
                }
            }

            // Dynamic Debtor Picker
            if (uiState.debtors.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Select Unpaid Friend",
                            style = MaterialTheme.typography.titleSmall,
                            color = SoftLavender
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.debtors.forEach { debtor ->
                                val selected = activeDebtor?.member?.id == debtor.member.id
                                val owed = ExpenseCalculator.formatRupees(-debtor.netBalancePaise)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        copiedFeedback = false
                                        onSelectDebtor(debtor.member.id)
                                    },
                                    label = { Text("${debtor.member.name} ($owed)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MomPayCoral.copy(alpha = 0.28f),
                                        selectedLabelColor = TextPrimary,
                                        containerColor = MomPayCardSurface,
                                        labelColor = TextSecondary
                                    ),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("debtor_picker_${debtor.member.id}")
                                )
                            }
                        }
                    }
                }
            }

            if (activeDebtor == null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MomPayCardSurface,
                        border = BorderStroke(1.dp, PositiveGreen.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Everyone paid on time!",
                                style = MaterialTheme.typography.titleLarge,
                                color = PositiveGreen
                            )
                            Text(
                                text = "Aunty has nobody to ping right now. Add an expense or reset demo data to test SMS escalation.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                val owedPaise = -activeDebtor.netBalancePaise
                val statusLabel = if (isExpired) "EXPIRED" else "ACTIVE"
                val statusColor = if (isExpired) MomPayCoral else PendingAmber

                // THE UNPAID FRIEND Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("unpaid_friend_card"),
                        shape = RoundedCornerShape(24.dp),
                        color = MomPayCardSurface,
                        border = BorderStroke(1.5.dp, statusColor.copy(alpha = 0.65f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "THE UNPAID FRIEND",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MomPayOrange,
                                    fontWeight = FontWeight.Bold
                                )
                                StatusPillBadge(
                                    label = "Status: $statusLabel",
                                    accentColor = statusColor,
                                    modifier = Modifier.testTag("mompay_status_badge")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Name: ${activeDebtor.member.name}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.testTag("mompay_debtor_name")
                                    )
                                    Text(
                                        text = "Expense: Goa Trip expenses",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Outstanding balance",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = ExpenseCalculator.formatRupees(owedPaise),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MomPayCoral,
                                        modifier = Modifier.testTag("mompay_debtor_amount")
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = CardSurface.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Grace period: 24 hours",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (isExpired) "Grace window elapsed" else formatRemainingGrace(uiState.graceAnchorMillis),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Demo Mode: Mark Overdue",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "Expire 24h timer immediately for hackathon demo",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                        Switch(
                                            checked = uiState.demoModeOverdue,
                                            onCheckedChange = onToggleDemoModeOverdue,
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = TextPrimary,
                                                checkedTrackColor = MomPayCoral
                                            ),
                                            modifier = Modifier.testTag("mompay_demo_mode_switch")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Mother's Contact Card (No Contacts permission — manual entry & local save)
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mother_contact_card"),
                        shape = RoundedCornerShape(20.dp),
                        color = MomPayCardSurface,
                        border = BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Aunty's SMS Contact (Phone B)",
                                style = MaterialTheme.typography.titleSmall,
                                color = SoftLavender,
                                fontWeight = FontWeight.SemiBold
                            )

                            OutlinedTextField(
                                value = uiState.motherName,
                                onValueChange = onUpdateMotherName,
                                label = { Text("Mother's name") },
                                placeholder = { Text("e.g., Aunty or Sunita Aunty") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricPurple,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedLabelColor = SoftLavender,
                                    unfocusedLabelColor = TextSecondary,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("mother_name_input")
                            )

                            OutlinedTextField(
                                value = uiState.motherPhone,
                                onValueChange = onUpdateMotherPhone,
                                label = { Text("Mother's Indian mobile number") },
                                placeholder = { Text("e.g., 9876543210 or +919876543210") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricPurple,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedLabelColor = SoftLavender,
                                    unfocusedLabelColor = TextSecondary,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("mother_phone_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Saved locally only • No Contacts permission",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                SecondaryOutlinedButton(
                                    text = "Save Contact",
                                    icon = Icons.Rounded.Save,
                                    accentColor = ElectricPurple,
                                    contentColor = SoftLavender,
                                    onClick = { onSaveMotherContact() },
                                    testTag = "save_mother_contact_button"
                                )
                            }

                            if (uiState.motherContactError != null) {
                                Text(
                                    text = uiState.motherContactError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CoralRed,
                                    modifier = Modifier.testTag("mother_contact_error")
                                )
                            } else if (uiState.motherContactNotice != null) {
                                Text(
                                    text = uiState.motherContactNotice,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PositiveGreen,
                                    modifier = Modifier.testTag("mother_contact_saved_notice")
                                )
                            }
                        }
                    }
                }

                // Satirical Microcopy Card
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MomPayCardSurface.copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "• \"Beta promised to pay tonight.\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "• \"Tonight became next week.\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SoftLavender
                            )
                            Text(
                                text = "• \"Escalating to the highest authority.\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MomPayOrange,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Prominent PING MOM NOW Button (opens default Android SMS app via ACTION_SENDTO)
                item {
                    PrimaryButton(
                        text = "PING MOM NOW",
                        icon = Icons.Rounded.Sms,
                        gradientColors = listOf(MomPayCoral, MomPayOrange),
                        onClick = {
                            copiedFeedback = false
                            val prepared = onPrepareSmsPayload(true) ?: return@PrimaryButton
                            val launched = launchSmsComposer(
                                context = context,
                                phoneNumber = prepared.normalizedPhone,
                                smsBody = prepared.smsBody
                            )
                            if (!launched) {
                                onReportSmsError(
                                    "No SMS messaging app found on this device. You can use Copy Invitation below."
                                )
                            }
                        },
                        modifier = Modifier.scale(ctaScale),
                        testTag = "ping_mom_now_button"
                    )
                }

                // SMS Error / Prepared / Sender Confirmation States
                if (uiState.smsLaunchError != null) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sms_launch_error_banner"),
                            shape = RoundedCornerShape(14.dp),
                            color = CoralRed.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, CoralRed.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = CoralRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = uiState.smsLaunchError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CoralRed
                                )
                            }
                        }
                    }
                }

                item {
                    AnimatedVisibility(visible = uiState.smsPreparedNotice) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mompay_sms_prepared_banner"),
                            shape = RoundedCornerShape(16.dp),
                            color = PendingAmber.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, PendingAmber.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Sms,
                                        contentDescription = null,
                                        tint = PendingAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (copiedFeedback) {
                                            "MOMPAY SMS PREPARED (COPIED TO CLIPBOARD)"
                                        } else {
                                            "MOMPAY SMS PREPARED"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = PendingAmber,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Review and press Send inside your Messages app, then confirm below.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )

                                if (!uiState.smsConfirmedBySender) {
                                    SecondaryOutlinedButton(
                                        text = "I HAVE SENT THE SMS",
                                        icon = Icons.Rounded.MarkEmailRead,
                                        accentColor = PositiveGreen,
                                        contentColor = PositiveGreen,
                                        onClick = onConfirmSmsSent,
                                        modifier = Modifier.fillMaxWidth(),
                                        testTag = "confirm_sms_sent_button"
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    AnimatedVisibility(visible = uiState.smsConfirmedBySender) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sms_shared_confirmed_banner"),
                            shape = RoundedCornerShape(14.dp),
                            color = PositiveGreen.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, PositiveGreen.copy(alpha = 0.45f))
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
                                    text = "Invitation shared by sender.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PositiveGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Secondary Copy & Details Actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecondaryOutlinedButton(
                            text = "Copy Invitation",
                            icon = Icons.Rounded.ContentCopy,
                            accentColor = SoftLavender.copy(alpha = 0.5f),
                            contentColor = SoftLavender,
                            onClick = {
                                val prepared = onPrepareSmsPayload(false) ?: return@SecondaryOutlinedButton
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(
                                    ClipData.newPlainText("MomPay SMS", prepared.smsBody)
                                )
                                copiedFeedback = true
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "copy_invitation_button"
                        )
                        SecondaryOutlinedButton(
                            text = "Show Details",
                            icon = Icons.Rounded.QrCode2,
                            accentColor = BorderSubtle,
                            contentColor = TextPrimary,
                            onClick = {
                                if (uiState.lastPreparedInvitation == null) {
                                    onPrepareSmsPayload(false)
                                }
                                showDetailsDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "show_invitation_details_button"
                        )
                    }
                }
            }

            // Receive Invitation Banner for Phone B
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mompay_receiver_entry_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.5f))
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(ElectricPurple.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PhoneInTalk,
                                    contentDescription = null,
                                    tint = SoftLavender,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "On Phone B (Receiver)?",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Paste the received SMS to trigger Aunty's call.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        SecondaryOutlinedButton(
                            text = "Receive Invite",
                            onClick = onOpenReceiverMode,
                            accentColor = ElectricPurple,
                            contentColor = SoftLavender,
                            testTag = "mompay_receive_invitation_button"
                        )
                    }
                }
            }

            // Explicit Simulated Call & Offline Notice
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CardSurface.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = FintechBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "PING MOM NOW opens your phone's default SMS app with the recipient and message prefilled. FairSplit does not send SMS silently or track carrier delivery.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showDetailsDialog) {
        val invitation = uiState.lastPreparedInvitation
        val token = invitation?.let { MomPayInvitationCodec.encodeInvitation(it) } ?: ""
        val timeFmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            containerColor = MomPayCardSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("MomPay SMS Payload") },
            text = {
                if (invitation == null) {
                    Text("No invitation prepared yet.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ID: ${invitation.invitationId}", color = TextPrimary)
                        Text("Version: V${invitation.version}")
                        Text("Sender: ${invitation.senderName}")
                        Text("Debtor: ${invitation.debtorName}")
                        Text("Amount: ${ExpenseCalculator.formatRupees(invitation.amountPaise)}")
                        Text("Expense: ${invitation.expenseDescription}")
                        Text("Created: ${timeFmt.format(Date(invitation.createdAt))}")
                        Text("Expires: ${timeFmt.format(Date(invitation.expiresAt))}")
                        Text(
                            text = "Note: Base64URL encodes SMS text for transport; it is not cryptographic authentication.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PendingAmber
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CardSurface
                        ) {
                            Text(
                                text = token,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = SoftLavender,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showDetailsDialog = false },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Close", color = SoftLavender)
                }
            }
        )
    }
}

private fun formatRemainingGrace(anchorMillis: Long): String {
    val expires = anchorMillis + FairSplitUiState.GRACE_PERIOD_MILLIS
    val remainingMillis = (expires - System.currentTimeMillis()).coerceAtLeast(0L)
    val hours = remainingMillis / 3600_000L
    val mins = (remainingMillis % 3600_000L) / 60_000L
    return "${hours}h ${mins}m left"
}

fun buildSmsComposerIntent(
    phoneNumber: String,
    smsBody: String
): Intent {
    val smsUri = Uri.parse("smsto:${Uri.encode(phoneNumber)}")
    return Intent(Intent.ACTION_SENDTO, smsUri).apply {
        putExtra("sms_body", smsBody)
    }
}

private fun launchSmsComposer(
    context: Context,
    phoneNumber: String,
    smsBody: String
): Boolean {
    val intent = buildSmsComposerIntent(phoneNumber, smsBody)
    if (intent.resolveActivity(context.packageManager) == null) {
        Log.w(MOMPAY_SCREEN_TAG, "No activity resolved for ACTION_SENDTO smsto intent")
        return false
    }
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Log.w(MOMPAY_SCREEN_TAG, "Unable to launch SMS composer activity", e)
        false
    }
}
