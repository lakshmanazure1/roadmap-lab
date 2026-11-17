# Roadmap Lab

A personal Android app that runs the **Fresh Start Career Roadmap: SAP Finance Edition**
(Tue 17 Nov 2026 – Sun 31 Jan 2027): what to do today, whether it's done, whether it's *learned*, how consistent
you've been in each of the 10 categories, and the evidence for your career move.

**Setup and install: see [SETUP.md](SETUP.md).**

## What's in version 3

| Tab / screen | What it holds |
|---|---|
| **Today** | Date, plan day (1–76), week focus, days left · **all 10 categories** (SAP, Accounting, Python, Career, French, Comms, Evidence, Health, Practice, Review), each with its own streak, a % ring and a done-today mark · **Yesterday** card: tick what you did yesterday and it counts for yesterday · minutes against today's minimum · mood 1–5 in one tap · one recommended next action · today's tasks in order · health, family and money (with "slept 7 h?") · daily practice · optional, not-today and paused lists · day options (reduced routine, catch-up day, plan a rest day) |
| **Category** (tap one) | Current and best streak, % done, last 7 and 28 days, on track (done ÷ due), a Mon–Sun calendar of the whole plan, anything not ticked in the last 7 days (tick it for its day), goals, dated checks, rules, levels, scores and every task |
| **Plan** | All 428 tasks from the CSV, week by week, filter by category, each week's checkpoint targets |
| **Progress** | Consistency, overall totals, weekly bars, the categories with streaks and "you are here" markers, needs attention, milestones, outputs |
| **Log** | Evidence log with the roadmap's 9 fields, the weekly ★, the 3 STAR stories (30 Nov, 30 Dec, 29 Jan); privacy warnings for PAN, GSTIN, IFSC, account and phone numbers; export as text |
| Weekly review | The Sunday checkpoint table, mood average, nights with 7 h sleep, the 5 go-live numbers, spending total, next week's load |
| Checks | French quizzes (this week's 6 words + last week's 6, the 20-word checks), your introduction, accounting MCQ and journal entries, calculations, Python, the 5-criterion rubric (Clarity, Structure, Accuracy, Audience, Delivery), SAP glossary score, checklists, demonstrations, LinkedIn replies |
| Settings | Light / dark / system · colours from your wallpaper (Material You) · daily minimum · cue and reminder · backup, restore (with a safety copy), start fresh / bring back · plan CSV import |

### The rules, in short
- **Follow the order, not the dates.** SAP Fridays, accounting, French, Python, career steps and Sunday recordings show
  the next task you haven't done, on the weekdays that category uses; a missed day moves it one slot later and never
  doubles up. Day-bound tasks (log line, work habits, SAP flashcards, health, practice) belong to their day; dated steps
  wait until done.
- **Category streaks.** A category's day counts when you complete any of its tasks that day (or its reduced-routine
  action). Days it has nothing planned, rest days and days before your fresh start are neutral. Near-daily categories
  forgive one miss a week; weekly ones (SAP, Career, Review) count every planned day. Forgot to tick? Mark it done for the
  day you did it (up to 7 days back).
- **Completion isn't mastery.** Levels 1–7 from the roadmap, plus *Needs revision* when the latest check is below target.
- **Health and daily practice are habits**, tracked as "most days", never counted as study.
- Dates use Africa/Kinshasa time.

## How it's built

- Kotlin + Jetpack Compose, **Material 3 / Material You**: dynamic colour from the wallpaper on Android 12+ (indigo
  baseline otherwise), Roboto with the M3 type scale, M3 cards, chips, buttons and motion.
- **Adaptive layout** by window size class: bottom navigation on phones (< 600 dp), a navigation rail on foldables
  (600–839 dp) and tablets, and a two-pane Today on tablets (≥ 840 dp).
- Room (SQLite) + DataStore; database version 2 (no schema change in v3: moods use the existing day log, go-live
  numbers the weekly review). Schemas are in `app/schemas/`.
- The plan is `app/src/main/assets/schedule.csv` (the roadmap's CSV, unchanged) plus `plan_meta.json`
  (made by `tools/build_plan.py` from the roadmap PDF's text). The Eight-Area generator is kept as `tools/build_plan_drc.py`.
- No internet permission. Backups go through Android's file picker (choose Google Drive). Practice data is fictional.
- Tests in `app/src/test` (run on every build): plan = CSV, weekly minutes = the roadmap's load check, today's list,
  category streaks and "done yesterday", streak engine, levels, question banks, backup, the database migration,
  repository integrity, and pictures of every screen on a phone, a small phone, a foldable and a tablet.

## Releasing a new version (for Claude)

1. Work on a branch: `ci.yml` runs all tests, draws every screen and saves pictures + results to the `ci-output` branch.
2. Change `VERSION_NAME` in `version.properties`, merge to `main`.
3. `build.yml` builds the signed APK and publishes release `vX.Y.Z` with the APK attached (once per version).
4. A database change needs a new schema version and a migration (never destructive).
