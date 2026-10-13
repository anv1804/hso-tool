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

### 3.3. Giao diện Radar & Danh sách quái 2 cột
- **Radar trực quan**:
  - Chấm đỏ: Quái vật đang sống.
  - Chấm xám: Quái vật đã chết (đang đếm ngược hồi sinh).
  - Vòng tròn trắng: Quái vật hoặc đối tượng đang được ghim mục tiêu.
  - Chấm vàng: Người chơi khác trong khu.
  - Chấm xanh lá: Bản thân nhân vật.
  - Biểu tượng đa giác: Cổng chuyển map, lối đi, khu vực, đá dịch chuyển.
- **Danh sách quái 2 cột (Grid View)**:
  - Hiển thị song song 2 cột, chứa được tới 60-90 quái mà không cần cuộn chuột.
  - Mỗi ô hiển thị: `#ID Tên` (ví dụ `#14 Chuột`, `#27 Lửa`), thanh máu mini và số HP rút gọn (ví dụ `1.2k`, `850` hoặc đếm ngược `HS 3.2s`).
  - Hỗ trợ click chuột vào ô để **Ghim mục tiêu** hoặc thêm vào cấu hình Farm.

---

## 4. CƠ CHẾ AUTO FARM 1-HIT TOÀN MAP (FULL MAP FARM BURST)

### 4.1. Đặc tính giao thức Server HSO & Cơ chế Double Hit Đồng Bộ 100%
- Gói tin tấn công `fire_monster(mVector targets, byte typekill)` gửi opcode 9 lên server.
- **Nguyên nhân gây mất đồng bộ trước đây**:
  - Khi gửi gói gộp `fire_monster(allTargets, (byte)0)` kèm theo gói lẻ: Server với Skill 0 chỉ trừ máu con đầu tiên, khiến con đầu tiên bị ăn **Double Hit** (2 đòn) trong khi các con còn lại chỉ ăn 1 đòn. Con đầu tiên chết trước, kéo theo chu kỳ lệch pha dây chuyền.
  - Đồng thời việc gán `player.IDAttack = targets[0]` khiến client khóa mục tiêu vào 1 con, tạo cảm giác nhân vật nhắm đánh 1 con trước rồi mới đánh cả map.
- **Giải pháp Double Hit Đồng Bộ Hoàn Hảo**:
  - Hủy bỏ việc khóa mục tiêu (`player.IDAttack = -1`, `player.KillFire = -1`, `GameScreen.ObjFocus = null`).
  - Loại bỏ hoàn toàn gói gộp thừa.
  - Áp dụng 2 vòng lặp gửi đòn đánh:
    - Vòng 1: Gửi Hit 1 cho 100% quái trong danh sách.
    - Vòng 2: Gửi Hit 2 (Double Hit) cho 100% quái trong danh sách.
  - **100% quái nhận chính xác 2 hit như nhau**, máu cùng tụt 2 nhịp đồng loạt, sát thương nhân đôi và không con nào bị lệch.

### 4.2. Tối ưu nhịp đánh chuẩn 1.0 giây (Quái cần 2-3 đợt đánh)
- Chiêu đánh thường cơ bản của tất cả các phái có **thời gian hồi gốc là 1s (1000ms)**.
- Khi đánh quái cấp cao hoặc máu lớn cần 2 đợt (4 hit) mới chết:
  - Hàm `getNormalAttackCooldownMs()` đọc thời gian hồi thực tế từ `skill.mLvSkill[lv].getdelay()`.
  - Tự động trừ bù **50ms độ trễ mạng** (`latency ~ 950ms`).
  - Đợt đánh kế tiếp (Hit 3, Hit 4) được phóng đi chính xác tại thời điểm chiêu vừa hồi, server chấp nhận 100% đòn đánh mà không bị trễ hay nghẽn nhịp.

### 4.3. Cơ chế đón đầu hồi sinh sớm 500ms (Preemptive Strike)
- Khi **toàn bộ quái được chọn đã chết** (`aliveCount == 0`):
  - Hệ thống tính toán thời điểm con quái chết **MUỘN NHẤT** hồi sinh (`latestDeadRespawnDue`).
  - Khi đồng hồ đếm ngược còn **500ms** (`now >= latestDeadRespawnDue - 500L`):
  - Phóng gói tin tấn công đón đầu lên server.
  - Khi quái vừa thành hình trên server thì sát thương đã có sẵn tại mili-giây 0, quái chết ngay lập tức mà không kịp xuất hiện trên màn hình hay phát hoạt ảnh.

