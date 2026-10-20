package com.example.healthkitdemo.push

import com.example.healthkitdemo.healthkit.HealthKitConfig
import com.example.healthkitdemo.healthkit.net.Http
import com.example.healthkitdemo.healthkit.net.requireSuccess
import org.json.JSONObject

object BackendApi {

    suspend fun subscribe(accessToken: String, registration: PushRegistration): Result<Unit> = runCatching {
        val body = JSONObject()
            .put("accessToken", accessToken)
            .put("pushToken", registration.token)
            .put("provider", registration.provider.wireName)
        Http.postJson("${HealthKitConfig.backendUrl}/subscriptions", body).requireSuccess()
    }

    suspend fun unsubscribe(accessToken: String): Result<Unit> = runCatching {
        Http.delete("${HealthKitConfig.backendUrl}/subscriptions", accessToken).requireSuccess()
    }
}
