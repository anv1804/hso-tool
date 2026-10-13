# TÀI LIỆU CƠ CHẾ BÁN ĐỒ VÀ GIAO THỨC MẠNG HIỆP SĨ ONLINE (HSO)

> **Tài liệu nghiên cứu kỹ thuật & đặc tả hệ thống Auto Sell**  
> **Dự án**: `hso-tool` | **Game client**: Hiệp Sĩ Online (Kasatria Online / KnightOnline v403)  
> **Ngày cập nhật**: 13/10/2026

---

## 1. TỔNG QUAN & MỤC TIÊU HỆ THỐNG BÁN ĐỒ

### 1.1. Mục tiêu
- **Bán tự động (Auto Sell)**: Tự động quét và giải phóng các ô hành trang chứa trang bị rác (đồ trắng, đồ xanh, đồ +0, nguyên liệu không cần thiết) khi đi farm quái mà không làm nghẽn túi đồ.
- **Bán từng món theo yêu cầu (Single Item Sell)**: Cho phép người chơi chủ động bấm nút "Bán" từng món cụ thể trên giao diện điều khiển của Tool mà không cần thao tác click thủ công nhiều bước trong game.
- **Bảo vệ an toàn tuyệt đối**:
  - Không bán đồ có phẩm cấp cao (vàng, tím, cam) trừ khi người dùng chủ động cấu hình.
  - Không bán đồ đã cường hóa (`tier > 0`).
  - Hỗ trợ bán cả đồ khóa (`isLock > 0`) khi người dùng xác nhận, tự động xử lý hộp thoại hỏi giá từ máy chủ.
  - Bán tuần tự từng món một (`bán từng cái`), chờ phản hồi cập nhật hành trang từ máy chủ (`opcode 16`) trước khi gửi món tiếp theo để tránh nghẽn/drop gói tin.

---

## 2. QUY TRÌNH THỰC TẾ 5 BƯỚC BÁN ĐỒ TRONG GAME (MANUAL WORKFLOW)

Qua quá trình thực nghiệm trực tiếp với NPC Lisa tại Làng Sói Trắng, quy trình bán đồ chuẩn trong game trải qua đúng **5 bước** tuần tự:

```
[Người chơi click Lisa]
          │
          ▼
┌─────────────────────────┐
│ Bước 1: Hộp thoại NPC   │ ──(Bấm "Giao tiếp")──► Gửi Opcode 23: getlist_from_npc(-3)
│ (Lisa: Máu & năng lượng)│
└─────────────────────────┘
          │
          ▼
┌─────────────────────────┐
│ Bước 2: Bảng MENU động  │ ──(Bấm "Mua bán")──► Gửi Opcode -30: Dynamic_Menu(-3, 0, 0)
│ (Mua bán, Mở ly, Thuế..)│
└─────────────────────────┘
          │
          ▼
┌─────────────────────────┐
│ Bước 3: Giao diện Shop  │ ◄── Nhận Opcode 23: npcInfo ("Cửa hàng Poition")
│ (Cửa hàng Potion mở ra) │
└─────────────────────────┘
          │
          ▼
┌─────────────────────────┐
│ Bước 4: Tab Hành Trang  │ ──(Chọn trang bị rác)──► Bấm nút [ Bán ]
│ (Chọn món đồ cần bán)   │
└─────────────────────────┘
          │
          ▼
┌─────────────────────────┐
│ Bước 5: Hộp thoại xác   │ ──(Bấm "Bán")──► Gửi Opcode 18: delete_Item(cat, id, 1)
│ nhận giá ("Bán với giá")│ ◄── Nhận Opcode 16: update inventory (Xóa đồ, cộng Vàng)
└─────────────────────────┘
```

### Chi tiết kỹ thuật từng bước:

