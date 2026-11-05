---
name: swing-screen
description: Dùng khi tạo hoặc sửa một màn hình/form Java Swing trong package ui của CarZone. Không dùng cho logic controller/dao.
---

# Kỹ năng và Quy chuẩn xây dựng Giao diện Swing (CarZone UI)

> Đúc kết từ thực tế chuyển đổi Figma sang Java Swing, xử lý lỗi lệch layout, bo góc nút bấm và luồng Use Case hoàn chỉnh.

---

## 1. Look & Feel và Bảng màu chủ đạo

- **Theme**: CarZone dùng giao diện **Sáng (Light Theme)** theo Figma.
- **Cấu hình Look & Feel**: Cài đặt một lần duy nhất tại `app.App` trước khi mở bất kỳ cửa sổ nào:
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

## 3. Quy chuẩn Bo góc cho Nút bấm (Rounded Buttons) & Chống cắt chữ (Anti-clipping)

- Không dùng `BorderFactory.createLineBorder(..., true)` của Swing vì góc bo rất thô (chỉ 1–2px) và làm hỏng bo góc của FlatLaf.
- Triển khai lớp nút bo tròn chuẩn `RoundedButton` (hoặc FlatLaf client property `"JButton.arc"`) có cơ chế **đo đạc kích thước động (dynamic width calculation)** để bảo đảm không bao giờ bị cắt chữ:
  ```java
  public static class RoundedButton extends JButton {
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
      public Dimension getPreferredSize() {
          Dimension pref = super.getPreferredSize();
          Font f = getFont() != null ? getFont() : new Font("SansSerif", Font.BOLD, 13);
          FontMetrics fm = getFontMetrics(f);
          int textW = getText() != null ? fm.stringWidth(getText()) : 0;
          int iconW = getIcon() != null ? getIcon().getIconWidth() + getIconTextGap() : 0;
          int neededW = textW + iconW + 34; // 17px padding mỗi bên để không bao giờ bị cắt chữ
          int neededH = Math.max(pref != null ? pref.height : 38, 38);
          int finalW = pref != null ? Math.max(pref.width, neededW) : neededW;
          return new Dimension(finalW, neededH);
      }

      @Override
      public Dimension getMinimumSize() {
          return getPreferredSize();
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
- **Quy chuẩn kích thước nút bấm tránh mất chữ / hiện dấu `...`**:
  - Nút ngắn (Lưu, Hủy, Sửa, Xóa): tối thiểu `90px – 95px`.
  - Nút trung bình (Tìm kiếm, Làm mới, Tạo mới, Đổi mật khẩu, Xuất báo cáo): tối thiểu `100px – 140px`.
  - Nút văn bản dài ("Cập nhật phiếu sửa chữa"): tối thiểu **`220px`**.
  - Nút điều hướng quay lại ("Quay lại danh sách"): tối thiểu **`180px`**.
  - Nút chi tiết ("Xem chi tiết yêu cầu"): tối thiểu **`180px`**.
  - Nút `Help`: tối thiểu `96px`; `About`: tối thiểu `96px`.
- **Quy chuẩn Sidebar Navigation (Figma 83:7716)**:
  - Bề rộng Sidebar: **`256px`** (tránh co hẹp gây ép dòng text menu).
  - Nút menu Sidebar: **`236px × 48px`** (chiều cao 48–52px).
  - Nhãn HTML menu: tối thiểu **`width: 172px`** với `line-height: 1.2` để các tiêu đề phân hệ dài ("Yêu cầu cập nhật linh kiện", "Lập phiếu tiếp nhận – sửa chữa", "Hồ sơ khách hàng và xe") hiển thị hoàn hảo, không bị ngắt thành "Yêu cầu cậ...", "Yêu cầu sử...".
- **Quy chuẩn Hiển thị Trạng thái (Status Badge)**:
  - Bảng dữ liệu có cột Trạng thái phải dùng `StatusBadgeRenderer` căn giữa, bo góc tròn dạng pill:
    + `Hoàn tất` / `Đã duyệt`: nền `#ECFDF5`, chữ xanh lá `#059669`.
    + `Đang sửa` / `Đã nhập kho`: nền `#EFF6FF`, chữ xanh dương `#2563EB`.
    + `Chờ linh kiện` / `Chờ duyệt`: nền `#FFFBEB`, chữ cam `#D97706`.
    + `Mới phân công` / `Chờ sửa`: nền `#F1F5F9`, chữ xám `#64748B`.
    + `Từ chối`: nền `#FEF2F2`, chữ đỏ `#DC2626`.

