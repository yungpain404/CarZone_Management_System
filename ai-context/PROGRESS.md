# Tiến độ CarZone

> Agent đọc file này đầu mỗi phiên và cập nhật khi xong việc. Giữ ngắn, dưới 70 dòng. Trạng thái dưới đây lấy từ hai bản zip ngày 25/10/2026.

## Đã xong
- Nhánh `develop`: 14 entity (`src/entity`) và 6 enum (`src/enums`), biên dịch được bằng `javac`. `pom.xml`, tài liệu trong `ai-context/`.
- Nhánh `feature/infrastructure`: `infra/docker/compose.yml` (SQL Server → tạo DB → Flyway), `V1__init_CarZone_Database.sql` (bảng, trigger, stored procedure, view, dữ liệu danh mục/nhân sự), `V2__update_KhachHang_Entity.sql`, seed `CarZone_Demo.sql` (kịch bản khách Nguyễn Văn A).

## Mới làm (26/10/2026)
- `connectDB`: `DatabaseConfig` (đọc `.env`) và `JpaUtil`.
- Entity refactor sang Hibernate theo V1/V2 (pom + persistence.xml cập nhật), mật khẩu băm SHA-256; mới biên dịch bằng stub và kiểm cột tĩnh, CHƯA chạy với Hibernate/DB thật.
- **Sửa build**: xóa `<testSourceDirectory>src</testSourceDirectory>` khỏi `pom.xml`, thêm `<testResources/>` rỗng, sửa `.classpath` để Eclipse compile `src` → `target/classes`. Xóa `bin/` cũ. Hết trùng lặp `target/test-classes`.
- **Usecase đăng nhập & UI hoàn chỉnh (26/10/2026)**: `dao/TaiKhoanDao`, `service/DangNhapService`, `ui/DangNhapUI`, `ui/Main`. Bám sát tuyệt đối Figma, chuẩn FlatLightLaf, xử lý triệt để lỗi lệch layout Swing, hoàn thiện bo góc 12px cho các nút, nhúng logo CarZone và đầy đủ luồng usecase.
- **Splash Screen (Frame Khởi động Figma 95:681 - 26/10/2026)**: `ui/KhoiDongUI`. Tái hiện chuẩn xác 100% frame "Carzone · Khởi động", thanh tiến trình RoundedProgressBar 280×4px bo tròn pill, thông điệp khởi động động, pre-warm kết nối Hibernate ở background thread, chuyển tiếp mượt mà sang `DangNhapUI`.
- **Dashboard phân quyền theo vai trò (Figma chuẩn 1:1 - Cập nhật 27/10/2026)**: Header, Navigation sidebar, ContentPanel CardLayout theo role.

## Mới làm (28/10/2026)
- **Tái cấu trúc Session & Controller**: `session.UserSession` Singleton, chuyển `service` → `controller`, xóa thư mục `service`.
- **Chuyển đổi ORM Hibernate sang JDBC thuần (28/10/2026)**: Gỡ bỏ Hibernate, Flyway khỏi Java. `connectDB.DbUtil` JDBC DriverManager. Lược bỏ toàn bộ JPA annotation khỏi entity. Chuẩn hóa kiến trúc DAO Interface/Impl.
- **Mở rộng dữ liệu Seed Demo**: 6 khách hàng & xe, phiếu sửa chữa đủ 4 trạng thái, hóa đơn, yêu cầu linh kiện. `mvn clean compile` thành công 100%.

## Mới làm (03/11/2026 – Hoàn thiện toàn bộ phân hệ Tiếp nhận viên TNV)
- **DAO & Controllers**: 10 DAO interface & impl JDBC thuần. 6 Controllers: KhachHangVaXeController, LapPhieuTiepNhanController, LapHoaDonThanhToanController, TaoPhieuDoiTraController, TraCuuController, ThongKeTiepNhanController.
- **Giao diện UI**:
  + **QuanLyKhachHangVaXeUI** (UC016): Bố cục 2 cột, Modal thêm/sửa KH và Xe, tìm kiếm đa năng SĐT/Biển số/Mã/Họ tên.
  + **LapPhieuTiepNhanUI** (UC010): Quản lý phiếu tiếp nhận – bảng danh sách + bộ lọc trạng thái (badge màu). Sửa phiếu khi CHO_SUA trong modal chi tiết phiếu. F2 tạo mới, F5 làm mới.
  + **LapHoaDonThanhToanUI** (UC014, UC015): 2 Tab (Chờ TT / Lịch sử). Modal thanh toán Figma 100%, tính VAT, khuyến mãi, Tiền mặt/Chuyển khoản. Print Preview + PrinterJob. Sửa crash tồn kho khi xem chi tiết (raw setters).
  + **TraCuuUI** (UC007): 4 tab Dịch vụ, Linh kiện, Khách hàng, Lịch sử tiếp nhận.
  + **ThongKeTiepNhanUI** (UC013): Dashboard 3 phần, DatePickerField popup lịch.

