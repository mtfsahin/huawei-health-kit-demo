package com.example.healthkitdemo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healthkitdemo.HealthKitViewModel
import com.example.healthkitdemo.R
import com.example.healthkitdemo.UiState
import com.example.healthkitdemo.healthkit.ActivitySummary
import com.example.healthkitdemo.healthkit.DaySummary
import com.example.healthkitdemo.healthkit.Goal
import com.example.healthkitdemo.healthkit.HealthFeature
import com.example.healthkitdemo.healthkit.HealthKitAuthMethod
import com.example.healthkitdemo.healthkit.HealthOverview
import com.example.healthkitdemo.healthkit.SleepSummary
import com.example.healthkitdemo.healthkit.WorkoutRecord
import com.example.healthkitdemo.healthkit.errors.ErrorInfo
import com.example.healthkitdemo.log.AppLog
import com.example.healthkitdemo.log.LogEntry
import com.example.healthkitdemo.log.LogLevel
import com.example.healthkitdemo.ui.theme.HealthKitDemoTheme

enum class Tab(val label: Int, val icon: ImageVector) {
    Today(R.string.tab_today, Icons.Outlined.Today),
    Workouts(R.string.tab_workouts, Icons.AutoMirrored.Outlined.DirectionsRun),
    Logs(R.string.tab_logs, Icons.AutoMirrored.Outlined.ReceiptLong),
}

data class HealthKitActions(
    val onAuthMethodSelected: (HealthKitAuthMethod) -> Unit = {},
    val onConnect: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onRangeSelected: (Long) -> Unit = {},
    val onRefreshWorkouts: () -> Unit = {},
    val onDisconnect: () -> Unit = {},
    val onDeleteData: () -> Unit = {},
    val onOpenHuaweiHealth: () -> Unit = {},
    val onDismissError: () -> Unit = {},
    val onCopyLog: () -> Unit = {},
    val onShareLog: () -> Unit = {},
    val onClearLog: () -> Unit = {},
)

@Composable
fun HealthKitRoute(
    viewModel: HealthKitViewModel,
    onConnect: () -> Unit,
    onOpenHuaweiHealth: () -> Unit,
    onCopyLog: () -> Unit,
    onShareLog: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val logs by AppLog.entries.collectAsStateWithLifecycle()

    HealthKitScreen(
        state = state,
        logs = logs,
        actions = HealthKitActions(
            onAuthMethodSelected = viewModel::selectAuthMethod,
            onConnect = onConnect,
            onRefresh = viewModel::refresh,
            onRangeSelected = viewModel::selectLookbackDays,
            onRefreshWorkouts = viewModel::loadWorkouts,
            onDisconnect = viewModel::disconnect,
            onDeleteData = viewModel::deleteData,
            onOpenHuaweiHealth = onOpenHuaweiHealth,
            onDismissError = viewModel::dismissError,
            onCopyLog = onCopyLog,
            onShareLog = onShareLog,
            onClearLog = AppLog::clear,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthKitScreen(
    state: UiState,
    logs: List<LogEntry>,
    actions: HealthKitActions,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(Tab.Today) }
    var openWorkout by remember { mutableStateOf<WorkoutRecord?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val errorCount = logs.count { it.level == LogLevel.ERROR }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(tab.label))
                        if (state.isAuthorized) {
                            Text(
                                state.accountOpenId?.let { stringResource(R.string.connected_as, it.take(8) + "…") }
                                    ?: stringResource(R.string.connected),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                actions = { if (state.isAuthorized) AccountActions(actions, onDelete = { confirmDelete = true }) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = {
                            BadgedBox(badge = { if (item == Tab.Logs && errorCount > 0) Badge { Text(errorCount.toString()) } }) {
                                Icon(item.icon, contentDescription = null)
                            }
                        },
                        label = { Text(stringResource(item.label)) },
                    )
                }
            }
        },
    ) { padding ->
        val content = Modifier.padding(padding)
        when {
            tab == Tab.Logs -> LogsScreen(logs, actions.onCopyLog, actions.onShareLog, actions.onClearLog, content)
            !state.isAuthorized -> OnboardingScreen(
                method = state.authMethod,
                isConnecting = state.isConnecting,
                error = state.error,
                onMethodSelected = actions.onAuthMethodSelected,
                onConnect = actions.onConnect,
                onDismissError = actions.onDismissError,
                modifier = content,
            )
            tab == Tab.Today -> TodayScreen(
                overview = state.overview,
                dataAccess = state.dataAccess,
                isLoading = state.isLoadingOverview,
                error = state.error,
                privacyOff = state.privacyOff,
                onRefresh = actions.onRefresh,
                onDismissError = actions.onDismissError,
                onOpenHuaweiHealth = actions.onOpenHuaweiHealth,
                modifier = content,
            )
            else -> WorkoutsScreen(
                workouts = state.workouts,
                lookbackDays = state.lookbackDays,
                isLoading = state.isLoadingWorkouts,
                notice = state.workoutsNotice,
                error = state.error,
                onRangeSelected = actions.onRangeSelected,
                onRefresh = actions.onRefreshWorkouts,
                onDismissError = actions.onDismissError,
                onWorkoutClick = { openWorkout = it },
                modifier = content,
            )
        }
    }

    openWorkout?.let { WorkoutDetailDialog(workout = it, onDismiss = { openWorkout = null }) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    actions.onDeleteData()
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun AccountActions(actions: HealthKitActions, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = actions.onRefresh) { Icon(Icons.Outlined.Refresh, stringResource(R.string.refresh)) }
    IconButton(onClick = { open = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.menu)) }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(text = { Text(stringResource(R.string.open_huawei_health)) }, onClick = {
            open = false
            actions.onOpenHuaweiHealth()
        })
        DropdownMenuItem(text = { Text(stringResource(R.string.disconnect)) }, onClick = {
            open = false
            actions.onDisconnect()
        })
        DropdownMenuItem(text = { Text(stringResource(R.string.delete_data), color = MaterialTheme.colorScheme.error) }, onClick = {
            open = false
            onDelete()
        })
    }
}

private val previewOverview = HealthOverview(
    days = (6 downTo 0).map { index ->
        DaySummary(
            dayStartMillis = 1_790_000_000_000 - index * 86_400_000L,
            steps = listOf(6200, 8400, 10350, 4800, 9100, 12040, 7342)[6 - index],
            caloriesKcal = 412.0,
            distanceMeters = 5240.0,
            heartRateAvg = 72.0,
            heartRateMin = 51.0,
            heartRateMax = 148.0,
            restingHeartRate = 58.0,
            stressAvg = 31.0,
            stressMin = 12.0,
            stressMax = 64.0,
            activity = ActivitySummary(Goal(7342, 10000), Goal(412, 500), Goal(24, 30), Goal(7, 12)),
            sleep = SleepSummary(1_789_970_000_000, 1_789_996_000_000, 433, 92, 241, 84, 16, 82),
        )
    },
)

private val previewAccess: Map<HealthFeature, ErrorInfo?> = HealthFeature.entries.associateWith { null } +
    (HealthFeature.STRESS to ErrorInfo("REST", "SC_FORBIDDEN", "Insufficient Permission", "", ""))

@Preview(showBackground = true)
@Composable
private fun TodayPreview() {
    HealthKitDemoTheme {
        HealthKitScreen(UiState(isAuthorized = true, accountOpenId = "MDFAMTAxNTI0", overview = previewOverview, dataAccess = previewAccess), emptyList(), HealthKitActions())
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() {
    HealthKitDemoTheme {
        HealthKitScreen(UiState(), emptyList(), HealthKitActions())
    }
}
