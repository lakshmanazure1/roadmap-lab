#!/usr/bin/env python3
"""
Builds app/src/main/assets/plan_meta.json for the "Fresh Start Career Roadmap: SAP Finance Edition"
(Tue 17 Nov 2026 – Sun 31 Jan 2027).

The schedule itself is the companion CSV (Fresh_Start_Schedule_17Nov2026-31Jan2027.csv), bundled unchanged as
app/src/main/assets/schedule.csv: the app reads every row from it (428 tasks, T1001–T1428), so the task count
always equals the CSV row count. This file adds what the CSV doesn't hold, copied from the roadmap PDF
(text in tools/source/Fresh_Start_Career_Roadmap_SAP_Finance_Edition.txt): the 10 categories' goals, the
5-criterion rubric, the French words, weekly checkpoint targets, the reduced routine, success criteria and rules,
plus short titles and the question banks used for in-app checks (fictional data only).

Nothing here adds a task or changes a date. Run: python3 tools/build_plan.py
"""
import csv
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CSV = ROOT / "app/src/main/assets/schedule.csv"
PDF_TEXT = ROOT / "tools/source/Fresh_Start_Career_Roadmap_SAP_Finance_Edition.txt"
OUT = ROOT / "app/src/main/assets/plan_meta.json"
VERSION = "fresh-start-sap-2026-11-17"

rows = list(csv.DictReader(open(CSV, encoding="utf-8")))
assert len(rows) == 428, len(rows)

AREA_IDS = {"SAP Finance": "sap", "Accounting": "accounting", "Python/AI": "python", "Career": "career", "French": "french",
            "Communication": "comms", "Evidence log": "evidence", "Health/family/money": "life", "Practice": "practice",
            "Weekly review": "review"}

