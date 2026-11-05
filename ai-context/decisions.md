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
- **Số lượng nhập thực tế**: Chủ cửa hàng được xác nhận số nhận khác số lượng yêu cầu; lưu riêng `soLuongNhapThucTe`, giữ `soLuongYeuCau` để đối chiếu và trigger cộng đúng số thực nhận một lần khi sang `DA_NHAP_KHO`.
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
- Màn hình Java Swing dùng hậu tố `UI` (ví dụ `DangNhapUI`, `DashboardUI`, `KhoiDongUI`, `CapNhatPhieuSuaChuaUI`, `TraCuuUI`).
- Khung giao diện chính điều phối: [MainFrame.java](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/ui/MainFrame.java) (Header + Sidebar điều hướng theo vai trò + ContentPanel `CardLayout`).
- Giao diện Sáng (`FlatLightLaf`), bo góc nút 10–12px bằng `RoundedButton`, không dùng mockup dữ liệu.
- **Chống cắt chữ tuyệt đối (Anti-clipping - 02/11/2026)**:
  + Sidebar Navigation rộng `256px`, nút `236×48px`, nhãn HTML `width: 172px` để không cắt cụt các tiêu đề dài ("Yêu cầu cập nhật linh kiện", "Cập nhật phiếu sửa chữa").
  + `RoundedButton` override `getPreferredSize()` và `getMinimumSize()` tính toán động theo `FontMetrics.stringWidth(text) + iconWidth + 34px padding`.
  + Nút thao tác dài ("Cập nhật phiếu sửa chữa" ≥ 220px, "Quay lại danh sách" ≥ 180px, "Xem chi tiết yêu cầu" ≥ 180px).
- **Phân quyền chức năng Tra Cứu 3 vai trò (RBAC - 02/11/2026, Figma 57:246)**:
  + CCH: 5 tab (Dịch vụ, Linh kiện, Khách hàng, Lịch sử sửa chữa, Lịch sử tiếp nhận).
  + TNV: 3 tab (Dịch vụ, Khách hàng, Lịch sử tiếp nhận).
  + KTV: 3 tab (Dịch vụ, Linh kiện, Lịch sử sửa chữa).
- **Phân hệ Kỹ thuật viên (KTV - 02/11/2026)**: Hoàn thiện đủ 5 màn hình bám sát Figma: Dashboard KTV (50:589), Thống kê sửa chữa (172:5247), Phiếu phân công (50:701), Cập nhật phiếu (50:787), Yêu cầu linh kiện KTV (50:922).
- **Quy chuẩn Nhập liệu & Bảng Swing (02/11/2026)**:
  + Cấm nhập bàn phím vào ô ngày, bắt buộc chọn qua lịch; editor JDateChooser lề trái tối thiểu 10px, bề rộng ≥ 170px chống che lấp số ngày do góc bo tròn.
  + Chặn gõ chữ vào các ô số lượng bằng `DocumentFilter` (`chiChoPhepNhapSo`).
  + Bảng kết quả tìm kiếm lịch sử hiển thị đúng 4 dòng (`220px`), bảng chi tiết hạng mục hiển thị đúng 3 dòng (`180px`); đặt đồng thời `setPreferredSize` và `setMinimumSize` để chống bị `BoxLayout` bóp méo thành 1 dòng; `tabs` đặt `maximumSize = MAX_VALUE`.
  + Nút "Xem chi tiết" mở modal dialog theo ngữ cảnh trạng thái (chưa xong: Cập nhật; đã xong: Xem lịch sử).
  + Cơ chế hàng đợi `maPhieuChoSua` chống lỗi race condition khi chuyển trang bất đồng bộ.
  + Combobox chọn phiếu form Cập nhật phiếu sửa chữa loại bỏ hoàn toàn phiếu `HOAN_TAT`.
  + KPI "Phiếu cần hoàn thành" = `Tổng phiếu - Phiếu đã hoàn tất`. Biểu đồ cột 7 ngày gom nhóm theo ngày tiếp nhận từ dữ liệu thực tế DB.

