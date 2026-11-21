package com.example.logic

import com.example.data.Expense
import com.example.data.GroupMember
import com.example.data.SettlementResult
import com.example.data.SettlementTransaction
import kotlin.math.min

object SettlementCalculator {

    private data class MutableAccount(
        val member: GroupMember,
        val orderIndex: Int,
        var amountPaise: Long
    )

    /**
     * Executes a Minimum Cash Flow greedy algorithm using exact integer paise (Long):
     * 1. Calculate each member's total paid amount.
     * 2. Calculate each member's total share.
     * 3. Net Balance = Total Paid - Total Share.
     * 4. Separate creditors (> 0) and debtors (< 0).
     * 5. Greedily match outstanding debtors with creditors.
     * 6. Transfer min(debtorOwed, creditorOwed).
     * 7. Update balances until all balances are zero.
     */
    fun calculate(
        members: List<GroupMember>,
        expenses: List<Expense>
    ): SettlementResult {
        val balances = ExpenseCalculator.calculateMemberBalances(members, expenses)
        val rawCount = ExpenseCalculator.countRawRepayments(members, expenses)

        if (balances.isEmpty()) {
            return SettlementResult(
                balances = emptyList(),
                transactions = emptyList(),
                rawRepaymentCount = 0,
                remainingUnsettledPaise = 0L
            )
        }

        val creditors = mutableListOf<MutableAccount>()
        val debtors = mutableListOf<MutableAccount>()

        balances.forEachIndexed { idx, balance ->
            when {
                balance.netBalancePaise > 0L -> creditors.add(
                    MutableAccount(balance.member, idx, balance.netBalancePaise)
                )
                balance.netBalancePaise < 0L -> debtors.add(
                    MutableAccount(balance.member, idx, -balance.netBalancePaise)
                )
            }
        }

        creditors.sortWith(compareByDescending<MutableAccount> { it.amountPaise }.thenBy { it.orderIndex })
        debtors.sortWith(compareByDescending<MutableAccount> { it.amountPaise }.thenBy { it.orderIndex })

        val transactions = mutableListOf<SettlementTransaction>()
        var cIdx = 0
        var dIdx = 0
        var step = 1

        while (dIdx < debtors.size && cIdx < creditors.size) {
            val debtor = debtors[dIdx]
            val creditor = creditors[cIdx]

            if (debtor.amountPaise <= 0L) {
                dIdx++
                continue
            }
            if (creditor.amountPaise <= 0L) {
                cIdx++
                continue
            }

            val transferAmount = min(debtor.amountPaise, creditor.amountPaise)
            if (transferAmount > 0L) {
                transactions.add(
                    SettlementTransaction(
                        id = "tx_${step++}_${debtor.member.id}_${creditor.member.id}",
                        fromMember = debtor.member,
                        toMember = creditor.member,
                        amountPaise = transferAmount
                    )
                )
                debtor.amountPaise -= transferAmount
                creditor.amountPaise -= transferAmount
            }

            if (debtor.amountPaise == 0L) dIdx++
            if (creditor.amountPaise == 0L) cIdx++
        }

        val remainingUnsettled = debtors.sumOf { it.amountPaise } + creditors.sumOf { it.amountPaise }

        return SettlementResult(
            balances = balances,
            transactions = transactions,
            rawRepaymentCount = rawCount,
            remainingUnsettledPaise = remainingUnsettled
        )
    }
}
