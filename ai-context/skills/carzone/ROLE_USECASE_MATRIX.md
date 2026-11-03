# ROLE_USECASE_MATRIX — quyền theo vai trò và use case

Nguồn: `enums.VaiTro`, `AuthController.layDanhSachNavigationTheoRole`, trigger/stored procedure trong V1, `ai-context/business-rules.md`. Khi code và bảng này lệch, bảng này là mục tiêu; ghi lỗi vào `KNOWN_DISCREPANCIES.md` nếu code chưa đạt.

Ký hiệu: `F` = toàn bộ, `O` = chỉ dữ liệu của mình/được giao, `-` = không được phép, `?` = chưa chốt, phải hỏi.

## 1. Ma trận UC

| UC | Tên | CHU_CUA_HANG | TIEP_NHAN_VIEN | KY_THUAT_VIEN | Ghi chú quyền |
|---|---|---|---|---|---|
| UC001 | Xem báo cáo thống kê | F | - | - | Doanh thu toàn cửa hàng |
| UC002 | Tạo tài khoản | F | - | - | Chỉ CCH; tiền tố phải khớp vai trò nhân viên |
| UC003 | Thêm chương trình khuyến mại | F | - | - | Chỉ CCH tạo; HĐ đã dùng thì khóa sửa mức/ngày/danh sách |
| UC004 | Thêm hồ sơ nhân viên | F | - | - | Chỉ CCH; DB chỉ cho một CCH |
| UC005 | Thêm dịch vụ | F | - | - | Chỉ CCH |
| UC006 | Thêm linh kiện | F | - | - | Chỉ CCH |
| UC007 | Tra cứu thông tin | F | F | O | KTV chỉ tra cứu dữ liệu liên quan phiếu của mình; không xem số liệu người khác |
| UC008 | Xem thống kê sửa chữa | F | O | O | TNV xem phiếu mình lập; KTV xem dịch vụ/linh kiện mình thực hiện |
| UC009 | Yêu cầu nhập linh kiện | F (duyệt/nhập) | - | F (tạo) | Chỉ KTV tạo; chỉ CCH duyệt/từ chối/nhập kho |
| UC010 | Lập phiếu tiếp nhận - sửa chữa | - | F | - | Chỉ TNV lập; có thể gán KTV chính ngay hoặc sau |
| UC011 | Cập nhật phiếu tiếp nhận - sửa chữa | - | F | O | KTV chỉ phiếu được giao; TNV cập nhật thông tin phiếu |
| UC012 | Tạo phiếu đổi trả | - | F | ? | DB chỉ cho TNV lập phiếu; KTV tham gia xử lý chi tiết nếu được duyệt |
| UC013 | Xem thống kê tiếp nhận | F | O | O | TNV xem phiếu mình lập; KTV xem phiếu mình thực hiện |
| UC014 | Áp dụng mã giảm giá | - | F | - | Gắn khuyến mại khi lập hóa đơn |
| UC015 | Thanh toán hóa đơn | - | F | - | Chỉ TNV; HĐ đã thanh toán bị khóa |
| UC016 | Tạo hồ sơ khách hàng | - | F | - | TNV quản lý khách hàng và xe |

## 2. Quy tắc cưỡng chế

Quyền phải được kiểm tra ở ba tầng, không chỉ ẩn nút:

1. **UI**: chỉ hiển thị menu/nút được phép; không phải lớp bảo vệ duy nhất.
2. **Controller**: đọc `UserSession.getInstance().getVaiTro()` và `getMaNV()`; từ chối nếu không đúng vai trò hoặc không phải chủ sở hữu.
3. **Database**: trigger/procedure là chốt cuối; không bỏ qua bằng câu SQL khác.

Không nhận vai trò từ tham số UI, không đọc role từ biến toàn cục tự tạo, không dùng nhân viên mặc định khi chưa đăng nhập.

## 3. Phạm vi dữ liệu "của tôi"

- `TIEP_NHAN_VIEN`: phiếu có `maNVTiepNhan = UserSession.getMaNV()`; hóa đơn/đổi trả do mình lập; thống kê tiếp nhận/đổi trả của mình.
- `KY_THUAT_VIEN`: phiếu có `maNVKyThuat = UserSession.getMaNV()`; yêu cầu nhập do mình lập; thống kê dịch vụ/linh kiện trên phiếu của mình.
- `CHU_CUA_HANG`: toàn bộ dữ liệu; là người duy nhất xem doanh thu tổng, tạo danh mục và tài khoản.

Nếu một câu query không có điều kiện `maNV`, mặc định là sai cho TNV/KTV. Kiểm tra trước khi hiển thị.

## 4. Menu theo vai trò (theo `AuthController`)

`CHU_CUA_HANG`: `NAV_DASHBOARD`, `NAV_THONG_KE`, `NAV_QUAN_LY_DICH_VU`, `NAV_LINH_KIEN_KHO`, `NAV_YC_LINH_KIEN`, `NAV_KHUYEN_MAI`, `NAV_NHAN_VIEN`, `NAV_TAI_KHOAN`, `NAV_TRA_CUU`, `NAV_THONG_TIN_CA_NHAN`.

`TIEP_NHAN_VIEN`: `NAV_DASHBOARD`, `NAV_KHACH_HANG_XE`, `NAV_PHIEU_TIEP_NHAN`, `NAV_HOA_DON`, `NAV_DOI_TRA`, `NAV_TRA_CUU`, `NAV_THONG_TIN_CA_NHAN`.

`KY_THUAT_VIEN`: `NAV_DASHBOARD`, `NAV_PHIEU_PHAN_CONG`, `NAV_CAP_NHAT_SUA_CHUA`, `NAV_YC_LINH_KIEN`, `NAV_TRA_CUU`, `NAV_THONG_TIN_CA_NHAN`.

Không thêm menu mới mà không cập nhật đồng thời `AuthController`, `MainFrame`/khung chính, `CardLayout` và skill này.

## 5. Điểm dễ sai

- Ẩn nút nhưng controller vẫn cho gọi: sai.
- KTV mở phiếu người khác bằng cách sửa `maPhieu` trên URL/ô tìm kiếm: phải chặn ở controller/DAO.
- TNV sửa dịch vụ/linh kiện do KTV ghi nhận: sai; chỉ KTV chính được cập nhật chi tiết/trạng thái phiếu.
- CCH tự tạo phiếu tiếp nhận/hóa đơn: DB sẽ chặn; UI không nên hiển thị.
- KTV tạo hóa đơn/phiếu đổi trả: DB sẽ chặn; không thiết kế luồng này.
- Dùng `MainFrame` không có session: hiện có thể tạo nhân viên mặc định; phải sửa thành chuyển về đăng nhập.
