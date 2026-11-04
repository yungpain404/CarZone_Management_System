# CarZone – Hệ thống quản lý Gara ô tô

> Tệp hướng dẫn chung cho MỌI công cụ AI/agent làm việc trong repo này (Claude Code, Codex, Cursor, Copilot, Gemini, ...). Nhiều người cùng code nên chỉ sửa ở đây và trong `ai-context/`, không tạo bản riêng cho từng công cụ.

Đồ án môn Phát triển ứng dụng (Nhóm 01, ĐH Công nghiệp TP.HCM). Phần mềm desktop nội bộ cho nhân viên gara, 3 vai trò: Chủ cửa hàng, Tiếp nhận viên (kiêm thu ngân), Kỹ thuật viên.

## Công nghệ (theo `pom.xml` thật, không đoán thêm)
- Java Swing. Maven: `maven.compiler.release` = 17. **Nhóm chốt dùng Java 17** (Eclipse `.classpath` của repo đang ghi JavaSE-21, mỗi người nên đổi JRE của project về 17).
- Trong pom: `mssql-jdbc` 12.6.4, `flatlaf` 3.7, JUnit 5. Shade có `ServicesResourceTransformer`.
- **Cơ chế truy cập dữ liệu: JDBC thuần (DriverManager)**, quản lý tập trung qua `connectDB/DbUtil` và `connectDB/DatabaseConfig`. **ĐÃ BỎ HOÀN TOÀN HIBERNATE/JPA** (không còn `persistence.xml`, không dùng `jakarta.persistence.*`, entity là POJO thuần).
- Cấu hình kết nối đọc từ `infra/docker/.env` qua `DatabaseConfig`.
- `pom.xml`: `sourceDirectory` là `src`; `main.class` = `app.App`. Entry point chính tại [src/app/App.java](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/app/App.java).
- Hạ tầng: Docker Compose chạy SQL Server 2022 (collation `Vietnamese_CI_AS`) → tạo DB → Flyway migrate (Flyway chạy trong container, không chạy từ Java).

## Cấu trúc thư mục thật
```
src/
├── app/          # Entry point chính (App.java)
├── connectDB/    # Kết nối JDBC thuần (DatabaseConfig.java, DbUtil.java)
├── controller/   # Điều phối nghiệp vụ (AuthController, DangNhapController, DashboardController, PhieuTiepNhanSuaChuaController, PhieuYeuCauNhapLinhKienController, TraCuuController, ...)
├── dao/          # Lớp DAO JDBC thuần, mỗi bảng/nhóm 1 class (TaiKhoanDao, NhanVienDao, HoaDonDao, ChiTietHoaDonDao, PhieuTiepNhanSuaChuaDAO, PhieuYeuCauNhapLinhKienDAO, TraCuuDAO, ...)
├── entity/       # Thực thể POJO (validate trong setter, raw setters cho DAO map DB)
├── enums/        # Các enum trạng thái và vai trò
├── session/      # Quản lý phiên làm việc người dùng (UserSession Singleton)
└── ui/           # Giao diện Swing FlatLaf bám sát Figma (MainFrame, DashboardUI, DangNhapUI, KhoiDongUI, PhieuPhanCongUI, CapNhatPhieuSuaChuaUI, YeuCauLinhKienKTVUI, ThongKeSuaChuaUI, TraCuuUI, ThongTinCaNhanUI, ...)
img/              # Ảnh dùng chung (carzone_logo.png, revenue_illustration.png, nav_*.png); pom copy vào gốc classpath
infra/docker/compose.yml, infra/docker/.env
infra/database/fly_way_migrations/V*.sql   # migration Flyway (tên thư mục có dấu gạch dưới)
infra/database/seed/CarZone_Demo.sql       # kịch bản chạy thử, KHÔNG phải migration
docs/                                      # tài liệu của nhóm (báo cáo Word...), KHÔNG dành riêng cho AI
ai-context/                                # ngữ cảnh + skill cho AI (xem bên dưới)
```
Làm việc trên nhánh `develop`; chủ repo tự merge sau, agent KHÔNG merge. Nhánh `feature/infrastructure` có V2 + seed mà `develop` chưa có: entity `KhachHang` đã map cột của V2 (`thanhVienThanThiet`, `diemTichLuy`), nên muốn chạy DB khớp entity thì V2 phải có trong DB (hỏi chủ repo trước khi tự sao chép).

