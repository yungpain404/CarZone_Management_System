---
name: swing-ui-development
description: Dùng khi tạo hoặc sửa màn hình, form, bảng, dialog Java Swing trong CarZone theo Figma và FlatLaf, bao gồm bố cục, validation, trạng thái màn hình và quy ước đặt tên. Không dùng cho logic DAO hay nghiệp vụ.
---

# Phát triển giao diện Swing

## Mục đích

Tạo màn hình đúng Figma, đúng vai trò, không chứa nghiệp vụ, không block EDT.

## Bắt buộc đọc

- `ai-context/skills/swing-screen.md` (chi tiết layout, bảng màu, bo góc).
- `AGENTS.md` mục "Quy tắc thiết kế UI".
- `03-ui/role-navigation-dashboard` nếu là trang/menu.
- `03-ui/swing-threading-responsiveness` nếu có DB.

## Trước khi code

1. Mở Figma hoặc ảnh chụp màn hình tương ứng. Nếu không có, dừng và xin ảnh/mô tả; không tự sáng tác.
2. Xác định màn hình thuộc vai trò nào, menu ID nào.
3. Xác định dữ liệu vào/ra và controller sẽ gọi.
4. Xác định trạng thái: loading, rỗng, lỗi, thành công.
5. Xác định validation và thứ tự focus/Enter.

## Cấu trúc màn hình

- Kế thừa `JPanel` nếu là trang trong `CardLayout`; `JDialog` nếu là hộp thoại; `JFrame` chỉ cho cửa sổ độc lập.
- Tên lớp hậu tố `UI`.
- Dùng bảng màu và font từ `MainFrame` (C_BG, C_PRIMARY, ...) hoặc hằng số dùng chung; không tự đặt màu rải rác.
- Dùng `BorderLayout`/`BoxLayout`/`GridBagLayout` có chủ đích.
- Trong `BoxLayout(Y_AXIS)`, đặt `alignmentX` nhất quán cho mọi con trực tiếp.
- Hàng nút dùng `BoxLayout(X_AXIS)` + `RigidArea`, tránh `FlowLayout` làm rớt nút.
- Nút bo góc dùng `RoundedButton`/FlatLaf client property, không dùng `LineBorder` vuông.

## Thành phần

- Header/breadcrumb theo `MainFrame`.
- Toolbar tìm kiếm/lọc.
- Bảng dùng `DefaultTableModel` chỉ đọc nếu chỉ hiển thị; override `isCellEditable` trả `false`.
- Cột tiền định dạng `#,##0`; ngày `dd/MM/yyyy`; enum hiển thị tiếng Việt.
- Form nhập có label, placeholder, thông báo lỗi dưới trường.
- Dialog xác nhận cho hành động không hoàn tác (xóa, hoàn tất, thanh toán).

## Validation

- Validate ở UI để báo sớm, nhưng controller/DB vẫn phải kiểm.
- Không chỉ dựa vào `JOptionPane`; hiển thị lỗi cạnh trường.
- Xóa lỗi khi người dùng gõ lại.
- Focus vào trường lỗi đầu tiên.
- Enter chuyển trường; Enter ở trường cuối submit.
- Không cho submit khi đang chạy.

## Trạng thái

- Loading: khóa nút, hiện progress/skeleton, không đóng form.
- Rỗng: thông báo "Chưa có dữ liệu", không hiện bảng trắng.
- Lỗi: thông báo thân thiện, giữ dữ liệu đã nhập.
- Thành công: thông báo ngắn, refresh bảng, reset form nếu phù hợp.
- Không mock dữ liệu để lấp màn hình.

## Quy tắc

- Không SQL, không `DbUtil`, không tính tiền trong UI.
- Không gọi DAO trực tiếp; gọi controller.
- Không chạy truy vấn trên EDT.
- Không hard-code dữ liệu mẫu.
- Không dùng `JFrame` lồng nhau cho mỗi trang; dùng `CardLayout`.
- Không tự đổi theme sang dark.
- Không dùng emoji/icon lạ nếu Figma không có.

## Checklist

- [ ] Đúng Figma (màu, font, khoảng cách, kích thước).
- [ ] Đúng vai trò/menu.
- [ ] Có loading/rỗng/lỗi/thành công.
- [ ] Có validation và focus/Enter.
- [ ] Không SQL/nghiệp vụ trong UI.
- [ ] Tác vụ nền dùng `SwingWorker`.
- [ ] Bảng chỉ đọc khi chỉ hiển thị.
- [ ] Định dạng tiền/ngày đúng.
- [ ] Tên lớp hậu tố `UI`.

## Liên quan

- `03-ui/role-navigation-dashboard`
- `03-ui/swing-threading-responsiveness`
- skill cũ `ai-context/skills/swing-screen.md`
