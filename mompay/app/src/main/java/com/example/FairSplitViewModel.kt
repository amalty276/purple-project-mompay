package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Expense
import com.example.data.GroupMember
import com.example.data.MemberBalance
import com.example.data.MomPayInvitation
import com.example.data.SettlementHistoryRecord
import com.example.data.SettlementResult
import com.example.logic.ExpenseCalculator
import com.example.logic.InvitationDecodeResult
import com.example.logic.MomPayInvitationCodec
import com.example.logic.SettlementCalculator
import com.example.repository.FairSplitRepository
import com.example.repository.NavTab
import com.example.voice.MomPayVoiceAssistant
import com.example.voice.VoiceAssistantState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.UUID

enum class OverlayScreen {
    NONE,
    MOM_RECEIVER,
    CALL_FLOW
}

enum class CallPhase {
    INCOMING,
    ACTIVE,
    SUMMARY
}

data class FairSplitUiState(
    val groupName: String = FairSplitRepository.DEMO_GROUP_NAME,
    val currentUserId: String = FairSplitRepository.CURRENT_USER_ID,
    val activeTab: NavTab = NavTab.HOME,
    val overlayScreen: OverlayScreen = OverlayScreen.NONE,
    val members: List<GroupMember> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val totalExpenditurePaise: Long = 0L,
    val userPaidPaise: Long = 0L,
    val userSharePaise: Long = 0L,
    val userNetPaise: Long = 0L,
    val settlementResult: SettlementResult = SettlementResult(emptyList(), emptyList(), 0, 0L),
    val settlementHistory: List<SettlementHistoryRecord> = emptyList(),
    val settlementStatusBanner: String? = null,
    val selectedDebtorId: String? = null,
    val demoModeOverdue: Boolean = true,
    val graceAnchorMillis: Long = System.currentTimeMillis(),
    val motherName: String = FairSplitRepository.DEFAULT_MOTHER_NAME,
    val motherPhone: String = "",
    val motherContactNotice: String? = null,
    val motherContactError: String? = null,
    val lastPreparedInvitation: MomPayInvitation? = null,
    val lastPreparedSmsText: String? = null,
    val smsPreparedNotice: Boolean = false,
    val smsConfirmedBySender: Boolean = false,
    val smsLaunchError: String? = null,
    val receiverInputText: String = "",
    val receiverError: String? = null,
    val validatedInvitation: MomPayInvitation? = null,
    val callPhase: CallPhase = CallPhase.INCOMING,
    val voiceState: VoiceAssistantState = VoiceAssistantState()
) {
    val debtors: List<MemberBalance>
        get() = settlementResult.balances
            .filter { it.netBalancePaise < 0L }
            .sortedBy { it.netBalancePaise }

    val activeDebtor: MemberBalance?
        get() = debtors.firstOrNull { it.member.id == selectedDebtorId } ?: debtors.firstOrNull()

    val isGraceExpired: Boolean
        get() = demoModeOverdue || System.currentTimeMillis() >= (graceAnchorMillis + GRACE_PERIOD_MILLIS)

    companion object {
        const val GRACE_PERIOD_MILLIS = 24L * 60L * 60L * 1000L
    }
}

data class PreparedSmsPayload(
    val normalizedPhone: String,
    val invitation: MomPayInvitation,
    val smsBody: String
)

class FairSplitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FairSplitRepository(application)
    private val voiceAssistant = MomPayVoiceAssistant(application)

    private val _internalState = MutableStateFlow(loadInitialUiState())

    val uiState: StateFlow<FairSplitUiState> = combine(
        _internalState,
        voiceAssistant.state
    ) { base, voice ->
        base.copy(voiceState = voice)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = _internalState.value.copy(voiceState = voiceAssistant.state.value)
    )

    private fun loadInitialUiState(): FairSplitUiState {
        val persisted = repository.loadState()
        return buildComputedState(
            base = FairSplitUiState(
                groupName = persisted.groupName,
                currentUserId = persisted.currentUserId,
                activeTab = persisted.lastTab,
                members = persisted.members,
                expenses = persisted.expenses,
                demoModeOverdue = persisted.demoModeOverdue,
                graceAnchorMillis = persisted.graceAnchorMillis,
                settlementHistory = persisted.settlementHistory,
                motherName = persisted.motherName,
                motherPhone = persisted.motherPhone,
                smsConfirmedBySender = persisted.smsInvitationConfirmedBySender
            )
        )
    }

    private fun buildComputedState(base: FairSplitUiState): FairSplitUiState {
        val totalPaise = ExpenseCalculator.calculateTotalExpenditurePaise(base.expenses)
        val settlement = SettlementCalculator.calculate(base.members, base.expenses)
        val userBalance = settlement.balances.firstOrNull { it.member.id == base.currentUserId }
        val preferredDemoDebtorId = settlement.balances
            .firstOrNull { it.member.id == "aman" && it.netBalancePaise < 0L }
            ?.member?.id
        val topDebtorId = preferredDemoDebtorId ?: settlement.balances
            .filter { it.netBalancePaise < 0L }
            .minByOrNull { it.netBalancePaise }
            ?.member?.id
        val currentSelectionValid = settlement.balances.any {
            it.member.id == base.selectedDebtorId && it.netBalancePaise < 0L
        }
        val resolvedDebtorId = if (currentSelectionValid) base.selectedDebtorId else topDebtorId

        return base.copy(
            totalExpenditurePaise = totalPaise,
            userPaidPaise = userBalance?.totalPaidPaise ?: 0L,
            userSharePaise = userBalance?.totalSharePaise ?: 0L,
            userNetPaise = userBalance?.netBalancePaise ?: 0L,
            settlementResult = settlement,
            selectedDebtorId = resolvedDebtorId
        )
    }

    fun selectTab(tab: NavTab) {
        repository.saveLastTab(tab)
        _internalState.update { it.copy(activeTab = tab, settlementStatusBanner = null) }
    }

    fun openReceiverMode() {
        voiceAssistant.stopAllPlayback()
        _internalState.update {
            it.copy(
                overlayScreen = OverlayScreen.MOM_RECEIVER,
                receiverError = null
            )
        }
    }

    fun closeOverlay() {
        voiceAssistant.stopAllPlayback()
        _internalState.update {
            it.copy(
                overlayScreen = OverlayScreen.NONE,
                callPhase = CallPhase.INCOMING
            )
        }
    }

    fun addExpense(
        title: String,
        amountRupeesText: String,
        paidById: String,
        participantIds: List<String>
    ): String? {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            return "Expense title cannot be empty."
        }
        val amountPaise = ExpenseCalculator.parseRupeesToPaise(amountRupeesText)
        if (amountPaise == null || amountPaise <= 0L) {
            return "Enter a valid positive amount in rupees."
        }
        val validMemberIds = _internalState.value.members.map { it.id }.toSet()
        if (paidById !in validMemberIds) {
            return "Select a valid group member who paid."
        }
        val validParticipants = participantIds.filter { it in validMemberIds }.distinct()
        if (validParticipants.isEmpty()) {
            return "Select at least one participant to split the expense."
        }

        val newExpense = Expense(
            id = "exp_${UUID.randomUUID().toString().take(8)}",
            title = trimmedTitle,
            amountPaise = amountPaise,
            paidById = paidById,
            participantIds = validParticipants,
            createdAtMillis = System.currentTimeMillis()
        )

        val updatedExpenses = listOf(newExpense) + _internalState.value.expenses
        repository.saveExpenses(updatedExpenses)
        _internalState.update { current ->
            buildComputedState(
                current.copy(
                    expenses = updatedExpenses,
                    settlementStatusBanner = null,
                    smsPreparedNotice = false
                )
            )
        }
        return null
    }

    fun deleteExpense(expenseId: String) {
        val updatedExpenses = _internalState.value.expenses.filterNot { it.id == expenseId }
        repository.saveExpenses(updatedExpenses)
        _internalState.update { current ->
            buildComputedState(
                current.copy(
                    expenses = updatedExpenses,
                    settlementStatusBanner = null,
                    smsPreparedNotice = false
                )
            )
        }
    }

    fun calculateSettlement() {
        val current = _internalState.value
        val result = SettlementCalculator.calculate(current.members, current.expenses)
        val totalMoved = result.transactions.sumOf { it.amountPaise }
        val newRecord = SettlementHistoryRecord(
            id = "settle_${UUID.randomUUID().toString().take(6)}",
            timestampMillis = System.currentTimeMillis(),
            totalSettledPaise = totalMoved,
            transferCount = result.transactions.size,
            summary = if (result.transactions.isEmpty()) {
                "All squad balances settled"
            } else {
                "${result.transactions.size} optimized transfers (${ExpenseCalculator.formatRupees(totalMoved)})"
            }
        )
        val updatedHistory = (listOf(newRecord) + current.settlementHistory).take(5)
        repository.saveSettlementHistory(updatedHistory)
        _internalState.update {
            it.copy(
                settlementResult = result,
                settlementHistory = updatedHistory,
                settlementStatusBanner = if (result.transactions.isEmpty()) {
                    "Everyone is already settled at ₹0!"
                } else {
                    "Minimum Cash Flow calculated: ${result.transactions.size} transfers, ₹0 remaining."
                }
            )
        }
    }

    fun resetDemo() {
        voiceAssistant.stopAllPlayback()
        val demo = repository.resetDemo()
        _internalState.update { current ->
            buildComputedState(
                current.copy(
                    members = demo.members,
                    expenses = demo.expenses,
                    demoModeOverdue = demo.demoModeOverdue,
                    graceAnchorMillis = demo.graceAnchorMillis,
                    settlementHistory = demo.settlementHistory,
                    settlementStatusBanner = "Goa Trip demo data restored.",
                    lastPreparedInvitation = null,
                    lastPreparedSmsText = null,
                    smsPreparedNotice = false,
                    smsConfirmedBySender = demo.smsInvitationConfirmedBySender,
                    smsLaunchError = null
                )
            )
        }
    }

    fun selectDebtor(memberId: String) {
        _internalState.update {
            it.copy(
                selectedDebtorId = memberId,
                smsPreparedNotice = false,
                smsConfirmedBySender = false,
                smsLaunchError = null
            )
        }
        repository.saveSmsConfirmedBySender(false)
    }

    fun toggleDemoModeOverdue(enabled: Boolean) {
        val now = System.currentTimeMillis()
        repository.saveDemoModeOverdue(enabled)
        if (!enabled) {
            repository.saveGraceAnchor(now)
        }
        _internalState.update {
            it.copy(
                demoModeOverdue = enabled,
                graceAnchorMillis = if (!enabled) now else it.graceAnchorMillis
            )
        }
    }

    fun updateMotherName(name: String) {
        _internalState.update {
            it.copy(
                motherName = name,
                motherContactNotice = null,
                motherContactError = null
            )
        }
    }

    fun updateMotherPhone(phone: String) {
        _internalState.update {
            it.copy(
                motherPhone = phone,
                motherContactNotice = null,
                motherContactError = null,
                smsLaunchError = null
            )
        }
    }

    fun saveMotherContact(): Boolean {
        val state = _internalState.value
        val trimmedName = state.motherName.trim()
        if (trimmedName.isEmpty()) {
            _internalState.update {
                it.copy(motherContactError = "Enter Mother's name before saving.", motherContactNotice = null)
            }
            return false
        }
        val normalized = MomPayInvitationCodec.normalizeIndianMobileNumber(state.motherPhone)
        if (normalized == null) {
            _internalState.update {
                it.copy(
                    motherContactError = "Enter a valid 10-digit Indian mobile number (starts with 6–9).",
                    motherContactNotice = null
                )
            }
            return false
        }
        repository.saveMotherContact(trimmedName, normalized)
        _internalState.update {
            it.copy(
                motherName = trimmedName,
                motherPhone = normalized,
                motherContactError = null,
                motherContactNotice = "Saved $trimmedName ($normalized) locally."
            )
        }
        return true
    }

    fun prepareSmsForActiveDebtor(requirePhoneNumber: Boolean = true): PreparedSmsPayload? {
        val state = _internalState.value
        val debtor = state.activeDebtor
        if (debtor == null || debtor.netBalancePaise >= 0L) {
            _internalState.update {
                it.copy(smsLaunchError = "No unpaid debtor with an outstanding balance.")
            }
            return null
        }

        val trimmedMotherName = state.motherName.trim().ifEmpty { FairSplitRepository.DEFAULT_MOTHER_NAME }
        val normalizedPhone = MomPayInvitationCodec.normalizeIndianMobileNumber(state.motherPhone)
        if (requirePhoneNumber && normalizedPhone == null) {
            _internalState.update {
                it.copy(
                    motherContactError = "Enter a valid Indian mobile number (10 digits starting with 6–9).",
                    smsLaunchError = "Enter Mother's Indian mobile number before pinging via SMS."
                )
            }
            return null
        }

        if (normalizedPhone != null) {
            repository.saveMotherContact(trimmedMotherName, normalizedPhone)
        }

        val senderName = state.members
            .firstOrNull { it.id == state.currentUserId }
            ?.name ?: "Abhay"

        val owedPaise = -debtor.netBalancePaise
        val invitation = MomPayInvitationCodec.generateInvitation(
            senderName = senderName,
            debtorName = debtor.member.name,
            amountPaise = owedPaise,
            expenseDescription = "Goa Trip",
            groupName = state.groupName
        )
        val smsBody = MomPayInvitationCodec.formatSmsMessage(
            invitation = invitation,
            motherName = trimmedMotherName
        )

        repository.saveSmsConfirmedBySender(false)
        _internalState.update {
            it.copy(
                motherName = trimmedMotherName,
                motherPhone = normalizedPhone ?: it.motherPhone,
                motherContactError = null,
                lastPreparedInvitation = invitation,
                lastPreparedSmsText = smsBody,
                smsPreparedNotice = true,
                smsConfirmedBySender = false,
                smsLaunchError = null
            )
        }
        return PreparedSmsPayload(
            normalizedPhone = normalizedPhone ?: "",
            invitation = invitation,
            smsBody = smsBody
        )
    }

    fun confirmSmsSentBySender() {
        repository.saveSmsConfirmedBySender(true)
        _internalState.update {
            it.copy(
                smsConfirmedBySender = true,
                smsLaunchError = null
            )
        }
    }

    fun reportSmsLaunchError(message: String) {
        _internalState.update {
            it.copy(
                smsPreparedNotice = false,
                smsLaunchError = message
            )
        }
    }

    fun updateReceiverInput(text: String) {
        _internalState.update {
            it.copy(
                receiverInputText = text,
                receiverError = null
            )
        }
    }

    fun loadExampleInvitation() {
        val example = MomPayInvitationCodec.buildExampleInvitation()
        val formatted = MomPayInvitationCodec.formatSmsMessage(example)
        _internalState.update {
            it.copy(
                receiverInputText = formatted,
                receiverError = null,
                validatedInvitation = example
            )
        }
    }

    fun validateInvitation(): Boolean {
        return when (val result = MomPayInvitationCodec.decodeInvitation(_internalState.value.receiverInputText)) {
            is InvitationDecodeResult.Success -> {
                _internalState.update {
                    it.copy(
                        validatedInvitation = result.invitation,
                        receiverError = null
                    )
                }
                true
            }
            is InvitationDecodeResult.Error -> {
                _internalState.update {
                    it.copy(
                        validatedInvitation = null,
                        receiverError = result.message
                    )
                }
                false
            }
        }
    }

    fun declineInvitationPreview() {
        _internalState.update {
            it.copy(
                validatedInvitation = null,
                receiverInputText = "",
                receiverError = null
            )
        }
    }

    fun startIncomingCallFromValidatedInvitation() {
        val invitation = _internalState.value.validatedInvitation
        if (invitation == null) {
            _internalState.update { it.copy(receiverError = "Validate a MomPay invitation first.") }
            return
        }
        val expiryError = MomPayInvitationCodec.validateInvitation(invitation)
        if (expiryError != null) {
            _internalState.update {
                it.copy(
                    validatedInvitation = null,
                    receiverError = expiryError
                )
            }
            return
        }
        voiceAssistant.startIncomingAlert()
        _internalState.update {
            it.copy(
                overlayScreen = OverlayScreen.CALL_FLOW,
                callPhase = CallPhase.INCOMING
            )
        }
    }

    fun answerSimulatedCall() {
        val invitation = _internalState.value.validatedInvitation ?: return
        _internalState.update { it.copy(callPhase = CallPhase.ACTIVE) }
        voiceAssistant.prepareAndSpeak(invitation)
    }

    fun declineSimulatedCall() {
        voiceAssistant.stopAllPlayback()
        _internalState.update {
            it.copy(
                overlayScreen = OverlayScreen.MOM_RECEIVER,
                callPhase = CallPhase.INCOMING
            )
        }
    }

    fun pauseIncomingAlertOnBackground() {
        if (_internalState.value.overlayScreen == OverlayScreen.CALL_FLOW &&
            _internalState.value.callPhase == CallPhase.INCOMING
        ) {
            voiceAssistant.stopIncomingAlert()
        }
    }

    fun toggleMute() {
        voiceAssistant.toggleMute()
    }

    fun replayAnnouncement() {
        voiceAssistant.replay()
    }

    fun endSimulatedCall() {
        voiceAssistant.stopAllPlayback()
        _internalState.update { it.copy(callPhase = CallPhase.SUMMARY) }
    }

    override fun onCleared() {
        super.onCleared()
        voiceAssistant.shutdown()
    }
}