## Kiến trúc phân tầng
`ui` → `controller` → `dao` → `entity`/`enums`. Kết nối DB ở `connectDB`, phiên ở `session`.
- `ui`: Chỉ dựng giao diện, bắt sự kiện, gọi `controller` qua `SwingWorker` ở luồng nền. Tuyệt đối không viết SQL, không mockup dữ liệu, không xử lý nghiệp vụ tại UI.
- `session`: `UserSession` (Singleton) lưu giữ thông tin nhân viên đăng nhập, vai trò, thời điểm phiên hiện tại.
- `controller`: Tiếp nhận yêu cầu từ `ui`, kiểm tra quyền từ `session`, gọi `dao`, điều phối luồng nghiệp vụ.
- `dao`: **Mỗi bảng/nhóm nghiệp vụ là MỘT class duy nhất** đặt tại `src/dao/` (từ 30/10/2026 đã bỏ interface và subpackage `dao.impl`):
  + Tên file viết hoa ký tự đầu, không tiền tố `I`, không hậu tố `Impl` (ví dụ `TaiKhoanDao.java`, `NhanVienDao.java`, `HoaDonDao.java`).
  + Class đó chính là lớp thực thi JDBC thuần qua `DbUtil.getConnection()`, luôn đóng tài nguyên trong `try-with-resources`.
- `entity`: POJO thuần Java, validate logic chặt chẽ trong các setter thông thường. Cung cấp thêm các raw setters (`setXxxRaw(...)`) để DAO gán dữ liệu trực tiếp khi đọc từ `ResultSet` mà không bị kiểm tra lặp.
- **Luật nghiệp vụ kết hợp hai tầng (đã chốt):** DB có trigger/stored procedure/CHECK (V1) và Java cũng validate. DB sinh mã và trừ/cộng tồn kho, Java không làm hai việc này; Java validate sớm, kiểm quyền, dịch lỗi DB (50xxx) sang tiếng Việt. Đổi luật thì sửa `docs/business-rules.md` trước rồi cập nhật cả hai tầng.

## Luật nghiệp vụ (chi tiết: `docs/business-rules.md`)
- Phiếu sửa chữa: `CHO_SUA → DANG_SUA ⇄ CHO_LINH_KIEN → HOAN_TAT`. Mỗi phiếu đúng 1 kỹ thuật viên chính.
- Chỉ phiếu `HOAN_TAT` mới lập được hóa đơn; tổng tiền do hệ thống tính, không nhập tay.
- Linh kiện dùng không vượt tồn; tồn tự trừ.
- Hóa đơn `DA_THANH_TOAN` không sửa/xóa/quay lại. Chuyển khoản bắt buộc có `maGiaoDich`.
- Đổi trả: hóa đơn đã thanh toán, trong 7 ngày; hoàn tiền trừ vào doanh thu ngày phát sinh.
- Thuế theo nội dung hóa đơn (chỉ linh kiện 1,5%, chỉ dịch vụ 7%, cả hai 4,5%). Khuyến mãi: tối đa 1/hóa đơn, còn hiệu lực; danh sách áp dụng rỗng = giảm tất cả, có danh sách = chỉ giảm các mặt hàng trong danh sách.
- Entity giữ `java.util.Date` và `double` (đã chốt), không đổi sang `LocalDateTime`/`BigDecimal`.

