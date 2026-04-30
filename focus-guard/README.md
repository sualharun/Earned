# FocusGuard

An Android app that uses on-device ML to track user attention and block distracting apps during focus sessions.

## Project Structure

```
focus-guard/
├── app/
│   ├── src/main/
│   │   ├── java/com/focusguard/
│   │   │   ├── ml/                        # ML models & pipeline
│   │   │   │   ├── FaceDetector.kt        # Person 1
│   │   │   │   ├── HeadPoseEstimator.kt   # Person 1
│   │   │   │   ├── EyeOpenEstimator.kt    # Person 1
│   │   │   │   └── MLPipeline.kt          # Person 1
│   │   │   ├── session/                   # Session management
│   │   │   │   ├── SessionManager.kt      # Person 2
│   │   │   │   ├── AttentionScorer.kt     # Person 2
│   │   │   │   └── SessionState.kt        # Person 2
│   │   │   ├── service/                   # Android services
│   │   │   │   └── FocusAccessibilityService.kt  # Person 3
│   │   │   └── ui/                        # Compose UI screens
│   │   │       ├── HomeScreen.kt          # Person 4
│   │   │       ├── SessionScreen.kt       # Person 4
│   │   │       └── BounceScreen.kt        # Person 4
│   │   ├── assets/                        # TFLite model files
│   │   │   ├── blazeface.tflite
│   │   │   ├── whenet.tflite
│   │   │   └── eye_landmark.tflite
│   │   └── res/
├── README.md
└── build.gradle
```

## Team Assignments

| Module | Owner |
|--------|-------|
| ML pipeline (face detection, head pose, eye tracking) | Person 1 |
| Session management & attention scoring | Person 2 |
| Accessibility service (app blocking) | Person 3 |
| UI screens (Home, Session, Bounce) | Person 4 |
