# P2PMine — Android Miner App

Native Kotlin + Jetpack Compose client for **[P2PMine](https://p2pmine.com)** — Mine Peer to Peer.

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

## Store updates

The public git repo is the source for F-Droid, Google Play, and direct APK
updates. Ship an update by raising `versionCode` / `versionName`, adding
`fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`, and pushing
a `v<versionName>` tag. See [RELEASING.md](RELEASING.md).

Do not commit `signing/`. The upload keystore stays offline and in GitHub
Actions secrets.

## Architecture

MVVM + Clean Architecture · Hilt · Retrofit/OkHttp · EncryptedSharedPreferences · Compose Material 3
