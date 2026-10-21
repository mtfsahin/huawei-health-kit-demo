package com.example.healthkitdemo

import com.example.healthkitdemo.healthkit.ActivitySummary
import com.example.healthkitdemo.healthkit.DaySummary
import com.example.healthkitdemo.healthkit.HealthFeature
import com.example.healthkitdemo.healthkit.fieldValues
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthDataTest {

    @Test
    fun readsValuesByFieldName() {
        val point = JSONObject("""{"value":[{"fieldName":"steps","integerValue":9000},{"fieldName":"distance","floatValue":4210.5},{"fieldName":"wakeup_time","longValue":1790000000000}]}""")
        assertEquals(mapOf("steps" to 9000.0, "distance" to 4210.5, "wakeup_time" to 1.79e12), point.fieldValues())
    }

    @Test
    fun buildsADayFromEveryFeature() {
        val day = DaySummary.from(
            0,
            mapOf(
                HealthFeature.STEPS to mapOf("steps" to 7342.0),
                HealthFeature.HEART_RATE to mapOf("avg" to 72.0, "min" to 51.0, "max" to 148.0),
                HealthFeature.ACTIVITY_SUMMARY to mapOf("activeCalories" to 120.0, "activeCaloriesGoal" to 240.0),
                HealthFeature.SLEEP to mapOf("all_sleep_time" to 433.0, "deep_sleep_time" to 92.0),
            ),
        )
        assertEquals(7342, day.steps)
        assertEquals(120.0, day.caloriesKcal)
        assertEquals(51.0, day.heartRateMin)
        assertEquals(0.5f, day.activity?.activeCalories?.progress)
        assertEquals(433, day.sleep?.totalMinutes)
        assertEquals(92, day.sleep?.deepMinutes)
    }

    @Test
    fun missingGoalMeansNoProgress() {
        assertEquals(0f, ActivitySummary.from(mapOf("steps" to 500.0)).steps.progress)
    }
}
