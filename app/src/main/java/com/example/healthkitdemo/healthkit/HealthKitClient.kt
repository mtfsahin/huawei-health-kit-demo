package com.example.healthkitdemo.healthkit

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.healthkitdemo.healthkit.auth.AuthorizationRedirect
import com.example.healthkitdemo.healthkit.auth.HealthKitAuthorizer
import com.example.healthkitdemo.healthkit.auth.LoginFreeAuthorizer
import com.example.healthkitdemo.healthkit.auth.WebOAuthAuthorizer
import com.example.healthkitdemo.healthkit.net.HealthKitHttpException
import com.example.healthkitdemo.healthkit.token.HealthKitTokenClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.UUID
import java.util.concurrent.TimeUnit

data class OverviewFetch(
    val overview: HealthOverview,
    val failures: Map<HealthFeature, Throwable>,
    val privacyOff: Boolean,
)

data class WorkoutFetch(
    val records: List<WorkoutRecord>,
    val notice: String? = null,
    val needsPrivacy: Boolean = false,
)

class HealthKitClient(context: Context) {

    private val session = SessionStore(context)
    private val api = HealthKitApi(session)
    private val tokens = HealthKitTokenClient()

    val authMethod: HealthKitAuthMethod get() = session.authMethod
    val isAuthorized: Boolean get() = session.isAuthorized
    val accountOpenId: String? get() = session.openId

    fun useMethod(method: HealthKitAuthMethod) {
        session.authMethod = method
    }

    fun authorizationIntent(context: Context): Result<Intent> {
        val missing = HealthKitConfig.missingValues(authMethod)
        if (missing.isNotEmpty()) {
            return Result.failure(IllegalStateException("Add ${missing.joinToString()} to local.properties."))
        }
        val state = UUID.randomUUID().toString()
        val authorizer = authorizer()
        session.pendingState = state
        session.pendingRedirectUri = authorizer.tokenRedirectUri
        return runCatching { authorizer.intent(context, state) }
    }

    fun ownsRedirect(uri: Uri): Boolean = HealthKitConfig.isDeepLink(uri)

    suspend fun completeAuthorization(uri: Uri): Result<Unit> {
        val code = AuthorizationRedirect.parseCode(uri, session.pendingState).getOrElse { return Result.failure(it) }
        val redirectUri = session.pendingRedirectUri ?: authorizer().tokenRedirectUri
        session.pendingState = null
        session.pendingRedirectUri = null

        return tokens.exchangeCode(code, redirectUri).map { response ->
            session.save(response)
            session.openId = tokens.openId(response.accessToken).getOrNull()
        }
    }

    suspend fun fetchOverview(): Result<OverviewFetch> {
        val accessToken = accessToken().getOrElse { return Result.failure(it) }
        val fetch = readOverview(accessToken)
        val expired = fetch.failures.values.any { (it as? HealthKitHttpException)?.isInvalidCredentials == true }
        if (!expired) return Result.success(fetch)
        return refreshAccessToken().map { readOverview(it) }
    }

    private suspend fun readOverview(accessToken: String): OverviewFetch {
        val days = Days.last(HealthKitConfig.OVERVIEW_DAYS).filter { it + DAY_MILLIS > earliestReadable() }
        val range = DayRange.of(days.first(), days.last())
        val sleepFrom = maxOf(days.first() - DAY_MILLIS / 2, earliestReadable())

        val results = coroutineScope {
            OVERVIEW_FEATURES.map { feature ->
                async {
                    feature to runCatching {
                        when (feature) {
                            HealthFeature.ACTIVITY_SUMMARY -> api.activitySummary(accessToken, range)
                            HealthFeature.SLEEP -> api.sleep(accessToken, sleepFrom, System.currentTimeMillis())
                            else -> api.dailyStats(accessToken, feature, range)
                        }
                    }
                }
            }.awaitAll().toMap()
        }

        val failures = results.mapNotNull { (feature, result) -> result.exceptionOrNull()?.let { feature to it } }.toMap()

        val byDay = results.mapValues { (_, result) ->
            result.getOrNull()?.data.orEmpty().associate { Days.startOf(it.timeMillis) to it.fields }
        }
        val summaries = days.map { day -> DaySummary.from(day, byDay.mapValues { it.value[day].orEmpty() }) }

        return OverviewFetch(
            overview = HealthOverview(summaries),
            failures = failures,
            privacyOff = results.values.any { it.getOrNull()?.privacyStatus == PRIVACY_NOT_GRANTED },
        )
    }