# ------------------------------------------------------------------------------------------- the 10 categories (sections 1, 4–12, 15)
AREAS = [
    dict(id="sap", name="SAP Finance", short="SAP", objective="SAP vocabulary and processes from day one",
         byEnd="10 SAP Fridays; 25-term glossary quiz 20/25; P2P explained in SAP words (7/10)",
         budget="Friday 40 + Tuesday flashcards 10 (50 a week)",
         assessed="Tuesday flashcards: 8 of 10 without looking. Check 1: 25-term glossary quiz 20/25, and P2P explained aloud "
                  "in 2 minutes, 7/10 on the rubric",
         notEnough="", success="10 SAP Fridays; glossary quiz 20/25; P2P explained in SAP words (7/10)", targetLevel="4–5",
         stretch="Explain O2C and data migration in SAP words too",
         rules=["Every Friday evening (40 min): one SAP topic from the free learning journeys on learning.sap.com, mapped to what "
                "you already do in your current ERP.",
                "Add 5 terms to your glossary deck every Friday. Every Tuesday (10 min): last Friday's 5 terms + 5 older ones, said "
                "aloud; missed cards go to the front.",
                "Download the next week's SAP pages during Sunday's review, so poor internet never blocks Friday.",
                "Fri 25 Dec is a rest day, so there is no SAP Friday in week 6."]),
    dict(id="accounting", name="Accounting", short="Accounting", objective="Strong fundamentals for SAP Finance",
         byEnd="NPTEL 1.2 to 3.8; journal and ledger; test #2 10/12 cold", budget="4 × 30 min, Mon–Thu (120 a week); Friday is SAP",
         assessed="Monday cold quizzes, monthly checks and cumulative tests, with no answers in view",
         notEnough="Watching a lecture", success="1.2 to 3.8 done; test #2 10/12 cold", targetLevel="5",
         stretch="12/12, and you can explain each entry",
         rules=["Watching a lecture is not mastery. Every lecture is followed by work you do before seeing the answer.",
                "Monday starts with a cold 10-card quiz on the previous week. Mistakes become new flashcards.",
                "Ravi Traders is your fictional business for all practice. Use made-up amounts.",
                "Lecture numbers follow the IIT Mandi NPTEL course list. If your run differs, keep the same topic order."]),
    dict(id="python", name="Python and AI", short="Python / AI", objective="Go-live Data Kit: validator, retrieval assistant, SQL",
         byEnd="Demo #1 and Demo #2 sent; retrieval 16/20 then 20/25; reconciliation report; template spec v0",
         budget="Saturday 120 + Sunday 90 (210 a week)",
         assessed="Monthly no-AI checks: functions written from a blank file and explained aloud; retrieval scored on fixed test questions",
         notEnough="Code that runs but you can't explain",
         success="Demo #1 and Demo #2 sent; validator finds every planted error; retrieval 16/20 then 20/25; reconciliation report",
         targetLevel="5–6", stretch="Template spec drives several checks",
         rules=["Every session starts with revision (15 min Saturday, 10 min Sunday): rewrite last session's main code from memory, "
                "then compare.",
                "Predict the output before you run anything. Type the code yourself. Stuck for 15 minutes? Ask for a hint, not the solution.",
                "Fake data only. Save code to your private GitHub repo and to Drive after each session. Monthly checks are done with no AI.",
                "Retrieval offline: keyword retrieval in plain Python, then TF-IDF; the answer is the top 3 chunks with their sources. "
                "Online only with a company-approved API key, kept in a .env file, never in your code.",
                "Describe it honestly: a “retrieval assistant with sources”, not advanced AI."]),
    dict(id="career", name="Career", short="Career", objective="Salary case and job-search basics",
         byEnd="Gap story; resume v0; salary targets; 4 LinkedIn conversations; role case sent", budget="Sunday 30",
         assessed="Each step's “done when”; messages sent and replies", notEnough="",
         success="Gap story said without notes; resume v0; salary targets; 4 conversations; role case sent", targetLevel="5",
         stretch="Manager names clear criteria for April",
         rules=["Keep official titles and dates; describe only what you really did. Use one calm, two-sentence answer for the gap.",
                "Never invent SAP experience, titles or dates. Interviewers probe for it and background checks verify it.",
                "December questions for your manager: What would I need to show to move up a level by April? Which results count "
                "most? May I publish work built only on fake data?",
                "Check your contract before publishing anything; build on personal time and devices.",
                "Never resign without a written offer."]),
    dict(id="french", name="French", short="French", objective="Workplace French for DRC",
         byEnd="60 work words; weekly quizzes; 1-minute introduction 6/10", budget="3 × 10 min: Mon, Wed, Thu (30 a week)",
         assessed="Thursday quizzes, both directions; the December and week-11 checks 16/20; your introduction on the rubric",
         notEnough="", success="60 words; checks 16/20; introduction 6/10", targetLevel="4–5", stretch="A 5-minute work chat in French",
         rules=["Monday: learn the week's 6 words and write a sentence with each.",
                "Wednesday: say your sentences aloud 3 times, use 2 words at work, record 1 minute.",
                "Thursday: quiz on this week's and last week's words."]),
    dict(id="comms", name="Communication", short="Comms", objective="Clear, confident professional communication",
         byEnd="Habits most days; Sunday recordings 7/10; 2 user trainings", budget="Habits inside work · Sunday recording 10",
         assessed="Recordings, demos and trainings scored on the rubric (out of 10); each habit's “done when”", notEnough="",
         success="Habits on most days; recordings 7/10; 2 trainings delivered", targetLevel="5–6", stretch="Speaking early feels normal",
         rules=["Mon: speak early, one question or point in a meeting's first 10 minutes · Tue: restate one request, “So to confirm…” · "
                "Wed: same-day written summary, decided · who · by when · Thu: explain one process twice, simple then detail · "
                "Fri: concise update, done · next · blocker, under 30 seconds.",
                "Sunday: plan 3 points (problem → what you did → result), then record 2 minutes.",
                "Rubric targets: 6/10 or better in November–December, 7/10 or better in January.",
                "User trainings (10–15 min) if the go-live schedule allows; otherwise the next slot. Prepare the day before."]),
    dict(id="evidence", name="Evidence log", short="Evidence", objective="Verifiable evidence of growth",
         byEnd="A line every day with mood; weekly go-live numbers; 3 STAR stories", budget="About 5 a day (40 a week)",
         assessed="A line has a real result with a number, not just “worked on X”", notEnough="“Worked on X”",
         success="A line on 80% of days; weekly go-live numbers; 3 STAR stories", targetLevel="5",
         stretch="Manager uses one of your stories",
         rules=["One line every day with the 9 fields (date, area, task, problem, action, result, skill, evidence, lesson), plus a "
                "mood score from 1 to 5.",
                "Every Sunday, star the best line of the week (★) and write the week's 5 go-live numbers: records loaded · rows "
                "rejected then fixed · users trained · issues closed · hours saved.",
                "At month end, turn the starred lines into a STAR story (under 150 words): #1 Mon 30 Nov · #2 Wed 30 Dec · #3 Fri 29 Jan."]),
    dict(id="life", name="Health · family · money · social", short="Health", objective="Sustainable progress and a life outside work",
         byEnd="Sleep and exercise most days; savings automated; 3 money checks; doctor or counsellor conversation; marriage process agreed",
         budget="Exercise 3 × 25 · alone time 2 × 15 · 60 off on Saturday · call home 25 (190 a week)",
         assessed="Habits ticked; nights with 7 h of sleep", notEnough="",
         success="Sleep 7 h most nights; exercise 2–3 times most weeks; savings automated; 3 money checks; health conversation held; "
                 "marriage process agreed", targetLevel="4", stretch="No week without a family call or a social contact",
         rules=["Every day: 7 hours of sleep with a fixed lights-out time · treated water · malaria prevention · 10 minutes of daylight.",
                "Mon · Wed · Fri: exercise 20–30 min (2 is fine in busy weeks). Tue · Thu: 10–20 minutes alone time, no screen. "
                "Wednesdays: one meal or walk with a colleague. Saturday: 60 minutes fully off. Sunday: call home, and one message "
                "or call to someone on your list of 10.",
                "Money plan: emergency fund of 6 × your essential monthly costs, before January 2028. Automatic transfers the day after "
                "salary (from 6 Dec): one to the emergency fund, one to an exam and practice fund. Avoid rented “SAP server access” "
                "and any “placement guarantee” courses.",
                "If low mood or poor sleep lasts more than two weeks, see a doctor or counsellor, through your company's medical "
                "contact in DRC. This is planned for 20 Dec, not left to chance. In India, Tele-MANAS is free and open 24×7: 14416 "
                "or 1-800-891-4416 (an Indian number; it may not connect from a Congolese SIM).",
                "Protect health, sleep and family responsibilities. No streak is worth losing them."]),
    dict(id="practice", name="Daily practice", short="Practice", objective="Calm daily rhythm and simple remedies",
         byEnd="Morning mantra and silence most days; weekly practices kept", budget="10–15 a day (75 a week)",
         assessed="Done (y/n)", notEnough="", success="Morning practice on most days", targetLevel="4", stretch="Every day",
         rules=["No gemstones and no paid pujas. About 10 minutes a day (15 on Mondays).",
                "Every morning: Om Gam Ganapataye Namah ×11 · 5 minutes of silence · one line: what you will finish today.",
                "Mon: Om Namah Shivaya ×108; a call or message to your mother · Tue: Hanuman Chalisa; hard exercise; no angry messages · "
                "Fri: Om Shum Shukraya Namah ×27; tidy room and clothes; one small kindness · Sat: one act of service (monthly, 5 Dec "
                "and 2 Jan: a gift in kind from a small fixed budget) · Sun: feed a stray dog or birds; write 3 things that went right.",
                "Tracked as a habit (most days), never as study time."]),
    dict(id="review", name="Weekly review", short="Review", objective="Honest weekly checkpoint",
         byEnd="11 checkpoint tables filled", budget="Sunday 15",
         assessed="Every area has an actual result written, not just a tick", notEnough="Ticks without results", success="",
         targetLevel="", stretch="",
         rules=["Fill in the checkpoint table during the Sunday review, writing what actually happened, not just ticks.",
                "Spending total · mood average · nights with 7 h sleep · next week's load: normal or reduced · download next week's "
                "lectures and SAP pages."]),
]

# Section 9: the rubric for recordings, demos and trainings (0–2 each, out of 10).
RUBRIC = dict(
    criteria=[
        dict(name="Clarity", levels=["Main point unclear", "Main point appears late", "Main point in the first 15 seconds"]),
        dict(name="Structure", levels=["No order", "Some order", "Problem → what you did → result"]),
        dict(name="Accuracy", levels=["A technical error", "Small slip", "Every fact checked against your notes"]),
        dict(name="Audience", levels=["Jargon, no example", "One example", "A non-expert could repeat it; one concrete example"]),
        dict(name="Delivery", levels=["Rushed or read out", "Some pauses", "Calm pace; eye contact or steady voice"]),
    ],
    note="Targets: 6/10 or better in November–December, 7/10 or better in January.",
)

