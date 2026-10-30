---
name: new-feature-slice
description: Dùng khi thêm một chức năng/thực thể mới cho CarZone cần đi qua nhiều lớp (migration, entity, enum, dao, controller, ui). Không dùng cho sửa lỗi nhỏ trong một lớp.
---

# Thêm một chức năng theo lát cắt dọc

## Trạng thái hiện tại cần biết
- Dự án sử dụng **Java 17**, **FlatLaf 3.7**, **JDBC thuần (DriverManager)** qua `connectDB/DbUtil` và `connectDB/DatabaseConfig`. Đã loại bỏ hoàn toàn Hibernate/JPA.
- Kiến trúc phân tầng: `ui` → `controller` → `dao` → `entity`/`enums`. Quản lý phiên làm việc qua `session.UserSession` (Singleton).
- **Không còn package `service`** (gỡ hoàn toàn 30/10/2026): `src/` chỉ có 8 package `app, connectDB, controller, dao, entity, enums, session, ui`. Lớp điều phối/tổng hợp nằm trong `controller` và tên theo quy ước `*Controller` (ví dụ `ThongKeDoanhThuController`). Entry point là `app.App` (`pom.xml` `main.class = app.App`), không còn `Main.java`.
- **Ảnh** nằm ở thư mục gốc `img/` (không còn `src/resources/images/`, không có `.png` trong `src/ui/`); `pom.xml` trỏ `<resources>` vào `img/` nên ảnh nằm ở gốc classpath — đọc bằng `getResource("/ten_anh.png")`, chi tiết xem skill `swing-screen`.
- Tầng DAO:
  + **MỘT class duy nhất** trong `src/dao/<Ten>Dao.java` — viết hoa ký tự đầu, không tiền tố `I`, không hậu tố `Impl` (ví dụ `NhanVienDao.java`).
  + **Không còn interface và không còn `src/dao/impl/`** (bỏ từ 30/10/2026); class đó chính là lớp thực thi JDBC thuần.
- DB (V1 + V2) đã có trigger/stored procedure/view cho nhiều luật. Đọc phần liên quan trong V1 trước khi viết kiểm tra ở Java; theo phân vai hai tầng trong `ai-context/decisions.md` (Java không sinh mã do procedure sinh, không trừ/cộng tồn).

## Trước khi viết code
1. Đọc `ai-context/PROGRESS.md`, `ai-context/business-rules.md`, `ai-context/decisions.md`, rồi làm theo skill `tranh-conflict` (tránh xung đột với nhóm).
2. Tìm use case và đặc tả entity của chức năng trong báo cáo Word (chỉ đọc đúng mục).
3. Liệt kê ngắn: entity/bảng nào, vai trò nào, trạng thái nào, luật nào đã nằm sẵn trong trigger/procedure (mã lỗi nào), luật nào kiểm tra ở tầng Java.
4. Có chỗ không chắc hoặc tài liệu, DB, entity lệch nhau thì hỏi, không tự suy diễn.

## Thứ tự làm (mỗi bước biên dịch được trước khi sang bước sau)
1. **Migration mới** nếu cần đổi schema CSDL (skill `flyway-migration`).
2. **`enums`, `entity`**: POJO thuần; validate logic trong các setter; bổ sung raw setter (`setXxxRaw(...)`) cho DAO khi map dữ liệu từ DB.
   + **Không tạo lớp `*Id.java`** cho khóa ghép (đã gỡ 30/10/2026): cột khóa ghép (`maPhieu`, `maDV`, `maLK`, `stt`, ...) là field thường của entity, không bọc trong object id kiểu JPA (`ChiTietDichVuId`, ...).
   + **Định dạng khai báo field**: mỗi thuộc tính nằm trên một dòng riêng; **không** để dòng trắng giữa hai khai báo field liền kề, **không** gộp nhiều field vào cùng một dòng; javadoc chú thích đặt ngay trên field đó.
3. **`dao`**:
   + Tạo lớp DAO trong `src/dao/XxxDao.java` (viết hoa ký tự đầu, không hậu tố `Impl`) — JDBC thuần, `DbUtil.getConnection()`, try-with-resources. **Không** tạo interface và **không** dùng `src/dao/impl/`.
   + Đọc các trường tính toán từ view `vw_*` nếu có.
   + Chi tiết xem skill `jdbc-dao-mapping`.
4. **`controller`**: Nhận yêu cầu từ UI, kiểm tra phiên đăng nhập từ `UserSession`, điều phối gọi DAO, xử lý ngoại lệ `SQLException` và dịch mã lỗi CSDL (50xxx) thành thông báo tiếng Việt.
5. **`ui`**: Xây dựng form bám sát Figma, kết nối qua `MainFrame` hoặc form độc lập, chạy các tác vụ nặng qua `SwingWorker` ở luồng nền (skill `swing-screen`).
6. **Kiểm tra biên dịch**: `mvn -q compile`, rồi kiểm tra lại luật theo skill `verify-business-rules`.
7. **Cập nhật tiến độ**: Ghi nhận vào `ai-context/PROGRESS.md`.

## Tránh conflict khi nhiều người cùng làm
Làm theo skill `tranh-conflict` (đọc đầu phiên, đặc biệt khi `git status` có `UU`): không tái tạo cấu trúc đã gỡ, chỉ sửa đúng đoạn cần, cập nhật tài liệu cùng lúc với code, không tự `git add`/`commit`/`merge`.

## Cấm
- Đặt luật nghiệp vụ hoặc viết câu lệnh SQL trong `ui`.
- Sử dụng dữ liệu mockup/fake trong `ui` hoặc `controller`.
- Cho người dùng nhập mã tự sinh hoặc tổng tiền.
- Sửa các file migration cũ (V1, V2).
- Thêm thư viện vào `pom.xml` khi chưa được đồng ý.
- Tạo lại cấu trúc đã gỡ ngày 30/10/2026: package `service`, thư mục `src/dao/impl/`, interface `I*Dao`, lớp `*Id.java` khóa ghép trong `entity`, thư mục `src/resources/images/`, `.png` trong `src/ui/`, hay `src/app/Main.java`.
- Format lại toàn file/thư mục không thuộc phạm vi việc đang làm.
