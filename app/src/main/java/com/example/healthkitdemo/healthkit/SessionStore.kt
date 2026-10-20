package com.example.healthkitdemo.healthkit

import android.content.Context
import androidx.core.content.edit
import com.example.healthkitdemo.healthkit.token.TokenResponse

class SessionStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("healthkit_session", Context.MODE_PRIVATE)

    var authMethod: HealthKitAuthMethod
        get() = prefs.getString(KEY_METHOD, null)
            ?.let { name -> HealthKitAuthMethod.entries.firstOrNull { it.name == name } }
            ?: HealthKitConfig.defaultAuthMethod
        set(value) = prefs.edit { putString(KEY_METHOD, value.name) }

    var pendingState: String?
        get() = prefs.getString(KEY_STATE, null)
        set(value) = prefs.edit { putString(KEY_STATE, value) }

    var pendingRedirectUri: String?
        get() = prefs.getString(KEY_REDIRECT_URI, null)
        set(value) = prefs.edit { putString(KEY_REDIRECT_URI, value) }

    var site: String
        get() = prefs.getString(KEY_SITE, null) ?: HealthKitConfig.apiHost
        set(value) = prefs.edit { putString(KEY_SITE, value) }

    var openId: String?
        get() = prefs.getString(KEY_OPEN_ID, null)
        set(value) = prefs.edit { putString(KEY_OPEN_ID, value) }

    val accessToken: String? get() = prefs.getString(KEY_ACCESS_TOKEN, null)
    val refreshToken: String? get() = prefs.getString(KEY_REFRESH_TOKEN, null)
    val authorizedAtMillis: Long get() = prefs.getLong(KEY_AUTHORIZED_AT, 0L)
    val isAuthorized: Boolean get() = refreshToken != null

    fun hasValidAccessToken(): Boolean =
        accessToken != null && System.currentTimeMillis() < prefs.getLong(KEY_EXPIRES_AT, 0L) - EXPIRY_MARGIN_MS

    fun save(tokens: TokenResponse) = prefs.edit {
        putString(KEY_ACCESS_TOKEN, tokens.accessToken)
        putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + tokens.expiresInSeconds * 1000)
        tokens.refreshToken?.let { putString(KEY_REFRESH_TOKEN, it) }
        if (!prefs.contains(KEY_AUTHORIZED_AT)) putLong(KEY_AUTHORIZED_AT, System.currentTimeMillis())
    }

    fun clearSession() = prefs.edit {
        listOf(KEY_ACCESS_TOKEN, KEY_REFRESH_TOKEN, KEY_EXPIRES_AT, KEY_AUTHORIZED_AT, KEY_OPEN_ID, KEY_SITE, KEY_STATE, KEY_REDIRECT_URI)
            .forEach(::remove)
    }

    private companion object {
        const val KEY_METHOD = "auth_method"
        const val KEY_STATE = "pending_state"
        const val KEY_REDIRECT_URI = "pending_redirect_uri"
        const val KEY_SITE = "api_host"
        const val KEY_OPEN_ID = "open_id"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_AUTHORIZED_AT = "authorized_at"
        const val EXPIRY_MARGIN_MS = 60_000L
    }
}
