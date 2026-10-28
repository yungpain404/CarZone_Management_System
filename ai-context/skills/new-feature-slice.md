---
name: new-feature-slice
description: Dùng khi thêm một chức năng/thực thể mới cho CarZone cần đi qua nhiều lớp (migration, entity, enum, dao, controller, ui). Không dùng cho sửa lỗi nhỏ trong một lớp.
---

# Thêm một chức năng theo lát cắt dọc

## Trạng thái hiện tại cần biết
- Dự án sử dụng **Java 17**, **FlatLaf 3.7**, **JDBC thuần (DriverManager)** qua `connectDB/DbUtil` và `connectDB/DatabaseConfig`. Đã loại bỏ hoàn toàn Hibernate/JPA.
- Kiến trúc phân tầng: `ui` → `controller` → `dao` (interface & `dao.impl`) → `entity`/`enums`. Quản lý phiên làm việc qua `session.UserSession` (Singleton).
- Tầng DAO:
  + Interface đặt trong `src/dao/` (không có tiền tố `I`, ví dụ `NhanVienDao.java`).
  + Lớp thực thi đặt trong `src/dao/impl/` (ví dụ `NhanVienDaoImpl.java`).
- DB (V1 + V2) đã có trigger/stored procedure/view cho nhiều luật. Đọc phần liên quan trong V1 trước khi viết kiểm tra ở Java; theo phân vai hai tầng trong `ai-context/decisions.md` (Java không sinh mã do procedure sinh, không trừ/cộng tồn).

## Trước khi viết code
1. Đọc `ai-context/PROGRESS.md`, `ai-context/business-rules.md`, `ai-context/decisions.md`.
2. Tìm use case và đặc tả entity của chức năng trong báo cáo Word (chỉ đọc đúng mục).
3. Liệt kê ngắn: entity/bảng nào, vai trò nào, trạng thái nào, luật nào đã nằm sẵn trong trigger/procedure (mã lỗi nào), luật nào kiểm tra ở tầng Java.
4. Có chỗ không chắc hoặc tài liệu, DB, entity lệch nhau thì hỏi, không tự suy diễn.

## Thứ tự làm (mỗi bước biên dịch được trước khi sang bước sau)
1. **Migration mới** nếu cần đổi schema CSDL (skill `flyway-migration`).
2. **`enums`, `entity`**: POJO thuần; validate logic trong các setter; bổ sung raw setter (`setXxxRaw(...)`) cho DAO khi map dữ liệu từ DB.
3. **`dao`**:
   + Tạo Interface trong `src/dao/XxxDao.java`.
   + Tạo lớp thực thi trong `src/dao/impl/XxxDaoImpl.java` sử dụng JDBC thuần, `DbUtil.getConnection()`, try-with-resources.
   + Đọc các trường tính toán từ view `vw_*` nếu có.
   + Chi tiết xem skill `jdbc-dao-mapping`.
4. **`controller`**: Nhận yêu cầu từ UI, kiểm tra phiên đăng nhập từ `UserSession`, điều phối gọi DAO, xử lý ngoại lệ `SQLException` và dịch mã lỗi CSDL (50xxx) thành thông báo tiếng Việt.
5. **`ui`**: Xây dựng form bám sát Figma, kết nối qua `MainFrame` hoặc form độc lập, chạy các tác vụ nặng qua `SwingWorker` ở luồng nền (skill `swing-screen`).
6. **Kiểm tra biên dịch**: `mvn -q compile`, rồi kiểm tra lại luật theo skill `verify-business-rules`.
7. **Cập nhật tiến độ**: Ghi nhận vào `ai-context/PROGRESS.md`.

## Cấm
- Đặt luật nghiệp vụ hoặc viết câu lệnh SQL trong `ui`.
- Sử dụng dữ liệu mockup/fake trong `ui` hoặc `controller`.
- Cho người dùng nhập mã tự sinh hoặc tổng tiền.
- Sửa các file migration cũ (V1, V2).
- Thêm thư viện vào `pom.xml` khi chưa được đồng ý.
