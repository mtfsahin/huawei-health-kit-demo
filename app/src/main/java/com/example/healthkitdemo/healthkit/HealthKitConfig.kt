package com.example.healthkitdemo.healthkit

import android.net.Uri
import androidx.core.net.toUri
import com.example.healthkitdemo.BuildConfig

enum class HealthKitAuthMethod(val label: String) {
    WEB_OAUTH("Web OAuth"),
    LOGIN_FREE_SDK("Login-free SDK"),
}

enum class HistoryWindow(val scope: String?, val days: Long) {
    NONE(null, 0),
    WEEK("https://www.huawei.com/healthkit/historydata.open.week", 7),
    MONTH("https://www.huawei.com/healthkit/historydata.open.month", 30),
    YEAR("https://www.huawei.com/healthkit/historydata.open.year", 365),
}

enum class HealthFeature(val title: String, val scope: String, val api: String, val dataType: String = "") {
    STEPS("Steps", "step.read", DAILY_POLYMERIZE, "com.huawei.continuous.steps.delta"),
    CALORIES("Active calories", "calories.read", DAILY_POLYMERIZE, "com.huawei.continuous.calories.burnt"),
    DISTANCE("Distance", "distance.read", DAILY_POLYMERIZE, "com.huawei.continuous.distance.delta"),
    HEART_RATE("Heart rate", "heartrate.read", DAILY_POLYMERIZE, "com.huawei.instantaneous.heart_rate"),
    RESTING_HEART_RATE("Resting heart rate", "heartrate.read", DAILY_POLYMERIZE, "com.huawei.instantaneous.resting_heart_rate"),
    STRESS("Stress", "stress.read", DAILY_POLYMERIZE, "com.huawei.instantaneous.stress"),
    ACTIVITY_SUMMARY("Activity goals", "dailyactivitysummary.read", "sampleSet:dailyActivitySummary"),
    SLEEP("Sleep", "sleep.read", "healthRecords", "com.huawei.health.record.sleep"),
    WORKOUTS("Workouts", "activityrecord.read", "activityRecords"),
}

private const val DAILY_POLYMERIZE = "sampleSet:dailyPolymerize"

object HealthKitScopes {
    private const val PREFIX = "https://www.huawei.com/healthkit/"

    val DEFAULT: List<String>
        get() = listOf("openid", PREFIX + "activity.read") +
            HealthFeature.entries.map { PREFIX + it.scope }.distinct() +
            listOfNotNull(HealthKitConfig.historyWindow.scope)
}

object HealthKitConfig {
    val clientId: String = BuildConfig.HEALTH_KIT_CLIENT_ID
    val deepLink: String = BuildConfig.HEALTH_KIT_DEEP_LINK
    val schemeSecret: String = BuildConfig.HEALTH_KIT_SCHEME_SECRET
    val backendUrl: String = BuildConfig.BACKEND_BASE_URL.trimEnd('/')
    val webRedirectUri: String get() = "$backendUrl/oauth/callback"
    val apiHost: String = BuildConfig.HEALTH_KIT_API_HOST.ifBlank { "health-api.cloud.huawei.com" }

    val defaultAuthMethod: HealthKitAuthMethod =
        HealthKitAuthMethod.entries.firstOrNull { it.name == BuildConfig.HEALTH_KIT_AUTH_METHOD }
            ?: HealthKitAuthMethod.WEB_OAUTH

    val historyWindow: HistoryWindow =
        HistoryWindow.entries.firstOrNull { it.name == BuildConfig.HEALTH_KIT_HISTORY_WINDOW } ?: HistoryWindow.NONE

    const val AUTHORIZE_URL = "https://oauth-login.cloud.huawei.com/oauth2/v3/authorize"
    const val TOKEN_INFO_URL =
        "https://oauth-api.cloud.huawei.com/rest.php?nsp_fmt=JSON&nsp_svc=huawei.oauth2.user.getTokenInfo"
    const val LOGIN_FREE_CALLBACK_URL = "https://h5hosting.dbankcdn.com/cch5/healthkit/oauth-h5/oauth-callback.html"
    const val HUAWEI_HEALTH_PACKAGE = "com.huawei.health"

    const val SITE_CROSS_ERROR_CODE = 121001
    const val REFRESH_TOKEN_RESULT_CODE = 1203
    val REFRESH_TOKEN_EXPIRED_SUB_ERRORS = setOf(11205, 31204)

    const val DEFAULT_LOOKBACK_DAYS = 7L
    const val OVERVIEW_DAYS = 7
    const val MAX_QUERY_RANGE_DAYS = 365L
    const val MAX_DAYS_PER_QUERY = 31L

    fun missingValues(method: HealthKitAuthMethod): List<String> {
        val required = mutableMapOf(
            "healthkit.clientId" to clientId,
            "backend.baseUrl" to backendUrl,
        )
        if (method == HealthKitAuthMethod.LOGIN_FREE_SDK) {
            required["healthkit.deepLink"] = deepLink
            required["healthkit.schemeSecret"] = schemeSecret
        }
        return required.filterValues { it.isBlank() }.keys.toList()
    }

    fun isDeepLink(uri: Uri): Boolean {
        val expected = deepLink.toUri()
        return uri.scheme.equals(expected.scheme, ignoreCase = true) &&
            uri.host.equals(expected.host, ignoreCase = true)
    }
}
