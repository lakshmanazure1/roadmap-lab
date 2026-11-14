package com.lakshman.roadmaplab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.domain.Assessment
import com.lakshman.roadmaplab.domain.CheckSpec
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.Mastery
import com.lakshman.roadmaplab.domain.PlanTask
import com.lakshman.roadmaplab.domain.Question
import com.lakshman.roadmaplab.domain.QuestionBank
import com.lakshman.roadmaplab.domain.Topics
import com.lakshman.roadmaplab.ui.components.Banner
import com.lakshman.roadmaplab.ui.components.Chip
import com.lakshman.roadmaplab.ui.components.ChipRow
import com.lakshman.roadmaplab.ui.components.Divider
import com.lakshman.roadmaplab.ui.components.Field
import com.lakshman.roadmaplab.ui.components.LabCard
import com.lakshman.roadmaplab.ui.components.Muted
import com.lakshman.roadmaplab.ui.components.PrimaryButton
import com.lakshman.roadmaplab.ui.components.ProgressLine
import com.lakshman.roadmaplab.ui.components.ScreenPadding
import com.lakshman.roadmaplab.ui.components.SecondaryButton
import com.lakshman.roadmaplab.ui.components.SectionTitle
import com.lakshman.roadmaplab.ui.components.Stepper
import com.lakshman.roadmaplab.ui.components.Tag
import com.lakshman.roadmaplab.ui.components.Tone
import com.lakshman.roadmaplab.ui.components.TopBar
import com.lakshman.roadmaplab.ui.theme.LocalPalette
import com.lakshman.roadmaplab.ui.theme.Type
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/** Runs one check of a task and saves the attempt. Retakes add new attempts; the history stays. */
@Composable
fun CheckScreen(ui: UiState, act: Actions, nav: Nav, taskId: String, index: Int) {
    val t = ui.plan.task(taskId) ?: return
    val spec = t.checks.getOrNull(index) ?: return
    fun save(type: String, score: Double, max: Double, target: Double, independent: Boolean, detail: JSONObject, seconds: Int = 0) {
        val pass = if (max > 0) 100.0 * score / max >= Mastery.passMark(type, max, target) else score > 0
        act.addAssessment(Assessment(0, t.id, t.area, Topics.of(ui.plan, t), type, score, max, pass, independent, detail.toString(), ui.today, 0, seconds))
        act.note(Note(if (pass) "Saved: passed" else "Saved: below target. It's in the history; retake it when ready."))
    }
    Column(Modifier.fillMaxSize()) {
        TopBar("${t.id} · ${spec.title}", nav.back)
        when (spec) {
            is CheckSpec.Quiz -> QuizRunner(ui, t, spec, nav) { score, max, independent, detail, secs ->
                save("quiz", score.toDouble(), max.toDouble(), spec.target.toDouble(), independent, detail, secs)
            }
            is CheckSpec.Score -> ScoreEntry(spec) { score, max, note ->
                save("score", score.toDouble(), max.toDouble(), if (spec.label == "%") spec.target.toDouble() else spec.target.toDouble(), false,
                    JSONObject().put("note", note)); nav.back()
            }
            is CheckSpec.Checklist -> ChecklistEntry(spec.criteria) { ticked ->
                save("checklist", ticked.size.toDouble(), spec.criteria.size.toDouble(), 0.0, false, JSONObject().put("ticked", JSONArray(ticked))); nav.back()
            }
            is CheckSpec.Rubric -> RubricEntry(ui, spec) { scores, note ->
                save("rubric", scores.sum().toDouble(), 10.0, spec.target.toDouble(), true,
                    JSONObject().put("scores", JSONArray(scores)).put("change", note)); nav.back()
            }
            is CheckSpec.Explain -> ExplainEntry(spec) { level, words ->
                save("explain", level.toDouble(), 2.0, 2.0, true, JSONObject().put("words", words)); nav.back()
            }
            is CheckSpec.Demonstrate -> DemoEntry { who, unaided ->
                save("demo", if (unaided) 1.0 else 0.0, 1.0, 1.0, unaided, JSONObject().put("who", who)); nav.back()
            }
            is CheckSpec.Outcome -> OutcomeEntry { replied ->
                save("outcome", if (replied) 1.0 else 0.0, 1.0, 1.0, false, JSONObject()); nav.back()
            }
            is CheckSpec.Star -> Column(Modifier.padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Muted("Write the story in the evidence log: Situation (1 line) · Task (1) · Action (2–3) · Result (1, with a number), under 150 words. " +
                    "The app checks the format and the privacy rules as you type.")
                PrimaryButton("Write the STAR story", { nav.open(Screen.EvidenceEdit(null, ui.today, t.id, true)) }, Modifier.fillMaxWidth())
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------- quiz

@Composable
private fun QuizRunner(ui: UiState, t: PlanTask, spec: CheckSpec.Quiz, nav: Nav, onSave: (Int, Int, Boolean, JSONObject, Int) -> Unit) {
    val p = LocalPalette.current
    val seed by rememberSaveable { mutableIntStateOf(Random.nextInt(1, Int.MAX_VALUE)) }
    val questions = remember(seed) { QuestionBank.build(ui.plan, t, spec, ui.s.cards.values, seed) }
    var i by rememberSaveable { mutableIntStateOf(0) }
    val answers = remember { mutableStateMapOf<Int, String>() }
    var revealed by rememberSaveable { mutableStateOf(false) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var usedNotes by rememberSaveable { mutableStateOf(false) }
    var saved by rememberSaveable { mutableStateOf(false) }
    var seconds by rememberSaveable { mutableIntStateOf(0) }
    val testMode = spec.timedMinutes > 0 || spec.bank == "journal"
    LaunchedEffect(finished) { while (!finished) { delay(1000); seconds++ } }

    if (questions.isEmpty()) {
        Column(Modifier.padding(ScreenPadding)) { Muted("There are no questions for this topic yet. Record your notebook score instead.") }
        return
    }
    val correct = questions.indices.count { k -> answers[k]?.let { QuestionBank.isCorrect(questions[k], it) } == true }
    val target = if (spec.target > 0) spec.target else kotlin.math.ceil(questions.size * Mastery.PASS / 100).toInt()

    if (finished) {
        val passed = correct >= target
        LazyColumn(contentPadding = PaddingValues(ScreenPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                LabCard(tint = if (passed) p.successSoft else p.dangerSoft, border = if (passed) p.successSoft else p.dangerSoft) {
                    Text("$correct / ${questions.size}", style = Type.display, color = p.text)
                    Text(if (passed) "Passed (target $target)" else "Below the target of $target: needs revision", style = Type.section,
                        color = if (passed) p.success else p.danger)
                    Muted("Time: ${seconds / 60} min ${seconds % 60} s" + if (spec.timedMinutes > 0) " (plan: ${spec.timedMinutes} min)" else "")
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("I used notes or looked at answers", style = Type.bodyStrong, color = p.text)
                        Muted("Be honest: a cold result counts as practised independently.")
                    }
                    Switch(usedNotes, { usedNotes = it }, enabled = !saved, colors = SwitchDefaults.colors(checkedTrackColor = p.accent))
                }
            }
            item {
                if (!saved) PrimaryButton("Save this result", {
                    val detail = JSONObject().put("asked", JSONArray(questions.map { it.id }))
                        .put("wrong", JSONArray(questions.indices.filter { answers[it]?.let { a -> QuestionBank.isCorrect(questions[it], a) } != true }.map { questions[it].id }))
                    onSave(correct, questions.size, !usedNotes, detail, seconds); saved = true
                }, Modifier.fillMaxWidth())
                else SecondaryButton("Done", nav.back, Modifier.fillMaxWidth())
            }
            val wrong = questions.indices.filter { answers[it]?.let { a -> QuestionBank.isCorrect(questions[it], a) } != true }
            if (wrong.isNotEmpty()) {
                item { SectionTitle("To revise", trailing = "${wrong.size}") }
                wrong.forEach { k ->
                    item {
                        LabCard {
                            Text(questions[k].prompt, style = Type.bodyStrong, color = p.text)
                            Muted("Your answer: ${answers[k]?.replace("\u0001", " ")?.replace("|", " / ") ?: "—"}")
                            Text("Right: ${questions[k].explanation}", style = Type.small, color = p.success, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
        return
    }

    val q = questions[i]
    val answer = answers[i]
    LazyColumn(contentPadding = PaddingValues(ScreenPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Muted("Question ${i + 1} of ${questions.size}", Modifier.weight(1f))
                    if (spec.timedMinutes > 0) {
                        val left = spec.timedMinutes * 60 - seconds
                        Tag(if (left >= 0) "%d:%02d left".format(left / 60, left % 60) else "Over time by %d:%02d".format(-left / 60, -left % 60),
                            if (left >= 0) Tone.NEUTRAL else Tone.WARNING)
                    }
                }
                ProgressLine((i + 1f) / questions.size, Modifier.padding(top = 6.dp), height = 4.dp)
            }
        }
        item { Text(q.prompt, style = Type.title, color = p.text) }
        item {
            when (q) {
                is Question.Choice -> Options(q.options, answer, revealed, q.answer) { if (!revealed) answers[i] = it }
                is Question.Journal -> JournalPick(q, answer, revealed) { if (!revealed) answers[i] = it }
                is Question.Number -> NumberAnswer(answer ?: "", revealed) { if (!revealed) answers[i] = it }
                is Question.Order -> OrderBuilder(q, answer, revealed) { if (!revealed) answers[i] = it }
            }
        }
        if (revealed && !testMode) item {
            val ok = answer != null && QuestionBank.isCorrect(q, answer)
            Banner(q.explanation, if (ok) Tone.SUCCESS else Tone.DANGER, title = if (ok) "Right" else "Not quite")
        }
        item {
            val last = i == questions.lastIndex
            val ready = answer != null && answer.isNotBlank() && (q !is Question.Journal || answer.split("|").all { it.isNotBlank() })
            PrimaryButton(
                when {
                    !testMode && !revealed -> "Check"
                    last -> "See the result"
                    else -> "Next"
                },
                onClick = {
                    if (!testMode && !revealed) revealed = true
                    else if (last) finished = true
                    else { i++; revealed = false }
                },
                modifier = Modifier.fillMaxWidth(), enabled = ready,
            )
        }
        item { Muted(if (testMode) "Test mode: answers are shown at the end. Work cold, like the notebook test." else "Cold: no notes. You'll see the right answer after each question.") }
    }
}

@Composable
private fun Options(options: List<String>, selected: String?, revealed: Boolean, right: String, onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o -> OptionRow(o, selected == o, revealed && o == right, revealed && selected == o && o != right) { onPick(o) } }
    }
}

@Composable
private fun OptionRow(text: String, selected: Boolean, right: Boolean, wrong: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val border = when { right -> p.success; wrong -> p.danger; selected -> p.accent; else -> p.border }
    val bg = when { right -> p.successSoft; wrong -> p.dangerSoft; selected -> p.accentSoft; else -> p.surface }
    Text(text, style = Type.body, color = p.text,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bg).border(1.5.dp, border, RoundedCornerShape(12.dp))
            .clickable(role = Role.RadioButton, onClick = onClick).heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 12.dp))
}

@Composable
private fun JournalPick(q: Question.Journal, answer: String?, revealed: Boolean, onChange: (String) -> Unit) {
    val parts = (answer ?: "|").split("|")
    val dr = parts.getOrElse(0) { "" }
    val cr = parts.getOrElse(1) { "" }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Debit", style = Type.section, color = LocalPalette.current.text)
        q.debitOptions.forEach { o -> OptionRow(o, dr == o, revealed && o == q.debit, revealed && dr == o && o != q.debit) { onChange("$o|$cr") } }
        Text("Credit", style = Type.section, color = LocalPalette.current.text, modifier = Modifier.padding(top = 6.dp))
        q.creditOptions.forEach { o -> OptionRow(o, cr == o, revealed && o == q.credit, revealed && cr == o && o != q.credit) { onChange("$dr|$o") } }
    }
}

@Composable
private fun NumberAnswer(value: String, revealed: Boolean, onChange: (String) -> Unit) {
    Field(value, { if (!revealed) onChange(it.filter { c -> c.isDigit() || c == ',' || c == '.' }) }, "Your answer in ₹", singleLine = true, number = true)
}

@Composable
private fun OrderBuilder(q: Question.Order, answer: String?, revealed: Boolean, onChange: (String) -> Unit) {
    val p = LocalPalette.current
    val picked = answer?.split('\u0001')?.filter { it.isNotEmpty() } ?: emptyList()
    // Each token by position so repeated words work.
    val usedIdx = remember(q.id) { mutableStateListOf<Int>() }
    if (picked.isEmpty() && usedIdx.isNotEmpty()) usedIdx.clear()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LabCard(tint = p.surface2) {
            Text(if (picked.isEmpty()) "Tap the words in order" else picked.joinToString(" "), style = Type.title,
                color = if (picked.isEmpty()) p.text3 else p.text)
        }
        ChipRow {
            q.tokens.forEachIndexed { k, w ->
                if (k !in usedIdx) Chip(w, false, { if (!revealed) { usedIdx.add(k); onChange((picked + w).joinToString("\u0001")) } })
            }
        }
        if (picked.isNotEmpty() && !revealed) SecondaryButton("Clear", { usedIdx.clear(); onChange("") })
    }
}

// ------------------------------------------------------------------------------------------------- other checks

@Composable
private fun ScoreEntry(spec: CheckSpec.Score, onSave: (Int, Int, String) -> Unit) {
    val pct = spec.label == "%"
    var max by remember { mutableStateOf(if (spec.max > 0) spec.max.toString() else "") }
    var score by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf("") }
    val m = max.toIntOrNull() ?: 0
    LazyColumn(contentPadding = PaddingValues(ScreenPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Muted("Mark your notebook work first, then enter the score here." + if (spec.target > 0) " Target: ${spec.target}${if (pct) "%" else "/${spec.max}"}." else " Any score counts; the point is to see it change.") }
        if (spec.max <= 0) item { Field(max, { max = it.filter(Char::isDigit).take(3) }, "Out of how many?", singleLine = true, number = true) }
        item { Stepper(score, { score = it }, 0, if (pct) 100 else m.coerceAtLeast(0), if (pct) 5 else 1, if (pct) "%" else "/ $m", "Your score") }
        item { Field(note, { note = it }, "What went wrong, if anything (optional)", minLines = 2) }
        item { PrimaryButton("Save score", { onSave(score, if (pct) 100 else m, note) }, Modifier.fillMaxWidth(), enabled = pct || m > 0) }
    }
}

@Composable
private fun ChecklistEntry(criteria: List<String>, onSave: (List<String>) -> Unit) {
    val p = LocalPalette.current
    val ticked = remember { mutableStateListOf<String>() }
    LazyColumn(contentPadding = PaddingValues(ScreenPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Muted("Tick what's really true. Output counts, not effort.") }
        criteria.forEach { c ->
            item {
                Row(Modifier.fillMaxWidth().clickable { if (c in ticked) ticked.remove(c) else ticked.add(c) }.heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(c in ticked, { if (it) ticked.add(c) else ticked.remove(c) }, colors = CheckboxDefaults.colors(checkedColor = p.accent))
                    Text(c, style = Type.body, color = p.text)
                }
            }
        }
        item { PrimaryButton("Save (${ticked.size} of ${criteria.size})", { onSave(ticked.toList()) }, Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun RubricEntry(ui: UiState, spec: CheckSpec.Rubric, onSave: (List<Int>, String) -> Unit) {
    val p = LocalPalette.current
    val crit = ui.plan.meta.rubric
    val scores = remember { mutableStateListOf<Int>().apply { repeat(crit.size) { add(-1) } } }
    var note by remember { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(ScreenPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Muted("Listen once, then score each criterion 0–2. Target ${spec.target}/10. ${ui.plan.meta.rubricNote}") }
        crit.forEachIndexed { k, c ->
            item {
                LabCard {
                    Text(c.name, style = Type.section, color = p.text)
                    Spacer(Modifier.padding(top = 6.dp))
                    c.levels.forEachIndexed { s, desc ->
                        OptionRow("$s · $desc", scores[k] == s, false, false) { scores[k] = s }
                        Spacer(Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
        item {
            val total = scores.filter { it >= 0 }.sum()
            Text("Total: $total / 10" + if (scores.all { it >= 0 }) (if (total >= spec.target) " · target met" else " · below ${spec.target}") else "",
                style = Type.section, color = p.text)
        }
        item { Field(note, { note = it }, "One thing to change next time", minLines = 2) }
        item { PrimaryButton("Save score", { onSave(scores.toList(), note) }, Modifier.fillMaxWidth(), enabled = scores.all { it >= 0 }) }
    }
}

@Composable
private fun ExplainEntry(spec: CheckSpec.Explain, onSave: (Int, String) -> Unit) {
    var level by remember { mutableIntStateOf(-1) }
    var words by remember { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(ScreenPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { LabCard { Text(spec.prompt, style = Type.body, color = LocalPalette.current.text) } }
        item { Muted("Close your notes and any AI. Say it aloud, as if to a colleague. Then be honest:") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionRow("Yes, clearly, without notes", level == 2, false, false) { level = 2 }
                OptionRow("Partly: I got stuck or needed a peek", level == 1, false, false) { level = 1 }
                OptionRow("Not yet", level == 0, false, false) { level = 0 }
            }
        }
        item { Field(words, { words = it }, "Your explanation in a few words (optional)", minLines = 3) }
        item { PrimaryButton("Save", { onSave(level, words) }, Modifier.fillMaxWidth(), enabled = level >= 0) }
    }
}

@Composable
private fun DemoEntry(onSave: (String, Boolean) -> Unit) {
    var who by remember { mutableStateOf("") }
    var unaided by remember { mutableStateOf(true) }
    val p = LocalPalette.current
    LazyColumn(contentPadding = PaddingValues(ScreenPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Muted("Level 6 means you showed or explained it to someone else, unaided. No names needed.") }
        item { ChipRow { listOf("Manager", "A colleague", "Users (training)", "Recording sent").forEach { Chip(it, who == it, { who = it }) } } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Unaided (no notes or help)", style = Type.bodyStrong, color = p.text, modifier = Modifier.weight(1f))
                Switch(unaided, { unaided = it }, colors = SwitchDefaults.colors(checkedTrackColor = p.accent))
            }
        }
        item { PrimaryButton("Save", { onSave(who, unaided) }, Modifier.fillMaxWidth(), enabled = who.isNotBlank()) }
    }
}

@Composable
private fun OutcomeEntry(onSave: (Boolean) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(ScreenPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Muted("A conversation is started when your note with one real question is sent. It's useful once you've exchanged at least two messages.") }
        item { PrimaryButton("They replied: we've exchanged 2+ messages", { onSave(true) }, Modifier.fillMaxWidth()) }
        item { SecondaryButton("No reply yet", { onSave(false) }, Modifier.fillMaxWidth()) }
    }
}

@Suppress("unused")
private fun today() = Days.today()

@Suppress("unused")
@Composable
private fun Spacer8() = Spacer(Modifier.width(8.dp))

@Suppress("unused")
@Composable
private fun Line() = Divider()
