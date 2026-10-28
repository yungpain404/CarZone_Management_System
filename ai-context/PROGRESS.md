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

## Mới làm (28/10/2026)
- **Tái cấu trúc Session & Controller**:
  + Tạo package `session` với `UserSession` (quản lý người dùng, nhân viên, vai trò, thời điểm đăng nhập toàn cục theo mô hình Singleton).
  + Chuyển đổi toàn bộ `service` sang `controller`: `AuthController`, `DangNhapController`, `DashboardController` đặt tại package `controller`. Xóa hoàn toàn thư mục `service`.
  + `AuthController`: Xác thực trực tiếp với Database qua `TaiKhoanDao`, đối chiếu băm SHA-256 hex, kiểm tra trạng thái hoạt động, cấp quyền và danh sách navigation menu theo Role, quản lý phiên và xử lý đăng xuất an toàn. Loại bỏ 100% dữ liệu mockup.
- **Hoàn thiện MainFrame chuẩn mực theo Figma (image45.png)**:
  + Tạo [MainFrame.java](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/ui/MainFrame.java) làm khung chính điều phối (Master Frame) chứa Header, Navigation và ContentPanel.
  + Header tự động cập nhật avatar, họ tên, vai trò và mã NV từ Session; search bar co giãn responsive với placeholder theo role.
  + Navigation lọc chuẩn xác danh mục chức năng theo Role; bo góc 10px, hiệu ứng active nền `#EFF6FF` viền `#BFDBFE` chữ xanh `#2563EB`.
  + ContentPanel (CardLayout) mở đúng Page tương ứng với navigation được chọn, nạp dữ liệu thật từ DB.
  + Di chuyển file khởi chạy chính sang [Main.java](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/app/Main.java) (`package app;`), cấu hình `main.class = app.Main` trong `pom.xml`.

- **Mở rộng dữ liệu Seed Demo & Hoàn thiện thông báo lỗi Đăng nhập (28/10/2026)**:
  + Mở rộng `infra/database/seed/CarZone_Demo.sql`: Bổ sung danh mục dịch vụ (`DV00004` -> `DV00007`), linh kiện (`LK00004` -> `LK00009` với 2 linh kiện tồn 0 kích hoạt cảnh báo hết hàng Figma), 6 khách hàng & xe (Toyota, Mazda, Honda, Hyundai, Kia, Ford), phiếu sửa chữa ở đủ 4 trạng thái (`CHO_SUA`, `DANG_SUA`, `CHO_LINH_KIEN`, `HOAN_TAT`), hóa đơn thanh toán tiền mặt & chuyển khoản, và các phiếu yêu cầu nhập linh kiện theo đúng luồng chuyển trạng thái (`CHO_DUYET`, `DA_DUYET`, `DA_NHAP_KHO`, `TU_CHOI`). Đã chạy nạp thành công vào SQL Server.
  + Sửa triệt để lỗi thông báo khi đăng nhập sai trong [DangNhapUI.java](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/ui/DangNhapUI.java): Ngăn `DocumentListener` tự động xoá khung lỗi khi gọi `pfMatKhau.setText("")`, làm nổi bật viền đỏ trường nhập, cảnh báo âm thanh `Toolkit.beep()`, cập nhật thanh trạng thái và hiển thị `JOptionPane.showMessageDialog` rõ ràng.

- **Chuyển đổi ORM Hibernate sang JDBC thuần & Tái cấu trúc DAO (28/10/2026)**:
  + Gỡ bỏ hoàn toàn `hibernate-core`, `flyway-core`, `flyway-sqlserver` khỏi `pom.xml`.
  + Thay thế `JpaUtil` bằng `connectDB.DbUtil` quản lý kết nối JDBC `DriverManager`.
  + Lược bỏ toàn bộ JPA annotation (`@Entity`, `@Table`, `@Id`, v.v.) khỏi các entity, giữ nguyên nghiệp vụ và validation.
  + Chuẩn hóa kiến trúc DAO theo mẫu người dùng cung cấp:
    * Interface DAO đặt trực tiếp trong package `dao` (không có tiền tố `I`): [TaiKhoanDao](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/dao/TaiKhoanDao.java), [NhanVienDao](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/dao/NhanVienDao.java), [HoaDonDao](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/dao/HoaDonDao.java), [ChiTietHoaDonDao](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/dao/ChiTietHoaDonDao.java), [PasswordResetDao](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/dao/PasswordResetDao.java).
    * Lớp thực thi đặt trong subpackage `dao.impl`: [TaiKhoanDaoImpl](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/dao/impl/TaiKhoanDaoImpl.java), [NhanVienDaoImpl](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/dao/impl/NhanVienDaoImpl.java), [HoaDonDaoImpl](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/dao/impl/HoaDonDaoImpl.java), [ChiTietHoaDonDaoImpl](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/dao/impl/ChiTietHoaDonDaoImpl.java), [PasswordResetDaoImpl](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/dao/impl/PasswordResetDaoImpl.java).
  + Chuyển đổi [DashboardController](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/controller/DashboardController.java), [AuthController](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/controller/AuthController.java), [DashboardUI](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/ui/DashboardUI.java), [MainFrame](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/ui/MainFrame.java), [KhoiDongUI](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/ui/KhoiDongUI.java) sang JDBC thuần qua `DbUtil`.
  + Kiểm thử biên dịch toàn dự án: `mvn clean compile` thành công 100% (46 source files, 0 error).

## Việc kế tiếp (gợi ý, chờ người dùng chốt thứ tự)
- Phát triển màn hình chức năng chuyên sâu: Tiếp nhận xe / Lập phiếu tiếp nhận sửa chữa (TNV) hoặc Quản lý linh kiện và kho (CCH).
