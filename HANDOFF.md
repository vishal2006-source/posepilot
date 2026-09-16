# PosePilot — Build Handoff (status as of 16 Sep 2026)

Android MVP: real-time AI photography pose coach.
Stack: Kotlin, Jetpack Compose, CameraX, ML Kit Pose Detection, DataStore, on-device/offline.
Package: com.posepilot.app | minSdk 29 | compile/targetSdk 35 | JVM 17

---------------------------------------------------------------------
## 1. BUILD STATUS

- Pure-Kotlin engine (no Android imports): COMPILED + 48/48 unit tests PASS.
- Android layer (CameraX, ML Kit, Compose UI): WRITTEN, NOT YET COMPILED.
  The build sandbox blocked Google Maven + Android SDK, so the first real
  build must happen in Android Studio. Expect some compile fixes.
- App will NOT build yet: missing screens + AndroidManifest + resources (see section 5).

Commands (in Android Studio terminal):
  ./gradlew testDebugUnitTest
  ./gradlew assembleDebug

---------------------------------------------------------------------
## 2. PROJECT SETUP (done)

settings.gradle.kts, build.gradle.kts, gradle.properties, gradlew/gradlew.bat,
gradle/wrapper (Gradle 8.9), app/build.gradle.kts, app/proguard-rules.pro, .gitignore

gradle/libs.versions.toml:
  AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.10.01, CameraX 1.4.0,
  ML Kit pose-detection + pose-detection-accurate 18.0.0-beta5, face-detection 16.1.7,
  navigation-compose 2.8.3, lifecycle 2.8.7, DataStore 1.1.1, coroutines 1.8.1, JUnit 4.13.2

---------------------------------------------------------------------
## 3. PIPELINE

Camera frame -> ML Kit PoseFrame -> LandmarkSmoother (EMA)
 -> CorrectionEngine: FrameAnalyzer + PoseNormalizer + FeatureExtractor + PoseMatcher + ghost projection
 -> GuidanceEngine (one instruction, hold, countdown, capture) -> UI state + VoiceGuide (TTS)
Capture -> CaptureHolder (memory) -> Review (PhotoAnalyzer quality report) -> PhotoStore (MediaStore)

---------------------------------------------------------------------
## 4. FILES — app/src/main/java/com/posepilot/app/

### utilities/ (engine, tested)
- Vectors.kt — Vec2/Vec3 (x right, y down).
- AngleMath.kt — jointAngle 0..180, heading, rotate, deg/rad.
- OverlayTransform.kt — analysis-image px -> view px for PreviewView FILL_CENTER, mirror for front cam.

### models/ (engine, tested)
- LandmarkType.kt — 33 ML Kit landmarks, .opposite, SKELETON_CONNECTIONS, OVERLAY_JOINTS.
  Mouth points are LEFT_MOUTH / RIGHT_MOUTH.
- Landmark.kt — Landmark, PoseFrame (VISIBILITY_THRESHOLD 0.5).
- BodyRegion.kt — weights HEAD .15, TORSO .20, L_ARM .20, R_ARM .20, LEGS .25; FramingType FULL_BODY/UPPER_BODY.
- PoseCorrection.kt — ArrowKind LIMB/STEP/ROTATE, CorrectionArrow, PoseCorrection, PoseComparison(+EMPTY).
- GuidanceInstruction.kt — GuidancePhase NO_PERSON/GUIDING/HOLD/COUNTDOWN/CAPTURED, tone, GuidanceUpdate.
- PhotoAnalysis.kt — CheckStatus, QualityCheck, PhotoAnalysis, PhotoMeasurements.
- UserSettings.kt — voice, skeleton, target, percentage, autoCapture, countdownSeconds=3,
  instructionIntervalMs=2500, mirrorFrontCamera, useFrontCamera; LensFacing; CameraState.

