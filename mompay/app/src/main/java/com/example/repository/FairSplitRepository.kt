package com.example.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.Expense
import com.example.data.GroupMember
import com.example.data.SettlementHistoryRecord
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

enum class NavTab {
    HOME,
    EXPENSES,
    SETTLEMENT,
    MOMPAY
}

data class PersistedAppState(
    val groupName: String,
    val currentUserId: String,
    val members: List<GroupMember>,
    val expenses: List<Expense>,
    val demoModeOverdue: Boolean,
    val graceAnchorMillis: Long,
    val settlementHistory: List<SettlementHistoryRecord>,
    val lastTab: NavTab,
    val motherName: String,
    val motherPhone: String,
    val smsInvitationConfirmedBySender: Boolean
)

class FairSplitRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadState(): PersistedAppState {
        val isInitialized = prefs.getBoolean(KEY_INITIALIZED, false)
        if (!isInitialized) {
            val initial = createDemoState()
            saveAll(initial)
            return initial
        }

        val members = decodeMembers(prefs.getString(KEY_MEMBERS, null))
            ?: defaultMembers()
        val expenses = decodeExpenses(prefs.getString(KEY_EXPENSES, null))
            ?: defaultExpenses()
        val demoOverdue = prefs.getBoolean(KEY_DEMO_OVERDUE, true)
        val anchor = prefs.getLong(KEY_GRACE_ANCHOR, System.currentTimeMillis())
        val history = decodeHistory(prefs.getString(KEY_SETTLEMENT_HISTORY, null))
        val tabName = prefs.getString(KEY_LAST_TAB, NavTab.HOME.name)
        val lastTab = NavTab.entries.firstOrNull { it.name == tabName } ?: NavTab.HOME
        val motherName = prefs.getString(KEY_MOTHER_NAME, DEFAULT_MOTHER_NAME) ?: DEFAULT_MOTHER_NAME
        val motherPhone = prefs.getString(KEY_MOTHER_PHONE, "") ?: ""
        val smsConfirmed = prefs.getBoolean(KEY_SMS_CONFIRMED, false)

