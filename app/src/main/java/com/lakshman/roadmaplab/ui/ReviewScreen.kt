package com.lakshman.roadmaplab.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.domain.Card
import com.lakshman.roadmaplab.domain.CardState
import com.lakshman.roadmaplab.domain.Leitner
import com.lakshman.roadmaplab.domain.Rating
import com.lakshman.roadmaplab.ui.components.Choice
import com.lakshman.roadmaplab.ui.components.Label
import com.lakshman.roadmaplab.ui.components.LabOutlinedButton
import com.lakshman.roadmaplab.ui.components.MonoText
import com.lakshman.roadmaplab.ui.components.Muted
import com.lakshman.roadmaplab.ui.components.Panel
import com.lakshman.roadmaplab.ui.theme.LabType
import com.lakshman.roadmaplab.ui.theme.LocalLab
import kotlin.random.Random

@Composable
fun ReviewScreen(ui: UiState, vm: LabViewModel, padding: PaddingValues) {
    val lab = LocalLab.current
    var session by remember { mutableStateOf<List<String>?>(null) }
    var index by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Review", style = MaterialTheme.typography.headlineSmall)
        val ids = session
        when {
            ids == null -> {
                Panel(grid = true) {
                    Row {
                        Stat("Due today", "${ui.reviewQueue.size}", Modifier.weight(1f))
                        Stat("Reviewed", "${ui.reviewedToday}", Modifier.weight(1f))
                        Stat("Recall 30 d", ui.recall?.let { Fmt.pct(it) } ?: "–", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row {
                        Stat("Learned", "${ui.learned}", Modifier.weight(1f))
                        Stat("Unlocked", "${ui.unlocked} / ${ui.s.cards.size}", Modifier.weight(1f))
                        Stat("Daily cap", "${Leitner.DAILY_CAP}", Modifier.weight(1f))
                    }
                }
                if (ui.reviewQueue.isEmpty()) {
                    Panel {
                        Text("Nothing due right now.", style = MaterialTheme.typography.titleMedium)
                        Muted("New cards unlock when you finish the task that teaches them. Cards you get wrong come back tomorrow.")
                    }
                } else {
                    Panel {
                        Text("Try to recall each answer before you look.", style = MaterialTheme.typography.bodyMedium)
                        Muted("Testing yourself beats rereading. Topics are mixed on purpose.")
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val n = minOf(5, ui.reviewQueue.size)
                            Button(
                                onClick = { session = ui.reviewQueue.take(n).map { it.cardId }; index = 0; correct = 0 },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(4.dp),
                            ) { Text("Review $n") }
                            if (ui.reviewQueue.size > n) {
                                LabOutlinedButton("All ${ui.reviewQueue.size}", onClick = {
                                    session = ui.reviewQueue.map { it.cardId }; index = 0; correct = 0
                                }, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            index < ids.size -> {
                val card = ui.s.cards[ids[index]]
                val state = ui.s.cardStates.firstOrNull { it.cardId == ids[index] }
                if (card == null || state == null) {
                    LaunchedEffect(index) { index++ }
                } else {
                    key(card.id) {
                        CardView(card, state, index, ids.size) { rating, asMcq ->
                            if (rating != Rating.AGAIN) correct++
                            vm.launch { review(card.id, rating, asMcq) }
                            index++
                        }
                    }
                }
            }
            else -> {
                Panel(grid = true) {
                    Label("Session done")
                    Spacer(Modifier.height(6.dp))
                    MonoText("${ids.size} reviewed · $correct correct", style = LabType.number, color = lab.accent)
                    Spacer(Modifier.height(6.dp))
                    Muted("Wrong answers come back tomorrow; right ones wait longer each time.")
                    Spacer(Modifier.height(10.dp))
                    LabOutlinedButton("Done", onClick = { session = null })
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Label(label)
        MonoText(value, style = LabType.number)
    }
}

@Composable
fun CardView(card: Card, state: CardState, index: Int, total: Int, onRated: (Rating, Boolean) -> Unit) {
    val lab = LocalLab.current
    val asMcq = remember { Leitner.askAsMcq(card, state) }
    val codeLike = card.topic == "Python" || card.topic == "SQL"
    var chosen by remember { mutableStateOf<String?>(null) }
    var shown by remember { mutableStateOf(false) }
    val options = remember { card.options.shuffled(Random(card.id.hashCode() + state.reviews)) }

    Panel(stripe = lab.topic(card.topic)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label(card.topic, Modifier.weight(1f), color = lab.topic(card.topic))
            MonoText("${index + 1} / $total", style = LabType.monoSmall, color = lab.muted)
        }
        Spacer(Modifier.height(4.dp))
        Muted(if (asMcq) "Multiple choice" else "Answer from memory, then check")
        Spacer(Modifier.height(10.dp))
        if (card.prompt.contains('\n')) {
            MonoText(card.prompt, style = LabType.mono)
        } else {
            Text(card.prompt, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(14.dp))

        if (asMcq) {
            options.forEach { opt ->
                val isAnswer = opt == card.answer
                val picked = chosen
                val border = when {
                    picked == null -> lab.grid
                    isAnswer -> lab.accent
                    opt == picked -> lab.amber
                    else -> lab.grid
                }
                OutlinedButton(
                    onClick = { if (chosen == null) chosen = opt },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 3.dp),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(if (picked != null && (isAnswer || opt == picked)) 2.dp else 1.dp, border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = lab.text),
                ) {
                    if (codeLike) MonoText(opt, style = LabType.mono, modifier = Modifier.fillMaxWidth())
                    else Text(opt, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                }
            }
            val picked = chosen
            if (picked != null) {
                val right = picked == card.answer
                Spacer(Modifier.height(10.dp))
                Text(if (right) "Correct." else "Not quite. The answer is: ${card.answer}",
                    style = MaterialTheme.typography.titleSmall, color = if (right) lab.accent else lab.amber)
                Spacer(Modifier.height(4.dp))
                Text(card.explanation, style = MaterialTheme.typography.bodyMedium, color = lab.text)
                Spacer(Modifier.height(12.dp))
                if (right) {
                    Label("How easy was it?")
                    Spacer(Modifier.height(6.dp))
                    RatingRow(listOf(Rating.HARD, Rating.GOOD, Rating.EASY)) { onRated(it, true) }
                } else {
                    LabOutlinedButton("Continue", onClick = { onRated(Rating.AGAIN, true) }, modifier = Modifier.fillMaxWidth())
                }
            }
        } else {
            if (!shown) {
                Muted("Say the answer aloud or in your head first.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { shown = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(4.dp)) { Text("Show answer") }
            } else {
                Label("Answer")
                Spacer(Modifier.height(4.dp))
                if (codeLike) MonoText(card.answer, style = LabType.mono, color = lab.accent)
                else Text(card.answer, style = MaterialTheme.typography.bodyLarge, color = lab.accent)
                Spacer(Modifier.height(6.dp))
                Text(card.explanation, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Label("How well did you recall it?")
                Spacer(Modifier.height(6.dp))
                RatingRow(Rating.entries) { onRated(it, false) }
            }
        }
    }
}

@Composable
private fun RatingRow(ratings: List<Rating>, onPick: (Rating) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ratings.forEach { r -> Choice(r.label, false, onClick = { onPick(r) }, modifier = Modifier.weight(1f)) }
    }
}
