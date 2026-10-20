package com.example.healthkitdemo.healthkit

import com.example.healthkitdemo.healthkit.HealthFeature.ACTIVITY_SUMMARY
import com.example.healthkitdemo.healthkit.HealthFeature.CALORIES
import com.example.healthkitdemo.healthkit.HealthFeature.DISTANCE
import com.example.healthkitdemo.healthkit.HealthFeature.HEART_RATE
import com.example.healthkitdemo.healthkit.HealthFeature.RESTING_HEART_RATE
import com.example.healthkitdemo.healthkit.HealthFeature.SLEEP
import com.example.healthkitdemo.healthkit.HealthFeature.STEPS
import com.example.healthkitdemo.healthkit.HealthFeature.STRESS

data class DaySummary(
    val dayStartMillis: Long,
    val steps: Int? = null,
    val caloriesKcal: Double? = null,
    val distanceMeters: Double? = null,
    val heartRateAvg: Double? = null,
    val heartRateMin: Double? = null,
    val heartRateMax: Double? = null,
    val restingHeartRate: Double? = null,
    val stressAvg: Double? = null,
    val stressMin: Double? = null,
    val stressMax: Double? = null,
    val activity: ActivitySummary? = null,
    val sleep: SleepSummary? = null,
) {
    companion object {
        fun from(dayStartMillis: Long, values: Map<HealthFeature, Map<String, Double>>): DaySummary {
            fun value(feature: HealthFeature, field: String): Double? = values[feature]?.get(field)
            return DaySummary(
                dayStartMillis = dayStartMillis,
                steps = (value(STEPS, "steps") ?: value(ACTIVITY_SUMMARY, "steps"))?.toInt(),
                caloriesKcal = value(CALORIES, "calories_total") ?: value(ACTIVITY_SUMMARY, "activeCalories"),
                distanceMeters = value(DISTANCE, "distance"),
                heartRateAvg = value(HEART_RATE, "avg"),
                heartRateMin = value(HEART_RATE, "min"),
                heartRateMax = value(HEART_RATE, "max"),
                restingHeartRate = value(RESTING_HEART_RATE, "last") ?: value(RESTING_HEART_RATE, "avg"),
                stressAvg = value(STRESS, "avg"),
                stressMin = value(STRESS, "min"),
                stressMax = value(STRESS, "max"),
                activity = values[ACTIVITY_SUMMARY]?.takeIf { it.isNotEmpty() }?.let(ActivitySummary::from),
                sleep = values[SLEEP]?.takeIf { it.isNotEmpty() }?.let(SleepSummary::from),
            )
        }
    }
}

data class Goal(val value: Int, val target: Int) {
    val progress: Float get() = if (target <= 0) 0f else (value.toFloat() / target).coerceIn(0f, 1f)
}

data class ActivitySummary(
    val steps: Goal,
    val activeCalories: Goal,
    val exerciseMinutes: Goal,
    val activeHours: Goal,
) {
    companion object {
        fun from(fields: Map<String, Double>): ActivitySummary {
            fun goal(name: String) = Goal(fields[name]?.toInt() ?: 0, fields[name + "Goal"]?.toInt() ?: 0)
            return ActivitySummary(goal("steps"), goal("activeCalories"), goal("exerciseTime"), goal("activeHours"))
        }
    }
}

data class SleepSummary(
    val startMillis: Long,
    val endMillis: Long,
    val totalMinutes: Int,
    val deepMinutes: Int? = null,
    val lightMinutes: Int? = null,
    val remMinutes: Int? = null,
    val awakeMinutes: Int? = null,
    val score: Int? = null,
) {
    companion object {
        fun from(fields: Map<String, Double>) = SleepSummary(
            startMillis = fields["fall_asleep_time"]?.toLong() ?: 0,
            endMillis = fields["wakeup_time"]?.toLong() ?: 0,
            totalMinutes = fields["all_sleep_time"]?.toInt() ?: 0,
            deepMinutes = fields["deep_sleep_time"]?.toInt(),
            lightMinutes = fields["light_sleep_time"]?.toInt(),
            remMinutes = fields["dream_time"]?.toInt(),
            awakeMinutes = fields["awake_time"]?.toInt(),
            score = fields["sleep_score"]?.toInt(),
        )
    }
}

data class HealthOverview(val days: List<DaySummary> = emptyList()) {
    val today: DaySummary? get() = days.lastOrNull()
    val lastSleep: SleepSummary? get() = days.lastOrNull { it.sleep != null }?.sleep
}