---

## 4. Quy chuẩn Luồng xử lý Use Case trên Màn hình

Mỗi màn hình/form tương tác phải triển khai đầy đủ các bước của Use Case:

1. **Khởi tạo và Auto-focus**:
   - Khi cửa sổ mở (`windowOpened`), tự động focus con trỏ vào ô nhập đầu tiên (`tf.requestFocusInWindow()`).
2. **Tương tác bàn phím (Keyboard Navigation)**:
   - Nhấn `Enter` ở ô trên tự động chuyển sang ô dưới.
   - Nhấn `Enter` ở ô cuối cùng tự động kích hoạt nút hành động chính (Đăng nhập / Lưu / Xác nhận).
3. **Phản hồi lỗi thời gian thực**:
   - Validate sớm các trường bắt buộc / định dạng tại tầng UI trước khi gọi Controller.
   - Hiển thị lỗi trên khung thông báo đỏ/hồng chuẩn Figma (ví dụ: `pnlError`).
   - Lắng nghe sự kiện gõ phím (`DocumentListener`): Khi người dùng bắt đầu chỉnh sửa lại, tự động ẩn thông báo lỗi cũ để giao diện sạch sẽ.
4. **Trạng thái đang xử lý (Loading State) & Chống spam click**:
   - Khóa form (`setFormEnabled(false)`).
   - Đổi nhãn nút sang trạng thái đang chạy (ví dụ: `"Đang đăng nhập…"`, `"Đang lưu…"`).
   - Cập nhật thông điệp tiến trình trên Status Bar bên dưới.
5. **Xử lý bất đồng bộ bằng `SwingWorker`**:
   - Tác vụ gọi Controller / DB / mã hóa bắt buộc chạy trong `doInBackground()` của `SwingWorker`.
   - Tuyệt đối không gọi tác vụ nặng trực tiếp trên Event Dispatch Thread (EDT) làm treo giao diện.
6. **Hỗ trợ đầy đủ các nút phụ trợ**:
   - Nút `Help`: Modal hướng dẫn định dạng mã, quy ước nghiệp vụ của màn hình.
   - Nút `About`: Modal thông tin phần mềm CarZone.
   - Nút `Thoát`: Hỏi xác nhận (`JOptionPane.showConfirmDialog`) trước khi thoát.

---

## 5. Xử lý Logo và Icon

1. **Logo ứng dụng**:
   - File ảnh lưu tại `img/carzone_logo.png` (thư mục gốc, cùng cấp `src`; `pom.xml` copy `img/*.png` vào gốc classpath nên đọc bằng `getResource("/carzone_logo.png")`, không đọc được thì fallback đọc file `img/carzone_logo.png`).
   - Thu phóng khử răng cưa bằng `Image.SCALE_SMOOTH`.
   - **Luôn có Fallback**: Nếu không đọc được file ảnh, tự động vẽ vector thay thế bằng `Graphics2D` để màn hình không bao giờ bị rỗng hay ném ngoại lệ.
2. **Thêm ảnh mới (icon, minh họa, banner)**:
   - Chỉ đặt file `.png` vào thư mục gốc **`img/`**; `pom.xml` đã trỏ `<resources>` vào `img/` nên ảnh tự động nằm ở gốc classpath.
   - Đọc bằng `getClass().getResourceAsStream("/ten_anh.png")`; nếu không có trên classpath thì fallback đọc file `new java.io.File("img/ten_anh.png")` (chạy từ thư mục gốc repo) — đúng pattern của logo trong `MainFrame`, `DangNhapUI`, `KhoiDongUI`, `DashboardUI`.
   - **Cấm** tạo lại `src/resources/images/` hay bỏ `.png` vào `src/ui/`: cấu trúc này đã gỡ ngày 30/10/2026; tái tạo sẽ làm sai đường dẫn đang dùng và conflict với code của nhóm.
