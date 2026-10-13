# Cách gọi menu NPC Graham trên Java/MicroEmulator

Ngày ghi nhận: 13/10/2026. Tài liệu mô tả đúng bản thử nghiệm đang có trong source và các lượt thử đã ghi log.

**Menu chính hiện được dựng cục bộ từ các nhãn cố định đã quan sát trên server. Menu shop có thể được mở từ dữ liệu lưu. Việc thấy menu/shop không có nghĩa server chấp nhận mua hoặc bán từ xa. Chưa xác nhận giao dịch ngoài map thành công.**

## 1. NPC và các lựa chọn

NPC được gọi là **Graham**, ID giao tiếp `-58`. Map đã nhận phản hồi shop thật là **Thung Lũng Kỳ Bí, map 8**. Menu gốc đã quan sát có `menuId = 0`:

| Index gửi đi | Nhãn menu |
| --- | --- |
| 0 | Mua lạc đà |
| 1 | Bán đá quý |
| 2 | Đồ thương nhân |
| 3 | Hướng dẫn thương nhân |

Các index bắt đầu từ 0. Không suy ra tác dụng trên server chỉ từ tên lựa chọn. Trong log live, lựa chọn 0 tại map 8 cũng trả shop có tiêu đề `Đồ thương nhân`.

## 2. Nút trên tool gọi như thế nào?

Nút **NPC Graham (Bán đá)** nằm dưới Auto Sell. Nó gọi `HsoStudioApp.open_graham_npc()` trong `hso_tool.py` và ghi lệnh vào hàng đợi `client_commands`:

```json
{"action":"test_npc_packet","npc_id":-58,"x":0,"y":0}
```

`write_command()` ghi file `.tmp` rồi đổi tên sang `.json` để Java đọc lệnh hoàn chỉnh. Python kiểm tra đã vào game, không có lượt bán đang chạy và giới hạn mỗi lần bấm cách nhau ít nhất 1 giây.

Cũng có thể mở cửa sổ nút riêng từ PowerShell:

```powershell
Set-Location -LiteralPath 'E:\hso'
python .\hso_tool.py --npc-graham
```

Cửa sổ riêng cần client đang chạy và trạng thái game mới trong `client_state.json` hoặc `client_state.json.tmp`. Nó dùng cùng lệnh với nút trong tool chính.

Trong `HsoBridge`, nhánh `test_npc_packet` nhận thấy NPC `-58` và tọa độ `(0,0)` thì thực hiện:

```java
if (GrahamNpcMenu.show()) {
    GlobalService.gI().remoteSellMenu(-58);
}
```

Đây là nhánh đặc biệt: **hiện menu local trước, rồi gửi yêu cầu menu thật**. Tọa độ 0/0 là dấu hiệu không gọi nhánh di chuyển ảo; không phải gửi nhân vật đến vị trí `(0,0)`. Nút này không gửi gói di chuyển, đổi map hay đổi khu.

## 3. Gói tin yêu cầu menu thật

`GlobalService.remoteSellMenu(int npcId)` tạo một `Message` riêng, tránh dùng lại message chung của các lệnh khác:

```java
Message packet = new Message((byte)23);
packet.writer().writeByte(-58);
sendRemoteSellPacket(packet);
```

| Thành phần | Giá trị |
| --- | --- |
| Opcode gửi | `23` (`0x17`) |
| Payload | Một byte NPC ID `-58` |
| Payload dạng hex | `C6` |

Các hex trong tài liệu là **payload trước bước đóng khung/mã hóa của session**, không phải toàn bộ dữ liệu trên socket. `writeShort` dùng thứ tự byte lớn trước (big-endian).

Nếu server trả menu động, opcode nhận là `-30`. `ReadMessenge.Dynamic_Menu(Message)` đọc theo thứ tự:

```text
short npcId
byte  menuId
byte  số lựa chọn
UTF   nhãn lựa chọn 0
UTF   nhãn lựa chọn 1
...   các nhãn còn lại
UTF   tiêu đề menu
```

Client gọi `GrahamNpcMenu.remember(...)` để cập nhật nhãn/menu ID từ phản hồi thật, rồi đưa menu vào `GameCanvas.menu2.setinfoDynamic(...)`. Nếu danh sách nhãn thay đổi, cache màn hình shop trong bộ nhớ được xóa.

## 4. Phần nào đang gọi cứng trên client?

