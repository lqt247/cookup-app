# 🍳 CookUp — App Công Thức Nấu Ăn

> Khám phá, nấu ăn và lên kế hoạch bữa ăn thông minh hơn mỗi ngày.

![Platform](https://img.shields.io/badge/Platform-Android-brightgreen) ![Backend](https://img.shields.io/badge/Backend-Spring%20Boot-brightgreen) ![Language](https://img.shields.io/badge/Language-Java-orange) ![Database](https://img.shields.io/badge/Database-MySQL-blue) ![Status](https://img.shields.io/badge/Status-In%20Development-yellow)

---

## 📱 Giới thiệu

**CookUp** là ứng dụng Android giúp người dùng khám phá công thức nấu ăn, lên kế hoạch bữa ăn và đi chợ thông minh. Điểm nổi bật của app là tính năng **AI gợi ý món ăn** từ nguyên liệu có sẵn trong tủ lạnh và **Cook Mode** hướng dẫn nấu ăn step-by-step.

---

## ✨ Tính năng chính

- 🔍 **Khám phá** — Tìm kiếm & lọc công thức theo quốc gia, khẩu vị, buổi ăn, loại món
- 🤖 **AI Gợi ý** — Nhập nguyên liệu có sẵn, AI gợi ý món phù hợp
- 👨‍🍳 **Cook Mode** — Hướng dẫn nấu ăn từng bước, có timer, màn hình không tắt
- 🛒 **Shopping List** — Tự động tổng hợp nguyên liệu từ công thức đã chọn
- ❤️ **Yêu thích** — Lưu và phân loại công thức vào bộ sưu tập
- 📝 **Chia sẻ** — Đăng công thức của bản thân cho cộng đồng
- ⭐ **Đánh giá** — Bình luận và rating công thức

---

## 🛠️ Tech Stack

| Phần | Công nghệ |
|---|---|
| Android (FE) | Java, XML Layout, Material Design 3 |
| Navigation | Android Navigation Component |
| Gọi API | Retrofit 2 + Gson |
| Load ảnh | Glide |
| Backend (BE) | Spring Boot 3, Spring Security |
| Authentication | JWT Token |
| Database | MySQL + JPA/Hibernate |
| AI Feature | Gemini API |
| Lưu ảnh | Cloudinary |
| Notification | Firebase Cloud Messaging |

---

## 📁 Cấu trúc Project

```
cookup-app/
├── cookup-android/          # Android Studio project (Java)
│   └── app/src/main/
│       ├── java/com.cookup/
│       │   ├── activity/    # Các màn hình
│       │   ├── fragment/    # Các tab
│       │   ├── adapter/     # RecyclerView adapters
│       │   ├── model/       # Data models
│       │   ├── network/     # Retrofit setup + API
│       │   ├── viewmodel/   # ViewModel
│       │   └── utils/       # SharedPreferences, Constants
│       └── res/
│           ├── layout/      # XML giao diện
│           ├── drawable/    # Icon, hình ảnh
│           └── values/      # Colors, strings, themes
│
├── cookup-backend/          # Spring Boot project (Java)
│   └── src/main/java/com.cookup/
│       ├── controller/      # REST API endpoints
│       ├── service/         # Business logic
│       ├── repository/      # Database queries
│       ├── model/           # Entity (User, Recipe,...)
│       ├── dto/             # Request/Response objects
│       ├── security/        # JWT, Spring Security config
│       └── exception/       # Error handling
│
└── README.md
```

---

## 🚀 Hướng dẫn chạy

### Backend
```bash
cd cookup-backend
# Cấu hình database trong application.properties
./mvnw spring-boot:run
# Server chạy tại http://localhost:8080
```

### Android
```
1. Mở cookup-android trong Android Studio
2. Chạy máy ảo (AVD) hoặc kết nối điện thoại thật
3. Nhấn Run ▶
```

> ⚠️ Khi test trên máy ảo, BE URL dùng `http://10.0.2.2:8080` thay vì `localhost`

---

## 👨‍💻 Tác giả

Đồ án môn Lập trình Mobile — Sinh viên thực hiện độc lập.

---

*🚧 Dự án đang trong quá trình phát triển.*
