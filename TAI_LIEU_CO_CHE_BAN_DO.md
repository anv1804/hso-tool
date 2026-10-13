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

## 4. PHÂN BIỆT CHẶN Ở CLIENT VÀ ĐIỀU KIỆN TRÊN SERVER

Phạm vi bản cập nhật: **bán bằng gói tin và vị trí ảo, giữ nguyên map/khu/tọa độ local**. Không có luồng chạy về làng, đi qua cổng, dùng đá dịch chuyển hoặc sửa `loadmap.idMap` để giả thành công.

### 4.1. Lỗi client đã sửa

- Luồng cũ gửi `delete_Item` sau đúng 300ms dù server chưa gửi menu/shop. Lượt bán mới theo dõi từng trạng thái và chờ phản hồi thực tế.
- Bán từng món từng tạo một thread, cho phép nhiều lượt đè vị trí và cờ `isAutoOpeningShop` lên nhau. Nay chỉ có một giao dịch hoạt động, chạy trong vòng cập nhật game.
- Các phương thức thông thường dùng chung `Cmd_Message.m`, dễ bị ghi đè nếu game loop và network collector gửi cùng lúc. Các gói bán từ xa dùng đối tượng `Message` độc lập.
- Vị trí ảo trước đây bị gói di chuyển thường ghi đè hoặc bị `objectMove` kéo nhân vật local đi. Nay chặn gói di chuyển thường trong giao dịch và bỏ qua phản hồi vị trí của chính nhân vật trong ngữ cảnh đó.
- Luồng cũ xác nhận mọi hộp thoại chứa câu “Bạn có muốn bán”. Nay chỉ xác nhận hộp thoại của món đang chờ bán trong giao dịch hiện tại. Đồ khóa cần quyền `allow_locked` từ xác nhận trên tool hoặc bộ lọc cho phép đồ khóa.
- Đếm nhặt/xóa đồ theo số ô là chưa đủ: nguyên liệu có thể chỉ giảm số lượng. Nay một phản hồi hành trang và số lượng giảm cũng xác nhận một lượt bán.

### 4.2. Những gì gói tin thực sự chứa

| Gói gửi | Payload |
| --- | --- |
| `4: player_move` | `short x`, `short y` |
| `23: getlist_from_npc` | `byte npcId` |
| `-30: Dynamic_Menu` | `short npcId`, `byte menuId`, `byte option` |
| `18: delete_Item` | `byte category`, `short itemId`, `byte 1` (bán lấy vàng) |
| `-32: dialog_Server` | `short dialogId`, `byte dialogType`, `byte 1` (xác nhận) |

Các gói gọi NPC/bán đồ **không có trường map ID hoặc tọa độ NPC**. Tạo NPC/modal giả hoặc sửa map ID trong bộ nhớ client không thay đổi map của nhân vật trên server. Không gửi thêm các byte ngoài cấu trúc giao thức để tránh làm sai gói tin.

Log thử nghiệm trước bản cập nhật có menu Lisa ở map xa nhưng không có phản hồi mở shop tương ứng, và item không bị trừ. Điều này cho thấy giao dịch chưa được server chấp nhận; log không chứng minh chính xác công thức khoảng cách hay tên biến kiểm tra nội bộ của server. Repo chỉ có client Java/C#, không có source server.

## 5. LUỒNG BÁN TỪ XA HIỆN TẠI

Mã điều phối: `src_java/com/silverknight/RemoteSellController.java`.

