@file:OptIn(ExperimentalMaterial3Api::class)

package com.lakshman.roadmaplab.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.ui.Fmt
import com.lakshman.roadmaplab.ui.theme.LabType
import com.lakshman.roadmaplab.ui.theme.LocalLab

private val PanelShape = RoundedCornerShape(12.dp)

/** A flat instrument panel with a 1 dp border. With [grid], a faint graph-paper grid is drawn behind. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    grid: Boolean = false,
    padding: Dp = 20.dp,
    stripe: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val lab = LocalLab.current
    val base = modifier
        .fillMaxWidth()
        .clip(PanelShape)
        .background(lab.panel)
        .border(BorderStroke(1.dp, lab.grid), PanelShape)
        .then(if (grid) Modifier.graphPaper(lab.grid.copy(alpha = 0.35f)) else Modifier)
        .then(
            if (stripe != null) Modifier.drawBehind {
                drawRect(stripe, size = androidx.compose.ui.geometry.Size(5.dp.toPx(), size.height))
            } else Modifier
        )
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    Column(base.padding(start = if (stripe != null) padding + 4.dp else padding, top = padding, end = padding, bottom = padding), content = content)
}

/** A big section heading with a coloured dot, e.g. "Morning". */
@Composable
fun SectionTitle(text: String, color: Color, detail: String? = null) {
    val lab = LocalLab.current
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(10.dp).height(10.dp).clip(RoundedCornerShape(5.dp)).background(color))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.titleLarge, color = lab.text, modifier = Modifier.weight(1f))
        if (detail != null) Text(detail, style = LabType.monoSmall, color = lab.muted)
    }
}

/** Track name with its colour dot. */
@Composable
fun TrackTag(track: String, modifier: Modifier = Modifier) {
    val lab = LocalLab.current
    val c = lab.track(track)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(8.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(c))
        Spacer(Modifier.width(6.dp))
        Text(track.uppercase(), style = LabType.label, color = lab.text)
    }
}

/** Faint graph-paper lines every 16 dp. */
fun Modifier.graphPaper(color: Color, step: Dp = 16.dp): Modifier = drawBehind {
    val s = step.toPx()
    var x = 0f
    while (x < size.width) {
        drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += s
    }
    var y = 0f
    while (y < size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += s
    }
}

/** Small mono caps label, e.g. "TODAY'S TASKS". */
@Composable
fun Label(text: String, modifier: Modifier = Modifier, color: Color = LocalLab.current.muted) {
    Text(text.uppercase(), modifier = modifier, style = LabType.label, color = color)
}

@Composable
fun MonoText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LabType.mono,
    color: Color = LocalLab.current.text,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(text, modifier = modifier, style = style, color = color, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodySmall) {
    Text(text, modifier = modifier, style = style, color = LocalLab.current.muted)
}

/** A row of five 48 dp boxes for mood or energy (1–5). */
@Composable
fun ScaleRow(label: String, value: Int?, onSelect: (Int) -> Unit, lowText: String, highText: String) {
    val lab = LocalLab.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label(label, Modifier.weight(1f))
            Text(if (value == null) "–" else "$value / 5", style = LabType.monoSmall, color = lab.muted)
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (n in 1..5) {
                val selected = value == n
                Box(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (selected) lab.accent else lab.bg)
                        .border(1.dp, if (selected) lab.accent else lab.grid, RoundedCornerShape(4.dp))
                        .clickable { onSelect(n) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$n", style = LabType.number, color = if (selected) lab.onAccent else lab.text)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
            Muted(lowText, Modifier.weight(1f))
            Muted(highText)
        }
    }
}

/** A thin bar with a label and a mono % on the right. */
@Composable
fun TrackBar(label: String, percent: Double, modifier: Modifier = Modifier, color: Color? = null) {
    val lab = LocalLab.current
    val barColor = color ?: lab.track(label)
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = lab.text, modifier = Modifier.weight(1f),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            MonoText(Fmt.pct(percent), style = LabType.monoSmall, color = lab.muted)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(lab.grid)
        ) {
            val f = (percent / 100.0).toFloat().coerceIn(0f, 1f)
            if (f > 0f) Box(Modifier.fillMaxWidth(f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(barColor))
        }
    }
}

@Composable
fun BackBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    val lab = LocalLab.current
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = lab.bg, titleContentColor = lab.text),
    )
}

/** A choice chip that is at least 48 dp tall. */
@Composable
fun Choice(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color? = null) {
    val lab = LocalLab.current
    val c = color ?: lab.accent
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) c else if (color != null) c.copy(alpha = 0.14f) else lab.bg,
        border = BorderStroke(1.dp, if (selected || color != null) c else lab.grid),
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) lab.onAccent else lab.text,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun LabOutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color? = null) {
    val lab = LocalLab.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color ?: lab.grid),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color ?: lab.text),
    ) { Text(text, fontWeight = FontWeight.Medium) }
}

@Composable
fun LabField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    singleLine: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        minLines = minLines,
        singleLine = singleLine,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    val lab = LocalLab.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = if (destructive) lab.error else lab.accent)) {
                Text(confirm)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = lab.panel,
    )
}

@Composable
fun KeyValue(key: String, value: String, modifier: Modifier = Modifier) {
    val lab = LocalLab.current
    Row(modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Text(key, style = MaterialTheme.typography.bodySmall, color = lab.muted, modifier = Modifier.width(96.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = lab.text, modifier = Modifier.weight(1f))
    }
}