## Mới làm (04/11/2026 – Quản lý phiếu đổi trả UC012, Thống kê đổi trả & Nâng cấp Dashboard Tiếp nhận viên)
- **TaoPhieuDoiTraUI** tái cấu trúc hoàn toàn thành màn hình Quản lý phiếu đổi trả:
  + Bảng danh sách tất cả phiếu đổi trả, tìm kiếm đa năng (mã phiếu/HĐ gốc/biển số/khách hàng), F2/F5.
  + `ChiTietPhieuDoiTraModalDialog`: Hiển thị HĐ gốc, xe, lý do, bảng hạng mục kèm badge hướng xử lý màu và tổng tiền hoàn bằng chữ. Fullscreen không che component.
  + `TaoPhieuDoiTraModalDialog`: Lọc HĐ đủ 7 ngày, chọn DV/LK, spinner SL còn lại tự cập nhật, hướng xử lý phù hợp (DV/LK), tính tổng tức thời, confirm dialog. Fullscreen.
  + `HuongXuLyBadgeRenderer` badge màu cho cả hai modal.
- **TaoPhieuDoiTraController**: Bổ sung `layTatCaPhieuDoiTra()` và `timKiemTatCaPhieuDoiTra()`.
- **ThongKeTiepNhanUI (Phần 4: Thống kê đổi trả - bảo hành)**:
  + KPI hôm nay (số lượng + tiền hoàn), KPI tháng này, số hạng mục hoàn tiền, đổi mới, sửa lại.
  + Biểu đồ xu hướng phiếu đổi trả 7 ngày gần nhất, phân bố 3 hướng xử lý theo badge màu.
  + Bảng danh sách 10 phiếu đổi trả gần nhất.
  + DAO `ThongKeTiepNhanDao`, `ThongKeTiepNhanDaoImpl` và `ThongKeTiepNhanController` kết nối dữ liệu hoàn chỉnh.
- **Nâng cấp Dashboard Tiếp nhận viên (DashboardUI & DashboardController)**:
  + 4 Thẻ KPI tức thời: Tiếp nhận hôm nay, Cần phân công KTV (cảnh báo đỏ khi > 0), Xe đang sửa, Chờ thu tiền & giao xe (xanh lá).
  + Biểu đồ 1: Tình trạng xe xưởng (Stacked Progress Bar + 4 box trạng thái và tỷ lệ %).
  + Biểu đồ 2: Tải công việc Kỹ thuật viên (Bar chart ngang hiển thị số xe KTV đang nhận, badge "Sẵn sàng" khi 0 xe).
  + Action Center: Danh sách xe hoàn tất cần thu tiền (nút `Thu tiền ↗`) & Cảnh báo can thiệp (nút `Xử lý ↗`).
  + Bảng Live xe tại gara kèm bộ nút lọc nhanh trạng thái (`Tất cả`, `Chờ sửa`, `Đang sửa`, `Chờ linh kiện`, `Hoàn tất`).
  + Quick Actions: `+ Tiếp nhận xe (F2)`, `💵 Thu tiền (F3)`, `🔄 Đổi trả (F4)`.
- **Biên dịch**: `mvn test-compile` BUILD SUCCESS 78 source files, 0 error.

## Việc kế tiếp (gợi ý, chờ người dùng chốt thứ tự)
- Phát triển phân hệ Chủ cửa hàng: Quản lý linh kiện và kho (CCH), Quản lý nhân viên, Thống kê doanh thu.
- Phát triển phân hệ Kỹ thuật viên: Cập nhật phiếu sửa chữa, Yêu cầu linh kiện.