1. Chụp danh sách món theo bộ lọc hoặc món người dùng chọn, giữ ID/category/tên/số lượng. Một giao dịch chỉ xử lý danh sách này, không bán vô hạn các món mới nhặt.
2. Tạm ngưng gửi farm/loot/quét quái và auto gốc trong giao dịch; giữ cờ Auto Attack/Auto Loot để vòng lặp tiếp tục sau khi hoàn tất.
3. Thử đúng một gói bán trực tiếp tại bãi treo, `typedelete=1`. Chờ tối đa 4 giây.
4. Nếu chưa có xác nhận: thử NPC thương nhân thực sự có trên map trước; sau đó thử ID Lisa, Hammer, Doubar. Gửi tọa độ ảo theo bước tối đa 36px, mỗi 40ms, đứng cạnh NPC thay vì bên trong ô vật cản. Không gán tọa độ đó cho nhân vật local/camera. Không gửi tọa độ sát các cổng chuyển map hoặc ngoài biên map.
5. Gọi menu NPC, chờ tối đa 10 giây. Chọn đúng `menuId` và `option` server trả về với nhãn mua bán/shop; Hammer/Doubar hỗ trợ chọn đúng lớp nhân vật. Không mặc định chọn mục 0 của NPC khác.
6. Chờ tối đa 6,5 giây cho phản hồi shop `npcInfo`. Chỉ nhận các kiểu shop NPC 0, 1, 4, 100, không nhận rương/tóc/chợ người chơi. Shop được nhận ở nền, không mở màn hình shop che bãi farm.
7. Khi có phản hồi shop thật, kiểm tra lại hành trang/bộ lọc rồi gửi bán. Món còn trong túi mà server không xác nhận sẽ không được tính là bán thành công.
8. Với từng món: chờ phản hồi hành trang opcode 16 và kiểm tra item biến mất hoặc số lượng giảm. Nghỉ ít nhất 250ms trước lượt tiếp theo. Phản hồi bán trực tiếp đến muộn trong lúc mở shop được xử lý trước, tránh gửi lại món đã bán.
9. Khi xong, lỗi hoặc hủy: hoàn trả vị trí server về tọa độ local hiện tại bằng gói di chuyển; không sửa tọa độ local. Giao dịch tối đa 120 giây; việc hoàn trả có giới hạn riêng.
10. Nếu nhân vật đổi map/khu/login hoặc mất kết nối: hủy giao dịch, không gửi tọa độ cũ sang ngữ cảnh mới. Nếu người dùng tự di chuyển hoặc nhân vật chết: ngừng gửi bán.

Tất cả gói bán đều dùng `typedelete=1`. Không có fallback vứt đồ (`typedelete=0`) và không xóa item/cộng tiền giả ở client.

### Trạng thái trên HSO Tool

`client_state.json.remote_sell` chứa `active`, `phase`, `message`, `error`, `sold_items`, `sold_units`, `total_items`. Modal bán đồ hiển thị tiến độ, kết quả xác nhận và nút **Dừng bán**. Nút này dừng giao dịch và tắt Auto Sell; gói đã gửi trước khi hủy vẫn có thể được server xử lý.

Auto Sell thử theo chu kỳ 3 giây khi rảnh. Nếu giao dịch thất bại, khoảng cách thử tự động được tăng lên 60 giây để tránh spam. Người dùng vẫn có thể bấm bán lại chủ động.

### Khi server không cho bán xuyên map

Nếu bán trực tiếp không có xác nhận và tất cả lựa chọn vị trí ảo/menu không trả shop, trả lỗi `remote_shop_unavailable`. Nếu shop đã mở nhưng item không bị trừ sau thời gian chờ, trả `sale_not_confirmed`. Hiển thị kèm thông báo liên quan của server nếu có.

Sửa client không thể bảo đảm bỏ được kiểm tra map/quyền mua bán trên một server do bên khác quản lý. Bản này thực hiện các phương án của giao thức hiện có và báo đúng kết quả, không tự chuyển map. **Chưa xác nhận bán xuyên map trên server thật sau bản cập nhật**; cần mở client JAR mới để kiểm tra phản hồi thực tế.

## 6. KIỂM THỬ VÀ BUILD

Kết quả thử server thật tại map 7, map 3 và lượt đối chiếu ở map 1 được ghi trong [TAI_LIEU_KIEM_THU_BAN_TU_XA.md](TAI_LIEU_KIEM_THU_BAN_TU_XA.md). **Chưa có lượt bán xuyên map được xác nhận**; lượt bán ở map 1 trừ 22 Sắt vụn với giá server báo 0 vàng.

Chạy trên Windows:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/test_remote_sell.ps1
```

Script biên dịch toàn bộ client, kiểm thử bộ điều phối bằng transport mô phỏng, kiểm tra byte thực tế của các gói qua `GlobalService`, và kiểm thử IPC/tool Python. Không đăng nhập hoặc bán vật phẩm trên tài khoản thật.

Các trường hợp kiểm thử bao gồm: bán trực tiếp thành công, menu đến chậm, shop bị từ chối, chọn sai NPC, nguyên liệu giảm số lượng, bán nhiều món tuần tự, bấm lặp, xác nhận đúng món, đổi map/mất kết nối, hủy, ID vật phẩm thay đổi và lỗi gửi gói.

Sau khi sửa Java, build/repack bằng script của dự án hoặc nút Repack JAR. Client đang chạy không tự nạp class mới; cần đóng/mở game để dùng bản JAR đã cập nhật.

Nếu Windows khóa JAR vì game cũ đang mở, bản đã build được lưu ở `KnightOnline_v403.jar.pending`. Đóng cả game và tool cũ, mở lại `run_tool.bat` rồi bấm **Play Game**: tool tự áp dụng bản chờ trước khi khởi chạy. Không cần sao chép file bằng tay. Bản build thủ công có thời gian sửa mới hơn được ưu tiên hơn bản chờ.
