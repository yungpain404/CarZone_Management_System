package ui;

import connectDB.DbUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import controller.AuthController;
import controller.AuthController.NavigationItem;
import controller.DashboardController;
import controller.DashboardController.*;
import entity.NhanVien;

import enums.VaiTro;
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
 * MainFrame: Khung giao diện chính của toàn bộ hệ thống CarZone.
 * Tái hiện chuẩn mực 100% theo bản thiết kế Figma (tham chiếu image45.png).
 *
 * Cấu trúc 3 thành phần cốt lõi:
 * 1. HEADER (Ở TRÊN CÙNG - 70px):
 *    - Logo CarZone + Tên thương hiệu + Khẩu hiệu gara.
 *    - Thanh tìm kiếm thông minh bo tròn pill (Placeholder thay đổi linh hoạt theo vai trò).
 *    - Nút Help, Nút About.
 *    - Cụm thông tin định danh người dùng: Avatar chữ cái đầu, Họ và tên, Vai trò · Mã NV (Lấy từ Session).
 *    - Nút Đăng xuất an toàn (kết nối trực tiếp với AuthController).
 *
 * 2. NAVIGATION (BÊN TRÁI - 240px):
 *    - Menu thanh bên (Sidebar) cấp quyền tự động theo vai trò xác thực từ UserSession:
 *      + CHU_CUA_HANG: 10 chức năng (Dashboard, Doanh thu, Dịch vụ, Kho linh kiện, YC linh kiện, Khuyến mãi, Nhân viên, Tài khoản, Tra cứu, Hồ sơ cá nhân).
 *      + TIEP_NHAN_VIEN: 7 chức năng (Dashboard, Khách & xe, Lập phiếu sửa chữa, Hóa đơn thanh toán, Đổi trả, Tra cứu, Hồ sơ cá nhân).
 *      + KY_THUAT_VIEN: 6 chức năng (Dashboard, Phiếu được giao, Cập nhật sửa chữa, YC linh kiện, Tra cứu, Hồ sơ cá nhân).
 *    - Các nút bo góc 10px, hiệu ứng active nền xanh nhạt #EFF6FF viền #BFDBFE chữ xanh #2563EB theo chuẩn image45.png.
 *
 * 3. CONTENTPANEL (Ở GIỮA - CARDLAYOUT):
 *    - Tự động hoán đổi page hiển thị dựa trên navigation mà người dùng bấm chọn:
 *      + Trang Dashboard: Tái hiện đầy đủ KPI card, khối công việc, cảnh báo tồn kho, biểu đồ doanh thu (Chủ cửa hàng chuẩn image45.png).
 *      + Các trang phân hệ chức năng: Mở đúng trang với tiêu đề, breadcrumb điều hướng, thanh toolbar tác vụ và bảng dữ liệu thật từ DB.
 *      + Trang Thông tin cá nhân: Hiển thị thẻ hồ sơ nhân sự trích xuất từ Session.
 */
public class MainFrame extends JFrame {

    // ── Bảng màu chuẩn tuyệt đối từ Figma (image45.png) ─────────────────────
    public static final Color C_BG           = new Color(0xF8FAFC); // Nền app #F8FAFC
    public static final Color C_WHITE        = Color.WHITE;         // Nền card, header, sidebar
    public static final Color C_BORDER       = new Color(0xE2E8F0); // Viền card #E2E8F0
    public static final Color C_INPUT_STROKE = new Color(0xCBD5E1); // Viền input #CBD5E1
    public static final Color C_TEXT_HEAD    = new Color(0x0F172A); // Tiêu đề lớn #0F172A
    public static final Color C_TEXT_MAIN    = new Color(0x334155); // Nhãn chính #334155
    public static final Color C_TEXT_SUBTLE  = new Color(0x64748B); // Text phụ #64748B
    public static final Color C_TEXT_MUTED   = new Color(0x94A3B8); // Muted text #94A3B8
    public static final Color C_PRIMARY      = new Color(0x2563EB); // Xanh chủ đạo #2563EB
    public static final Color C_PRIMARY_HV   = new Color(0x1D4ED8); // Hover nút chính #1D4ED8
    public static final Color C_PRIMARY_BG   = new Color(0xEFF6FF); // Nền active / thẻ phụ #EFF6FF
    public static final Color C_PRIMARY_BR   = new Color(0xBFDBFE); // Viền nút Help #BFDBFE
    public static final Color C_DANGER       = new Color(0xDC2626); // Đỏ cảnh báo / Đăng xuất #DC2626
    public static final Color C_DANGER_BG    = new Color(0xFEF2F2); // Nền đỏ cảnh báo #FEF2F2
    public static final Color C_WARN         = new Color(0xD97706); // Cam / vàng #D97706
    public static final Color C_WARN_BG      = new Color(0xFFFBEB); // Nền vàng nhạt
    public static final Color C_SUCCESS      = new Color(0x059669); // Xanh lá #059669
    public static final Color C_SUCCESS_BG   = new Color(0xECFDF5); // Nền xanh lá nhạt

    // ── Font chữ chuẩn Arimo / SansSerif ─────────────────────────────────────
    private static final String FONT_FAMILY = detectFont();

    private static String detectFont() {
        for (String f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (f.equalsIgnoreCase("Arimo")) return "Arimo";
        }
        return "SansSerif";
    }

    public static Font f(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }

    public static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###");
    public static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy");
    /** Dùng riêng cho LocalDate (entity NhanVien.ngaySinh, ngayVaoLam). SimpleDateFormat không nhận LocalDate. */
    public static final DateTimeFormatter LOCAL_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Thuộc tính điều khiển ───────────────────────────────────────────────
    private final NhanVien nhanVien;
    private final AuthController authController = new AuthController();
    private final DashboardController dashboardController = new DashboardController();

    private CardLayout contentCardLayout;
    private JPanel pnlContentPanel;
    private final Map<String, JButton> mapNavButtons = new HashMap<>();
    private String activeNavId = "NAV_DASHBOARD";

