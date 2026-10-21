package com.example.healthkitdemo.push

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.healthkitdemo.MainActivity
import com.example.healthkitdemo.R

object PushNotifications {

    fun show(context: Context, title: String?, body: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(PushConfig.CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(PushConfig.CHANNEL_NAME)
                .build()
        )

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, PushConfig.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title ?: DEFAULT_TITLE)
            .setContentText(body ?: DEFAULT_BODY)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body ?: DEFAULT_BODY))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private const val DEFAULT_TITLE = "New workout"
    private const val DEFAULT_BODY = "A new workout is ready in Huawei Health."
}
