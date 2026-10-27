---
name: swing-screen
description: Dùng khi tạo hoặc sửa một màn hình/form Java Swing trong package ui của CarZone. Không dùng cho logic service/dao.
---

# Kỹ năng và Quy chuẩn xây dựng Giao diện Swing (CarZone UI)

> Đúc kết từ thực tế chuyển đổi Figma sang Java Swing, xử lý lỗi lệch layout, bo góc nút bấm và luồng Use Case hoàn chỉnh.

---

## 1. Look & Feel và Bảng màu chủ đạo

- **Theme**: CarZone dùng giao diện **Sáng (Light Theme)** theo Figma.
- **Cấu hình Look & Feel**: Cài đặt một lần duy nhất tại `ui.Main` trước khi mở bất kỳ cửa sổ nào:
  ```java
  FlatLightLaf.setup(); // Tuyệt đối KHÔNG dùng FlatDarkLaf
  UIManager.put("Button.arc", 12);
  UIManager.put("Component.arc", 12);
  UIManager.put("TextComponent.arc", 12);
  UIManager.put("defaultFont", new Font("SansSerif", Font.PLAIN, 13));
  ```
- **Bảng màu chuẩn từ Figma (Inspect HEX)**:
  - `C_BG` = `#F8FAFC`: Nền toàn màn hình, thanh trạng thái (Status Bar).
  - `C_WHITE` = `#FFFFFF`: Nền Card, Header, nền input.
  - `C_CARD_STROKE` = `#E2E8F0`: Viền Card, viền Header, viền Status Bar.
  - `C_INPUT_STROKE` = `#CBD5E1`: Viền ô nhập liệu, viền checkbox, viền nút phụ.
  - `C_PRIMARY` = `#2563EB`: Màu xanh chủ đạo (nút chính, link, icon Help/khiên).
  - `C_PRIMARY_HV` = `#1D4ED8`: Màu hover nút chính.
  - `C_TEXT_HEAD` = `#202A36`: Tiêu đề lớn, tên form, chữ đậm.
  - `C_TEXT_LABEL` = `#334155`: Nhãn trường nhập, chữ nút phụ.
  - `C_TEXT_SUBTLE` = `#64748B`: Dòng phụ header, text status bar.
  - `C_PLACEHOLDER` = `#94A3B8`: Màu gợi ý trong ô nhập liệu.
  - `C_ERR_BG` = `#FDECEC`, `C_ERR_STROKE` = `#E7B7B7`, `C_ERR_TEXT` = `#D12828`: Khung thông báo lỗi.
  - `C_HELP_BG` = `#EFF6FF`, `C_HELP_STROKE` = `#BFDBFE`: Nút Help/Trợ giúp.

---

## 2. Kỹ thuật Layout & Cảnh báo tránh lỗi lệch giao diện

### ⚠️ Cạm bẫy chết người của `BoxLayout(Y_AXIS)` (Swing Alignment Trap)
- **Tuyệt đối KHÔNG ĐƯỢC TRỘN LẪN** `alignmentX = 0.5f` (CENTER) và `0.0f` (LEFT) giữa các thành phần con trực tiếp trong cùng một `BoxLayout(Y_AXIS)`.
- **Hậu quả nếu vi phạm**: Swing sẽ căn lề trái của component `0.0f` trùng với **tọa độ tâm** của component `0.5f`, làm toàn bộ phần form bên dưới bị thụt lùi sang phải 50% bề rộng card, khiến giao diện lệch lạc hoàn toàn!
- **Giải pháp chuẩn**:
  1. Gán `child.setAlignmentX(Component.CENTER_ALIGNMENT)` (hoặc `0.5f`) cho **MỌI** thành phần con trực tiếp trong `card`.
  2. Xác định `INNER_WIDTH = CARD_WIDTH - (paddingLeft + paddingRight)` (ví dụ: `480 - 56 = 424px`).
  3. Mọi panel con trực tiếp đều set `setPreferredSize(new Dimension(INNER_WIDTH, h))` và `setMaximumSize(new Dimension(INNER_WIDTH, h))`.
  4. Việc căn lề trái/phải bên trong được ủy quyền cho layout nội bộ của panel con đó (dùng `BoxLayout(X_AXIS)`, `BorderLayout`, hoặc `FlowLayout`).

### ⚠️ Cạm bẫy của `FlowLayout` (Tràn dòng và rớt nút)
- `FlowLayout(align, hgap, vgap)` tự động cộng thêm margin `hgap` ở cả hai mép ngoài cùng.
- Nếu đặt `width1 + width2 + hgap == panelWidth`, `FlowLayout` sẽ bị quá tải (do thừa `2 * hgap` ở mép) và tự động đẩy nút thứ hai rớt xuống dòng dưới!
- **Giải pháp**: Với hàng nút bấm hoặc hàng có khoảng cách cố định, ưu tiên dùng `BoxLayout(X_AXIS)` kết hợp `Box.createRigidArea(new Dimension(gap, 0))` để nút luôn nằm cùng một hàng và không bao giờ rớt dòng.

---

## 3. Quy chuẩn Bo góc cho Nút bấm (Rounded Buttons)

