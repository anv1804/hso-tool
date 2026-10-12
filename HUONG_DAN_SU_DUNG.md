# Hướng Dẫn Sử Dụng & Đối Chiếu Bản Java - C# Hiệp Sĩ Online (HSO v403)

## 1. Cấu Trúc Thư Mục Đã Dịch Ngược

| Thư mục / Tập tin | Mô tả |
| :--- | :--- |
| `src_csharp/` | **Mã nguồn C# tham chiếu** (226 files `.cs`). Dịch ngược từ `Assembly-CSharp.dll` bằng ILSpy. Không bị obfuscate, tên biến, tên hàm, cấu trúc class rõ ràng. |
| `src_java/` | **Mã nguồn Java chính** (195 files `.java`). Dịch ngược từ `KnightOnline_v403.jar` bằng CFR. |
| `jar_resources/` | Toàn bộ tài nguyên gốc của bản Java (ảnh `x1/`, `weapon/`, `c/`, sprite, âm thanh, `icon.png`, `META-INF/MANIFEST.MF`). |
| `extracted_exe/` | Toàn bộ file giải nén từ `HSO_v403B.exe` (Unity build, data, các dll Mono/Unity). |
| `microemulator-3.0.0.jar` | Giả lập Java ME (CLDC/MIDP) chuyên dụng để chạy bản Java trên PC/Linux. |
| `run_java.sh` | Script bash tự động khởi chạy bản Java bằng MicroEmulator trên OpenJDK 8. |

---

## 2. Cách Khởi Chạy Bản Java Trên Môi Trường

Hệ thống đã được cài đặt **OpenJDK 8 JRE** (`/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java`) và **MicroEmulator**.

### Lệnh chạy trực tiếp:
```bash
./run_java.sh
```
Hoặc nếu muốn chỉ định file jar cụ thể:
```bash
./run_java.sh KnightOnline_v403.jar
```

*Lưu ý:* Khi chạy trên giao diện Desktop (Cinnamon/X11), cửa sổ MicroEmulator sẽ tự động mở lên, bạn có thể chỉnh Options -> Select Device để chọn độ phân giải màn hình hoặc kích thước giả lập phù hợp.

---

## 3. Bản Tham Chiếu C# Đối Chiếu Cho Bản Java

Vì bản C# (Unity) được Teamobi port trực tiếp từ bản Java J2ME gốc mà không áp dụng obfuscator, nên toàn bộ logic và cấu trúc class tương đồng 1:1.

### Bảng đối chiếu các thành phần cốt lõi:

| Chức năng chính | Bản Java (`src_java/`) | Bản C# tham chiếu (`src_csharp/`) |
| :--- | :--- | :--- |
| **Nhân vật & Trang bị** | `GameObjects/Player.java`<br>`GameObjects/MainObject.java`<br>`GameObjects/Item.java` | `Player.cs`<br>`MainObject.cs`<br>`Item.cs` |
| **Xử lý gói tin mạng** | `netcommand/global/ReadMessenge.java`<br>`netcommand/global/GlobalService.java`<br>`CLib/Session_ME.java` | `ReadMessenge.cs`<br>`GlobalService.cs`<br>`Session_ME.cs` |
| **Canvas & Vòng lặp game** | `Main/GameCanvas.java`<br>`com/silverknight/TemCanvas.java` | `GameCanvas.cs`<br>`TemCanvas.cs` |
| **Hiệu ứng & Kỹ năng** | `Skill/Skill.java`<br>`GameEffect/MainEffect.java` | `Skill.cs`<br>`MainEffect.cs` |
| **Giao diện & Màn hình** | `GameScreen/GameScreen.java`<br>`GameScreen/LoginScreen.java`<br>`InterfaceComponents/MainTabNew.java` | `GameScreen.cs`<br>`LoginScreen.cs`<br>`MainTabNew.cs` |
| **Tài nguyên & Bảng tra cứu** | `Model/CRes.java`<br>`Model/T.java` | `CRes.cs`<br>`T.cs` |

### Mẹo đối chiếu mã nguồn:
- Khi gặp một phương thức hoặc tham số bị đổi tên (ví dụ: `int n`, `int n2`, `s`, `s2` trong Java), mở file `.cs` tương ứng trong `src_csharp/` để xem tên gốc của tham số và logic giải thuật.
- Luồng giải mã packet: xem `ReadMessenge.cs` để biết chính xác từng opcode đọc `readByte()`, `readShort()`, `readUTF()`, `readInt()`.

---

## 4. Cách Đóng Gói (Re-pack) Bản Java Sau Khi Chỉnh Sửa

Nếu bạn thay đổi file `.class` (hoặc compile lại):
1. Cập nhật các class vào file jar:
   ```bash
   jar uf KnightOnline_v403.jar -C bin/ .
   ```
2. Chạy thử lại ngay bằng:
   ```bash
   ./run_java.sh KnightOnline_v403.jar
   ```
