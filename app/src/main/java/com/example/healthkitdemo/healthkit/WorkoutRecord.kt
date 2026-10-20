package com.example.healthkitdemo.healthkit

import org.json.JSONArray
import org.json.JSONObject

data class WorkoutRecord(
    val id: String,
    val name: String,
    val startTime: Long,
    val endTime: Long,
    val activityType: Int,
    val activeTimeMillis: Long,
    val timeZone: String,
    val sourceType: Int? = null,
    val summary: Map<String, String> = emptyMap(),
) {
    val activityTypeName: String get() = WorkoutType.displayName(activityType)

    val durationMinutes: Long get() = activeTimeMillis / 60_000

    fun summaryLabel(key: String): String =
        key.removePrefix("com.huawei.")
            .removePrefix("continuous.")
            .replace('.', ' ')
            .replace('_', ' ')
            .replaceFirstChar { it.uppercase() }

    companion object {
        fun fromJson(json: JSONObject): WorkoutRecord = WorkoutRecord(
            id = json.optString("id"),
            name = json.optString("name"),
            startTime = json.optLong("startTime"),
            endTime = json.optLong("endTime"),
            activityType = json.optInt("activityType"),
            activeTimeMillis = json.optLong("activeTime"),
            timeZone = json.optString("timeZone"),
            sourceType = if (json.has("sourceType")) json.optInt("sourceType") else null,
            summary = summaryOf(json.optJSONObject("activitySummary")?.optJSONArray("dataSummary")),
        )

        private fun summaryOf(entries: JSONArray?): Map<String, String> {
            if (entries == null) return emptyMap()
            return (0 until entries.length())
                .map { entries.getJSONObject(it) }
                .filter { it.optString("dataTypeName").isNotBlank() }
                .associate { it.optString("dataTypeName") to valueOf(it.optJSONArray("value")) }
        }

        private fun valueOf(values: JSONArray?): String {
            if (values == null) return ""
            return (0 until values.length())
                .mapNotNull { values.optJSONObject(it) }
                .mapNotNull { value -> VALUE_FIELDS.firstOrNull(value::has)?.let { format(value.opt(it)) } }
                .joinToString(", ")
        }

        private fun format(value: Any?): String = when (value) {
            is Double -> if (value % 1.0 == 0.0) "%.0f".format(value) else "%.2f".format(value)
            else -> value?.toString().orEmpty()
        }

        private val VALUE_FIELDS = listOf("floatValue", "integerValue", "longValue", "stringValue")
    }
}
