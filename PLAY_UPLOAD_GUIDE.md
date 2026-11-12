# P2PMine — Google Play upload guide

P2PMine is not being published on Google Play. Install from Obtainium or,
once accepted, F-Droid. See the README. This file is only a leftover guide
if that decision changes.

Ongoing updates are built from a git tag. After the GitHub repo and Actions
secrets in [RELEASING.md](RELEASING.md) are in place, `git push origin vX.Y.Z`
publishes `P2PMine-X.Y.Z.aab` on the GitHub Release (and uploads it to the
Play internal track when `PLAY_SERVICE_ACCOUNT_JSON` is set). This guide is
still the first-time Play Console listing: account, store text, data safety,
and the first upload.

## Why AAB (`.aab`) instead of APK?

You are right that phones install **APK** packages. That does not change.

**What happens on Play:**

1. You upload an **Android App Bundle** (`.aab`) — a *shipping box* of your app, not the final install file.
2. Google re-packages optimized **APKs** for each device (screen size, CPU, language).
3. When a user taps **Install**, Play downloads the right APK(s) for *their* phone.

**Why Google requires AAB for new apps:**

| | APK upload | AAB upload |
|--|------------|------------|
| File size for users | Often larger (one fat package) | Smaller downloads |
| Who builds device APKs | You | Google Play |
| New Play apps (policy) | Generally not accepted | **Required** |
| Sideload / website install | Use APK | Use APK you build separately |

**You can still build an APK yourself** for testers:

```bash
./gradlew assembleRelease   # → app-release.apk
./gradlew bundleRelease     # → app-release.aab  ← upload this to Play
```

Think of it as: **AAB = master recipe for Play; APK = finished meal for one phone.**

---

## What is already prepared for you (on this machine)

| Item | Path |
|------|------|
| **Upload keystore** | `signing/p2pmine-upload.jks` |
| **Passwords + alias** | `signing/keystore.properties` and `signing/READ_ME_BACKUP_THESE.txt` |
| **Upload certificate (PEM)** | `signing/upload_certificate.pem` |
| **Release AAB** (after you run the build) | `app/build/outputs/bundle/release/app-release.aab` |
| **Package name** | `com.p2pmine.app` |
| **Version** | `1.2.0` (versionCode **3**) |
| **Privacy policy URL** | `https://p2pmine.com/privacy` |

### CRITICAL — back up the keystore now

Copy these offline (USB + password manager):

- `signing/p2pmine-upload.jks`
- `signing/READ_ME_BACKUP_THESE.txt`

If you lose them, **you cannot update the same Play listing** with new versions signed by this upload key.

---

## What only YOU can do (I cannot)

### 1. Create Play Console account
1. Go to [https://play.google.com/console](https://play.google.com/console)
2. Pay the one-time **$25** developer fee
3. Complete identity / developer verification (can take hours to days)

### 2. Create the app
1. **Create app** → name **P2PMine**
2. Default language: English (or English + Hindi later)
3. App or game: **App**
4. Free / paid: **Free**
5. Declarations: accept policies

### 3. Enable Play App Signing (default — keep it ON)
Google keeps the **app signing key**; you keep the **upload key** (the `.jks` we made).  
This is normal and recommended. Do **not** lose the upload keystore.

### 4. Store listing (assets only you can make/upload)
You need:

| Asset | Spec |
|-------|------|
| **App icon** | 512 × 512 PNG (no transparency for Play high-res icon rules — check current console tip) |
| **Feature graphic** | 1024 × 500 PNG/JPG |
| **Phone screenshots** | At least **2** (recommend 4–8). Phone frames optional |
| **Short description** | max 80 characters |
| **Full description** | max 4000 characters |
| **Privacy policy** | `https://p2pmine.com/privacy` |

Suggested short description (edit freely):

```text
Live Bitcoin mining pool dashboard — hashrate, workers, payouts for P2PMine.
```

Suggested full description starter:

```text
P2PMine is the official mobile dashboard for the p2pmine.com Bitcoin mining pool.

• Live hashrate and unpaid balance
• Worker list with stratum setup (copy URL, username, password)
• Earnings and payout history
• Optional biometric lock and two-factor authentication
• Local alerts for important pool events

All earnings shown in the app are after the published pool fee.

Website: https://p2pmine.com
Support: admin@p2pmine.com
Privacy: https://p2pmine.com/privacy
```

### 5. Data safety form (paste answers carefully)

Match your privacy page. Typical for this app:

| Question theme | Suggested answer |
|----------------|------------------|
| Collects user data? | **Yes** |
| Account info | Email / wallet address (account ID) — **collected, required for app function** |
| Financial | Mining balances / payouts — **app functionality** |
| App activity | (if you only use server-side logs) be honest |
| Device IDs | Only if you add analytics later; currently **no advertising ID** |
| Data encrypted in transit? | **Yes** (HTTPS) |
| Can users request deletion? | Yes — via support email / account process on website |
| Data sold? | **No** |
| Shared with third parties? | Only if you use third-party crash/analytics later; base app talks to **p2pmine.com** |

### 6. Content rating
Complete IARC questionnaire honestly (utility / finance-adjacent / no user-generated social feed / no ads if true).

### 7. Target audience
Not primarily children. Complete age groups accordingly.

### 8. Upload the AAB
1. **Test and release** → **Internal testing** (recommended first) or **Closed testing**
2. Create a new release → **Upload** `app-release.aab`
3. Release name: `1.2.0 (3)`
4. Release notes example:

```text
First Play release of P2PMine.
Live dashboard, workers, earnings, payouts, biometric lock, and 2FA setup.
```

5. Review and **start rollout to Internal testing**
6. Add yourself as a tester (email)

### 9. After internal works → Production
Only promote when install + login + workers look good from Play install (not sideload).

---

## Build commands (already wired)

```bash
export JAVA_HOME="$HOME/.local/opt/jdk-17"
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
cd ~/hashcore-android

# Play upload file
./gradlew bundleRelease
# → app/build/outputs/bundle/release/app-release.aab

# Optional: signed APK for direct install testing
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

---

## Checklist before you click Submit

- [ ] Keystore + passwords backed up offline  
- [ ] Privacy policy live and linked  
- [ ] Listing text + screenshots + feature graphic  
- [ ] Data safety completed  
- [ ] Content rating completed  
- [ ] AAB uploaded to **Internal testing** first  
- [ ] You installed from Play and signed in successfully  

---

## What I cannot do for you

1. Pay / create Play Console account  
2. Pass Google identity verification  
3. Design final marketing screenshots / feature graphic (I can help with text or icons if you ask)  
4. Click “Submit for review”  
5. Answer government ID / tax forms if Google asks  
6. Own your keystore forever if you don’t back it up  

When the Console account is ready and you have screenshots, upload the **AAB** path above. If Play shows any error text, paste it here and I will fix the build.
