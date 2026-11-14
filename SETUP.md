# Roadmap Lab — setup, one small step at a time

You don't need Android Studio. GitHub builds the app in the cloud; you download it on your phone.

---

## Part 1 — Let Claude upload the code (done once)

1. On **github.com**, create a **private** repository called `roadmap-lab` (already done ✓).
2. In **claude.ai → Settings → Connectors → GitHub → Connect**, choose **Only select repositories → roadmap-lab**.
3. Tell Claude "connected". Claude uploads the code, and GitHub starts building.

---

## Part 2 — Make your signing key (done once, about 10 minutes, laptop is easiest)

Every APK is "signed" with a key. **Updates only install over the old app if they use the same key.**
So you make the key once, keep it in GitHub Secrets (for building) and in your private Google Drive (as a backup).

**Step 2.1 — Choose a password**
- Make up a password of 12+ characters. Write it in a note in a **private** Google Drive folder called `Roadmap Lab key`.

**Step 2.2 — Put the password in GitHub**
1. Open your repository on github.com → **Settings** → **Secrets and variables** → **Actions**.
2. Tap **New repository secret**.
3. Name: `SIGNING_PASSWORD` — Secret: your password. Tap **Add secret**.

**Step 2.3 — Let GitHub make the key**
1. Open the **Actions** tab → on the left, **Create signing key (run once)**.
2. Tap **Run workflow** → **Run workflow**. Wait about 1 minute for the green tick ✓.
3. Open the finished run. Under **Artifacts**, download **signing-key** (a zip file).

**Step 2.4 — Back up the key to Drive**
1. Unzip it. You'll see `roadmaplab-release.jks`, `SIGNING_KEY_BASE64.txt` and `README.txt`.
2. Upload all three to your private `Roadmap Lab key` folder in Google Drive, next to your password note.

**Step 2.5 — Put the key in GitHub**
1. Open `SIGNING_KEY_BASE64.txt`, select **all** the text, copy it.
2. GitHub → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**.
3. Name: `SIGNING_KEY_BASE64` — Secret: paste. Tap **Add secret**.
4. Delete the zip from your laptop's Downloads (the Drive copy is your backup). The copy on GitHub deletes itself after 1 day.

Tell Claude "key added". Claude then publishes **v1.0.0**.

---

## Part 3 — Install on your phone

1. On your phone, open the repository on github.com → **Releases** → **Roadmap Lab v1.0.0**.
2. Tap **RoadmapLab-v1.0.0.apk** to download it.
3. Open the downloaded file. The first time, Android asks to **allow installs from this source** — allow it for your browser or My Files, then go back and tap **Install**.
4. Open **Roadmap Lab** and follow the 3 setup screens.

**Updates (v1.0.1, v1.1.0, …):** same steps; Android shows **Update** instead of Install, and all your data stays.

> Never install a file named `…CHECK-ONLY-do-not-install.apk`. Those are test builds made before the key exists.

---

## Part 4 — Every Sunday: back up to Drive

Today screen → **Back up to Drive** (or Settings → Backup). In the save screen tap **☰ → Drive**, open your backup folder, tap **Save**.
Each backup is a separate dated file, e.g. `RoadmapLab_backup_2026-11-22.json`, so older ones stay.

**Restore after a phone change:** install the APK → Settings → **Restore from a backup file** → pick the newest file in Drive.

---

## Privacy

No analytics, no account, no server. The app has **no internet permission** — it cannot send anything.
Never enter company or client data.
