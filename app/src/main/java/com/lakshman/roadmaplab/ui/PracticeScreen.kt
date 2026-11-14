package com.lakshman.roadmaplab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.domain.Leitner
import com.lakshman.roadmaplab.domain.Rating
import com.lakshman.roadmaplab.domain.TodayEngine
import com.lakshman.roadmaplab.ui.components.Banner
import com.lakshman.roadmaplab.ui.components.Chip
import com.lakshman.roadmaplab.ui.components.ChipRow
import com.lakshman.roadmaplab.ui.components.Field
import com.lakshman.roadmaplab.ui.components.LabCard
import com.lakshman.roadmaplab.ui.components.Muted
import com.lakshman.roadmaplab.ui.components.PrimaryButton
import com.lakshman.roadmaplab.ui.components.ScreenPadding
import com.lakshman.roadmaplab.ui.components.SecondaryButton
import com.lakshman.roadmaplab.ui.components.SectionTitle
import com.lakshman.roadmaplab.ui.components.Stat
import com.lakshman.roadmaplab.ui.components.Tone
import com.lakshman.roadmaplab.ui.components.TopBar
import com.lakshman.roadmaplab.ui.theme.LocalPalette
import com.lakshman.roadmaplab.ui.theme.Type

/**
 * Flashcards with spaced repetition (Leitner boxes). Cards unlock as you finish the plan's tasks;
 * your own cards (the plan asks you to make some each week) join the same queue.
 */
@Composable
fun PracticeScreen(ui: UiState, act: Actions, nav: Nav) {
    val p = LocalPalette.current
    val queue = remember(ui.s.cardStates, ui.reviewedToday) { Leitner.queue(ui.s.cards, ui.s.cardStates, ui.today, ui.reviewedToday) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var shown by rememberSaveable { mutableStateOf(false) }
    var reviewedHere by rememberSaveable { mutableIntStateOf(0) }
    val ticked = ui.s.ticks.any { it.day == ui.today && it.routineId == TodayEngine.FLASHCARDS && it.done }
    // 10 reviews (or an empty queue after some) counts as the plan's "15 min of flashcards".
    LaunchedEffect(reviewedHere, queue.isEmpty()) {
        if (!ticked && (reviewedHere >= 10 || (reviewedHere > 0 && queue.isEmpty()))) act.tick(ui.today, TodayEngine.FLASHCARDS, true, 15)
    }
    Column(Modifier.fillMaxSize()) {
        TopBar("Flashcards", nav.back)
        LazyColumn(contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                LabCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Stat("Due now", "${queue.size}", Modifier.weight(1f))
                        Stat("Reviewed today", "${ui.reviewedToday}", Modifier.weight(1f))
                        Stat("Learned", "${Leitner.learnedCount(ui.s.cardStates)}", Modifier.weight(1f), sub = "of ${ui.s.cardStates.size} unlocked")
                    }
                    val rate = Leitner.recallRate(ui.s.reviewLogs, ui.today)
                    if (rate != null) Muted("Recall over 30 days: %.0f%%".format(rate), Modifier.padding(top = 8.dp))
                }
            }
            val st = queue.firstOrNull()
            val card = st?.let { ui.s.cards[it.cardId] }
            if (card == null) {
                item { Banner(if (ui.s.cardStates.isEmpty()) "Cards unlock as you finish tasks (accounting from 1.2, Python from the validator)."
                    else "Nothing due. Come back tomorrow: spacing is what makes it stick.", Tone.SUCCESS) }
            } else {
                val asMcq = Leitner.askAsMcq(card, st)
                item {
                    LabCard {
                        Muted("${card.topic} · box ${st.box} · ${queue.size} left", style = Type.caption)
                        Text(card.prompt, style = Type.title, color = p.text, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                if (asMcq && !shown) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            card.options.forEach { o ->
                                SecondaryButton(o, {
                                    val right = o == card.answer
                                    act.review(card.id, if (right) Rating.GOOD else Rating.AGAIN, true)
                                    act.note(Note(if (right) "Right" else "Not quite: ${card.answer}"))
                                    reviewedHere++
                                }, Modifier.fillMaxWidth())
                            }
                        }
                    }
                } else if (!shown) {
                    item { PrimaryButton("Show the answer", { shown = true }, Modifier.fillMaxWidth()) }
                } else {
                    item {
                        LabCard(tint = p.surface2) {
                            Text(card.answer, style = Type.bodyStrong, color = p.text)
                            if (card.explanation.isNotBlank()) Muted(card.explanation, Modifier.padding(top = 4.dp))
                        }
                    }
                    item {
                        Muted("How well did you remember it?")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                            Rating.entries.forEach { r ->
                                SecondaryButton(r.label, { act.review(card.id, r, false); shown = false; reviewedHere++ }, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            item { SectionTitle("Your own cards", trailing = if (adding) null else "Add", onTrailing = if (adding) null else ({ adding = true })) }
            if (adding) item { AddCard(act) { adding = false } }
        }
    }
}

@Composable
private fun AddCard(act: Actions, onDone: () -> Unit) {
    var topic by remember { mutableStateOf("Accounting") }
    var prompt by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    LabCard {
        ChipRow { listOf("Accounting", "Python", "French", "Communication").forEach { Chip(it, topic == it, { topic = it }) } }
        Field(prompt, { prompt = it }, "Question (fictional data only)", Modifier.padding(top = 8.dp), minLines = 2)
        Field(answer, { answer = it }, "Answer", Modifier.padding(top = 8.dp), minLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            SecondaryButton("Cancel", onDone, Modifier.weight(1f))
            PrimaryButton("Add card", { act.addCard(topic, prompt, answer); act.note(Note("Card added: due today")); onDone() }, Modifier.weight(1f),
                enabled = prompt.isNotBlank() && answer.isNotBlank())
        }
    }
}
