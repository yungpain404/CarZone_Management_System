package ui;

import audit.SecurityConfig;
import audit.ThongTinViPham;
import ui.MainFrame.AlertTriangleIcon;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Modal Dialog cảnh báo an ninh cấp 2 và yêu cầu nhập mã PIN bảo mật.
 * Thiết kế chuẩn FlatLightLaf theo bảng màu Figma và quy tắc chống lệch chữ.
 */
public class PinVerificationDialog extends JDialog {

    private final ThongTinViPham viPham;
    private final int soLanThuConLai;
    private boolean daXacNhan = false;

    private JPasswordField txtPin;
    private JLabel lblError;
    private JLabel lblAttempts;

    public PinVerificationDialog(Frame parent, ThongTinViPham viPham, int soLanThuConLai) {
        super(parent, "Xác thực bảo mật cấp 2 - CarZone", true);
        this.viPham = viPham;
        this.soLanThuConLai = soLanThuConLai;

        khoiTaoGiaoDien();
    }

    private void khoiTaoGiaoDien() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        setSize(520, 420);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());

        JPanel pnlRoot = new JPanel();
        pnlRoot.setLayout(new BoxLayout(pnlRoot, BoxLayout.Y_AXIS));
        pnlRoot.setBackground(Color.WHITE);
        pnlRoot.setBorder(new EmptyBorder(20, 24, 20, 24));

        // 1. Tiêu đề cảnh báo đỏ rực
        JPanel pnlTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlTitle.setOpaque(false);
        pnlTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel lblIcon = new JLabel(new AlertTriangleIcon(new Color(0xDC, 0x26, 0x26), 22));
        JLabel lblTitle = new JLabel("CẢNH BÁO AN NINH: PHÁT HIỆN HÀNH VI BẤT THƯỜNG");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblTitle.setForeground(new Color(0xDC, 0x26, 0x26));
        pnlTitle.add(lblIcon);
        pnlTitle.add(lblTitle);
        pnlRoot.add(pnlTitle);
        pnlRoot.add(Box.createVerticalStrut(12));

        // 2. Thẻ chi tiết hành vi phi lý (RoundedPanel viền đỏ nhạt)
        RoundedPanel cardAlert = new RoundedPanel(new Color(0xFE, 0xF2, 0xF2), 10, new Color(0xFE, 0xCA, 0xCA), new Insets(10, 16, 10, 16));
        cardAlert.setLayout(new BoxLayout(cardAlert, BoxLayout.Y_AXIS));
        cardAlert.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardAlert.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        Color cAlertDark = new Color(0x99, 0x1B, 0x1B);

        JLabel lblLoai = new JLabel("• Nhóm vi phạm: " + viPham.loai().getMoTa());
        lblLoai.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblLoai.setForeground(cAlertDark);
        lblLoai.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblChiTiet = new JLabel("<html>• Chi tiết: " + viPham.chiTiet() + "</html>");
        lblChiTiet.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblChiTiet.setForeground(cAlertDark);
        lblChiTiet.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblLuuY = new JLabel("• Hệ thống đã tạm dừng tiến trình để ngăn ngừa nguy cơ phá hoại dữ liệu.");
        lblLuuY.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblLuuY.setForeground(cAlertDark);
        lblLuuY.setAlignmentX(Component.LEFT_ALIGNMENT);

        cardAlert.add(lblLoai);
        cardAlert.add(Box.createVerticalStrut(4));
        cardAlert.add(lblChiTiet);
        cardAlert.add(Box.createVerticalStrut(4));
        cardAlert.add(lblLuuY);
        pnlRoot.add(cardAlert);
        pnlRoot.add(Box.createVerticalStrut(14));

        // 3. Hướng dẫn & Ô nhập Mã PIN bảo mật
        JLabel lblPrompt = new JLabel("Nhập Mã PIN Chủ cửa hàng (6 chữ số) để xác thực và tiếp tục:");
        lblPrompt.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblPrompt.setForeground(new Color(0x1E, 0x29, 0x3B));
        lblPrompt.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlRoot.add(lblPrompt);
        pnlRoot.add(Box.createVerticalStrut(8));

        txtPin = new JPasswordField();
        txtPin.setFont(new Font("Monospaced", Font.BOLD, 18));
        txtPin.setHorizontalAlignment(JTextField.CENTER);
        txtPin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtPin.setPreferredSize(new Dimension(460, 40));
        txtPin.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xCB, 0xD5, 0xE1), 1, true),
                new EmptyBorder(0, 12, 0, 12)
        ));

        // Ràng buộc chỉ cho phép nhập tối đa 6 chữ số
        ((AbstractDocument) txtPin.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string != null && string.matches("\\d+") && (fb.getDocument().getLength() + string.length() <= 6)) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text == null || text.matches("\\d*") && (fb.getDocument().getLength() - length + text.length() <= 6)) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });

        txtPin.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    xuLyXacNhan();
                }
            }
        });
        pnlRoot.add(txtPin);
        pnlRoot.add(Box.createVerticalStrut(6));

        // 4. Dòng thông báo số lần thử còn lại & Lỗi
        JPanel pnlStatus = new JPanel(new BorderLayout());
        pnlStatus.setOpaque(false);
        pnlStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        lblAttempts = new JLabel("Số lần thử còn lại: " + soLanThuConLai + "/" + SecurityConfig.getMaxAttempts());
        lblAttempts.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblAttempts.setForeground(soLanThuConLai <= 1 ? new Color(0xDC, 0x26, 0x26) : new Color(0x64, 0x74, 0x8B));

        lblError = new JLabel("");
        lblError.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblError.setForeground(new Color(0xDC, 0x26, 0x26));

        pnlStatus.add(lblAttempts, BorderLayout.WEST);
        pnlStatus.add(lblError, BorderLayout.EAST);
        pnlRoot.add(pnlStatus);
        pnlRoot.add(Box.createVerticalStrut(18));

        // 5. Hàng nút thao tác (Xác nhận / Hủy bỏ)
        JPanel pnlButtons = new JPanel();
        pnlButtons.setLayout(new BoxLayout(pnlButtons, BoxLayout.X_AXIS));
        pnlButtons.setOpaque(false);
        pnlButtons.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlButtons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        RoundedButton btnCancel = new RoundedButton("Hủy thao tác", 10,
                new Color(0xF1, 0xF5, 0xF9), new Color(0xE2, 0xE8, 0xF0), new Color(0xCB, 0xD5, 0xE1), new Color(0x47, 0x55, 0x69));
        btnCancel.addActionListener(e -> {
            daXacNhan = false;
            dispose();
        });

        RoundedButton btnConfirm = new RoundedButton("Xác nhận mở khóa", 10,
                new Color(0xDC, 0x26, 0x26), new Color(0xB9, 0x1C, 0x1C), null, Color.WHITE);
        btnConfirm.addActionListener(e -> xuLyXacNhan());

        pnlButtons.add(Box.createHorizontalGlue());
        pnlButtons.add(btnCancel);
        pnlButtons.add(Box.createRigidArea(new Dimension(10, 0)));
        pnlButtons.add(btnConfirm);

        pnlRoot.add(pnlButtons);
        add(pnlRoot, BorderLayout.CENTER);

        // Auto-focus vào ô nhập PIN
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                txtPin.requestFocusInWindow();
            }
        });
    }

    private void xuLyXacNhan() {
        String pin = new String(txtPin.getPassword());
        if (pin.length() < 4) {
            lblError.setText("Vui lòng nhập đủ mã PIN!");
            txtPin.requestFocusInWindow();
            return;
        }

        if (SecurityConfig.kiemTraPin(pin)) {
            daXacNhan = true;
            dispose();
        } else {
            daXacNhan = false;
            dispose();
        }
    }

    public boolean isDaXacNhan() {
        return daXacNhan;
    }

    public String getPinDaNhap() {
        return new String(txtPin.getPassword());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CUSTOM SWING COMPONENTS (FLATLAF COMPLIANT)
    // ─────────────────────────────────────────────────────────────────────────

    public static class RoundedPanel extends JPanel {
        private final int arc;
        private final Color bgColor;
        private final Color strokeColor;

        public RoundedPanel(Color bgColor, int arc, Color strokeColor, Insets insets) {
            this.bgColor = bgColor;
            this.arc = arc;
            this.strokeColor = strokeColor;
            setOpaque(false);
            if (insets != null) {
                setBorder(new EmptyBorder(insets));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor != null ? bgColor : getBackground());
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

    public static class RoundedButton extends JButton {
        private final int arc;
        private final Color normalBg, hoverBg, strokeColor, textColor;

        public RoundedButton(String text, int arc, Color bg, Color hoverBg, Color strokeColor, Color textColor) {
            super(text);
            this.arc = arc;
            this.normalBg = bg;
            this.hoverBg = hoverBg;
            this.strokeColor = strokeColor;
            this.textColor = textColor;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(textColor);
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension pref = super.getPreferredSize();
            FontMetrics fm = getFontMetrics(getFont());
            int textW = getText() != null ? fm.stringWidth(getText()) : 0;
            int neededW = textW + 36;
            int neededH = Math.max(pref != null ? pref.height : 36, 36);
            return new Dimension(Math.max(pref != null ? pref.width : neededW, neededW), neededH);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color bg = isEnabled() ? (getModel().isPressed() ? hoverBg.darker()
                    : (getModel().isRollover() ? hoverBg : normalBg))
                    : new Color(normalBg.getRed(), normalBg.getGreen(), normalBg.getBlue(), 120);
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
}