Trong `GrahamNpcMenu.java`, giá trị khởi tạo là:

```java
NPC_ID = -58;
menuId = 0;
title = "Graham";
options = new String[] {
    "Mua lạc đà", "Bán đá quý", "Đồ thương nhân", "Hướng dẫn thương nhân"
};
```

`show()` tạo `iCommand` cho từng nhãn và gọi:

```java
GameCanvas.menu2.setinfoDynamic(commands, 2, menuId, NPC_ID, title);
```

Vì thế menu vẫn có thể hiện khi server không trả lời yêu cầu mở Graham. Đây là lý do đã thấy menu ở ngoài map 8 nhưng chọn mục không dẫn đến phản hồi shop thật. `show()` trả `true` chỉ xác nhận trạng thái menu client đang hiển thị.

## 5. Bấm lựa chọn gửi gì?

`Model/Menu2.java`, nhánh thao tác 2, gọi hàm gốc:

```java
GlobalService.gI().Dynamic_Menu((short)IdNpc, (byte)IdMenu, (byte)menuSelectedItem);
```

`GlobalService.Dynamic_Menu(...)` thử mở shop local qua `GrahamNpcMenu.showSubmenu(...)`, sau đó vẫn gửi gói thật:

```text
opcode: -30 (0xE2)
payload: short npcId + byte menuId + byte index
```

Ví dụ với NPC `-58`, menu `0`:

| Lựa chọn | Payload hex |
| --- | --- |
| Mua lạc đà, index 0 | `FF C6 00 00` |
| Bán đá quý, index 1 | `FF C6 00 01` |
| Đồ thương nhân, index 2 | `FF C6 00 02` |
| Hướng dẫn thương nhân, index 3 | `FF C6 00 03` |

Menu hiện tại giữ đúng NPC ID/menu ID để gửi lựa chọn. Tuy nhiên, việc gửi được gói không chứng minh server chấp nhận nó. Nút mở menu không tự chọn mục `Bán đá quý`.

## 6. Menu con và catalog đã lưu

Shop thật đi về bằng opcode `23`, được `ReadMessenge.npcInfo(Message)` xử lý. Với shop type 0 đã quan sát, phần dữ liệu dùng để lưu catalog gồm:

```text
UTF   "Đồ thương nhân"
byte  type = 0
short số lượng món
short itemId 0
short itemId 1
...   các item ID còn lại
```

`rememberMerchantShop()` chỉ giữ shop khi có lựa chọn đang chờ của đúng nhân vật, trong cửa sổ 10 giây. Nó lưu màn hình `GameCanvas.shopNpc` và catalog các item category 4 từ `vecShop`.

Hai file catalog, tính từ thư mục chạy JVM:

- `npc_shop_cache/graham-shop-camel-v2.bin`: lựa chọn Mua lạc đà.
- `npc_shop_cache/graham-shop-equipment-v2.bin`: lựa chọn Đồ thương nhân.

Lựa chọn Đồ thương nhân có fallback đọc file cũ `npc_shop_cache/graham-merchant-shop.bin` nếu chưa có file v2. **Catalog cũ không bảo đảm đúng riêng danh sách của lựa chọn 2**, do bản trước dùng một file chung; cần phản hồi thật để cập nhật.

Khi mở lại catalog, `restoreMerchantCatalog()` tạo `new Message((byte)23, bytes)` rồi gọi **cục bộ** `GameCanvas.readMessenge.npcInfo(...)`. Đây là tái dựng giao diện bằng parser client, không phải nhận một gói shop mới từ server và cũng không gửi catalog này lên server.

`showSubmenu()` chỉ áp dụng cache cho nhãn Mua lạc đà/Đồ thương nhân. Nó không mô phỏng kết quả Bán đá quý/Hướng dẫn thương nhân. Dữ liệu lưu không chứa tiền, hành trang hay phản hồi giao dịch; không tái phát opcode 16.

## 7. Gói mua và cách phân biệt thành công

Khi người dùng mua trên giao diện shop, `GlobalService.buy_item(...)` gửi:

```text
opcode: 24 (0x18)
payload: byte typebuy + short idbuy + short soluong
```

Ví dụ type 0, ID 82, số lượng 1: payload `00 00 52 00 01`. Gói này không mang tọa độ hay NPC ID; server có thể dùng trạng thái nhân vật/ngữ cảnh server để quyết định. Source hiện có chưa xác định được điều kiện cụ thể của server đang chơi.