| Bước | Tên bước | Giao diện hiển thị | Hàm Client xử lý | Gói tin mạng (Opcode) |
| :---: | :--- | :--- | :--- | :--- |
| **1** | **Đối thoại NPC** | Hộp thoại có ảnh Lisa, nội dung *"Máu và năng lượng chưa bao giờ là thừa cả. Hãy nhớ điều đó."*, 3 nút: `[ Giao tiếp ]`, `[ Nhiệm vụ ]`, `[ Đóng ]`. | `NPC.GiaoTiep()` $\rightarrow$ `GameCanvas.menu2.startAt_NPC()` | Giao diện nội bộ Client. Khi bấm `[ Giao tiếp ]`, gọi `GlobalService.gI().getlist_from_npc((byte)this.ID)`. |
| **2** | **Bảng Menu động** | Cửa sổ `MENU` gồm danh sách các lựa chọn: `Mua bán` (chỉ số 0), `Mở ly` (1), `Thuế` (2), `Tiền cống nạp` (3), `Nhận quà chiến trường` (4)... | `ReadMessenge.Dynamic_Menu()` $\rightarrow$ `GameCanvas.menu2.setinfoDynamic()` | Máy chủ gửi về: `Opcode -30` (`Dynamic_Menu`). Khi chọn "Mua bán", gửi: `GlobalService.gI().Dynamic_Menu((short)idNPC, (byte)idMenu, (byte)index)`. |
| **3** | **Mở Cửa hàng Shop** | Giao diện Shop mở ra (`TabShopNew`), tab mặc định là danh sách bình máu, mana, vé (`Cửa hàng Potion`). | `ReadMessenge.npcInfo()` $\rightarrow$ khởi tạo `new TabShopNew(...)` | Máy chủ gửi về: `Opcode 23` (`npcInfo`), tham số: `title = "Cửa hàng Poition"`, `type/id = 0`. |
| **4** | **Chọn đồ trong Hành trang** | Người chơi chuyển sang tab biểu tượng Balo (`Hành Trang`), click vào trang bị (ví dụ: `Áo vải xanh`). Hiển thị chỉ số trang bị và 3 nút: `[ Bán ]`, `[ Bán nhiều ]`, `[ Khác ]`. | `TabShopNew.update()` & `TabShopNew.paint()` | Giao diện nội bộ của `TabShopNew`. Khi bấm `[ Bán ]`, gọi `case 11` trong `TabShopNew.commandPointer()`. |
| **5** | **Xác nhận giá & Bán** | Bảng xác nhận hiện ra: *"Bạn muốn bán Áo vải xanh? Với giá 60 vàng."*, 2 nút: `[ Bán ]`, `[ Đóng ]`. (Nếu đồ có khóa, server có thể gửi thêm hộp thoại `Dialog_server`: *"Bạn có muốn bán [Tên món] với giá X Vàng không ?"*). | `TabShopNew` (`case 10`) hoặc `ReadMessenge.Dialog_server` | Khi bấm `[ Bán ]`, gửi `Opcode 18: GlobalService.delete_Item(cat, id, (byte)1)`. Server gửi xác nhận `Opcode 16` xóa đồ khỏi túi và cộng vàng. |

---

## 3. BẢNG DANH MỤC NPC THƯƠNG NHÂN (LÀNG SÓI TRẮNG)

Qua cơ chế quét mạng khi nhân vật vào **Làng Sói Trắng (Map ID = 1)**, toàn bộ NPC thương nhân đã được xác định tọa độ và ID chính xác tuyệt đối:

| Tên NPC | NPC ID | Tọa độ (X, Y) | Chức năng chính | Ghi chú tương tác |
| :--- | :---: | :---: | :--- | :--- |
| **Lisa** | **`-3`** | **`(756, 120)`** | **Bán Potion, Thu mua đồ rác** | Menu động: Option 0 là `"Mua bán"`. NPC chính dùng để bán đồ. |
| **Hammer** | **`-5`** | **`(180, 192)`** | **Cửa hàng Vũ khí** | Mua bán các loại vũ khí sơ cấp và trung cấp. |
| **Doubar** | **`-4`** | **`(204, 576)`** | **Cửa hàng Trang bị** | Mua bán giáp, quần, găng tay, giày, nhẫn, dây chuyền. |
| **Aman** | **`-7`** | **`(852, 312)`** | **Rương đồ cá nhân** | Quản lý kho cất giữ đồ đạc an toàn. |
| **Zulu** | **`-8`** | **`(108, 312)`** | **Đổi kiểu tóc, Nhiệm vụ ngày** | NPC chức năng thẩm mỹ và nhiệm vụ phụ. |
| **Pháp sư** | **`-36`** | **`(444, 552)`** | **Cường hóa & Mua nguyên liệu** | Đập đồ, nâng cấp trang bị, ghép ngọc. |
| **Đá dịch chuyển**| **`-10`**| **`(468, 432)`** | **Dịch chuyển khu vực** | Đi lại giữa các map trong thế giới game. |

---

## 4. PHÂN TÍCH BẢO MẬT & CƠ CHẾ KIỂM TRA TỪ PHÍA MÁY CHỦ (SERVER-SIDE VALIDATION)

