# PROJECT_CONTEXT — CarZone

Cập nhật lần cuối: 30/10/2026. Tài liệu này là bản đồ ngữ cảnh đã kiểm chứng từ code, migration và quyết định của nhóm. Khi có mâu thuẫn, xem mục "Thứ tự ưu tiên nguồn sự thật".

## 1. Bài toán

CarZone là phần mềm desktop nội bộ cho gara ô tô, ba vai trò: Chủ cửa hàng (`CHU_CUA_HANG`), Tiếp nhận viên (`TIEP_NHAN_VIEN`), Kỹ thuật viên (`KY_THUAT_VIEN`). Phạm vi nghiệp vụ: khách hàng và xe, tiếp nhận/sửa chữa, linh kiện và tồn kho, yêu cầu nhập linh kiện, khuyến mại, hóa đơn/thanh toán, đổi trả/bảo hành, tra cứu và thống kê.

## 2. Thứ tự ưu tiên nguồn sự thật

1. Database đang chạy: `V1__init_CarZone_Database.sql`, `V2__update_KhachHang_Entity.sql`, trigger, stored procedure, view.
2. Quyết định đã chốt: `ai-context/decisions.md`, `ai-context/business-rules.md`, `AGENTS.md`.
3. Code đang chạy trong `src/`.
4. Tài liệu Word `References/Nhom01_v3_ ApplicationDevelopment.docx` (đặc tả UC, màn hình, thiết kế).
5. `ai-context/PROGRESS.md`.

`PROGRESS.md` đang chứa thông tin cũ về Hibernate/JPA. Không dùng nó để suy ra kiến trúc hiện tại.

Nếu mục 1–3 xung đột với mục 4–5: dừng, ghi rõ chỗ lệch, hỏi người dùng. Không tự chọn.

## 3. Công nghệ thật

| Thành phần | Giá trị |
|---|---|
| Ngôn ngữ | Java 17 (`maven.compiler.release=17`) |
| UI | Java Swing, FlatLaf 3.7, giao diện sáng `FlatLightLaf` |
| Database | SQL Server 2022, collation `Vietnamese_CI_AS` |
| Truy cập dữ liệu | JDBC thuần (`DriverManager`), mssql-jdbc 12.6.4 |
| Kết nối | `connectDB.DbUtil.getConnection()`; cấu hình từ `infra/docker/.env` qua `connectDB.DatabaseConfig` |
| Test | JUnit 5 (dependency có sẵn; chưa có bộ test và chưa cấu hình surefire JUnit 5) |
| Build | Maven; `sourceDirectory=src`; `main.class=app.Main`; shade plugin đóng jar |
| Hạ tầng | Docker Compose chạy SQL Server; Flyway chạy trong container, không chạy từ Java |
| ORM | Không có. Đã bỏ hoàn toàn Hibernate/JPA, không có `persistence.xml`, entity là POJO thuần |

## 4. Cấu trúc thư mục thật

```text
CarZone/
├── AGENTS.md
├── pom.xml
├── ai-context/
│   ├── PROGRESS.md
│   ├── decisions.md
│   ├── business-rules.md
│   └── skills/                  # skill cũ + bộ carzone/
├── docs/                        # báo cáo Word, không dành riêng cho AI
├── infra/
│   ├── docker/compose.yml
│   ├── docker/.env
│   └── database/
│       ├── fly_way_migrations/V1__init_CarZone_Database.sql
│       ├── fly_way_migrations/V2__update_KhachHang_Entity.sql
│       └── seed/CarZone_Demo.sql
└── src/
    ├── app/          # Main.java, Test.java
    ├── connectDB/    # DatabaseConfig, DbUtil
    ├── controller/   # AuthController, DangNhapController, DashboardController
    ├── dao/          # interface DAO
    ├── dao/impl/     # JDBC implementation
    ├── entity/       # POJO
    ├── enums/        # trạng thái, vai trò
    ├── session/      # UserSession Singleton
    └── ui/           # DangNhapUI, DashboardUI, KhoiDongUI, MainFrame
```

