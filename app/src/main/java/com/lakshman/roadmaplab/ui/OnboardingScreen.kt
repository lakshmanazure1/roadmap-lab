package com.lakshman.roadmaplab.ui

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.domain.Mode
import com.lakshman.roadmaplab.reminder.Reminders
import com.lakshman.roadmaplab.ui.components.Choice
import com.lakshman.roadmaplab.ui.components.Label
import com.lakshman.roadmaplab.ui.components.MonoText
import com.lakshman.roadmaplab.ui.components.Muted
import com.lakshman.roadmaplab.ui.components.Panel
import com.lakshman.roadmaplab.ui.theme.LabType
import com.lakshman.roadmaplab.ui.theme.LocalLab

@Composable
fun OnboardingScreen(ui: UiState, vm: LabViewModel) {
    val lab = LocalLab.current
    val context = LocalContext.current
    val roadmap = ui.s.roadmap ?: return
    var step by rememberSaveable { mutableIntStateOf(0) }
    var modeKey by rememberSaveable { mutableStateOf(Mode.DRC.key) }
    var reminderOn by rememberSaveable { mutableStateOf(true) }
    var hour by rememberSaveable { mutableIntStateOf(21) }
    var minute by rememberSaveable { mutableIntStateOf(0) }
    val pastTasks = remember(roadmap) { roadmap.tasks.filter { it.plannedDay < ui.today } }
    val checked = remember(roadmap) { mutableStateMapOf<String, Boolean>().apply { pastTasks.forEach { put(it.id, it.assumedDone) } } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) reminderOn = false
        step = 2
    }

    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MonoText("Step ${step + 1} / 3", style = LabType.monoSmall, color = lab.muted)
            when (step) {
                0 -> {
                    Text("Roadmap Lab", style = MaterialTheme.typography.headlineMedium, color = lab.text)
                    Text("Your career roadmap as a 3-minute daily routine.", style = MaterialTheme.typography.titleMedium, color = lab.muted)
                    Panel(grid = true) {
                        Label("Morning · about 1 minute")
                        Muted("Today's line and where you are. Mood and energy. Up to 3 tasks. One if-then plan.")
                        Spacer(Modifier.height(10.dp))
                        Label("Evening · 2–3 minutes")
                        Muted("Mark tasks done, partly or skipped. About 5 review questions. One line of notes and the evidence log.")
                    }
                    Panel {
                        Label("Built on learning science")
                        Muted("Testing yourself and spacing reviews are the two methods with the strongest evidence. Cards unlock only after you study their topic.")
                    }
                    Muted("Everything stays on this phone. It works with no internet; only the Drive backup needs it.")
                    Button(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(4.dp)) {
                        Text("Start setup")
                    }
                }
                1 -> {
                    Text("Your days", style = MaterialTheme.typography.headlineSmall)
                    Panel {
                        Label("Mode")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Mode.entries.forEach { m ->
                                Choice(m.label, modeKey == m.key, onClick = { modeKey = m.key }, modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Muted("DRC about 60 min a day · India about 120 · heavy go-live week about 15. You can change it any time.")
                    }
                    Panel {
                        Label("Evening reminder")
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Remind me if the evening log isn't done", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Switch(checked = reminderOn, onCheckedChange = { reminderOn = it })
                        }
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable {
                                TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, true).show()
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Time", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            MonoText(Fmt.clock(hour, minute), style = LabType.number, color = lab.accent)
                        }
                    }
                    Button(
                        onClick = {
                            if (reminderOn && Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(context)) {
                                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                step = 2
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(4.dp),
                    ) { Text("Next") }
                    TextButton(onClick = { step = 0 }) { Text("Back") }
                }
                else -> {
                    Text("What's already done?", style = MaterialTheme.typography.headlineSmall)
                    Muted("From your roadmap, these were done before today. Untick anything you haven't finished — it will come back into your plan.")
                    Panel(padding = 8.dp) {
                        pastTasks.forEach { t ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { checked[t.id] = !(checked[t.id] ?: false) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = checked[t.id] ?: false, onCheckedChange = { checked[t.id] = it })
                                Column(Modifier.weight(1f)) {
                                    Text(t.title, style = MaterialTheme.typography.bodyMedium)
                                    MonoText("${Fmt.short(t.plannedDay)} · ${t.track}", style = LabType.monoSmall, color = lab.muted)
                                }
                            }
                        }
                    }
                    Button(
                        onClick = {
                            val done = checked.filterValues { it }.keys.toSet()
                            val mode = Mode.of(modeKey)
                            vm.launch {
                                completeOnboarding(done) { it.copy(mode = mode, reminderOn = reminderOn, reminderHour = hour, reminderMinute = minute) }
                                Reminders.schedule(context, settings.current())
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(4.dp),
                    ) { Text("Start using Roadmap Lab") }
                    TextButton(onClick = { step = 1 }) { Text("Back") }
                }
            }
        }
    }
}
