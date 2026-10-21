package com.example.healthkitdemo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.healthkitdemo.R
import com.example.healthkitdemo.healthkit.WorkoutRecord
import com.example.healthkitdemo.healthkit.errors.ErrorInfo
import com.example.healthkitdemo.ui.theme.Metric
import java.text.DateFormat
import java.util.Date

private val RANGES = listOf(7L to R.string.range_week, 30L to R.string.range_month, 90L to R.string.range_quarter, 365L to R.string.range_year)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    workouts: List<WorkoutRecord>,
    lookbackDays: Long,
    isLoading: Boolean,
    notice: String?,
    error: ErrorInfo?,
    onRangeSelected: (Long) -> Unit,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    onWorkoutClick: (WorkoutRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(isRefreshing = isLoading, onRefresh = onRefresh, modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            error?.let { item { ErrorCard(it, onDismissError) } }
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    RANGES.forEachIndexed { index, (days, label) ->
                        SegmentedButton(
                            selected = lookbackDays == days,
                            onClick = { onRangeSelected(days) },
                            shape = SegmentedButtonDefaults.itemShape(index, RANGES.size),
                        ) {
                            Text(stringResource(label))
                        }
                    }
                }
            }
            item {
                Text(
                    pluralStringResource(R.plurals.workout_count, workouts.size, workouts.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (workouts.isEmpty() && !isLoading) {
                item { EmptyState(notice ?: stringResource(R.string.no_workouts), Modifier.padding(top = 24.dp)) }
            }
            items(workouts, key = { it.id.ifBlank { it.startTime.toString() } }) { workout ->
                WorkoutRow(workout, onClick = { onWorkoutClick(workout) })
            }
        }
    }
}

@Composable
private fun WorkoutRow(workout: WorkoutRecord, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            MetricIcon(workoutIcon(workout), Metric.Workout)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(workout.activityTypeName, style = MaterialTheme.typography.titleSmall)
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(workout.startTime)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                stringResource(R.string.minutes, workout.durationMinutes.toInt()),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

fun workoutIcon(workout: WorkoutRecord): ImageVector {
    val name = workout.activityTypeName.lowercase()
    return when {
        "run" in name -> Icons.AutoMirrored.Outlined.DirectionsRun
        "walk" in name || "hik" in name -> Icons.AutoMirrored.Outlined.DirectionsWalk
        "cycl" in name || "bik" in name -> Icons.AutoMirrored.Outlined.DirectionsBike
        "swim" in name -> Icons.Outlined.Pool
        "badminton" in name || "tennis" in name || "squash" in name -> Icons.Outlined.SportsTennis
        "basketball" in name -> Icons.Outlined.SportsBasketball
        "soccer" in name -> Icons.Outlined.SportsSoccer
        "yoga" in name || "pilates" in name || "meditation" in name -> Icons.Outlined.SelfImprovement
        else -> Icons.Outlined.FitnessCenter
    }
}
