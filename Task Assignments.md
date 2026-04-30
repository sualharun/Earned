# FocusGuard — Task Assignments

Hackathon goal: get a working Android demo that uses on-device ML attention detection to extend focus sessions and block selected distracting apps.

---

## Shared Rule

Everyone builds against the shared contracts already in `focus-guard/app/src/main/java/com/focusguard`:

- `ml/AttentionSignal.kt`
- `ml/FaceCrop.kt`
- `ml/MLPipeline.kt`
- `session/SessionState.kt`
- `session/SessionManager.kt`

Use dummy/hardcoded data when upstream work is not ready. Do not wait for another person's module if your component can be tested with mocked values.

---

## Person 1: Gabe — Camera + Face Detection

**Owner files:**
- `ml/FaceDetector.kt`
- `ml/MLPipeline.kt`
- CameraX setup files as needed
- `app/src/main/assets/mediapipe_face_detector.tflite`

**Responsibilities:**
- Set up CameraX with the front-facing camera
- Capture frames via `ImageAnalysis` use case
- Target ~15fps at 640x480 resolution
- Convert `YUV_420_888` frames to RGB Bitmap, then to normalized float tensor `[1, 256, 256, 3]` (MediaPipe face detector input resolution)
- Integrate `mediapipe_face_detector.tflite` via LiteRT `CompiledModel` API with `Accelerator.NPU`
- Parse output: bounding box coordinates + confidence score
- If confidence >= 0.7: crop face region from frame into a `FaceCrop` bitmap, pass to Person 2
- If confidence < 0.7: emit `AttentionSignal(faceDetected = false, ...)` directly via `MLPipeline.onAttentionSignal`
- Call `MLPipeline.onAttentionSignal` with final signal every frame

**Output contract to Person 2:**
```kotlin
FaceCrop(
    bitmap = croppedFaceBitmap,  // cropped to bounding box
    confidence = 0.93f
)
```

**First milestone:**
- CameraX frames flowing on S25
- `FaceDetector.detect(frame)` returns a real FaceCrop on-device via NPU
- Console/log output confirms NPU delegation (not CPU fallback)

