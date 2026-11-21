package com.example

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Base64
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.logic.InvitationDecodeResult
import com.example.logic.MomPayInvitationCodec
import com.example.repository.FairSplitRepository
import com.example.ui.buildSmsComposerIntent
import com.example.ui.theme.FairSplitTheme
import com.example.voice.MomPayVoiceAssistant
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `manifest contains only VIBRATE and zero SMS or Contacts or Call permissions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pkgInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val perms = pkgInfo.requestedPermissions?.toSet().orEmpty()
        assertTrue(Manifest.permission.VIBRATE in perms)
        assertFalse(Manifest.permission.SEND_SMS in perms)
        assertFalse(Manifest.permission.READ_SMS in perms)
        assertFalse(Manifest.permission.RECEIVE_SMS in perms)
        assertFalse(Manifest.permission.READ_CONTACTS in perms)
        assertFalse(Manifest.permission.CALL_PHONE in perms)
        assertFalse(Manifest.permission.RECORD_AUDIO in perms)
    }

    @Test
    fun `read string from context returns FairSplit`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FairSplit", appName)
    }

    @Test
    fun `mompay invitation codec round trip malformed and expired validation`() {
        val now = 1_750_000_000_000L
        val original = MomPayInvitationCodec.generateInvitation(
            senderName = "Abhay",
            debtorName = "Aman",
            amountPaise = 50_000L,
            expenseDescription = "Goa Trip",
            groupName = "Goa Trip – Squad",
            createdAtMillis = now,
            validityMillis = 24L * 3600_000L
        )
        val token = MomPayInvitationCodec.encodeInvitation(original)
        assertTrue(token.startsWith("MOMPAY1:"))

        val smsMessage = MomPayInvitationCodec.formatSmsMessage(original, "Aunty")
        assertTrue(smsMessage.contains("Abhay"))
        assertTrue(smsMessage.contains("Aman"))
        assertTrue(smsMessage.contains("₹500"))
        assertTrue(smsMessage.contains(token))

        val decoded = MomPayInvitationCodec.decodeInvitation(smsMessage, nowMillis = now + 1000L)
        assertTrue(decoded is InvitationDecodeResult.Success)
        val parsed = (decoded as InvitationDecodeResult.Success).invitation
        assertEquals(original.invitationId, parsed.invitationId)
        assertEquals("Abhay", parsed.senderName)
        assertEquals("Aman", parsed.debtorName)
        assertEquals(50_000L, parsed.amountPaise)
        assertEquals("Goa Trip", parsed.expenseDescription)

        // Expired invitation check
        val expiredResult = MomPayInvitationCodec.decodeInvitation(
            smsMessage,
            nowMillis = now + 25L * 3600_000L
        )
        assertTrue(expiredResult is InvitationDecodeResult.Error)
        assertTrue((expiredResult as InvitationDecodeResult.Error).message.contains("expired", ignoreCase = true))

        // Malformed inputs
        assertTrue(MomPayInvitationCodec.decodeInvitation("") is InvitationDecodeResult.Error)
        assertTrue(MomPayInvitationCodec.decodeInvitation("hello there") is InvitationDecodeResult.Error)
        assertTrue(MomPayInvitationCodec.decodeInvitation("MOMPAY2:abc") is InvitationDecodeResult.Error)
        assertTrue(MomPayInvitationCodec.decodeInvitation("MOMPAY1:!!!notbase64!!!") is InvitationDecodeResult.Error)

        // Non-positive amount inside valid Base64 JSON
        val badAmountJson = JSONObject().apply {
            put("v", 1)
            put("i", "MP-BAD")
            put("s", "Abhay")
            put("d", "Aman")
            put("a", 0L)
            put("e", "Goa Trip")
            put("g", "Squad")
            put("t", now)
            put("x", now + 100_000L)
        }
        val badToken = "MOMPAY1:" + Base64.encodeToString(
            badAmountJson.toString().toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        assertTrue(MomPayInvitationCodec.decodeInvitation(badToken, nowMillis = now + 10L) is InvitationDecodeResult.Error)
    }

    @Test
    fun `sms intent uses ACTION_SENDTO smsto URI and sms_body extra`() {
        val normalized = MomPayInvitationCodec.normalizeIndianMobileNumber("9876543210")
        assertEquals("+919876543210", normalized)
        assertEquals("+919876543210", MomPayInvitationCodec.normalizeIndianMobileNumber("09876543210"))
        assertNull(MomPayInvitationCodec.normalizeIndianMobileNumber("12345"))

        val intent = buildSmsComposerIntent("+919876543210", "MOMPAY1:abc")
        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("smsto", intent.data?.scheme)
        assertTrue(intent.data.toString().contains("9876543210"))
        assertEquals("MOMPAY1:abc", intent.getStringExtra("sms_body"))
    }

    @Test
    fun `voice assistant builds personalized script with sender debtor amount and expense`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val assistant = MomPayVoiceAssistant(context)
        val inv = MomPayInvitationCodec.buildExampleInvitation()
        val script = assistant.buildScript(inv)
        assertTrue(script.contains("Aman"))
        assertTrue(script.contains("five hundred rupees"))
        assertTrue(script.contains("Abhay"))
        assertTrue(script.contains("Goa Trip"))
        assistant.shutdown()
    }

    @Test
    fun `repository persists expenses and mother contact across restarts and resets demo`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FairSplitRepository(context)
        val initial = repo.resetDemo()
        assertEquals(6, initial.expenses.size)

        repo.saveMotherContact("Sunita Aunty", "+919876543210")
        repo.saveSmsConfirmedBySender(true)
        val trimmed = initial.expenses.drop(1)
        repo.saveExpenses(trimmed)

        val reloaded = FairSplitRepository(context).loadState()
        assertEquals(5, reloaded.expenses.size)
        assertEquals("Sunita Aunty", reloaded.motherName)
        assertEquals("+919876543210", reloaded.motherPhone)
        assertTrue(reloaded.smsInvitationConfirmedBySender)

        val restored = repo.resetDemo()
        assertEquals(6, restored.expenses.size)
        assertFalse(restored.smsInvitationConfirmedBySender)
    }

    @Test
    fun `dashboard settlement mompay sms clipboard paste and receiver call CUJ`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        FairSplitRepository(context).resetDemo()

        composeTestRule.setContent {
            FairSplitTheme {
                FairSplitApp()
            }
        }
        composeTestRule.waitForIdle()

        // Dashboard check
        composeTestRule.onNodeWithTag("goa_trip_group_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹3,200").assertIsDisplayed()

        // Navigate to Settlement and scroll to generated transfers
        composeTestRule.onNodeWithTag("dashboard_lazy_list")
            .performScrollToNode(hasTestTag("dashboard_view_settlement_button"))
        composeTestRule.onNodeWithTag("dashboard_view_settlement_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("after_transfer_count").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settlement_lazy_list")
            .performScrollToNode(hasTestTag("settlement_tx_card_1"))
        composeTestRule.onNodeWithTag("settlement_tx_card_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settlement_lazy_list")
            .performScrollToNode(hasTestTag("settlement_tx_card_3"))
        composeTestRule.onNodeWithTag("settlement_tx_card_3").assertIsDisplayed()

        // Navigate to Expenses and add a new expense
        composeTestRule.onNodeWithTag("nav_expenses").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("add_expense_fab").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("expense_title_input").performTextInput("Kayaking")
        composeTestRule.onNodeWithTag("expense_amount_input").performTextInput("400")
        composeTestRule.onNodeWithTag("save_expense_button").performClick()
        composeTestRule.waitForIdle()

        // Reset Demo from Settlement tab restores ₹3,200
        composeTestRule.onNodeWithTag("nav_settlement").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reset_demo_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("₹3,200").assertIsDisplayed()

        // Navigate to MomPay & verify Aman + Mother Contact + Copy Invitation
        composeTestRule.onNodeWithTag("nav_mompay").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mompay_lazy_list")
            .performScrollToNode(hasTestTag("unpaid_friend_card"))
        composeTestRule.onNodeWithTag("unpaid_friend_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Name: Aman").assertIsDisplayed()
        composeTestRule.onNodeWithText("Status: EXPIRED").assertIsDisplayed()

        composeTestRule.onNodeWithTag("mompay_lazy_list")
            .performScrollToNode(hasTestTag("copy_invitation_button"))
        composeTestRule.onNodeWithTag("copy_invitation_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("mompay_lazy_list")
            .performScrollToNode(hasTestTag("confirm_sms_sent_button"))
        composeTestRule.onNodeWithTag("confirm_sms_sent_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mompay_lazy_list")
            .performScrollToNode(hasTestTag("sms_shared_confirmed_banner"))
        composeTestRule.onNodeWithTag("sms_shared_confirmed_banner").assertIsDisplayed()

        // Open Mom Receiver Mode, paste copied SMS from clipboard, validate & accept call
        composeTestRule.onNodeWithTag("topbar_receive_invite_button").performClick()
        composeTestRule.waitForIdle()

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        assertNotNull(clipText)
        assertTrue(clipText!!.contains("MOMPAY1:"))

        composeTestRule.onNodeWithTag("paste_clipboard_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("validate_invitation_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("receiver_lazy_list")
            .performScrollToNode(hasTestTag("validated_invitation_card"))
        composeTestRule.onNodeWithTag("validated_invitation_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("receiver_lazy_list")
            .performScrollToNode(hasTestTag("accept_mompay_call_button"))
        composeTestRule.onNodeWithTag("accept_mompay_call_button").performClick()
        composeTestRule.waitForIdle()

        // Incoming Call -> Answer -> Mute -> Replay -> End Call -> Summary
        composeTestRule.onNodeWithTag("incoming_call_debtor").assertIsDisplayed()
        composeTestRule.onNodeWithTag("answer_call_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mute_call_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mute_call_button").performClick() // Unmute
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("replay_call_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("end_call_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("call_summary_card").assertIsDisplayed()
    }
}
