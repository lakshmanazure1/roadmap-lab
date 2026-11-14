"""Builds app/src/main/assets/roadmap.json and cards.json from the career roadmap (14 Nov 2026).

Run:  python3 tools/build_content.py
Rules checked at the end: at most 3 tasks a day, DRC minutes per day <= 60 unless the day
has a single task, every card's unlocking task exists.
"""
import datetime as dt
import json
import os
import sys
from collections import defaultdict

sys.path.insert(0, os.path.dirname(__file__))
from cards_data import CARDS  # noqa: E402

D = dt.date
VERSION = "2026-11-14"
TODAY = D(2026, 11, 14)  # tasks before this date are "assumed done" (pre-app work)
AI, ACC, COM, NET = "AI skills", "Accounting → SAP", "Communication", "Networking"

tasks = []
by_id = {}


def add(id, phase, track, date, title, learn, apply, done, full, drc, minimum, core=True):
    assert id not in by_id, id
    assert minimum <= max(15, minimum) and minimum <= drc <= full or full == drc == minimum, (id, full, drc, minimum)
    t = dict(id=id, phase=phase, track=track, plannedDate=date.isoformat(), title=title,
             learn=learn, apply=apply, doneWhen=done,
             minutes=dict(full=full, drc=drc, minimum=minimum),
             unlocksCards=[], core=core, assumedDone=date < TODAY)
    tasks.append(t)
    by_id[id] = t
    return t


def days(start, end):
    d = start
    while d <= end:
        yield d
        d += dt.timedelta(days=1)


def weekdays(start, end, which=(0, 1, 2, 3, 4)):
    return [d for d in days(start, end) if d.weekday() in which]


def sundays(start, end):
    return [d for d in days(start, end) if d.weekday() == 6]


def first_weekday_of_month(year, month, weekday):
    d = D(year, month, 1)
    while d.weekday() != weekday:
        d += dt.timedelta(days=1)
    return d


B_MIN = (45, 30, 15)        # Track B weekday session: full / DRC / minimum
A_SAT = (120, 60, 15)       # Track A weekend session
A_SMALL = (30, 20, 10)      # short Track A task on a weekday

# ------------------------------------------------------------------ phases
phases = [
    dict(id="A1", track=AI, title="Vendor validator + Demo #1", start="2026-10-22", end="2026-11-30"),
    dict(id="A2", track=AI, title="Q&A assistant + Roadmap Lab v1", start="2026-12-01", end="2026-12-31"),
    dict(id="A3", track=AI, title="Go-live Data Kit + Demo #2", start="2026-12-15", end="2027-01-31"),
    dict(id="A4", track=AI, title="AI invoice-to-journal checker", start="2027-01-21", end="2027-02-28"),
    dict(id="A5", track=AI, title="PlantFlow: data checks + one more module", start="2027-03-01", end="2027-04-03"),
    dict(id="A6", track=AI, title="Portfolio for June (Demo #3)", start="2027-04-04", end="2027-05-31"),
    dict(id="A7", track=AI, title="Portfolio upkeep (optional D365)", start="2027-06-01", end="2027-12-31"),
    dict(id="B1", track=ACC, title="Journal → ledger → trial balance", start="2026-10-01", end="2026-12-31"),
    dict(id="B2", track=ACC, title="Final accounts, P2P and O2C", start="2027-01-01", end="2027-01-31"),
    dict(id="B3", track=ACC, title="Bank rec, depreciation, GST + SAP start", start="2027-02-01", end="2027-02-28"),
    dict(id="B4", track=ACC, title="Month-end close + revision", start="2027-03-01", end="2027-03-31"),
    dict(id="B5", track=ACC, title="SAP core: G/L, partners, documents, periods", start="2027-04-01", end="2027-05-31"),
    dict(id="B6", track=ACC, title="SAP payables + document types", start="2027-06-01", end="2027-06-30"),
    dict(id="B7", track=ACC, title="SAP receivables, bank, payment run, assets", start="2027-07-01", end="2027-08-31"),
    dict(id="B8", track=ACC, title="Exam practice + C_TS4FI", start="2027-09-01", end="2027-11-30"),
    dict(id="C1", track=COM, title="Weekly recordings + user trainings", start="2026-11-15", end="2027-11-30"),
    dict(id="C2", track=COM, title="CV, LinkedIn and STAR stories", start="2027-12-01", end="2027-12-31"),
    dict(id="N1", track=NET, title="Weekly LinkedIn messages + market checks", start="2026-11-15", end="2027-05-31"),
    dict(id="N2", track=NET, title="Bridge step: applications + referrals", start="2027-06-01", end="2027-12-31"),
]

milestones = [
    dict(id="M1", title="Validator finished · Demo #1 · P2P note", date="2026-11-30", track=AI),
    dict(id="M2", title="Trial balance done · Q&A assistant at 80%", date="2026-12-18", track=ACC),
    dict(id="M3", title="Roadmap Lab v1 in daily use", date="2026-12-31", track=AI),
    dict(id="M4", title="Demo #2: Go-live Data Kit", date="2027-01-18", track=AI),
    dict(id="M5", title="AI invoice checker · SAP course started", date="2027-02-27", track=AI),
    dict(id="M6", title="Accounting basics complete", date="2027-03-31", track=ACC),
    dict(id="M7", title="Bridge step: first applications", date="2027-06-02", track=NET),
    dict(id="M8", title="All SAP course units done", date="2027-08-31", track=ACC),
    dict(id="M9", title="C_TS4FI exam", date="2027-10-28", track=ACC),
    dict(id="M10", title="CV, LinkedIn and portfolio updated", date="2027-12-31", track=COM),
]

# ------------------------------------------------------------------ Track A: before the app (assumed done)
add("A-PY-01", "A1", AI, D(2026, 10, 22), "Python Day 1: VS Code, variables and print()",
    "Install Python and VS Code; variables, print(), comments", "Print a fake vendor's name and city",
    "Script runs from VS Code without errors", 60, 45, 15)
