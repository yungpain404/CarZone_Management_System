---
name: crud-master-data
description: Dùng cho UC002–UC006 và UC016 khi thêm/sửa/tra cứu khách hàng, xe, dịch vụ, linh kiện, nhân viên, tài khoản hoặc khuyến mại trong CarZone, bao gồm validate, quyền, quy tắc sinh mã và ngừng sử dụng thay vì xóa.
---

# CRUD và dữ liệu danh mục

## Phạm vi

| UC | Đối tượng | Vai trò |
|---|---|---|
| UC002 | Tài khoản | CCH |
| UC003 | Khuyến mại | CCH |
| UC004 | Nhân viên | CCH |
| UC005 | Dịch vụ | CCH |
| UC006 | Linh kiện | CCH |
| UC016 | Khách hàng và xe | TNV |

## Nguyên tắc chung

- Mã do DB sinh khi đã có procedure/sequence; Java không tự sinh.
- Ngoại lệ chưa có cơ chế sinh mã: `DichVu.maDV`, `LinhKien.maLK`, `Xe.bienSoXe`. Với DV/LK, xem D19 và hỏi trước khi chọn nhập tay hay thêm sequence/default bằng migration.
- Không xóa cứng bản ghi đã được tham chiếu; dùng `dangCungCap`/`dangKinhDoanh`/`trangThaiHoatDong` nếu có.
- Kiểm tra quyền ở controller, không chỉ ẩn nút.
- Validate ở entity/controller; DB vẫn là chốt cuối.
- Tên duy nhất theo ràng buộc DB; bắt lỗi trùng và báo thân thiện.
- Tìm kiếm không phân biệt hoa/thường theo collation `Vietnamese_CI_AS`.

## Validate theo đối tượng

### Khách hàng

- `hoTen` không rỗng, tối đa 100.
- `soDienThoai`: 10–11 số, bắt đầu `0`, duy nhất.
- `CCCD`: 12 số, duy nhất nếu có.
- `ghiChu` tối đa 500.
- `thanhVienThanThiet`, `diemTichLuy >= 0` (V2).
- Không xóa khách đã có xe/phiếu.

### Xe

- `bienSoXe`: 6–15 ký tự, duy nhất, không rỗng.
- `hangXe` bắt buộc; `model`, `mauSac` tùy chọn.
- `namSanXuat` 1900–2100.
- `soKmHienTai >= 0`.
- Bắt buộc gắn `maKH`.

### Dịch vụ

- `maDV` dạng `DV` + ít nhất 5 số. DB chưa có sequence/default/procedure sinh mã (D19); chưa được tự sinh ở Java khi chưa chốt.
- `tenDV` không rỗng, tối đa 150, duy nhất.
- `donGia > 0`.
- `donViTinh` mặc định "lần".
- Ngừng cung cấp: `dangCungCap = 0`; không thêm vào phiếu mới.

### Linh kiện

- `maLK` dạng `LK` + ít nhất 5 số. DB chưa có sequence/default/procedure sinh mã (D19); chưa được tự sinh ở Java khi chưa chốt.
- `tenLK` duy nhất, tối đa 150.
- `donGia > 0`.
- `soLuongTon >= 0`, chỉ trigger/procedure được sửa.
- Ngừng kinh doanh: `dangKinhDoanh = 0`; không thêm vào phiếu mới.

### Nhân viên

- `maNV` do `sp_ThemNhanVien` sinh, 6 ký tự, tiền tố khớp vai trò.
- `hoTen` không rỗng.
- `ngaySinh` + `ngayVaoLam`: đủ 18 tuổi tại ngày vào làm.
- `soDienThoai` 10–11 số, duy nhất.
- `CCCD` 12 số, duy nhất.
- `vaiTro` thuộc ba giá trị; chỉ một `CHU_CUA_HANG`.
- Không xóa nhân viên đã có phiếu/hóa đơn; dùng khóa tài khoản.

### Tài khoản

- Chỉ CCH tạo.
- `maNV` phải tồn tại; mỗi nhân viên một tài khoản.
- `tenDangNhap` do `sp_TaoTaiKhoan` sinh, khớp vai trò.
- Mật khẩu tối thiểu 8 ký tự, có hoa/thường/số/ký tự đặc biệt.
- Không log/không hiển thị mật khẩu.
- Vô hiệu hóa: `trangThaiHoatDong = 0`.

### Khuyến mại

- Chỉ CCH.
- `maKM` do sequence sinh, `KM` + 3 số.
- `mucGiamPhanTram` trong (0, 100].
- `ngayBatDau <= ngayKetThuc`.
- Tên chương trình duy nhất.
- Danh sách áp dụng rỗng = toàn hóa đơn; có danh sách = chỉ các mặt hàng đó.
- Không sửa mức giảm/ngày/danh sách khi đã dùng trong hóa đơn (trigger 50172, 50181, 50182).

## Luồng DAO/controller

1. Controller kiểm quyền.
2. Validate DTO.
3. Gọi DAO.
4. DAO dùng procedure nếu có; nếu không, `INSERT/UPDATE` với `PreparedStatement`.
5. Bắt lỗi trùng khóa/CHECK, dịch thông báo.
6. Trả kết quả có mã bản ghi mới.

## UI

- Form nhập + bảng danh sách + toolbar thêm/sửa/ngừng.
- Tìm kiếm theo mã/tên/SĐT.
- Dialog xác nhận khi ngừng sử dụng.
- Không cho sửa mã.
- Hiển thị trạng thái "Đang cung cấp/Đã ngừng".
- Làm mới bảng sau khi lưu.

## Checklist

- [ ] Đúng quyền.
- [ ] Mã do DB sinh hoặc theo quyết định đã chốt cho DV/LK.
- [ ] Validate đủ trường.
- [ ] Bắt lỗi trùng/CHECK.
- [ ] Không xóa cứng dữ liệu đã tham chiếu.
- [ ] Không sửa tồn kho trực tiếp.
- [ ] Không log mật khẩu.
- [ ] Có test trùng, biên, quyền.

## Liên quan

- `01-analysis/role-authorization`
- `02-architecture/database-transaction-integrity`
- `04-features/inventory-replenishment`
- `04-features/invoice-payment-promotion`