## Quy ước
- Giữ nguyên tên lớp/thuộc tính tiếng Việt không dấu theo tài liệu (`HoaDon`, `maHoaDon`, ...).
- Mã dịch vụ/linh kiện: `DV00001`/`LK00001` (≥ 5 số, mở rộng được). Mã hóa đơn có tiền tố `HD`.
- Không hard-code mật khẩu/chuỗi kết nối trong code; đọc từ `.env`. `.env` được cố ý đẩy lên git cho cả nhóm dùng chung (dữ liệu chỉ lưu local), đừng "sửa" việc này. Không chép nội dung `.env` vào tài liệu hay câu trả lời.
- Mật khẩu tài khoản lưu dạng băm SHA-256 hex in hoa (cùng dạng dữ liệu mẫu trong DB); việc băm nằm trong `TaiKhoan.setMatKhau`.
- Lớp `UIInputRules`: Bắt buộc là `public final class UIInputRules` với toàn bộ static methods là `public static` để đảm bảo trình biên dịch và Language Server luôn phân giải được xuyên suốt các file trong package `ui`.
- Tệp `.classpath`: Là tệp sống còn của Eclipse JDT / VS Code Java Extension. TUYỆT ĐỐI KHÔNG XÓA `.classpath` và KHÔNG THÊM `<classpathentry kind="lib" path="pom.xml"/>` (pom.xml là XML, không phải thư viện ZIP/JAR, thêm vào sẽ gây lỗi build path fatal).
- Định dạng tiền tệ và Locale: Luôn dùng `Locale.forLanguageTag("vi-VN")` thay cho constructor deprecated `new Locale("vi", "VN")`.

## Lệnh
- Khởi động DB + migrate: `docker compose -f infra/docker/compose.yml up -d`
- Dựng lại DB sạch: `docker compose -f infra/docker/compose.yml down -v` rồi `up -d`
- Biên dịch: `mvn -q compile` (Maven tải được thư viện trên máy nhóm). Tuyệt đối KHÔNG chạy lệnh `javac` đơn lẻ mà thiếu cờ `-d target/classes`, tránh tạo các file `.class` rác vào trong thư mục nguồn `src/`.
- Chưa cần viết test ở giai đoạn này (nhóm quyết định tạm hoãn).

## Ngữ cảnh và skill cho AI (đọc theo nhu cầu, đừng đọc hết)
| Tệp | Khi nào đọc |
|---|---|
| `ai-context/PROGRESS.md` | Đầu mỗi phiên; xong việc thì cập nhật |
| `ai-context/skills/tranh-conflict.md` | Đầu mỗi phiên và khi `git status` có `UU` — tránh xung đột với nhóm |
| `ai-context/decisions.md` | Quyết định đã chốt + câu hỏi còn mở. Không lật lại |
| `ai-context/business-rules.md` | Làm bất cứ thứ gì chạm luật nghiệp vụ |
| `ai-context/skills/new-feature-slice.md` | Thêm một chức năng đi qua nhiều lớp |
| `ai-context/skills/jdbc-dao-mapping.md` | Viết/sửa lớp DAO JDBC thuần (mỗi bảng 1 class) và ánh xạ ResultSet → entity |
| `ai-context/skills/flyway-migration.md` | Đổi schema DB |
| `ai-context/skills/swing-screen.md` | Làm màn hình Swing bám sát Figma |
| `ai-context/skills/verify-business-rules.md` | Kiểm tra lại luật nghiệp vụ trước khi báo xong |

Mỗi skill là một tệp Markdown thường (có phần đầu `name`/`description`), công cụ nào cũng đọc được: tới lượt thì mở tệp tương ứng và làm theo.

## Cách làm việc
- Gặp mâu thuẫn giữa code, DB và tài liệu: dừng, nêu rõ chỗ lệch, hỏi. Không tự chọn bên. Điều chưa chắc thì hỏi, không tự suy diễn.
- Không sửa migration cũ; đổi schema bằng migration mới.
- Nếu công cụ hỗ trợ subagent: tìm kiếm rộng nhiều file thì giao việc đó, chỉ lấy kết luận.
- Báo cáo Word trong `docs/` rất dài: chỉ đọc đúng mục của chức năng đang làm.
- Nhiều người cùng sửa: đổi `ai-context/` thì ghi rõ trong commit; không ghi thông tin cá nhân, mật khẩu hay nội dung `.env` vào đó.