### pose/ (engine, tested)
- landmarks/PoseNormalizer.kt — origin = hip midpoint, unit = torso length; null if shoulders/hips unreliable.
- landmarks/LandmarkSmoother.kt — EMA alpha 0.55, resets after 500 ms dropout.
- analysis/PoseFeature.kt — 15 features: BODY_TURN, TORSO_LEAN, L/R_ARM_RAISE, L/R_ELBOW_ANGLE,
  L/R_HAND_REACH, STANCE_WIDTH, L/R_KNEE_ANGLE, SHOULDER_TILT, HEAD_TURN, HEAD_TILT, CHIN_HEIGHT
  (region, MeasureUnit, landmarks, tolerance, priority, instruction texts).
- analysis/FeatureExtractor.kt — pure geometry for each feature.
- analysis/FrameAnalyzer.kt — FEET_CUT_OFF, HEAD_CUT_OFF, TOO_CLOSE, TOO_FAR, MOVE_TO_SUBJECT_LEFT/RIGHT.
- rules/PoseRule.kt, rules/RuleEngine.kt — scoring: err<=tol/2 PERFECT (1.0-0.95), err<=tol CLOSE (0.95-0.85),
  else OFF falling to 0 at 4x tol.
- matching/PoseMatcher.kt — region score = 50/50 weighted mean + worst rule;
  overall = raw x (0.6+0.4 x worstRegion) x (0.4+0.6 x coverage).
- detection/MlKitPoseMapper.kt (Android) — ML Kit Pose -> PoseFrame.
- detection/LivePoseAnalyzer.kt (Android) — STREAM_MODE, keep-latest backpressure, low-light luma check.
- detection/StillImagePoseDetector.kt (Android) — accurate single-image pose + face count.

### targetpose/ (engine, tested)
- PoseTemplate.kt — PoseCategory, TemplateSource, FrameTarget, PoseTemplate(+mirrored), TemplateFactory
  (rules DERIVED by measuring the authored pose; targets never hand-typed).
- PoseBuilder.kt — forward-kinematics DSL with adult proportions.
- PoseLibrary.kt — 10 poses: neutral-standing, hands-in-pockets, one-hand-raised, crossed-arms,
  casual-side, looking-away, leaning, one-leg-forward, hand-on-waist, simple-portrait.
- ReferencePoseFactory.kt — reference photo frame -> template (Success with warnings / Failure),
  tolerances x1.2, auto UPPER_BODY if legs hidden, warns on >1 face.

### guidance/ (engine, tested)
- CorrectionEngine.kt — stateless frame analysis, ranked corrections with arrows (limb arrows live -> ghost).
- GuidanceEngine.kt — one instruction at a time, 400 ms switch delay, 700 ms no-person grace,
  ready threshold 0.85 with hysteresis (-0.07, broken by severity >= 2.0), hold 1000 ms -> countdown -> capture,
  manual photographer override (2500 ms).
- SpeechScheduler.kt — min interval 2500 ms, repeat cooldown 6000 ms, force bypass.

### photoanalysis/
- ImageMetrics.kt (tested) — Laplacian variance, exposure, gray conversion.
- PhotoQualityEvaluator.kt (tested) — Pose match, Face, Eyes, Framing, Centering, Sharpness
  (fail 40 / warn 90 at 480 px), Lighting.
- PhotoAnalyzer.kt (Android) — downscale 480 px metrics, 1280 px detection, builds report.

### camera/ audio/ gallery/ data/ (Android, not compiled)
- camera/CameraSession.kt — Preview + ImageAnalysis (~640x480) + ImageCapture, all 4:3; front mirroring.
- audio/VoiceGuide.kt — TextToSpeech wrapper.
- gallery/PhotoStore.kt — save/list in Pictures/PosePilot/ via MediaStore.
- data/SettingsRepository.kt — DataStore; update { transform }.
- data/TemplateRepository.kt — library + saved references as JSON (skeleton only, never the photo); setTransient().
- data/CaptureHolder.kt — last capture in memory (no recycling; GC frees it).
- data/AppContainer.kt — manual DI: settings, templates, captures, photos, stillDetector.

### app entry (Android)
- PosePilotApplication.kt — creates AppContainer, loads references.
- MainActivity.kt — edge-to-edge, PosePilotTheme { PosePilotNavHost(container) }.

