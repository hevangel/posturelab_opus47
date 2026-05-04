"""Unit tests for posture math."""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "shared"))
from posture_math import (  # type: ignore
    Point, analyze_front, analyze_side, effective_head_weight_lb, tilt_deg,
)


def make_normal_front() -> list[Point]:
    """Build a perfectly aligned standing person facing the camera."""
    # Indices we use: ears (7,8), shoulders (11,12), hips (23,24), knees (25,26), ankles (27,28)
    pts = [Point(0, 0) for _ in range(33)]
    cx = 500
    # y grows down; place ears highest, ankles lowest
    pts[7]  = Point(cx + 30, 100)   # left ear  (image right)
    pts[8]  = Point(cx - 30, 100)   # right ear (image left)
    pts[11] = Point(cx + 80, 250)   # left shoulder
    pts[12] = Point(cx - 80, 250)   # right shoulder
    pts[23] = Point(cx + 60, 600)   # left hip
    pts[24] = Point(cx - 60, 600)   # right hip
    pts[25] = Point(cx + 60, 900)
    pts[26] = Point(cx - 60, 900)
    pts[27] = Point(cx + 60, 1200)
    pts[28] = Point(cx - 60, 1200)
    pts[0]  = Point(cx, 90)
    return pts


def test_normal_posture_front_close_to_zero():
    f = analyze_front(make_normal_front(), patient_height_in=68.0)
    assert abs(f.head_shift_in) < 0.05
    assert abs(f.head_tilt_deg) < 0.5
    assert abs(f.shoulders_shift_in) < 0.05
    assert abs(f.shoulders_tilt_deg) < 0.5
    assert abs(f.hips_shift_in) < 0.05
    assert abs(f.hips_tilt_deg) < 0.5
    assert f.total_tilt_deg < 1.0


def test_head_shifted_right_reads_positive():
    pts = make_normal_front()
    # Shift both ears 50 px to image-LEFT (== patient's right)
    pts[7] = Point(pts[7].x - 50, pts[7].y)
    pts[8] = Point(pts[8].x - 50, pts[8].y)
    f = analyze_front(pts, patient_height_in=68.0)
    assert f.head_shift_in > 0.5, f"expected positive (right), got {f.head_shift_in}"


def test_tilt_independent_of_left_right_order():
    a = Point(0, 100)
    b = Point(100, 90)  # b is higher (smaller y)
    t1 = tilt_deg(a, b)
    t2 = tilt_deg(b, a)
    # Both should report the same magnitude / sign sign-convention is "b higher → positive";
    # swapping inverts sign.
    assert abs(t1 + t2) < 1e-6
    assert abs(t1) < 90 and abs(t2) < 90


def test_hansraj_curve_monotonic():
    base = effective_head_weight_lb(0)
    assert abs(base - 13) < 1e-6
    prev = base
    for d in range(5, 65, 5):
        v = effective_head_weight_lb(d)
        assert v >= prev - 1e-6
        prev = v


def make_normal_side(shift_x_for_head: float = 0.0) -> list[Point]:
    """Standing person photographed from the right side (facing image-right)."""
    pts = [Point(0, 0) for _ in range(33)]
    base_x = 500
    # Use right-side landmarks (visibility = 1 for right side, 0 for left)
    def L(p): p.visibility = 0.1; return p
    def R(p): p.visibility = 0.9; return p
    pts[8]  = R(Point(base_x, 100))   # right ear
    pts[12] = R(Point(base_x, 250))
    pts[24] = R(Point(base_x, 600))
    pts[26] = R(Point(base_x, 900))
    pts[28] = R(Point(base_x, 1200))
    pts[7]  = L(Point(base_x, 100))
    pts[11] = L(Point(base_x, 250))
    pts[23] = L(Point(base_x, 600))
    pts[25] = L(Point(base_x, 900))
    pts[27] = L(Point(base_x, 1200))
    # Nose to the right of shoulder => facing right
    pts[0]  = Point(base_x + 60, 100)
    if shift_x_for_head:
        pts[8] = Point(pts[8].x + shift_x_for_head, pts[8].y)
        pts[0] = Point(pts[0].x + shift_x_for_head, pts[0].y)
    return pts


def test_normal_side_close_to_zero():
    s = analyze_side(make_normal_side(), patient_height_in=68.0)
    assert abs(s.head_forward_in) < 0.2
    assert abs(s.shoulder_shift_in) < 0.2
    assert s.effective_head_weight_lb == s.base_head_weight_lb  # 0° => 11 -> 11


def test_forward_head_increases_effective_weight():
    s = analyze_side(make_normal_side(shift_x_for_head=80), patient_height_in=68.0)
    assert s.head_forward_in > 1.0
    assert s.head_off_vertical_deg > 5.0
    assert s.effective_head_weight_lb > s.base_head_weight_lb


if __name__ == "__main__":
    import traceback
    funcs = [v for k, v in globals().items() if k.startswith("test_") and callable(v)]
    failed = 0
    for fn in funcs:
        try:
            fn()
            print(f"PASS {fn.__name__}")
        except Exception:
            failed += 1
            print(f"FAIL {fn.__name__}")
            traceback.print_exc()
    print(f"\n{len(funcs) - failed}/{len(funcs)} passed")
    sys.exit(0 if failed == 0 else 1)