## Quy chuẩn Build Path & Tệp cấu hình IDE (02/11/2026)
- TUYỆT ĐỐI KHÔNG XÓA `.classpath`.
- TUYỆT ĐỐI KHÔNG thêm `<classpathentry kind="lib" path="pom.xml"/>` vào `.classpath` (pom.xml không phải tệp ZIP/JAR, gây lỗi fatal `cannot be read or is not a valid ZIP file`).
- TUYỆT ĐỐI KHÔNG chạy `javac` đơn lẻ thiếu cờ `-d target/classes` làm sinh `.class` rác vào trong `src/` (mọi build qua `mvn compile`).
- Lớp tiện ích UI dùng chung (`UIInputRules`) bắt buộc là `public final class` với `public static` methods.
- Định dạng Locale: Dùng `Locale.forLanguageTag("vi-VN")` thay vì constructor deprecated `new Locale(...)`.

## Kiến trúc Sao lưu & Phục hồi CSDL và Phối hợp Đa máy trạm (Chốt 03/11/2026)
- **Khóa phân tán đa máy trạm (`sp_getapplock`)**:
  + Mô hình gara có nhiều máy trạm (CCH, TNV, KTV) cùng kết nối tới máy chủ SQL Server. Để tránh xung đột sao lưu đồng thời (hai máy trạm cùng lúc ghi đè hoặc cùng chạy job Quartz), hệ thống sử dụng khóa phân tán `sp_getapplock` (`Resource = 'CarZone_Backup_Lock'`, `LockMode = 'Exclusive'`, `LockOwner = 'Session'`, timeout 5 giây).
  + Quy tắc "Đến trước phục vụ trước": Máy nào xin được khóa trước sẽ thực hiện sao lưu. Máy xin khóa sau nếu hết thời gian chờ (timeout) sẽ ghi log và tự động bỏ qua lượt mà không gây tắc nghẽn hay xung đột dữ liệu.
- **Phân quyền tiến trình nền (Multi-client RBAC Scheduling)**:
  + Chỉ máy trạm đăng nhập vai trò **`CHU_CUA_HANG`** mới khởi chạy tiến trình lập lịch tự động Quartz (`QuartzScheduler`) và tác vụ sao lưu cuối ca khi đóng ứng dụng (`AppShutdownManager`).
  + Các máy trạm `TIEP_NHAN_VIEN` và `KY_THUAT_VIEN` khi thoát ứng dụng sẽ đóng ngay mà không kích hoạt sao lưu kết ca, tránh phát sinh các bản sao lưu trùng lặp không cần thiết.
- **Quy chuẩn Chuỗi phục hồi CSDL SQL Server (SQL Server Restore Chain)**:
  + Cơ chế xác định chuỗi phục hồi thông minh: Khi chọn một bản sao lưu vi sai (DIFF) hoặc nhật ký giao dịch (LOG) bất kỳ để phục hồi, hệ thống tự động truy vấn bảng lịch sử sao lưu `msdb.dbo.backupset` để bóc tách cây phụ thuộc:
    `Bản FULL gốc gần nhất -> (Bản DIFF gần nhất nếu có) -> Chuỗi các bản LOG theo thứ tự thời gian tăng dần (backup_set_id ASC) -> Bản đích`.
  + **Quy tắc cú pháp T-SQL bắt buộc chống lỗi `Msg 4306`**:
    * Bản FULL đầu tiên: `RESTORE DATABASE CarZone FROM DISK = ? WITH REPLACE, NORECOVERY`.
    * Các bản trung gian (DIFF hoặc LOG trung gian): `RESTORE DATABASE CarZone FROM DISK = ? WITH NORECOVERY` (hoặc `RESTORE LOG CarZone FROM DISK = ? WITH NORECOVERY`). **TUYỆT ĐỐI KHÔNG CÓ `REPLACE`** vì SQL Server cấm cờ `REPLACE` trên các tệp rollforward kế tiếp.
    * Bản đích cuối cùng: `WITH RECOVERY` (không có `REPLACE`) để đưa CSDL trở lại trạng thái sẵn sàng (`ONLINE`).
  + Sau khi phục hồi thành công, tự động làm mới kết nối `DbUtil.getConnection()` và cập nhật giao diện ngay lập tức.