## Quy tắc thiết kế UI – BẮT BUỘC
**Figma chính thức:** https://www.figma.com/design/DgNDrY8qVy5NyzHHjDzyVH/GaraOTO_CarZone

- **Mọi màn hình Swing PHẢI bám sát thiết kế Figma** (màu sắc, layout, font, kích thước, vị trí thành phần). Không được tự sáng tác layout khi Figma đã vẽ sẵn.
- **Theme & Look and Feel**: Dự án dùng giao diện **Sáng** (`FlatLightLaf.setup()`, tuyệt đối không dùng DarkLaf). Bảng màu HEX inspect từ Figma.
- **Khung giao diện chính (Master Frame) & Phóng to toàn màn hình**:
  - Sử dụng [MainFrame.java](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/ui/MainFrame.java) làm layout tổng thể điều hướng (Header + Side Navigation theo vai trò + ContentPanel `CardLayout`).
  - **Tự động phóng to Full màn hình (Maximized on Login - BẮT BUỘC)**: Khi nhân viên đăng nhập thành công vào hệ thống, `MainFrame` BẮT BUỘC tự động phóng to toàn màn hình (`setExtendedState(JFrame.MAXIMIZED_BOTH)`). Vẫn khai báo đồng thời `setSize(1170, 800)` và `setMinimumSize(new Dimension(1024, 700))` để khi người dùng nhấn Restore Down, cửa sổ co về kích thước chuẩn không bị vỡ giao diện.
- **Quy tắc Kiểm soát Thoát ứng dụng (Centralized Shutdown Handling - BẮT BUỘC)**:
  - Mọi thao tác thoát (nút đóng cửa sổ `X`, nút Đăng xuất, đóng MainFrame hay `System.exit`) đều phải được kiểm soát tập trung qua `AppShutdownManager` và `WindowListener`.
  - **Phân quyền thoát đa máy trạm**: CHỈ máy trạm của **Chủ cửa hàng (`VaiTro.CHU_CUA_HANG`)** mới thực hiện tác vụ sao lưu ca làm việc khi thoát. Các vai trò Tiếp nhận viên và Kỹ thuật viên thoát ngay lập tức mà không chạy sao lưu để tránh xung đột và ghi đè file trên server.
- **Quy tắc Giám sát Hành vi Phi lý & Hàng rào Xác thực Mã PIN Cấp 2 (Behavioral Anomaly & PIN Barrier - BẮT BUỘC)**:
  - Khi kẻ tấn công chiếm quyền hoặc có hành vi phá hoại dữ liệu, hệ thống tự động đánh chặn ngang tại tầng `AuditStatementProxy` và UI trước khi câu lệnh xuống CSDL.
  - **Danh sách 3 nhóm hành vi phi lý bị chặn**:
    1. *Bất thường tốc độ & số lượng (Bulk & Velocity Anomalies)*: Xóa hàng loạt dữ liệu cốt lõi (≥ 3 bản ghi trong < 2 giây); xóa không kèm `WHERE`; sửa đổi hàng loạt trạng thái tài khoản hoặc câu lệnh DDL phá hủy (`DROP`, `TRUNCATE`, `ALTER TABLE ... DROP`).
    2. *Bất thường thời gian (Temporal Anomalies)*: Thao tác nhạy cảm (xóa/sửa dữ liệu cốt lõi) vào khung giờ nửa đêm / đóng cửa (mặc định 00:00 - 05:00 sáng).
    3. *Bất thường cấu trúc & liên kết (Structural & Scope Anomalies)*: Can thiệp cấu hình bảo mật hệ thống; xóa Chủ thể cốt lõi (`CCH...` / Chủ cửa hàng) gây sụp đổ toàn bộ bảng con phụ thuộc (Cascading Danger).
  - **Cơ chế xác thực Mã PIN cấp 2**:
    + Tạm dừng lập tức tiến trình, hiển thị modal dialog `PinVerificationDialog` trên EDT thông báo rõ hành vi vi phạm.
    + Yêu cầu nhập đúng mã PIN bảo mật cấp 2 (băm SHA-256, cấu hình qua `backup.properties` / biến môi trường).
    + Nhập đúng PIN: Cấp phép miễn trừ tạm thời (`suppression window` 60 giây) và cho phép tiến trình tiếp tục chạy.
    + Nhập sai quá 3 lần: **CƯỠNG CHẾ KHÓA PHIÊN NGAY LẬP TỨC (Force Logout)**, hủy toàn bộ giao dịch, kích hoạt sao lưu khẩn cấp (`BackupService.thucHienBackup(BackupType.LOG)`), gửi cảnh báo khẩn cấp tới `ntfy.sh` / Audit Log và đóng ứng dụng về màn hình Đăng nhập.
