package com.lakshman.roadmaplab.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.lakshman.roadmaplab.BuildConfig
import com.lakshman.roadmaplab.data.json.LabJson
import com.lakshman.roadmaplab.domain.Days
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Backup, restore, export and plan import, through Android's own file screens (choose Google Drive there).
 * Nothing is ever sent by the app itself: it has no internet permission.
 */
@Composable
fun rememberBackupHooks(vm: LabViewModel): BackupHooks {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var version by remember { mutableIntStateOf(0) }
    var imported by remember(version) { mutableStateOf(vm.repo.usingImportedPlan()) }
    val copies = remember(version) { vm.repo.safetyCopies().size }

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching {
                withContext(Dispatchers.IO) {
                    val json = LabJson.encodeBackup(vm.repo.snapshot(), BuildConfig.VERSION_NAME, LocalDateTime.now(Days.ZONE).withNano(0).toString())
                    write(context, uri, json)
                }
            }
            if (ok.isSuccess) { vm.repo.markBackedUp(); toast(context, "Backup saved") }
            else toast(context, "Couldn't save the backup. Try again, or pick another folder.")
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            val text = withContext(Dispatchers.IO) { read(context, uri) }
            if (text == null) { toast(context, "Couldn't open that file."); return@launch }
            // Read and check the whole file before touching anything on the phone.
            val snapshot = runCatching { LabJson.decodeBackup(text) }
            snapshot.onFailure { toast(context, it.message ?: "That file isn't a Roadmap Lab backup.") }
            snapshot.onSuccess { s ->
                runCatching { vm.repo.restore(s, BuildConfig.VERSION_NAME) }
                    .onSuccess {
                        version++
                        toast(context, "Restored: ${s.events.size} task records, ${s.assessments.size} scores, ${s.evidence.size} log entries")
                    }
                    .onFailure { toast(context, "Restore failed; nothing was changed. ${it.message ?: ""}") }
            }
        }
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching { withContext(Dispatchers.IO) { write(context, uri, vm.repo.evidenceText(vm.ui.value?.plan)) } }
            toast(context, if (ok.isSuccess) "Evidence log exported" else "Couldn't save the file.")
        }
    }
    val importCsv = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            val text = withContext(Dispatchers.IO) { read(context, uri) }
            if (text == null) { toast(context, "Couldn't open that file."); return@launch }
            runCatching { vm.repo.importPlanCsv(text) }
                .onSuccess { n -> version++; imported = true; toast(context, "Plan imported: $n tasks. Your records are kept.") }
                .onFailure { toast(context, "Not imported: ${it.message}") }
        }
    }
    return BackupHooks(
        backupNow = { save.launch("RoadmapLab_backup_${Days.iso(Days.today())}.json") },
        restore = { open.launch(arrayOf("application/json", "text/plain", "*/*")) },
        exportEvidence = { export.launch("RoadmapLab_evidence_${Days.iso(Days.today())}.md") },
        importPlan = { importCsv.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*")) },
        useBundledPlan = { scope.launch { vm.repo.useBundledPlan(); version++; imported = false; toast(context, "Using the bundled plan") } },
        usingImportedPlan = imported,
        safetyCopies = copies,
    )
}

private fun write(context: Context, uri: Uri, text: String) {
    val out = context.contentResolver.openOutputStream(uri) ?: error("Can't open the file")
    out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
}

private fun read(context: Context, uri: Uri): String? =
    runCatching { context.contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) } }.getOrNull()

fun toast(context: Context, text: String) {
    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
}