## 5. Kiến trúc mục tiêu

Luồng phụ thuộc một chiều:

```text
ui -> controller -> dao (interface) -> dao.impl -> entity/enums
                                  \-> connectDB.DbUtil
session <- controller/ui (chỉ đọc thông tin phiên)
```

- `ui`: dựng giao diện, bắt sự kiện, gọi controller. Không SQL, không nghiệp vụ, không mock dữ liệu. Việc nặng chạy qua `SwingWorker`.
- `controller`: kiểm tra quyền từ `UserSession`, validate sớm, điều phối DAO, dịch lỗi DB sang thông báo tiếng Việt, trả DTO/kết quả cho UI.
- `dao`: interface nghiệp vụ, không SQL.
- `dao.impl`: JDBC, `PreparedStatement`, `try-with-resources`, mapping ResultSet, gọi procedure.
- `entity`: POJO, validate trong setter thường, có `setXxxRaw` cho DAO.
- `enums`: `VaiTro`, `TrangThaiPhieu`, `TrangThaiThanhToan`, `TrangThaiYeuCau`, `PhuongThucThanhToan`, `HuongXuLyDoiTra`.
- `session.UserSession`: Singleton giữ `TaiKhoan`, `NhanVien`, thời điểm đăng nhập.

Không có `service` layer. Không tạo lại `service` nếu chưa được duyệt. Controller hiện tại có thể đang chứa SQL (`DashboardController`); đó là nợ kỹ thuật cần chuyển về DAO, không phải chuẩn để noi theo.

## 6. Quy ước code

- Giữ tên lớp/thuộc tính tiếng Việt không dấu theo tài liệu: `HoaDon`, `maHoaDon`, `PhieuTiepNhanSuaChua`.
- Entity phần lớn dùng `java.util.Date` và `double` theo quyết định nhóm; không tự đổi sang `LocalDateTime`/`BigDecimal`. Ngoại lệ đã có trong code: `NhanVien.ngaySinh` và `NhanVien.ngayVaoLam` dùng `java.time.LocalDate`; giữ nguyên, không đổi hàng loạt khi chưa được duyệt (xem D18).
- DAO đọc dữ liệu DB đã hợp lệ thì dùng raw setter; setter thường dùng cho dữ liệu người dùng nhập.
- Tiền/thuế/khuyến mại lấy từ view `vw_HoaDon_TongTien`; không tính lại ở Java.
- Mã do DB sinh khi đã có procedure/sequence; Java không tự sinh. Ngoại lệ: `DichVu.maDV`, `LinhKien.maLK` chưa có cơ chế sinh mã (xem D19).
- Tồn kho do trigger `trg_CTLK_KiemTra` và `trg_PYCNLK_KiemTra` cập nhật; Java không tự trừ/cộng.
- Lỗi nghiệp vụ DB dùng mã 50001–50261; controller/DAO phải giữ nguyên nguyên nhân và dịch thông báo.
- Không hard-code mật khẩu hay chuỗi kết nối. `DbUtil` hiện hard-code username `sa`; đây là nợ bảo mật, không nhân rộng.
- Không commit `.env` mới hoặc nội dung bí mật; quyết định hiện tại là `.env` có trong git cho dữ liệu local, nhưng không thêm thông tin thật.
- Không sửa migration cũ; thay đổi schema bằng migration mới.
- Không tạo bản sao skill cho từng công cụ AI; tất cả nằm trong `ai-context/`.

## 7. Entity hiện có

