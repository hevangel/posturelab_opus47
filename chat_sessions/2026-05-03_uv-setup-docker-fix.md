# 2026-05-03 — uv setup and Docker fix

## Summary

- Set up `uv` as the Python package manager for `ai-service/`
- Created `pyproject.toml` and generated `uv.lock`
- Updated `AGENTS.md` to mandate `uv run python` for all Python commands
- Updated `ai-service/Dockerfile` to use uv (multi-stage copy from `ghcr.io/astral-sh/uv`)
- Fixed Docker build: added `libgles2` and `libegl1` for MediaPipe's OpenGL ES dependency
- Updated `docker-compose.yml` healthcheck to use `uv run python`
- Verified Docker and native produce identical analysis results (sub-mm parity)
- Ran web app end-to-end with example images (front + side view), confirmed analysis and PDF generation work

## Key decisions

- Used `dependency-groups.dev` (PEP 735) instead of deprecated `tool.uv.dev-dependencies`
- Kept `requirements.txt` for reference but `pyproject.toml` + `uv.lock` are now the source of truth
- Docker image uses `--frozen` flag to ensure reproducible installs from lockfile

## Commands run

```powershell
cd ai-service
uv sync
uv run python -c "import fastapi, mediapipe, cv2, numpy; print('OK')"
docker compose build ai
docker run --rm -d --name posturelab-ai-test -p 8001:8000 chrio_opus47-ai:latest
python tests/test_docker_parity.py  # PASS
```

## Files created or modified

- `ai-service/pyproject.toml` (new)
- `ai-service/uv.lock` (new, generated)
- `ai-service/Dockerfile` (updated to use uv + added GL libs)
- `ai-service/tests/test_docker_parity.py` (new)
- `docker-compose.yml` (healthcheck updated)
- `AGENTS.md` (uv instructions added)
- `.gitignore` (added .idea/, models/.hf-cache/)
- `example/posturelab-Patient-2026-05-04.pdf` (example report)
- `example/annotated_front.jpg`, `example/annotated_side.jpg` (annotated output)
- `example/screenshot_web_app.png` (web app screenshot)