3. **Icon trên nút / tiêu đề**:
   - Ưu tiên vẽ bằng `Icon` vector tùy biến (anti-aliased) để hiển thị sắc nét trên mọi màn hình (Retina, 4K, scale DPI).

---

---

## 7. Quy chuẩn Ô chọn ngày `JDateChooser` (Anti-clipping & Lock keyboard)

- **Cấm nhập bàn phím, bắt buộc chọn từ lịch**:
  ```java
  if (chooser.getDateEditor() instanceof JTextFieldDateEditor editor) {
      editor.setEditable(false); // Bắt buộc chọn từ lịch
      editor.setBackground(Color.WHITE);
      editor.setFont(new Font("SansSerif", Font.PLAIN, 13));
      // BẮT BUỘC padding trái 10px để tránh góc bo 10px của RoundedLineBorder che mất ký tự đầu
      editor.setBorder(new EmptyBorder(0, 10, 0, 2));
      editor.setMargin(new Insets(0, 0, 0, 0));
  }
  ```
- **Kích thước chuẩn**:
  - Chiều rộng `JDateChooser`: tối thiểu **`170px`** (để chứa đủ chuỗi `dd/MM/yyyy`, nút lịch 28px và lề trong mà không bị cuộn ngang).
  - Chiều cao: chuẩn **`38px` – `42px`** khớp với các ComboBox và Input lân cận.

---

## 8. Quy chuẩn Kiểm soát Nhập số nguyên (`DocumentFilter` - Chặn gõ chữ tức thì)

- Không để người dùng nhập chữ tự do rồi mới báo lỗi khi bấm nút; BẮT BUỘC gắn `DocumentFilter` chặn trực tiếp các ký tự không phải số:
  ```java
  public static void chiChoPhepNhapSo(JTextField field) {
      ((javax.swing.text.AbstractDocument) field.getDocument()).setDocumentFilter(new javax.swing.text.DocumentFilter() {
          @Override
          public void insertString(FilterBypass fb, int offset, String string, javax.swing.text.AttributeSet attr)
                  throws javax.swing.text.BadLocationException {
              if (string != null && string.matches("\\d+")) {
                  super.insertString(fb, offset, string, attr);
              }
          }

          @Override
          public void replace(FilterBypass fb, int offset, int length, String text, javax.swing.text.AttributeSet attrs)
                  throws javax.swing.text.BadLocationException {
              if (text == null || text.isEmpty() || text.matches("\\d+")) {
                  super.replace(fb, offset, length, text, attrs);
              }
          }
      });
  }
  ```
- Áp dụng bắt buộc cho: ô số lượng dịch vụ, ô số lượng linh kiện, ô số lượng nhập kho, ô số lượng yêu cầu.

---

## 9. Cạm bẫy Bóp méo Bảng trong `BoxLayout` & Kỹ thuật Cố định Số dòng (Anti-squeeze JTable)

### ⚠️ Cạm bẫy ép bẹp JTable / JScrollPane xuống 1 dòng
- Trong `BoxLayout(Y_AXIS)`, nếu panel cha bị giới hạn không gian, Swing sẽ tự động bóp méo các component con xuống kích thước tối thiểu (`minimumSize`).
- Mặc định, `JScrollPane` có `minimumSize` rất nhỏ (~60px), khiến bảng bị co dúm lại chỉ thấy được đúng **1 dòng** hoặc bị che mất header!
- Ngoài ra, nếu đặt `maximumSize` cho `JTabbedPane` hoặc container chứa bảng bằng `child.getPreferredSize().height` lúc khởi tạo (khi chưa có dữ liệu), tab sẽ bị khóa trần chiều cao và không bao giờ nở ra được khi nạp kết quả tìm kiếm!

