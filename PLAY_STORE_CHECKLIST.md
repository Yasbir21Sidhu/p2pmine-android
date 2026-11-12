# P2PMine — Google Play launch checklist

P2PMine is not being published on Google Play. This checklist is unused.

## Security posture (as of this build)

| Control | Status |
|---------|--------|
| HTTPS only / cleartext disabled | Yes |
| Certificate pinning (release) | Yes — leaf + LE intermediate SPKI |
| Tokens in EncryptedSharedPreferences | Yes |
| Tokens excluded from backup | Yes |
| HTTP body logging in release | Off |
| Auth header redaction in logs | Yes |
| 2FA temp token not in error strings | Yes |
| Minify + shrink resources (release) | Yes |
| JWT in WebSocket query string | API design constraint (p2pmine.com uses `?token=`) |

## Ready for Play? **Almost — not fully**

### Done for a technical store build
- `applicationId` `com.p2pmine.app`
- `targetSdk 35` / `minSdk 26`
- Branding / logo
- Production API base URL
- R8 minify for release

### You still must complete (cannot skip for a real listing)

1. **Play Console developer account** ($25 one-time).
2. **Upload key / Play App Signing**  
   Create a release keystore and configure signing in `app/build.gradle.kts` or CI secrets.  
   Debug APK is **not** what you publish.
3. **Privacy policy URL** (required)  
   Publish a page (e.g. `https://p2pmine.com/privacy`) covering: account data, device IDs if any, notifications, server logs.  
   App declares `PRIVACY_POLICY_URL` — page must exist and match Data safety answers.
4. **Data safety form** in Play Console  
   Account email/wallet, auth tokens on device, network transmission to your VPS.
5. **Store listing**  
   Title, short/full description, screenshots (phone), feature graphic 1024×500, icon 512×512.
6. **Content rating** questionnaire (IARC).
7. **Target audience / ads / COVID** declarations (no ads → say so).
8. **Test track** first (internal → closed → production).
9. **Optional but recommended**  
   - In-app register already present  
   - Notification runtime permission prompt (Android 13+) before posting alerts  
   - Crash reporting (Play Vitals / Firebase Crashlytics)  
   - Certificate pin rotation process when Let’s Encrypt leaf key changes  

### Suggested release command (after signing config)

```bash
./gradlew assembleRelease
# or
./gradlew bundleRelease   # preferred AAB for Play
```

## Pin rotation

If TLS breaks after a certificate renewal, recompute:

```bash
echo | openssl s_client -servername p2pmine.com -connect p2pmine.com:443 2>/dev/null \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | openssl enc -base64
```

Update `CERT_PINS` in `app/build.gradle.kts` and ship an update **before** removing old pins when possible.
