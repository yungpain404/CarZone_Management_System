# USECASE_SKILL_TRACEABILITY — UC001–UC016

Bảng nối use case với quyền, database, code, màn hình, skill và điểm kiểm thử. Dùng khi lập kế hoạch hoặc rà soát độ phủ.

Cột "Code hiện có" phản ánh repo tại 29/10/2026. "Màn hình mục tiêu" là tên lớp đề xuất theo quy ước `UI`; phải đối chiếu Figma trước khi tạo.

| UC | Actor | Kết quả nghiệp vụ | DB liên quan | Code hiện có | Màn hình mục tiêu | Skill chính | Trọng tâm kiểm thử |
|---|---|---|---|---|---|---|---|
| UC001 Xem báo cáo thống kê | CCH | Doanh thu, hoàn tiền, doanh thu thuần theo ngày/khoảng | `HoaDon`, `PhieuDoiTra`, `vw_HoaDon_TongTien`, `vw_DoanhThuTheoNgay` | `DashboardController` (một phần, có SQL trực tiếp) | `ThongKeDoanhThuUI` | `search-statistics`, `invoice-payment-promotion`, `role-navigation-dashboard` | Doanh thu chỉ HĐ đã thanh toán; hoàn tiền trừ đúng ngày phát sinh; quyền CCH |
| UC002 Tạo tài khoản | CCH | Tạo tài khoản đăng nhập cho nhân viên | `TaiKhoan`, `NhanVien`, `sp_TaoTaiKhoan`, `trg_TaiKhoan_KiemTra` | `TaiKhoanDao`, `AuthController` (đăng nhập); chưa có luồng tạo | `QuanLyTaiKhoanUI` | `crud-master-data`, `authentication-session`, `role-authorization` | Tiền tố khớp vai trò; mật khẩu mạnh; không tạo trùng; chỉ CCH |
| UC003 Thêm chương trình khuyến mại | CCH | Tạo KM và danh sách áp dụng | `KhuyenMai`, `KhuyenMai_DichVu`, `KhuyenMai_LinhKien`, `trg_KhuyenMai_KiemTra` | Chưa có DAO/UI | `QuanLyKhuyenMaiUI` | `invoice-payment-promotion`, `crud-master-data` | Mức giảm (0,100]; ngày hợp lệ; danh sách rỗng = tất cả; khóa sau khi dùng |
| UC004 Thêm hồ sơ nhân viên | CCH | Tạo nhân viên, sinh mã theo vai trò | `NhanVien`, `sp_ThemNhanVien`, `CK_NhanVien_*` | `NhanVienDao` | `QuanLyNhanVienUI` | `crud-master-data`, `authentication-session` | Tuổi >=18; SĐT/CCCD duy nhất; mã đúng tiền tố; chỉ một CCH |
| UC005 Thêm dịch vụ | CCH | Thêm/sửa/ngừng cung cấp dịch vụ | `DichVu`, `ChiTietDichVu` | Chưa có DAO/UI | `QuanLyDichVuUI` | `crud-master-data` | Đơn giá >0; tên duy nhất; dịch vụ ngừng cung cấp không thêm vào phiếu |
| UC006 Thêm linh kiện | CCH | Thêm/sửa/ngừng kinh doanh linh kiện | `LinhKien`, `ChiTietLinhKien` | Chưa có DAO/UI | `QuanLyLinhKienUI` | `crud-master-data`, `inventory-replenishment` | Đơn giá >0; tồn >=0; tên duy nhất; không sửa tồn trực tiếp |
| UC007 Tra cứu thông tin | Cả ba | Tra cứu khách/xe/phiếu/lịch sử | `vw_LichSuSuaChua`, `KhachHang`, `Xe`, `PhieuTiepNhanSuaChua` | Chưa có | `TraCuuUI` | `search-statistics`, `role-authorization` | Tìm theo biển số/SĐT; KTV chỉ thấy dữ liệu của mình; không lộ dữ liệu người khác |
| UC008 Xem thống kê sửa chữa | CCH, TNV, KTV | Thống kê phiếu/dịch vụ/linh kiện | `PhieuTiepNhanSuaChua`, `ChiTietDichVu`, `ChiTietLinhKien` | Chưa có | `ThongKeSuaChuaUI` | `search-statistics`, `role-authorization` | TNV lọc `maNVTiepNhan`; KTV lọc `maNVKyThuat`; CCH toàn bộ |
| UC009 Yêu cầu nhập linh kiện | KTV tạo; CCH duyệt | Yêu cầu và nhập kho linh kiện | `PhieuYeuCauNhapLinhKien`, `LinhKien`, `trg_PYCNLK_KiemTra`, `sp_TaoYeuCauNhapLinhKien` | Chưa có | `YeuCauNhapLinhKienUI` | `inventory-replenishment` | Chỉ KTV tạo; chỉ CCH duyệt/từ chối/nhập; tồn chỉ cộng khi `DA_NHAP_KHO` một lần |
| UC010 Lập phiếu tiếp nhận - sửa chữa | TNV | Tạo phiếu, gán KTV chính, ghi yêu cầu | `PhieuTiepNhanSuaChua`, `Xe`, `NhanVien`, `sp_LapPhieuTiepNhan`, `trg_PTNSC_KiemTra` | Chưa có | `LapPhieuTiepNhanUI` | `repair-order-workflow`, `crud-master-data` | Xe tồn tại; KTV đúng vai trò; khác người tiếp nhận; mã sinh tự động; trạng thái đầu `CHO_SUA` |
| UC011 Cập nhật phiếu tiếp nhận - sửa chữa | TNV, KTV | Thêm DV/LK, đổi trạng thái, hoàn tất | `PhieuTiepNhanSuaChua`, `ChiTietDichVu`, `ChiTietLinhKien`, trigger 50101–50123 | Chưa có | `CapNhatPhieuSuaChuaUI` | `repair-order-workflow`, `database-transaction-integrity` | Chỉ KTV chính sửa chi tiết; luồng trạng thái hợp lệ; phiếu `HOAN_TAT` bất biến; tồn kho tự trừ |
| UC012 Tạo phiếu đổi trả | TNV (DB); KTV xử lý `?` | Đổi/trả hàng, hoàn tiền | `PhieuDoiTra`, `ChiTietDoiTra`, `trg_PhieuDoiTra_KiemTra`, `trg_CTDT_KiemTra`, `vw_PhieuDoiTra_HoanTien` | Chưa có | `DoiTraUI` | `returns-warranty` | HĐ đã thanh toán; trong 7 ngày; SL không vượt; đúng phiếu gốc; hoàn tiền trừ doanh thu ngày trả |
| UC013 Xem thống kê tiếp nhận | CCH, TNV, KTV | Thống kê phiếu tiếp nhận/đổi trả | `PhieuTiepNhanSuaChua`, `PhieuDoiTra`, `vw_DoanhThuTheoNgay` | Chưa có | `ThongKeTiepNhanUI` | `search-statistics`, `role-authorization` | TNV/KTV chỉ số của mình; CCH toàn bộ; khoảng ngày biên |
| UC014 Áp dụng mã giảm giá | TNV | Gắn KM hợp lệ vào hóa đơn | `KhuyenMai`, `KhuyenMai_DichVu`, `KhuyenMai_LinhKien`, `vw_HoaDon_TongTien` | Chưa có | `LapHoaDonUI` | `invoice-payment-promotion` | KM còn hiệu lực tại ngày lập; tối đa 1; đúng danh sách; không sửa KM đã dùng |
| UC015 Thanh toán hóa đơn | TNV | Lập HĐ, thu tiền, khóa HĐ | `HoaDon`, `sp_LapHoaDon`, `sp_ThanhToanHoaDon`, `trg_HoaDon_*` | `HoaDonDao`, `ChiTietHoaDonDao` (đọc) | `HoaDonThanhToanUI` | `invoice-payment-promotion` | Chỉ phiếu `HOAN_TAT`; mỗi phiếu 1 HĐ; chuyển khoản có mã giao dịch; HĐ đã trả bất biến |
| UC016 Tạo hồ sơ khách hàng | TNV | Tạo khách và xe | `KhachHang`, `Xe`, `seq_KhachHang` | Chưa có | `QuanLyKhachHangUI` | `crud-master-data` | SĐT 10–11 số duy nhất; CCCD 12 số; biển số 6–15; không xóa khách đã có phiếu |

