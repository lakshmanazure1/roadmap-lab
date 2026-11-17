package com.lakshman.roadmaplab.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.domain.DayResult
import com.lakshman.roadmaplab.domain.DayStatus
import com.lakshman.roadmaplab.domain.Level
import com.lakshman.roadmaplab.ui.theme.LocalPalette
import com.lakshman.roadmaplab.ui.theme.Type

// The building blocks every screen uses, so spacing, shapes and colours stay consistent.

val ScreenPadding = 16.dp
val CardShape = RoundedCornerShape(20.dp)

@Composable
fun LabCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(16.dp),
    tint: Color? = null,
    border: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = LocalPalette.current
    // Material 3 card: a tonal surface; an outline only when it marks something (e.g. the fresh-start offer).
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(tint ?: p.surface)
            .then(if (border != null) Modifier.border(1.dp, border, CardShape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .animateContentSize(spring(stiffness = 600f))
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: String? = null, onTrailing: (() -> Unit)? = null) {
    val p = LocalPalette.current
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = Type.section, color = p.text, modifier = Modifier.weight(1f))
        if (trailing != null) {
            if (onTrailing != null) TextButton(onClick = onTrailing, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(trailing, style = Type.smallStrong, color = p.accent)
            } else Text(trailing, style = Type.small, color = p.text2)
        }
    }
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier, style: TextStyle = Type.small, maxLines: Int = Int.MAX_VALUE) {
    Text(text, style = style, color = LocalPalette.current.text2, modifier = modifier, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
fun AreaDot(area: String, size: Dp = 8.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(LocalPalette.current.area(area)))
}

/** Area name with its colour dot: colour is never the only signal. */
@Composable
fun AreaLabel(area: String, label: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        AreaDot(area)
        Spacer(Modifier.width(6.dp))
        Text(label, style = Type.caption, color = LocalPalette.current.text2, maxLines = 1)
    }
}

enum class Tone { NEUTRAL, ACCENT, SUCCESS, WARNING, DANGER }

@Composable
fun Tag(text: String, tone: Tone = Tone.NEUTRAL, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val (bg, fg) = when (tone) {
        Tone.NEUTRAL -> p.surface2 to p.text2
        Tone.ACCENT -> p.accentSoft to p.accent
        Tone.SUCCESS -> p.successSoft to p.success
        Tone.WARNING -> p.warningSoft to p.warning
        Tone.DANGER -> p.dangerSoft to p.danger
    }
    Text(text, style = Type.caption, color = fg, maxLines = 1,
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp))
}

@Composable
fun LevelTag(level: Level, modifier: Modifier = Modifier) {
    val tone = when (level) {
        Level.NOT_STARTED, Level.SKIPPED -> Tone.NEUTRAL
        Level.IN_PROGRESS -> Tone.ACCENT
        Level.NEEDS_REVISION -> Tone.DANGER
        Level.COMPLETED, Level.PRACTISED -> Tone.ACCENT
        Level.ASSESSED, Level.DEMONSTRATED, Level.MASTERED -> Tone.SUCCESS
    }
    val label = if (level.number > 0) "${level.number} · ${level.label}" else level.label
    Tag(label, tone, modifier)
}

/** A round tick: 28 dp visible, 48 dp to touch. */
@Composable
fun CheckCircle(checked: Boolean, onClick: (() -> Unit)?, label: String, skipped: Boolean = false, color: Color? = null) {
    val p = LocalPalette.current
    val fill by animateColorAsState(if (checked) (color ?: p.accent) else Color.Transparent, tween(180), label = "check")
    Box(
        Modifier
            .size(48.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Checkbox, onClick = onClick) else Modifier)
            .semantics {
                contentDescription = label
                stateDescription = when { checked -> "Done"; skipped -> "Skipped"; else -> "Not done" }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(fill)
                .border(1.5.dp, if (checked) Color.Transparent else p.text3, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(Icons.Outlined.Check, null, tint = p.onAccent, modifier = Modifier.size(17.dp))
            if (skipped) Icon(Icons.Outlined.Remove, null, tint = p.text3, modifier = Modifier.size(17.dp))
        }
    }
}

