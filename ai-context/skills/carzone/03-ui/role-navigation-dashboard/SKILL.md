---
name: role-navigation-dashboard
description: Dùng khi thêm/sửa menu, điều hướng, dashboard hoặc trang mặc định theo vai trò trong CarZone. Bao gồm ánh xạ menu ID với CardLayout, KPI theo vai trò và chặn truy cập trái quyền.
---

# Điều hướng và dashboard theo vai trò

## Nguồn

- `controller.AuthController.layDanhSachNavigationTheoRole`
- `session.UserSession`
- `ui.MainFrame` (khung chính theo `AGENTS.md`)
- `ui.DashboardUI` (đang trùng lặp, cần hợp nhất)

## Menu chuẩn

| Vai trò | Menu ID |
|---|---|
| CCH | `NAV_DASHBOARD`, `NAV_THONG_KE`, `NAV_QUAN_LY_DICH_VU`, `NAV_LINH_KIEN_KHO`, `NAV_YC_LINH_KIEN`, `NAV_KHUYEN_MAI`, `NAV_NHAN_VIEN`, `NAV_TAI_KHOAN`, `NAV_TRA_CUU`, `NAV_THONG_TIN_CA_NHAN` |
| TNV | `NAV_DASHBOARD`, `NAV_KHACH_HANG_XE`, `NAV_PHIEU_TIEP_NHAN`, `NAV_HOA_DON`, `NAV_DOI_TRA`, `NAV_TRA_CUU`, `NAV_THONG_TIN_CA_NHAN` |
| KTV | `NAV_DASHBOARD`, `NAV_PHIEU_PHAN_CONG`, `NAV_CAP_NHAT_SUA_CHUA`, `NAV_YC_LINH_KIEN`, `NAV_TRA_CUU`, `NAV_THONG_TIN_CA_NHAN` |

Không thêm menu mới mà không cập nhật: `AuthController`, khung chính, `CardLayout`, skill này, test điều hướng.

## Các bước thêm trang

1. Xác định menu ID và vai trò.
2. Tạo lớp `XxxUI` là `JPanel`.
3. Đăng ký vào `CardLayout` với đúng key.
4. Thêm `NavigationItem` vào danh sách role.
5. Bảo đảm bấm menu đổi card và cập nhật trạng thái active.
6. Kiểm tra role khác không thấy menu và không mở được card bằng mã.
7. Kiểm tra resize 1200×760 và 1440×920.

## Dashboard

- Dữ liệu lấy qua controller/DAO, không SQL trong UI.
- CCH: doanh thu, hoàn tiền, doanh thu thuần, số hóa đơn, tồn kho thấp, yêu cầu chờ duyệt, nhân viên chưa có tài khoản.
- TNV: phiếu chờ sửa/đang sửa/chờ linh kiện/hoàn tất, danh sách phiếu gần đây.
- KTV: phiếu được giao, đang làm, chờ linh kiện, hoàn tất, yêu cầu linh kiện.
- KPI phải có định nghĩa rõ (nguồn bảng/view, bộ lọc ngày, bộ lọc `maNV`).
- Không trộn số liệu toàn cửa hàng vào dashboard TNV/KTV.

## Chặn truy cập

- Không chỉ ẩn nút; kiểm tra lại khi mở trang.
- Không nhận menu ID/role từ tham số không tin cậy.
- Nếu session rỗng, chuyển về đăng nhập; không tạo nhân viên mặc định.
- Đăng xuất phải đóng khung chính và mở lại đăng nhập.

## Nợ kỹ thuật

- `MainFrame` và `DashboardUI` trùng chức năng. Chọn `MainFrame` làm khung chính, chuyển dashboard thành panel; không phát triển song song hai bản.
- `DashboardUI` có truy vấn SQL trực tiếp; chuyển sang DAO.
- `MainFrame`/`DashboardUI` có `taoNhanVienMacDinh()`; phải bỏ.

## Checklist

- [ ] Menu đúng role, đúng số lượng.
- [ ] Menu ID khớp key `CardLayout`.
- [ ] Active state đúng.
- [ ] Dashboard đúng phạm vi dữ liệu.
- [ ] KPI có định nghĩa.
- [ ] Không SQL trong UI.
- [ ] Không fallback nhân viên mặc định.
- [ ] Role khác không mở được trang.
- [ ] Resize không vỡ layout.

## Liên quan

- `01-analysis/role-authorization`
- `03-ui/swing-ui-development`
- `04-features/search-statistics`
- `AuthController.java`
