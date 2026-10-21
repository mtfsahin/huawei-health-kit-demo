package com.example.healthkitdemo

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthkitdemo.healthkit.HealthKitAuthMethod
import com.example.healthkitdemo.healthkit.HealthKitClient
import com.example.healthkitdemo.healthkit.HealthFeature
import com.example.healthkitdemo.healthkit.HealthKitConfig
import com.example.healthkitdemo.healthkit.HealthOverview
import com.example.healthkitdemo.healthkit.WorkoutRecord
import com.example.healthkitdemo.healthkit.auth.LoginFreeAuthorizer
import com.example.healthkitdemo.healthkit.errors.ErrorCatalog
import com.example.healthkitdemo.healthkit.errors.ErrorInfo
import com.example.healthkitdemo.log.AppLog
import com.example.healthkitdemo.log.LogCategory
import com.example.healthkitdemo.push.WorkoutSubscriptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val authMethod: HealthKitAuthMethod = HealthKitConfig.defaultAuthMethod,
    val isAuthorized: Boolean = false,
    val isConnecting: Boolean = false,
    val accountOpenId: String? = null,
    val overview: HealthOverview = HealthOverview(),
    val isLoadingOverview: Boolean = false,
    val workouts: List<WorkoutRecord> = emptyList(),
    val isLoadingWorkouts: Boolean = false,
    val lookbackDays: Long = HealthKitConfig.DEFAULT_LOOKBACK_DAYS,
    val workoutsNotice: String? = null,
    val privacyOff: Boolean = false,
    val dataAccess: Map<HealthFeature, ErrorInfo?> = emptyMap(),
    val error: ErrorInfo? = null,
)

class HealthKitViewModel(application: Application) : AndroidViewModel(application) {

    private val healthKit = HealthKitClient(application)
    private val subscriptions = WorkoutSubscriptions(application)
    private val errors = ErrorCatalog(application)
    private var overviewJob: Job? = null
    private var workoutsJob: Job? = null

