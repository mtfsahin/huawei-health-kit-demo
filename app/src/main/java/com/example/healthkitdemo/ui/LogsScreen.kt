package com.example.healthkitdemo.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthkitdemo.R
import com.example.healthkitdemo.log.LogEntry
import com.example.healthkitdemo.log.LogLevel

@Composable
fun LogsScreen(
    entries: List<LogEntry>,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var minimum by rememberSaveable { mutableStateOf(LogLevel.DEBUG) }
    val visible = entries.filter { it.level >= minimum }.asReversed()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 4.dp),
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(LogLevel.entries) { level ->
                    FilterChip(
                        selected = minimum == level,
                        onClick = { minimum = level },
                        label = { Text(if (level == LogLevel.DEBUG) stringResource(R.string.log_all) else level.name) },
                    )
                }
            }
            IconButton(onClick = onCopy) { Icon(Icons.Outlined.ContentCopy, stringResource(R.string.log_copy)) }
            IconButton(onClick = onShare) { Icon(Icons.Outlined.Share, stringResource(R.string.log_share)) }
            IconButton(onClick = onClear) { Icon(Icons.Outlined.DeleteSweep, stringResource(R.string.log_clear)) }
        }
        HorizontalDivider()

        if (visible.isEmpty()) {
            EmptyState(text = stringResource(R.string.log_empty), modifier = Modifier.padding(24.dp))
            return@Column
        }

        LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
            items(visible, key = { it.id }) { entry ->
                LogRow(entry)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = entry.detail != null) { expanded = !expanded }
            .animateContentSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LevelBadge(entry.level)
            Spacer(Modifier.width(8.dp))
            Text(
                entry.category.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            Text(entry.time, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(entry.message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        if (expanded && entry.detail != null) {
            Text(
                entry.detail,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp))
                    .padding(10.dp),
            )
        }
    }
}

@Composable
private fun LevelBadge(level: LogLevel) {
    val (container, content) = levelColors(level)
    Text(
        level.name,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = content,
        modifier = Modifier
            .background(container, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun levelColors(level: LogLevel): Pair<Color, Color> = with(MaterialTheme.colorScheme) {
    when (level) {
        LogLevel.DEBUG -> surfaceContainerHighest to onSurfaceVariant
        LogLevel.INFO -> secondaryContainer to onSecondaryContainer
        LogLevel.WARN -> tertiaryContainer to onTertiaryContainer
        LogLevel.ERROR -> errorContainer to onErrorContainer
    }
}
