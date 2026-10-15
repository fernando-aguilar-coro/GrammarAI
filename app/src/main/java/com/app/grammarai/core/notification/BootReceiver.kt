package com.app.grammarai.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.app.grammarai.GrammarApplication

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = GrammarApplication.instance.preferencesManager
            if (prefs.dailyReminderEnabled) {
                val (hour, minute) = DailyReminderManager.parseReminderTime(prefs.dailyReminderTime)
                DailyReminderManager.scheduleDailyReminder(context, hour, minute)
                Log.d("BootReceiver", "Daily reminder successfully rescheduled on device boot for $hour:$minute")
            }
        }
    }
}
