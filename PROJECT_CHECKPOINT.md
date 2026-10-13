# CHECKPOINT DỰ ÁN HSO TOOL & KNIGHT ONLINE v403
*Phiên bản: v1.0 | Ngày cập nhật: 13/10/2026*

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

## 4. Nâng cấp Rada Full Map & Auto Fam 1-Hit Đón Đầu Hồi Sinh (13/10/2026)
1. **Rada tự động quét toàn bộ bản đồ (Full Map Monster Scan):**
   - Tự động bắt đầu quét toàn bộ ID quái trên map ngay khi bước vào map hoặc đổi khu vực (`ensureMonsterScanContext()`).
   - Tăng tốc quét theo nhóm 6 ID / 25ms, hoàn tất nhận diện toàn map (40-60 quái) chỉ trong 1.5 - 2 giây.
   - Trích xuất đầy đủ thông tin quái: Tọa độ (X, Y), tên loại quái, máu tối đa, và **thời gian hồi sinh gốc từ server (`timeReveice` trong gói `monsterInfo`)**.
   - Tab "MONSTERS" và Radar Canvas hiển thị toàn bộ quái trên toàn map (không còn giới hạn ở phạm vi cục bộ ~300px xung quanh nhân vật).
   - Tự động lưu và kế thừa cấu hình farm theo map (`map_id:*`), giữ nguyên chế độ farm khi đổi khu.

2. **Auto Fam 1-hit trúng 100% quái toàn map (Đồng thời cùng 1 thời điểm):**
   - Dồn toàn bộ ID quái toàn map vào một danh sách duy nhất `allTargets`.
   - Phóng gói tin 1-hit `GlobalService.gI().fire_monster(allTargets, ...)` gửi toàn bộ quái trong 1 gói duy nhất lên server.
   - Server nhận và xử lý sát thương đồng loạt cho 100% quái toàn map tại đúng 1 thời điểm duy nhất (tất cả quái cùng tụt máu và chết cùng lúc).
   - Tuyệt đối không dùng vòng lặp `player_move` tuần tự (tránh việc di chuyển ảo qua từng con làm phân mảnh nhịp đánh thành đánh lần lượt).

3. **Đo thời gian hồi sinh & Ra đòn đón đầu đồng loạt toàn map:**
   - Khi toàn bộ quái chết cùng 1 lúc, mốc thời gian chết `deathTime` và chu kỳ hồi sinh `respawnIntervalMs` được đồng bộ chuẩn xác.
   - Khi chu kỳ đếm ngược còn **250ms** trước khi hồi sinh (`now >= deathTime + interval - 250L`), hệ thống kích hoạt **đòn đánh 1-hit toàn map đón đầu**:
   - Gom toàn bộ quái trong map và bắn đồng loạt 1 nhịp, quái vừa thành hình là nhận sát thương chết ngay lập tức tại mili-giây 0.
   - Không đánh lẻ tẻ, không đánh lần lượt từng con; toàn map luôn vận hành theo 1 nhịp đồng nhất hoàn hảo.

4. **Nâng cấp Giao diện Radar & Tab Quái 2 Cột Tổng Thể (13/10/2026):**
   - **Radar nhỏ gọn & Chú thích 1 dòng thanh lịch**:
     - Thu gọn khung bản đồ radar với khoảng đệm an toàn, tăng chiều cao panel LOGS (`height=4`) giúp giao diện trung tâm hài hòa và thoáng đãng.
     - Thay thế cụm chú thích 2 dòng rườm rà bằng 1 dòng duy nhất phân bổ đều chuẩn HUD: `● Quái  ● Chết  ◆ Cổng  ▲ Khu  ◆ Đá  ● Người  ● Bạn`.
   - **Chế độ xem Quái 2 Cột Toàn Map (2-Column Monster Grid)**:
     - Thêm thanh chuyển đổi chế độ xem tiện lợi: `[ 🎯 Gần (Ghim) ]` và `[ ☷ 2 Cột (All) ]`.
     - Chế độ 2 cột hiển thị tên quái siêu ngắn gọn (`#ID Tên` ví dụ `#14 Chuột`, `#27 Lửa`) và thanh máu mini (kèm số HP rút gọn như `1.2k`, `850` hoặc đếm ngược hồi sinh `HS Xs`).
     - Tối ưu kích thước ô (cao 12px) để chiếu cùng lúc 40 quái (trung bình của map) mà không cần cuộn chuột, và hỗ trợ tải mượt mà lên tới 90 quái.
     - Cho phép người chơi quan sát toàn bộ quái trong map chịu sát thương và tụt máu/chết đồng thời cùng 1 lúc khi Auto Fam 1-hit kích hoạt.
     - Hỗ trợ click chuột vào bất kỳ ô quái nào trong 2 cột để ghim/bỏ ghim mục tiêu linh hoạt.

