package com.example.data

data class MomPayInvitation(
    val version: Int = CURRENT_VERSION,
    val invitationId: String,
    val senderName: String,
    val debtorName: String,
    val amountPaise: Long,
    val expenseDescription: String,
    val groupName: String,
    val createdAt: Long,
    val expiresAt: Long
) {
    companion object {
        const val CURRENT_VERSION = 1
        const val TOKEN_PREFIX = "MOMPAY1:"
    }
}
