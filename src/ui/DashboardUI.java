package ui;

import connectDB.DbUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import entity.NhanVien;
import enums.TrangThaiPhieu;
import enums.VaiTro;
import controller.AuthController;
import controller.AuthController.NavigationItem;
import controller.DashboardController;
import controller.DashboardController.*;
import session.UserSession;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Màn hình Dashboard phân quyền theo vai trò (Chủ cửa hàng, Tiếp nhận viên, Kỹ thuật viên).
 * Chuẩn mực tuyệt đối theo tài liệu thiết kế và hình ảnh inspect từ Figma.
 *
 * Cấu trúc chung:
 * - Header (70px): Logo CarZone, Search bar bo tròn, Nút Help, Nút About, Avatar & Thông tin nhân viên, Nút Đăng xuất.
 * - Sidebar Menu (235px): Danh mục chức năng tùy biến động theo vai trò xác thực từ session.
 * - Content Area: CardLayout thay đổi linh hoạt theo navigation hướng đến.
 *   + NAV_DASHBOARD: Dashboard chi tiết tương ứng vai trò.
 *   + Các chức năng khác: View phân hệ nghiệp vụ chuẩn giao diện FlatLightLaf + Figma.
 *   + NAV_THONG_TIN_CA_NHAN: Hồ sơ thông tin cá nhân nhân viên.
 */
public class DashboardUI extends JFrame {

    // ── Bảng màu chuẩn tuyệt đối từ Figma ────────────────────────────────────
    private static final Color C_BG           = new Color(0xF8FAFC); // Nền app #F8FAFC
    private static final Color C_WHITE        = Color.WHITE;         // Nền card, header, sidebar
    private static final Color C_BORDER       = new Color(0xE2E8F0); // Viền card #E2E8F0
    private static final Color C_INPUT_STROKE = new Color(0xCBD5E1); // Viền input
    private static final Color C_TEXT_HEAD    = new Color(0x0F172A); // Tiêu đề lớn #0F172A
    private static final Color C_TEXT_MAIN    = new Color(0x334155); // Nhãn chính #334155
    private static final Color C_TEXT_SUBTLE  = new Color(0x64748B); // Text phụ #64748B
    private static final Color C_TEXT_MUTED   = new Color(0x94A3B8); // Muted text #94A3B8
    private static final Color C_PRIMARY      = new Color(0x2563EB); // Xanh chủ đạo #2563EB
    private static final Color C_PRIMARY_HV   = new Color(0x1D4ED8); // Hover nút chính
    private static final Color C_PRIMARY_BG   = new Color(0xEFF6FF); // Nền active / thẻ phụ #EFF6FF
    private static final Color C_PRIMARY_BR   = new Color(0xBFDBFE); // Viền nút Help #BFDBFE
    private static final Color C_DANGER       = new Color(0xDC2626); // Đỏ cảnh báo #DC2626
    private static final Color C_DANGER_BG    = new Color(0xFEF2F2); // Nền đỏ cảnh báo #FEF2F2
    private static final Color C_WARN         = new Color(0xD97706); // Cam / vàng #D97706
    private static final Color C_WARN_BG      = new Color(0xFFFBEB); // Nền vàng nhạt
    private static final Color C_SUCCESS      = new Color(0x059669); // Xanh lá #059669
    private static final Color C_SUCCESS_BG   = new Color(0xECFDF5); // Nền xanh lá nhạt

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

    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###");
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy");

    // ── Thuộc tính phiên làm việc và điều khiển giao diện ─────────────────────
    private final NhanVien nhanVien;
    private final DashboardController dashboardController = new DashboardController();
    private final AuthController authController = new AuthController();
    private CardLayout contentCardLayout;
    private JPanel pnlContentContainer;
    private final Map<String, JButton> mapNavButtons = new HashMap<>();
    private String activeNavId = "NAV_DASHBOARD";