add("A-PY-02", "A1", AI, D(2026, 10, 24), "Python Day 2: strings and f-strings",
    "strip(), upper(), len(), slicing, f-strings", "Clean a messy vendor name and print it with an f-string",
    "Output looks right for 3 test names", 60, 45, 15)
add("A-PY-03", "A1", AI, D(2026, 10, 25), "Python Day 3: lists and for-loops",
    "Lists, append(), range(), for-loops", "Loop over 5 fake vendors and print each one",
    "All 5 printed, numbered from 1", 60, 45, 15)
add("A-PLANT-01", "A1", AI, D(2026, 10, 28), "PlantFlow: Customer-module prototype live",
    "AI-assisted build: forms, validations, role-based approvals, saved data",
    "Deploy on GitHub + Render; log it in your manager's sheet", "Live link logged in the sheet", 120, 120, 15)
add("A-PY-04", "A1", AI, D(2026, 10, 31), "Python Day 4: if / elif / else",
    "Comparisons (== vs =), and / or, elif", "Label each vendor 'OK' or 'Check' by a simple rule",
    "Labels correct for your 5 test vendors", 60, 45, 15)
add("A-PY-05", "A1", AI, D(2026, 11, 1), "Python Day 5: functions and return",
    "def, parameters, return vs print, default values", "Write is_missing(value) and use it in a loop",
    "Function returns True/False correctly for 5 cases", 60, 45, 15)
add("A-PY-06", "A1", AI, D(2026, 11, 7), "Python Day 6: pandas read_excel; mandatory fields",
    "Dictionaries; pd.read_excel; isna()", "Report vendors with any mandatory field blank",
    "Every planted blank field is reported", 90, 60, 15)
add("A-PY-07", "A1", AI, D(2026, 11, 8), "Python Day 7: regex basics; PAN check",
    "re.fullmatch, [A-Z]{5}[0-9]{4}[A-Z], raw strings", "check_pan() on the fake file",
    "All planted bad PANs are reported", 90, 40, 15)
add("A-PY-08", "A1", AI, D(2026, 11, 11), "Python Day 8: GSTIN, duplicates, Excel error report",
    "GSTIN format; df.duplicated(); to_excel()", "Add GSTIN and duplicate checks; write errors to Excel",
    "Error report opens in Excel with one row per problem", 90, 60, 15)

# ------------------------------------------------------------------ Track A: Nov 2026 (DRC weekends + short weekday tasks)
add("A-PY-09", "A1", AI, D(2026, 11, 15), "Python Day 9: loop over rows; NaN with pd.isna()",
    "iterrows and pd.isna()", "Treat NaN as missing in is_missing; collect errors",
    "Missing-field count equals the number planted", 90, 40, 15)
add("A-P2P-1", "A1", AI, D(2026, 11, 17), "P2P agent note: process and agents (sections 1–2)",
    "Procure-to-pay steps; one agent per step", "Write sections 1–2 of the one-page note",
    "Process line and 5 agents written", *A_SMALL)
add("A-PLANT-PW", "A1", AI, D(2026, 11, 18), "PlantFlow: change the admin password",
    "Why shared admin logins are risky", "Change it; share the login only with your manager",
    "New password saved somewhere safe", 10, 10, 5)
add("A-P2P-2", "A1", AI, D(2026, 11, 19), "P2P agent note: rules and human approval (sections 3–4)",
    "What each agent must check; where a person signs off", "Write sections 3–4",
    "Checks listed per agent; sign-offs for new vendors, bank changes, big payments", *A_SMALL)
add("A-PY-10", "A1", AI, D(2026, 11, 21), "Python Day 10: IFSC check",
    "IFSC format: 4 letters, 0, 6 letters/digits; ^ and $", "check_ifsc(); plant 3 bad IFSCs",
    "All 3 planted IFSC errors reported", *A_SAT)
add("A-PY-11", "A1", AI, D(2026, 11, 22), "Python Day 11: phone and email checks",
    "Regex: \\d{10}, optional +91, simple email pattern", "check_phone() and check_email(); plant bad values",
    "Every planted bad phone and email reported", 90, 40, 15)
add("A-P2P-3", "A1", AI, D(2026, 11, 24), "P2P agent note: risks and testing (sections 5–6); send it",
    "Risks: wrong data, wrong approvals, duplicate payments, AI confidently wrong",
    "Write sections 5–6; offer process mapping, rules, tests and your validator", "Note sent to your manager", *A_SMALL)
add("A-MGR", "A1", AI, D(2026, 11, 25), "Short note to your manager on the CEO's 'do something different' ask",
    "Make your work visible", "3–5 lines: what you built, what it saves, what's next", "Note sent", 20, 15, 10)
add("A-DEMO1-PREP", "A1", AI, D(2026, 11, 27), "Demo #1: 10-minute script and time-saved number",
    "Problem → live run → error report → time saved on 500 rows → PlantFlow → next steps",
    "Time a manual check of 50 rows; scale to 500", "Script fits 10 minutes; time-saved number written", *A_SMALL)
add("A-PY-12", "A1", AI, D(2026, 11, 28), "Python Day 12: try/except; one function per check",
    "try / except ValueError; small functions", "Wrap each check so a blank or odd cell can't crash the run",
    "Run finishes on a file full of blanks and bad types", *A_SAT)
add("A-PY-13", "A1", AI, D(2026, 11, 29), "Python Day 13: second fake file; works offline",
    "Testing with a fresh file; no internet needed", "Run on a second fake file; turn Wi-Fi off and run again",
    "All planted errors found on both files, no crash, offline", 90, 40, 15)
add("A-DEMO1", "A1", AI, D(2026, 11, 30), "Demo #1 by video call (10 min)",
    "Show, don't tell", "Run the demo; ask for one piece of feedback", "Demo given; feedback in your evidence log", 30, 30, 15)

