# FocusGuard Task Assignments

Hackathon goal: get a working Android demo that uses on-device ML attention detection to extend focus sessions and block selected distracting apps.

## Shared Rule

Everyone should build against the shared contracts already in `focus-guard/app/src/main/java/com/focusguard`:

- `ml/AttentionSignal.kt`
- `ml/FaceCrop.kt`
- `ml/MLPipeline.kt`
- `session/SessionState.kt`
- `session/SessionManager.kt`

Use dummy data when another person's module is not ready yet. Do not wait for upstream work if your screen/service/scorer can be tested with mocked values.

## Person 1: Gabe - Face Detection + Camera

Owner files:

- `ml/FaceDetector.kt`
- `ml/MLPipeline.kt`
- CameraX setup files/classes as needed
- `app/src/main/assets/blazeface.tflite`

Responsibilities:

- Set up CameraX with the front camera.
- Capture frames using `ImageAnalysis`.
- Target roughly 15fps and a practical resolution such as 640x480.
- Convert camera frames from `YUV_420_888` to RGB bitmap or the tensor format required by the model.
- Integrate BlazeFace through the LiteRT `CompiledModel` API.
- Prefer NPU acceleration with the required LiteRT accelerator configuration.
- Detect face bounding boxes and confidence.
- Crop the detected face region into a `FaceCrop`.
- Return `FaceDetectionResult.Detected(faceCrop)` when confidence is high enough.
- Return `FaceDetectionResult.NoFace` when no reliable face is found.
- Pass the face crop into the rest of `MLPipeline.processFrame(...)`.

Output contract:

```kotlin
FaceDetectionResult.Detected(
    FaceCrop(
        bitmap = croppedFaceBitmap,
        confidence = confidence,
        bounds = faceBounds
    )
)
```

First milestone:

- Camera preview or frame stream is working.
- `FaceDetector.detect(...)` returns a real face crop on-device.
- `MLPipeline.processFrame(...)` can emit `AttentionSignal(faceDetected = true/false)`.

## Person 2: Rayan - Head Pose + Eye Openness

Owner files:

- `ml/HeadPoseEstimator.kt`
- `ml/EyeOpenEstimator.kt`
- Any model preprocessing helpers needed for face crops
- `app/src/main/assets/whenet.tflite`
- `app/src/main/assets/eye_landmark.tflite`

Responsibilities:

- Take `FaceCrop` from Gabe's face detector.
- Resize and normalize the crop for WHENet.
- Integrate WHENet through the LiteRT `CompiledModel` API.
- Output yaw, pitch, and roll in degrees.
- Integrate the eye landmark/openness model through LiteRT.
- Compute eye aspect ratio, also called EAR.
- Treat EAR below `0.2` as eyes closed unless testing shows a better threshold.
- Package face detection, head pose, and eye openness into `AttentionSignal`.
- Call `MLPipeline.onAttentionSignal` with the final signal.

Output contract:

```kotlin
AttentionSignal(
    faceDetected = true,
    yaw = yaw,
    pitch = pitch,
    roll = roll,
    eyeAspectRatio = ear
)
```

First milestone:

- `HeadPoseEstimator.estimate(faceCrop)` returns yaw/pitch/roll.
- `EyeOpenEstimator.estimate(faceCrop)` returns EAR.
- A fake or real `FaceCrop` can produce a complete `AttentionSignal`.

## Person 3: Sanjiv - Attention Scorer + Session Manager + Accessibility Service

Owner files:

- `session/AttentionScorer.kt`
- `session/SessionManager.kt`
- `session/SessionState.kt`
- `service/FocusAccessibilityService.kt`
- `AndroidManifest.xml`
- SharedPreferences storage helpers/classes as needed

Responsibilities:

- Implement and tune attention scoring.
- Focused zone:
  - `faceDetected == true`
  - `abs(yaw) < 20`
  - `abs(pitch) < 15`
  - `eyeAspectRatio > 0.2`
- Score starts at 100.
- Focused frames recover score.
- Distracted frames decay score.
- Clamp score from 0 to 100.
- Add 2 seconds to the session for every distracted second.
- Manage active session state with `StateFlow<SessionState>`.
- Implement session start/stop behavior.
- Implement countdown timer.
- Store blacklisted app package names in SharedPreferences.
- Expose blacklist add/remove/update functions.
- Connect ML output to app state using `SessionManager.onAttentionSignal(...)`.
- Configure the Accessibility Service in the manifest and XML.
- Monitor foreground app changes with `TYPE_WINDOW_STATE_CHANGED`.
- If an active session is running and a blacklisted app opens, launch FocusGuard back to the foreground.
- Always allow emergency phone/dialer package access.
- Mark blocked app events with `SessionManager.onBlockedAppOpened(...)`.

Input contract from ML:

```kotlin
SessionManager.onAttentionSignal(signal)
```

Output contract to UI:

```kotlin
SessionManager.stateFlow
```

First milestone:

- A dummy `AttentionSignal` changes the score and timer.
- Starting/stopping a session updates `SessionState`.
- Accessibility service can detect a test package and bounce back to FocusGuard.

## Person 4: Sual - UI

Owner files:

- `ui/HomeScreen.kt`
- `ui/SessionScreen.kt`
- `ui/BounceScreen.kt`
- `MainActivity.kt`
- Navigation setup files/classes as needed

Responsibilities:

- Build the home/setup screen.
- Let the user select installed apps to block.
- Let the user choose session duration.
- Start the session through `SessionManager.startSession(...)`.
- Build the active session screen.
- Display remaining time.
- Display attention score with clear color states:
  - Green: focused/high score
  - Yellow: slipping
  - Red: distracted/low score
- Display the current distraction reason.
- Include camera preview if Gabe's CameraX surface is available in time.
- Build the bounce screen for blocked app events.
- Show the blocked app package/name, remaining time, and current score.
- Auto-return to the session screen after a short delay.
- Observe `SessionManager.stateFlow` from Compose.
- Use dummy state while Sanjiv finalizes session behavior.

Input contract:

```kotlin
SessionManager.stateFlow
```

Actions to call:

```kotlin
SessionManager.startSession(durationSeconds, selectedPackageNames)
SessionManager.stopSession()
SessionManager.clearBlockedApp()
```

First milestone:

- Home screen can start a fake session.
- Session screen reacts to `SessionState`.
- Bounce screen appears when `blockedPackageName` is not null.

## Parallel Work Plan

1. Gabe gets CameraX frames flowing and stubs BlazeFace output if needed.
2. Rayan tests head pose and eye models against a saved/static face crop while waiting for live camera.
3. Sanjiv finishes session state, scoring, storage, and accessibility bounce independently using fake package names.
4. Sual builds UI against dummy `SessionState`, then swaps to the real `SessionManager.stateFlow`.

## Demo Priority

Must work for judging:

- Start a focus session.
- Select at least one app to block.
- Show live or simulated attention score.
- Extend session time when distracted.
- Bounce back when a blocked app opens.
- Clearly mention LiteRT + NPU model path in README/demo, even if fallback stubs are used during development.