- **Quy tắc Chuỗi phục hồi CSDL SQL Server (SQL Server Restore Chain - BẮT BUỘC)**:
  - Khi phục hồi một bản sao lưu vi sai (DIFF) hoặc nhật ký giao dịch (LOG), SQL Server bắt buộc cơ sở dữ liệu phải ở chế độ `RESTORING` (được dựng từ bản Full Backup gốc với tùy chọn `NORECOVERY`).
  - Lệnh `RESTORE DATABASE ... WITH REPLACE` **CHỈ DÀNH RIÊNG** cho bản Full Backup đầu tiên. Tuyệt đối KHÔNG có `REPLACE` ở các bản DIFF hoặc LOG tiếp theo (nếu có sẽ ném lỗi `The log or differential backup cannot be restored because no files are ready to rollforward`).
  - Xác định chuỗi phục hồi tự động dựa trên khóa chính tự tăng `backup_set_id` từ `msdb.dbo.backupset` (FULL gốc `WITH REPLACE, NORECOVERY` $\rightarrow$ các bản trung gian `WITH NORECOVERY` $\rightarrow$ bản đích `WITH RECOVERY`).
- **Quy tắc Hệ thống Icon 3D & Cấm Emoji Unicode (BẮT BUỘC)**:
  - Sidebar Navigation và Tiêu đề các trang nội dung phải sử dụng hình ảnh 3D chuyên biệt trong thư mục `img/` (ví dụ `img/nav_sao_luu_phuc_hoi.png`, `nav_kho.png`, `nav_dich_vu.png`... scale 30×30px cho sidebar, 28×28px cho tiêu đề trang) hoặc icon vector 2D FlatLaf vẽ bằng `Graphics2D` trong `MainFrame` (`DatabaseIcon`, `CalendarIcon`, `DotIcon`, `InfoIcon`, `BackupIcon`, `RestoreIcon`, `RefreshIcon`, `AlertTriangleIcon`).
  - **TUYỆT ĐỐI KHÔNG DÙNG emoji Unicode** trực tiếp (`💾`, `🚀`, `⚙️`, `⚠️`, `🔄`, `📋`, ...) làm icon vì sẽ bị lỗi ô vuông `□` hoặc vỡ font trên các máy Windows/macOS không có font emoji tương thích.
- **Quy tắc Căn lề chữ & Khung thẻ Modal Dialog (Anti-misalignment Dialog - BẮT BUỘC)**:
  - Các khối thông báo, ghi chú cảnh báo trong modal dialog BẮT BUỘC dùng `RoundedPanel` với padding trong cố định (ví dụ `new Insets(10, 16, 10, 16)`) kết hợp các `JLabel` riêng lẻ cho từng dòng (thay vì `JTextArea` thô sơ), đảm bảo mọi dòng chữ thẳng tắp cùng một trục lề trái, line-height thoáng đãng, không lệch so le.
  - Các ô nhập liệu (`JTextField`) trong modal dialog BẮT BUỘC có lề padding trong tối thiểu 10-12px (`new EmptyBorder(0, 12, 0, 12)`) kết hợp viền bo tròn (`createLineBorder(..., 1, true)`) để chữ nhập vào không bị dính sát vào mép viền trái, thẳng hàng với lề chữ của các nhãn bên trên và bên dưới.
  - Tiêu đề modal dialog có icon tam giác cảnh báo đỏ + tiêu đề chữ in hoa dùng `FlowLayout(FlowLayout.LEFT, 10, 0)` hoặc `BorderLayout(10, 0)`, không đặt `maximumSize` quá hẹp tránh làm cắt cụt chữ thành dấu ba chấm `...`.
