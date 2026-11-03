---
name: role-authorization
description: Dùng khi thêm hoặc sửa chức năng có phân quyền, menu theo vai trò, hoặc dữ liệu chỉ được xem theo nhân viên trong CarZone. Bao gồm kiểm tra quyền ở UI, controller và database, không chỉ ẩn nút.
---

# Phân quyền theo vai trò

## Mục đích

Bảo đảm mỗi hành động chỉ được thực hiện bởi đúng vai trò, đúng phạm vi dữ liệu, và được chặn ở nhiều tầng.

## Nguồn

- `enums.VaiTro`
- `session.UserSession`
- `controller.AuthController.layDanhSachNavigationTheoRole`
- `ai-context/skills/carzone/ROLE_USECASE_MATRIX.md`
- Trigger/procedure trong V1

## Các bước

1. Xác định vai trò được phép cho từng hành động (xem, tạo, sửa, xóa, duyệt, thanh toán, hoàn tất).
2. Xác định phạm vi dữ liệu:
   - CCH: toàn bộ.
   - TNV: bản ghi có `maNVTiepNhan`/`maNV` là của mình.
   - KTV: bản ghi có `maNVKyThuat`/`maNVLap` là của mình.
3. Thiết kế chặn ở UI: ẩn menu/nút, không mở trang.
4. Thiết kế chặn ở controller: đọc `UserSession`, trả lỗi nếu sai vai trò hoặc sai chủ sở hữu.
5. Thiết kế chặn ở DAO: thêm điều kiện `maNV` vào query cho TNV/KTV.
6. Đối chiếu trigger/procedure: không thiết kế luồng mà DB chắc chắn chặn.
7. Viết test quyền cho từng vai trò, gồm cả trường hợp cố tình gọi sai.

## Bảng quyền tối thiểu

| Hành động | CCH | TNV | KTV |
|---|---|---|---|
| Tạo/sửa/xóa dịch vụ, linh kiện | Có | Không | Không |
| Tạo nhân viên/tài khoản | Có | Không | Không |
| Tạo khuyến mại | Có | Không | Không |
| Duyệt/nhập yêu cầu linh kiện | Có | Không | Không |
| Tạo khách hàng/xe | Không | Có | Không |
| Lập phiếu tiếp nhận | Không | Có | Không |
| Cập nhật phiếu | Không | Có | Chỉ phiếu được giao |
| Lập hóa đơn/thanh toán | Không | Có | Không |
| Lập phiếu đổi trả | Không | Có | Chưa chốt |
| Tạo yêu cầu nhập linh kiện | Không | Không | Có |
| Xem doanh thu toàn cửa hàng | Có | Không | Không |
| Xem thống kê của mình | Có (toàn bộ) | Có | Có |

## Mẫu kiểm tra trong controller

```java
VaiTro vaiTro = UserSession.getInstance().getVaiTro();
String maNV = UserSession.getInstance().getMaNV();
if (vaiTro != VaiTro.TIEP_NHAN_VIEN) {
    return KetQua.thatBai("Bạn không có quyền thực hiện thao tác này.");
}
```

Không dùng tham số `vaiTro` do UI truyền vào. Không đọc từ biến static tự tạo.

## Quy tắc

- Ẩn nút không phải bảo vệ.
- Không tin `maNV` do form gửi lên; lấy từ session.
- Không hiển thị dữ liệu người khác rồi mới chặn thao tác.
- Không mở quyền CCH cho vai trò khác để "tiện test".
- Không tạo tài khoản/nhân viên mặc định khi chưa đăng nhập.
- Nếu quyền trong tài liệu khác DB, ghi vào `KNOWN_DISCREPANCIES.md` và hỏi.

## Checklist

- [ ] Có bảng hành động × vai trò.
- [ ] Có điều kiện sở hữu dữ liệu.
- [ ] UI ẩn đúng.
- [ ] Controller chặn lại.
- [ ] DAO lọc theo `maNV`.
- [ ] DB trigger/procedure không bị vượt.
- [ ] Có test cho cả ba vai trò.
- [ ] Không có fallback nhân viên mặc định.
- [ ] Không lộ dữ liệu qua tìm kiếm/dashboard.

## Lỗi thường gặp

- KTV sửa `maPhieu` trên form để mở phiếu người khác.
- TNV xem dashboard toàn bộ phiếu thay vì phiếu mình lập.
- CCH xuất hiện trong luồng lập phiếu/hóa đơn.
- Chỉ kiểm tra role ở `MainFrame`, không kiểm tra ở controller.
- Dùng `CCH001` mặc định khi session rỗng.

## Liên quan

- `02-architecture/authentication-session`
- `03-ui/role-navigation-dashboard`
- `04-features/search-statistics`
- `ROLE_USECASE_MATRIX.md`