| Entity | Khóa/Trường chính |
|---|---|
| `KhachHang` | `maKH`, `hoTen`, `soDienThoai`, `ghiChu`, `CCCD`, `thanhVienThanThiet`, `diemTichLuy` |
| `Xe` | `bienSoXe`, `hangXe`, `model`, `mauSac`, `namSanXuat`, `soKmHienTai`, `chuXe` |
| `NhanVien` | `maNV`, `taiKhoan`, `hoTen`, `ngaySinh`, `soDienThoai`, `CCCD`, `diaChi`, `ngayVaoLam`, `vaiTro` |
| `TaiKhoan` | `maNV`, `nhanVien`, `tenDangNhap`, `matKhau`, `trangThaiHoatDong` |
| `DichVu` | `maDV`, `tenDV`, `donGia`, `donViTinh`, `moTa`, `dangCungCap` |
| `LinhKien` | `maLK`, `tenLK`, `donGia`, `donViTinh`, `soLuongTon`, `dangKinhDoanh` |
| `KhuyenMai` | `maKM`, `nhanVien`, `tenChuongTrinh`, `mucGiamPhanTram`, `ngayBatDau`, `ngayKetThuc`, `linhKienApDung`, `dichVuApDung` |
| `PhieuTiepNhanSuaChua` | `maPhieu`, `xe`, `nhanVienLap`, `nhanVienSuaChua`, `ngayGioTiepNhan`, `yeuCauBanDau`, `trangThai` |
| `ChiTietDichVu` | `id(maPhieu, maDV)`, `phieuSuaChua`, `dichVu`, `soLuong`, `donGiaThucHien`, `ghiChu` |
| `ChiTietLinhKien` | `id(maPhieu, maLK)`, `phieuSuaChua`, `linhKien`, `soLuong`, `donGiaXuat`, `ghiChu` |
| `HoaDon` | `maHoaDon`, `phieuSuaChua`, `nhanVien`, `khuyenMai`, `ngayLap`, `phuongThucTT`, `trangThaiThanhToan`, `thoiGianThanhToan`, `maGiaoDich`, các trường tiền suy diễn |
| `PhieuDoiTra` | `maPhieuDT`, `hoaDon`, `nhanVien`, `ngayLap`, `lyDoDoiTra`, `soTienHoanTra` |
| `ChiTietDoiTra` | `id(maPhieuDT, stt)`, `phieuDT`, `maPhieu`, `maDV`, `maLK`, `soLuong`, `donGia`, `huongXuLy` |
| `PhieuYeuCauNhapLinhKien` | `maYeuCau`, `linhKien`, `nhanVienLap`, `nhanVienXuLy`, `ngayTao`, `ngayXuLy`, `soLuongYeuCau`, `trangThai`, `ghiChu`, `ghiChuXuLy` |

## 8. Enum và trạng thái

- `VaiTro`: `CHU_CUA_HANG`, `TIEP_NHAN_VIEN`, `KY_THUAT_VIEN`.
- `TrangThaiPhieu`: `CHO_SUA`, `DANG_SUA`, `CHO_LINH_KIEN`, `HOAN_TAT`.
- `TrangThaiThanhToan`: `CHUA_THANH_TOAN`, `DA_THANH_TOAN`.
- `TrangThaiYeuCau`: `CHO_DUYET`, `DA_DUYET`, `TU_CHOI`, `DA_NHAP_KHO`.
- `PhuongThucThanhToan`: `TIEN_MAT`, `CHUYEN_KHOAN`.
- `HuongXuLyDoiTra`: `DOI_MOI`, `HOAN_TIEN`, `SUA_LAI_MIEN_PHI`.

Máy trạng thái phiếu sửa chữa theo DB:

```text
CHO_SUA -> DANG_SUA | CHO_LINH_KIEN
DANG_SUA -> CHO_LINH_KIEN | HOAN_TAT
CHO_LINH_KIEN -> DANG_SUA
HOAN_TAT -> (không chuyển tiếp)
```

Máy trạng thái yêu cầu nhập linh kiện:

```text
CHO_DUYET -> DA_DUYET | TU_CHOI
DA_DUYET -> DA_NHAP_KHO
```

