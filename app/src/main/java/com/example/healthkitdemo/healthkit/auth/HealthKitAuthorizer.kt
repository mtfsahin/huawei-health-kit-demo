package com.example.healthkitdemo.healthkit.auth

import android.content.Context
import android.content.Intent

interface HealthKitAuthorizer {
    val tokenRedirectUri: String

    fun intent(context: Context, state: String): Intent
}