### 🛡️ Giải pháp chuẩn mực (BẮT BUỘC)
1. **Giải phóng trần chiều cao cho `tabs`**:
   ```java
   tabs.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
   ```
2. **Cố định số dòng hiển thị của JTable bằng cặp `setPreferredSize` + `setMinimumSize`**:
   - Công thức tính chiều cao: `H = headerHeight (42px) + (numRows * rowHeight (42px)) + đệm (10-12px)`.
   - **Bảng kết quả tìm kiếm (cần thấy 4 dòng)**:
     ```java
     int tblH = 220; // 42 + 4 * 42 + 10 = 220px
     resultScroll.setPreferredSize(new Dimension(980, tblH));
     resultScroll.setMinimumSize(new Dimension(0, tblH));
     resultCard.setPreferredSize(new Dimension(980, tblH + 16));
     resultCard.setMinimumSize(new Dimension(0, tblH + 16));
     resultCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, tblH + 16));
     ```
   - **Bảng chi tiết hạng mục (cần thấy 3 dòng)**:
     ```java
     int itemH = 180; // 42 + 3 * 42 + 12 = 180px
     itemScroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, itemH));
     itemScroll.setMinimumSize(new Dimension(0, itemH));
     itemScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, itemH));
     ```
   - Muốn xem thêm các dòng vượt quá số dòng hiển thị chuẩn, người dùng tự cuộn thanh cuộn của bảng.
3. **Cập nhật lại layout khi dữ liệu thay đổi**:
   Khi nạp chi tiết động (`setDetail`), gọi đồng thời:
   ```java
   builtComponent.revalidate();
   tabs.revalidate();
   root.revalidate();
   root.repaint();
   ```

---

## 10. Xử lý Cuộc đua Bất đồng bộ khi Điều hướng Chọn phiếu (`maPhieuChoSua`)

- **Vấn đề**: Khi bấm "Cập nhật phiếu" từ Dashboard hoặc màn hình ngoài, sự kiện chuyển trang và gọi `capNhatPhieuUI.datMaPhieuCanSua(maPhieu)` diễn ra ngay lập tức, nhưng danh sách `listPhieu` của form đích đang được nạp bất đồng bộ bởi `SwingWorker`. Nếu `listPhieu` chưa tải xong, việc tìm phiếu sẽ thất bại và form bị trống ("Chưa chọn phiếu").
- **Giải pháp hàng đợi (Queue Target ID)**:
  ```java
  private String maPhieuChoSua = null;

  public void datMaPhieuCanSua(String maPhieu) {
      if (maPhieu == null) return;
      this.maPhieuChoSua = maPhieu;
      if (listPhieu != null && !listPhieu.isEmpty()) {
          for (int i = 0; i < listPhieu.size(); i++) {
              if (maPhieu.equalsIgnoreCase(listPhieu.get(i).maPhieu())) {
                  cboChonPhieu.setSelectedIndex(i + 1);
                  this.maPhieuChoSua = null;
                  break;
              }
          }
      }
  }

  // Trong done() của SwingWorker nạp danh sách phiếu:
  if (maPhieuChoSua != null) {
      for (int i = 0; i < listPhieu.size(); i++) {
          if (maPhieuChoSua.equalsIgnoreCase(listPhieu.get(i).maPhieu())) {
              cboChonPhieu.setSelectedIndex(i + 1);
              maPhieuChoSua = null;
              break;
          }
      }
  }
  ```

---

## 11. Pattern Hộp thoại "Xem chi tiết" theo Ngữ cảnh Trạng thái

