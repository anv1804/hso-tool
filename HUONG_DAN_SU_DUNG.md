# Hướng Dẫn Sử Dụng & Đối Chiếu Bản Java - C# Hiệp Sĩ Online (HSO v403)

## 1. Cấu Trúc Thư Mục Dự Án

| Thư mục / Tập tin | Mô tả |
| :--- | :--- |
| `src_csharp/` | **Mã nguồn C# tham chiếu** (226 files `.cs`). Dịch ngược từ `Assembly-CSharp.dll` bằng ILSpy. Không bị obfuscate, tên biến, tên hàm, cấu trúc class rõ ràng. |
| `src_java/` | **Mã nguồn Java chính** (196 files `.java`). Dịch ngược từ `KnightOnline_v403.jar` bằng CFR và đã được **khôi phục 3,265 tham số/biến** đối chiếu từ C# sang, tích hợp sẵn `com.silverknight.HsoBridge` phục vụ Radar HUD và Auto. |
| `tools/restore_java_symbols.py` | Công cụ tự động đối chiếu AST/Signature giữa C# và Java để khôi phục tên tham số và biến gốc. |
| `jar_resources/` | Toàn bộ tài nguyên gốc của bản Java (ảnh `x1/`, `weapon/`, `c/`, sprite, âm thanh, `icon.png`, `META-INF/MANIFEST.MF`). |
| `KnightOnline_v403.jar` | File JAR game J2ME chính thức sau khi mod tích hợp bridge. |
| `microemulator-3.0.0.jar` | Giả lập Java ME (CLDC/MIDP) chuyên dụng để chạy bản Java trên Windows / PC / Linux. |
| `hso_tool.py` | Giao diện đồ họa Studio HUD pixel-art bằng Python/Tkinter (Radar bản đồ, máu quái, auto đánh, nhặt đồ, đổi khu, đổi tốc độ). |
| `run_tool.bat` / `run_tool.ps1` | Script 1-click khởi chạy HSO Tool Studio trên Windows. |
| `run_java.bat` / `run_java.ps1` | Script 1-click khởi chạy game trực tiếp bằng MicroEmulator trên Windows. |
| `build_jar.bat` / `build_jar.ps1` | Script 1-click biên dịch toàn bộ `src_java/` và đóng gói lại vào `KnightOnline_v403.jar`. |
| `run_tool.sh` / `run_java.sh` | Script khởi chạy trên môi trường Linux / macOS. |

---

## 2. Chuẩn Bị Môi Trường Trên Windows

