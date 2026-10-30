# Quyết định đã chốt

> Chỉ ghi quyết định đã được người dùng/giảng viên xác nhận. Agent không lật lại các mục này.

## Từ giảng viên (Q&A, 20–21/08/2026, 18/09/2026)
- Mỗi phiếu sửa chữa chỉ có 1 kỹ thuật viên chính.
- Thêm trạng thái `CHO_LINH_KIEN` cho phiếu sửa chữa.
- Trừ tồn kho tự động và chặn dùng vượt tồn.
- Chỉ kỹ thuật viên chính được cập nhật dịch vụ/linh kiện/trạng thái của phiếu.
- Chỉ lập hóa đơn khi phiếu `HOAN_TAT`; tổng tiền do hệ thống tính.
- Tiếp nhận viên xem được thống kê phiếu của mình; kỹ thuật viên xem được linh kiện đã dùng của mình.
- Xe bắt buộc gắn khách hàng, định danh bằng biển số, tạo/chọn trước khi lập phiếu.
- Mã hóa đơn tự sinh theo ngày + ca (A/P) + số thứ tự.

## Từ nhóm & Chốt kiến trúc dự án (Cập nhật 28/10/2026)
- Mã hóa đơn có tiền tố `HD`: `HDddMMyyyyAXXX`.
- Loại thuế xác định theo nội dung hóa đơn (chỉ linh kiện 1,5% / chỉ dịch vụ 7% / cả hai 4,5%), không thêm thuộc tính loại vào `HoaDon`.
- Khuyến mãi: không liệt kê mặt hàng = giảm tất cả; có liệt kê = chỉ các mặt hàng trong danh sách được giảm.
- Mã dịch vụ/linh kiện dùng `DV00001`/`LK00001` (≥ 5 số).
- **Luật nghiệp vụ kết hợp hai tầng:** vừa trigger/stored procedure ở DB, vừa validate ở Java.
- **BỎ HIBERNATE / JPA HOÀN TOÀN (28/10/2026)**:
  + Chuyển sang dùng **JDBC thuần (`DriverManager`)** qua `connectDB/DbUtil` và `connectDB/DatabaseConfig`.
  + Gỡ bỏ hoàn toàn `hibernate-core`, `flyway-core`, `flyway-sqlserver` khỏi `pom.xml`, xóa `persistence.xml`.
  + Xóa toàn bộ annotation JPA trên các entity (`@Entity`, `@Table`, `@Id`, ...), entity là Java POJO thuần túy.
- **Cấu trúc thư mục tầng DAO (chốt lại ngày 30/10/2026, thay cho cấu trúc 28/10)**:
  + Mỗi bảng/nhóm nghiệp vụ là **MỘT class duy nhất** đặt tại package `dao` (`src/dao/`), tên file viết hoa ký tự đầu, không tiền tố `I`, không hậu tố `Impl` (ví dụ: `TaiKhoanDao`, `NhanVienDao`, `HoaDonDao`, `ChiTietHoaDonDao`, `PasswordResetDao`, `ThongKeDoanhThuDao`).
  + Đã **bỏ hoàn toàn interface và subpackage `dao.impl`**: class trong `dao` chính là lớp thực thi JDBC thuần, quản lý `Connection` theo từng hàm bằng `try-with-resources`, giữ nguyên javadoc nghiệp vụ (chữ ký + chú thích cột trả về).
- **Chuyển đổi Service sang Controller & Quản lý Session (28/10/2026)**:
  + Thư mục `service` đã được xóa và thay thế bằng `controller` (`AuthController`, `DangNhapController`, `DashboardController`). *Hoàn tất ngày 30/10/2026: xóa nốt file còn sót `src/service/ThongKeDoanhThuService.java` → `src/controller/ThongKeDoanhThuController.java` (đổi tên class theo quy ước `*Controller`), `ThongKeDoanhThuUI` gọi qua `controller`. Trong `src/` không còn package `service`.*
  + Ảnh đặt tại thư mục gốc `img/` (không còn `src/resources/images/`); `pom.xml` trỏ `<resources>` vào `img/` nên `getResource("/carzone_logo.png")`, `getResourceAsStream("/revenue_illustration.png")` vẫn đọc được từ classpath (kèm fallback đọc file `img/...` khi chạy từ thư mục gốc repo).
  + Tạo `session.UserSession` (Singleton) lưu giữ thông tin nhân viên đăng nhập, vai trò, thời điểm phiên hiện tại.
  + Entry point chính của ứng dụng đặt tại [src/app/App.java](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/app/App.java) (`main.class = app.App`).
- **`.env` vẫn được đẩy lên git** để bạn bè pull về dùng chung; dữ liệu lưu local nên không ảnh hưởng kết quả của nhóm. Không coi đây là lỗi lộ bí mật, nhưng không đặt thông tin thật nào khác vào `.env`.

## Phân vai hai tầng (đã được nhóm xác nhận)
Mục đích là tránh hai bên làm trùng hoặc mâu thuẫn:
- **DB là nguồn cuối cùng** cho: sinh mã theo ngày/ca/vai trò (stored procedure, an toàn khi nhiều người cùng lập), trừ/cộng tồn kho (trigger đã có), khóa phiếu `HOAN_TAT` và hóa đơn đã thanh toán, các CHECK/UNIQUE/FK.
- **Java** validate sớm để báo lỗi thân thiện (setter entity, controller), kiểm quyền theo vai trò, dịch mã lỗi DB (50xxx) thành thông báo tiếng Việt.
- **Java không được tự trừ/cộng tồn kho** (trigger đã làm, làm thêm sẽ trừ hai lần) và không tự sinh mã đã giao cho stored procedure.
- Thêm/đổi luật: sửa `business-rules.md` trước, rồi cập nhật cả hai tầng.

## Quy chuẩn Entity & JDBC
- Giữ `java.util.Date` và `double` theo quy ước của nhóm.
- Số tiền tổng của `HoaDon` và `soTienHoanTra` của `PhieuDoiTra` là thuộc tính suy diễn, chỉ getter; DAO đọc từ view `vw_HoaDon_TongTien` và gán qua raw setter (`setTongThanhToanRaw`). Thành tiền từng dòng (`ChiTietDichVu/LinhKien/DoiTra`) tính bằng Java.
- Entity bổ sung raw setters (`setXxxRaw(...)`) để DAO gán dữ liệu từ `ResultSet` mà không bị chạy lại validation.
- FlatLaf **3.7**, Java **17**.
- Mật khẩu: băm SHA-256, hex in hoa, trong `TaiKhoan.setMatKhau`.

## Quy chuẩn Giao diện UI (Figma)
- Màn hình Java Swing dùng hậu tố `UI` (ví dụ `DangNhapUI`, `DashboardUI`, `KhoiDongUI`).
- Khung giao diện chính điều phối: [MainFrame.java](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/ui/MainFrame.java) (Header + Sidebar điều hướng theo vai trò + ContentPanel `CardLayout`).
- Giao diện Sáng (`FlatLightLaf`), bo góc nút 12px bằng `RoundedButton`, không dùng mockup dữ liệu.
