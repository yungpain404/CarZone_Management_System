---
name: repair-order-workflow
description: Dùng cho UC010 và UC011 khi lập phiếu tiếp nhận, gán kỹ thuật viên chính, thêm dịch vụ/linh kiện, chuyển trạng thái hoặc hoàn tất phiếu sửa chữa trong CarZone.
---

# Quy trình phiếu tiếp nhận - sửa chữa

## Phạm vi

- UC010: lập phiếu, chọn xe, gán KTV chính, ghi yêu cầu ban đầu.
- UC011: cập nhật thông tin, thêm/sửa/xóa dịch vụ và linh kiện, chuyển trạng thái, hoàn tất.

## Vai trò

- TNV: lập phiếu, cập nhật thông tin phiếu, gán/đổi KTV chính khi còn `CHO_SUA`.
- KTV: chỉ cập nhật phiếu có `maNVKyThuat` là mình; thêm/sửa chi tiết, chuyển trạng thái.
- CCH: không lập/cập nhật phiếu; chỉ xem/thống kê.

DB chỉ kiểm tra vai trò của `maNVTiepNhan` và `maNVKyThuat`, không kiểm tra quyền sở hữu khi UPDATE. Java phải chặn KTV sửa phiếu người khác.

## Máy trạng thái

```text
CHO_SUA -> DANG_SUA | CHO_LINH_KIEN
DANG_SUA -> CHO_LINH_KIEN | HOAN_TAT
CHO_LINH_KIEN -> DANG_SUA
HOAN_TAT -> STOP
```

Quy tắc:

- Trạng thái khác `CHO_SUA` bắt buộc có `maNVKyThuat` (`CK_PTNSC_KTVChinh`).
- KTV chính phải khác người tiếp nhận (`CK_PTNSC_KhacNguoi`).
- Chỉ phiếu `HOAN_TAT` mới được lập hóa đơn.
- `HOAN_TAT` khóa toàn bộ thông tin phiếu và chi tiết.
- Không nhảy cóc `CHO_SUA -> HOAN_TAT`.
- Không quay lại từ `HOAN_TAT`.

## Lập phiếu (UC010)

1. Chọn/tạo khách hàng và xe; xe phải tồn tại.
2. Nhập yêu cầu ban đầu, không rỗng, tối đa 500.
3. Gán KTV chính (có thể để trống khi mới tạo).
4. Gọi `sp_LapPhieuTiepNhan` để sinh `maPhieu`.
5. Trạng thái mặc định `CHO_SUA`.
6. Trả mã phiếu cho UI.

Không tự sinh mã. Không tự đặt trạng thái khác.

## Cập nhật (UC011)

### Thông tin phiếu

- TNV được sửa khi phiếu chưa `HOAN_TAT`.
- Không đổi xe/người tiếp nhận/ngày tiếp nhận khi đã `HOAN_TAT`.
- Đổi KTV chính: chỉ khi `CHO_SUA` hoặc theo quy trình được duyệt; KTV mới phải đúng vai trò và khác TNV.

### Chi tiết dịch vụ

- Mỗi cặp `(maPhieu, maDV)` chỉ một dòng; tăng số lượng thì UPDATE, không INSERT trùng.
- `soLuong > 0`; `donGiaThucHien >= 0`.
- Dịch vụ phải `dangCungCap = 1`.
- Không thêm/sửa/xóa khi phiếu `HOAN_TAT` (lỗi 50111).
- `thanhTien` do DB tính; không lưu tay.

### Chi tiết linh kiện

- Mỗi cặp `(maPhieu, maLK)` chỉ một dòng.
- `soLuong > 0`; `donGiaXuat >= 0`.
- Linh kiện phải `dangKinhDoanh = 1`.
- Không thêm/sửa/xóa khi phiếu `HOAN_TAT` (lỗi 50121).
- Tồn kho do trigger tự trừ/cộng; Java không cập nhật `soLuongTon`.
- Không bán vượt tồn; trigger 50123 chặn.

### Chuyển trạng thái

- Chỉ KTV chính thực hiện; TNV cập nhật thông tin phiếu nhưng không đổi trạng thái sửa chữa, trừ khi có quyết định mới.
- Kiểm tra chuyển tiếp hợp lệ trước khi UPDATE.
- Khi sang `HOAN_TAT`, xác nhận với người dùng; sau đó khóa.
- Không cập nhật trạng thái bằng câu SQL không kiểm tra quyền.

## Transaction

- Lập phiếu: một lời gọi procedure.
- Thêm nhiều chi tiết trong một lần lưu: một connection, autoCommit false, rollback nếu một dòng lỗi.
- Không giữ transaction qua nhiều lần bấm.

## UI

- Màn hình lập phiếu: thông tin xe/khách, yêu cầu, KTV, nút lưu.
- Màn hình cập nhật: header, tab dịch vụ, tab linh kiện, lịch sử trạng thái, nút chuyển trạng thái.
- Hiển thị tồn kho hiện tại khi chọn linh kiện.
- Khóa form khi `HOAN_TAT`.
- Cảnh báo khi rời màn hình còn thay đổi chưa lưu.

## Lỗi DB cần dịch

| Mã | Nghĩa |
|---|---|
| 50101 | Người lập không phải TNV |
| 50102 | KTV chính không đúng vai trò |
| 50103 | Chuyển trạng thái sai |
| 50104 | Sửa phiếu đã hoàn tất |
| 50111/50121 | Sửa chi tiết phiếu đã hoàn tất |
| 50112/50122 | Dịch vụ/linh kiện đã ngừng |
| 50123 | Vượt tồn kho |

## Checklist

- [ ] Đúng quyền TNV/KTV.
- [ ] KTV chỉ sửa phiếu được giao.
- [ ] Có KTV chính trước khi rời `CHO_SUA`.
- [ ] Chuyển trạng thái đúng.
- [ ] Không sinh mã ở Java.
- [ ] Không sửa tồn kho ở Java.
- [ ] Không sửa chi tiết khi `HOAN_TAT`.
- [ ] Không tạo hóa đơn khi chưa hoàn tất.
- [ ] Có test mọi chuyển hợp lệ/không hợp lệ.

## Liên quan

- `01-analysis/business-rules-workflow`
- `02-architecture/database-transaction-integrity`
- `04-features/invoice-payment-promotion`
- `04-features/inventory-replenishment`
