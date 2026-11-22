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