# ------------------------------------------------------------------ Track A: Dec 2026 – Dec 2027
opt = dict(core=False)  # reduced plan pauses Track A builds after Demo #1 (roadmap §10)
add("A-QA-1", "A2", AI, D(2026, 12, 5), "Q&A assistant 1: gather your notes into a ChatGPT Project",
    "ChatGPT Projects: files + instructions", "Add 5–8 note files in your own words (config steps, go-live checklist, common questions). No client data.",
    "Project created with your files", *A_SAT, **opt)
add("A-QA-2", "A2", AI, D(2026, 12, 9), "Q&A assistant 2: write 20 test questions",
    "A test needs known right answers", "Write 20 questions with the right answer and its file name",
    "20 questions + answers in a sheet", *A_SMALL, **opt)
add("A-QA-3", "A2", AI, D(2026, 12, 12), "Q&A assistant 3: instructions and scoring",
    "Tell it to answer only from the notes and name the file", "Ask all 20; mark right/wrong and whether the file is named",
    "16/20 correct (80%), each naming the right file", *A_SAT, **opt)
add("A-APP-1", "A2", AI, D(2026, 12, 19), "Roadmap Lab: review your fix list",
    "Testing your own tool", "Read the week's app notes; send the fix list to Claude", "Fix list sent", *A_SAT, **opt)
add("A-ST-1", "A3", AI, D(2026, 12, 26), "Streamlit 1: upload a file",
    "pip install streamlit; st.file_uploader", "A page that uploads your fake Excel file and shows it",
    "streamlit run shows the uploaded file", *A_SAT, **opt)
add("A-ST-2", "A3", AI, D(2027, 1, 2), "Streamlit 2: validate and download",
    "st.download_button", "Run the validator on the upload; offer the error report", "Error report downloads from the page", *A_SAT, **opt)
add("A-XL-1", "A3", AI, D(2027, 1, 9), "Excel template generator with dropdowns",
    "openpyxl DataValidation", "Generate a vendor template with dropdowns for state and vendor type",
    "Dropdowns work when the file opens in Excel", *A_SAT, **opt)
add("A-ST-3", "A3", AI, D(2027, 1, 11), "Streamlit 3: a colleague tries it (fake file)",
    "Watch, don't help", "Give a colleague the page and a fake file; note where they get stuck",
    "A colleague uses the screen without help", *A_SMALL, **opt)
add("A-SQL-1", "A3", AI, D(2027, 1, 6), "SQL 1: SELECT and WHERE (SQLite)",
    "sqlbolt.com lessons 1–4", "Load your fake vendors into SQLite; select by state", "3 queries that return the right rows", *A_SMALL, **opt)
add("A-SQL-2", "A3", AI, D(2027, 1, 13), "SQL 2: GROUP BY and COUNT",
    "GROUP BY, COUNT(*)", "Count vendors per state", "Counts match pandas", *A_SMALL, **opt)
add("A-SQL-3", "A3", AI, D(2027, 1, 20), "SQL 3: JOIN",
    "INNER and LEFT JOIN", "Join fake invoices to vendors; find invoices with unknown vendors", "Unknown-vendor list is right", *A_SMALL, **opt)
add("A-SQL-4", "A3", AI, D(2027, 1, 27), "SQL 4: find duplicate vendors with SQL",
    "GROUP BY … HAVING COUNT(*) > 1", "Find duplicate PANs in SQL", "SQL finds the same duplicates as Python", *A_SMALL, **opt)
add("A-DEMO2-PREP", "A3", AI, D(2027, 1, 16), "Demo #2 prep: Go-live Data Kit",
    "Validator screen + templates + Q&A score", "Rehearse once with a timer", "10-minute run-through done", *A_SAT)
add("A-DEMO2", "A3", AI, D(2027, 1, 18), "Demo #2: Go-live Data Kit",
    "May slip 2–4 weeks because of DRC — that's fine", "Give the demo; log feedback", "Demo given", 30, 30, 15)
add("A-INV-1", "A4", AI, D(2027, 1, 23), "Invoice checker 1: 20 fake invoices",
    "Joining the two tracks: AI + accounting", "Make 20 fake invoices in Excel (vendor, amount, GST rate)", "20 invoices ready", *A_SAT, **opt)
add("A-INV-2", "A4", AI, D(2027, 1, 30), "Invoice checker 2: ChatGPT suggests journal entries",
    "Give it only fake data", "Ask ChatGPT for the entry for each invoice; save its answers", "20 suggested entries saved", *A_SAT, **opt)
add("A-INV-3", "A4", AI, D(2027, 2, 6), "Invoice checker 3: Python checks debits = credits",
    "Never post an unbalanced entry", "Check every suggested entry balances", "Unbalanced entries flagged", *A_SAT, **opt)
add("A-INV-4", "A4", AI, D(2027, 2, 13), "Invoice checker 4: GST rate and vendor exists",
    "Recalculate GST from base and rate", "Add GST and vendor-exists checks", "Every planted mistake flagged", *A_SAT, **opt)
add("A-INV-5", "A4", AI, D(2027, 2, 20), "Invoice checker 5: log every AI mistake",
    "An error log is your evidence", "Count AI mistakes by type", "Error log shows what the AI got wrong", *A_SAT, **opt)
add("A-INV-6", "A4", AI, D(2027, 2, 27), "Invoice checker: write it up for your manager's AI sheet",
    "Numbers + method", "One paragraph: what it checks, what the AI got wrong, time saved", "Logged in the sheet", *A_SAT, **opt)
