#!/usr/bin/env python3
"""
Builds app/src/main/assets/plan_meta.json for the "Eight-Area Career Development Roadmap: DRC Edition".

The schedule itself is the companion CSV (Eight_Area_Schedule_15Nov2026-31Jan2027.csv), bundled unchanged as
app/src/main/assets/schedule.csv: the app reads every row from it (380 tasks), so the task count always equals
the CSV row count. This file adds what the CSV doesn't hold, copied from the roadmap PDF (sections 1, 4, 7, 8,
10–14, 16): area goals, the 5-criterion rubric, French words and model sentences, weekly checkpoint targets,
the reduced routine, success criteria, plus short titles and the question banks used for in-app checks.

Nothing here adds a task or changes a date. Run: python3 tools/build_plan.py
"""
import csv
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CSV = ROOT / "app/src/main/assets/schedule.csv"
PDF_TEXT = ROOT / "tools/source/Eight_Area_Roadmap_DRC_Edition.txt"
OUT = ROOT / "app/src/main/assets/plan_meta.json"
VERSION = "drc-2026-11-14"

rows = list(csv.DictReader(open(CSV, encoding="utf-8")))
assert len(rows) == 380, len(rows)

AREA_IDS = {"Python/AI": "python", "Accounting": "accounting", "French": "french", "Communication": "comms",
            "LinkedIn": "linkedin", "Evidence log": "evidence", "App": "app", "Health/family/money": "life",
            "Weekly review": "review"}