/** Ring progress: the day's minutes against the minimum. */
@Composable
fun ProgressRing(fraction: Float, modifier: Modifier = Modifier, size: Dp = 72.dp, stroke: Dp = 7.dp, color: Color? = null,
                 track: Color? = null, center: @Composable () -> Unit = {}) {
    val p = LocalPalette.current
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(400), label = "ring")
    val c = color ?: p.accent
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val sw = stroke.toPx()
            val arc = Size(this.size.width - sw, this.size.height - sw)
            val tl = Offset(sw / 2, sw / 2)
            drawArc(track ?: p.surface2, -90f, 360f, false, tl, arc, style = Stroke(sw))
            if (f > 0f) drawArc(c, -90f, 360f * f, false, tl, arc, style = Stroke(sw, cap = StrokeCap.Round))
        }
        center()
    }
}

/** A thin bar; [marker] shows where the plan expects you to be today ("you are here"). */
@Composable
fun ProgressLine(fraction: Float, modifier: Modifier = Modifier, color: Color? = null, marker: Float? = null, height: Dp = 8.dp) {
    val p = LocalPalette.current
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(400), label = "line")
    val c = color ?: p.accent
    Canvas(modifier.fillMaxWidth().height(height)) {
        val r = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(p.surface2, cornerRadius = r)
        if (f > 0f) drawRoundRect(c, size = Size(size.width * f, size.height), cornerRadius = r)
        if (marker != null) {
            val x = (size.width * marker.coerceIn(0f, 1f)).coerceIn(1.5f, size.width - 1.5f)
            drawLine(p.text, Offset(x, -2f), Offset(x, size.height + 2f), strokeWidth = 3f)
        }
    }
}

@Composable
fun Stat(label: String, value: String, modifier: Modifier = Modifier, sub: String? = null, valueColor: Color? = null) {
    val p = LocalPalette.current
    Column(modifier) {
        Text(value, style = Type.stat, color = valueColor ?: p.text, maxLines = 1)
        Text(label, style = Type.small, color = p.text2)
        if (sub != null) Text(sub, style = Type.caption, color = p.text3)
    }
}

/** Week bars: planned (outline) behind completed (filled). Restrained on purpose. */
@Composable
fun WeekBars(planned: List<Int>, done: List<Int>, current: Int, modifier: Modifier = Modifier, labels: List<String> = emptyList()) {
    val p = LocalPalette.current
    val max = (planned.maxOrNull() ?: 1).coerceAtLeast(1)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(96.dp)) {
            val n = planned.size.coerceAtLeast(1)
            val gap = 6.dp.toPx()
            val w = (size.width - gap * (n - 1)) / n
            for (i in planned.indices) {
                val x = i * (w + gap)
                val hp = size.height * planned[i] / max
                val hd = size.height * done.getOrElse(i) { 0 } / max
                drawRoundRect(p.surface2, Offset(x, size.height - hp), Size(w, hp), CornerRadius(4f, 4f))
                if (hd > 0) drawRoundRect(if (i == current) p.accent else p.accent.copy(alpha = 0.55f),
                    Offset(x, size.height - hd), Size(w, hd), CornerRadius(4f, 4f))
            }
        }
        if (labels.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            labels.forEachIndexed { i, l ->
                Text(l, style = Type.caption, color = if (i == current) p.text else p.text3, modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
            }
        }
    }
}