for i, (d, title, learn, apply, done) in enumerate([
    (D(2027, 3, 6), "PlantFlow: plan the validator as a data-check feature", "Reuse your checks inside the app", "List which checks run on save", "Plan written"),
    (D(2027, 3, 13), "PlantFlow: build the data-check feature", "Show errors next to fields", "Add checks to the Customer form", "Bad data can't be saved"),
    (D(2027, 3, 20), "Second module: choose Vendor or Sales Order; list fields and rules", "Start from the process, not the screen", "Fields + rules list", "List ready"),
    (D(2027, 3, 27), "Second module: forms and validations", "Same pattern as Customer", "Build the form with checks", "Form saves valid data"),
    (D(2027, 4, 3), "Second module: approvals, deploy, log it", "Role-based approval", "Deploy and log in the AI sheet", "One more module live"),
], start=1):
    add(f"A-PF-{i}", "A5", AI, d, title, learn, apply, done, *A_SAT, **opt)
for id, d, title, learn, apply, done, core in [
    ("A-GL-1", D(2027, 4, 10), "Go-live readiness tracker 1", "What 'ready' means per module", "Sheet or page: item, owner, status", "Tracker skeleton ready", False),
    ("A-GL-2", D(2027, 4, 17), "Go-live readiness tracker 2", "Show % ready", "Add progress per module", "Tracker shows % ready", False),
    ("A-GST-1", D(2027, 4, 24), "GSTIN state-code check", "First 2 digits = state code", "Check the GSTIN state against the vendor's state", "Planted mismatches flagged", False),
    ("A-API-1", D(2027, 5, 1), "REST API practice 1: GET from a free test API", "requests.get, status codes, JSON", "Fetch and print 5 records", "Script prints the data", False),
    ("A-API-2", D(2027, 5, 8), "REST API practice 2: POST and errors", "POST, timeouts, status checks", "Send fake data; handle errors", "Errors handled without a crash", False),
    ("A-RM-1", D(2027, 5, 15), "README with screenshots for each project", "A README sells the project", "Problem, how to run, screenshots, results", "READMEs done", False),
    ("A-DEMO3", D(2027, 5, 22), "Demo #3: record a 3-minute video", "Problem → demo → result", "Record and watch once", "Video saved", True),
    ("A-PORT", D(2027, 5, 29), "Portfolio check: ready for June", "Recruiters skim", "Check links, READMEs, video", "Portfolio ready for June applications", True),
]:
    add(id, "A6", AI, d, title, learn, apply, done, *A_SAT, core=core)
for i, (y, m) in enumerate([(2027, 6), (2027, 7), (2027, 8), (2027, 9), (2027, 10), (2027, 11), (2027, 12)], start=1):
    add(f"A-UP-{i}", "A7", AI, first_weekday_of_month(y, m, 5), "Portfolio upkeep: polish one project",
        "Small, steady improvements", "Fix one thing; add one result number", "One change pushed", *A_SAT, **opt)
add("A-D365-1", "A7", AI, D(2027, 11, 13), "Dynamics 365 basics 1 (only if SAP hiring looks slow)",
    "Microsoft Learn: D365 Finance overview", "One module", "Module finished", *A_SAT, **opt)
add("A-D365-2", "A7", AI, D(2027, 11, 20), "Dynamics 365 basics 2 (only if SAP hiring looks slow)",
    "Microsoft Learn: general ledger in D365", "One module", "Module finished", *A_SAT, **opt)

# ------------------------------------------------------------------ Track B: before the app (assumed done)
add("B-NP-W1", "B1", ACC, D(2026, 10, 30), "NPTEL Week 1 lectures",
    "What accounting is; the accounting equation; concepts", "Notes for each lecture", "Week 1 finished", 180, 180, 15)
add("B-NP-2A", "B1", ACC, D(2026, 11, 13), "NPTEL 2.1–2.5: debit, credit and journal basics",
    "Types of accounts; golden rules", "First Ravi Traders entries", "Lectures 2.1–2.5 finished", 150, 150, 15)


def place(sessions, slots, phase):
    """Put sessions on slots in order. sessions: (id, title, learn, apply, done[, core])."""
    assert len(sessions) <= len(slots), (phase, len(sessions), len(slots))
    for s, d in zip(sessions, slots):
        core = s[5] if len(s) > 5 else True
        add(s[0], phase, ACC, d, s[1], s[2], s[3], s[4], *B_MIN, core=core)
    return slots[len(sessions):]


# Nov–Dec 2026: journal entries, ledger, trial balance (about one lecture every two weekdays)
b1 = []
for lec in ["2.6", "2.7", "2.8", "2.9", "2.10", "2.11", "2.12", "2.13"]:
    b1.append((f"B-NP-{lec}a", f"NPTEL {lec}: watch and take notes", f"Lecture {lec} (journal entries)",
               "Write the 3 main rules from it in your notebook", "Lecture watched; 3 rules in your own words"))
    b1.append((f"B-NP-{lec}b", f"NPTEL {lec}: Ravi Traders entries", f"Use lecture {lec} on new transactions",
               "Invent 5 Ravi Traders transactions; write each journal entry by hand", "5 entries; debits = credits in each"))
for lec in ["3.1", "3.2", "3.3", "3.4"]:
    b1.append((f"B-NP-{lec}", f"NPTEL {lec}: ledger posting", f"Lecture {lec}: posting to ledger accounts",
               "Post 10 Ravi Traders entries to their ledgers", "Each ledger balanced with c/d and b/d"))
for lec in ["4.1", "4.2", "4.3"]:
    b1.append((f"B-NP-{lec}", f"NPTEL {lec}: trial balance", f"Lecture {lec}",
               "Add the balances to your Ravi Traders trial balance draft", "Draft updated; any difference noted"))
b1.append(("B-TB", "Ravi Traders: a trial balance that balances", "What a trial balance does and doesn't catch",
           "Prepare the full trial balance from all ledgers", "Debit total = credit total"))
b1.append(("B-NOTE-JLT", "One-page note: Journal → Ledger → Trial balance", "Explaining builds memory",
           "Write it in your own words with one worked example", "One page done"))
