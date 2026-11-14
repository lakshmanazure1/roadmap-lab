package com.lakshman.roadmaplab.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/** A text field that saves itself half a second after you stop typing, and when you leave the screen. */
@Composable
fun AutosaveField(
    key: Any,
    initial: String,
    label: String,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    singleLine: Boolean = false,
) {
    var text by remember(key) { mutableStateOf(initial) }
    var dirty by remember(key) { mutableStateOf(false) }
    val save by rememberUpdatedState(onSave)
    LaunchedEffect(key, text) {
        if (dirty) {
            delay(500)
            save(text)
            dirty = false
        }
    }
    DisposableEffect(key) {
        onDispose { if (dirty) save(text) }
    }
    LabField(
        value = text,
        onChange = {
            text = it
            dirty = true
        },
        label = label,
        modifier = modifier,
        minLines = minLines,
        singleLine = singleLine,
    )
}
