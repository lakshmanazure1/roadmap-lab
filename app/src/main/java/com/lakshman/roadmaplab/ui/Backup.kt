package com.lakshman.roadmaplab.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.lakshman.roadmaplab.BuildConfig
import com.lakshman.roadmaplab.data.json.LabJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * "Back up to Drive": opens Android's save screen with a dated file name
 * (RoadmapLab_backup_2026-11-22.json). Choose Google Drive and your folder there.
 * Every backup is a separate file, so older ones stay.
 */
@Composable
fun rememberBackupToDrive(vm: LabViewModel): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = runCatching {
                    withContext(Dispatchers.IO) {
                        val json = LabJson.encodeBackup(vm.repo.snapshot(), BuildConfig.VERSION_NAME, LocalDateTime.now().withNano(0).toString())
                        val out = context.contentResolver.openOutputStream(uri) ?: error("Can't open the file")
                        out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                    }
                }
                if (ok.isSuccess) {
                    vm.repo.markBackedUp()
                    toast(context, "Backup saved")
                } else {
                    toast(context, "Couldn't save the backup. Try again, or pick another folder.")
                }
            }
        }
    }
    return { launcher.launch("RoadmapLab_backup_${LocalDate.now()}.json") }
}

/** Opens a file picker and hands back the chosen file's text (or null). */
@Composable
fun rememberOpenTextFile(onText: (String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) }
                    }.getOrNull()
                }
                if (text == null) toast(context, "Couldn't open that file.")
                onText(text)
            }
        }
    }
    return { launcher.launch(arrayOf("*/*")) }
}

fun toast(context: Context, text: String) {
    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
}
