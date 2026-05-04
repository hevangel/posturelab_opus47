# 2026-05-03 — Initial scaffold

## Summary

Built the entire PostureLab monorepo from scratch, starting from a reference chiropractic posture report PDF.

## What was requested

- Read the reference chiro report PDF and build a web app + Android app that replicates the posture analysis report
- AI model for pose detection (MediaPipe / ML Kit), running locally
- Replace "Burnaby Chiropractic" branding with a generic brand
- Both apps: capture front + side photos, analyze posture, display report, export PDF
- Web: support upload and webcam
- Android: use camera and gallery
- Shared posture math between web (Python server) and Android (Kotlin on-device)

## Key decisions

| Decision | Rationale |
| -------- | --------- |
| **Brand name → "PostureLab"** | Generic, professional, no clinic-specific references |
| **3-component architecture** | `ai-service/` (FastAPI + MediaPipe), `web/` (React + Vite), `android/` (Compose + ML Kit) |
| **MediaPipe Pose Landmarker** for web stack | 33-keypoint model, runs on CPU, ~9 MB, fetched from Google CDN |
| **ML Kit Pose Detection** for Android | On-device, no extra model asset, same 33-keypoint output |
| **Shared posture math** in `shared/posture_math.py` | Canonical source; Kotlin port kept in numeric parity with unit tests |
| **Kotlin 1.9 + composeOptions** | `org.jetbrains.kotlin.plugin.compose` only works on Kotlin 2.0+; used `kotlinCompilerExtensionVersion = "1.5.14"` instead |
| **mediapipe 0.10.35** | 0.10.14 has no Python 3.13 wheel; pinned to latest available |
| **Hansraj 2014 table** for effective head weight | Interpolated lookup embedded in posture math |
| **jsPDF + html2canvas** for web PDF | Renders the DOM report node to a letter-sized PDF |
| **PdfDocument + Canvas** for Android PDF | Native Android API, no third-party library |

## Commands run

```powershell
# Python env + deps
cd ai-service; py -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m app.fetch_model
.\.venv\Scripts\python.exe tests/test_posture_math.py   # 6/6 passed
.\.venv\Scripts\python.exe tests/smoke.py               # 200 OK

# Web
npm --prefix web install
npm --prefix web run build        # clean
npm --prefix web run dev          # verified in browser

# Android
gradle wrapper --gradle-version 8.9
.\gradlew.bat :app:testDebugUnitTest   # 4/4 passed
.\gradlew.bat :app:assembleDebug       # BUILD SUCCESSFUL
```

## Files created / modified

### New files

- `.gitignore`
- `README.md`, `AGENTS.md`, `CONTRIBUTING.md`, `LICENSE`
- `docker-compose.yml`
- `shared/posture_math.py` — canonical posture analysis math
- `shared/brand/README.md`, `shared/brand/logo.svg` — brand tokens + logo
- `ai-service/` — `requirements.txt`, `Dockerfile`, `app/{__init__,main,pose,fetch_model}.py`, `tests/{smoke,test_posture_math}.py`, `models/.gitkeep`
- `web/` — `package.json`, `tsconfig.json`, `vite.config.ts`, `tailwind.config.js`, `postcss.config.js`, `index.html`, `Dockerfile`, `nginx.conf`, `public/logo.svg`, `src/{main,App,PhotoSource,Report,Logo,api,brand,pdf,index.css}.tsx/ts`
- `android/` — `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `local.properties`, `gradle/wrapper/*`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/**`, `app/src/main/java/com/posturelab/app/{MainActivity,ui/{Theme,AppViewModel,CaptureScreen,ReportScreen,BrandLogo},analysis/{PostureMath,PoseAnalyzer},report/{PostureReportText,PdfExporter}}.kt`, `app/src/test/**/PostureMathTest.kt`
- `chat_sessions/2026-05-03_initial-scaffold.md` (this file)