- **Không dùng breadcrumb/path trên trang**: Không thêm breadcrumb, đường dẫn phân cấp, hay chuỗi điều hướng kiểu `Trang A / Trang B` vào bất kỳ màn hình/trang nội dung nào. Side Navigation và tiêu đề trang hiện tại là đủ để định vị.
- **Quy tắc Layout chống lệch (BẮT BUỘC)**:
  - Trong `BoxLayout(Y_AXIS)`, mọi component con trực tiếp PHẢI CÙNG `alignmentX` (khuyến nghị `0.5f` với chiều rộng chuẩn `INNER_WIDTH`). Tuyệt đối không trộn `0.5f` và `0.0f` gây lệch toàn bộ form sang phải.
  - Hàng nút ngang dùng `BoxLayout(X_AXIS)` với `RigidArea(gap, 0)` để tránh bị `FlowLayout` đẩy rớt dòng.
- **Bo góc nút bấm & Khung giao diện mềm mại (Rounded & Soft Look)**:
  - Nút bấm bo góc 10–12px theo Figma phải dùng custom `RoundedButton` (khử răng cưa bằng `Graphics2D`) hoặc token chuẩn FlatLaf. Không dùng `LineBorder` của Swing vì sẽ bị vuông góc.
  - Các card, khung chứa bảng, vùng bộ lọc dùng `RoundedPanel` (arc 12–14px, viền `#E3E9F2`).
  - Các ô input, combobox, info cell dùng `RoundedLineBorder` (arc 10px) kết hợp padding trong để giao diện mềm mại, không vuông vức thô cứng.
  - Trạng thái công việc/yêu cầu hiển thị bằng `StatusBadgeRenderer` dạng pill bo góc mềm (Hoàn tất: xanh lá `#ECFDF5`, Đang sửa: xanh dương `#EFF6FF`, Chờ linh kiện: cam nhạt `#FFFBEB`, Mới: xám `#F1F5F9`).
- **Quy tắc Chống cắt chữ / mất chữ (Anti-clipping - BẮT BUỘC)**:
  - **Sidebar Navigation**: Bề rộng Sidebar tối thiểu **`256px`** (Figma 83:7716). Nút điều hướng kích thước **`236×48px`** (chiều cao 48–52px). Vùng văn bản HTML tối thiểu **`width: 172px`** với `line-height: 1.2` để tiêu đề dài (ví dụ: *"Yêu cầu cập nhật linh kiện"*, *"Cập nhật phiếu sửa chữa"*, *"Lập phiếu tiếp nhận – sửa chữa"*, *"Hồ sơ khách hàng và xe"*) hiển thị trọn vẹn, tuyệt đối không bị ngắt thành *"Yêu cầu cậ..."*, *"Yêu cầu sử..."*, v.v.
  - **Nút bấm (`RoundedButton`)**: Lớp `RoundedButton` BẮT BUỘC override `getPreferredSize()` và `getMinimumSize()` tự động đo đạc độ dài chữ thực tế bằng `FontMetrics.stringWidth(getText()) + iconWidth + 34px padding`. Tuyệt đối không đặt `setPreferredSize` quá hẹp khiến chữ bị cắt cụt thành dấu `...` (ví dụ: nút *"Cập nhật phiếu sửa chữa"* tối thiểu `220px`, *"Quay lại danh sách"* tối thiểu `180px`, *"Xem chi tiết yêu cầu"* tối thiểu `180px`, *"Xem thống kê"* tối thiểu `140px`).
  - **Tab tiêu đề (`JTabbedPane`)**: Để tự động co giãn theo text Tiếng Việt có dấu, không fix cứng kích thước tab.
  - **Ô chọn ngày (`JDateChooser`)**: Tuyệt đối không cho nhập tay từ bàn phím (`editor.setEditable(false)`), bắt buộc người dùng chọn trên popup lịch. Editor bên trong BẮT BUỘC có `EmptyBorder(0, 10, 0, 2)` (lề trái tối thiểu 10px) và bề rộng chooser tối thiểu **`170px`** để góc bo tròn (arc 10px) không bao giờ che khuất chữ số đầu tiên (ví dụ `26/10/2026`).