### ui/ (Android, not compiled)
- theme/Theme.kt — monochrome palette object Pp (Ink, Paper, Graphite, Line, Smoke, Scrim, Ghost).
- components/Common.kt — TopBar, NavRow, PrimaryButton, SecondaryButton, GlassIconButton.
- components/SkeletonFigure.kt — draws NormalizedPose; lerpPose().
- PosePilotNavHost.kt — routes: home, coach?poseId={poseId}, library, reference, review, gallery, settings.
  Creates CoachViewModel(container, app, poseId), ReferenceViewModel(container, app), ReviewViewModel(container).
- home/HomeScreen.kt — animated morphing skeleton + nav rows (Coach, Reference, Library, My Photos) + settings.
- camera/CoachViewModel.kt — CoachUiState, CoachEvent (TakePhoto/OpenReview/Message), frame pipeline,
  selectTemplate, manualInstruction, captureNow, onCaptureStarted/onPhotoCaptured/onCaptureFailed,
  onResume/onPause, toggleVoice, flipCamera (clears cameraError), MANUAL_INSTRUCTIONS (14).
- camera/CameraScreen.kt — permission flow (+ open settings), preview, overlay, flash, countdown,
  low-light badge, message pill, top controls (back, pose name, mute, flip, settings),
  photographer panel, score + instruction card, bottom controls (pose thumb, shutter, photographer),
  camera error view with Switch camera.
- camera/CameraPreview.kt — AndroidView PreviewView (FILL_CENTER, COMPATIBLE), binds CameraSession,
  rejection-safe executor, releases on dispose.
- camera/GuidanceOverlay.kt — viewfinder corners, dashed ghost (solid when ready), live skeleton,
  pulsing highlight joint, animated LIMB/STEP/ROTATE arrows.
- camera/CoachPanels.kt — InstructionCard, ScoreReadout, CountdownOverlay, PhotographerPanel, MessagePill, ShutterButton.
- camera/PosePickerSheet.kt — ModalBottomSheet grid; PoseTile.
- poses/PoseLibraryScreen.kt — category chips (only non-empty), 2-col grid, delete for reference poses.

### tests — app/src/test/java/com/posepilot/app/
TestPoses.kt, AngleMathTest, NormalizationTest, FeatureExtractorTest, RuleEngineTest, PoseMatchingTest,
FrameAndCorrectionTest, ReferencePoseTest, GuidanceEngineTest, PhotoQualityTest, OverlayTransformTest.
48 tests, all pass.

Bugs already fixed during testing:
1. Arm raise measured 97.4 vs 90 -> PoseBuilder now uses per-side shoulder->hip line.
2. simple-portrait too similar to neutral -> stronger body/head turn, tilt, chin.
3. One wrong arm barely lowered score -> worst-rule + worst-region blending.
4. Breaking pose didn't cancel countdown -> hysteresis breaks on severity >= 2.0.
5. rotateToward now honours negative angles.

---------------------------------------------------------------------
## 5. REMAINING WORK (in order)

