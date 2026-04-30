Here's a thorough summary you can paste:

---

**PROJECT: FocusGuard**

An Android app that uses on-device computer vision to track student attention and block distracting apps during study sessions. Built for the Qualcomm x Google LiteRT Hackathon (April 30 - May 1, 2026), running on a Samsung Galaxy S25 Ultra (Snapdragon 8 Elite with Hexagon NPU).

**Core mechanic:** User starts a study session, selects apps to block, and sets a duration. The front camera continuously monitors attention via ML inference. If the user looks away, closes their eyes, or loses face detection, their attention score decays and time is added to the session. If they try to open a blacklisted app, the Accessibility Service bounces them back immediately.

---

**TECH STACK**
- Language: Kotlin
- Camera: CameraX (ImageAnalysis use case, 15fps, YUV→RGB→float tensor)
- ML Runtime: Google LiteRT CompiledModel API with Accelerator.NPU (Hexagon)
- Models: BlazeFace (face detection), WHENet (head pose), eye landmark/openness model — all .tflite format from Qualcomm AI Hub
- App blocking: Android Accessibility Service monitoring TYPE_WINDOW_STATE_CHANGED events
- State management: StateFlow + Kotlin coroutines
- Storage: SharedPreferences for blacklisted apps

---

**ARCHITECTURE**

```
CameraX (front camera)
    ↓ raw frame (YUV→RGB bitmap)
Person 1: FaceDetector (BlazeFace via LiteRT NPU)
    ↓ FaceCrop(bitmap, confidence)
Person 2: HeadPoseEstimator (WHENet) + EyeOpenEstimator
    ↓ AttentionSignal(faceDetected, yaw, pitch, roll, eyeAspectRatio)
Person 3: AttentionScorer → SessionManager (StateFlow<SessionState>)
    ↓ SessionState(isActive, remainingSeconds, attentionScore, blacklistedApps)
Person 3: AccessibilityService (monitors foreground app, bounces blacklisted apps)
Person 4: UI (HomeScreen, SessionScreen, BounceScreen) observes StateFlow
```

---

**DIVISION OF LABOR**

**Person 1 — Camera + Face Detection**
- CameraX setup: ImageAnalysis use case, background executor, 640x480 resolution
- Frame preprocessing: YUV_420_888 → RGB Bitmap → normalized float tensor [1, H, W, 3]
- BlazeFace model integration via LiteRT CompiledModel API with NPU acceleration
- Output: FaceCrop (cropped bitmap of face region + confidence score)
- Calls Person 2's pipeline with FaceCrop every frame
- If confidence < 0.7, passes FaceCrop with faceDetected=false signal

**Person 2 — Head Pose + Eye Openness**
- Takes FaceCrop bitmap from Person 1
- WHENet integration: resize crop to 224x224, normalize, run inference → yaw, pitch, roll (floats, degrees)
- Eye model integration: facial landmark model → compute Eye Aspect Ratio (EAR = eye height / eye width). EAR < 0.2 = closed
- Packages results into AttentionSignal, calls MLPipeline.onAttentionSignal callback
- Both models run via LiteRT CompiledModel API with Accelerator.NPU

**Person 3 — Accessibility Service + Session Manager**
- AttentionScorer: consumes AttentionSignal every frame
  - Focused zone: faceDetected==true AND abs(yaw)<20° AND abs(pitch)<15° AND EAR>0.2
  - Score: Float 0-100, starts at 100
  - Focused: score += 0.5 per frame (clamp to 100)
  - Distracted: score -= 1.5 per frame (clamp to 0)
  - Every second distracted: add 2 seconds to remaining time
- SessionManager: singleton exposing StateFlow<SessionState>
  - start/stop session, countdown timer as coroutine, blacklist CRUD
- AccessibilityService:
  - Manifest: accessibilityEventTypes="typeWindowStateChanged"
  - onAccessibilityEvent: if session active AND foreground package in blacklistedApps AND package != "com.android.dialer" → launch FocusGuard via Intent
  - Communicates with SessionManager via singleton reference

**Person 4 — UI**
- HomeScreen: installed app picker (PackageManager), duration selector, Start Session button
- SessionScreen: attention score (color coded green/yellow/red), countdown timer, distraction reason label, optional camera preview
- BounceScreen: shown when blocked app intercepted, displays remaining time + score, auto-redirects after 3 seconds
- All screens observe SessionManager.stateFlow, update reactively

---

**SHARED INTERFACES (already defined, push to repo immediately)**

```kotlin
// ml/AttentionSignal.kt
data class AttentionSignal(
    val faceDetected: Boolean,
    val yaw: Float,        // degrees, negative=left, positive=right
    val pitch: Float,      // degrees, negative=down, positive=up
    val roll: Float,       // degrees, head tilt
    val eyeAspectRatio: Float  // 0.0 (closed) to ~0.4 (wide open open)
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
    val blacklistedApps: List<String>  // package names e.g. "com.instagram.android"
)
```

---

**REPO STRUCTURE**
```
focus-guard/
├── app/src/main/
│   ├── java/com/focusguard/
│   │   ├── ml/
│   │   │   ├── AttentionSignal.kt
│   │   │   ├── FaceCrop.kt
│   │   │   ├── MLPipeline.kt
│   │   │   ├── FaceDetector.kt        ← Person 1
│   │   │   ├── HeadPoseEstimator.kt   ← Person 2
│   │   │   └── EyeOpenEstimator.kt    ← Person 2
│   │   ├── session/
│   │   │   ├── SessionState.kt
│   │   │   ├── AttentionScorer.kt     ← Person 3
│   │   │   └── SessionManager.kt      ← Person 3
│   │   ├── service/
│   │   │   └── FocusAccessibilityService.kt  ← Person 3
│   │   └── ui/
│   │       ├── HomeScreen.kt          ← Person 4
│   │       ├── SessionScreen.kt       ← Person 4
│   │       └── BounceScreen.kt        ← Person 4
│   └── assets/
│       ├── blazeface.tflite
│       ├── whenet.tflite
│       └── eye_landmark.tflite
```

---

**CRITICAL PATH**
1. Person 1 gets BlazeFace running on NPU → unblocks Person 2
2. Person 2 gets AttentionSignal flowing → unblocks Person 3's scorer
3. Person 3 gets SessionState flowing → unblocks Person 4's UI wiring
4. Person 3 gets Accessibility Service bouncing apps → testable independently

Person 2, 3, 4 should all use dummy/hardcoded values of AttentionSignal and SessionState until real values are available from upstream.

---

**KEY CONSTRAINTS**
- Must use LiteRT CompiledModel API (not old Interpreter API) — judging requirement
- Must run on S25 Ultra, NPU acceleration required for judging score
- Emergency calls (com.android.dialer) always whitelisted in Accessibility Service
- All models must be .tflite format
- Repo must be public, open source licensed, with README by submission deadline (1:30pm May 1)