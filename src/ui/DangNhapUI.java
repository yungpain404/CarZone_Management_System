package ui;

import com.formdev.flatlaf.FlatClientProperties;
import entity.NhanVien;
import controller.DangNhapController;
import controller.DangNhapController.KetQuaDangNhap;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextAttribute;
import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Màn hình Đăng nhập CarZone – Chuẩn mực tuyệt đối theo hình ảnh thiết kế Figma.
 *
 * Cấu trúc thiết kế:
 * - Frame: 1280×840px, nền #F8FAFC
 * - Header: 1280×72px, nền #FFFFFF, viền dưới #E2E8F0, padding 16px 24px
 *   + Trái: Logo CarZone + "Hệ thống quản lý gara ô tô"
 *   + Phải: Nút Help (#EFF6FF, text #2563EB) & Nút About (#FFFFFF, viền #E2E8F0) bo góc 12px
 * - Body: Nền #F8FAFC, Card đăng nhập căn giữa chính xác
 * - Login Card: 480×556px, nền #FFFFFF, bo góc 16px, viền #E2E8F0, đổ bóng mềm
 *   + Logo CarZone 180×53px
 *   + Tiêu đề in hoa: "HỆ THỐNG QUẢN LÝ GARA Ô TÔ" (#202A36, bold 14px)
 *   + Tiêu đề form: Icon khiên bảo mật + "Đăng nhập" (bold 18px)
 *   + Trường Tên đăng nhập & Mật khẩu (cao 44px, bo góc 12px, viền #CBD5E1)
 *   + Checkbox "Hiện mật khẩu"
 *   + Hộp thông báo lỗi (#FDECEC, viền #E7B7B7, bo góc 12px, icon cảnh báo)
 *   + Link "Quên mật khẩu?" căn phải (#2563EB, bold 13px)
 *   + Hàng nút: "Đăng nhập" (296×40px, bo góc 12px) + "Thoát" (120×40px, bo góc 12px)
 * - Status Bar: 1280×28px, nền #F8FAFC, viền trên #E2E8F0
 */
public class DangNhapUI extends JFrame {

    // ── Bảng màu chuẩn tuyệt đối từ Figma ────────────────────────────────────
    private static final Color C_BG           = new Color(0xF8FAFC); // Nền app & status bar
    private static final Color C_WHITE        = Color.WHITE;         // Nền card & header
    private static final Color C_CARD_STROKE  = new Color(0xE2E8F0); // Viền card, header, status
    private static final Color C_INPUT_STROKE = new Color(0xCBD5E1); // Viền input & checkbox
    private static final Color C_ERR_STROKE   = new Color(0xE7B7B7); // Viền khung lỗi
    private static final Color C_ERR_BG       = new Color(0xFDECEC); // Nền khung lỗi
    private static final Color C_ERR_TEXT     = new Color(0xD12828); // Màu chữ lỗi
    private static final Color C_TEXT_HEAD    = new Color(0x202A36); // Tiêu đề chính & form
    private static final Color C_TEXT_LABEL   = new Color(0x334155); // Nhãn trường, nút phụ
    private static final Color C_TEXT_SUBTLE  = new Color(0x64748B); // Text phụ header, status
    private static final Color C_PRIMARY      = new Color(0x2563EB); // Xanh dương chủ đạo
    private static final Color C_PRIMARY_HV   = new Color(0x1D4ED8); // Xanh dương hover
    private static final Color C_HELP_BG      = new Color(0xEFF6FF); // Nền nút Help
    private static final Color C_HELP_STROKE  = new Color(0xBFDBFE); // Viền nút Help

    // ── Font chữ: Ưu tiên Arimo, fallback SansSerif ───────────────────────────
    private static final String FONT_FAMILY = detectFont();

    private static String detectFont() {
        for (String f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (f.equalsIgnoreCase("Arimo")) return "Arimo";
        }
        return "SansSerif";
    }

    private static Font f(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }

    // ── Thành phần giao diện ──────────────────────────────────────────────────
    private JButton btnHelp;
    private JButton btnAbout;
    private JTextField tfTenDangNhap;
    private JPasswordField pfMatKhau;
    private JCheckBox chkHienMatKhau;
    private JLabel lblQuenMatKhau;
    private JButton btnDangNhap;
    private JButton btnThoat;
    private JPanel pnlError;
    private JLabel lblError;
    private Component errorBottomSpace;
    private JLabel lblStatusLeft;
    private boolean isProgrammaticClearing = false;

    private final DangNhapController controller = new DangNhapController();
    private DangNhapListener listener;

    public interface DangNhapListener {
        void onDangNhapThanhCong(NhanVien nhanVien);
    }

    public DangNhapUI() {
        setTitle("CarZone – Hệ thống quản lý gara ô tô");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1280, 840);
        setMinimumSize(new Dimension(1024, 720));
        setLocationRelativeTo(null);
        getContentPane().setBackground(C_BG);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                tfTenDangNhap.requestFocusInWindow();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                xuLyThoat();
            }
        });

        initComponents();
        bindActions();
    }

    public void setDangNhapListener(DangNhapListener l) {
        this.listener = l;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // KHUNG CHÍNH (BorderLayout: Header, Body, Status Bar)
    // ─────────────────────────────────────────────────────────────────────────

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. HEADER (1280×72px)
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 0));
        header.setBackground(C_WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, C_CARD_STROKE),
                new EmptyBorder(16, 24, 16, 24)));
        header.setPreferredSize(new Dimension(0, 72));

        // Trái: Brand (Logo + Text "Hệ thống quản lý gara ô tô")
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brand.setOpaque(false);
        brand.add(createLogoComponent(120, 35));

        JLabel lblSub = new JLabel("Hệ thống quản lý gara ô tô");
        lblSub.setFont(f(Font.PLAIN, 13));
        lblSub.setForeground(C_TEXT_SUBTLE);
        brand.add(lblSub);
        header.add(brand, BorderLayout.WEST);

        // Phải: Nút Help & About bo góc 12px
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        nav.setOpaque(false);
        btnHelp = makeNavButton("Help", true);
        btnAbout = makeNavButton("About", false);
        nav.add(btnHelp);
        nav.add(btnAbout);
        header.add(nav, BorderLayout.EAST);

        return header;
    }

    /**
     * Nút Help / About với bo góc 12px chuẩn theo Figma.
     */
    private JButton makeNavButton(String text, boolean isHelp) {
        Color bg = isHelp ? C_HELP_BG : C_WHITE;
        Color hoverBg = isHelp ? new Color(0xDBEAFE) : new Color(0xF8FAFC);
        Color stroke = isHelp ? C_HELP_STROKE : C_CARD_STROKE;

        RoundedButton btn = new RoundedButton(text, 12, bg, hoverBg, stroke);
        btn.setFont(f(isHelp ? Font.BOLD : Font.PLAIN, 13));
        btn.setForeground(isHelp ? C_PRIMARY : C_TEXT_LABEL);
        btn.setIcon(createNavIcon(isHelp));
        btn.setIconTextGap(7);
        btn.setPreferredSize(new Dimension(isHelp ? 86 : 92, 38));
        return btn;
    }

    private Icon createNavIcon(boolean isHelp) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isHelp ? C_PRIMARY : C_TEXT_SUBTLE);
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawOval(x + 1, y + 1, 14, 14);

                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                FontMetrics fm = g2.getFontMetrics();
                String glyph = isHelp ? "?" : "i";
                int tx = x + 1 + (14 - fm.stringWidth(glyph)) / 2;
                int ty = y + 1 + ((14 - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(glyph, tx, ty);
                g2.dispose();
            }

            @Override public int getIconWidth() { return 16; }
            @Override public int getIconHeight() { return 16; }
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. BODY & LOGIN CARD (Căn giữa tuyệt đối)
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel buildBody() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(C_BG);
        body.add(buildLoginCard());
        return body;
    }

    /**
     * Card đăng nhập 480×556px, nền trắng, bo góc 16px, viền #E2E8F0, đổ bóng mềm.
     */
    private JPanel buildLoginCard() {
        final int CARD_W = 480;
        final int CARD_H = 556;
        final int INNER_W = 424; // 480 - 28*2

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Hiệu ứng đổ bóng mờ xung quanh (Soft drop shadow)
                for (int i = 8; i >= 1; i--) {
                    g2.setColor(new Color(15, 23, 42, 3));
                    g2.fillRoundRect(i, i + 3, getWidth() - i * 2, getHeight() - i, 22, 22);
                }

                // Nền trắng bo tròn 16px
                g2.setColor(C_WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);

                // Đường viền 1px #E2E8F0
                g2.setColor(C_CARD_STROKE);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(26, 28, 26, 28));
        card.setPreferredSize(new Dimension(CARD_W, CARD_H));
        card.setMaximumSize(new Dimension(CARD_W, CARD_H));

        // 1. Logo CarZone lớn (180×53px)
        JPanel pnlLogo = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        pnlLogo.setOpaque(false);
        pnlLogo.setPreferredSize(new Dimension(INNER_W, 53));
        pnlLogo.setMaximumSize(new Dimension(INNER_W, 53));
        pnlLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlLogo.add(createLogoComponent(180, 53));
        card.add(pnlLogo);

        card.add(Box.createRigidArea(new Dimension(0, 8)));

        // 2. Tiêu đề in hoa: "HỆ THỐNG QUẢN LÝ GARA Ô TÔ"
        JLabel lblHeaderTitle = new JLabel("HỆ THỐNG QUẢN LÝ GARA Ô TÔ", SwingConstants.CENTER);
        lblHeaderTitle.setFont(f(Font.BOLD, 14));
        lblHeaderTitle.setForeground(C_TEXT_HEAD);
        lblHeaderTitle.setPreferredSize(new Dimension(INNER_W, 20));
        lblHeaderTitle.setMaximumSize(new Dimension(INNER_W, 20));
        lblHeaderTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblHeaderTitle);

        card.add(Box.createRigidArea(new Dimension(0, 18)));

        // 3. Tiêu đề form: Icon khiên + "Đăng nhập"
        JPanel titleRow = buildFormTitleRow(INNER_W);
        titleRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(titleRow);

        card.add(Box.createRigidArea(new Dimension(0, 14)));

        // 4. Trường "Tên đăng nhập"
        tfTenDangNhap = new JTextField();
        JPanel fieldUser = buildInputField("Tên đăng nhập", tfTenDangNhap, INNER_W);
        fieldUser.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(fieldUser);

        card.add(Box.createRigidArea(new Dimension(0, 14)));

        // 5. Trường "Mật khẩu"
        JPanel fieldPwd = buildPasswordField(INNER_W);
        fieldPwd.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(fieldPwd);

        card.add(Box.createRigidArea(new Dimension(0, 12)));

        // 6. Checkbox "Hiện mật khẩu"
        JPanel chkRow = buildCheckboxRow(INNER_W);
        chkRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(chkRow);

        card.add(Box.createRigidArea(new Dimension(0, 10)));

        // 7. Khung thông báo lỗi (ẩn mặc định, hiện khi có lỗi)
        pnlError = buildErrorPanel(INNER_W);
        pnlError.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlError.setVisible(false);
        card.add(pnlError);

        errorBottomSpace = Box.createRigidArea(new Dimension(0, 10));
        errorBottomSpace.setVisible(false);
        card.add(errorBottomSpace);

        // 8. Link "Quên mật khẩu?" căn phải
        JPanel forgotRow = buildForgotRow(INNER_W);
        forgotRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(forgotRow);

        card.add(Box.createRigidArea(new Dimension(0, 14)));

        // 9. Hàng nút Hành động: "Đăng nhập" + "Thoát"
        JPanel actionRow = buildActionRow(INNER_W);
        actionRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(actionRow);

        return card;
    }

    /**
     * Tiêu đề form: Icon khiên bảo mật + "Đăng nhập"
     */
    private JPanel buildFormTitleRow(int width) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(width, 26));
        p.setMaximumSize(new Dimension(width, 26));

        JLabel icon = new JLabel(createShieldIcon());
        JLabel title = new JLabel("Đăng nhập");
        title.setFont(f(Font.BOLD, 18));
        title.setForeground(C_TEXT_HEAD);

        p.add(icon);
        p.add(Box.createRigidArea(new Dimension(8, 0)));
        p.add(title);
        return p;
    }

    private Icon createShieldIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Khiên màu xanh dương
                g2.setColor(C_PRIMARY);
                int[] xs = { x + 10, x + 18, x + 18, x + 10, x + 2, x + 2 };
                int[] ys = { y + 2,  y + 4,  y + 11, y + 18, y + 11, y + 4 };
                g2.fillPolygon(xs, ys, xs.length);

                // Dấu check trắng
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 6, y + 10, x + 9, y + 13);
                g2.drawLine(x + 9, y + 13, x + 14, y + 7);

                g2.dispose();
            }

            @Override public int getIconWidth() { return 20; }
            @Override public int getIconHeight() { return 20; }
        };
    }

    /**
     * Trường nhập liệu Tên đăng nhập (rộng chuẩn, cao 44px, viền #CBD5E1, bo góc 12px)
     */
    private JPanel buildInputField(String labelText, JTextField tf, int width) {
        JPanel outer = new JPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
        outer.setOpaque(false);
        outer.setPreferredSize(new Dimension(width, 68));
        outer.setMaximumSize(new Dimension(width, 68));

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(f(Font.PLAIN, 13));
        lbl.setForeground(C_TEXT_LABEL);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        outer.add(lbl);

        outer.add(Box.createRigidArea(new Dimension(0, 5)));

        tf.setFont(f(Font.PLAIN, 14));
        tf.setForeground(C_TEXT_HEAD);
        tf.setBackground(C_WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_INPUT_STROKE, 1, true),
                new EmptyBorder(0, 14, 0, 14)));
        tf.setPreferredSize(new Dimension(width, 44));
        tf.setMaximumSize(new Dimension(width, 44));
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        tf.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập " + labelText.toLowerCase());
        addFocusBorder(tf);
        outer.add(tf);

        return outer;
    }

    /**
     * Trường nhập liệu Mật khẩu (rộng chuẩn, cao 44px, viền #CBD5E1, bo góc 12px)
     */
    private JPanel buildPasswordField(int width) {
        JPanel outer = new JPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
        outer.setOpaque(false);
        outer.setPreferredSize(new Dimension(width, 68));
        outer.setMaximumSize(new Dimension(width, 68));

        JLabel lbl = new JLabel("Mật khẩu");
        lbl.setFont(f(Font.PLAIN, 13));
        lbl.setForeground(C_TEXT_LABEL);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        outer.add(lbl);

        outer.add(Box.createRigidArea(new Dimension(0, 5)));

        pfMatKhau = new JPasswordField();
        pfMatKhau.setEchoChar('●');
        pfMatKhau.setFont(f(Font.PLAIN, 14));
        pfMatKhau.setForeground(C_TEXT_HEAD);
        pfMatKhau.setBackground(C_WHITE);
        pfMatKhau.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_INPUT_STROKE, 1, true),
                new EmptyBorder(0, 14, 0, 14)));
        pfMatKhau.setPreferredSize(new Dimension(width, 44));
        pfMatKhau.setMaximumSize(new Dimension(width, 44));
        pfMatKhau.setAlignmentX(Component.LEFT_ALIGNMENT);
        pfMatKhau.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập mật khẩu");
        addFocusBorder(pfMatKhau);

        outer.add(pfMatKhau);
        return outer;
    }

    private void addFocusBorder(JTextField tf) {
        tf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(C_PRIMARY, 2, true),
                        new EmptyBorder(0, 13, 0, 13)));
            }

            @Override
            public void focusLost(FocusEvent e) {
                Color stroke = (pnlError != null && pnlError.isVisible()) ? C_ERR_TEXT : C_INPUT_STROKE;
                tf.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(stroke, 1, true),
                        new EmptyBorder(0, 14, 0, 14)));
            }
        });
    }

    /**
     * Checkbox "Hiện mật khẩu"
     */
    private JPanel buildCheckboxRow(int width) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(width, 24));
        p.setMaximumSize(new Dimension(width, 24));

        chkHienMatKhau = new JCheckBox("Hiện mật khẩu");
        chkHienMatKhau.setFont(f(Font.PLAIN, 14));
        chkHienMatKhau.setForeground(C_TEXT_LABEL);
        chkHienMatKhau.setOpaque(false);
        p.add(chkHienMatKhau);
        return p;
    }

    /**
     * Khung lỗi (nền hồng #FDECEC, viền #E7B7B7, bo góc 12px, icon tròn !)
     */
    private JPanel buildErrorPanel(int width) {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_ERR_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(C_ERR_STROKE);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(8, 12, 8, 12));
        p.setPreferredSize(new Dimension(width, 38));
        p.setMaximumSize(new Dimension(width, 38));

        JLabel icon = new JLabel(createAlertCircleIcon());
        p.add(icon);
        p.add(Box.createRigidArea(new Dimension(8, 0)));

        lblError = new JLabel("Vui lòng nhập tên đăng nhập và mật khẩu.");
        lblError.setFont(f(Font.PLAIN, 13));
        lblError.setForeground(C_ERR_TEXT);
        p.add(lblError);

        return p;
    }

    private Icon createAlertCircleIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xC62828));
                g2.setStroke(new BasicStroke(1.6f));
                g2.drawOval(x + 1, y + 1, 14, 14);

                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 8, y + 4, x + 8, y + 9);
                g2.fillRect(x + 7, y + 11, 2, 2);
                g2.dispose();
            }

            @Override public int getIconWidth() { return 16; }
            @Override public int getIconHeight() { return 16; }
        };
    }

    /**
     * Link "Quên mật khẩu?" căn lề phải chuẩn xác
     */
    private JPanel buildForgotRow(int width) {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(width, 22));
        p.setMaximumSize(new Dimension(width, 22));

        lblQuenMatKhau = new JLabel("Quên mật khẩu?");
        lblQuenMatKhau.setFont(f(Font.BOLD, 13));
        lblQuenMatKhau.setForeground(C_PRIMARY);
        lblQuenMatKhau.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        @SuppressWarnings("unchecked")
        Map<TextAttribute, Object> attrs = (Map<TextAttribute, Object>) lblQuenMatKhau.getFont().getAttributes();
        attrs.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        lblQuenMatKhau.setFont(lblQuenMatKhau.getFont().deriveFont(attrs));
        p.add(lblQuenMatKhau, BorderLayout.EAST);
        return p;
    }

    /**
     * Hàng nút thao tác cân đối bo góc 12px: "Đăng nhập" (296×40px) + gap 8px + "Thoát" (120×40px) = 424px
     */
    private JPanel buildActionRow(int width) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(width, 40));
        p.setMaximumSize(new Dimension(width, 40));

        int gap = 8;
        int exitW = 120;
        int loginW = width - exitW - gap; // 424 - 120 - 8 = 296px

        btnDangNhap = makePrimaryBtn("Đăng nhập", loginW, 40);
        btnThoat = makeExitBtn("Thoát", exitW, 40);

        p.add(btnDangNhap);
        p.add(Box.createRigidArea(new Dimension(gap, 0)));
        p.add(btnThoat);
        return p;
    }

    private JButton makePrimaryBtn(String text, int w, int h) {
        RoundedButton btn = new RoundedButton(text, 12, C_PRIMARY, C_PRIMARY_HV, null);
        btn.setFont(f(Font.BOLD, 14));
        btn.setForeground(C_WHITE);
        Dimension d = new Dimension(w, h);
        btn.setPreferredSize(d);
        btn.setMaximumSize(d);
        return btn;
    }

    private JButton makeExitBtn(String text, int w, int h) {
        RoundedButton btn = new RoundedButton(text, 12, C_WHITE, new Color(0xF1F5F9), C_INPUT_STROKE);
        btn.setFont(f(Font.BOLD, 14));
        btn.setForeground(C_TEXT_LABEL);
        Dimension d = new Dimension(w, h);
        btn.setPreferredSize(d);
        btn.setMaximumSize(d);
        return btn;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. STATUS BAR (1280×28px)
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(0, 0));
        bar.setBackground(C_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, C_CARD_STROKE),
                new EmptyBorder(6, 20, 6, 20)));
        bar.setPreferredSize(new Dimension(0, 28));

        lblStatusLeft = new JLabel("Chưa đăng nhập");
        lblStatusLeft.setFont(f(Font.PLAIN, 12));
        lblStatusLeft.setForeground(C_TEXT_SUBTLE);

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        JLabel lblRight = new JLabel(date + " · CarZone 1.0");
        lblRight.setFont(f(Font.PLAIN, 12));
        lblRight.setForeground(C_TEXT_SUBTLE);

        bar.add(lblStatusLeft, BorderLayout.WEST);
        bar.add(lblRight, BorderLayout.EAST);
        return bar;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CUSTOM ROUNDED BUTTON (Bo góc chuẩn xác 12px, khử răng cưa mượt mà)
    // ─────────────────────────────────────────────────────────────────────────

    private static class RoundedButton extends JButton {
        private final int arc;
        private final Color normalBg;
        private final Color hoverBg;
        private final Color strokeColor;

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

            Color bg = normalBg;
            if (isEnabled()) {
                if (getModel().isPressed()) {
                    bg = hoverBg.darker();
                } else if (getModel().isRollover()) {
                    bg = hoverBg;
                }
            } else {
                bg = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 160);
            }

            // Nền bo góc
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

            // Viền bo góc
            if (strokeColor != null) {
                g2.setColor(strokeColor);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGO COMPONENT (Tự động tải ảnh logo thật, fallback sang vector)
    // ─────────────────────────────────────────────────────────────────────────    // ─────────────────────────────────────────────────────────────────────────
    // LOGO COMPONENT (Vẽ ảnh gốc độ nét cao với Bicubic Antialiasing & giữ đúng tỷ lệ)
    // ─────────────────────────────────────────────────────────────────────────

    private JComponent createLogoComponent(int targetW, int targetH) {
        Image rawImg = loadRawLogoImage();
        if (rawImg != null) {
            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                    int cw = getWidth();
                    int ch = getHeight();
                    double iw = rawImg.getWidth(null);
                    double ih = rawImg.getHeight(null);
                    if (iw > 0 && ih > 0) {
                        double imgAspect = iw / ih;
                        double compAspect = (double) cw / ch;
                        int drawW, drawH;
                        if (compAspect > imgAspect) {
                            drawH = ch;
                            drawW = (int) Math.round(drawH * imgAspect);
                        } else {
                            drawW = cw;
                            drawH = (int) Math.round(drawW / imgAspect);
                        }
                        int x = (cw - drawW) / 2;
                        int y = (ch - drawH) / 2;
                        g2.drawImage(rawImg, x, y, drawW, drawH, null);
                    }
                    g2.dispose();
                }
            };
            panel.setOpaque(false);
            panel.setPreferredSize(new Dimension(targetW, targetH));
            panel.setMaximumSize(new Dimension(targetW, targetH));
            return panel;
        }

        JPanel fallback = new JPanel();
        fallback.setOpaque(false);
        fallback.setPreferredSize(new Dimension(targetW, targetH));
        return fallback;
    }

    private static Image rawLogoImage = null;

    private static Image loadRawLogoImage() {
        if (rawLogoImage != null) return rawLogoImage;
        try {
            java.net.URL url = DangNhapUI.class.getResource("/carzone_logo.png");
            if (url == null) {
                url = DangNhapUI.class.getResource("carzone_logo.png");
            }
            if (url != null) {
                rawLogoImage = javax.imageio.ImageIO.read(url);
            } else {
                java.io.File file = new java.io.File("img/carzone_logo.png");
                if (file.exists()) {
                    rawLogoImage = javax.imageio.ImageIO.read(file);
                }
            }
        } catch (Exception ignored) {
        }
        return rawLogoImage;
    }

    private static ImageIcon loadLogoImage(int targetW, int targetH) {
        try {
            Image img = loadRawLogoImage();
            if (img != null) {
                Image scaled = img.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
    // ─────────────────────────────────────────────────────────────────────────
    // LUỒNG XỬ LÝ USE CASE ĐĂNG NHẬP & SỰ KIỆN GIAO DIỆN
    // ─────────────────────────────────────────────────────────────────────────

    private void bindActions() {
        // Nút chính & phím Enter
        btnDangNhap.addActionListener(e -> xuLyDangNhap());
        pfMatKhau.addActionListener(e -> xuLyDangNhap());
        tfTenDangNhap.addActionListener(e -> pfMatKhau.requestFocusInWindow());

        // Ẩn / hiện mật khẩu
        chkHienMatKhau.addActionListener(e -> pfMatKhau.setEchoChar(chkHienMatKhau.isSelected() ? (char) 0 : '●'));

        // Nút Thoát
        btnThoat.addActionListener(e -> xuLyThoat());

        // Nút Help & About ở Header
        btnHelp.addActionListener(e -> xuLyHelp());
        btnAbout.addActionListener(e -> xuLyAbout());

        // Link Quên mật khẩu
        lblQuenMatKhau.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                xuLyQuenMatKhau();
            }
        });

        // Tự động xóa thông báo lỗi khi người dùng bắt đầu nhập lại
        DocumentListener clearErrorListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { clearErrorIfVisible(); }
            @Override public void removeUpdate(DocumentEvent e) { clearErrorIfVisible(); }
            @Override public void changedUpdate(DocumentEvent e) { clearErrorIfVisible(); }
        };
        tfTenDangNhap.getDocument().addDocumentListener(clearErrorListener);
        pfMatKhau.getDocument().addDocumentListener(clearErrorListener);
    }

    private void clearErrorIfVisible() {
        if (isProgrammaticClearing) {
            return;
        }
        if (pnlError != null && pnlError.isVisible()) {
            hienLoi(null);
            resetFieldBorders();
        }
    }

    private void highlightErrorFields() {
        Border errBrd = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_ERR_TEXT, 1, true),
                new EmptyBorder(0, 14, 0, 14));
        tfTenDangNhap.setBorder(errBrd);
        pfMatKhau.setBorder(errBrd);
    }

    private void resetFieldBorders() {
        Border normalBrd = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_INPUT_STROKE, 1, true),
                new EmptyBorder(0, 14, 0, 14));
        tfTenDangNhap.setBorder(normalBrd);
        pfMatKhau.setBorder(normalBrd);
    }

    /**
     * Quy trình xử lý Use Case Đăng nhập:
     * 1. Validate sớm: Kiểm tra trống, định dạng mã nhân viên (CCHxxx, TNVxxx, KTVxxx).
     * 2. Khóa giao diện (Loading state), đổi trạng thái nút & status bar.
     * 3. Chạy kiểm tra tài khoản, mã băm SHA-256 qua DangNhapService bằng SwingWorker.
     * 4. Phản hồi kết quả: Hiển thị lỗi tương ứng trên khung lỗi Figma, viền đỏ, popup và âm thanh cảnh báo.
     */
    private void xuLyDangNhap() {
        String ten = tfTenDangNhap.getText().trim();
        String mk = new String(pfMatKhau.getPassword());

        // Bước 1: Validate trống
        if (ten.isBlank() || mk.isBlank()) {
            hienLoi("Vui lòng nhập tên đăng nhập và mật khẩu.");
            highlightErrorFields();
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập đầy đủ Tên đăng nhập và Mật khẩu.",
                    "Thông báo đăng nhập",
                    JOptionPane.WARNING_MESSAGE);
            if (ten.isBlank()) {
                tfTenDangNhap.requestFocusInWindow();
            } else {
                pfMatKhau.requestFocusInWindow();
            }
            return;
        }

        // Bước 2: Validate định dạng mã nhân viên (3 chữ cái vai trò + 3 chữ số)
        if (!ten.toUpperCase().matches("^(CCH|TNV|KTV)\\d{3}$")) {
            hienLoi("Tên đăng nhập không đúng định dạng (VD: CCH001, TNV001, KTV001).");
            highlightErrorFields();
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(this,
                    "Tên đăng nhập không đúng định dạng!\nQuy ước:\n• CCHxxx: Chủ cửa hàng (VD: CCH001)\n• TNVxxx: Tiếp nhận viên (VD: TNV001)\n• KTVxxx: Kỹ thuật viên (VD: KTV001)",
                    "Sai định dạng tài khoản",
                    JOptionPane.WARNING_MESSAGE);
            tfTenDangNhap.requestFocusInWindow();
            return;
        }

        // Bước 3: Đưa giao diện vào trạng thái xử lý
        setFormEnabled(false);
        hienLoi(null);
        resetFieldBorders();
        btnDangNhap.setText("Đang đăng nhập…");
        lblStatusLeft.setText("Đang xác thực thông tin tài khoản…");

        final String username = ten.toUpperCase();

        new SwingWorker<KetQuaDangNhap, Void>() {
            @Override
            protected KetQuaDangNhap doInBackground() {
                return controller.dangNhap(username, mk);
            }

            @Override
            protected void done() {
                try {
                    KetQuaDangNhap kq = get();
                    if (kq.isThanhCong()) {
                        btnDangNhap.setText("Thành công!");
                        lblStatusLeft.setText("Đăng nhập thành công! Đang chuyển trang…");
                        Timer t = new Timer(350, ev -> {
                            if (listener != null) {
                                listener.onDangNhapThanhCong(kq.getNhanVien());
                            }
                            dispose();
                        });
                        t.setRepeats(false);
                        t.start();
                    } else {
                        btnDangNhap.setText("Đăng nhập");
                        hienLoi(kq.getThongBao());
                        highlightErrorFields();
                        lblStatusLeft.setText("Đăng nhập thất bại: " + kq.getThongBao());

                        // Dùng cờ isProgrammaticClearing để việc xoá mật khẩu không vô tình kích hoạt DocumentListener ẩn lỗi
                        isProgrammaticClearing = true;
                        try {
                            pfMatKhau.setText("");
                        } finally {
                            isProgrammaticClearing = false;
                        }

                        pfMatKhau.requestFocusInWindow();
                        setFormEnabled(true);
                        Toolkit.getDefaultToolkit().beep();

                        JOptionPane.showMessageDialog(DangNhapUI.this,
                                kq.getThongBao(),
                                "Đăng nhập thất bại",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    btnDangNhap.setText("Đăng nhập");
                    String msg = "Không thể kết nối cơ sở dữ liệu. Vui lòng kiểm tra Docker/SQL Server.";
                    hienLoi(msg);
                    highlightErrorFields();
                    lblStatusLeft.setText("Lỗi kết nối cơ sở dữ liệu");
                    setFormEnabled(true);
                    Toolkit.getDefaultToolkit().beep();

                    JOptionPane.showMessageDialog(DangNhapUI.this,
                            msg + "\nChi tiết lỗi: " + ex.getMessage(),
                            "Lỗi kết nối cơ sở dữ liệu",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void xuLyHelp() {
        JOptionPane.showMessageDialog(this,
                "<html><body style='font-family:sans-serif; width: 320px; padding: 4px;'>"
                + "<h3 style='color:#2563EB; margin-top:0;'>Hướng dẫn đăng nhập – CarZone</h3>"
                + "<p><b>Quy tắc tài khoản nhân viên:</b></p>"
                + "<ul>"
                + "<li><b>CCHxxx</b>: Chủ cửa hàng (VD: <code>CCH001</code>)</li>"
                + "<li><b>TNVxxx</b>: Tiếp nhận viên / Thu ngân (VD: <code>TNV001</code>)</li>"
                + "<li><b>KTVxxx</b>: Kỹ thuật viên (VD: <code>KTV001</code>)</li>"
                + "</ul>"
                + "<p><b>Mật khẩu mặc định:</b> <code>Carzone@123</code></p>"
                + "<p style='color:#64748B; font-size:11px;'>Nếu quên mật khẩu hoặc bị vô hiệu hóa, vui lòng liên hệ Chủ cửa hàng.</p>"
                + "</body></html>",
                "Trợ giúp đăng nhập",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void xuLyAbout() {
        JOptionPane.showMessageDialog(this,
                "<html><body style='font-family:sans-serif; width: 300px; padding: 4px;'>"
                + "<h3 style='color:#202A36; margin-top:0;'>CarZone – Quản lý Gara Ô tô</h3>"
                + "<p>Phiên bản: <b>1.0.0 (2026)</b></p>"
                + "<hr style='border: 0; border-top: 1px solid #E2E8F0;'>"
                + "<p style='font-size:12px; color:#475569;'>"
                + "Đồ án môn Phát triển ứng dụng<br>"
                + "Nhóm 01 – Đại học Công nghiệp TP.HCM (IUH)<br>"
                + "Hệ thống hỗ trợ 3 vai trò: Chủ cửa hàng, Tiếp nhận viên, Kỹ thuật viên."
                + "</p>"
                + "</body></html>",
                "Thông tin ứng dụng",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void xuLyQuenMatKhau() {
        JOptionPane.showMessageDialog(this,
                "<html><body style='font-family:sans-serif; width: 300px; padding: 4px;'>"
                + "<h4 style='color:#2563EB; margin-top:0;'>Hỗ trợ quên mật khẩu</h4>"
                + "<p>Để đảm bảo an toàn nghiệp vụ gara ô tô, hệ thống không cấp lại mật khẩu tự động qua email hay SMS cá nhân.</p>"
                + "<p>Vui lòng liên hệ trực tiếp <b>Chủ cửa hàng</b> hoặc <b>Quản trị viên</b> để được cấp lại mật khẩu mới.</p>"
                + "</body></html>",
                "Quên mật khẩu",
                JOptionPane.WARNING_MESSAGE);
    }

    private void xuLyThoat() {
        int c = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn thoát ứng dụng CarZone không?", "Xác nhận thoát",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (c == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    private void hienLoi(String msg) {
        if (msg == null || msg.isBlank()) {
            pnlError.setVisible(false);
            if (errorBottomSpace != null)
                errorBottomSpace.setVisible(false);
        } else {
            lblError.setText(msg);
            pnlError.setVisible(true);
            if (errorBottomSpace != null)
                errorBottomSpace.setVisible(true);
        }
        revalidate();
        repaint();
    }

    private void setFormEnabled(boolean b) {
        tfTenDangNhap.setEnabled(b);
        pfMatKhau.setEnabled(b);
        btnDangNhap.setEnabled(b);
        btnThoat.setEnabled(b);
        chkHienMatKhau.setEnabled(b);
    }
}
