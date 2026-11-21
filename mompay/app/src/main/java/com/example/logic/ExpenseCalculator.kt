package com.example.logic

import com.example.data.Expense
import com.example.data.GroupMember
import com.example.data.MemberBalance
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object ExpenseCalculator {

    fun calculateSplitShares(
        amountPaise: Long,
        participantIds: List<String>
    ): Map<String, Long> {
        require(amountPaise > 0L) { "Amount must be positive." }
        val distinctParticipants = participantIds.distinct()
        require(distinctParticipants.isNotEmpty()) { "At least one participant is required." }

        val count = distinctParticipants.size.toLong()
        val baseShare = amountPaise / count
        val remainder = (amountPaise % count).toInt()

        val shares = LinkedHashMap<String, Long>(distinctParticipants.size)
        distinctParticipants.forEachIndexed { index, memberId ->
            val extraPaisa = if (index < remainder) 1L else 0L
            shares[memberId] = baseShare + extraPaisa
        }
        return shares
    }

    fun calculateMemberBalances(
        members: List<GroupMember>,
        expenses: List<Expense>
    ): List<MemberBalance> {
        val validIds = members.map { it.id }.toSet()
        val paidByMember = members.associate { it.id to 0L }.toMutableMap()
        val shareByMember = members.associate { it.id to 0L }.toMutableMap()

        for (expense in expenses) {
            if (expense.amountPaise <= 0L) continue
            if (expense.paidById !in validIds) continue
            val validParticipants = expense.participantIds.filter { it in validIds }.distinct()
            if (validParticipants.isEmpty()) continue

            paidByMember[expense.paidById] =
                (paidByMember[expense.paidById] ?: 0L) + expense.amountPaise

            val shares = calculateSplitShares(expense.amountPaise, validParticipants)
            for ((participantId, sharePaise) in shares) {
                shareByMember[participantId] =
                    (shareByMember[participantId] ?: 0L) + sharePaise
            }
        }

        return members.map { member ->
            val paid = paidByMember[member.id] ?: 0L
            val share = shareByMember[member.id] ?: 0L
            MemberBalance(
                member = member,
                totalPaidPaise = paid,
                totalSharePaise = share,
                netBalancePaise = paid - share
            )
        }
    }

    fun calculateTotalExpenditurePaise(expenses: List<Expense>): Long {
        return expenses.sumOf { it.amountPaise.coerceAtLeast(0L) }
    }

    fun countRawRepayments(
        members: List<GroupMember>,
        expenses: List<Expense>
    ): Int {
        val validIds = members.map { it.id }.toSet()
        var count = 0
        for (expense in expenses) {
            if (expense.amountPaise <= 0L || expense.paidById !in validIds) continue
            val nonPayerParticipants = expense.participantIds
                .filter { it in validIds && it != expense.paidById }
                .distinct()
            count += nonPayerParticipants.size
        }
        return count
    }

    fun formatRupees(
        paise: Long,
        includePlusSign: Boolean = false
    ): String {
        val isNegative = paise < 0L
        val absPaise = abs(paise)
        val wholeRupees = absPaise / 100L
        val remainderPaise = absPaise % 100L

        val indiaFormat = NumberFormat.getIntegerInstance(Locale("en", "IN"))
        val formattedWhole = indiaFormat.format(wholeRupees)
        val amountText = if (remainderPaise == 0L) {
            "₹$formattedWhole"
        } else {
            val paiseStr = remainderPaise.toString().padStart(2, '0')
            "₹$formattedWhole.$paiseStr"
        }

        return when {
            isNegative -> "-$amountText"
            includePlusSign && paise > 0L -> "+$amountText"
            else -> amountText
        }
    }

    fun parseRupeesToPaise(input: String): Long? {
        val cleaned = input.trim().replace(",", "").replace("₹", "")
        if (cleaned.isEmpty()) return null
        val regex = Regex("^\\d+(\\.\\d{1,2})?$")
        if (!regex.matches(cleaned)) return null
        val parts = cleaned.split(".")
        val rupees = parts[0].toLongOrNull() ?: return null
        val paise = if (parts.size == 2) {
            parts[1].padEnd(2, '0').toLongOrNull() ?: return null
        } else {
            0L
        }
        if (rupees > 100_000_000L) return null
        return rupees * 100L + paise
    }

    fun amountToSpokenRupees(amountPaise: Long): String {
        val absPaise = abs(amountPaise)
        val rupees = absPaise / 100L
        val paise = absPaise % 100L
        val rupeeWords = numberToWords(rupees)
        val rupeeUnit = if (rupees == 1L) "rupee" else "rupees"
        return if (paise == 0L) {
            "$rupeeWords $rupeeUnit"
        } else {
            val paiseWords = numberToWords(paise)
            val paiseUnit = if (paise == 1L) "paisa" else "paise"
            "$rupeeWords $rupeeUnit and $paiseWords $paiseUnit"
        }
    }

    private fun numberToWords(number: Long): String {
        if (number == 0L) return "zero"
        val ones = arrayOf(
            "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
            "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
            "seventeen", "eighteen", "nineteen"
        )
        val tens = arrayOf(
            "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"
        )

        fun twoDigits(n: Int): String {
            return when {
                n < 20 -> ones[n]
                n % 10 == 0 -> tens[n / 10]
                else -> "${tens[n / 10]} ${ones[n % 10]}"
            }
        }

        fun threeDigits(n: Int): String {
            val h = n / 100
            val rest = n % 100
            return buildString {
                if (h > 0) {
                    append("${ones[h]} hundred")
                    if (rest > 0) append(" ")
                }
                if (rest > 0) append(twoDigits(rest))
            }
        }

        val parts = mutableListOf<String>()
        var remaining = number
        val crores = (remaining / 10_000_000L).toInt()
        remaining %= 10_000_000L
        val lakhs = (remaining / 100_000L).toInt()
        remaining %= 100_000L
        val thousands = (remaining / 1_000L).toInt()
        remaining %= 1_000L
        val hundreds = remaining.toInt()

        if (crores > 0) parts.add("${twoDigits(crores)} crore")
        if (lakhs > 0) parts.add("${twoDigits(lakhs)} lakh")
        if (thousands > 0) parts.add("${twoDigits(thousands)} thousand")
        if (hundreds > 0) parts.add(threeDigits(hundreds))

        return parts.joinToString(" ")
    }
}
