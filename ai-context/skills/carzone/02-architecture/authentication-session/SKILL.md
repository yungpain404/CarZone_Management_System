---
name: authentication-session
description: Dùng khi làm đăng nhập, đăng xuất, tạo tài khoản, đặt lại mật khẩu, quản lý phiên UserSession hoặc cần bảo đảm chỉ người đã xác thực mới vào CarZone.
---

# Xác thực và phiên làm việc

## Luồng hiện tại

```text
DangNhapUI -> DangNhapController -> AuthController
           -> TaiKhoanDao.timTheoTenDangNhap
           -> so khớp SHA-256
           -> UserSession.batDauPhien(taiKhoan, nhanVien)
           -> mở MainFrame
```

Đăng xuất: `AuthController.dangXuat` xác nhận, `UserSession.huyPhien()`, đóng frame, mở lại `DangNhapUI`.

## Quy tắc xác thực

- Tên đăng nhập chuẩn hóa `trim().toUpperCase()`, đúng `^(CCH|TNV|KTV)\d{3}$`.
- Tài khoản phải `trangThaiHoatDong = true`.
- Mật khẩu so khớp hash SHA-256 hex in hoa; không so plaintext.
- Sai tên đăng nhập hoặc sai mật khẩu trả cùng một thông báo chung.
- Không log mật khẩu, hash, hay token.
- Không trả mật khẩu trong `toString`.
- Không hard-code tài khoản/mật khẩu trong code.
- Không tạo nhân viên mặc định khi chưa đăng nhập.

## Phiên

- `UserSession` là Singleton, giữ `TaiKhoan`, `NhanVien`, `thoiDiemDangNhap`.
- Chỉ controller/UI đọc session; không truyền `VaiTro`/`maNV` từ form.
- Sau đăng xuất phải xóa cả hai đối tượng; không chỉ ẩn cửa sổ.
- Mọi màn hình nghiệp vụ kiểm tra `daDangNhap()` trước khi thao tác.
- Không lưu session vào file/DB.
- Không dùng session để cache dữ liệu nghiệp vụ lâu dài.

## Tạo tài khoản

- Chỉ CCH.
- Nhân viên phải tồn tại.
- Tiền tố tên đăng nhập khớp vai trò; DB trigger `trg_TaiKhoan_KiemTra` chặn nếu sai.
- Mã tài khoản do `sp_TaoTaiKhoan` sinh.
- Mật khẩu tối thiểu 8 ký tự, có chữ hoa, chữ thường, số, ký tự đặc biệt.
- Không gửi mật khẩu qua log/thông báo.
- Không tạo tài khoản trùng nhân viên (PK `maNV`).

## Đặt lại mật khẩu

- Kiểm tra quyền và danh tính theo quy trình hiện có (`PasswordResetDao`).
- Không đặt mật khẩu mặc định dùng chung.
- Không trả mật khẩu mới qua màn hình công khai.
- Nếu chưa có quy trình được duyệt: dừng, hỏi.

## Nợ bảo mật đã biết

- SHA-256 không salt: giữ theo quyết định nhóm, không tự đổi. Ghi rõ trong báo cáo.
- `DbUtil` hard-code username `sa`: không nhân rộng; đề xuất đưa vào cấu hình khi có dịp.
- `.env` nằm trong git theo quyết định nhóm: không thêm bí mật thật.

## Checklist

- [ ] Mật khẩu không lưu/không log plaintext.
- [ ] Sai đăng nhập không tiết lộ tài khoản tồn tại.
- [ ] Tài khoản bị vô hiệu hóa bị chặn.
- [ ] Session lấy từ Singleton, không từ form.
- [ ] Đăng xuất xóa phiên.
- [ ] Không có fallback nhân viên mặc định.
- [ ] Tạo tài khoản đúng quyền và đúng tiền tố.
- [ ] Có test đăng nhập đúng/sai/bị khóa.

## Liên quan

- `01-analysis/role-authorization`
- `04-features/crud-master-data`
- `03-ui/role-navigation-dashboard`
- `src/session/UserSession.java`
- `src/controller/AuthController.java`