A. Screens (referenced by PosePilotNavHost — build fails without them):

 1. ui/reference/ReferenceViewModel.kt  (class ReferenceViewModel(container: AppContainer, app: Application) : AndroidViewModel(app))
    - States: Idle, Processing, Success(template, warnings, previewBitmap), Error(message).
    - process(uri): decode with ImageDecoder (auto EXIF, setAllocator(SOFTWARE), cap longest side ~1600)
      -> container.stillDetector.detect(bitmap)
      -> ReferencePoseFactory.fromFrame(frame, id = "ref-" + System.currentTimeMillis(), name, faces.size).
    - startCoaching(): container.templates.setTransient(template), return template.id.
    - save(name): container.templates.save(template.copy(name = name)).
    - Never store the photo; delete camera temp file after decoding.

 2. ui/reference/ReferenceScreen.kt  (vm, onBack, onStartCoaching: (String) -> Unit)
    - Inputs: PickVisualMedia (gallery), OpenDocument("image/*") (files),
      TakePicture(uri via FileProvider) (camera; request CAMERA permission first).
    - Show photo preview + extracted SkeletonFigure + warnings + name field,
      buttons "Start coaching" and "Save to library".

 3. ui/review/ReviewViewModel.kt  (class ReviewViewModel(container: AppContainer) : ViewModel())
    - capture = container.captures.current; template = container.templates.byId(capture.templateId).
    - analysis via PhotoAnalyzer(container.stillDetector).analyze(bitmap, template) in viewModelScope.
    - save() -> container.photos.save(bitmap); states saving/saved/error.
    - onCleared -> container.captures.clear().

 4. ui/review/ReviewScreen.kt  (vm, onRetake, onDone)
    - Photo (fit), summary, check rows (PASS / WARN / FAIL / UNAVAILABLE symbols + detail),
      Retake + Save; after save show "Saved to Pictures/PosePilot" and Done.

 5. ui/gallery/GalleryScreen.kt  (container, onBack)
    - container.photos.list(), grid thumbnails via contentResolver.loadThumbnail(uri, Size(360,360), null),
      tap -> ACTION_VIEW intent, refresh on resume.
    - Optional: request READ_MEDIA_IMAGES (33+) / READ_EXTERNAL_STORAGE (<=32) to see photos from old installs.

 6. ui/settings/SettingsScreen.kt  (container, onBack)
    - Switches: voice, skeleton, target pose, match %, auto-capture, mirror front camera, front camera default.
    - Countdown: 2 / 3 / 5 s. Instruction interval: 1500 / 2500 / 4000 ms.
    - Use container.settings.update { it.copy(...) } and collect container.settings.settings.

B. app/src/main/AndroidManifest.xml
   - uses-permission CAMERA; READ_MEDIA_IMAGES; READ_EXTERNAL_STORAGE maxSdkVersion="32"
   - uses-feature android.hardware.camera.any required="true"
   - <queries>: intent action android.intent.action.TTS_SERVICE (REQUIRED for TTS on API 30+),
     and android.media.action.IMAGE_CAPTURE
   - application: name=".PosePilotApplication", allowBackup="false", icon/roundIcon, theme
   - activity .MainActivity exported=true, MAIN/LAUNCHER, windowSoftInputMode="adjustResize"
   - provider androidx.core.content.FileProvider, authorities="${applicationId}.fileprovider",
     exported=false, grantUriPermissions=true, meta-data res/xml/file_paths

C. Resources (app/src/main/res/)
   - values/strings.xml (app_name "PosePilot")
   - values/themes.xml: Theme.PosePilot parent android:Theme.Material.NoActionBar, windowBackground black
   - xml/file_paths.xml: <cache-path name="refs" path="reference/" />
   - mipmap-anydpi/ic_launcher.xml + ic_launcher_round.xml (adaptive icon),
     drawable/ic_launcher_foreground.xml (white stick figure vector), values/colors (black background)

D. Docs: README.md (include spec's product-positioning paragraph), ARCHITECTURE.md, TODO.md

E. First Android Studio build: fix compile errors, run on device, tune thresholds
   (readyThreshold 0.85, tolerances) with real people.

---------------------------------------------------------------------
## 6. DEFERRED (post-MVP)

- Group Pose Mode (spec Mode 5): ML Kit live detector tracks one person only.
- Extra library categories/poses beyond the 10 (only non-empty categories shown).
- Mirrored-pose toggle in picker (PoseTemplate.mirrored() already exists).

---------------------------------------------------------------------
## 7. KNOWN RISKS TO CHECK IN FIRST BUILD

- Compose APIs: FlowRow (ExperimentalLayoutApi), ModalBottomSheet (ExperimentalMaterial3Api),
  deprecated VolumeUp/VolumeOff icons (warnings only).
- Overlay alignment assumes preview and analysis both 4:3 and PreviewView FILL_CENTER.
- Front camera: overlay mirrored; saved photo mirrored if mirrorFrontCamera = true.
- Unused import android.os.SystemClock in CoachViewModel.kt (harmless).
