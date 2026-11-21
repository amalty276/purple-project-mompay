package com.example.logic

import android.util.Base64
import com.example.data.MomPayInvitation
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID

sealed class InvitationDecodeResult {
    data class Success(val invitation: MomPayInvitation) : InvitationDecodeResult()
    data class Error(val message: String) : InvitationDecodeResult()
}

/**
 * Encodes, extracts, decodes, and validates MomPay SMS invitation tokens.
 *
 * Note: Base64URL is used solely as a compact transport encoding for SMS text,
 * not as cryptographic encryption or sender authentication.
 */
object MomPayInvitationCodec {

    private const val DEFAULT_VALIDITY_MILLIS = 24L * 60L * 60L * 1000L
    private val versionedTokenRegex = Regex("""MOMPAY(\d+):([A-Za-z0-9_\-=]+)""")
    private val anyPrefixRegex = Regex("""MOMPAY(\d+):([^\s]*)""")

    fun generateInvitation(
        senderName: String,
        debtorName: String,
        amountPaise: Long,
        expenseDescription: String,
        groupName: String,
        createdAtMillis: Long = System.currentTimeMillis(),
        validityMillis: Long = DEFAULT_VALIDITY_MILLIS
    ): MomPayInvitation {
        require(amountPaise > 0L) { "Outstanding amount must be positive." }
        require(senderName.isNotBlank()) { "Sender name cannot be blank." }
        require(debtorName.isNotBlank()) { "Debtor name cannot be blank." }
        require(expenseDescription.isNotBlank()) { "Expense description cannot be blank." }
        require(validityMillis > 0L) { "Invitation validity duration must be positive." }

        return MomPayInvitation(
            version = MomPayInvitation.CURRENT_VERSION,
            invitationId = "MP-" + UUID.randomUUID().toString().take(8).uppercase(),
            senderName = senderName.trim(),
            debtorName = debtorName.trim(),
            amountPaise = amountPaise,
            expenseDescription = expenseDescription.trim(),
            groupName = groupName.trim().ifEmpty { "Goa Trip – Squad" },
            createdAt = createdAtMillis,
            expiresAt = createdAtMillis + validityMillis
        )
    }

    fun encodeInvitation(invitation: MomPayInvitation): String {
        val json = JSONObject().apply {
            put("v", invitation.version)
            put("i", invitation.invitationId)
            put("s", invitation.senderName)
            put("d", invitation.debtorName)
            put("a", invitation.amountPaise)
            put("e", invitation.expenseDescription)
            put("g", invitation.groupName)
            put("t", invitation.createdAt)
            put("x", invitation.expiresAt)
        }
        val bytes = json.toString().toByteArray(StandardCharsets.UTF_8)
        val encoded = Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        return "${MomPayInvitation.TOKEN_PREFIX}$encoded"
    }

    fun formatSmsMessage(
        invitation: MomPayInvitation,
        motherName: String = "Aunty"
    ): String {
        val token = encodeInvitation(invitation)
        val formattedAmount = ExpenseCalculator.formatRupees(invitation.amountPaise)
        val greetingName = motherName.trim().ifEmpty { "Aunty" }
        return buildString {
            appendLine("🚨 MOMPAY PAYMENT ALERT 🚨")
            appendLine()
            appendLine("Namaste $greetingName!")
            appendLine()
            appendLine(
                "Your son ${invitation.debtorName} has an outstanding group expense " +
                    "of $formattedAmount from the ${invitation.expenseDescription}."
            )
            appendLine()
            appendLine("His friend ${invitation.senderName} has been waiting patiently.")
            appendLine()
            appendLine("The MomPay assistant has prepared a special voice reminder for you.")
            appendLine()
            appendLine("Open FairSplit → Mom Receiver Mode.")
            appendLine()
            appendLine("Copy the invitation below and paste it into the app:")
            appendLine()
            appendLine(token)
            appendLine()
            append("Thank you, $greetingName!")
        }
    }

