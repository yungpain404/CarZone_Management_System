# Luật nghiệp vụ CarZone (bản tra nhanh)

Nguồn: báo cáo `Nhom01_v3_ApplicationDevelopment.docx`, bảng Q&A, và migration `V1`/`V2` (nhiều luật đã được cài thành trigger/stored procedure, mã lỗi 50001–50261). Nếu code, DB và file này lệch nhau, hỏi người dùng trước khi sửa.

## 1. Vai trò và quyền
| Vai trò (`VaiTro`) | Được làm | Không được |
|---|---|---|
| `CHU_CUA_HANG` (duy nhất 1 người, DB có chỉ mục unique) | CRUD dịch vụ, linh kiện, tài khoản/nhân viên, khuyến mãi; duyệt yêu cầu nhập linh kiện và nhập kho; xem báo cáo doanh thu, lịch sử | |
| `TIEP_NHAN_VIEN` | Quản lý khách hàng, xe, phiếu sửa chữa, hóa đơn, thanh toán, phiếu đổi trả; tra cứu lịch sử; xem thống kê phiếu lập/đổi trả **của mình** | Sửa dịch vụ/linh kiện do kỹ thuật viên ghi |
| `KY_THUAT_VIEN` | Xem/cập nhật phiếu **được giao**; tạo yêu cầu nhập linh kiện; xử lý đổi trả; tra cứu; xem thống kê dịch vụ/linh kiện đã sửa **của mình** | Xem phiếu người khác; xem số liệu người khác |

## 2. Máy trạng thái
- Phiếu sửa chữa (`TrangThaiPhieu`): `CHO_SUA → DANG_SUA ⇄ CHO_LINH_KIEN`, và `DANG_SUA → HOAN_TAT`. Phiếu `HOAN_TAT` bị khóa (không thêm/sửa/xóa dịch vụ, linh kiện).
- Hóa đơn (`TrangThaiThanhToan`): `CHUA_THANH_TOAN → DA_THANH_TOAN`. Đã thanh toán thì không sửa, không xóa. `thoiGianThanhToan` và `phuongThucTT` chỉ có khi đã thanh toán.
- Yêu cầu nhập linh kiện (`TrangThaiYeuCau`): tạo ở `CHO_DUYET → DA_DUYET | TU_CHOI`; `DA_DUYET → DA_NHAP_KHO` (lúc này tồn kho được cộng).
- `PhuongThucThanhToan`: `TIEN_MAT`, `CHUYEN_KHOAN`. `HuongXuLyDoiTra`: `DOI_MOI`, `HOAN_TIEN`, `SUA_LAI_MIEN_PHI`.

## 3. Mã tự sinh
| Đối tượng | Định dạng | Ví dụ |
|---|---|---|
| Phiếu sửa chữa | `PSC` + ddMMyyyy + `A`/`P` + 3 số | PSC23092026A001 |
| Hóa đơn | `HD` + ddMMyyyy + `A`/`P` + 3 số (A: trước 12h, P: từ 12h) | HD23092026A001 |
| Phiếu đổi trả | `PDT` + ddMMyyyy + `A`/`P` + 2 số | PDT23092026A01 |
| Yêu cầu nhập linh kiện | `PYC` + ddMMyyyy + `A`/`P` + 2 số | PYC23092026A01 |
| Khuyến mãi | `KM` + đúng 3 số (DB: `CK_KhuyenMai_Ma`, mặc định lấy từ `seq_KhuyenMai`; entity đang cho `KM\d{3,}`) | KM001 |
| Dịch vụ / Linh kiện | `DV`/`LK` + ≥ 5 số (`^DV\d{5,}$`, `^LK\d{5,}$`) | DV00001 |
| Khách hàng | `KH` + 6 số | KH000001 |
| Nhân viên / tên đăng nhập | `CCH`/`TNV`/`KTV` + 3 số | KTV003 |

Hiện V1 sinh mã theo ngày/ca và theo vai trò bằng stored procedure (`sp_LapHoaDon`, `sp_LapPhieuTiepNhan`, `sp_LapPhieuDoiTra`, `sp_TaoYeuCauNhapLinhKien`, `sp_ThemNhanVien`, `sp_TaoTaiKhoan`), tối đa 999 mã/ca (hóa đơn, phiếu), 99 (đổi trả, yêu cầu).

