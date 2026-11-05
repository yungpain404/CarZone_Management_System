# Tiến độ CarZone

> Agent đọc file này đầu mỗi phiên và cập nhật khi xong việc. Giữ ngắn, dưới 60 dòng. Trạng thái dưới đây lấy từ hai bản zip ngày 25/10/2026.

## Đã xong
- **Cấu hình Gmail quên mật khẩu local:** `GmailSmtpSender` đọc biến môi trường trước, nếu trống sẽ đọc `infra/docker/gmail.local.properties`; tệp mẫu được Git bỏ qua để người dùng tự điền App Password, không lưu bí mật trong mã nguồn.
- **Sửa luồng và chẩn đoán gửi OTP (05/11/2026):** Link Quên mật khẩu hỏi tên tài khoản trước khi mở trang OTP; lỗi gửi được phân biệt thành xác thực Gmail, kết nối SMTP và Gmail từ chối nhận thư. Khi gửi lỗi, cho phép thử lại ngay thay vì khóa nhầm 60 giây.
- **Sửa lỗi không bấm được Đặt lại mật khẩu:** Sau khi gửi OTP, trạng thái busy trước đó đã để nút đặt lại ở disabled; bật lại nút theo trạng thái OTP, giới hạn hàng nút theo bề rộng form và co dialog theo vùng màn hình khi chuyển bước.
- **Tách xác minh OTP và đặt mật khẩu thành hai trang:** OTP được xác minh thật ở controller (đếm tối đa 5 lần sai) trước khi chuyển sang trang đặt mật khẩu; hai trang dùng card/field/button row cùng bề rộng và hộp thoại tự canh giữa, co theo màn hình.
- **Tinh chỉnh luồng khôi phục:** bỏ nút quay lại OTP sau khi xác minh thành công, căn giữa nút đặt lại mật khẩu; hộp thoại đầu tiên yêu cầu mã nhân viên.
- **Căn chỉnh thông báo khôi phục:** tự ngắt dòng theo bề rộng và căn giữa từng dòng trong vùng trạng thái cố định; bỏ câu hướng dẫn trùng ở trang tạo mật khẩu.
- **Gmail nhân viên bắt buộc:** Cột `NhanVien.email` trong V1 là `NOT NULL`, có CHECK không rỗng/đúng dạng email cơ bản; `sp_ThemNhanVien` nhận và kiểm tra email, DAO truyền email cùng giao dịch tạo nhân viên, entity từ chối giá trị trống. Dữ liệu demo dùng 5 Gmail mẫu cố định, không đảm bảo có hộp thư thật.
- **Khắc phục DB sạch không hiện Gmail (05/11/2026):** Flyway dừng trước V1 vì SQL Server `sp_getapplock` ném lỗi 17750. Khởi động lại SQL Server rồi chạy lại db-init/Flyway đã migrate thành công V1–V5; xác nhận 7 nhân viên có Gmail, tất cả tài khoản hoạt động. Giữ healthcheck Compose ở mức kết nối cơ bản; DAO khôi phục tra đúng `maNV`; UI báo riêng lỗi tra cứu CSDL thay vì kẹt “Đang xác định Gmail”.
- **Hiển thị số lần nhập OTP sai:** Thông báo cho biết số lần sai trên tổng 5 lần, số lượt còn lại, cảnh báo ở lượt cuối và số lần khi mã bị khóa.
- **Cưỡng chế thoát khi nhập sai OTP đủ ngưỡng:** Ở lần sai thứ 5, ghi audit và gửi cảnh báo ntfy có nhãn OTP/số lần sai; sau khi gửi xong, hủy phiên và thoát tập trung qua `AppShutdownManager`.
- **Bổ sung tài khoản mẫu (05/11/2026):** Giữ duy nhất một Chủ cửa hàng và thay hồ sơ seed CCH001 thành Nguyễn Ngọc Hậu theo xác nhận; TNV003 Hồ Nhật Tân và KTV003 Nguyễn Vĩnh Lợi đã được tạo trong DB hiện tại qua stored procedure và thêm vào seed V1. Email theo người dùng, dữ liệu hồ sơ thiếu dùng mock; mật khẩu mẫu `Carzone@123`.
- **Sửa Flyway checksum V1:** Docker volume cũ giữ checksum V1 cũ và lịch sử V6; đã chạy `flyway repair` rồi áp dụng tạm V7 không mất dữ liệu. Theo yêu cầu, schema cuối cùng được giữ trong V1 và file V7 đã xóa khỏi repo; DB local vẫn ở V7 cho tới khi người dùng tự xóa volume để tạo mới từ V1–V5.
- Nhánh `develop`: 14 entity (`src/entity`) và 6 enum (`src/enums`), biên dịch được bằng `javac`. `pom.xml`, tài liệu trong `ai-context/`.
- Nhánh `feature/infrastructure`: `infra/docker/compose.yml` (SQL Server → tạo DB → Flyway), `V1__init_CarZone_Database.sql` (bảng, trigger, stored procedure, view, dữ liệu danh mục/nhân sự), `V2__update_KhachHang_Entity.sql`, seed `CarZone_Demo.sql` (kịch bản khách Nguyễn Văn A).

## Mới làm (05/11/2026)
- **Tái thiết kế & tích hợp toàn diện Dashboard Tiếp nhận viên mới vào `MainFrame.java`**:
  + Thay thế phiên bản cũ bằng `TiepNhanVienDashboardPanel` kế thừa `ScrollableContentPanel`, nạp dữ liệu bất đồng bộ qua `SwingWorker` chống đóng băng UI.
  + Header ca làm việc: Lời nhắc ca trực hôm nay, nút "Làm mới" dữ liệu và bộ 3 phím tắt nhanh (+ Tiếp nhận xe F2, Thu tiền F3, Đổi trả F4, Refresh F5).
  + 4 Thẻ Stat Cards thông minh: vẽ icon vector 2D FlatLaf (`CarVectorIcon`, `CheckVectorIcon`, `WrenchVectorIcon`, `MoneyVectorIcon`, `AlertTriangleIcon`) thay thế hoàn toàn Unicode emoji tránh lỗi font; thẻ "Cần phân công KTV" tự đổi màu đỏ cảnh báo khi có xe chưa gán thợ; thẻ "Chờ thanh toán" tự đổi xanh lá khi có xe xong chờ thu tiền.
  + 2 Biểu đồ trực quan số liệu: Biểu đồ thanh tiến trình 4 màu trạng thái xe tại xưởng (`ThanhTienDoXePanel`) kèm 4 ô mini box số liệu/tỷ lệ %; Biểu đồ thanh ngang tải công việc KTV kèm huy hiệu "✓ Sẵn sàng" hoặc số lượng xe đang phụ trách.
  + Action Center: Khối "Xe đã sửa xong – Chờ thu tiền & giao xe" (60%) kèm nút "Thu tiền ↗" sang thẳng Lập hóa đơn; Khối "Cảnh báo & Việc cần can thiệp" (40%) cảnh báo xe chưa gán KTV và xe chờ linh kiện kèm nút "Xử lý ↗".
  + Bảng Live xe tại Gara hôm nay: Bảng dữ liệu kèm huy hiệu trạng thái `StatusBadgeRenderer` bo góc mềm và bộ 5 nút lọc nhanh (Tất cả, Chờ sửa, Đang sửa, Chờ linh kiện, Hoàn tất).
