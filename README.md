# PostureLab

A posture analysis application inspired by clinical posture reports. Capture front and side photos of a person, run an on-device / local AI pose detection model, and produce a printable PDF posture report.

## Components

| Folder | Description |
| ------ | ----------- |
| [ai-service/](ai-service/) | FastAPI service running MediaPipe Pose Landmarker locally (CPU/CUDA) |
| [web/](web/) | React + Vite web app (upload or webcam capture) |
| [android/](android/) | Android app (Jetpack Compose, CameraX, on-device ML Kit Pose) |
| [shared/](shared/) | Shared posture math reference (Python + Kotlin parity) |
| [reference/](reference/) | Source PDF used to inform the report layout |

## Quick start (web stack)

```powershell
docker compose up --build
```

Then open <http://localhost:5173>. The AI service listens on <http://localhost:8000>.

## Quick start (Android)

Open [android/](android/) in Android Studio, plug in a device or start an emulator with API 30+, and press Run. ML Kit Pose Detection runs entirely on-device.

See [AGENTS.md](AGENTS.md) for AI-agent oriented architecture / build notes.
