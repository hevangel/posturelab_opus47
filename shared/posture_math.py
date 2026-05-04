"""Reference posture analysis math.

This module is the canonical source of truth for posture metric formulas.
The Kotlin implementation in `android/app/src/main/java/com/posturelab/app/analysis/PostureMath.kt`
is kept in numeric parity with this file.
"""
from __future__ import annotations
from dataclasses import dataclass, asdict
from math import atan2, degrees, hypot, sqrt
from typing import Optional


# MediaPipe Pose 33-landmark indices we care about.
NOSE = 0
LEFT_EAR = 7
RIGHT_EAR = 8
LEFT_SHOULDER = 11
RIGHT_SHOULDER = 12
LEFT_HIP = 23
RIGHT_HIP = 24
LEFT_KNEE = 25
RIGHT_KNEE = 26
LEFT_ANKLE = 27
RIGHT_ANKLE = 28


@dataclass
class Point:
    x: float  # pixels (image-space, x grows right)
    y: float  # pixels (image-space, y grows down)
    visibility: float = 1.0


def midpoint(a: Point, b: Point) -> Point:
    return Point((a.x + b.x) / 2.0, (a.y + b.y) / 2.0,
                 min(a.visibility, b.visibility))


def tilt_deg(a: Point, b: Point) -> float:
    """Signed tilt of segment a->b from horizontal in degrees, in (-90, 90].
    Positive = b is higher (smaller image-y) than a.
    Independent of whether a is to the left or right of b."""
    dx = b.x - a.x
    dy = b.y - a.y
    # Reduce to acute angle from horizontal regardless of left/right ordering.
    # Use arctan(dy / |dx|) so the segment is treated as horizontal-ish.
    if abs(dx) < 1e-6:
        return 0.0
    return degrees(atan2(-dy, abs(dx)))


def angle_from_vertical(a: Point, b: Point) -> float:
    """Absolute angle between vector a->b and the vertical axis (deg)."""
    dx = b.x - a.x
    dy = b.y - a.y
    return abs(degrees(atan2(dx, -dy)))


# -- Calibration ---------------------------------------------------------

def pixels_per_inch(landmarks: list[Point], patient_height_inches: float) -> float:
    """Estimate scale: distance from mid-ear to mid-ankle in pixels."""
    head = midpoint(landmarks[LEFT_EAR], landmarks[RIGHT_EAR])
    ankle = midpoint(landmarks[LEFT_ANKLE], landmarks[RIGHT_ANKLE])
    px = hypot(head.x - ankle.x, head.y - ankle.y)
    # Approx: ear-to-ankle ~= 92% of total standing height
    inches = patient_height_inches * 0.92
    return px / max(inches, 1e-3)


# -- Effective head weight (Hansraj-style table) -------------------------

_HANSRAJ = [(0, 13), (15, 27), (30, 40), (45, 49), (60, 60)]


def effective_head_weight_lb(forward_tilt_deg: float) -> float:
    """Interpolate Hansraj 2014 effective head-weight table."""
    a = abs(forward_tilt_deg)
    if a <= _HANSRAJ[0][0]:
        return _HANSRAJ[0][1]
    if a >= _HANSRAJ[-1][0]:
        return _HANSRAJ[-1][1]
    for (x0, y0), (x1, y1) in zip(_HANSRAJ, _HANSRAJ[1:]):
        if x0 <= a <= x1:
            t = (a - x0) / (x1 - x0)
            return y0 + t * (y1 - y0)
    return 13.0


# -- Front view ----------------------------------------------------------

@dataclass
class FrontMetrics:
    head_shift_in: float          # +right / -left
    head_tilt_deg: float          # +right-tilted (right ear lower)
    shoulders_shift_in: float
    shoulders_tilt_deg: float
    ribcage_shift_in: float
    hips_shift_in: float
    hips_tilt_deg: float
    q_angle_left_deg: float
    q_angle_right_deg: float
    total_shift_in: float
    total_tilt_deg: float


def analyze_front(lms: list[Point], patient_height_in: float) -> FrontMetrics:
    ppi = pixels_per_inch(lms, patient_height_in)

    mid_ears = midpoint(lms[LEFT_EAR], lms[RIGHT_EAR])
    mid_shoulders = midpoint(lms[LEFT_SHOULDER], lms[RIGHT_SHOULDER])
    mid_hips = midpoint(lms[LEFT_HIP], lms[RIGHT_HIP])
    mid_ankles = midpoint(lms[LEFT_ANKLE], lms[RIGHT_ANKLE])
    mid_torso = midpoint(mid_shoulders, mid_hips)

    # In image: x grows right. Patient faces camera so patient-right == image-left.
    # We report from the patient's perspective: shift right in report ==
    # patient's right side, which is image-left. Negate x deltas.
    def shift_in(top: Point, base: Point) -> float:
        return -(top.x - base.x) / ppi

    head_shift = shift_in(mid_ears, mid_ankles)
    sh_shift = shift_in(mid_shoulders, mid_ankles)
    rib_shift = shift_in(mid_torso, mid_ankles)
    hips_shift = shift_in(mid_hips, mid_ankles)

    # Tilt: positive when right side (patient's right == image left) is lower.
    # tilt_deg(a=left, b=right) returns angle from horizontal where positive means b higher.
    # We invert to report "right side lower = positive".
    def tilt(left_pt: Point, right_pt: Point) -> float:
        # Patient's left is image right, patient's right is image left.
        return -tilt_deg(lms[LEFT_EAR], lms[RIGHT_EAR]) if False else 0.0  # placeholder

    head_tilt = -tilt_deg(lms[LEFT_EAR], lms[RIGHT_EAR])
    sh_tilt = -tilt_deg(lms[LEFT_SHOULDER], lms[RIGHT_SHOULDER])
    hips_tilt = -tilt_deg(lms[LEFT_HIP], lms[RIGHT_HIP])

    def q_angle(hip: Point, knee: Point, ankle: Point) -> float:
        """Frontal-plane Q-angle approximation: deviation of knee from
        the hip->ankle line, in degrees."""
        # vector hip->ankle
        vx, vy = ankle.x - hip.x, ankle.y - hip.y
        L = hypot(vx, vy) or 1.0
        # perpendicular distance of knee from the line
        # cross product magnitude / L
        d = abs((knee.x - hip.x) * vy - (knee.y - hip.y) * vx) / L
        # convert to deg
        return degrees(atan2(d, max(L / 2, 1.0)))

    qL = q_angle(lms[LEFT_HIP], lms[LEFT_KNEE], lms[LEFT_ANKLE])
    qR = q_angle(lms[RIGHT_HIP], lms[RIGHT_KNEE], lms[RIGHT_ANKLE])

    total_shift = abs(head_shift) + abs(sh_shift) + abs(rib_shift) + abs(hips_shift)
    total_tilt = abs(head_tilt) + abs(sh_tilt) + abs(hips_tilt)

    return FrontMetrics(
        head_shift_in=head_shift,
        head_tilt_deg=head_tilt,
        shoulders_shift_in=sh_shift,
        shoulders_tilt_deg=sh_tilt,
        ribcage_shift_in=rib_shift,
        hips_shift_in=hips_shift,
        hips_tilt_deg=hips_tilt,
        q_angle_left_deg=qL,
        q_angle_right_deg=qR,
        total_shift_in=total_shift,
        total_tilt_deg=total_tilt,
    )


