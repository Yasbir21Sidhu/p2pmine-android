# Releasing P2PMine updates

Google Play is not used. One git tag publishes the GitHub Release that
Obtainium installs. F-Droid, after the app is accepted, builds from that
same tag.

These installs cannot update each other when the signing key differs.
Pick one channel per phone.

| Channel | Who signs | How users update |
|---|---|---|
| GitHub Release APK | Your upload key (`signing/p2pmine-upload.jks`) | Obtainium, or any installer that watches that APK |
| F-Droid | F-Droid's key, unless their rebuild matches the GitHub APK | F-Droid |
| Google Play | Not in use | |

Pick one channel per device. Back up `signing/p2pmine-upload.jks` and
`signing/READ_ME_BACKUP_THESE.txt` offline. They are gitignored. If that
keystore is lost, Play and GitHub-APK updates of this package stop.

## One-time GitHub setup

The public repository is `Yasbir21Sidhu/p2pmine-android`. Obtainium and
F-Droid cannot see a private repo. `fdroid/com.p2pmine.app.yml` already
points at that address. The app build does not read that file.

Do not paste the keystore or the passwords into a chat, a commit, or an
issue. The release workflow does not receive the keystore. Sign the APK
on a machine that has `signing/`, then commit only `download/P2PMine-<version>.apk`.

## Every update

1. Raise `versionCode` by 1 and set `versionName` in `app/build.gradle.kts`.
   F-Droid rejects a repeated `versionCode`. Android will not install an
   update unless the new `versionCode` is higher.
2. Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
   That text becomes the GitHub Release notes and the F-Droid changelog.
3. Build the signed release APK and copy it to
   `download/P2PMine-<versionName>.apk`.
4. Commit and push `main`.
5. Tag that same commit and push the tag. The tag must be `v` plus
   `versionName`, for example `v1.2.0` for `1.2.0`.

```bash
git tag v1.2.0
git push origin v1.2.0
```

The **Release** workflow then attaches `download/P2PMine-<versionName>.apk`
to the GitHub Release. It does not build the app and it does not talk to
Google Play. Obtainium installs that release APK.

## F-Droid

F-Droid clones the tag and runs `./gradlew assembleRelease` with no
keystore. `Binaries` and `AllowedAPKSigningKeys` ask F-Droid to publish
the GitHub APK when that rebuild matches it, so Obtainium and F-Droid
can share a signature. If the rebuild does not match, F-Droid will not
publish until the recipe is adjusted, and a later F-Droid-signed build
cannot update an Obtainium install. After `v1.2.0` is on GitHub and the
signed APK is on the release:

1. Copy `fdroid/com.p2pmine.app.yml` to
   [fdroiddata](https://gitlab.com/fdroid/fdroiddata) as
   `metadata/com.p2pmine.app.yml`.
2. Open a merge request. The inclusion guide is
   <https://f-droid.org/docs/Inclusion_How-To/>.
   A request for a brand-new app is also accepted by email to
   `team@f-droid.org`, but a merge request is the normal path.

Later updates are the tag plus a changelog file. `UpdateCheckMode: Tags`
and `AutoUpdateMode: Version` tell F-Droid to add a build for each new
`v*` tag. The first inclusion still needs a person to merge it.

The metadata declares **NonFreeNet** and **TetheredNet** because the app
only talks to p2pmine.com and the address cannot be changed in the app.
The license in `LICENSE` is Apache-2.0. F-Droid will
not list the app under another license, and it will not list it at all
without a free license. Change `LICENSE` and the `License:` line before
the first push if Apache-2.0 is not the license you want.

`AllowedAPKSigningKeys` is the SHA-256 of `signing/upload_certificate.pem`.
F-Droid uses it to check that its build matches `P2PMine-<version>.apk`.
Keep signing GitHub releases with that same upload key.

## Obtainium and similar APK installers

Add `https://github.com/Yasbir21Sidhu/p2pmine-android`. Obtainium uses the
APK on the latest GitHub Release. Updates work only for installs that
were first installed from that APK, because the signature has to match.
