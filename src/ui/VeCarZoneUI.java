package ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.net.URL;

/**
 * VeCarZoneUI – Trang "Về Carzone" hiển thị thông tin ứng dụng.
 * Bám sát thiết kế Figma: màn hình About Carzone.
 * Tích hợp vào ContentPanel của MainFrame qua card "PAGE_VE_CARZONE".
 */
public class VeCarZoneUI extends JPanel {

    // ── Bảng màu (lấy từ MainFrame) ─────────────────────────────────────────
    private static final Color C_BG          = new Color(0xF8FAFC);
    private static final Color C_WHITE       = Color.WHITE;
    private static final Color C_BORDER      = new Color(0xE2E8F0);
    private static final Color C_TEXT_HEAD   = new Color(0x0F172A);
    private static final Color C_TEXT_MAIN   = new Color(0x334155);
    private static final Color C_TEXT_SUBTLE = new Color(0x64748B);
    private static final Color C_PRIMARY     = new Color(0x2563EB);
    private static final Color C_PRIMARY_BG  = new Color(0xEFF6FF);
    private static final Color C_PRIMARY_BR  = new Color(0xBFDBFE);

    private final Runnable onNavigateToHelp;

    public VeCarZoneUI() {
        this(null);
    }

    public VeCarZoneUI(Runnable onNavigateToHelp) {
        this.onNavigateToHelp = onNavigateToHelp;
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel content = buildContent();
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel();
        root.setOpaque(false);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(28, 32, 32, 32));

        // ── Tiêu đề trang ────────────────────────────────────────────────────
        JPanel pnlTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlTitle.setOpaque(false);
        pnlTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        JLabel iconTitle = new JLabel(new MainFrame.InfoIcon(C_PRIMARY, 28));
        pnlTitle.add(iconTitle);

        JLabel lblTitle = new JLabel("Về Carzone");
        lblTitle.setFont(MainFrame.f(Font.BOLD, 24));
        lblTitle.setForeground(C_TEXT_HEAD);
        pnlTitle.add(lblTitle);

        root.add(pnlTitle);
        root.add(Box.createVerticalStrut(20));

        // ── Card thông tin ứng dụng ───────────────────────────────────────────
        root.add(buildAppInfoCard());
        root.add(Box.createVerticalStrut(28));

        // ── Tiêu đề "Một hệ thống, ba vai trò" ─────────────────────────────
        root.add(sectionLabel("Một hệ thống, ba vai trò"));
        root.add(Box.createVerticalStrut(12));
        root.add(buildRoleCards());
        root.add(Box.createVerticalStrut(28));

        // ── Tiêu đề "Các phân hệ chính" ──────────────────────────────────────
        root.add(sectionLabel("Các phân hệ chính"));
        root.add(Box.createVerticalStrut(12));
        root.add(buildModuleGrid());
        root.add(Box.createVerticalStrut(28));

        // ── Footer ────────────────────────────────────────────────────────────
        root.add(buildFooter());
        root.add(Box.createVerticalStrut(16));

