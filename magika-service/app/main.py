import base64
import os
import time
from contextlib import asynccontextmanager
from typing import Any

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse
from magika import Magika
from pydantic import BaseModel, Field

PORT = int(os.environ.get("MAGIKA_PORT", "8090"))
MAX_BYTES = int(os.environ.get("MAGIKA_MAX_BYTES", "4096"))

PNG_SIGNATURE = bytes([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A])


def _warmup_sample() -> bytes:
    sample = bytearray(max(MAX_BYTES, len(PNG_SIGNATURE)))
    sample[: len(PNG_SIGNATURE)] = PNG_SIGNATURE
    return bytes(sample)


_engine: Magika | None = None


@asynccontextmanager
async def lifespan(_app: FastAPI):
    global _engine
    print("[magika-service] loading Magika engine...")
    started = time.perf_counter()
    _engine = Magika()
    print(f"[magika-service] engine ready ({(time.perf_counter() - started) * 1000:.0f}ms)")

    print("[magika-service] inference warm-up...")
    warm_started = time.perf_counter()
    _engine.identify_bytes(_warmup_sample())
    print(
        f"[magika-service] warm-up done ({(time.perf_counter() - warm_started) * 1000:.0f}ms)"
    )
    yield
    _engine = None


app = FastAPI(title="magika-service", version="0.1.0", lifespan=lifespan)


class IdentifyJsonBody(BaseModel):
    data: str = Field(description="Base64-encoded file head bytes")


def _identify(buffer: bytes) -> dict[str, Any]:
    if _engine is None:
        raise HTTPException(status_code=503, detail="Magika engine not initialized")
    if len(buffer) == 0:
        raise HTTPException(status_code=400, detail="empty payload")
    if len(buffer) > MAX_BYTES:
        raise HTTPException(
            status_code=400,
            detail=f"payload exceeds MAGIKA_MAX_BYTES ({MAX_BYTES})",
        )

    result = _engine.identify_bytes(buffer)
    output = result.output
    return {
        "label": output.label,
        "mime": output.mime_type,
        "score": result.score,
        "description": output.description,
        "isText": output.is_text,
    }


@app.get("/health")
def health() -> dict[str, Any]:
    return {
        "status": "ok" if _engine is not None else "starting",
        "maxBytes": MAX_BYTES,
    }


@app.post("/identify")
async def identify(request: Request) -> JSONResponse:
    content_type = (request.headers.get("content-type") or "").split(";")[0].strip().lower()

    if content_type == "application/json":
        body = IdentifyJsonBody.model_validate(await request.json())
        try:
            buffer = base64.b64decode(body.data, validate=True)
        except Exception as exc:
            raise HTTPException(status_code=400, detail="invalid base64") from exc
    else:
        buffer = await request.body()

    return JSONResponse(_identify(buffer))