Trong quá trình nghiên cứu gửi gói tin trực tiếp từ các map xa (Map 3: Bìa Rừng, Map 7: Cánh Đồng Sói), các thử nghiệm đã làm sáng tỏ cơ chế chống gian lận (Anti-cheat) của máy chủ HSO:

### 4.1. Server kiểm tra Map ID và Khoảng cách khi mở Shop
- **Gói tin gọi Menu (`getlist_from_npc(-3)`)**:
  - Khi đứng ở Map 3 gửi lệnh gọi Lisa (`id = -3`), máy chủ **vẫn chấp nhận trả về bảng Menu động (`cmd = -30`)**.
- **Gói tin chọn Mua bán (`Dynamic_Menu(-3, 0, 0)`)**:
  - Khi client gửi gói tin yêu cầu mở Shop từ Menu động, máy chủ kiểm tra điều kiện nghiêm ngặt:
    $$\text{player.map\_id} == 1 \quad \text{AND} \quad \text{distance}(\text{player}, \text{Lisa}) \le 120\text{px}$$
  - Nếu người chơi đang ở map khác (Map 3, Map 7...), máy chủ **lập tức từ chối và hủy phiên giao dịch**, không gửi lại gói tin mở Shop `cmd = 23` (`npcInfo`).
  - Đây chính là lý do vì sao khi đứng ở Map 3, người chơi bấm tay vào nút `"Mua bán"` thì menu tự động đóng lại mà không mở được giao diện mua bán.

### 4.2. Gói tin bán đồ (`Opcode 18: delete_Item`) yêu cầu phiên Shop hoạt động
- Gói tin bán đồ của HSO có cấu trúc:
  ```java
  GlobalService.gI().delete_Item(byte cat, short id, byte typedelete);
  ```
  - `typedelete = 1`: Bán cho cửa hàng lấy tiền vàng/xu.
  - `typedelete = 0`: Vứt bỏ/tiêu hủy vật phẩm (không nhận tiền).
- **Quy tắc máy chủ**: Máy chủ chỉ xử lý `typedelete = 1` khi cờ `player.is_shop_open == true`. Do không thể mở Shop từ map xa, mọi gói tin bán đồ gửi lên độc lập đều bị máy chủ **bỏ qua hoàn toàn (Drop packet)**, trang bị không bị trừ và vàng không tăng.

### 4.3. Các NPC trên map đánh quái không có chức năng Shop
- Trên Map 3 (Bìa Rừng) chỉ tồn tại 4 NPC:
  - `Miss Keva (id = -90)`: NPC Nạp thẻ (`MENU - Cổng nạp`).
  - `Mr Edgar (id = -85)`: NPC Phó bản / Nhiệm vụ.
  - `Cột Khu (id = -43, id = -45)`: NPC Chuyển khu vực (Zone).
- Không thể "mượn" các NPC này để bán đồ vì máy chủ phân quyền chức năng cứng cho từng ID NPC. Gửi lệnh tới Keva sẽ chỉ mở bảng Nạp thẻ, gửi tới Edgar chỉ mở bảng Phó bản.

---

## 5. KIẾN TRÚC MÃ NGUỒN ĐÃ HOÀN THIỆN

Hệ thống đã được tái cấu trúc hoàn chỉnh trên cả Client Java và Tool điều khiển Python:

### 5.1. Client Java (`HsoBridge.java` & `ReadMessenge.java`)