## 9. Ràng buộc và mã định danh

| Đối tượng | Định dạng | Ghi chú |
|---|---|---|
| Khách hàng | `KH` + 6 số | `seq_KhachHang` |
| Nhân viên | `CCH`/`TNV`/`KTV` + 3 số | `sp_ThemNhanVien`, tối đa 999 mỗi vai trò |
| Tài khoản | trùng tiền tố vai trò + 3 số | `sp_TaoTaiKhoan`; trigger `trg_TaiKhoan_KiemTra` |
| Dịch vụ / linh kiện | `DV`/`LK` + ít nhất 5 số | `DV00001`, `LK00001`; chưa có sequence/default sinh mã (D19) |
| Khuyến mại | `KM` + 3 số | `seq_KhuyenMai` |
| Phiếu sửa chữa | `PSC` + ddMMyyyy + `A/P` + 3 số | `sp_LapPhieuTiepNhan` |
| Hóa đơn | `HD` + ddMMyyyy + `A/P` + 3 số | `sp_LapHoaDon` |
| Phiếu đổi trả | `PDT` + ddMMyyyy + `A/P` + 2 số | `sp_LapPhieuDoiTra` |
| Yêu cầu nhập | `PYC` + ddMMyyyy + `A/P` + 2 số | `sp_TaoYeuCauNhapLinhKien` |

Ràng buộc dữ liệu đáng nhớ:

- Khách hàng: SĐT 10–11 số, bắt đầu `0`, duy nhất; CCCD 12 số, duy nhất nếu có.
- Nhân viên: SĐT và CCCD duy nhất; đủ 18 tuổi tại ngày vào làm; chỉ một `CHU_CUA_HANG`.
- Xe: biển số 6–15 ký tự; năm sản xuất 1900–2100; số km không âm.
- Dịch vụ/linh kiện: đơn giá > 0; tên duy nhất; linh kiện tồn >= 0.
- Khuyến mại: mức giảm (0, 100]; ngày bắt đầu <= ngày kết thúc; chỉ Chủ cửa hàng tạo.
- Hóa đơn: mỗi phiếu sửa chữa tối đa một hóa đơn; chỉ Tiếp nhận viên lập; chỉ phiếu `HOAN_TAT`; khuyến mại phải còn hiệu lực tại ngày lập.
- Đổi trả: hóa đơn `DA_THANH_TOAN`; trong 7 ngày kể từ ngày lập; chỉ Tiếp nhận viên lập phiếu; chi tiết phải thuộc phiếu gốc; số lượng đổi trả không vượt số lượng đã thực hiện/dùng.
- Thanh toán: chuyển khoản bắt buộc `maGiaoDich`; hóa đơn đã thanh toán không sửa/xóa.
- Tài khoản: tên đăng nhập 6 ký tự, khớp vai trò; mật khẩu tối thiểu 8 ký tự có chữ hoa, chữ thường, số, ký tự đặc biệt; lưu SHA-256 hex in hoa.

## 10. Công thức tiền

Nguồn: `vw_HoaDon_TongTien`, không tính lại ở Java.

```text
tongTienDichVu   = SUM(ChiTietDichVu.thanhTien)
tongTienLinhKien = SUM(ChiTietLinhKien.thanhTien)
tienGiam         = mucGiamPhanTram% * các dòng được áp dụng
thue             = 1,5% nếu chỉ có linh kiện
                 = 7%   nếu chỉ có dịch vụ
                 = 4,5% nếu có cả hai
tongThanhToan    = tongTienDichVu + tongTienLinhKien - tienGiam + thue
```

Thuế tính trên số tiền trước giảm giá. Khuyến mại không liệt kê mặt hàng nào = giảm toàn hóa đơn; có liệt kê = chỉ giảm các mặt hàng trong danh sách.

Công thức `(tongTienDichVu + tongTienLinhKien) * (1 - khuyenMai) + thue` trong báo cáo Word là công thức cũ, đã bị `decisions.md` và view sửa. Không dùng lại.