AREAS = [
    dict(id="python", name="Python and AI", short="Python / AI", objective="Go-live Data Kit: validator + coded RAG assistant",
         byEnd="Demo #1 and Demo #2 sent; 16/20 then 20/25 test scores", budget="Sat 120 · Sun 90 min (210 a week)",
         assessed="Write and explain a function without AI; the validator finds exactly the planted errors. RAG: explain chunking, "
                  "retrieval and source attribution aloud; test score on fixed questions",
         notEnough="Code that runs but you can't explain; one good-looking answer",
         success="Demo #1 and Demo #2 sent; validator finds every planted error in both files; RAG 16/20, then 20/25",
         targetLevel="5–6", stretch="Streamlit screen working",
         rules=["Every session starts with revision (15 min Sat, 10 min Sun): rewrite last session's main code from memory, then compare.",
                "Predict the output before you run anything. Type the code yourself. Stuck for 15 minutes? Ask for a hint, not the solution.",
                "Fake data only. Save code to your private GitHub repo and to Drive after each session."]),
    dict(id="accounting", name="Accounting", short="Accounting", objective="Strong fundamentals for SAP Finance",
         byEnd="Journal and ledger, tested cold: 10/12 on test #2", budget="5 × 30 min on weekdays (150 a week)",
         assessed="Cold transactions with no answers in view; cumulative tests", notEnough="Watching the lecture",
         success="1.2 to 3.8 done; test #2 10/12 cold", targetLevel="5", stretch="12/12, and you can explain each entry",
         rules=["Watching a lecture is not mastery. Every lecture is followed by work you do before seeing the answer.",
                "Every Monday starts with a cold 10-card quiz on the previous week. Mistakes become new flashcards.",
                "Ravi Traders is your fictional business for all practice. Use made-up amounts.",
                "Lecture numbers match the IIT Mandi course list. If your course run numbers differ, follow the same topic order."]),
    dict(id="french", name="French", short="French", objective="Comfortable workplace French",
         byEnd="100 work words; weekly quizzes; 1-minute introduction", budget="5 × 10 min on weekdays (50 a week)",
         assessed="Quiz both directions; sentences you build; a 1-minute recording", notEnough="Duolingo streak alone",
         success="100 words; 3 review quizzes 16/20; introduction 6/10", targetLevel="5", stretch="A 5-minute work chat in French",
         rules=["Mon: words 1–5, one sentence each · Tue: words 6–10, listen 3 min · Wed: read the 5 model sentences, translate back · "
                "Thu: say them aloud 3 times, record 1 min · Fri: use 2 words at work; 10-word quiz both directions.",
                "Holiday weeks 6 and 7 have 4 days: Wednesday combines reading, speaking and recording; Thursday is the quiz."]),
    dict(id="comms", name="Communication", short="Comms", objective="Clear, confident professional communication",
         byEnd="Habits most days; recordings 7/10; 2 user trainings", budget="Inside work · Sunday recording 10 min",
         assessed="Rubric score; listener can repeat the point", notEnough="Feeling that it went well",
         success="Habits on most days; recordings 7/10; 2 trainings delivered", targetLevel="5–6", stretch="Speaking early feels normal",
         rules=["Mon: speak early · Tue: “So to confirm…” · Wed: same-day written summary · Thu: explain one process twice · "
                "Fri: one concise update (done · next · blocker).",
                "Recordings, demos and trainings are scored with the 5-criterion rubric (0–2 each, out of 10). "
                "Targets: 6/10 in November–December, 7/10 in January. Listen once and write one thing to change next time."]),
    dict(id="linkedin", name="LinkedIn", short="LinkedIn", objective="Relevant professional network",
         byEnd="Profile updated; 9 thoughtful conversations started", budget="Sunday 15 min",
         assessed="Conversations started and useful (2+ messages each)", notEnough="Number of connections",
         success="Profile updated; 9 conversations started", targetLevel="5", stretch="3 useful conversations",
         rules=["One person a week. Ask about their work, never for a job, and never send mass requests.",
                "A conversation counts as started when you've sent a personal note with one real question; it counts as useful when "
                "you've exchanged at least two messages.",
                "If LinkedIn won't let you add a note, connect without one and message after they accept.",
                "Note template (about 240 characters): Hi [Name], I work on ERP implementations (right now a go-live in DR Congo) and "
                "I'm moving towards SAP finance. Your work on [X] caught my eye. Could I ask you one question about [their topic]? Thanks, Nani"]),
    dict(id="evidence", name="Evidence log", short="Evidence", objective="Verifiable evidence of growth",
         byEnd="A line most working days; 3 STAR stories", budget="5 min a day (35 a week)",
         assessed="Lines with a real result; STAR stories written", notEnough="Lines that say only “worked on X”",
         success="Lines on 80% of working days; 3 STAR stories", targetLevel="5", stretch="Manager uses one of your stories",
         rules=["One line at the end of each working day, even on bad days. Say “the client”, never a name; use rough numbers; nothing "
                "confidential, and only what you're allowed to use.",
                "Each Sunday, star the best line of the week (★). Three STAR stories, each from that month's starred lines: #1 Mon 30 Nov · "
                "#2 Thu 31 Dec · #3 Fri 29 Jan. Situation (1 line) · Task (1) · Action (2–3) · Result (1, with a number), under 150 words."]),
    dict(id="app", name="Tracker app", short="App", objective="A reliable app for all 8 areas",
         byEnd="v1.0.1 on your phone with this plan imported; restore tested", budget="Sunday 30 min",
         assessed="Features tested on the phone; data survives restart, update and restore", notEnough="A successful build",
         success="v1.0.1 installed; this plan imported; restore drill passed", targetLevel="5", stretch="Used every day since 11 Jan",
         rules=["30 minutes a step, on Sundays. If a step isn't finished at 40 minutes, stop and continue next Sunday: the order matters, "
                "not the date. The app never takes time from accounting or Python.",
                "Safety: private repo; no company or client data in the app; the signing key lives only in GitHub Secrets and your private Drive."]),
    dict(id="life", name="Health, family and money", short="Health · family · money", objective="Sustainable progress in DRC",
         byEnd="Sleep and exercise most days; 3 money checks; family steps", budget="Exercise 3 × 25 · alone time 2 × 15 · Sat 60 off · call 25",
         assessed="Counts: sleep nights, exercise sessions, calls, money checks", notEnough="Perfection (not expected)",
         success="Sleep 7 h on most nights; exercise 2–3 times most weeks; 3 money checks; dated family steps", targetLevel="4",
         stretch="No week without a family call",
         rules=["Every day: 7 hours of sleep with a fixed lights-out time; bottled or treated water; malaria prevention exactly as your "
                "doctor prescribed (net and repellent too). Measured as nights with 7 h (target 5 of 7).",
                "Mon · Wed · Fri: 20–30 min exercise (target 3 sessions; 2 is fine). Tue · Thu: 10–20 min good alone time. "
                "Saturday: 60 minutes fully off. Sunday: call home; note the week's spending total in the review.",
                "After a very long day: next evening, only the log line, then sleep. Nothing else is owed.",
                "Never trade sleep, health or family for a streak. If you feel low most days for 2 weeks or more: tell someone you trust and "
                "see a doctor (in DRC, through your company's medical contact). In India, Tele-MANAS is free and open 24×7: 14416 or "
                "1-800-891-4416. It's an Indian number, so it may not connect from a Congolese SIM."]),
    dict(id="review", name="Weekly review", short="Review", objective="Honest weekly checkpoint",
         byEnd="11 checkpoint tables filled", budget="Sunday 15 min", assessed="Every area has an actual result written, not just ticks",
         notEnough="Ticks without results", success="", targetLevel="", stretch="",
         rules=["Be honest in the review. A ticked task with a weak result is written as weak. Two weeks in a row below target in one "
                "area means the plan changes (fewer new topics, more practice), not that you try harder on less sleep."]),
]

