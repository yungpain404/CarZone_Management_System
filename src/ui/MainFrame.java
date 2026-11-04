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
import javax.swing.table.TableRowSorter;
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

        // Icon ứng dụng cho Title bar và Windows Taskbar
        ImageIcon appIcon32 = loadSquareAppIcon(32);
        ImageIcon appIcon64 = loadSquareAppIcon(64);
        ImageIcon appIcon16 = loadSquareAppIcon(16);
        if (appIcon32 != null) {
            setIconImage(appIcon32.getImage());
            if (appIcon16 != null && appIcon64 != null) {
                setIconImages(List.of(appIcon16.getImage(), appIcon32.getImage(), appIcon64.getImage()));
            }
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

        private static final Map<String, Image> CACHE_ICON_SIDEBAR = new HashMap<>();

    /** Đọc icon từ classpath (/img/ten_file.png hoặc /ten_file.png) hoặc file img/ten_file.png */
    private static Image docIconSidebar(String fileName) {
        if (CACHE_ICON_SIDEBAR.containsKey(fileName)) return CACHE_ICON_SIDEBAR.get(fileName);
        Image img = null;
        try {
            URL url = MainFrame.class.getResource("/" + fileName);
            if (url == null) url = MainFrame.class.getResource("/img/" + fileName);
            if (url == null) url = MainFrame.class.getResource("/resources/images/" + fileName);
            if (url != null) {
                img = ImageIO.read(url);
            } else {
                File f = new File("img/" + fileName);
                if (!f.exists()) f = new File("src/resources/images/" + fileName);
                if (f.exists()) img = ImageIO.read(f);
            }
        } catch (Exception ignored) {
        }
        CACHE_ICON_SIDEBAR.put(fileName, img);
        return img;
    }

    /** Ánh xạ navId → file icon màu theo chuẩn Figma (img/nav_*.png) */
    private static String iconTheoNavId(String navId) {
        return switch (navId) {
            case "NAV_DASHBOARD"                                -> "nav_dashboard.png";
            case "NAV_THONG_KE", "NAV_THONG_KE_TIEP_NHAN"       -> "nav_thong_ke.png";
            case "NAV_QUAN_LY_DICH_VU", "NAV_CAP_NHAT_SUA_CHUA" -> "nav_dich_vu.png";
            case "NAV_LINH_KIEN_KHO"                            -> "nav_kho.png";
            case "NAV_YC_LINH_KIEN"                             -> "nav_yeu_cau.png";
            case "NAV_KHUYEN_MAI", "NAV_DOI_TRA"               -> "nav_khuyen_mai.png";
            case "NAV_NHAN_VIEN", "NAV_KHACH_HANG_XE"           -> "nav_nhan_vien.png";
            case "NAV_TAI_KHOAN"                                -> "nav_tai_khoan.png";
            case "NAV_TRA_CUU"                                  -> "nav_tra_cuu.png";
            case "NAV_THONG_TIN_CA_NHAN"                        -> "nav_ho_so.png";
            case "NAV_PHIEU_TIEP_NHAN", "NAV_PHIEU_PHAN_CONG"   -> "nav_yeu_cau.png";
            case "NAV_HOA_DON"                                  -> "nav_thong_ke.png";
            default -> null;
        };
    }

    private JLabel taoIconSidebar(NavigationItem item) {
        String fileName = iconTheoNavId(item.getId());
        Image raw = fileName != null ? docIconSidebar(fileName) : null;

        if (raw != null) {
            Image scaled = raw.getScaledInstance(22, 22, Image.SCALE_SMOOTH);
            JLabel lbl = new JLabel(new ImageIcon(scaled));
            lbl.setPreferredSize(new Dimension(28, 24));
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setOpaque(false);
            return lbl;
        }

        // Fallback: emoji
        JLabel lbl = new JLabel(item.getIcon(), SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        lbl.setPreferredSize(new Dimension(28, 24));
        lbl.setOpaque(false);
        return lbl;
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

        btn.add(taoIconSidebar(item), BorderLayout.WEST);

        boolean isInitialActive = item.getId().equals(activeNavId);
        JLabel lblText = new JLabel(htmlText);
        lblText.putClientProperty("navText", Boolean.TRUE);
        lblText.setFont(f(isInitialActive ? Font.BOLD : Font.PLAIN, 12));
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
                if (c instanceof JLabel lbl && Boolean.TRUE.equals(lbl.getClientProperty("navText"))) {
                    lbl.setFont(f(isCur ? Font.BOLD : Font.PLAIN, 12));
                    lbl.setForeground(isCur ? C_PRIMARY : C_TEXT_MAIN);
                }
            }
            b.repaint();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. CONTENTPANEL: Nạp các Page tương ứng với Navigation theo Role
    // ─────────────────────────────────────────────────────────────────────────
    public void moLapPhieuTiepNhanVoiXe(String maKH, String bienSoXe) {
        moPageTheoNavigation("NAV_PHIEU_TIEP_NHAN");
        if (pnlContentPanel != null) {
            for (Component c : pnlContentPanel.getComponents()) {
                if (c instanceof LapPhieuTiepNhanUI ui) {
                    ui.thietLapKhachHangVaXe(maKH, bienSoXe);
                    break;
                }
            }
        }
    }

    private void napCacPageTheoRole() {
        List<NavigationItem> items = authController.layDanhSachNavigationTheoRole(nhanVien.getVaiTro());

        for (NavigationItem item : items) {
            if ("NAV_DASHBOARD".equals(item.getId())) {
                pnlContentPanel.add(buildRoleDashboardPage(), item.getId());
            } else if ("NAV_THONG_TIN_CA_NHAN".equals(item.getId())) {
                pnlContentPanel.add(buildProfilePage(item), item.getId());
            } else if ("NAV_KHACH_HANG_XE".equals(item.getId())) {
                pnlContentPanel.add(new QuanLyKhachHangVaXeUI(), item.getId());
            } else if ("NAV_PHIEU_TIEP_NHAN".equals(item.getId())) {
                pnlContentPanel.add(new LapPhieuTiepNhanUI(), item.getId());
            } else if ("NAV_HOA_DON".equals(item.getId())) {
                pnlContentPanel.add(new LapHoaDonThanhToanUI(), item.getId());
            } else if ("NAV_DOI_TRA".equals(item.getId())) {
                pnlContentPanel.add(new TaoPhieuDoiTraUI(), item.getId());
            } else if ("NAV_TRA_CUU".equals(item.getId())) {
                pnlContentPanel.add(new TraCuuUI(), item.getId());
            } else if ("NAV_THONG_KE_TIEP_NHAN".equals(item.getId())) {
                pnlContentPanel.add(new ThongKeTiepNhanUI(), item.getId());
            } else {
                pnlContentPanel.add(buildModulePage(item), item.getId());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

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

    private JPanel createWhiteCardPanel(int arc) {
        JPanel pnl = new JPanel() {
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
        pnl.setOpaque(false);
        return pnl;
    }

    private static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setOpaque(true);

            String valStr = value != null ? value.toString() : "";
            String txt = valStr;
            Color fg = C_TEXT_MAIN;
            Color bg = new Color(0xF1F5F9);

            if ("CHO_SUA".equals(valStr) || "Chờ sửa".equals(valStr)) {
                txt = "Chờ sửa";
                fg = new Color(0xEA580C);
                bg = new Color(0xFFEDD5);
            } else if ("DANG_SUA".equals(valStr) || "Đang sửa".equals(valStr)) {
                txt = "Đang sửa";
                fg = new Color(0x2563EB);
                bg = new Color(0xDBEAFE);
            } else if ("CHO_LINH_KIEN".equals(valStr) || "Chờ linh kiện".equals(valStr)) {
                txt = "Chờ linh kiện";
                fg = new Color(0xD97706);
                bg = new Color(0xFEF3C7);
            } else if ("HOAN_TAT".equals(valStr) || "Hoàn tất".equals(valStr)) {
                txt = "Hoàn tất";
                fg = new Color(0x059669);
                bg = new Color(0xECFDF5);
            }

            lbl.setText(txt);
            lbl.setFont(f(Font.BOLD, 11));
            lbl.setForeground(fg);
            lbl.setBackground(bg);
            lbl.setBorder(new EmptyBorder(2, 6, 2, 6));

            return lbl;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DASHBOARD TIẾP NHẬN VIÊN (ĐÃ NÂNG CẤP: ACTION-DRIVEN & VISUAL CHARTS)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTiepNhanVienDashboardView() {
        TiepNhanVienDashboardData data = dashboardController.layDataTiepNhanVien();

        ScrollableContentPanel main = new ScrollableContentPanel();
        main.setBorder(new EmptyBorder(16, 24, 24, 24));

        // ── 1. Header ca làm việc & Quick Actions ───────────────────────────
        JPanel pnlTitle = new JPanel(new BorderLayout(12, 0));
        pnlTitle.setOpaque(false);
        pnlTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel lblTitle = new JLabel("Bảng điều hành Tiếp nhận viên");
        lblTitle.setFont(f(Font.BOLD, 22));
        lblTitle.setForeground(C_TEXT_HEAD);
        titleBlock.add(lblTitle);

        String ngayHienTai = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        JLabel lblSubTitle = new JLabel("Ca trực hôm nay: " + ngayHienTai + " · Giám sát xe xưởng, điều phối KTV và hàng đợi xử lý");
        lblSubTitle.setFont(f(Font.PLAIN, 12));
        lblSubTitle.setForeground(C_TEXT_SUBTLE);
        titleBlock.add(lblSubTitle);
        pnlTitle.add(titleBlock, BorderLayout.WEST);

        // Nút Quick Actions
        JPanel pnlQuickActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlQuickActions.setOpaque(false);

        JButton btnAddPhieu = new RoundedButton("+ Tiếp nhận xe (F2)", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
        btnAddPhieu.setForeground(C_WHITE);
        btnAddPhieu.setFont(f(Font.BOLD, 12));
        btnAddPhieu.setPreferredSize(new Dimension(160, 34));
        btnAddPhieu.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnAddPhieu.addActionListener(e -> moPageTheoNavigation("NAV_PHIEU_TIEP_NHAN"));
        pnlQuickActions.add(btnAddPhieu);

        JButton btnAddHoaDon = new RoundedButton("💵 Thu tiền (F3)", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnAddHoaDon.setForeground(C_TEXT_MAIN);
        btnAddHoaDon.setFont(f(Font.BOLD, 12));
        btnAddHoaDon.setPreferredSize(new Dimension(130, 34));
        btnAddHoaDon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnAddHoaDon.addActionListener(e -> moPageTheoNavigation("NAV_HOA_DON"));
        pnlQuickActions.add(btnAddHoaDon);

        JButton btnDoiTra = new RoundedButton("🔄 Đổi trả (F4)", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnDoiTra.setForeground(C_TEXT_MAIN);
        btnDoiTra.setFont(f(Font.BOLD, 12));
        btnDoiTra.setPreferredSize(new Dimension(120, 34));
        btnDoiTra.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnDoiTra.addActionListener(e -> moPageTheoNavigation("NAV_DOI_TRA"));
        pnlQuickActions.add(btnDoiTra);

        pnlTitle.add(pnlQuickActions, BorderLayout.EAST);
        main.add(pnlTitle);

        main.add(Box.createVerticalStrut(16));

        // ── 2. Hàng 4 Thẻ KPI Tức thời trong ngày ───────────────────────────
        JPanel pnlStats = new JPanel(new GridLayout(1, 4, 14, 0));
        pnlStats.setOpaque(false);
        pnlStats.setPreferredSize(new Dimension(800, 130));
        pnlStats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 135));

        // Card 1: Tiếp nhận hôm nay (Primary Blue)
        pnlStats.add(createClickableStatCard(
                String.format("%02d", data.tiepNhanHomNay),
                "Tiếp nhận hôm nay",
                "Lượt xe vào xưởng hôm nay",
                "Lập phiếu tiếp nhận mới ↗",
                "🚗",
                C_PRIMARY, C_PRIMARY, C_WHITE, C_WHITE, new Color(0xDBEAFE), C_WHITE,
                () -> moPageTheoNavigation("NAV_PHIEU_TIEP_NHAN")
        ));

        // Card 2: Cần phân công ngay (Nếu > 0 thì cảnh báo đỏ)
        boolean coChuaPhanCong = data.chuaPhanCongKTV > 0;
        pnlStats.add(createClickableStatCard(
                String.format("%02d", data.chuaPhanCongKTV),
                "Cần phân công KTV",
                coChuaPhanCong ? "Phiếu chờ chỉ định thợ!" : "Đã phân công đầy đủ",
                coChuaPhanCong ? "Phân công KTV ngay ↗" : "Xem danh sách xe ↗",
                coChuaPhanCong ? "⚠️" : "✓",
                coChuaPhanCong ? new Color(0xFEF2F2) : C_WHITE,
                coChuaPhanCong ? new Color(0xFCA5A5) : C_BORDER,
                coChuaPhanCong ? C_DANGER : C_TEXT_HEAD,
                coChuaPhanCong ? C_DANGER : C_TEXT_HEAD,
                coChuaPhanCong ? new Color(0xB91C1C) : C_TEXT_SUBTLE,
                coChuaPhanCong ? C_DANGER : C_PRIMARY,
                () -> moPageTheoNavigation("NAV_PHIEU_TIEP_NHAN")
        ));

        // Card 3: Xe đang sửa chữa tại xưởng
        pnlStats.add(createClickableStatCard(
                String.format("%02d", data.phieuDangSua),
                "Xe đang sửa chữa",
                "Đang tiến hành tại xưởng",
                "Xem tiến độ sửa chữa ↗",
                "🔧",
                C_WHITE, C_BORDER, C_TEXT_HEAD, C_TEXT_HEAD, C_TEXT_SUBTLE, C_PRIMARY,
                () -> moPageTheoNavigation("NAV_PHIEU_TIEP_NHAN")
        ));

        // Card 4: Sẵn sàng giao xe & chờ thu tiền (Xanh lá)
        boolean coChoThu = data.phieuChoThuTien > 0;
        pnlStats.add(createClickableStatCard(
                String.format("%02d", data.phieuChoThuTien),
                "Chờ thanh toán & giao xe",
                coChoThu ? "Sẵn sàng lập hóa đơn thu tiền" : "Không có xe tồn thu",
                "Lập hóa đơn thanh toán ↗",
                "💵",
                coChoThu ? new Color(0xECFDF5) : C_WHITE,
                coChoThu ? new Color(0xA7F3D0) : C_BORDER,
                coChoThu ? new Color(0x059669) : C_TEXT_HEAD,
                coChoThu ? new Color(0x059669) : C_TEXT_HEAD,
                coChoThu ? new Color(0x047857) : C_TEXT_SUBTLE,
                coChoThu ? new Color(0x059669) : C_PRIMARY,
                () -> moPageTheoNavigation("NAV_HOA_DON")
        ));

        main.add(pnlStats);

        main.add(Box.createVerticalStrut(18));

        // ── 3. Hàng 2 Biểu đồ Trực quan số liệu (Charts) ───────────────────
        JPanel pnlChartsRow = new JPanel(new GridLayout(1, 2, 14, 0));
        pnlChartsRow.setOpaque(false);
        pnlChartsRow.setPreferredSize(new Dimension(800, 220));
        pnlChartsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));

        // Chart 1: Trực quan trạng thái xe xưởng
        pnlChartsRow.add(buildTnvStatusProgressPanel(data));

        // Chart 2: Tải công việc Kỹ thuật viên
        pnlChartsRow.add(buildTnvWorkloadBarChart(data.taiCongViecKTV));

        main.add(pnlChartsRow);

        main.add(Box.createVerticalStrut(18));

        // ── 4. Action Center: Hàng đợi công việc cần xử lý ngay ────────────
        JPanel pnlActionRow = new JPanel(new GridBagLayout());
        pnlActionRow.setOpaque(false);
        pnlActionRow.setPreferredSize(new Dimension(800, 240));
        pnlActionRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // Card Trái: Xe chờ giao & thanh toán (60%)
        gbc.gridx = 0;
        gbc.weightx = 0.60;
        gbc.insets = new Insets(0, 0, 0, 14);
        pnlActionRow.add(buildTnvXeChoGiaoCard(data), gbc);

        // Card Phải: Cảnh báo & hàng đợi cần can thiệp (40%)
        gbc.gridx = 1;
        gbc.weightx = 0.40;
        gbc.insets = new Insets(0, 0, 0, 0);
        pnlActionRow.add(buildTnvCanhBaoCard(data), gbc);

        main.add(pnlActionRow);

        main.add(Box.createVerticalStrut(18));

        // ── 5. Bảng tổng hợp xe tại Gara hôm nay ───────────────────────────
        main.add(buildTnvLiveXeTable(data));

        return main;
    }

    /**
     * Helper tạo Card KPI nhấp chuột điều hướng trong MainFrame.
     */
    private JPanel createClickableStatCard(String number, String title, String subtitle, String linkText, String icon,
                                          Color bg, Color border, Color textNum, Color textTitle, Color textSub, Color textLink,
                                          Runnable onClick) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.setColor(border);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(0, 4));
        card.setBorder(new EmptyBorder(12, 14, 10, 14));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (onClick != null) onClick.run();
            }
        });

        // Header
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel lblNum = new JLabel(number);
        lblNum.setFont(f(Font.BOLD, 26));
        lblNum.setForeground(textNum);
        top.add(lblNum, BorderLayout.WEST);

        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        top.add(lblIcon, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        // Center
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(f(Font.BOLD, 13));
        lblTitle.setForeground(textTitle);
        center.add(lblTitle);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(f(Font.PLAIN, 11));
        lblSub.setForeground(textSub);
        center.add(lblSub);
        card.add(center, BorderLayout.CENTER);

        // Link
        JLabel lblLink = new JLabel(linkText);
        lblLink.setFont(f(Font.BOLD, 11));
        lblLink.setForeground(textLink);
        card.add(lblLink, BorderLayout.SOUTH);

        return card;
    }

    /**
     * Biểu đồ 1 (TNV): Tình trạng xe tại xưởng.
     */
    private JPanel buildTnvStatusProgressPanel(TiepNhanVienDashboardData data) {
        JPanel card = createWhiteCardPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(14, 16, 14, 16));

        int choSua = data.phanBoTrangThai.getOrDefault("Chờ sửa chữa", 0);
        int dangSua = data.phanBoTrangThai.getOrDefault("Đang sửa chữa", 0);
        int choLK = data.phanBoTrangThai.getOrDefault("Chờ linh kiện", 0);
        int choThu = data.phanBoTrangThai.getOrDefault("Chờ thanh toán", 0);
        int tongXe = choSua + dangSua + choLK + choThu;

        JPanel pnlHead = new JPanel(new BorderLayout());
        pnlHead.setOpaque(false);
        JLabel lblTitle = new JLabel("Tình trạng xe tại xưởng");
        lblTitle.setFont(f(Font.BOLD, 14));
        lblTitle.setForeground(C_TEXT_HEAD);
        pnlHead.add(lblTitle, BorderLayout.WEST);

        JLabel lblBadge = new JLabel(tongXe + " xe có mặt");
        lblBadge.setFont(f(Font.BOLD, 11));
        lblBadge.setForeground(C_PRIMARY);
        lblBadge.setBackground(C_PRIMARY_BG);
        lblBadge.setOpaque(true);
        lblBadge.setBorder(new EmptyBorder(2, 8, 2, 8));
        pnlHead.add(lblBadge, BorderLayout.EAST);
        card.add(pnlHead, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                if (tongXe == 0) {
                    g2.setColor(new Color(0xE2E8F0));
                    g2.fillRoundRect(0, 0, w, h, 8, 8);
                    g2.dispose();
                    return;
                }

                g2.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, w, h, 8, 8));
                int wChoSua = (int) Math.round((double) choSua * w / tongXe);
                int wDangSua = (int) Math.round((double) dangSua * w / tongXe);
                int wChoLK = (int) Math.round((double) choLK * w / tongXe);
                int wChoThu = w - (wChoSua + wDangSua + wChoLK);

                int curX = 0;
                if (wChoSua > 0) {
                    g2.setColor(new Color(0xEA580C));
                    g2.fillRect(curX, 0, wChoSua, h);
                    curX += wChoSua;
                }
                if (wDangSua > 0) {
                    g2.setColor(new Color(0x2563EB));
                    g2.fillRect(curX, 0, wDangSua, h);
                    curX += wDangSua;
                }
                if (wChoLK > 0) {
                    g2.setColor(new Color(0xD97706));
                    g2.fillRect(curX, 0, wChoLK, h);
                    curX += wChoLK;
                }
                if (wChoThu > 0) {
                    g2.setColor(new Color(0x059669));
                    g2.fillRect(curX, 0, wChoThu, h);
                }
                g2.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(0, 16));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        bar.setOpaque(false);
        body.add(bar);

        body.add(Box.createVerticalStrut(14));

        JPanel pnlBoxes = new JPanel(new GridLayout(2, 2, 8, 8));
        pnlBoxes.setOpaque(false);
        pnlBoxes.add(createTnvMiniBox("Chờ sửa chữa", choSua, tongXe, new Color(0xEA580C), new Color(0xFFEDD5)));
        pnlBoxes.add(createTnvMiniBox("Đang sửa chữa", dangSua, tongXe, new Color(0x2563EB), new Color(0xDBEAFE)));
        pnlBoxes.add(createTnvMiniBox("Chờ linh kiện", choLK, tongXe, new Color(0xD97706), new Color(0xFEF3C7)));
        pnlBoxes.add(createTnvMiniBox("Chờ thu tiền", choThu, tongXe, new Color(0x059669), new Color(0xD1FAE5)));

        body.add(pnlBoxes);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel createTnvMiniBox(String label, int val, int total, Color dotColor, Color bgColor) {
        JPanel box = new JPanel(new BorderLayout(6, 0));
        box.setBackground(bgColor);
        box.setBorder(new EmptyBorder(6, 10, 6, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        JLabel dot = new JLabel("●");
        dot.setFont(f(Font.BOLD, 12));
        dot.setForeground(dotColor);
        left.add(dot);

        JLabel lbl = new JLabel(label);
        lbl.setFont(f(Font.PLAIN, 12));
        lbl.setForeground(C_TEXT_MAIN);
        left.add(lbl);
        box.add(left, BorderLayout.WEST);

        int pct = total > 0 ? (int) Math.round((double) val * 100.0 / total) : 0;
        JLabel lblVal = new JLabel(val + " (" + pct + "%)");
        lblVal.setFont(f(Font.BOLD, 12));
        lblVal.setForeground(dotColor.darker());
        box.add(lblVal, BorderLayout.EAST);

        return box;
    }

    /**
     * Biểu đồ 2 (TNV): Tải công việc Kỹ thuật viên.
     */
    private JPanel buildTnvWorkloadBarChart(Map<String, Integer> taiCongViecKTV) {
        JPanel card = createWhiteCardPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel pnlHead = new JPanel(new BorderLayout());
        pnlHead.setOpaque(false);
        JLabel lblTitle = new JLabel("Tải công việc Kỹ thuật viên");
        lblTitle.setFont(f(Font.BOLD, 14));
        lblTitle.setForeground(C_TEXT_HEAD);
        pnlHead.add(lblTitle, BorderLayout.WEST);

        JLabel lblSub = new JLabel("Chọn KTV ít việc để phân công");
        lblSub.setFont(f(Font.PLAIN, 11));
        lblSub.setForeground(C_TEXT_SUBTLE);
        pnlHead.add(lblSub, BorderLayout.EAST);
        card.add(pnlHead, BorderLayout.NORTH);

        JPanel pnlList = new JPanel();
        pnlList.setLayout(new BoxLayout(pnlList, BoxLayout.Y_AXIS));
        pnlList.setOpaque(false);

        if (taiCongViecKTV == null || taiCongViecKTV.isEmpty()) {
            JLabel lblEmpty = new JLabel("Chưa có thông tin kỹ thuật viên");
            lblEmpty.setFont(f(Font.ITALIC, 12));
            lblEmpty.setForeground(C_TEXT_MUTED);
            pnlList.add(lblEmpty);
        } else {
            int maxJobs = 1;
            for (int v : taiCongViecKTV.values()) if (v > maxJobs) maxJobs = v;

            for (Map.Entry<String, Integer> e : taiCongViecKTV.entrySet()) {
                String ktvName = e.getKey();
                int jobs = e.getValue();

                JPanel row = new JPanel(new BorderLayout(8, 0));
                row.setOpaque(false);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

                JLabel lblName = new JLabel(ktvName);
                lblName.setFont(f(Font.PLAIN, 12));
                lblName.setForeground(C_TEXT_MAIN);
                lblName.setPreferredSize(new Dimension(130, 24));
                row.add(lblName, BorderLayout.WEST);

                final int finalJobs = jobs;
                final int finalMax = maxJobs;
                JPanel barPanel = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        int w = getWidth();
                        int h = 10;
                        int y = (getHeight() - h) / 2;

                        g2.setColor(new Color(0xF1F5F9));
                        g2.fillRoundRect(0, y, w, h, 6, 6);

                        if (finalJobs > 0) {
                            int barW = Math.max(12, (int) Math.round((double) finalJobs * w / Math.max(finalMax, 4)));
                            Color barCol = finalJobs >= 3 ? new Color(0xDC2626) : (finalJobs >= 2 ? new Color(0xD97706) : C_PRIMARY);
                            g2.setColor(barCol);
                            g2.fillRoundRect(0, y, barW, h, 6, 6);
                        }
                        g2.dispose();
                    }
                };
                barPanel.setOpaque(false);
                row.add(barPanel, BorderLayout.CENTER);

                JPanel badge = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                badge.setOpaque(false);
                badge.setPreferredSize(new Dimension(95, 24));

                JLabel lblStatus = new JLabel(jobs == 0 ? "✓ Sẵn sàng" : jobs + " xe");
                lblStatus.setFont(f(Font.BOLD, 11));
                lblStatus.setForeground(jobs == 0 ? new Color(0x059669) : (jobs >= 3 ? C_DANGER : C_TEXT_HEAD));
                badge.add(lblStatus);
                row.add(badge, BorderLayout.EAST);

                pnlList.add(row);
                pnlList.add(Box.createVerticalStrut(4));
            }
        }

        JScrollPane scroll = new JScrollPane(pnlList);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    /**
     * Action Card Trái (TNV): Xe đã sửa xong – Chờ thu tiền & giao xe.
     */
    private JPanel buildTnvXeChoGiaoCard(TiepNhanVienDashboardData data) {
        JPanel card = createWhiteCardPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel pnlHead = new JPanel(new BorderLayout());
        pnlHead.setOpaque(false);

        JPanel pnlTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlTitle.setOpaque(false);
        JLabel lblTitle = new JLabel("Xe đã sửa xong – Chờ thu tiền & giao xe");
        lblTitle.setFont(f(Font.BOLD, 14));
        lblTitle.setForeground(C_TEXT_HEAD);
        pnlTitle.add(lblTitle);

        JLabel lblBadge = new JLabel(data.danhSachChoGiao.size() + " xe");
        lblBadge.setFont(f(Font.BOLD, 11));
        lblBadge.setForeground(new Color(0x059669));
        lblBadge.setBackground(new Color(0xECFDF5));
        lblBadge.setOpaque(true);
        lblBadge.setBorder(new EmptyBorder(2, 6, 2, 6));
        pnlTitle.add(lblBadge);
        pnlHead.add(pnlTitle, BorderLayout.WEST);

        JLabel lblOpen = new JLabel("Sang Lập hóa đơn ↗");
        lblOpen.setFont(f(Font.BOLD, 11));
        lblOpen.setForeground(C_PRIMARY);
        lblOpen.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblOpen.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                moPageTheoNavigation("NAV_HOA_DON");
            }
        });
        pnlHead.add(lblOpen, BorderLayout.EAST);
        card.add(pnlHead, BorderLayout.NORTH);

        String[] cols = {"MÃ PHIẾU", "BIỂN SỐ XE", "KHÁCH HÀNG", "KTV CHÍNH", "TẠM TÍNH", "THAO TÁC"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        for (XeChoGiaoRow r : data.danhSachChoGiao) {
            model.addRow(new Object[]{
                    r.maPhieu,
                    r.bienSo,
                    r.khachHang,
                    r.ktvChinh,
                    MONEY_FMT.format(r.tienTamTinh) + " đ",
                    "💵 Thu tiền ↗"
            });
        }

        JTable table = new JTable(model);
        table.setFont(f(Font.PLAIN, 12));
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.getTableHeader().setFont(f(Font.BOLD, 11));
        table.getTableHeader().setBackground(new Color(0xF8FAFC));
        table.getTableHeader().setPreferredSize(new Dimension(0, 32));

        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSel, boolean hasFoc, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, val, isSel, hasFoc, r, c);
                lbl.setFont(f(Font.BOLD, 11));
                lbl.setForeground(new Color(0x059669));
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                return lbl;
            }
        });

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    moPageTheoNavigation("NAV_HOA_DON");
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        scroll.getViewport().setBackground(C_WHITE);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    /**
     * Action Card Phải (TNV): Cảnh báo & Việc cần can thiệp.
     */
    private JPanel buildTnvCanhBaoCard(TiepNhanVienDashboardData data) {
        JPanel card = createWhiteCardPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel pnlHead = new JPanel(new BorderLayout());
        pnlHead.setOpaque(false);

        JPanel pnlTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlTitle.setOpaque(false);
        JLabel lblTitle = new JLabel("Cảnh báo & Việc cần can thiệp");
        lblTitle.setFont(f(Font.BOLD, 14));
        lblTitle.setForeground(C_TEXT_HEAD);
        pnlTitle.add(lblTitle);

        int count = data.danhSachCanhBao != null ? data.danhSachCanhBao.size() : 0;
        JLabel lblBadge = new JLabel(count + " mục");
        lblBadge.setFont(f(Font.BOLD, 11));
        lblBadge.setForeground(count > 0 ? C_DANGER : new Color(0x059669));
        lblBadge.setBackground(count > 0 ? new Color(0xFEF2F2) : new Color(0xECFDF5));
        lblBadge.setOpaque(true);
        lblBadge.setBorder(new EmptyBorder(2, 6, 2, 6));
        pnlTitle.add(lblBadge);
        pnlHead.add(pnlTitle, BorderLayout.WEST);
        card.add(pnlHead, BorderLayout.NORTH);

        JPanel pnlList = new JPanel();
        pnlList.setLayout(new BoxLayout(pnlList, BoxLayout.Y_AXIS));
        pnlList.setOpaque(false);

        if (data.danhSachCanhBao == null || data.danhSachCanhBao.isEmpty()) {
            JPanel pnlOk = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 40));
            pnlOk.setOpaque(false);
            JLabel lblOk = new JLabel("✓ Không có cảnh báo tồn đọng. Mọi việc đang trôi chảy!");
            lblOk.setFont(f(Font.PLAIN, 12));
            lblOk.setForeground(new Color(0x059669));
            pnlOk.add(lblOk);
            pnlList.add(pnlOk);
        } else {
            for (CanhBaoTNVRow cb : data.danhSachCanhBao) {
                boolean isDanger = "DANGER".equals(cb.mucDo);
                JPanel item = new JPanel(new BorderLayout(8, 0));
                item.setBackground(isDanger ? new Color(0xFEF2F2) : new Color(0xFFFBEB));
                item.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(isDanger ? new Color(0xFCA5A5) : new Color(0xFDE68A), 1),
                        new EmptyBorder(6, 10, 6, 10)
                ));
                item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

                JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                left.setOpaque(false);
                JLabel icon = new JLabel(isDanger ? "⚠️" : "⏳");
                icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
                left.add(icon);

                JLabel bs = new JLabel(cb.bienSo);
                bs.setFont(f(Font.BOLD, 12));
                bs.setForeground(isDanger ? C_DANGER : new Color(0xB45309));
                left.add(bs);

                JLabel note = new JLabel("– " + cb.thongTin);
                note.setFont(f(Font.PLAIN, 11));
                note.setForeground(C_TEXT_MAIN);
                left.add(note);
                item.add(left, BorderLayout.CENTER);

                JLabel btnAction = new JLabel("Xử lý ↗");
                btnAction.setFont(f(Font.BOLD, 11));
                btnAction.setForeground(C_PRIMARY);
                btnAction.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btnAction.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        moPageTheoNavigation("NAV_PHIEU_TIEP_NHAN");
                    }
                });
                item.add(btnAction, BorderLayout.EAST);

                pnlList.add(item);
                pnlList.add(Box.createVerticalStrut(6));
            }
        }

        JScrollPane scroll = new JScrollPane(pnlList);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    /**
     * Bảng Live xe tại Gara hôm nay (MainFrame).
     */
    private JPanel buildTnvLiveXeTable(TiepNhanVienDashboardData data) {
        JPanel cardTable = createWhiteCardPanel(16);
        cardTable.setLayout(new BorderLayout(0, 10));
        cardTable.setBorder(new EmptyBorder(14, 16, 14, 16));
        cardTable.setMaximumSize(new Dimension(Integer.MAX_VALUE, 460));

        JPanel tblHead = new JPanel(new BorderLayout());
        tblHead.setOpaque(false);

        JLabel lblTblTitle = new JLabel("Danh sách xe đang tiếp nhận & xử lý tại xưởng");
        lblTblTitle.setFont(f(Font.BOLD, 15));
        lblTblTitle.setForeground(C_TEXT_HEAD);
        tblHead.add(lblTblTitle, BorderLayout.WEST);

        String[] cols = {"MÃ PHIẾU", "BIỂN SỐ XE", "KHÁCH HÀNG", "SỐ ĐIỆN THOẠI", "KTV PHỤ TRÁCH", "GIỜ TIẾP NHẬN", "TRẠNG THÁI"};
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
        table.setFont(f(Font.PLAIN, 12));
        table.setRowHeight(34);
        table.setShowGrid(false);
        table.getTableHeader().setFont(f(Font.BOLD, 11));
        table.getTableHeader().setBackground(new Color(0xF8FAFC));
        table.getTableHeader().setPreferredSize(new Dimension(0, 32));
        table.getColumnModel().getColumn(6).setCellRenderer(new StatusBadgeRenderer());

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JPanel pnlFilters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pnlFilters.setOpaque(false);

        String[] filterNames = {"Tất cả", "Chờ sửa", "Đang sửa", "Chờ linh kiện", "Hoàn tất"};
        for (String fn : filterNames) {
            JButton btnF = new JButton(fn);
            btnF.setFont(f(Font.PLAIN, 11));
            btnF.setBackground(C_WHITE);
            btnF.setForeground(C_TEXT_MAIN);
            btnF.setFocusPainted(false);
            btnF.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnF.addActionListener(e -> {
                if ("Tất cả".equals(fn)) {
                    sorter.setRowFilter(null);
                } else {
                    String query = switch (fn) {
                        case "Chờ sửa" -> "CHO_SUA";
                        case "Đang sửa" -> "DANG_SUA";
                        case "Chờ linh kiện" -> "CHO_LINH_KIEN";
                        case "Hoàn tất" -> "HOAN_TAT";
                        default -> fn;
                    };
                    sorter.setRowFilter(RowFilter.regexFilter(query, 6));
                }
            });
            pnlFilters.add(btnF);
        }

        tblHead.add(pnlFilters, BorderLayout.EAST);
        cardTable.add(tblHead, BorderLayout.NORTH);

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        scrollTable.getViewport().setBackground(C_WHITE);
        cardTable.add(scrollTable, BorderLayout.CENTER);

        return cardTable;
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
            java.net.URL url = MainFrame.class.getResource("/img/carzone_logo.png");
            if (url == null) url = MainFrame.class.getResource("/carzone_logo.png");
            if (url == null) url = MainFrame.class.getResource("/resources/images/carzone_logo.png");
            if (url != null) {
                rawLogoImage = javax.imageio.ImageIO.read(url);
            } else {
                java.io.File file = new java.io.File("img/carzone_logo.png");
                if (!file.exists()) file = new java.io.File("src/resources/images/carzone_logo.png");
                if (!file.exists()) file = new java.io.File("src/ui/carzone_logo.png");
                if (file.exists()) {
                    rawLogoImage = javax.imageio.ImageIO.read(file);
                }
            }
        } catch (Exception ignored) {
        }
        return rawLogoImage;
    }

    private static ImageIcon loadSquareAppIcon(int size) {
        try {
            Image raw = loadRawLogoImage();
            if (raw == null) return null;
            java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = bi.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            int iw = raw.getWidth(null);
            int ih = raw.getHeight(null);
            if (iw > 0 && ih > 0) {
                double scale = Math.min((double) size / iw, (double) size / ih);
                int sw = (int) Math.round(iw * scale);
                int sh = (int) Math.round(ih * scale);
                int sx = (size - sw) / 2;
                int sy = (size - sh) / 2;
                g2.drawImage(raw, sx, sy, sw, sh, null);
            }
            g2.dispose();
            return new ImageIcon(bi);
        } catch (Exception e) {
            return null;
        }
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