- Nút "Xem chi tiết" trong các bảng tóm tắt (ví dụ Dashboard) không nhảy thẳng vào form Sửa (tránh làm người dùng bối rối khi phiếu đã Hoàn tất).
- Mở modal dialog chi tiết (`JDialog`) với các thông tin cốt lõi (Mã phiếu, Trạng thái, Biển số xe & Hãng, Ngày tiếp nhận, Khách hàng & SĐT, KTV, Nội dung yêu cầu).
- **Nút hành động theo ngữ cảnh trạng thái**:
  - Phiếu chưa hoàn tất (`CHO_SUA`, `DANG_SUA`, `CHO_LINH_KIEN`): Nút chính là **`"Cập nhật phiếu này ↗"`** chuyển sang form sửa chữa.
  - Phiếu đã hoàn tất (`HOAN_TAT`): Nút chính là **`"Xem lịch sử sửa chữa ↗"`** chuyển sang tab tra cứu lịch sử (vì phiếu hoàn tất không được sửa đổi).
  - Nút phụ: **`"Đóng"`** giữ nguyên màn hình hiện tại.

---

## 13. Quy chuẩn Modal Dialog Chống Lệch Chữ & Căn Lề Đều Đặn (Anti-misalignment Dialog Pattern)

> Đúc kết từ lỗi dialog xác nhận phục hồi bị lệch thụt dòng và cắt chữ tiêu đề cảnh báo (`CẢNH BÁO QUAN TRỌNG VỀ PHỤC HỒ...`).

1. **Thay thế `JTextArea` bằng `RoundedPanel` chứa danh sách nhãn (`JLabel`)**:
   - `JTextArea` thường có lề mặc định không đồng nhất giữa các hệ điều hành và font rendering, dễ gây thụt dòng so le khi đặt cạnh các thành phần khác.
   - Để làm khung thẻ cảnh báo / thông tin: Dùng `RoundedPanel` với insets đồng nhất (ví dụ `new Insets(10, 16, 10, 16)`), bên trong dùng `BoxLayout(Y_AXIS)`. Mỗi gạch đầu dòng là một `JLabel` độc lập với icon chấm tròn hoặc text `• `, đảm bảo các dòng luôn thẳng tắp một hàng dọc bên trái.
2. **Padding bên trong cho ô nhập liệu (`JTextField`)**:
   - Mặc định, `JTextField` dán chữ sát mép trái, khiến chữ bị lệch trục đứng so với lề 16px của các khung thẻ phía trên.
   - BẮT BUỘC gán compound border: `new CompoundBorder(new RoundedLineBorder(...), new EmptyBorder(0, 12, 0, 12))` để có lề trong tối thiểu **12px**, giúp văn bản người dùng gõ vào thẳng hàng đẹp mắt với nội dung thẻ.
3. **Tiêu đề cảnh báo chống cắt chữ**:
   - Tiêu đề modal hoặc khung cảnh báo dài (như *"CẢNH BÁO QUAN TRỌNG VỀ PHỤC HỒI DỮ LIỆU"*): Bọc tiêu đề và icon trong một hàng `BorderLayout(10, 0)` hoặc `FlowLayout(FlowLayout.LEFT, 10, 0)` với chiều rộng khung dialog tối thiểu **`480px – 540px`**, tránh fix cứng kích thước nhãn gây cắt thành dấu ba chấm (`...`).
4. **Hàng nút xác nhận (Hủy / Thực hiện)**:
   - Dùng `BoxLayout(X_AXIS)` với `Box.createHorizontalGlue()` và `Box.createRigidArea(new Dimension(10, 0))` để nút luôn neo về góc phải và không bao giờ bị rớt dòng.

---

## 14. Quy chuẩn Phóng To Toàn Màn Hình Khi Đăng Nhập (Maximized Window on Login)

- **Trải nghiệm người dùng đồng nhất**: Sau khi đăng nhập thành công tại `DangNhapUI` hoặc từ `App.moManHinhChinh()`, cửa sổ chính (`MainFrame`) BẮT BUỘC được phóng to cực đại toàn màn hình:
  ```java
  mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
  mainFrame.setVisible(true);
  ```
