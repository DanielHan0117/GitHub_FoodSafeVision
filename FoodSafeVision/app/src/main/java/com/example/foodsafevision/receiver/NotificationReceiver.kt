package com.example.foodsafevision.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import android.app.NotificationManager

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val foodName = intent.getStringExtra("foodName") ?: ""
        val daysUntilExpiry = intent.getLongExtra("daysUntilExpiry", 0)

        val notification = NotificationCompat.Builder(context, "expiry_notification")
            .setContentTitle("유통기한 임박 알림")
            .setContentText("${foodName}의 유통기한이 ${daysUntilExpiry}일 남았습니다.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