## Ma trận phủ skill

| Skill | UC trực tiếp |
|---|---|
| `project-context-analysis` | Tất cả (bắt buộc đầu phiên) |
| `use-case-analysis` | Tất cả |
| `role-authorization` | UC001–UC016 |
| `business-rules-workflow` | UC001, UC003, UC009–UC015 |
| `java-architecture` | UC002–UC016 |
| `database-transaction-integrity` | UC002–UC006, UC009–UC016 |
| `authentication-session` | UC002, UC004 |
| `swing-ui-development` | UC002–UC016 |
| `role-navigation-dashboard` | UC001, UC007, UC008, UC013, UC015 |
| `swing-threading-responsiveness` | UC001, UC007–UC015 |
| `usecase-orchestrator` | Bất kỳ UC nào làm trọn gói |
| `crud-master-data` | UC002–UC006, UC016 |
| `repair-order-workflow` | UC010, UC011 |
| `inventory-replenishment` | UC006, UC009 |
| `invoice-payment-promotion` | UC001, UC003, UC014, UC015 |
| `returns-warranty` | UC012 |
| `search-statistics` | UC001, UC007, UC008, UC013 |
| `testing-acceptance` | Tất cả |
| `debugging-regression` | Tất cả khi có lỗi |
| `code-review-completion` | Tất cả trước khi báo xong |

## Quy tắc cập nhật bảng

Khi hoàn thành một UC, cập nhật:

1. Cột "Code hiện có" (DAO/controller/UI thực tế).
2. Cột "Màn hình mục tiêu" nếu tên lớp đã chốt.
3. Thêm ghi chú test đã chạy vào báo cáo UC, không chỉ sửa bảng.
4. Nếu phát hiện lệch tài liệu/DB, thêm vào `KNOWN_DISCREPANCIES.md`.
