from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
from groq import Groq
from dotenv import load_dotenv

import os
import re
import traceback
from pathlib import Path


# =========================
# LOAD ENV
# =========================

BASE_DIR = Path(__file__).resolve().parent
ENV_PATH = BASE_DIR.parent / ".env"

load_dotenv(dotenv_path=ENV_PATH)

print("ENV FILE:", ENV_PATH)
print("ENV EXISTS:", ENV_PATH.exists())


# =========================
# FASTAPI
# =========================

app = FastAPI(
    title="ChatBot Agent Service",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# =========================
# GROQ CONFIG
# =========================

def get_groq_api_key() -> str | None:
    api_key = os.getenv("GROQ_API_KEY", "").strip()
    return api_key or None


def is_valid_groq_api_key(api_key: str | None) -> bool:
    return bool(api_key and api_key.startswith("gsk_"))


def build_groq_client() -> Groq:
    api_key = get_groq_api_key()

    print("API KEY FOUND:", bool(api_key))

    if api_key:
        print("API KEY PREFIX:", api_key[:10])

    if not is_valid_groq_api_key(api_key):
        raise HTTPException(
            status_code=503,
            detail="GROQ_API_KEY is missing or invalid. Expected key starting with gsk_."
        )

    return Groq(api_key=api_key)


# =========================
# SYSTEM PROMPT
# =========================

SYSTEM_PROMPT = """
Bạn là một trợ lý AI thông minh được tích hợp vào chatbox công ty.

Khi người dùng tag @ChatBot và đưa ra yêu cầu, bạn sẽ giúp họ soạn thảo tin nhắn,
văn bản hay trả lời các câu hỏi một cách chuyên nghiệp.

Hướng dẫn:

- Trong lịch sử hội thoại: role "assistant" là các lần bạn (ChatBot) đã trả lời trước đó;
  role "user" là người đang nhắn @ChatBot (và có thể có dòng [Thành viên khác] từ người khác trong nhóm).
- Luôn trả lời bằng ngôn ngữ mà người dùng sử dụng
- Với yêu cầu soạn tin nhắn/văn bản: chỉ trả về nội dung cần soạn
- Với câu hỏi thông thường: trả lời trực tiếp, súc tích
- Giữ tone phù hợp với yêu cầu
- KHÔNG bao gồm các câu như:
  "Đây là tin nhắn cho bạn"
  "Tôi đã soạn..."
  "Không có yêu cầu cụ thể nào được đặt ra cho @ChatBot."
"""


# =========================
# MODELS
# =========================

class MessageRequest(BaseModel):
    message: str
    history: list = []


class MessageResponse(BaseModel):
    reply: str
    triggered: bool
    original_message: str


# =========================
# UTILITIES
# =========================

def extract_chatbot_query(message: str) -> str | None:
    pattern = r"@ChatBot\s+(.*)"
    match = re.search(pattern, message, re.IGNORECASE | re.DOTALL)

    if match:
        return match.group(1).strip()

    return None


# =========================
# API
# =========================

@app.post("/api/chat", response_model=MessageResponse)
async def chat(request: MessageRequest):

    query = extract_chatbot_query(request.message)

    if not query:
        return MessageResponse(
            reply="",
            triggered=False,
            original_message=request.message
        )

    history_messages = []

    for msg in request.history:
        if (
            isinstance(msg, dict)
            and msg.get("role")
            and msg.get("content")
        ):
            history_messages.append(
                {
                    "role": msg["role"],
                    "content": msg["content"]
                }
            )

    history_messages.append(
        {
            "role": "user",
            "content": query
        }
    )

    try:
        client = build_groq_client()

        response = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {
                    "role": "system",
                    "content": SYSTEM_PROMPT
                },
                *history_messages
            ],
            temperature=0.7,
            max_tokens=1024
        )

        reply = response.choices[0].message.content or ""

        return MessageResponse(
            reply=reply,
            triggered=True,
            original_message=request.message
        )

    except Exception as e:
        print("=" * 60)
        traceback.print_exc()
        print("=" * 60)

        raise HTTPException(
            status_code=500,
            detail=str(e)
        )


@app.get("/health")
async def health():
    api_key = get_groq_api_key()

    return {
        "status": "ok",
        "api_key_configured": is_valid_groq_api_key(api_key),
        "key_prefix": api_key[:10] if api_key else None,
        "env_file": str(ENV_PATH),
        "env_exists": ENV_PATH.exists()
    }


# =========================
# STATIC FILES
# =========================

STATIC_DIR = BASE_DIR / "static"

if STATIC_DIR.exists():
    app.mount(
        "/",
        StaticFiles(directory=str(STATIC_DIR), html=True),
        name="static"
    )


# =========================
# RUN
# =========================

if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8088,
        reload=True
    )