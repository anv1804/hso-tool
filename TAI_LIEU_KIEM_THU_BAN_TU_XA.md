# Kiểm thử bán từ xa trên JVM đang chạy — 13/10/2026

## Kết quả

**Chưa xác nhận bán xuyên map thành công.** Đã thử trực tiếp trên Java/MicroEmulator ở Cánh Đồng Sói (map 7) và Bìa Rừng (map 3). Không có gói cập nhật hành trang xác nhận bán cho các nhánh bên dưới; vàng vẫn 14.092.

Một lượt ở Làng Sói Trắng (map 1, trạng thái nhân vật do người dùng cung cấp) được server chấp nhận. Đó là bán nguyên liệu giá 0, không phải bằng chứng bán xuyên map hoặc tăng vàng. Các diagnostic không gửi đổi map/khu, không chạy nhân vật về làng và không gán tọa độ local/camera.

## Các nhánh đã thử thật

| Nhánh | Phạm vi/kết quả |
| --- | --- |
| Opcode 18 trực tiếp | Sắt vụn không khóa ở map 7; Vòng bình an trắng có khóa ở map 3: chưa xác nhận |
| Menu không đổi vị trí | Lisa → Mua bán; Hammer/Doubar → cả bốn lớp nhân vật: có menu, không có shop |
| Nhảy tọa độ ảo một gói | Map 7: nhận opcode 4 kéo tọa độ server về `(408,792)` |
| Vị trí ảo nhiều bước | Đã thử 36px/40ms, 6px/40ms và đường đi tránh vật cản/cổng 24px/250ms; chưa xác nhận bán ở map 7/3 |
| Bán ngay tại vị trí ảo | Không đợi shop, trước/sau chọn menu Lisa/Hammer/Doubar: chưa xác nhận |
| Modal shop thật của client | Gọi `TabShopNew.commandPointer(10,0)` với đúng món trong hành trang, tại bãi và vị trí ảo: chưa xác nhận |
| Lấy thông tin NPC trước | Gửi opcode -44 rồi gọi menu/shop: không có shop được xác nhận |
| Kiểm tra tọa độ bằng char_info | Server không trả thông tin của chính nhân vật; không coi việc thiếu correction là bằng chứng đã đứng đúng vị trí trên server |
| Bán sau lượt đã được chấp nhận ở làng | Vòng bình an tại Bìa Rừng: chưa xác nhận |
| NPC thật trên Bìa Rừng | Miss Keva → Cống nạp; Mr Edgar → Báo thù/Mã quà tặng. Không có mục mua bán; thử bán tại vị trí ảo cạnh họ vẫn chưa xác nhận |
| Xác nhận thử nghiệm không đợi dialog | Gửi opcode 18 đúng chiếc vòng, rồi -32 với self ID, type -3, value 1: chưa xác nhận |

Nhánh xác nhận thử nghiệm chỉ nằm trong diagnostic, **không được đưa vào bộ bán tự động**. Bộ bán chính vẫn chỉ xác nhận dialog đúng món do server gửi.

## Phản hồi dương dùng để kiểm tra giao thức

Lượt 20:10:48–20:11:02 bắt đầu ở map 1, local `(528,270)`, 22 Sắt vụn, vàng 14.092:

1. Gửi tọa độ ảo đến điểm đứng `(228,600)` cạnh Doubar; không gán tọa độ local.
2. Gửi opcode 18, payload `07 00 01 01`.
3. Server trả opcode -32: `Bạn có muốn bán 22 Sắt vụn với giá 0 Vàng không ?`, dialog ID 5975, type -7.
4. Gửi xác nhận `17 57 F9 01`.
5. Server trả opcode 16; Sắt vụn giảm 22 → 0; vàng 14.092 → 14.092.
6. Hoàn trả tọa độ server; local cuối lượt vẫn `(528,270)`, map 1.

Như vậy opcode 18/-32/16 và cách gửi message hoạt động, kể cả xác nhận giá nguyên liệu. Không được lấy `priceItem` trong template mua làm giá server thu mua; stack nguyên liệu có thể bị bán toàn bộ với giá 0.

## Lượt cuối trên Bìa Rừng

20:21:33–20:21:40, player ID 7668, map 3/khu 0, local `(720,288)`:

- ID hiện tại của Vòng bình an là 26. Sau đăng nhập lại, ID cũ 27 đã thuộc Gậy gỗ; diagnostic phát hiện và không dùng ID cũ.
- Yêu cầu bán: opcode 18, payload `03 00 1A 01`.
- Gói xác nhận thử nghiệm: opcode -32, payload `1D F4 FD 01`.
- Không có xác nhận hành trang tương ứng; chiếc vòng còn nguyên; vàng 14.092.
- Diagnostic kết thúc, hoàn trả handler/cờ điều khiển; map/local vẫn giữ nguyên.

## Sửa client và kiểm thử offline

