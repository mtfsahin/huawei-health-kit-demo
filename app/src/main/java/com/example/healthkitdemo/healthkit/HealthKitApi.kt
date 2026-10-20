package com.example.healthkitdemo.healthkit

import androidx.core.net.toUri
import com.example.healthkitdemo.healthkit.net.HealthKitHttpException
import com.example.healthkitdemo.healthkit.net.Http
import com.example.healthkitdemo.healthkit.net.HttpResponse
import com.example.healthkitdemo.healthkit.net.privacyStatus
import com.example.healthkitdemo.healthkit.net.requireSuccess
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class HealthKitApi(private val session: SessionStore) {

    suspend fun dailyStats(accessToken: String, feature: HealthFeature, range: DayRange): Fetched<List<DayValues>> {
        val body = range.toJson().put("dataTypes", JSONArray(listOf(feature.dataType)))
        val response = onUserSite(accessToken) { site -> Http.postJson(url(site, feature.api), body, accessToken) }
        val days = response.json().optJSONArray("group").objects().map { group ->
            val points = group.optJSONArray("sampleSet").objects().flatMap { it.optJSONArray("samplePoints").objects() }
            DayValues(group.optLong("startTime"), points.fold(emptyMap()) { fields, point -> fields + point.fieldValues() })
        }
        return Fetched(days, response.privacyStatus)
    }

    suspend fun activitySummary(accessToken: String, range: DayRange): Fetched<List<DayValues>> {
        val response = onUserSite(accessToken) { site ->
            Http.postJson(url(site, HealthFeature.ACTIVITY_SUMMARY.api), range.toJson(), accessToken)
        }
        val days = response.json().optJSONArray("dailyActivitySummary").objects().map { point ->
            DayValues(TimeUnit.NANOSECONDS.toMillis(point.optLong("startTime")), point.fieldValues())
        }
        return Fetched(days, response.privacyStatus)
    }

    suspend fun sleep(accessToken: String, startTime: Long, endTime: Long): Fetched<List<DayValues>> {
        val response = onUserSite(accessToken) { site -> Http.get(sleepUrl(site, startTime, endTime), accessToken) }
        val nights = response.json().optJSONArray("healthRecords").objects()
            .filter { it.optString("dataTypeName") == HealthFeature.SLEEP.dataType }
            .map { it.fieldValues() }
            .filter { it["sleep_type"] != SLEEP_TYPE_NAP }
            .mapNotNull { fields -> fields["wakeup_time"]?.let { DayValues(it.toLong(), fields) } }
            .sortedBy { it.timeMillis }
        return Fetched(nights, response.privacyStatus)
    }

    suspend fun workouts(accessToken: String, startTime: Long, endTime: Long): Fetched<List<WorkoutRecord>> {
        val records = mutableListOf<WorkoutRecord>()
        var privacyStatus: Int? = null

        windows(startTime, endTime).forEach { (from, to) ->
            var cursor: String? = null
            do {
                val response = onUserSite(accessToken) { site -> Http.get(workoutsUrl(site, from, to, cursor), accessToken) }
                privacyStatus = response.privacyStatus ?: privacyStatus

                val json = response.json()
                val array = json.optJSONArray("activityRecord")
                if (array != null) records += (0 until array.length()).map { WorkoutRecord.fromJson(array.getJSONObject(it)) }
                cursor = json.optString("cursor").takeIf { json.optBoolean("hasMoreData") && it.isNotBlank() }
            } while (cursor != null)
        }

        return Fetched(records.sortedByDescending { it.startTime }, privacyStatus)
    }

    suspend fun cancelAuthorization(accessToken: String) {
        onUserSite(accessToken) { site ->
            Http.delete(url(site, "consents/${HealthKitConfig.clientId}?deleteData=true"), accessToken)
        }
    }

    private suspend fun onUserSite(accessToken: String, call: suspend (String) -> HttpResponse): HttpResponse {
        val response = call(session.site)
        val error = runCatching { response.requireSuccess() }.exceptionOrNull() as? HealthKitHttpException
            ?: return response
        val newSite = error.redirectHost
        if (!error.isSiteCross || newSite == null) throw error

        session.site = newSite
        return call(newSite).requireSuccess()
    }

    private fun windows(startTime: Long, endTime: Long): List<Pair<Long, Long>> {
        val step = TimeUnit.DAYS.toMillis(HealthKitConfig.MAX_DAYS_PER_QUERY)
        return (startTime until endTime step step).map { it to minOf(it + step, endTime) }
    }

    private fun url(site: String, path: String): String = "https://$site/healthkit/v2/$path"

    private fun sleepUrl(site: String, startTime: Long, endTime: Long): String =
        url(site, HealthFeature.SLEEP.api).toUri().buildUpon()
            .appendQueryParameter("startTime", TimeUnit.MILLISECONDS.toNanos(startTime).toString())
            .appendQueryParameter("endTime", TimeUnit.MILLISECONDS.toNanos(endTime).toString())
            .appendQueryParameter("dataType", HealthFeature.SLEEP.dataType)
            .build()
            .toString()

    private fun workoutsUrl(site: String, startTime: Long, endTime: Long, cursor: String?): String {
        val builder = url(site, HealthFeature.WORKOUTS.api).toUri().buildUpon()
        if (cursor != null) return builder.appendQueryParameter("cursor", cursor).build().toString()
        return builder
            .appendQueryParameter("startTime", startTime.toString())
            .appendQueryParameter("endTime", endTime.toString())
            .build()
            .toString()
    }

    private companion object {
        const val SLEEP_TYPE_NAP = 3.0
    }
}
