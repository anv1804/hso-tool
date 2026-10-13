# Tài Liệu Kỹ Thuật & Hướng Dẫn Hệ Thống Auto - Hiệp Sĩ Online (HSO v403)
**Phiên bản: v1.0 | Ngày phát hành: 13/10/2026**

---

## 1. TỔNG QUAN HỆ THỐNG
Hệ thống **Auto HSO Tool (v1.0)** là giải pháp tự động hóa và hỗ trợ chơi game toàn diện dành cho phiên bản **Hiệp Sĩ Online (Knight Online v403)** trên nền tảng PC/Java (chạy qua giả lập MicroEmulator).

Hệ thống bao gồm 2 thành phần chính hoạt động song song và đồng bộ thời gian thực:
1. **Core Bridge Client J2ME (`com.silverknight.HsoBridge`)**: Lớp cầu nối mod trực tiếp vào mã nguồn Java của game, can thiệp vào vòng lặp logic (`GameCanvas.update()`), tầng mạng gửi/nhận gói tin (`GlobalService`, `ReadMessenge`), và đối tượng người chơi (`Player`).
2. **HSO Tool Studio GUI (`hso_tool.py`)**: Ứng dụng điều khiển đồ họa bằng Python/Tkinter thiết kế chuẩn phong cách pixel-art HSO (gỗ viền vàng kim), trang bị Radar quét toàn map, thanh máu quái 2 cột, bảng điều khiển tự động hóa và cơ chế bảo vệ an toàn tài khoản.

---

## 2. KIẾN TRÚC & GIAO THỨC TRUYỀN THÔNG (IPC)

```
+-------------------------------------------------------------------------+
|                       HSO Tool Studio (Python GUI)                      |
|  - Radar canvas toàn map            - Quản lý cấu hình Farm (profile)   |
|  - Danh sách quái 2 cột thời gian thực - Modal cấu hình Loot Filter        |
|  - Bảng điều khiển Auto / Safety     - Giám sát người lạ & Bot Captcha  |
+------------------------------------+------------------------------------+
                                     |
               Ghi lệnh JSON         | Đọc trạng thái JSON (20-30 FPS)
            (Atomic write POSIX)     | (Atomic write POSIX via .tmp)
                                     v
+-------------------------------------------------------------------------+
|             Tệp IPC: hso_cmd.json  <--->  client_state.json             |
+------------------------------------+------------------------------------+
                                     ^
                                     |
+------------------------------------+------------------------------------+
|                Core Bridge J2ME (com.silverknight.HsoBridge)            |
|  - Monster Scanner & Cache toàn map - Đồng bộ chu kỳ hồi sinh (Sync)    |
|  - Burst Loop 1-Hit toàn map        - Định thời hồi chiêu chuẩn 1.0s   |
|  - Đón đầu hồi sinh sớm 500ms      - Ghim mục tiêu & Tấn công từ xa    |
|  - Lọc đồ rơi thông minh            - Khóa chặt tự đánh nội tại        |
+------------------------------------+------------------------------------+
                                     |
                                     v
+-------------------------------------------------------------------------+
|                  Server Game Hiệp Sĩ Online (Socket TCP)               |
|            - Opcode 9: fire_monster (Tấn công quái)                    |
|            - Opcode 6: fire_Pk (Tấn công người chơi)                   |
|            - Opcode 1: player_move (Di chuyển)                         |
|            - Opcode 14: pick_item (Nhặt vật phẩm)                      |
+-------------------------------------------------------------------------+
```

### Cơ chế đảm bảo tính toàn vẹn dữ liệu IPC:
- **Ghi nguyên tử (Atomic Write)**: Client Java ghi ra tệp tạm `client_state.json.tmp` rồi thực hiện `renameTo` sang `client_state.json`. Nhờ đó tiến trình Python không bao giờ đọc phải tệp rỗng hoặc bị ngắt ngang.
- **Bộ nhớ đệm dự phòng (State Cache)**: Giao diện Python duy trì `self.cached_player` để khi chuyển map hoặc tải dữ liệu, các chỉ số HP, MP, EXP không bị nhấp nháy về 0.

