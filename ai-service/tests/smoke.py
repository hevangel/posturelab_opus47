"""Smoke test the AI service against a sample image."""
import io
import sys
from pathlib import Path

import requests
from PIL import Image, ImageDraw

URL = "http://127.0.0.1:8000"

ROOT = Path(__file__).resolve().parents[2]

def get_sample_image() -> bytes:
    """Use the bundled MediaPipe sample pose photo."""
    candidate = ROOT / "ai-service" / "tests" / "fixtures" / "pose.jpg"
    if candidate.exists():
        # Use a piece of the PDF photo (front view) just to exercise pose detection.
        return candidate.read_bytes()
    # Synthetic person silhouette
    img = Image.new("RGB", (480, 800), "white")
    d = ImageDraw.Draw(img)
    d.ellipse((220, 80, 260, 140), fill="black")          # head
    d.line((240, 140, 240, 420), fill="black", width=12)  # spine
    d.line((180, 200, 300, 200), fill="black", width=10)  # shoulders
    d.line((200, 420, 280, 420), fill="black", width=10)  # hips
    d.line((200, 420, 180, 660), fill="black", width=10)  # left leg
    d.line((280, 420, 300, 660), fill="black", width=10)  # right leg
    d.line((180, 200, 140, 380), fill="black", width=10)  # left arm
    d.line((300, 200, 340, 380), fill="black", width=10)  # right arm
    buf = io.BytesIO()
    img.save(buf, "JPEG")
    return buf.getvalue()


def main() -> int:
    h = requests.get(f"{URL}/health", timeout=5).json()
    print("health:", h)

    img_bytes = get_sample_image()
    files = {
        "front": ("front.jpg", img_bytes, "image/jpeg"),
        "side":  ("side.jpg",  img_bytes, "image/jpeg"),
    }
    data = {
        "patient_name": "Test User",
        "report_date": "2026-05-03",
        "patient_height_in": "68",
        "patient_weight_lb": "160",
        "base_head_weight_lb": "11",
    }
    r = requests.post(f"{URL}/analyze", files=files, data=data, timeout=60)
    print("status:", r.status_code)
    if r.status_code != 200:
        print(r.text[:1000])
        return 1
    j = r.json()
    print("front:", j["front"])
    print("side:", j["side"])
    print("front_b64 len:", len(j["annotated_front_b64"]))
    return 0


if __name__ == "__main__":
    sys.exit(main())
