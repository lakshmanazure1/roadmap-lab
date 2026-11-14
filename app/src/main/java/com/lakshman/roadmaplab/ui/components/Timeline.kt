package com.lakshman.roadmaplab.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.domain.Milestone
import com.lakshman.roadmaplab.ui.Fmt
import com.lakshman.roadmaplab.ui.theme.LabType
import com.lakshman.roadmaplab.ui.theme.LocalLab
import java.time.LocalDate

/**
 * The "you are here" line: one horizontal timeline (e.g. Oct 2026 – Dec 2027) with month ticks,
 * milestone ticks, a Today marker (the date) and a You marker (where finished work puts you).
 */
@Composable
fun Timeline(
    start: Long,
    end: Long,
    today: Long,
    you: Long,
    milestones: List<Milestone>,
    modifier: Modifier = Modifier,
    tall: Boolean = false,
) {
    val lab = LocalLab.current
    val span = (end - start).coerceAtLeast(1).toFloat()
    val desc = "Timeline from ${Fmt.full(start)} to ${Fmt.full(end)}. Today ${Fmt.full(today)}. You ${Fmt.full(you)}."
    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(if (tall) 56.dp else 40.dp)
                .semantics { contentDescription = desc }
        ) {
            val w = size.width
            val h = size.height
            val mid = h * 0.55f
            fun x(day: Long): Float = ((day - start) / span).coerceIn(0f, 1f) * w
            val px = 1.dp.toPx()

            // Month ticks on graph paper.
            var m = LocalDate.ofEpochDay(start).withDayOfMonth(1)
            while (m.toEpochDay() <= end) {
                val mx = x(m.toEpochDay())
                drawLine(lab.grid, Offset(mx, mid - 6 * px), Offset(mx, mid + 6 * px), strokeWidth = px)
                m = m.plusMonths(1)
            }
            // Base line and covered part.
            drawLine(lab.grid, Offset(0f, mid), Offset(w, mid), strokeWidth = 2 * px)
            drawLine(lab.accent, Offset(0f, mid), Offset(x(you), mid), strokeWidth = 2 * px, cap = StrokeCap.Round)
            // Milestones.
            for (ms in milestones) {
                val mx = x(ms.day)
                drawLine(lab.muted, Offset(mx, mid - 10 * px), Offset(mx, mid + 10 * px), strokeWidth = 2 * px)
            }
            // Today: a thin vertical line.
            val tx = x(today)
            drawLine(lab.text, Offset(tx, 0f), Offset(tx, h), strokeWidth = px)
            // You: a filled dot.
            drawCircle(lab.accent, radius = 5 * px, center = Offset(x(you), mid))
            drawCircle(lab.bg, radius = 2 * px, center = Offset(x(you), mid))
        }
        Row(Modifier.fillMaxWidth()) {
            MonoText(Fmt.month(start) + " " + LocalDate.ofEpochDay(start).year, style = LabType.monoSmall, color = lab.muted,
                modifier = Modifier.weight(1f))
            MonoText("│ Today   ● You", style = LabType.monoSmall, color = lab.muted, modifier = Modifier.weight(1f))
            MonoText(Fmt.month(end) + " " + LocalDate.ofEpochDay(end).year, style = LabType.monoSmall, color = lab.muted)
        }
    }
}