---

## 3. CƠ CHẾ QUÉT QUÁI & RADAR TOÀN MAP (FULL MAP RADAR)

### 3.1. Quét toàn bộ quái trên map (Full Map Scanner)
- **Bối cảnh**: Client game gốc chỉ quản lý quái trong phạm vi tầm nhìn (~300px xung quanh nhân vật). Quái ngoài màn hình bị ẩn và không có dữ liệu máu.
- **Giải pháp**:
  - Khi bước vào map hoặc chuyển khu, `HsoBridge.ensureMonsterScanContext()` tự động kích hoạt tiến trình quét ngầm.
  - Quét theo từng mẻ 6 ID mỗi 25ms, truy vấn gói `GlobalService.gI().monster_info(id)`.
  - Hoàn tất lập bản đồ toàn bộ 30 - 60 quái trên map chỉ trong **1.5 - 2.0 giây**.
  - Lưu vào bảng ghi `MonsterRecord` gồm: Tọa độ gốc `(x, y)`, tên loại quái, máu tối đa (`maxHp`), máu hiện tại (`hp`), và **chu kỳ hồi sinh gốc từ server (`respawnIntervalMs` / `timeReveice`)**.

### 3.2. Giữ tươi dữ liệu thời gian thực (Realtime Refresh)
- Vòng lặp `HsoBridge.advanceMonsterRefresh(now)` chạy mỗi 60ms, round-robin truy vấn 4 quái để cập nhật liên tục tọa độ và máu quái.
- Tích hợp hook trong `ReadMessenge.java`:
  - Khi quái di chuyển: `updateMonsterPosition(id, x, y)` cập nhật ngay lập tức lên Radar.
  - Khi quái hồi sinh: `onMonsterRespawned(id)` phục hồi `hp = maxHp` tức thì mà không cần đợi gói tin phụ.

### 3.3. Giao diện Radar & Danh sách Đa Thẻ (Quái / Người chơi / NPC)
- **Radar trực quan toàn diện (Quét cả NPC & Người & Quái)**:
  - Chấm đỏ (`●`): Quái vật đang sống.
  - Chấm xám (`●`): Quái vật đã chết (đang đếm ngược hồi sinh).
  - Vòng tròn trắng viền ngoài: Quái vật hoặc đối tượng đang được ghim mục tiêu.
  - Ô vuông xanh lam ngọc (`■`): **NPC nhân vật & chức năng** (Aman, Lisa, Hammer, Doubar, Zoro, v.v.).
  - Tam giác cam (`▲`): Cột chuyển khu vực (Khu).
  - Hình thoi xanh ngọc (`◆`): Đá dịch chuyển (Teleport stone).
  - Hình thoi tím (`◆`): Cổng chuyển map, lối đi.
  - Chấm vàng (`●`): Người chơi khác trong khu.
  - Chấm xanh lá (`●`): Bản thân nhân vật.
  - **HUD Tooltip tương tác khi rê chuột (Hover Tooltip)**: Rê chuột tới bất kỳ điểm nào trên Radar sẽ hiện vòng tròn chỉ điểm kèm nhãn thông tin chi tiết (ví dụ: `[NPC] Aman`, `[Cổng] Khu mỏ`, `[Người] tramanh356 (Lv 34)`, `[Quái] #9 Chuột`).
- **Hệ thống 3 Thẻ Danh Sách (MON / PL / NPC)**:
  - **MON**: Danh sách quái 2 cột (Grid View) hiển thị song song tới 60-90 quái, thanh máu mini, số HP rút gọn và đếm ngược hồi sinh mượt mà 0.1s.
  - **PL**: Danh sách người chơi trong khu kèm Level, thanh máu HP và khoảng cách.
  - **NPC**: Danh sách toàn bộ NPC trên bản đồ sắp xếp theo khoảng cách từ gần đến xa kèm cự ly chính xác tính theo pixel.

