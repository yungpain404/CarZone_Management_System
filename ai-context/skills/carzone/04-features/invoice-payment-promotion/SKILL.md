---
name: invoice-payment-promotion
description: Dùng cho UC001, UC003, UC014, UC015 khi lập hóa đơn từ phiếu đã hoàn tất, áp khuyến mại, tính thuế/tổng tiền, thanh toán, khóa hóa đơn hoặc báo cáo doanh thu trong CarZone.
---

# Hóa đơn, thanh toán và khuyến mại

## Phạm vi

- UC003: tạo chương trình khuyến mại.
- UC014: áp dụng mã giảm giá khi lập hóa đơn.
- UC015: lập và thanh toán hóa đơn.
- UC001: doanh thu.

## Điều kiện lập hóa đơn

- Phiếu sửa chữa `HOAN_TAT`.
- Chưa có hóa đơn cho phiếu (`UQ_HoaDon_maPhieu`).
- Người lập là `TIEP_NHAN_VIEN` (trigger 50131).
- Khuyến mại (nếu có) còn hiệu lực tại `ngayLap` (trigger 50133), tối đa một.
- Không lập hóa đơn cho phiếu đang sửa/chờ linh kiện.

## Sinh mã

Gọi `sp_LapHoaDon(@maPhieu, @maNV, @maKM, @maHoaDon OUTPUT)`.

- Định dạng `HDddMMyyyyAXXX`.
- `A` trước 12h, `P` từ 12h.
- Tối đa 999 hóa đơn mỗi ca.
- Không tự sinh mã ở Java.

## Tiền

Lấy từ `vw_HoaDon_TongTien`; không tính lại ở Java.

```text
tongTienDichVu   = SUM(ChiTietDichVu.thanhTien)
tongTienLinhKien = SUM(ChiTietLinhKien.thanhTien)
thue:
  chỉ linh kiện: 1,5% tongTienLinhKien
  chỉ dịch vụ:   7%   tongTienDichVu
  cả hai:        4,5% (tongTienDichVu + tongTienLinhKien)
  không có gì:   0
tienGiam = mucGiamPhanTram% * các dòng được áp dụng
tongThanhToan = tongTienDichVu + tongTienLinhKien - tienGiam + thue
```

- Thuế tính trên tiền trước giảm.
- Khuyến mại không liệt kê mặt hàng = giảm toàn hóa đơn.
- Có liệt kê = chỉ giảm đúng dịch vụ/linh kiện trong danh sách.
- Làm tròn theo DB (`DECIMAL(18,0)`); không tự làm tròn kiểu khác.

## Thanh toán

Gọi `sp_ThanhToanHoaDon(@maHoaDon, @phuongThucTT, @maGiaoDich)`.

- Chỉ hóa đơn `CHUA_THANH_TOAN`.
- `TIEN_MAT` hoặc `CHUYEN_KHOAN`.
- `CHUYEN_KHOAN` bắt buộc `maGiaoDich`.
- Ghi `thoiGianThanhToan`.
- Hóa đơn `DA_THANH_TOAN` không sửa/xóa (trigger 50141, 50142).
- Không thanh toán lại.

## Khuyến mại

- Chỉ CCH tạo/sửa.
- Mức giảm (0, 100]; ngày bắt đầu <= ngày kết thúc.
- Tên duy nhất.
- Khi đã dùng trong hóa đơn: không đổi mức giảm, ngày, danh sách áp dụng (trigger 50172, 50181, 50182).
- Không xóa khuyến mại đã dùng.
- UI chỉ hiển thị khuyến mại còn hiệu lực tại ngày lập.

## Doanh thu

- Chỉ hóa đơn `DA_THANH_TOAN`.
- Dùng `vw_DoanhThuTheoNgay`; doanh thu ghi nhận theo `thoiGianThanhToan`, không theo `ngayLap`.
- Hoàn tiền trừ vào đúng ngày phát sinh đổi trả (`PhieuDoiTra.ngayLap`).
- Không cộng hóa đơn chưa thanh toán.
- Không trừ hoàn tiền vào ngày hóa đơn gốc.
- Doanh thu thuần = doanh thu - hoàn trả.

## UI

- Màn hình lập hóa đơn: chọn phiếu `HOAN_TAT`, xem chi tiết, chọn khuyến mại, xem tạm tính/thuế/tổng, xác nhận.
- Màn hình thanh toán: chọn phương thức, nhập mã giao dịch nếu chuyển khoản, xác nhận.
- Hiển thị rõ "Chưa thanh toán/Đã thanh toán".
- Sau thanh toán: khóa nút sửa, cho in/xem.
- Không cho nhập tay tổng tiền.

## Transaction và lỗi

- Lập hóa đơn: một procedure.
- Thanh toán: một procedure/UPDATE có điều kiện trạng thái.
- Bắt và dịch 50131–50133, 50141–50142, 50201–50211.
- Nếu procedure tạo HĐ xong nhưng thanh toán lỗi, hóa đơn vẫn `CHUA_THANH_TOAN`; không tự xóa nếu chưa có quy trình.

## Checklist

- [ ] Chỉ phiếu `HOAN_TAT`.
- [ ] Mỗi phiếu một hóa đơn.
- [ ] Chỉ TNV lập.
- [ ] Khuyến mại hợp lệ, tối đa một.
- [ ] Tiền lấy từ view.
- [ ] Thuế đúng ba trường hợp.
- [ ] Chuyển khoản có mã giao dịch.
- [ ] Hóa đơn đã trả bất biến.
- [ ] Doanh thu chỉ HĐ đã trả, hoàn tiền đúng ngày.
- [ ] Có test biên và lỗi.

## Liên quan

- `04-features/repair-order-workflow`
- `04-features/returns-warranty`
- `04-features/search-statistics`
- `02-architecture/database-transaction-integrity`