for i in range(1, 6):
    b1.append((f"B-EX-{i}", f"Ravi Traders: 8 more entries ({i}/5)", "Mixed transactions: discounts, returns, GST-free",
               "Write 8 entries without looking at notes first", "8 entries; check against the rules"))
b1.append(("B-EX-6", "Post the new entries to the ledgers", "Posting practice", "Post all new entries", "Ledgers balanced"))
b1.append(("B-EX-7", "Redo the trial balance with all ~80 entries", "Full cycle", "New trial balance", "About 80 entries; trial balance balances"))
b1.append(("B-EX-8", "Revision: 10 entries from memory", "Testing beats rereading", "Cover the answers; write 10 entries", "Score noted"))
b1.append(("B-EX-9", "Read your one-page note aloud; fix gaps", "Self-explanation", "Fix anything unclear", "Note updated"))
place(b1, weekdays(D(2026, 11, 16), D(2026, 12, 31)), "B1")

# Jan 2027: final accounts, payables/receivables, P2P and O2C
fa = [
    ("Final accounts: the trading account", "Trading account and gross profit", "List what goes on each side", "Gross profit formula with an example"),
    ("Closing stock and cost of goods sold", "Opening stock + purchases − closing stock", "Work out Ravi Traders' cost of goods sold", "COGS worked out"),
    ("Ravi Traders: trading account", "Put it together", "Prepare the trading account", "Gross profit worked out"),
    ("Final accounts: the profit and loss account", "Expenses and incomes below gross profit", "List Ravi Traders' P&L items", "List done"),
    ("Ravi Traders: P&L account", "Net profit", "Prepare the P&L", "Net profit worked out"),
    ("Final accounts: the balance sheet", "Assets = liabilities + capital, in a statement", "Sort Ravi Traders' balances", "Items sorted"),
    ("Adjustments: outstanding and prepaid expenses", "Accrual and matching", "Adjust rent and insurance", "Two adjustments done"),
    ("Ravi Traders: balance sheet", "It must balance", "Prepare the balance sheet", "Ravi Traders' P&L and balance sheet ready; the balance sheet balances"),
    ("Payables: creditors and the purchase ledger", "Who you owe", "List Ravi Traders' creditors", "Creditors list matches the ledger"),
    ("Receivables: debtors, sales ledger, bad debts", "Who owes you", "List debtors; write off one bad debt", "Debtors list matches the ledger"),
    ("Purchase-to-pay: steps and documents", "PR → PO → goods receipt → invoice → payment; 3-way match", "Write the documents for each step", "Steps listed"),
    ("Draw the purchase-to-pay flow", "One picture beats a page", "Draw the P2P flow", "One P2P diagram"),
    ("Order-to-cash: steps and documents", "Sales order → delivery → invoice → payment", "Write the documents for each step", "Steps listed"),
    ("Draw the order-to-cash flow", "One picture beats a page", "Draw the O2C flow", "One O2C diagram"),
    ("Capital vs revenue expenditure", "Benefit beyond this year = capital", "Sort 10 items", "10 items sorted"),
    ("Accounting concepts: accrual, matching, going concern, prudence", "Why the rules exist", "One example each", "4 examples"),
    ("Ravi Traders: 6 entries with adjustments", "Apply adjustments", "Write 6 entries", "6 entries; debits = credits"),
    ("Revision: journal to balance sheet in one go", "The full cycle", "5 transactions → balance sheet", "Balance sheet balances"),
    ("Revision: redo 5 old entries without notes", "Retrieval practice", "Cover the answers", "Score noted"),
    ("Revision: explain P2P and O2C aloud in 2 minutes", "Explain in your own words", "Record yourself", "Recording done"),
    ("Revision: trading account quiz", "Testing beats rereading", "Make 5 questions and answer them", "Score noted"),
]
place([(f"B-FA-{i}",) + s for i, s in enumerate(fa, start=1)], weekdays(D(2027, 1, 1), D(2027, 1, 31)), "B2")

# Feb 2027: accounting on Mon/Wed/Fri, SAP course on Tue/Thu
feb_acc = [
    ("B-BRS-1", "Bank reconciliation: why the balances differ", "Unpresented cheques, uncredited deposits, bank charges", "List the 4 common reasons", "Reasons listed with examples"),
    ("B-BRS-2", "Ravi Traders: one bank reconciliation", "Adjust the cash book first", "Prepare one reconciliation", "Adjusted cash book = adjusted bank balance"),
    ("B-DEP-1", "Depreciation: straight-line method", "(Cost − residual) ÷ life", "3 examples", "3 answers checked"),
    ("B-DEP-2", "Depreciation: written-down value method", "Rate × book value", "3 examples", "3 answers checked"),
    ("B-DEP-3", "Ravi Traders: depreciation schedule", "Year-by-year table", "5-year schedule for one asset", "One depreciation schedule"),
    ("B-GST-1", "GST basics: CGST, SGST and IGST", "Same state vs another state", "Sort 6 sales", "6 sales sorted"),
    ("B-GST-2", "GST: input tax credit and purchase entries", "Input tax is claimed back", "Write 3 purchase entries with GST", "3 entries balanced"),
    ("B-GST-3", "GST: sales entries", "Output tax is owed", "Write 3 sales entries with GST", "3 entries balanced"),
    ("B-GST-4", "Ravi Traders: 5 entries with GST", "Mix of purchases and sales", "Write 5 entries", "5 entries balanced"),
    ("B-FEB-R1", "Revision: bank reconciliation from memory", "Retrieval practice", "Redo one without notes", "Done"),
    ("B-FEB-R2", "Revision: depreciation both ways", "Retrieval practice", "2 quick sums", "Both right"),
    ("B-FEB-R3", "Revision: GST entries", "Retrieval practice", "3 entries without notes", "Score noted"),
]
sap_unit = lambda n: (f"B-SAP-{n:02d}", "SAP course: next unit", "learning.sap.com: 'Implementing Financial Accounting in SAP S/4HANA'",
                      "Finish one unit; write 3 key terms", "Unit done; 3 terms noted")