        return root;
    }

    // ── Card thông tin ứng dụng (logo + tên + mô tả) ─────────────────────────
    private JPanel buildAppInfoCard() {
        JPanel card = new RoundedCardPanel();
        card.setLayout(new BorderLayout(24, 0));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Logo bên trái
        JLabel lblLogo = new JLabel();
        lblLogo.setPreferredSize(new Dimension(160, 100));
        lblLogo.setHorizontalAlignment(SwingConstants.CENTER);
        lblLogo.setVerticalAlignment(SwingConstants.CENTER);
        Image logo = loadImage("carzone_logo.png");
        if (logo != null) {
            lblLogo.setIcon(new ImageIcon(logo.getScaledInstance(160, 80, Image.SCALE_SMOOTH)));
        } else {
            lblLogo.setText("CarZone");
            lblLogo.setFont(MainFrame.f(Font.BOLD, 18));
            lblLogo.setForeground(C_PRIMARY);
        }
        card.add(lblLogo, BorderLayout.WEST);

        // Thông tin bên phải
        JPanel pnlInfo = new JPanel();
        pnlInfo.setOpaque(false);
        pnlInfo.setLayout(new BoxLayout(pnlInfo, BoxLayout.Y_AXIS));

        // Tên + badge version
        JPanel pnlNameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlNameRow.setOpaque(false);

        JLabel lblName = new JLabel("CARZONE");
        lblName.setFont(MainFrame.f(Font.BOLD, 22));
        lblName.setForeground(C_TEXT_HEAD);
        pnlNameRow.add(lblName);

        // Badge "CarZone 1.0"
        JLabel badge = new JLabel("CarZone 1.0") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_PRIMARY_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(C_PRIMARY_BR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(MainFrame.f(Font.BOLD, 11));
        badge.setForeground(C_PRIMARY);
        badge.setOpaque(false);
        badge.setBorder(new EmptyBorder(3, 10, 3, 10));
        badge.setPreferredSize(new Dimension(88, 24));
        pnlNameRow.add(badge);

        pnlInfo.add(pnlNameRow);
        pnlInfo.add(Box.createVerticalStrut(6));

        JLabel lblSubtitle = new JLabel("Hệ thống quản lý gara ô tô");
        lblSubtitle.setFont(MainFrame.f(Font.BOLD, 14));
        lblSubtitle.setForeground(C_TEXT_MAIN);
        lblSubtitle.setBorder(new EmptyBorder(0, 0, 0, 0));
        pnlInfo.add(lblSubtitle);
        pnlInfo.add(Box.createVerticalStrut(8));

        JLabel lblDesc = new JLabel(
                "<html><body style='width:560px'>"
                + "Carzone kết nối các công việc từ tiếp nhận xe, sửa chữa đến thanh toán trong một hệ thống. "
                + "Giúp gara quản lý dữ liệu tập trung, phối hợp công việc và theo dõi hoạt động kinh doanh."
                + "</body></html>"
        );
        lblDesc.setFont(MainFrame.f(Font.PLAIN, 13));
        lblDesc.setForeground(C_TEXT_SUBTLE);
        pnlInfo.add(lblDesc);

        card.add(pnlInfo, BorderLayout.CENTER);
        return card;
    }

    // ── 3 card vai trò ────────────────────────────────────────────────────────
    private JPanel buildRoleCards() {
        JPanel row = new JPanel(new GridLayout(1, 3, 12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        row.add(buildRoleCard("nav_thong_ke.png", "Chủ cửa hàng",
                "Quản lý vận hành, doanh thu, nhân viên và phân quyền."));
        row.add(buildRoleCard("nav_yeu_cau.png", "Tiếp nhận viên",
                "Quản lý khách hàng, tiếp nhận xe, lập phiếu và thanh toán."));
        row.add(buildRoleCard("nav_dich_vu.png", "Kỹ thuật viên",
                "Theo dõi phân công, cập nhật sửa chữa và yêu cầu linh kiện."));
        return row;
    }

    private JPanel buildRoleCard(String iconFile, String title, String desc) {
        JPanel card = new RoundedCardPanel();
        card.setLayout(new BorderLayout(0, 8));

        // Icon + Tên vai trò
        JPanel pnlTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlTop.setOpaque(false);

        Image rawIcon = loadImage(iconFile);
        if (rawIcon != null) {
            pnlTop.add(new JLabel(new ImageIcon(rawIcon.getScaledInstance(20, 20, Image.SCALE_SMOOTH))));
        }

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(MainFrame.f(Font.BOLD, 14));
        lblTitle.setForeground(C_TEXT_HEAD);
        pnlTop.add(lblTitle);

        card.add(pnlTop, BorderLayout.NORTH);

        JLabel lblDesc = new JLabel(
                "<html><body style='width:180px'>" + desc + "</body></html>"
        );
        lblDesc.setFont(MainFrame.f(Font.PLAIN, 12));
        lblDesc.setForeground(C_TEXT_SUBTLE);
        card.add(lblDesc, BorderLayout.CENTER);

        return card;
    }

    // ── Lưới 6 phân hệ chính ──────────────────────────────────────────────────
    private JPanel buildModuleGrid() {
        JPanel grid = new JPanel(new GridLayout(3, 2, 12, 12));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        String[][] modules = {
            {"nav_tra_cuu.png",   "Khách hàng và xe",         "Hồ sơ khách hàng, thông tin xe và lịch sử sửa chữa."},
            {"nav_yeu_cau.png",   "Tiếp nhận và sửa chữa",    "Lập phiếu, phân công kỹ thuật viên và theo dõi tiến độ."},
            {"nav_dich_vu.png",   "Dịch vụ và linh kiện",     "Danh mục dịch vụ, giá bán, nhập kho và tồn kho."},
            {"nav_khuyen_mai.png","Hóa đơn và khuyến mãi",    "Tính chi phí, áp dụng ưu đãi, thanh toán và đổi trả."},
            {"nav_thong_ke.png",  "Báo cáo doanh thu",        "Thống kê kết quả kinh doanh, tra cứu theo thời gian."},
            {"nav_nhan_vien.png", "Nhân viên và phân quyền",  "Quản lý hồ sơ, tài khoản và quyền truy cập theo vai trò."}
        };

        for (String[] m : modules) {
            grid.add(buildModuleCard(m[0], m[1], m[2]));
        }
        return grid;
    }

    private JPanel buildModuleCard(String iconFile, String title, String desc) {
        JPanel card = new RoundedCardPanel();
        card.setLayout(new BorderLayout(14, 0));
        card.setPreferredSize(new Dimension(0, 80));

        // Icon bên trái
        JLabel lblIcon = new JLabel();
        lblIcon.setPreferredSize(new Dimension(40, 40));
        lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
        lblIcon.setVerticalAlignment(SwingConstants.CENTER);
        Image rawIcon = loadImage(iconFile);
        if (rawIcon != null) {
            lblIcon.setIcon(new ImageIcon(rawIcon.getScaledInstance(28, 28, Image.SCALE_SMOOTH)));
        }

        // Tạo nền bo tròn cho icon
        JPanel iconBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xF1F5F9));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBox.setOpaque(false);
        iconBox.setPreferredSize(new Dimension(48, 48));
        iconBox.setLayout(new GridBagLayout());
        iconBox.add(lblIcon);

        card.add(iconBox, BorderLayout.WEST);

        // Nội dung
        JPanel pnlText = new JPanel();
        pnlText.setOpaque(false);
        pnlText.setLayout(new BoxLayout(pnlText, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(MainFrame.f(Font.BOLD, 13));
        lblTitle.setForeground(C_TEXT_HEAD);
        pnlText.add(lblTitle);
        pnlText.add(Box.createVerticalStrut(4));

        JLabel lblDesc = new JLabel("<html><body style='width:260px'>" + desc + "</body></html>");
        lblDesc.setFont(MainFrame.f(Font.PLAIN, 12));
        lblDesc.setForeground(C_TEXT_SUBTLE);
        pnlText.add(lblDesc);

        card.add(pnlText, BorderLayout.CENTER);
        return card;
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new RoundedCardPanel();
        footer.setLayout(new BorderLayout(0, 0));
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        footer.setPreferredSize(new Dimension(0, 80));

        // Trái: copyright
        JPanel pnlLeft = new JPanel();
        pnlLeft.setOpaque(false);
        pnlLeft.setLayout(new BoxLayout(pnlLeft, BoxLayout.Y_AXIS));
        pnlLeft.setBorder(new EmptyBorder(2, 0, 2, 0));

        JLabel lblApp = new JLabel("Ứng dụng quản lý gara · Phiên bản 1.0");
        lblApp.setFont(MainFrame.f(Font.PLAIN, 12));
        lblApp.setForeground(C_TEXT_SUBTLE);
        pnlLeft.add(lblApp);

        JLabel lblCopy = new JLabel("© 2026 Carzone. Bảo lưu mọi quyền.");
        lblCopy.setFont(MainFrame.f(Font.PLAIN, 12));
        lblCopy.setForeground(C_TEXT_SUBTLE);
        pnlLeft.add(lblCopy);

        footer.add(pnlLeft, BorderLayout.WEST);

        // Phải: nút "Xem hướng dẫn sử dụng"
        JButton btnHelp = new MainFrame.RoundedButton("  \uD83D\uDCD6  Xem hướng dẫn sử dụng", 10,
                C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnHelp.setFont(MainFrame.f(Font.BOLD, 13));
        btnHelp.setForeground(C_TEXT_MAIN);
        btnHelp.setMargin(new java.awt.Insets(4, 16, 4, 16));
        if (onNavigateToHelp != null) {
            btnHelp.addActionListener(e -> onNavigateToHelp.run());
        }

        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 14));
        pnlRight.setOpaque(false);
        pnlRight.add(btnHelp);
        footer.add(pnlRight, BorderLayout.EAST);

        return footer;
    }

    // ── Label tiêu đề section ─────────────────────────────────────────────────
    private JPanel sectionLabel(String text) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel lbl = new JLabel(text);
        lbl.setFont(MainFrame.f(Font.BOLD, 16));
        lbl.setForeground(C_TEXT_HEAD);
        wrapper.add(lbl);
        return wrapper;
    }

    // ── Helper: card nền trắng bo góc ────────────────────────────────────────
    private static class RoundedCardPanel extends JPanel {
        RoundedCardPanel() {
            setOpaque(false);
            setBorder(new EmptyBorder(16, 18, 16, 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(new Color(0xE2E8F0));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ── Helper: tải ảnh từ classpath hoặc img/ ────────────────────────────────
    static Image loadImage(String fileName) {
        try {
            URL url = VeCarZoneUI.class.getResource("/" + fileName);
            if (url == null) url = VeCarZoneUI.class.getResource(fileName);
            if (url != null) return ImageIO.read(url);
            File f = new File("img/" + fileName);
            if (f.exists()) return ImageIO.read(f);
        } catch (Exception ignored) {}
        return null;
    }
}
