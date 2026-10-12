# Tài Liệu Chi Tiết: Các File và Hàm Quan Trọng Trong Hiệp Sĩ Online (HSO v403)

Tài liệu này tổng hợp cấu trúc kiến trúc, vai trò, các thuộc tính và hàm cốt lõi của mã nguồn client **Hiệp Sĩ Online (HSO v403)**, làm kim chỉ nam để nghiên cứu, phát triển và tùy biến (modding/private server).

---

## MỤC LỤC
1. [Kiến Trúc Tổng Thể](#1-kiến-trúc-tổng-thể)
2. [Tầng Mạng & Xử Lý Gói Tin (Networking)](#2-tầng-mạng--xử-lý-gói-tin-networking)
   - [CLib/Session_ME.java](#clibsession_mejava)
   - [netcommand/global/GlobalService.java](#netcommandglobalglobalservicejava)
   - [netcommand/global/ReadMessenge.java](#netcommandglobalreadmessengejava)
3. [Tầng Đồ Họa & Vòng Lặp Trò Chơi (Core Engine)](#3-tầng-đồ-họa--vòng-lặp-trò-chơi-core-engine)
   - [Main/GameCanvas.java](#maingamecanvasjava)
   - [GameScreen/GameScreen.java](#gamescreengamescreenjava)
   - [GameScreen/LoginScreen.java](#gamescreenloginscreenjava)
4. [Tầng Thực Thể & Nhân Vật (Game Objects)](#4-tầng-thực-thể--nhân-vật-game-objects)
   - [GameObjects/MainObject.java](#gameobjectsmainobjectjava)
   - [GameObjects/Player.java](#gameobjectsplayerjava)
   - [GameObjects/MainMonster.java](#gameobjectsmainmonsterjava)
5. [Hệ Thống Kỹ Năng & Hiệu Ứng](#5-hệ-thống-kỹ-năng--hiệu-ứng)
   - [Skill/Skill.java](#skillskilljava)
   - [GameEffect/AnimateEffect.java](#gameeffectanimateeffectjava)
6. [Hệ Thống Trang Bị & Cường Hóa](#6-hệ-thống-trang-bị--cường-hóa)
   - [GameObjects/Item.java & MainItem.java](#gameobjectsitemjava--mainitemjava)
   - [InterfaceComponents/TabRebuildItem.java](#interfacecomponentstabrebuilditemjava)
7. [Bảng Tra Cứu Nhanh Cho Lập Trình Viên / Modder](#7-bảng-tra-cứu-nhanh-cho-lập-trình-viên--modder)

---

## 1. Kiến Trúc Tổng Thể

```
+--------------------------------------------------------------+
|                     TemMidlet / TemCanvas                    |
|                (Lifecycle MIDP 2.0 / J2ME Entry)             |
+------------------------------+-------------------------------+
                               |
                               v
+--------------------------------------------------------------+
|                          GameCanvas                          |
|         (Render Loop: paint() | Update Loop: update())       |
|            (Input: keyPressed, keyReleased, Pointer)         |
+------------------------------+-------------------------------+
                               |
            +------------------+------------------+
            |                                     |
            v                                     v
+-----------------------+             +-----------------------+
|      GameScreen       |             |      Session_ME       |
| (World, Camera, Map,  |             |  (Socket TCP Client)  |
|  Players, Monsters)   |<----------->| - Sender Thread       |
+-----------------------+   Packets   | - MessageCollector    |
                                      +-----------+-----------+
                                                  |
                         +------------------------+------------------------+
                         v                                                 v
            +--------------------------+                      +--------------------------+
            |      GlobalService       |                      |       ReadMessenge       |
            | (Gửi lệnh Client->Server)|                      | (Giải mã Server->Client) |
            +--------------------------+                      +--------------------------+
```

---

## 2. Tầng Mạng & Xử Lý Gói Tin (Networking)

### [CLib/Session_ME.java](file:///home/anv/Documents/tools/hso-tool/src_java/CLib/Session_ME.java)
Lớp quản lý kết nối Socket TCP cấp thấp giữa Client và Server.

#### Các thuộc tính quan trọng:
* `public static Session_ME instance`: Singleton instance truy cập qua `Session_ME.gI()`.
* `private SocketConnection sc`: Đối tượng socket kết nối mạng J2ME.
* `private DataInputStream dis`: Luồng đọc byte từ socket.
* `private DataOutputStream dos`: Luồng ghi byte ra socket.
* `public boolean connected`: Trạng thái kết nối thành công.
* `public boolean connecting`: Trạng thái đang trong quá trình bắt tay kết nối.

#### Các phương thức quan trọng:
* `public static Session_ME gI()`: Lấy thể hiện duy nhất của Session.
* `public void connect(String host, int port)`: Khởi tạo luồng ngầm kết nối tới máy chủ thông qua socket `socket://host:port`.
* `public void sendMessage(Message message)`: Đưa gói tin `Message` vào hàng đợi `sender` để gửi đi trong background thread.
* `public void close()`: Đóng toàn bộ socket, stream và hủy 2 thread ngầm.
* **Cơ chế 2 Thread ngầm:**
  * `Session_ME$Sender`: Chờ các tin nhắn trong hàng đợi `mVector sendQueue` và ghi byte qua `DataOutputStream`.
  * `Session_ME$MessageCollector`: Vòng lặp liên tục đọc byte header opcode, đọc độ dài dữ liệu và tạo đối tượng `Message` chuyển cho `IMessageHandler` xử lý.

---

### [netcommand/global/GlobalService.java](file:///home/anv/Documents/tools/hso-tool/src_java/netcommand/global/GlobalService.java)
Lớp đóng gói và gửi các yêu cầu nghiệp vụ từ Client lên Server.

#### Các phương thức quan trọng:
* `public void login(String user, String pass, String version, String clinePro, String pro, String agent, int id, byte area)`: Gửi gói tin đăng nhập với tài khoản, mật khẩu, phiên bản và khu vực.
* `public void player_move(short x, short y)`: Gửi tọa độ mới của nhân vật khi di chuyển trên bản đồ.
* `public void fire_monster(mVector vec, byte typekill)`: Gửi lệnh đánh quái với danh sách quái (`vec`) và loại chiêu thức/kỹ năng (`typekill`).
* `public void char_info(short id)`: Yêu cầu server gửi chi tiết trang bị/chỉ số của người chơi có ID chỉ định.
* `public void monster_info(short id)`: Yêu cầu thông tin chi tiết của quái vật.
* `public void Ok_Change_Map()`: Xác nhận chuyển map sau khi đi qua cổng dịch chuyển.
* `public void buy_item(byte typebuy, short idbuy, short soluong)`: Gửi lệnh mua vật phẩm từ NPC/Shop.
* `public void quest(short id, byte main_sub, byte type)`: Tương tác với nhiệm vụ (nhận/trả/hủy).
* `public void Dynamic_Menu(short idNPC, byte idMenu, byte index)`: Gửi lựa chọn menu hội thoại với NPC.
* `public void load_image(short id)`: Yêu cầu server gửi dữ liệu hình ảnh/icon còn thiếu về client.

---

### [netcommand/global/ReadMessenge.java](file:///home/anv/Documents/tools/hso-tool/src_java/netcommand/global/ReadMessenge.java)
File trung tâm xử lý toàn bộ các gói tin (Packet Handler) máy chủ gửi về.

#### Các phương thức giải mã packet quan trọng:
* `public void onMessage(Message msg)`: Router phân luồng opcode của packet để gọi các hàm con tương ứng.
* `public void Login_Ok(Message msg)`: Nhận thông tin nhân vật sau khi đăng nhập thành công.
* `public void mainCharInfo(Message msg)`: Cập nhật thông số nhân vật chính (HP, MP, Level, EXP, tiền vàng, ngọc).
* `public void objectMove(Message msg)`: Nhận tọa độ di chuyển của các nhân vật/quái vật khác trên map để đồng bộ hiển thị.
* `public void charInfo(Message msg)`: Cập nhật thông tin và ngoại trang của một người chơi khác xuất hiện trong tầm nhìn.
* `public void monsterInfo(Message msg)`: Tải thông tin quái vật (máu, vị trí, loại quái).
* `public void dieMonster(Message msg)`: Xử lý sự kiện quái vật chết, phát hiệu ứng nổ và xóa khỏi màn hình.
* `public void changeMap(Message msg)`: Nhận dữ liệu bản đồ mới (tilemap, vị trí xuất hiện, danh sách NPC, điểm chuyển map).
* `public void charInventory(Message msg)`: Tải dữ liệu hành trang (các món đồ đang có trong túi).
* `public void ItemDrop(Message msg)`: Xử lý item rơi từ quái vật rơi xuống đất.
* `public void GetItemMap(Message msg)`: Nhặt item thành công từ mặt đất vào túi.
* `public void kHamNgoc(Message msg)` / `khamNgoc(Message msg)`: Nhận kết quả khảm ngọc/cường hóa từ server.

---

## 3. Tầng Đồ Họa & Vòng Lặp Trò Chơi (Core Engine)

### [Main/GameCanvas.java](file:///home/anv/Documents/tools/hso-tool/src_java/Main/GameCanvas.java)
Bộ máy trung tâm điều phối Render Loop và Input Loop.

#### Các thuộc tính quan trọng:
* `public static MainScreen currentScreen`: Màn hình đang hiển thị hiện tại (`LoginScreen`, `GameScreen`, `SelectCharScreen`, v.v.).
* `public static int w, h`: Chiều rộng và chiều cao màn hình game.
* `public static int hw, hh`: Nửa chiều rộng và nửa chiều cao màn hình (tọa độ trung tâm).
* `public static long timeNow`: Timestamp thời gian hiện tại của client (dùng tính frame rate, cooldown).
* `public static boolean isPointerSelect`: Trạng thái con trỏ cảm ứng/chuột đang được kích hoạt.

#### Các phương thức quan trọng:
* `public void update()`: Cập nhật logic của màn hình hiện tại `currentScreen.update()` và các dialog thông báo nổi.
* `public void paint(TemGraphics gx)`: Vẽ toàn bộ khung hình: vẽ `currentScreen.paint(g)`, vẽ dialog, vẽ con trỏ chuột, vẽ thông báo chat.
* `public static void resetTrans(mGraphics g)`: Khôi phục lại ma trận tịnh tiến về `(0, 0)`.
* `public static void start_Ok_Dialog(String str)`: Hiển thị hộp thoại thông báo có nút "Đóng/OK".
* `public static void start_Wait_Dialog(String str, iCommand cmd)`: Hiển thị hộp thoại chờ tải ("Đang kết nối...", "Đang tải dữ liệu...").
* `public static void start_Select_Dialog(String str, mVector cmd)`: Hiển thị bảng chọn nhiều phương án.

---

### [GameScreen/GameScreen.java](file:///home/anv/Documents/tools/hso-tool/src_java/GameScreen/GameScreen.java)
Màn hình thế giới in-game khi nhân vật đang chơi.

#### Các thuộc tính quan trọng:
* `public static Player player`: Thể hiện nhân vật của chính người chơi.
* `public static mVector Vecplayers`: Danh sách tất cả người chơi khác đang ở trong cùng khu vực/map.
* `public static mVector VecMonster`: Danh sách quái vật đang tồn tại trong map.
* `public static mVector VecItemDrop`: Danh sách vật phẩm đang rơi dưới đất.
* `public static Camera cameraMain`: Camera theo dõi nhân vật chính và di chuyển khung nhìn thế giới.

#### Các phương thức quan trọng:
* `public void paint(mGraphics g)`: Vẽ toàn bộ thế giới: bản đồ nền, item rơi, quái vật, người chơi, NPC, hiệu ứng skill, thanh HP/MP, giao diện phím tắt.
* `public void update()`: Cập nhật vị trí nhân vật, AI quái, hiệu ứng chiêu thức, camera cuộn theo nhân vật.
* `public void updatekey()`: Xử lý các phím bấm điều khiển (phím 1-9 gán chiêu, phím di chuyển 2-4-6-8 hoặc mũi tên).
* `public void doMenu()`: Mở bảng Menu chức năng (Hành trang, Nhân vật, Kỹ năng, Bang hội, Cài đặt).
* `public void doArea()`: Mở bảng chọn đổi Khu vực (đổi Channel).

---

### [GameScreen/LoginScreen.java](file:///home/anv/Documents/tools/hso-tool/src_java/GameScreen/LoginScreen.java)
Màn hình đăng nhập tài khoản và chọn máy chủ.

#### Các phương thức quan trọng:
* `public void init()`: Khởi tạo các ô nhập tài khoản, mật khẩu và nút bấm.
* `public void saveUser_Pass()`: Lưu tài khoản và mật khẩu đã nhập vào bộ nhớ cục bộ RMS (`MainRMS`).
* `public void loadUser_Pass()`: Đọc tài khoản mật khẩu đã lưu để tự động điền khi mở game.
* `public void showCumSv()`: Hiển thị danh sách các máy chủ (Chiến Thần, Rồng Lửa, v.v.) để chọn kết nối.

---

## 4. Tầng Thực Thể & Nhân Vật (Game Objects)

### [GameObjects/MainObject.java](file:///home/anv/Documents/tools/hso-tool/src_java/GameObjects/MainObject.java)
Lớp cha của mọi đối tượng có mặt trên bản đồ game (Player, Monster, NPC, Pet).

#### Các thuộc tính quan trọng:
* `public int x, y`: Tọa độ vị trí hiện tại trên bản đồ (pixel).
* `public int toX, toY`: Tọa độ đích đang di chuyển tới.
* `public int hp, maxhp`: Máu hiện tại và máu tối đa.
* `public int mp, maxmp`: Năng lượng mana hiện tại và tối đa.
* `public byte dir`: Hướng quay mặt (`DIR_LEFT = 0`, `DIR_RIGHT = 1`, v.v.).
* `public byte Action`: Trạng thái hành động (`ACT_STAND = 0`, `ACT_MOVE = 1`, `ACT_ATTACK = 2`, `ACT_DIE = 4`).
* `public int speed`: Tốc độ di chuyển cơ bản của đối tượng.
* `public boolean isDongBang`: Trạng thái bị đóng băng (không thể di chuyển/đánh).

#### Các phương thức quan trọng:
* `public void paint(mGraphics g)`: Vẽ bóng nhân vật, thân thể, trang bị, đầu, tóc, vũ khí theo frame hoạt ảnh.
* `public void paintAvatarFocus(mGraphics g, int x, int y)`: Vẽ icon avatar khi đối tượng này đang được chọn làm mục tiêu.
* `public static int getDistance(int x1, int y1, int x2, int y2)`: Hàm tiện ích tính khoảng cách giữa hai tọa độ.

---

### [GameObjects/Player.java](file:///home/anv/Documents/tools/hso-tool/src_java/GameObjects/Player.java)
Lớp kế thừa `MainObject`, đại diện cho nhân vật người chơi.

#### Các phương thức quan trọng:
* `public boolean isMainChar()`: Kiểm tra xem đối tượng này có phải là nhân vật của chính người chơi hay không.
* `public void update()`: Cập nhật vị trí, trạng thái cooldown kỹ năng, tự động đánh/nhặt đồ nếu bật auto.
* `public void movePC()`: Xử lý logic di chuyển theo phím bấm bàn phím hoặc click chuột.
* `public void setFocus()`: Tìm và chọn mục tiêu gần nhất (quái hoặc người) để tấn công/giao tiếp.
* `private int setTypeFocus(int typeObj)`: Xác định độ ưu tiên của mục tiêu khi nhấn phím đổi mục tiêu (`typeObj == 0`: quái, `typeObj == 2`: người).
* `public void setStartSkill(int id, boolean isset)`: Kích hoạt xuất chiêu một kỹ năng theo ID.
* `public void setDelaySkill(int t, int index)`: Thiết lập thời gian hồi chiêu (cooldown) cho kỹ năng sau khi tung chiêu.
* `public void autoGetItem()`: Quét và tự động di chuyển lại gần nhặt các vật phẩm rơi xung quanh.

---

### [GameObjects/MainMonster.java](file:///home/anv/Documents/tools/hso-tool/src_java/GameObjects/MainMonster.java)
Lớp quái vật trên bản đồ.

#### Các phương thức quan trọng:
* `public void update()`: Cập nhật hoạt ảnh thở, bước đi tuần tra hoặc đuổi theo người chơi.
* `public void paint(mGraphics g)`: Vẽ sprite quái vật và thanh máu đỏ trên đầu.

---

## 5. Hệ Thống Kỹ Năng & Hiệu Ứng

### [Skill/Skill.java](file:///home/anv/Documents/tools/hso-tool/src_java/Skill/Skill.java)
Quản lý cây kỹ năng của 4 hệ phái: Chiến binh, Sát thủ, Pháp sư, Xạ thủ.

#### Các thuộc tính quan trọng:
* `public short id`: ID định danh của skill.
* `public byte lvSkill`: Cấp độ hiện tại của chiêu (1 - 10).
* `public int timeDelay`: Thời gian hồi chiêu tính bằng mili-giây.
* `public int manaLost`: Lượng mana tiêu hao mỗi lần sử dụng.
* `public short range`: Phạm vi khoảng cách có thể đánh tới mục tiêu.

---

### [GameEffect/AnimateEffect.java](file:///home/anv/Documents/tools/hso-tool/src_java/GameEffect/AnimateEffect.java)
Quản lý các hiệu ứng môi trường trong game.

#### Các hằng số loại hiệu ứng:
* `public static final byte RAIN = 0;` (Mưa rơi)
* `public static final byte FALLING_LEAVES = 1;` (Lá rơi)
* `public static final byte SNOW = 2;` (Tuyết rơi)
* `public static final byte FALLING_FLOWER = 3;` (Hoa đào / hoa mai rơi)
* `public static final byte THIEN_THACH = 4;` (Thiên thạch rơi)
* `public static final byte CLOUD = 5;` (Mây bay tầng nền)

#### Các phương thức quan trọng:
* `public void paint(mGraphics g)`: Điều phối vẽ hiệu ứng tương ứng với `this.type`.
* `private void rndPos(Point pos)`: Tạo ngẫu nhiên tọa độ xuất hiện mới của hạt hiệu ứng xung quanh khung nhìn camera.

---

## 6. Hệ Thống Trang Bị & Cường Hóa

### [GameObjects/Item.java](file:///home/anv/Documents/tools/hso-tool/src_java/GameObjects/Item.java) & [MainItem.java](file:///home/anv/Documents/tools/hso-tool/src_java/GameObjects/MainItem.java)
Đại diện cho trang bị (Vũ khí, Nón, Áo, Quần, Găng tay, Giày, Nhẫn, Dây chuyền, Cánh) và nguyên liệu.

#### Các thuộc tính quan trọng:
* `public short ID`: Mã định danh vật phẩm.
* `public byte type`: Loại trang bị (vũ khí, phòng thủ, hồi phục, ngọc).
* `public byte tier`: Cấp độ cường hóa (+1, +2, ..., +15).
* `public byte colorName`: Phẩm chất màu của trang bị (Trắng, Xanh lá, Xanh dương, Tím, Cam).
* `public MainInfoItem[] itemOption`: Mảng chứa các dòng thuộc tính mở rộng (Sát thương, Né tránh, Chí mạng, Kháng nguyên tố).

---

### [InterfaceComponents/TabRebuildItem.java](file:///home/anv/Documents/tools/hso-tool/src_java/InterfaceComponents/TabRebuildItem.java)
Giao diện và cơ chế thợ rèn: Đập đồ, Khảm ngọc, Ghép ngọc, Đục lỗ.

#### Các chế độ chức năng:
* `TYPE_REBUILD = 0`: Cường hóa trang bị bằng đá may mắn.
* `TYPE_REPLACE_PLUS = 1`: Chuyển đổi cấp cường hóa giữa hai trang bị.
* `TYPE_KHAM_NGOC = 3`: Khảm ngọc vào lỗ đã đục trên trang bị.
* `TYPE_GHEP_NGOC = 4`: Hợp thể 5 viên ngọc cấp thấp thành 1 viên ngọc cấp cao.
* `TYPE_DUC_LO = 5`: Đục thêm lỗ khảm trên trang bị.

---

## 7. Bảng Tra Cứu Nhanh Cho Lập Trình Viên / Modder

| Mục tiêu can thiệp | File cần sửa | Hàm / Thuộc tính cần chú ý | Ghi chú |
| :--- | :--- | :--- | :--- |
| **Đổi IP / Port Server** | `GameScreen/LoginScreen.java`<br>`CLib/Session_ME.java` | `LoginScreen.showCumSv()`<br>`Session_ME.connect(host, port)` | Trỏ IP về VPS hoặc `localhost:19129` để kết nối server riêng |
| **Thay đổi tốc độ chạy** | `GameObjects/MainObject.java`<br>`GameObjects/Player.java` | `this.speed`<br>`Player.movePC()` | Client gửi tọa độ qua `GlobalService.player_move()` |
| **Can thiệp Cooldown** | `GameObjects/Player.java` | `Player.setDelaySkill(t, index)` | Cooldown client chỉ là hiển thị giao diện, server vẫn check thời gian thực |
| **Auto Đánh & Nhặt đồ** | `GameObjects/Player.java` | `Player.autoGetItem()`<br>`Player.setFocus()`<br>`GlobalService.fire_monster()` | Có thể viết logic tự tìm quái trong `GameScreen.VecMonster` và gọi đánh liên tục |
| **Bỏ giới hạn FPS** | `Main/GameCanvas.java` | `GameCanvas.update()` | Thay đổi biến `timeDelay` trong vòng lặp chính của canvas |
| **Đổi đồ họa / Sprite** | Thư mục `jar_resources/` | `jar_resources/x1/`, `weapon/`, `c/` | Thay thế các file ảnh PNG tương ứng rồi repack lại file jar |