feb_sap = [("B-SAP-01", "SAP course: start the learning journey", "Enrol on learning.sap.com: 'Implementing Financial Accounting in SAP S/4HANA'; organisational units",
            "Finish the first unit", "First unit done"),
           ("B-SAP-02", "SAP course: next unit (the Universal Journal)", "S/4HANA keeps FI and CO in one table", "Finish one unit; write 3 key terms", "Unit done")]
feb_sap += [sap_unit(n) for n in range(3, 9)]
place(feb_acc, weekdays(D(2027, 2, 1), D(2027, 2, 28), (0, 2, 4)), "B3")
place(feb_sap, weekdays(D(2027, 2, 1), D(2027, 2, 28), (1, 3)), "B3")

# Mar 2027: month-end close + revision; SAP continues Tue/Thu
mar_acc = [
    ("B-MEC-1", "Month-end close: accruals and provisions", "Expenses belong to the month they were used", "List 5 month-end accruals", "List done"),
    ("B-MEC-2", "Month-end close: cut-off and reconciliations", "Right period; bank and ledger checks", "Write the cut-off rule with 2 examples", "Done"),
    ("B-MEC-3", "Month-end close: the closing checklist", "Order matters", "Draft a 10-step checklist", "Checklist drafted"),
    ("B-MEC-4", "'Month-end close in Kagami' guide (part 1)", "Map each step to Kagami", "Write steps 1–5", "Half done"),
    ("B-MEC-5", "'Month-end close in Kagami' guide (part 2)", "Map each step to Kagami", "Write steps 6–10", "One-page guide finished"),
    ("B-NPTEL-EXAM", "NPTEL exam: check the fee and date (optional)", "Optional certificate", "Check the course page", "Decision made", False),
]
for topic in ["journal entries", "ledger and trial balance", "final accounts", "adjustments", "bank reconciliation",
              "depreciation", "GST", "P2P and O2C"]:
    mar_acc.append((f"B-REV-{topic.split()[0][:4].upper()}", f"Revision: {topic}", "Testing beats rereading",
                    "Answer 5 questions from memory, then check", "Score noted"))
place(mar_acc, weekdays(D(2027, 3, 1), D(2027, 3, 31), (0, 2, 4)), "B4")
place([sap_unit(n) for n in range(9, 18)], weekdays(D(2027, 3, 1), D(2027, 3, 31), (1, 3)), "B4")

# Apr–May 2027: SAP becomes the main focus; map each setting to Kagami
named = [
    ("B-SAP-GL-1", "SAP: G/L account master (FS00) and chart of accounts", "G/L accounts, chart of accounts", "Note the key fields", "Fields noted"),
    ("B-SAP-GL-2", "Map: G/L accounts → Kagami", "Same idea, different screens", "Map each field to its Kagami equivalent", "Mapping table done"),
    ("B-SAP-FY-1", "SAP: fiscal year variants (V3, K4)", "Periods and special periods", "Note how V3 fits India", "Done"),
    ("B-SAP-PP-1", "SAP: posting periods (OB52)", "Open and close periods", "Write the month-end period steps", "Step-list written"),
    ("B-SAP-FYPP-2", "Map: fiscal year and posting periods → Kagami", "", "Mapping table", "Mapping done"),
    ("B-SAP-BP-1", "SAP: business partners (BP)", "One master for vendors and customers", "Note the roles", "Done"),
    ("B-SAP-BP-2", "Map: vendor and customer master → Kagami", "", "Mapping table", "Mapping done"),
    ("B-SAP-DOC-1", "SAP: document types and number ranges", "SA, KR, KZ, DR, DZ", "Write what each type is for", "Table done"),
    ("B-SAP-DOC-2", "SAP: posting keys", "40/50 for G/L; 31/21/25 vendors; 01/11/15 customers", "Make a posting-key table", "Table done"),
    ("B-SAP-DOC-3", "Map: document control → Kagami", "", "Mapping table", "Mapping done"),
    ("B-SAP-CC-1", "SAP: company code global settings", "Currency, country, fiscal year variant", "Note the settings", "Done"),
    ("B-SAP-FS-1", "SAP: field status variants", "Required, optional, hidden fields", "Note 3 examples", "Done"),
    ("B-SAP-TOL-1", "SAP: tolerance groups", "Posting limits per user group", "Note why limits matter", "Done"),
]
gen = []
n = 18
slots = weekdays(D(2027, 4, 1), D(2027, 5, 31))
while len(named) + len(gen) < len(slots):
    if len(gen) % 2 == 0:
        gen.append(sap_unit(n)); n += 1
    else:
        gen.append((f"B-SAP-SL-{len(gen)}", "Step-list: write one SAP setting step by step", "Written step-lists are exam gold",
                    "Pick one setting from this week; write its steps", "Step-list written"))
mixed = []
gi = iter(gen)
for s in named:  # one named topic, then one or two generic sessions
    mixed.append(s)
    for _ in range(2):
        nxt = next(gi, None)
        if nxt:
            mixed.append(nxt)
mixed += list(gi)
place(mixed, slots, "B5")