# ------------------------------------------------------------------------------------------- French (section 9)
G = lambda *p: [dict(fr=f, en=e) for f, e in p]
FRENCH = [
    dict(week=1, theme="Greetings and basics", words=G(("bonjour", "hello"), ("merci", "thank you"), ("s'il vous plaît", "please"),
         ("excusez-moi", "excuse me, sorry"), ("je m'appelle", "my name is"), ("enchanté", "nice to meet you")), sentences=[]),
    dict(week=2, theme="Meetings and time", words=G(("une réunion", "a meeting"), ("aujourd'hui", "today"), ("demain", "tomorrow"),
         ("le matin", "the morning"), ("l'après-midi", "the afternoon"), ("la semaine", "the week")), sentences=[]),
    dict(week=3, theme="The system", words=G(("le système", "the system"), ("l'écran", "the screen"), ("l'utilisateur", "the user"),
         ("le champ obligatoire", "the mandatory field"), ("enregistrer", "to save"), ("le mot de passe", "the password")), sentences=[]),
    dict(week=4, theme="Purchasing", words=G(("un fournisseur", "a vendor, supplier"), ("un bon de commande", "a purchase order"),
         ("la quantité", "the quantity"), ("le prix", "the price"), ("la réception", "the goods receipt"), ("une facture", "an invoice")),
         sentences=[]),
    dict(week=5, theme="Accounting", words=G(("une écriture", "an accounting entry"), ("le débit", "the debit"), ("le crédit", "the credit"),
         ("un compte", "an account"), ("le solde", "the balance"), ("le grand livre", "the general ledger")), sentences=[]),
    dict(week=6, theme="Review: the 30 words from weeks 1–5", words=[], sentences=[]),
    dict(week=7, theme="Support and problems", words=G(("un problème", "a problem"), ("un message d'erreur", "an error message"),
         ("bloqué", "stuck, blocked"), ("une capture d'écran", "a screenshot"), ("vérifier", "to check"), ("corrigé", "fixed, corrected")),
         sentences=[]),
    dict(week=8, theme="Training users", words=G(("une formation", "a training"), ("une étape", "a step"), ("d'abord", "first"),
         ("ensuite", "next, then"), ("un exemple", "an example"), ("essayer", "to try")), sentences=[]),
    dict(week=9, theme="Numbers and money", words=G(("dix", "ten"), ("vingt", "twenty"), ("cinquante", "fifty"), ("cent", "a hundred"),
         ("mille", "a thousand"), ("le taux de change", "the exchange rate")), sentences=[]),
    dict(week=10, theme="Go-live and data", words=G(("le démarrage", "the go-live"), ("les données de base", "master data"),
         ("un modèle", "a template"), ("une colonne", "a column"), ("un doublon", "a duplicate"), ("une sauvegarde", "a backup")),
         sentences=[]),
    # Week 11: review + the 1-minute introduction (T1402), whose sentences are built in the app.
    dict(week=11, theme="Review and your introduction", words=[],
         sentences=G(("Je m'appelle Nani.", "My name is Nani."),
                     ("Je travaille sur le démarrage du système.", "I work on the system go-live."),
                     ("Je vérifie les données et je forme les utilisateurs.", "I check the data and train the users."),
                     ("Je parle un peu français.", "I speak a little French."),
                     ("J'apprends chaque semaine.", "I'm learning every week."))),
]


# ------------------------------------------------------------------------------------------- weekly checkpoints (section 13)
def checkpoints():
    lines = PDF_TEXT.read_text(encoding="utf-8").split("\n")
    names = [("SAP Finance", "sap"), ("Accounting", "accounting"), ("Python / AI", "python"), ("Career", "career"), ("French", "french"),
             ("Comms", "comms"), ("Evidence log", "evidence"), ("Health ·", "life")]
    out = {}
    for i, l in enumerate(lines):
        m = re.search(r"Week (\d+) checkpoint · Sunday review", l)
        if not m:
            continue
        res, cur = {}, None
        for x in lines[i + 3:]:
            if "Planned / done" in x:
                break
            if not x.strip() or "Fresh Start Career Roadmap" in x or x.strip().startswith("Area "):
                continue
            hit = next(((n, a) for n, a in names if x.lstrip().startswith(n)), None)
            # The health row's name runs over 3 lines ("Health · / family · / money"): drop those words.
            if hit:
                cur = hit[1]
                text = x.strip()[len(hit[0]):].strip()
            else:
                text = x.strip()
            text = re.sub(r"^(?:family ·|money)(?:\s+|$)", "", text)
            if hit:
                res[cur] = text
            elif cur:
                res[cur] = (res[cur] + " " + text).strip()
        out[int(m.group(1))] = {a: re.sub(r"\s+", " ", t).strip() for a, t in res.items()}
    return out


CHECKPOINTS = checkpoints()
assert sorted(CHECKPOINTS) == list(range(1, 12)) and all(len(v) == 8 for v in CHECKPOINTS.values()), CHECKPOINTS
# Section 4, "Load check, week by week".
WEEK_FOCUS = {1: "Fresh start · the validator finds all 10 errors", 2: "Manager update", 3: "Retrieval begins · automatic savings",
              4: "Better retrieval · user training #1", 5: "The December conversation · retrieval 16/20", 6: "Demo #1 (holiday week)",
              7: "SQL and resume v0 (holiday week)", 8: "Validator v2 · salary targets", 9: "SQL joins and reconciliation · training #2",
              10: "Migration template spec · role case draft", 11: "Demo #2 and the role case"}

# Section 14: the reduced routine for busy go-live weeks (about 20 minutes a day). days: 1 = Monday … 7 = Sunday.
REDUCED = [
    dict(id="red-evidence", area="evidence", action="One line every day, with mood: busy weeks produce the best evidence", time="5 min/day",
         minutes=5, days=[1, 2, 3, 4, 5, 6, 7]),
    dict(id="red-accounting", area="accounting", action="Flashcards only, no new lecture", time="15 min/day", minutes=15, days=[1, 2, 3, 4]),
    dict(id="red-sap", area="sap", action="Tuesday flashcards only; Friday topic moves to next week", time="10 min/week", minutes=10, days=[2]),
    dict(id="red-french", area="french", action="2 words used at work; Thursday quiz", time="10 min/week", minutes=0, days=[1, 2, 3, 4]),
    dict(id="red-comms", area="comms", action="“So to confirm…” once a day (it saves you time)", time="at work", minutes=0, days=[1, 2, 3, 4, 5]),
    dict(id="red-python", area="python", action="Saturday: 15-min revision + finish only the current step. Sunday off", time="60 min",
         minutes=60, days=[6]),
    dict(id="red-career", area="career", action="Only fixed-date steps (manager email, December conversation); everything else waits",
         time="as needed", minutes=0, days=[]),
    dict(id="red-life", area="life", action="Sleep first; two 10-minute walks; Sunday call home", time="—", minutes=0, days=[1, 2, 3, 4, 5, 6, 7]),
    dict(id="red-practice", area="practice", action="Morning mantra and silence only", time="5 min/day", minutes=5, days=[1, 2, 3, 4, 5, 6, 7]),
    dict(id="red-review", area="review", action="10 min Sunday: which week to repeat", time="10 min", minutes=10, days=[7]),
]
RECOVERY = ["Missed a day? Don't double up. Do the next task in order tomorrow.",
            "Missed a week? Repeat the Monday quiz first, then continue where you stopped.",
            "Two reduced weeks in a row? Shift every later date by one week. The sequence stays; only dates move.",
            "Missing a week is fine. Don't let it become a month."]