    suspend fun fetchWorkouts(days: Long): Result<WorkoutFetch> {
        val endTime = System.currentTimeMillis()
        val asked = endTime - TimeUnit.DAYS.toMillis(days.coerceIn(1L, HealthKitConfig.MAX_QUERY_RANGE_DAYS))
        val startTime = maxOf(asked, earliestReadable())

        val accessToken = accessToken().getOrElse { return Result.failure(it) }
        val page = runCatching { api.workouts(accessToken, startTime, endTime) }
            .recoverCatching { error ->
                if ((error as? HealthKitHttpException)?.isInvalidCredentials != true) throw error
                api.workouts(refreshAccessToken().getOrThrow(), startTime, endTime)
            }
            .getOrElse { return Result.failure(it) }

        return Result.success(
            WorkoutFetch(
                records = page.data,
                notice = noticeFor(page, startTime > asked),
                needsPrivacy = page.privacyStatus == PRIVACY_NOT_GRANTED,
            )
        )
    }

    suspend fun accessToken(): Result<String> {
        val token = session.accessToken
        if (token != null && session.hasValidAccessToken()) return Result.success(token)
        return refreshAccessToken()
    }

    suspend fun cancelAuthorization(): Result<Unit> {
        val accessToken = accessToken().getOrElse { return Result.failure(it) }
        return runCatching { api.cancelAuthorization(accessToken) }.onSuccess { signOut() }
    }

    fun signOut() {
        session.clearSession()
    }

    private suspend fun refreshAccessToken(): Result<String> {
        val refreshToken = session.refreshToken
            ?: return Result.failure(IllegalStateException("Connect a Huawei Health account first."))

        return tokens.refresh(refreshToken)
            .onSuccess(session::save)
            .map { it.accessToken }
            .onFailure { error ->
                if ((error as? HealthKitHttpException)?.isRefreshTokenExpired == true) signOut()
            }
    }

    private fun authorizer(): HealthKitAuthorizer = when (authMethod) {
        HealthKitAuthMethod.WEB_OAUTH -> WebOAuthAuthorizer()
        HealthKitAuthMethod.LOGIN_FREE_SDK -> LoginFreeAuthorizer()
    }

    private fun earliestReadable(): Long =
        session.authorizedAtMillis - TimeUnit.DAYS.toMillis(HealthKitConfig.historyWindow.days)

    private fun noticeFor(page: Fetched<List<WorkoutRecord>>, clampedToAuthorization: Boolean): String? {
        if (page.data.isNotEmpty()) return null
        return when {
            page.privacyStatus == PRIVACY_NOT_GRANTED ->
                "Huawei Health has not shared data with this app. Turn on HUAWEI Health Kit in Huawei Health " +
                    "(Me > Privacy management) and check that this is the same HUAWEI ID."
            page.privacyStatus == PRIVACY_NOT_A_USER ->
                "This HUAWEI ID is not a Huawei Health user, so there is no workout data."
            clampedToAuthorization ->
                "No workouts in the readable range. Older workouts need a longer history scope (healthkit.historyWindow)."
            else -> null
        }
    }

    private companion object {
        val OVERVIEW_FEATURES = HealthFeature.entries - HealthFeature.WORKOUTS
        val DAY_MILLIS = TimeUnit.DAYS.toMillis(1)
        const val PRIVACY_NOT_GRANTED = 2
        const val PRIVACY_NOT_A_USER = 3
    }
}
