package com.example.healthkitdemo.healthkit.token

import com.example.healthkitdemo.healthkit.HealthKitConfig
import com.example.healthkitdemo.healthkit.net.Http
import com.example.healthkitdemo.healthkit.net.requireSuccess
import org.json.JSONObject

class HealthKitTokenClient {

    suspend fun exchangeCode(code: String, redirectUri: String): Result<TokenResponse> = runCatching {
        val body = JSONObject().put("code", code).put("redirectUri", redirectUri)
        TokenResponse.from(Http.postJson("${HealthKitConfig.backendUrl}/oauth/token", body).requireSuccess().json())
    }

    suspend fun refresh(refreshToken: String): Result<TokenResponse> = runCatching {
        val body = JSONObject().put("refreshToken", refreshToken)
        TokenResponse.from(Http.postJson("${HealthKitConfig.backendUrl}/oauth/refresh", body).requireSuccess().json())
    }

    suspend fun openId(accessToken: String): Result<String?> = runCatching {
        val form = mapOf("open_id" to "OPENID", "access_token" to accessToken)
        Http.postForm(HealthKitConfig.TOKEN_INFO_URL, form).requireSuccess().json()
            .optString("open_id").takeIf { it.isNotBlank() }
    }
}