# Section 16: rules that don't change.
RULES = ["Fictional data only for all practice.",
         "Never put client names, PAN, GSTIN, bank details, invoices or confidential internal information into external AI tools or "
         "personal repositories, unless the company expressly approves it.",
         "Check your contract before publishing anything; build on personal time and devices.",
         "Type and understand code; don't copy AI output blindly. Predict the output before running it.",
         "Stuck for 15 minutes? Ask for a hint, not a complete solution.",
         "Never invent experience, titles or dates.",
         "Never resign without a written offer.",
         "Keep the plan manageable during go-live periods: use the reduced routine without guilt.",
         "Recover after missed days by continuing in order, not by doubling up.",
         "Protect health, sleep and family responsibilities. No streak is worth losing them.",
         "No new subjects unless this plan is reviewed first.",
         "Private GitHub repos; the app's signing key only in GitHub Secrets and your private Drive."]
# The dated checks of each month, collected from sections 5–12 (summary only; the tasks are the CSV's).
MONTHLY = {
    "sap": ["Fri 20 Nov: first SAP Friday", "SAP Fridays: organisation, master data, P2P, P2P accounting",
            "Fri 29 Jan: SAP check 1 — glossary quiz 20/25; P2P aloud 7/10"],
    "accounting": ["Mon 30 Nov: November check 16/20", "Mon 21 Dec: test #1 8/10 · Mon 4 Jan: self-test 8/10", "Thu 28 Jan: test #2 10/12"],
    "python": ["Sun 22 Nov: validator finds all 10 errors · Sat 28 Nov: no-AI check #1",
               "Sun 20 Dec: retrieval 16/20 · Sat 26 Dec: no-AI check #2 · Sun 27 Dec: Demo #1",
               "Sun 24 Jan: retrieval 20/25 · Sat 30 Jan: no-AI check #3 · Sun 31 Jan: Demo #2"],
    "career": ["LinkedIn headline and About; gap story; contract check", "December conversation; LinkedIn messages 1–2",
               "Resume v0; salary targets; messages 3–4; role case sent (31 Jan)"],
    "french": ["Thursday quizzes 10/12", "Thu 24 Dec: December check 16/20", "Mon 25 Jan: review 16/20 · Thu 28 Jan: introduction 6/10"],
    "comms": ["Sunday recordings 6/10; manager update (29 Nov)", "Training #1 (10 Dec); Demo #1 video", "Training #2 (14 Jan); recordings 7/10; Demo #2 video"],
    "evidence": ["STAR #1 (Mon 30 Nov)", "STAR #2 (Wed 30 Dec)", "STAR #3 (Fri 29 Jan); a line on 80% of days"],
    "life": ["Lights-out time set · money check #1 (30 Nov)", "Automatic savings (6 Dec) · health conversation (20 Dec) · money check #2 (31 Dec)",
             "Money check #3 (29 Jan) · marriage process agreed"],
    "practice": ["Morning practice most days", "Gift in kind (5 Dec)", "Gift in kind (2 Jan)"],
}
LEVELS = [
    dict(level=1, name="Not started", meaning="Nothing done yet"),
    dict(level=2, name="In progress", meaning="Started, not finished"),
    dict(level=3, name="Completed", meaning="Watched / built once, possibly with help"),
    dict(level=4, name="Practised independently", meaning="Done again without help or answers"),
    dict(level=5, name="Assessed", meaning="Passed a timed or marked check"),
    dict(level=6, name="Demonstrated independently", meaning="Shown or explained to someone else, unaided"),
    dict(level=7, name="Mastered", meaning="Consistent over time, after a gap"),
]
# Section 4: your weekly routine. [day, morning, at work, evening study, study time]
WEEK_ROUTINE = [
    ["Mon", "Mantra + silence · exercise 20–30 min", "Speak early in a meeting",
     "Accounting 30 (cold quiz first) · French 10 · Log 5 · Om Namah Shivaya, call your mother", "45 min"],
    ["Tue", "Mantra + silence", "“So to confirm…”", "Accounting 30 · SAP flashcards 10 · Log 5 · alone time · Hanuman Chalisa", "45 min"],
    ["Wed", "Mantra + silence · exercise 20–30 min", "Same-day written summary",
     "Accounting 30 · French 10 · Log 5 · meal or walk with a colleague this week", "45 min"],
    ["Thu", "Mantra + silence", "Explain one thing twice", "Accounting 30 · French quiz 10 · Log 5 · alone time", "45 min"],
    ["Fri", "Mantra + silence · exercise 20–30 min", "3-sentence update", "SAP Friday 40 · Log 5 · Venus mantra, tidy room, one kindness",
     "45 min"],
    ["Sat", "Mantra + silence · one act of service", "—", "Python / AI 2 h · Log 5 · 60 min fully off", "2 h 05"],
    ["Sun", "Mantra + silence · feed animals", "—", "Python / AI 1.5 h · Career 30 · Recording 10 · Log 5 · Review 15", "2 h 30"],
]

# ------------------------------------------------------------------------------------------- short titles for the bundled CSV
PY_TITLES = {"T1023": "Recap without AI · validate_gstin() shape check", "T1027": "PAN inside GSTIN: all 10 errors found",
             "T1063": "No-AI check #1 · error_report.xlsx · run_all()", "T1067": "User guide, go-live notes, manager update",
             "T1103": "Retrieval 1: chunks with their source", "T1107": "Retrieval 2: keyword retrieval, top 3",
             "T1143": "Retrieval 3: TF-IDF retrieval", "T1147": "Retrieval 4: answers name the source",
             "T1184": "Retrieval test: 20 questions", "T1188": "Retrieval 16/20 · validator + assistant",
             "T1221": "No-AI check #2 · demo script", "T1225": "Demo #1: 3-minute Data Kit video",
             "T1258": "SQL with SQLite: 5 queries", "T1262": "Duplicates in SQL and pandas · v2 file",
             "T1298": "IFSC, mobile and email checks · try / except", "T1302": "Regression test on both files",
             "T1338": "SQL joins: find orphan bank rows", "T1342": "Reconciliation report",
             "T1378": "template_spec.csv v0", "T1382": "Extend the assistant: 20/25",
             "T1418": "No-AI check #3 · record Demo #2", "T1422": "Send Demo #2 · tidy the repo"}