- **Giải quyết triệt để xung đột merge từ `develop`**:
  + Giữ nguyên và chuẩn hóa theo kiến trúc mới: class DAO đơn trong `src/dao/*DAO.java`, bỏ interface và `dao.impl`.
  + Tích hợp đầy đủ tính năng Tiếp nhận viên: Quản lý phiếu đổi trả UC012, Thống kê đổi trả Phần 4, chỉnh sửa phiếu chờ sửa, và Dashboard TNV mới.
  + Biên dịch Maven (`mvn compile`) đạt BUILD SUCCESS (108 file, 0 lỗi).

## Mới làm (04/11/2026)
- **Dashboard Chủ cửa hàng:** truy vấn 4 KPI trực tiếp từ SQL, hiển thị trạng thái đang tải/lỗi và thời điểm cập nhật, thêm nút làm mới; giảm danh sách yêu cầu còn 5 dòng và cảnh báo hết hàng còn 3 mục ưu tiên để dashboard thoáng hơn.
- **Tinh chỉnh Dashboard Chủ cửa hàng:** đưa cả 4 KPI về thẻ trắng, bỏ các dòng mô tả nhỏ; bấm KPI mở danh sách yêu cầu chờ duyệt/đã duyệt, linh kiện hết hàng hoặc hồ sơ chưa có tài khoản với bộ lọc được chạy sẵn.
- **Ràng buộc số và chọn ngày CCH:** spinner giá dịch vụ/linh kiện, tồn ban đầu, phần trăm khuyến mãi và số nhập kho chặn ký tự chữ/khoảng trắng khi gõ; ngày sinh/ngày vào làm và ngày bắt đầu/kết thúc khuyến mãi dùng JCalendar popup, không nhập tay.
- **Sửa ba lỗi UI CCH:** tìm kiếm header Enter hiện gợi ý các màn theo quyền và cho chọn điều hướng; spinner tiền/phần trăm/tồn/số lượng dùng `NumberFormatter` nghiêm ngặt; form thêm nhân viên truy vấn mã kế tiếp để hiển thị, form tạo tài khoản luôn hiện mã hồ sơ nhân viên đã chọn.
- **Nút Làm mới danh mục dịch vụ/linh kiện:** hạ cờ trạng thái tìm kiếm trước khi reset combobox; tránh listener tự chạy truy vấn toàn bộ danh mục, kết quả trở lại 0 phần tử.
- **Làm gọn bảng yêu cầu trên Dashboard CCH:** bỏ dấu điều hướng `>` ở cuối từng dòng và bỏ dải đếm bốn trạng thái bên dưới bảng.
- **Tìm kiếm header dịch vụ/linh kiện:** nối thanh tìm kiếm MainFrame tới TraCuuController, hiển thị gợi ý dữ liệu thật theo mã/tên và điều hướng tới danh mục đã lọc theo vai trò; truy vấn chạy nền.

