package com.lakshman.roadmaplab.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lakshman.roadmaplab.RoadmapLabApp
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.Leitner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/** Fires in the reminder window: books tomorrow's alarm, then reminds only if the evening log isn't done. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val repo = (context.applicationContext as RoadmapLabApp).repository
        receiverScope.launch {
            try {
                val settings = repo.settings.current()
                Reminders.schedule(context, settings)
                if (settings.reminderOn && settings.onboarded) {
                    val today = Days.today()
                    val dao = repo.db.dao()
                    val done = dao.dayLog(today)?.eveningDone == true
                    if (!done) {
                        val dueCards = dao.cardStates().count { it.dueDay <= today }.coerceAtMost(Leitner.DAILY_CAP)
                        Reminders.show(context, dueCards)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Alarms are cleared when the phone restarts or the app updates, so book the next one again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val repo = (context.applicationContext as RoadmapLabApp).repository
        receiverScope.launch {
            try {
                Reminders.schedule(context, repo.settings.current())
            } finally {
                pending.finish()
            }
        }
    }
}