- **Kỹ thuật chống vỡ bố cục khi thu nhỏ (Restore Down)**:
  - Vẫn phải khai báo đầy đủ kích thước mặc định và kích thước tối thiểu trước khi phóng to:
    ```java
    setSize(1170, 800); // Kích thước chuẩn khi thu nhỏ
    setMinimumSize(new Dimension(1024, 700)); // Ngưỡng tối thiểu chống co rúm
    setLocationRelativeTo(null);
    setExtendedState(JFrame.MAXIMIZED_BOTH);
    ```
  - Khi người dùng bấm nút Unmaximize / Restore Down của hệ điều hành, cửa sổ sẽ co về `1170×800` đặt ngay giữa màn hình mà không bị vỡ giao diện.

---

## 15. Quy chuẩn Icon Sidebar 3D & Tiêu Đề Trang Đồng Bộ (3D Sidebar & Page Icons)

1. **Hệ thống Icon 3D chuẩn hóa**:
   - Các mục chức năng lớn trên Sidebar Navigation sử dụng file ảnh 3D đặt tại thư mục `img/nav_*.png` (ví dụ `img/nav_sao_luu_phuc_hoi.png`).
   - Kích thước thu phóng hiển thị trên Sidebar: **`30×30px`** hoặc **`28×28px`** (sử dụng thuật toán `Image.SCALE_SMOOTH`).
   - Tiêu đề đầu mỗi trang nội dung (Page Title) tái sử dụng chính file ảnh 3D này (kích thước **`28×28px`** hoặc **`32×32px`**) để tạo sự liên kết thị giác trực tiếp với menu Sidebar.
2. **Fallback Vector Icon trên `MainFrame`**:
   - Luôn cung cấp lớp vector icon tùy biến kế thừa `Icon` bên trong `MainFrame` (ví dụ `DatabaseIcon`, `BackupIcon`, `RestoreIcon`, `RefreshIcon`, `AlertTriangleIcon`) để dự phòng khi chạy môi trường thiếu asset hoặc dùng cho các nút bấm phụ trợ.
3. **TUYỆT ĐỐI CẤM SỬ DỤNG EMOJI UNICODE**:
   - Tuyệt đối không dùng emoji Unicode (`💾`, `⚠️`, `🔄`, `⚙️`, `📊`, ...) trên nhãn, nút bấm hoặc bảng điều khiển. Emoji Unicode phụ thuộc vào font chữ hệ thống, bị lệch màu, biến dạng hoặc hiển thị thành ô vuông lỗi (`[]` / `?`) trên Windows/Linux. Mọi biểu tượng phải là ảnh PNG chuẩn hoặc Graphics2D vector.

---

## 16. Quy trình triển khai một Màn hình UI mới

1. **Mở Figma**, xác định Page, Frame và inspect mã màu HEX, kích thước, font size.
2. Không thêm breadcrumb/path (ví dụ `Dashboard / Tên trang`) vào màn hình; điều hướng hiện tại được thể hiện bằng Side Navigation và tiêu đề.
3. Tạo file trong package `ui`, hậu tố `UI` (ví dụ `DangNhapUI.java`, `QuanLyKhachHangUI.java`).
4. Khai báo bảng màu `Color` tĩnh bám sát Figma.
5. Lắp ráp khung lớn bằng `BorderLayout` (Header NORTH, Content CENTER, Status Bar SOUTH).
6. Sử dụng cấu trúc `INNER_WIDTH` và `BoxLayout` hoặc `GridBagLayout` đã chuẩn hóa ở trên.
7. Gắn kết nối sự kiện `bindActions()`, hỗ trợ phím `Enter`, DocumentListener và `SwingWorker`.
8. Biên dịch kiểm tra: `mvn compile`.
9. Chạy thử nghiệm và kiểm tra bằng mắt: `mvn exec:java`.
10. **Trước khi báo xong, kiểm tra conflict**: `git status` — file nào đang `UU` thì báo người dùng, không tự chọn bên, không tự `git add`; khi sửa màn hình nhiều người sửa (`MainFrame`, `DashboardUI`, `DangNhapUI`) chỉ đổi đúng đoạn cần, không format lại toàn file.
