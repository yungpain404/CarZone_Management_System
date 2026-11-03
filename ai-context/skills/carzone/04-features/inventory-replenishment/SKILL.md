---
name: inventory-replenishment
description: Dùng cho UC006 và UC009 khi quản lý linh kiện, tạo yêu cầu nhập, duyệt/từ chối, nhập kho, cảnh báo tồn thấp hoặc xử lý tồn kho trong CarZone.
---

# Linh kiện và yêu cầu nhập kho

## Phạm vi

- UC006: danh mục linh kiện, giá, trạng thái kinh doanh.
- UC009: KTV tạo yêu cầu nhập; CCH duyệt/từ chối/nhập kho.

## Vai trò

- CCH: tạo/sửa/ngừng kinh doanh linh kiện; duyệt, từ chối, xác nhận nhập kho.
- KTV: tạo yêu cầu nhập; xem yêu cầu của mình.
- TNV: không quản lý kho.

## Trạng thái yêu cầu

```text
CHO_DUYET -> DA_DUYET | TU_CHOI
DA_DUYET -> DA_NHAP_KHO
```

Quy tắc:

- Tạo mới luôn `CHO_DUYET`.
- Chỉ KTV được tạo (`trg_PYCNLK_KiemTra`, lỗi 50191).
- Chỉ CCH được xử lý (`maNVXuLy`), lỗi 50192.
- Khi rời `CHO_DUYET`, bắt buộc có `maNVXuLy`, `ngayXuLy`.
- Không nhảy từ `CHO_DUYET` sang `DA_NHAP_KHO`.
- Không từ chối sau khi đã duyệt.
- `DA_NHAP_KHO` là cuối.
- Tồn kho chỉ cộng khi chuyển sang `DA_NHAP_KHO`, đúng một lần, do trigger.
- Không cộng tồn ở Java.

## Tạo yêu cầu

- Chọn linh kiện đang kinh doanh.
- `soLuongYeuCau > 0`.
- `ghiChu` bắt buộc (DB NOT NULL), tối đa 300.
- Gọi `sp_TaoYeuCauNhapLinhKien` để sinh `maYeuCau`.
- Không tự sinh mã.
- Hiển thị tồn hiện tại và gợi ý số lượng.

## Duyệt / từ chối / nhập kho

- Kiểm tra quyền CCH.
- Kiểm tra trạng thái hiện tại.
- Khi duyệt: ghi `maNVXuLy`, `ngayXuLy`, `ghiChuXuLy` nếu có.
- Khi từ chối: bắt buộc lý do theo quy ước UI/controller; DB không có CHECK bắt buộc `ghiChuXuLy`.
- Khi nhập kho: chỉ từ `DA_DUYET`; xác nhận; sau đó tồn kho tăng.
- Không sửa `soLuongYeuCau` sau khi đã duyệt: DB hiện không chặn, controller/UI phải chặn.
- Không xóa yêu cầu đã xử lý: DB hiện không chặn, controller/UI phải chặn.

## Quản lý linh kiện

- `maLK` chưa có sequence/default/procedure sinh mã (D19); không tự sinh ở Java khi chưa chốt. Không sửa mã sau khi tạo.
- `tenLK` duy nhất, `donGia > 0`.
- `soLuongTon` chỉ đọc trên UI; không cho nhập tay.
- Ngừng kinh doanh bằng `dangKinhDoanh = 0`; không xóa linh kiện đã dùng.
- Cảnh báo tồn thấp: `soLuongTon <= 0` và `dangKinhDoanh = 1`.

## UI

- Danh sách linh kiện có tìm kiếm, lọc trạng thái, cột tồn.
- Form tạo yêu cầu từ màn hình linh kiện hoặc màn hình KTV.
- Màn hình duyệt có chi tiết yêu cầu, tồn hiện tại, nút duyệt/từ chối/nhập.
- Hiển thị trạng thái bằng tiếng Việt.
- Không cho bấm nhập kho hai lần.

## Transaction và lỗi

- Mỗi chuyển trạng thái là một UPDATE có điều kiện hoặc procedure.
- Kiểm tra `trangThai` cũ trước khi update để tránh lost update.
- Dịch lỗi 50191–50194.

## Checklist

- [ ] Chỉ KTV tạo yêu cầu.
- [ ] Chỉ CCH xử lý.
- [ ] Trạng thái đúng luồng.
- [ ] Tồn chỉ cộng khi `DA_NHAP_KHO`, một lần.
- [ ] Không sửa tồn trực tiếp.
- [ ] Mã do procedure sinh.
- [ ] Không xóa linh kiện đã dùng.
- [ ] Có test duyệt/từ chối/nhập và chuyển sai.

## Liên quan

- `04-features/repair-order-workflow`
- `04-features/crud-master-data`
- `02-architecture/database-transaction-integrity`
