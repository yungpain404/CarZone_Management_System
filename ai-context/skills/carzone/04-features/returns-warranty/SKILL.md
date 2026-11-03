---
name: returns-warranty
description: Dùng cho UC012 khi lập phiếu đổi trả, chọn hàng đổi/trả, hoàn tiền, xử lý bảo hành hoặc điều chỉnh doanh thu do đổi trả trong CarZone.
---

# Đổi trả và bảo hành

## Phạm vi

- UC012: lập phiếu đổi trả cho hóa đơn đã thanh toán.
- Bảo hành: chính sách hiển thị/tính từ hóa đơn; chưa có bảng riêng.

## Điều kiện lập phiếu

- Hóa đơn `DA_THANH_TOAN`.
- Trong vòng 7 ngày kể từ `HoaDon.ngayLap` (không tính từ thời điểm thanh toán).
- Lý do đổi trả không rỗng, tối đa 500.
- Người lập là `TIEP_NHAN_VIEN` (trigger 50151).
- Không lập cho hóa đơn chưa thanh toán hoặc quá hạn.

## Chi tiết đổi trả

- Mỗi dòng thuộc đúng `maPhieu` của hóa đơn gốc (trigger 50161).
- Mỗi dòng chỉ chọn một trong hai: `maDV` hoặc `maLK`.
- `soLuong > 0`, `donGia >= 0`.
- Tổng số lượng đổi trả không vượt số lượng đã thực hiện/dùng (trigger 50162, 50163).
- `huongXuLy`: `DOI_MOI`, `HOAN_TIEN`, `SUA_LAI_MIEN_PHI`.
- `thanhTien` chỉ > 0 khi `HOAN_TIEN`; các hướng khác bằng 0.
- `soTienHoanTra` = tổng `thanhTien` các dòng `HOAN_TIEN`, lấy từ view.

## Sinh mã

Gọi `sp_LapPhieuDoiTra(@maHoaDon, @maNV, @lyDoDoiTra, @maPhieuDT OUTPUT)`.

- Định dạng `PDTddMMyyyyAXX`.
- Tối đa 99 phiếu mỗi ca.
- Không tự sinh mã ở Java.

## Tồn kho

DB hiện **không** tự cộng lại linh kiện vào kho khi đổi trả. Không được giả định có nhập kho. Nếu nghiệp vụ cần nhập lại, phải có quyết định và migration/trigger mới; chưa có thì chỉ ghi nhận đổi trả và hoàn tiền.

## Doanh thu

- Hoàn tiền trừ vào doanh thu đúng ngày phát sinh phiếu đổi trả.
- Không trừ vào ngày hóa đơn gốc.
- Dùng `vw_PhieuDoiTra_HoanTien`, `vw_DoanhThuTheoNgay`.
- Không sửa hóa đơn gốc.

## Bảo hành

Chính sách theo tài liệu:

- Điện: 6 tháng hoặc 5.000 km.
- Động cơ/hộp số: 6 tháng hoặc 5.000 km.
- Thay dầu và phụ kiện: 3 tháng hoặc 3.000 km.
- Tính từ ngày lập hóa đơn.

Hiện trạng:

- Không có bảng/trường bảo hành.
- Không có số km tại thời điểm lập hóa đơn, nên không thể kiểm tra điều kiện km một cách đáng tin.
- Chỉ được tính/hiển thị suy diễn từ hóa đơn và danh mục; không tạo bảng mới khi chưa được duyệt.
- Ghi rõ giới hạn này trong tài liệu/báo cáo.

## Vai trò

- DB: chỉ TNV được tạo header phiếu đổi trả.
- KTV tham gia "xử lý đổi trả" theo yêu cầu tổng quát nhưng chưa có cơ chế DB/UI chốt.
- Chưa có quyết định: không thiết kế KTV tạo phiếu. Nếu cần, hỏi và cập nhật `KNOWN_DISCREPANCIES.md`.

## UI

- Chọn hóa đơn đã thanh toán trong 7 ngày.
- Hiển thị chi tiết hóa đơn và số lượng còn được đổi trả.
- Cho chọn hướng xử lý từng dòng.
- Tính và hiển thị số tiền hoàn.
- Cảnh báo hóa đơn quá hạn/chưa thanh toán.
- Sau lưu: không sửa hóa đơn gốc; hiển thị phiếu đổi trả.

## Transaction

- Tạo header: một procedure.
- Thêm chi tiết: một transaction; rollback toàn bộ nếu một dòng vượt số lượng.
- Không để header mồ côi không có chi tiết nếu nghiệp vụ yêu cầu ít nhất một dòng; kiểm tra trước khi commit.

## Checklist

- [ ] Hóa đơn đã thanh toán.
- [ ] Trong 7 ngày.
- [ ] Chỉ TNV lập header.
- [ ] Chi tiết đúng phiếu gốc.
- [ ] Không vượt số lượng.
- [ ] Chỉ `HOAN_TIEN` mới tính tiền.
- [ ] Không sửa hóa đơn gốc.
- [ ] Hoàn tiền trừ đúng ngày.
- [ ] Không giả định nhập lại kho.
- [ ] Bảo hành ghi rõ là suy diễn.

## Liên quan

- `04-features/invoice-payment-promotion`
- `04-features/search-statistics`
- `02-architecture/database-transaction-integrity`
- `KNOWN_DISCREPANCIES.md` mục D05, D06
