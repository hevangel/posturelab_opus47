# AGENTS.md

Notes for AI coding agents working in this monorepo.

## Mission

Posture analysis for clinic-style reports. Two clients (web + Android) share a common analysis algorithm and brand styling. Reports mirror the layout in [reference/horace_chrio_report.pdf](reference/horace_chrio_report.pdf) but with all clinic-specific names/addresses replaced by the generic brand **PostureLab**.

## Folder map

| Folder | Stack | Tests |
| ------ | ----- | ----- |
| [ai-service/](ai-service/) | Python 3.11 + FastAPI + MediaPipe Pose Landmarker | `python tests/test_posture_math.py` |
| [web/](web/) | Vite + React 18 + TypeScript + Tailwind + jsPDF | `npm test` (vitest) |
| [android/](android/) | Kotlin 1.9 + Compose + CameraX + ML Kit Pose | `./gradlew :app:testDebugUnitTest` |
| [shared/](shared/) | Source-of-truth reference for posture math + brand tokens | – |

## Build / test commands (Windows PowerShell)

```powershell
# AI service
cd ai-service
.\.venv\Scripts\python.exe -m app.fetch_model            # one-time model download
$env:PYTHONPATH = "$PWD;$PWD\..\shared"
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8000
.\.venv\Scripts\python.exe tests\test_posture_math.py    # unit tests
.\.venv\Scripts\python.exe tests\smoke.py                # E2E smoke (server must be up)

# Web
npm --prefix web install
npm --prefix web run dev          # http://localhost:5173, proxies /api -> 8000
npm --prefix web run build

# Android  (Java from Android Studio, no system Gradle needed beyond bootstrap)
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
cd android
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug

# Whole stack via Docker
docker compose up --build
```

## Conventions you must follow

- **Brand**: do not reintroduce "Burnaby Chiropractic" or any clinic-specific name. Brand tokens live in three files that must stay in sync:
  - [shared/brand/README.md](shared/brand/README.md) (canonical)
  - [web/src/brand.ts](web/src/brand.ts)
  - [android/app/src/main/java/com/posturelab/app/ui/Theme.kt](android/app/src/main/java/com/posturelab/app/ui/Theme.kt)
- **Posture math**: [shared/posture_math.py](shared/posture_math.py) is the canonical algorithm. The Kotlin port at [android/app/src/main/java/com/posturelab/app/analysis/PostureMath.kt](android/app/src/main/java/com/posturelab/app/analysis/PostureMath.kt) must remain numerically equivalent. Unit tests on both sides pin the parity (same fixtures, same thresholds).
- **Pose landmark indices** follow MediaPipe (0=nose, 7/8=ears, 11/12=shoulders, 23/24=hips, 25/26=knees, 27/28=ankles). The Android app translates ML Kit's `PoseLandmark.*` enums to these indices in [`PoseAnalyzer.mlkitToMpIndex`](android/app/src/main/java/com/posturelab/app/analysis/PoseAnalyzer.kt).
- **Coordinates**: image-pixel space, x grows right, y grows down. The patient faces the camera in front-view, so patient-right is image-LEFT — sign flips happen inside `analyzeFront`. Don't change orientation conventions without updating both implementations and their tests.
- **Reports**: the report layout (band headers, two image columns, alternating tinted rows, disclaimer) is intentionally close to the reference PDF. Keep `Web ↔ Android ↔ PDF` text consistent — the Kotlin canonical source for the prose strings is [report/PostureReportText.kt](android/app/src/main/java/com/posturelab/app/report/PostureReportText.kt); the web mirrors them in [Report.tsx](web/src/Report.tsx).

## Pitfalls

- Python 3.13 is the only interpreter on this machine; pin `mediapipe>=0.10.30`. Older versions (`0.10.14` from the original requirements) have no 3.13 wheel.
- MediaPipe model file (`pose_landmarker.task`, ~9 MB) is not committed — fetch via `python -m app.fetch_model`. The Hugging Face mirrors are best-effort; the Google CDN fallback is the canonical source.
- Vite dev server proxies `/api` to `http://localhost:8000`. If the AI service isn't up, the browser shows network errors.
- Android Studio's JBR (Java 21) works for Gradle 8.9 + AGP 8.5 + Kotlin 1.9. **Do not** use Compose Compiler Plugin (`org.jetbrains.kotlin.plugin.compose`) — that plugin only exists for Kotlin 2.0+. Use `composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }` instead.
- The repo has no AVD installed by default; `./gradlew :app:connectedAndroidTest` will fail unless you create one. Stick to unit tests in CI.

## When extending

- **New posture metric**: add to `shared/posture_math.py` first (with a unit test in `ai-service/tests/test_posture_math.py`), then port to Kotlin (with a parity test in `android/app/src/test/java/.../PostureMathTest.kt`), then surface in both UIs.
- **Branding change**: update `shared/brand/README.md` first, then propagate to `web/src/brand.ts` and `Theme.kt`. Logo SVG lives at [shared/brand/logo.svg](shared/brand/logo.svg) and [web/public/logo.svg](web/public/logo.svg); the Android logo is drawn programmatically in [BrandLogo.kt](android/app/src/main/java/com/posturelab/app/ui/BrandLogo.kt) and [PdfExporter.kt](android/app/src/main/java/com/posturelab/app/report/PdfExporter.kt).

## Commit conventions

Every commit **must** include a chat-session extract saved under `chat_sessions/` with filename:

```
<YYYY-MM-DD>_<slug>.md
```

Where `<slug>` is a short kebab-case description of the work done in that session (e.g. `2026-05-03_initial-scaffold.md`). The file should contain:

- A summary of what was requested and what was done
- Key decisions made
- Commands that were run
- Files created or modified

This ensures full traceability of AI-authored changes.

## Reference

- Hansraj 2014 (effective head weight at forward tilt) — table embedded in `posture_math.py::effective_head_weight_lb`.
- MediaPipe Pose Landmarker model: <https://ai.google.dev/edge/mediapipe/solutions/vision/pose_landmarker>
- ML Kit Pose Detection (Android): <https://developers.google.com/ml-kit/vision/pose-detection>