## Mới làm (30/10/2026)
- **Quy tắc UI:** cập nhật `AGENTS.md` và skill `swing-screen`: không thêm breadcrumb/path phân cấp trên bất kỳ trang nào; dùng Side Navigation và tiêu đề hiện tại để định vị.
- **Căn chỉnh icon header theo ảnh giao diện gửi kèm:** icon tìm kiếm chuyển sang `Icon` Swing để JLabel căn giữa theo chiều dọc; icon Help vẽ lại dấu hỏi (nét cong + chấm) trong vòng tròn, nút căn giữa cụm icon/chữ rõ ràng. Figma MCP chưa khả dụng trong phiên nên đối chiếu ảnh người dùng cung cấp; cần inspect node `83:7716` trực tiếp khi MCP hoạt động.
- **Code lại Dashboard Chủ cửa hàng theo Figma node 83:7716** (ảnh hưởng mọi role vì dùng chung `MainFrame`):
  + `src/ui/MainFrame.java`: bảng màu mới `F_*` (#F4F7FB nền, #E3E9F2 viền, #14233D tiêu đề, #7A889C mô tả, #EDF4FF nhấn, #CA4850 đỏ, #183C78 doanh thu, #BDD5FE/#39B9D0 biểu đồ); `buildHeader()` 76px (brand CARZONE + search 300×40 bo 10px nền #F4F7FB không viền + Help/About bo 12px + avatar tròn + Đăng xuất có vạch phân cách); `buildSidebar()` 260px item 56px gap 6 radius 10, active nền #EDF4FF + vạch trái 3px #2563EB, icon màu PNG 30px từ `img/nav_*.png` (fallback emoji theo vai trò TNV/KTV); lớp mới `ChuCuaHangDashboardPanel` (title 28px + chip ngày, 4 thẻ KPI 177px card1 nền xanh + minh họa 3D 46px, bảng YC chờ duyệt header #F4F7FB cột 108/138/flex/72/84 + footer 4 đếm trạng thái có dot, card "Cảnh báo hết hàng" 352px, card "Kinh doanh trong ngày" box #EDF4FF 310px + biểu đồ 4 cột 338px + cơ cấu thanh toán thanh 9px), nạp dữ liệu qua `SwingWorker`; icon vector Java mới (`ArrowUpRightIcon`, `ChevronRightIcon`, `HelpIcon`, `InfoIcon`, `LogoutIcon`, `CalendarIcon`, `DotIcon`) + `RoundedPanel`, `CotBieuDo`, `ThanhCoCauPanel`.
  + `src/controller/DashboardController.java`: đếm YC nhập theo 4 trạng thái (GROUP BY, không giới hạn TOP), danh sách YC `CHO_DUYET` TOP 10 JOIN `LinhKien`/`NhanVien` (mã + tên + tồn thật), linh kiện hết hàng kèm YC chờ duyệt gần nhất, số liệu "Kinh doanh trong ngày" scope theo `GETDATE()` qua `vw_DoanhThuTheoNgay` + biểu đồ 4 khung giờ (`phanKhungGio`) + cơ cấu theo `phuongThucTT`.
  + `img/`: tải 14 asset PNG từ Figma (4 minh họa 3D `illustration_*.png` + 10 icon sidebar `nav_*.png`). Figma MCP hết quota Starter → icon nhỏ vẽ vector Java, icon vai trò khác fallback emoji.
  + `mvn -q compile` thành công. Chưa chạy thử trên DB/kiểm tra trực quan.
- **Dashboard + Thống kê doanh thu CCH theo ảnh người dùng**: thu khung về 1170×800, bỏ breadcrumb khỏi các trang, nối `ThongKeDoanhThuUI` vào Side Navigation; bỏ bộ nút loại trùng, giữ combobox 3 loại + khoảng ngày. Doanh thu có biểu đồ cột, donut, nhận xét, tổng hợp và bảng giờ; xe/phiếu có trạng thái, số xe/phiếu; dịch vụ/linh kiện xếp theo giá trị sử dụng. Dashboard dùng controller; thống kê đọc qua DAO. Figma MCP chưa khả dụng, đối chiếu ảnh được gửi.
- **Quản lý danh mục CCH**: dịch vụ và linh kiện chỉ hiện kết quả sau khi tìm kiếm; form Thêm/Sửa dùng chung thiết kế và nạp giá trị DB khi Sửa; có đổi trạng thái, xóa an toàn. Thêm DAO/controller phân quyền; Flyway V3 cấp mã DV, V4 cấp mã LK và nhận tồn ban đầu. Form giới hạn tên theo UI; tồn kho chỉ đọc khi Sửa. `mvn -q compile` đạt; Flyway áp dụng tới version 4.
- **Xử lý yêu cầu cập nhật linh kiện CCH**: màn hình tìm theo mã yêu cầu/mã hoặc tên linh kiện, lọc trạng thái; bảng và chi tiết chỉ hiện sau khi tìm. Nối vào điều hướng; hỗ trợ duyệt, từ chối có lý do, xác nhận nhập kho với cập nhật trạng thái có điều kiện; trigger DB tự cộng tồn. Tìm kiếm mặc định rỗng; nút Tìm kiếm/Làm mới nằm ngang hàng input. `mvn -q compile` đạt.
- **Quản lý khuyến mãi CCH**: danh sách chỉ hiện sau tìm mã/tên và lọc khoảng ngày; form Thêm/Sửa nạp giá trị hiện tại, còn danh mục dịch vụ/linh kiện chỉ truy vấn theo mã và thêm từng mục. Dùng sequence hiện có để cấp mã, lưu chương trình + bảng nối trong giao dịch; nếu đã gắn hóa đơn thì chỉ đổi tên/ghi chú theo trigger V1. Nối menu; `mvn -q compile` đạt.
- **Xác nhận nhập kho từ Quản lý linh kiện**: thêm dialog theo thiết kế, chọn yêu cầu đã duyệt của linh kiện đang chọn; nhập số thực nhận, ghi chú và xem tồn trước/sau. Cho phép số nhận khác yêu cầu theo xác nhận của người dùng; V5 lưu số thực nhận và trigger cộng đúng lượng này. Nối chung dialog vào màn xử lý yêu cầu; dữ liệu cũ được backfill bằng số yêu cầu. `mvn -q compile` đạt; Flyway áp dụng V5 thành công.

## Mới làm (31/10/2026)
- **Database và nhân viên CCH**: nạp seed/Flyway V5, chuyển cổng CarZone sang 1434 vì SQL Server Windows chiếm 1433, xác nhận AuthController đăng nhập được; thêm màn quản lý nhân viên tìm kiếm có lọc vai trò và form Thêm/Sửa, mã được sinh bằng `sp_ThemNhanVien`.
- **Quản lý tài khoản CCH**: danh sách mặc định rỗng, tìm theo mã/tên/điện thoại/username; bỏ tab tài khoản/quyền theo vai trò và thao tác khóa, thêm cột đã có tài khoản, form tạo/sửa mật khẩu và xóa account nhưng giữ hồ sơ. Username tạo qua `sp_TaoTaiKhoan`; DAO/controller kiểm quyền CCH. `mvn -q compile` đạt.
- **Tra cứu Chủ cửa hàng**: thay trang tra cứu chung bằng ba tab chỉ đọc Linh kiện, Khách hàng và Lịch sử sửa chữa; bỏ Dịch vụ. Không tải danh sách ban đầu, chỉ tìm khi có từ khóa; chi tiết khách tải xe theo lựa chọn, chi tiết phiếu lấy dịch vụ/linh kiện và tổng tiền từ view lịch sử. Nối `NAV_TRA_CUU` riêng cho CCH; `mvn -q compile` đạt.
- **Thông tin cá nhân**: cập nhật trang trong `MainFrame` theo ảnh; tab Hồ sơ cá nhân đứng trước, hiển thị họ tên, mã nhân viên, username từ session, điện thoại, CCCD, ngày sinh, ngày vào làm, tuổi và địa chỉ từ hồ sơ hiện tại. Tab Thông tin vai trò đứng sau với bảng chức năng/phạm vi theo navigation role, không dùng breadcrumb; `mvn -q compile` đạt.

## Mới làm (03/11/2026)
- **Cửa sổ sau đăng nhập và splash:** `MainFrame` mở maximized toàn màn hình sau đăng nhập; splash `KhoiDongUI` thu gọn 600×380px, bố cục thương hiệu/progress/footer cân lại.
- **Tự động sao lưu CSDL SQL Server (Quartz) & Audit Log (SLF4J/Logback)**:
  + Cấu hình Quartz lập lịch 3 Job (FULL 00:00/12:00, DIFF 04:00/08:00/16:00/20:00, LOG mỗi 15 phút), dùng chung `ReentrantLock(fair=true)` phân quyền ưu tiên FULL > DIFF > LOG (LOG skip ghi WARN nếu lock bận).
  + **Hỗ trợ Đa máy trạm (Multi-Client) chống đè dữ liệu:** 
    * Triển khai Khóa phân tán trên SQL Server qua `sp_getapplock` (`CarZone_Backup_Distributed_Lock`) kết hợp khóa cục bộ.
    * Cơ chế "Ai backup đầu tiên thì lấy kết quả người đó": Kiểm tra lịch sử `msdb.dbo.backupset` trước và sau khi nhận khóa; nếu máy khác đã hoàn thành mốc này (FULL < 60p, DIFF < 30p, LOG < 10p, Thoát < 5p), máy sau tự động bỏ qua để tránh ghi đè file trên server.
    * Ghi vết định danh máy trạm và nhân viên thực hiện (`UserSession` + Hostname) vào metadata bản sao lưu trên SQL Server.
    * **Phân quyền vai trò sao lưu (RBAC):** Chỉ chạy hệ thống sao lưu tự động (Quartz Scheduler) và quy trình sao lưu ca làm việc khi thoát trên máy của **Chủ cửa hàng (`VaiTro.CHU_CUA_HANG`)**; các role Tiếp nhận viên và Kỹ thuật viên không chạy lập lịch và thoát ứng dụng bình thường.
    * **Tính năng Sao lưu & Phục hồi thủ công (`SaoLuuPhucHoiUI.java`):** Dành riêng cho Chủ cửa hàng (`NAV_SAO_LUU_PHUC_HOI`):
      - Thẻ sao lưu thủ công (FULL/DIFF/LOG) kèm ghi chú tùy chọn, chạy qua `SwingWorker` kèm thông báo chi tiết (dung lượng, file, ms).
      - Thẻ giám sát trạng thái lập lịch Quartz và thời điểm sao lưu gần nhất.

## Mới làm (05/11/2026)
- **Tái thiết kế & tích hợp toàn diện Dashboard Tiếp nhận viên mới vào `MainFrame.java`**:
  + Thay thế phiên bản cũ bằng `TiepNhanVienDashboardPanel` kế thừa `ScrollableContentPanel`, nạp dữ liệu bất đồng bộ qua `SwingWorker` chống đóng băng UI.
  + Header ca làm việc: Lời nhắc ca trực hôm nay, nút "Làm mới" dữ liệu và bộ 3 phím tắt nhanh (+ Tiếp nhận xe F2, Thu tiền F3, Đổi trả F4, Refresh F5).
  + 4 Thẻ Stat Cards thông minh: vẽ icon vector 2D FlatLaf (`CarVectorIcon`, `CheckVectorIcon`, `WrenchVectorIcon`, `MoneyVectorIcon`, `AlertTriangleIcon`) thay thế hoàn toàn Unicode emoji tránh lỗi font; thẻ "Cần phân công KTV" tự đổi màu đỏ cảnh báo khi có xe chưa gán thợ; thẻ "Chờ thanh toán" tự đổi xanh lá khi có xe xong chờ thu tiền.
  + 2 Biểu đồ trực quan số liệu: Biểu đồ thanh tiến trình 4 màu trạng thái xe tại xưởng (`ThanhTienDoXePanel`) kèm 4 ô mini box số liệu/tỷ lệ %; Biểu đồ thanh ngang tải công việc KTV kèm huy hiệu "✓ Sẵn sàng" hoặc số lượng xe đang phụ trách.
  + Action Center: Khối "Xe đã sửa xong – Chờ thu tiền & giao xe" (60%) kèm nút "Thu tiền ↗" sang thẳng Lập hóa đơn; Khối "Cảnh báo & Việc cần can thiệp" (40%) cảnh báo xe chưa gán KTV và xe chờ linh kiện kèm nút "Xử lý ↗".
  + Bảng Live xe tại Gara hôm nay: Bảng dữ liệu kèm huy hiệu trạng thái `StatusBadgeRenderer` bo góc mềm và bộ 5 nút lọc nhanh (Tất cả, Chờ sửa, Đang sửa, Chờ linh kiện, Hoàn tất).
- **Giải quyết triệt để xung đột merge từ `develop`**:
  + Giữ nguyên và chuẩn hóa theo kiến trúc mới: class DAO đơn trong `src/dao/*DAO.java`, bỏ interface và `dao.impl`.
  + Tích hợp đầy đủ tính năng Tiếp nhận viên: Quản lý phiếu đổi trả UC012, Thống kê đổi trả Phần 4, chỉnh sửa phiếu chờ sửa, và Dashboard TNV mới.
  + Biên dịch Maven (`mvn compile`) đạt BUILD SUCCESS (108 file, 0 lỗi).

## Mới làm (04/11/2026)
- **Khôi phục mật khẩu qua Gmail (Figma “Carzone · Quên mật khẩu”)**:
  + Dùng tên đăng nhập được nhập tại hộp thoại trước khi mở trang khôi phục; tự gửi mã vào Gmail khôi phục và chỉ hiện email đã che cùng ô OTP trên trang này.
  + Sau OTP hợp lệ, hiện form nhập mật khẩu mới; gửi lại mã giới hạn 60 giây.
  + Tăng kích thước hộp thoại khôi phục và mở rộng nút phụ, bọc dòng trạng thái để tránh cắt chữ/tràn giao diện.
  + Thêm thuộc tính `NhanVien.email` và form quản lý hồ sơ; email bắt buộc qua `NOT NULL`/CHECK ở DB, kiểm tra trong `sp_ThemNhanVien`, entity, controller và UI; DAO truyền email ngay khi tạo nhân viên. Nhân viên demo nhận Gmail mẫu cố định. Không tạo bảng OTP mới. OTP băm và giới hạn gửi lại 60 giây/5 lần sai được giữ trong bộ nhớ tiến trình.
  + Gửi email qua Gmail SMTP STARTTLS; đọc `CARZONE_GMAIL_USERNAME` và `CARZONE_GMAIL_APP_PASSWORD` từ biến môi trường hoặc `infra/docker/gmail.local.properties` (Git ignore), không đưa mật khẩu ứng dụng vào `.env` đã commit.
  + Gmail SMTP dùng cấu hình local; Gmail mẫu cố định trong V1 không đảm bảo có hộp thư thật, cần thay bằng Gmail thật để nhận OTP. DB local đã chạy V7 tạm thời, giữ lại Gmail hợp lệ đang có; người dùng sẽ tự xóa volume để dựng DB mới từ V1–V5.
- **Hệ thống Giám sát Hành vi Phi lý & Hàng rào Xác thực Mã PIN Cấp 2 (Behavioral Anomaly & PIN Barrier)**:
  + Sắp xếp chuẩn vào cấu trúc package hiện có: `enums/LoaiHanhViBatThuong.java`, `ui/PinVerificationDialog.java`, `audit/` (`ThongTinViPham`, `SecurityConfig`, `PhatHienHanhViPhiLy`, `PinVerificationManager`, `AlertNotifier`). Không tạo thêm package gốc `security` bừa bãi.
  + **Đánh chặn tập trung tại tầng Statement Proxy:** `AuditStatementProxy` chặn ngang mọi câu lệnh SQL ghi dữ liệu trước khi gửi xuống database.
  + **3 Nhóm hành vi phi lý:**
    1. *Bulk & Velocity:* Xóa hàng loạt dữ liệu cốt lõi (≥ 3 bản ghi trong < 2 giây); xóa không có `WHERE`; lệnh DDL phá hủy (`DROP`, `TRUNCATE`, `ALTER TABLE ... DROP`).
    2. *Temporal:* Thao tác xóa/sửa dữ liệu cốt lõi ngoài giờ mở cửa (00:00 - 05:00 sáng).
    3. *Cascading Danger & High Sensitivity:* Thao tác xóa Chủ thể cốt lõi (`CCH...` / Chủ cửa hàng); can thiệp cấu hình hệ thống / quản trị SQL Server.
  + **Quy trình vượt rào & Trừng phạt:**
    * Tạm dừng luồng DB, hiển thị `PinVerificationDialog` trên EDT yêu cầu mã PIN cấp 2 (SHA-256).
    * Nhập đúng PIN: Cấp quyền ngoại lệ tạm thời trong 60 giây (suppression window).
    * Nhập sai ≥ 3 lần: Cưỡng chế thoát (Force Logout), hủy toàn bộ giao dịch, kích hoạt sao lưu khẩn cấp (Emergency Log Backup), gửi cảnh báo khẩn cấp tới `ntfy.sh` và Audit Log, đóng toàn bộ Frame về màn hình Đăng nhập.
  + Cấu hình an ninh bổ sung trong `backup.properties`. Đã kiểm thử logic và biên dịch đạt `mvn -q compile` (0 lỗi).
      - Bảng lịch sử các bản sao lưu từ `msdb.dbo.backupset` (badge loại, thời gian, kích thước, người/máy tạo, đường dẫn).
      - Dialog Phục hồi CSDL (`RESTORE DATABASE`) an toàn cao: cảnh báo ghi đè dữ liệu, yêu cầu nhập mã `PHUCHOI` xác nhận, ngắt kết nối các client qua `SINGLE_USER WITH ROLLBACK IMMEDIATE`, restore qua kết nối `master` và đưa về `MULTI_USER`.
      - **Cơ chế phục hồi chuỗi liên hoàn tự động (SQL Server Restore Chain):**
        * Giải quyết triệt để lỗi *"The log or differential backup cannot be restored because no files are ready to rollforward"*.
        * Khi phục hồi từ tệp Transaction Log (`.trn`) hoặc Vi sai (`.bak`), hệ thống tự động truy vấn `msdb.dbo.backupset` theo `backup_set_id`:
          - FULL: `RESTORE DATABASE WITH REPLACE, RECOVERY`
          - DIFF: FULL gốc `WITH REPLACE, NORECOVERY` -> DIFF `WITH RECOVERY` (không có `REPLACE`)
          - LOG: FULL gốc `WITH REPLACE, NORECOVERY` -> các bản LOG tuần tự `WITH NORECOVERY` -> bản LOG đích `WITH RECOVERY` (chuỗi liên hoàn không bị đứt đoạn).
      - **Giao diện Sao lưu & Phục hồi chuẩn FlatLaf & Figma (`SaoLuuPhucHoiUI`):**
        * Tích hợp tài nguyên ảnh 3D `img/nav_sao_luu_phuc_hoi.png` cho Sidebar và Header tiêu đề trang `SaoLuuPhucHoiUI`.
        * Tự động phóng to toàn màn hình (`JFrame.MAXIMIZED_BOTH`) khi đăng nhập thành công vào `MainFrame`.
        * Căn chỉnh lề chữ, nhịp điệu khoảng cách và padding đồng đều 100% trong Dialog Xác nhận phục hồi (khung cảnh báo đỏ và khung chuỗi khôi phục thông minh dùng `RoundedPanel` với padding trong chuẩn 16px, các ô textfield có padding 12px chống dính mép).
        * Loại bỏ hoàn toàn emoji Unicode khỏi các nhãn và nút bấm.
      - **Khắc phục triệt để lỗi icon ô vuông (`□`) và mất chữ:** 
        + Xóa bỏ toàn bộ ký tự emoji Unicode khỏi tiêu đề, nhãn, nút bấm và dialog (tránh lỗi font hệ điều hành không hiển thị được glyph).
        + Thêm vector icon 2D chuẩn FlatLaf `DatabaseIcon` (vẽ 3 đĩa trụ CSDL mượt mà bằng `Graphics2D`) riêng biệt cho mục Sao lưu & Phục hồi trong Side Navigation của `MainFrame`.
        + Tái cấu trúc Card Lập lịch Quartz: thay thế lưới 2 cột bị bóp méo thành 3 hàng full-width (mỗi hàng gồm badge phân loại + mô tả lịch định kỳ + dòng thời gian chạy gần nhất), triệt tiêu hoàn toàn hiện tượng mất chữ/cắt chữ.
        + Thay thế chấm unicode bằng pill badge bo góc hiển thị trạng thái lập lịch ("Đang hoạt động" / "Đã tạm dừng").
  + Intercept tập trung qua dynamic proxy `AuditConnectionProxy`/`AuditStatementProxy` bọc `DbUtil.getConnection()`, ghi audit log DML kèm thời gian, người dùng (UserSession), số dòng affected, tham số (tự mask mật khẩu/chuỗi nhạy cảm). Tách file `app.log` và `audit.log` qua `AsyncAppender` Logback rolling 30 ngày.
  + Modal `ShutdownDialog` + `AppShutdownManager`: Bắt toàn bộ thao tác thoát trên mọi JFrame (`MainFrame`, `DangNhapUI`, `KhoiDongUI`), chạy DIFF + LOG ca làm việc, hiển thị tiến độ, timeout bảo vệ, chống tắt ngang.
  + Chuẩn hóa trạng thái phiếu sửa chữa KTV: xóa bỏ toàn bộ nhãn sai lệch "Mới phân công" / "Mới", đồng bộ chuẩn 4 trạng thái theo enum (`CHO_SUA` -> "Chờ sửa", `DANG_SUA` -> "Đang sửa", `CHO_LINH_KIEN` -> "Chờ linh kiện", `HOAN_TAT` -> "Hoàn tất") trên KPI card, bảng công việc gần đây, dialog chi tiết phiếu, biểu đồ tiến độ và donut tỷ lệ hoàn thành (`MainFrame.java`, `ThongKeSuaChuaUI.java`).
  + Dùng JCalendar cho bộ lọc ngày của Thống kê báo cáo và Quản lý khuyến mãi Chủ cửa hàng; khóa nhập tay và chuẩn hóa đọc ngày qua `UIInputRules`.
  + Khôi phục điều hướng `MainFrame` tới ba màn Chủ cửa hàng Dịch vụ, Linh kiện/tồn kho và Xử lý yêu cầu linh kiện; các lớp đã có nhưng bị route KTV ghi đè khi nhập nhánh.
  + Rà soát lớp trùng theo vai trò: gỡ `TraCuuChuCuaHangUI` chỉ kế thừa `TraCuuUI`; các vai trò dùng chung `TraCuuUI(VaiTro)` và `TraCuuController`/`TraCuuDAO`.
  + `mvn -q compile` đạt 100%, không sinh file `.class` rác.

## Mới làm (02/11/2026)
- **Hoàn thiện UI/UX và logic nghiệp vụ phân hệ Kỹ thuật viên & Tra cứu:**
  + KPI "Phiếu cần hoàn thành" Dashboard KTV: tính chuẩn `tong - hoanTat` (trừ đi số phiếu đã hoàn tất).
  + Bảng "Phiếu được phân công gần đây" Dashboard KTV: bỏ cột Thao tác, hỗ trợ click đúp mở phiếu sửa chữa; nút footer đổi thành "Xem chi tiết" (mở trực tiếp phiếu được chọn vào form Cập nhật phiếu).
  + Biểu đồ Dashboard KTV: kết nối 100% số liệu thực 7 ngày gần nhất & tuần này từ DB (`layThongKeKTV`), vẽ động thanh tiến độ và donut tỷ lệ hoàn thành.
  + `ThongKeSuaChuaUI`: biểu đồ cột kết nối dữ liệu thực DB theo ngày tiếp nhận; tính toán động thời gian xử lý từng phiếu từ `ngayGioTiepNhan` và `ngayHoanTat`; tính thời gian xử lý trung bình cho thẻ phụ.
  + `CapNhatPhieuSuaChuaUI` & `YeuCauLinhKienKTVUI`: thêm `DocumentFilter` chỉ cho phép nhập số nguyên dương cho các ô số lượng (chặn hoàn toàn việc nhập ký tự chữ).
  + Combobox chọn phiếu trong `CapNhatPhieuSuaChuaUI`: chỉ hiển thị phiếu chưa hoàn tất (chờ sửa / đang sửa / chờ linh kiện).
  + `TraCuuUI`: điều chỉnh chiều cao table kết quả tab "Lịch sử sửa chữa" hiển thị đúng 4 dòng (212px), table chi tiết dịch vụ/linh kiện hiển thị đúng 3 dòng (172px) kèm thanh cuộn mượt mà; hỗ trợ tự cuộn khi có nhiều dòng hơn.
  + Dashboard KTV: sửa nút "Xem chi tiết" và sự kiện click đúp trong bảng "Phiếu được phân công gần đây" hiển thị modal dialog chi tiết phiếu với đầy đủ trường thông tin; hỗ trợ nút hành động "Cập nhật phiếu này ↗" (với phiếu chưa hoàn tất) và "Xem lịch sử sửa chữa ↗" (với phiếu hoàn tất).
  + `CapNhatPhieuSuaChuaUI`: bổ sung biến hàng đợi `maPhieuChoSua` để tự động chọn đúng phiếu kể cả khi dữ liệu phiếu đang được nạp bất đồng bộ.
  + Chuẩn hóa JDateChooser: tăng lề trong lên 10px và chiều rộng 170px, triệt tiêu hoàn toàn lỗi góc bo cắt chữ số ngày.
  + `mvn -q compile` đạt 100%, không còn file `.class` rác.

## Mới làm (01/11/2026)
- **Căn lại Thông tin cá nhân**: thống nhất alignmentX cho tiêu đề, tab và các thành phần BoxLayout để hồ sơ kéo về mép nội dung chuẩn, tránh tab/hồ sơ bị thụt sang phải.
- **Validate định dạng input ở UI**: thêm `UIInputRules` dùng regex cho từ khóa tìm kiếm, tên dịch vụ/linh kiện/khuyến mãi, họ tên, số điện thoại, CCCD, địa chỉ và ghi chú; validate ngày dd/MM/yyyy nghiêm ngặt ở thống kê, hồ sơ nhân viên và lọc/form khuyến mãi. Hiển thị thông báo trước khi gọi controller/DB; select enum/trạng thái/danh mục giữ giới hạn bằng option có sẵn.
- **Sửa biểu đồ thống kê báo cáo**: biểu đồ doanh thu gom theo ngày/kỳ trong khoảng lọc (theo ngày tới 14 ngày, tuần tới 180 ngày, tháng tới 2 năm, dài hơn theo năm), thay cho bốn ca cố định; bảng chi tiết dùng cùng nhóm kỳ. Điều chỉnh donut có vùng đệm theo độ dày nét và kích thước thành phần để vòng tròn không bị cắt.
- **Thống kê báo cáo**: đổi tên mục điều hướng và tiêu đề; thêm Xuất báo cáo ra CSV UTF-8 theo loại/khoảng ngày đang xem, gồm tiêu đề cột và số liệu bảng. Nút xuất chỉ bật khi dữ liệu đã tải khớp bộ lọc hiện tại.
- **Thống kê doanh thu**: bổ sung nút Xem ngang hàng bộ lọc loại báo cáo/khoảng ngày, cùng chiều cao 44px; nút chạy truy vấn báo cáo theo bộ lọc hiện tại và khóa trong lúc tải.
- **Tinh chỉnh màn tìm kiếm CCH**: bỏ lời nhắc nhập mã/tên ở trạng thái rỗng của dịch vụ, linh kiện, khuyến mãi và nhân viên. Tìm khuyến mãi được lọc chỉ bằng khoảng ngày khi từ khóa trống; nút Tìm kiếm/Làm mới cao 44px khớp input. `mvn -q compile` đạt.
- **Rà nhãn trạng thái dữ liệu trong UI**: gỡ các cụm “tự sinh”, “chỉ đọc”, “tự tính” khỏi nhãn màn dịch vụ, linh kiện, hồ sơ nhân viên, tài khoản, nhập kho, tra cứu và chi tiết phiếu kỹ thuật. Giữ nguyên field không cho sửa, mã do DB cấp và tuổi được tính từ hồ sơ. `mvn -q compile` đạt.
- **Quản lý dịch vụ**: thêm nút Tìm kiếm và Làm mới ngay hàng bộ lọc, giữ tìm kiếm bằng Enter và Escape để reset; Làm mới xóa từ khóa, bộ lọc, thông báo và kết quả đang hiển thị.
- **Dọn lời nhắc trên trang yêu cầu/tài khoản/hồ sơ**: xóa câu hướng dẫn lọc ở footer và trạng thái ban đầu, thay placeholder tra cứu dài bằng trạng thái rỗng ngắn; bỏ ghi chú nguồn hồ sơ trên tab cá nhân.
- **Khớp chiều cao nút lọc**: nút Tìm kiếm/Làm mới trên trang yêu cầu cập nhật linh kiện và khuyến mãi được cố định đúng 44px, bằng chiều cao input.
- **Khuyến mãi**: cố định thêm min/max 44px cho hai nút Thêm chương trình và Sửa trong hàng BoxLayout để chúng không bị layout thu nhỏ.
- **Tra cứu theo bộ lọc**: các màn dịch vụ, linh kiện/tồn kho, yêu cầu cập nhật linh kiện và nhân viên cho phép để trống từ khóa rồi bấm Tìm kiếm để áp dụng dropdown (kể cả “Tất cả”); DAO hỗ trợ bộ lọc rỗng và màn tra cứu linh kiện cũng dùng trạng thái lọc khi không nhập từ khóa. Màn mới mở vẫn không tải dữ liệu.
- **Làm mới danh sách**: các nút Làm mới reset ô nhập/bộ lọc và xóa bảng, lựa chọn, chi tiết để quay về trạng thái rỗng 0 dòng; áp dụng cho dịch vụ, linh kiện, yêu cầu cập nhật, khuyến mãi, nhân viên, tài khoản, tra cứu và bộ lọc phiếu trên Dashboard.

## Mới làm (30/10/2026 – hạ tầng & dọn code)
- **Chạy hạ tầng DB + seed**: `docker compose -f infra/docker/compose.yml up -d` → SQL Server 2022 healthy, `db-init` tạo database, Flyway migrate thành công V1 + V2 (`flyway_schema_history` = 2 dòng, success), nạp seed `infra/database/seed/CarZone_Demo.sql` qua `sqlcmd -b` exit 0 (6 KH, 6 xe, 7 DV, 9 LK, phiếu đủ 4 trạng thái, 2 hóa đơn, 6 yêu cầu nhập LK, 5 nhân viên/5 tài khoản).
- Lưu ý vận hành: lần chạy Flyway đầu tiên lỗi `sp_getapplock` (Could not load the DLL, 126) → chỉ cần `docker restart carzone_flyway` là migrate bình thường.
- **Đổi tên entry point & di chuyển logo**: xóa `src/app/Test.java`, đổi `src/app/Main.java` → `src/app/App.java` (lớp `App`, `pom.xml` `main.class = app.App`, `AuthController` gọi `app.App.moManHinhChinh`). Chuyển `src/ui/carzone_logo.png` → `img/carzone_logo.png` (cùng cấp `src`), cập nhật fallback path trong `MainFrame`, `DangNhapUI`, `KhoiDongUI`, `DashboardUI`.
- **Giải quyết conflict `DashboardUI`**: giữ điều hướng `NavigationItem`/`navId`, ghép Thống kê doanh thu vào nav `NAV_THONG_KE`; không tự xóa bên nào khi chưa hỏi.
- **`src/dao/ThongKeDoanhThuDao` chuyển sang JDBC thuần** (`DbUtil`, try-with-resources) thay cho JPA/Hibernate đã gỡ khỏi repo; tầng tổng hợp báo cáo sau đó chuyển từ `service/` sang `controller/` (xem mục dưới).
- **Đơn giản hóa tầng DAO**: bỏ interface + subpackage `dao.impl`, gộp mỗi DAO thành **MỘT class duy nhất** tại `src/dao/` (javadoc method giữ nguyên từ interface; lồng lại class `ChiTietHoaDonDao.ChiTietItem` bị mất khi gộp). Sửa `AuthController`, `ThongKeDoanhThuController` không còn gọi `*DaoImpl`; cập nhật `AGENTS.md`, `decisions.md`, skill `jdbc-dao-mapping` và `new-feature-slice`.
- **Bỏ package `service` + gom ảnh về `img/`**: xóa `src/service/ThongKeDoanhThuService.java` → `src/controller/ThongKeDoanhThuController.java` (đổi tên class + field `service` → `controller` trong `ThongKeDoanhThuUI`; `src/` giờ chỉ còn 8 package: app, connectDB, controller, dao, entity, enums, session, ui). Chuyển `src/resources/images/*` → `img/` (bản `carzone_logo.png` trùng byte với bản đã có nên chỉ giữ 1), xóa `src/resources/`; đổi `<resources>` trong `pom.xml` từ `src` sang `img` để ảnh vẫn vào gốc classpath. Cập nhật đường dẫn trong `MainFrame`, `DangNhapUI`, `KhoiDongUI`, `DashboardUI` (`getResource("/carzone_logo.png")` + fallback `img/carzone_logo.png`), `ThongKeDoanhThuUI` (`/revenue_illustration.png`).
- **Dọn entity**: xóa 3 file khoá ghép còn sót từ JPA — `ChiTietDichVuId.java`, `ChiTietLinhKienId.java`, `ChiTietDoiTraId.java` — cùng trường `id` và các dòng `this.id.set...` trong 3 entity; `ChiTietDoiTra` tách hẳn trường `stt` (trước nằm trong `id`) ra thành `private int stt` (`getStt`/`setStt`/`hashCode`/`equals`/`toString` dùng trực tiếp `stt`). Tách 3 khai báo `KhuyenMai` đang gộp trên 1 dòng. Bỏ toàn bộ dòng trắng thừa giữa các khai báo thuộc tính ở 14 entity (javadoc vẫn dính field). Không còn tham chiếu `*Id` nào trong `src/`.
- **Cập nhật skill để code không conflict**: tạo `ai-context/skills/tranh-conflict.md` (đọc đầu phiên và khi `git status` có `UU`: dừng hỏi thay vì tự chọn bên, không tái tạo cấu trúc đã gỡ, chỉ sửa đúng đoạn cần — không format lại toàn file, cập nhật tài liệu cùng lúc với code, không tự `git add`/`commit`/`merge`). Bổ sung `new-feature-slice` (8 package còn lại, entry `app.App`, ảnh ở `img/`, cấm tạo `*Id.java`, quy tắc định dạng field, mục "Cấm" liệt kê cấu trúc đã gỡ), `swing-screen` (quy tắc thêm ảnh mới chỉ vào `img/` + bước kiểm tra conflict trước khi báo xong), sửa mô tả `jdbc-dao-mapping` (còn ghi "interface và JDBC impl"). Thêm dòng `tranh-conflict` vào bảng skill của `AGENTS.md`.
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
  + Di chuyển file khởi chạy chính sang [App.java](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/app/App.java) (`package app;`), cấu hình `main.class = app.App` trong `pom.xml` (đổi tên từ `Main.java` ngày 30/10/2026).
- **Mở rộng dữ liệu Seed Demo & Hoàn thiện thông báo lỗi Đăng nhập**:
  + Mở rộng `infra/database/seed/CarZone_Demo.sql`: Bổ sung danh mục dịch vụ (`DV00004` -> `DV00007`), linh kiện (`LK00004` -> `LK00009` với 2 linh kiện tồn 0 kích hoạt cảnh báo hết hàng Figma), 6 khách hàng & xe (Toyota, Mazda, Honda, Hyundai, Kia, Ford), phiếu sửa chữa ở đủ 4 trạng thái (`CHO_SUA`, `DANG_SUA`, `CHO_LINH_KIEN`, `HOAN_TAT`), hóa đơn thanh toán tiền mặt & chuyển khoản, và các phiếu yêu cầu nhập linh kiện theo đúng luồng chuyển trạng thái (`CHO_DUYET`, `DA_DUYET`, `DA_NHAP_KHO`, `TU_CHOI`). Đã chạy nạp thành công vào SQL Server.
  + Sửa triệt để lỗi thông báo khi đăng nhập sai trong [DangNhapUI.java](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/ui/DangNhapUI.java): Ngăn `DocumentListener` tự động xoá khung lỗi khi gọi `pfMatKhau.setText("")`, làm nổi bật viền đỏ trường nhập, cảnh báo âm thanh `Toolkit.beep()`, cập nhật thanh trạng thái và hiển thị `JOptionPane.showMessageDialog` rõ ràng.
- **Chuyển đổi ORM Hibernate sang JDBC thuần & Tái cấu trúc DAO**:
  + Gỡ bỏ hoàn toàn `hibernate-core`, `flyway-core`, `flyway-sqlserver` khỏi `pom.xml`.
  + Thay thế `JpaUtil` bằng `connectDB.DbUtil` quản lý kết nối JDBC `DriverManager`.
  + Lược bỏ toàn bộ JPA annotation (`@Entity`, `@Table`, `@Id`, v.v.) khỏi các entity, giữ nguyên nghiệp vụ và validation.
  + Chuẩn hóa kiến trúc DAO theo mẫu người dùng cung cấp:
    * Interface DAO đặt trực tiếp trong package `dao` (không có tiền tố `I`): [TaiKhoanDao](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/dao/TaiKhoanDao.java), [NhanVienDao](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/dao/NhanVienDao.java), [HoaDonDao](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/dao/HoaDonDao.java), [ChiTietHoaDonDao](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/dao/ChiTietHoaDonDao.java), [PasswordResetDao](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/dao/PasswordResetDao.java).
    * Lớp thực thi đặt trong subpackage `dao.impl`: [TaiKhoanDaoImpl](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/dao/impl/TaiKhoanDaoImpl.java), [NhanVienDaoImpl](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/dao/impl/NhanVienDaoImpl.java), [HoaDonDaoImpl](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/dao/impl/HoaDonDaoImpl.java), [ChiTietHoaDonDaoImpl](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/dao/impl/ChiTietHoaDonDaoImpl.java), [PasswordResetDaoImpl](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/dao/impl/PasswordResetDaoImpl.java). *(Cấu trúc này đã thay đổi ngày 30/10/2026: gộp hết về `src/dao/`, xem mục 30/10.)*
  + Chuyển đổi [DashboardController](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/controller/DashboardController.java), [AuthController](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/controller/AuthController.java), [DashboardUI](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/ui/DashboardUI.java), [MainFrame](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/ui/MainFrame.java), [KhoiDongUI](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/ui/KhoiDongUI.java) sang JDBC thuần qua `DbUtil`.
  + Kiểm thử biên dịch toàn dự án: `mvn clean compile` thành công 100% (46 source files, 0 error).
## Mới làm (03/11/2026 - Hoàn thiện toàn bộ phân hệ Tiếp nhận viên TNV)
- **DAO & Controllers**: Tích hợp các nghiệp vụ tiếp nhận, thanh toán, đổi trả, khách hàng/xe và thống kê vào tầng JDBC theo cấu trúc DAO hiện hành (một class thực thi duy nhất cho mỗi nhóm dữ liệu). Controllers: KhachHangVaXeController, LapPhieuTiepNhanController, LapHoaDonThanhToanController, TaoPhieuDoiTraController, TraCuuController, ThongKeTiepNhanController.
- **Giao diện UI (Figma 100%)**:
  + QuanLyKhachHangVaXeUI (UC016): Quản lý 2 cột Khách hàng & Xe, tìm kiếm, thêm/sửa, nạp xe theo KH.
  + LapPhieuTiepNhanUI (UC010): Lập phiếu tiếp nhận, phân công KTV, thêm dịch vụ đề nghị, tính tiền tạm tính.
  + LapHoaDonThanhToanUI (UC014, UC015): Lập hóa đơn từ phiếu HOAN_TAT, áp dụng khuyến mãi, popup thanh toán tiền mặt/chuyển khoản có mã giao dịch.
  + TaoPhieuDoiTraUI (UC012): Đổi trả hóa đơn trong 7 ngày, chọn dịch vụ/linh kiện, chọn hướng xử lý (HOAN_TIEN, DOI_MOI, SUA_LAI_MIEN_PHI).
  + TraCuuUI (UC007): Dùng chung một màn hình theo vai trò, phân quyền các tab cho CCH/TNV/KTV.
  + ThongKeTiepNhanUI (UC013): Nâng cấp 3 phần dashboard, bộ chọn ngày DatePickerField popup lịch, lọc khi bấm nút, reset chuẩn.
- **Tích hợp MainFrame**: Đăng ký toàn bộ các trang TNV vào MainFrame.java, gắn navigation item NAV_THONG_KE_TIEP_NHAN, fix lỗi SQL join/where trong DashboardController.
- **Biên dịch**: mvn compile thành công 100% (77 source files, 0 error).

## Việc kế tiếp (gợi ý, chờ người dùng chốt thứ tự)
- Phát triển màn hình chức năng chuyên sâu: Tiếp nhận xe / Lập phiếu tiếp nhận sửa chữa (TNV) hoặc Quản lý linh kiện và kho (CCH).

## Tích hợp nhánh (04/11/2026)
- Đã merge `feature/tan/recipient`, `feature/backup` và `feature/hau/store-owner` vào `develop`.
- Khi có khác biệt, giữ dashboard, màn hình CCH, tìm kiếm chung và `TraCuuUI` trên `develop`; giữ các màn TNV, thống kê tiếp nhận và sao lưu/phục hồi đã tích hợp.
- Chuẩn hóa tên các DAO sang hậu tố `DAO`; gộp DAO TNV vào một class cụ thể trong `src/dao/`, không giữ cặp interface/implementation trong `dao.impl`.
- Kiểm tra `mvn -q compile` thành công sau khi tích hợp.
- Pull cập nhật tiếp theo từ `feature/backup`: giữ instance màn CCH trong `MainFrame`, giữ chọn ngày bằng JCalendar và các bộ lọc số trong `UIInputRules`; bổ sung các thay đổi audit/PIN của backup. Đã giải quyết các conflict và `mvn -q compile` thành công.

- **Gắn UI phân quyền vào Quản lý tài khoản (05/11/2026):** thêm nút Phân quyền và dialog bật/tắt quyền chỉ cho Tiếp nhận viên/Kỹ thuật viên; lưu qua controller/DAO vào QuyenChucNang, báo rõ nếu thiếu migration V6. Đồng thời bỏ lại cột Thao tác theo yêu cầu trước đó.

- **Hoàn thiện phân quyền theo vai trò (05/11/2026):** cấu hình chỉ gồm Tiếp nhận viên và Kỹ thuật viên; quyền được tải từ DB khi đăng nhập, menu lọc theo quyền và MainFrame chặn điều hướng tới module bị thu hồi. Chủ cửa hàng giữ toàn quyền.

- **Hiển thị email trong Quản lý nhân viên (05/11/2026):** thêm cột Email vào bảng kết quả và giữ renderer trạng thái tài khoản ở cột cuối; email lấy từ trường nhân viên đã được DAO map.
- **Tinh gọn nút màn quản trị hồ sơ/tài khoản (05/11/2026):** bỏ nút Quay lại ở Quản lý nhân viên và Quản lý tài khoản; bỏ Tra cứu và Tạo tài khoản khỏi Quản lý nhân viên (cả thanh tác vụ và chi tiết hồ sơ). Tạo tài khoản vẫn thao tác tại màn Quản lý tài khoản.
- **Căn lại card cảnh báo Dashboard CCH (05/11/2026):** đặt card cảnh báo hết hàng trong cột căn NORTH để giữ chiều cao theo nội dung thay vì bị kéo bằng bảng yêu cầu; các hàng linh kiện giữ chiều cao tự nhiên, tránh tạo khoảng trắng lớn giữa các mã.
- **Chuyển phân quyền từ vai trò sang tài khoản (05/11/2026):** thêm migration V7 lưu tập quyền cho từng tài khoản; dialog chọn hồ sơ hiện tại, toàn bộ màn hình TNV/KTV hoặc màn hình tùy chỉnh. Quyền chéo vai trò mở trang ở chế độ xem, giữ kiểm tra vai trò khi ghi nghiệp vụ; thống kê tiếp nhận của người không phải CCH được giới hạn theo mã người đăng nhập.