- Không dùng `BorderFactory.createLineBorder(..., true)` của Swing vì góc bo rất thô (chỉ 1–2px) và làm hỏng bo góc của FlatLaf.
- Triển khai lớp nút bo tròn chuẩn `RoundedButton` (hoặc FlatLaf client property `"JButton.arc"`):
  ```java
  private static class RoundedButton extends JButton {
      private final int arc;
      private final Color normalBg, hoverBg, strokeColor;

      public RoundedButton(String text, int arc, Color bg, Color hoverBg, Color strokeColor) {
          super(text);
          this.arc = arc;
          this.normalBg = bg;
          this.hoverBg = hoverBg;
          this.strokeColor = strokeColor;
          setContentAreaFilled(false);
          setFocusPainted(false);
          setBorderPainted(false);
          setOpaque(false);
          setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      }

      @Override
      protected void paintComponent(Graphics g) {
          Graphics2D g2 = (Graphics2D) g.create();
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          Color bg = isEnabled() ? (getModel().isPressed() ? hoverBg.darker() 
                                 : (getModel().isRollover() ? hoverBg : normalBg)) 
                                 : new Color(normalBg.getRed(), normalBg.getGreen(), normalBg.getBlue(), 150);
          g2.setColor(bg);
          g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
          if (strokeColor != null) {
              g2.setColor(strokeColor);
              g2.setStroke(new BasicStroke(1f));
              g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
          }
          g2.dispose();
          super.paintComponent(g);
      }
  }
  ```
- **Không để chữ bị tràn / cụt**:
  - Tính toán padding (`EmptyBorder`) và `preferredSize` rộng rãi cho các nút có icon + text như `Help` (tối thiểu `86px`), `About` (tối thiểu `92px`).

---

## 4. Quy chuẩn Luồng xử lý Use Case trên Màn hình

Mỗi màn hình/form tương tác phải triển khai đầy đủ các bước của Use Case:

1. **Khởi tạo và Auto-focus**:
   - Khi cửa sổ mở (`windowOpened`), tự động focus con trỏ vào ô nhập đầu tiên (`tf.requestFocusInWindow()`).
2. **Tương tác bàn phím (Keyboard Navigation)**:
   - Nhấn `Enter` ở ô trên tự động chuyển sang ô dưới.
   - Nhấn `Enter` ở ô cuối cùng tự động kích hoạt nút hành động chính (Đăng nhập / Lưu / Xác nhận).
3. **Phản hồi lỗi thời gian thực**:
   - Validate sớm các trường bắt buộc / định dạng tại tầng UI trước khi gọi Service.
   - Hiển thị lỗi trên khung thông báo đỏ/hồng chuẩn Figma (ví dụ: `pnlError`).
   - Lắng nghe sự kiện gõ phím (`DocumentListener`): Khi người dùng bắt đầu chỉnh sửa lại, tự động ẩn thông báo lỗi cũ để giao diện sạch sẽ.
4. **Trạng thái đang xử lý (Loading State) & Chống spam click**:
   - Khóa form (`setFormEnabled(false)`).
   - Đổi nhãn nút sang trạng thái đang chạy (ví dụ: `"Đang đăng nhập…"`, `"Đang lưu…"`).
   - Cập nhật thông điệp tiến trình trên Status Bar bên dưới.
5. **Xử lý bất đồng bộ bằng `SwingWorker`**:
   - Tác vụ gọi Service / DB / mã hóa bắt buộc chạy trong `doInBackground()` của `SwingWorker`.
   - Tuyệt đối không gọi tác vụ nặng trực tiếp trên Event Dispatch Thread (EDT) làm treo giao diện.
6. **Hỗ trợ đầy đủ các nút phụ trợ**:
   - Nút `Help`: Modal hướng dẫn định dạng mã, quy ước nghiệp vụ của màn hình.
   - Nút `About`: Modal thông tin phần mềm CarZone.
   - Nút `Thoát`: Hỏi xác nhận (`JOptionPane.showConfirmDialog`) trước khi thoát.

---

## 5. Xử lý Logo và Icon

1. **Logo ứng dụng**:
   - File ảnh lưu tại `src/resources/images/carzone_logo.png` (và đã cấu hình trong `pom.xml`).
   - Thu phóng khử răng cưa bằng `Image.SCALE_SMOOTH`.
   - **Luôn có Fallback**: Nếu không đọc được file ảnh, tự động vẽ vector thay thế bằng `Graphics2D` để màn hình không bao giờ bị rỗng hay ném ngoại lệ.
2. **Icon trên nút / tiêu đề**:
   - Ưu tiên vẽ bằng `Icon` vector tùy biến (anti-aliased) để hiển thị sắc nét trên mọi màn hình (Retina, 4K, scale DPI).

---

## 6. Quy trình triển khai một Màn hình UI mới

1. **Mở Figma**, xác định Page, Frame và inspect mã màu HEX, kích thước, font size.
2. Tạo file trong package `ui`, hậu tố `UI` (ví dụ `DangNhapUI.java`, `QuanLyKhachHangUI.java`).
3. Khai báo bảng màu `Color` tĩnh bám sát Figma.
4. Lắp ráp khung lớn bằng `BorderLayout` (Header NORTH, Content CENTER, Status Bar SOUTH).
5. Sử dụng cấu trúc `INNER_WIDTH` và `BoxLayout` hoặc `GridBagLayout` đã chuẩn hóa ở trên.
6. Gắn kết nối sự kiện `bindActions()`, hỗ trợ phím `Enter`, DocumentListener và `SwingWorker`.
7. Biên dịch kiểm tra: `mvn compile`.
8. Chạy thử nghiệm và kiểm tra bằng mắt: `mvn exec:java`.
