# Roadmap Lab

A personal Android app that runs the **Eight-Area Career Development Roadmap: DRC Edition**
(16 Nov 2026 – 31 Jan 2027): what to do today, whether it's done, whether it's *learned*, how consistent
you've been, and the evidence for your career move.

**Setup and install: see [SETUP.md](SETUP.md).**

## What's in version 2

| Tab / screen | What it holds |
|---|---|
| **Today** | Date, plan day (1–77), week focus, days left · minutes done against today's minimum (ring) · one recommended next action with the reason · today's tasks in order with area, time and priority · health, family and money (with "slept 7 h?") · optional, not-today and paused lists · review the day · day options (reduced routine, catch-up day, plan a rest day) |
| **Plan** | All 380 tasks from the CSV, week by week, filter by area, each week's checkpoint targets; tick anything already done |
| **Progress** | Consistency (current and longest streak, active days this week, 7- and 28-day %, last 28 days) · overall (tasks done, planned by today, study time, weekly bars, monthly totals) · the 8 areas with "you are here" markers, scores and levels · needs attention · next milestones · outputs (LinkedIn conversations, logged working days, health counts) |
| **Log** | Evidence log with the roadmap's 9 fields, confidence, the weekly ★, the 3 STAR stories with a format check; privacy warnings for PAN, GSTIN, IFSC, account numbers, phone numbers and e-mail; export as text |
| Task | What to do, done when, evidence; the 7-level scale; mark done (time, done on, without help); started / did part / not today / let go; every check with its history |
| Checks | In-app quizzes (French words both ways, translate-back sentence builder, accounting MCQ, journal entries Dr/Cr, fresh calculations, Python/RAG), timed tests, notebook scores against the CSV's targets, the 5-criterion rubric (out of 10), checklists, "explain it without notes", demonstrations, LinkedIn replies |
| Weekly review | The Sunday checkpoint table: numbers filled in, actual result / assessment / next week per area, obstacles, best, weakest, top 3, spending, next week's load (normal or reduced), suggestions from the plan's own rules |
| Flashcards | Leitner spaced repetition; cards unlock with the plan's tasks; add your own |
| Settings | Light / dark / system · daily minimum per kind of day · cue and reminder · backup, restore (with a safety copy), export · plan CSV import · where your data lives |

### The rules, in short
- **Follow the order, not the dates.** Numbered work shows the next task after where you are, on the weekdays that
  area uses; a missed day moves it one slot later and never doubles up. Day-bound tasks belong to their day; dated
  steps (STAR stories, money checks, trainings, reviews) wait until done.
- **Completion isn't mastery.** Levels: 1 Not started · 2 In progress · 3 Completed · 4 Practised independently ·
  5 Assessed · 6 Demonstrated independently · 7 Mastered, plus *Needs revision* when the latest check is below target.
- **Streak:** a day counts when completed study reaches *your* minimum for that kind of day. Rest days and the reduced
  routine never break it; one missed day a week is forgiven; the longest streak is never erased. Dates use
  Africa/Kinshasa time.

## How it's built

- Kotlin + Jetpack Compose (Material 3), minSdk 26, targetSdk 35; IBM Plex Sans/Mono (SIL OFL, see `licenses/`)
- Room (SQLite) + DataStore. Version 2 adds 6 tables through `AutoMigration(1 → 2)`; every version-1 table is kept,
  and version-1 evidence lines are copied into the new log. Schemas are saved in `app/schemas/`.
- The plan is `app/src/main/assets/schedule.csv` (the roadmap's CSV, unchanged) plus `plan_meta.json`
  (made by `tools/build_plan.py` from the roadmap PDF: areas, rubric, French words, checkpoints, question banks).
- No internet permission. Backups go through Android's file picker (choose Google Drive).
- Tests in `app/src/test` (run on every build): plan = CSV, today's list, streak engine, levels, question banks,
  backup format, scenario tests, the 1 → 2 database migration, repository data integrity, and pictures of every screen.

## Releasing a new version (for Claude)

1. Work on a branch: `ci.yml` runs all tests, draws every screen and saves pictures + results to the `ci-output` branch.
2. Change `VERSION_NAME` in `version.properties`, merge to `main`.
3. `build.yml` builds the signed APK and publishes release `vX.Y.Z` with the APK attached (once per version).
4. A database change needs a new schema version and a migration (never destructive).
