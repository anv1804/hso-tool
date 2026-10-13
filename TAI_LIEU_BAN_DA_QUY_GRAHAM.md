# Bán đá quý trong thú đi buôn qua Graham

Ghi nhận ngày 13/10/2026, giờ Việt Nam (UTC+7). Đây là giao dịch hàng của thú đi buôn, khác gói bán đồ hành trang opcode 18.

## Gói xác nhận người dùng cung cấp

```java
GlobalService.gI().dialog_Server((short)tesst, (byte)50, (byte)1);
```

`tesst` là ID dialog. Trong source client, hàm này tạo opcode **-32** và ghi:

| Offset payload | Trường | Kiểu | Giá trị |
| --- | --- | --- | --- |
| 0 | Dialog ID | short, 2 byte | ID do server cấp cho dialog |
| 2 | Loại thao tác/dialog | byte | **50** (`0x32`) theo thông tin người dùng |
| 3 | Lựa chọn xác nhận | byte | **1**, đồng ý |

Opcode -32 tương ứng byte `E0`; type 50 tương ứng byte `32`. Hai trường này khác nhau.

Gói xác nhận không mang ID Saphia, số lượng, giá, tọa độ, map hoặc NPC ID. Không coi đó là bằng chứng server không kiểm tra map/thú/vai trò/ngữ cảnh. Đặc biệt, không suy ra đây là bán riêng một viên: phạm vi hàng được xử lý còn phụ thuộc logic server.

## ID dialog phải lấy từ đâu?

`ReadMessenge.Dialog_server(Message)` đọc gói server -32 theo thứ tự:

```text
short idDialog
byte  typeDialog
UTF   nội dung hỏi xác nhận
```

Client lưu `idDialog` và `typeDialog` trong `GameCanvas.readMessenge`. Nút OK của dialog gọi `ReadMessenge.commandPointer(2,1)`, rồi gửi lại đúng bộ ID/type/value qua `GlobalService.dialog_Server(...)`.

Đã đối chiếu thêm lượt bán thật: dialog type **50** của lronic có ID **-6979**, trùng ID nhân vật của phiên đó; các dialog type 57/58 cùng phiên cũng dùng -6979. Phiên trước nhân vật có ID -8209. Vì vậy không giữ ID cứng qua lần đăng nhập/đổi tài khoản; ưu tiên lấy ID trực tiếp từ phản hồi server -32 của giao dịch.

Sự trùng ID giữa các loại dialog không chứng minh có thể bỏ qua ngữ cảnh server hoặc tự đổi loại 57 → 50 ở bất kỳ map nào. Với nhánh thử không nhận được dialog 50, dùng ID nhân vật hiện tại là giả thuyết có căn cứ từ lượt thật, vẫn cần phản hồi giao dịch để xác minh.

## Khác với chọn Bán đá quý trong menu

Chọn mục Bán đá quý của menu Graham hiện tại gửi:

```text
opcode -30
short NPC ID = -58
byte  menu ID = 0 trong menu đang lưu
byte  index   = 1
payload: FF C6 00 01
```

Đây là yêu cầu chọn mục. Nó không phải xác nhận bán `-32/type50`. Nếu server không phát dialog bán, client chưa đi đến bước OK/xác nhận theo luồng native.

Menu Graham đang có fallback dựng local. Việc thấy các nút hoặc log `Graham menu displayed locally` không chứng minh đã nhận menu/dialog bán mới từ server. Cache shop không xử lý giao dịch Bán đá quý.

## Trạng thái được kiểm tra trực tiếp

23:12:48, nhân vật **lronic** ở map 19 Cửa Biển, `(996,186)`, vai trò `typePk=11`, vàng 206.885:

- Có thú vận chuyển class `Monstervantieu`, ID **8286**, template **131** (bò trắng).
- Chủ thú là lronic, HP 400.000, tọa độ thú cũng `(996,186)`.
- Trang bị slot 11 có tên `Thương nhân tập sự [Khoá]`. Template ID client lúc audit là -1, nên không khẳng định một ID trang bị server cụ thể từ giá trị này.
- Graham không nằm trong danh sách NPC của map 19; Dylan có mặt.

Thông báo `Ta không thấy con vật buôn của ngươi` được audit trước đó khi nhân vật ở **map 24**, lúc 23:04:36. Không dùng nó để phủ nhận việc con bò hiện đang ở cạnh nhân vật hoặc để kết luận nguyên nhân của mọi lượt thất bại.

## Lượt thử type 50 ở Cửa Biển

23:14:51–23:15:01, cùng tài khoản/map/thú ở trên:

1. Đọc ID dialog đang lưu từ client: **-8209**, trùng ID nhân vật; loại dialog đang lưu là **57**, không phải 50.
2. Theo hướng thử do người dùng cung cấp, gửi một lần opcode -32 với ID đó, type 50, value 1. Dùng Message riêng qua `remoteSellConfirm()`; cấu trúc tương đương `dialog_Server(id,50,1)`.
3. Payload gửi: **`DF EF 32 01`**. Khung logic chưa mã hóa: **`E0 00 04 DF EF 32 01`**.
4. Trong cửa sổ 10 giây: không ghi nhận cập nhật hành trang xác nhận, không có gói exit ID con bò 8286, vàng **206.885 → 206.885**.
5. Handler được trả về delegate gốc. Không gửi tọa độ, đổi map/khu hoặc xác nhận dialog loại khác; không retry xác nhận bán.