- **Quy chuẩn Trải nghiệm Người dùng & Giao diện (03/11/2026)**:
  + Tự động mở toàn màn hình (`JFrame.MAXIMIZED_BOTH`) khi đăng nhập thành công.
  + Thống nhất hệ thống biểu tượng ảnh 3D (`img/nav_*.png`, ví dụ `nav_sao_luu_phuc_hoi.png`) cho Sidebar và tiêu đề trang, fallback bằng vector icon của `MainFrame`. Cấm dùng emoji Unicode (`💾`, `⚠️`, `🔄`, ...).
  + Khung Modal Dialog căn chỉnh lề đều đặn: Dùng `RoundedPanel` với insets 16px thay cho `JTextArea`, `JTextField` đệm lề trái 12px để không lệch trục mắt.

## Giám sát Hành vi Phi lý & Hàng rào Bảo mật Mã PIN Cấp 2 (Chốt 04/11/2026)
- **Đánh chặn tập trung tại Tầng Proxy JDBC**: Tích hợp `PhatHienHanhViPhiLy.kiemTraTruocKhiThucThi` ngay trong `AuditStatementProxy`. Mọi câu lệnh SQL ghi dữ liệu (`INSERT`, `UPDATE`, `DELETE`, `DDL`) từ tất cả các DAO đều được giám sát tự động mà không cần can thiệp từng DAO đơn lẻ.
- **3 Nhóm Hành vi Phi lý được bảo vệ**:
  1. *Bulk & Velocity*: Xóa hàng loạt dữ liệu cốt lõi (≥ 3 bản ghi trong < 2 giây); xóa không có `WHERE`; DDL phá hủy (`DROP`, `TRUNCATE`, `ALTER TABLE ... DROP`).
  2. *Temporal*: Thao tác xóa/sửa dữ liệu cốt lõi ngoài giờ hành chính (00:00 - 05:00 sáng).
  3. *Structural & Cascading Danger*: Xóa tài khoản / hồ sơ Chủ cửa hàng `CCH...` gây sụp đổ toàn bộ dữ liệu phụ thuộc; truy cập can thiệp tài nguyên quản trị SQL Server.
- **Cơ chế Vượt rào & Trừng phạt (PIN Barrier Workflow)**:
  + Tạm dừng luồng DB đang chạy, hiển thị `PinVerificationDialog` trên EDT yêu cầu mã PIN cấp 2 (băm SHA-256).
  + Đúng PIN: Mở khóa, thêm bảng vào `whitelist` tạm thời 60 giây (suppression window) để không bị hỏi lại.
  + Sai PIN ≥ 3 lần: Cưỡng chế thoát (Force Logout), hủy giao dịch, kích hoạt sao lưu khẩn cấp (`BackupService.thucHienBackup(BackupType.LOG)`), gửi cảnh báo khẩn cấp tới `ntfy.sh` và Audit Log, đóng toàn bộ Frame về màn hình Đăng nhập.

## Quyền truy cập riêng theo tài khoản (Chốt 05/11/2026)
- Chủ cửa hàng cấu hình quyền cho **một tài khoản nhân viên cụ thể**, không thay đổi cấu hình quyền dùng chung của vai trò.
- Tài khoản có thể dùng tập quyền theo hồ sơ hiện tại, toàn bộ màn hình TNV/KTV, hoặc tập màn hình tùy chỉnh.
- Cấp màn hình của vai trò khác là **chỉ xem**; controller và DB vẫn áp dụng vai trò hồ sơ cho thao tác ghi nghiệp vụ. Mã nhân viên, tên đăng nhập và lịch sử giao dịch không bị đổi.
- Đây là quyền truy cập ứng dụng, không phải quy trình chuyển đổi hồ sơ nhân sự giữa KTV và TNV. Ràng buộc DB gắn vai trò với mã nhân viên và lưu lịch sử bằng mã đó.
