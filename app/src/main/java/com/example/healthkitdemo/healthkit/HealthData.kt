package com.example.healthkitdemo.healthkit

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Fetched<T>(val data: T, val privacyStatus: Int?)

data class DayValues(val timeMillis: Long, val fields: Map<String, Double>)

data class DayRange(val startDay: String, val endDay: String, val timeZone: String) {

    fun toJson(): JSONObject = JSONObject()
        .put("startDay", startDay)
        .put("endDay", endDay)
        .put("timeZone", timeZone)

    companion object {
        fun of(firstDayMillis: Long, lastDayMillis: Long): DayRange {
            val day = SimpleDateFormat("yyyyMMdd", Locale.US)
            val zone = SimpleDateFormat("Z", Locale.US).format(Date())
            return DayRange(day.format(Date(firstDayMillis)), day.format(Date(lastDayMillis)), zone)
        }
    }
}

object Days {
    fun startOf(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun last(count: Int): List<Long> = (count - 1 downTo 0).map { daysAgo ->
        startOf(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }.timeInMillis)
    }
}

fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).map(::getJSONObject)

fun JSONObject.fieldValues(): Map<String, Double> =
    optJSONArray("value").objects().mapNotNull { entry ->
        val key = NUMBER_KEYS.firstOrNull(entry::has) ?: return@mapNotNull null
        entry.optString("fieldName") to entry.getDouble(key)
    }.toMap()

private val NUMBER_KEYS = listOf("integerValue", "floatValue", "longValue")
