package com.lakshman.roadmaplab.ui

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.BuildConfig
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.ui.components.Chip
import com.lakshman.roadmaplab.ui.components.ChipRow
import com.lakshman.roadmaplab.ui.components.ConfirmDialog
import com.lakshman.roadmaplab.ui.components.Field
import com.lakshman.roadmaplab.ui.components.KeyValue
import com.lakshman.roadmaplab.ui.components.LabCard
import com.lakshman.roadmaplab.ui.components.Muted
import com.lakshman.roadmaplab.ui.components.PrimaryButton
import com.lakshman.roadmaplab.ui.components.ScreenPadding
import com.lakshman.roadmaplab.ui.components.SecondaryButton
import com.lakshman.roadmaplab.ui.components.SectionTitle
import com.lakshman.roadmaplab.ui.components.Stepper
import com.lakshman.roadmaplab.ui.components.TopBar
import com.lakshman.roadmaplab.ui.theme.LocalPalette
import com.lakshman.roadmaplab.ui.theme.Type

@Composable
fun SettingsScreen(ui: UiState, act: Actions, nav: Nav, backup: BackupHooks) {
    val p = LocalPalette.current
    val s = ui.s.settings
    val context = LocalContext.current
    var cue by remember { mutableStateOf(s.cue) }
    var confirmRestore by remember { mutableStateOf(false) }
    var confirmFresh by remember { mutableStateOf(false) }
    var confirmBringBack by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        act.updateSettings { it.copy(reminderOn = granted) }
        if (!granted) act.note(Note("Notifications are off for this app. You can allow them in Android settings."))
    }
    Column(Modifier.fillMaxSize()) {
        TopBar("Settings", nav.back)
        LazyColumn(contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { SectionTitle("Look") }
            item {
                LabCard {
                    ChipRow {
                        Chip("Light", s.theme == "light", { act.updateSettings { it.copy(theme = "light") } })
                        Chip("Dark", s.theme == "dark", { act.updateSettings { it.copy(theme = "dark") } })
                        Chip("Same as phone", s.theme == "system", { act.updateSettings { it.copy(theme = "system") } })
                    }
                    val supported = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Colours from my wallpaper", style = Type.bodyStrong, color = LocalPalette.current.text)
                            Muted(if (supported) "Material You: the app takes its colours from your wallpaper." else "Needs Android 12 or later.")
                        }
                        Switch(checked = s.dynamicColor && supported, onCheckedChange = { v -> act.updateSettings { it.copy(dynamicColor = v) } },
                            enabled = supported)
                    }
                }
            }
            item { SectionTitle("Daily minimum", trailing = "what makes a day count") }
            item {
                LabCard {
                    Muted("Minutes of planned study you must finish for the day to count in your streak. Health habits and opening the app don't count. " +
                        "Keep them small: the plan's reduced routine is about 20 minutes.")
                    val t = s.targets
                    Stepper(t.weekday, { v -> act.updateSettings { it.copy(targets = it.targets.copy(weekday = v)) } }, 5, 120, 5, "min", "Weekday")
                    Stepper(t.saturday, { v -> act.updateSettings { it.copy(targets = it.targets.copy(saturday = v)) } }, 5, 180, 5, "min", "Saturday")
                    Stepper(t.sunday, { v -> act.updateSettings { it.copy(targets = it.targets.copy(sunday = v)) } }, 5, 180, 5, "min", "Sunday")
                    Stepper(t.reduced, { v -> act.updateSettings { it.copy(targets = it.targets.copy(reduced = v)) } }, 5, 60, 5, "min", "Reduced routine")
                    Stepper(t.catchup, { v -> act.updateSettings { it.copy(targets = it.targets.copy(catchup = v)) } }, 5, 180, 5, "min", "Catch-up day")
                }
            }
            item { SectionTitle("Cue and reminder") }
            item {
                LabCard {
                    Field(cue, { cue = it }, "Your stable cue (a time or routine)", supporting = "e.g. “After dinner, at my desk”. Same cue every day builds the habit.")
                    if (cue != s.cue) SecondaryButton("Save cue", { act.updateSettings { it.copy(cue = cue.trim()) } }, Modifier.padding(top = 8.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Daily reminder", style = Type.bodyStrong, color = p.text)
                            Muted("One a day, only if today's minimum isn't met. Quiet on rest days.")
                        }
                        Switch(s.reminderOn, { on ->
                            if (on && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else act.updateSettings { it.copy(reminderOn = on) }
                        }, colors = SwitchDefaults.colors(checkedTrackColor = p.accent))
                    }
                    if (s.reminderOn) {
                        SecondaryButton("Time: %02d:%02d".format(s.reminderHour, s.reminderMinute), {
                            TimePickerDialog(context, { _, h, m -> act.updateSettings { it.copy(reminderHour = h, reminderMinute = m) } },
                                s.reminderHour, s.reminderMinute, true).show()
                        }, Modifier.padding(top = 8.dp))
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("On Sundays, remind me of the weekly review", style = Type.body, color = p.text, modifier = Modifier.weight(1f))
                            Switch(s.weeklyReminder, { on -> act.updateSettings { it.copy(weeklyReminder = on) } },
                                colors = SwitchDefaults.colors(checkedTrackColor = p.accent))
                        }
                    }
                }
            }
            item { SectionTitle("Backup and restore") }
            item {
                LabCard {
                    Muted(if (s.lastBackupDay > 0) "Last backup: ${Days.short(s.lastBackupDay)} (${Days.relative(s.lastBackupDay, ui.today)})" else "No backup yet.")
                    Muted("Saves one dated file (RoadmapLab_backup_YYYY-MM-DD.json). Choose Google Drive in the save screen to keep it off the phone.",
                        Modifier.padding(top = 4.dp))
                    PrimaryButton("Back up now", backup.backupNow, Modifier.fillMaxWidth().padding(top = 10.dp))
                    SecondaryButton("Restore from a backup file", { confirmRestore = true }, Modifier.fillMaxWidth().padding(top = 8.dp))
                    SecondaryButton("Export the evidence log (text)", backup.exportEvidence, Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
            item { SectionTitle("Start fresh") }
            item {
                LabCard {
                    Muted(if (s.freshStartDay > 0) "You started fresh on ${Days.short(s.freshStartDay)}. Days before it count as \"nothing scheduled\", never as missed."
                        else "Clears every record (ticks, scores, streak days, log lines, flashcard progress) and counts from today. The plan and your settings stay.")
                    SecondaryButton("Start fresh from today", { confirmFresh = true }, Modifier.fillMaxWidth().padding(top = 8.dp))
                    if (backup.latestCopy != null) {
                        Muted("Safety copy on this phone: ${backup.latestCopy}. Bringing it back replaces what's here now " +
                            "(and saves a safety copy of that first).", Modifier.padding(top = 10.dp))
                        SecondaryButton("Bring back the safety copy", { confirmBringBack = true }, Modifier.fillMaxWidth().padding(top = 8.dp))
                    }
                }
            }
            item { SectionTitle("Plan file") }
            item {
                LabCard {
                    KeyValue("Plan", "${ui.plan.meta.title}\n${ui.plan.csvTasks.size} tasks · ${if (backup.usingImportedPlan) "your imported CSV" else ui.plan.meta.csvName}")
                    Muted("Import a CSV with the same columns (for example a February–March plan). Your ticks and scores stay: they're linked by task id.")
                    SecondaryButton("Import a plan CSV", backup.importPlan, Modifier.fillMaxWidth().padding(top = 8.dp))
                    if (backup.usingImportedPlan) SecondaryButton("Use the bundled plan again", backup.useBundledPlan, Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
            item { SectionTitle("Your data and privacy") }
            item {
                LabCard {
                    Text("Where your data lives", style = Type.bodyStrong, color = p.text)
                    Muted("Only on this phone, in the app's private storage. The app has no internet permission, so it can't send anything anywhere. " +
                        "Nobody else can read it unless they have your unlocked phone.")
                    Text("Who can see backups", style = Type.bodyStrong, color = p.text, modifier = Modifier.padding(top = 8.dp))
                    Muted("A backup or export file is plain text. Anyone who can open the place you save it (for example your Google Drive) can read it. " +
                        "Android's own phone backup may also copy app data to your Google account.")
                    Text("What never goes in", style = Type.bodyStrong, color = p.text, modifier = Modifier.padding(top = 8.dp))
                    Muted("Client names, PAN, GSTIN, bank details, invoices, company source code or internal documents. Use “the client” and fictional data. " +
                        "The evidence log warns you if text looks like a PAN, GSTIN, IFSC, account number, phone number or e-mail.")
                    if (ui.s.legacyTaskLogs > 0) Muted("Kept from version 1: ${ui.s.legacyTaskLogs} task records from the October roadmap (in backups).",
                        Modifier.padding(top = 8.dp))
                }
            }
            item { SecondaryButton("How levels and streaks work", { nav.open(Screen.HowItWorks) }, Modifier.fillMaxWidth()) }
            item { Muted("Roadmap Lab ${BuildConfig.VERSION_NAME} · plan ${ui.plan.meta.version} · dates use Africa/Kinshasa time") }
        }
    }
    if (confirmFresh) FreshStartDialog({ confirmFresh = false; act.startFresh() }, { confirmFresh = false })
    if (confirmBringBack) ConfirmDialog(
        "Bring back the safety copy?",
        "What's on the phone now is replaced by the copy from ${backup.latestCopy}. A safety copy of what's here now is saved first.",
        "Bring back", { confirmBringBack = false; backup.bringBack() }, { confirmBringBack = false },
    )
    if (confirmRestore) ConfirmDialog(
        "Restore from a backup?",
        "Everything on the phone is replaced by the file's contents. Before that, a safety copy of what's here now is saved inside the app.",
        "Choose file", { confirmRestore = false; backup.restore() }, { confirmRestore = false },
    )
}

/** Plain-language rules for levels, streaks and the plan's order. Transparent on purpose. */
@Composable
fun HowItWorksScreen(ui: UiState, nav: Nav) {
    Column(Modifier.fillMaxSize()) {
        TopBar("How it works", nav.back)
        LazyColumn(contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                LabCard {
                    Text("Follow the order, not the dates", style = Type.section, color = LocalPalette.current.text)
                    Muted("SAP Fridays, accounting, French, Python, career steps and Sunday recordings each show the next task you haven't done, on the days " +
                        "that category uses. A missed day moves the next task one slot later; nothing is lost, and it never doubles up. Day-bound tasks (the " +
                        "log line, work habits, SAP flashcards, health, daily practice) belong to their day. Dated steps (STAR stories, money checks, " +
                        "trainings, the December conversation, reviews) wait until done.")
                    ui.plan.meta.recovery.forEach { Muted("• $it", Modifier.padding(top = 4.dp)) }
                }
            }
            item {
                LabCard {
                    Text("The 7 levels (section 3)", style = Type.section, color = LocalPalette.current.text)
                    ui.plan.meta.levels.forEachIndexed { i, (name, meaning) -> Muted("${i + 1} $name: $meaning", Modifier.padding(top = 4.dp)) }
                    Muted("Needs revision: your latest marked check is below its target. Retaking it adds a new attempt; the old ones stay so you can see improvement. " +
                        "Mastered needs a passing check again at least 14 days after the first pass.", Modifier.padding(top = 6.dp))
                }
            }
            item {
                LabCard {
                    Text("Streak and consistency", style = Type.section, color = LocalPalette.current.text)
                    listOf(
                        "A day counts when your completed study reaches that day's minimum (Settings). Opening the app or ticking a health habit adds nothing.",
                        "Planned rest days (25 Dec, 1 Jan and any you plan ahead) and reduced-routine days never break the streak.",
                        "The first missed day in each Monday–Sunday week is forgiven: the streak pauses instead of resetting.",
                        "Today is never a miss while it's still open.",
                        "The longest streak and every past day are kept, even after a streak ends.",
                        "7- and 28-day consistency = days that met the minimum ÷ days that had work scheduled.",
                        "Dates follow Africa/Kinshasa time, so travel can't split or merge a day.",
                    ).forEach { Muted("• $it", Modifier.padding(top = 4.dp)) }
                }
            }
            item {
                LabCard {
                    Text("Category streaks", style = Type.section, color = LocalPalette.current.text)
                    listOf(
                        "Each of the 10 categories has its own streak: the days in a row you did at least one of its tasks.",
                        "Days a category has nothing planned don't break its streak (accounting has no Friday; SAP has only Tuesday and Friday).",
                        "Rest days, days before your fresh start and reduced-routine days without an action for it are neutral.",
                        "Near-daily categories forgive one missed day a week. Weekly ones (SAP, career, review) count each planned day.",
                        "Forgot to tick? The Yesterday card on Today, and each category's page (last 7 days), let you tick a task for the day it was planned. " +
                            "Marking a task done also lets you pick the day you did it.",
                        "% done = tasks done ÷ all its tasks. On track = done ÷ due by today. Health and daily practice are habits: never counted as study.",
                    ).forEach { Muted("• $it", Modifier.padding(top = 4.dp)) }
                }
            }
        }
    }
}

/** First run of 2.0: what changed, where data lives, the cue. One screen, then straight to Today. */
@Composable
fun IntroScreen(ui: UiState, act: Actions) {
    val p = LocalPalette.current
    var cue by remember { mutableStateOf(ui.s.settings.cue) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(ScreenPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.padding(top = 32.dp)) {
                Text("Your Fresh Start plan is ready", style = Type.display, color = p.text)
                Muted("${ui.plan.meta.title} · ${ui.plan.tasks.size} tasks, ${Days.short(ui.plan.start)} – ${Days.short(ui.plan.end)}", Modifier.padding(top = 4.dp))
            }
        }
        item {
            LabCard {
                listOf(
                    "Today shows what to do, in order. A missed day just moves the next task; nothing is lost.",
                    "All 10 categories sit at the top of Today, each with its own streak and % done. Forgot to tick yesterday? Tick it there: it counts for yesterday.",
                    "Done isn't the same as learned: record scores, take cold checks, and watch each task climb the 7 levels.",
                    "A day counts when your study reaches a small minimum you set. Rest days and reduced weeks never break it.",
                    "Every Sunday, a 15-minute checkpoint review. The evidence log keeps your 9-field lines and STAR stories.",
                ).forEach { Text("• $it", style = Type.body, color = p.text, modifier = Modifier.padding(vertical = 3.dp)) }
            }
        }
        item {
            LabCard {
                Text("Your data stays on this phone", style = Type.bodyStrong, color = p.text)
                Muted("No internet permission. Backups go only where you save them. Use fictional data: no client names, PAN, GSTIN or bank details." +
                    if (ui.s.legacyTaskLogs > 0 || ui.s.evidence.isNotEmpty()) " Everything you recorded before is kept." else "")
            }
        }
        item { Field(cue, { cue = it }, "Your daily cue", supporting = "A time or routine you already have, e.g. “After dinner, at my desk”.") }
        item {
            PrimaryButton("Start", { act.updateSettings { it.copy(v2Intro = true, onboarded = true, cue = cue.trim().ifBlank { it.cue }) } },
                Modifier.fillMaxWidth())
        }
    }
}
