package com.example.healthkitdemo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.healthkitdemo.R
import com.example.healthkitdemo.healthkit.HealthKitAuthMethod
import com.example.healthkitdemo.healthkit.errors.ErrorInfo
import com.example.healthkitdemo.ui.theme.Metric

@Composable
fun OnboardingScreen(
    method: HealthKitAuthMethod,
    isConnecting: Boolean,
    error: ErrorInfo?,
    onMethodSelected: (HealthKitAuthMethod) -> Unit,
    onConnect: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.onboarding_text),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.onboarding_reads), style = MaterialTheme.typography.titleSmall)
                DataRow(Icons.AutoMirrored.Outlined.DirectionsRun, Metric.Workout, R.string.data_workouts)
                DataRow(Icons.AutoMirrored.Outlined.DirectionsWalk, Metric.Steps, R.string.data_activity)
                DataRow(Icons.Outlined.LocalFireDepartment, Metric.Calories, R.string.data_calories)
                DataRow(Icons.Outlined.FavoriteBorder, Metric.HeartRate, R.string.data_heart_rate)
                DataRow(Icons.Outlined.SelfImprovement, Metric.Stress, R.string.data_stress)
                DataRow(Icons.Outlined.Bedtime, Metric.Sleep, R.string.data_sleep)
            }
        }

        Text(stringResource(R.string.authorization_method), style = MaterialTheme.typography.titleSmall)
        MethodOption(
            selected = method == HealthKitAuthMethod.WEB_OAUTH,
            title = stringResource(R.string.method_web_title),
            text = stringResource(R.string.method_web_text),
            onClick = { onMethodSelected(HealthKitAuthMethod.WEB_OAUTH) },
        )
        MethodOption(
            selected = method == HealthKitAuthMethod.LOGIN_FREE_SDK,
            title = stringResource(R.string.method_login_free_title),
            text = stringResource(R.string.method_login_free_text),
            onClick = { onMethodSelected(HealthKitAuthMethod.LOGIN_FREE_SDK) },
        )

        error?.let { ErrorCard(it, onDismissError) }

        Button(onClick = onConnect, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (isConnecting) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(stringResource(if (isConnecting) R.string.waiting else R.string.connect))
        }
    }
}

@Composable
private fun DataRow(icon: ImageVector, metric: Metric, label: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MetricIcon(icon, metric)
        Spacer(Modifier.width(12.dp))
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun MethodOption(selected: Boolean, title: String, text: String, onClick: () -> Unit) {
    Card(
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(top = 2.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
