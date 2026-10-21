package com.example.healthkitdemo.push

import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage

class HmsPushService : HmsMessageService() {

    override fun onMessageReceived(message: RemoteMessage) {
        PushNotifications.show(
            context = this,
            title = message.notification?.title ?: message.dataOfMap?.get("title"),
            body = message.notification?.body ?: message.dataOfMap?.get("body"),
        )
    }

    override fun onNewToken(token: String) {
        WorkoutSubscriptions.onNewToken(this, PushRegistration(PushProvider.HMS, token))
    }
}
