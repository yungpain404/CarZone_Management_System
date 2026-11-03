---
name: java-architecture
description: Dùng khi tạo một feature slice, thêm lớp/package, refactor luồng CarZone, hoặc cần quyết định code đặt ở ui, controller, dao, entity hay session. Không dùng để thiết kế schema database chi tiết.
---

# Kiến trúc ứng dụng Java

## Mục đích

Giữ luồng phụ thuộc một chiều và đặt code đúng tầng. Dự án không có service layer.

## Kiến trúc chuẩn

```text
ui -> controller -> dao (interface) -> dao.impl -> connectDB.DbUtil -> SQL Server
                                  \-> entity/enums
session <- controller/ui (đọc phiên)
```

## Trách nhiệm từng tầng

### `ui`

- Dựng Swing, bố cục, sự kiện, trạng thái hiển thị.
- Gọi controller, nhận DTO/kết quả.
- Không SQL, không `DbUtil`, không tính tiền, không kiểm tra quyền thay controller.
- Tác vụ DB chạy qua `SwingWorker`.

### `controller`

- Kiểm tra quyền từ `UserSession`.
- Validate đầu vào sớm.
- Điều phối một hoặc nhiều DAO.
- Dịch lỗi DB 50xxx sang tiếng Việt.
- Trả DTO/kết quả cho UI.
- Không SQL; không mở `Connection` trực tiếp.

### `dao`

- Interface nghiệp vụ, không SQL.
- Tên `<Entity>Dao`, không tiền tố `I`.

### `dao/impl`

- JDBC thuần, `PreparedStatement`, `CallableStatement`.
- `try-with-resources`.
- Mapping ResultSet, dùng raw setter cho dữ liệu DB.
- Gọi procedure khi DB đã có.
- Không validate lại nghiệp vụ DB đã làm, trừ khi cần thông báo sớm.

### `entity`

- POJO, validation trong setter thường.
- Raw setter cho DAO.
- Không annotation JPA.
- Không gọi DB.

### `session`

- `UserSession` Singleton.
- Chỉ giữ thông tin phiên; không chứa logic nghiệp vụ.

## Công thức thêm một feature slice

1. Xác định UC và quyền.
2. Kiểm tra entity đã đủ trường chưa.
3. Tạo/sửa DAO interface.
4. Tạo/sửa DAO impl.
5. Tạo/sửa controller.
6. Tạo/sửa UI.
7. Đăng ký menu/trang nếu cần.
8. Test.

## Quy ước đặt tên

- UI: `<NghiepVu>UI`, hậu tố `UI`.
- Controller: `<NghiepVu>Controller`.
- DAO: `<Entity>Dao`; impl `<Entity>DaoImpl`.
- DTO nội bộ: lớp static trong controller hoặc `dto` nếu đã có quy ước; không tạo package mới tùy tiện.
- Phương thức tiếng Việt không dấu theo code hiện tại: `layDanhSach`, `timTheo...`, `capNhat...`.

## Xử lý lỗi

- DAO bọc `SQLException` thành `RuntimeException` có nguyên nhân, hoặc ném exception nghiệp vụ nếu có.
- Controller bắt và dịch mã 50xxx.
- UI hiển thị thông báo thân thiện, không stack trace.
- Không nuốt exception bằng catch rỗng.
- Không trả `null` mơ hồ; dùng kết quả có trạng thái/thông báo khi phù hợp.

## Chống chỉ định

- Không tạo `service` layer.
- Không thêm Hibernate/JPA.
- Không để controller/UI chứa SQL.
- Không để DAO mở transaction kéo dài chờ người dùng.
- Không để UI gọi DAO trực tiếp.
- Không để entity gọi DAO.
- Không dùng static tiện ích để che giấu phụ thuộc.

## Nợ kỹ thuật phải biết

- `DashboardController` đang chứa SQL trực tiếp; khi sửa, chuyển sang DAO.
- `DashboardUI`/`MainFrame` đang có SQL và dữ liệu mặc định; không nhân rộng.
- Chưa có DAO cho phần lớn nghiệp vụ; tạo mới theo quy ước, không nhồi vào DAO hiện có.

## Checklist

- [ ] Code đúng tầng.
- [ ] Không SQL sai tầng.
- [ ] Không service/Hibernate.
- [ ] DAO đóng tài nguyên.
- [ ] Controller kiểm quyền và dịch lỗi.
- [ ] UI không xử lý nghiệp vụ.
- [ ] Tên lớp/phương thức khớp quy ước.
- [ ] Có test hoặc kế hoạch test.

## Liên quan

- `01-analysis/project-context-analysis`
- `02-architecture/database-transaction-integrity`
- `03-ui/swing-ui-development`
- skill cũ `ai-context/skills/new-feature-slice.md`
- skill cũ `ai-context/skills/jdbc-dao-mapping.md`
