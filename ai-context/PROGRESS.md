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
- **Dashboard phân quyền theo vai trò (Figma chuẩn 1:1 - Cập nhật 27/10/2026)**:
  + Thanh tìm kiếm Header bo tròn pill 22px với kính lúp vector chống răng cưa, co giãn linh hoạt (responsive `GridBagLayout` min 140px, max 480px), không bị cố định chiều rộng làm tràn layout khi thu nhỏ cửa sổ.
  + Giãn chiều cao các khối của Dashboard Chủ cửa hàng: thẻ KPI cao 135px, khối công việc cao 285px, khối kinh doanh trong ngày cao 235px cùng khoảng cách `18-20px` tạo không gian thoáng đãng, sang trọng, không còn bị thu hẹp quá mức.
  + Tái hiện chuẩn xác 100% frame Figma mới của Dashboard Kỹ thuật viên: Breadcrumb điều hướng, Tiêu đề kèm icon công cụ 3D, Card bộ lọc Kỳ thống kê (Hôm nay/Tuần/Tháng, Từ ngày, Đến ngày, Trạng thái), Hàng 4 thẻ Summary Card (Phiếu mới phân công, Sửa chữa đang thực hiện, Yêu cầu chờ duyệt, Phản hồi cần xử lý), Thanh Tabs điều hướng nhanh (Tổng quan | Phiếu được phân công | Yêu cầu linh kiện), Bảng công việc chi tiết và Card 5 ô chỉ đọc thông tin phiếu đang chọn.

## Việc kế tiếp (gợi ý, chờ người dùng chốt thứ tự)
- Phát triển màn hình chức năng kế tiếp: Tiếp nhận xe / Lập phiếu tiếp nhận sửa chữa (TNV) hoặc Quản lý linh kiện và kho (CCH).
- Chạy DB Docker, thử kết nối Hibernate và seed data.

