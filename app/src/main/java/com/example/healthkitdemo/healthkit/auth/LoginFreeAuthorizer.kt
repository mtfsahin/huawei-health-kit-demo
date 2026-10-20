package com.example.healthkitdemo.healthkit.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.healthkitdemo.healthkit.HealthKitConfig
import com.example.healthkitdemo.healthkit.HealthKitScopes
import com.example.healthkitdemo.healthkit.errors.LoginFreeException
import com.huawei.hms.hihealthlite.HuaweiHiHealthLite

class LoginFreeAuthorizer : HealthKitAuthorizer {

    override val tokenRedirectUri: String = HealthKitConfig.LOGIN_FREE_CALLBACK_URL

    override fun intent(context: Context, state: String): Intent =
        HuaweiHiHealthLite.getLoginFreeAuthController(context).requestLoginFreeAuthorizationIntent(
            HealthKitScopes.DEFAULT.toTypedArray(),
            state,
            HealthKitConfig.clientId,
            HealthKitConfig.deepLink,
            HealthKitConfig.schemeSecret,
        )

    companion object {
        fun failureOf(resultCode: Int, data: Intent?): Throwable? {
            val code = data?.getIntExtra("DEEPLINK_RESULT", 0) ?: 0
            if (code != 0) return LoginFreeException(code)
            if (resultCode == Activity.RESULT_CANCELED) return AuthorizationDeniedException("The sign-in was closed before Huawei sent a code.")
            return null
        }
    }
}