SAP_TITLES = {"T1018": "SAP Friday: set up and the landscape", "T1058": "SAP Friday: organisation", "T1098": "SAP Friday: master data",
              "T1138": "SAP Friday: P2P in SAP", "T1179": "SAP Friday: the accounting side of P2P",
              "T1293": "SAP Friday: G/L accounts and a journal entry in SAP", "T1333": "SAP Friday: data migration in SAP",
              "T1373": "SAP Friday: O2C in SAP", "T1413": "SAP check 1: glossary quiz · P2P aloud"}


def clean(t):
    t = re.sub(r"\s*\((?:rewatch|answer|solve|training day|10 min|15 min|25 min)[^)]*\)", "", t)
    t = t.replace("NPTEL ", "")
    return re.sub(r"\s+", " ", t).strip(" ·")


def title(r):
    tid, area, learn, build = r["task_id"], AREA_IDS[r["area"]], r["learn_practise"], r["build_deliver"]
    learn = "" if learn == "—" else learn
    build = "" if build == "—" else build
    if area == "python":
        return PY_TITLES[tid]
    if area == "sap":
        if learn.startswith("SAP flashcards"):
            return "SAP flashcards: 5 new + 5 older terms"
        return SAP_TITLES[tid]
    if area == "accounting":
        if learn.startswith("November check"):
            return "November check: 20-card quiz · 1.10 Income Statement"
        if learn.startswith("December check"):
            return "December check: 10-entry self-test"
        if learn.startswith("January check"):
            return "January check: mark test #2"
        if learn.startswith("Quiz:"):
            parts = learn.split(" · ", 1)
            lead = "Which side? quiz" if "which side" in parts[0] else "Cold quiz"
            return lead + (" · " + clean(parts[1]) if len(parts) > 1 else "")
        if learn == "Light day":
            return "Light day: 15 min flashcards"
        if learn == "Part 2":
            return "Test #2, part 2: post and balance"
        if learn.startswith("No video: cumulative test #2"):
            return "Test #2, part 1: 12 transactions → journal"
        if learn.startswith("No video: "):
            t = clean(learn.split(": ", 1)[1])
            return t[:1].upper() + t[1:]
        if learn.startswith("2.6 (second half)"):
            return "2.6 Journal entries (second half)"
        return clean(learn)
    if area == "career":
        return re.sub(r"\s*\([^)]*\)", "", learn).strip()
    if area == "french":
        m = re.match(r"Week (\d+) · ([^:]+): (.+)", learn)
        if m and "Review" not in m.group(2):
            return f"Week {m.group(1)} · {m.group(2)}: 6 new words"
        if learn.startswith("Week 6 · Review"):
            return "Week 6 review: translate 20 words"
        if learn.startswith("Week 11 · Review"):
            return "Week 11 review: 20 words (weeks 7–10)"
        if learn.startswith("Say your 6 sentences"):
            return "Say your 6 sentences · record 1 min"
        if learn.startswith("Say 5 sentences"):
            return "Say 5 work sentences · record 1 min"
        if learn.startswith("Quiz: this week's 6"):
            return "Quiz: 12 words, both directions"
        if learn.startswith("December French check"):
            return "December check: 20 words"
        if learn.startswith("Your 1-minute"):
            return "Your 1-minute introduction"
        if learn.startswith("January French check"):
            return "January check: your introduction"
        raise ValueError(tid)
    if area == "comms":
        if r["weekday"] == "Sun":
            topic = build.split(": ", 1)[1] if ": " in build else build
            topic = re.sub(r"\s*\([^)]*\)", "", topic).replace(" in 30 seconds", "")
            return ("Recording: " + topic[:1].upper() + topic[1:])[:72].rstrip(" ,")
        if learn.startswith("User training"):
            return learn.split(" (")[0] + ": " + build.split(":")[0]
        if learn.startswith("Speak early"):
            return "Speak early in a meeting"
        if learn.startswith("Restate"):
            return "Restate: “So to confirm…”"
        if learn.startswith("Explain one process"):
            return "Explain one process twice"
        if learn == "Concise writing":
            return "Same-day summary + training prep" if "prepare training" in build else "Same-day written summary"
        if learn == "Concise update":
            return "Concise update: done · next · blocker"
        return clean(learn)
    if area == "evidence":
        if learn.startswith("Set up the log"):
            return "Set up the log · first line with mood"
        star = re.search(r"STAR #(\d)", build)
        if star:
            return f"Log line + mood · STAR #{star.group(1)}"
        if learn.startswith("Star the best"):
            return "★ Star the week's best line · 5 go-live numbers"
        if "Python session" in build:
            return "Log today's Python session + mood"
        return "Today's log line + mood"
    if area == "life":
        if learn.startswith("Rest day"):
            return learn.split(":")[0]
        if "lights-out" in build:
            return "Choose a fixed lights-out time"
        if "MONEY:" in build:
            m = re.search(r"Money check #\d", build)
            return (m.group(0) + " · " + build.split(" (")[0].split(" · ")[0].lower()) if m else "Call home · set up automatic savings"
        if "HEALTH:" in build:
            return "Call home · talk to a doctor or counsellor"
        if "FAMILY:" in build:
            return "Call home · family step"
        if learn == "Recovery":
            return "60 min fully off" + (" · group activity" if "group activity" in build else "")
        if learn.startswith("Family"):
            return "Call home · list of 10 people" if "write a list" in build else "Call home · one reach-out"
        return build.split(" (")[0].split(" · ")[0]
    if area == "practice":
        if learn.startswith("Tonight"):
            return "First practice tonight · Hanuman Chalisa"
        extra = {"Hanuman Chalisa": "Hanuman Chalisa", "Om Shum Shukraya": "Venus mantra, tidy room", "One act of service": "one act of service",
                 "Feed a stray": "feed animals, 3 good things", "Evening: Om Namah Shivaya": "Om Namah Shivaya, call your mother"}
        hit = next((v for k, v in extra.items() if build.startswith(k)), None)
        return "Morning mantra + silence" + (" · " + hit if hit else "")
    if area == "review":
        return f"Weekly review · week {r['week']} checkpoint"
    raise ValueError(tid)


TITLES = {r["task_id"]: title(r) for r in rows}

