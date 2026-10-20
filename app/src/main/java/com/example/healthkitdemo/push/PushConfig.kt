package com.example.healthkitdemo.push

import com.example.healthkitdemo.BuildConfig

enum class PushProvider(val wireName: String) {
    FCM("fcm"),
    HMS("hms"),
}

object PushConfig {
    val hmsAppId: String = BuildConfig.HEALTH_KIT_CLIENT_ID

    const val CHANNEL_ID = "workouts"
    const val CHANNEL_NAME = "Workout updates"
}
