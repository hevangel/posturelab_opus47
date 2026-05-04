FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1

RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 libglib2.0-0 libgles2 libegl1 libgomp1 ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Install uv
COPY --from=ghcr.io/astral-sh/uv:latest /uv /usr/local/bin/uv

WORKDIR /app
COPY ai-service/pyproject.toml ai-service/uv.lock /app/
RUN uv sync --frozen --no-dev

COPY ai-service/app /app/app
COPY shared /shared

# Pre-fetch the model at build time so the container is fully offline-capable.
RUN uv run python -m app.fetch_model

ENV PYTHONPATH=/app:/shared
EXPOSE 8000
CMD ["uv", "run", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
