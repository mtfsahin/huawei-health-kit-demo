package com.example.healthkitdemo.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FcmService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        PushNotifications.show(
            context = this,
            title = message.notification?.title ?: message.data["title"],
            body = message.notification?.body ?: message.data["body"],
        )
    }

    override fun onNewToken(token: String) {
        WorkoutSubscriptions.onNewToken(this, PushRegistration(PushProvider.FCM, token))
    }
}