Để chạy và phát triển trên Windows, bạn cần:
1. **Java Development Kit (JDK)**: Phiên bản 8, 11 hoặc 17+ (Khuyên dùng [Eclipse Adoptium Temurin OpenJDK](https://adoptium.net/)).
   - Đảm bảo lệnh `java -version` và `javac -version` nhận diện trong cmd/powershell.
2. **Python**: Phiên bản 3.8+ (Khuyên dùng Python 3.10 - 3.12 từ [python.org](https://www.python.org/)).
   - Tích chọn **"Add python.exe to PATH"** khi cài đặt.
3. **Thư viện Python**:
   ```cmd
   pip install -r requirements.txt
   ```
   *(Script `run_tool.bat` sẽ tự động kiểm tra và cài đặt Pillow nếu thiếu).*

---

## 3. Cách Khởi Chạy Game Trên Windows

### Cách 1: Sử Dụng Giao Diện HSO Tool Studio (Khuyên dùng):
Chỉ cần nhấp đúp chuột vào:
```cmd
run_tool.bat
```
Hoặc qua PowerShell:
```powershell
.\run_tool.ps1
```
*Giao diện HUD thiết kế theo phong cách game Hiệp Sĩ Online (panel gỗ viền vàng, mini HUD 560x365):*
- **Radar thời gian thực**: Quét toàn bộ map hiển thị người chơi, NPC, lối chuyển map, đá dịch chuyển, quái vật sống (đỏ) và quái vật đã chết (xám `#555555`).
- **Thanh máu quái (HP Bar)**: Hiển thị quái sống/chết, khoảng cách, HP hiện tại, tự động ghim mục tiêu hoặc Auto Farm gom quái 1-hit toàn map.
- **Băng chữ chạy ngang (Marquee)**: Bảng trên và dưới chạy chữ thông báo thời gian thực như trong game (cảnh báo Bot Captcha, cảnh báo người chơi lạ vào khu, trạng thái an toàn).
- **Cơ chế an toàn chống ban nick (Dual-layer Safety)**:
  - Chỉ cho phép bật Auto Fam / Auto Atk trực tiếp khi khu có 0 người chơi khác.
  - Nếu khu có người chơi khác (> 0 người), hiển thị Modal xác nhận cảnh báo an toàn trước khi kích hoạt.
  - Khi đang Auto Fam mà có người chơi khác bước vào khu, hệ thống sẽ **ngay lập tức tắt Auto Fam gom quái** và tự động hạ cấp xuống đánh thường đơn mục tiêu từng con để tránh bị quay clip tố cáo.
  - Tự động phát hiện Bot Captcha (`typeBoss == 2`) và phát thông báo khẩn cấp cả trên tool lẫn trong game.
- **Auto Đánh & Auto Nhặt Đồ**: Bật/tắt 1 chạm qua IPC file JSON đồng bộ với game.
- **Tốc độ di chuyển**: Tùy chỉnh x1.0, x1.2, x1.4, x1.6, x1.8, x2.0.
- **Khu vực (Zone)**: Xem và đổi nhanh khu vực đang đứng.
- **Màn hình game**: 1-click chọn độ phân giải chuẩn (`800x480`, `1024x600`, `640x360`, `480x320` hoặc tự nhập kích thước).

---

### Cách 2: Khởi chạy trực tiếp bản Game qua MicroEmulator:
Nhấp đúp chuột vào:
```cmd
run_java.bat
```
Hoặc mở từ dòng lệnh với kích thước tùy chỉnh:
```cmd
:: Mở mặc định (800x480, không vỏ điện thoại):
run_java.bat

:: Mở với kích thước tùy chỉnh:
run_java.bat 640 360
run_java.bat 1024 600

:: Mở file JAR khác:
run_java.bat KnightOnline_v403.jar 800 480
```
*(Trên PowerShell, thay bằng `.\run_java.ps1`)*

*Lưu ý:* Khi cửa sổ game mở lên, bạn có thể dùng chuột kéo giãn mép cửa sổ tùy thích để phóng to/thu nhỏ màn hình.

---

## 4. Biên Dịch & Đóng Gói (Re-pack) Sau Khi Sửa Mã Nguồn

Khi bạn chỉnh sửa code trong thư mục `src_java/`:

### Cách 1: Bằng Script 1-click trên Windows:
Nhấp đúp chuột vào:
```cmd
build_jar.bat
```
Hoặc qua PowerShell:
```powershell
.\build_jar.ps1
```

### Cách 2: Trực tiếp trong HSO Tool Studio:
Bấm nút **"Repack JAR"** trong menu cài đặt của tool, hệ thống sẽ tự động gọi javac biên dịch 196 file Java với bytecode tương thích và chèn ngược vào file `KnightOnline_v403.jar`.

---

## 5. Bản Tham Chiếu C# Đối Chiếu Cho Bản Java

Vì bản C# (Unity) được Teamobi port trực tiếp từ bản Java J2ME gốc mà không áp dụng obfuscator, nên toàn bộ logic và cấu trúc class tương đồng 1:1.

### Bảng đối chiếu các thành phần cốt lõi:

| Chức năng chính | Bản Java (`src_java/`) | Bản C# tham chiếu (`src_csharp/`) |
| :--- | :--- | :--- |
| **Cầu nối IPC & Auto Tool** | `com/silverknight/HsoBridge.java` | *(Thành phần mở rộng mod)* |
| **Nhân vật & Trang bị** | `GameObjects/Player.java`<br>`GameObjects/MainObject.java`<br>`GameObjects/Item.java` | `Player.cs`<br>`MainObject.cs`<br>`Item.cs` |
| **Xử lý gói tin mạng** | `netcommand/global/ReadMessenge.java`<br>`netcommand/global/GlobalService.java`<br>`CLib/Session_ME.java` | `ReadMessenge.cs`<br>`GlobalService.cs`<br>`Session_ME.cs` |
| **Canvas & Vòng lặp game** | `Main/GameCanvas.java`<br>`com/silverknight/TemCanvas.java` | `GameCanvas.cs`<br>`TemCanvas.cs` |
| **Hiệu ứng & Kỹ năng** | `Skill/Skill.java`<br>`GameEffect/MainEffect.java` | `Skill.cs`<br>`MainEffect.cs` |
| **Giao diện & Màn hình** | `GameScreen/GameScreen.java`<br>`GameScreen/LoginScreen.java`<br>`InterfaceComponents/MainTabNew.java` | `GameScreen.cs`<br>`LoginScreen.cs`<br>`MainTabNew.cs` |
| **Tài nguyên & Bảng tra cứu** | `Model/CRes.java`<br>`Model/T.java` | `CRes.cs`<br>`T.cs` |

### Mẹo đối chiếu mã nguồn:
- Khi gặp biến hoặc tham số khó hiểu trong Java (ví dụ: `n`, `s`, `b`), mở file `.cs` tương ứng trong `src_csharp/` để xem tên gốc của biến và logic giải thuật.
- Luồng giải mã packet: xem `ReadMessenge.cs` để biết chính xác từng opcode đọc `readByte()`, `readShort()`, `readUTF()`, `readInt()`.

---

## 6. Hướng Dẫn Tính Năng Ghim Mục Tiêu & PK Từ Xa (Remote Strike)

### 6.1. Quy trình Ghim & Tấn Công Người Chơi
1. **Bật Đồ Sát**:
   - Bấm nút **"Đồ Sát: TẮT"** trên panel điều khiển của HSO Tool để chuyển sang **"Đồ Sát: BẬT"** (màu vàng kim).
   - Tool sẽ gửi lệnh lên Server để chuyển trạng thái sang Đồ Sát (`typePk = 0`).
2. **Ghim Người Chơi**:
   - Chọn tab **"PLAYER"** trên danh sách bên trái. Tool quét toàn bộ người chơi trong map (cả trong lẫn ngoài tầm nhìn).
   - Nhấp chuột vào một người chơi:
     - **Trên Tool**: Hàng nhân vật sẽ lập tức đổi sang nền nâu đỏ `#7a2a0a` với **viền vàng kim rực rỡ `#ffd700`** và gắn icon `📌 [GHIM]`. Nút chuyển thành `Bỏ ghim [tên]`.
     - **Trong Game**: Xuất hiện ngay **HUD Ghim Mục Tiêu** ở mép trên màn hình hiển thị:
       - Tên mục tiêu, Level, trạng thái Đồ Sát (`DO SAT: BAT`).
       - Thanh máu đỏ tươi hiển thị HP thời gian thực.
       - Cự ly chính xác (pixel) và mũi tên định vị ngoài màn hình (`<-` hoặc `->`).
     - *Bỏ ghim*: Nhấp lại vào hàng đó hoặc bấm nút **"Bỏ ghim"**.
3. **Tấn Công Từ Xa (Remote Strike)**:
   - **Cách 1 (Trên Tool)**: Nhấp nút **"⚔️ Tấn công ngay"** để tung đòn ngay lập tức mà không cần chuyển sang cửa sổ game.
   - **Cách 2 (Trong Game)**: Bấm phím đánh thường (phím 5 / space) hoặc bất kỳ phím tắt chiêu (1-5), hệ thống sẽ ưu tiên tuyệt đối tấn công mục tiêu đang ghim.
   - **Cách 3 (Tự Động)**: Bật **"Auto Attack: ON"**, nhân vật sẽ tự động spam chiêu từ xa vào mục tiêu ghim mỗi 350ms.
   - *Cơ chế kỹ thuật*: Hệ thống gửi chuỗi tọa độ ảo nội suy từng bước 36px qua mặt anticheat server, gửi gói `fire_Pk` (opcode `0x06`), đợi server trừ máu mục tiêu rồi kéo tọa độ về vị trí gốc. Camera và vị trí nhân vật cục bộ được giữ nguyên tuyệt đối, không bị giật/teleport.
   - *Hỗ trợ tự động*: Nếu người chơi quên bật Đồ Sát, hệ thống sẽ tự động gửi lệnh bật Đồ Sát khi ra đòn.

### 6.2. Giải Thích & Xử Lý Lỗi `NullPointerException WInputMethod`
- **Nguyên nhân**: Khi click chuột giữa cửa sổ Python Tkinter và cửa sổ Java MicroEmulator trên Windows trong khi máy đang bật bộ gõ tiếng Việt (Unikey / EVKey / Windows IME), Java AWT EventQueue đôi khi in ra thông báo:
  ```
  Exception in thread "AWT-EventQueue-0" java.lang.NullPointerException: Cannot read field "x" because ... at sun.awt.windows.WInputMethod$1.run
  ```
- **Bản chất**: Đây là lỗi nội bộ của bộ xử lý gõ chữ IME trong OpenJDK trên Windows, **hoàn toàn vô hại, không ảnh hưởng đến game, không làm crash game và không làm mất kết nối**.
- **Giải pháp đã áp dụng**:
  - Đã tích hợp cờ `-Dsun.awt.im.active=false` vào lệnh khởi động Java.
  - Tự động gọi `disableAwtIme()` trên cửa sổ game để tắt bộ bắt phím AWT IME, triệt tiêu thông báo lỗi này.

