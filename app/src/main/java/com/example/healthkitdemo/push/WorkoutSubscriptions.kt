package com.example.healthkitdemo.push

import android.content.Context
import com.example.healthkitdemo.healthkit.HealthKitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WorkoutSubscriptions(private val context: Context) {

    suspend fun enable(accessToken: String): Result<PushProvider> {
        val registration = PushTokens.fetch(context)
            ?: return Result.failure(IllegalStateException("No push token on this phone"))

        return BackendApi.subscribe(accessToken, registration).map { registration.provider }
    }

    suspend fun disable(accessToken: String): Result<Unit> = BackendApi.unsubscribe(accessToken)

    companion object {
        fun onNewToken(context: Context, registration: PushRegistration) {
            CoroutineScope(Dispatchers.IO).launch {
                val client = HealthKitClient(context.applicationContext)
                if (!client.isAuthorized) return@launch
                client.accessToken().onSuccess { BackendApi.subscribe(it, registration) }
            }
        }
    }
}
