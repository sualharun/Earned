# EarnedIt / FocusGuard

EarnedIt is an Android study app that uses fully on-device computer vision to keep focus sessions fair. During a session, the front camera estimates whether the user is still studying, pauses the countdown when attention is lost, and blocks selected distracting apps through an Android Accessibility Service. The experience is wrapped in a reward loop with pets, insights, and optional social features.

The project was built for the Qualcomm x Google LiteRT Hackathon on a Samsung Galaxy S25 Ultra with Snapdragon 8 Elite / Hexagon NPU acceleration.

## What It Does

- Starts timed focus sessions with a selectable app blacklist.
- Uses the front camera only while a focus session is active.
- Runs face detection, facial landmark extraction, and gaze estimation on device using Google LiteRT `CompiledModel`.
- Calibrates the user's normal study posture from the first 45 detected-face frames.
- Converts raw yaw/pitch into baseline-subtracted attention signals.
- Uses a tuned asymmetric focused-zone rule to decide whether the user is focused.
- Pauses the focus countdown when the user is distracted, missing from frame, low-confidence, or eyes closed.
- Blocks blacklisted apps during focus sessions and bounces the user back into EarnedIt.
- Allows emergency dialer/system packages and reward windows.
- Records session stats, focus score, distractions, pet progress, store purchases, privacy events, and social state locally.
- Supports optional Supabase-backed social profiles and friend requests.
- Includes debug instrumentation for benchmark reports, quality capture, frame stamping, memory, and thermal tracking.

## Core Flow

```text
CameraX front camera
    -> frame preprocess, rotation, mirroring
    -> face_detector.tflite
    -> face crop
    -> face_landmark_detector.tflite
    -> eye region, eye aspect ratio, landmark confidence
    -> eyegaze.tflite
    -> pitch/yaw in radians -> degrees
    -> 45-frame calibration baseline
    -> baseline-subtracted AttentionSignal
    -> AttentionScorer
    -> SessionManager StateFlow
    -> Compose UI + AccessibilityService enforcement
```

## ML Pipeline

The ML code lives in `app/src/main/java/com/focusguard/ml`.

The app currently ships three frozen `.tflite` assets:

- `face_detector.tflite`: MediaPipe face detector. The app resizes frames to 256x256, decodes the best anchor, and crops the detected face when confidence is at least `0.7`.
- `face_landmark_detector.tflite`: extracts 468 face landmarks from the face crop. The app uses eye landmarks to compute eye aspect ratio and locate the eye crop.
- `eyegaze.tflite`: consumes a 160x96 grayscale eye crop and outputs gaze pitch/yaw in radians, converted to degrees.

`MLPipeline` orchestrates these models and emits:

```kotlin
AttentionSignal(
    faceDetected = true,
    yaw = -12.3f,
    pitch = 5.1f,
    roll = 0f,
    eyeAspectRatio = 0.31f,
    faceConfidence = 0.93f,
    eyeConfidence = 0.88f
)
```

## Accelerator Fallback

Model execution is wrapped by `FallbackCompiledModel`, which tries accelerators in this order:

```text
NPU -> GPU -> CPU
```

This fallback applies both when a model fails to start on an accelerator and when `run(...)` fails during inference. On runtime failure, the wrapper recreates the model and buffers on the next accelerator, rewrites the input, and retries. The goal is to prefer Hexagon NPU for the hackathon demo without letting an accelerator failure break focus detection.

## Attention Scoring

The scorer lives in `app/src/main/java/com/focusguard/session/AttentionScorer.kt`.

A frame is treated as distracted when:

- no face is detected,
- face or landmark confidence is too low,
- yaw/pitch falls outside the tuned focused zone,
- eye aspect ratio is below the closed-eye threshold.

The tuned focused-zone rule is:

```kotlin
yaw in -25.0..20.0 && pitch in -15.0..18.0
```

The score ranges from `0.0` to `1.0`, starts at `1.0`, recovers by `0.075` per focused scoring update, and decays by `0.075` per distracted scoring update. `SessionManager` sends averaged attention signals once per second after smoothing over recent frames.

## Calibration

When a focus session starts, `MainActivity` enters calibration mode:

- collects 45 frames with a detected face,
- averages raw yaw and pitch,
- freezes the session timer during calibration,
- subtracts that baseline from later yaw/pitch signals.

This lets the app account for normal phone placement and the user's natural study posture instead of assuming the camera is perfectly centered.

## Fine-Tuning Process

We started with an off-the-shelf head-pose-based focus detection pipeline that produced too many false negatives: the timer paused unfairly during normal studying. We did not retrain any ML model. All three models stayed frozen; the tuning happened in the decision rule on top of model outputs.

The original rule was:

```kotlin
abs(yaw) < 20.0 && abs(pitch) < 15.0
```

On a labeled 6-minute training session, that rule produced:

| Metric | Original rule |
| --- | ---: |
| Overall agreement | 79.3% |
| Focused recall | 73.4% |
| Distraction recall | 96.6% |
| False negatives | 116 |

The failure mode was mostly pitch. About 79% of false negatives failed only the pitch threshold, with a median false-negative pitch of 17.3 degrees. That matched the real study posture: people naturally tilt downward while reading a laptop, notebook, or keyboard.

We swept asymmetric thresholds and landed on:

```kotlin
yaw in -25.0..20.0 && pitch in -15.0..18.0
```

This recovered normal downward reading posture while keeping distraction detection high.