        return PersistedAppState(
            groupName = DEMO_GROUP_NAME,
            currentUserId = CURRENT_USER_ID,
            members = members,
            expenses = expenses,
            demoModeOverdue = demoOverdue,
            graceAnchorMillis = anchor,
            settlementHistory = history,
            lastTab = lastTab,
            motherName = motherName,
            motherPhone = motherPhone,
            smsInvitationConfirmedBySender = smsConfirmed
        )
    }

    fun saveExpenses(expenses: List<Expense>) {
        prefs.edit().putString(KEY_EXPENSES, encodeExpenses(expenses)).apply()
    }

    fun saveDemoModeOverdue(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEMO_OVERDUE, enabled).apply()
    }

    fun saveGraceAnchor(anchorMillis: Long) {
        prefs.edit().putLong(KEY_GRACE_ANCHOR, anchorMillis).apply()
    }

    fun saveSettlementHistory(history: List<SettlementHistoryRecord>) {
        prefs.edit().putString(KEY_SETTLEMENT_HISTORY, encodeHistory(history)).apply()
    }

    fun saveLastTab(tab: NavTab) {
        prefs.edit().putString(KEY_LAST_TAB, tab.name).apply()
    }

    fun saveMotherContact(name: String, phone: String) {
        prefs.edit()
            .putString(KEY_MOTHER_NAME, name.trim())
            .putString(KEY_MOTHER_PHONE, phone.trim())
            .apply()
    }

    fun saveSmsConfirmedBySender(confirmed: Boolean) {
        prefs.edit().putBoolean(KEY_SMS_CONFIRMED, confirmed).apply()
    }

    fun resetDemo(): PersistedAppState {
        val currentMotherName = prefs.getString(KEY_MOTHER_NAME, DEFAULT_MOTHER_NAME) ?: DEFAULT_MOTHER_NAME
        val currentMotherPhone = prefs.getString(KEY_MOTHER_PHONE, "") ?: ""
        val demo = createDemoState(
            motherName = currentMotherName,
            motherPhone = currentMotherPhone
        )
        saveAll(demo)
        return demo
    }

    private fun saveAll(state: PersistedAppState) {
        prefs.edit()
            .putBoolean(KEY_INITIALIZED, true)
            .putString(KEY_MEMBERS, encodeMembers(state.members))
            .putString(KEY_EXPENSES, encodeExpenses(state.expenses))
            .putBoolean(KEY_DEMO_OVERDUE, state.demoModeOverdue)
            .putLong(KEY_GRACE_ANCHOR, state.graceAnchorMillis)
            .putString(KEY_SETTLEMENT_HISTORY, encodeHistory(state.settlementHistory))
            .putString(KEY_LAST_TAB, state.lastTab.name)
            .putString(KEY_MOTHER_NAME, state.motherName)
            .putString(KEY_MOTHER_PHONE, state.motherPhone)
            .putBoolean(KEY_SMS_CONFIRMED, state.smsInvitationConfirmedBySender)
            .apply()
    }

    private fun createDemoState(
        motherName: String = DEFAULT_MOTHER_NAME,
        motherPhone: String = ""
    ): PersistedAppState {
        val now = System.currentTimeMillis()
        return PersistedAppState(
            groupName = DEMO_GROUP_NAME,
            currentUserId = CURRENT_USER_ID,
            members = defaultMembers(),
            expenses = defaultExpenses(now),
            demoModeOverdue = true,
            graceAnchorMillis = now,
            settlementHistory = emptyList(),
            lastTab = NavTab.HOME,
            motherName = motherName,
            motherPhone = motherPhone,
            smsInvitationConfirmedBySender = false
        )
    }

    private fun defaultMembers(): List<GroupMember> = listOf(
        GroupMember(id = "abhay", name = "Abhay"),
        GroupMember(id = "rahul", name = "Rahul"),
        GroupMember(id = "priya", name = "Priya"),
        GroupMember(id = "aman", name = "Aman")
    )

    private fun defaultExpenses(baseMillis: Long = System.currentTimeMillis()): List<Expense> {
        val allIds = listOf("abhay", "rahul", "priya", "aman")
        return listOf(
            Expense(
                id = "exp_hotel",
                title = "Hotel",
                amountPaise = 100_000L, // ₹1,000
                paidById = "abhay",
                participantIds = allIds,
                createdAtMillis = baseMillis - 6 * 3600_000L
            ),
            Expense(
                id = "exp_food",
                title = "Food",
                amountPaise = 60_000L, // ₹600
                paidById = "abhay",
                participantIds = allIds,
                createdAtMillis = baseMillis - 5 * 3600_000L
            ),
            Expense(
                id = "exp_petrol",
                title = "Petrol",
                amountPaise = 70_000L, // ₹700
                paidById = "rahul",
                participantIds = allIds,
                createdAtMillis = baseMillis - 4 * 3600_000L
            ),
            Expense(
                id = "exp_dinner",
                title = "Dinner",
                amountPaise = 50_000L, // ₹500
                paidById = "rahul",
                participantIds = allIds,
                createdAtMillis = baseMillis - 3 * 3600_000L
            ),
            Expense(
                id = "exp_snacks",
                title = "Snacks",
                amountPaise = 30_000L, // ₹300
                paidById = "aman",
                participantIds = allIds,
                createdAtMillis = baseMillis - 2 * 3600_000L
            ),
            Expense(
                id = "exp_tickets",
                title = "Tickets",
                amountPaise = 10_000L, // ₹100
                paidById = "priya",
                participantIds = allIds,
                createdAtMillis = baseMillis - 1 * 3600_000L
            )
        )
    }

    private fun encodeMembers(members: List<GroupMember>): String {
        val arr = JSONArray()
        members.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id)
                put("name", m.name)
            })
        }
        return arr.toString()
    }

    private fun decodeMembers(raw: String?): List<GroupMember>? {
        if (raw.isNullOrBlank()) return null
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<GroupMember>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(GroupMember(obj.getString("id"), obj.getString("name")))
            }
            list.ifEmpty { null }
        } catch (_: JSONException) {
            null
        }
    }

    private fun encodeExpenses(expenses: List<Expense>): String {
        val arr = JSONArray()
        expenses.forEach { e ->
            val pArr = JSONArray()
            e.participantIds.forEach { pArr.put(it) }
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("title", e.title)
                put("amountPaise", e.amountPaise)
                put("paidById", e.paidById)
                put("participantIds", pArr)
                put("createdAtMillis", e.createdAtMillis)
            })
        }
        return arr.toString()
    }

    private fun decodeExpenses(raw: String?): List<Expense>? {
        if (raw == null) return null
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<Expense>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val pArr = obj.getJSONArray("participantIds")
                val participants = mutableListOf<String>()
                for (j in 0 until pArr.length()) {
                    participants.add(pArr.getString(j))
                }
                list.add(
                    Expense(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        amountPaise = obj.getLong("amountPaise"),
                        paidById = obj.getString("paidById"),
                        participantIds = participants,
                        createdAtMillis = obj.getLong("createdAtMillis")
                    )
                )
            }
            list
        } catch (_: JSONException) {
            null
        }
    }

    private fun encodeHistory(history: List<SettlementHistoryRecord>): String {
        val arr = JSONArray()
        history.forEach { h ->
            arr.put(JSONObject().apply {
                put("id", h.id)
                put("timestampMillis", h.timestampMillis)
                put("totalSettledPaise", h.totalSettledPaise)
                put("transferCount", h.transferCount)
                put("summary", h.summary)
            })
        }
        return arr.toString()
    }

    private fun decodeHistory(raw: String?): List<SettlementHistoryRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<SettlementHistoryRecord>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SettlementHistoryRecord(
                        id = obj.getString("id"),
                        timestampMillis = obj.getLong("timestampMillis"),
                        totalSettledPaise = obj.getLong("totalSettledPaise"),
                        transferCount = obj.getInt("transferCount"),
                        summary = obj.getString("summary")
                    )
                )
            }
            list
        } catch (_: JSONException) {
            emptyList()
        }
    }

    companion object {
        private const val PREFS_NAME = "fairsplit_prefs"
        private const val KEY_INITIALIZED = "initialized_v1"
        private const val KEY_MEMBERS = "members_json"
        private const val KEY_EXPENSES = "expenses_json"
        private const val KEY_DEMO_OVERDUE = "mompay_demo_overdue"
        private const val KEY_GRACE_ANCHOR = "mompay_grace_anchor_millis"
        private const val KEY_SETTLEMENT_HISTORY = "settlement_history_json"
        private const val KEY_LAST_TAB = "last_tab"
        private const val KEY_MOTHER_NAME = "mother_name"
        private const val KEY_MOTHER_PHONE = "mother_phone"
        private const val KEY_SMS_CONFIRMED = "sms_confirmed_by_sender"

        const val DEMO_GROUP_NAME = "Goa Trip – Squad"
        const val CURRENT_USER_ID = "abhay"
        const val DEFAULT_MOTHER_NAME = "Aunty"
    }
}