**Notes:**
- MediaPipe-Face-Detection confirmed available on Qualcomm AI Hub for Snapdragon 8 Elite
- Model page: https://aihub.qualcomm.com/models/mediapipe_face
- Two .tflite files ship together: face_detector (yours) and face_landmark_detector (Person 2's)
- Input resolution is 256x256 per model spec

---

## Person 2: Rayan — Head Pose + Eye Openness

**Owner files:**
- `ml/HeadPoseEstimator.kt`
- `ml/EyeOpenEstimator.kt`
- `app/src/main/assets/mediapipe_face_landmark_detector.tflite`
- `app/src/main/assets/head_pose.tflite` (confirm exact filename from AI Hub)

**Responsibilities:**
- Receive `FaceCrop` from Gabe's FaceDetector
- **Eye Openness (EyeOpenEstimator):**
  - Run `mediapipe_face_landmark_detector.tflite` on face crop via LiteRT CompiledModel API with NPU
  - Extract eye landmark positions from output
  - Compute Eye Aspect Ratio: EAR = eye height / eye width
  - EAR < 0.2 = eyes closed; EAR >= 0.2 = eyes open
- **Head Pose (HeadPoseEstimator):**
  - Search AI Hub for "head pose" — WHENet is preferred if available
  - Resize face crop to model's expected input size, normalize
  - Run inference via LiteRT CompiledModel API with NPU
  - Output: yaw (left/right), pitch (up/down), roll (tilt) in degrees
  - Fallback if no dedicated model found: derive approximate yaw/pitch from MediaPipe 3D landmark positions
- Package all results into `AttentionSignal` and call `MLPipeline.onAttentionSignal`

**Output contract:**
```kotlin
AttentionSignal(
    faceDetected = true,
    yaw = -12.3f,        // negative = looking left
    pitch = 5.1f,        // positive = looking up
    roll = 2.0f,
    eyeAspectRatio = 0.31f
)
```

**First milestone:**
- `EyeOpenEstimator.estimate(faceCrop)` returns a real EAR value
- `HeadPoseEstimator.estimate(faceCrop)` returns yaw/pitch/roll
- Full `AttentionSignal` emitted from a real or static face crop

**Notes:**
- `mediapipe_face_landmark_detector.tflite` is the second file from the MediaPipe-Face-Detection download — already available
- Test with a static saved bitmap of a face while waiting for Gabe's live camera output
- Confirm head pose model name and tensor shapes from AI Hub before writing preprocessing code

---

## Person 3: Sanjiv — Attention Scorer + Session Manager + Accessibility Service

**Owner files:**
- `session/AttentionScorer.kt`
- `session/SessionManager.kt`
- `session/SessionState.kt`
- `service/FocusAccessibilityService.kt`
- `AndroidManifest.xml`
- SharedPreferences helpers as needed

**Responsibilities:**

**AttentionScorer:**
- Consume `AttentionSignal` every frame
- Focused zone definition:
  - `faceDetected == true`
  - `abs(yaw) < 20`
  - `abs(pitch) < 15`
  - `eyeAspectRatio > 0.2`
- Score: Float 0–100, starts at 100
  - Focused frame: `score += 0.5` (clamp max 100)
  - Distracted frame: `score -= 1.5` (clamp min 0)
- Time penalty: every distracted second adds 2 seconds to remaining time

**SessionManager:**
- Singleton exposing `StateFlow<SessionState>`
- Functions: `startSession(durationSeconds, blacklistedApps)`, `stopSession()`
- Countdown timer as a coroutine on IO dispatcher
- Blacklist CRUD backed by SharedPreferences
- `onAttentionSignal(signal)` — feeds AttentionScorer, updates state
- `onBlockedAppOpened(packageName)` — sets `blockedPackageName` in state

**FocusAccessibilityService:**
- Extend `AccessibilityService`
- Manifest config: `accessibilityEventTypes="typeWindowStateChanged"`
- `onAccessibilityEvent`: if session active AND foreground package in blacklist AND package != `com.android.dialer` → relaunch FocusGuard via Intent
- Always allow `com.android.dialer` (emergency calls)
- Reference SessionManager singleton directly

**Input from ML:**
```kotlin
SessionManager.onAttentionSignal(signal: AttentionSignal)
```

**Output to UI:**
```kotlin
SessionManager.stateFlow  // StateFlow<SessionState>
```

**First milestone:**
- Dummy `AttentionSignal` changes score and extends timer correctly
- `startSession` / `stopSession` updates `SessionState`
- Accessibility Service detects a test package (e.g. Chrome) and bounces back to FocusGuard

**Notes:**
- Accessibility Service is fully independent of ML — start and test this immediately
- Pre-enable the Accessibility Service on the S25 before demo (Settings → Accessibility)
- Test bounce logic with Chrome or another installed app as the blocked package

---

## Person 4: Sual — UI

**Owner files:**
- `ui/HomeScreen.kt`
- `ui/SessionScreen.kt`
- `ui/BounceScreen.kt`
- `MainActivity.kt`
- Navigation setup as needed

**Responsibilities:**

**HomeScreen:**
- List installed apps via `PackageManager` with icons, let user toggle which to block
- Duration picker (e.g. 5 / 15 / 25 / 45 / 60 minutes)
- "Start Session" button → calls `SessionManager.startSession(...)`

**SessionScreen:**
- Large attention score display, color coded:
  - Green: score >= 70
  - Yellow: score >= 40
  - Red: score < 40
- Countdown timer display, visually pulses when time is being extended
- Distraction reason label: "No face detected" / "Looking away" / "Eyes closed"
- Optional: small camera preview if Gabe's CameraX surface is available

**BounceScreen:**
- Triggered when `SessionState.blockedPackageName != null`
- Show blocked app name, current score, remaining time
- "Back to studying" button → calls `SessionManager.clearBlockedApp()`
- Auto-redirects to SessionScreen after 3 seconds

**Input:**
```kotlin
SessionManager.stateFlow  // observe with collectAsState()
```

**Actions to call:**
```kotlin
SessionManager.startSession(durationSeconds, selectedPackageNames)
SessionManager.stopSession()
SessionManager.clearBlockedApp()
```

**First milestone:**
- HomeScreen starts a fake session
- SessionScreen reacts to dummy `SessionState` values
- BounceScreen appears when `blockedPackageName` is not null

**Notes:**
- Build entirely against dummy SessionState first — do not wait for Sanjiv or Gabe
- Polish matters for judges: clean UI with clear color states will score well on UX criterion
- Use Jetpack Compose for all screens

---

## Parallel Work Plan

| Person | Can start now | Blocked until |
|--------|--------------|---------------|
| Gabe (1) | CameraX setup, BlazeFace integration | Nothing — start immediately |
| Rayan (2) | Eye model on static face crop, head pose model search | Gabe's FaceCrop for live integration |
| Sanjiv (3) | Full Accessibility Service + Session Manager with dummy signals | Nothing — start immediately |
| Sual (4) | All 3 UI screens with dummy SessionState | Nothing — start immediately |

---

## Demo Script (for judges)

1. Open app → HomeScreen
2. Select Instagram / TikTok as blocked apps, set 5 minute session
3. Tap "Start Session" → SessionScreen appears, camera activates
4. Look straight at phone → score stays green, timer counts down normally
5. Look away / close eyes → score drops red, timer visibly extends
6. Try to open Instagram → immediately bounced back to FocusGuard
7. Return attention → score recovers

**Make sure to verbally call out during demo:**
- "Running entirely on-device, no network required"
- "Using LiteRT CompiledModel API with Hexagon NPU acceleration"
- "Privacy-preserving — no attention data leaves the device"