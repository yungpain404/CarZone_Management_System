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

## Từ nhóm (25/10/2026)
- Mã hóa đơn có tiền tố `HD`: `HDddMMyyyyAXXX`.
- Loại thuế xác định theo nội dung hóa đơn (chỉ linh kiện / chỉ dịch vụ / cả hai), không thêm thuộc tính loại vào `HoaDon`.
- Khuyến mãi: không liệt kê mặt hàng = giảm tất cả; có liệt kê = chỉ các mặt hàng trong danh sách được giảm.
- Mã dịch vụ/linh kiện dùng `DV00001`/`LK00001` (≥ 5 số).
- **Luật nghiệp vụ kết hợp hai tầng:** vừa trigger/stored procedure ở DB, vừa validate ở Java.
- **ORM: Hibernate.** Sửa lại entity cho phù hợp (annotation, quan hệ, bỏ setter trường dẫn xuất).
- **Entity phải khớp DB:** thêm tham chiếu phiếu sửa chữa vào `HoaDon`, bỏ setter của trường dẫn xuất, các điểm lệch khác ở `business-rules.md` mục 6.
- **`.env` vẫn được đẩy lên git** để bạn bè pull về dùng chung; dữ liệu lưu local nên không ảnh hưởng kết quả của nhóm. Không coi đây là lỗi lộ bí mật, nhưng không đặt thông tin thật nào khác vào `.env`.

## Phân vai hai tầng (đã được nhóm xác nhận)
Mục đích là tránh hai bên làm trùng hoặc mâu thuẫn:
- **DB là nguồn cuối cùng** cho: sinh mã theo ngày/ca/vai trò (stored procedure, an toàn khi nhiều người cùng lập), trừ/cộng tồn kho (trigger đã có), khóa phiếu `HOAN_TAT` và hóa đơn đã thanh toán, các CHECK/UNIQUE/FK.
- **Java** validate sớm để báo lỗi thân thiện (setter entity, service), kiểm quyền theo vai trò, dịch mã lỗi DB (50xxx) thành thông báo tiếng Việt.
- **Java không được tự trừ/cộng tồn kho** (trigger đã làm, làm thêm sẽ trừ hai lần) và không tự sinh mã đã giao cho stored procedure.
- Thêm/đổi luật: sửa `business-rules.md` trước, rồi cập nhật cả hai tầng.

## Đã chốt khi refactor entity sang Hibernate (26/10/2026)
- Hibernate ORM **7.4.13.Final** (do AI đề xuất; nhóm chưa phản đối nhưng chưa build được thử, xem câu hỏi mở 1).
- FlatLaf **3.7** (AI chọn theo yêu cầu "bản phù hợp nhất"; chưa kiểm tra trên Maven Central).
- Giữ `java.util.Date` và `double`.
- Cấu hình bằng `src/META-INF/persistence.xml` (persistence unit `CarZonePU`); pom có `<resources>` để đưa file này vào classpath. URL/user/password KHÔNG ghi trong file, truyền qua map khi tạo `EntityManagerFactory`.
- `hbm2ddl.auto=none` (Flyway quản lý schema; `validate` sẽ lỗi với `CHAR(14)` và cột tính toán).
- Số tiền tổng của `HoaDon` và `soTienHoanTra` của `PhieuDoiTra` là thuộc tính suy diễn, chỉ getter, đọc từ view bằng `@Formula`. Thành tiền từng dòng (`ChiTietDichVu/LinhKien/DoiTra`) tính bằng Java.
- Khóa ghép dùng `@EmbeddedId` + `@MapsId`; `TaiKhoan` dùng chung khóa với `NhanVien` (`@OneToOne @MapsId`); `ChiTietDoiTra` giữ `maPhieu/maDV/maLK` dạng chuỗi (trigger DB kiểm tra).
- `PhieuTiepNhanSuaChua` có thêm `Xe`; `NhanVien` thêm `soDienThoai`, `CCCD`, quy tắc đủ 18 tuổi; `PhieuYeuCauNhapLinhKien` thêm `ngayXuLy`, `ghiChuXuLy`.

## Đã chốt thêm (26/10/2026, nhóm trả lời)
- Maven tải được Hibernate 7.4.13.Final và FlatLaf 3.7 (nhóm xác nhận có tải được).
- **Java 17.** Mỗi người đổi JRE của project trong IDE về 17 (`.classpath` đang ghi 21).
- **Mật khẩu:** băm SHA-256, hex in hoa, trong `TaiKhoan.setMatKhau` (khớp dữ liệu mẫu `HASHBYTES('SHA2_256', ...)` trong DB, không cần thêm thư viện). Không salt: đủ cho đồ án; muốn mạnh hơn (BCrypt) thì phải đổi cả dữ liệu mẫu.
- **`connectDB` đọc `infra/docker/.env`** (`DatabaseConfig`), biến môi trường hệ điều hành được ưu tiên hơn; `JpaUtil` giữ `EntityManagerFactory` dùng chung. Chạy ứng dụng từ thư mục gốc dự án.
- **Mã do DB sinh cho `KhachHang`/`KhuyenMai` (sequence + DEFAULT):** làm giống các bảng khác, tức DB sinh mã, Java nhận mã trả về rồi nạp entity; không `persist()` với mã tự đặt. Hai bảng này không có stored procedure nên dao dùng một lệnh native `DECLARE @t TABLE(ma VARCHAR(10)); INSERT ... OUTPUT inserted.<cột mã> INTO @t ... ; SELECT ma FROM @t;` (dạng `INTO` vì `KhuyenMai` có trigger, `OUTPUT` không `INTO` sẽ lỗi). Sau đó `find` entity theo mã. Không cần migration mới.
- **Chưa viết test** ở giai đoạn này; giữ nguyên `testSourceDirectory` = `src` trong pom, đừng thêm test.
- **Nhánh:** làm trên `develop`, chủ repo tự merge; agent không merge, không sao chép V2 từ nhánh khác nếu chưa được hỏi.
- **`Xe`:** `model`, `mauSac`, `namSanXuat` bắt buộc ở Java (DB cho phép NULL; Java chặt hơn có chủ đích).
- **Thư mục AI:** ngữ cảnh AI nằm ở `ai-context/` (không dùng `docs/` vì trùng thư mục tài liệu của nhóm); hướng dẫn chung ở `AGENTS.md`, `CLAUDE.md`/`GEMINI.md`/`.github/copilot-instructions.md` chỉ trỏ về đó.
- **Đặt tên class UI (đã chốt 26/10/2026):** Màn hình Java Swing dùng hậu tố `UI`, không dùng `Frame` hay `Panel`. Ví dụ: `DangNhapUI`, `QuanLyKhachHangUI`, `LapPhieuTiepNhanUI`. File tương ứng: `DangNhapUI.java`. Entry point vẫn là `ui.Main`.

## Câu hỏi mở (agent phải hỏi, không tự chọn)
1. Các `@Formula` (view), `@MapsId` và khóa ghép mới chỉ được kiểm bằng stub; chưa chạy với Hibernate + DB Docker thật. Ai chạy trước thì báo kết quả, lỗi nào sửa vào entity tương ứng.
2. `ChiTietLinhKien.setSoLuong` so với tồn kho khi sửa dòng đã lưu: chỉ kiểm sơ bộ, trigger DB là chốt. Giữ vậy hay siết thêm?
3. Báo cáo Word và comment đầu V1 (tên file demo cũ) chưa cập nhật.
