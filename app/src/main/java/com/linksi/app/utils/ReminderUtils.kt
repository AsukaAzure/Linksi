package com.linksi.app.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.linksi.app.R

const val CHANNEL_ID = "linksi_reminders"
const val EXTRA_LINK_ID = "link_id"
const val EXTRA_LINK_TITLE = "link_title"
const val EXTRA_LINK_URL = "link_url"

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Link Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders to read saved links"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}

fun scheduleReminder(context: Context, linkId: Long, title: String, url: String, timeMs: Long) {
    val alarmManager = context.getSystemService(AlarmManager::class.java)

    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra(EXTRA_LINK_ID, linkId)
        putExtra(EXTRA_LINK_TITLE, title)
        putExtra(EXTRA_LINK_URL, url)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        linkId.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pendingIntent)
        }
    } else {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pendingIntent)
    }
}

fun cancelReminder(context: Context, linkId: Long) {
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val intent = Intent(context, ReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        linkId.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_LINK_TITLE) ?: "Saved Link"
        val url = intent.getStringExtra(EXTRA_LINK_URL) ?: ""
        val linkId = intent.getLongExtra(EXTRA_LINK_ID, 0)

        createNotificationChannel(context)

        val openIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context, linkId.toInt(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📖 Time to read!")
            .setContentText(title.ifBlank { url })
            .setStyle(NotificationCompat.BigTextStyle().bigText(title.ifBlank { url }))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(linkId.toInt(), notification)
    }
}