/** The last 28 days: filled = minimum met, outline = missed, faded = forgiven, small dot = rest or nothing scheduled. */
@Composable
fun DayStrip(days: List<DayResult>, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Canvas(modifier.fillMaxWidth().height(22.dp)) {
        val n = days.size.coerceAtLeast(1)
        val gap = 3.dp.toPx()
        val w = (size.width - gap * (n - 1)) / n
        days.forEachIndexed { i, d ->
            val x = i * (w + gap)
            val r = CornerRadius(4f, 4f)
            when (d.status) {
                DayStatus.QUALIFIED -> drawRoundRect(p.success, Offset(x, 0f), Size(w, size.height), r)
                DayStatus.GRACE -> drawRoundRect(p.success.copy(alpha = 0.25f), Offset(x, 0f), Size(w, size.height), r)
                DayStatus.MISSED -> drawRoundRect(p.text3, Offset(x + 1f, 1f), Size(w - 2f, size.height - 2f), r, style = Stroke(2f))
                DayStatus.TODAY -> drawRoundRect(p.accent, Offset(x + 1f, 1f), Size(w - 2f, size.height - 2f), r, style = Stroke(3f))
                else -> drawCircle(p.border, 3.dp.toPx(), Offset(x + w / 2, size.height / 2))
            }
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    val p = LocalPalette.current
    Button(onClick, modifier.heightIn(min = 48.dp), enabled = enabled, shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.onAccent),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = Type.smallStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null,
                    color: Color? = null) {
    val p = LocalPalette.current
    OutlinedButton(onClick, modifier.heightIn(min = 48.dp), enabled = enabled, shape = CircleShape,
        border = BorderStroke(1.dp, p.text3), colors = ButtonDefaults.outlinedButtonColors(contentColor = color ?: p.accent),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = Type.smallStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    val p = LocalPalette.current
    Row(modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp).heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = p.text) }
        else Spacer(Modifier.width(12.dp))
        Text(title, style = Type.title, color = p.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

@Composable
fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    singleLine: Boolean = false,
    number: Boolean = false,
    supporting: String? = null,
    isError: Boolean = false,
) {
    val p = LocalPalette.current
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = modifier.fillMaxWidth(),
        label = { Text(label, style = Type.small) }, minLines = minLines, singleLine = singleLine, textStyle = Type.body,
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        supportingText = supporting?.let { { Text(it, style = Type.caption, color = if (isError) p.warning else p.text2) } },
        isError = false, shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = p.accent, unfocusedBorderColor = p.text3,
            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
    )
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, dot: String? = null) {
    val p = LocalPalette.current
    // Material 3 filter chip: a check appears when selected (a category chip keeps its colour dot).
    val lead: (@Composable () -> Unit)? = when {
        dot != null -> { { AreaDot(dot) } }
        selected -> { { Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) } }
        else -> null
    }
    FilterChip(
        selected = selected, onClick = onClick, modifier = modifier,
        label = { Text(text, style = Type.smallStrong, maxLines = 1) },
        leadingIcon = lead,
        shape = RoundedCornerShape(8.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = p.accentSoft, selectedLabelColor = p.onAccentSoft, selectedLeadingIconColor = p.onAccentSoft,
            labelColor = p.text2,
        ),
        border = FilterChipDefaults.filterChipBorder(true, selected, borderColor = p.text3, selectedBorderColor = Color.Transparent),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
fun Banner(text: String, tone: Tone = Tone.ACCENT, action: String? = null, onAction: (() -> Unit)? = null, title: String? = null) {
    val p = LocalPalette.current
    val (bg, fg) = when (tone) {
        Tone.SUCCESS -> p.successSoft to p.success
        Tone.WARNING -> p.warningSoft to p.warning
        Tone.DANGER -> p.dangerSoft to p.danger
        Tone.NEUTRAL -> p.surface2 to p.text2
        Tone.ACCENT -> p.accentSoft to p.accent
    }
    Column(Modifier.fillMaxWidth().clip(CardShape).background(bg).animateContentSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        if (title != null) Text(title, style = Type.bodyStrong, color = p.text)
        Text(text, style = Type.small, color = p.text)
        if (action != null && onAction != null) TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) {
            Text(action, style = Type.smallStrong, color = fg)
        }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit, danger: Boolean = false) {
    val p = LocalPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = Type.title) },
        text = { Text(text, style = Type.body, color = p.text2) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, style = Type.smallStrong, color = if (danger) p.danger else p.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", style = Type.smallStrong, color = p.accent) } },
    )
}

@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit, min: Int, max: Int, step: Int = 5, suffix: String = "min", label: String) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = Type.body, color = p.text, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange((value - step).coerceAtLeast(min)) }, enabled = value > min) {
            Icon(Icons.Outlined.Remove, "Less for $label", tint = p.text2)
        }
        Text("$value $suffix", style = Type.bodyStrong, color = p.text, modifier = Modifier.width(64.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        IconButton(onClick = { onChange((value + step).coerceAtMost(max)) }, enabled = value < max) {
            Icon(Icons.Outlined.Add, "More for $label", tint = p.text2)
        }
    }
}

@Composable
fun KeyValue(key: String, value: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Column(modifier.padding(vertical = 4.dp)) {
        Text(key, style = Type.caption, color = p.text3)
        Text(value, style = Type.body, color = p.text)
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(LocalPalette.current.border))
}
