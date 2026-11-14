package com.lakshman.roadmaplab.ui

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.BuildConfig
import com.lakshman.roadmaplab.data.json.LabFileException
import com.lakshman.roadmaplab.data.json.LabJson
import com.lakshman.roadmaplab.data.json.Snapshot
import com.lakshman.roadmaplab.domain.Card
import com.lakshman.roadmaplab.domain.Mode
import com.lakshman.roadmaplab.domain.Roadmap
import com.lakshman.roadmaplab.domain.plural
import com.lakshman.roadmaplab.reminder.Reminders
import com.lakshman.roadmaplab.ui.components.BackBar
import com.lakshman.roadmaplab.ui.components.Choice
import com.lakshman.roadmaplab.ui.components.ConfirmDialog
import com.lakshman.roadmaplab.ui.components.Label
import com.lakshman.roadmaplab.ui.components.LabOutlinedButton
import com.lakshman.roadmaplab.ui.components.MonoText
import com.lakshman.roadmaplab.ui.components.Muted
import com.lakshman.roadmaplab.ui.components.Panel
import com.lakshman.roadmaplab.ui.theme.LabType
import com.lakshman.roadmaplab.ui.theme.LocalLab

@Composable
fun SettingsScreen(ui: UiState, vm: LabViewModel, onBack: () -> Unit, onBackup: () -> Unit) {
    val lab = LocalLab.current
    val context = LocalContext.current
    val s = ui.s.settings
    var pendingRestore by remember { mutableStateOf<Snapshot?>(null) }
    var pendingRoadmap by remember { mutableStateOf<Roadmap?>(null) }
    var pendingCards by remember { mutableStateOf<Pair<String, List<Card>>?>(null) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.updateSettings { it.copy(reminderOn = true) }
        else toast(context, "Allow notifications for Roadmap Lab in your phone's settings to get the reminder.")
    }
    fun readOr(text: String?, parse: (String) -> Unit) {
        if (text == null) return
        try {
            parse(text)
        } catch (e: LabFileException) {
            toast(context, e.message ?: "That file couldn't be read.")
        } catch (e: Exception) {
            toast(context, "That file couldn't be read.")
        }
    }
    val pickBackup = rememberOpenTextFile { text -> readOr(text) { pendingRestore = LabJson.decodeBackup(it) } }
    val pickRoadmap = rememberOpenTextFile { text -> readOr(text) { pendingRoadmap = LabJson.parseRoadmap(it) } }
    val pickCards = rememberOpenTextFile { text -> readOr(text) { pendingCards = LabJson.parseCards(it) } }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { BackBar("Settings", onBack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Panel {
                Label("Mode")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Mode.entries.forEach { m ->
                        Choice(m.label, s.mode == m, onClick = {
                            vm.updateSettings { it.copy(mode = m) }
                            vm.launch { replanToday() }
                        }, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Muted("Daily capacity: India about 120 min, DRC about 60 min, heavy week about 15 min. At most 3 tasks a day.")
                Spacer(Modifier.height(12.dp))
                SwitchRow("Reduced plan", "Sets optional tasks aside. Your roadmap's rule: switch, don't quit.", s.reducedPlan) { on ->
                    vm.updateSettings { it.copy(reducedPlan = on) }
                    vm.launch { replanToday() }
                }
                SwitchRow("Evidence log on Today", "Date | Module | Problem | Action | Result | Learning", s.showEvidenceLog) { on ->
                    vm.updateSettings { it.copy(showEvidenceLog = on) }
                }
            }

            Panel {
                Label("Evening reminder")
                Spacer(Modifier.height(4.dp))
                SwitchRow("Remind me", "At most one a day, only if the evening log isn't done. Arrives within 15 minutes of the time.", s.reminderOn) { on ->
                    if (on && Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(context)) {
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        vm.updateSettings { it.copy(reminderOn = on) }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable {
                        TimePickerDialog(context, { _, h, m -> vm.updateSettings { it.copy(reminderHour = h, reminderMinute = m) } },
                            s.reminderHour, s.reminderMinute, true).show()
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Time", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    MonoText(Fmt.clock(s.reminderHour, s.reminderMinute), style = LabType.number, color = lab.accent)
                }
            }

            Panel {
                Label("Theme")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("soft" to "Light", "dark" to "Dark", "system" to "Phone").forEach { (key, name) ->
                        Choice(name, s.theme == key, onClick = { vm.updateSettings { it.copy(theme = key) } }, modifier = Modifier.weight(1f))
                    }
                }
            }

            Panel {
                Label("Backup")
                Spacer(Modifier.height(6.dp))
                Text("Back up to Drive every Sunday.", style = MaterialTheme.typography.bodyMedium)
                Muted("Saves a dated file such as RoadmapLab_backup_${java.time.LocalDate.now()}.json. In the next screen tap ☰, choose Drive, open your backup folder and tap Save. Older backups stay.")
                Spacer(Modifier.height(4.dp))
                MonoText("Last backup: " + (if (s.lastBackupDay > 0) Fmt.full(s.lastBackupDay) else "never"), style = LabType.monoSmall, color = lab.muted)
                Spacer(Modifier.height(10.dp))
                LabOutlinedButton("Back up to Drive", onClick = onBackup, color = lab.accent, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                LabOutlinedButton("Restore from a backup file", onClick = pickBackup, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Muted("Android's own phone backup is also on, as a safety net.")
            }

            Panel {
                Label("Roadmap files")
                Spacer(Modifier.height(6.dp))
                MonoText("roadmap.json · ${ui.s.roadmap?.version ?: "–"} · ${ui.s.roadmap?.tasks?.size ?: 0} tasks", style = LabType.monoSmall)
                MonoText("cards.json · ${ui.s.cardsVersion.ifBlank { "–" }} · ${ui.s.cards.size} cards", style = LabType.monoSmall)
                Spacer(Modifier.height(8.dp))
                Muted("When the plan changes, import the new file Claude gives you. Your logs, card progress and history stay.")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabOutlinedButton("Import roadmap.json", onClick = pickRoadmap, modifier = Modifier.weight(1f))
                    LabOutlinedButton("Import cards.json", onClick = pickCards, modifier = Modifier.weight(1f))
                }
            }

            Panel {
                Label("About")
                Spacer(Modifier.height(6.dp))
                MonoText("Roadmap Lab v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})", style = LabType.mono)
                Spacer(Modifier.height(6.dp))
                Muted("No analytics, no account, no server. The app has no internet permission, so it can't send anything anywhere; backups go only where you save them. Never enter company or client data.")
                Spacer(Modifier.height(6.dp))
                Muted("Fonts: IBM Plex, SIL Open Font License.")
            }
        }
    }

    pendingRestore?.let { b ->
        ConfirmDialog(
            title = "Restore this backup?",
            message = "Everything on this phone will be replaced with the backup: ${plural(b.taskLogs.size, "task log")}, " +
                "${plural(b.dayLogs.size, "day log")}, ${plural(b.cardStates.size, "card")} in review, and its settings.",
            confirm = "Restore",
            destructive = true,
            onDismiss = { pendingRestore = null },
            onConfirm = {
                pendingRestore = null
                vm.launch {
                    restore(b)
                    Reminders.schedule(context, settings.current())
                }
                toast(context, "Backup restored")
            },
        )
    }
    pendingRoadmap?.let { r ->
        ConfirmDialog(
            title = "Import this roadmap?",
            message = "Version ${r.version.ifBlank { "(none)" }} with ${plural(r.tasks.size, "task")}. The plan is replaced; your logs and history stay.",
            confirm = "Import",
            onDismiss = { pendingRoadmap = null },
            onConfirm = {
                pendingRoadmap = null
                vm.launch { importRoadmap(r) }
                toast(context, "Roadmap updated")
            },
        )
    }
    pendingCards?.let { (version, cards) ->
        ConfirmDialog(
            title = "Import these cards?",
            message = "Version ${version.ifBlank { "(none)" }} with ${plural(cards.size, "card")}. Progress on cards you already have is kept.",
            confirm = "Import",
            onDismiss = { pendingCards = null },
            onConfirm = {
                pendingCards = null
                vm.launch { importCards(version, cards) }
                toast(context, "Cards updated")
            },
        )
    }
}

@Composable
private fun SwitchRow(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Muted(detail)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
