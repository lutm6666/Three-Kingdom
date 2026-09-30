# Master roster integration validation

Scope: PR #13, five existing playable roster entries. No art identity mapping
was introduced and no original numeric value was inferred by these tests.

## Passed on 2026-09-30

- Host regression: canonical Master loads; changing JSON HP changes the runtime
  General; symbolic troop mismatch rejects the complete update; save IDs and
  roster size remain stable.
- Invalid numeric endpoints (negative, fractional, numeric string, overflow,
  null) and malformed JSON reject the update without mutating the roster.
- All production Java sources compiled against Android 37 API classes with
  ECJ 3.37.0 and Java 17. JDK-provided packages were removed from a temporary API
  classpath copy to avoid ECJ's JDK module duplicate-package conflict.
- Android build-tools 36.0.0 D8 generated DEX with min API 23; aapt2 linked the
  manifest against the unmodified Android 37 platform jar.
- Both Master JSON assets and classes.dex were packaged into a debug APK.
  The canonical asset contains six character rows and zero verified art IDs.
- zipalign and apksigner verification passed (v1/v2/v3 signatures). Manifest
  metadata: application com.openai.threekingdoms, version 2.5.0/code 26,
  min SDK 23, target SDK 37, launchable MainActivity.
- `git diff --check` passed.

## Limits and ongoing checks

The SDK-tool APK is a build test, not a device regression or release artifact.
It uses a temporary debug signing key, not the user's existing signing key.
No Android device/emulator is available in this execution environment, so no
launch, saved-team, battle or activity recreation test is claimed.

Local Gradle 9.6.0 resolved AGP 9.4.0 after applying the environment's network
proxy, but stopped because the installed Java runtime lacks javac. This is
not reported as a Gradle build pass. `android-build.yml` supplies a full JDK 17
and runs the standard Gradle build plus roster tests for relevant PR changes.
Its current run result must be checked separately.

## Standard CI build result

Commit `4a41b6181a749354a49f42eaa0ea6c0119b30329` passed both checks:

- Android build and roster regression, run `36732513640`: SUCCESS. Full JDK 17
  roster tests, standard Gradle `:app:assembleDebug`, and packaged Master/DEX
  assertions all completed successfully.
- Validate reconstructed master, run `36732513787`: SUCCESS.

The first CI attempt failed because setup-android defaulted to the removed
legacy `tools` package. Explicit `packages: platform-tools` fixed setup.
Device launch/battle/save checks remain pending; CI success does not replace them.
