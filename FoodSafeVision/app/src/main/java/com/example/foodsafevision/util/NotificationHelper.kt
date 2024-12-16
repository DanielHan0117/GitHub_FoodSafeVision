package com.example.foodsafevision.util

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.foodsafevision.data.model.FoodEntity
import com.example.foodsafevision.receiver.NotificationReceiver
import java.time.LocalDate
import java.time.temporal.ChronoUnit
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

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNotification(
        food: FoodEntity,
        dDayPeriod: Int,
        isEnabled: Boolean,
        hour: Int,
        minute: Int
    ) {
        if (!isEnabled) {
            cancelExistingAlarm(food.id.toInt())
            return
        }

        val currentDate = LocalDate.now()
        val expirationDate = LocalDate.parse(food.expirationDate)
        val daysUntilExpiry = ChronoUnit.DAYS.between(currentDate, expirationDate)

        if (daysUntilExpiry > dDayPeriod.toLong()) {
            cancelExistingAlarm(food.id.toInt())
            return
        }

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
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)

            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        if (daysUntilExpiry >= 0) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
                pendingIntent
            )
        }
    }

    private fun cancelExistingAlarm(requestCode: Int) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
}