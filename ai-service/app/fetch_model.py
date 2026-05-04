"""Download the MediaPipe pose landmarker model file.

Tries Hugging Face first (qualcomm/MediaPipe-Pose-Estimation mirror), then
falls back to Google's CDN. The resulting file is placed at
`ai-service/models/pose_landmarker.task`.
"""
from __future__ import annotations
import os
import sys
import urllib.request
from pathlib import Path

MODEL_NAME = "pose_landmarker_full.task"
DEST = Path(__file__).resolve().parent.parent / "models" / "pose_landmarker.task"

GOOGLE_URL = (
    "https://storage.googleapis.com/mediapipe-models/pose_landmarker/"
    "pose_landmarker_full/float16/latest/pose_landmarker_full.task"
)


def _try_huggingface() -> Path | None:
    try:
        from huggingface_hub import hf_hub_download
    except ImportError:
        return None
    candidates = [
        ("qualcomm/MediaPipe-Pose-Estimation", "MediaPipePoseLandmarkDetector.task"),
        ("ahsen-aliyev/mediapipe-pose-landmarker", "pose_landmarker_full.task"),
    ]
    for repo, fname in candidates:
        try:
            print(f"[fetch_model] trying HuggingFace: {repo}/{fname}", flush=True)
            p = hf_hub_download(repo_id=repo, filename=fname,
                                cache_dir=str(DEST.parent / ".hf-cache"))
            return Path(p)
        except Exception as e:
            print(f"[fetch_model] HF miss {repo}: {e}", flush=True)
    return None


def main() -> int:
    DEST.parent.mkdir(parents=True, exist_ok=True)
    if DEST.exists() and DEST.stat().st_size > 1_000_000:
        print(f"[fetch_model] already present: {DEST}")
        return 0
    src = _try_huggingface()
    if src is not None:
        DEST.write_bytes(src.read_bytes())
        print(f"[fetch_model] copied from HF cache -> {DEST}")
        return 0
    print(f"[fetch_model] falling back to {GOOGLE_URL}", flush=True)
    with urllib.request.urlopen(GOOGLE_URL) as resp, open(DEST, "wb") as f:
        f.write(resp.read())
    print(f"[fetch_model] downloaded {DEST.stat().st_size} bytes -> {DEST}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
