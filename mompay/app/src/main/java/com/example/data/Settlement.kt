package com.example.data

data class MemberBalance(
    val member: GroupMember,
    val totalPaidPaise: Long,
    val totalSharePaise: Long,
    val netBalancePaise: Long
)

data class SettlementTransaction(
    val id: String,
    val fromMember: GroupMember,
    val toMember: GroupMember,
    val amountPaise: Long
)

data class SettlementResult(
    val balances: List<MemberBalance>,
    val transactions: List<SettlementTransaction>,
    val rawRepaymentCount: Int,
    val remainingUnsettledPaise: Long
)

data class SettlementHistoryRecord(
    val id: String,
    val timestampMillis: Long,
    val totalSettledPaise: Long,
    val transferCount: Int,
    val summary: String
)
