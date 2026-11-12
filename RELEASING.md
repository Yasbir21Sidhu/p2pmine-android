# Releasing P2PMine updates

One git tag publishes the files the stores update from. F-Droid builds
from the tag. Google Play and direct-install apps use the files attached
to the GitHub Release.

These three installs cannot update each other. Each one is signed by a
different key:

| Channel | Who signs | How users update |
|---|---|---|
| Google Play | Google, after you upload the AAB with the upload key | Play Store |
| GitHub Release APK | Your upload key (`signing/p2pmine-upload.jks`) | Obtainium, or any installer that watches that APK |
| F-Droid | F-Droid, after it rebuilds the tag | F-Droid |

Pick one channel per device. Back up `signing/p2pmine-upload.jks` and
`signing/READ_ME_BACKUP_THESE.txt` offline. They are gitignored. If that
keystore is lost, Play and GitHub-APK updates of this package stop.

## One-time GitHub setup

Create an empty **public** repository named `p2pmine-android`. F-Droid and
Obtainium cannot see a private repo. Then:

```bash
cd ~/hashcore-android
git remote add origin git@github.com:GITHUB_OWNER/p2pmine-android.git
git push -u origin main
```

Replace `GITHUB_OWNER` in `fdroid/com.p2pmine.app.yml` with that owner
before you send the file to F-Droid. The app build does not read that file.

In the repo settings, under **Secrets and variables → Actions**, add:

| Secret | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | `base64 -w0 signing/p2pmine-upload.jks` |
| `ANDROID_KEYSTORE_PASSWORD` | `storePassword` from the offline backup |
| `ANDROID_KEY_ALIAS` | `p2pmine_upload` |
| `ANDROID_KEY_PASSWORD` | `keyPassword` from the offline backup |
| `PLAY_SERVICE_ACCOUNT_JSON` | optional; full JSON key for Play uploads |

Do not paste the keystore or the passwords into a chat, a commit, or an
issue. The release workflow writes them only on the runner, then discards
the runner.

`PLAY_SERVICE_ACCOUNT_JSON` is optional. Without it, download
`P2PMine-<version>.aab` from the GitHub Release and upload it in Play
Console. With it, the same tag is also uploaded to the **internal** track.
Create the Play app and finish the first listing once by hand (see
`PLAY_UPLOAD_GUIDE.md`) before expecting the API upload to succeed. The
service account needs Play Console access and the Release permission.

## Every update

1. Raise `versionCode` by 1 and set `versionName` in `app/build.gradle.kts`.
   Play and F-Droid both reject a repeated `versionCode`.
2. Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
   That text becomes the GitHub Release notes and the F-Droid changelog.
3. Commit and push `main`. Wait until the **CI** workflow's unsigned
   release build is green. That is the build F-Droid runs.
4. Tag the same commit and push the tag. The tag must be `v` plus
   `versionName`, for example `v1.2.0` for `1.2.0`.

```bash
git tag v1.2.0
git push origin v1.2.0
```

The **Release** workflow then:

- fails if the tag does not match `versionName`, or the changelog file is missing
- signs `P2PMine-<version>.apk` and `P2PMine-<version>.aab`
- attaches both to the GitHub Release
- uploads the AAB to Play's internal track when the Play secret is set

Promote the Play release from internal to production in Play Console after
you have installed it from Play and signed in.

The R8 mapping file is uploaded to Play only. It is not attached to the
public GitHub Release.

## F-Droid

F-Droid does not install your APK. It clones the tag, runs
`./gradlew assembleRelease` with no keystore, and signs the result with
its own key. After `v1.2.0` is on GitHub and the signed APK is on the
release:

1. Copy `fdroid/com.p2pmine.app.yml` to
   [fdroiddata](https://gitlab.com/fdroid/fdroiddata) as
   `metadata/com.p2pmine.app.yml`.
2. Replace `GITHUB_OWNER`.
3. Open a merge request. The inclusion guide is
   <https://f-droid.org/docs/Inclusion_How-To/>.
   A request for a brand-new app is also accepted by email to
   `team@f-droid.org`, but a merge request is the normal path.

Later updates are the tag plus a changelog file. `UpdateCheckMode: Tags`
and `AutoUpdateMode: Version` tell F-Droid to add a build for each new
`v*` tag. The first inclusion still needs a person to merge it.

The metadata declares the **NonFreeNet** anti-feature because the app only
talks to p2pmine.com. The license in `LICENSE` is Apache-2.0. F-Droid will
not list the app under another license, and it will not list it at all
without a free license. Change `LICENSE` and the `License:` line before
the first push if Apache-2.0 is not the license you want.

`AllowedAPKSigningKeys` is the SHA-256 of `signing/upload_certificate.pem`.
F-Droid uses it to check that its build matches `P2PMine-<version>.apk`.
Keep signing GitHub releases with that same upload key.

## Obtainium and similar APK installers

Add `https://github.com/GITHUB_OWNER/p2pmine-android`. Obtainium uses the
APK on the latest GitHub Release. Updates work only for installs that
were first installed from that APK, because the signature has to match.