---

## 4. CƠ CHẾ AUTO FARM 1-HIT TOÀN MAP (FULL MAP FARM BURST)

### 4.1. Đặc tính giao thức Server HSO & Cơ chế Ra Đòn Đồng Bộ Tuyệt Đối (Zero-Desync Burst)
- Gói tin tấn công `fire_monster(mVector targets, byte typekill)` gửi opcode 9 lên server.
- **Nguyên nhân cốt lõi gây hiện tượng lệch máu (con về 732, con về 1.2k, con bị delay máu)**:
  1. *Lỗi gửi 2 vòng lặp (Double Hit) cùng lúc*: Server HSO áp dụng cooldown nhận đòn cho từng quái. Khi client gửi 2 vòng lặp liên tiếp trong 0.05ms (40 gói tin), server bị quá tải socket và DROP gói thứ 2 của hầu hết các quái, chỉ tình cờ lọt qua 1 con (như con #28) nên con đó bị trừ 2 hit (732) còn các con khác chỉ trừ 1 hit (1.2k).
  2. *Lỗi race condition từ monster_info round-robin*: Vòng lặp spam 4 gói `monster_info` mỗi 60ms làm nghẽn socket TCP; gói tin `monster_info` gửi trước đòn đánh bay về sau mang giá trị máu cũ 1700 ghi đè làm thanh máu 2 con bị dài ra, khi dừng auto hàng đợi mới thông thoáng và máu mới tụt xuống.
- **Giải pháp Tối Ưu Tuyệt Đối**:
  1. *Gửi chính xác đúng 1 hit duy nhất cho mỗi quái trong mỗi đợt*: Mỗi chu kỳ 1.0s chỉ gửi đúng 1 gói tin `fire_monster` cho mỗi con quái trong danh sách. Socket truyền cực nhẹ, 100% quái cùng nhận 1 hit, máu cùng tụt đều 100% về 1.2k (không con nào bị rơi về 732).
  2. *Tạm dừng round-robin monster_info khi đang Auto Farm*: Loại bỏ hoàn toàn bão gói tin gây nghẽn socket và triệt tiêu race condition. Máu quái được server tự động cập nhật chuẩn xác từng mili-giây qua gói sát thương `updateMonsterHealth`.
  3. *Chặn gói tin trễ ghi đè tăng máu quái sống*: Trong `rememberMonsterInfo` và `rememberLocalMonster`, tuyệt đối không ghi đè tăng máu nếu quái đang sống đã bị trừ sát thương.
  4. *Đồng bộ hai chiều tức thời*: `updateMonsterHealth` cập nhật đồng thời cả bảng ghi `record.hp` lẫn đối tượng `localObj.hp` trong tầm nhìn client, thanh máu trên Tool và trong game đồng bộ tức thời.

### 4.2. Tối ưu nhịp đánh chuẩn 1.08 giây (Chống Drop gói tin & Đảm bảo đồng bộ 100%)
- Chiêu đánh thường cơ bản của tất cả các phái có **thời gian hồi gốc trên server là 1s (1000ms)**.
- Khi đánh quái cần 2-3 đợt đánh mới chết:
  - Hàm `getNormalAttackCooldownMs()` đọc thời gian hồi thực tế từ `skill.mLvSkill[lv].getdelay()`.
  - Tự động cộng thêm **khoảng đệm an toàn 80ms** (`attackCooldown = 1080ms`):
    - Khắc phục triệt để hiện tượng gói tin bay tới server ở mốc 950-980ms bị server anticheat drop do chưa hết 1.000ms hồi chiêu.
    - Đảm bảo 100% gói tin của TẤT CẢ các con quái trong danh sách đều vượt qua mốc 1.000ms trên server, không sót bất kỳ con nào.
    - Mọi con quái cùng nhận sát thương ở mọi chu kỳ, thanh máu tụt đều tăm tắp và chết cùng một lúc.

### 4.3. Cơ chế đồng bộ nhịp hồi sinh an toàn (+100ms buffer)
- Khi **toàn bộ quái được chọn đã chết** (`aliveCount == 0`):
  - Hệ thống tính toán thời điểm con quái chết **MUỘN NHẤT** hồi sinh (`latestDeadRespawnDue`).
  - Chờ đến mốc quái đã chắc chắn hồi sinh xong trên server (`now >= latestDeadRespawnDue + 100L`).
  - Phóng gói tin Hit 1 đồng loạt cho toàn bộ quái trong danh sách.
  - Ngăn chặn hoàn toàn việc đánh sớm trước khi quái kịp xuất hiện trên server (tránh việc con sinh sớm thì nhận đòn còn con sinh muộn bị hụt đòn gây lệch máu).

### 4.4. Thuật toán Đồng bộ Pha Hội tụ (Phase Sync Convergence / Hold Fire)
- Khi trong map có sự lệch pha (một số quái sống và một số quái đang đếm ngược hồi sinh):
  1. **Nếu có quái sống đang bị thương dở dang** (`hp < maxHp`): Hệ thống đánh tiếp các con đang bị thương để dứt điểm chúng, đưa toàn bộ đàn về trạng thái CHẾT.
  2. **Nếu các quái sống đều full máu (mới hồi sinh sớm)** trong khi các quái khác vẫn đang đếm ngược hồi sinh:
     - Hệ thống kích hoạt trạng thái **HOLD FIRE (Giữ nhịp)**: Tuyệt đối không đánh lẻ tẻ con vừa hồi sinh sớm.
     - Chờ cho đến khi con chết muộn nhất hồi sinh hoàn toàn trên server (`+100ms`).
     - Lúc này 100% đàn quái đều đã hồi sinh, hệ thống tung đòn gom toàn bộ quái trong map vào **đúng 1 nhịp đánh duy nhất**.
### 4.5. Nút chuyển đổi Chế độ Đồng bộ Farm (Sync / Free Farm Mode)
- Nút chuyển đổi tích hợp trực tiếp trên thanh công cụ của modal Auto Fam:
  - **🔄 ĐỒNG BỘ: BẬT (Mặc định)**:
    - Kích hoạt thuật toán **Hold Fire & Hội tụ**: Hệ thống sẽ kiên nhẫn đợi cả đàn quái hồi sinh hoặc dứt điểm các con quái dở dang rồi gom toàn bộ vào đúng 1 nhịp đánh.
    - Đảm bảo 100% quái cùng nhận đòn, cùng tụt máu đều, chết cùng 1 giây và hồi sinh cùng 1 giây.
  - **⚡ TỰ DO: RA LÀ HIT (TẮT Đồng bộ)**:
    - Bỏ qua toàn bộ cơ chế Hold Fire và không đợi con chết muộn nhất.
    - Cứ con nào trong danh sách chọn còn sống hoặc vừa hồi sinh sớm là hệ thống **HIT NGAY LẬP TỨC**.
    - Phù hợp với người chơi thích farm liên tục, tối đa hóa sát thương dồn mà không cần chờ đợi chu kỳ hội tụ của cả bầy quái.

### 4.6. Quản lý cấu hình Farm theo từng Map
- Cấu hình farm được lưu trữ theo hồ sơ từng bản đồ (`map_id`).
- Khi người chơi di chuyển sang map khác chưa có cấu hình: Auto Farm tự động tạm dừng để tránh đánh nhầm.
- Khi quay trở lại map đã cấu hình: Hệ thống tự động nhận diện và tiếp tục farm.

### 4.7. Cơ chế Đếm Ngược & Thời Gian Hồi Sinh Quái Chuẩn Xác (Respawn Countdown & 0.1s Tick)
- **Thời gian hồi sinh quái thực tế trong HSO**:
  - Các loại quái thường (Chuột, Lửa, Nấm, Sói, Nhện, Bọ cạp...) có thời gian hồi sinh chuẩn là **5.0 giây** (`5000ms`).
  - *Khắc phục triệt để lỗi kẹt 1.8s*: Trước đây hệ thống tự đo đạc nhầm khoảng thời gian hiệu ứng bay chết (`startDeadFly` ~1.5s-1.8s) hoặc gói tin delay từ server, dẫn tới việc ghi đè thời gian hồi sinh thành 1.8s. Hiện tại giá trị mặc định được cố định chuẩn 5.0s, bỏ qua giá trị rác sentinel 1500 của client gốc, và chỉ hiệu chỉnh khi nhận gói tạo quái mới thực tế (`by == 1`).
  - *Triệt tiêu vòng lặp reset thời gian chết*: Client không tự tiện gán quái hồi sinh giả trong `advanceMonsterRefresh`, loại bỏ hoàn toàn hiện tượng reset `death_time` lặp lại làm đồng hồ nhảy giật lùi về 1.8s.
- **Bước nhảy đếm ngược mượt mà 0.1 giây (Smooth 100ms Tick)**:
  - Bổ sung luồng `countdown_tick_loop` chạy chu kỳ độc lập **100ms** (0.1 giây) trên giao diện Tool Python.
  - Đồng hồ đếm ngược trên bảng quái và modal Auto Fam giảm đều đặn mượt mà đúng từng **0.1s** (`5.0s -> 4.9s -> 4.8s -> ... -> 0.1s -> 0.0s`), triệt tiêu hoàn toàn hiện tượng giật cục 0.4s.
  - Khi đếm ngược về 0s mà server chưa gửi gói spawn, hiển thị trạng thái chờ `0.0s` màu xanh lá cây sẵn sàng.
- **Hỗ trợ nhấp đúp quái trong Game**:
  - Nhấp 1 lần vào quái: Hiện con trỏ chỉ điểm (`paintFocus`) trên đầu quái.
  - Nhấp đúp (Double-click): Nhân vật tự động khóa mục tiêu và lao vào tấn công quái ngay lập tức.

---

## 5. GHIM MỤC TIÊU & TẤN CÔNG TỪ XA (REMOTE STRIKE & TARGET PINNING)

### 5.1. Ghim Người chơi & PK từ xa
- Tab **"PLAYER"** trên Tool hiển thị danh sách người chơi trong map kèm Level, HP và cự ly.
- Click chọn người chơi -> Kích hoạt trạng thái **Ghim mục tiêu PK** (`pinnedPlayerId`).
- **HUD Banner trong Game**:
  - Hiển thị bảng ghim bằng gỗ viền vàng ở mép trên màn hình game.
  - Thể hiện: Tên người chơi, Level, trạng thái Đồ Sát, thanh máu đỏ thời gian thực, cự ly và mũi tên chỉ hướng ngoài màn hình.
- **Tấn công từ xa mượt mà (Remote Strike)**:
  - Gửi chuỗi di chuyển ảo nội suy 36px qua mặt anticheat server.
  - Gửi gói tin tấn công `fire_Pk` (opcode 6).
  - Tọa độ cục bộ của nhân vật và camera hoàn toàn cố định, không giật màn hình, không bị server giật lùi (rubberband).
  - Tự động bật cờ Đồ Sát (`set_Pk(0)`) nếu chưa bật.

### 5.2. Ghim Quái vật toàn map
- Hỗ trợ ghim bất kỳ quái nào trên map trực tiếp từ Radar hoặc Tab Quái 2 cột.
- Cơ chế tấn công quái ghim gửi trực tiếp gói tin `fire_monster` tới ID quái mục tiêu, xử lý đồng nhất cho cả quái ở cự ly gần lẫn quái ở xa ngoài tầm mắt.

---

## 6. AUTO NHẶT ĐỒ & BỘ LỌC ĐỒ RƠI (AUTO LOOT & LOOT FILTER)

### 6.1. Giao diện Modal Bộ Lọc (Loot Filter Modal)
- Nút `[ ⚙ ]` cạnh nút Auto Loot mở hộp thoại cấu hình trực quan, phân loại theo 3 chuyên mục:
  1. **Trang bị theo phẩm cấp / màu sắc**:
     - ⚪ Đồ Trắng (Thường)
     - 🔵 Đồ Xanh (Đặc biệt)
     - 🟡 Đồ Vàng (Hiếm)
     - 🟣 Đồ Tím (Huyền bí)
     - 🟠 Đồ Cam (Sử thi)
     - 🟢 Đồ Xanh lá (Hoàn mỹ)
  2. **Hồi phục & Tiền tệ**:
     - 🩸 Bình Máu (HP)
     - 💧 Bình Mana (MP)
     - 💰 Tiền Vàng (Gold / Xu)
  3. **Vật phẩm khác**:
     - 💎 Nguyên liệu & Đá cường hóa
     - 📜 Vật phẩm Nhiệm vụ
- Nút chức năng: `[ Chọn tất cả ]`, `[ Bỏ chọn hết ]`, hiển thị bộ đếm số loại vật phẩm đang chọn.

### 6.2. Thuật toán lọc nhặt đồ trong Java (`HsoBridge.shouldPickupItem`)
- Kiểm tra loại đối tượng rơi trên map (`MainObject.typeObject`):
  - `typeObject == 3`: Trang bị (đối chiếu mã màu `colorName` tương ứng).
  - `typeObject == 4`: Bình máu, bình mana, vàng.
  - `typeObject == 5`: Vật phẩm nhiệm vụ.
  - `typeObject == 7`: Đá đập đồ, ngọc, nguyên liệu chế tạo.
- Tự động gửi gói tin nhặt đồ `GlobalService.gI().pick_item(item.ID)` ngay khi vật phẩm rơi thỏa mãn bộ lọc.

---

## 7. TỰ ĐỘNG BÁN ĐỒ TỪ XA (REMOTE AUTO SELL & COIN HARVEST)

### 7.1. Luồng giao dịch bán từ xa
- Bán lấy vàng dùng opcode 18, `delete_Item(category, id, 1)`; không dùng kiểu vứt đồ 0.
- `RemoteSellController` điều phối một giao dịch trong game loop: thử bán trực tiếp; nếu chưa xác nhận thì thử vị trí ảo và chuỗi menu NPC -> lựa chọn mua bán -> phản hồi shop -> bán tuần tự.
- Giữ nguyên map và vị trí local/camera; không chạy về làng hoặc dùng cổng dịch chuyển.
- Chỉ tính thành công khi có phản hồi hành trang từ server và món bị trừ/xóa. Menu hoặc modal cục bộ không chứng minh server đã mở shop.
- Điều kiện map/quyền mua bán do server quyết định. Bản client mới có kiểm thử mô phỏng, chưa xác nhận bán xuyên map trên server thật.
- Xem `TAI_LIEU_CO_CHE_BAN_DO.md` để biết các trạng thái, timeout, gói tin và giới hạn thực tế.

### 7.2. Giao diện & Modal cấu hình Auto Bán Đồ
- Nằm ngay dưới hàng Auto Loot trên Right Sidebar:
  - **Nút Toggle**: `Auto Sell: OFF / ON` (bật/tắt tự động quét bán nền mỗi 3 giây).
  - **Nút ⚙**: Mở Modal cấu hình chi tiết `CẤU HÌNH AUTO BÁN ĐỒ - THU VÀNG`.
- **Thanh trạng thái thời gian thực trong Modal**:
  - `🎒 Túi: {inven_count}/{inven_max} ô  |  💰 Vàng: {coin:,} xu`
  - `Đang chọn bán: {match_count} món (Trang bị: {match_equip}, NL: {match_mat})`
  - Cập nhật số liệu động mỗi 0.5s theo thời gian thực.
- **Phân loại trang bị & màu sắc**:
  - ⚪ Đồ Trắng (Thường)
  - 🔵 Đồ Xanh (Đặc biệt)
  - 🟡 Đồ Vàng (Hiếm)
  - 🟣 Đồ Tím (Huyền bí)
  - 🟠 Đồ Cam (Sử thi)
  - 🟢 Đồ Xanh lá (Hoàn mỹ)
- **Tùy chọn bảo vệ an toàn tuyệt đối**:
  - 🔒 **Chỉ bán đồ +0 (`tier0_only = True`)**: Bảo vệ 100% trang bị đã cường hóa (+1, +2, +3,...), không bao giờ bán nhầm đồ đang nâng cấp.
  - 🛡️ **Không bán đồ có khóa (`unlocked_only = True`)**: Không bao giờ bán trang bị hoặc vật phẩm có biểu tượng khóa bảo hiểm (`item.isLock > 0`).
  - Chỉ chọn trang bị và nguyên liệu; server quyết định món có bán được hay không. Không coi `canSell` bằng 0 trong snapshot cũ là bằng chứng đủ để chặn bán ở client.
- **Nút thao tác nhanh**:
  - `[ Đồ rác (Trắng+Xanh) ]`: Tự động tick chọn đồ trắng và xanh, bỏ chọn các phẩm cấp quý hơn.
  - `[ 💰 BÁN NGAY ]`: Lập tức quét túi và gửi chuỗi gói tin bán toàn bộ các món đồ thỏa mãn bộ lọc hiện tại (chờ xác nhận từng món, sau đó nghỉ tối thiểu 250ms) mà không cần bật Auto Sell liên tục.
  - `[ LƯU CẤU HÌNH ]`: Lưu cấu hình và đồng bộ xuống client.

---

## 8. CƠ CHẾ AN TOÀN BẢO VỆ TÀI KHOẢN (DUAL-LAYER SAFETY)

Để phòng tránh tuyệt đối nguy cơ bị người chơi khác quay clip tố cáo hoặc bị GM kiểm tra:

1. **Giám sát sự hiện diện của người chơi lạ**:
   - Khi khu vực có người chơi khác (`other_count > 0`):
     - Hệ thống **ngay lập tức tạm dừng Auto Farm gom quái toàn map** và tắt Auto Attack.
     - Giữ nguyên cấu hình farm trong bộ nhớ (không xóa).
     - Banner thông báo chuyển sang màu cam/đỏ cảnh báo.
   - Khi người lạ rời khỏi khu vực hoặc khu trở về an toàn (0 người):
     - Hệ thống **tự động kích hoạt lại (Auto-Resume)** Auto Farm và Auto Attack mà không cần thao tác thủ công.
2. **Cảnh báo Bot Captcha**:
   - Tự động nhận diện thực thể Bot Captcha chống auto (`typeBoss == 2`).
   - Phát cảnh báo khẩn cấp cả trên giao diện Tool lẫn trong game để người chơi can thiệp kịp thời.
3. **Tương thích hoàn hảo giữa Thao tác thủ công và Auto (Manual Control & Lockout)**:
   - **Thao tác thủ công (Click / Double Click trong Game)**:
     - Khi người chơi click vào quái trong game: `GameScreen.ObjFocus` nhận mục tiêu và hiển thị con trỏ mũi tên trên đầu quái (`paintFocus`) bình thường.
     - Khi double click vào quái: Client tự động kích hoạt `isCurAutoFire = true` và nạp phím đánh ảo để nhân vật tự động chiến đấu liên tục vào quái đó.
   - **Khi tắt Auto Attack trên Tool (`stopAllAutoAttack`)**:
     - Lệnh chỉ ngắt chu trình tự tìm quái một lần duy nhất tại thời điểm tắt; không can thiệp vòng lặp vĩnh viễn mỗi frame, cho phép người chơi tự do click quái và đánh tay bình thường.
   - **Khi Auto Farm đang hoạt động (`isFarmAttackActive`)**:
     - Tự động bỏ qua việc auto-search quái ngẫu nhiên khi `ObjFocus == null` để giữ vững vị trí và tập trung toàn lực vào các đòn đánh chớp nhoáng (Burst Loop) cho đàn quái đã chọn.
4. **Giãn cách bật/tắt Đồ Sát (180 giây)**:
   - Nút Đồ Sát tích hợp bộ đếm lùi 180s thời gian thực, ngăn việc spam lệnh vi phạm quy định game.

---

## 9. BẢNG PHÍM TẮT & LỆNH ĐIỀU KHIỂN

### 9.1. Điều khiển trên HSO Tool Studio:
| Thao tác / Nút | Chức năng |
| :--- | :--- |
| **Auto Attack (BẬT/TẮT)** | Bật hoặc tắt chế độ tự động đánh |
| **Nút ⚙ (cạnh Auto Attack)** | Mở hộp thoại cấu hình chọn quái cần Farm |
| **Auto Loot (BẬT/TẮT)** | Bật hoặc tắt tính năng tự nhặt đồ rơi |
| **Nút ⚙ (cạnh Auto Loot)** | Mở Modal cấu hình bộ lọc loại vật phẩm cần nhặt |
| **Auto Sell (BẬT/TẮT)** | Bật hoặc tắt chế độ tự động bán đồ rác nền từ xa |
| **Nút ⚙ (cạnh Auto Sell)** | Mở Modal cấu hình chọn loại đồ bán & nút "Bán ngay" |
| **Đồ Sát (PK)** | Bật/tắt cờ đồ sát (kèm đếm ngược 180s) |
| **Tốc độ (Speed)** | Tùy chỉnh tốc độ di chuyển: x1.0, x1.2, x1.4, x1.6, x1.8, x2.0 |
| **Đổi Khu (Zone)** | Xem khu hiện tại và chuyển nhanh sang khu khác |
| **Click quái trên Radar / Grid** | Ghim mục tiêu quái vật |
| **Click người chơi trong Tab Player** | Ghim mục tiêu người chơi để PK từ xa |

### 9.2. Phím tắt trong Game Client (MicroEmulator):
| Phím | Chức năng |
| :--- | :--- |
| **Phím 1 - 5** | Sử dụng kỹ năng tương ứng trên thanh Hotkey |
| **Phím Space (Cách) / Enter** | Tấn công mục tiêu đang chọn hoặc mục tiêu đã ghim |
| **Phím điều hướng (W/A/S/D hoặc Mũi tên)** | Di chuyển nhân vật |
| **Phím 0** | Bật menu game / Hành trang |

---

## 9. HƯỚNG DẪN BIÊN DỊCH VÀ ĐÓNG GÓI (BUILD & RE-PACK)

Mỗi khi chỉnh sửa mã nguồn Java trong thư mục `src_java/`:

### Trên Linux / macOS:
```bash
./build_jar.sh
```

### Trên Windows (CMD hoặc PowerShell):
```cmd
build_jar.bat
# hoặc
.\build_jar.ps1
```

### Trực tiếp trên Tool:
- Vào menu **Cài đặt** trên Tool Studio -> Bấm nút **"Repack JAR"**.

Hệ thống sẽ biên dịch toàn bộ 196 file Java bằng `javac` (tương thích Java 8) và tự động cập nhật bytecode mới nhất vào file `KnightOnline_v403.jar`.
