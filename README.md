# P2PMine — Android Miner App

Native Kotlin + Jetpack Compose client for **[P2PMine](https://p2pmine.com)** — Mine Peer to Peer.

## Install the app

P2PMine is not published on Google Play. Use Obtainium, or download the APK.

### Obtainium

Obtainium installs the app from GitHub and notices later versions when a new release is published.

On the phone:

1. Install [Obtainium](https://github.com/ImranR98/Obtainium/releases).
2. Open [Add P2PMine in Obtainium](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.p2pmine.app%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2FYasbir21Sidhu%2Fp2pmine-android%22%2C%22author%22%3A%22Yasbir21Sidhu%22%2C%22name%22%3A%22P2PMine%22%7D).
3. If that link does not open, add this address by hand: `https://github.com/Yasbir21Sidhu/p2pmine-android`
4. When Android asks, allow Obtainium to install apps.

Leave pre-releases off. There is only one APK in each release, so no extra filter is needed.

### Download the APK yourself

Install file: [P2PMine-1.2.0.apk](download/P2PMine-1.2.0.apk)

Direct link: https://github.com/Yasbir21Sidhu/p2pmine-android/releases/download/v1.2.0/P2PMine-1.2.0.apk

### F-Droid

Not listed yet. The packaging file is `fdroid/com.p2pmine.app.yml`. F-Droid publishes an app only after that file is accepted into their catalog. An install from F-Droid and an install from Obtainium are separate: do not mix them on the same phone.

## Production endpoints (configured)

| Setting | Value |
|---------|--------|
| API | `https://p2pmine.com/` |
| WebSocket | `wss://p2pmine.com/ws?token=…` |
| Application ID | `com.p2pmine.app` |
| Brand | P2PMine (logo from pool assets) |

### Live API (from production website)

```
POST /api/auth/login          { identifier, password? }
POST /api/auth/2fa/verify     { tempToken, code }
GET  /api/miner/me
PATCH /api/miner/me           { btcAddress, fiatCurrency, … }
GET  /api/miner/dashboard?range=24h
GET  /api/miner/workers
GET  /api/miner/workers/{id}?range=24h          // detail + per-worker history
GET  /api/miner/workers/{id}/history?range=24h  // hashrate series only
GET  /api/miner/history?range=24h&workerId=…    // account or worker-filtered history
POST /api/miner/payouts       { amountBtc? }   // request payout
GET  /api/public/config
WS   /ws?token=<jwt>          type=miner_dashboard
```

Login is **wallet-native**: email **or** BTC wallet as `identifier`.

## Features

- Dashboard with live hashrate (WebSocket + poll fallback)
- Hashrate chart with **X/Y axis labels** and **long-press value popup**
- Workers list + detail
- Earnings (after visible fee)
- Payouts history + **Request payout**
- Account, biometric lock, dark P2PMine orange theme

## Build

```bash
source ~/.android-env.sh
cd ~/hashcore-android
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Register accounts on https://p2pmine.com then sign in with the same credentials.

## Updates

Ship an update by raising `versionCode` / `versionName`, adding
`fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`, committing
the signed APK under `download/`, and pushing a `v<versionName>` tag.
That tag publishes the GitHub Release Obtainium installs. See [RELEASING.md](RELEASING.md).

Do not commit `signing/`. The upload keystore stays offline. Google Play is not used.

## Architecture

MVVM + Clean Architecture · Hilt · Retrofit/OkHttp · EncryptedSharedPreferences · Compose Material 3
