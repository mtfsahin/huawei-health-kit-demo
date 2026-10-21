package com.example.healthkitdemo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.healthkitdemo.R
import com.example.healthkitdemo.healthkit.DaySummary
import com.example.healthkitdemo.healthkit.Goal
import com.example.healthkitdemo.healthkit.HealthFeature
import com.example.healthkitdemo.healthkit.HealthOverview
import com.example.healthkitdemo.healthkit.SleepSummary
import com.example.healthkitdemo.healthkit.errors.ErrorInfo
import com.example.healthkitdemo.ui.theme.Metric
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date

private const val DEFAULT_STEP_GOAL = 10_000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    overview: HealthOverview,
    dataAccess: Map<HealthFeature, ErrorInfo?>,
    isLoading: Boolean,
    error: ErrorInfo?,
    privacyOff: Boolean,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    onOpenHuaweiHealth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = overview.today
    PullToRefreshBox(isRefreshing = isLoading, onRefresh = onRefresh, modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().testTag("today"),
        ) {
            error?.let { item { ErrorCard(it, onDismissError) } }
            if (privacyOff) item { PrivacyCard(onOpenHuaweiHealth, onDismissError) }
            item { StepsHero(today) }
            today?.activity?.let { activity ->
                item { GoalsCard(activity.activeCalories, activity.exerciseMinutes, activity.activeHours) }
            }
            item { WeekCard(overview.days) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HeartRateCard(today, Modifier.weight(1f))
                    StressCard(today, Modifier.weight(1f))
                }
            }
            item { SleepCard(overview.lastSleep) }
            if (dataAccess.isNotEmpty()) item { DataAccessCard(dataAccess) }
        }
    }
}

@Composable
private fun StepsHero(today: DaySummary?) {
    val steps = today?.steps ?: 0
    val goal = today?.activity?.steps?.target?.takeIf { it > 0 } ?: DEFAULT_STEP_GOAL
    val progress = (steps.toFloat() / goal).coerceIn(0f, 1f)
    val content = MaterialTheme.colorScheme.onPrimaryContainer
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = content)) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
                CircularProgressIndicator(
                    progress = { progress },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = content.copy(alpha = 0.12f),
                    strokeWidth = 10.dp,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    modifier = Modifier.fillMaxSize(),
                )
                Icon(Icons.AutoMirrored.Outlined.DirectionsWalk, contentDescription = null)
            }
            Spacer(Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.today_steps), style = MaterialTheme.typography.labelLarge)
                Text(number(today?.steps), style = MaterialTheme.typography.displaySmall)
                Text(stringResource(R.string.steps_goal, (progress * 100).toInt(), number(goal)), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 6.dp)) {
                    InlineValue(Icons.Outlined.LocalFireDepartment, "${number(today?.caloriesKcal)} ${stringResource(R.string.unit_kcal)}")
                    InlineValue(Icons.Outlined.Straighten, "${kilometers(today?.distanceMeters)} ${stringResource(R.string.unit_km)}")
                }
            }
        }
    }
}

@Composable
private fun InlineValue(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun GoalsCard(calories: Goal, exercise: Goal, activeHours: Goal) {
    SurfaceCard(title = stringResource(R.string.activity_goals)) {
        GoalRow(Icons.Outlined.LocalFireDepartment, Metric.Calories, stringResource(R.string.active_calories), calories, stringResource(R.string.unit_kcal))
        GoalRow(Icons.Outlined.FitnessCenter, Metric.Workout, stringResource(R.string.exercise), exercise, stringResource(R.string.unit_min))
        GoalRow(Icons.Outlined.Timer, Metric.Distance, stringResource(R.string.active_hours), activeHours, stringResource(R.string.unit_h))
    }
}

@Composable
private fun GoalRow(icon: ImageVector, metric: Metric, title: String, goal: Goal, unit: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MetricIcon(icon, metric)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row {
                Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.goal_value, number(goal.value), number(goal.target), unit),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { goal.progress },
                color = metric.color,
                trackColor = metric.container,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
        }
    }
}

@Composable
private fun WeekCard(days: List<DaySummary>) {
    SurfaceCard(title = stringResource(R.string.steps_last_7_days)) {
        if (days.isEmpty()) {
            EmptyState(stringResource(R.string.no_data))
            return@SurfaceCard
        }
        val format = SimpleDateFormat("EEE", LocalConfiguration.current.locales[0])
        BarChart(
            values = days.map { format.format(Date(it.dayStartMillis)).take(2) to (it.steps ?: 0).toFloat() },
            color = Metric.Steps.color,
        )
    }
}