- **Quy tắc Kiểm soát nhập liệu số lượng (Numeric Input Filter - BẮT BUỘC)**:
  - Mọi ô nhập số lượng (số lượng dịch vụ, số lượng linh kiện sử dụng, số lượng linh kiện yêu cầu) BẮT BUỘC gắn `DocumentFilter` (`chiChoPhepNhapSo`) chặn đứng ký tự chữ ngay khi người dùng gõ phím; không để lọt text rồi mới kiểm tra lúc submit.
- **Quy tắc Chống bóp méo bảng trong BoxLayout (Anti-squeeze Table Rules - BẮT BUỘC)**:
  - Trong `BoxLayout(Y_AXIS)`, `JScrollPane` chứa `JTable` nếu không đặt `minimumSize` sẽ bị Swing tự động bóp nghẹt xuống còn 1 dòng cụt ngủn khi khung chứa bị thu hẹp. BẮT BUỘC đặt đồng thời cả `setPreferredSize` và `setMinimumSize`:
    + Bảng danh sách/kết quả tìm kiếm (yêu cầu thấy 4 dòng): chiều cao tối thiểu **`220px`** (Header 42px + 4 dòng x 42px = 210px + 10px viền đệm).
    + Bảng chi tiết hạng mục (yêu cầu thấy 3 dòng): chiều cao tối thiểu **`180px`** (Header 42px + 3 dòng x 42px = 168px + 12px viền đệm).
    + Muốn xem thêm các dòng vượt quá số dòng chuẩn thì người dùng tự cuộn thanh cuộn của bảng.
  - Các component chứa linh hoạt như `JTabbedPane` bên trong `ScrollableContentPanel` BẮT BUỘC đặt `maximumSize = Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE)` để cho phép co giãn tự do theo nội dung dữ liệu nạp vào, không dùng `addFull` làm khóa cứng chiều cao khởi tạo ban đầu.
- **Quy tắc Nút "Xem chi tiết" & Điều hướng chọn phiếu bất đồng bộ**:
  - Nút "Xem chi tiết" trong bảng dữ liệu (ví dụ Dashboard KTV) không được điều hướng trùng lặp với link "Mở danh sách ↗". Nút này mở modal dialog hiển thị đầy đủ thông tin chi tiết của phiếu, kèm nút hành động phù hợp: phiếu chưa xong có nút *"Cập nhật phiếu này ↗"*, phiếu đã xong có nút *"Xem lịch sử sửa chữa ↗"*.
  - Khi điều hướng sang form cập nhật phiếu (`CapNhatPhieuSuaChuaUI`), BẮT BUỘC hỗ trợ biến hàng đợi `maPhieuChoSua` để tự động chọn đúng phiếu kể cả khi danh sách phiếu đang nạp bất đồng bộ qua `SwingWorker`.
- **Ràng buộc nghiệp vụ phiếu hoàn tất**:
  - Phiếu `HOAN_TAT` là bất biến; combobox chọn phiếu của `CapNhatPhieuSuaChuaUI` BẮT BUỘC lọc bỏ phiếu `HOAN_TAT`, chỉ hiển thị `CHO_SUA`, `DANG_SUA`, `CHO_LINH_KIEN`.
  - KPI "Phiếu cần hoàn thành" = `Tổng phiếu được giao - Phiếu đã hoàn tất`.
  - Biểu đồ thống kê hiệu suất theo ngày phải gom nhóm và đếm từ ngày tiếp nhận thực tế của các phiếu trong database, không dùng số liệu giả lập.
