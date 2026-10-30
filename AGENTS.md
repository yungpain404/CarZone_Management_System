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
├── controller/   # Điều phối nghiệp vụ (AuthController, DangNhapController, DashboardController)
├── dao/          # Lớp DAO JDBC thuần, mỗi bảng/nhóm 1 class (TaiKhoanDao, NhanVienDao, HoaDonDao, ChiTietHoaDonDao, ThongKeDoanhThuDao, ...)
├── entity/       # Thực thể POJO (validate trong setter, raw setters cho DAO map DB)
├── enums/        # Các enum trạng thái và vai trò
├── session/      # Quản lý phiên làm việc người dùng (UserSession Singleton)
└── ui/           # Giao diện Swing FlatLaf bám sát Figma (MainFrame, DashboardUI, DangNhapUI, KhoiDongUI)
img/              # Ảnh dùng chung (carzone_logo.png, revenue_illustration.png); pom copy vào gốc classpath
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

## Lệnh
- Khởi động DB + migrate: `docker compose -f infra/docker/compose.yml up -d`
- Dựng lại DB sạch: `docker compose -f infra/docker/compose.yml down -v` rồi `up -d`
- Biên dịch: `mvn -q compile` (Maven tải được thư viện trên máy nhóm).
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
- **Khung giao diện chính (Master Frame)**: Sử dụng [MainFrame.java](file:///Users/apple/Documents/2026/Project/CarZone_Management_System/src/ui/MainFrame.java) làm layout tổng thể điều hướng (Header + Side Navigation theo vai trò + ContentPanel `CardLayout`).
- **Không dùng breadcrumb/path trên trang**: Không thêm breadcrumb, đường dẫn phân cấp, hay chuỗi điều hướng kiểu `Trang A / Trang B` vào bất kỳ màn hình/trang nội dung nào. Side Navigation và tiêu đề trang hiện tại là đủ để định vị.
- **Quy tắc Layout chống lệch (BẮT BUỘC)**:
  - Trong `BoxLayout(Y_AXIS)`, mọi component con trực tiếp PHẢI CÙNG `alignmentX` (khuyến nghị `0.5f` với chiều rộng chuẩn `INNER_WIDTH`). Tuyệt đối không trộn `0.5f` và `0.0f` gây lệch toàn bộ form sang phải.
  - Hàng nút ngang dùng `BoxLayout(X_AXIS)` với `RigidArea(gap, 0)` để tránh bị `FlowLayout` đẩy rớt dòng.
- **Bo góc nút bấm (Rounded Buttons)**: Nút bấm bo góc 12px theo Figma phải dùng custom `RoundedButton` (khử răng cưa bằng `Graphics2D`) hoặc token chuẩn FlatLaf. Không dùng `LineBorder` của Swing vì sẽ bị vuông góc. Tính toán padding đủ rộng để chữ không bị tràn/cắt cụt (ví dụ Help, About).
- **Luồng Use Case hoàn chỉnh**:
  - Tự động focus ô nhập đầu tiên khi mở màn hình.
  - Hỗ trợ phím `Enter` (chuyển ô tiếp theo, Enter ô cuối để submit).
  - Tự động xóa khung lỗi khi người dùng gõ phím lại (`DocumentListener`).
  - Khóa form và cập nhật trạng thái "Đang xử lý…" khi gửi dữ liệu, luôn chạy qua `SwingWorker` ở luồng nền (không block EDT).
- Trước khi code bất kỳ màn hình nào, mở Figma, tìm đúng frame/page tương ứng, đọc kỹ rồi mới viết code.
- Nếu không mở được Figma (lỗi trình duyệt, chưa đăng nhập...): **báo ngay cho người dùng**, hỏi để lấy mô tả hoặc ảnh chụp – không được tự suy diễn thiết kế.
- Tên class UI dùng hậu tố `UI` (ví dụ: `DangNhapUI`, `QuanLyKhachHangUI`). Chi tiết xem skill `swing-screen`.