@Composable
private fun HeartRateCard(today: DaySummary?, modifier: Modifier) {
    MetricCard(
        title = stringResource(R.string.heart_rate),
        value = number(today?.heartRateAvg),
        unit = stringResource(R.string.unit_bpm),
        icon = Icons.Outlined.FavoriteBorder,
        metric = Metric.HeartRate,
        footnote = listOfNotNull(
            today?.heartRateMin?.let { stringResource(R.string.range, number(it), number(today.heartRateMax)) },
            today?.restingHeartRate?.let { stringResource(R.string.heart_rate_resting, number(it)) },
        ).joinToString("\n").ifEmpty { stringResource(R.string.no_data) },
        modifier = modifier,
    )
}

@Composable
private fun StressCard(today: DaySummary?, modifier: Modifier) {
    MetricCard(
        title = stringResource(R.string.stress),
        value = number(today?.stressAvg),
        unit = stringResource(R.string.unit_avg),
        icon = Icons.Outlined.SelfImprovement,
        metric = Metric.Stress,
        footnote = today?.stressMin?.let { stringResource(R.string.range, number(it), number(today.stressMax)) }
            ?: stringResource(R.string.no_data),
        modifier = modifier,
    )
}

@Composable
private fun SleepCard(sleep: SleepSummary?) {
    SurfaceCard(title = stringResource(R.string.sleep_last_night)) {
        if (sleep == null) {
            EmptyState(stringResource(R.string.no_data))
            return@SurfaceCard
        }
        val time = SimpleDateFormat("HH:mm", LocalConfiguration.current.locales[0])
        Row(verticalAlignment = Alignment.CenterVertically) {
            MetricIcon(Icons.Outlined.Bedtime, Metric.Sleep)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(duration(sleep.totalMinutes), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.sleep_window, time.format(Date(sleep.startMillis)), time.format(Date(sleep.endMillis))),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            sleep.score?.let { Pill(stringResource(R.string.sleep_score, it), Metric.Sleep.container, Metric.Sleep.color) }
        }
        val stages = listOf(
            Triple(R.string.sleep_deep, sleep.deepMinutes, Metric.Sleep.color),
            Triple(R.string.sleep_light, sleep.lightMinutes, Metric.Sleep.color.copy(alpha = 0.45f)),
            Triple(R.string.sleep_rem, sleep.remMinutes, Metric.Workout.color),
            Triple(R.string.sleep_awake, sleep.awakeMinutes, Metric.Stress.color),
        ).mapNotNull { (label, minutes, color) -> minutes?.takeIf { it > 0 }?.let { Triple(label, it, color) } }
        if (stages.isEmpty()) return@SurfaceCard

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
        ) {
            stages.forEach { (_, minutes, color) ->
                Box(Modifier.weight(minutes.toFloat()).fillMaxSize().background(color))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            stages.forEach { (label, minutes, color) -> Legend(color, stringResource(label), duration(minutes)) }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text("$label $value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DataAccessCard(access: Map<HealthFeature, ErrorInfo?>) {
    SurfaceCard(title = stringResource(R.string.data_access)) {
        Text(stringResource(R.string.data_access_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HealthFeature.entries.forEachIndexed { index, feature ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AccessRow(feature, checked = access.containsKey(feature), error = access[feature])
        }
    }
}

@Composable
private fun AccessRow(feature: HealthFeature, checked: Boolean, error: ErrorInfo?) {
    val (icon, tint) = when {
        !checked -> Icons.Outlined.Schedule to MaterialTheme.colorScheme.onSurfaceVariant
        error == null -> Icons.Outlined.CheckCircle to MaterialTheme.colorScheme.primary
        else -> Icons.Outlined.ErrorOutline to MaterialTheme.colorScheme.error
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(feature.title, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${feature.scope} · ${error?.label ?: feature.api}",
                style = MaterialTheme.typography.labelSmall,
                color = if (error == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SurfaceCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
private fun PrivacyCard(onOpenHuaweiHealth: () -> Unit, onDismiss: () -> Unit) {
    ErrorCard(
        error = ErrorInfo(
            domain = "PRIVACY",
            code = "2",
            title = stringResource(R.string.privacy_off_title),
            cause = stringResource(R.string.privacy_off_text),
            solution = stringResource(R.string.privacy_off_solution),
        ),
        onDismiss = onDismiss,
        actionLabel = stringResource(R.string.open_huawei_health),
        onAction = onOpenHuaweiHealth,
    )
}

private fun number(value: Number?): String = value?.let { NumberFormat.getIntegerInstance().format(it.toDouble().toLong()) } ?: "–"

private fun kilometers(meters: Double?): String = meters?.let { "%.1f".format(it / 1000) } ?: "–"

private fun duration(minutes: Int): String = if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"
