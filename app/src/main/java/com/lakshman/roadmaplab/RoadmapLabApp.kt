package com.lakshman.roadmaplab

import android.app.Application
import com.lakshman.roadmaplab.data.LabRepository
import com.lakshman.roadmaplab.data.SettingsStore
import com.lakshman.roadmaplab.data.db.LabDatabase
import com.lakshman.roadmaplab.reminder.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RoadmapLabApp : Application() {
    lateinit var repository: LabRepository
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        repository = LabRepository(this, LabDatabase.build(this), SettingsStore(this))
        Reminders.createChannel(this)
        appScope.launch {
            repository.loadPlan()
            Reminders.schedule(this@RoadmapLabApp, repository.settings.current())
        }
    }
}