# Jun 2027: accounts payable + document types (exam weights from the roadmap)
jun = [
    ("B-SAP-AP-1", "SAP: vendor invoice (FB60)", "Accounts payable is 21–30% of the exam", "Write the FB60 step-list", "Step-list written"),
    ("B-SAP-AP-2", "SAP: vendor posting keys (31, 21, 25)", "Credit memo vs invoice vs payment", "Table with examples", "Done"),
    ("B-SAP-AP-3", "SAP: outgoing payment (F-53)", "Manual payment", "Write the step-list", "Step-list written"),
    ("B-SAP-AP-4", "SAP: vendor credit memo", "Reverse direction", "Write the step-list", "Step-list written"),
    ("B-SAP-AP-5", "Step-list: vendor invoice to payment", "End to end", "Join the step-lists", "One full AP step-list"),
    ("B-SAP-DT-1", "SAP: document types in detail (11–20% of the exam)", "Number ranges, reversal types", "Write notes", "Done"),
    ("B-SAP-DT-2", "Step-list: document types", "Exam tasks", "Write the step-list", "Step-list written"),
]
slots = weekdays(D(2027, 6, 1), D(2027, 6, 30))
k = 0
while len(jun) < len(slots):
    jun.append(sap_unit(n) if k % 2 == 0 else (f"B-AP-PR-{k}", "Exam topic practice: accounts payable", "Retrieval practice",
                                               "Redo one AP step-list from memory", "Done"))
    if k % 2 == 0:
        n += 1
    k += 1
place(jun, slots, "B6")

# Jul–Aug 2027: receivables, house bank, payment run, asset accounting, company code, G/L
ja_named = [
    ("B-SAP-AR-1", "SAP: customer invoice (FB70)", "Receivables", "Write the step-list", "Step-list written"),
    ("B-SAP-AR-2", "SAP: incoming payment (F-28)", "Clearing open items", "Write the step-list", "Step-list written"),
    ("B-SAP-AR-3", "Step-list: receivables end to end", "Join the step-lists", "One full AR step-list", "Done"),
    ("B-SAP-HB-1", "SAP: house banks and bank accounts", "Where the money sits", "Write notes", "Done"),
    ("B-SAP-HB-2", "Step-list: house bank set-up", "Exam task", "Write the step-list", "Step-list written"),
    ("B-SAP-PAY-1", "SAP: automatic payment program (F110)", "Parameters, proposal, payment run", "Write the step-list", "Step-list written"),
    ("B-SAP-PAY-2", "Step-list: F110 payment run", "Exam task", "Redo from memory", "Done"),
    ("B-SAP-AA-1", "SAP: asset master (AS01) and asset classes", "Asset accounting", "Write the step-list", "Step-list written"),
    ("B-SAP-AA-2", "SAP: depreciation run (AFAB)", "Planned vs posted depreciation", "Write the step-list", "Step-list written"),
    ("B-SAP-AA-3", "Step-list: asset acquisition to depreciation", "End to end", "Join the step-lists", "Done"),
    ("B-SAP-CC-2", "SAP: company code and G/L review", "Tie it together", "One-page summary", "Done"),
    ("B-EXAM-CHECK", "Recheck the C_TS4FI exam page", "Topics, weights and price change", "Note any changes", "Exam page checked"),
]
slots = weekdays(D(2027, 7, 1), D(2027, 8, 31))
gen = []
while len(ja_named) + len(gen) + 1 < len(slots):
    gen.append(sap_unit(n)); n += 1
mixed = []
gi = iter(gen)
for s in ja_named:
    mixed.append(s)
    for _ in range(2):
        nxt = next(gi, None)
        if nxt:
            mixed.append(nxt)
mixed += list(gi)
mixed.append(("B-SAP-FINAL", "SAP course: finish the last units", "Close the course", "Finish what's left", "All SAP course units done"))
place(mixed, slots, "B7")

# Sep–Nov 2027: buy the pack in Sep, 10 practice hours, exam in Oct, attempt 2 in Nov if needed
add("B-EXAM-PACK", "B8", ACC, D(2027, 9, 1), "Buy the two-attempt C_TS4FI exam pack",
    "It runs on a 12-month clock, so buy now, not earlier", "Buy it on SAP's site; note the practice-hours dates",
    "Pack bought", 30, 30, 15)
topics = ["accounts payable", "document types", "G/L accounts", "receivables", "house bank", "payment run",
          "asset accounting", "company code", "posting periods", "business partners"]
slots = weekdays(D(2027, 9, 2), D(2027, 10, 26))
place([(f"B-EP-{i + 1}", f"Exam topic: {topics[i % len(topics)]}", "Step-lists you wrote earlier",
        "Redo the step-list from memory, then check", "Done from memory") for i in range(len(slots))], slots, "B8")
practice_days = [D(2027, 9, 11), D(2027, 9, 12), D(2027, 9, 18), D(2027, 9, 19), D(2027, 9, 25), D(2027, 9, 26),
                 D(2027, 10, 9), D(2027, 10, 16), D(2027, 10, 17), D(2027, 10, 23)]
for i, d in enumerate(practice_days, start=1):
    drc = 60 if d.weekday() == 5 else 40
    add(f"B-PRACT-{i}", "B8", ACC, d, f"Exam practice hour {i} of 10", "Use the practice hours on exam tasks only",
        "Do one exam task in the system", "Task done once in the system", 60, drc, 15)
add("B-EXAM-1", "B8", ACC, D(2027, 10, 28), "C_TS4FI exam: attempt 1",
    "One system-based activity, 3 hours, 60% to pass", "Sleep well; take the exam", "Exam taken; result noted", 180, 180, 180)
slots = weekdays(D(2027, 11, 1), D(2027, 11, 23))
place([(f"B-EX2-{i + 1}", "Attempt 2 prep (skip if you passed)", "Weak topics from attempt 1",
        "Redo one weak step-list", "Done", False) for i in range(len(slots))], slots, "B8")
add("B-EXAM-2", "B8", ACC, D(2027, 11, 25), "C_TS4FI exam: attempt 2 (only if needed)",
    "60% or more", "Take attempt 2", "Exam taken", 180, 180, 180, core=False)