# -- Side view -----------------------------------------------------------

@dataclass
class SideMetrics:
    head_forward_in: float
    head_off_vertical_deg: float
    base_head_weight_lb: float
    effective_head_weight_lb: float
    shoulder_shift_in: float          # backward(+) / forward(-)
    shoulder_off_vertical_deg: float
    hip_shift_in: float
    hip_off_vertical_deg: float
    knee_shift_in: float
    head_vs_ankle_in: float
    head_vs_ankle_flex_deg: float
    total_shift_in: float
    total_tilt_deg: float


def _pick_side(lms: list[Point]) -> tuple[int, int, int, int, int]:
    """Pick the side facing the camera (more visible): returns indices for
    (ear, shoulder, hip, knee, ankle)."""
    left_score = (lms[LEFT_EAR].visibility + lms[LEFT_SHOULDER].visibility +
                  lms[LEFT_HIP].visibility + lms[LEFT_ANKLE].visibility)
    right_score = (lms[RIGHT_EAR].visibility + lms[RIGHT_SHOULDER].visibility +
                   lms[RIGHT_HIP].visibility + lms[RIGHT_ANKLE].visibility)
    if right_score >= left_score:
        return RIGHT_EAR, RIGHT_SHOULDER, RIGHT_HIP, RIGHT_KNEE, RIGHT_ANKLE
    return LEFT_EAR, LEFT_SHOULDER, LEFT_HIP, LEFT_KNEE, LEFT_ANKLE


def analyze_side(lms: list[Point], patient_height_in: float,
                 base_head_weight_lb: float = 11.0) -> SideMetrics:
    ppi = pixels_per_inch(lms, patient_height_in)
    ear_i, sh_i, hip_i, knee_i, ankle_i = _pick_side(lms)
    ear, sh, hip, knee, ankle = (lms[ear_i], lms[sh_i], lms[hip_i],
                                 lms[knee_i], lms[ankle_i])

    # Determine forward direction. We assume patient is facing image-right
    # if shoulder.x > hip.x; otherwise image-left. We define forward(+) as
    # patient-forward (the direction the nose points).
    facing_right = (lms[NOSE].x >= sh.x)
    sgn = 1.0 if facing_right else -1.0

    def fwd_in(p: Point, base: Point) -> float:
        return sgn * (p.x - base.x) / ppi

    head_forward = fwd_in(ear, ankle)
    head_off_vert = angle_from_vertical(sh, ear)
    if not facing_right:
        head_off_vert = head_off_vert  # always positive magnitude

    sh_shift = fwd_in(sh, ankle)
    sh_off_vert = angle_from_vertical(hip, sh)
    hip_shift = fwd_in(hip, ankle)
    hip_off_vert = angle_from_vertical(ankle, hip)
    knee_shift = fwd_in(knee, ankle)

    head_vs_ankle = fwd_in(ear, ankle)
    head_vs_ankle_flex = angle_from_vertical(ankle, ear)

    eff_w = base_head_weight_lb / 13.0 * effective_head_weight_lb(head_off_vert)

    total_shift = abs(head_forward) + abs(sh_shift) + abs(hip_shift) + abs(knee_shift)
    total_tilt = head_off_vert + sh_off_vert + hip_off_vert

    return SideMetrics(
        head_forward_in=head_forward,
        head_off_vertical_deg=head_off_vert,
        base_head_weight_lb=base_head_weight_lb,
        effective_head_weight_lb=eff_w,
        shoulder_shift_in=-sh_shift,  # report uses backward(+)
        shoulder_off_vertical_deg=sh_off_vert,
        hip_shift_in=hip_shift,
        hip_off_vertical_deg=hip_off_vert,
        knee_shift_in=knee_shift,
        head_vs_ankle_in=head_vs_ankle,
        head_vs_ankle_flex_deg=head_vs_ankle_flex,
        total_shift_in=total_shift,
        total_tilt_deg=total_tilt,
    )


def to_dict(obj) -> dict:
    return asdict(obj)
