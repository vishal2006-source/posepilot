# PosePilot

**AI Real-Time Photography Pose Coach** — an offline Android app that turns the phone camera into a
personal pose coach.

When someone wants to take a photo, the person standing in front of the camera often does not know how to
pose. They usually search Pinterest, Instagram, Google Images, or other photos for pose references. This is
inconvenient, and a reference image does not actively guide the person while they are standing in front of
the camera. PosePilot solves that by turning the phone camera into a real-time AI pose coach: the
photographer opens the app, points the camera at the person, selects a pose or reference photo, and the
application analyses the person's body in real time and tells them exactly how to move.

The loop:

```
CAMERA -> DETECT PERSON -> UNDERSTAND POSE -> COMPARE TO TARGET -> GIVE REAL-TIME INSTRUCTIONS
       -> PERSON CORRECTS POSE -> POSE MATCHES -> CAPTURE PHOTO
```

This is not a pose gallery. The innovation is *active* guidance — "Raise your right hand", "Move slightly
left", "Turn your face slightly right", "Perfect pose", "3… 2… 1… capture" — so the subject feels like there
is a photographer inside the phone.

## What it does

- **AI Live Pose Coach** — live skeleton, ghost target pose, correction arrows, one spoken instruction at a
  time, hold detection, countdown and automatic capture.
- **Reference Photo mode** — pick any photo; PosePilot extracts the pose from it and coaches your subject
  into the same shape. Only the skeleton is saved, never the photo.
- **Pose Library** — 10 ready-made poses across casual, social, formal, portrait and travel categories.
- **Photographer mode** — a panel of 14 instructions the photographer can trigger by hand, which override
  the automatic coaching for a couple of seconds.
- **Review** — every capture gets a quality report (pose match, face, eyes, framing, centering, sharpness,
  lighting) before you decide to keep it.
- **My Photos** — everything you saved, in `Pictures/PosePilot/`.

## Privacy

Every stage — pose detection, photo analysis, speech — runs on the device. Nothing is uploaded, there is no
network permission, and captures stay in memory until you tap Save. Reference photos are never written to
storage; a photo taken for a reference is deleted from the cache as soon as it has been read.

## Build and run

Requirements: Android Studio Ladybug or newer, JDK 17, a device on Android 10 (API 29) or later. The camera
features need a physical device.

```bash
./gradlew testDebugUnitTest   # engine unit tests
./gradlew assembleDebug       # debug APK
```

| | |
|---|---|
| Language | Kotlin 2.0.21, Jetpack Compose (BOM 2024.10.01) |
| Camera | CameraX 1.4.0 (Preview + ImageAnalysis + ImageCapture, all 4:3) |
| ML | ML Kit Pose Detection 18.0.0-beta5 (fast for live, accurate for stills), Face Detection 16.1.7 |
| Storage | DataStore for settings, JSON files for reference skeletons, MediaStore for photos |
| Min / target SDK | 29 / 35 |

## Documentation

- `ARCHITECTURE.md` — how a camera frame becomes an instruction.
- `TODO.md` — what is left, in order.
- `HANDOFF.md` — full file-by-file inventory and the decisions behind it.
- `docs/spec.pdf` — the original product specification.
