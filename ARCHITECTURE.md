# Architecture

How a camera frame becomes a spoken instruction, and why the pieces are split the way they are.

## Layers

```
camera/ audio/ gallery/ data/ ui/     Android: CameraX, ML Kit, TTS, MediaStore, DataStore, Compose
------------------------------------  ----------------------------------------------------------
utilities/ models/ pose/ targetpose/  pure Kotlin: no android imports, unit-testable on the JVM
guidance/ photoanalysis/
```

The engine below the line has no Android dependency, which is what makes the 48 unit tests possible without
a device or emulator. The only exceptions in those packages are the three files that must touch Android
types: `pose/detection/*` (ML Kit) and `photoanalysis/PhotoAnalyzer.kt` (Bitmap).

## The live pipeline

```
CameraX ImageAnalysis (~640x480, 4:3, keep-latest backpressure)
  -> LivePoseAnalyzer            ML Kit STREAM_MODE + a luma check for low light
  -> MlKitPoseMapper             ML Kit Pose -> PoseFrame (33 landmarks, upright image pixels)
  -> LandmarkSmoother            EMA alpha 0.55, resets after a 500 ms dropout
  -> CorrectionEngine            stateless per-frame analysis:
       PoseNormalizer              origin = hip midpoint, unit = torso length
       FeatureExtractor            15 geometric features (arm raise, elbow angle, body turn, …)
       RuleEngine                  per-rule score from error vs tolerance
       PoseMatcher                 region scores, then one overall score
       FrameAnalyzer               whole-body framing (too close, feet cut off, off-centre, …)
  -> GuidanceEngine              picks ONE instruction, tracks phase and timing
  -> CoachUiState + VoiceGuide   overlay redraw + TextToSpeech
```

Everything up to `CoachUiState` runs on the camera analysis thread; the UI only ever reads a `StateFlow`.

### Coordinates

Landmarks are always **un-mirrored, upright image pixels**. Mirroring for the front camera happens once, at
draw time, in `OverlayTransform` — which also maps analysis-image pixels to view pixels for a `PreviewView`
in `FILL_CENTER`. Keeping one convention end to end is why the ghost skeleton and the live skeleton line up.

`NormalizedPose` is the second coordinate system: hip-midpoint origin, torso-length unit, so a tall person
far from the camera and a short person close to it produce the same numbers.

## Scoring

A rule scores its feature by how far the measured value is from the target:

| error | status | score |
|---|---|---|
| `<= tolerance / 2` | perfect | 1.0 → 0.95 |
| `<= tolerance` | close | 0.95 → 0.85 |
| beyond | off | falls to 0 at 4× tolerance |

Region score blends the weighted mean of its rules 50/50 with its *worst* rule, and the overall score is

```
raw x (0.6 + 0.4 x worstRegion) x (0.4 + 0.6 x coverage)
```

The worst-rule and worst-region terms exist because averaging alone let one badly wrong arm hide inside a
good total. `coverage` keeps a half-visible body from scoring high.

## Guidance timing

`GuidanceEngine` is the only stateful part of the engine, and it owns every timing decision:

- one instruction at a time, ranked by severity, with a 400 ms delay before switching to a different one;
- 700 ms grace before declaring nobody is there, so a single dropped frame doesn't reset the session;
- ready at 0.85 with **hysteresis** (drops out at 0.78) so the score can't flicker around the threshold —
  but a severity ≥ 2.0 correction breaks out immediately, because genuinely breaking the pose must cancel
  the countdown;
- hold the pose 1000 ms → countdown → capture;
- a manual instruction from photographer mode overrides the automatic one for 2500 ms.

`SpeechScheduler` separately rate-limits the voice: a configurable minimum interval (default 2500 ms) and a
6000 ms cooldown before the same sentence repeats, with a force path for phase changes like the countdown.

## Target poses

A `PoseTemplate` holds a normalized target pose plus the rules derived from it. Rules are always **measured
from the authored pose** by `TemplateFactory` rather than hand-typed, so the ghost skeleton and the numbers
being scored can never disagree.

Two ways a template is born:

- `PoseBuilder` + `PoseLibrary` — a forward-kinematics DSL with adult body proportions, used for the 10
  built-in poses.
- `ReferencePoseFactory` — a still photo detected with the accurate ML Kit model, tolerances relaxed by 20%
  because real photos are noisier, downgraded to `UPPER_BODY` if the legs aren't visible, with a warning if
  more than one face is present (ML Kit only tracks the most prominent person).

## Capture and review

`ImageCapture` → `CaptureHolder` (memory only) → review. `PhotoAnalyzer` downscales to 480 px for the
Laplacian-variance blur and exposure metrics — a fixed working width keeps the thresholds comparable across
phones — and to 1280 px for pose and face detection, then `PhotoQualityEvaluator` (pure Kotlin, tested)
turns those measurements into the check list. Nothing reaches storage until the user taps Save, which hands
the bitmap to `PhotoStore` and MediaStore.

Bitmaps in `CaptureHolder` are deliberately never recycled: the review screen may still be drawing or
analysing one on a background thread. The GC frees them.

## State and DI

`AppContainer` is manual dependency injection — settings, templates, captures, photos, still detector —
created once in `PosePilotApplication` and passed down through `PosePilotNavHost`. Screens that need
lifecycle-scoped work (`CoachViewModel`, `ReferenceViewModel`, `ReviewViewModel`) get a ViewModel; the rest
read the container directly. There is no repository interface layer and no DI framework, because an MVP
with one implementation of everything doesn't earn either.
