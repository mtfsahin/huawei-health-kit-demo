package com.example.healthkitdemo

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.healthkitdemo.healthkit.HealthFeature
import com.example.healthkitdemo.healthkit.HealthKitAuthMethod
import com.example.healthkitdemo.healthkit.errors.ErrorInfo
import com.example.healthkitdemo.log.LogCategory
import com.example.healthkitdemo.log.LogEntry
import com.example.healthkitdemo.log.LogLevel
import com.example.healthkitdemo.ui.HealthKitActions
import com.example.healthkitdemo.ui.HealthKitScreen
import com.example.healthkitdemo.ui.theme.HealthKitDemoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HealthKitScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun connectStartsTheAuthorization() {
        var connects = 0
        show(UiState(), HealthKitActions(onConnect = { connects++ }))

        compose.onNodeWithText("Connect Huawei Health").performClick()
        assertEquals(1, connects)
    }

    @Test
    fun choosingTheLoginFreeMethodIsReported() {
        var method: HealthKitAuthMethod? = null
        show(UiState(), HealthKitActions(onAuthMethodSelected = { method = it }))

        compose.onNodeWithText("Login-free SDK").performClick()
        assertEquals(HealthKitAuthMethod.LOGIN_FREE_SDK, method)
    }

    @Test
    fun deletingAsksForConfirmationFirst() {
        var deletes = 0
        show(UiState(isAuthorized = true), HealthKitActions(onDeleteData = { deletes++ }))

        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Delete this app's data").performClick()
        assertEquals(0, deletes)
        compose.onNodeWithText("Delete").performClick()
        assertEquals(1, deletes)
    }

    @Test
    fun unreadableDataShowsItsErrorCode() {
        val forbidden = ErrorInfo("REST", "SC_FORBIDDEN", "Insufficient Permission", "", "")
        show(UiState(isAuthorized = true, dataAccess = mapOf(HealthFeature.STRESS to forbidden, HealthFeature.STEPS to null)))

        compose.onNodeWithTag("today").performScrollToNode(hasText("Data access"))
        compose.onNodeWithText("stress.read · REST SC_FORBIDDEN").assertExists()
    }

    @Test
    fun logsTabShowsTheEntries() {
        val entry = LogEntry(1, 1_790_000_000_000, LogLevel.ERROR, LogCategory.API, "GET /activityRecords -> 403 (120 ms)", null)
        show(UiState(isAuthorized = true), logs = listOf(entry))

        compose.onNodeWithText("Logs").performClick()
        compose.onNodeWithText("GET /activityRecords -> 403 (120 ms)").assertExists()
    }

    private fun show(state: UiState, actions: HealthKitActions = HealthKitActions(), logs: List<LogEntry> = emptyList()) {
        compose.setContent {
            HealthKitDemoTheme {
                HealthKitScreen(state, logs, actions)
            }
        }
    }
}