## 4. Công thức
- `thanhTien` dịch vụ = `soLuong × donGiaThucHien`; linh kiện = `soLuong × donGiaXuat` (giá chụp tại thời điểm ghi nhận).
- `tongTienDichVu`, `tongTienLinhKien` = tổng `thanhTien` các dòng của phiếu.
- **Thuế (đã chốt 25/10/2026):** loại thuế suy ra từ nội dung hóa đơn, không có thuộc tính "loại hóa đơn":
  - chỉ có linh kiện: `tongTienLinhKien × 1,5%`
  - chỉ có dịch vụ: `tongTienDichVu × 7%`
  - có cả hai: `(tongTienLinhKien + tongTienDichVu) × 4,5%`
  - thuế tính trên số tiền **chưa** trừ khuyến mãi (theo view `vw_HoaDon_TongTien`).
- **Khuyến mãi (đã chốt 25/10/2026):** giảm `mucGiamPhanTram`%.
  - Không liệt kê dịch vụ/linh kiện nào (hai danh sách rỗng) = giảm tất cả, không điều kiện ràng buộc.
  - Có liệt kê = chỉ các mặt hàng nằm trong danh sách được giảm, mặt hàng khác không giảm (kể cả khi chỉ liệt kê dịch vụ thì linh kiện không được giảm, và ngược lại).
  - View `vw_HoaDon_TongTien` đã cài đúng như vậy.
- `tongThanhToan = tongTienDichVu + tongTienLinhKien − tienGiam + thue`. (Công thức `× (1 − khuyenMai)` trong báo cáo đã cũ, cần sửa báo cáo.)
- `ChiTietDoiTra.thanhTien = soLuong × donGia` nếu `HOAN_TIEN`, ngược lại `0`. `soTienHoanTra` = tổng các dòng `HOAN_TIEN`. Tổng số lượng đổi trả không vượt số đã thực hiện/đã dùng.

## 5. Ràng buộc dữ liệu chính
- Số lượng > 0; đơn giá dịch vụ/linh kiện > 0; tồn kho ≥ 0; số lượng dùng ≤ tồn; không thêm dịch vụ/linh kiện đã ngừng cung cấp/kinh doanh vào phiếu.
- Khuyến mãi: mức giảm trong (0, 100]; ngày kết thúc ≥ ngày bắt đầu; mã áp dụng phải tồn tại, không trùng; chỉ Chủ cửa hàng tạo; đã gắn vào hóa đơn thì không đổi mức giảm, thời gian, danh sách áp dụng. Danh sách áp dụng lưu ở bảng `KhuyenMai_DichVu`, `KhuyenMai_LinhKien`.
- Xe: biển số duy nhất; bắt buộc có chủ xe; số km không giảm; năm sản xuất ≤ năm hiện tại.
- Khách hàng: SĐT đúng 10 số, bắt đầu bằng 0, duy nhất (V2); CCCD nếu có thì không trùng; có `thanhVienThanThiet` và `diemTichLuy ≥ 0` (V2).
- Tài khoản: tên đăng nhập khớp vai trò của nhân viên; mật khẩu ≥ 8 ký tự gồm chữ hoa, chữ thường, số, ký tự đặc biệt; lưu dạng băm SHA-256 (hex in hoa).
- Đổi trả: hóa đơn `DA_THANH_TOAN`, ≤ 7 ngày từ ngày lập hóa đơn, lý do không rỗng; chỉ Tiếp nhận viên lập phiếu.
- Hóa đơn: mỗi phiếu sửa chữa tối đa 1 hóa đơn (`UQ_HoaDon_maPhieu`); chỉ Tiếp nhận viên lập.
- Bảo hành: điện 6 tháng; động cơ/hộp số 6 tháng hoặc 5000 km; thay dầu và phụ kiện 3 tháng hoặc 3000 km (từ ngày lập hóa đơn).

## 6. Chỗ lệch giữa tài liệu, DB và code
Đã xử lý khi refactor entity sang Hibernate (26/10/2026): mục 1–4 cũ (HoaDon↔phiếu, bỏ setter trường dẫn xuất, `KhuyenMai` dùng `Set` + bảng nối, mã `KM` đúng 3 số), cùng một số lỗi ngầm: ngày cuối khuyến mãi bị từ chối (nay so theo ngày), `equals/hashCode` của `ChiTiet*` thiếu dịch vụ/linh kiện, constructor `PhieuYeuCauNhapLinhKien` luôn ném lỗi khi có người xử lý, `TaiKhoan.toString` lộ mật khẩu, tuổi tối thiểu nhân viên 16 (DB: 18).
Còn lại:
1. Báo cáo Word chưa cập nhật công thức thuế/khuyến mãi và định dạng mã mới.
2. Mã mẫu `DV01/LK01` trong bảng giá giấy chỉ là minh họa hiện trạng, hệ thống dùng `DV00001/LK00001`.
3. Comment đầu V1 nhắc tên file demo cũ.