| Metric | Old rule | Tuned rule |
| --- | ---: | ---: |
| Overall agreement | 79.3% | 88.4% |
| Focused recall | 73.4% | 86.9% |
| Distraction recall | 96.6% | 92.6% |
| False negatives | 116 | 57 |

The trade-off was 6 more missed distractions in exchange for 59 fewer unfair timer pauses, which is a strong net positive for a study app.

A held-out 3-minute validation session confirmed the change generalized:

- frame-level agreement: 84.5%,
- rolling-window agreement: 92.7%,
- clean rolling-window agreement after removing a setup-geometry confound: 93.6%,
- distraction recall for look-down and look-right: 98.4%,
- reaction time: under one captured frame.

The remaining known limitation is setup geometry: if study material is significantly offset from the phone camera, calibration can learn a non-ideal baseline. The best setup is to place the phone near the primary study target.

## Measurement And Quality Infrastructure

The instrumentation code lives in `app/src/main/java/com/focusguard/instrumentation`.

It includes:

- `BenchmarkRegistry`: named per-stage latency timers.
- `BenchmarkReporter`: JSON benchmark dumps with p50, p95, p99, mean, sample count, memory, FPS, and thermal state.
- `SystemMetricsSampler`: heap/native memory, GC pressure, frame count, FPS, and thermal throttling.
- `QualityCaptureManager`: opt-in recording mode that saves every fifth frame as JPEG plus sidecar JSON.
- `DebugInstrumentationState`: UI-driven switches for recording mode and stamped frames.

Captured sidecars include face confidence, bounding box, landmark score, raw pitch/yaw, calibration baseline, baseline-subtracted pitch/yaw, eye landmarks, focused-zone decision, session score, timestamp, and labels for offline review.

This gives the project a reusable labeled validation set, so future optimizations such as quantization, frame skipping, AOT compilation, or accelerator changes can be evaluated against ground truth instead of tuned by feel.

## Sessions And Blocking

`SessionManager` owns the focus lifecycle:

- `Idle`
- `FocusActive`
- `RewardActive`
- `Paused`
- `Failed`
- `Complete`

During `FocusActive`, it accepts `AttentionSignal`s, updates the score, tracks focused/distracted seconds, and pauses the countdown while distracted. When focus time reaches zero, the session enters a reward window.

`FocusAccessibilityService` listens for `TYPE_WINDOW_STATE_CHANGED` and `TYPE_WINDOWS_CHANGED`. If the foreground package is blacklisted during a focus session, it records the blocked attempt and relaunches EarnedIt. It ignores the app itself, Android/system UI/settings, the emergency dialer, duplicate rapid-fire events, and reward windows.

## Product Features

The UI is built with Jetpack Compose and includes:

- onboarding and pet selection,
- home dashboard with pet progress,
- session setup with duration and blocked apps,
- active session screen with camera-driven focus status,
- bounce screen after blocked app attempts,
- results and insights screens,
- pet collection, evolution, cosmetics, feeding, play, and encouragement,
- social screen with Supabase-backed profiles and friend requests when configured,
- more/settings-style feature surfaces.

Local state is managed by `EarnedItStore` and persisted with app storage. Social tables are defined in `supabase/social_schema.sql`.

## Tech Stack

- Kotlin
- Android Gradle Plugin
- Jetpack Compose + Material 3
- CameraX
- Kotlin coroutines and StateFlow
- Google LiteRT `CompiledModel`
- Android Accessibility Service
- Supabase REST API for optional social features

## Repository Map

```text
focus-guard/
  app/src/main/assets/
    face_detector.tflite
    face_landmark_detector.tflite
    eyegaze.tflite
  app/src/main/java/com/focusguard/
    MainActivity.kt
    ml/
      MLPipeline.kt
      FaceDetector.kt
      HeadPoseEstimator.kt
      FallbackCompiledModel.kt
      AttentionSignal.kt
      FaceCrop.kt
    session/
      SessionManager.kt
      AttentionScorer.kt
      SessionState.kt
      BlacklistStore.kt
    service/
      FocusAccessibilityService.kt
      AccessibilityServiceStatus.kt
    instrumentation/
      BenchmarkRegistry.kt
      BenchmarkReporter.kt
      QualityCaptureManager.kt
      SystemMetricsSampler.kt
    state/
      EarnedItState.kt
    ui/
      HomeScreen.kt
      SetupScreen.kt
      SessionScreen.kt
      BounceScreen.kt
      ResultsScreen.kt
      SocialScreen.kt
      MoreDetailScreens.kt
    pet/
      PetAssets.kt
      PetState.kt
      PetViewModel.kt
      EvolutionCutscene.kt
    social/
      SocialRepository.kt
  docs/
    accessibility-service-qa.md
  supabase/
    social_schema.sql
```

## Build

From the `focus-guard` directory:

```powershell
.\gradlew.bat :app:assembleDebug
```

For a quicker Kotlin compile check:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

## Demo Script

1. Open EarnedIt.
2. Complete onboarding or choose a pet.
3. Select a focus duration and distracting apps to block.
4. Start a focus session.
5. Keep studying normally and watch the timer count down.
6. Look away, leave frame, or close eyes and watch the session pause.
7. Try opening a blacklisted app and confirm EarnedIt bounces back.
8. Finish the focus session to enter the reward flow.

Key points to call out:

- inference runs on device,
- LiteRT uses NPU first with GPU/CPU fallback,
- camera frames and attention data do not need to leave the device,
- the focus rule was tuned from labeled captures rather than guessed.
