package com.example.healthkitdemo.healthkit.auth

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.browser.customtabs.CustomTabsIntent
import com.example.healthkitdemo.healthkit.HealthKitConfig
import com.example.healthkitdemo.healthkit.HealthKitScopes

class WebOAuthAuthorizer : HealthKitAuthorizer {

    override val tokenRedirectUri: String get() = HealthKitConfig.webRedirectUri

    override fun intent(context: Context, state: String): Intent {
        val url = HealthKitConfig.AUTHORIZE_URL.toUri().buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", HealthKitConfig.clientId)
            .appendQueryParameter("redirect_uri", tokenRedirectUri)
            .appendQueryParameter("scope", HealthKitScopes.DEFAULT.joinToString(" "))
            .appendQueryParameter("state", state)
            .appendQueryParameter("access_type", "offline")
            .appendQueryParameter("display", "touch")
            .build()

        return CustomTabsIntent.Builder().build().intent.setData(url)
    }
}
