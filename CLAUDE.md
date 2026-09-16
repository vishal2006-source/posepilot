# PosePilot — notes for Claude Code

Read `HANDOFF.md` first — it lists every file, the design decisions and the remaining work in order.
Spec: `docs/spec.pdf`.

- Build: `./gradlew assembleDebug`   Tests: `./gradlew testDebugUnitTest` (48 engine tests must stay green)
- Engine packages (utilities, models, pose/analysis|landmarks|rules|matching, targetpose, guidance, photoanalysis except PhotoAnalyzer) are pure Kotlin — no android imports there.
- UI is monochrome (theme object `Pp`); show state by inversion, not colour.
- Coordinates are un-mirrored upright image pixels; mirror only when drawing.
- Don't build Group Pose Mode until the MVP runs on a device.
