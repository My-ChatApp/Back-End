# 🤖 ChatBot Agent Service

Service AI agent tích hợp vào chatbox, được kích hoạt bằng `@ChatBot`.

## Tính năng

- 🏷️ Tag `@ChatBot` để kích hoạt AI
- 🔒 Phản hồi chỉ hiển thị cho người dùng (private reply)
- 📋 Nút Copy để dùng ngay
- ✏️ Nút Chỉnh sửa trước khi copy
- 💬 Giao diện chatbox demo tích hợp sẵn
- 🔗 REST API có thể tích hợp vào bất kỳ chatbox nào

## Cài đặt

### 1. Cài dependencies

```bash
pip install -r requirements.txt
```

### 2. Tạo file `.env`

```bash
cp .env.example .env
```

Mở file `.env` và điền API key:

```
GROQ_API_KEY=gsk_xxxxxxxxxxxxxxxx
```

API key của Groq phải bắt đầu bằng `gsk_`. Nếu bạn đang dùng key từ dịch vụ khác, hệ thống sẽ trả về lỗi xác thực 401/503.

### 3. Khởi chạy service

```bash
python main.py
```

Service sẽ chạy tại: **http://localhost:8088**

## Sử dụng

### Giao diện demo
Mở trình duyệt tại `http://localhost:8088`

Gõ ví dụ:
- `@ChatBot soạn tin nhắn trang trọng hỏi thăm sức khỏe sếp`
- `@ChatBot viết email xin phép nghỉ phép ngày mai`
- `@ChatBot dịch "Good morning, I hope you are well" sang tiếng Việt lịch sự`

### API trực tiếp

**POST** `/api/chat`

```json
{
  "message": "@ChatBot soạn tin nhắn trang trọng hỏi thăm sức khỏe sếp",
  "history": []
}
```

**Response:**
```json
{
  "reply": "Kính gửi Anh/Chị...",
  "triggered": true,
  "original_message": "@ChatBot soạn tin nhắn..."
}
```

- `triggered: true` → có @ChatBot, có phản hồi AI
- `triggered: false` → không có @ChatBot, bỏ qua

**GET** `/health` — Kiểm tra trạng thái service

## Tích hợp vào chatbox thực tế

Khi người dùng gửi tin, gọi API:

```javascript
const response = await fetch('http://localhost:8088/api/chat', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ message: userMessage, history: [] })
});

const data = await response.json();

if (data.triggered) {
  // Hiển thị data.reply chỉ cho người dùng đó (ephemeral message)
}
```

## Cấu trúc thư mục

```
chatbot-service/
├── main.py          # FastAPI server chính
├── requirements.txt # Dependencies
├── .env.example     # Template biến môi trường
├── .env             # File env thực (tạo từ .env.example)
└── static/
    └── index.html   # Giao diện chatbox demo
```