- NPCserver dùng `getIDnpc()`/`idImage` để giao tiếp, theo đúng hàm gốc.
- Hammer/Doubar chọn menu theo lớp nhân vật; không nhầm menu NPC khác thành cửa hàng.
- Chờ menu 10 giây, shop 6,5 giây để không bỏ qua menu đến chậm như trong trace.
- Thông báo timeout nói rõ chưa nhận xác nhận, không trình bày nó như lời từ chối trực tiếp của server.
- Script test dọn đúng thư mục output trong `patch_bin` và ưu tiên class game mới, tránh nạp class test cũ.
- Biên dịch toàn bộ client thành công: 81 kiểm tra controller, 8 kiểm tra packet, 15 kiểm tra parser/menu và 9 test Python đều đạt.
- Đã đóng gói 211 class vào `KnightOnline_v403.jar.pending`; ZIP được kiểm tra. Tool áp dụng bản chờ khi mở game lần tiếp theo.

Log chi tiết: `hso_remote_sell_probe.log`. Source diagnostic trong `tools/diagnostics`; không đóng gói chúng vào game JAR.

## Giới hạn kết luận

Repo chỉ có client Java/C#, không có code server. Chưa biết chính xác điều kiện server kiểm tra map/shop/khoảng cách. Menu NPC có thể được trả từ xa trong khi gói mở shop/bán bị bỏ qua.

Đã đối chiếu lịch sử Git, reflog và tree không gắn commit: chưa tìm thấy bản bán xuyên map được xác minh thành công. Điều này không bác bỏ lần chạy thành công trước đây của người dùng. Các kết quả trên chỉ mô tả những nhánh đã thử trong phiên này, không chứng minh mọi biến thể trên server đều không thể hoạt động.

## Các hướng bổ sung và nút Graham

- Lượt 20:41:47–20:41:55 thử mã xác nhận `-7` đúng như log đã quan sát, thay cho giả thuyết `-category`. Chiếc vòng còn nguyên, vàng 14.092.
- Lượt 20:43:15–20:45:05 thử thêm NPC -20, -21, -22, -77, -36, -42, -8, -57. **Shop thức ăn thú cưng (-42, option 1) mở thật từ xa**: server trả opcode 23, type 0, tiêu đề `Thức ăn thú cưng`. Bán chiếc vòng sau khi mở shop vẫn chưa có xác nhận. Vì vậy không thể kết luận mọi shop đều bị chặn từ xa.
- Lượt thử xử lý shop qua UI gốc và một bình HP lớn chưa thực hiện: client mất kết nối trước khi gửi gói; sau đó người dùng đổi tài khoản và chuyển yêu cầu sang thương nhân.
- Trên tài khoản `phuctien`, lượt 20:54:43–20:54:53 ở map 8/khu 5: -57 trả menu `Mua bán` nhưng chưa mở shop; -58 trả menu Graham gồm `Mua lạc đà`, `Bán đá quý`, `Đồ thương nhân`, `Hướng dẫn thương nhân`. Vàng giữ 86.265; chỉ mở menu.
- Nút **NPC Graham (Bán đá)** được thêm dưới Auto Sell trong tool. Phiên bản đầu chỉ gửi yêu cầu NPC -58 và không nhận menu tại map 7. Đã sửa bằng `GrahamNpcMenu`: hiện menu native từ các mục đã ghi nhận, cập nhật cache khi nhận menu mới từ server. Nút không tự chọn mục bán; thao tác chọn mục đi theo gói `Dynamic_Menu` gốc.
- Đã biên dịch/test cache menu và build lại `.pending`. Đồng thời nạp bản vá live trên JVM 8064, móc riêng yêu cầu mở Graham để hiện menu ở game đang chạy. JVM xác nhận `native menu visible=true`, NPC -58/menu 0. Bản vá không chặn gửi gói thật hay tự mua/bán/di chuyển.
- Người dùng đối chiếu: ở Thung Lũng Kỳ Bí (map 8), menu con trả shop `Đồ thương nhân` type 0; ngoài map đó không có phản hồi shop. Đã giữ lại đúng màn hình shop được server trả và mở lại cho mục `Đồ thương nhân`. Lượt 21:29:23 ở map 7 ghi `retained_shop_shown=true`, đồng thời vẫn gửi -30 NPC -58/menu 0/option 2. Chưa phát sinh gói mua 24 do agent tự gửi, chưa xác minh giao dịch mua/bán ngoài map.
- Danh sách shop type 0 được lưu dưới dạng catalog NPC gồm tiêu đề, loại shop và các ID potion; bản live đã lưu 33 byte từ bốn món thật trong màn hình server. Bản build mới phục hồi giao diện từ catalog khi khởi động lại, dùng hành trang hiện tại. Không tái phát gói cập nhật hành trang/vàng hay phản hồi thành công giả. Test catalog từ chối dữ liệu thiếu, quá lớn và loại shop khác.
- Lượt thử do người dùng thao tác ngoài map: 21:30:04/05/08 gửi mua type 0, ID 82, số lượng 1, nhưng không nhận cập nhật hành trang/vàng xác nhận trong cửa sổ thử. Không tính là mua thành công. Catalog được tách cho hai mục mở shop (0/2); mục `Bán đá quý` không được thay bằng phản hồi bán giả. Các lần người dùng tự đổi map/nhận vàng khác sau đó không được quy cho chức năng từ xa.

Các ID thương nhân bổ sung được đối chiếu với [MenuController của server HSO công khai](https://github.com/ALKAstraverse/hso-server/blob/main/src/main/java/core/MenuController.java). Bản tham khảo này có nhiều khác biệt với phản hồi live và không được coi là source của server đang chơi.
