---
name: new-feature-slice
description: Dùng khi thêm một chức năng/thực thể mới cho CarZone cần đi qua nhiều lớp (migration, entity, enum, dao, service, ui). Không dùng cho sửa lỗi nhỏ trong một lớp.
---

# Thêm một chức năng theo lát cắt dọc

## Trạng thái hiện tại cần biết
- `entity` (14 lớp + 3 lớp `*Id`) và `enums` (6) đã có annotation Hibernate/JPA khớp DB; `persistence.xml` ở `src/META-INF`. `connectDB` đã có `DatabaseConfig` + `JpaUtil`; `dao`, `service`, `ui` còn trống.
- Pom đã có Hibernate 7.4.13.Final và FlatLaf 3.7 (Maven tải được; mapping chưa chạy thử với DB thật, "Câu hỏi mở" 1). Thêm dependency khác thì hỏi người dùng.
- Viết dao theo skill `hibernate-entity-mapping`.
- DB (V1) đã có trigger/stored procedure/view cho nhiều luật. Đọc phần liên quan trong V1 trước khi viết kiểm tra ở Java; theo phân vai hai tầng trong `ai-context/decisions.md` (Java không sinh mã do procedure sinh, không trừ/cộng tồn).

## Trước khi viết code
1. Đọc `ai-context/PROGRESS.md`, `ai-context/business-rules.md`, `ai-context/decisions.md`.
2. Tìm use case và đặc tả entity của chức năng trong báo cáo Word (chỉ đọc đúng mục).
3. Liệt kê ngắn: entity/bảng nào, vai trò nào, trạng thái nào, luật nào đã nằm sẵn trong trigger (mã lỗi nào), luật nào còn thiếu.
4. Có chỗ không chắc hoặc tài liệu, DB, entity lệch nhau thì hỏi, không suy diễn.

## Thứ tự làm (mỗi bước biên dịch được trước khi sang bước sau)
1. Migration mới nếu cần đổi schema (skill `flyway-migration`).
2. `enums`, `entity`: giữ tên thuộc tính theo tài liệu; trường dẫn xuất chỉ có getter.
3. `dao`: truy vấn/lưu, không nghiệp vụ. Ưu tiên đọc dữ liệu dẫn xuất từ view `vw_*` thay vì tính lại.
4. `service`: kiểm vai trò người gọi, chuyển trạng thái, transaction; dịch lỗi DB (mã 50xxx) thành thông báo tiếng Việt cho `ui`.
5. `ui`: skill `swing-screen`.
6. `mvn -q compile` (hoặc `javac` nếu chưa tải được thư viện), rồi skill `verify-business-rules`.
7. Cập nhật `ai-context/PROGRESS.md`.

## Cấm
- Đặt luật nghiệp vụ trong `ui` hoặc `dao`.
- Cho người dùng nhập mã tự sinh hoặc tổng tiền.
- Sửa migration cũ.
- Đổi tên thuộc tính khác với tài liệu thiết kế.
- Thêm thư viện vào `pom.xml` khi chưa được đồng ý.
