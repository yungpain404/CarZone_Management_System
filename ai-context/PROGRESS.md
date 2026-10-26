# Tiến độ CarZone

> Agent đọc file này đầu mỗi phiên và cập nhật khi xong việc. Giữ ngắn, dưới 60 dòng. Trạng thái dưới đây lấy từ hai bản zip ngày 25/10/2026.

## Đã xong
- Nhánh `develop`: 14 entity (`src/entity`) và 6 enum (`src/enums`), biên dịch được bằng `javac`. `pom.xml`, tài liệu trong `ai-context/`.
- Nhánh `feature/infrastructure`: `infra/docker/compose.yml` (SQL Server → tạo DB → Flyway), `V1__init_CarZone_Database.sql` (bảng, trigger, stored procedure, view, dữ liệu danh mục/nhân sự), `V2__update_KhachHang_Entity.sql`, seed `CarZone_Demo.sql` (kịch bản khách Nguyễn Văn A).

## Mới làm (26/10/2026)
- `connectDB`: `DatabaseConfig` (đọc `.env`) và `JpaUtil`.
- Entity refactor sang Hibernate theo V1/V2 (pom + persistence.xml cập nhật), mật khẩu băm SHA-256; mới biên dịch bằng stub và kiểm cột tĩnh, CHƯA chạy với Hibernate/DB thật.
- **Sửa build**: xóa `<testSourceDirectory>src</testSourceDirectory>` khỏi `pom.xml`, thêm `<testResources/>` rỗng, sửa `.classpath` để Eclipse compile `src` → `target/classes`. Xóa `bin/` cũ. Hết trùng lặp `target/test-classes`.
- **Usecase đăng nhập & UI hoàn chỉnh (26/10/2026)**: `dao/TaiKhoanDao`, `service/DangNhapService`, `ui/DangNhapUI`, `ui/Main`. Bám sát tuyệt đối Figma, chuẩn FlatLightLaf, xử lý triệt để lỗi lệch layout Swing, hoàn thiện bo góc 12px cho các nút, nhúng logo CarZone và đầy đủ luồng usecase.
- **Splash Screen (Frame Khởi động Figma 95:681 - 26/10/2026)**: `ui/KhoiDongUI`. Tái hiện chuẩn xác 100% frame "Carzone · Khởi động", thanh tiến trình RoundedProgressBar 280×4px bo tròn pill, thông điệp khởi động động, pre-warm kết nối Hibernate ở background thread, chuyển tiếp mượt mà sang `DangNhapUI`.
- **Quy chuẩn UI**: Viết lại skill `ai-context/skills/swing-screen.md` và cập nhật `AGENTS.md` về quy tắc layout chống lệch, bo góc nút bấm và luồng use case.

## Chưa có
- DB Docker chưa chạy thử với Hibernate thật (câu hỏi mở 1).
- `ui` còn thiếu: màn hình chính cho từng vai trò (Dashboard, quản lý linh kiện, lập phiếu sửa chữa, lập hóa đơn, v.v.).

## Đang làm
- Đã hoàn thành Splash Screen (`KhoiDongUI`) và Màn hình Đăng nhập (`DangNhapUI`). Chuẩn bị phát triển màn hình chính theo vai trò.

## Việc kế tiếp (gợi ý, chờ người dùng chốt thứ tự)
- Chạy DB Docker, thử kết nối Hibernate, xử lý câu hỏi mở 1.
- Sau đó làm màn hình chính theo vai trò (Dashboard, quản lý linh kiện…).

## Ghi chú cho phiên sau
- `ui.Main` đang gọi `JOptionPane` tạm sau đăng nhập; cần thay bằng màn hình chính thật.
- `DangNhapFrame`: toggle nhớ tài khoản chưa lưu `Preferences`; đánh dấu TODO.

