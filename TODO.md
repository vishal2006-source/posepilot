# TODO

## Next: the first real build

The engine compiles and its 51 unit tests pass. The Android layer — CameraX, ML Kit, Compose, all screens,
manifest and resources — is written but has **never been compiled**, because the sandbox it was written in
had no Android SDK and no access to Google's Maven repository. So the first job is a build in Android
Studio, and it will find mistakes.

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Already checked without an SDK, so these are *not* where it will break:

- The engine and its tests compile and pass standalone (kotlinc 2.0.21 + JUnit): 51/51.
- The screens parse cleanly, and every `when` over a project type is exhaustive — verified by compiling
  the UI sources against the engine sources, where only androidx symbols stay unresolved.
- `OutlinedTextFieldDefaults.colors` (7 args) and `SwitchDefaults.colors` (6 args) use real parameter
  names, checked against the androidx API surface.
- All six Material icons used by the new screens exist in the Outlined set.

Known places to look when it does fail:

- Opt-in annotations: `FlowRow` needs `@OptIn(ExperimentalLayoutApi::class)`, `ModalBottomSheet` needs
  `@OptIn(ExperimentalMaterial3Api::class)`.
- Deprecated `Icons.Outlined.VolumeUp` / `VolumeOff` (warnings only).
- Anything the Compose compiler plugin enforces — composable call context, stability, remember rules —
  none of which a plain kotlinc run can see.
- Resource linking and manifest merging, which need aapt2 and were never run.
- Unused import `android.os.SystemClock` in `CoachViewModel.kt`.

## Then: tune on a real device

Every threshold below was chosen by reasoning about geometry, not by watching people. Expect to move them.

- `GuidanceConfig.readyThreshold` (0.85) and the hysteresis margin (0.07) — too strict and nobody ever
  reaches "ready", too loose and it fires mid-movement.
- Per-feature tolerances in `PoseFeature` — especially the head features, which ML Kit estimates least well.
- `LandmarkSmoother` alpha (0.55): higher is steadier but laggier.
- `SpeechScheduler` intervals — the honest test is whether a stranger can follow the voice without the
  screen.
- `PhotoQualityEvaluator` blur thresholds (fail 40 / warn 90 at 480 px) against real handheld shots.

Also worth checking on device: overlay alignment assumes the preview and the analysis stream are both 4:3
with `PreviewView` in `FILL_CENTER`; front-camera photos are mirrored when `mirrorFrontCamera` is on; and
TTS needs the `TTS_SERVICE` entry in `<queries>` to be visible at all on API 30+.

## Deferred (post-MVP)

- **Group Pose Mode** (spec Mode 5). ML Kit's live detector tracks one person only, so this needs a
  different detection strategy — not a UI change.
- More library poses and categories. The picker only shows categories that contain poses, so adding them is
  additive.
- Mirrored-pose toggle in the picker — `PoseTemplate.mirrored()` already exists and is unused.
- Launcher icon is a placeholder vector stick figure; a designed icon would be an easy win.
