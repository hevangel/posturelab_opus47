"""FastAPI server: posture analysis endpoint."""
from __future__ import annotations
import base64
import io
import sys
from pathlib import Path
from typing import Optional

import numpy as np
from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image
from pydantic import BaseModel

SHARED = Path(__file__).resolve().parent.parent.parent / "shared"
sys.path.insert(0, str(SHARED))
from posture_math import (  # type: ignore  # noqa: E402
    analyze_front, analyze_side, to_dict,
)

from .pose import detect  # noqa: E402

app = FastAPI(title="PostureLab AI Service", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


def _decode(file: UploadFile) -> np.ndarray:
    raw = file.file.read()
    if not raw:
        raise HTTPException(400, f"Empty upload: {file.filename}")
    img = Image.open(io.BytesIO(raw)).convert("RGB")
    return np.asarray(img)


def _b64(img: np.ndarray) -> str:
    pil = Image.fromarray(img)
    buf = io.BytesIO()
    pil.save(buf, format="JPEG", quality=85)
    return base64.b64encode(buf.getvalue()).decode("ascii")


def _annotate(img: np.ndarray, lms) -> np.ndarray:
    """Draw skeleton + plumb lines on a copy of the image."""
    import cv2
    out = img.copy()
    h, w = out.shape[:2]
    # Plumb line through ankle midpoint
    from posture_math import (  # type: ignore
        LEFT_ANKLE, RIGHT_ANKLE, LEFT_EAR, RIGHT_EAR,
        LEFT_SHOULDER, RIGHT_SHOULDER, LEFT_HIP, RIGHT_HIP,
        LEFT_KNEE, RIGHT_KNEE, midpoint,
    )
    ank = midpoint(lms[LEFT_ANKLE], lms[RIGHT_ANKLE])
    ear = midpoint(lms[LEFT_EAR], lms[RIGHT_EAR])
    sh = midpoint(lms[LEFT_SHOULDER], lms[RIGHT_SHOULDER])
    hip = midpoint(lms[LEFT_HIP], lms[RIGHT_HIP])
    # vertical reference line (green)
    cv2.line(out, (int(ank.x), 0), (int(ank.x), h), (40, 200, 80), 2)
    # body chain (red)
    chain = [ear, sh, hip, ank]
    for a, b in zip(chain, chain[1:]):
        cv2.line(out, (int(a.x), int(a.y)), (int(b.x), int(b.y)), (220, 60, 60), 3)
    # horizontal reference rails at shoulders & hips (blue)
    for pair in [(LEFT_SHOULDER, RIGHT_SHOULDER), (LEFT_HIP, RIGHT_HIP),
                 (LEFT_EAR, RIGHT_EAR), (LEFT_KNEE, RIGHT_KNEE)]:
        a, b = lms[pair[0]], lms[pair[1]]
        cv2.line(out, (int(a.x), int(a.y)), (int(b.x), int(b.y)), (60, 120, 220), 2)
    # landmark dots
    for p in [ear, sh, hip, ank, lms[LEFT_KNEE], lms[RIGHT_KNEE]]:
        cv2.circle(out, (int(p.x), int(p.y)), 5, (255, 220, 40), -1)
    return out


class AnalyzeResponse(BaseModel):
    front: dict
    side: dict
    annotated_front_b64: str
    annotated_side_b64: str
    patient_name: str
    report_date: str
    patient_height_in: float
    patient_weight_lb: float
    base_head_weight_lb: float


@app.get("/health")
def health() -> dict:
    return {"ok": True, "service": "posturelab-ai"}


@app.post("/analyze", response_model=AnalyzeResponse)
async def analyze(
    front: UploadFile = File(...),
    side: UploadFile = File(...),
    patient_name: str = Form("Patient"),
    report_date: str = Form(""),
    patient_height_in: float = Form(68.0),
    patient_weight_lb: float = Form(160.0),
    base_head_weight_lb: float = Form(11.0),
) -> AnalyzeResponse:
    if patient_height_in <= 0:
        raise HTTPException(400, "patient_height_in must be positive")

    front_img = _decode(front)
    side_img = _decode(side)

    try:
        front_lms = detect(front_img)
    except ValueError as e:
        raise HTTPException(422, f"front: {e}")
    try:
        side_lms = detect(side_img)
    except ValueError as e:
        raise HTTPException(422, f"side: {e}")

    front_metrics = analyze_front(front_lms, patient_height_in)
    side_metrics = analyze_side(side_lms, patient_height_in, base_head_weight_lb)

    return AnalyzeResponse(
        front=to_dict(front_metrics),
        side=to_dict(side_metrics),
        annotated_front_b64=_b64(_annotate(front_img, front_lms)),
        annotated_side_b64=_b64(_annotate(side_img, side_lms)),
        patient_name=patient_name,
        report_date=report_date,
        patient_height_in=patient_height_in,
        patient_weight_lb=patient_weight_lb,
        base_head_weight_lb=base_head_weight_lb,
    )