### 4.4. Thuật toán Đồng bộ Pha Hội tụ (Phase Sync Convergence / Hold Fire)
- Khi trong map có sự lệch pha (một số quái sống và một số quái đang đếm ngược hồi sinh):
  1. **Nếu có quái sống đang bị thương dở dang** (`hp < maxHp`): Hệ thống đánh tiếp các con đang bị thương để dứt điểm chúng, đưa toàn bộ đàn về trạng thái CHẾT.
  2. **Nếu các quái sống đều full máu (mới hồi sinh sớm)** trong khi các quái khác vẫn đang đếm ngược hồi sinh:
     - Hệ thống kích hoạt trạng thái **HOLD FIRE (Giữ nhịp)**: Tuyệt đối không đánh lẻ tẻ con vừa hồi sinh sớm.
     - Chờ cho đến khi con chết muộn nhất chạm mốc đón đầu 500ms.
     - Lúc này 100% đàn quái đều có mặt (hoặc vừa đón đầu hồi sinh), hệ thống tung đòn gom toàn bộ quái trong map vào **đúng 1 nhịp đánh duy nhất**.
  3. Kể từ thời điểm đó, **toàn bộ đàn quái hội tụ về chung 1 giây hồi sinh và 1 giây chết**, duy trì trạng thái đồng bộ vĩnh viễn!

### 4.5. Quản lý cấu hình Farm theo từng Map
- Cấu hình farm được lưu trữ theo hồ sơ từng bản đồ (`map_id`).
- Khi người chơi di chuyển sang map khác chưa có cấu hình: Auto Farm tự động tạm dừng để tránh đánh nhầm.
- Khi quay trở lại map đã cấu hình: Hệ thống tự động nhận diện và tiếp tục farm.

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

## 7. CƠ CHẾ AN TOÀN BẢO VỆ TÀI KHOẢN (DUAL-LAYER SAFETY)

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
3. **Khóa chặt tự đánh nội tại (Auto Attack Lockout)**:
   - Khi tắt Auto Attack (`isAutoAttack = false`), hàm `stopAllAutoAttack()` triệt tiêu toàn bộ:
     - Biến cờ tự đánh nội tại của client (`Player.isAutoFire = -1`, `isCurAutoFire = false`).
     - Reset phím đánh ảo (`keyMyPressed[25] = false`, `keyMyPressed[5] = false`).
     - Hủy mục tiêu trỏ (`ObjFocus = null`, `KillFire = -1`, `IDAttack = -1`).
     - Ngăn chặn hoàn toàn hiện tượng nhân vật tự ý vung kiếm chém bậy khi đang đứng cạnh quái.
4. **Giãn cách bật/tắt Đồ Sát (180 giây)**:
   - Nút Đồ Sát tích hợp bộ đếm lùi 180s thời gian thực, ngăn việc spam lệnh vi phạm quy định game.

---

## 8. BẢNG PHÍM TẮT & LỆNH ĐIỀU KHIỂN

### 8.1. Điều khiển trên HSO Tool Studio:
| Thao tác / Nút | Chức năng |
| :--- | :--- |
| **Auto Attack (BẬT/TẮT)** | Bật hoặc tắt chế độ tự động đánh |
| **Nút ⚙ (cạnh Auto Attack)** | Mở hộp thoại cấu hình chọn quái cần Farm |
| **Auto Loot (BẬT/TẮT)** | Bật hoặc tắt tính năng tự nhặt đồ rơi |
| **Nút ⚙ (cạnh Auto Loot)** | Mở Modal cấu hình bộ lọc loại vật phẩm cần nhặt |
| **Đồ Sát (PK)** | Bật/tắt cờ đồ sát (kèm đếm ngược 180s) |
| **Tốc độ (Speed)** | Tùy chỉnh tốc độ di chuyển: x1.0, x1.2, x1.4, x1.6, x1.8, x2.0 |
| **Đổi Khu (Zone)** | Xem khu hiện tại và chuyển nhanh sang khu khác |
| **Click quái trên Radar / Grid** | Ghim mục tiêu quái vật |
| **Click người chơi trong Tab Player** | Ghim mục tiêu người chơi để PK từ xa |

### 8.2. Phím tắt trong Game Client (MicroEmulator):
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
