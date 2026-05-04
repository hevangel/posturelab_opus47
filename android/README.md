# PostureLab Android

Native Android app: Jetpack Compose, CameraX (live capture), system photo picker, **on-device ML Kit Pose Detection** (no network needed). Generates the same posture report and saves it as a PDF using `android.graphics.pdf.PdfDocument`.

## Run

1. Open this folder in Android Studio (Hedgehog or newer).
2. Let Gradle sync — it pulls all dependencies from Google + Maven Central.
3. Plug in a phone (API 26+) or start an emulator with a virtual camera, then **Run app**.

## Build from CLI

```powershell
cd android
./gradlew.bat :app:assembleDebug
./gradlew.bat :app:testDebugUnitTest
```

## Architecture

```
ui/CaptureScreen.kt   → camera + gallery, captures front and side photos
ui/ReportScreen.kt    → renders the report (Compose-pixel-styled)
ui/Theme.kt           → brand colors (mirrors web `brand.ts`)
analysis/PoseAnalyzer.kt → ML Kit Pose Detection wrapper (PoseDetectionAccurate)
analysis/PostureMath.kt  → posture math (numeric parity with `shared/posture_math.py`)
report/PdfExporter.kt    → render Compose layout to letter-size PDF
```

## Why ML Kit instead of MediaPipe?

ML Kit ships a pre-quantized PoseNet model that runs entirely on-device with no extra `.task` asset, no GPU permissions, and trivial Kotlin API — ideal for an APK. The 33-keypoint output is compatible with MediaPipe indices for the points we use (ears, shoulders, hips, knees, ankles), so the math file ports verbatim from the Python reference.