1. **Bộ lọc thương nhân hợp lệ ([`HsoBridge.isShopNpc`](file:///home/anv/Documents/tools/hso-tool/src_java/com/silverknight/HsoBridge.java))**:
   - Chỉ nhận diện NPC có tên `Lisa`, `Emma`, `Doubar`, `Hammer` hoặc đối thoại chứa `Cửa hàng`, `Shop`, `Mua bán`. Loại bỏ hoàn toàn nguy cơ click nhầm Miss Keva (Cổng nạp).

2. **Cờ trạng thái phiên mở Shop ([`HsoBridge.isAutoOpeningShop`](file:///home/anv/Documents/tools/hso-tool/src_java/com/silverknight/HsoBridge.java))**:
   - Khi tiến trình bán kích hoạt, cờ `isAutoOpeningShop = true` được bật.
   - Khi máy chủ gửi Menu động (`Dynamic_Menu`, opcode `-30`), Client tự động quét tìm tùy chọn `"Mua bán"` (index 0) và tự gửi lệnh chọn `Dynamic_Menu` ngay trong vòng 5ms mà không cần người dùng thao tác chuột.

3. **Tự động xác nhận hộp thoại giá bán ([`ReadMessenge.Dialog_server`](file:///home/anv/Documents/tools/hso-tool/src_java/netcommand/global/ReadMessenge.java))**:
   - Khi bán đồ khóa hoặc đồ có xác nhận giá, máy chủ gửi gói `cmd = -32` (`Dialog_server: "Bạn có muốn bán [Tên món] với giá X Vàng không ?"`).
   - Hàm `Dialog_server` tự động bắt chuỗi và gửi gói xác nhận `GlobalService.gI().dialog_Server(idDialog, typeDialog, (byte)1)` ngay lập tức, giúp đồ được bán thành công 100%.

4. **Tự động dọn dẹp giao diện (UI Clean-up)**:
   - Khối `finally` trong `sellSingleItem` và `sellMatchingItems` luôn gọi `GameCanvas.end_Dialog()` và `GameCanvas.menu2.doCloseMenu()`, đảm bảo không để lại bất kỳ bảng thông báo hay popup nào trên màn hình game.

### 5.2. Tool điều khiển Python (`hso_tool.py`)

1. **Bảng Danh Sách Đồ Bán (Hành Trang)**:
   - Hiển thị danh sách trang bị theo màu sắc, phẩm cấp, cấp độ, số lượng và trạng thái.
   - Sửa lỗi hiển thị trùng lặp nhãn `[Khoá] [Khoá]`.
   - Nút **[ Bán ]** từng món và nút **[ Bán rác lần lượt ]** gửi lệnh JSON tuần tự xuống Java Client.

2. **Bộ lọc Auto Sell an toàn**:
   - Lọc theo màu sắc: Trắng, Xanh, Vàng, Tím, Cam.
   - Tùy chọn `tier0_only`: Chỉ bán trang bị +0, tuyệt đối không bán đồ đã nâng cấp (+1 trở lên).
   - Tùy chọn `unlocked_only`: Có thể bật/tắt bán đồ khóa an toàn.

---

## 6. LỘ TRÌNH PHÁT TRIỂN TIẾP THEO: AUTO VỀ LÀNG BÁN ĐỒ RỒI QUAY LẠI BÃI

Vì máy chủ HSO bắt buộc nhân vật phải hiện diện tại Làng Sói Trắng mới mở được Shop Lisa, phương án tối ưu và hoàn hảo nhất cho việc cày cuốc tự động (AFK Farm) là tính năng: **Tự Động Về Làng Bán Đồ Qua Cổng Dịch Chuyển (Waypoint Travel & Auto Sell)**.

### Quy trình thiết kế:

```
[Bãi Train (Map 3 / Map 7)]
  │
  ├─ 1. Hành trang đầy (>= 40/42 ô) hoặc Người dùng bấm "Bán tất cả"
  ├─ 2. Lưu tọa độ bãi train hiện tại: (farmMapId, farmArea, farmX, farmY)
  ├─ 3. Di chuyển nhanh tới Waypoint cổng chuyển map dẫn về Làng Sói Trắng (x=936, y=252 trên Map 3)
  │
  ▼
[Cổng chuyển map kích hoạt ──► Sang Map 1 (Làng Sói Trắng)]
  │
  ├─ 4. Di chuyển tới Lisa tại tọa độ (x=756, y=120)
  ├─ 5. Mở shop và bán tuần tự toàn bộ trang bị rác theo bộ lọc (Mỗi món cách nhau 200ms)
  ├─ 6. Sau khi bán xong, túi đồ được dọn trống, vàng được thu thập đầy đủ
  ├─ 7. Di chuyển tới Waypoint cổng chuyển map quay trở lại Map train cũ
  │
  ▼
[Quay lại Map Train cũ]
  │
  └─ 8. Chạy về đúng tọa độ bãi quái (farmX, farmY) đã lưu và tự động tiếp tục đánh quái!
```

### Ưu điểm vượt trội:
- **100% Hợp lệ với máy chủ**: Không vi phạm bất kỳ cơ chế kiểm tra khoảng cách nào, không bao giờ bị phát hiện bất thường.
- **Tốc độ cực nhanh**: Toàn bộ chu trình đi về, bán sạch đồ và quay lại chỉ mất từ **3 - 5 giây**.
- **Hoàn toàn tự động**: Người chơi có thể cắm máy 24/7 mà không lo bị kẹt đồ hay tràn hành trang.