RUBRIC = dict(
    criteria=[
        dict(name="Clarity", levels=["Main point unclear", "Main point appears late", "Main point in the first 15 seconds"]),
        dict(name="Structure", levels=["Jumps around", "Partly ordered", "Problem → action → result, in order"]),
        dict(name="Confidence", levels=["Many fillers or apologies", "Some fillers", "Steady pace; 3 fillers a minute or fewer; no apologising"]),
        dict(name="Accuracy", levels=["A technical error", "Small slip", "Every fact checked against your notes"]),
        dict(name="Audience", levels=["Jargon, no example", "One example", "A non-expert could repeat it; one concrete example"]),
    ],
    note="Listen once and write one thing to change next time.",
)

# ------------------------------------------------------------------------------------------- French (section 7)
G = lambda *p: [dict(fr=f, en=e) for f, e in p]
FRENCH = [
    dict(week=1, theme="Greetings and basics", words=G(("bonjour", "hello"), ("merci", "thank you"), ("s'il vous plaît", "please"),
        ("excusez-moi", "excuse me, sorry"), ("je m'appelle", "my name is"), ("enchanté", "nice to meet you"), ("oui", "yes"), ("non", "no"),
        ("d'accord", "OK, agreed"), ("à demain", "see you tomorrow")),
         sentences=G(("Bonjour, je m'appelle Nani. Enchanté.", "Hello, I'm Nani. Nice to meet you."),
                     ("Excusez-moi, vous pouvez répéter, s'il vous plaît ?", "Sorry, can you repeat, please?"),
                     ("Je ne comprends pas. Plus lentement, s'il vous plaît.", "I don't understand. More slowly, please."),
                     ("D'accord, merci beaucoup.", "OK, thank you very much."),
                     ("Bonne journée, à demain !", "Have a good day, see you tomorrow!"))),
    dict(week=2, theme="Meetings and time", words=G(("une réunion", "a meeting"), ("aujourd'hui", "today"), ("demain", "tomorrow"),
        ("le matin", "the morning"), ("l'après-midi", "the afternoon"), ("une heure", "an hour, one o'clock"), ("lundi", "Monday"),
        ("vendredi", "Friday"), ("la semaine", "the week"), ("commencer", "to start")),
         sentences=G(("La réunion est à dix heures.", "The meeting is at ten."), ("On commence à neuf heures ?", "Shall we start at nine?"),
                     ("On se voit demain matin.", "See you tomorrow morning."), ("Vendredi, je présente une démo.", "On Friday I'm presenting a demo."),
                     ("Cette semaine, on finit les tests.", "This week we finish the tests."))),
    dict(week=3, theme="The system", words=G(("le système", "the system"), ("l'écran", "the screen"), ("l'utilisateur", "the user"),
        ("le champ obligatoire", "the mandatory field"), ("remplir", "to fill in"), ("enregistrer", "to save"), ("modifier", "to change, edit"),
        ("supprimer", "to delete"), ("importer", "to import"), ("le mot de passe", "the password")),
         sentences=G(("Ouvrez cet écran, s'il vous plaît.", "Please open this screen."), ("Ce champ est obligatoire.", "This field is mandatory."),
                     ("Remplissez ce champ, puis cliquez sur « Enregistrer ».", "Fill in this field, then click “Save”."),
                     ("On importe le fichier Excel.", "We're importing the Excel file."),
                     ("Je ne peux pas me connecter : le mot de passe est incorrect.", "I can't log in: the password is wrong."))),
    dict(week=4, theme="Purchasing", words=G(("un fournisseur", "a vendor, supplier"), ("un achat", "a purchase"),
        ("un bon de commande", "a purchase order"), ("la quantité", "the quantity"), ("le prix", "the price"), ("la réception", "the goods receipt"),
        ("la marchandise", "the goods"), ("l'entrepôt", "the warehouse"), ("une facture", "an invoice"), ("approuver", "to approve")),
         sentences=G(("Le fournisseur est créé ?", "Is the vendor created?"), ("Le bon de commande est validé.", "The purchase order is approved."),
                     ("La marchandise est arrivée à l'entrepôt.", "The goods have reached the warehouse."),
                     ("Le montant de la facture est correct.", "The invoice amount is correct."),
                     ("La facture est en attente d'approbation.", "The invoice is waiting for approval."))),
    dict(week=5, theme="Sales", words=G(("un client", "a customer"), ("un devis", "a quote"), ("une commande", "an order"),
        ("la livraison", "the delivery"), ("prévue", "planned"), ("une remise", "a discount"), ("le total", "the total"), ("en retard", "late"),
        ("le paiement", "the payment"), ("payé", "paid")),
         sentences=G(("Le client a accepté le devis.", "The customer accepted the quote."), ("La livraison est prévue jeudi.", "Delivery is planned for Thursday."),
                     ("Il y a une remise de dix pour cent.", "There's a 10% discount."), ("La livraison est en retard.", "The delivery is late."),
                     ("Le client a payé la facture.", "The customer has paid the invoice."))),
    dict(week=6, theme="Accounting", words=G(("une écriture", "an accounting entry"), ("le débit", "the debit"), ("le crédit", "the credit"),
        ("le grand livre", "the general ledger"), ("un compte", "an account"), ("le solde", "the balance"), ("la balance", "the trial balance"),
        ("équilibrée", "balanced, agrees"), ("le bilan", "the balance sheet"), ("la TVA", "VAT, sales tax")),
         sentences=G(("On passe une écriture comptable.", "We post an accounting entry."),
                     ("Ce montant va au débit du compte.", "This amount goes on the debit side of the account."),
                     ("Le solde du compte est correct.", "The account balance is correct."), ("La balance est équilibrée.", "The trial balance agrees."),
                     ("Le montant de la TVA est correct.", "The VAT amount is correct."))),
    dict(week=7, theme="Support and problems", words=G(("un problème", "a problem"), ("un message d'erreur", "an error message"),
        ("bloqué", "stuck, blocked"), ("une capture d'écran", "a screenshot"), ("envoyer", "to send"), ("vérifier", "to check"),
        ("corrigé", "fixed, corrected"), ("résolu", "solved"), ("urgent", "urgent"), ("un ticket", "a (support) ticket")),
         sentences=G(("J'ai un message d'erreur.", "I have an error message."), ("L'utilisateur est bloqué.", "The user is stuck."),
                     ("Envoyez-moi une capture d'écran, s'il vous plaît.", "Please send me a screenshot."),
                     ("C'est corrigé. Vous pouvez vérifier ?", "It's fixed. Can you check?"), ("Le problème est résolu.", "The problem is solved."))),
    dict(week=8, theme="Training users", words=G(("une formation", "a training"), ("une étape", "a step"), ("d'abord", "first"),
        ("ensuite", "next, then"), ("enfin", "finally"), ("un exemple", "an example"), ("montrer", "to show"), ("essayer", "to try"),
        ("cliquer", "to click"), ("une question", "a question")),
         sentences=G(("D'abord, on ouvre le module Achats.", "First, we open the Purchasing module."),
                     ("Ensuite, on choisit le fournisseur.", "Next, we choose the vendor."), ("Je vous montre un exemple.", "I'll show you an example."),
                     ("Maintenant, essayez, s'il vous plaît.", "Now you try, please."), ("Vous avez des questions ?", "Do you have any questions?"))),
    dict(week=9, theme="Numbers and money", words=G(("dix", "ten"), ("vingt", "twenty"), ("cinquante", "fifty"), ("cent", "a hundred"),
        ("mille", "a thousand"), ("soixante-dix", "seventy"), ("quatre-vingts", "eighty"), ("quatre-vingt-dix", "ninety"),
        ("le franc congolais", "the Congolese franc"), ("le taux de change", "the exchange rate")),
         sentences=G(("Il y a cinquante lignes dans le fichier.", "There are fifty rows in the file."),
                     ("Le total est de mille cinq cents dollars.", "The total is 1,500 dollars."),
                     ("Soixante-dix, quatre-vingts, quatre-vingt-dix.", "Seventy, eighty, ninety."),
                     ("Le prix est en francs congolais ou en dollars ?", "Is the price in Congolese francs or dollars?"),
                     ("Quel est le taux de change aujourd'hui ?", "What's the exchange rate today?"))),
    dict(week=10, theme="Go-live and data", words=G(("le démarrage", "the go-live"), ("la mise en production", "the move to production"),
        ("les données de base", "master data"), ("un modèle", "a template"), ("une colonne", "a column"), ("un doublon", "a duplicate"),
        ("le solde d'ouverture", "the opening balance"), ("un écart", "a difference, gap"), ("le plan de bascule", "the cutover plan"),
        ("une sauvegarde", "a backup")),
         sentences=G(("Le démarrage est prévu lundi.", "Go-live is planned for Monday."),
                     ("Remplissez le modèle, colonne par colonne.", "Fill in the template, column by column."),
                     ("Ce fournisseur est un doublon.", "This vendor is a duplicate."),
                     ("Il y a un écart dans le solde d'ouverture.", "There's a difference in the opening balance."),
                     ("Le plan de bascule est prêt.", "The cutover plan is ready."))),
    dict(week=11, theme="Review and your introduction", words=[],
         sentences=G(("Je m'appelle Nani.", "I'm Nani."), ("Je travaille sur le démarrage du système.", "I work on the system go-live."),
                     ("Je vérifie les données et je forme les utilisateurs.", "I check the data and train the users."),
                     ("Je parle un peu français.", "I speak a little French."), ("J'apprends chaque semaine.", "I'm learning every week."))),
]


