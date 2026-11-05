---
name: business-rules-workflow
description: Dùng khi công việc có trạng thái, điều kiện tiên quyết, tiền, thuế, khuyến mại, tồn kho, khóa dữ liệu hoặc chuyển trạng thái trong CarZone. Giúp xác định guard, bất biến, mã lỗi và ranh giới transaction trước khi code.
---

# Luật nghiệp vụ và máy trạng thái

## Mục đích

Biến luật nghiệp vụ thành guard và bất biến kiểm tra được, tránh viết code theo cảm tính hoặc bỏ qua trigger.

## Nguồn ưu tiên

1. Trigger/stored procedure/view trong V1.
2. `ai-context/business-rules.md`, `ai-context/decisions.md`.
3. Tài liệu Word (có thể cũ).
4. `ai-context/skills/carzone/KNOWN_DISCREPANCIES.md`.

## Các bước

1. Xác định đối tượng trạng thái: phiếu sửa chữa, hóa đơn, yêu cầu nhập, đổi trả.
2. Vẽ máy trạng thái từ DB, không từ tài liệu.
3. Với mỗi chuyển trạng thái, ghi:
   - Ai được làm.
   - Điều kiện dữ liệu.
   - Side effect (tồn kho, khóa, sinh mã).
   - Mã lỗi DB nếu vi phạm.
4. Liệt kê bất biến:
   - Phiếu `HOAN_TAT` không sửa chi tiết.
   - Hóa đơn `DA_THANH_TOAN` không sửa/xóa.
   - Mỗi phiếu tối đa một hóa đơn.
   - Không bán vượt tồn.
   - Không đổi trả quá số lượng đã dùng.
   - Không đổi trả quá 7 ngày.
5. Xác định nơi kiểm tra:
   - Java validate sớm để báo lỗi thân thiện.
   - DB trigger/procedure là chốt cuối.
   - Không lặp lại việc DB đã làm (sinh mã, trừ tồn).
6. Xác định ranh giới transaction cho chuỗi ghi nhiều bảng.
7. Viết test cho từng chuyển hợp lệ và không hợp lệ.

## Máy trạng thái chuẩn

Phiếu sửa chữa:

```text
CHO_SUA -> DANG_SUA
CHO_SUA -> CHO_LINH_KIEN
DANG_SUA -> CHO_LINH_KIEN
DANG_SUA -> HOAN_TAT
CHO_LINH_KIEN -> DANG_SUA
HOAN_TAT -> STOP
```

Yêu cầu nhập linh kiện:

```text
CHO_DUYET -> DA_DUYET | TU_CHOI
DA_DUYET -> DA_NHAP_KHO
```

Hóa đơn:

```text
CHUA_THANH_TOAN -> DA_THANH_TOAN
DA_THANH_TOAN -> STOP
```

## Bảng guard mẫu

| Hành động | Guard | Nơi chặn | Lỗi |
|---|---|---|---|
| Lập hóa đơn | Phiếu `HOAN_TAT`, chưa có HĐ, người lập là TNV | Trigger `trg_HoaDon_KiemTra`, UNIQUE `maPhieu` | 50131, 50132 |
| Áp khuyến mại | KM tồn tại, còn hiệu lực tại `ngayLap`, tối đa 1 | Trigger `trg_HoaDon_KiemTra` | 50133 |
| Thanh toán | HĐ `CHUA_THANH_TOAN`; chuyển khoản có `maGiaoDich` | CHECK + `sp_ThanhToanHoaDon` | 50211 |
| Đổi trả | HĐ đã trả, trong 7 ngày, đúng phiếu gốc, SL không vượt | Trigger `trg_PhieuDoiTra_KiemTra`, `trg_CTDT_KiemTra` | 50151–50163 |
| Duyệt yêu cầu | Chỉ CCH; `CHO_DUYET -> DA_DUYET/TU_CHOI` | `trg_PYCNLK_KiemTra` | 50192, 50194 |
| Nhập kho | `DA_DUYET -> DA_NHAP_KHO`, cộng tồn một lần | `trg_PYCNLK_KiemTra` | 50194 |

## Quy tắc CarZone

- Không tự chế trạng thái mới nếu chưa có migration và quyết định.
- `CHO_LINH_KIEN` là nhánh hợp lệ, không bắt buộc mọi phiếu phải đi qua; không nhảy cóc ngoài máy trạng thái.
- Không cho phiếu `HOAN_TAT` quay lại.
- Không tính tiền bằng công thức Word cũ.
- Không tự trừ/cộng tồn kho.
- Không dùng `double` để làm tròn tiền tùy tiện; DB dùng `DECIMAL(18,0)` và view đã xử lý.
- Nếu luật trong Word khác DB, ghi nhận và hỏi; không tự nới lỏng trigger.

## Checklist

- [ ] Máy trạng thái lấy từ DB.
- [ ] Mỗi chuyển có actor, guard, side effect, mã lỗi.
- [ ] Bất biến được liệt kê.
- [ ] Biết phần nào Java kiểm, phần nào DB kiểm.
- [ ] Không lặp side effect.
- [ ] Có test chuyển hợp lệ/không hợp lệ.
- [ ] Có ghi nhận mâu thuẫn nếu có.

## Liên quan

- `02-architecture/database-transaction-integrity`
- `04-features/repair-order-workflow`
- `04-features/invoice-payment-promotion`
- `04-features/returns-warranty`
