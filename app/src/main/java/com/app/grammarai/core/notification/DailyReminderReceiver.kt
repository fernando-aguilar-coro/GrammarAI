package com.app.grammarai.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.app.grammarai.MainActivity
import com.app.grammarai.R
import com.app.grammarai.GrammarApplication

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = GrammarApplication.instance.preferencesManager
        if (!prefs.dailyReminderEnabled) return

        val isEs = prefs.appLanguage.equals("es", ignoreCase = true)
        val title = if (isEs) {
            "¡Momento de tu práctica de inglés! 🚀"
        } else {
            "Time for your daily English practice! 🚀"
        }
        val text = if (isEs) {
            "Solo 5 minutos hoy para avanzar en tu objetivo de lecciones completadas. 🎯"
        } else {
            "Just 5 minutes today to make progress on your completed lessons goal! 🎯"
        }

        createNotificationChannel(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission not granted on Android 13+
        }

        // Reschedule for next day using the user's customized reminder time
        val (hour, minute) = DailyReminderManager.parseReminderTime(prefs.dailyReminderTime)
        DailyReminderManager.scheduleDailyReminder(context, hour, minute)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily English Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily reminders to practice English lessons and speaking"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "daily_english_reminder_channel"
        const val NOTIFICATION_ID = 2001
    }
}