## 11. Đối tượng database quan trọng

Bảng: `KhachHang`, `Xe`, `NhanVien`, `TaiKhoan`, `DichVu`, `LinhKien`, `KhuyenMai`, `KhuyenMai_DichVu`, `KhuyenMai_LinhKien`, `PhieuTiepNhanSuaChua`, `ChiTietDichVu`, `ChiTietLinhKien`, `HoaDon`, `PhieuDoiTra`, `ChiTietDoiTra`, `PhieuYeuCauNhapLinhKien`.

Procedure:

- `sp_LapHoaDon`, `sp_ThanhToanHoaDon`
- `sp_ThemNhanVien`, `sp_TaoTaiKhoan`
- `sp_LapPhieuTiepNhan`, `sp_LapPhieuDoiTra`
- `sp_TaoYeuCauNhapLinhKien`

View:

- `vw_PhieuSuaChua_TongTien`
- `vw_HoaDon_TongTien`
- `vw_PhieuDoiTra_HoanTien`
- `vw_DoanhThuTheoNgay`
- `vw_LichSuSuaChua`

Trigger chính:

- `trg_TaiKhoan_KiemTra`, `trg_PTNSC_KiemTra`
- `trg_CTDV_KiemTra`, `trg_CTLK_KiemTra`
- `trg_HoaDon_KiemTra`, `trg_HoaDon_KhoaSauThanhToan`
- `trg_PhieuDoiTra_KiemTra`, `trg_CTDT_KiemTra`
- `trg_KhuyenMai_KiemTra`, `trg_KMDV_Khoa`, `trg_KMLK_Khoa`
- `trg_PYCNLK_KiemTra`

## 12. Hiện trạng triển khai

Đã có:

- Kết nối DB, session, đăng nhập, dashboard, khung giao diện chính.
- Entity/enum cho toàn bộ mô hình.
- DAO cho `TaiKhoan`, `NhanVien`, `HoaDon`, `ChiTietHoaDon`, `PasswordReset`.
- Migration V1/V2 và seed demo.

Chưa có:

- DAO cho khách hàng, xe, dịch vụ, linh kiện, khuyến mại, phiếu sửa chữa, chi tiết phiếu, đổi trả, yêu cầu nhập.
- Controller và màn hình nghiệp vụ cho hầu hết UC002–UC016.
- Bộ test tự động.

Nợ kỹ thuật đã biết (xem chi tiết trong `KNOWN_DISCREPANCIES.md`):

- `DashboardController` dùng sai tên cột (`maXe`, `maNVSuaChua`) và chứa SQL trực tiếp.
- Tồn tại hai khung giao diện gần trùng nhau: `MainFrame` và `DashboardUI`.
- `MainFrame`/`DashboardUI` có thể tạo nhân viên mặc định khi không có session.
- UI dashboard có truy vấn SQL trực tiếp.
- `DbUtil` hard-code username `sa`.
- Mật khẩu SHA-256 không salt; giữ theo quyết định nhóm, không tự đổi.

## 13. Lệnh

```powershell
# DB + Flyway
docker compose -f infra/docker/compose.yml up -d

# Dừng và xóa dữ liệu local
docker compose -f infra/docker/compose.yml down -v

# Biên dịch
mvn -q compile

# Chạy ứng dụng
mvn exec:java

# Đóng gói
mvn -q package
```

Chưa có lệnh test chuẩn. Nếu thêm test JUnit 5, cần bổ sung `maven-surefire-plugin` 3.x. Vì `sourceDirectory=src`, không nên đặt test dưới `src/test/java` nếu chưa exclude `test/**` khỏi main compile; khuyến nghị `<testSourceDirectory>test/java</testSourceDirectory>` rồi dùng `mvn test`. Không tuyên bố "đã test" nếu chưa thực sự chạy.
