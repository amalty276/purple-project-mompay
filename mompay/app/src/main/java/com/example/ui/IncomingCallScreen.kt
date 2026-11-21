package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.CallPhase
import com.example.FairSplitUiState
import com.example.R
import com.example.components.PrimaryButton
import com.example.components.StatusPillBadge
import com.example.data.MomPayInvitation
import com.example.logic.ExpenseCalculator
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MomPayCallBgBottom
import com.example.ui.theme.MomPayCallBgTop
import com.example.ui.theme.MomPayCardSurface
import com.example.ui.theme.MomPayCoral
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun IncomingCallScreen(
    uiState: FairSplitUiState,
    invitation: MomPayInvitation,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onToggleMute: () -> Unit,
    onReplay: () -> Unit,
    onEndCall: () -> Unit,
    onCloseSummary: () -> Unit,
    onBackgroundPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        when (uiState.callPhase) {
            CallPhase.INCOMING -> onDecline()
            CallPhase.ACTIVE -> onEndCall()
            CallPhase.SUMMARY -> onCloseSummary()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                onBackgroundPause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val pulse = rememberInfiniteTransition(label = "call_ring_pulse")
    val ringScale by pulse.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_scale"
    )

    val formattedAmount = ExpenseCalculator.formatRupees(invitation.amountPaise)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MomPayCallBgTop, MomPayCallBgBottom)))
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusPillBadge(
                    label = "SIMULATED AI VOICE CALL",
                    accentColor = PendingAmber,
                    modifier = Modifier.testTag("simulated_call_badge")
                )
                Text(
                    text = "MOMPAY AI ASSISTANT",
                    style = MaterialTheme.typography.titleMedium,
                    color = SoftLavender,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Family Financial Reminder Service",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Center Content per Phase
            when (uiState.callPhase) {
                CallPhase.INCOMING -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(170.dp)
                                    .scale(ringScale)
                                    .clip(CircleShape)
                                    .background(ElectricPurple.copy(alpha = 0.16f))
                            )
                            Image(
                                painter = painterResource(id = R.drawable.img_mompay_avatar),
                                contentDescription = "MomPay AI Assistant avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, ElectricPurple, CircleShape)
                            )
                        }

                        Text(
                            text = "Debtor: ${invitation.debtorName}",
                            style = MaterialTheme.typography.displaySmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("incoming_call_debtor")
                        )

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MomPayCardSurface,
                            border = BorderStroke(1.dp, MomPayCoral.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Outstanding: $formattedAmount",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MomPayCoral
                                )
                                Text(
                                    text = "Expense: ${invitation.expenseDescription}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Sender: ${invitation.senderName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SoftLavender
                                )
                            }
                        }

                        Text(
                            text = "This is a simulated in-app call triggered after accepting a MomPay SMS invitation. No actual cellular call is taking place.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FloatingActionButton(
                                onClick = onDecline,
                                containerColor = CoralRed,
                                contentColor = TextPrimary,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(72.dp)
                                    .testTag("decline_call_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CallEnd,
                                    contentDescription = "Decline call",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "DECLINE",
                                style = MaterialTheme.typography.labelLarge,
                                color = CoralRed
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FloatingActionButton(
                                onClick = onAnswer,
                                containerColor = PositiveGreen,
                                contentColor = TextPrimary,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(72.dp)
                                    .testTag("answer_call_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Call,
                                    contentDescription = "Answer call",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "ANSWER",
                                style = MaterialTheme.typography.labelLarge,
                                color = PositiveGreen
                            )
                        }
                    }
                }

                CallPhase.ACTIVE -> {
                    val voiceState = uiState.voiceState
                    val voiceStatusText = when {
                        voiceState.isMuted -> "Muted"
                        voiceState.isInitializing -> "Connecting voice assistant..."
                        voiceState.isSpeaking -> "Speaking..."
                        voiceState.ttsErrorMessage != null -> "Transcript fallback"
                        else -> "Connected • Announcement ready"
                    }
                    val statusColor = when {
                        voiceState.isMuted -> PendingAmber
                        voiceState.isSpeaking -> PositiveGreen
                        voiceState.ttsErrorMessage != null -> CoralRed
                        else -> SoftLavender
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_mompay_avatar),
                            contentDescription = "MomPay AI Assistant avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(92.dp)
                                .clip(CircleShape)
                                .border(2.5.dp, statusColor, CircleShape)
                        )

                        Text(
                            text = "${invitation.debtorName} • $formattedAmount",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "From ${invitation.senderName} • ${invitation.expenseDescription}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        StatusPillBadge(
                            label = voiceStatusText,
                            accentColor = statusColor,
                            modifier = Modifier.testTag("active_call_voice_status")
                        )

                        if (voiceState.ttsErrorMessage != null) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("tts_error_banner"),
                                shape = RoundedCornerShape(14.dp),
                                color = CoralRed.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, CoralRed.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = voiceState.ttsErrorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CoralRed,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("voice_script_card"),
                            shape = RoundedCornerShape(20.dp),
                            color = CardSurface,
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "AUNTY VOICE SCRIPT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SoftLavender
                                )
                                Text(
                                    text = voiceState.activeScript,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FloatingActionButton(
                                onClick = onToggleMute,
                                containerColor = if (voiceState.isMuted) PendingAmber else CardSurface,
                                contentColor = if (voiceState.isMuted) MomPayCallBgBottom else TextPrimary,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(60.dp)
                                    .testTag("mute_call_button")
                            ) {
                                Icon(
                                    imageVector = if (voiceState.isMuted) {
                                        Icons.AutoMirrored.Rounded.VolumeOff
                                    } else {
                                        Icons.AutoMirrored.Rounded.VolumeUp
                                    },
                                    contentDescription = if (voiceState.isMuted) "Unmute" else "Mute"
                                )
                            }
                            Text(
                                text = if (voiceState.isMuted) "UNMUTE" else "MUTE",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FloatingActionButton(
                                onClick = onReplay,
                                containerColor = ElectricPurple,
                                contentColor = TextPrimary,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(60.dp)
                                    .testTag("replay_call_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Replay,
                                    contentDescription = "Replay announcement"
                                )
                            }
                            Text(
                                text = "REPLAY",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FloatingActionButton(
                                onClick = onEndCall,
                                containerColor = CoralRed,
                                contentColor = TextPrimary,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(60.dp)
                                    .testTag("end_call_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CallEnd,
                                    contentDescription = "End Call"
                                )
                            }
                            Text(
                                text = "END CALL",
                                style = MaterialTheme.typography.labelMedium,
                                color = CoralRed
                            )
                        }
                    }
                }

                CallPhase.SUMMARY -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("call_summary_card"),
                        shape = RoundedCornerShape(24.dp),
                        color = MomPayCardSurface,
                        border = BorderStroke(1.dp, PositiveGreen.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = PositiveGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Simulated Call Ended",
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "MomPay reminder for ${invitation.debtorName} ($formattedAmount, requested by ${invitation.senderName}) completed.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "This was a simulated in-app AI voice call. No real cellular call occurred.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SoftLavender,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    PrimaryButton(
                        text = "Done",
                        onClick = onCloseSummary,
                        testTag = "call_summary_done_button"
                    )
                }
            }
        }
    }
}