# ------------------------------------------------------------------------------------------- weekly checkpoints (section 11)
def checkpoints():
    lines = PDF_TEXT.read_text(encoding="utf-8").split("\n")
    names = [("Python / AI", "python"), ("Accounting", "accounting"), ("French", "french"), ("Comms", "comms"), ("LinkedIn", "linkedin"),
             ("Evidence log", "evidence"), ("Tracker app", "app"), ("Health ·", "life")]
    out = {}
    for i, l in enumerate(lines):
        m = re.search(r"Week (\d+) checkpoint · Sunday review", l)
        if not m:
            continue
        res, cur = {}, None
        for x in lines[i + 3:]:
            if "Planned / done" in x:
                break
            if not x.strip() or "Eight-Area Career" in x or x.strip().startswith("Area "):
                continue
            hit = next(((n, a) for n, a in names if x.lstrip().startswith(n)), None)
            if hit:
                cur = hit[1]
                res[cur] = x.strip()[len(hit[0]):].strip()
            elif cur:
                res[cur] = (res[cur] + " " + x.strip()).strip()
        out[int(m.group(1))] = {a: re.sub(r"\s+", " ", re.sub(r"\s*family ·\s*|\bmoney\b(?=$)", " ", t)).strip() for a, t in res.items()}
    return out


