package com.example.foodsafevision.util

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.foodsafevision.data.model.FoodEntity
import com.example.foodsafevision.receiver.NotificationReceiver
import java.util.Calendar

class NotificationHelper(private val context: Context) {
    private val channelId = "expiry_notification"
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "유통기한 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "유통기한 임박 알림"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNotification(food: FoodEntity, daysUntilExpiry: Long) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("foodName", food.foodName)
            putExtra("daysUntilExpiry", daysUntilExpiry)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            food.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
            pendingIntent
        )
    }
}
