# FocusGuard — Project Summary

## Overview
An Android app that uses on-device computer vision to track student attention and block distracting apps during study sessions. Built for the Qualcomm x Google LiteRT Hackathon (April 30 - May 1, 2026), running on a Samsung Galaxy S25 Ultra (Snapdragon 8 Elite with Hexagon NPU).

**Core mechanic:** User starts a study session, selects apps to block, and sets a duration. The front camera continuously monitors attention via ML inference. If the user looks away, closes their eyes, or loses face detection, their attention score decays and time is added to the session. If they try to open a blacklisted app, the Accessibility Service bounces them back immediately.

---

## Tech Stack
- **Language:** Kotlin
- **Camera:** CameraX (ImageAnalysis use case, 15fps, YUV→RGB→float tensor)
- **ML Runtime:** Google LiteRT CompiledModel API with Accelerator.NPU (Hexagon)
- **Models (confirmed from Qualcomm AI Hub):**
  - `mediapipe_face_detector.tflite` — face detection, bounding boxes + 6 facial landmarks (Person 1)
  - `mediapipe_face_landmark_detector.tflite` — 3D facial landmarks for eye openness EAR computation (Person 2)
  - Head pose model TBD — Person 2 to confirm from AI Hub search "head pose"
- **App blocking:** Android Accessibility Service monitoring TYPE_WINDOW_STATE_CHANGED
- **State management:** StateFlow + Kotlin coroutines
- **Storage:** SharedPreferences for blacklisted apps

---

## Model Decisions

### Face Detection: MediaPipe-Face-Detection (replaces BlazeFace)
BlazeFace is not available on Qualcomm AI Hub. MediaPipe-Face-Detection is the confirmed replacement:
- Explicitly supports Samsung Galaxy S25 Ultra and Snapdragon 8 Elite Mobile NPU
- Ships as two .tflite files: face_detector and face_landmark_detector
- Input resolution: 256x256
- Outputs bounding boxes + 6 facial landmarks (left eye, right eye, nose tip, mouth, left/right eye tragion)
- Sub-millisecond processing
- Apache 2.0 license
- Source: https://aihub.qualcomm.com/models/mediapipe_face

### Eye Openness
Derived from face_landmark_detector output. Compute Eye Aspect Ratio (EAR = eye height / eye width) from landmark positions. No separate eye model needed. EAR < 0.2 = eyes closed.

### Head Pose
Person 2 to confirm. Search "head pose" on aihub.qualcomm.com. WHENet is preferred if available. Fallback: derive pose from 3D facial landmarks if a dedicated model is unavailable.

---

## Architecture

```
CameraX (front camera, 15fps)
    ↓ YUV_420_888 → RGB Bitmap → normalized float tensor
Person 1: FaceDetector (mediapipe_face_detector.tflite via LiteRT NPU)
    ↓ FaceCrop(bitmap, confidence)
Person 2: HeadPoseEstimator (head pose .tflite) → yaw, pitch, roll
          EyeOpenEstimator (mediapipe_face_landmark_detector.tflite) → EAR
    ↓ AttentionSignal(faceDetected, yaw, pitch, roll, eyeAspectRatio)
Person 3: AttentionScorer → SessionManager (StateFlow<SessionState>)
    ↓ SessionState(isActive, remainingSeconds, attentionScore, blacklistedApps, blockedPackageName)
Person 3: FocusAccessibilityService (monitors foreground app, bounces blacklisted apps)
Person 4: UI (HomeScreen, SessionScreen, BounceScreen) observes StateFlow
```

---

## Shared Interfaces

```kotlin
// ml/AttentionSignal.kt
data class AttentionSignal(
    val faceDetected: Boolean,
    val yaw: Float,            // degrees, negative=left, positive=right
    val pitch: Float,          // degrees, negative=down, positive=up
    val roll: Float,           // degrees, head tilt
    val eyeAspectRatio: Float  // 0.0 (closed) to ~0.4 (wide open)
)

// ml/FaceCrop.kt
data class FaceCrop(
    val bitmap: Bitmap,
    val confidence: Float
)

// ml/MLPipeline.kt
class MLPipeline {
    var onAttentionSignal: ((AttentionSignal) -> Unit)? = null
    fun start() { /* Person 1 implements */ }
    fun stop() { /* Person 1 implements */ }
}

// session/SessionState.kt
data class SessionState(
    val isActive: Boolean,
    val remainingSeconds: Int,
    val attentionScore: Float,
    val blacklistedApps: List<String>,     // package names e.g. "com.instagram.android"
    val blockedPackageName: String? = null // non-null triggers BounceScreen
)
```

---

## Attention Scoring Logic
- **Focused zone:** faceDetected == true AND abs(yaw) < 20° AND abs(pitch) < 15° AND EAR > 0.2
- **Score:** Float 0–100, starts at 100
  - Focused frame: score += 0.5 (clamp to 100)
  - Distracted frame: score -= 1.5 (clamp to 0)
- **Time penalty:** Every second distracted adds 2 seconds to remaining session time

---

## Repo Structure

```
focus-guard/
├── app/src/main/
│   ├── java/com/focusguard/
│   │   ├── ml/
│   │   │   ├── AttentionSignal.kt
│   │   │   ├── FaceCrop.kt
│   │   │   ├── MLPipeline.kt
│   │   │   ├── FaceDetector.kt                      ← Person 1
│   │   │   ├── HeadPoseEstimator.kt                 ← Person 2
│   │   │   └── EyeOpenEstimator.kt                  ← Person 2
│   │   ├── session/
│   │   │   ├── SessionState.kt
│   │   │   ├── AttentionScorer.kt                   ← Person 3
│   │   │   └── SessionManager.kt                    ← Person 3
│   │   ├── service/
│   │   │   └── FocusAccessibilityService.kt         ← Person 3
│   │   └── ui/
│   │       ├── HomeScreen.kt                        ← Person 4
│   │       ├── SessionScreen.kt                     ← Person 4
│   │       └── BounceScreen.kt                      ← Person 4
│   └── assets/
│       ├── mediapipe_face_detector.tflite
│       ├── mediapipe_face_landmark_detector.tflite
│       └── head_pose.tflite                         ← Person 2 to confirm filename
```

---

## Critical Path
1. Person 1 gets MediaPipe face detector running on NPU → unblocks Person 2
2. Person 2 gets AttentionSignal flowing → unblocks Person 3's scorer
3. Person 3 gets SessionState flowing → unblocks Person 4's UI wiring
4. Person 3 gets Accessibility Service bouncing apps → fully testable independently right now

Everyone should use dummy/hardcoded AttentionSignal and SessionState values until real upstream values are available.

---

## Key Constraints
- Must use LiteRT CompiledModel API (not old Interpreter API) — explicit judging requirement
- Must run on S25 Ultra with NPU acceleration — scored on resource utilization
- Emergency calls (com.android.dialer) always whitelisted in Accessibility Service
- All models must be .tflite format from Qualcomm AI Hub
- Repo must be public, open source licensed, with README by submission deadline 1:30pm May 1
- Use of AI coding tools (Claude, Gemini, Copilot) explicitly permitted by organizers`