**Lượt này chưa bán thành công.** Đối chiếu lượt thật sau đó xác nhận cách ánh xạ ID sang nhân vật, nhưng chưa xác minh ngữ cảnh server type 50 của lượt ở map 19. Không kết luận type 50 sai hoặc đã vượt điều kiện server.

Source diagnostic: [HsoGraham50Probe.java](E:/hso/tools/diagnostics/HsoGraham50Probe.java). Log: `E:\hso\hso_remote_sell_probe.log`, tiền tố `GRAHAM50`.

## Lượt người dùng bán thật tại Graham

Người dùng tự dắt thú về Thung Lũng Kỳ Bí, map 8, rồi chọn Bán đá quý và xác nhận. JVM đổi sang PID 22400 sau khi mở lại game; ID hiện tại của lronic là **-6979**.

- 23:23:54.683: server trả menu Graham thật, NPC -58, menu 0, thứ tự Mua lạc đà / Bán đá quý / Đồ thương nhân / Hướng dẫn thương nhân.
- 23:23:58.043: server trả opcode -32, **ID -6979, type 50**, nội dung **`Bạn có muốn bán tất cả với giá 20000 Vàng ?`**. Một dialog cùng nội dung/ID/type đã xuất hiện trước đó lúc 23:22:50.714.
- 23:24:00.025: log có opcode 16 sau thao tác xác nhận của người dùng.
- Snapshot sau lượt cho thấy vàng **226.885**, cao hơn 20.000 so với số dư **206.885** đã ghi trước khi người dùng về Graham. Không có capture thô payload 16 của lượt này; không trình bày giá trị trước/sau như một dump tiền đầy đủ trong chính gói đó.

Theo hàm native đã đối chiếu, gói OK tương ứng là `dialog_Server(-6979,50,1)`: payload **`E4 BD 32 01`**, opcode **-32**. Giá trị này suy ra từ dialog thật và source gửi; chưa có hook TX tại thời điểm người dùng bấm OK để coi nó là capture socket thô.

Nội dung server ghi rõ **bán tất cả**, không phải gói bán riêng một item/quantity. Theo cơ chế người dùng mô tả, bán hàng làm thú biến mất; khi kiểm thử phải theo dõi cả tiền và thú/cargo, không chỉ menu hiện ra.

## Mua tại Dylan, xác nhận bán tại chỗ — 23:32:37–23:32:48

Theo yêu cầu, mua thêm đúng một Saphia trên map 19 Cửa Biển rồi giữ nguyên vị trí `(972,198)` để thử bán về Graham. Nhân vật lronic, ID -6979, bò trắng template 131, ID **8299**, cùng chủ và ở gần nhân vật.

| Bước | Gói/giá trị | Kết quả |
| --- | --- | --- |
| Mở Dylan | 23: `C7`; chọn -30: `FF C7 02 00` | Nhận shop thật type 1 |
| Mua một Saphia | 24: `01 0E 06 00 01` | RX 16: 226.885 → 216.885; opcode 37 báo `Đã mua Saphia` |
| Gọi Graham ở map 19 | 23: `C6` | Không ghi nhận menu/dialog bán thật trong lượt |
| Chọn Bán đá quý | -30: `FF C6 00 01` | Chờ 300 ms, không nhận dialog 50 |
| Xác nhận theo ánh xạ ID đã xác minh | -32: `E4 BD 32 01` | Một lần, ngay sau gói chọn mục |
| Kết thúc kiểm tra | Chờ 10 giây sau xác nhận | Không có tiền cộng/cập nhật hành trang xác nhận; vẫn 216.885; bò 8299 còn |

Không đổi map/khu, không gán tọa độ local và không dựng dialog bán giả. Chỉ có **một lần mua** và một lần xác nhận bán trong nhánh này. Log kết thúc của bản v3 có dòng cũ `no ... dialog confirmations`; đó là nhãn còn sót từ chế độ mua trước đây, không phản ánh đúng nhánh mới. Các dòng TX phía trên chứng minh đã gửi -32; source đã sửa log kết thúc để ghi riêng số lần xác nhận bán.

Nhánh này chưa bán từ xa thành công dù đã dùng ID hiện tại đúng với lượt type 50 thật. Giả thuyết chỉ thiếu bước xác nhận ngay sau chọn menu chưa được xác nhận.

## Thử thêm vị trí ảo với type 50 — không mua thêm

Giữ nguyên Saphia của lượt mua 23:32 và bò 8299. Map server/client vẫn là map 19; không gửi đổi map/khu.