# ------------------------------------------------------------------------------------------- question banks (fictional data)
Q = lambda id, week, prompt, options, answer, why: dict(id=id, week=week, prompt=prompt, options=options, answer=answer, explanation=why)
ACC_Q = [
    Q("AQ01", 1, "Who is personally liable, without limit, for a sole trader's debts?", ["The owner", "The bank", "The customers", "Nobody"],
      "The owner", "A sole trader and the business are the same person in law."),
    Q("AQ02", 1, "A company's shareholders are liable for its debts…", ["Only up to the capital they put in", "Without limit",
      "Only if they are directors", "For every debt, personally"], "Only up to the capital they put in",
      "That's limited liability: the company is a separate legal person."),
    Q("AQ03", 1, "Which is a separate legal entity?", ["A company", "A sole trader", "The owner's family", "The bank account"],
      "A company", "A company can own assets and be sued in its own name."),
    Q("AQ04", 1, "Ravi Traders' machinery is…", ["An asset", "A liability", "An income", "An expense"], "An asset", "It's owned and used for years."),
    Q("AQ05", 1, "A bank loan Ravi Traders must repay is…", ["A liability", "An asset", "An income", "An expense"], "A liability", "It's owed to someone else."),
    Q("AQ06", 1, "Sales of goods are…", ["Income", "An asset", "A liability", "An expense"], "Income", "Money earned by the business."),
    Q("AQ07", 1, "Rent paid for the shop is…", ["An expense", "An asset", "A liability", "Income"], "An expense", "A cost of running the business."),
    Q("AQ08", 1, "In an accounting information system, a vendor invoice keyed into the ERP is the…", ["Input", "Process", "Output", "Ledger"],
      "Input", "Input → process (checks, posting) → output (entries, reports)."),
    Q("AQ09", 2, "A current asset is…", ["Stock you expect to sell this year", "A machine used for 5 years", "A 3-year bank loan", "The owner's capital"],
      "Stock you expect to sell this year", "Current means within about a year."),
    Q("AQ10", 2, "Where does a vendor's unpaid invoice appear?", ["Current liabilities", "Current assets", "Non-current assets", "Income"],
      "Current liabilities", "You owe it soon, so it's a current liability."),
    Q("AQ11", 2, "Gross profit is…", ["Sales minus cost of goods sold", "Sales minus all expenses", "Assets minus liabilities", "Cash in minus cash out"],
      "Sales minus cost of goods sold", "Net profit then subtracts the running expenses."),
    Q("AQ12", 2, "An expense is booked when…", ["The invoice arrives (it is incurred)", "It is paid", "The year ends", "The bank statement shows it"],
      "The invoice arrives (it is incurred)", "That's the accrual principle."),
    Q("AQ13", 2, "Prudence (conservatism) means…", ["Don't overstate income or assets", "Record only cash", "Use a new method every year",
      "Ignore small amounts"], "Don't overstate income or assets", "Expect losses; don't count gains before they happen."),
    Q("AQ14", 3, "Cash is which type of account?", ["Real", "Personal", "Nominal", "Contra"], "Real", "Things you own (cash, furniture, goods) are real accounts."),
    Q("AQ15", 3, "A vendor is which type of account?", ["Personal", "Real", "Nominal", "Capital"], "Personal", "People and businesses are personal accounts."),
    Q("AQ16", 3, "Salaries are which type of account?", ["Nominal", "Real", "Personal", "Asset"], "Nominal",
      "Expenses, losses, incomes and gains are nominal accounts."),
    Q("AQ17", 5, "Ravi Traders buys goods for ₹10,000 + 18% GST on credit. The entry is…",
      ["Dr Purchases 10,000 · Dr Input GST 1,800 · Cr Vendor 11,800", "Dr Purchases 11,800 · Cr Vendor 11,800",
       "Dr Vendor 11,800 · Cr Purchases 10,000 · Cr Input GST 1,800", "Dr Purchases 10,000 · Cr Vendor 10,000 · Cr Output GST 1,800"],
      "Dr Purchases 10,000 · Dr Input GST 1,800 · Cr Vendor 11,800", "GST paid on purchases is Input GST (a debit); the vendor is owed the total."),
    Q("AQ18", 5, "Ravi Traders sells goods for ₹20,000 + 18% GST on credit. The entry is…",
      ["Dr Customer 23,600 · Cr Sales 20,000 · Cr Output GST 3,600", "Dr Customer 20,000 · Cr Sales 20,000",
       "Dr Sales 20,000 · Dr Output GST 3,600 · Cr Customer 23,600", "Dr Customer 23,600 · Cr Sales 23,600"],
      "Dr Customer 23,600 · Cr Sales 20,000 · Cr Output GST 3,600", "GST charged on sales is Output GST, a credit owed to the government."),
    Q("AQ19", 5, "Ravi Traders pays a vendor ₹9,800 in full settlement of ₹10,000. The entry is…",
      ["Dr Vendor 10,000 · Cr Bank 9,800 · Cr Discount received 200", "Dr Vendor 9,800 · Cr Bank 9,800",
       "Dr Vendor 10,000 · Dr Discount allowed 200 · Cr Bank 10,200", "Dr Bank 9,800 · Dr Discount received 200 · Cr Vendor 10,000"],
      "Dr Vendor 10,000 · Cr Bank 9,800 · Cr Discount received 200", "The full debt is cleared; the ₹200 not paid is income."),
    Q("AQ20", 8, "After the journal, the next step is…", ["Posting to the ledger", "The balance sheet", "Closing the year", "Paying the vendor"],
      "Posting to the ledger", "Journal → ledger → (in February) trial balance."),
    Q("AQ21", 9, "Why does Capital normally have a credit balance?", ["The business owes it to the owner", "It's an asset", "It's an expense",
      "Because of GST"], "The business owes it to the owner", "Liabilities and capital sit on the credit side."),
]
J = lambda id, week, text, dr, cr: dict(id=id, week=week, text=text, dr=dr, cr=cr)
JOURNAL = [
    J("J01", 4, "Ravi starts Ravi Traders with ₹1,00,000 in cash.", "Cash", "Capital"),
    J("J02", 4, "Ravi Traders deposits ₹40,000 cash into the bank.", "Bank", "Cash"),
    J("J03", 4, "Buys goods for ₹15,000 in cash.", "Purchases", "Cash"),
    J("J04", 4, "Buys goods on credit from a vendor for ₹20,000.", "Purchases", "Vendor"),
    J("J05", 4, "Sells goods for ₹12,000 in cash.", "Cash", "Sales"),
    J("J06", 4, "Sells goods on credit to a customer for ₹18,000.", "Customer", "Sales"),
    J("J07", 4, "Pays shop rent of ₹6,000 by bank transfer.", "Rent", "Bank"),
    J("J08", 4, "Pays salaries of ₹25,000 from the bank.", "Salaries", "Bank"),
    J("J09", 4, "Buys office furniture for ₹9,000 in cash.", "Furniture", "Cash"),
    J("J10", 4, "Buys a machine on credit from a vendor for ₹60,000.", "Machinery", "Vendor"),
    J("J11", 4, "Pays the vendor ₹20,000 by bank transfer.", "Vendor", "Bank"),
    J("J12", 4, "Receives ₹18,000 from the customer into the bank.", "Bank", "Customer"),
    J("J13", 4, "Ravi takes ₹3,000 cash for personal use.", "Drawings", "Cash"),
    J("J14", 4, "Pays the electricity bill of ₹1,500 in cash.", "Electricity", "Cash"),
    J("J15", 5, "Takes a bank loan of ₹50,000 into the bank account.", "Bank", "Bank loan"),
    J("J16", 5, "The bank credits ₹400 interest to the account.", "Bank", "Interest received"),
    J("J17", 5, "Returns damaged goods worth ₹2,000 to the vendor.", "Vendor", "Purchase returns"),
    J("J18", 5, "A customer returns goods worth ₹1,000.", "Sales returns", "Customer"),
    J("J19", 5, "Withdraws ₹10,000 cash from the bank for office use.", "Cash", "Bank"),
    J("J20", 5, "Receives a commission of ₹2,500 in cash.", "Cash", "Commission received"),
    J("J21", 5, "Pays ₹1,200 for advertising by bank transfer.", "Advertisement", "Bank"),
    J("J22", 6, "Writes off ₹800 that a customer will never pay.", "Bad debts", "Customer"),
    J("J23", 5, "The bank deducts ₹150 charges from the account.", "Bank charges", "Bank"),
    J("J24", 5, "Pays insurance of ₹3,600 by bank transfer.", "Insurance", "Bank"),
    J("J25", 5, "Ravi brings in another ₹30,000 into the bank.", "Bank", "Capital"),
    J("J26", 6, "Ravi takes goods worth ₹1,000 home for personal use.", "Drawings", "Purchases"),
    J("J27", 6, "Goods worth ₹2,000 are given away as free samples.", "Advertisement", "Purchases"),
    J("J28", 5, "Repays ₹10,000 of the bank loan.", "Bank loan", "Bank"),
]
ACCOUNTS = ["Cash", "Bank", "Capital", "Drawings", "Purchases", "Sales", "Vendor", "Customer", "Rent", "Salaries", "Electricity",
            "Furniture", "Machinery", "Bank loan", "Interest received", "Commission received", "Purchase returns", "Sales returns",
            "Bad debts", "Advertisement", "Insurance", "Bank charges", "Input GST", "Output GST", "Discount received", "Discount allowed"]
