package com.example.healthkitdemo.healthkit.token

import org.json.JSONObject

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String?,
    val expiresInSeconds: Long,
) {
    companion object {
        fun from(json: JSONObject): TokenResponse = TokenResponse(
            accessToken = json.getString("access_token"),
            refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() },
            expiresInSeconds = json.optLong("expires_in", 3600L),
        )
    }
}