CHECKPOINTS = checkpoints()
assert sorted(CHECKPOINTS) == list(range(1, 12)) and all(len(v) == 8 for v in CHECKPOINTS.values()), CHECKPOINTS
WEEK_FOCUS = {1: "Finish the validator", 2: "Manager update", 3: "RAG: chunks and keyword search", 4: "RAG: better retrieval and sources",
              5: "RAG testing, 16/20", 6: "Demo #1 (holiday week)", 7: "Validator v2 (holiday week)", 8: "Error handling and regression",
              9: "SQL", 10: "Extend the assistant", 11: "Demo #2"}

# Section 13: the reduced routine for busy go-live weeks (about 20 minutes a day).
REDUCED = [
    dict(id="red-evidence", area="evidence", action="One line every day: busy weeks produce the best evidence", time="5 min/day", minutes=5,
         days=[1, 2, 3, 4, 5, 6, 7]),
    dict(id="red-accounting", area="accounting", action="Flashcards only, no new lecture", time="15 min/day", minutes=15, days=[1, 2, 3, 4, 5]),
    dict(id="red-french", area="french", action="2 words used at work; Friday 10-word quiz", time="10 min/week", minutes=0, days=[1, 2, 3, 4, 5]),
    dict(id="red-comms", area="comms", action="“So to confirm…” once a day (it saves you time)", time="at work", minutes=0, days=[1, 2, 3, 4, 5]),
    dict(id="red-python", area="python", action="Saturday: 15-min revision + finish only the current step. Sunday off", time="60 min", minutes=60,
         days=[6]),
    dict(id="red-app", area="app", action="Pause building; just tick tasks", time="1 min/day", minutes=0, days=[]),
    dict(id="red-linkedin", area="linkedin", action="Reply to messages only", time="5 min", minutes=5, days=[7]),
    dict(id="red-life", area="life", action="Sleep first; two 10-min walks; Sunday call home", time="—", minutes=0, days=[1, 2, 3, 4, 5, 6, 7]),
    dict(id="red-review", area="review", action="10 min Sunday: which week to repeat", time="10 min", minutes=10, days=[7]),
]
RECOVERY = ["Missed a day? Don't double up. Do the next task in order tomorrow.",
            "Missed a week? Repeat the Monday quiz first, then continue where you stopped.",
            "Two reduced weeks in a row? Shift every later date by one week. The sequence stays; only dates move.",
            "Missing a week is fine. Don't let it become a month."]
RULES = ["Fictional data only for all practice.",
         "Never put client names, PAN, GSTIN, bank details, invoices or confidential internal information into external AI tools or "
         "personal repositories, unless the company expressly approves it.",
         "Type and understand code; don't copy AI output blindly. Predict the output before running it.",
         "Stuck for 15 minutes? Ask for a hint, not a complete solution.",
         "Keep the plan manageable during go-live periods: use the reduced routine without guilt.",
         "Recover after missed days by continuing in order, not by doubling up.",
         "Protect health, sleep and family responsibilities. No streak is worth losing them.",
         "No new subjects unless this plan is reviewed first.",
         "Keep the original objectives unless there's a clear, written reason to change one.",
         "Private GitHub repos; app signing key only in GitHub Secrets and your private Drive."]
