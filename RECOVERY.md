# Recovery checkpoint

Last refreshed: 2026-11-22 UTC

## Repository identity

- Active accessible repository: `1989johnharrison-oss/ai-collective-nexus.`
- Review branch: `codex/recovery-ci`
- Base branch: `main`
- Base source commit: `4a045e7ca61823dad1d4eba45a39ea2b00ff9f27`
- The previously named repository `johnharrison19899-sys/aisuperhub-android` and PR #2 were not accessible through the current GitHub connection on this run.
- Do not assume continuity from chat. Re-read this file and `RUN_STATE.md`, then refresh branch heads and Actions.

## Recovery procedure

1. Fetch the review branch and main branch heads.
2. Inspect open pull requests and the newest workflow run for the review head.
3. If the current-head run is active, inspect it without starting another.
4. Repair deterministic failures before product work.
5. Retry an infrastructure-only failure at most once for an unchanged source head.
6. Record the tested source SHA, run ID, artifact ID, file size and SHA-256 in both checkpoint files.
7. Never merge, publish a release, add secrets, or install an APK automatically.

## Current bounded change

- Replace the Java checked-exception response decoding that broke APK compilation.
- Restore automatic CI for pushes and pull requests while retaining manual dispatch.
- Verify the expanded backend test suite and APK before further product changes.

## Verified evidence

- Tested product source: `4d23bb8423a47a115a90fe6d9038bc43fe1d4c1b`
- Workflow: run 65, ID `45034624433`, event `push`, conclusion `success`
- Backend tests: passed
- Android `:app:assembleDebug`: passed
- APK metadata gate: versionCode 10 and versionName 2.4 passed
- Artifact: ID `14759062011`, `AI-Collective-Nexus-v2.4-Dark-Edition`
- Artifact ZIP size: 33,788 bytes
- Artifact ZIP SHA-256: `999e7ebf95f59c7c01bc1d512300984899a72c9eac8acd0d8a9d956b4543aca8`
- APK file: `Nexus-v2.4-Dark-Edition.apk`, 37,358 bytes
- APK SHA-256: `d6a79e94f2a61acf62f9460686ef9116cc1931096239be13ca8f9bb42e31c45f`
- Independent archive checks: ZIP integrity passed, APK ZIP integrity passed, file identified as an Android APK with an APK Signing Block.