Bằng chứng trong `hso_remote_sell_probe.log` ngày 13/10/2026:

| Thời điểm | Ghi nhận | Ý nghĩa |
| --- | --- | --- |
| 21:29:23, map 7 | `TX select npc=-58 menu=0 option=2 ... retained_shop_shown=true` | Shop local/cache được hiện; chưa phải phản hồi server |
| 21:30:04/05/08, map 7 | `TX purchase type=0 id=82 quantity=1` | Đã gửi gói mua thật; chưa có cập nhật xác nhận tương ứng trong lượt thử |
| 21:31:07–08, map 8 | TX option 0, rồi `RX shop title=Đồ thương nhân type=0` | Có phản hồi shop thật từ server |

Phải đối chiếu cập nhật hành trang/tiền từ server với đúng giao dịch và trạng thái trước/sau. Riêng log `TX`, `retained_shop_shown=true`, shop đang hiện hoặc một gói inventory không liên quan đều chưa đủ. Tiền thay đổi do hoạt động khác không được tính vào chức năng này.

## 8. Bản vá live và bản build

Có hai cách đưa logic này vào game:

1. **Source/JAR:** `HsoBridge`, `GlobalService` và `ReadMessenge` gọi trực tiếp `GrahamNpcMenu`. Build lại Java, đóng gói `.pending`, rồi tool áp dụng khi mở game lần tiếp theo.
2. **Agent live:** `HsoGrahamShopEntry` nạp `HsoGrahamShopAgent` vào JVM đang chạy. Agent bọc `GlobalService.session` để bắt gói gửi và `Session_ME.messageHandler` để quan sát gói nhận; nó vẫn chuyển tiếp đến session/handler gốc. Helper được nạp từ `patch_bin/remote_sell_game`.

Agent bắt TX 23/NPC -58 để hiện menu local, bắt TX -30/NPC -58 để thử mở shop cache, ghi log TX 24 và RX 23/16. Khi cài, agent còn thử giữ màn hình shop hiện có cho lựa chọn 0 và gửi lựa chọn 2; đó là hành vi riêng của bản chẩn đoán, không phải bước cần thiết của nút thông thường. Không coi việc cài agent là đã mở phiên mua bán hợp lệ trên server.

Agent live hết tác dụng khi JVM đóng. `HsoResetGrahamHooks` gỡ các proxy do agent Graham tạo và trả lại delegate gốc; nó không xóa logic đã build sẵn trong JAR. Tài liệu này không cài/gỡ agent và không thực hiện giao dịch mới.

Nếu cần tái build bản source hiện có:

```powershell
Set-Location -LiteralPath 'E:\hso'
.\tools\test_remote_sell.ps1
python .\tools\stage_remote_sell_jar.py
```

Kiểm thử offline/build thành công chỉ xác nhận code chạy theo kiểm tra đã viết, không xác nhận bán từ xa trên server. Bản `.pending` hiện chứa cả cơ chế menu/cache mô tả ở trên.

## 9. Các file cần đọc tiếp

| File trong `E:\hso` | Vai trò |
| --- | --- |
| `hso_tool.py` | Nút, IPC, cửa sổ `--npc-graham` |
| `src_java/com/silverknight/HsoBridge.java` | Nhận lệnh `test_npc_packet` |
| `src_java/com/silverknight/GrahamNpcMenu.java` | Nhãn cố định, menu native, catalog và mở lại shop |
| `src_java/Model/Menu2.java` | Chuyển thao tác menu thành lời gọi Dynamic_Menu |
| `src_java/netcommand/global/GlobalService.java` | Gửi opcode 23/-30/24 |
| `src_java/netcommand/global/ReadMessenge.java` | Parse menu và shop server trả |
| `tools/diagnostics/HsoGrahamShopAgent.java` | Hook/ghi log trên JVM đang chạy |
| `tools/diagnostics/HsoResetGrahamHooks.java` | Gỡ hook Graham live |
| `TAI_LIEU_KIEM_THU_BAN_TU_XA.md` | Kết quả các hướng đã thử và giới hạn kết luận |

Luồng hiện tại là: **bấm nút → dựng menu local + gửi yêu cầu 23 → bấm lựa chọn → thử hiện shop cache + gửi -30 → thao tác mua gửi 24 → chờ server xác nhận**. Phần còn thiếu là giao dịch được server chấp nhận khi nhân vật ở ngoài map phù hợp.
