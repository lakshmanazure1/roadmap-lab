package com.lakshman.roadmaplab.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.lakshman.roadmaplab.R
import com.lakshman.roadmaplab.domain.AppSettings
import java.time.ZonedDateTime

/**
 * At most one reminder a day, only if the evening log isn't done by your chosen time.
 * Uses an inexact alarm with a 15-minute window: exact alarms are denied by default on
 * Android 14, and a window is enough for a reminder.
 */
object Reminders {
    const val CHANNEL_ID = "evening_log"
    const val EXTRA_OPEN_TODAY = "com.lakshman.roadmaplab.OPEN_TODAY"
    const val WINDOW_MILLIS = 15 * 60 * 1000L
    private const val REQUEST_ALARM = 2001
    private const val REQUEST_OPEN = 2002
    private const val NOTIFICATION_ID = 1

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(CHANNEL_ID, "Evening log reminder", NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "One reminder a day, only when the evening log isn't done"
        manager.createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_ALARM, Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Books the next reminder (or cancels it when reminders are off). */
    fun schedule(context: Context, settings: AppSettings) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = alarmIntent(context)
        alarms.cancel(pending)
        if (!settings.reminderOn || !settings.onboarded) return
        val now = ZonedDateTime.now()
        var next = now.withHour(settings.reminderHour).withMinute(settings.reminderMinute).withSecond(0).withNano(0)
        if (!next.isAfter(now.plusSeconds(30))) next = next.plusDays(1)
        alarms.setWindow(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), WINDOW_MILLIS, pending)
    }

    fun show(context: Context, dueCards: Int) {
        if (!canNotify(context)) return
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        open.putExtra(EXTRA_OPEN_TODAY, true)
        val content = PendingIntent.getActivity(
            context, REQUEST_OPEN, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = if (dueCards > 0) "2–3 minutes: mark today's tasks, review $dueCards cards, one line of notes."
        else "2–3 minutes: mark today's tasks and write one line of notes."
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle("Evening log")
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(content)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
    }
}
