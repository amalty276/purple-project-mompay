package com.example

import com.example.data.Expense
import com.example.data.GroupMember
import com.example.logic.ExpenseCalculator
import com.example.logic.SettlementCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    private val demoMembers = listOf(
        GroupMember("abhay", "Abhay"),
        GroupMember("rahul", "Rahul"),
        GroupMember("priya", "Priya"),
        GroupMember("aman", "Aman")
    )

    private fun demoExpenses(): List<Expense> {
        val all = demoMembers.map { it.id }
        return listOf(
            Expense("1", "Hotel", 100_000L, "abhay", all, 1L),
            Expense("2", "Food", 60_000L, "abhay", all, 2L),
            Expense("3", "Petrol", 70_000L, "rahul", all, 3L),
            Expense("4", "Dinner", 50_000L, "rahul", all, 4L),
            Expense("5", "Snacks", 30_000L, "aman", all, 5L),
            Expense("6", "Tickets", 10_000L, "priya", all, 6L)
        )
    }

    @Test
    fun `goa trip demo data yields expected balances and exactly 3 transfers`() {
        val result = SettlementCalculator.calculate(demoMembers, demoExpenses())

        val balancesById = result.balances.associateBy { it.member.id }
        assertEquals(80_000L, balancesById["abhay"]?.netBalancePaise)
        assertEquals(40_000L, balancesById["rahul"]?.netBalancePaise)
        assertEquals(-70_000L, balancesById["priya"]?.netBalancePaise)
        assertEquals(-50_000L, balancesById["aman"]?.netBalancePaise)

        assertEquals(3, result.transactions.size)
        assertEquals(0L, result.remainingUnsettledPaise)

        val tx1 = result.transactions[0]
        assertEquals("priya", tx1.fromMember.id)
        assertEquals("abhay", tx1.toMember.id)
        assertEquals(70_000L, tx1.amountPaise)

        val tx2 = result.transactions[1]
        assertEquals("aman", tx2.fromMember.id)
        assertEquals("abhay", tx2.toMember.id)
        assertEquals(10_000L, tx2.amountPaise)

        val tx3 = result.transactions[2]
        assertEquals("aman", tx3.fromMember.id)
        assertEquals("rahul", tx3.toMember.id)
        assertEquals(40_000L, tx3.amountPaise)
    }

    @Test
    fun `unequal participants and odd paise splits conserve total money and settle to zero`() {
        val expenses = listOf(
            Expense("a", "Cab", 10_000L, "abhay", listOf("abhay", "rahul", "priya"), 1L),
            Expense("b", "Water", 2_501L, "aman", listOf("aman", "priya"), 2L),
            Expense("c", "ZeroSelf", 5_000L, "rahul", listOf("rahul"), 3L)
        )
        val result = SettlementCalculator.calculate(demoMembers, expenses)

        val sumCredits = result.balances.filter { it.netBalancePaise > 0 }.sumOf { it.netBalancePaise }
        val sumDebts = result.balances.filter { it.netBalancePaise < 0 }.sumOf { -it.netBalancePaise }
        assertEquals(sumCredits, sumDebts)
        assertEquals(0L, result.balances.sumOf { it.netBalancePaise })
        assertEquals(0L, result.remainingUnsettledPaise)
        assertEquals(sumCredits, result.transactions.sumOf { it.amountPaise })
    }

    @Test
    fun `adding and deleting expenses dynamically updates settlement`() {
        val initial = demoExpenses()
        val added = initial + Expense(
            id = "7",
            title = "Parasailing",
            amountPaise = 80_000L,
            paidById = "aman",
            participantIds = demoMembers.map { it.id },
            createdAtMillis = 7L
        )
        val afterAdd = SettlementCalculator.calculate(demoMembers, added)
        val amanAfterAdd = afterAdd.balances.first { it.member.id == "aman" }
        // Aman paid 300+800=1100, share = 800+200=1000 => +100 (+10_000 paise)
        assertEquals(10_000L, amanAfterAdd.netBalancePaise)
        assertEquals(0L, afterAdd.remainingUnsettledPaise)

        val afterDelete = SettlementCalculator.calculate(demoMembers, initial)
        val amanAfterDelete = afterDelete.balances.first { it.member.id == "aman" }
        assertEquals(-50_000L, amanAfterDelete.netBalancePaise)
    }

    @Test
    fun `odd paise equal split distributes remainder deterministically`() {
        val shares = ExpenseCalculator.calculateSplitShares(10_000L, listOf("abhay", "rahul", "priya"))
        assertEquals(3334L, shares["abhay"])
        assertEquals(3333L, shares["rahul"])
        assertEquals(3333L, shares["priya"])
        assertEquals(10_000L, shares.values.sum())
    }

    @Test
    fun `spoken rupee words match expected demo announcement`() {
        assertEquals("five hundred rupees", ExpenseCalculator.amountToSpokenRupees(50_000L))
        assertEquals("one thousand two hundred rupees", ExpenseCalculator.amountToSpokenRupees(120_000L))
    }

    @Test
    fun `empty expenses produce zero transfers and zero unsettled balance`() {
        val result = SettlementCalculator.calculate(demoMembers, emptyList())
        assertEquals(0, result.transactions.size)
        assertEquals(0L, result.remainingUnsettledPaise)
    }
}