    // Header dynamic components
    private JLabel lblHeaderUserName;
    private JLabel lblHeaderUserRole;
    private JLabel lblHeaderAvatar;
    private HeaderSearchBox searchBox;

    public MainFrame() {
        this(null);
    }

    public MainFrame(NhanVien nv) {
        // Đồng bộ đối tượng nhân viên vào phiên UserSession
        if (UserSession.getInstance().daDangNhap()) {
            this.nhanVien = UserSession.getInstance().getNhanVien();
        } else if (nv != null) {
            this.nhanVien = nv;
            UserSession.getInstance().batDauPhien(null, nv);
        } else {
            this.nhanVien = taoNhanVienMacDinh();
            UserSession.getInstance().batDauPhien(null, this.nhanVien);
        }

        String tenVaiTro = AuthController.layTenVaiTro(this.nhanVien.getVaiTro());
        setTitle("CarZone – " + tenVaiTro + " (" + this.nhanVien.getHoTen() + ")");
        setSize(1440, 920);
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

    // ─────────────────────────────────────────────────────────────────────────
    // KHỞI TẠO CẤU TRÚC 3 THÀNH PHẦN (BorderLayout)
    // ─────────────────────────────────────────────────────────────────────────
    private void initComponents() {
        setLayout(new BorderLayout(0, 0));

        // 1. HEADER Ở TRÊN CÙNG (NORTH)
        add(buildHeader(), BorderLayout.NORTH);

        // 2. KHỐI TRUNG TÂM: SIDEBAR TRÁI (WEST) + CONTENTPANEL GIỮA (CENTER)
        JPanel centerContainer = new JPanel(new BorderLayout(0, 0));
        centerContainer.setOpaque(false);

        // Navigation bên trái
        centerContainer.add(buildSidebar(), BorderLayout.WEST);

        // ContentPanel ở giữa dùng CardLayout để mở đúng Page theo navigation
        contentCardLayout = new CardLayout();
        pnlContentPanel = new JPanel(contentCardLayout);
        pnlContentPanel.setOpaque(false);

        // Nạp tất cả các Page dựa trên quyền của Role vào ContentPanel
        napCacPageTheoRole();

        centerContainer.add(pnlContentPanel, BorderLayout.CENTER);
        add(centerContainer, BorderLayout.CENTER);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. HEADER (70px cố định – Bám sát tuyệt đối image45.png)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 0));
        header.setBackground(C_WHITE);
        header.setPreferredSize(new Dimension(1440, 70));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER));

        // ── Trái: Logo CarZone + Tên hệ thống ────────────────────────────────
        JPanel pnlLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 16));
        pnlLeft.setOpaque(false);

        JComponent logoComp = createLogoComponent(120, 35);
        pnlLeft.add(logoComp);

        JLabel lblSub = new JLabel("Hệ thống quản lý gara ô tô");
        lblSub.setFont(f(Font.PLAIN, 12));
        lblSub.setForeground(C_TEXT_SUBTLE);
        pnlLeft.add(lblSub);

        header.add(pnlLeft, BorderLayout.WEST);

        // ── Giữa: Search Bar bo tròn pill theo Figma (Responsive theo vai trò) ──
        JPanel pnlCenter = new JPanel(new GridBagLayout());
        pnlCenter.setOpaque(false);
        pnlCenter.setBorder(new EmptyBorder(0, 16, 0, 16));

        String searchPlaceholder = switch (nhanVien.getVaiTro()) {
            case CHU_CUA_HANG   -> "Tìm kiếm dịch vụ, linh kiện…";
            case TIEP_NHAN_VIEN -> "Tìm khách hàng, xe, phiếu…";
            case KY_THUAT_VIEN  -> "Tìm phiếu, linh kiện…";
        };

        searchBox = new HeaderSearchBox(searchPlaceholder);
        GridBagConstraints gbcSearch = new GridBagConstraints();
        gbcSearch.gridx = 0;
        gbcSearch.gridy = 0;
        gbcSearch.weightx = 1.0;
        gbcSearch.fill = GridBagConstraints.HORIZONTAL;
        pnlCenter.add(searchBox, gbcSearch);
        header.add(pnlCenter, BorderLayout.CENTER);

        // ── Phải: Help, About, Avatar & Thông tin nhân viên, Nút Đăng xuất ────
        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 15));
        pnlRight.setOpaque(false);

        // Nút ? Help
        JButton btnHelp = new RoundedButton("?  Help", 12, C_PRIMARY_BG, new Color(0xDBEAFE), C_PRIMARY_BR);
        btnHelp.setFont(f(Font.BOLD, 12));
        btnHelp.setForeground(C_PRIMARY);
        btnHelp.setPreferredSize(new Dimension(86, 36));
        btnHelp.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Hệ thống Quản lý Gara Ô tô CarZone v1.0\nHướng dẫn sử dụng: Tra cứu tài liệu nghiệp vụ hoặc liên hệ quản trị viên.",
                "Trợ giúp CarZone", JOptionPane.INFORMATION_MESSAGE));
        pnlRight.add(btnHelp);

        // Nút ⓘ About
        JButton btnAbout = new RoundedButton("ⓘ  About", 12, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnAbout.setFont(f(Font.PLAIN, 12));
        btnAbout.setForeground(C_TEXT_MAIN);
        btnAbout.setPreferredSize(new Dimension(88, 36));
        btnAbout.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "CarZone - Phiên bản 1.0.0 (Nhóm 01 - ĐH Công nghiệp TP.HCM)\nĐồ án môn Phát triển ứng dụng.\n© 2026 Carzone. Bảo lưu mọi quyền.",
                "Về CarZone", JOptionPane.INFORMATION_MESSAGE));
        pnlRight.add(btnAbout);

        // Vạch phân cách dọc mỏng
        JSeparator sep1 = new JSeparator(SwingConstants.VERTICAL);
        sep1.setPreferredSize(new Dimension(1, 24));
        sep1.setForeground(C_BORDER);
        pnlRight.add(sep1);

        // Avatar tròn chứa ký tự đầu họ tên
        String initial = (nhanVien.getHoTen() != null && !nhanVien.getHoTen().isBlank())
                ? String.valueOf(nhanVien.getHoTen().trim().charAt(0)).toUpperCase() : "U";
        lblHeaderAvatar = new JLabel(initial, SwingConstants.CENTER) {
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
        lblHeaderAvatar.setPreferredSize(new Dimension(36, 36));
        lblHeaderAvatar.setFont(f(Font.BOLD, 15));
        lblHeaderAvatar.setForeground(C_PRIMARY);
        lblHeaderAvatar.setOpaque(false);
        pnlRight.add(lblHeaderAvatar);

        // Cụm Họ tên và Vai trò · Mã NV
        JPanel pnlUserText = new JPanel();
        pnlUserText.setLayout(new BoxLayout(pnlUserText, BoxLayout.Y_AXIS));
        pnlUserText.setOpaque(false);

        lblHeaderUserName = new JLabel(nhanVien.getHoTen());
        lblHeaderUserName.setFont(f(Font.BOLD, 13));
        lblHeaderUserName.setForeground(C_TEXT_HEAD);
        pnlUserText.add(lblHeaderUserName);

        String roleStr = AuthController.layTenVaiTro(nhanVien.getVaiTro()) + " · " + (nhanVien.getMaNV() != null ? nhanVien.getMaNV() : "");
        lblHeaderUserRole = new JLabel(roleStr);
        lblHeaderUserRole.setFont(f(Font.PLAIN, 11));
        lblHeaderUserRole.setForeground(C_TEXT_SUBTLE);
        pnlUserText.add(lblHeaderUserRole);

        pnlRight.add(pnlUserText);

        // Vạch phân cách dọc mỏng
        JSeparator sep2 = new JSeparator(SwingConstants.VERTICAL);
        sep2.setPreferredSize(new Dimension(1, 24));
        sep2.setForeground(C_BORDER);
        pnlRight.add(sep2);

        // Nút Đăng xuất màu đỏ #DC2626
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
    // 2. NAVIGATION (SIDEBAR 240px – Cấp quyền tự động theo Role)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(C_WHITE);
        sidebar.setPreferredSize(new Dimension(240, 800));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, C_BORDER),
                new EmptyBorder(14, 10, 14, 10)
        ));

        // Lấy danh sách chức năng CHÍNH XÁC theo vai trò trong UserSession
        List<NavigationItem> items = authController.layDanhSachNavigationTheoRole(nhanVien.getVaiTro());
        mapNavButtons.clear();

        for (NavigationItem item : items) {
            JButton btnItem = createSidebarButton(item);
            btnItem.setAlignmentX(Component.CENTER_ALIGNMENT);
            mapNavButtons.put(item.getId(), btnItem);
            sidebar.add(btnItem);
            sidebar.add(Box.createVerticalStrut(4));
        }

        // Khoảng trống co giãn đẩy Footer xuống đáy
        sidebar.add(Box.createVerticalGlue());

        // Dòng thông tin phiên bản ở đáy sidebar
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

        btn.setLayout(new BorderLayout(10, 0));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(isMultiLine ? 4 : 5, 10, isMultiLine ? 4 : 5, 10));

        int btnH = isMultiLine ? 44 : 36;
        btn.setMaximumSize(new Dimension(220, btnH));
        btn.setPreferredSize(new Dimension(220, btnH));

        JLabel lblIcon = new JLabel(item.getIcon(), SwingConstants.CENTER);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        lblIcon.setPreferredSize(new Dimension(24, 24));
        btn.add(lblIcon, BorderLayout.WEST);

        boolean isInitialActive = item.getId().equals(activeNavId);
        JLabel lblText = new JLabel(htmlText);
        lblText.setFont(f(isInitialActive ? Font.BOLD : Font.PLAIN, 11));
        lblText.setForeground(isInitialActive ? C_PRIMARY : C_TEXT_MAIN);
        btn.add(lblText, BorderLayout.CENTER);

        // Click vào nút navigation sẽ đổi Page trong ContentPanel
        btn.addActionListener(e -> moPageTheoNavigation(item.getId()));

        return btn;
    }

    /**
     * Mở đúng Page tương ứng với Navigation được chọn và cập nhật trạng thái Sidebar.
     */
    public void moPageTheoNavigation(String navId) {
        if (navId == null || navId.equals(activeNavId)) return;
        this.activeNavId = navId;

        // Chuyển card trong contentPanel
        if (contentCardLayout != null && pnlContentPanel != null) {
            contentCardLayout.show(pnlContentPanel, navId);
        }

        // Cập nhật lại màu sắc của các nút bên Navigation Sidebar
        for (Map.Entry<String, JButton> entry : mapNavButtons.entrySet()) {
            boolean isCur = entry.getKey().equals(activeNavId);
            JButton b = entry.getValue();
            for (Component c : b.getComponents()) {
                if (c instanceof JLabel lbl && !lbl.getText().matches("^[\\p{So}\\p{Sm}].*")) {
                    lbl.setFont(f(isCur ? Font.BOLD : Font.PLAIN, 11));
                    lbl.setForeground(isCur ? C_PRIMARY : C_TEXT_MAIN);
                }
            }
            b.repaint();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. CONTENTPANEL: Nạp các Page tương ứng với Navigation theo Role
    // ─────────────────────────────────────────────────────────────────────────
    private void napCacPageTheoRole() {
        List<NavigationItem> items = authController.layDanhSachNavigationTheoRole(nhanVien.getVaiTro());

        for (NavigationItem item : items) {
            if ("NAV_DASHBOARD".equals(item.getId())) {
                pnlContentPanel.add(buildRoleDashboardPage(), item.getId());
            } else if ("NAV_THONG_TIN_CA_NHAN".equals(item.getId())) {
                pnlContentPanel.add(buildProfilePage(item), item.getId());
            } else {
                pnlContentPanel.add(buildModulePage(item), item.getId());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3A. TRANG DASHBOARD THEO VAI TRÒ (Bám sát 100% Figma image45.png)
    // ─────────────────────────────────────────────────────────────────────────
    private JComponent buildRoleDashboardPage() {
        JComponent dashboardView = switch (nhanVien.getVaiTro()) {
            case CHU_CUA_HANG   -> buildChuCuaHangDashboardView();
            case TIEP_NHAN_VIEN -> buildTiepNhanVienDashboardView();
            case KY_THUAT_VIEN  -> buildKyThuatVienDashboardView();
        };

        JScrollPane scroll = new JScrollPane(dashboardView);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }

    // Dashboard Chủ cửa hàng: Tái hiện chuẩn xác 100% frame image45.png
    private JPanel buildChuCuaHangDashboardView() {
        ChuCuaHangDashboardData data = dashboardController.layDataChuCuaHang();

        ScrollableContentPanel main = new ScrollableContentPanel();
        main.setBorder(new EmptyBorder(16, 24, 16, 24));

        // 1. Tiêu đề Dashboard + Badge Ngày
        JPanel pnlTitle = new JPanel(new BorderLayout(0, 0));
        pnlTitle.setOpaque(false);
        pnlTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JLabel lblTitle = new JLabel("Dashboard");
        lblTitle.setFont(f(Font.BOLD, 22));
        lblTitle.setForeground(C_TEXT_HEAD);
        pnlTitle.add(lblTitle, BorderLayout.WEST);

        JLabel lblDateBadge = new JLabel("📅  " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), SwingConstants.CENTER);
        lblDateBadge.setFont(f(Font.PLAIN, 12));
        lblDateBadge.setForeground(C_TEXT_MAIN);
        lblDateBadge.setBackground(C_WHITE);
        lblDateBadge.setOpaque(true);
        lblDateBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
        pnlTitle.add(lblDateBadge, BorderLayout.EAST);
        main.add(pnlTitle);

        main.add(Box.createVerticalStrut(16));

        // 2. Hàng 4 thẻ KPI Card (image45.png)
        JPanel pnlKpis = new JPanel(new GridLayout(1, 4, 14, 0));
        pnlKpis.setOpaque(false);
        pnlKpis.setMaximumSize(new Dimension(Integer.MAX_VALUE, 135));

        // Card 1: Yêu cầu chờ phê duyệt (Nổi bật nền xanh #2563EB)
        pnlKpis.add(createBlueKpiCard(
                String.format("%02d", data.yeuCauChoDuyet),
                "Yêu cầu chờ phê duyệt",
                (data.tongYeuCauTheoDoi > 0 ? (data.yeuCauChoDuyet + " / " + data.tongYeuCauTheoDoi + " yêu cầu đang theo dõi") : "Chưa có yêu cầu nào"),
                "Xử lý yêu cầu cập nhật linh kiện ↗",
                "NAV_YC_LINH_KIEN"
        ));

        // Card 2: Chờ xác nhận nhập kho
        pnlKpis.add(createWhiteKpiCard(
                String.format("%02d", data.choXacNhanNhapKho),
                "Chờ xác nhận nhập kho",
                "Đã duyệt · Chưa nhập kho",
                "Xử lý yêu cầu cập nhật linh kiện ↗",
                "📦",
                "NAV_YC_LINH_KIEN"
        ));

        // Card 3: Linh kiện hết hàng (Cảnh báo cam)
        pnlKpis.add(createWhiteKpiCard(
                String.format("%02d", data.linhKienHetHang),
                "Linh kiện hết hàng",
                "Các mã đang kinh doanh",
                "Quản lý linh kiện và tồn kho ↗",
                "⚠️",
                "NAV_LINH_KIEN_KHO"
        ));

        // Card 4: Hồ sơ chưa có tài khoản
        pnlKpis.add(createWhiteKpiCard(
                String.format("%02d", data.hoSoChuaCoTaiKhoan),
                "Hồ sơ chưa có tài khoản",
                "Trong các hồ sơ đang hiển thị",
                "Quản lý nhân viên / Tạo tài khoản ↗",
                "👥",
                "NAV_NHAN_VIEN"
        ));

        main.add(pnlKpis);
        main.add(Box.createVerticalStrut(20));

        // 3. Khối Công việc cần xử lý & Cảnh báo hết hàng (Bảng 2 cột theo image45.png)
        JPanel pnlSectionWork = new JPanel(new BorderLayout(0, 8));
        pnlSectionWork.setOpaque(false);
        pnlSectionWork.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel lblWorkHead = new JLabel("Công việc cần xử lý");
        lblWorkHead.setFont(f(Font.BOLD, 15));
        lblWorkHead.setForeground(C_TEXT_HEAD);
        pnlSectionWork.add(lblWorkHead, BorderLayout.WEST);

        JLabel lblWorkScope = new JLabel("Phạm vi: yêu cầu, linh kiện và hồ sơ đang hiển thị");
        lblWorkScope.setFont(f(Font.PLAIN, 11));
        lblWorkScope.setForeground(C_TEXT_MUTED);
        pnlSectionWork.add(lblWorkScope, BorderLayout.EAST);
        main.add(pnlSectionWork);

        main.add(Box.createVerticalStrut(8));

        JPanel pnlWorkRow = new JPanel(new GridBagLayout());
        pnlWorkRow.setOpaque(false);
        pnlWorkRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        GridBagConstraints gbcL = new GridBagConstraints();
        gbcL.gridx = 0;
        gbcL.weightx = 0.65;
        gbcL.fill = GridBagConstraints.BOTH;
        gbcL.insets = new Insets(0, 0, 0, 12);
        pnlWorkRow.add(buildCardTableYeuCau(data), gbcL);

        GridBagConstraints gbcR = new GridBagConstraints();
        gbcR.gridx = 1;
        gbcR.weightx = 0.35;
        gbcR.fill = GridBagConstraints.BOTH;
        pnlWorkRow.add(buildCardCanhBaoHetHang(data), gbcR);

        main.add(pnlWorkRow);
        main.add(Box.createVerticalStrut(20));

        // 4. Khối Kinh doanh trong ngày (Biểu đồ cột + Cơ cấu thanh toán theo image45.png)
        main.add(buildCardKinhDoanhTrongNgay(data));

        return main;
    }

    private JPanel createBlueKpiCard(String number, String title, String sub, String link, String targetNav) {
        JPanel c = new JPanel(new BorderLayout(0, 6));
        c.setBackground(C_PRIMARY);
        c.setBorder(new EmptyBorder(14, 16, 12, 16));

        JPanel top = new JPanel(new BorderLayout(0, 2));
        top.setOpaque(false);
        JLabel num = new JLabel(number);
        num.setFont(f(Font.BOLD, 28));
        num.setForeground(C_WHITE);
        top.add(num, BorderLayout.WEST);

        JLabel icon = new JLabel("📋", SwingConstants.RIGHT);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        top.add(icon, BorderLayout.EAST);

        JLabel t = new JLabel(title);
        t.setFont(f(Font.BOLD, 12));
        t.setForeground(C_WHITE);
        top.add(t, BorderLayout.SOUTH);
        c.add(top, BorderLayout.NORTH);

        JLabel s = new JLabel(sub);
        s.setFont(f(Font.PLAIN, 11));
        s.setForeground(new Color(0xDBEAFE));
        c.add(s, BorderLayout.CENTER);

        JButton lnk = new JButton(link);
        lnk.setFont(f(Font.BOLD, 10));
        lnk.setForeground(new Color(0xBFDBFE));
        lnk.setContentAreaFilled(false);
        lnk.setBorderPainted(false);
        lnk.setFocusPainted(false);
        lnk.setHorizontalAlignment(SwingConstants.LEFT);
        lnk.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lnk.addActionListener(e -> moPageTheoNavigation(targetNav));
        c.add(lnk, BorderLayout.SOUTH);

        return c;
    }

    private JPanel createWhiteKpiCard(String number, String title, String sub, String link, String iconStr, String targetNav) {
        JPanel c = new JPanel(new BorderLayout(0, 6));
        c.setBackground(C_WHITE);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(14, 16, 12, 16)
        ));

        JPanel top = new JPanel(new BorderLayout(0, 2));
        top.setOpaque(false);
        JLabel num = new JLabel(number);
        num.setFont(f(Font.BOLD, 28));
        num.setForeground(C_TEXT_HEAD);
        top.add(num, BorderLayout.WEST);

        JLabel icon = new JLabel(iconStr, SwingConstants.RIGHT);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        top.add(icon, BorderLayout.EAST);

        JLabel t = new JLabel(title);
        t.setFont(f(Font.BOLD, 12));
        t.setForeground(C_TEXT_HEAD);
        top.add(t, BorderLayout.SOUTH);
        c.add(top, BorderLayout.NORTH);

        JLabel s = new JLabel(sub);
        s.setFont(f(Font.PLAIN, 11));
        s.setForeground(C_TEXT_SUBTLE);
        c.add(s, BorderLayout.CENTER);

        JButton lnk = new JButton(link);
        lnk.setFont(f(Font.BOLD, 10));
        lnk.setForeground(C_PRIMARY);
        lnk.setContentAreaFilled(false);
        lnk.setBorderPainted(false);
        lnk.setFocusPainted(false);
        lnk.setHorizontalAlignment(SwingConstants.LEFT);
        lnk.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lnk.addActionListener(e -> moPageTheoNavigation(targetNav));
        c.add(lnk, BorderLayout.SOUTH);

        return c;
    }

    private JPanel buildCardTableYeuCau(ChuCuaHangDashboardData data) {
        JPanel c = new JPanel(new BorderLayout(0, 8));
        c.setBackground(C_WHITE);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(14, 16, 12, 16)
        ));

        JPanel head = new JPanel(new BorderLayout(0, 0));
        head.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JLabel title = new JLabel("Yêu cầu chờ duyệt");
        title.setFont(f(Font.BOLD, 13));
        title.setForeground(C_TEXT_HEAD);
        left.add(title);

        JLabel badge = new JLabel(data.yeuCauChoDuyet + " yêu cầu", SwingConstants.CENTER);
        badge.setFont(f(Font.PLAIN, 10));
        badge.setForeground(C_PRIMARY);
        badge.setBackground(C_PRIMARY_BG);
        badge.setOpaque(true);
        badge.setBorder(new EmptyBorder(2, 6, 2, 6));
        left.add(badge);
        head.add(left, BorderLayout.WEST);

        JButton btnOpen = new JButton("Mở danh sách ↗");
        btnOpen.setFont(f(Font.BOLD, 11));
        btnOpen.setForeground(C_PRIMARY);
        btnOpen.setContentAreaFilled(false);
        btnOpen.setBorderPainted(false);
        btnOpen.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnOpen.addActionListener(e -> moPageTheoNavigation("NAV_YC_LINH_KIEN"));
        head.add(btnOpen, BorderLayout.EAST);
        c.add(head, BorderLayout.NORTH);

        // Bảng yêu cầu
        String[] cols = {"MÃ / NGÀY TẠO", "KTV LẬP", "LINH KIỆN", "YÊU CẦU", "TỒN KHO"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        for (YeuCauRow r : data.danhSachYeuCau) {
            model.addRow(new Object[]{r.maYeuCau + " (" + r.ngayTao + ")", r.ktvLap, r.linhKien, r.soLuongYeuCau, r.tonKho});
        }
        if (model.getRowCount() == 0) {
            model.addRow(new Object[]{"Chưa có yêu cầu nào chờ duyệt", "—", "—", "—", "—"});
        }

        JTable tbl = new JTable(model);
        tbl.setFont(f(Font.PLAIN, 11));
        tbl.setRowHeight(32);
        tbl.setShowGrid(false);
        tbl.setIntercellSpacing(new Dimension(0, 0));
        tbl.getTableHeader().setFont(f(Font.BOLD, 10));
        tbl.getTableHeader().setBackground(new Color(0xF8FAFC));
        tbl.getTableHeader().setForeground(C_TEXT_SUBTLE);
        tbl.getTableHeader().setPreferredSize(new Dimension(0, 28));

        JScrollPane sp = new JScrollPane(tbl);
        sp.setBorder(null);
        c.add(sp, BorderLayout.CENTER);

        return c;
    }

    private JPanel buildCardCanhBaoHetHang(ChuCuaHangDashboardData data) {
        JPanel c = new JPanel(new BorderLayout(0, 10));
        c.setBackground(C_WHITE);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(14, 16, 12, 16)
        ));

        JPanel head = new JPanel(new BorderLayout(0, 0));
        head.setOpaque(false);
        JLabel title = new JLabel("⚠️  Cảnh báo hết hàng");
        title.setFont(f(Font.BOLD, 13));
        title.setForeground(C_TEXT_HEAD);
        head.add(title, BorderLayout.WEST);

        JLabel badge = new JLabel(data.linhKienHetHang + " mã", SwingConstants.CENTER);
        badge.setFont(f(Font.BOLD, 10));
        badge.setForeground(C_DANGER);
        badge.setBackground(C_DANGER_BG);
        badge.setOpaque(true);
        badge.setBorder(new EmptyBorder(2, 6, 2, 6));
        head.add(badge, BorderLayout.EAST);
        c.add(head, BorderLayout.NORTH);

        JPanel listPnl = new JPanel();
        listPnl.setLayout(new BoxLayout(listPnl, BoxLayout.Y_AXIS));
        listPnl.setOpaque(false);

        if (data.danhSachHetHang.isEmpty()) {
            JLabel empty = new JLabel("Hiện không có linh kiện nào bị hết hàng.");
            empty.setFont(f(Font.PLAIN, 11));
            empty.setForeground(C_TEXT_MUTED);
            listPnl.add(empty);
        } else {
            for (LinhKienHetHangRow r : data.danhSachHetHang) {
                JPanel item = new JPanel(new BorderLayout(0, 2));
                item.setOpaque(false);
                item.setBorder(new EmptyBorder(4, 0, 4, 0));

                JLabel name = new JLabel(r.tenLinhKien);
                name.setFont(f(Font.BOLD, 12));
                name.setForeground(C_TEXT_HEAD);
                item.add(name, BorderLayout.WEST);

                JLabel val = new JLabel("0 " + r.donVi);
                val.setFont(f(Font.BOLD, 12));
                val.setForeground(C_DANGER);
                item.add(val, BorderLayout.EAST);

                JLabel sub = new JLabel(r.maLinhKien + " • " + r.ghiChuYeuCau);
                sub.setFont(f(Font.PLAIN, 10));
                sub.setForeground(C_TEXT_MUTED);
                item.add(sub, BorderLayout.SOUTH);

                listPnl.add(item);
                listPnl.add(Box.createVerticalStrut(6));
            }
        }
        c.add(listPnl, BorderLayout.CENTER);

        JButton lnk = new JButton("Quản lý linh kiện và tồn kho ↗");
        lnk.setFont(f(Font.BOLD, 11));
        lnk.setForeground(C_PRIMARY);
        lnk.setContentAreaFilled(false);
        lnk.setBorderPainted(false);
        lnk.setHorizontalAlignment(SwingConstants.LEFT);
        lnk.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lnk.addActionListener(e -> moPageTheoNavigation("NAV_LINH_KIEN_KHO"));
        c.add(lnk, BorderLayout.SOUTH);

        return c;
    }

    private JPanel buildCardKinhDoanhTrongNgay(ChuCuaHangDashboardData data) {
        JPanel c = new JPanel(new BorderLayout(0, 12));
        c.setBackground(C_WHITE);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(16, 20, 16, 20)
        ));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));

        JPanel head = new JPanel(new BorderLayout(0, 0));
        head.setOpaque(false);
        JLabel title = new JLabel("Kinh doanh trong ngày  •  " + data.soHoaDonThanhToan + " hóa đơn đã thanh toán");
        title.setFont(f(Font.BOLD, 14));
        title.setForeground(C_TEXT_HEAD);
        head.add(title, BorderLayout.WEST);

        JButton btnMore = new JButton("Thống kê doanh thu ↗");
        btnMore.setFont(f(Font.BOLD, 11));
        btnMore.setForeground(C_PRIMARY);
        btnMore.setContentAreaFilled(false);
        btnMore.setBorderPainted(false);
        btnMore.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnMore.addActionListener(e -> moPageTheoNavigation("NAV_THONG_KE"));
        head.add(btnMore, BorderLayout.EAST);
        c.add(head, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(1, 3, 20, 0));
        body.setOpaque(false);

        // Khối Doanh thu sau hoàn
        JPanel boxRevenue = new JPanel(new BorderLayout(0, 4));
        boxRevenue.setBackground(new Color(0xF0F9FF));
        boxRevenue.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xBAE6FD), 1),
                new EmptyBorder(12, 14, 12, 14)
        ));
        JLabel lblRevSub = new JLabel("DOANH THU SAU HOÀN");
        lblRevSub.setFont(f(Font.BOLD, 10));
        lblRevSub.setForeground(new Color(0x0284C7));
        boxRevenue.add(lblRevSub, BorderLayout.NORTH);

        JLabel lblRevVal = new JLabel(MONEY_FMT.format(data.doanhThuSauHoan) + " VNĐ");
        lblRevVal.setFont(f(Font.BOLD, 20));
        lblRevVal.setForeground(new Color(0x0369A1));
        boxRevenue.add(lblRevVal, BorderLayout.CENTER);

        JLabel lblRevFoot = new JLabel("Tổng thu: " + MONEY_FMT.format(data.tongThu) + " VNĐ  |  Hoàn: " + MONEY_FMT.format(data.hoanTien) + " VNĐ");
        lblRevFoot.setFont(f(Font.PLAIN, 10));
        lblRevFoot.setForeground(C_TEXT_SUBTLE);
        boxRevenue.add(lblRevFoot, BorderLayout.SOUTH);
        body.add(boxRevenue);

        // Khối Cơ cấu thanh toán
        JPanel boxPay = new JPanel(new GridLayout(2, 1, 0, 8));
        boxPay.setOpaque(false);

        JPanel rowCK = new JPanel(new BorderLayout(0, 0));
        rowCK.setOpaque(false);
        rowCK.add(new JLabel("● Chuyển khoản"), BorderLayout.WEST);
        rowCK.add(new JLabel(MONEY_FMT.format(data.doanhThuChuyenKhoan) + " VNĐ"), BorderLayout.EAST);
        boxPay.add(rowCK);

        JPanel rowTM = new JPanel(new BorderLayout(0, 0));
        rowTM.setOpaque(false);
        rowTM.add(new JLabel("● Tiền mặt"), BorderLayout.WEST);
        rowTM.add(new JLabel(MONEY_FMT.format(data.doanhThuTienMat) + " VNĐ"), BorderLayout.EAST);
        boxPay.add(rowTM);

        body.add(boxPay);
        c.add(body, BorderLayout.CENTER);

        return c;
    }

    // Dashboard Tiếp nhận viên
    private JPanel buildTiepNhanVienDashboardView() {
        TiepNhanVienDashboardData data = dashboardController.layDataTiepNhanVien();

        ScrollableContentPanel main = new ScrollableContentPanel();
        main.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel title = new JLabel("Dashboard Tiếp Nhận Xe");
        title.setFont(f(Font.BOLD, 22));
        title.setForeground(C_TEXT_HEAD);
        main.add(title);
        main.add(Box.createVerticalStrut(16));

        JPanel pnlKpis = new JPanel(new GridLayout(1, 4, 14, 0));
        pnlKpis.setOpaque(false);
        pnlKpis.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        pnlKpis.add(createWhiteKpiCard(String.format("%02d", data.phieuChoSua), "Chờ sửa chữa", "Xe đã tiếp nhận", "Xem danh sách ↗", "⏳", "NAV_PHIEU_TIEP_NHAN"));
        pnlKpis.add(createWhiteKpiCard(String.format("%02d", data.phieuDangSua), "Đang sửa chữa", "KTV đang xử lý", "Xem danh sách ↗", "🔧", "NAV_PHIEU_TIEP_NHAN"));
        pnlKpis.add(createWhiteKpiCard(String.format("%02d", data.phieuChoLinhKien), "Chờ linh kiện", "Chờ xuất kho", "Xem danh sách ↗", "📦", "NAV_PHIEU_TIEP_NHAN"));
        pnlKpis.add(createWhiteKpiCard(String.format("%02d", data.phieuHoanTatChoThu), "Hoàn tất chờ thu", "Sẵn sàng xuất hóa đơn", "Lập hóa đơn ↗", "🧾", "NAV_HOA_DON"));
        main.add(pnlKpis);

        main.add(Box.createVerticalStrut(20));

        // Bảng phiếu tiếp nhận gần đây
        JPanel cardTable = new JPanel(new BorderLayout(0, 10));
        cardTable.setBackground(C_WHITE);
        cardTable.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(14, 16, 14, 16)
        ));
        cardTable.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));

        JLabel tblTitle = new JLabel("Tiến độ xe tại xưởng trong ngày");
        tblTitle.setFont(f(Font.BOLD, 14));
        tblTitle.setForeground(C_TEXT_HEAD);
        cardTable.add(tblTitle, BorderLayout.NORTH);

        String[] cols = {"MÃ PHIẾU", "BIỂN SỐ XE", "KHÁCH HÀNG", "SỐ ĐIỆN THOẠI", "KTV PHỤ TRÁCH", "GIỜ TIẾP NHẬN", "TRẠNG THÁI"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        for (PhieuTiepNhanRow r : data.danhSachPhieu) {
            model.addRow(new Object[]{r.maPhieu, r.bienSo, r.khachHang, r.soDienThoai, r.ktvChinh, r.thoiDiem, r.trangThai.toString()});
        }
        if (model.getRowCount() == 0) {
            model.addRow(new Object[]{"Chưa có phiếu tiếp nhận nào trong ngày", "—", "—", "—", "—", "—", "—"});
        }

        JTable tbl = new JTable(model);
        tbl.setFont(f(Font.PLAIN, 12));
        tbl.setRowHeight(34);
        tbl.getTableHeader().setFont(f(Font.BOLD, 11));
        tbl.getTableHeader().setBackground(new Color(0xF8FAFC));
        tbl.getTableHeader().setPreferredSize(new Dimension(0, 32));

        JScrollPane sp = new JScrollPane(tbl);
        sp.setBorder(null);
        cardTable.add(sp, BorderLayout.CENTER);
        main.add(cardTable);

        return main;
    }

    // Dashboard Kỹ thuật viên
    private JPanel buildKyThuatVienDashboardView() {
        KyThuatVienDashboardData data = dashboardController.layDataKyThuatVien(nhanVien);

        ScrollableContentPanel main = new ScrollableContentPanel();
        main.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel title = new JLabel("Dashboard Kỹ Thuật Viên");
        title.setFont(f(Font.BOLD, 22));
        title.setForeground(C_TEXT_HEAD);
        main.add(title);
        main.add(Box.createVerticalStrut(16));

        JPanel pnlKpis = new JPanel(new GridLayout(1, 4, 14, 0));
        pnlKpis.setOpaque(false);
        pnlKpis.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        pnlKpis.add(createWhiteKpiCard(String.format("%02d", data.phieuMoiPhanCong), "Phiếu mới phân công", "Chờ bắt đầu", "Xem phiếu ↗", "📋", "NAV_PHIEU_PHAN_CONG"));
        pnlKpis.add(createWhiteKpiCard(String.format("%02d", data.suaChuaDangThucHien), "Đang sửa chữa", "Tiến độ hiện tại", "Cập nhật ↗", "🛠️", "NAV_CAP_NHAT_SUA_CHUA"));
        pnlKpis.add(createWhiteKpiCard(String.format("%02d", data.yeuCauChoDuyet), "YC linh kiện chờ duyệt", "Gửi tới CCH", "Lịch sử YC ↗", "📦", "NAV_YC_LINH_KIEN"));
        pnlKpis.add(createWhiteKpiCard(String.format("%02d", data.phieuHoanTat), "Đã hoàn tất", "Hoàn thành trong kỳ", "Xem chi tiết ↗", "✅", "NAV_PHIEU_PHAN_CONG"));
        main.add(pnlKpis);

        main.add(Box.createVerticalStrut(20));

        // Bảng phiếu được giao
        JPanel cardTable = new JPanel(new BorderLayout(0, 10));
        cardTable.setBackground(C_WHITE);
        cardTable.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(14, 16, 14, 16)
        ));
        cardTable.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));

        JLabel tblTitle = new JLabel("Danh sách xe được phân công sửa chữa");
        tblTitle.setFont(f(Font.BOLD, 14));
        tblTitle.setForeground(C_TEXT_HEAD);
        cardTable.add(tblTitle, BorderLayout.NORTH);

        String[] cols = {"MÃ PHIẾU", "BIỂN SỐ XE", "DÒNG XE", "NGÀY PHÂN CÔNG", "NỘI DUNG CÔNG VIỆC", "TRẠNG THÁI"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        for (PhieuPhanCongRow r : data.danhSachPhanCong) {
            model.addRow(new Object[]{r.maPhieu, r.bienSo, r.dongXe, r.ngayPhanCong, r.noiDungCongViec, r.trangThai.toString()});
        }
        if (model.getRowCount() == 0) {
            model.addRow(new Object[]{"Hiện chưa có phiếu sửa chữa nào được phân công", "—", "—", "—", "—", "—"});
        }

        JTable tbl = new JTable(model);
        tbl.setFont(f(Font.PLAIN, 12));
        tbl.setRowHeight(34);
        tbl.getTableHeader().setFont(f(Font.BOLD, 11));
        tbl.getTableHeader().setBackground(new Color(0xF8FAFC));
        tbl.getTableHeader().setPreferredSize(new Dimension(0, 32));

        JScrollPane sp = new JScrollPane(tbl);
        sp.setBorder(null);
        cardTable.add(sp, BorderLayout.CENTER);
        main.add(cardTable);

        return main;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3B. TRANG THÔNG TIN CÁ NHÂN (Hồ sơ trích xuất từ UserSession)
    // ─────────────────────────────────────────────────────────────────────────
    private JComponent buildProfilePage(NavigationItem item) {
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

        String ngaySinhStr = (nhanVien.getNgaySinh() != null) ? nhanVien.getNgaySinh().format(LOCAL_DATE_FMT) : "Chưa cập nhật";
        String ngayVaoLamStr = (nhanVien.getNgayVaoLam() != null) ? nhanVien.getNgayVaoLam().format(LOCAL_DATE_FMT) : "Chưa cập nhật";
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

    // ─────────────────────────────────────────────────────────────────────────
    // 3C. TRANG PHÂN HỆ NGHIỆP VỤ (Mở đúng trang và truy vấn dữ liệu từ DB)
    // ─────────────────────────────────────────────────────────────────────────
    private JComponent buildModulePage(NavigationItem item) {
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

        // Bảng dữ liệu thật từ cơ sở dữ liệu theo từng phân hệ
        JPanel cardTable = new JPanel(new BorderLayout(0, 0));
        cardTable.setBackground(C_WHITE);
        cardTable.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        cardTable.setMaximumSize(new Dimension(Integer.MAX_VALUE, 520));

        DefaultTableModel model = taoModelDuLieuTheoPhanHe(item.getId());
        JTable table = new JTable(model);
        cauHinhBang(table);

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(null);
        scrollTable.setPreferredSize(new Dimension(1000, 460));
        cardTable.add(scrollTable, BorderLayout.CENTER);

        pnl.add(cardTable);

        JScrollPane scroll = new JScrollPane(pnl);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }

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
                            String ngay = nvl != null ? nvl.toLocalDate().format(LOCAL_DATE_FMT) : "N/A";
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
    // XỬ LÝ ĐĂNG XUẤT (AuthController & UserSession)
    // ─────────────────────────────────────────────────────────────────────────
    private void xuLyDangXuat() {
        authController.dangXuat(this);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGO LOADING & FALLBACK
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
            java.net.URL url = MainFrame.class.getResource("/carzone_logo.png");
            if (url == null) {
                url = MainFrame.class.getResource("carzone_logo.png");
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
    // CUSTOM COMPONENTS PHỤ TRỢ (Bo góc & responsive)
    // ─────────────────────────────────────────────────────────────────────────
    public static class RoundedButton extends JButton {
        private final int arc;
        private final Color bgNormal;
        private final Color bgHover;
        private final Color borderClr;

        public RoundedButton(String text, int arc, Color bgNormal, Color bgHover, Color borderClr) {
            super(text);
            this.arc = arc;
            this.bgNormal = bgNormal;
            this.bgHover = bgHover;
            this.borderClr = borderClr;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = getModel().isRollover() ? bgHover : bgNormal;
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            if (borderClr != null) {
                g2.setColor(borderClr);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class HeaderSearchBox extends JPanel {
        private final JTextField txtInput;

        public HeaderSearchBox(String placeholder) {
            setLayout(new BorderLayout(10, 0));
            setOpaque(false);
            setPreferredSize(new Dimension(380, 42));
            setMinimumSize(new Dimension(140, 42));
            setMaximumSize(new Dimension(480, 42));
            setBorder(new EmptyBorder(2, 14, 2, 14));

            JLabel lblSearchIcon = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(C_TEXT_MUTED);
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawOval(2, 2, 11, 11);
                    g2.drawLine(10, 10, 15, 15);
                    g2.dispose();
                }

                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(18, 18);
                }
            };
            add(lblSearchIcon, BorderLayout.WEST);

            txtInput = new JTextField();
            txtInput.putClientProperty("JTextField.placeholderText", placeholder);
            txtInput.setFont(f(Font.PLAIN, 12));
            txtInput.setForeground(C_TEXT_HEAD);
            txtInput.setBorder(null);
            txtInput.setOpaque(false);
            add(txtInput, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = getHeight();
            g2.setColor(new Color(0xF8FAFC));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.setColor(C_INPUT_STROKE);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class ScrollableContentPanel extends JPanel implements Scrollable {
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
}