- **Phân quyền Tra cứu 3 vai trò (RBAC) & Màn hình dùng chung**:
  - `TraCuuUI` (Figma `57-246`): Dùng chung 3 vai trò với phân quyền tab:
    + **Chủ cửa hàng (CCH)**: 5 tab toàn quyền (*Dịch vụ*, *Linh kiện*, *Khách hàng*, *Lịch sử sửa chữa*, *Lịch sử tiếp nhận*).
    + **Tiếp nhận viên (TNV)**: 3 tab (*Dịch vụ*, *Khách hàng*, *Lịch sử tiếp nhận*). Bị ẩn tab Linh kiện và Lịch sử sửa chữa.
    + **Kỹ thuật viên (KTV)**: 3 tab (*Dịch vụ*, *Linh kiện*, *Lịch sử sửa chữa*). Bị ẩn tab Khách hàng và Lịch sử tiếp nhận.
  - `ThongTinCaNhanUI` (Figma `57:454`): Dùng chung 3 vai trò, trích xuất dữ liệu từ `UserSession` (Hồ sơ cá nhân, bảng phân quyền vai trò hệ thống, đổi mật khẩu băm SHA-256).
- **Phân hệ chức năng Kỹ thuật viên (KTV)**:
  - Dashboard KTV (Figma `50-589`): 4 thẻ KPI ưu tiên, bảng công việc gần đây, biểu đồ tiến độ & tỷ lệ hoàn tất, khối truy cập nhanh.
  - Thống kê sửa chữa (Figma `172-5247`): 4 thẻ KPI năng suất, 2 thẻ phụ, biểu đồ cột 7 ngày, biểu đồ donut trạng thái, bảng chi tiết phiếu kèm badge và xuất CSV UTF-8 BOM.
  - Phiếu được phân công (Figma `50-701`): Lọc phiếu, toolbar tác vụ, bảng phiếu phân công, khối chi tiết chỉ đọc footer.
  - Cập nhật phiếu sửa chữa (Figma `50-787`): Chọn phiếu, 6 ô chỉ đọc, khối cập nhật dịch vụ, khối cập nhật linh kiện (kho tự động trừ/cộng tồn qua trigger DB), chuyển trạng thái công việc (`CHO_SUA` -> `DANG_SUA` ⇄ `CHO_LINH_KIEN` -> `HOAN_TAT`).
  - Yêu cầu cập nhật linh kiện (Figma `50-922`): Lịch sử yêu cầu kèm 4 chip bộ lọc nhanh, form tạo yêu cầu gọi `sp_TaoYeuCauNhapLinhKien`, chi tiết phản hồi chỉ đọc.
- **Luồng Use Case hoàn chỉnh**:
  - Tự động focus ô nhập đầu tiên khi mở màn hình.
  - Hỗ trợ phím `Enter` (chuyển ô tiếp theo, Enter ô cuối để submit).
  - Tự động xóa khung lỗi khi người dùng gõ phím lại (`DocumentListener`).
  - Khóa form và cập nhật trạng thái "Đang xử lý…" khi gửi dữ liệu, luôn chạy qua `SwingWorker` ở luồng nền (không block EDT).
- Trước khi code bất kỳ màn hình nào, mở Figma, tìm đúng frame/page tương ứng, đọc kỹ rồi mới viết code.
- Nếu không mở được Figma (lỗi trình duyệt, chưa đăng nhập...): **báo ngay cho người dùng**, hỏi để lấy mô tả hoặc ảnh chụp – không được tự suy diễn thiết kế.
- Tên class UI dùng hậu tố `UI` (ví dụ: `DangNhapUI`, `QuanLyKhachHangUI`). Chi tiết xem skill `swing-screen`.
