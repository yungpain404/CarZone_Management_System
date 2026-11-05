---
name: search-statistics
description: Dùng cho UC001, UC007, UC008, UC013 khi tra cứu khách/xe/phiếu/lịch sử, xem thống kê sửa chữa, tiếp nhận hoặc doanh thu trong CarZone, bao gồm phạm vi dữ liệu theo vai trò và định nghĩa KPI.
---

# Tra cứu và thống kê

## Phạm vi

| UC | Nội dung | Vai trò |
|---|---|---|
| UC001 | Báo cáo doanh thu | CCH |
| UC007 | Tra cứu khách/xe/phiếu/lịch sử | Cả ba, có giới hạn |
| UC008 | Thống kê sửa chữa | CCH toàn bộ; TNV/KTV của mình |
| UC013 | Thống kê tiếp nhận/đổi trả | CCH toàn bộ; TNV/KTV của mình |

## Nguồn dữ liệu

- `vw_LichSuSuaChua`: biển số, khách, phiếu, trạng thái, KTV, tổng tiền.
- `vw_PhieuSuaChua_TongTien`: tổng dịch vụ/linh kiện.
- `vw_HoaDon_TongTien`: tiền hóa đơn.
- `vw_PhieuDoiTra_HoanTien`: tiền hoàn.
- `vw_DoanhThuTheoNgay`: doanh thu, hoàn trả, doanh thu thuần.
- Bảng gốc khi cần lọc chi tiết.

## Nguyên tắc

- Chỉ đọc; không sửa dữ liệu.
- Luôn lọc theo vai trò:
  - CCH: không lọc `maNV`.
  - TNV: `maNVTiepNhan = session.maNV` cho phiếu; `maNV = session.maNV` cho hóa đơn/đổi trả.
  - KTV: `maNVKyThuat = session.maNV`; thống kê dịch vụ/linh kiện chỉ trên phiếu của mình.
- Không hiển thị dữ liệu người khác rồi mới chặn.
- Dùng `PreparedStatement`; không nối chuỗi SQL.
- Phân trang hoặc giới hạn `TOP` cho danh sách lớn.
- Không N+1: dùng JOIN/view thay vì query từng dòng.
- Ngày lọc bao gồm cả hai đầu; nếu DB dùng `DATETIME2`, dùng `< ngày kế tiếp` thay vì `<= 23:59:59`.
- Định dạng tiền `#,##0`; ngày `dd/MM/yyyy`.

## Định nghĩa KPI

- Doanh thu: tổng `tongThanhToan` của hóa đơn `DA_THANH_TOAN`, ghi nhận theo `thoiGianThanhToan`.
- Hoàn trả: tổng `soTienHoanTra` của phiếu đổi trả, ghi nhận theo `PhieuDoiTra.ngayLap`.
- Doanh thu thuần: doanh thu - hoàn trả.
- Số hóa đơn: đếm hóa đơn đã thanh toán.
- Tồn thấp: `soLuongTon <= 0` và `dangKinhDoanh = 1`.
- Phiếu chờ sửa/đang sửa/chờ linh kiện/hoàn tất: đếm theo `trangThai`.
- KTV hoàn tất: phiếu `HOAN_TAT` có `maNVKyThuat` là mình.

Không trộn doanh thu (gộp) với doanh thu thuần. Ghi rõ KPI nào đang hiển thị.

## Tra cứu

- Theo biển số: `Xe.bienSoXe`; có thể tìm gần đúng.
- Theo SĐT: `KhachHang.soDienThoai` (duy nhất).
- Theo mã phiếu/hóa đơn: tìm chính xác.
- Kết quả gồm: khách, xe, phiếu, trạng thái, ngày, KTV, tổng tiền.
- Không trả CCCD/địa chỉ nếu không cần.
- KTV chỉ thấy phiếu của mình, kể cả khi tìm bằng biển số của khách khác.

## Thống kê

- Theo khoảng ngày, vai trò, trạng thái, KTV.
- CCH xem toàn bộ; TNV/KTV chỉ xem của mình.
- Có thể xuất CSV nếu được yêu cầu; không tự thêm thư viện nếu chưa có.
- Ngày không có giao dịch: view có thể không trả dòng; UI tự điền 0 nếu cần biểu đồ.

## UI

- Bộ lọc ngày, từ khóa, trạng thái.
- Bảng kết quả + tổng hợp KPI.
- Nút làm mới, xóa lọc.
- Trạng thái rỗng/lỗi/loading.
- Không hiển thị số liệu toàn cục cho TNV/KTV.

## Test

- Lọc ngày đúng biên (đầu ngày, cuối ngày, ngày không có dữ liệu).
- Hoàn tiền trừ đúng ngày phát sinh.
- Chỉ hóa đơn đã thanh toán được tính.
- TNV/KTV chỉ thấy dữ liệu của mình.
- Tìm theo biển số/SĐT/mã.
- Không lỗi SQL injection với ký tự đặc biệt.

## Checklist

- [ ] Đúng phạm vi vai trò.
- [ ] Chỉ đọc.
- [ ] Dùng view đúng.
- [ ] KPI có định nghĩa.
- [ ] Ngày biên đúng.
- [ ] Không N+1.
- [ ] Không lộ dữ liệu nhạy cảm.
- [ ] Có loading/rỗng/lỗi.
- [ ] Có test quyền và biên.

## Liên quan

- `01-analysis/role-authorization`
- `03-ui/role-navigation-dashboard`
- `04-features/invoice-payment-promotion`
- `04-features/returns-warranty`