## 5. Đồng bộ Máu & Di Chuyển Quái Toàn Map và Khóa Chặt Auto Attack (13/10/2026)
1. **Khắc phục triệt để lỗi quái ở xa hiển thị 0 máu và đứng yên trên Radar**:
   - **Nguyên nhân cốt lõi**: Cơ chế AOI (Area of Interest) của game chỉ tự động phát gói di chuyển/hồi sinh cho quái trong tầm nhìn xung quanh nhân vật. Quái ở xa sau khi bị 1-hit chết và hồi sinh lại trên server sẽ không có thông báo chủ động nếu client không truy vấn.
   - **Giải pháp xử lý**:
     - Thêm vòng lặp làm tươi liên tục `advanceMonsterRefresh(now)` trong `HsoBridge.java`: cứ mỗi 60ms sẽ round-robin gửi 4 truy vấn `monster_info` lên server. Toàn bộ 30-60 quái trên map được cập nhật tọa độ (X, Y) và máu (HP) liên tục theo thời gian thực.
     - Khi quái chết đến mốc thời gian hồi sinh (`now >= deathTime + interval`), hệ thống tự động khôi phục HP và truy vấn server ngay lập tức.
     - Tích hợp `HsoBridge.updateMonsterPosition(s2, s3, s4)` trực tiếp vào hàm xử lý `objectMove` trong `ReadMessenge.java`. Toàn bộ quái dù ở xa hay gần đều liên tục di chuyển sống động trên Radar và hiển thị thanh máu đỏ đầy đủ trên panel 2 cột.