    private val _uiState = MutableStateFlow(
        UiState(
            authMethod = healthKit.authMethod,
            isAuthorized = healthKit.isAuthorized,
            accountOpenId = healthKit.accountOpenId,
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        AppLog.info(LogCategory.APP, "App started", "connected ${healthKit.isAuthorized}, method ${healthKit.authMethod}")
        if (healthKit.isAuthorized) {
            refresh()
            enableWorkoutPush()
        }
    }

    fun selectAuthMethod(method: HealthKitAuthMethod) {
        healthKit.useMethod(method)
        _uiState.update { it.copy(authMethod = method, error = null) }
        AppLog.debug(LogCategory.AUTH, "Sign-in method set to ${method.label}")
    }

    fun authorizationIntent(context: Context): Intent? {
        _uiState.update { it.copy(isConnecting = true, error = null) }
        return healthKit.authorizationIntent(context)
            .onSuccess { AppLog.info(LogCategory.AUTH, "Sign-in started with ${_uiState.value.authMethod.label}") }
            .onFailure { fail(it, "Could not start the sign-in") }
            .getOrNull()
    }

    fun onLoginFreeResult(resultCode: Int, data: Intent?) {
        val failure = LoginFreeAuthorizer.failureOf(resultCode, data) ?: return
        fail(failure, "The login-free sign-in did not finish")
    }

    fun handleRedirect(uri: Uri) {
        if (!healthKit.ownsRedirect(uri)) return
        AppLog.info(LogCategory.AUTH, "Sign-in redirect received", "params ${uri.queryParameterNames.joinToString()}")

        viewModelScope.launch {
            healthKit.completeAuthorization(uri)
                .onSuccess {
                    _uiState.update {
                        it.copy(isConnecting = false, isAuthorized = true, accountOpenId = healthKit.accountOpenId, error = null)
                    }
                    AppLog.info(LogCategory.AUTH, "Huawei Health account connected", "openId ${healthKit.accountOpenId}")
                    refresh()
                    enableWorkoutPush()
                }
                .onFailure { fail(it, "Sign-in failed") }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(privacyOff = false) }
        loadOverview()
        loadWorkouts()
    }

    private fun loadOverview() {
        _uiState.update { it.copy(isLoadingOverview = true) }
        overviewJob?.cancel()
        overviewJob = viewModelScope.launch {
            healthKit.fetchOverview()
                .also { ensureActive() }
                .onSuccess { fetch ->
                    val access = (HealthFeature.entries - HealthFeature.WORKOUTS).associateWith { feature ->
                        fetch.failures[feature]?.let(errors::explain)
                    }
                    _uiState.update {
                        it.copy(
                            isLoadingOverview = false,
                            overview = fetch.overview,
                            privacyOff = it.privacyOff || fetch.privacyOff,
                            dataAccess = it.dataAccess + access,
                        )
                    }
                    logAccess(access, fetch.overview.days.size)
                    if (fetch.failures.size == access.size) fail(fetch.failures.values.first(), "Could not read any health data")
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingOverview = false) }
                    fail(error, "Could not load the health data")
                }
        }
    }

    fun selectLookbackDays(days: Long) {
        _uiState.update { it.copy(lookbackDays = days) }
        loadWorkouts()
    }

    fun loadWorkouts() {
        _uiState.update { it.copy(isLoadingWorkouts = true) }
        val days = _uiState.value.lookbackDays

        workoutsJob?.cancel()
        workoutsJob = viewModelScope.launch {
            healthKit.fetchWorkouts(days)
                .also { ensureActive() }
                .onSuccess { fetch ->
                    _uiState.update {
                        it.copy(
                            isLoadingWorkouts = false,
                            workouts = fetch.records,
                            workoutsNotice = fetch.notice,
                            privacyOff = it.privacyOff || fetch.needsPrivacy,
                            dataAccess = it.dataAccess + (HealthFeature.WORKOUTS to null),
                        )
                    }
                    AppLog.info(LogCategory.APP, "Read ${fetch.records.size} workout(s) of the last $days days", fetch.notice)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoadingWorkouts = false, dataAccess = it.dataAccess + (HealthFeature.WORKOUTS to errors.explain(error)))
                    }
                    fail(error, "Could not read the workouts")
                }
        }
    }

    private fun logAccess(access: Map<HealthFeature, ErrorInfo?>, days: Int) {
        val readable = access.filterValues { it == null }.keys
        AppLog.info(LogCategory.API, "Read ${readable.size} of ${access.size} data types for $days days", readable.joinToString { it.title })
        access.forEach { (feature, error) ->
            if (error == null) return@forEach
            AppLog.warn(
                LogCategory.API,
                "${feature.title} is not readable: ${error.title}",
                "scope ${feature.scope}\napi ${feature.api} ${feature.dataType}\ncode ${error.label}\nfix ${error.solution}",
            )
        }
    }

    private fun enableWorkoutPush() {
        viewModelScope.launch {
            val accessToken = healthKit.accessToken().getOrElse {
                AppLog.warn(LogCategory.PUSH, "Workout notifications need a valid session", it.message)
                return@launch
            }
            subscriptions.enable(accessToken)
                .onSuccess { AppLog.info(LogCategory.PUSH, "Workout notifications are on", "provider ${it.wireName}") }
                .onFailure { AppLog.warn(LogCategory.PUSH, "Workout notifications are off", it.message) }
        }
    }

    private suspend fun stopWorkoutPush(): Result<Unit> {
        val accessToken = healthKit.accessToken().getOrNull() ?: return Result.success(Unit)
        return subscriptions.disable(accessToken)
    }

    fun disconnect() {
        viewModelScope.launch {
            stopWorkoutPush().onFailure {
                fail(it, "Could not turn off workout notifications")
                return@launch
            }
            healthKit.signOut()
            _uiState.value = UiState(authMethod = healthKit.authMethod)
            AppLog.info(LogCategory.AUTH, "Disconnected the Huawei Health account")
        }
    }

    fun deleteData() {
        viewModelScope.launch {
            stopWorkoutPush().onFailure {
                fail(it, "Could not turn off workout notifications")
                return@launch
            }
            healthKit.cancelAuthorization()
                .onSuccess {
                    _uiState.value = UiState(authMethod = healthKit.authMethod)
                    AppLog.info(LogCategory.AUTH, "Cancelled the authorization and deleted the app's data")
                }
                .onFailure { fail(it, "Could not delete the data") }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null, privacyOff = false) }
    }

    fun showMessage(message: String) {
        AppLog.warn(LogCategory.APP, message)
    }

    private fun fail(error: Throwable, action: String) {
        val info = errors.explain(error)
        _uiState.update {
            it.copy(
                isConnecting = false,
                isAuthorized = healthKit.isAuthorized,
                error = info,
            )
        }
        AppLog.error(
            LogCategory.APP,
            "$action: ${info.title}",
            "code ${info.label}\ncause ${info.cause}\nfix ${info.solution}" + (info.url?.let { "\ndocs $it" } ?: ""),
        )
    }
}
