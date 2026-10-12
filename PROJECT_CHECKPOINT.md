# CHECKPOINT DỰ ÁN HSO TOOL & KNIGHT ONLINE v403
*Ngày cập nhật: 12/10/2026*

---

## 1. Trạng thái hoạt động hiện tại
- **Game client (J2ME / MicroEmulator):** Đã patch `StdOutAppender.class` im lặng hoàn toàn, không còn bão log `Loading MIDlet class ... of version 52`.
- **HSO Tool Studio (`hso_tool.py`):** Giao diện Tkinter chạy mượt mà, IPC file-based (`client_state.json` / `client_commands/`).
- **Windows Update:** Đã tạm dừng 7 ngày (đến 19/10/2026), tắt hoàn toàn `TiWorker.exe` và `TrustedInstaller.exe`.

---

## 2. Các tính năng cốt lõi đã hoàn thiện
1. **Auto Fam 1-hit gom quái toàn map:**
   - Quét ID quái sống trên map qua menu Quét quái.
   - Khi bật Auto Fam (và không có người lạ): gửi tấn công dồn toàn bộ quái trong danh sách.
   - Radar hiển thị quái sống (đỏ), quái trong cấu hình farm (vàng), quái chết (xám).
2. **Cơ chế An toàn bảo vệ tài khoản:**
   - Khi có người chơi lạ vào khu (`other_count > 0`): Tự động ngắt ngay lập tức Auto Fam 1-hit, chuyển về đánh thường từng con để tránh bị soi acc/tố cáo.
   - Có banner chạy chữ màu vàng/cam thông báo số người trong khu.
   - Modal cảnh báo xuất hiện nếu cố bật Auto Fam khi có người trong khu.
   - Phát hiện Bot Captcha và cảnh báo trên banner/màn hình.
3. **Panel Người chơi & Ghim mục tiêu PK từ xa (Remote Strike):**
   - Tab "PLAYER" hiển thị toàn bộ người chơi trong map kèm Level, HP và cự ly.
   - Click vào người chơi trong danh sách -> Ghim mục tiêu (`pinnedPlayerId`). Hàng người chơi nổi bật với viền vàng kim `#ffd700` và icon `📌 [GHIM]`.
   - **HUD Banner trong Game**: Hiển thị bảng ghim mục tiêu bằng gỗ viền vàng ở mép trên màn hình game: Tên, Level, trạng thái Đồ Sát, thanh máu đỏ tươi HP thời gian thực, cự ly pixel và mũi tên định vị ngoài màn hình (`<-` hoặc `->`).
   - **Tấn công từ xa mượt mà (Remote Strike)**:
     - Gửi chuỗi di chuyển ảo nội suy 36px qua mặt anticheat server, chờ tick 100ms, gửi gói `fire_Pk` (opcode 0x06), chờ server tính sát thương 160ms, rút tọa độ ảo về vị trí gốc.
     - Vị trí nhân vật cục bộ và camera hoàn toàn cố định, không giật màn hình, không bị server giật lùi (rubberband).
     - Hỗ trợ nút **"⚔️ Tấn công ngay"** trực tiếp trên Tool, hoặc bấm phím tắt chiêu (1-5/space) trong game, hoặc bật **"Auto Attack: ON"** để tự động spam chiêu.
     - Tự động kích hoạt Đồ Sát (`set_Pk(0)`) nếu nhân vật chưa kịp bật.
   - **Xử lý lỗi AWT IME Windows**: Tích hợp cờ `-Dsun.awt.im.active=false` và hàm `disableAwtIme()` vô hiệu hóa bộ gõ IME native, triệt tiêu lỗi `NullPointerException WInputMethod` khi tương tác giữa 2 cửa sổ trên Windows.

---

## 3. Các file quan trọng
- `src_java/com/silverknight/HsoBridge.java`: Cầu nối giao tiếp giữa Tool và Game Client J2ME.
- `src_java/GameObjects/Player.java`: Xử lý phím tắt, di chuyển, ưu tiên mục tiêu ghim (`setActionHotKey`).
- `src_java/Skill/ListSkill.java`: Gửi gói tin kỹ năng, chunking mục tiêu, kiểm tra cự ly.
- `src_java/com/silverknight/TemMidlet.java`: Khởi tạo MIDlet game.
- `hso_tool.py`: Ứng dụng điều khiển HSO Tool Studio GUI (Tkinter).
- `run_java.bat`: Script khởi chạy MicroEmulator và KnightOnline_v403.jar.
- `KnightOnline_v403.jar`: File JAR game đã tích hợp mod.
- `microemulator-3.0.0.jar`: Giả lập J2ME (đã tắt bão log).