    public DashboardUI(NhanVien nhanVien) {
        if (UserSession.getInstance().daDangNhap()) {
            this.nhanVien = UserSession.getInstance().getNhanVien();
        } else if (nhanVien != null) {
            this.nhanVien = nhanVien;
            UserSession.getInstance().batDauPhien(null, nhanVien);
        } else {
            this.nhanVien = taoNhanVienMacDinh();
            UserSession.getInstance().batDauPhien(null, this.nhanVien);
        }

        String tenVaiTro = AuthController.layTenVaiTro(this.nhanVien.getVaiTro());
        setTitle("CarZone – " + tenVaiTro + " (" + this.nhanVien.getHoTen() + ")");
        setSize(1440, 900);
        setMinimumSize(new Dimension(1200, 760));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(C_BG);

        // Icon ứng dụng
        ImageIcon appIcon = loadLogoImage(32, 32);
        if (appIcon != null) {
            setIconImage(appIcon.getImage());
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                DbUtil.close();
            }
        });

        initComponents();
    }

    private static NhanVien taoNhanVienMacDinh() {
        NhanVien nv = new NhanVien("CCH001");
        nv.setHoTen("Nguyễn Minh Đức");
        nv.setVaiTro(VaiTro.CHU_CUA_HANG);
        return nv;
    }

    private static String layTenVaiTro(VaiTro vaiTro) {
        if (vaiTro == null) return "Chủ cửa hàng";
        return switch (vaiTro) {
            case CHU_CUA_HANG   -> "Chủ cửa hàng";
            case TIEP_NHAN_VIEN -> "Tiếp nhận viên";
            case KY_THUAT_VIEN  -> "Kỹ thuật viên";
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // KHUNG CHÍNH (BorderLayout: Header ở Bắc, Khối Sidebar + Content ở Giữa)
    // ─────────────────────────────────────────────────────────────────────────
    private void initComponents() {
        setLayout(new BorderLayout(0, 0));

        // 1. Header trên cùng
        add(buildHeader(), BorderLayout.NORTH);

        // 2. Vùng trung tâm: Sidebar menu trái + Vùng nội dung phải (CardLayout)
        JPanel centerPanel = new JPanel(new BorderLayout(0, 0));
        centerPanel.setOpaque(false);

        centerPanel.add(buildSidebar(), BorderLayout.WEST);

        contentCardLayout = new CardLayout();
        pnlContentContainer = new JPanel(contentCardLayout);
        pnlContentContainer.setOpaque(false);

        // Nạp tất cả các module view theo phân quyền role từ session
        napCacTrangNoiDung();

        centerPanel.add(pnlContentContainer, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. HEADER (70px cố định)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 0));
        header.setBackground(C_WHITE);
        header.setPreferredSize(new Dimension(1440, 70));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));

        // ── Nhánh trái: Logo + App Subtitle ──────────────────────────────────
        JPanel pnlLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 16));
        pnlLeft.setOpaque(false);

        JComponent logoComp = createLogoComponent(120, 35);
        pnlLeft.add(logoComp);

        JLabel lblSub = new JLabel("Hệ thống quản lý gara ô tô");
        lblSub.setFont(f(Font.PLAIN, 12));
        lblSub.setForeground(C_TEXT_SUBTLE);
        pnlLeft.add(lblSub);

        header.add(pnlLeft, BorderLayout.WEST);

        // ── Nhánh giữa: Search bar bo tròn với kính lúp theo Figma (Responsive) ──
        JPanel pnlCenter = new JPanel(new GridBagLayout());
        pnlCenter.setOpaque(false);
        pnlCenter.setBorder(new EmptyBorder(0, 16, 0, 16));

        String searchPlaceholder = switch (nhanVien.getVaiTro()) {
            case CHU_CUA_HANG   -> "Tìm kiếm dịch vụ, linh kiện…";
            case TIEP_NHAN_VIEN -> "Tìm khách hàng, xe, phiếu…";
            case KY_THUAT_VIEN  -> "Tìm phiếu, linh kiện…";
        };

        GridBagConstraints gbcSearch = new GridBagConstraints();
        gbcSearch.gridx = 0;
        gbcSearch.gridy = 0;
        gbcSearch.weightx = 1.0;
        gbcSearch.fill = GridBagConstraints.HORIZONTAL;
        pnlCenter.add(new HeaderSearchBox(searchPlaceholder), gbcSearch);
        header.add(pnlCenter, BorderLayout.CENTER);

        // ── Nhánh phải: Help, About, User Profile, Đăng xuất ─────────────────
        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        pnlRight.setOpaque(false);

        // Nút Help
        JButton btnHelp = new RoundedButton("?  Help", 12, C_PRIMARY_BG, new Color(0xDBEAFE), C_PRIMARY_BR);
        btnHelp.setFont(f(Font.BOLD, 12));
        btnHelp.setForeground(C_PRIMARY);
        btnHelp.setPreferredSize(new Dimension(86, 36));
        btnHelp.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Hệ thống Quản lý Gara Ô tô CarZone 1.0\nHướng dẫn sử dụng: Tra cứu tài liệu nghiệp vụ hoặc liên hệ quản trị viên.",
                "Trợ giúp CarZone", JOptionPane.INFORMATION_MESSAGE));
        pnlRight.add(btnHelp);

        // Nút About
        JButton btnAbout = new RoundedButton("ⓘ  About", 12, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnAbout.setFont(f(Font.PLAIN, 12));
        btnAbout.setForeground(C_TEXT_MAIN);
        btnAbout.setPreferredSize(new Dimension(88, 36));
        btnAbout.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "CarZone - Phiên bản 1.0.0 (Nhóm 01 - ĐH Công nghiệp TP.HCM)\nĐồ án môn Phát triển ứng dụng.\n© 2026 Carzone. Bảo lưu mọi quyền.",
                "Về CarZone", JOptionPane.INFORMATION_MESSAGE));
        pnlRight.add(btnAbout);

        // Vạch phân cách mỏng
        JSeparator sep1 = new JSeparator(SwingConstants.VERTICAL);
        sep1.setPreferredSize(new Dimension(1, 24));
        sep1.setForeground(C_BORDER);
        pnlRight.add(sep1);

        // Avatar tròn hiển thị ký tự đầu
        String initial = (nhanVien.getHoTen() != null && !nhanVien.getHoTen().isBlank())
                ? String.valueOf(nhanVien.getHoTen().trim().charAt(0)).toUpperCase() : "U";
        JLabel lblAvatar = new JLabel(initial, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_PRIMARY_BG);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(C_PRIMARY_BR);
                g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblAvatar.setPreferredSize(new Dimension(36, 36));
        lblAvatar.setFont(f(Font.BOLD, 15));
        lblAvatar.setForeground(C_PRIMARY);
        lblAvatar.setOpaque(false);
        pnlRight.add(lblAvatar);

        // Cụm thông tin: Tên & Vai trò
        JPanel pnlUserText = new JPanel();
        pnlUserText.setLayout(new BoxLayout(pnlUserText, BoxLayout.Y_AXIS));
        pnlUserText.setOpaque(false);

        JLabel lblName = new JLabel(nhanVien.getHoTen());
        lblName.setFont(f(Font.BOLD, 13));
        lblName.setForeground(C_TEXT_HEAD);
        pnlUserText.add(lblName);

        String roleStr = layTenVaiTro(nhanVien.getVaiTro()) + " · " + (nhanVien.getMaNV() != null ? nhanVien.getMaNV() : "");
        JLabel lblRole = new JLabel(roleStr);
        lblRole.setFont(f(Font.PLAIN, 11));
        lblRole.setForeground(C_TEXT_SUBTLE);
        pnlUserText.add(lblRole);

        pnlRight.add(pnlUserText);

        // Vạch phân cách mỏng
        JSeparator sep2 = new JSeparator(SwingConstants.VERTICAL);
        sep2.setPreferredSize(new Dimension(1, 24));
        sep2.setForeground(C_BORDER);
        pnlRight.add(sep2);

        // Nút Đăng xuất
        JButton btnLogout = new RoundedButton("Đăng xuất", 10, C_WHITE, C_DANGER_BG, C_BORDER);
        btnLogout.setFont(f(Font.BOLD, 12));
        btnLogout.setForeground(C_DANGER);
        btnLogout.setPreferredSize(new Dimension(96, 36));
        btnLogout.addActionListener(e -> xuLyDangXuat());
        pnlRight.add(btnLogout);

        header.add(pnlRight, BorderLayout.EAST);
        return header;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. SIDEBAR MENU ĐỘNG THEO VAI TRÒ XÁC THỰC TỪ SESSION (235px)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(C_WHITE);
        sidebar.setPreferredSize(new Dimension(235, 800));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, C_BORDER),
                new EmptyBorder(12, 8, 12, 8)
        ));

        // Lấy danh sách menu items tương ứng theo Role được cấp trong Session
        List<NavigationItem> menuItems = authController.layDanhSachNavigationTheoRole(nhanVien.getVaiTro());
        mapNavButtons.clear();

        for (NavigationItem item : menuItems) {
            JButton btnItem = createSidebarButton(item);
            btnItem.setAlignmentX(Component.CENTER_ALIGNMENT);
            mapNavButtons.put(item.getId(), btnItem);
            sidebar.add(btnItem);
            sidebar.add(Box.createVerticalStrut(4));
        }

        // Đẩy footer khoảng trống xuống đáy
        sidebar.add(Box.createVerticalGlue());

        // Dòng phiên bản nhỏ ở đáy sidebar
        JLabel lblVer = new JLabel("CarZone v1.0 • Desktop");
        lblVer.setFont(f(Font.PLAIN, 11));
        lblVer.setForeground(C_TEXT_MUTED);
        lblVer.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(lblVer);

        return sidebar;
    }

    private String formatSidebarLabel(String text) {
        if (text.equals("Yêu cầu cập nhật linh kiện")) return "Yêu cầu cập nhật<br>linh kiện";
        if (text.equals("Lập phiếu tiếp nhận – sửa chữa")) return "Lập phiếu tiếp nhận –<br>sửa chữa";
        if (text.equals("Chương trình khuyến mãi")) return "Chương trình<br>khuyến mãi";
        if (text.equals("Tài khoản và phân quyền")) return "Tài khoản và<br>phân quyền";
        if (text.equals("Hồ sơ khách hàng và xe")) return "Hồ sơ khách hàng<br>và xe";
        return text;
    }

    private JButton createSidebarButton(NavigationItem item) {
        String wrapped = formatSidebarLabel(item.getTieuDe());
        boolean isMultiLine = wrapped.contains("<br>");
        String htmlText = "<html><body style='margin:0; padding:0; line-height: 1.15;'>" + wrapped + "</body></html>";

        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isActive = item.getId().equals(activeNavId);
                Color bg;
                if (isActive) {
                    bg = C_PRIMARY_BG;
                } else if (getModel().isRollover()) {
                    bg = new Color(0xF1F5F9);
                } else {
                    bg = C_WHITE;
                }
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                if (isActive) {
                    g2.setColor(C_PRIMARY_BR);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setLayout(new BorderLayout(8, 0));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(isMultiLine ? 4 : 5, 8, isMultiLine ? 4 : 5, 8));

        int btnH = isMultiLine ? 44 : 36;
        btn.setMaximumSize(new Dimension(216, btnH));
        btn.setPreferredSize(new Dimension(216, btnH));

        JLabel lblIcon = new JLabel(item.getIcon(), SwingConstants.CENTER);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        lblIcon.setPreferredSize(new Dimension(24, 24));
        btn.add(lblIcon, BorderLayout.WEST);

        JLabel lblText = new JLabel(htmlText);
        lblText.setFont(f(Font.PLAIN, 11));
        lblText.setForeground(C_TEXT_MAIN);
        btn.add(lblText, BorderLayout.CENTER);

        btn.addActionListener(e -> chuyenTrang(item.getId()));

        return btn;
    }

    /**
     * Chuyển đổi vùng nội dung contentPanel dựa trên Navigation được chọn.
     * Cập nhật trạng thái active/inactive của tất cả các nút navigation bên sidebar.
     */
    public void chuyenTrang(String navId) {
        if (navId == null || navId.equals(activeNavId)) return;
        this.activeNavId = navId;

        // Cập nhật card layout
        if (contentCardLayout != null && pnlContentContainer != null) {
            contentCardLayout.show(pnlContentContainer, navId);
        }

        // Cập nhật giao diện toàn bộ nút navigation
        for (Map.Entry<String, JButton> entry : mapNavButtons.entrySet()) {
            boolean isCur = entry.getKey().equals(activeNavId);
            JButton b = entry.getValue();
            Component[] comps = b.getComponents();
            for (Component c : comps) {
                if (c instanceof JLabel lbl && !lbl.getText().matches("^[\\p{So}\\p{Sm}].*")) {
                    lbl.setFont(f(isCur ? Font.BOLD : Font.PLAIN, 11));
                    lbl.setForeground(isCur ? C_PRIMARY : C_TEXT_MAIN);
                }
            }
            b.repaint();
        }
    }

    /**
     * Nạp toàn bộ các phân hệ nội dung (Card) vào contentPanel tương ứng với vai trò.
     */
    private void napCacTrangNoiDung() {
        List<NavigationItem> items = authController.layDanhSachNavigationTheoRole(nhanVien.getVaiTro());

        for (NavigationItem item : items) {
            if ("NAV_DASHBOARD".equals(item.getId())) {
                pnlContentContainer.add(buildRoleDashboardContent(), item.getId());
            } else if ("NAV_THONG_TIN_CA_NHAN".equals(item.getId())) {
                pnlContentContainer.add(buildProfileView(item), item.getId());
            } else {
                pnlContentContainer.add(buildGenericModuleView(item), item.getId());
            }
        }
    }

    /**
     * Màn hình Thông tin cá nhân nhân viên (NAV_THONG_TIN_CA_NHAN).
     * Dữ liệu lấy trực tiếp từ đối tượng NhanVien & TaiKhoan trong Session.
     */
    private JComponent buildProfileView(NavigationItem item) {
        ScrollableContentPanel pnl = new ScrollableContentPanel();
        pnl.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header tiêu đề trang
        JPanel pnlHeader = new JPanel(new BorderLayout(0, 0));
        pnlHeader.setOpaque(false);
        pnlHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        JPanel pnlTitles = new JPanel(new GridLayout(2, 1, 0, 2));
        pnlTitles.setOpaque(false);
        JLabel lblBreadcrumb = new JLabel(item.getBreadcrumb());
        lblBreadcrumb.setFont(f(Font.PLAIN, 12));
        lblBreadcrumb.setForeground(C_TEXT_MUTED);
        pnlTitles.add(lblBreadcrumb);

        JLabel lblTitle = new JLabel("Hồ sơ thông tin cá nhân");
        lblTitle.setFont(f(Font.BOLD, 22));
        lblTitle.setForeground(C_TEXT_HEAD);
        pnlTitles.add(lblTitle);
        pnlHeader.add(pnlTitles, BorderLayout.WEST);

        // Badge vai trò
        JLabel lblRoleBadge = new JLabel("Vai trò: " + AuthController.layTenVaiTro(nhanVien.getVaiTro()), SwingConstants.CENTER);
        lblRoleBadge.setFont(f(Font.BOLD, 12));
        lblRoleBadge.setForeground(C_PRIMARY);
        lblRoleBadge.setBackground(C_PRIMARY_BG);
        lblRoleBadge.setOpaque(true);
        lblRoleBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_PRIMARY_BR, 1),
                new EmptyBorder(6, 14, 6, 14)
        ));
        pnlHeader.add(lblRoleBadge, BorderLayout.EAST);
        pnl.add(pnlHeader);

        pnl.add(Box.createVerticalStrut(20));

        // Card chi tiết hồ sơ
        JPanel cardProfile = new JPanel(new BorderLayout(24, 0));
        cardProfile.setBackground(C_WHITE);
        cardProfile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(24, 24, 24, 24)
        ));
        cardProfile.setMaximumSize(new Dimension(Integer.MAX_VALUE, 380));

        // Cột trái: Avatar lớn & Tên
        JPanel pnlAvatarCol = new JPanel();
        pnlAvatarCol.setLayout(new BoxLayout(pnlAvatarCol, BoxLayout.Y_AXIS));
        pnlAvatarCol.setOpaque(false);
        pnlAvatarCol.setPreferredSize(new Dimension(200, 320));

        String initial = (nhanVien.getHoTen() != null && !nhanVien.getHoTen().isBlank())
                ? String.valueOf(nhanVien.getHoTen().trim().charAt(0)).toUpperCase() : "U";
        JLabel lblBigAvatar = new JLabel(initial, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_PRIMARY_BG);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(C_PRIMARY_BR);
                g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblBigAvatar.setPreferredSize(new Dimension(84, 84));
        lblBigAvatar.setMaximumSize(new Dimension(84, 84));
        lblBigAvatar.setFont(f(Font.BOLD, 36));
        lblBigAvatar.setForeground(C_PRIMARY);
        lblBigAvatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlAvatarCol.add(lblBigAvatar);

        pnlAvatarCol.add(Box.createVerticalStrut(14));

        JLabel lblProfileName = new JLabel(nhanVien.getHoTen());
        lblProfileName.setFont(f(Font.BOLD, 16));
        lblProfileName.setForeground(C_TEXT_HEAD);
        lblProfileName.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlAvatarCol.add(lblProfileName);

        pnlAvatarCol.add(Box.createVerticalStrut(4));

        JLabel lblProfileId = new JLabel(nhanVien.getMaNV() != null ? nhanVien.getMaNV() : "");
        lblProfileId.setFont(f(Font.PLAIN, 12));
        lblProfileId.setForeground(C_TEXT_SUBTLE);
        lblProfileId.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlAvatarCol.add(lblProfileId);

        pnlAvatarCol.add(Box.createVerticalStrut(12));

        JLabel lblStatusTag = new JLabel("● Đang hoạt động", SwingConstants.CENTER);
        lblStatusTag.setFont(f(Font.BOLD, 11));
        lblStatusTag.setForeground(C_SUCCESS);
        lblStatusTag.setBackground(C_SUCCESS_BG);
        lblStatusTag.setOpaque(true);
        lblStatusTag.setBorder(new EmptyBorder(4, 10, 4, 10));
        lblStatusTag.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlAvatarCol.add(lblStatusTag);

        cardProfile.add(pnlAvatarCol, BorderLayout.WEST);

        // Cột phải: Grid thông tin chi tiết
        JPanel pnlInfoGrid = new JPanel(new GridLayout(4, 2, 16, 14));
        pnlInfoGrid.setOpaque(false);

        String ngaySinhStr = (nhanVien.getNgaySinh() != null) ? DATE_FMT.format(nhanVien.getNgaySinh()) : "Chưa cập nhật";
        String ngayVaoLamStr = (nhanVien.getNgayVaoLam() != null) ? DATE_FMT.format(nhanVien.getNgayVaoLam()) : "Chưa cập nhật";
        String tenDN = UserSession.getInstance().getTenDangNhap();
        if (tenDN.isBlank()) tenDN = nhanVien.getMaNV();

        pnlInfoGrid.add(createInfoField("Họ và tên", nhanVien.getHoTen()));
        pnlInfoGrid.add(createInfoField("Mã nhân viên", nhanVien.getMaNV()));
        pnlInfoGrid.add(createInfoField("Tên đăng nhập hệ thống", tenDN));
        pnlInfoGrid.add(createInfoField("Vai trò nghiệp vụ", AuthController.layTenVaiTro(nhanVien.getVaiTro())));
        pnlInfoGrid.add(createInfoField("Số điện thoại liên hệ", nhanVien.getSoDienThoai() != null ? nhanVien.getSoDienThoai() : "N/A"));
        pnlInfoGrid.add(createInfoField("Số Căn cước công dân (CCCD)", nhanVien.getCCCD() != null ? nhanVien.getCCCD() : "N/A"));
        pnlInfoGrid.add(createInfoField("Địa chỉ thường trú", nhanVien.getDiaChi() != null ? nhanVien.getDiaChi() : "N/A"));
        pnlInfoGrid.add(createInfoField("Ngày bắt đầu công tác", ngayVaoLamStr));

        cardProfile.add(pnlInfoGrid, BorderLayout.CENTER);

        pnl.add(cardProfile);

        pnl.add(Box.createVerticalStrut(18));

        // Hàng nút tác vụ
        JPanel pnlActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        pnlActions.setOpaque(false);
        pnlActions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JButton btnChangePass = new RoundedButton("Đổi mật khẩu", 12, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnChangePass.setFont(f(Font.BOLD, 12));
        btnChangePass.setForeground(C_TEXT_MAIN);
        btnChangePass.setPreferredSize(new Dimension(130, 38));
        btnChangePass.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Chức năng đổi mật khẩu đang được bảo mật trong phiên làm việc của " + nhanVien.getHoTen() + ".",
                "Đổi mật khẩu", JOptionPane.INFORMATION_MESSAGE));
        pnlActions.add(btnChangePass);

        JButton btnLogoutSelf = new RoundedButton("Đăng xuất tài khoản", 12, C_DANGER_BG, new Color(0xFEE2E2), C_DANGER);
        btnLogoutSelf.setFont(f(Font.BOLD, 12));
        btnLogoutSelf.setForeground(C_DANGER);
        btnLogoutSelf.setPreferredSize(new Dimension(150, 38));
        btnLogoutSelf.addActionListener(e -> xuLyDangXuat());
        pnlActions.add(btnLogoutSelf);

        pnl.add(pnlActions);

        JScrollPane scroll = new JScrollPane(pnl);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }

    private JPanel createInfoField(String label, String value) {
        JPanel f = new JPanel();
        f.setLayout(new BoxLayout(f, BoxLayout.Y_AXIS));
        f.setBackground(new Color(0xF8FAFC));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel lbl = new JLabel(label);
        lbl.setFont(f(Font.PLAIN, 11));
        lbl.setForeground(C_TEXT_SUBTLE);
        f.add(lbl);

        f.add(Box.createVerticalStrut(3));

        JLabel val = new JLabel(value != null && !value.isBlank() ? value : "—");
        val.setFont(f(Font.BOLD, 13));
        val.setForeground(C_TEXT_HEAD);
        f.add(val);

        return f;
    }

    /**
     * View phân hệ nghiệp vụ theo danh mục Navigation.
     * Tự động truy vấn dữ liệu thực tế từ cơ sở dữ liệu qua Hibernate.
     */
    private JComponent buildGenericModuleView(NavigationItem item) {
        ScrollableContentPanel pnl = new ScrollableContentPanel();
        pnl.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header phân hệ
        JPanel pnlHeader = new JPanel(new BorderLayout(0, 0));
        pnlHeader.setOpaque(false);
        pnlHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        JPanel pnlTitles = new JPanel(new GridLayout(2, 1, 0, 2));
        pnlTitles.setOpaque(false);
        JLabel lblBreadcrumb = new JLabel(item.getBreadcrumb());
        lblBreadcrumb.setFont(f(Font.PLAIN, 12));
        lblBreadcrumb.setForeground(C_TEXT_MUTED);
        pnlTitles.add(lblBreadcrumb);

        JLabel lblTitle = new JLabel(item.getIcon() + "  " + item.getTieuDe());
        lblTitle.setFont(f(Font.BOLD, 22));
        lblTitle.setForeground(C_TEXT_HEAD);
        pnlTitles.add(lblTitle);
        pnlHeader.add(pnlTitles, BorderLayout.WEST);

        // Badge ngày và vai trò
        JPanel pnlRightBadge = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlRightBadge.setOpaque(false);

        JLabel lblDate = new JLabel("Ngày: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lblDate.setFont(f(Font.PLAIN, 12));
        lblDate.setForeground(C_TEXT_SUBTLE);
        pnlRightBadge.add(lblDate);

        JLabel lblBadge = new JLabel(AuthController.layTenVaiTro(nhanVien.getVaiTro()), SwingConstants.CENTER);
        lblBadge.setFont(f(Font.BOLD, 11));
        lblBadge.setForeground(C_PRIMARY);
        lblBadge.setBackground(C_PRIMARY_BG);
        lblBadge.setOpaque(true);
        lblBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_PRIMARY_BR, 1),
                new EmptyBorder(4, 10, 4, 10)
        ));
        pnlRightBadge.add(lblBadge);
        pnlHeader.add(pnlRightBadge, BorderLayout.EAST);

        pnl.add(pnlHeader);
        pnl.add(Box.createVerticalStrut(16));

        // Toolbar tra cứu & làm mới dữ liệu
        JPanel pnlToolbar = new JPanel(new BorderLayout(12, 0));
        pnlToolbar.setOpaque(false);
        pnlToolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JTextField txtSearch = new JTextField();
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm kiếm trong " + item.getTieuDe() + "…");
        txtSearch.setPreferredSize(new Dimension(320, 36));
        txtSearch.setFont(f(Font.PLAIN, 12));
        pnlToolbar.add(txtSearch, BorderLayout.WEST);

        JPanel pnlToolsRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlToolsRight.setOpaque(false);

        JButton btnFilter = new RoundedButton("Bộ lọc", 10, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnFilter.setFont(f(Font.PLAIN, 12));
        btnFilter.setPreferredSize(new Dimension(84, 36));
        pnlToolsRight.add(btnFilter);

        JButton btnAction = new RoundedButton("+ Thao tác mới", 10, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
        btnAction.setFont(f(Font.BOLD, 12));
        btnAction.setForeground(C_WHITE);
        btnAction.setPreferredSize(new Dimension(120, 36));
        pnlToolsRight.add(btnAction);

        pnlToolbar.add(pnlToolsRight, BorderLayout.EAST);
        pnl.add(pnlToolbar);

        pnl.add(Box.createVerticalStrut(14));

        // Bảng dữ liệu thật từ cơ sở dữ liệu theo phân hệ
        JPanel cardTable = new JPanel(new BorderLayout(0, 0));
        cardTable.setBackground(C_WHITE);
        cardTable.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        cardTable.setMaximumSize(new Dimension(Integer.MAX_VALUE, 500));

        DefaultTableModel model = taoModelDuLieuTheoPhanHe(item.getId());
        JTable table = new JTable(model);
        cauHinhBang(table);

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(null);
        scrollTable.setPreferredSize(new Dimension(1000, 440));
        cardTable.add(scrollTable, BorderLayout.CENTER);

        pnl.add(cardTable);

        JScrollPane scroll = new JScrollPane(pnl);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }

    /**
     * Tạo dữ liệu JTable thật từ Database (không mockup).
     */
    private DefaultTableModel taoModelDuLieuTheoPhanHe(String navId) {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        if (!DbUtil.isDatabaseOnline()) {
            model.setColumnIdentifiers(new String[]{"Thông báo"});
            model.addRow(new Object[]{"Không thể kết nối đến cơ sở dữ liệu. Vui lòng kiểm tra Docker SQL Server."});
            return model;
        }

        try (Connection con = DbUtil.getConnection()) {
            switch (navId) {
                case "NAV_QUAN_LY_DICH_VU" -> {
                    model.setColumnIdentifiers(new String[]{"STT", "Mã DV", "Tên dịch vụ sửa chữa", "Đơn vị tính", "Đơn giá (VNĐ)", "Trạng thái"});
                    String sql = "SELECT maDV, tenDV, donViTinh, donGia, dangCungCap FROM DichVu ORDER BY maDV";
                    try (PreparedStatement ps = con.prepareStatement(sql);
                         ResultSet rs = ps.executeQuery()) {
                        int stt = 1;
                        while (rs.next()) {
                            model.addRow(new Object[]{
                                    stt++, rs.getString("maDV"), rs.getString("tenDV"), rs.getString("donViTinh"),
                                    MONEY_FMT.format(rs.getDouble("donGia")), rs.getBoolean("dangCungCap") ? "Đang cung cấp" : "Tạm ngưng"
                            });
                        }
                    }
                }
                case "NAV_LINH_KIEN_KHO" -> {
                    model.setColumnIdentifiers(new String[]{"STT", "Mã LK", "Tên linh kiện", "Đơn vị tính", "Đơn giá (VNĐ)", "Tồn kho", "Trạng thái"});
                    String sql = "SELECT maLK, tenLK, donViTinh, donGia, soLuongTon, dangKinhDoanh FROM LinhKien ORDER BY maLK";
                    try (PreparedStatement ps = con.prepareStatement(sql);
                         ResultSet rs = ps.executeQuery()) {
                        int stt = 1;
                        while (rs.next()) {
                            int ton = rs.getInt("soLuongTon");
                            boolean kd = rs.getBoolean("dangKinhDoanh");
                            model.addRow(new Object[]{
                                    stt++, rs.getString("maLK"), rs.getString("tenLK"), rs.getString("donViTinh"),
                                    MONEY_FMT.format(rs.getDouble("donGia")), ton,
                                    kd ? (ton > 0 ? "Còn hàng" : "Hết hàng") : "Ngừng kinh doanh"
                            });
                        }
                    }
                }
                case "NAV_KHACH_HANG_XE" -> {
                    model.setColumnIdentifiers(new String[]{"STT", "Mã KH", "Họ và tên khách hàng", "Số điện thoại", "Ghi chú", "Hạng thành viên"});
                    String sql = "SELECT TOP 50 maKH, hoTen, soDienThoai, ghiChu, thanhVienThanThiet FROM KhachHang ORDER BY maKH";
                    try (PreparedStatement ps = con.prepareStatement(sql);
                         ResultSet rs = ps.executeQuery()) {
                        int stt = 1;
                        while (rs.next()) {
                            String gc = rs.getString("ghiChu");
                            boolean tv = rs.getBoolean("thanhVienThanThiet");
                            model.addRow(new Object[]{
                                    stt++, rs.getString("maKH"), rs.getString("hoTen"), rs.getString("soDienThoai"),
                                    gc != null ? gc : "—",
                                    tv ? "Thân thiết" : "Tiêu chuẩn"
                            });
                        }
                    }
                }
                case "NAV_PHIEU_TIEP_NHAN", "NAV_PHIEU_PHAN_CONG", "NAV_CAP_NHAT_SUA_CHUA" -> {
                    model.setColumnIdentifiers(new String[]{"STT", "Mã phiếu", "Biển số xe", "Thời gian tiếp nhận", "Yêu cầu sửa chữa", "KTV phụ trách", "Trạng thái"});
                    String sql = "SELECT TOP 50 p.maPhieu, x.bienSoXe, p.ngayGioTiepNhan, p.yeuCauBanDau, nv.hoTen AS tenKtv, p.trangThai " +
                                 "FROM PhieuTiepNhanSuaChua p " +
                                 "LEFT JOIN Xe x ON p.maXe = x.maXe " +
                                 "LEFT JOIN NhanVien nv ON p.maNVSuaChua = nv.maNV " +
                                 "ORDER BY p.ngayGioTiepNhan DESC";
                    try (PreparedStatement ps = con.prepareStatement(sql);
                         ResultSet rs = ps.executeQuery()) {
                        int stt = 1;
                        while (rs.next()) {
                            String bienSo = rs.getString("bienSoXe");
                            if (bienSo == null) bienSo = "N/A";
                            java.sql.Timestamp tg = rs.getTimestamp("ngayGioTiepNhan");
                            String gio = tg != null ? DATE_FMT.format(tg) : "N/A";
                            String ktv = rs.getString("tenKtv");
                            if (ktv == null) ktv = "Chưa phân công";
                            model.addRow(new Object[]{
                                    stt++, rs.getString("maPhieu"), bienSo, gio, rs.getString("yeuCauBanDau"), ktv, rs.getString("trangThai")
                            });
                        }
                    }
                }
                case "NAV_HOA_DON", "NAV_THONG_KE" -> {
                    model.setColumnIdentifiers(new String[]{"STT", "Mã HĐ", "Mã phiếu sửa", "Thời gian lập", "Phương thức", "Tổng tiền thanh toán (VNĐ)", "Trạng thái"});
                    String sql = "SELECT TOP 50 h.maHoaDon, h.maPhieu, h.ngayLap, h.phuongThucTT, v.tongThanhToan, h.trangThaiThanhToan " +
                                 "FROM HoaDon h " +
                                 "LEFT JOIN vw_HoaDon_TongTien v ON h.maHoaDon = v.maHoaDon " +
                                 "ORDER BY h.ngayLap DESC";
                    try (PreparedStatement ps = con.prepareStatement(sql);
                         ResultSet rs = ps.executeQuery()) {
                        int stt = 1;
                        while (rs.next()) {
                            java.sql.Timestamp ngay = rs.getTimestamp("ngayLap");
                            String ngayStr = ngay != null ? DATE_FMT.format(ngay) : "N/A";
                            String pt = rs.getString("phuongThucTT");
                            model.addRow(new Object[]{
                                    stt++, rs.getString("maHoaDon"), rs.getString("maPhieu"), ngayStr,
                                    pt != null ? pt : "TIEN_MAT",
                                    MONEY_FMT.format(rs.getDouble("tongThanhToan")), rs.getString("trangThaiThanhToan")
                            });
                        }
                    }
                }
                case "NAV_NHAN_VIEN", "NAV_TAI_KHOAN" -> {
                    model.setColumnIdentifiers(new String[]{"STT", "Mã NV", "Họ và tên", "Số điện thoại", "Vai trò", "Ngày vào làm", "Tài khoản"});
                    String sql = "SELECT maNV, hoTen, soDienThoai, vaiTro, ngayVaoLam FROM NhanVien ORDER BY maNV";
                    try (PreparedStatement ps = con.prepareStatement(sql);
                         ResultSet rs = ps.executeQuery()) {
                        int stt = 1;
                        while (rs.next()) {
                            java.sql.Date nvl = rs.getDate("ngayVaoLam");
                            String ngay = nvl != null ? DATE_FMT.format(nvl) : "N/A";
                            String vtStr = rs.getString("vaiTro");
                            VaiTro vt = vtStr != null ? VaiTro.valueOf(vtStr) : null;
                            model.addRow(new Object[]{
                                    stt++, rs.getString("maNV"), rs.getString("hoTen"), rs.getString("soDienThoai"),
                                    AuthController.layTenVaiTro(vt), ngay, rs.getString("maNV")
                            });
                        }
                    }
                }
                default -> {
                    model.setColumnIdentifiers(new String[]{"Thông tin phân hệ", "Ghi chú"});
                    model.addRow(new Object[]{
                            "Phân hệ " + navId + " đang kết nối cơ sở dữ liệu thời gian thực.",
                            "Tài khoản đang đăng nhập: " + nhanVien.getHoTen() + " (" + AuthController.layTenVaiTro(nhanVien.getVaiTro()) + ")"
                    });
                }
            }
        } catch (Exception e) {
            model.setColumnIdentifiers(new String[]{"Lỗi truy vấn CSDL"});
            model.addRow(new Object[]{e.getMessage()});
        }

        if (model.getRowCount() == 0) {
            model.addRow(new Object[]{"Chưa có dữ liệu nào trong cơ sở dữ liệu."});
        }

        return model;
    }

    private void cauHinhBang(JTable table) {
        table.setFont(f(Font.PLAIN, 12));
        table.setRowHeight(36);
        table.setShowGrid(true);
        table.setGridColor(C_BORDER);
        table.setSelectionBackground(C_PRIMARY_BG);
        table.setSelectionForeground(C_PRIMARY);
        table.setFillsViewportHeight(true);

        JTableHeader th = table.getTableHeader();
        th.setFont(f(Font.BOLD, 12));
        th.setBackground(new Color(0xF1F5F9));
        th.setForeground(C_TEXT_HEAD);
        th.setPreferredSize(new Dimension(0, 36));
        th.setReorderingAllowed(false);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. VÙNG NỘI DUNG DASHBOARD THEO VAI TRÒ
    // ─────────────────────────────────────────────────────────────────────────
    private JComponent buildRoleDashboardContent() {
        JComponent content = switch (nhanVien.getVaiTro()) {
            case CHU_CUA_HANG   -> buildChuCuaHangContent();
            case TIEP_NHAN_VIEN -> buildTiepNhanVienContent();
            case KY_THUAT_VIEN  -> buildKyThuatVienContent();
        };

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3A. DASHBOARD CHỦ CỬA HÀNG (Bám sát image45.png từ Figma)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildChuCuaHangContent() {
        ChuCuaHangDashboardData data = dashboardController.layDataChuCuaHang();

        ScrollableContentPanel main = new ScrollableContentPanel();
        main.setBorder(new EmptyBorder(16, 20, 16, 20));

        // ── Tiêu đề trang + Ngày hiện tại ────────────────────────────────────
        JPanel pnlTitle = new JPanel(new BorderLayout(0, 0));
        pnlTitle.setOpaque(false);
        pnlTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel lblTitle = new JLabel("Dashboard");
        lblTitle.setFont(f(Font.BOLD, 22));
        lblTitle.setForeground(C_TEXT_HEAD);
        pnlTitle.add(lblTitle, BorderLayout.WEST);

        String ngayHienTai = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        JLabel lblDateBadge = new JLabel("📅  " + ngayHienTai);
        lblDateBadge.setFont(f(Font.BOLD, 12));
        lblDateBadge.setForeground(C_TEXT_MAIN);
        lblDateBadge.setBackground(C_WHITE);
        lblDateBadge.setOpaque(true);
        lblDateBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1, true),
                new EmptyBorder(5, 12, 5, 12)
        ));
        pnlTitle.add(lblDateBadge, BorderLayout.EAST);
        main.add(pnlTitle);

        main.add(Box.createVerticalStrut(18));

        // ── Hàng 4 Thẻ Stat Cards (KPIs) ─────────────────────────────────────
        JPanel pnlStats = new JPanel(new GridLayout(1, 4, 16, 0));
        pnlStats.setOpaque(false);
        pnlStats.setPreferredSize(new Dimension(800, 135));
        pnlStats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));

        // Card 1: Yêu cầu chờ phê duyệt (Nổi bật màu xanh dương)
        pnlStats.add(createPrimaryStatCard(
                String.format("%02d", data.yeuCauChoDuyet),
                "Yêu cầu chờ phê duyệt",
                data.yeuCauChoDuyet + " / " + data.tongYeuCauTheoDoi + " yêu cầu đang theo dõi",
                "Xử lý yêu cầu cập nhật linh kiện ↗",
                "📄"
        ));

        // Card 2: Chờ xác nhận nhập kho
        pnlStats.add(createWhiteStatCard(
                String.format("%02d", data.choXacNhanNhapKho),
                "Chờ xác nhận nhập kho",
                "Đã duyệt · Chưa nhập kho",
                "Xử lý yêu cầu cập nhật linh kiện ↗",
                "📦"
        ));

        // Card 3: Linh kiện hết hàng
        pnlStats.add(createWhiteStatCard(
                String.format("%02d", data.linhKienHetHang),
                "Linh kiện hết hàng",
                "Các mã đang kinh doanh",
                "Quản lý linh kiện và tồn kho ↗",
                new JLabel(createWarningIcon(20, new Color(0xF59E0B)))
        ));

        // Card 4: Hồ sơ chưa có tài khoản
        pnlStats.add(createWhiteStatCard(
                String.format("%02d", data.hoSoChuaCoTaiKhoan),
                "Hồ sơ chưa có tài khoản",
                "Trong các hồ sơ đang hiển thị",
                "Quản lý nhân viên ↗",
                "👥"
        ));

        main.add(pnlStats);

        main.add(Box.createVerticalStrut(20));

        // ── Khối "Công việc cần xử lý" (Bảng yêu cầu + Cảnh báo hết hàng) ────
        JPanel pnlTasksSection = new JPanel(new BorderLayout(0, 10));
        pnlTasksSection.setOpaque(false);
        pnlTasksSection.setPreferredSize(new Dimension(800, 285));
        pnlTasksSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        JPanel pnlTasksHeader = new JPanel(new BorderLayout(0, 0));
        pnlTasksHeader.setOpaque(false);
        JLabel lblTasksTitle = new JLabel("Công việc cần xử lý");
        lblTasksTitle.setFont(f(Font.BOLD, 16));
        lblTasksTitle.setForeground(C_TEXT_HEAD);
        pnlTasksHeader.add(lblTasksTitle, BorderLayout.WEST);

        JLabel lblTasksScope = new JLabel("Phạm vi: yêu cầu, linh kiện và hồ sơ đang hiển thị");
        lblTasksScope.setFont(f(Font.PLAIN, 11));
        lblTasksScope.setForeground(C_TEXT_SUBTLE);
        pnlTasksHeader.add(lblTasksScope, BorderLayout.EAST);
        pnlTasksSection.add(pnlTasksHeader, BorderLayout.NORTH);

        // Body: 2 Card (Card Trái: Bảng yêu cầu 68%, Card Phải: Cảnh báo hết hàng 32%)
        JPanel pnlTasksCards = new JPanel(new GridBagLayout());
        pnlTasksCards.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // Card Trái: Bảng yêu cầu
        gbc.gridx = 0;
        gbc.weightx = 0.68;
        gbc.insets = new Insets(0, 0, 0, 14);
        pnlTasksCards.add(buildYeuCauChoDuyetCard(data), gbc);

        // Card Phải: Cảnh báo hết hàng
        gbc.gridx = 1;
        gbc.weightx = 0.32;
        gbc.insets = new Insets(0, 0, 0, 0);
        pnlTasksCards.add(buildCanhBaoHetHangCard(data), gbc);

        pnlTasksSection.add(pnlTasksCards, BorderLayout.CENTER);
        main.add(pnlTasksSection);

        main.add(Box.createVerticalStrut(20));

        // ── Khối "Kinh doanh trong ngày" (Thống kê doanh thu, Biểu đồ, Cơ cấu)
        main.add(buildKinhDoanhTrongNgaySection(data));

        return main;
    }

    private JPanel createPrimaryStatCard(String number, String title, String subtitle, String linkText, String icon) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_PRIMARY);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(0, 4));
        card.setBorder(new EmptyBorder(12, 14, 10, 14));

        // Header thẻ: Số lớn bên trái, Icon trong khung bo tròn bên phải
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel lblNum = new JLabel(number);
        lblNum.setFont(f(Font.BOLD, 26));
        lblNum.setForeground(C_WHITE);
        top.add(lblNum, BorderLayout.WEST);

        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        lblIcon.setForeground(C_WHITE);

        JPanel iconBox = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 45));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        iconBox.setOpaque(false);
        iconBox.setPreferredSize(new Dimension(32, 32));
        iconBox.add(lblIcon);
        top.add(iconBox, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        // Giữa: Title + Subtitle
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(f(Font.BOLD, 13));
        lblTitle.setForeground(C_WHITE);
        center.add(lblTitle);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(f(Font.PLAIN, 11));
        lblSub.setForeground(new Color(0xDBEAFE));
        center.add(lblSub);
        card.add(center, BorderLayout.CENTER);

        // Đáy: Link điều hướng
        JLabel lblLink = new JLabel(linkText);
        lblLink.setFont(f(Font.BOLD, 11));
        lblLink.setForeground(C_WHITE);
        lblLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.add(lblLink, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createWhiteStatCard(String number, String title, String subtitle, String linkText, String icon) {
        JLabel lbl = new JLabel(icon);
        lbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        return createWhiteStatCard(number, title, subtitle, linkText, lbl);
    }

    private JPanel createWhiteStatCard(String number, String title, String subtitle, String linkText, JComponent iconComp) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(0, 4));
        card.setBorder(new EmptyBorder(12, 14, 10, 14));

        // Header thẻ: Số lớn bên trái, Icon bên phải
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel lblNum = new JLabel(number);
        lblNum.setFont(f(Font.BOLD, 26));
        lblNum.setForeground(C_TEXT_HEAD);
        top.add(lblNum, BorderLayout.WEST);

        JPanel iconBox = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xF8FAFC));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        iconBox.setOpaque(false);
        iconBox.setPreferredSize(new Dimension(32, 32));
        iconBox.add(iconComp);
        top.add(iconBox, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        // Giữa: Title + Subtitle
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(f(Font.BOLD, 13));
        lblTitle.setForeground(C_TEXT_HEAD);
        center.add(lblTitle);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(f(Font.PLAIN, 11));
        lblSub.setForeground(C_TEXT_SUBTLE);
        center.add(lblSub);
        card.add(center, BorderLayout.CENTER);

        // Đáy: Link điều hướng
        JLabel lblLink = new JLabel(linkText);
        lblLink.setFont(f(Font.BOLD, 11));
        lblLink.setForeground(C_PRIMARY);
        lblLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.add(lblLink, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildYeuCauChoDuyetCard(ChuCuaHangDashboardData data) {
        JPanel card = createWhiteCardPanel(16);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(12, 14, 10, 14));

        // Header card
        JPanel header = new JPanel(new BorderLayout(0, 0));
        header.setOpaque(false);

        JPanel pnlTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlTitle.setOpaque(false);
        JLabel lblTitle = new JLabel("Yêu cầu chờ duyệt");
        lblTitle.setFont(f(Font.BOLD, 14));
        lblTitle.setForeground(C_TEXT_HEAD);
        pnlTitle.add(lblTitle);

        JLabel lblBadge = new JLabel(data.yeuCauChoDuyet + " yêu cầu");
        lblBadge.setFont(f(Font.BOLD, 11));
        lblBadge.setForeground(C_PRIMARY);
        lblBadge.setBackground(C_PRIMARY_BG);
        lblBadge.setOpaque(true);
        lblBadge.setBorder(new EmptyBorder(2, 6, 2, 6));
        pnlTitle.add(lblBadge);
        header.add(pnlTitle, BorderLayout.WEST);

        JLabel lblOpen = new JLabel("Mở danh sách ↗");
        lblOpen.setFont(f(Font.BOLD, 11));
        lblOpen.setForeground(C_PRIMARY);
        lblOpen.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.add(lblOpen, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // Bảng danh sách
        String[] cols = {"MÃ / NGÀY TẠO", "KTV LẬP", "LINH KIỆN", "YÊU CẦU", "TỒN KHO", ""};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (YeuCauRow r : data.danhSachYeuCau) {
            model.addRow(new Object[]{
                    "<html><b>" + r.maYeuCau + "</b><br><font color='#64748B'>" + r.ngayTao + "</font></html>",
                    r.ktvLap,
                    "<html><b>" + r.linhKien + "</b><br><font color='#64748B'>" + r.maLinhKien + "</font></html>",
                    "<html><b>" + r.soLuongYeuCau + "</b></html>",
                    r.tonKhoHet ? "<html><font color='#DC2626'><b>" + r.tonKho + "</b></font></html>"
                               : "<html><b>" + r.tonKho + "</b></html>",
                    "›"
            });
        }

        JTable table = new JTable(model);
        styleTable(table);
        table.setRowHeight(34);
        table.getColumnModel().getColumn(5).setPreferredWidth(26);

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        scrollTable.getViewport().setBackground(C_WHITE);
        card.add(scrollTable, BorderLayout.CENTER);

        // Footer tóm tắt trạng thái
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 2));
        footer.setOpaque(false);
        footer.add(createStatusDotLabel("Chờ duyệt 3", C_PRIMARY));
        footer.add(createStatusDotLabel("Đã duyệt 1", C_SUCCESS));
        footer.add(createStatusDotLabel("Từ chối 1", C_DANGER));
        footer.add(createStatusDotLabel("Đã nhập kho 1", C_TEXT_SUBTLE));
        card.add(footer, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildCanhBaoHetHangCard(ChuCuaHangDashboardData data) {
        JPanel card = createWhiteCardPanel(16);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(12, 14, 10, 14));

        // Header card
        JPanel header = new JPanel(new BorderLayout(0, 0));
        header.setOpaque(false);

        JPanel pnlTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pnlTitle.setOpaque(false);
        JLabel lblIcon = new JLabel(createWarningIcon(16, new Color(0xF59E0B)));
        pnlTitle.add(lblIcon);

        JLabel lblTitle = new JLabel("Cảnh báo hết hàng");
        lblTitle.setFont(f(Font.BOLD, 14));
        lblTitle.setForeground(C_TEXT_HEAD);
        pnlTitle.add(lblTitle);
        header.add(pnlTitle, BorderLayout.WEST);

        JLabel lblBadge = new JLabel(data.danhSachHetHang.size() + " mã");
        lblBadge.setFont(f(Font.BOLD, 11));
        lblBadge.setForeground(C_DANGER);
        lblBadge.setBackground(C_DANGER_BG);
        lblBadge.setOpaque(true);
        lblBadge.setBorder(new EmptyBorder(2, 6, 2, 6));
        header.add(lblBadge, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // Subtitle
        JLabel lblSub = new JLabel("Linh kiện đang kinh doanh có tồn kho bằng 0.");
        lblSub.setFont(f(Font.PLAIN, 11));
        lblSub.setForeground(C_TEXT_SUBTLE);

        // Danh sách các mặt hàng hết
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        listPanel.add(lblSub);
        listPanel.add(Box.createVerticalStrut(8));

        for (LinhKienHetHangRow item : data.danhSachHetHang) {
            JPanel row = new JPanel(new BorderLayout(0, 2));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER),
                    new EmptyBorder(6, 0, 6, 0)
            ));

            JLabel lblTen = new JLabel(item.tenLinhKien);
            lblTen.setFont(f(Font.BOLD, 12));
            lblTen.setForeground(C_TEXT_HEAD);
            row.add(lblTen, BorderLayout.WEST);

            JLabel lblQty = new JLabel("0 " + item.donVi);
            lblQty.setFont(f(Font.BOLD, 12));
            lblQty.setForeground(C_DANGER);
            row.add(lblQty, BorderLayout.EAST);

            JLabel lblNote = new JLabel(item.maLinhKien + " · " + item.ghiChuYeuCau);
            lblNote.setFont(f(Font.PLAIN, 10));
            lblNote.setForeground(C_TEXT_SUBTLE);
            row.add(lblNote, BorderLayout.SOUTH);

            listPanel.add(row);
        }

        listPanel.add(Box.createVerticalGlue());
        card.add(listPanel, BorderLayout.CENTER);

        // Footer link
        JLabel lblLink = new JLabel("Quản lý linh kiện và tồn kho ↗");
        lblLink.setFont(f(Font.BOLD, 11));
        lblLink.setForeground(C_PRIMARY);
        lblLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.add(lblLink, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildKinhDoanhTrongNgaySection(ChuCuaHangDashboardData data) {
        JPanel card = createWhiteCardPanel(16);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setPreferredSize(new Dimension(800, 235));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));

        // Header section
        JPanel header = new JPanel(new BorderLayout(0, 0));
        header.setOpaque(false);

        String ngayHienTai = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        JLabel lblTitle = new JLabel("Kinh doanh trong ngày  ·  " + ngayHienTai + " • " + data.soHoaDonThanhToan + " hóa đơn đã thanh toán");
        lblTitle.setFont(f(Font.BOLD, 15));
        lblTitle.setForeground(C_TEXT_HEAD);
        header.add(lblTitle, BorderLayout.WEST);

        JLabel lblLink = new JLabel("Thống kê doanh thu ↗");
        lblLink.setFont(f(Font.BOLD, 11));
        lblLink.setForeground(C_PRIMARY);
        lblLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.add(lblLink, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // 3 Cột thống kê
        JPanel body = new JPanel(new GridLayout(1, 3, 18, 0));
        body.setOpaque(false);

        // Cột 1: Thẻ doanh thu sau hoàn
        JPanel pnlCol1 = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_PRIMARY_BG);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(C_PRIMARY_BR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        pnlCol1.setOpaque(false);
        pnlCol1.setLayout(new BoxLayout(pnlCol1, BoxLayout.Y_AXIS));
        pnlCol1.setBorder(new EmptyBorder(12, 14, 10, 14));

        JLabel lblCap = new JLabel("DOANH THU SAU HOÀN");
        lblCap.setFont(f(Font.BOLD, 10));
        lblCap.setForeground(C_PRIMARY);
        pnlCol1.add(lblCap);

        pnlCol1.add(Box.createVerticalStrut(2));

        JLabel lblMoney = new JLabel(MONEY_FMT.format(data.doanhThuSauHoan) + " VNĐ");
        lblMoney.setFont(f(Font.BOLD, 20));
        lblMoney.setForeground(C_PRIMARY);
        pnlCol1.add(lblMoney);

        pnlCol1.add(Box.createVerticalStrut(6));

        JLabel lblTongThu = new JLabel("Tổng thu: " + MONEY_FMT.format(data.tongThu) + " VNĐ");
        lblTongThu.setFont(f(Font.PLAIN, 11));
        lblTongThu.setForeground(C_TEXT_MAIN);
        pnlCol1.add(lblTongThu);

        JLabel lblHoanTien = new JLabel("Hoàn tiền: " + MONEY_FMT.format(data.hoanTien) + " VNĐ");
        lblHoanTien.setFont(f(Font.PLAIN, 11));
        lblHoanTien.setForeground(C_DANGER);
        pnlCol1.add(lblHoanTien);

        body.add(pnlCol1);

        // Cột 2: Biểu đồ cột doanh thu theo khung giờ
        JPanel pnlCol2 = new JPanel(new BorderLayout(0, 6));
        pnlCol2.setOpaque(false);

        JPanel pnlBarHead = new JPanel(new BorderLayout());
        pnlBarHead.setOpaque(false);
        JLabel lblChartTitle = new JLabel("Doanh thu sau hoàn");
        lblChartTitle.setFont(f(Font.BOLD, 12));
        lblChartTitle.setForeground(C_TEXT_HEAD);
        pnlBarHead.add(lblChartTitle, BorderLayout.WEST);

        JLabel lblUnit = new JLabel("Triệu VNĐ");
        lblUnit.setFont(f(Font.PLAIN, 10));
        lblUnit.setForeground(C_TEXT_SUBTLE);
        pnlBarHead.add(lblUnit, BorderLayout.EAST);
        pnlCol2.add(pnlBarHead, BorderLayout.NORTH);

        pnlCol2.add(new MiniBarChart(data.doanhThuKhungGio, data.nhanKhungGio), BorderLayout.CENTER);
        body.add(pnlCol2);

        // Cột 3: Cơ cấu thanh toán
        JPanel pnlCol3 = new JPanel();
        pnlCol3.setLayout(new BoxLayout(pnlCol3, BoxLayout.Y_AXIS));
        pnlCol3.setOpaque(false);

        JLabel lblPaymentTitle = new JLabel("Cơ cấu thanh toán");
        lblPaymentTitle.setFont(f(Font.BOLD, 12));
        lblPaymentTitle.setForeground(C_TEXT_HEAD);
        pnlCol3.add(lblPaymentTitle);

        pnlCol3.add(Box.createVerticalStrut(8));

        pnlCol3.add(new DualProgressBar(67.5));

        pnlCol3.add(Box.createVerticalStrut(10));

        JLabel lblCK = new JLabel("● Chuyển khoản (67,5%): " + MONEY_FMT.format(data.doanhThuChuyenKhoan) + " VNĐ");
        lblCK.setFont(f(Font.BOLD, 11));
        lblCK.setForeground(C_PRIMARY);
        pnlCol3.add(lblCK);

        pnlCol3.add(Box.createVerticalStrut(4));

        JLabel lblTM = new JLabel("● Tiền mặt (32,5%): " + MONEY_FMT.format(data.doanhThuTienMat) + " VNĐ");
        lblTM.setFont(f(Font.PLAIN, 11));
        lblTM.setForeground(new Color(0x0284C7));
        pnlCol3.add(lblTM);

        body.add(pnlCol3);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3B. DASHBOARD TIẾP NHẬN VIÊN (Bám sát image47.png từ Figma)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTiepNhanVienContent() {
        TiepNhanVienDashboardData data = dashboardController.layDataTiepNhanVien();

        ScrollableContentPanel main = new ScrollableContentPanel();
        main.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Tiêu đề trang
        JPanel pnlTitle = new JPanel(new BorderLayout(0, 0));
        pnlTitle.setOpaque(false);
        pnlTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel lblTitle = new JLabel("Dashboard Tiếp nhận viên");
        lblTitle.setFont(f(Font.BOLD, 22));
        lblTitle.setForeground(C_TEXT_HEAD);
        pnlTitle.add(lblTitle, BorderLayout.WEST);

        String ngayHienTai = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        JLabel lblDateBadge = new JLabel("Ca làm việc: " + ngayHienTai);
        lblDateBadge.setFont(f(Font.BOLD, 12));
        lblDateBadge.setForeground(C_TEXT_MAIN);
        lblDateBadge.setBackground(C_WHITE);
        lblDateBadge.setOpaque(true);
        lblDateBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1, true),
                new EmptyBorder(5, 12, 5, 12)
        ));
        pnlTitle.add(lblDateBadge, BorderLayout.EAST);
        main.add(pnlTitle);

        main.add(Box.createVerticalStrut(14));

        // 4 Thẻ thống kê
        JPanel pnlStats = new JPanel(new GridLayout(1, 4, 14, 0));
        pnlStats.setOpaque(false);
        pnlStats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        pnlStats.add(createPrimaryStatCard(
                String.format("%02d", data.phieuChoSua),
                "Chờ tiếp nhận / sửa",
                "Cần phân công kỹ thuật viên",
                "Lập phiếu tiếp nhận mới ↗",
                "📝"
        ));
        pnlStats.add(createWhiteStatCard(
                String.format("%02d", data.phieuDangSua),
                "Xe đang sửa chữa",
                "Đang tiến hành tại xưởng",
                "Theo dõi tiến độ xưởng ↗",
                "🚗"
        ));
        pnlStats.add(createWhiteStatCard(
                String.format("%02d", data.phieuChoLinhKien),
                "Tạm dừng chờ linh kiện",
                "Chờ duyệt / nhập kho",
                "Xem phiếu chờ linh kiện ↗",
                "⏳"
        ));
        pnlStats.add(createWhiteStatCard(
                String.format("%02d", data.phieuHoanTatChoThu),
                "Hoàn tất chờ thanh toán",
                "Sẵn sàng lập hóa đơn",
                "Lập hóa đơn thanh toán ↗",
                "🧾"
        ));
        main.add(pnlStats);

        main.add(Box.createVerticalStrut(14));

        // Khối Bảng tiến độ tiếp nhận xe tại xưởng trong ngày
        JPanel cardTable = createWhiteCardPanel(16);
        cardTable.setLayout(new BorderLayout(0, 10));
        cardTable.setBorder(new EmptyBorder(14, 16, 14, 16));
        cardTable.setMaximumSize(new Dimension(Integer.MAX_VALUE, 460));

        JPanel tblHead = new JPanel(new BorderLayout());
        tblHead.setOpaque(false);
        JLabel lblTblTitle = new JLabel("Danh sách xe đang tiếp nhận & sửa chữa trong ngày");
        lblTblTitle.setFont(f(Font.BOLD, 15));
        lblTblTitle.setForeground(C_TEXT_HEAD);
        tblHead.add(lblTblTitle, BorderLayout.WEST);

        JPanel pnlQuickActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlQuickActions.setOpaque(false);
        JButton btnAddPhieu = new RoundedButton("+ Lập phiếu tiếp nhận", 10, C_PRIMARY, C_PRIMARY_HV, null);
        btnAddPhieu.setForeground(C_WHITE);
        btnAddPhieu.setFont(f(Font.BOLD, 12));
        btnAddPhieu.setPreferredSize(new Dimension(160, 34));
        pnlQuickActions.add(btnAddPhieu);

        JButton btnAddHoaDon = new RoundedButton("+ Lập hóa đơn", 10, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnAddHoaDon.setForeground(C_TEXT_MAIN);
        btnAddHoaDon.setFont(f(Font.BOLD, 12));
        btnAddHoaDon.setPreferredSize(new Dimension(120, 34));
        pnlQuickActions.add(btnAddHoaDon);

        tblHead.add(pnlQuickActions, BorderLayout.EAST);
        cardTable.add(tblHead, BorderLayout.NORTH);

        // Bảng
        String[] cols = {"MÃ PHIẾU", "BIỂN SỐ XE", "KHÁCH HÀNG", "SỐ ĐIỆN THOẠI", "KTV CHÍNH", "THỜI ĐIỂM", "TRẠNG THÁI"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        for (PhieuTiepNhanRow r : data.danhSachPhieu) {
            model.addRow(new Object[]{
                    r.maPhieu,
                    r.bienSo,
                    r.khachHang,
                    r.soDienThoai,
                    r.ktvChinh,
                    r.thoiDiem,
                    r.trangThai
            });
        }

        JTable table = new JTable(model);
        styleTable(table);
        table.setRowHeight(36);
        table.getColumnModel().getColumn(6).setCellRenderer(new StatusBadgeRenderer());

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        scrollTable.getViewport().setBackground(C_WHITE);
        cardTable.add(scrollTable, BorderLayout.CENTER);

        main.add(cardTable);
        return main;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3C. DASHBOARD KỸ THUẬT VIÊN (Chuẩn 1:1 theo Figma frame image49.png)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildKyThuatVienContent() {
        KyThuatVienDashboardData data = dashboardController.layDataKyThuatVien(nhanVien);

        ScrollableContentPanel main = new ScrollableContentPanel();
        main.setBorder(new EmptyBorder(16, 20, 16, 20));

        // 1. Tiêu đề trang
        JPanel pnlTitle = new JPanel(new BorderLayout(0, 0));
        pnlTitle.setOpaque(false);
        pnlTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        // Breadcrumb + Title
        JPanel pnlTitleLeft = new JPanel();
        pnlTitleLeft.setLayout(new BoxLayout(pnlTitleLeft, BoxLayout.Y_AXIS));
        pnlTitleLeft.setOpaque(false);

        JLabel lblBreadcrumb = new JLabel("CarZone  ›  Kỹ thuật viên  ›  Dashboard");
        lblBreadcrumb.setFont(f(Font.PLAIN, 11));
        lblBreadcrumb.setForeground(C_TEXT_SUBTLE);
        pnlTitleLeft.add(lblBreadcrumb);
        pnlTitleLeft.add(Box.createVerticalStrut(2));

        JPanel pnlMainHeading = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlMainHeading.setOpaque(false);
        JLabel lblToolIcon = new JLabel("🛠️");
        lblToolIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        pnlMainHeading.add(lblToolIcon);

        JLabel lblTitle = new JLabel("Dashboard Kỹ thuật viên");
        lblTitle.setFont(f(Font.BOLD, 20));
        lblTitle.setForeground(C_TEXT_HEAD);
        pnlMainHeading.add(lblTitle);
        pnlTitleLeft.add(pnlMainHeading);

        pnlTitle.add(pnlTitleLeft, BorderLayout.WEST);

        JLabel lblUserBadge = new JLabel("Kỹ thuật viên: " + nhanVien.getHoTen());
        lblUserBadge.setFont(f(Font.BOLD, 12));
        lblUserBadge.setForeground(C_PRIMARY);
        lblUserBadge.setBackground(C_PRIMARY_BG);
        lblUserBadge.setOpaque(true);
        lblUserBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_PRIMARY_BR, 1, true),
                new EmptyBorder(5, 12, 5, 12)
        ));
        pnlTitle.add(lblUserBadge, BorderLayout.EAST);
        main.add(pnlTitle);

        main.add(Box.createVerticalStrut(14));

        // 2. Card Bộ lọc kỳ thống kê & phạm vi (Theo Figma frame Dashboard KTV mới)
        JPanel pnlFilterCard = createWhiteCardPanel(16);
        pnlFilterCard.setLayout(new BorderLayout(0, 10));
        pnlFilterCard.setBorder(new EmptyBorder(12, 16, 12, 16));
        pnlFilterCard.setPreferredSize(new Dimension(800, 75));
        pnlFilterCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));

        JPanel pnlFilterFields = new JPanel(new GridLayout(1, 4, 12, 0));
        pnlFilterFields.setOpaque(false);

        // Trường 1: Kỳ thống kê
        JPanel pnlColPeriod = new JPanel(new BorderLayout(0, 3));
        pnlColPeriod.setOpaque(false);
        JLabel lblKy = new JLabel("Kỳ thống kê");
        lblKy.setFont(f(Font.BOLD, 11));
        lblKy.setForeground(C_TEXT_MAIN);
        pnlColPeriod.add(lblKy, BorderLayout.NORTH);

        String[] periods = {"Hôm nay", "Tuần này", "Tháng này", "Tùy chọn ngày"};
        JComboBox<String> cboPeriod = new JComboBox<>(periods);
        cboPeriod.setFont(f(Font.PLAIN, 12));
        cboPeriod.setBackground(C_WHITE);
        cboPeriod.setPreferredSize(new Dimension(100, 32));
        pnlColPeriod.add(cboPeriod, BorderLayout.CENTER);
        pnlFilterFields.add(pnlColPeriod);

        // Trường 2: Từ ngày
        JPanel pnlColFrom = createFilterFieldGroup("Từ ngày", "01/10/2026", "📅");
        pnlFilterFields.add(pnlColFrom);

        // Trường 3: Đến ngày
        JPanel pnlColTo = createFilterFieldGroup("Đến ngày", "31/10/2026", "📅");
        pnlFilterFields.add(pnlColTo);

        // Trường 4: Trạng thái công việc
        JPanel pnlColSt = new JPanel(new BorderLayout(0, 3));
        pnlColSt.setOpaque(false);
        JLabel lblSt = new JLabel("Trạng thái công việc");
        lblSt.setFont(f(Font.BOLD, 11));
        lblSt.setForeground(C_TEXT_MAIN);
        pnlColSt.add(lblSt, BorderLayout.NORTH);

        String[] statuses = {"Tất cả", "Chờ sửa", "Đang sửa", "Chờ linh kiện", "Hoàn tất"};
        JComboBox<String> cboStatus = new JComboBox<>(statuses);
        cboStatus.setFont(f(Font.PLAIN, 12));
        cboStatus.setBackground(C_WHITE);
        cboStatus.setPreferredSize(new Dimension(100, 32));
        pnlColSt.add(cboStatus, BorderLayout.CENTER);
        pnlFilterFields.add(pnlColSt);

        JPanel pnlFilterActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlFilterActions.setOpaque(false);

        JButton btnApply = new RoundedButton("Áp dụng", 10, C_PRIMARY, C_PRIMARY_HV, null);
        btnApply.setForeground(C_WHITE);
        btnApply.setFont(f(Font.BOLD, 12));
        btnApply.setPreferredSize(new Dimension(88, 32));

        JButton btnReset = new RoundedButton("Làm mới", 10, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnReset.setForeground(C_TEXT_MAIN);
        btnReset.setFont(f(Font.BOLD, 12));
        btnReset.setPreferredSize(new Dimension(82, 32));

        pnlFilterActions.add(btnApply);
        pnlFilterActions.add(btnReset);

        JPanel pnlFilterTop = new JPanel(new BorderLayout(12, 0));
        pnlFilterTop.setOpaque(false);
        pnlFilterTop.add(pnlFilterFields, BorderLayout.CENTER);

        JPanel pnlBtnWrap = new JPanel(new BorderLayout());
        pnlBtnWrap.setOpaque(false);
        pnlBtnWrap.add(pnlFilterActions, BorderLayout.SOUTH);
        pnlFilterTop.add(pnlBtnWrap, BorderLayout.EAST);

        pnlFilterCard.add(pnlFilterTop, BorderLayout.CENTER);
        main.add(pnlFilterCard);

        main.add(Box.createVerticalStrut(14));

        // 3. Hàng 4 Thẻ Tổng hợp trong kỳ (Summary Cards chuẩn Figma)
        JPanel pnlSummaryCards = new JPanel(new GridLayout(1, 4, 14, 0));
        pnlSummaryCards.setOpaque(false);
        pnlSummaryCards.setPreferredSize(new Dimension(800, 105));
        pnlSummaryCards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));

        // Card 1: Phiếu mới được phân công
        pnlSummaryCards.add(createPrimaryStatCard(
                String.format("%02d", data.phieuMoiPhanCong),
                "Phiếu mới phân công",
                "Chờ bắt đầu thực hiện",
                "Xem phiếu phân công ↗",
                "📋"
        ));

        // Card 2: Sửa chữa đang thực hiện
        pnlSummaryCards.add(createWhiteStatCard(
                String.format("%02d", data.suaChuaDangThucHien),
                "Đang thực hiện",
                "Tiến độ sửa chữa tại xưởng",
                "Cập nhật sửa chữa ↗",
                "🔧"
        ));

        // Card 3: Yêu cầu linh kiện chờ phê duyệt
        pnlSummaryCards.add(createWhiteStatCard(
                String.format("%02d", data.yeuCauChoDuyet),
                "Yêu cầu chờ duyệt",
                "Yêu cầu linh kiện gửi CCH",
                "Lịch sử yêu cầu ↗",
                "📦"
        ));

        // Card 4: Phản hồi cần xử lý
        pnlSummaryCards.add(createWhiteStatCard(
                String.format("%02d", data.phanHoiCanXuLy),
                "Phản hồi cần xử lý",
                "Yêu cầu bị từ chối / cần sửa",
                "Xem phản hồi ↗",
                "💬"
        ));

        main.add(pnlSummaryCards);

        main.add(Box.createVerticalStrut(16));

        // 4. Tabs điều hướng nhanh theo Figma (Tổng quan | Phiếu được phân công | Yêu cầu linh kiện)
        JPanel pnlTabsBar = new JPanel(new BorderLayout());
        pnlTabsBar.setOpaque(false);
        pnlTabsBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JPanel pnlTabsLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlTabsLeft.setOpaque(false);

        JButton tabTongQuan = new RoundedButton("Tổng quan", 8, C_PRIMARY, C_PRIMARY_HV, null);
        tabTongQuan.setForeground(C_WHITE);
        tabTongQuan.setFont(f(Font.BOLD, 12));
        tabTongQuan.setPreferredSize(new Dimension(105, 32));
        pnlTabsLeft.add(tabTongQuan);

        JButton tabPhanCong = new RoundedButton("Phiếu được phân công", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        tabPhanCong.setForeground(C_TEXT_MAIN);
        tabPhanCong.setFont(f(Font.PLAIN, 12));
        tabPhanCong.setPreferredSize(new Dimension(160, 32));
        pnlTabsLeft.add(tabPhanCong);

        JButton tabYeuCau = new RoundedButton("Yêu cầu linh kiện", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        tabYeuCau.setForeground(C_TEXT_MAIN);
        tabYeuCau.setFont(f(Font.PLAIN, 12));
        tabYeuCau.setPreferredSize(new Dimension(140, 32));
        pnlTabsLeft.add(tabYeuCau);

        pnlTabsBar.add(pnlTabsLeft, BorderLayout.WEST);

        JPanel pnlTabsRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlTabsRight.setOpaque(false);

        JButton btnViewDetail = new RoundedButton("Xem chi tiết", 10, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnViewDetail.setFont(f(Font.BOLD, 12));
        btnViewDetail.setForeground(C_TEXT_MAIN);
        btnViewDetail.setPreferredSize(new Dimension(110, 32));
        pnlTabsRight.add(btnViewDetail);

        JButton btnUpdatePhieu = new RoundedButton("Cập nhật phiếu sửa chữa", 10, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnUpdatePhieu.setFont(f(Font.BOLD, 12));
        btnUpdatePhieu.setForeground(C_TEXT_MAIN);
        btnUpdatePhieu.setPreferredSize(new Dimension(185, 32));
        pnlTabsRight.add(btnUpdatePhieu);

        pnlTabsBar.add(pnlTabsRight, BorderLayout.EAST);
        main.add(pnlTabsBar);

        main.add(Box.createVerticalStrut(10));

        // 4. Bảng Phiếu được phân công (Theo Figma image49.png)
        JPanel cardTable = createWhiteCardPanel(16);
        cardTable.setLayout(new BorderLayout(0, 10));
        cardTable.setBorder(new EmptyBorder(14, 16, 12, 16));
        cardTable.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        String[] cols = {"MÃ PHIẾU SỬA CHỮA", "BIỂN SỐ XE", "HÃNG / DÒNG XE", "NGÀY PHÂN CÔNG", "NỘI DUNG PHÂN CÔNG", "TRẠNG THÁI"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        for (PhieuPhanCongRow r : data.danhSachPhanCong) {
            model.addRow(new Object[]{
                    r.maPhieu,
                    r.bienSo,
                    r.dongXe,
                    r.ngayPhanCong,
                    r.noiDungCongViec,
                    r.trangThai
            });
        }

        JTable table = new JTable(model);
        styleTable(table);
        table.setRowHeight(38);
        table.getColumnModel().getColumn(5).setCellRenderer(new StatusBadgeRenderer());

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        scrollTable.getViewport().setBackground(C_WHITE);
        cardTable.add(scrollTable, BorderLayout.CENTER);

        // Status bar ở đáy bảng
        JPanel pnlTblFooter = new JPanel(new BorderLayout());
        pnlTblFooter.setOpaque(false);
        JLabel lblTblStatus = new JLabel("Hiển thị " + data.danhSachPhanCong.size() + " phiếu được phân công · Nhấp chọn dòng để xem chi tiết.");
        lblTblStatus.setFont(f(Font.PLAIN, 11));
        lblTblStatus.setForeground(C_TEXT_SUBTLE);
        pnlTblFooter.add(lblTblStatus, BorderLayout.WEST);

        JLabel lblTblSource = new JLabel("Nguồn: Phiếu sửa chữa đang hoạt động");
        lblTblSource.setFont(f(Font.PLAIN, 11));
        lblTblSource.setForeground(C_TEXT_MUTED);
        pnlTblFooter.add(lblTblSource, BorderLayout.EAST);
        cardTable.add(pnlTblFooter, BorderLayout.SOUTH);

        main.add(cardTable);

        main.add(Box.createVerticalStrut(14));

        // 5. Card Chi tiết phiếu đang chọn · Chỉ đọc (Theo Figma image49.png)
        JPanel cardDetail = createWhiteCardPanel(16);
        cardDetail.setLayout(new BorderLayout(0, 10));
        cardDetail.setBorder(new EmptyBorder(14, 16, 14, 16));
        cardDetail.setMaximumSize(new Dimension(Integer.MAX_VALUE, 175));

        JPanel pnlDetailHead = new JPanel(new BorderLayout());
        pnlDetailHead.setOpaque(false);
        JLabel lblDetailTitle = new JLabel("Chi tiết phiếu đang chọn · Chỉ đọc");
        lblDetailTitle.setFont(f(Font.BOLD, 14));
        lblDetailTitle.setForeground(C_TEXT_HEAD);
        pnlDetailHead.add(lblDetailTitle, BorderLayout.WEST);

        JLabel lblDetailSub = new JLabel("Thông tin phân công chi tiết của phiếu sửa chữa");
        lblDetailSub.setFont(f(Font.PLAIN, 11));
        lblDetailSub.setForeground(C_TEXT_SUBTLE);
        pnlDetailHead.add(lblDetailSub, BorderLayout.EAST);
        cardDetail.add(pnlDetailHead, BorderLayout.NORTH);

        // Khối 5 ô chỉ đọc: Hàng trên 3 ô, hàng dưới 2 ô
        JPanel pnlDetailBoxes = new JPanel(new GridLayout(2, 1, 0, 8));
        pnlDetailBoxes.setOpaque(false);

        // Hàng 1: Mã phiếu, Xe, Trạng thái
        JPanel pnlRow1 = new JPanel(new GridLayout(1, 3, 10, 0));
        pnlRow1.setOpaque(false);
        JLabel lblValMa = new JLabel("—");
        JLabel lblValXe = new JLabel("—");
        JLabel lblValSt = new JLabel("—");

        pnlRow1.add(createReadOnlyBox("Mã phiếu · Chỉ đọc", lblValMa));
        pnlRow1.add(createReadOnlyBox("Xe · Chỉ đọc", lblValXe));
        pnlRow1.add(createReadOnlyBox("Trạng thái · Chỉ đọc", lblValSt));
        pnlDetailBoxes.add(pnlRow1);

        // Hàng 2: Ngày phân công, Nội dung phân công
        JPanel pnlRow2 = new JPanel(new GridBagLayout());
        pnlRow2.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        gbc.gridx = 0;
        gbc.weightx = 0.35;
        gbc.insets = new Insets(0, 0, 0, 10);
        JLabel lblValNgay = new JLabel("—");
        pnlRow2.add(createReadOnlyBox("Ngày phân công · Chỉ đọc", lblValNgay), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.65;
        gbc.insets = new Insets(0, 0, 0, 0);
        JLabel lblValNoiDung = new JLabel("—");
        pnlRow2.add(createReadOnlyBox("Nội dung phân công · Chỉ đọc", lblValNoiDung), gbc);
        pnlDetailBoxes.add(pnlRow2);

        cardDetail.add(pnlDetailBoxes, BorderLayout.CENTER);
        main.add(cardDetail);

        // Hàm cập nhật dữ liệu vào các ô chỉ đọc
        Runnable updateDetail = () -> {
            int sel = table.getSelectedRow();
            if (sel >= 0 && sel < data.danhSachPhanCong.size()) {
                PhieuPhanCongRow r = data.danhSachPhanCong.get(sel);
                lblValMa.setText(r.maPhieu);
                lblValXe.setText(r.bienSo + " (" + r.dongXe + ")");
                lblValSt.setText(switch (r.trangThai) {
                    case CHO_SUA -> "Chờ sửa";
                    case DANG_SUA -> "Đang sửa";
                    case CHO_LINH_KIEN -> "Chờ linh kiện";
                    case HOAN_TAT -> "Hoàn tất";
                });
                lblValNgay.setText(r.ngayPhanCong);
                lblValNoiDung.setText(r.noiDungCongViec);
                lblTblStatus.setText("Đã chọn phiếu " + r.maPhieu + " · Biển số: " + r.bienSo);
            }
        };

        // Lắng nghe click chọn dòng
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateDetail.run();
            }
        });

        // Mặc định chọn dòng đầu tiên nếu có dữ liệu
        if (!data.danhSachPhanCong.isEmpty()) {
            table.setRowSelectionInterval(0, 0);
            updateDetail.run();
        }

        btnViewDetail.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel >= 0) {
                updateDetail.run();
                JOptionPane.showMessageDialog(this,
                        "Thông tin chi tiết phiếu: " + lblValMa.getText() + "\n" +
                        "Phương tiện: " + lblValXe.getText() + "\n" +
                        "Trạng thái: " + lblValSt.getText() + "\n" +
                        "Ngày phân công: " + lblValNgay.getText() + "\n" +
                        "Nội dung công việc: " + lblValNoiDung.getText(),
                        "Chi tiết phiếu sửa chữa", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        btnUpdatePhieu.addActionListener(e -> {
            int sel = table.getSelectedRow();
            String ma = (sel >= 0) ? data.danhSachPhanCong.get(sel).maPhieu : "phiếu đang chọn";
            JOptionPane.showMessageDialog(this,
                    "Chuyển sang màn hình \"Cập nhật phiếu sửa chữa\" cho " + ma + "!",
                    "Cập nhật phiếu", JOptionPane.INFORMATION_MESSAGE);
        });

        btnReset.addActionListener(e -> {
            cboPeriod.setSelectedIndex(0);
            cboStatus.setSelectedIndex(0);
            if (!data.danhSachPhanCong.isEmpty()) {
                table.setRowSelectionInterval(0, 0);
            }
        });

        btnApply.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Đã áp dụng bộ lọc kỳ: " + cboPeriod.getSelectedItem() + " · Trạng thái: " + cboStatus.getSelectedItem(),
                    "Lọc công việc", JOptionPane.INFORMATION_MESSAGE);
        });

        tabTongQuan.addActionListener(e -> {
            // Đã ở tab tổng quan
        });

        tabPhanCong.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Chuyển sang danh sách toàn bộ phiếu được phân công cho " + nhanVien.getHoTen(),
                    "Phiếu được phân công", JOptionPane.INFORMATION_MESSAGE);
        });

        tabYeuCau.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Chuyển sang màn hình \"Yêu cầu cập nhật linh kiện\" của Kỹ thuật viên.",
                    "Yêu cầu linh kiện", JOptionPane.INFORMATION_MESSAGE);
        });

        return main;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER UI: TABLE STYLING, BADGES, CARDS, AND CHARTS
    // ─────────────────────────────────────────────────────────────────────────

    private static JPanel createWhiteCardPanel(int arc) {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                g2.dispose();
            }
        };
    }

    private static JPanel createFilterFieldGroup(String label, String placeholder, String iconText) {
        JPanel pnl = new JPanel(new BorderLayout(0, 4));
        pnl.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(f(Font.BOLD, 11));
        lbl.setForeground(C_TEXT_MAIN);
        pnl.add(lbl, BorderLayout.NORTH);

        JPanel pnlInput = new JPanel(new BorderLayout(6, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pnlInput.setOpaque(false);
        pnlInput.setPreferredSize(new Dimension(100, 34));
        pnlInput.setBorder(new EmptyBorder(0, 10, 0, 10));

        JTextField txt = new JTextField(placeholder);
        txt.setFont(f(Font.PLAIN, 12));
        txt.setForeground(C_TEXT_MUTED);
        txt.setOpaque(false);
        txt.setBorder(null);

        JLabel lblIcon = new JLabel(iconText);
        lblIcon.setFont(f(Font.PLAIN, 13));
        lblIcon.setForeground(C_TEXT_SUBTLE);

        pnlInput.add(txt, BorderLayout.CENTER);
        pnlInput.add(lblIcon, BorderLayout.EAST);

        pnl.add(pnlInput, BorderLayout.CENTER);
        return pnl;
    }

    private static JPanel createReadOnlyBox(String label, JLabel valLabel) {
        JPanel pnl = new JPanel(new BorderLayout(0, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xF8FAFC));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(6, 12, 6, 12));

        JLabel lblCap = new JLabel(label);
        lblCap.setFont(f(Font.PLAIN, 10));
        lblCap.setForeground(C_TEXT_SUBTLE);
        pnl.add(lblCap, BorderLayout.NORTH);

        valLabel.setFont(f(Font.BOLD, 12));
        valLabel.setForeground(C_TEXT_HEAD);
        pnl.add(valLabel, BorderLayout.CENTER);

        return pnl;
    }

    private void styleTable(JTable table) {
        table.setFont(f(Font.PLAIN, 13));
        table.setRowHeight(40);
        table.setGridColor(new Color(0xF1F5F9));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setSelectionBackground(C_PRIMARY_BG);
        table.setSelectionForeground(C_TEXT_HEAD);

        JTableHeader th = table.getTableHeader();
        th.setFont(f(Font.BOLD, 11));
        th.setForeground(C_TEXT_SUBTLE);
        th.setBackground(new Color(0xF8FAFC));
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));
        th.setPreferredSize(new Dimension(100, 36));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private JLabel createStatusDotLabel(String text, Color dotColor) {
        JLabel lbl = new JLabel("● " + text);
        lbl.setFont(f(Font.PLAIN, 12));
        lbl.setForeground(dotColor);
        return lbl;
    }

    /** Renderer hiển thị Trạng thái dưới dạng Badge bo góc có màu */
    private static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);

            if (value instanceof TrangThaiPhieu st) {
                String txt = switch (st) {
                    case CHO_SUA       -> "Chờ sửa";
                    case DANG_SUA      -> "Đang sửa";
                    case CHO_LINH_KIEN -> "Chờ linh kiện";
                    case HOAN_TAT      -> "Hoàn tất";
                };
                lbl.setText(txt);
                lbl.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));

                switch (st) {
                    case CHO_SUA -> {
                        lbl.setForeground(C_WARN);
                        lbl.setBackground(C_WARN_BG);
                    }
                    case DANG_SUA -> {
                        lbl.setForeground(C_PRIMARY);
                        lbl.setBackground(C_PRIMARY_BG);
                    }
                    case CHO_LINH_KIEN -> {
                        lbl.setForeground(new Color(0xC2410C));
                        lbl.setBackground(new Color(0xFFF7ED));
                    }
                    case HOAN_TAT -> {
                        lbl.setForeground(C_SUCCESS);
                        lbl.setBackground(C_SUCCESS_BG);
                    }
                }
                lbl.setOpaque(true);
            }
            return lbl;
        }
    }

    /** Biểu đồ cột mini cho doanh thu theo khung giờ */
    private static class MiniBarChart extends JPanel {
        private final double[] values;
        private final String[] labels;

        public MiniBarChart(double[] values, String[] labels) {
            this.values = values;
            this.labels = labels;
            setOpaque(false);
            setPreferredSize(new Dimension(280, 110));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int barCount = values.length;
            int gap = 16;
            int barW = Math.max(24, (w - (barCount + 1) * gap) / barCount);
            int maxH = h - 30; // dành chỗ cho label

            double maxVal = 8.0;

            for (int i = 0; i < barCount; i++) {
                int x = gap + i * (barW + gap);
                int bh = (int) Math.round((values[i] / maxVal) * maxH);
                int y = maxH - bh + 10;

                // Cột: Cột cuối (hoặc cột cao nhất) màu xanh đậm, các cột trước màu xanh nhạt
                Color barColor = (i == barCount - 1) ? C_PRIMARY : new Color(0xBFDBFE);
                g2.setColor(barColor);
                g2.fillRoundRect(x, y, barW, bh, 6, 6);

                // Giá trị trên đầu cột
                g2.setColor(C_TEXT_MAIN);
                g2.setFont(new Font(FONT_FAMILY, Font.BOLD, 10));
                String valStr = String.valueOf(values[i]);
                int valW = g2.getFontMetrics().stringWidth(valStr);
                g2.drawString(valStr, x + (barW - valW) / 2, y - 3);

                // Nhãn giờ dưới đáy
                g2.setColor(C_TEXT_SUBTLE);
                g2.setFont(new Font(FONT_FAMILY, Font.PLAIN, 10));
                int lblW = g2.getFontMetrics().stringWidth(labels[i]);
                g2.drawString(labels[i], x + (barW - lblW) / 2, h - 4);
            }
            g2.dispose();
        }
    }

    /** Thanh phân chia tỷ lệ kép (Chuyển khoản vs Tiền mặt) */
    private static class DualProgressBar extends JComponent {
        private final double percentLeft;

        public DualProgressBar(double percentLeft) {
            this.percentLeft = percentLeft;
            setPreferredSize(new Dimension(280, 8));
            setMaximumSize(new Dimension(320, 8));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int splitX = (int) Math.round((w * percentLeft) / 100.0);

            // Nền tiền mặt (xanh nhạt)
            g2.setColor(new Color(0x38BDF8));
            g2.fillRoundRect(0, 0, w, h, h, h);

            // Phần chuyển khoản (xanh đậm)
            g2.setColor(C_PRIMARY);
            g2.fillRoundRect(0, 0, splitX, h, h, h);

            g2.dispose();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGO VÀ NÚT BO GÓC CHUẨN FLATLAF
    // ─────────────────────────────────────────────────────────────────────────

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
    }    // ─────────────────────────────────────────────────────────────────────────
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
            java.net.URL url = DashboardUI.class.getResource("/resources/images/carzone_logo.png");
            if (url == null) {
                url = DashboardUI.class.getResource("carzone_logo.png");
            }
            if (url != null) {
                rawLogoImage = javax.imageio.ImageIO.read(url);
            } else {
                java.io.File file = new java.io.File("src/resources/images/carzone_logo.png");
                if (!file.exists()) file = new java.io.File("src/ui/carzone_logo.png");
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
    private static Icon createWarningIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);
                g2.setColor(color);

                float w = size;
                float h = size;
                java.awt.geom.Path2D.Float path = new java.awt.geom.Path2D.Float();
                path.moveTo(w / 2f, 2f);
                path.lineTo(w - 2f, h - 3f);
                path.lineTo(2f, h - 3f);
                path.closePath();
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(path);

                // Dấu chấm than !
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine((int)(w / 2f), (int)(h * 0.38f), (int)(w / 2f), (int)(h * 0.62f));
                g2.fillOval((int)(w / 2f - 1.2f), (int)(h * 0.74f), 3, 3);

                g2.dispose();
            }

            @Override
            public int getIconWidth() { return size; }
            @Override
            public int getIconHeight() { return size; }
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CUSTOM SEARCH BOX & SCROLLABLE FLUID PANEL
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Khung tìm kiếm bo tròn pill-shape kèm icon kính lúp vector chuẩn Figma
     */
    private static class HeaderSearchBox extends JPanel {
        private final JTextField txtSearch;
        private boolean isFocused = false;

        public HeaderSearchBox(String placeholder) {
            setLayout(new BorderLayout(0, 0));
            setOpaque(false);
            setMinimumSize(new Dimension(140, 38));
            setPreferredSize(new Dimension(340, 38));
            setMaximumSize(new Dimension(480, 38));

            txtSearch = new JTextField(placeholder);
            txtSearch.setFont(f(Font.PLAIN, 13));
            txtSearch.setForeground(C_TEXT_MUTED);
            txtSearch.setOpaque(false);
            txtSearch.setBorder(new EmptyBorder(0, 38, 0, 14));

            txtSearch.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    isFocused = true;
                    if (txtSearch.getText().equals(placeholder)) {
                        txtSearch.setText("");
                        txtSearch.setForeground(C_TEXT_HEAD);
                    }
                    repaint();
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    isFocused = false;
                    if (txtSearch.getText().isBlank()) {
                        txtSearch.setText(placeholder);
                        txtSearch.setForeground(C_TEXT_MUTED);
                    }
                    repaint();
                }
            });

            add(txtSearch, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            // Nền pill bo tròn 22px
            g2.setColor(new Color(0xF1F5F9));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 22, 22);

            // Viền nhẹ
            g2.setColor(isFocused ? C_PRIMARY : C_INPUT_STROKE);
            g2.setStroke(new BasicStroke(isFocused ? 1.5f : 1.0f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 22, 22);

            // Icon Kính lúp (Magnifying glass vector) vẽ ở x=14
            int cx = 14;
            int cy = h / 2 - 2;
            int r = 5;
            g2.setColor(isFocused ? C_PRIMARY : C_TEXT_SUBTLE);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(cx, cy - r, r * 2, r * 2);

            // Cán kính lúp chéo 45 độ
            int hx = cx + r * 2 - 1;
            int hy = cy + r - 1;
            g2.drawLine(hx, hy, hx + 5, hy + 5);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Panel cuộn tự co giãn theo chiều ngang của Viewport,
     * ngăn chặn hoàn toàn việc tràn viền và xuất hiện thanh cuộn ngang khó chịu.
     */
    private static class ScrollableContentPanel extends JPanel implements Scrollable {
        public ScrollableContentPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 64;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // XỬ LÝ ĐĂNG XUẤT (Xác thực qua AuthController và xóa session)
    // ─────────────────────────────────────────────────────────────────────────
    private void xuLyDangXuat() {
        authController.dangXuat(this);
    }
}