# Calculation questions the app makes fresh each time, from the week their topic is taught.
CALCS = [dict(kind="equation", week=1), dict(kind="gst", week=5), dict(kind="debits", week=5), dict(kind="ledger", week=8)]

PY_Q = [
    Q("RQ01", 3, "In your RAG assistant, a chunk is…", ["A small piece of a note, stored with its source", "A whole folder of notes",
      "An Excel row", "A Python error"], "A small piece of a note, stored with its source", "RAG 1: split every note into chunks {text, source}."),
    Q("RQ02", 3, "Why store the source file with every chunk?", ["So every answer can name where it came from", "To make the file smaller",
      "Python needs it to run", "It sorts the chunks"], "So every answer can name where it came from", "Source attribution: the answer names the right file."),
    Q("RQ03", 3, "Keyword retrieval scores a chunk by…", ["How many of the question's words it contains", "Its length", "Its file date",
      "A random number"], "How many of the question's words it contains", "RAG 2: score, sort, keep the top 3."),
    Q("RQ04", 4, "TF-IDF gives a word more weight when it is…", ["Frequent in this chunk but rare across all chunks", "Common in every chunk",
      "Very short", "In capital letters"], "Frequent in this chunk but rare across all chunks", "Rare, specific words tell chunks apart."),
    Q("RQ05", 4, "Without an approved API key, the offline 'answer' is…", ["The top 3 chunks with their sources", "Nothing at all",
      "A made-up summary", "The whole notes folder"], "The top 3 chunks with their sources", "Offline route: keyword or TF-IDF retrieval only."),
    Q("RQ06", 4, "Where should an approved API key live?", ["In a .env file, never in the code", "In the code, near the top",
      "In the notes folder", "In the error report"], "In a .env file, never in the code", "Keys in code leak through GitHub."),
    Q("RQ07", 5, "Why test with 20 fixed questions that have expected answers?", ["To measure accuracy and see whether a change helps",
      "To make the assistant faster", "Because the library needs 20", "To fill the notes folder"],
      "To measure accuracy and see whether a change helps", "A fixed test turns 'seems good' into a score."),
    Q("RQ08", 5, "Your score is 14/20. What do you fix first?", ["The notes behind the wrong answers, then re-test", "Add more questions",
      "Change the expected answers", "Delete the wrong questions"], "The notes behind the wrong answers, then re-test",
      "Fix the weakest notes and re-test (Sun 20 Dec)."),
]


# The banks above were written for the DRC Edition; the Fresh Start plan teaches some topics in a different week
# (section 6: income statement and GAAP in week 3, the accounting equation in week 2). A check never asks about
# a later week, so these move to the week the topic is now taught.
ACC_WEEK = {"AQ11": 3, "AQ12": 3, "AQ13": 3}
for q in ACC_Q:
    q["week"] = ACC_WEEK.get(q["id"], q["week"])
CALC_WEEK = {"equation": 2}
for c in CALCS:
    c["week"] = CALC_WEEK.get(c["kind"], c["week"])

# Practice cards: the plan task whose topic each bundled card covers. The card unlocks when you complete that task.
# Cards for topics this plan doesn't teach yet (trial balance, final accounts, Streamlit, asset accounting) stay locked.
def first_task(area, week):
    ids = [r["task_id"] for r in rows if AREA_IDS[r["area"]] == area and int(r["week"]) == week]
    return ids[0] if ids else None