    fun extractInvitationFromSms(rawInput: String): String? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return null
        val validMatch = versionedTokenRegex.find(trimmed)
        if (validMatch != null) {
            return validMatch.value
        }
        return anyPrefixRegex.find(trimmed)?.value
    }

    fun decodeInvitation(
        rawInput: String,
        nowMillis: Long = System.currentTimeMillis()
    ): InvitationDecodeResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return InvitationDecodeResult.Error(
                "Invitation is empty. Paste the MomPay SMS message first."
            )
        }

        val token = extractInvitationFromSms(trimmed)
            ?: return InvitationDecodeResult.Error(
                "Missing MomPay token. Paste an SMS containing ${MomPayInvitation.TOKEN_PREFIX}..."
            )

        val match = anyPrefixRegex.matchEntire(token)
            ?: return InvitationDecodeResult.Error(
                "Invalid MomPay token format. Expected ${MomPayInvitation.TOKEN_PREFIX}<payload>."
            )

        val versionTag = match.groupValues[1].toIntOrNull()
        if (versionTag != MomPayInvitation.CURRENT_VERSION) {
            return InvitationDecodeResult.Error(
                "Unsupported invitation version (V${match.groupValues[1]}). Expected ${MomPayInvitation.TOKEN_PREFIX}."
            )
        }

        val payloadPart = match.groupValues[2]
        if (payloadPart.isBlank()) {
            return InvitationDecodeResult.Error("Invitation token payload is empty.")
        }

        val decodedBytes = try {
            Base64.decode(payloadPart, Base64.URL_SAFE or Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            return InvitationDecodeResult.Error(
                "Malformed Base64URL invitation payload. Please copy the entire SMS again."
            )
        }

        if (decodedBytes.isEmpty()) {
            return InvitationDecodeResult.Error("Decoded invitation payload is empty.")
        }

        val jsonString = String(decodedBytes, StandardCharsets.UTF_8)
        val json = try {
            JSONObject(jsonString)
        } catch (_: JSONException) {
            return InvitationDecodeResult.Error("Invalid JSON inside MomPay invitation payload.")
        }

        val version = json.optInt("v", -1)
        if (version != MomPayInvitation.CURRENT_VERSION) {
            return InvitationDecodeResult.Error(
                "Unsupported payload version ($version). Expected version ${MomPayInvitation.CURRENT_VERSION}."
            )
        }

        val invitationId = json.optString("i", "").trim()
        val senderName = json.optString("s", "").trim()
        val debtorName = json.optString("d", "").trim()
        val amountPaise = if (json.has("a")) json.optLong("a", -1L) else -1L
        val expenseDescription = json.optString("e", "").trim()
        val groupName = json.optString("g", "").trim()
        val createdAt = if (json.has("t")) json.optLong("t", -1L) else -1L
        val expiresAt = if (json.has("x")) json.optLong("x", -1L) else -1L

        val invitation = MomPayInvitation(
            version = version,
            invitationId = invitationId,
            senderName = senderName,
            debtorName = debtorName,
            amountPaise = amountPaise,
            expenseDescription = expenseDescription,
            groupName = groupName,
            createdAt = createdAt,
            expiresAt = expiresAt
        )

        val validationError = validateInvitation(invitation, nowMillis)
        return if (validationError != null) {
            InvitationDecodeResult.Error(validationError)
        } else {
            InvitationDecodeResult.Success(invitation)
        }
    }

    fun validateInvitation(
        invitation: MomPayInvitation,
        nowMillis: Long = System.currentTimeMillis()
    ): String? {
        if (invitation.version != MomPayInvitation.CURRENT_VERSION) {
            return "Unsupported invitation version (${invitation.version})."
        }
        if (invitation.invitationId.isBlank()) {
            return "Invitation is missing a unique identifier."
        }
        if (invitation.senderName.isBlank()) {
            return "Invitation is missing the sender name."
        }
        if (invitation.debtorName.isBlank()) {
            return "Invitation is missing the debtor name."
        }
        if (invitation.amountPaise <= 0L) {
            return "Invitation amount must be positive."
        }
        if (invitation.expenseDescription.isBlank()) {
            return "Invitation is missing the expense description."
        }
        if (invitation.groupName.isBlank()) {
            return "Invitation is missing the group name."
        }
        if (invitation.createdAt <= 0L || invitation.expiresAt <= 0L || invitation.expiresAt < invitation.createdAt) {
            return "Invitation contains invalid timestamp values."
        }
        if (nowMillis > invitation.expiresAt) {
            return "This MomPay invitation has expired. Ask the sender to prepare a fresh SMS."
        }
        return null
    }

    fun normalizeIndianMobileNumber(rawInput: String): String? {
        val digitsOnly = rawInput.filter { it.isDigit() }
        val tenDigits = when {
            digitsOnly.length == 10 -> digitsOnly
            digitsOnly.length == 11 && digitsOnly.startsWith("0") -> digitsOnly.drop(1)
            digitsOnly.length == 12 && digitsOnly.startsWith("91") -> digitsOnly.drop(2)
            else -> return null
        }
        return if (tenDigits.firstOrNull() in '6'..'9') {
            "+91$tenDigits"
        } else {
            null
        }
    }

    fun buildExampleInvitation(nowMillis: Long = System.currentTimeMillis()): MomPayInvitation {
        return generateInvitation(
            senderName = "Abhay",
            debtorName = "Aman",
            amountPaise = 50_000L,
            expenseDescription = "Goa Trip",
            groupName = "Goa Trip – Squad",
            createdAtMillis = nowMillis,
            validityMillis = DEFAULT_VALIDITY_MILLIS
        )
    }
}
