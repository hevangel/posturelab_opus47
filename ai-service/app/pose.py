"""MediaPipe Pose Landmarker wrapper."""
from __future__ import annotations
import os
from functools import lru_cache
from pathlib import Path
from typing import List

import numpy as np
import mediapipe as mp
from mediapipe.tasks import python as mp_python
from mediapipe.tasks.python import vision as mp_vision

# Re-export Point from posture_math for convenience.
import sys
SHARED = Path(__file__).resolve().parent.parent.parent / "shared"
sys.path.insert(0, str(SHARED))
from posture_math import Point  # type: ignore  # noqa: E402

MODEL_PATH = Path(__file__).resolve().parent.parent / "models" / "pose_landmarker.task"


@lru_cache(maxsize=1)
def _landmarker() -> mp_vision.PoseLandmarker:
    if not MODEL_PATH.exists():
        raise FileNotFoundError(
            f"Model file missing: {MODEL_PATH}. Run `python -m app.fetch_model`."
        )
    base_opts = mp_python.BaseOptions(model_asset_path=str(MODEL_PATH))
    options = mp_vision.PoseLandmarkerOptions(
        base_options=base_opts,
        running_mode=mp_vision.RunningMode.IMAGE,
        num_poses=1,
        min_pose_detection_confidence=0.5,
        min_pose_presence_confidence=0.5,
        min_tracking_confidence=0.5,
        output_segmentation_masks=False,
    )
    return mp_vision.PoseLandmarker.create_from_options(options)


def detect(image_rgb: np.ndarray) -> List[Point]:
    """Run pose detection and return 33 landmarks in image-pixel coordinates."""
    h, w = image_rgb.shape[:2]
    mp_img = mp.Image(image_format=mp.ImageFormat.SRGB, data=image_rgb)
    res = _landmarker().detect(mp_img)
    if not res.pose_landmarks:
        raise ValueError("No person detected in image")
    pts = []
    for lm in res.pose_landmarks[0]:
        pts.append(Point(x=lm.x * w, y=lm.y * h, visibility=getattr(lm, "visibility", 1.0)))
    return pts
