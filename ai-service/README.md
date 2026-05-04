# PostureLab AI Service

FastAPI service that wraps Google's [MediaPipe Pose Landmarker](https://ai.google.dev/edge/mediapipe/solutions/vision/pose_landmarker) (33-keypoint model) and exposes a single `/analyze` endpoint.

The model file is fetched on first run (preferring the [`qualcomm/MediaPipe-Pose-Estimation`](https://huggingface.co/qualcomm/MediaPipe-Pose-Estimation) Hugging Face mirror, falling back to Google's CDN). Once cached at `models/pose_landmarker.task` the service runs fully offline.

## Local dev

```powershell
cd ai-service
py -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python -m app.fetch_model
$env:PYTHONPATH = "$PWD;$PWD\..\shared"
uvicorn app.main:app --reload --port 8000
```

## Endpoint

`POST /analyze` — multipart form with fields:

| field | type | description |
| ----- | ---- | ----------- |
| `front` | file | front-view photo |
| `side`  | file | side-view photo |
| `patient_name` | str | display name |
| `report_date`  | str | ISO date |
| `patient_height_in` | float | total standing height in inches |
| `patient_weight_lb` | float | body weight in pounds |
| `base_head_weight_lb` | float | nominal head weight (default 11 lb) |

Returns JSON with `front`, `side` metric dicts and base64 annotated JPEGs.
