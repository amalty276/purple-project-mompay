package com.example.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.FairSplitUiState
import com.example.components.PrimaryButton
import com.example.components.SecondaryOutlinedButton
import com.example.components.StatusPillBadge
import com.example.logic.ExpenseCalculator
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricPurple
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

@Composable
fun MomReceiverScreen(
    uiState: FairSplitUiState,
    onInputChange: (String) -> Unit,
    onLoadExample: () -> Unit,
    onValidate: () -> Boolean,
    onAcceptCall: () -> Unit,
    onDeclineInvitation: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var clipboardError by remember { mutableStateOf<String?>(null) }
    val activeError = clipboardError ?: uiState.receiverError
    val validated = uiState.validatedInvitation

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
                .testTag("receiver_lazy_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("receiver_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "MOM RECEIVER MODE",
                            style = MaterialTheme.typography.titleLarge,
                            color = MomPayCoral,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Have you received a MomPay message?",
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftLavender
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MomPayCardSurface,
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Copy the MomPay SMS from Messages and paste the entire message (or the MOMPAY1: token) below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        OutlinedTextField(
                            value = uiState.receiverInputText,
                            onValueChange = {
                                clipboardError = null
                                onInputChange(it)
                            },
                            label = { Text("Paste SMS or MomPay invitation") },
                            placeholder = { Text("MOMPAY1:...") },
                            minLines = 5,
                            maxLines = 8,
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
                                .testTag("receiver_invitation_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SecondaryOutlinedButton(
                                text = "PASTE FROM CLIPBOARD",
                                icon = Icons.Rounded.ContentPaste,
                                accentColor = SoftLavender.copy(alpha = 0.55f),
                                contentColor = SoftLavender,
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clipText = clipboard?.primaryClip
                                        ?.takeIf { it.itemCount > 0 }
                                        ?.getItemAt(0)
                                        ?.coerceToText(context)
                                        ?.toString()
                                        ?.trim()

                                    if (clipText.isNullOrEmpty()) {
                                        clipboardError = "Clipboard is empty. Copy the MomPay SMS from Messages first."
                                    } else {
                                        clipboardError = null
                                        onInputChange(clipText)
                                    }
                                },
                                modifier = Modifier.weight(1.3f),
                                testTag = "paste_clipboard_button"
                            )

                            SecondaryOutlinedButton(
                                text = "Example",
                                icon = Icons.Rounded.AutoFixHigh,
                                accentColor = MomPayOrange.copy(alpha = 0.55f),
                                contentColor = MomPayOrange,
                                onClick = {
                                    clipboardError = null
                                    onLoadExample()
                                },
                                modifier = Modifier.weight(0.8f),
                                testTag = "load_example_invitation_button"
                            )
                        }

                        PrimaryButton(
                            text = "VALIDATE INVITATION",
                            icon = Icons.Rounded.VerifiedUser,
                            onClick = {
                                clipboardError = null
                                onValidate()
                            },
                            testTag = "validate_invitation_button"
                        )
                    }
                }
            }

            if (activeError != null) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("receiver_error_banner"),
                        shape = RoundedCornerShape(16.dp),
                        color = CoralRed.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, CoralRed.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = "Validation error",
                                tint = CoralRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = activeError,
                                style = MaterialTheme.typography.bodySmall,
                                color = CoralRed
                            )
                        }
                    }
                }
            }

            if (validated != null) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("validated_invitation_card"),
                        shape = RoundedCornerShape(22.dp),
                        color = CardSurface,
                        border = BorderStroke(1.5.dp, PositiveGreen.copy(alpha = 0.55f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = PositiveGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "MomPay invitation received.",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = PositiveGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                StatusPillBadge(
                                    label = validated.invitationId,
                                    accentColor = SoftLavender
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Sender: ${validated.senderName}.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SoftLavender
                                    )
                                    Text(
                                        text = "Debtor: ${validated.debtorName}.",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary,
                                        modifier = Modifier.testTag("validated_debtor_name")
                                    )
                                    Text(
                                        text = "Expense: ${validated.expenseDescription}.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        modifier = Modifier.testTag("validated_expense_desc")
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Outstanding",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = ExpenseCalculator.formatRupees(validated.amountPaise),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MomPayCoral,
                                        modifier = Modifier.testTag("validated_amount_text")
                                    )
                                }
                            }

                            Text(
                                text = "Verify sender and amount above before accepting. Decoded from pasted SMS text.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PendingAmber
                            )

                            PrimaryButton(
                                text = "ACCEPT MOMPAY CALL",
                                icon = Icons.Rounded.Call,
                                gradientColors = listOf(PositiveGreen, ElectricPurple),
                                onClick = onAcceptCall,
                                testTag = "accept_mompay_call_button"
                            )

                            SecondaryOutlinedButton(
                                text = "DECLINE INVITATION",
                                icon = Icons.Rounded.Close,
                                accentColor = CoralRed.copy(alpha = 0.65f),
                                contentColor = CoralRed,
                                onClick = onDeclineInvitation,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "decline_invitation_button"
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}
