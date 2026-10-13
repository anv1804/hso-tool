# HSO Tool & Knight Online v403 Mod

Bộ công cụ hỗ trợ chơi game, Radar thời gian thực, Auto Farm toàn map và nền tảng Modding cho **Hiệp Sĩ Online (Knight Online v403)** trên PC (Java ME / MicroEmulator).

---

## 📌 Tính Năng Nổi Bật (Phiên Bản v1.1)

- 📡 **Radar Bản Đồ & Quét Quái Toàn Map**:
  - Quét nhận diện toàn bộ quái trên map chỉ trong 1.5 - 2 giây.
  - Cập nhật vị trí, HP và đếm ngược thời gian hồi sinh theo thời gian thực.
  - Chế độ xem danh sách quái 2 cột tiện lợi, trực quan.
- ⚡ **Auto Farm 1-Hit Toàn Map**:
  - Tấn công đồng loạt 100% quái trong map trong cùng một tích tắc (Burst Loop).
  - Tối ưu hóa nhịp đánh chuẩn 1.0 giây theo hồi chiêu đánh thường khi quái cần 2-3 hit.
  - Đón đầu hồi sinh sớm 500ms, quái vừa thành hình là nhận sát thương chết ngay.
  - Tự động đồng bộ pha hồi sinh cho toàn bộ đàn quái.
- 🎯 **Ghim Mục Tiêu & Tấn Công Từ Xa (Remote Strike)**:
  - Ghim người chơi để PK từ xa qua mặt anticheat server mà không giật camera.
  - Ghim quái vật và tung đòn trực tiếp bất kể cự ly.
- 🎒 **Auto Nhặt Đồ & Bộ Lọc Đồ Rơi (Loot Filter)**:
  - Phân loại trang bị theo 6 phẩm cấp màu (Trắng, Xanh, Vàng, Tím, Cam, Xanh lá).
  - Tùy chọn nhặt bình máu, mana, vàng, đá cường hóa, ngọc, đồ nhiệm vụ.
- 🛡️ **Hệ Thống An Toàn Bảo Vệ Tài Khoản**:
  - Tự động tạm dừng Auto Farm khi có người chơi lạ bước vào khu vực.
  - Tự động tiếp tục (Auto-Resume) khi khu vực an toàn trở lại (0 người).
  - Cảnh báo tức thì khi xuất hiện Bot Captcha.
  - Khóa chặt cơ chế tự đánh nội tại khi tắt Auto.
- 🚀 **Tiện Ích Nhanh**:
  - Đổi khu vực tức thì, tùy chỉnh tốc độ chạy (x1.0 - x2.0).
  - Đếm ngược 180s cho nút Đồ Sát (PK).

---

## 🚀 Hướng Dẫn Cài Đặt & Sử Dụng

### Yêu Cầu Môi Trường:
1. **Java Development Kit (JDK)**: Java 8 trở lên (khuyên dùng JDK 8, 11 hoặc 17).
2. **Python**: Phiên bản 3.8+ (Cài thư viện: `pip install -r requirements.txt`).

### Khởi Chạy:
- **Trên Windows**:
  - Khởi chạy Tool Studio GUI: Nhấp đúp vào `run_tool.bat` (hoặc PowerShell: `.\run_tool.ps1`).
  - Khởi chạy Game trực tiếp: Nhấp đúp vào `run_java.bat` (hoặc PowerShell: `.\run_java.ps1`).
- **Trên Linux / macOS**:
  - Khởi chạy Tool Studio: `./run_tool.sh`
  - Khởi chạy Game: `./run_java.sh`

### Biên Dịch & Đóng Gói (Re-pack JAR):
Sau khi sửa mã nguồn Java trong `src_java/`:
- Windows: Chạy `build_jar.bat` hoặc `build_jar.ps1`
- Linux: Chạy `./build_jar.sh`
- Hoặc bấm nút **"Repack JAR"** trong cài đặt của Tool Studio.

---

## 📚 Tài Liệu Kỹ Thuật

- 📖 [Tài Liệu Hệ Thống Auto (v1.0)](file:///home/anv/Documents/tools/hso-tool/TAI_LIEU_HE_THONG_AUTO.md): Hướng dẫn chi tiết nguyên lý hoạt động, cấu trúc IPC, cơ chế Burst Loop, đón đầu hồi sinh, bộ lọc loot và cơ chế an toàn.
- 📖 [Hướng Dẫn Sử Dụng & Đối Chiếu](file:///home/anv/Documents/tools/hso-tool/HUONG_DAN_SU_DUNG.md): Hướng dẫn sử dụng công cụ, đối chiếu C# - Java và xử lý lỗi môi trường.
- 📖 [Tài Liệu Các File & Hàm Quan Trọng](file:///home/anv/Documents/tools/hso-tool/TAI_LIEU_CAC_FILE_VA_HAM_QUAN_TRONG.md): Bản đồ kiến trúc chi tiết client game HSO v403.
- 📖 [Project Checkpoint](file:///home/anv/Documents/tools/hso-tool/PROJECT_CHECKPOINT.md): Nhật ký tiến trình phát triển và các giải pháp kỹ thuật đã áp dụng.

---

## ⚖️ Giấy Phép & Tuyên Bố Miễn Trừ Trách Nhiệm
Dự án được thực hiện nhằm mục đích nghiên cứu học thuật, dịch ngược và phát triển công cụ hỗ trợ cá nhân.