MONTHLY = {
    "python": ["Sat 28 Nov: validate_pan() from memory, no AI", "Sat 26 Dec: explain retrieval and sources, no notes",
               "Sun 31 Jan: one check function from a blank file, no AI"],
    "accounting": ["Mon 30 Nov: 20-card quiz, 16/20", "Fri 18 Dec test #1 8/10; Mon 4 Jan self-test 8/10", "Thu 28 Jan: test #2, 10/12"],
    "french": ["Fri 27 Nov: 20 words, 16/20", "Thu 31 Dec: 20 words, 16/20", "25–27 Jan: three quizzes 16/20; Fri 29 Jan intro 6/10"],
    "comms": ["Sun 29 Nov manager recording 6/10", "Demo #1 video 6/10; training #1", "Demo #2 video 7/10; training #2"],
    "linkedin": ["Profile done; 1 message", "6 messages by 3 Jan", "9 messages; replies followed up"],
    "evidence": ["STAR #1", "STAR #2", "STAR #3; 80% of working days logged"],
    "app": ["Step 1; prompt ready", "Steps 2–4", "Steps 5–10; restore drill passed"],
    "life": ["Money check #1", "Money check #2", "Money check #3"],
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
WEEK_ROUTINE = [
    ["Mon", "Exercise 20–30 min", "Speak early (question or point)", "Accounting 30 · French 10 · Log 5", "45 min"],
    ["Tue", "—", "“So to confirm…”", "Accounting 30 · French 10 · Log 5 · alone time 15", "45 min"],
    ["Wed", "Exercise 20–30 min", "Same-day written summary", "Accounting 30 · French 10 · Log 5", "45 min"],
    ["Thu", "—", "Explain one thing twice", "Accounting 30 · French 10 · Log 5 · alone time 15", "45 min"],
    ["Fri", "Exercise 20–30 min", "3-sentence update · 2 French words", "Accounting 30 · French quiz 10 · Log 5", "45 min"],
    ["Sat", "—", "—", "Python / AI 2 h (15 min revision first) · Log 5 · 60 min fully off", "2 h 05"],
    ["Sun", "—", "—", "Python / AI 1.5 h · App 30 · Recording 10 · LinkedIn 15 · Log 5 · Review 15 · Call home", "2 h 45"],
]

# ------------------------------------------------------------------------------------------- short titles for the bundled CSV
PY_TITLES = {"T001": "Lead-in: validate_gstin() shape check", "T029": "PAN inside GSTIN: all 10 errors found",
             "T032": "Error report: error_report.xlsx", "T065": "Monthly check: validate_pan() from memory · run_all()",
             "T068": "User guide, go-live notes, manager update", "T099": "RAG 1: chunks with their source",
             "T102": "RAG 2: keyword retrieval, top 3", "T134": "RAG 3: TF-IDF retrieval", "T137": "RAG 4: answers name the source",
             "T169": "RAG test: 20 questions", "T172": "RAG 16/20 · validator + assistant", "T201": "Monthly check: explain retrieval · demo script",
             "T204": "Demo #1: 3-minute video", "T231": "Validator v2 file · duplicate check", "T234": "IFSC and mobile checks",
             "T266": "Email check · try / except", "T269": "Regression test on both files", "T301": "SQL with SQLite: 5 queries",
             "T304": "SQL duplicates: GROUP BY … HAVING", "T336": "Extend the assistant: 20/25", "T339": "Simple screen (Streamlit or text menu)",
             "T371": "Record Demo #2", "T374": "Monthly check · send Demo #2"}


def clean(t):
    t = re.sub(r"\s*\((?:rewatch|if short|answer|solve|training day|offline|online|section|10 min|15 min|25 min)[^)]*\)", "", t)
    t = t.replace("NPTEL ", "")
    return re.sub(r"\s+", " ", t).strip(" ·")


def title(r):
    tid, area, learn, build = r["task_id"], AREA_IDS[r["area"]], r["learn_practise"], r["build_deliver"]
    if area == "python":
        return PY_TITLES[tid]
    if area == "app":
        return learn.split(":")[0].strip()
    if area == "accounting":
        if learn.startswith("Quiz:"):
            parts = learn.split(" · ", 1)
            return "Cold quiz" + (" · " + clean(parts[1]) if len(parts) > 1 else "")
        if learn.startswith("November check"):
            return "November check: 20-card quiz · 2.0 Recap"
        if learn.startswith("January check"):
            return "January check: mark test #2"
        if learn == "Light day":
            return "Light day: 15 min flashcards"
        if learn == "Part 2":
            return "Test #2, part 2: post and balance"
        if learn.startswith("No video"):
            lead = learn.split(": ", 1)[1] if ": " in learn else build
            if lead.startswith("(training day"):
                lead = build
            if len(lead.split()) <= 3 and lead != build:
                lead = lead + ": " + build.replace(" in your own words with one example", "").split(";")[0]
            lead = clean(lead)
            return (lead[:1].upper() + lead[1:])[:72].rstrip(" ,")
        return clean(learn)
    if area == "french":
        m = re.match(r"Week (\d+) · ([^:]+): words 1–5", learn)
        if m:
            return f"Week {m.group(1)} · {m.group(2)}: words 1–5"
        if learn.startswith("Words 6–10"):
            return "Words 6–10 · listen 3 min"
        if learn.startswith("Read the week's 5"):
            return "Read the 5 model sentences, translate back"
        if learn.startswith("Read and say"):
            return "Read, say and record the 5 sentences"
        if learn.startswith("Say the 5"):
            return "Say the 5 sentences · record 1 min"
        if learn.startswith("At work: use 2"):
            return "November check: 20 words" if "November French check" in build else "Friday quiz: 10 words"
        if learn.startswith("Quiz: 20 words"):
            return "December check: 20 words"
        if learn.startswith("Quiz: 10 words"):
            return "Quiz: 10 words (week 6)"
        if learn.startswith("Review quiz"):
            return "Review quiz: " + re.search(r"weeks \d+–\d+", learn).group(0)
        if learn.startswith("Your 1-minute"):
            return "Your 1-minute introduction"
        if learn.startswith("At work: say your introduction"):
            return "January check: your introduction"
        return clean(learn)
    if area == "comms":
        if r["weekday"] == "Sun":
            topic = build.split(": ", 1)[1] if ": " in build else build
            return "Recording: " + topic
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
    if area == "linkedin":
        if learn.startswith("Projects section"):
            return "Projects section · find someone who passed C_TS4FI"
        if learn.startswith("Skills section"):
            return "Skills section · find a data-migration consultant"
        if learn.startswith("Follow-ups"):
            return "Follow-ups: answer every reply"
        return clean(learn.split(" · ")[0])
    if area == "evidence":
        if tid == "T002":
            return "Set up the log"
        star = re.search(r"STAR #(\d)", build)
        if star:
            return f"Today's log line · STAR #{star.group(1)}"
        if learn.startswith("Star the best"):
            return "★ Star the week's best line · log line"
        if "Python session" in build:
            return "Log today's Python session"
        return "Today's log line"
    if area == "life":
        if learn.startswith("Rest day"):
            return learn.split(":")[0]
        if tid == "T003":
            return "Choose a fixed lights-out time"
        if "MONEY:" in build:
            return re.search(r"Money check #\d", build).group(0)
        if "FAMILY:" in build:
            return "Call home · family step"
        if learn == "Recovery":
            return "60 min fully off"
        if learn == "Family":
            return "Call home"
        return build.split(" (")[0]
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

# Practice cards: the week each bundled card's topic is taught. It unlocks with that area's first task of the week.
CARD_WEEKS = {}
for i in range(1, 29): CARD_WEEKS[f"PY-{i:03d}"] = 1
for i in (29, 30, 31, 36, 37, 38): CARD_WEEKS[f"PY-{i:03d}"] = 7
for i in (32, 33, 34, 35, 39, 40, 41, 42, 43, 44): CARD_WEEKS[f"PY-{i:03d}"] = 8
for i in (45, 46, 47): CARD_WEEKS[f"PY-{i:03d}"] = 10
for i in list(range(1, 12)) + [23, 24]: CARD_WEEKS[f"ER-{i:02d}"] = 1
for i in (12, 13, 14, 15, 16, 17): CARD_WEEKS[f"ER-{i:02d}"] = 7
CARD_WEEKS["ER-18"] = 8
for i in (19, 20, 21, 22): CARD_WEEKS[f"ER-{i:02d}"] = 11
for i in range(1, 6): CARD_WEEKS[f"SQL-{i:02d}"] = 9
AC_W = {1: 1, 2: 2, 3: 2, 4: 3, 5: 3, 6: 3, 7: 3, 8: 3, 9: 3, 10: 3, 11: 4, 12: 4, 13: 4, 14: 4, 15: 4, 16: 4, 17: 4, 18: 4, 19: 4, 20: 5,
        21: 5, 22: 5, 23: 5, 24: 5, 25: 5, 26: 6, 27: 6, 28: 6, 29: 6, 30: 6, 31: 6, 32: 6, 33: 6, 34: 6, 35: 8, 36: 8, 37: 9, 38: 9}
for k, w in AC_W.items(): CARD_WEEKS[f"AC-{k:02d}"] = w
CARD_WEEKS.update({"CM-01": 2, "CM-03": 1, "CM-04": 1, "CM-05": 2, "CM-07": 2})


def first_task(area, week):
    ids = [r["task_id"] for r in rows if AREA_IDS[r["area"]] == area and int(r["week"]) == week]
    return ids[0] if ids else None


AREA_OF_CARD = {"PY": "python", "ER": "python", "SQ": "python", "AC": "accounting", "CM": "comms"}
CARD_UNLOCKS = {cid: first_task(AREA_OF_CARD[cid[:2]], w) for cid, w in CARD_WEEKS.items()}
CARD_UNLOCKS.update({"CM-02": "T077", "CM-09": "T077", "CM-06": "T126"})  # STAR #1 line; user training #1
assert all(CARD_UNLOCKS.values()), [k for k, v in CARD_UNLOCKS.items() if not v]

meta = dict(
    app="roadmap-lab", kind="plan-meta", version=VERSION,
    title="Eight-Area Career Development Roadmap: DRC Edition",
    subtitle="16 November 2026 – 31 January 2027 · follow the order, not the dates",
    csvName="Eight_Area_Schedule_15Nov2026-31Jan2027.csv", taskCount=len(rows),
    start="2026-11-16", leadIn="2026-11-15", end="2027-01-31", timeZone="Africa/Kinshasa",
    bottomLine="One plan, eight areas, all moving every week, for about 8.5 hours of study a week plus exercise: 45 minutes on weekday "
               "evenings, about 2 hours on Saturday and about 2¾ hours on Sunday. Busy go-live week? Switch to the reduced routine "
               "(about 20 minutes a day). Follow the order, not the dates. A missed day moves the next task one slot later; nothing is lost.",
    areas=AREAS, rubric=RUBRIC, levels=LEVELS, french=FRENCH, checkpoints={str(k): v for k, v in CHECKPOINTS.items()},
    weekFocus={str(k): v for k, v in WEEK_FOCUS.items()}, reduced=REDUCED, recovery=RECOVERY, rules=RULES, monthly=MONTHLY,
    weekRoutine=WEEK_ROUTINE, titles=TITLES, accountingQuestions=ACC_Q, journal=JOURNAL, accounts=ACCOUNTS, calcs=CALCS,
    pythonQuestions=PY_Q, cardUnlocks=CARD_UNLOCKS,
)


def check():
    ids = [r["task_id"] for r in rows]
    assert len(set(ids)) == 380 and ids[0] == "T001" and ids[-1] == "T380"
    assert {r["area"] for r in rows} == set(AREA_IDS), {r["area"] for r in rows}
    assert all(len(f["words"]) == 10 for f in FRENCH[:10]) and sum(len(f["words"]) for f in FRENCH) == 100
    assert all(len(f["sentences"]) == 5 for f in FRENCH)
    for q in ACC_Q + PY_Q:
        assert q["answer"] in q["options"] and len(set(q["options"])) == 4, q["id"]
    for j in JOURNAL:
        assert j["dr"] in ACCOUNTS and j["cr"] in ACCOUNTS, j["id"]
    # Every French word list in the CSV matches section 7 of the PDF.
    for r in rows:
        if AREA_IDS[r["area"]] != "french":
            continue
        m = re.search(r"words (1–5|6–10): (.+?)(?: · listen|$)", r["learn_practise"])
        if m:
            wk = FRENCH[int(r["week"]) - 1]["words"]
            half = wk[:5] if m.group(1) == "1–5" else wk[5:]
            csv_words = [w.strip() for w in m.group(2).split(",")]
            assert csv_words == [w["fr"] for w in half], (r["task_id"], csv_words, [w["fr"] for w in half])
    long = [(k, t) for k, t in TITLES.items() if not t or len(t) > 72]
    assert not long, long
    print(f"OK: {len(rows)} tasks · {len(TITLES)} titles · {len(ACC_Q)} accounting + {len(JOURNAL)} journal + {len(PY_Q)} python questions · "
          f"{len(CARD_UNLOCKS)} card unlocks · {sum(len(f['words']) for f in FRENCH)} French words · {len(CHECKPOINTS)} checkpoints")


check()
OUT.write_text(json.dumps(meta, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
print("wrote", OUT.relative_to(ROOT), OUT.stat().st_size // 1024, "KB")