CARD_UNLOCKS = {}
for i in range(1, 29): CARD_UNLOCKS[f"PY-{i:03d}"] = "T1023"           # recap: basics, functions, pandas, PAN regex
for i in (36, 37): CARD_UNLOCKS[f"PY-{i:03d}"] = "T1023"               # ^ $ and \d{10}: the PAN / GSTIN checks
for i in (29, 30, 31): CARD_UNLOCKS[f"PY-{i:03d}"] = "T1262"           # duplicated(), to_excel, seen-set
CARD_UNLOCKS["PY-032"] = "T1063"                                        # iterrows: error report row by row
for i in (33, 34, 35, 38, 39, 40, 41, 42): CARD_UNLOCKS[f"PY-{i:03d}"] = "T1298"  # odd cells, +91, email, try / except
for i in (43, 44): CARD_UNLOCKS[f"PY-{i:03d}"] = "T1302"               # regression testing
for i in list(range(1, 12)) + [23, 24]: CARD_UNLOCKS[f"ER-{i:02d}"] = "T1027"  # PAN, GSTIN, state codes
for i in (12, 13, 14, 15, 16, 17, 18): CARD_UNLOCKS[f"ER-{i:02d}"] = "T1298"   # IFSC, mobile, email
CARD_UNLOCKS["ER-19"] = "T1138"                                         # P2P in order (SAP Friday: P2P)
for i in (20, 21, 22): CARD_UNLOCKS[f"ER-{i:02d}"] = "T1418"           # agents in P2P (Demo #2 slide)
CARD_UNLOCKS.update({"SQL-01": "T1258", "SQL-02": "T1258", "SQL-03": "T1262", "SQL-05": "T1262", "SQL-04": "T1338"})
AC_W = {1: 2, 2: 3, 3: 3, 9: 3, 4: 4, 5: 4, 6: 4, 7: 4, 8: 4, 10: 4, 11: 4, 12: 4, 13: 4, 14: 4, 15: 4, 16: 4, 17: 4, 18: 4, 19: 4,
        20: 4, 21: 4, 22: 5, 23: 5, 24: 5, 25: 5, 54: 5, 55: 5, 56: 5, 26: 6, 27: 6, 28: 6, 29: 6, 30: 6, 31: 6, 32: 6, 33: 6, 34: 7,
        35: 8, 36: 8, 37: 9, 38: 9}
for k, w in AC_W.items(): CARD_UNLOCKS[f"AC-{k:02d}"] = first_task("accounting", w)
CARD_UNLOCKS.update({"AC-48": "T1138", "AC-49": "T1373"})               # 3-way match (P2P), O2C order (SAP Fridays)
CARD_UNLOCKS.update({"SAP-02": "T1018", "SAP-01": "T1058", "SAP-04": "T1058", "SAP-07": "T1098", "SAP-14": "T1138", "SAP-15": "T1138",
                     "SAP-03": "T1293", "SAP-05": "T1293", "SAP-06": "T1293", "SAP-08": "T1293", "SAP-09": "T1293", "SAP-10": "T1293",
                     "SAP-11": "T1293", "SAP-12": "T1293", "SAP-13": "T1373"})
CARD_UNLOCKS.update({"CM-03": "T1029", "CM-04": "T1029", "CM-07": "T1069", "CM-05": "T1189", "CM-02": "T1077", "CM-09": "T1077",
                     "CM-06": "T1134", "CM-01": "T1221"})
assert all(CARD_UNLOCKS.values()), [k for k, v in CARD_UNLOCKS.items() if not v]

meta = dict(
    app="roadmap-lab", kind="plan-meta", version=VERSION,
    title="Fresh Start Career Roadmap: SAP Finance Edition",
    subtitle="Tuesday 17 November 2026 – Sunday 31 January 2027 · follow the order, not the dates",
    csvName="Fresh_Start_Schedule_17Nov2026-31Jan2027.csv", taskCount=len(rows),
    # No lead-in in this plan: leadIn = start.
    start="2026-11-17", leadIn="2026-11-17", end="2027-01-31", timeZone="Africa/Kinshasa",
    bottomLine="A fresh start on Tuesday 17 November. One plan, all areas moving every week, for about 8.3 hours of study a week, "
               "plus about 4 hours of exercise, recovery, family time and daily practice. Weekdays are about 45–55 minutes; Saturday "
               "about 2 hours; Sunday about 2½ hours. Busy go-live week? Switch to the reduced routine. Follow the order, not the "
               "dates. A missed day moves the next task one slot later; nothing is lost.",
    areas=AREAS, rubric=RUBRIC, levels=LEVELS, french=FRENCH, checkpoints={str(k): v for k, v in CHECKPOINTS.items()},
    weekFocus={str(k): v for k, v in WEEK_FOCUS.items()}, reduced=REDUCED, recovery=RECOVERY, rules=RULES, monthly=MONTHLY,
    weekRoutine=WEEK_ROUTINE, titles=TITLES, accountingQuestions=ACC_Q, journal=JOURNAL, accounts=ACCOUNTS, calcs=CALCS,
    pythonQuestions=PY_Q, cardUnlocks=CARD_UNLOCKS,
)


def check():
    ids = [r["task_id"] for r in rows]
    assert len(set(ids)) == 428 and ids[0] == "T1001" and ids[-1] == "T1428"
    assert {r["area"] for r in rows} == set(AREA_IDS), {r["area"] for r in rows}
    assert [a["id"] for a in AREAS] == list(AREA_IDS.values())
    words = [w for f in FRENCH for w in f["words"]]
    assert all(len(f["words"]) == 6 for f in FRENCH if f["week"] not in (6, 11)) and len({w["fr"] for w in words}) == len(words) == 54
    for q in ACC_Q + PY_Q:
        assert q["answer"] in q["options"] and len(set(q["options"])) == 4, q["id"]
    for j in JOURNAL:
        assert j["dr"] in ACCOUNTS and j["cr"] in ACCOUNTS, j["id"]
    # Every French word list in the CSV matches section 9 of the PDF.
    for r in rows:
        if AREA_IDS[r["area"]] != "french":
            continue
        m = re.match(r"Week (\d+) · [^:]+: (.+)", r["learn_practise"])
        if m and "Review" not in r["learn_practise"]:
            csv_words = [w.strip() for w in m.group(2).split(",")]
            assert csv_words == [w["fr"] for w in FRENCH[int(m.group(1)) - 1]["words"]], (r["task_id"], csv_words)
    # Every SAP Friday topic is the PDF's (section 5).
    pdf = PDF_TEXT.read_text(encoding="utf-8")
    for r in rows:
        if r["area"] == "SAP Finance" and r["learn_practise"].startswith("SAP Friday · "):
            topic = r["learn_practise"].split(" · ", 1)[1].split(":")[0]
            assert topic.split()[0] in pdf, topic
    long = [(k, t) for k, t in TITLES.items() if not t or len(t) > 72]
    assert not long, long
    unknown = [c for c in CARD_UNLOCKS.values() if c not in ids]
    assert not unknown, unknown
    print(f"OK: {len(rows)} tasks · {len(TITLES)} titles · {len(ACC_Q)} accounting + {len(JOURNAL)} journal + {len(PY_Q)} python questions · "
          f"{len(CARD_UNLOCKS)} card unlocks · {len(words)} French words · {len(CHECKPOINTS)} checkpoints")


check()
OUT.write_text(json.dumps(meta, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
print("wrote", OUT.relative_to(ROOT), OUT.stat().st_size // 1024, "KB")