# ------------------------------------------------------------------ Communication + Networking
rec_i = li_i = 0
for d in sundays(D(2026, 11, 15), D(2027, 12, 26)):
    rec_i += 1
    add(f"C-REC-{rec_i:02d}", "C1" if d < D(2027, 12, 1) else "C2", COM, d, "Record a 2-minute explanation of what you built",
        "Problem → what you did → result, in plain words", "Record on your phone; listen once", "2-minute recording saved", 15, 10, 5)
    if d in (D(2027, 6, 6), D(2027, 6, 20), D(2027, 7, 4)):
        add(f"N-REF-{[D(2027, 6, 6), D(2027, 6, 20), D(2027, 7, 4)].index(d) + 1}", "N2", NET, d, "Ask one contact for a referral",
            "Ask people you've already talked to", "Short message: role, why you fit, a link to your portfolio", "Message sent", 15, 10, 5)
    else:
        li_i += 1
        add(f"N-LI-{li_i:02d}", "N1" if d < D(2027, 6, 1) else "N2", NET, d, "LinkedIn: one message to someone in SAP finance or data migration",
            "Ask about their work, not for a job", "One short, specific question", "Message sent", 15, 10, 5)

for i, (sun, thu) in enumerate([(D(2027, 1, 10), D(2027, 1, 14)), (D(2027, 4, 11), D(2027, 4, 15)),
                                (D(2027, 7, 11), D(2027, 7, 15)), (D(2027, 10, 10), D(2027, 10, 14))], start=1):
    add(f"N-MKT-{i}a", "N1" if sun < D(2027, 6, 1) else "N2", NET, sun, "Market check: read 20 real job postings",
        "What target roles really ask for", "Note the top 5 skills that repeat", "20 postings read; top 5 noted", 60, 40, 15, core=False)
    add(f"N-MKT-{i}b", "N1" if sun < D(2027, 6, 1) else "N2", NET, thu, "Market check: message 2 people in target roles",
        "Learn from people doing the job", "Ask each one question about their work", "2 messages sent", 20, 15, 10, core=False)

for i, d in enumerate(weekdays(D(2027, 6, 2), D(2027, 12, 29), (2,)), start=1):
    add(f"N-APP-{i:02d}", "N2", NET, d, "Bridge step: send 2 applications",
        "ERP data-migration, finance systems analyst, SAP support roles", "Tailor the first 3 lines of each",
        "2 applications sent", 30, 20, 10)

for i, (y, m) in enumerate([(2026, 12), (2027, 1), (2027, 2), (2027, 3), (2027, 4), (2027, 5)], start=1):
    add(f"C-TRAIN-{i:02d}", "C1", COM, first_weekday_of_month(y, m, 3), "Run a short user training for the client team",
        "One real task end to end, then they try it", "Prepare one task; run 20 minutes", "Training given; 1 lesson noted", 30, 25, 15, core=False)

dec = [("C-CV", "Update your CV with the certificate"), ("C-LINKEDIN", "Update LinkedIn"), ("C-PORTFOLIO", "Update the portfolio")]
dec += [(f"C-STAR-{i}", f"STAR story {i} of 5") for i in range(1, 6)]
for (id, title), d in zip(dec, weekdays(D(2027, 12, 1), D(2027, 12, 31), (0, 2, 4))):
    learn = "Situation, Task, Action, Result — with a number" if id.startswith("C-STAR") else "Lead with what you made correct"
    add(id, "C2", COM, d, title, learn, "Use your evidence log's best entries", "Done", 45, 30, 15)

# ------------------------------------------------------------------ cards: link unlocks
card_ids = set()
for c in CARDS:
    assert c["id"] not in card_ids, c["id"]
    card_ids.add(c["id"])
    assert c["unlockedBy"] in by_id, (c["id"], c["unlockedBy"])
    if c["type"] == "mcq":
        assert c["answer"] in c["options"] and len(set(c["options"])) == len(c["options"]) == 4, c["id"]
    by_id[c["unlockedBy"]]["unlocksCards"].append(c["id"])

# ------------------------------------------------------------------ checks
tasks.sort(key=lambda t: (t["plannedDate"], t["track"] != AI, t["id"]))
per_day = defaultdict(list)
for t in tasks:
    per_day[t["plannedDate"]].append(t)
problems = []
for day, ts in per_day.items():
    if len(ts) > 3:
        problems.append(f"{day}: {len(ts)} tasks")
    drc = sum(t["minutes"]["drc"] for t in ts)
    if len(ts) > 1 and drc > 60:
        problems.append(f"{day}: {drc} DRC minutes over {len(ts)} tasks")
phase_ids = {p["id"] for p in phases}
for t in tasks:
    assert t["phase"] in phase_ids, t["id"]
if problems:
    print("PROBLEMS:\n" + "\n".join(problems))
    sys.exit(1)

roadmap = dict(app="roadmaplab", kind="roadmap", version=VERSION,
               title="Career Roadmap: AI Skills + SAP S/4HANA Finance",
               timeline=dict(start="2026-10-01", end="2027-12-31"),
               tracks=[AI, ACC, COM, NET], phases=phases, milestones=milestones, tasks=tasks)
cards = dict(app="roadmaplab", kind="cards", version=VERSION, cards=CARDS)

out = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets")
os.makedirs(out, exist_ok=True)
with open(os.path.join(out, "roadmap.json"), "w", encoding="utf-8") as f:
    json.dump(roadmap, f, ensure_ascii=False, indent=1)
with open(os.path.join(out, "cards.json"), "w", encoding="utf-8") as f:
    json.dump(cards, f, ensure_ascii=False, indent=1)

from collections import Counter
print(f"tasks: {len(tasks)} ({sum(t['assumedDone'] for t in tasks)} before the app), phases: {len(phases)}, milestones: {len(milestones)}")
print("tasks per track:", dict(Counter(t["track"] for t in tasks)))
print(f"cards: {len(CARDS)} by topic:", dict(Counter(c["topic"] for c in CARDS)))
print("busiest day:", max(len(v) for v in per_day.values()), "tasks; core tasks:", sum(t["core"] for t in tasks))
print("total planned minutes (full):", sum(t["minutes"]["full"] for t in tasks))
