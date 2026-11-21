package com.example.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.FairSplitUiState
import com.example.components.ExpenseCard
import com.example.components.PrimaryButton
import com.example.components.SecondaryOutlinedButton
import com.example.logic.ExpenseCalculator
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.FintechBlue
import com.example.ui.theme.IndigoDark
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpensesScreen(
    uiState: FairSplitUiState,
    onAddExpense: (title: String, amountText: String, paidById: String, participantIds: List<String>) -> String?,
    onDeleteExpense: (String) -> Unit,
    onResetDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val membersById = uiState.members.associateBy { it.id }
    var showAddSheet by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = 92.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${uiState.groupName} • ${ExpenseCalculator.formatRupees(uiState.totalExpenditurePaise)} total",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    SecondaryOutlinedButton(
                        text = "Add",
                        icon = Icons.Rounded.Add,
                        onClick = { showAddSheet = true },
                        accentColor = ElectricPurple,
                        contentColor = SoftLavender,
                        testTag = "expenses_header_add_button"
                    )
                }
            }

            // Squad Members & Live Net Balances
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Squad Members & Live Balances",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.settlementResult.balances, key = { it.member.id }) { balance ->
                            val net = balance.netBalancePaise
                            val color = when {
                                net > 0L -> PositiveGreen
                                net < 0L -> CoralRed
                                else -> PendingAmber
                            }
                            Surface(
                                modifier = Modifier.testTag("member_summary_chip_${balance.member.id}"),
                                shape = RoundedCornerShape(16.dp),
                                color = CardSurface,
                                border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = balance.member.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = ExpenseCalculator.formatRupees(net, includePlusSign = true),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = color
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Recorded Expenses (${uiState.expenses.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }

            if (uiState.expenses.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_expenses_card"),
                        shape = RoundedCornerShape(22.dp),
                        color = CardSurface,
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ReceiptLong,
                                contentDescription = null,
                                tint = SoftLavender,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No expenses in this squad yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Tap Add Expense below to log a bill or restore the Goa Trip demo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SecondaryOutlinedButton(
                                    text = "Reset Demo",
                                    onClick = onResetDemo,
                                    testTag = "empty_reset_demo_button"
                                )
                            }
                        }
                    }
                }
            } else {
                items(uiState.expenses, key = { it.id }) { expense ->
                    ExpenseCard(
                        expense = expense,
                        membersById = membersById,
                        onDelete = { onDeleteExpense(expense.id) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddSheet = true },
            containerColor = ElectricPurple,
            contentColor = TextPrimary,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_expense_fab")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add expense")
                Text(
                    text = "Add Expense",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    if (showAddSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var title by remember { mutableStateOf("") }
        var amountText by remember { mutableStateOf("") }
        var selectedPayerId by remember { mutableStateOf(uiState.currentUserId) }
        val selectedParticipants = remember {
            mutableStateListOf<String>().apply {
                addAll(uiState.members.map { it.id })
            }
        }
        var validationError by remember { mutableStateOf<String?>(null) }

        val parsedPaise = ExpenseCalculator.parseRupeesToPaise(amountText)
        val previewPerPerson = if (parsedPaise != null && parsedPaise > 0L && selectedParticipants.isNotEmpty()) {
            ExpenseCalculator.formatRupees(parsedPaise / selectedParticipants.size)
        } else {
            null
        }

        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState,
            containerColor = IndigoDark,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Group Expense",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationError = null
                    },
                    label = { Text("Expense title") },
                    placeholder = { Text("e.g., Ferry tickets, Beach shack") },
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
                        .testTag("expense_title_input")
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        validationError = null
                    },
                    label = { Text("Amount in rupees (₹)") },
                    placeholder = { Text("e.g., 400") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                        .testTag("expense_amount_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paid by",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.members.forEach { member ->
                            val selected = selectedPayerId == member.id
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedPayerId = member.id
                                    validationError = null
                                },
                                label = { Text(member.name) },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FintechBlue.copy(alpha = 0.25f),
                                    selectedLabelColor = TextPrimary,
                                    containerColor = CardSurface,
                                    labelColor = TextSecondary
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("payer_chip_${member.id}")
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Participants (${selectedParticipants.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        TextButton(
                            onClick = {
                                selectedParticipants.clear()
                                selectedParticipants.addAll(uiState.members.map { it.id })
                                validationError = null
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("split_equally_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Balance,
                                contentDescription = null,
                                tint = SoftLavender,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Split Equally", color = SoftLavender)
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.members.forEach { member ->
                            val isSelected = member.id in selectedParticipants
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        selectedParticipants.remove(member.id)
                                    } else {
                                        selectedParticipants.add(member.id)
                                    }
                                    validationError = null
                                },
                                label = { Text(member.name) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricPurple.copy(alpha = 0.28f),
                                    selectedLabelColor = TextPrimary,
                                    containerColor = CardSurface,
                                    labelColor = TextSecondary
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("participant_chip_${member.id}")
                            )
                        }
                    }

                    if (previewPerPerson != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CardSurfaceElevated
                        ) {
                            Text(
                                text = "Equal split: $previewPerPerson per selected person",
                                style = MaterialTheme.typography.bodySmall,
                                color = PositiveGreen,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                if (validationError != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CoralRed.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, CoralRed.copy(alpha = 0.45f)),
                        modifier = Modifier.testTag("expense_validation_error")
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
                                text = validationError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = CoralRed
                            )
                        }
                    }
                }

                PrimaryButton(
                    text = "Save Expense",
                    onClick = {
                        val error = onAddExpense(
                            title,
                            amountText,
                            selectedPayerId,
                            selectedParticipants.toList()
                        )
                        if (error == null) {
                            showAddSheet = false
                        } else {
                            validationError = error
                        }
                    },
                    testTag = "save_expense_button"
                )
            }
        }
    }
}