2. **Khóa chặt tuyệt đối cơ chế Tự Động Đánh (Auto Attack Lockout)**:
   - **Nguyên nhân**: Khi chưa bật hoặc đã tắt Auto Attack, cơ chế nội tại `Player.isAutoFire` và hàm `setPointFocus()`/`dofire()` trong `Player.java` vẫn kích hoạt và tự động spam chiêu vào quái được trỏ (`ObjFocus`).
   - **Giải pháp xử lý**:
     - Chặn đứng mọi hành vi tự đánh trong `Player.java`: `setAutoFire()`, `dofire()`, `dofire_()`, `setStartSkill()` và vòng lặp `Player.update()` đều kiểm tra nghiêm ngặt cờ `HsoBridge.isAutoAttack`. Nếu `!isAutoAttack`, hủy ngay `isAutoFire = -1`, `isCurAutoFire = false` và thoát ra ngay lập tức.
     - Thêm hàm `stopAllAutoAttack()` trong `HsoBridge.java`: Khi nhận lệnh tắt Auto Attack (`enabled: false`), xóa sạch đường đi `posTransRoad`, reset phím bấm đánh `keyMyPressed[25] = false`, `keyMyPressed[5] = false`, và xóa ID mục tiêu `IDAttack = -1`, `KillFire = -1`.
     - Trong `HsoBridge.update()`: Khi `!isAutoAttack`, liên tục duy trì trạng thái đóng băng tự đánh để nhân vật tuyệt đối không tự ý ra đòn.

   - **Khóa triệt để đánh đơn lẻ khi bật Auto Fam & Thuật toán Đồng Bộ Pha Hồi Sinh (Sync Convergence)**:
     - **Nguyên nhân cốt lõi**:
       1. Khi nhân vật đứng gần quái, engine game mặc định auto-target vào con gần nhất (`GameScreen.ObjFocus`). Khi vừa bấm bật Auto Attack, nếu gửi skill hotkey (Skill 1, 2) thì server chỉ xử lý sát thương cho đúng 1 con đầu tiên (do skill hotkey trên server là đơn mục tiêu), khiến con đó chết trước 0.3s - 0.5s so với đàn.
       2. Do con đó chết trước, đồng hồ hồi sinh của nó đếm ngược xong trước, nó hồi sinh đơn lẻ sớm hơn. Client lại tấn công ngay khi có quái sống (`aliveCount > 0`), tạo thành vòng lặp mất đồng bộ vĩnh viễn (Desync Loop).
     - **Giải pháp xử lý**:
       1. Khóa hoàn toàn `ObjFocus`, `setFocus()`, `setActionHotKey()` khi Auto Fam đang bật.
       2. Cố định Auto Farm 1-Hit TOÀN MAP chỉ sử dụng duy nhất **Chiêu 0 (Skill 0 - Đánh thường)**, loại bỏ hoàn toàn việc gọi skill hotkey đơn mục tiêu.
       3. Áp dụng thuật toán **3 Trạng Thái Đồng Bộ Pha**: Nếu đàn quái đang bị lệch pha, hệ thống giữ nhịp (Hold Fire) đợi các con chết muộn nhất hồi sinh đầy đủ, sau đó tung đúng 1 hit gom cả đàn và gán chung 1 mốc `deathTime` để các đợt sau 100% quái hồi sinh cùng tích tắc.

   - **Sửa lỗi khi không chọn quái (Auto Fam: 0 quái) không tắt mà lại đánh toàn map**:
     - **Nguyên nhân**: Khi bỏ chọn hết quái, `hso_tool.py` lưu dict rỗng và gửi `configured: 1, ids: ""`. Trong `HsoBridge.java`, khi `farmTargetIds` rỗng, logic cũ có đoạn fallback `if (idsToCheck.length == 0) idsToCheck = mapMonsterRecords` tự ý gán toàn bộ quái trong map để đánh.
     - **Giải pháp**:
       1. `hso_tool.py`: Khi bỏ chọn quái, xóa cấu hình farm, gửi `configured: 0, ids: ""`, nút trở về `Auto Fam: Cấu hình` (tắt).
       2. `HsoBridge.java`: Khi `ids` rỗng, đặt `farmConfigured = false`, `farmTargetIds = new int[0]`. Loại bỏ hoàn toàn đoạn fallback sang `mapMonsterRecords`.

   - **Đánh 1-Hit toàn bộ quái trong 1 gói tin (Không chia nhóm lẻ) & Đón đầu 500ms triệt tiêu hoạt ảnh**:
     - **Nguyên nhân quái bị chia nhỏ thành nhóm lẻ**:
       1. Vòng lặp chunking `allTargets.size() / 7` trước đó chia 29 con thành 5 gói tin gửi lần lượt.
       2. Khi một số quái sống (như Chuột) và một số quái chết (như Lửa), logic cũ vội vàng đánh nhóm sống trước thay vì giữ nhịp đợi quái chết cùng hồi sinh, khiến 2 nhóm quái bị lệch pha vĩnh viễn.
     - **Nguyên nhân quái vẫn kịp hiện hình**:
       - Mốc đón đầu `-250ms` quá trễ (bằng ping + tick server), lúc gói tin đến nơi quái đã xuất hiện trên client và hoạt ảnh chết `startDeadFly` còn bay trên màn hình một lúc.
     - **Giải pháp tối ưu**:
       1. `fireFarmBurst`: Gửi đúng **1 gói tin duy nhất** chứa toàn bộ quái qua `GlobalService.gI().fire_monster(allTargets, (byte)0)`, tuyệt đối không chia mẩu 7 con.
       2. Hội tụ toàn diện (Total Sync): Khi lệch pha (vừa có quái sống vừa có quái chết), giữ nhịp (Hold Fire) đợi con quái chết muộn nhất chạm mốc đón đầu thì gom cả 2 nhóm đánh trong 1 hit duy nhất.
       3. Tăng thời gian đón đầu sớm lên **500ms** (`PREEMPTIVE_LEAD_MS = 500L`): Gói tin tới server trước khi server broadcast quái sống.
       4. `MainMonster.startDeadFly`: Khi Auto Farm kích hoạt, triệt tiêu toàn bộ thời gian bay chết (`timeBienmat = 0; isDie = true; isRemove = true;`), quái lập tức biến mất ngay tức thì.

    - **Sửa lỗi đã chọn quái + bật Auto Attack nhưng không đánh & Lưu cấu hình fam theo Map với cơ chế Auto-Resume an toàn**:
      - **Nguyên nhân không đánh khi đã bật Auto Attack**:
        1. Trong `configureFarmTargets(cmd)`, biến `farmArea` không được gán giá trị (giữ nguyên `-1`). Trong khi đó ở vòng lặp đánh, điều kiện kiểm tra `farmArea == LoadMap.Area` (-1 == 5) luôn bị sai dẫn tới bỏ qua `fireFarmBurst()`, đồng thời `isFarmConfigured()` chặn đòn đánh thường khiến nhân vật đứng im bất động.
        2. Trong `getLiveFarmTargets()`, các lệnh gán giả tạo `rec.hp = 0` và `rec.deathTime = now` khiến quái ngoài tầm nhìn bị coi là chết vĩnh viễn, dẫn đến kẹt vòng lặp chờ vô tận ở Tình huống 3.
      - **Giải pháp xử lý**:
        1. Bỏ ép buộc `farmArea == LoadMap.Area` trong [HsoBridge.java](file:///home/anv/Documents/tools/hso-tool/src_java/com/silverknight/HsoBridge.java), cho phép Auto Farm áp dụng xuyên suốt tất cả các khu của Map được cấu hình (`farmMapId == GameCanvas.loadmap.idMap`).
        2. Loại bỏ hoàn toàn việc can thiệp `rec.hp = 0` giả lập trong `getLiveFarmTargets()`. Khi có quái sống thì lập tức đánh ngay (chỉ chờ tối đa 600ms nếu có quái chết sắp hồi sinh cùng), tuyệt đối không để nhân vật đứng im ngơ ngác.
        3. **Lưu cấu hình farm theo Map**: Lưu trữ profile theo key Map ID (`f"{map_id}:*"` và `str(map_id)`). Khi chuyển sang map khác chưa có cấu hình, Auto Fam tự động tắt. Khi quay trở lại map đã cấu hình, Auto Fam tự động kích hoạt lại.
        4. **Cơ chế An toàn Tuyệt đối khi có người chơi khác**:
           - Khi phát hiện có người chơi khác trong khu (`other_count > 0`): VẪN GIỮ NGUYÊN cấu hình farm trong config (tuyệt đối không xóa), nhưng lập tức TẮT Auto Fam (`configured: 0`) và TẮT LUÔN Auto Attack (`enabled: False`) để bảo vệ tài khoản khỏi bị quay clip tố cáo.
           - Khi khu vực an toàn trở lại (0 người) hoặc người lạ rời đi: Tự động BẬT LẠI Auto Fam và Auto Attack mà không cần thao tác thủ công.

    - **Sửa màu chấm Radar, trả lại can thiệp Client game, mặc định 2 cột toàn map và cho phép ghim quái toàn map**:
      - **Giải thích chấm vàng trên Radar**: Trước đó, các quái được chọn trong Auto Farm hoặc quái sắp hồi sinh được vẽ bằng màu vàng nhạt (`COLOR_TEXT_GOLD` / `#bfa15f`), gây hiểu nhầm thành chấm "Người" (màu vàng). Đã sửa: Quái sống luôn luôn là màu đỏ thuần (`COLOR_HP_RED`), quái ghim/focus có viền tròn trắng bao quanh nổi bật, chấm màu vàng chỉ dành riêng cho người chơi khác (`players`).
      - **Trả lại can thiệp Client Game**: Revert hoàn toàn đoạn override hoạt ảnh chết `startDeadFly` trong [MainMonster.java](file:///home/anv/Documents/tools/hso-tool/src_java/GameObjects/MainMonster.java). Giữ nguyên trải nghiệm hiển thị gốc của client game.
      - **Tab Monster mặc định 2 cột toàn map & Ghim quái toàn map**:
        - Bỏ thanh chuyển đổi "Gần" và "2 Cột". Tab Monster luôn mặc định hiển thị 2 cột danh sách quái toàn map, tối ưu không gian cuộn.
        - Sửa `pinMonster()` trong [HsoBridge.java](file:///home/anv/Documents/tools/hso-tool/src_java/com/silverknight/HsoBridge.java): Trước đó hàm chỉ tìm trong `Vecplayers` (quái trong tầm nhìn). Nay cho phép ghim bất kỳ quái nào trên map qua `mapMonsterRecords`, đồng thời hỗ trợ nhân vật tự động tìm đường di chuyển tới quái đã ghim từ xa khi bật Auto Attack hoặc tấn công trực tiếp.

    - **Tinh giản Panel Controls & Nâng cấp Giao diện (13/10/2026)**:
      - **Sửa triệt để lỗi HP/MP/EXP nhảy về 0 trên Tool**:
        - *Nguyên nhân*: Trong Java [HsoBridge.java](file:///home/anv/Documents/tools/hso-tool/src_java/com/silverknight/HsoBridge.java), `writeState()` ghi file trực tiếp qua `FileOutputStream` làm file bị truncate 0 byte trước khi ghi, đồng thời việc lặp trực tiếp `GameScreen.Vecplayers` có thể văng ngoại lệ `ArrayIndexOutOfBoundsException` khi mở hành trang hoặc trỏ quái làm ngắt ngang tiến trình ghi JSON.
        - *Khắc phục*: Snapshot an toàn mảng đối tượng trong `Vecplayers`, ghi file atomic POSIX qua `client_state.json.tmp` rồi rename, đồng thời bổ sung `self.cached_player` trên Python để giữ vững chỉ số HP, MP, EXP không bao giờ bị chớp nháy hoặc nhảy về 0.
      - **Tinh giản giao diện Controls**:
        - Ghép **Auto Attack** và **Auto Fam** thành 1 hàng duy nhất: Nút icon option `⚙` ở bên trái (bật sáng vàng khi map đã có cấu hình quái), nút **Auto Attack** ở bên phải chiếm toàn bộ chiều rộng còn lại.
        - **Active sáng rực rỡ**: Nâng cấp bộ màu render `is_selected` với gradient vàng cam hổ phách rực rỡ, viền kim loại sáng bóng `#ffea70` và chữ trắng tinh khôi `#ffffff` giúp nhận biết nút đang BẬT cực kỳ nổi bật.
        - **Bỏ nút Tấn công ngay**: Loại bỏ hoàn toàn `btn_attack` ("⚔️ Tấn công ngay") khỏi giao diện điều khiển.
        - **Đếm ngược 180s cho nút Đồ Sát (PK Cooldown)**:
          - Khi bấm bật hoặc tắt Đồ Sát, áp dụng thời gian giãn cách 3 phút (180 giây) chuẩn theo game.
          - Hiển thị đếm ngược theo giây thời gian thực: `Đồ Sát: BẬT (180s)` -> `179s` ... `0s`.
      - **Hàng nút Auto Loot & Modal Cấu hình Bộ Lọc Đồ Rơi (Loot Filter)**:
        - Tái cấu trúc cụm nút **Auto Loot** thành hàng kép đối xứng tương tự hàng Auto Attack / Auto Fam:
          - Nút icon cấu hình `[ ⚙ ]` (Option) ở bên trái.
          - Nút bật/tắt `[ Auto Loot: ON/OFF ]` ở bên phải chiếm toàn bộ phần còn lại của hàng.
          - Khi Auto Loot BẬT hoặc có cấu hình, cả hai nút sáng vàng đồng bộ.
        - **Modal Cấu hình Bộ Lọc Đồ Rơi (`open_auto_loot_modal`)**:
          - Mở hộp thoại popup chuẩn giao diện HSO (`HsoPanel` gỗ viền vàng kim).
          - Phân nhóm trực quan theo 3 chuyên mục kèm màu sắc đặc trưng:
            1. **Trang bị theo phẩm cấp / màu sắc**: ⚪ Đồ Trắng (Thường), 🔵 Đồ Xanh (Đặc biệt), 🟡 Đồ Vàng (Hiếm), 🟣 Đồ Tím (Huyền bí), 🟠 Cam (Sử thi), 🟢 Xanh lá (Hoàn mỹ).
            2. **Hồi phục & Tiền tệ**: 🩸 Bình Máu (HP), 💧 Bình Mana (MP), 💰 Tiền Vàng (Gold / Xu).
            3. **Khác**: 💎 Nguyên liệu & Đá cường hóa, 📜 Vật phẩm Nhiệm vụ.
          - Nút công cụ nhanh: `[ Chọn tất cả ]` và `[ Bỏ chọn hết ]`, hiển thị bộ đếm số loại vật phẩm đang chọn theo thời gian thực.
          - Chân modal: Nút `[ Hủy ]` và `[ LƯU CẤU HÌNH ]`.
          - Lưu cấu hình vào `tool_config.json` (`auto_loot_filter`) và đồng bộ ngay lập tức qua IPC `loot_filter` và `auto_pickup` tới game client Java.
        - **Lọc thông minh trong Java [HsoBridge.java](file:///home/anv/Documents/tools/hso-tool/src_java/com/silverknight/HsoBridge.java)**:
          - Hàm `shouldPickupItem(MainObject obj)` kiểm tra nghiêm ngặt `typeObject` (3=Trang bị, 4=Bình/Vàng, 5=Nhiệm vụ, 7=Nguyên liệu), `colorName` và tên đối tượng.
      - **Sửa triệt để lỗi ghim quái ở xa đánh chết nhưng ở gần đánh không chết**:
        - *Nguyên nhân cốt lõi*:
          1. Với quái ở xa ngoài tầm mắt (`target == null`): Hệ thống gửi trực tiếp gói tin `GlobalService.gI().fire_monster(targets, (byte)0)` lên server, server xử lý sát thương ngay lập tức nên quái ở xa chết.
          2. Với quái ở gần trong tầm mắt (`target != null`): Logic cũ trong `checkCommand("attack_monster")`, `firePinnedSkill` và `HsoBridge.update()` lại rẽ nhánh gọi `GameScreen.player.setAutoFire()` hoặc `player.setStartSkill()`. Cả 2 hàm này đều bị chặn đứng bởi các điều kiện bảo vệ: `if (!isAutoAttack) return`, `if (isFarmConfigured()) return`, hoặc `needMove` nhảy 1 bước xa bị anticheat chặn. Kết quả: Quái ở gần không hề được gửi bất kỳ gói tin tấn công nào lên server, dẫn đến quái ở gần đứng yên 800/800 HP không chết!
          3. HUD banner ghim quái trong `paintPinnedTargetHud` bị vẽ đè đúng vào tọa độ hiển thị thông tin quái gốc của client game (`paintInfoFocus`), gây chèn chữ lung tung (`[Ghi đượcdỤNgọc khi mở aThùng gỗ.5`).
        - *Giải pháp triệt để*:
          1. **Đồng bộ cơ chế tấn công quái ghim**: Mọi hành vi tấn công quái đã ghim (`attack_monster`, `firePinnedSkill`, và vòng lặp `isAutoAttack`) đều gửi trực tiếp gói tin `fire_monster(targets, (byte)skillId)` thẳng tới ID quái ghim. Quái ở gần hay ở xa đều nhận sát thương và chết tức thì 100%!
          2. Trong [Player.java](file:///home/anv/Documents/tools/hso-tool/src_java/GameObjects/Player.java): `setFocus()`, `setAutoFire()`, `setStartSkill()` ưu tiên tuyệt đối mục tiêu ghim (`pinnedMonsterId != -1`), không bị chặn bởi cấu hình fam.
          3. `paintPinnedTargetHud`: Chỉ vẽ HUD banner riêng khi ghim NGƯỜI CHƠI (PK), trả lại hiển thị focus gốc tinh tế, sạch sẽ của game khi trỏ/ghim quái vật.

---

## 5. Khắc phục triệt để lỗi đánh lẻ bậc thang & Tối ưu hóa 1-Hit toàn map từ Git (13/10/2026)
- **Hiện tượng lỗi**:
  - Quái trên map chết tuần tự từng con theo thứ tự ID từ trên xuống dưới (#0 -> #1 -> #2 ... -> #28) cách nhau đúng ~150-200ms mỗi con, tạo thành chuỗi đếm ngược bậc thang (`#0: 0.7s, #1: 0.9s, #2: 1.0s ... #28: 5.2s`). Quái không chết đồng loạt 100% trong 1 hit mà bị rải rác suốt 4-5 giây.
- **Phân tích nguyên nhân gốc rễ (Root Cause)**:
  1. **Đặc tính giao thức Server HSO với Chiêu 0 (Đánh thường)**:
     - Gói tin tấn công `fire_monster(mVector vec, byte typekill)` gửi opcode 9 lên server.
     - Với `typekill == 0` (đánh thường), mã nguồn server HSO chỉ xử lý duy nhất quái đầu tiên trong danh sách mục tiêu `vec`, toàn bộ các mục tiêu từ vị trí 1 đến 28 phía sau bị server bỏ qua (drop).
     - Trong chỉnh sửa trước, vòng lặp bắn gói từng con bị xóa và thay bằng "chỉ gửi duy nhất 1 gói gộp chứa 29 con". Do đó, mỗi tick 150ms client chỉ làm server trừ máu được đúng con đầu tiên!
     - Tick 1 (t=0ms): Server chỉ trừ máu #0. #0 chết.
     - Tick 2 (t=150ms): Server chỉ trừ máu #1. #1 chết.
     - ... Đến tick 29 (t=4500ms): #28 mới chết!
     - Chu kỳ hồi sinh 5s do đó cũng bị lệch thành bậc thang dãn cách 150ms.
  2. **Bài học từ bản Git (`7d741ad` và `a8dbc57`)**:
     - Trong các commit chuẩn trên git, sau khi gửi gói gộp (hoặc chiêu lan), hệ thống luôn chạy vòng lặp gửi kèm các gói đơn lẻ `fire_monster(single, (byte)0)` cho toàn bộ quái trong danh sách mục tiêu.
     - Vì vòng lặp Java chạy trong chưa đầy 1 mili-giây, toàn bộ 29 gói tin được đẩy vào socket TCP trong cùng 1 frame mạng. Server tiếp nhận và xử lý sát thương toàn bộ 29 con trong cùng 1 tick server!
- **Giải pháp tối ưu hóa hoàn thiện**:
  1. **Khôi phục Burst Loop từ Git**:
     - Duyệt `for (int i = 0; i < targets.length; ++i)` và bắn gói `single` cho từng con ngay sau gói gộp.
     - Nếu có chiêu lan sẵn sàng trên hotkey, bắn kèm gói gộp `readySkillId` để tối đa hóa sát thương.
  2. **Đánh liên tục đồng bộ chuẩn 1s theo thời gian hồi chiêu đánh thường (Quái cần 2-3 hit)**:
     - *Phát hiện & Sửa lỗi*: Trước đó logic lệch pha đã vô tình ép client nhịn đánh 5.5s nếu có bất kỳ con quái nào chết trước (như `#0 Chuột` 5.8s hoặc Boss `#31` 3586s) trong khi các quái khác (`#1..#29` Nấm 1.2k-1.7k HP) vẫn còn sống.
     - *Đồng bộ chuẩn xác theo chiêu đánh thường*: Kỹ năng đánh thường cơ bản ("Gậy tự vệ Lv1", "Kiếm gỗ Lv1", ...) có **thời gian hồi là 1s (1000ms)**.
     - *Tối ưu hóa nhịp đánh*: Hàm `getNormalAttackCooldownMs()` đọc trực tiếp chỉ số hồi chiêu gốc từ `skill.mLvSkill[lv].getdelay()` (chuẩn 1000ms), tự động trừ bù 50ms độ trễ mạng TCP (latency ~950ms).
     - Khi quái chưa chết, nhịp đánh Hit 1 -> Hit 2 -> Hit 3 được tung ra **chính xác mỗi 1.0 giây**, gói tin đến server đúng mili-giây chiêu vừa hồi xong, server chấp nhận 100% sát thương mà không bị drop packet và không để người chơi phải chờ đợi quá 1s!
     - Chỉ khi **TOÀN BỘ** quái trên map đã chết (`aliveCount == 0`), hệ thống mới chuyển sang chế độ chờ đón đầu hồi sinh sớm 500ms.
  3. **Phản hồi tức thì khi quái tái sinh (`onMonsterRespawned`)**:
     - Ngay khi nhận được `createMonster` trong [ReadMessenge.java](file:///home/anv/Documents/tools/hso-tool/src_java/netcommand/global/ReadMessenge.java), lập tức gọi `HsoBridge.onMonsterRespawned(id)` để reset `deathTime = 0` và phục hồi `hp = maxHp` ngay lập tức mà không phải chờ gói `monster_info`.
  4. **Khắc phục triệt để lỗi ghim quái ở gần không chết**:
     - Khi cự ly giữa nhân vật và quái ghim > 25 pixel (ngoài tầm cận chiến), tự động gửi `player_move` sát tọa độ quái để server chấp nhận sát thương.
     - Chặn `Player.setAutoFire()` kích hoạt khi đang ghim quái (`pinnedMonsterId != -1`) để tránh xung đột animation chém thường của client.

---

## 6. Các file quan trọng
- `src_java/com/silverknight/HsoBridge.java`: Cầu nối giao tiếp giữa Tool và Game Client J2ME (quét map, làm tươi quái toàn map, đo hồi sinh, 1-hit đón đầu, gửi burst loop theo Git, đồng bộ đàn quái lệch pha, thread-safe IPC).
- `src_java/netcommand/global/ReadMessenge.java`: Xử lý gói tin server, trích xuất `timeReveice` (hồi sinh), cập nhật tọa độ `objectMove`, gọi `onMonsterRespawned` ngay khi quái hồi sinh.
- `src_java/GameObjects/Player.java`: Xử lý phím tắt, di chuyển, ngăn xung đột animation khi ghim quái, kiểm soát tự đánh an toàn.
- `src_java/Skill/ListSkill.java`: Gửi gói tin kỹ năng, chunking mục tiêu, kiểm tra cự ly.
- `hso_tool.py`: Ứng dụng điều khiển HSO Tool Studio GUI (Tkinter, Controls tinh giản, icon Auto Fam ⚙, đếm ngược Đồ Sát 180s, Radar nhỏ gọn, Monster Grid 2 cột).
- `build_jar.sh`: Script 1-click biên dịch và re-pack `KnightOnline_v403.jar`.
- `KnightOnline_v403.jar`: File JAR game đã tích hợp mod mới nhất.