- 23:36:53: nhánh dùng tìm đường trên map 19 không tìm được đường đến vùng tọa độ Graham. Diagnostic dừng trước khi gửi gói bán. Không coi lỗi tìm đường này là phản hồi chặn của server.
- 23:38:25–23:38:44: thử packet-only, 44 bước opcode 4, mỗi bước tối đa 24px/60ms, từ `(972,198)` đến tọa độ `(150,804)` từng bán thật ở Graham trên map 8. Bỏ kiểm tra đường đi của diagnostic, vẫn kiểm tra biên và cổng đổi map ở mỗi bước.
- Tại endpoint gửi `23/C6`, `-30/FF C6 00 01`, rồi **`-32/E4 BD 32 01`** một lần. Không có dialog bán 50 thật được ghi nhận trước xác nhận.
- Không có cập nhật hành trang xác nhận hay tiền cộng trong 8 giây; vàng 216.885 → 216.885. Bò trên client vẫn quanh `(957,208)`, không đi cùng đến endpoint ảo.
- Trong lượt hoàn trả vị trí có phản hồi self-position `(245,734)`. Không có xác nhận độc lập rằng server đã chấp nhận đứng ở endpoint ảo hoặc tọa độ cuối; không lấy việc thiếu correction làm bằng chứng tọa độ được chấp nhận.
- Đã gửi 44 bước hoàn trả về `(972,198)`; cuối lượt local vẫn `(972,198)`, map 19/area 5, vàng 216.885. Handler và các cờ điều khiển/scan được trả lại. Không gán tọa độ local/camera.

Kết luận trong phạm vi các lượt này: mua ở Dylan thành công; bán tại Graham thành công khi người dùng tự dắt thú đến đó; chọn menu + xác nhận 50 từ Dylan, kể cả nhánh thử tọa độ ảo, **chưa bán thành công**. Chưa xác định đầy đủ điều kiện server: map/ngữ cảnh NPC, trạng thái dialog và vị trí server của nhân vật/thú đều còn là những yếu tố cần phân biệt. Đây không phải bản sửa giao dịch từ xa đã hoạt động.

## Phần đã sửa để không bỏ sót lý do từ server

`ReadMessenge.InfoServer_Download()` đã được bổ sung ghi nội dung opcode 37, loại và chuỗi phụ. Trước đó log thường chỉ hiện `RECV PKT: cmd=37`, nên không biết nội dung thông báo.

Source đã biên dịch và qua bộ kiểm tra hiện có. Bản chờ cập nhật log đã được áp dụng sau khi mở lại game; trên JVM 22400 đã ghi được opcode 37 với nội dung `Đã mua Saphia` ở lượt 23:32. Đây là sửa chẩn đoán, không phải bản sửa bán từ xa đã thành công.

Tài khoản này chơi máy chủ bên ngoài và chỉ có source client. Điều kiện server cụ thể vẫn cần phản hồi hoặc dữ liệu xác minh; không coi source server tham khảo là source live.

## Đối chiếu phản hồi chat NPC — 23:51 và 23:55 ngày 13/10/2026

Hai lượt kiểm tra phản hồi chat không phát sinh mua thêm, xác nhận bán hay di chuyển. Lượt 23:55 ghi được phản hồi 34 byte cho cả Dylan và Graham, giải mã thành lời thoại `Chúc bạn chơi game vui vẻ` và ID NPC tương ứng. Bộ đọc cũ gặp `EOFException` vì đọc ID trước lời thoại, trong khi phản hồi được quan sát chứa lời thoại trước ID.

Đã bổ sung `NpcChatPayload` để đọc định dạng quan sát được và giữ tương thích với định dạng cũ. `ReadMessenge.chat_npc()` sử dụng bộ đọc này và ghi lỗi phản hồi không hợp lệ. Bộ kiểm tra có 11 kiểm tra đã đạt, gồm dữ liệu capture thực tế, định dạng cũ, chuỗi rỗng, Modified UTF và dữ liệu bị cắt hoặc dư. Đây là sửa phần đọc phản hồi; thay đổi source chưa được đóng gói và áp dụng cho JVM đang chạy tại thời điểm ghi chú.

Log kết thúc lượt 23:55 ghi `fresh_type50=false`, không gửi xác nhận bán; số dư vẫn 216.885. Lời chào NPC không phải dialog bán, không chứng minh server đã mở giao dịch và không xác nhận có thể bán từ xa.

## Phạm vi kết luận

Luồng hiển thị menu, luồng đọc chat và giao dịch được server chấp nhận là các trạng thái riêng. Sửa bộ đọc hoặc dựng menu local không tự tạo được trạng thái giao dịch phía server. Các lượt đã ghi nhận chưa phân biệt dứt điểm điều kiện map/NPC, dialog đang chờ và vị trí server của thú; vì vậy không quy nguyên nhân chắc chắn cho một điều kiện cụ thể, cũng không kết luận rằng chỉ cần sửa một ID là thành công.

Chưa có bằng chứng về vòng mua tại một bên rồi bán bên kia ngay tại chỗ. Bản sửa chat không được mô tả là bản sửa mua bán từ xa. Phần chẩn đoán tiếp theo cần giới hạn ở đọc log và sửa lỗi client thông thường; không tiếp tục thử giả vị trí hay phát lại yêu cầu nhằm vượt điều kiện giao dịch của máy chủ bên ngoài.
