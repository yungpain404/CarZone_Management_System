package ui;

import connectDB.DbUtil;
import backup.AppShutdownManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import controller.AuthController;
import controller.AuthController.NavigationItem;
import controller.DashboardController;
import controller.DashboardController.*;
import controller.TraCuuController;
import entity.NhanVien;
import entity.DichVu;
import entity.LinhKien;

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
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * MainFrame: Khung giao diện chính của toàn bộ hệ thống CarZone.
 * Tái hiện chuẩn mực 100% theo bản thiết kế Figma (tham chiếu image45.png).
 *
 * Cấu trúc 3 thành phần cốt lõi:
 * 1. HEADER (Ở TRÊN CÙNG - 70px):
 * - Logo CarZone + Tên thương hiệu + Khẩu hiệu gara.
 * - Thanh tìm kiếm thông minh bo tròn pill (Placeholder thay đổi linh hoạt theo
 * vai trò).
 * - Nút Help, Nút About.
 * - Cụm thông tin định danh người dùng: Avatar chữ cái đầu, Họ và tên, Vai trò
 * · Mã NV (Lấy từ Session).
 * - Nút Đăng xuất an toàn (kết nối trực tiếp với AuthController).
 *
 * 2. NAVIGATION (BÊN TRÁI - 240px):
 * - Menu thanh bên (Sidebar) cấp quyền tự động theo vai trò xác thực từ
 * UserSession:
 * + CHU_CUA_HANG: 10 chức năng (Dashboard, Doanh thu, Dịch vụ, Kho linh kiện,
 * YC linh kiện, Khuyến mãi, Nhân viên, Tài khoản, Tra cứu, Hồ sơ cá nhân).
 * + TIEP_NHAN_VIEN: 7 chức năng (Dashboard, Khách & xe, Lập phiếu sửa chữa, Hóa
 * đơn thanh toán, Đổi trả, Tra cứu, Hồ sơ cá nhân).
 * + KY_THUAT_VIEN: 6 chức năng (Dashboard, Phiếu được giao, Cập nhật sửa chữa,
 * YC linh kiện, Tra cứu, Hồ sơ cá nhân).
 * - Các nút bo góc 10px, hiệu ứng active nền xanh nhạt #EFF6FF viền #BFDBFE chữ
 * xanh #2563EB theo chuẩn image45.png.
 *
 * 3. CONTENTPANEL (Ở GIỮA - CARDLAYOUT):
 * - Tự động hoán đổi page hiển thị dựa trên navigation mà người dùng bấm chọn:
 * + Trang Dashboard: Tái hiện đầy đủ KPI card, khối công việc, cảnh báo tồn
 * kho, biểu đồ doanh thu (Chủ cửa hàng chuẩn image45.png).
 * + Các trang phân hệ chức năng: Mở đúng trang với tiêu đề, thanh toolbar tác
 * vụ và bảng dữ liệu thật từ DB.
 * + Trang Thông tin cá nhân: Hiển thị thẻ hồ sơ nhân sự trích xuất từ Session.
 */
public class MainFrame extends JFrame {

    // ── Bảng màu chuẩn tuyệt đối từ Figma (image45.png) ─────────────────────
    public static final Color C_BG = new Color(0xF8FAFC); // Nền app #F8FAFC
    public static final Color C_WHITE = Color.WHITE; // Nền card, header, sidebar
    public static final Color C_BORDER = new Color(0xE2E8F0); // Viền card #E2E8F0
    public static final Color C_INPUT_STROKE = new Color(0xCBD5E1); // Viền input #CBD5E1
    public static final Color C_TEXT_HEAD = new Color(0x0F172A); // Tiêu đề lớn #0F172A
    public static final Color C_TEXT_MAIN = new Color(0x334155); // Nhãn chính #334155
    public static final Color C_TEXT_SUBTLE = new Color(0x64748B); // Text phụ #64748B
    public static final Color C_TEXT_MUTED = new Color(0x94A3B8); // Muted text #94A3B8
    public static final Color C_PRIMARY = new Color(0x2563EB); // Xanh chủ đạo #2563EB
    public static final Color C_PRIMARY_HV = new Color(0x1D4ED8); // Hover nút chính #1D4ED8
    public static final Color C_PRIMARY_BG = new Color(0xEFF6FF); // Nền active / thẻ phụ #EFF6FF
    public static final Color C_PRIMARY_BR = new Color(0xBFDBFE); // Viền nút Help #BFDBFE
    public static final Color C_DANGER = new Color(0xDC2626); // Đỏ cảnh báo / Đăng xuất #DC2626
    public static final Color C_DANGER_BG = new Color(0xFEF2F2); // Nền đỏ cảnh báo #FEF2F2
    public static final Color C_WARN = new Color(0xD97706); // Cam / vàng #D97706
    public static final Color C_WARN_BG = new Color(0xFFFBEB); // Nền vàng nhạt
    public static final Color C_SUCCESS = new Color(0x059669); // Xanh lá #059669
    public static final Color C_SUCCESS_BG = new Color(0xECFDF5); // Nền xanh lá nhạt

    // ── Bảng màu khung Dashboard Chủ cửa hàng (Figma node 83:7716) ──────────
    public static final Color F_BG = new Color(0xF4F7FB); // Nền khung nội dung #F4F7FB
    public static final Color F_LINE = new Color(0xE3E9F2); // Viền card / header / sidebar #E3E9F2
    public static final Color F_TITLE = new Color(0x14233D); // Tiêu đề, số liệu đậm #14233D
    public static final Color F_DESC = new Color(0x7A889C); // Chữ mô tả phụ #7A889C
    public static final Color F_LABEL = new Color(0x40516A); // Nhãn bảng / mục lục #40516A
    public static final Color F_ACCENT_BG = new Color(0xEDF4FF); // Nền xanh nhạt (active, badge) #EDF4FF
    public static final Color F_DANGER = new Color(0xCA4850); // Đỏ cảnh báo #CA4850
    public static final Color F_DANGER_BG = new Color(0xFFF0F1); // Nền badge đỏ #FFF0F1
    public static final Color F_DEEP = new Color(0x183C78); // Số doanh thu đậm #183C78
    public static final Color F_BAR_SOFT = new Color(0xBDD5FE); // Cột biểu đồ nhạt #BDD5FE
    public static final Color F_TEAL = new Color(0x39B9D0); // Thanh cơ cấu: tiền mặt #39B9D0
    public static final Color F_ON_BLUE = new Color(0xDFEAFE); // Chữ phụ trên nền xanh #DFEAFE

    // ── Font chữ chuẩn Arimo / SansSerif ─────────────────────────────────────
    private static final String FONT_FAMILY = detectFont();

    private static String detectFont() {
        for (String f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (f.equalsIgnoreCase("Arimo"))
                return "Arimo";
        }
        return "SansSerif";
    }

    public static Font f(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }

    public static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###");
    public static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy");
    /**
     * Dùng riêng cho LocalDate (entity NhanVien.ngaySinh, ngayVaoLam).
     * SimpleDateFormat không nhận LocalDate.
     */
    public static final DateTimeFormatter LOCAL_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Thuộc tính điều khiển ───────────────────────────────────────────────
    private final NhanVien nhanVien;
    private final AuthController authController = new AuthController();
    private final DashboardController dashboardController = new DashboardController();
    private final TraCuuController traCuuController = new TraCuuController();

    private CardLayout contentCardLayout;
    private JPanel pnlContentPanel;
    private final Map<String, JButton> mapNavButtons = new HashMap<>();
    private String activeNavId = "NAV_DASHBOARD";
    private CapNhatPhieuSuaChuaUI capNhatPhieuUI;
    private XuLyYeuCauLinhKienUI xuLyYeuCauCCHUI;
    private QuanLyDichVuUI quanLyDichVuCCHUI;
    private QuanLyLinhKienUI quanLyLinhKienCCHUI;
    private QuanLyTaiKhoanUI quanLyTaiKhoanCCHUI;
    private TraCuuUI traCuuUI;

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
        setSize(1170, 800);
        setMinimumSize(new Dimension(1024, 700));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        getContentPane().setBackground(C_BG);

        // Icon ứng dụng
        ImageIcon appIcon = loadLogoImage(32, 32);
        if (appIcon != null) {
            setIconImage(appIcon.getImage());
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                AppShutdownManager.handleExit(MainFrame.this);
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
        header.setPreferredSize(new Dimension(1170, 64));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, F_LINE));

        // ── Trái: Logo CarZone + Tên thương hiệu + Khẩu hiệu ─────────────────
        JPanel pnlLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 14));
        pnlLeft.setOpaque(false);

        JComponent logoComp = createLogoComponent(95, 32);
        pnlLeft.add(logoComp);

        JPanel pnlBrand = new JPanel();
        pnlBrand.setLayout(new BoxLayout(pnlBrand, BoxLayout.Y_AXIS));
        pnlBrand.setOpaque(false);

        JLabel lblBrandName = new JLabel("CARZONE");
        lblBrandName.setFont(f(Font.BOLD, 16));
        lblBrandName.setForeground(F_TITLE);
        pnlBrand.add(lblBrandName);

        JLabel lblSub = new JLabel("Hệ thống quản lý gara ô tô");
        lblSub.setFont(f(Font.PLAIN, 9));
        lblSub.setForeground(F_DESC);
        pnlBrand.add(lblSub);

        pnlLeft.add(pnlBrand);
        header.add(pnlLeft, BorderLayout.WEST);

        // ── Phải: Search + Help/About + Định danh + Đăng xuất ────────────────
        // (Figma 83:7716 – cụm "Header actions" xếp cạnh nhau, gap 20px)
        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        pnlRight.setOpaque(false);

        String searchPlaceholder = switch (nhanVien.getVaiTro()) {
            case CHU_CUA_HANG -> "Tìm kiếm dịch vụ, linh kiện";
            case TIEP_NHAN_VIEN -> "Tìm khách hàng, xe, phiếu";
            case KY_THUAT_VIEN -> "Tìm phiếu, linh kiện";
        };

        searchBox = new HeaderSearchBox(searchPlaceholder);
        searchBox.setHandlers(this::hienThiGoiYTimKiem, this::chonGoiYTimKiem);
        pnlRight.add(searchBox);

        // ── Nhóm link nhanh Help / About (gap 4px) ───────────────────────────
        JPanel pnlLinks = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pnlLinks.setOpaque(false);

        // Nút Help – nền trắng bình thường (chỉ đổi nền xanh khi đang ở trang Help)
        JButton btnHelp = new RoundedButton("Help", 12, C_WHITE, new Color(0xF1F5F9), F_LINE);
        btnHelp.setIcon(new HelpIcon(C_TEXT_MAIN, 16));
        btnHelp.setIconTextGap(6);
        btnHelp.setHorizontalAlignment(SwingConstants.CENTER);
        btnHelp.setVerticalAlignment(SwingConstants.CENTER);
        btnHelp.setHorizontalTextPosition(SwingConstants.TRAILING);
        btnHelp.setVerticalTextPosition(SwingConstants.CENTER);
        btnHelp.setFont(f(Font.PLAIN, 13));
        btnHelp.setForeground(C_TEXT_MAIN);
        btnHelp.setMargin(new Insets(4, 14, 4, 14));
        btnHelp.addActionListener(e -> moPageHeader("PAGE_TRO_GIUP"));
        pnlLinks.add(btnHelp);

        JButton btnAbout = new RoundedButton("About", 12, C_WHITE, new Color(0xF1F5F9), F_LINE);
        btnAbout.setIcon(new InfoIcon(C_TEXT_MAIN, 16));
        btnAbout.setIconTextGap(6);
        btnAbout.setFont(f(Font.PLAIN, 13));
        btnAbout.setForeground(C_TEXT_MAIN);
        btnAbout.setMargin(new Insets(4, 14, 4, 14));
        btnAbout.addActionListener(e -> moPageHeader("PAGE_VE_CARZONE"));
        pnlLinks.add(btnAbout);

        pnlRight.add(pnlLinks);

        // ── Định danh chủ cửa hàng: Avatar tròn + Họ tên + Vai trò · Mã NV ───
        JPanel pnlIdentity = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        pnlIdentity.setOpaque(false);

        String initial = (nhanVien.getHoTen() != null && !nhanVien.getHoTen().isBlank())
                ? String.valueOf(nhanVien.getHoTen().trim().charAt(0)).toUpperCase()
                : "U";
        lblHeaderAvatar = new JLabel(initial, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(F_ACCENT_BG);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblHeaderAvatar.setPreferredSize(new Dimension(30, 30));
        lblHeaderAvatar.setFont(f(Font.BOLD, 13));
        lblHeaderAvatar.setForeground(C_PRIMARY);
        lblHeaderAvatar.setOpaque(false);
        pnlIdentity.add(lblHeaderAvatar);

        JPanel pnlUserText = new JPanel();
        pnlUserText.setLayout(new BoxLayout(pnlUserText, BoxLayout.Y_AXIS));
        pnlUserText.setOpaque(false);

        lblHeaderUserName = new JLabel(nhanVien.getHoTen());
        lblHeaderUserName.setFont(f(Font.BOLD, 13));
        lblHeaderUserName.setForeground(F_TITLE);
        pnlUserText.add(lblHeaderUserName);

        String roleStr = AuthController.layTenVaiTro(nhanVien.getVaiTro())
                + " · " + (nhanVien.getMaNV() != null ? nhanVien.getMaNV() : "");
        lblHeaderUserRole = new JLabel(roleStr);
        lblHeaderUserRole.setFont(f(Font.PLAIN, 11));
        lblHeaderUserRole.setForeground(F_DESC);
        pnlUserText.add(lblHeaderUserRole);

        pnlIdentity.add(pnlUserText);
        pnlRight.add(pnlIdentity);

        // ── Đăng xuất: vạch dọc phân cách + icon + nhãn đỏ #CA4850 ───────────
        JPanel pnlLogout = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 12));
        pnlLogout.setOpaque(false);
        pnlLogout.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, F_LINE),
                new EmptyBorder(0, 12, 0, 0)));

        JLabel lblLogoutIcon = new JLabel(new LogoutIcon(F_DANGER, 16));
        lblLogoutIcon.setPreferredSize(new Dimension(16, 16));
        pnlLogout.add(lblLogoutIcon);

        JLabel lblLogoutText = new JLabel("Đăng xuất");
        lblLogoutText.setFont(f(Font.BOLD, 12));
        lblLogoutText.setForeground(F_DANGER);
        pnlLogout.add(lblLogoutText);

        pnlLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pnlLogout.setToolTipText("Đăng xuất khỏi phiên làm việc");
        pnlLogout.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                xuLyDangXuat();
            }
        });
        pnlRight.add(pnlLogout);

        header.add(pnlRight, BorderLayout.EAST);
        return header;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. NAVIGATION (SIDEBAR 260px – Figma 83:7716, icon màu 30px, cao 56px)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(C_WHITE);
        sidebar.setPreferredSize(new Dimension(256, 800));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, F_LINE),
                new EmptyBorder(14, 10, 10, 10)));

        // Lấy danh sách chức năng CHÍNH XÁC theo vai trò trong UserSession
        List<NavigationItem> items = authController.layDanhSachNavigationTheoRole(nhanVien.getVaiTro());
        mapNavButtons.clear();

        for (NavigationItem item : items) {
            JButton btnItem = createSidebarButton(item);
            btnItem.setAlignmentX(Component.CENTER_ALIGNMENT);
            mapNavButtons.put(item.getId(), btnItem);
            sidebar.add(btnItem);
            sidebar.add(Box.createVerticalStrut(6));
        }

        // Khoảng trống co giãn giữ khoảng trắng dưới cùng như bản thiết kế
        sidebar.add(Box.createVerticalGlue());

        return sidebar;
    }

    /** Ánh xạ navId → file icon màu đã tải từ Figma (img/nav_*.png). */
    private static String iconTheoNavId(String navId) {
        return switch (navId) {
            case "NAV_DASHBOARD" -> "nav_dashboard.png";
            case "NAV_THONG_KE" -> "nav_thong_ke.png";
            case "NAV_PHIEU_PHAN_CONG" -> "nav_yeu_cau.png";
            case "NAV_CAP_NHAT_SUA_CHUA" -> "nav_dich_vu.png";
            case "NAV_QUAN_LY_DICH_VU" -> "nav_dich_vu.png";
            case "NAV_LINH_KIEN_KHO" -> "nav_kho.png";
            case "NAV_YC_LINH_KIEN" -> "nav_kho.png";
            case "NAV_KHUYEN_MAI" -> "nav_khuyen_mai.png";
            case "NAV_NHAN_VIEN" -> "nav_nhan_vien.png";
            case "NAV_TAI_KHOAN" -> "nav_tai_khoan.png";
            case "NAV_TRA_CUU" -> "nav_tra_cuu.png";
            case "NAV_THONG_TIN_CA_NHAN" -> "nav_ho_so.png";
            default -> null;
        };
    }

    private static final Map<String, Image> CACHE_ICON_SIDEBAR = new HashMap<>();

    /**
     * Đọc icon từ classpath (/ten_file.png) hoặc fallback file img/ten_file.png.
     */
    private static Image docIconSidebar(String fileName) {
        if (CACHE_ICON_SIDEBAR.containsKey(fileName))
            return CACHE_ICON_SIDEBAR.get(fileName);
        Image img = null;
        try {
            URL url = MainFrame.class.getResource("/" + fileName);
            if (url == null)
                url = MainFrame.class.getResource(fileName);
            if (url != null) {
                img = ImageIO.read(url);
            } else {
                File f = new File("img/" + fileName);
                if (f.exists())
                    img = ImageIO.read(f);
            }
        } catch (Exception ignored) {
        }
        CACHE_ICON_SIDEBAR.put(fileName, img);
        return img;
    }

    private JLabel taoIconSidebar(NavigationItem item) {
        String fileName = iconTheoNavId(item.getId());
        Image raw = fileName != null ? docIconSidebar(fileName) : null;

        if (raw != null) {
            Image scaled = raw.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
            JLabel lbl = new JLabel(new ImageIcon(scaled));
            lbl.setPreferredSize(new Dimension(30, 30));
            lbl.setOpaque(false);
            return lbl;
        }

        // Fallback: emoji theo NavigationItem (dùng cho vai trò chưa có bộ icon màu)
        JLabel lbl = new JLabel(item.getIcon(), SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 17));
        lbl.setPreferredSize(new Dimension(30, 30));
        lbl.setOpaque(false);
        return lbl;
    }

    private JButton createSidebarButton(NavigationItem item) {
        // Văn bản cho phép xuống dòng theo bề rộng còn lại (như Figma: 1 dòng tròn)
        String htmlText = "<html><body style='margin:0; line-height:1.2; width:172px;'>"
                + item.getTieuDe() + "</body></html>";

        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean isActive = item.getId().equals(activeNavId);
                Color bg;
                if (isActive) {
                    bg = F_ACCENT_BG;
                } else if (getModel().isRollover()) {
                    bg = new Color(0xF1F5F9);
                } else {
                    bg = C_WHITE;
                }

                Shape clip = new java.awt.geom.RoundRectangle2D.Float(
                        0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(bg);
                g2.fill(clip);
                if (isActive) {
                    // Vạch trái 3px màu xanh chủ đạo, cắt theo bo góc 10px
                    g2.setColor(C_PRIMARY);
                    g2.clip(clip);
                    g2.fillRect(0, 0, 3, getHeight());
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
        btn.setBorder(new EmptyBorder(8, 12, 8, 10));

        btn.setPreferredSize(new Dimension(236, 48));
        btn.setMaximumSize(new Dimension(236, 52));
        btn.setMinimumSize(new Dimension(236, 44));

        btn.add(taoIconSidebar(item), BorderLayout.WEST);

        boolean isInitialActive = item.getId().equals(activeNavId);
        JLabel lblText = new JLabel(htmlText);
        lblText.putClientProperty("navText", Boolean.TRUE);
        lblText.setFont(f(isInitialActive ? Font.BOLD : Font.PLAIN, 13));
        lblText.setForeground(isInitialActive ? C_PRIMARY : F_LABEL);
        btn.add(lblText, BorderLayout.CENTER);

        // Click vào nút navigation sẽ đổi Page trong ContentPanel
        btn.addActionListener(e -> moPageTheoNavigation(item.getId()));

        return btn;
    }

    /**
     * Mở đúng Page tương ứng với Navigation được chọn và cập nhật trạng thái
     * Sidebar.
     */
    public void moPageTheoNavigation(String navId) {
        if (navId == null || navId.equals(activeNavId))
            return;
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
                    lbl.setFont(f(isCur ? Font.BOLD : Font.PLAIN, 13));
                    lbl.setForeground(isCur ? C_PRIMARY : F_LABEL);
                }
            }
            b.repaint();
        }
    }

    private void moDanhSachTheoKpi(int index) {
        switch (index) {
            case 0 -> {
                moPageTheoNavigation("NAV_YC_LINH_KIEN");
                if (xuLyYeuCauCCHUI != null) xuLyYeuCauCCHUI.locTuDashboard("Chờ duyệt");
            }
            case 1 -> {
                moPageTheoNavigation("NAV_YC_LINH_KIEN");
                if (xuLyYeuCauCCHUI != null) xuLyYeuCauCCHUI.locTuDashboard("Đã duyệt");
            }
            case 2 -> {
                moPageTheoNavigation("NAV_LINH_KIEN_KHO");
                if (quanLyLinhKienCCHUI != null) quanLyLinhKienCCHUI.locHetHangTuDashboard();
            }
            case 3 -> {
                moPageTheoNavigation("NAV_TAI_KHOAN");
                if (quanLyTaiKhoanCCHUI != null) quanLyTaiKhoanCCHUI.locHoSoChuaCoTaiKhoanTuDashboard();
            }
            default -> { }
        }
    }

    /**
     * Mở page từ nút trên Header (Help / About).
     * Không đổi activeNavId của Sidebar – chỉ chuyển card ContentPanel.
     */
    public void moPageHeader(String pageId) {
        if (contentCardLayout != null && pnlContentPanel != null) {
            contentCardLayout.show(pnlContentPanel, pageId);
        }
        // Bỏ active sidebar khi đang ở trang header
        this.activeNavId = pageId;
        for (Map.Entry<String, JButton> entry : mapNavButtons.entrySet()) {
            JButton b = entry.getValue();
            for (Component c : b.getComponents()) {
                if (c instanceof JLabel lbl && Boolean.TRUE.equals(lbl.getClientProperty("navText"))) {
                    lbl.setFont(f(Font.PLAIN, 13));
                    lbl.setForeground(F_LABEL);
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
            } else if ("NAV_THONG_KE".equals(item.getId()) && nhanVien.getVaiTro() == VaiTro.CHU_CUA_HANG) {
                pnlContentPanel.add(new ThongKeDoanhThuUI(nhanVien), item.getId());
            } else if ("NAV_THONG_KE".equals(item.getId()) && nhanVien.getVaiTro() == VaiTro.KY_THUAT_VIEN) {
                ThongKeSuaChuaUI thongKeUI = new ThongKeSuaChuaUI();
                thongKeUI.setOnMoYeuCauLinhKien(() -> moPageTheoNavigation("NAV_YC_LINH_KIEN"));
                pnlContentPanel.add(thongKeUI, item.getId());
            } else if ("NAV_QUAN_LY_DICH_VU".equals(item.getId()) && nhanVien.getVaiTro() == VaiTro.CHU_CUA_HANG) {
                quanLyDichVuCCHUI = new QuanLyDichVuUI();
                pnlContentPanel.add(quanLyDichVuCCHUI, item.getId());
            } else if ("NAV_LINH_KIEN_KHO".equals(item.getId()) && nhanVien.getVaiTro() == VaiTro.CHU_CUA_HANG) {
                quanLyLinhKienCCHUI = new QuanLyLinhKienUI();
                pnlContentPanel.add(quanLyLinhKienCCHUI, item.getId());
            } else if ("NAV_YC_LINH_KIEN".equals(item.getId()) && nhanVien.getVaiTro() == VaiTro.CHU_CUA_HANG) {
                xuLyYeuCauCCHUI = new XuLyYeuCauLinhKienUI();
                pnlContentPanel.add(xuLyYeuCauCCHUI, item.getId());
            } else if ("NAV_PHIEU_PHAN_CONG".equals(item.getId()) && nhanVien.getVaiTro() == VaiTro.KY_THUAT_VIEN) {
                pnlContentPanel.add(new PhieuPhanCongUI(maPhieu -> {
                    moPageTheoNavigation("NAV_CAP_NHAT_SUA_CHUA");
                    if (capNhatPhieuUI != null) {
                        capNhatPhieuUI.datMaPhieuCanSua(maPhieu);
                    }
                }), item.getId());
            } else if ("NAV_CAP_NHAT_SUA_CHUA".equals(item.getId()) && nhanVien.getVaiTro() == VaiTro.KY_THUAT_VIEN) {
                capNhatPhieuUI = new CapNhatPhieuSuaChuaUI(v -> moPageTheoNavigation("NAV_PHIEU_PHAN_CONG"));
                pnlContentPanel.add(capNhatPhieuUI, item.getId());
            } else if ("NAV_YC_LINH_KIEN".equals(item.getId()) && nhanVien.getVaiTro() == VaiTro.KY_THUAT_VIEN) {
                pnlContentPanel.add(new YeuCauLinhKienKTVUI(), item.getId());
            } else if ("NAV_KHUYEN_MAI".equals(item.getId()) && nhanVien.getVaiTro() == VaiTro.CHU_CUA_HANG) {
                pnlContentPanel.add(new QuanLyKhuyenMaiUI(), item.getId());
            } else if ("NAV_NHAN_VIEN".equals(item.getId()) && nhanVien.getVaiTro() == VaiTro.CHU_CUA_HANG) {
                pnlContentPanel.add(new QuanLyNhanVienUI(), item.getId());
            } else if ("NAV_TAI_KHOAN".equals(item.getId()) && nhanVien.getVaiTro() == VaiTro.CHU_CUA_HANG) {
                quanLyTaiKhoanCCHUI = new QuanLyTaiKhoanUI();
                pnlContentPanel.add(quanLyTaiKhoanCCHUI, item.getId());
            } else if ("NAV_TRA_CUU".equals(item.getId())) {
                traCuuUI = new TraCuuUI(nhanVien.getVaiTro());
                pnlContentPanel.add(traCuuUI, item.getId());
            } else if ("NAV_THONG_TIN_CA_NHAN".equals(item.getId())) {
                pnlContentPanel.add(new ThongTinCaNhanUI(), item.getId());
            } else {
                pnlContentPanel.add(buildModulePage(item), item.getId());
            }
        }

        // Các trang Header (Help / About) – dùng chung cho mọi vai trò
        pnlContentPanel.add(new TroGiupUI(), "PAGE_TRO_GIUP");
        pnlContentPanel.add(new VeCarZoneUI(() -> moPageHeader("PAGE_TRO_GIUP")), "PAGE_VE_CARZONE");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3A. TRANG DASHBOARD THEO VAI TRÒ (Bám sát 100% Figma image45.png)
    // ─────────────────────────────────────────────────────────────────────────
    private JComponent buildRoleDashboardPage() {
        JComponent dashboardView = switch (nhanVien.getVaiTro()) {
            case CHU_CUA_HANG -> buildChuCuaHangDashboardView();
            case TIEP_NHAN_VIEN -> buildTiepNhanVienDashboardView();
            case KY_THUAT_VIEN -> buildKyThuatVienDashboardView();
        };

        JScrollPane scroll = new JScrollPane(dashboardView);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }

    /**
     * Dashboard Chủ cửa hàng (Figma node 83:7716).
     * Chỉ dựng giao diện trên EDT, dữ liệu được nạp bằng SwingWorker ở luồng nền
     * rồi đổ vào giao diện qua {@code capNhatDuLieu(...)}.
     */
    private JComponent buildChuCuaHangDashboardView() {
        ChuCuaHangDashboardPanel panel = new ChuCuaHangDashboardPanel();
        panel.setOnRefresh(() -> taiDuLieuDashboardChuCuaHang(panel));
        taiDuLieuDashboardChuCuaHang(panel);
        return panel;
    }

    private void taiDuLieuDashboardChuCuaHang(ChuCuaHangDashboardPanel panel) {
        if (!panel.batDauTaiDuLieu()) return;
        new SwingWorker<ChuCuaHangDashboardData, Void>() {
            @Override
            protected ChuCuaHangDashboardData doInBackground() throws Exception {
                return dashboardController.layDataChuCuaHang();
            }

            @Override
            protected void done() {
                try {
                    panel.capNhatDuLieu(get());
                } catch (Exception ex) {
                    panel.baoLoiTaiDuLieu();
                }
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER CHUNG CHO DASHBOARD CHỦ CỬA HÀNG (Figma 83:7716)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Định dạng số thập phân kiểu Việt Nam (dấu phẩy thập phân) – dùng cho biểu đồ,
     * phần trăm.
     */
    private static final DecimalFormat SO_NHO_FMT = new DecimalFormat("0.#",
            new DecimalFormatSymbols(Locale.forLanguageTag("vi-VN")));

    private JLabel taoNhan(String text, int style, int size, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(f(style, size));
        lbl.setForeground(color);
        return lbl;
    }

    /** Gắn sự kiện điều hướng (chuột + con trỏ tay) cho một thành phần bất kỳ. */
    private void ganDieuHuong(JComponent comp, String navId) {
        comp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        comp.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                moPageTheoNavigation(navId);
            }
        });
    }

    /** Tạo JLabel hiển thị ảnh minh họa 3D từ img/ (mặc định tỉ lệ vuông). */
    private JLabel taoAnh(String fileName, int w, int h) {
        Image raw = docIconSidebar(fileName);
        JLabel lbl = (raw != null)
                ? new JLabel(new ImageIcon(raw.getScaledInstance(w, h, Image.SCALE_SMOOTH)))
                : new JLabel();
        lbl.setPreferredSize(new Dimension(w, h));
        lbl.setMaximumSize(new Dimension(w, h));
        lbl.setOpaque(false);
        return lbl;
    }

    /** Nhãn liên kết dạng "Văn bản ↗" có thể bấm để điều hướng. */
    private JPanel taoLink(String text, int size, Color clrChu, Color clrIcon, String navId) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);
        p.add(taoNhan(text, Font.BOLD, size, clrChu));
        p.add(new JLabel(new ArrowUpRightIcon(clrIcon, 14)));
        ganDieuHuong(p, navId);
        return p;
    }

    /**
     * Ép bề rộng cố định cho một component (dùng trong GridBagLayout cột weightx =
     * 0).
     */
    private JComponent oCoRong(JComponent c, int w) {
        c.setPreferredSize(new Dimension(w, c.getPreferredSize().height));
        c.setMaximumSize(new Dimension(w, Integer.MAX_VALUE));
        return c;
    }

    /**
     * Dashboard Chủ cửa hàng – Figma node 83:7716.
     * Chỉ dựng giao diện; dữ liệu thật được nạp nền qua SwingWorker rồi đổ vào
     * bằng {@link #capNhatDuLieu(ChuCuaHangDashboardData)}.
     */
    public class ChuCuaHangDashboardPanel extends ScrollableContentPanel {

        private final JLabel lblNgay = taoNhan(LocalDate.now().format(LOCAL_DATE_FMT), Font.BOLD, 12, F_TITLE);
        private final JButton btnLamMoiDashboard = new RoundedButton("Làm mới", 10, C_WHITE,
                new Color(0xF1F5F9), F_LINE);
        private final JLabel[] kpiSo = new JLabel[4];
        private final JLabel lblBadgeYeuCau = taoNhan("0 yêu cầu", Font.BOLD, 11, C_PRIMARY);
        private final JPanel pnlBangYeuCau = new JPanel();
        private final JLabel lblBadgeHetHang = taoNhan("0 mã", Font.BOLD, 11, F_DANGER);
        private final JPanel pnlDanhSachHetHang = new JPanel();
        private final JLabel lblThuThuan = taoNhan("0", Font.BOLD, 29, F_DEEP);
        private final JLabel lblTongThu = taoNhan("0 VNĐ", Font.BOLD, 11, F_TITLE);
        private final JLabel lblHoanTien = taoNhan("0 VNĐ", Font.BOLD, 11, F_DANGER);
        private final JLabel[] giaTriCot = new JLabel[4];
        private final CotBieuDo[] cotGio = new CotBieuDo[4];
        private final ThanhCoCauPanel thanhCoCau = new ThanhCoCauPanel();
        private final JLabel lblTyLeCK = taoNhan("0%", Font.PLAIN, 11, F_DESC);
        private final JLabel lblTyLeTM = taoNhan("0%", Font.PLAIN, 11, F_DESC);
        private final JLabel lblTienCK = taoNhan("0 VNĐ", Font.BOLD, 14, F_TITLE);
        private final JLabel lblTienTM = taoNhan("0 VNĐ", Font.BOLD, 14, F_TITLE);
        private Runnable onRefresh;
        private boolean dangTaiDuLieu;

        public ChuCuaHangDashboardPanel() {
            setBorder(new EmptyBorder(20, 26, 22, 26));

            // ── 1. Tiêu đề + chip ngày báo cáo ───────────────────────────────
            JPanel pnlTieuDe = new JPanel(new BorderLayout(0, 0));
            pnlTieuDe.setOpaque(false);
            pnlTieuDe.setAlignmentX(Component.CENTER_ALIGNMENT);
            pnlTieuDe.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

            JLabel lblTitle = taoNhan("Dashboard", Font.BOLD, 24, F_TITLE);
            pnlTieuDe.add(lblTitle, BorderLayout.WEST);

            btnLamMoiDashboard.setFont(f(Font.BOLD, 12));
            btnLamMoiDashboard.setForeground(F_LABEL);
            btnLamMoiDashboard.setPreferredSize(new Dimension(88, 36));
            btnLamMoiDashboard.addActionListener(e -> {
                if (onRefresh != null) onRefresh.run();
            });

            RoundedPanel chipNgay = new RoundedPanel(C_WHITE, 10, F_LINE, new Insets(9, 14, 9, 14));
            chipNgay.setLayout(new BorderLayout(9, 0));
            chipNgay.add(new JLabel(new CalendarIcon(F_DESC, 16)), BorderLayout.WEST);
            chipNgay.add(lblNgay, BorderLayout.CENTER);
            JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            headerRight.setOpaque(false);
            headerRight.add(btnLamMoiDashboard);
            headerRight.add(chipNgay);
            pnlTieuDe.add(headerRight, BorderLayout.EAST);
            add(pnlTieuDe);
            add(Box.createVerticalStrut(16));

            // ── 2. Hàng 4 thẻ KPI ────────────────────────────────────────────
            JPanel pnlKpi = new JPanel(new GridLayout(1, 4, 13, 0));
            pnlKpi.setOpaque(false);
            pnlKpi.setAlignmentX(Component.CENTER_ALIGNMENT);
            pnlKpi.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

            pnlKpi.add(taoTheKpi(0, "Yêu cầu chờ phê duyệt", "illustration_request.png"));
            pnlKpi.add(taoTheKpi(1, "Chờ xác nhận nhập kho", "illustration_stock.png"));
            pnlKpi.add(taoTheKpi(2, "Linh kiện hết hàng", "illustration_alert.png"));
            pnlKpi.add(taoTheKpi(3, "Hồ sơ chưa có tài khoản", "illustration_staff.png"));
            add(pnlKpi);
            add(Box.createVerticalStrut(18));

            // ── 3. Heading "Công việc cần xử lý" ────────────────────────────
            JPanel pnlHeadViec = new JPanel(new BorderLayout(0, 0));
            pnlHeadViec.setOpaque(false);
            pnlHeadViec.setAlignmentX(Component.CENTER_ALIGNMENT);
            pnlHeadViec.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            pnlHeadViec.add(taoNhan("Công việc cần xử lý", Font.BOLD, 18, F_TITLE), BorderLayout.WEST);
            add(pnlHeadViec);
            add(Box.createVerticalStrut(12));

            // ── 4. Hàng: Bảng YC chờ duyệt (flex) + Cảnh báo hết hàng (352px) ─
            JPanel pnlHangViec = new JPanel(new BorderLayout(16, 0));
            pnlHangViec.setOpaque(false);
            pnlHangViec.setAlignmentX(Component.CENTER_ALIGNMENT);
            pnlHangViec.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            pnlHangViec.add(taoBangYeuCauChoDuyet(), BorderLayout.CENTER);
            pnlHangViec.add(taoCardCanhBaoHetHang(), BorderLayout.EAST);
            add(pnlHangViec);
            add(Box.createVerticalStrut(18));

            // ── 5. Card "Kinh doanh trong ngày" ─────────────────────────────
            JPanel cardKD = taoCardKinhDoanhTrongNgay();
            cardKD.setAlignmentX(Component.CENTER_ALIGNMENT);
            cardKD.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            add(cardKD);
        }

        /** Thẻ KPI nền trắng, bấm toàn thẻ để mở danh sách đã lọc. */
        private JPanel taoTheKpi(int viTri, String tieuDe, String iconTen) {
            RoundedPanel card = new RoundedPanel(C_WHITE, 12, F_LINE, new Insets(14, 16, 14, 16));
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setMinimumSize(new Dimension(180, 112));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            card.setToolTipText("Bấm để mở danh sách đã lọc.");

            // Hàng số + minh họa 3D
            JPanel rowSo = new JPanel(new BorderLayout(0, 0));
            rowSo.setOpaque(false);
            rowSo.setAlignmentX(Component.LEFT_ALIGNMENT);
            rowSo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            JLabel lblSo = taoNhan("—", Font.BOLD, 30, F_TITLE);
            kpiSo[viTri] = lblSo;
            rowSo.add(lblSo, BorderLayout.WEST);
            rowSo.add(taoAnh(iconTen, 36, 36), BorderLayout.EAST);
            card.add(rowSo);
            card.add(Box.createVerticalStrut(12));
            JLabel lblTieuDe = taoNhan(tieuDe, Font.BOLD, 14, F_TITLE);
            lblTieuDe.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(lblTieuDe);
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            java.awt.event.MouseAdapter click = new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) { moDanhSachTheoKpi(viTri); }
            };
            ganClickToanThe(card, click);
            return card;
        }

        private void ganClickToanThe(Component component, java.awt.event.MouseAdapter click) {
            component.addMouseListener(click);
            component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (component instanceof Container container) {
                for (Component child : container.getComponents()) ganClickToanThe(child, click);
            }
        }

        /**
         * Card "Yêu cầu chờ duyệt" gồm header, bảng tiêu đề, danh sách dòng và footer 4
         * đếm.
         */
        private JPanel taoBangYeuCauChoDuyet() {
            RoundedPanel card = new RoundedPanel(C_WHITE, 16, F_LINE, null);
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

            // Header
            JPanel head = new JPanel(new BorderLayout(0, 0));
            head.setOpaque(false);
            head.setAlignmentX(Component.LEFT_ALIGNMENT);
            head.setBorder(new EmptyBorder(10, 16, 10, 16));
            head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            left.setOpaque(false);
            left.add(taoNhan("Yêu cầu chờ duyệt", Font.BOLD, 15, F_TITLE));
            RoundedPanel badge = new RoundedPanel(F_ACCENT_BG, 6, null, new Insets(4, 9, 4, 9));
            badge.add(lblBadgeYeuCau);
            left.add(badge);
            head.add(left, BorderLayout.WEST);
            head.add(taoLink("Mở danh sách", 12, C_PRIMARY, C_PRIMARY, "NAV_YC_LINH_KIEN"), BorderLayout.EAST);
            card.add(head);

            // Bảng tiêu đề cột
            JPanel tieuDeBang = taoTieuDeBangYeuCau();
            tieuDeBang.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(tieuDeBang);

            // Danh sách dòng dữ liệu (cập nhật lại khi có dữ liệu thật)
            pnlBangYeuCau.setLayout(new BoxLayout(pnlBangYeuCau, BoxLayout.Y_AXIS));
            pnlBangYeuCau.setOpaque(false);
            pnlBangYeuCau.setAlignmentX(Component.LEFT_ALIGNMENT);
            pnlBangYeuCau.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            card.add(pnlBangYeuCau);

            return card;
        }

        private JPanel taoTieuDeBangYeuCau() {
            JPanel row = new JPanel(new GridBagLayout());
            row.setBackground(F_BG);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setBorder(new EmptyBorder(7, 16, 7, 16));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            GridBagConstraints gc = new GridBagConstraints();
            gc.gridy = 0;
            gc.anchor = GridBagConstraints.WEST;
            gc.fill = GridBagConstraints.VERTICAL;
            gc.insets = new Insets(0, 0, 0, 10);

            gc.gridx = 0;
            gc.weightx = 0;
            gc.fill = GridBagConstraints.NONE;
            row.add(oCoRong(taoNhan("MÃ / NGÀY TẠO", Font.BOLD, 10, F_DESC), 108), gc);
            gc.gridx = 1;
            row.add(oCoRong(taoNhan("KTV LẬP", Font.BOLD, 10, F_DESC), 138), gc);
            gc.gridx = 2;
            gc.weightx = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            row.add(taoNhan("LINH KIỆN", Font.BOLD, 10, F_DESC), gc);
            gc.gridx = 3;
            gc.weightx = 0;
            gc.fill = GridBagConstraints.NONE;
            row.add(oCoRong(taoNhan("YÊU CẦU", Font.BOLD, 10, F_DESC), 72), gc);
            gc.gridx = 4;
            row.add(oCoRong(taoNhan("TỒN KHO", Font.BOLD, 10, F_DESC), 84), gc);
            return row;
        }

        /** Một dòng dữ liệu trong bảng "Yêu cầu chờ duyệt". */
        private JPanel taoHangYeuCau(DashboardController.YeuCauRow r, boolean coVienDuoi) {
            JPanel row = new JPanel(new GridBagLayout());
            row.setOpaque(false);
            row.setBorder(BorderFactory.createCompoundBorder(
                    coVienDuoi ? BorderFactory.createMatteBorder(0, 0, 1, 0, F_LINE)
                            : BorderFactory.createEmptyBorder(),
                    new EmptyBorder(8, 16, 8, 16)));

            GridBagConstraints gc = new GridBagConstraints();
            gc.gridy = 0;
            gc.anchor = GridBagConstraints.WEST;
            gc.fill = GridBagConstraints.VERTICAL;
            gc.insets = new Insets(0, 0, 0, 10);

            // Cột 1: mã + ngày tạo
            gc.gridx = 0;
            gc.weightx = 0;
            gc.fill = GridBagConstraints.NONE;
            JPanel ref = new JPanel();
            ref.setLayout(new BoxLayout(ref, BoxLayout.Y_AXIS));
            ref.setOpaque(false);
            JLabel lblMa = taoNhan(r.maYeuCau, Font.BOLD, 12, C_PRIMARY);
            lblMa.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel lblNgayTao = taoNhan(r.ngayTao, Font.PLAIN, 10, F_DESC);
            lblNgayTao.setAlignmentX(Component.LEFT_ALIGNMENT);
            ref.add(lblMa);
            ref.add(Box.createVerticalStrut(3));
            ref.add(lblNgayTao);
            row.add(oCoRong(ref, 108), gc);

            // Cột 2: KTV lập
            gc.gridx = 1;
            row.add(oCoRong(taoNhan(r.ktvLap, Font.PLAIN, 12, F_TITLE), 138), gc);

            // Cột 3: linh kiện + mã (co giãn)
            gc.gridx = 2;
            gc.weightx = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            JPanel lk = new JPanel();
            lk.setLayout(new BoxLayout(lk, BoxLayout.Y_AXIS));
            lk.setOpaque(false);
            JLabel lblTenLk = taoNhan(r.linhKien, Font.BOLD, 12, F_TITLE);
            lblTenLk.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel lblMaLk = taoNhan(r.maLinhKien, Font.PLAIN, 10, F_DESC);
            lblMaLk.setAlignmentX(Component.LEFT_ALIGNMENT);
            lk.add(lblTenLk);
            lk.add(Box.createVerticalStrut(3));
            lk.add(lblMaLk);
            row.add(lk, gc);

            // Cột 4: số lượng yêu cầu
            gc.gridx = 3;
            gc.weightx = 0;
            gc.fill = GridBagConstraints.NONE;
            row.add(oCoRong(taoNhan(r.soLuongYeuCau, Font.BOLD, 12, F_TITLE), 72), gc);

            // Cột 5: tồn kho (đỏ nếu hết)
            gc.gridx = 4;
            row.add(oCoRong(taoNhan(r.tonKho, Font.BOLD, 12, r.tonKhoHet ? F_DANGER : F_LABEL), 84), gc);

            return row;
        }

        /** Card "Cảnh báo hết hàng" – chiều rộng 310px cố định, cao theo nội dung. */
        private JPanel taoCardCanhBaoHetHang() {
            RoundedPanel card = new RoundedPanel(C_WHITE, 14, F_LINE, new Insets(14, 14, 14, 14));
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setPreferredSize(new Dimension(310, 0)); // chỉ cố định rộng, cao tự co
            card.setMinimumSize(new Dimension(310, 80));
            card.setMaximumSize(new Dimension(310, Integer.MAX_VALUE));

            // Heading: hàng trên – icon cảnh báo ⚠ + tiêu đề (trái); badge số mã (phải)
            JPanel headRow = new JPanel(new BorderLayout(8, 0));
            headRow.setOpaque(false);
            headRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            headRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            JPanel headLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            headLeft.setOpaque(false);
            headLeft.add(taoAnh("illustration_alert.png", 22, 22));
            headLeft.add(taoNhan("Cảnh báo hết hàng", Font.BOLD, 14, F_TITLE));
            headRow.add(headLeft, BorderLayout.WEST);

            RoundedPanel badge = new RoundedPanel(F_DANGER_BG, 6, null, new Insets(3, 8, 3, 8));
            badge.add(lblBadgeHetHang);
            headRow.add(badge, BorderLayout.EAST);
            card.add(headRow);
            card.add(Box.createVerticalStrut(8));

            // Danh sách mã hết hàng (cập nhật lại khi có dữ liệu thật)
            pnlDanhSachHetHang.setLayout(new BoxLayout(pnlDanhSachHetHang, BoxLayout.Y_AXIS));
            pnlDanhSachHetHang.setOpaque(false);
            pnlDanhSachHetHang.setAlignmentX(Component.LEFT_ALIGNMENT);
            pnlDanhSachHetHang.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            card.add(pnlDanhSachHetHang);
            card.add(Box.createVerticalStrut(10));

            JPanel linkRow = taoLink("Quản lý linh kiện và tồn kho", 12, C_PRIMARY, C_PRIMARY, "NAV_LINH_KIEN_KHO");
            linkRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(linkRow);

            return card;
        }

        private JPanel taoHangHetHang(DashboardController.LinhKienHetHangRow r) {
            JPanel item = new JPanel();
            item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));
            item.setOpaque(false);
            item.setAlignmentX(Component.LEFT_ALIGNMENT);
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            item.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, F_LINE),
                    new EmptyBorder(0, 0, 14, 0)));

            JPanel top = new JPanel(new BorderLayout(0, 0));
            top.setOpaque(false);
            top.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
            top.add(taoNhan(r.tenLinhKien, Font.BOLD, 13, F_TITLE), BorderLayout.WEST);
            top.add(taoNhan("0 " + r.donVi, Font.BOLD, 13, F_DANGER), BorderLayout.EAST);
            item.add(top);
            return item;
        }

        /**
         * Card "Kinh doanh trong ngày": doanh thu sau hoàn + biểu đồ khung giờ + cơ cấu
         * thanh toán.
         */
        private JPanel taoCardKinhDoanhTrongNgay() {
            RoundedPanel card = new RoundedPanel(C_WHITE, 14, F_LINE, new Insets(16, 16, 16, 16));
            card.setLayout(new BorderLayout(0, 14));

            // Heading ở NORTH
            JPanel head = new JPanel(new BorderLayout(0, 0));
            head.setOpaque(false);
            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            left.setOpaque(false);
            left.add(taoNhan("Kinh doanh trong ngày", Font.BOLD, 16, F_TITLE));
            head.add(left, BorderLayout.WEST);
            head.add(taoLink("Thống kê báo cáo", 12, C_PRIMARY, C_PRIMARY, "NAV_THONG_KE"), BorderLayout.EAST);
            card.add(head, BorderLayout.NORTH);

            // Thân card: 3 cột co giãn đều nhau – GridLayout tự fill width
            JPanel body = new JPanel(new GridLayout(1, 3, 18, 0));
            body.setOpaque(false);
            body.add(taoKhoiDoanhThu());
            body.add(taoKhoiBieuDo());
            body.add(taoKhoiCoCauThanhToan());
            card.add(body, BorderLayout.CENTER);

            return card;
        }

        private JPanel taoKhoiDoanhThu() {
            RoundedPanel box = new RoundedPanel(F_ACCENT_BG, 10, null, new Insets(12, 14, 12, 14));
            box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
            // Không set kích thước cứng – co giãn theo GridLayout cha

            JLabel lbl = taoNhan("DOANH THU SAU HOÀN", Font.PLAIN, 11, C_PRIMARY);
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            box.add(lbl);
            box.add(Box.createVerticalStrut(5));

            JPanel rowValue = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            rowValue.setOpaque(false);
            rowValue.setAlignmentX(Component.LEFT_ALIGNMENT);
            lblThuThuan.setHorizontalAlignment(SwingConstants.LEFT);
            rowValue.add(lblThuThuan);
            rowValue.add(taoNhan("VNĐ", Font.PLAIN, 11, C_PRIMARY));
            box.add(rowValue);
            box.add(Box.createVerticalStrut(5));

            box.add(taoHangTien("Tổng thu", lblTongThu, F_LABEL));
            box.add(Box.createVerticalStrut(5));
            box.add(taoHangTien("Hoàn tiền", lblHoanTien, F_LABEL));
            box.add(Box.createVerticalStrut(5));

            return box;
        }

        private JPanel taoHangTien(String nhan, JLabel giaTri, Color clrNhan) {
            JPanel row = new JPanel(new BorderLayout(0, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
            row.add(taoNhan(nhan, Font.PLAIN, 11, clrNhan), BorderLayout.WEST);
            row.add(giaTri, BorderLayout.EAST);
            return row;
        }

        private JPanel taoKhoiBieuDo() {
            JPanel box = new JPanel();
            box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
            box.setOpaque(false);
            // Không set kích thước cứng – co giãn theo GridLayout cha

            JPanel title = new JPanel(new BorderLayout(0, 0));
            title.setOpaque(false);
            title.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
            title.add(taoNhan("Doanh thu sau hoàn", Font.BOLD, 12, F_TITLE), BorderLayout.WEST);
            title.add(taoNhan("Triệu VNĐ", Font.PLAIN, 10, F_DESC), BorderLayout.EAST);
            box.add(title);
            box.add(Box.createVerticalStrut(10));

            String[] nhanKhung = { "08–10", "10–12", "13–15", "15–18" };
            JPanel bars = new JPanel(new GridLayout(1, 4, 12, 0));
            bars.setOpaque(false);
            bars.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, F_LINE),
                    new EmptyBorder(0, 8, 0, 8)));
            bars.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
            bars.setMinimumSize(new Dimension(160, 90));

            for (int i = 0; i < 4; i++) {
                JPanel col = new JPanel();
                col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
                col.setOpaque(false);
                col.add(Box.createVerticalGlue());
                giaTriCot[i] = taoNhan("0", Font.BOLD, 11, C_PRIMARY);
                giaTriCot[i].setHorizontalAlignment(SwingConstants.CENTER);
                giaTriCot[i].setAlignmentX(Component.CENTER_ALIGNMENT);
                col.add(giaTriCot[i]);
                col.add(Box.createVerticalStrut(5));
                cotGio[i] = new CotBieuDo(F_BAR_SOFT);
                cotGio[i].setAlignmentX(Component.CENTER_ALIGNMENT);
                col.add(cotGio[i]);
                col.add(Box.createVerticalStrut(5));
                JLabel nhan = taoNhan(nhanKhung[i], Font.PLAIN, 10, F_DESC);
                nhan.setHorizontalAlignment(SwingConstants.CENTER);
                nhan.setAlignmentX(Component.CENTER_ALIGNMENT);
                col.add(nhan);
                bars.add(col);
            }
            box.add(bars);

            return box;
        }

        private JPanel taoKhoiCoCauThanhToan() {
            JPanel box = new JPanel();
            box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
            box.setOpaque(false);
            box.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 1, 0, 0, F_LINE),
                    new EmptyBorder(0, 24, 0, 0)));

            JLabel title = taoNhan("Cơ cấu thanh toán", Font.BOLD, 12, F_TITLE);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            box.add(title);
            box.add(Box.createVerticalStrut(15));

            thanhCoCau.setAlignmentX(Component.LEFT_ALIGNMENT);
            box.add(thanhCoCau);
            box.add(Box.createVerticalStrut(15));

            box.add(taoHangThanhToan("Chuyển khoản", C_PRIMARY, lblTyLeCK, lblTienCK));
            box.add(Box.createVerticalStrut(15));
            box.add(taoHangThanhToan("Tiền mặt", F_TEAL, lblTyLeTM, lblTienTM));

            return box;
        }

        private JPanel taoHangThanhToan(String nhan, Color mauDot, JLabel tyLe, JLabel tien) {
            JPanel grp = new JPanel();
            grp.setLayout(new BoxLayout(grp, BoxLayout.Y_AXIS));
            grp.setOpaque(false);
            grp.setAlignmentX(Component.LEFT_ALIGNMENT);
            grp.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

            JPanel row = new JPanel(new BorderLayout(0, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
            JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
            legend.setOpaque(false);
            legend.add(new JLabel(new DotIcon(mauDot, 6)));
            legend.add(taoNhan(nhan, Font.PLAIN, 11, F_LABEL));
            row.add(legend, BorderLayout.WEST);
            row.add(tyLe, BorderLayout.EAST);
            grp.add(row);
            grp.add(Box.createVerticalStrut(4));

            tien.setAlignmentX(Component.LEFT_ALIGNMENT);
            grp.add(tien);

            return grp;
        }

        public void setOnRefresh(Runnable callback) {
            onRefresh = callback;
        }

        public boolean batDauTaiDuLieu() {
            if (dangTaiDuLieu) return false;
            dangTaiDuLieu = true;
            btnLamMoiDashboard.setEnabled(false);
            btnLamMoiDashboard.setText("Đang tải…");
            return true;
        }

        public void baoLoiTaiDuLieu() {
            dangTaiDuLieu = false;
            btnLamMoiDashboard.setEnabled(true);
            btnLamMoiDashboard.setText("Làm mới");
            btnLamMoiDashboard.setToolTipText("Không tải được số liệu từ CSDL. Bấm để thử lại.");
            for (int i = 0; i < kpiSo.length; i++) {
                kpiSo[i].setText("—");
            }
        }

        /** Đổ dữ liệu thật (luồng EDT – gọi từ SwingWorker.done()). */
        public void capNhatDuLieu(ChuCuaHangDashboardData data) {
            dangTaiDuLieu = false;
            btnLamMoiDashboard.setEnabled(true);
            btnLamMoiDashboard.setText("Làm mới");
            btnLamMoiDashboard.setToolTipText("Cập nhật số liệu từ CSDL.");
            lblNgay.setText(data.ngayBaoCao == null || data.ngayBaoCao.isEmpty()
                    ? LocalDate.now().format(LOCAL_DATE_FMT)
                    : data.ngayBaoCao);

            // 4 thẻ KPI
            kpiSo[0].setText(String.format("%02d", data.yeuCauChoDuyet));
            kpiSo[1].setText(String.format("%02d", data.choXacNhanNhapKho));
            kpiSo[2].setText(String.format("%02d", data.linhKienHetHang));
            kpiSo[3].setText(String.format("%02d", data.hoSoChuaCoTaiKhoan));

            // Bảng yêu cầu chờ duyệt
            lblBadgeYeuCau.setText(data.yeuCauChoDuyet + " yêu cầu");
            pnlBangYeuCau.removeAll();
            if (data.danhSachYeuCau.isEmpty()) {
                JPanel rowRong = new JPanel(new BorderLayout(0, 0));
                rowRong.setOpaque(false);
                rowRong.setAlignmentX(Component.LEFT_ALIGNMENT);
                rowRong.setBorder(new EmptyBorder(14, 20, 14, 20));
                rowRong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
                rowRong.add(taoNhan("Chưa có yêu cầu nào chờ duyệt", Font.PLAIN, 11, F_DESC), BorderLayout.WEST);
                pnlBangYeuCau.add(rowRong);
            } else {
                int n = data.danhSachYeuCau.size();
                for (int i = 0; i < n; i++) {
                    JPanel row = taoHangYeuCau(data.danhSachYeuCau.get(i), i < n - 1);
                    row.setAlignmentX(Component.LEFT_ALIGNMENT);
                    pnlBangYeuCau.add(row);
                }
            }
            pnlBangYeuCau.revalidate();
            pnlBangYeuCau.repaint();

            // Cảnh báo hết hàng
            lblBadgeHetHang.setText(data.linhKienHetHang + " mã");
            pnlDanhSachHetHang.removeAll();
            if (data.danhSachHetHang.isEmpty()) {
                JLabel empty = taoNhan("Hiện không có linh kiện nào bị hết hàng.", Font.PLAIN, 11, F_DESC);
                empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                pnlDanhSachHetHang.add(empty);
            } else {
                for (LinhKienHetHangRow r : data.danhSachHetHang) {
                    JPanel item = taoHangHetHang(r);
                    item.setAlignmentX(Component.LEFT_ALIGNMENT);
                    pnlDanhSachHetHang.add(item);
                }
                int conLai = data.linhKienHetHang - data.danhSachHetHang.size();
                if (conLai > 0) {
                    JLabel more = taoNhan("Còn " + conLai + " mã hết hàng khác", Font.PLAIN, 11, F_DESC);
                    more.setAlignmentX(Component.LEFT_ALIGNMENT);
                    pnlDanhSachHetHang.add(Box.createVerticalStrut(8));
                    pnlDanhSachHetHang.add(more);
                }
            }
            pnlDanhSachHetHang.revalidate();
            pnlDanhSachHetHang.repaint();

            // Kinh doanh trong ngày
            lblThuThuan.setText(MONEY_FMT.format(data.doanhThuSauHoan));
            lblTongThu.setText(MONEY_FMT.format(data.tongThu) + " VNĐ");
            lblHoanTien.setText(MONEY_FMT.format(data.hoanTien) + " VNĐ");

            // Biểu đồ 4 khung giờ
            double maxGio = 0;
            for (double v : data.doanhThuKhungGio)
                maxGio = Math.max(maxGio, v);
            int chiSoCaoNhat = 0;
            for (int i = 1; i < 4; i++) {
                if (data.doanhThuKhungGio[i] > data.doanhThuKhungGio[chiSoCaoNhat])
                    chiSoCaoNhat = i;
            }
            for (int i = 0; i < 4; i++) {
                double trieu = data.doanhThuKhungGio[i] / 1_000_000.0;
                giaTriCot[i].setText(SO_NHO_FMT.format(trieu));
                int h = maxGio > 0
                        ? Math.max(8, (int) Math.round(trieu / (maxGio / 1_000_000.0) * 70))
                        : 8;
                cotGio[i].datChieuCao(h);
                cotGio[i].datMau(i == chiSoCaoNhat && maxGio > 0 ? C_PRIMARY : F_BAR_SOFT);
            }

            // Cơ cấu thanh toán
            double tong = data.doanhThuChuyenKhoan + data.doanhThuTienMat;
            double tiLeCK = tong > 0 ? data.doanhThuChuyenKhoan / tong : 0;
            thanhCoCau.datTiLe(tiLeCK);
            lblTyLeCK.setText(SO_NHO_FMT.format(tiLeCK * 100) + "%");
            lblTyLeTM.setText(SO_NHO_FMT.format((1 - tiLeCK) * 100) + "%");
            lblTienCK.setText(MONEY_FMT.format(data.doanhThuChuyenKhoan) + " VNĐ");
            lblTienTM.setText(MONEY_FMT.format(data.doanhThuTienMat) + " VNĐ");

            revalidate();
            repaint();
        }
    }

    private JPanel createWhiteKpiCard(String number, String title, String sub, String link, String iconStr,
            String targetNav) {
        JPanel c = new JPanel(new BorderLayout(0, 6));
        c.setBackground(C_WHITE);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(14, 16, 12, 16)));

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

        pnlKpis.add(createWhiteKpiCard(String.format("%02d", data.phieuChoSua), "Chờ sửa chữa", "Xe đã tiếp nhận",
                "Xem danh sách ↗", "⏳", "NAV_PHIEU_TIEP_NHAN"));
        pnlKpis.add(createWhiteKpiCard(String.format("%02d", data.phieuDangSua), "Đang sửa chữa", "KTV đang xử lý",
                "Xem danh sách ↗", "🔧", "NAV_PHIEU_TIEP_NHAN"));
        pnlKpis.add(createWhiteKpiCard(String.format("%02d", data.phieuChoLinhKien), "Chờ linh kiện", "Chờ xuất kho",
                "Xem danh sách ↗", "📦", "NAV_PHIEU_TIEP_NHAN"));
        pnlKpis.add(createWhiteKpiCard(String.format("%02d", data.phieuHoanTatChoThu), "Hoàn tất chờ thu",
                "Sẵn sàng xuất hóa đơn", "Lập hóa đơn ↗", "🧾", "NAV_HOA_DON"));
        main.add(pnlKpis);

        main.add(Box.createVerticalStrut(20));

        // Bảng phiếu tiếp nhận gần đây
        JPanel cardTable = new JPanel(new BorderLayout(0, 10));
        cardTable.setBackground(C_WHITE);
        cardTable.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(14, 16, 14, 16)));
        cardTable.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));

        JLabel tblTitle = new JLabel("Tiến độ xe tại xưởng trong ngày");
        tblTitle.setFont(f(Font.BOLD, 14));
        tblTitle.setForeground(C_TEXT_HEAD);
        cardTable.add(tblTitle, BorderLayout.NORTH);

        String[] cols = { "MÃ PHIẾU", "BIỂN SỐ XE", "KHÁCH HÀNG", "SỐ ĐIỆN THOẠI", "KTV PHỤ TRÁCH", "GIỜ TIẾP NHẬN",
                "TRẠNG THÁI" };
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        for (PhieuTiepNhanRow r : data.danhSachPhieu) {
            model.addRow(new Object[] { r.maPhieu, r.bienSo, r.khachHang, r.soDienThoai, r.ktvChinh, r.thoiDiem,
                    r.trangThai.toString() });
        }
        if (model.getRowCount() == 0) {
            model.addRow(new Object[] { "Chưa có phiếu tiếp nhận nào trong ngày", "—", "—", "—", "—", "—", "—" });
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

    private void addFull(JComponent parent, JComponent comp) {
        comp.setAlignmentX(Component.CENTER_ALIGNMENT);
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, comp.getPreferredSize().height));
        parent.add(comp);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DASHBOARD KỸ THUẬT VIÊN (Figma node 50:589)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildKyThuatVienDashboardView() {
        ScrollableContentPanel main = new ScrollableContentPanel();
        main.setBorder(new EmptyBorder(20, 26, 20, 26));

        // 1. Heading
        JPanel pnlHead = new JPanel(new BorderLayout());
        pnlHead.setOpaque(false);
        pnlHead.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlHead.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel headLeft = new JPanel();
        headLeft.setOpaque(false);
        headLeft.setLayout(new BoxLayout(headLeft, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleRow.setOpaque(false);
        titleRow.add(taoNhan("🛠️", Font.PLAIN, 24, F_TITLE));
        titleRow.add(taoNhan("Dashboard", Font.BOLD, 24, F_TITLE));
        headLeft.add(titleRow);
        headLeft.add(Box.createVerticalStrut(4));
        pnlHead.add(headLeft, BorderLayout.WEST);

        JButton btnThongKeTop = new RoundedButton("Thống kê sửa chữa ↗", 12, C_WHITE, new Color(0xF1F5F9), F_LINE);
        btnThongKeTop.setFont(f(Font.BOLD, 12));
        btnThongKeTop.setForeground(C_TEXT_MAIN);
        btnThongKeTop.setPreferredSize(new Dimension(170, 38));
        btnThongKeTop.addActionListener(e -> moPageTheoNavigation("NAV_THONG_KE"));
        pnlHead.add(btnThongKeTop, BorderLayout.EAST);

        main.add(pnlHead);
        main.add(Box.createVerticalStrut(18));

        // 2. Section: Công việc ưu tiên
        JPanel sec1Head = new JPanel(new BorderLayout());
        sec1Head.setOpaque(false);
        sec1Head.setAlignmentX(Component.CENTER_ALIGNMENT);
        sec1Head.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        sec1Head.add(taoNhan("Công việc ưu tiên", Font.BOLD, 15, F_TITLE), BorderLayout.WEST);
        main.add(sec1Head);
        main.add(Box.createVerticalStrut(12));

        // 4 Thẻ KPI ưu tiên - dữ liệu thực từ DB
        JPanel pnlKpis = new JPanel(new GridLayout(1, 4, 13, 0));
        pnlKpis.setOpaque(false);
        pnlKpis.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlKpis.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Khởi tạo label động để cập nhật sau khi load DB
        JLabel lblKpi1Num = taoNhan("...", Font.BOLD, 28, F_TITLE);
        JLabel lblKpi1Sub = taoNhan("Đang tải dữ liệu...", Font.PLAIN, 11, F_ON_BLUE);

        JLabel lblKpi2Num = taoNhan("...", Font.BOLD, 28, F_TITLE);
        JLabel lblKpi2Sub = taoNhan("Đang tải...", Font.PLAIN, 11, F_DESC);

        JLabel lblKpi3Num = taoNhan("...", Font.BOLD, 28, F_TITLE);
        JLabel lblKpi3Sub = taoNhan("Đang tải...", Font.PLAIN, 11, F_DESC);

        JLabel lblKpi4Num = taoNhan("...", Font.BOLD, 28, F_TITLE);
        JLabel lblKpi4Sub = taoNhan("Đang tải...", Font.PLAIN, 11, F_DESC);

        pnlKpis.add(taoKpiKtvCardDynamic("Hôm nay", "phiếu", new Color(0xEFF6FF), C_PRIMARY, lblKpi1Num, "📋",
                "Phiếu cần hoàn thành", lblKpi1Sub, "Mở phiếu cần hoàn thành ↗", "NAV_PHIEU_PHAN_CONG"));
        pnlKpis.add(taoKpiKtvCardDynamic("Đang sửa", "phiếu", new Color(0xFFFBEB), C_WARN, lblKpi2Num, "🔧",
                "Phiếu đang xử lý", lblKpi2Sub, "Xem phiếu ↗", "NAV_PHIEU_PHAN_CONG"));
        pnlKpis.add(taoKpiKtvCardDynamic("Chờ phản hồi", "yêu cầu", new Color(0xEFF6FF), C_PRIMARY, lblKpi3Num, "📦",
                "Yêu cầu linh kiện chờ duyệt", lblKpi3Sub, "Xem yêu cầu linh kiện ↗", "NAV_YC_LINH_KIEN"));
        pnlKpis.add(taoKpiKtvCardDynamic("Hoàn tất", "phiếu", new Color(0xF1F5F9), C_TEXT_SUBTLE, lblKpi4Num, "✅",
                "Phiếu đã hoàn thành", lblKpi4Sub, "Xem thống kê ↗", "NAV_THONG_KE"));

        main.add(pnlKpis);
        main.add(Box.createVerticalStrut(20));

        // 3. Section: Công việc gần đây
        JPanel sec2Head = new JPanel(new BorderLayout());
        sec2Head.setOpaque(false);
        sec2Head.setAlignmentX(Component.CENTER_ALIGNMENT);
        sec2Head.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        JLabel lblSec2Count = taoNhan("Đang tải...", Font.PLAIN, 12, F_DESC);
        sec2Head.add(taoNhan("Công việc gần đây", Font.BOLD, 15, F_TITLE), BorderLayout.WEST);
        sec2Head.add(lblSec2Count, BorderLayout.EAST);
        main.add(sec2Head);
        main.add(Box.createVerticalStrut(10));

        // Card bảng công việc gần đây
        RoundedPanel cardTable = new RoundedPanel(C_WHITE, 12, F_LINE, new Insets(16, 18, 14, 18));
        cardTable.setLayout(new BoxLayout(cardTable, BoxLayout.Y_AXIS));
        cardTable.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardTable.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel tblTop = new JPanel(new BorderLayout());
        tblTop.setOpaque(false);
        tblTop.add(taoNhan("Phiếu được phân công gần đây", Font.BOLD, 14, F_TITLE), BorderLayout.WEST);
        JLabel lblMoDanhSach = taoNhan("Mở danh sách ↗", Font.BOLD, 12, C_PRIMARY);
        lblMoDanhSach.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblMoDanhSach.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                moPageTheoNavigation("NAV_PHIEU_PHAN_CONG");
            }
        });
        tblTop.add(lblMoDanhSach, BorderLayout.EAST);
        addFull(cardTable, tblTop);
        cardTable.add(Box.createVerticalStrut(10));

        String[] cols = { "Mã phiếu", "Biển số xe", "Nội dung sửa chữa", "Ngày phân công", "Trạng thái" };
        DefaultTableModel modelRecent = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        JTable tblRecent = new JTable(modelRecent);
        tblRecent.setFont(f(Font.PLAIN, 13));
        tblRecent.setRowHeight(38);
        tblRecent.setShowVerticalLines(false);
        tblRecent.setGridColor(F_LINE);
        tblRecent.setSelectionBackground(F_ACCENT_BG);
        tblRecent.setSelectionForeground(C_PRIMARY);
        tblRecent.getTableHeader().setFont(f(Font.BOLD, 12));
        tblRecent.getTableHeader().setForeground(F_LABEL);
        final java.util.List<dao.PhieuTiepNhanSuaChuaDAO.PhieuSuaChuaItem> recentPhieuList = new java.util.ArrayList<>();
        tblRecent.getTableHeader().setBackground(new Color(0xF8FAFC));
        tblRecent.getTableHeader().setPreferredSize(new Dimension(0, 36));
        // Renderer trạng thái badge
        tblRecent.getColumnModel().getColumn(4).setCellRenderer(new ThongKeSuaChuaUI.StatusBadgeRenderer());

        JScrollPane sp = new JScrollPane(tblRecent);
        sp.setBorder(new TraCuuUI.RoundedLineBorder(F_LINE, 8));
        sp.getViewport().setBackground(C_WHITE);
        sp.setPreferredSize(new Dimension(980, 160));
        addFull(cardTable, sp);
        cardTable.add(Box.createVerticalStrut(10));

        JPanel tblFoot = new JPanel(new BorderLayout());
        tblFoot.setOpaque(false);
        JLabel lblTblFootLeft = taoNhan("Đang tải dữ liệu...", Font.PLAIN, 12, F_DESC);
        tblFoot.add(lblTblFootLeft, BorderLayout.WEST);
        JButton btnAll = new RoundedButton("Xem chi tiết", 10, C_WHITE, new Color(0xF1F5F9), F_LINE);
        btnAll.setFont(f(Font.BOLD, 12));
        btnAll.setPreferredSize(new Dimension(110, 34));
        btnAll.addActionListener(e -> {
            int row = tblRecent.getSelectedRow();
            if (row < 0 && tblRecent.getRowCount() > 0) {
                row = 0;
                tblRecent.setRowSelectionInterval(0, 0);
            }
            if (row < 0 || row >= recentPhieuList.size()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một phiếu trong bảng để xem chi tiết.",
                        "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            dao.PhieuTiepNhanSuaChuaDAO.PhieuSuaChuaItem p = recentPhieuList.get(row);
            hienThiDialogChiTietPhieu(p);
        });

        tblRecent.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    btnAll.doClick();
                }
            }
        });
        tblFoot.add(btnAll, BorderLayout.EAST);
        addFull(cardTable, tblFoot);

        main.add(cardTable);
        main.add(Box.createVerticalStrut(18));

        // Load dữ liệu thực từ DB bằng SwingWorker
        controller.PhieuTiepNhanSuaChuaController ktvController = new controller.PhieuTiepNhanSuaChuaController();
        controller.PhieuYeuCauNhapLinhKienController ycCtrl = new controller.PhieuYeuCauNhapLinhKienController();

        new SwingWorker<dao.PhieuTiepNhanSuaChuaDAO.ThongKeKTVData, Void>() {
            @Override
            protected dao.PhieuTiepNhanSuaChuaDAO.ThongKeKTVData doInBackground() {
                return ktvController.layThongKeKTV(null, null);
            }

            @Override
            protected void done() {
                try {
                    dao.PhieuTiepNhanSuaChuaDAO.ThongKeKTVData d = get();
                    int tong = d.tongPhieu(), hoanTat = d.phieuHoanTat(), dangSua = d.phieuDangSua(),
                            choLK = d.phieuChoLinhKien();
                    int choSua = Math.max(0, tong - hoanTat - dangSua - choLK);
                    int canHoanThanh = Math.max(0, tong - hoanTat);
                    lblKpi1Num.setText(String.valueOf(canHoanThanh));
                    lblKpi1Sub.setText(choSua + " chờ sửa · " + dangSua + " đang sửa · " + choLK + " chờ linh kiện");
                    lblKpi2Num.setText(String.valueOf(dangSua));
                    lblKpi2Sub.setText(dangSua + " phiếu đang được xử lý");
                    lblKpi4Num.setText(String.valueOf(hoanTat));
                    lblKpi4Sub.setText(hoanTat + " phiếu đã hoàn tất");
                } catch (Exception ignored) {
                    lblKpi1Num.setText("?");
                    lblKpi2Num.setText("?");
                    lblKpi4Num.setText("?");
                }
            }
        }.execute();

        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                try {
                    return ycCtrl.demYeuCauKTV(enums.TrangThaiYeuCau.CHO_DUYET, null, null);
                } catch (Exception e) {
                    return 0;
                }
            }

            @Override
            protected void done() {
                try {
                    int count = get();
                    lblKpi3Num.setText(String.valueOf(count));
                    lblKpi3Sub.setText(count + " yêu cầu đang chờ chủ cửa hàng duyệt");
                } catch (Exception ignored) {
                    lblKpi3Num.setText("?");
                }
            }
        }.execute();

        new SwingWorker<java.util.List<dao.PhieuTiepNhanSuaChuaDAO.PhieuSuaChuaItem>, Void>() {
            @Override
            protected java.util.List<dao.PhieuTiepNhanSuaChuaDAO.PhieuSuaChuaItem> doInBackground() {
                return ktvController.timPhieuKTV(null, null, null, null);
            }

            @Override
            protected void done() {
                try {
                    java.util.List<dao.PhieuTiepNhanSuaChuaDAO.PhieuSuaChuaItem> list = get();
                    recentPhieuList.clear();
                    recentPhieuList.addAll(list);
                    modelRecent.setRowCount(0);
                    int limit = Math.min(list.size(), 5);
                    for (int i = 0; i < limit; i++) {
                        dao.PhieuTiepNhanSuaChuaDAO.PhieuSuaChuaItem p = list.get(i);
                        String trangThai = switch (p.trangThai() == null ? "" : p.trangThai()) {
                            case "CHO_SUA" -> "Chờ sửa";
                            case "DANG_SUA" -> "Đang sửa";
                            case "CHO_LINH_KIEN" -> "Chờ linh kiện";
                            case "HOAN_TAT" -> "Hoàn tất";
                            default -> p.trangThai();
                        };
                        modelRecent.addRow(new Object[] {
                                p.maPhieu(), p.bienSoXe(),
                                p.yeuCauBanDau() != null && p.yeuCauBanDau().length() > 50
                                        ? p.yeuCauBanDau().substring(0, 50) + "..."
                                        : p.yeuCauBanDau(),
                                DATE_FMT.format(p.ngayTiepNhan()),
                                trangThai
                        });
                    }
                    lblSec2Count.setText(list.size() + " phiếu (hiển thị " + limit + " gần nhất)");
                    lblTblFootLeft.setText(list.size() + " phiếu được phân công");
                } catch (Exception ex) {
                    lblTblFootLeft.setText("Lỗi tải dữ liệu: " + ex.getMessage());
                }
            }
        }.execute();

        // 4. Section: Hai biểu đồ hàng ngang
        JProgressBar pbChoSua = new JProgressBar(0, 10);
        JLabel lblChoSuaVal = taoNhan("0", Font.BOLD, 12, F_TITLE);
        JProgressBar pbDangSua = new JProgressBar(0, 10);
        JLabel lblDangSuaVal = taoNhan("0", Font.BOLD, 12, F_TITLE);
        JProgressBar pbChoLK = new JProgressBar(0, 10);
        JLabel lblChoLKVal = taoNhan("0", Font.BOLD, 12, F_TITLE);
        JProgressBar pbHoanThanh = new JProgressBar(0, 10);
        JLabel lblHoanThanhVal = taoNhan("0", Font.BOLD, 12, F_TITLE);

        JLabel lblDonutCenter = taoNhan("0%", Font.BOLD, 15, F_TITLE);
        JLabel lblLegendHT = taoNhan("0%", Font.BOLD, 11, F_TITLE);
        JLabel lblLegendChoDS = taoNhan("0%", Font.BOLD, 11, F_TITLE);
        JLabel lblLegendCLK = taoNhan("0%", Font.BOLD, 11, F_TITLE);
        int[] donutAngles = new int[] { 0, 0, 0 };
        JPanel[] donutRef = new JPanel[1];

        JPanel pnlCharts = new JPanel(new GridLayout(1, 2, 14, 0));
        pnlCharts.setOpaque(false);
        pnlCharts.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlCharts.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        pnlCharts.add(taoChartPhieuTheoTrangThaiDynamic(pbChoSua, lblChoSuaVal, pbDangSua, lblDangSuaVal, pbChoLK, lblChoLKVal, pbHoanThanh, lblHoanThanhVal));
        pnlCharts.add(taoChartTyLeHoanThanhDynamic(lblDonutCenter, lblLegendHT, lblLegendChoDS, lblLegendCLK, donutAngles, donutRef));

        // Load số liệu 7 ngày gần nhất cho 2 biểu đồ
        new SwingWorker<dao.PhieuTiepNhanSuaChuaDAO.ThongKeKTVData, Void>() {
            @Override
            protected dao.PhieuTiepNhanSuaChuaDAO.ThongKeKTVData doInBackground() {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                java.util.Date den = cal.getTime();
                cal.add(java.util.Calendar.DAY_OF_MONTH, -7);
                java.util.Date tu = cal.getTime();
                return ktvController.layThongKeKTV(tu, den);
            }

            @Override
            protected void done() {
                try {
                    dao.PhieuTiepNhanSuaChuaDAO.ThongKeKTVData d = get();
                    int tong = d.tongPhieu(), hoanTat = d.phieuHoanTat(), dangSua = d.phieuDangSua(), choLK = d.phieuChoLinhKien();
                    int choSua = Math.max(0, tong - hoanTat - dangSua - choLK);
                    int max = Math.max(1, Math.max(Math.max(choSua, dangSua), Math.max(choLK, hoanTat)));

                    pbChoSua.setMaximum(max); pbChoSua.setValue(choSua); lblChoSuaVal.setText(String.valueOf(choSua));
                    pbDangSua.setMaximum(max); pbDangSua.setValue(dangSua); lblDangSuaVal.setText(String.valueOf(dangSua));
                    pbChoLK.setMaximum(max); pbChoLK.setValue(choLK); lblChoLKVal.setText(String.valueOf(choLK));
                    pbHoanThanh.setMaximum(max); pbHoanThanh.setValue(hoanTat); lblHoanThanhVal.setText(String.valueOf(hoanTat));

                    int pctHT = tong > 0 ? (int) Math.round(hoanTat * 100.0 / tong) : 0;
                    int pctChoDS = tong > 0 ? (int) Math.round((choSua + dangSua) * 100.0 / tong) : 0;
                    int pctCLK = tong > 0 ? Math.max(0, 100 - pctHT - pctChoDS) : 0;

                    lblDonutCenter.setText(pctHT + "%");
                    lblLegendHT.setText(pctHT + "%");
                    lblLegendChoDS.setText(pctChoDS + "%");
                    lblLegendCLK.setText(pctCLK + "%");

                    donutAngles[0] = (int) Math.round(pctHT * 3.6);
                    donutAngles[1] = (int) Math.round(pctChoDS * 3.6);
                    donutAngles[2] = Math.max(0, 360 - donutAngles[0] - donutAngles[1]);
                    if (donutRef[0] != null) {
                        donutRef[0].repaint();
                    }
                } catch (Exception ignored) {}
            }
        }.execute();

        main.add(pnlCharts);
        main.add(Box.createVerticalStrut(18));

        // 5. Section: Truy cập nhanh
        JPanel sec4Head = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        sec4Head.setOpaque(false);
        sec4Head.setAlignmentX(Component.CENTER_ALIGNMENT);
        sec4Head.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        sec4Head.add(taoNhan("Truy cập nhanh", Font.BOLD, 15, F_TITLE));
        main.add(sec4Head);
        main.add(Box.createVerticalStrut(10));

        JPanel pnlQuick = new JPanel(new GridLayout(1, 4, 12, 0));
        pnlQuick.setOpaque(false);
        pnlQuick.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlQuick.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        pnlQuick.add(
                taoQuickCard("📋", "Phiếu được phân công", "Xem chi tiết công việc của tôi", "NAV_PHIEU_PHAN_CONG"));
        pnlQuick.add(
                taoQuickCard("🛠️", "Cập nhật phiếu sửa chữa", "Ghi nhận tiến độ và kết quả", "NAV_CAP_NHAT_SUA_CHUA"));
        pnlQuick.add(taoQuickCard("📦", "Yêu cầu cập nhật linh kiện", "Tạo và theo dõi yêu cầu", "NAV_YC_LINH_KIEN"));
        pnlQuick.add(taoQuickCard("💬", "Phản hồi yêu cầu", "Theo dõi phản hồi từ cửa hàng", "NAV_YC_LINH_KIEN"));

        main.add(pnlQuick);
        return main;
    }

    private void hienThiDialogChiTietPhieu(dao.PhieuTiepNhanSuaChuaDAO.PhieuSuaChuaItem p) {
        if (p == null)
            return;
        JDialog dlg = new JDialog(this, "Chi tiết phiếu sửa chữa — " + p.maPhieu(), true);
        dlg.setSize(580, 420);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel pnl = new JPanel();
        pnl.setBackground(Color.WHITE);
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        pnl.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel lblTitle = new JLabel("Thông tin chi tiết phiếu " + p.maPhieu());
        lblTitle.setFont(f(Font.BOLD, 16));
        lblTitle.setForeground(F_TITLE);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnl.add(lblTitle);
        pnl.add(Box.createVerticalStrut(14));

        JPanel grid = new JPanel(new GridLayout(4, 2, 10, 8));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        String stTen = switch (p.trangThai() == null ? "" : p.trangThai()) {
            case "CHO_SUA" -> "Chờ sửa";
            case "DANG_SUA" -> "Đang sửa";
            case "CHO_LINH_KIEN" -> "Chờ linh kiện";
            case "HOAN_TAT" -> "Hoàn tất";
            default -> p.trangThai();
        };

        grid.add(TraCuuUI.infoCell("Mã phiếu", p.maPhieu()));
        grid.add(TraCuuUI.infoCell("Trạng thái", stTen));
        grid.add(TraCuuUI.infoCell("Biển số xe", p.bienSoXe() + (p.hangModel() != null ? " · " + p.hangModel() : "")));
        grid.add(TraCuuUI.infoCell("Ngày tiếp nhận", p.ngayTiepNhan() != null ? DATE_FMT.format(p.ngayTiepNhan()) : "—"));
        grid.add(TraCuuUI.infoCell("Khách hàng", p.tenKhachHang() != null ? p.tenKhachHang() : "—"));
        grid.add(TraCuuUI.infoCell("Số điện thoại", p.soDienThoai() != null ? p.soDienThoai() : "—"));
        grid.add(TraCuuUI.infoCell("Kỹ thuật viên", p.tenKyThuatVien() != null ? p.tenKyThuatVien() : "—"));
        grid.add(TraCuuUI.infoCell("Nội dung yêu cầu", p.yeuCauBanDau() != null ? p.yeuCauBanDau() : "—"));

        pnl.add(grid);
        pnl.add(Box.createVerticalStrut(18));

        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlBtns.setOpaque(false);
        pnlBtns.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnDong = new RoundedButton("Đóng", 10, Color.WHITE, new Color(0xF1F5F9), F_LINE);
        btnDong.setFont(f(Font.BOLD, 12));
        btnDong.setPreferredSize(new Dimension(80, 36));
        btnDong.addActionListener(e -> dlg.dispose());
        pnlBtns.add(btnDong);

        boolean isHoanTat = "HOAN_TAT".equalsIgnoreCase(p.trangThai());
        if (!isHoanTat) {
            JButton btnSua = new RoundedButton("Cập nhật phiếu này ↗", 10, C_PRIMARY, new Color(0x1D4ED8), C_PRIMARY);
            btnSua.setFont(f(Font.BOLD, 12));
            btnSua.setForeground(Color.WHITE);
            btnSua.setPreferredSize(new Dimension(170, 36));
            btnSua.addActionListener(e -> {
                dlg.dispose();
                moPageTheoNavigation("NAV_CAP_NHAT_SUA_CHUA");
                if (capNhatPhieuUI != null) {
                    capNhatPhieuUI.datMaPhieuCanSua(p.maPhieu());
                }
            });
            pnlBtns.add(btnSua);
        } else {
            JButton btnTraCuu = new RoundedButton("Xem lịch sử sửa chữa ↗", 10, C_PRIMARY, new Color(0x1D4ED8), C_PRIMARY);
            btnTraCuu.setFont(f(Font.BOLD, 12));
            btnTraCuu.setForeground(Color.WHITE);
            btnTraCuu.setPreferredSize(new Dimension(185, 36));
            btnTraCuu.addActionListener(e -> {
                dlg.dispose();
                moPageTheoNavigation("NAV_TRA_CUU");
            });
            pnlBtns.add(btnTraCuu);
        }

        pnl.add(pnlBtns);
        dlg.add(pnl, BorderLayout.CENTER);
        dlg.setVisible(true);
    }

    private JPanel taoKpiKtvCard(String badgeText, String countText, Color badgeBg, Color badgeFg,
            String number, String icon, String title, String sub,
            String linkText, String navId) {
        RoundedPanel card = new RoundedPanel(C_WHITE, 12, F_LINE, new Insets(16, 16, 14, 16));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // Top row: Badge + Count
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel lblBadge = new JLabel(" " + badgeText + " ");
        lblBadge.setFont(f(Font.BOLD, 11));
        lblBadge.setForeground(badgeFg);
        lblBadge.setOpaque(true);
        lblBadge.setBackground(badgeBg);
        topRow.add(lblBadge, BorderLayout.WEST);
        topRow.add(taoNhan(countText, Font.PLAIN, 11, F_DESC), BorderLayout.EAST);
        addFull(card, topRow);
        card.add(Box.createVerticalStrut(10));

        // Number row: Large number + Icon
        JPanel numRow = new JPanel(new BorderLayout());
        numRow.setOpaque(false);
        numRow.add(taoNhan(number, Font.BOLD, 28, F_TITLE), BorderLayout.WEST);
        numRow.add(taoNhan(icon, Font.PLAIN, 22, F_TITLE), BorderLayout.EAST);
        addFull(card, numRow);
        card.add(Box.createVerticalStrut(8));

        addFull(card, taoNhan(title, Font.BOLD, 13, F_TITLE));
        card.add(Box.createVerticalStrut(4));
        addFull(card, taoNhan(sub, Font.PLAIN, 11, F_DESC));
        card.add(Box.createVerticalStrut(12));

        JLabel lblLink = taoNhan(linkText, Font.BOLD, 11, C_PRIMARY);
        lblLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblLink.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                moPageTheoNavigation(navId);
            }
        });
        addFull(card, lblLink);

        return card;
    }

    /**
     * Phiên bản động – nhận JLabel từ bên ngoài để SwingWorker cập nhật số liệu
     * thực.
     */
    private JPanel taoKpiKtvCardDynamic(String badgeText, String unit, Color badgeBg, Color badgeFg,
            JLabel lblNumber, String icon, String title, JLabel lblSub,
            String linkText, String navId) {
        RoundedPanel card = new RoundedPanel(C_WHITE, 12, F_LINE, new Insets(16, 16, 14, 16));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // Top row: Badge + unit label
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel lblBadge = new JLabel(" " + badgeText + " ");
        lblBadge.setFont(f(Font.BOLD, 11));
        lblBadge.setForeground(badgeFg);
        lblBadge.setOpaque(true);
        lblBadge.setBackground(badgeBg);
        topRow.add(lblBadge, BorderLayout.WEST);
        topRow.add(taoNhan(unit, Font.PLAIN, 11, F_DESC), BorderLayout.EAST);
        addFull(card, topRow);
        card.add(Box.createVerticalStrut(10));

        // Number row: Dynamic number + Icon
        JPanel numRow = new JPanel(new BorderLayout());
        numRow.setOpaque(false);
        lblNumber.setFont(f(Font.BOLD, 28));
        lblNumber.setForeground(F_TITLE);
        numRow.add(lblNumber, BorderLayout.WEST);
        numRow.add(taoNhan(icon, Font.PLAIN, 22, F_TITLE), BorderLayout.EAST);
        addFull(card, numRow);
        card.add(Box.createVerticalStrut(8));

        addFull(card, taoNhan(title, Font.BOLD, 13, F_TITLE));
        card.add(Box.createVerticalStrut(4));
        lblSub.setFont(f(Font.PLAIN, 11));
        lblSub.setForeground(F_DESC);
        addFull(card, lblSub);
        card.add(Box.createVerticalStrut(12));

        JLabel lblLink = taoNhan(linkText, Font.BOLD, 11, C_PRIMARY);
        lblLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblLink.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                moPageTheoNavigation(navId);
            }
        });
        addFull(card, lblLink);

        return card;
    }

    private JPanel taoChartPhieuTheoTrangThaiDynamic(
            JProgressBar pbChoSua, JLabel lblChoSuaVal,
            JProgressBar pbDangSua, JLabel lblDangSuaVal,
            JProgressBar pbChoLK, JLabel lblChoLKVal,
            JProgressBar pbHoanThanh, JLabel lblHoanThanhVal) {
        RoundedPanel card = new RoundedPanel(C_WHITE, 12, F_LINE, new Insets(16, 18, 14, 18));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(taoNhan("Phiếu theo trạng thái", Font.BOLD, 14, F_TITLE), BorderLayout.WEST);
        top.add(taoNhan("7 ngày gần nhất", Font.PLAIN, 11, F_DESC), BorderLayout.EAST);
        addFull(card, top);
        card.add(Box.createVerticalStrut(14));

        card.add(taoProgressBarRowDynamic("Chờ sửa", pbChoSua, lblChoSuaVal, new Color(0x94A3B8)));
        card.add(Box.createVerticalStrut(8));
        card.add(taoProgressBarRowDynamic("Đang sửa", pbDangSua, lblDangSuaVal, C_PRIMARY));
        card.add(Box.createVerticalStrut(8));
        card.add(taoProgressBarRowDynamic("Chờ linh kiện", pbChoLK, lblChoLKVal, C_WARN));
        card.add(Box.createVerticalStrut(8));
        card.add(taoProgressBarRowDynamic("Hoàn thành", pbHoanThanh, lblHoanThanhVal, new Color(0x1E3A8A)));
        card.add(Box.createVerticalStrut(14));

        JLabel lblLink = taoNhan("Xem chi tiết biểu đồ ↗", Font.BOLD, 11, C_PRIMARY);
        lblLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblLink.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                moPageTheoNavigation("NAV_THONG_KE");
            }
        });
        addFull(card, lblLink);
        return card;
    }

    private JPanel taoProgressBarRowDynamic(String label, JProgressBar pb, JLabel val, Color barColor) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        JLabel lbl = taoNhan(label, Font.PLAIN, 12, F_LABEL);
        lbl.setPreferredSize(new Dimension(84, 18));
        row.add(lbl, BorderLayout.WEST);

        pb.setForeground(barColor);
        pb.setBackground(new Color(0xF1F5F9));
        pb.setBorderPainted(false);
        pb.setPreferredSize(new Dimension(160, 8));
        row.add(pb, BorderLayout.CENTER);

        val.setPreferredSize(new Dimension(24, 18));
        row.add(val, BorderLayout.EAST);
        return row;
    }

    private JPanel taoChartTyLeHoanThanhDynamic(
            JLabel lblDonutCenter, JLabel lblLegendHT, JLabel lblLegendChoDS, JLabel lblLegendCLK,
            int[] donutAngles, JPanel[] outDonutPanel) {
        RoundedPanel card = new RoundedPanel(C_WHITE, 12, F_LINE, new Insets(16, 18, 14, 18));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(taoNhan("Tỷ lệ hoàn thành", Font.BOLD, 14, F_TITLE), BorderLayout.WEST);
        top.add(taoNhan("Tuần này", Font.PLAIN, 11, F_DESC), BorderLayout.EAST);
        addFull(card, top);
        card.add(Box.createVerticalStrut(10));

        JPanel chartContent = new JPanel(new BorderLayout(14, 0));
        chartContent.setOpaque(false);

        // Donut vẽ tỷ lệ động từ DB
        JPanel donut = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2, cy = getHeight() / 2, r = 38, thick = 14;
                g2.setStroke(new BasicStroke(thick, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));

                int startAngle = 90;
                if (donutAngles[0] > 0) {
                    g2.setColor(C_PRIMARY);
                    g2.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, startAngle, -donutAngles[0], Arc2D.OPEN));
                    startAngle -= donutAngles[0];
                }
                if (donutAngles[1] > 0) {
                    g2.setColor(new Color(0x38BDF8));
                    g2.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, startAngle, -donutAngles[1], Arc2D.OPEN));
                    startAngle -= donutAngles[1];
                }
                if (donutAngles[2] > 0) {
                    g2.setColor(C_WARN);
                    g2.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, startAngle, -donutAngles[2], Arc2D.OPEN));
                }
                if (donutAngles[0] == 0 && donutAngles[1] == 0 && donutAngles[2] == 0) {
                    g2.setColor(new Color(0xE2E8F0));
                    g2.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, 0, 360, Arc2D.OPEN));
                }

                g2.setFont(new Font("SansSerif", Font.BOLD, 15));
                g2.setColor(F_TITLE);
                String cText = lblDonutCenter.getText();
                FontMetrics fm = g2.getFontMetrics();
                int tx = cx - fm.stringWidth(cText) / 2;
                g2.drawString(cText, tx, cy + 2);

                g2.setFont(new Font("SansSerif", Font.PLAIN, 8));
                g2.setColor(F_DESC);
                g2.drawString("Hoàn thành", cx - 20, cy + 14);
                g2.dispose();
            }
        };
        donut.setPreferredSize(new Dimension(110, 110));
        if (outDonutPanel != null && outDonutPanel.length > 0) {
            outDonutPanel[0] = donut;
        }
        chartContent.add(donut, BorderLayout.WEST);

        JPanel legend = new JPanel();
        legend.setOpaque(false);
        legend.setLayout(new BoxLayout(legend, BoxLayout.Y_AXIS));
        legend.add(taoLegendRowDynamic(C_PRIMARY, "Hoàn thành", lblLegendHT));
        legend.add(Box.createVerticalStrut(6));
        legend.add(taoLegendRowDynamic(new Color(0x38BDF8), "Chờ sửa / Đang sửa", lblLegendChoDS));
        legend.add(Box.createVerticalStrut(6));
        legend.add(taoLegendRowDynamic(C_WARN, "Chờ linh kiện", lblLegendCLK));
        chartContent.add(legend, BorderLayout.CENTER);
        addFull(card, chartContent);
        card.add(Box.createVerticalStrut(10));

        JLabel lblLink = taoNhan("Xem báo cáo chi tiết ↗", Font.BOLD, 11, C_PRIMARY);
        lblLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblLink.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                moPageTheoNavigation("NAV_THONG_KE");
            }
        });
        addFull(card, lblLink);
        return card;
    }

    private JPanel taoLegendRowDynamic(Color clr, String label, JLabel lblPct) {
        JPanel r = new JPanel(new BorderLayout(8, 0));
        r.setOpaque(false);
        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(clr);
                g.fillRect(0, 4, 8, 8);
            }
        };
        dot.setPreferredSize(new Dimension(8, 16));
        r.add(dot, BorderLayout.WEST);
        r.add(taoNhan(label, Font.PLAIN, 11, F_DESC), BorderLayout.CENTER);
        r.add(lblPct, BorderLayout.EAST);
        return r;
    }

    private JPanel taoQuickCard(String icon, String title, String sub, String navId) {
        RoundedPanel card = new RoundedPanel(C_WHITE, 10, F_LINE, new Insets(12, 14, 12, 14));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(taoNhan(icon, Font.PLAIN, 18, F_TITLE), BorderLayout.WEST);
        top.add(new JLabel(new ArrowUpRightIcon(C_PRIMARY, 12)), BorderLayout.EAST);
        addFull(card, top);
        card.add(Box.createVerticalStrut(8));

        addFull(card, taoNhan(title, Font.BOLD, 12, F_TITLE));
        card.add(Box.createVerticalStrut(3));
        addFull(card, taoNhan(sub, Font.PLAIN, 10, F_DESC));

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                moPageTheoNavigation(navId);
            }
        });
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3B. TRANG THÔNG TIN CÁ NHÂN (Hồ sơ trích xuất từ UserSession)
    // ─────────────────────────────────────────────────────────────────────────
    private JComponent buildProfilePage(NavigationItem item) {
        ScrollableContentPanel pnl = new ScrollableContentPanel();
        pnl.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        heading.setOpaque(false);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel icon = new JLabel("👤");
        icon.setFont(f(Font.PLAIN, 25));
        JLabel title = new JLabel("Thông tin cá nhân");
        title.setFont(f(Font.BOLD, 24));
        title.setForeground(F_TITLE);
        heading.add(icon);
        heading.add(title);
        pnl.add(heading);
        pnl.add(Box.createVerticalStrut(18));

        // Hồ sơ cá nhân nằm trước thông tin quyền theo vai trò.
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(f(Font.PLAIN, 13));
        tabs.setFocusable(false);
        tabs.addTab("Hồ sơ cá nhân", buildPersonalProfileTab());
        tabs.addTab("Thông tin vai trò", buildRoleInformationTab());
        tabs.setAlignmentX(Component.CENTER_ALIGNMENT);
        tabs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 620));
        pnl.add(tabs);

        JScrollPane scroll = new JScrollPane(pnl);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }

    private JComponent buildPersonalProfileTab() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(18, 0, 0, 0));
        content.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel profile = new JPanel(new BorderLayout(20, 0));
        profile.setBackground(C_WHITE);
        profile.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(F_LINE),
                new EmptyBorder(20, 22, 20, 22)));
        profile.setAlignmentX(Component.LEFT_ALIGNMENT);

        String fullName = nhanVien.getHoTen() == null ? "Người dùng" : nhanVien.getHoTen();
        String initial = fullName.isBlank() ? "U" : fullName.trim().substring(0, 1).toUpperCase();
        JLabel avatar = new JLabel(initial, SwingConstants.CENTER);
        avatar.setFont(f(Font.BOLD, 32));
        avatar.setForeground(C_PRIMARY);
        avatar.setOpaque(true);
        avatar.setBackground(C_PRIMARY_BG);
        avatar.setPreferredSize(new Dimension(76, 76));
        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        identity.add(avatar);
        identity.add(Box.createVerticalStrut(12));
        JLabel name = new JLabel(fullName);
        name.setFont(f(Font.BOLD, 17));
        name.setForeground(F_TITLE);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        identity.add(name);
        identity.add(Box.createVerticalStrut(4));
        JLabel code = new JLabel(nhanVien.getMaNV() == null ? "" : nhanVien.getMaNV());
        code.setFont(f(Font.PLAIN, 12));
        code.setForeground(C_TEXT_SUBTLE);
        code.setAlignmentX(Component.CENTER_ALIGNMENT);
        identity.add(code);
        profile.add(identity, BorderLayout.WEST);

        JPanel fields = new JPanel(new GridLayout(3, 3, 12, 12));
        fields.setOpaque(false);
        String username = UserSession.getInstance().getTenDangNhap();
        if (username == null || username.isBlank())
            username = "Chưa có tài khoản";
        fields.add(createInfoField("Họ và tên", fullName));
        fields.add(createInfoField("Mã nhân viên", nhanVien.getMaNV()));
        fields.add(createInfoField("Tên đăng nhập", username));
        fields.add(createInfoField("Số điện thoại", nhanVien.getSoDienThoai()));
        fields.add(createInfoField("CCCD", nhanVien.getCCCD()));
        fields.add(createInfoField("Ngày sinh",
                nhanVien.getNgaySinh() == null ? null : nhanVien.getNgaySinh().format(LOCAL_DATE_FMT)));
        fields.add(createInfoField("Ngày vào làm",
                nhanVien.getNgayVaoLam() == null ? null : nhanVien.getNgayVaoLam().format(LOCAL_DATE_FMT)));
        fields.add(createInfoField("Tuổi", String.valueOf(nhanVien.getTuoi())));
        fields.add(createInfoField("Địa chỉ", nhanVien.getDiaChi()));
        profile.add(fields, BorderLayout.CENTER);
        profile.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        JComponent profileGap = (JComponent) Box.createVerticalStrut(14);
        profileGap.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(profile);
        content.add(profileGap);

        return content;
    }

    private JComponent buildRoleInformationTab() {
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(18, 0, 0, 0));
        JLabel intro = new JLabel("Vai trò hiện tại: " + AuthController.layTenVaiTro(nhanVien.getVaiTro()));
        intro.setFont(f(Font.BOLD, 15));
        intro.setForeground(F_TITLE);
        content.add(intro, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(new Object[] { "Chức năng", "Phạm vi tác vụ" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (NavigationItem nav : authController.layDanhSachNavigationTheoRole(nhanVien.getVaiTro())) {
            if ("NAV_DASHBOARD".equals(nav.getId()))
                continue;
            model.addRow(new Object[] { nav.getTieuDe(), moTaQuyen(nav.getId()) });
        }
        JTable table = new JTable(model);
        table.setFont(f(Font.PLAIN, 13));
        table.setForeground(F_LABEL);
        table.setRowHeight(44);
        table.setShowVerticalLines(false);
        table.setGridColor(F_LINE);
        table.setSelectionBackground(F_ACCENT_BG);
        table.getTableHeader().setFont(f(Font.BOLD, 12));
        table.getTableHeader().setForeground(F_LABEL);
        table.getTableHeader().setBackground(new Color(0xF7F9FC));
        table.getTableHeader().setPreferredSize(new Dimension(0, 42));
        table.getColumnModel().getColumn(0).setPreferredWidth(240);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(F_LINE));
        tableScroll.getViewport().setBackground(C_WHITE);
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(C_WHITE);
        card.setBorder(BorderFactory.createLineBorder(F_LINE));
        card.add(tableScroll, BorderLayout.CENTER);
        content.add(card, BorderLayout.CENTER);
        return content;
    }

    private String moTaQuyen(String id) {
        return switch (id) {
            case "NAV_THONG_KE" -> "Xem thống kê và xuất báo cáo hoạt động cửa hàng.";
            case "NAV_QUAN_LY_DICH_VU" -> "Quản lý danh mục, đơn giá và trạng thái cung cấp dịch vụ.";
            case "NAV_LINH_KIEN_KHO" -> "Quản lý linh kiện, theo dõi tồn kho và xác nhận nhập kho.";
            case "NAV_YC_LINH_KIEN" -> nhanVien.getVaiTro() == VaiTro.CHU_CUA_HANG
                    ? "Xem, duyệt hoặc từ chối yêu cầu cập nhật linh kiện."
                    : "Xem và cập nhật yêu cầu linh kiện gắn với công việc được giao.";
            case "NAV_KHUYEN_MAI" -> "Quản lý các chương trình khuyến mãi của cửa hàng.";
            case "NAV_NHAN_VIEN" -> "Tìm kiếm và quản lý hồ sơ nhân viên.";
            case "NAV_TAI_KHOAN" -> "Tạo tài khoản, đổi mật khẩu và quản lý tài khoản nhân viên.";
            case "NAV_TRA_CUU" -> nhanVien.getVaiTro() == VaiTro.CHU_CUA_HANG
                    ? "Tra cứu linh kiện, khách hàng và lịch sử sửa chữa."
                    : "Tra cứu theo phạm vi nghiệp vụ của vai trò.";
            case "NAV_THONG_TIN_CA_NHAN" -> "Xem thông tin tài khoản và hồ sơ cá nhân.";
            case "NAV_KHACH_HANG_XE" -> "Quản lý hồ sơ khách hàng và xe.";
            case "NAV_PHIEU_TIEP_NHAN" -> "Tiếp nhận xe và lập phiếu sửa chữa.";
            case "NAV_HOA_DON" -> "Lập hóa đơn và ghi nhận thanh toán.";
            case "NAV_DOI_TRA" -> "Lập phiếu đổi trả theo quy định.";
            case "NAV_PHIEU_PHAN_CONG" -> "Xem phiếu sửa chữa được phân công.";
            case "NAV_CAP_NHAT_SUA_CHUA" -> "Cập nhật nội dung và trạng thái phiếu được giao.";
            default -> "Thực hiện chức năng theo quyền của vai trò hiện tại.";
        };
    }

    private JPanel createInfoField(String label, String value) {
        JPanel f = new JPanel();
        f.setLayout(new BoxLayout(f, BoxLayout.Y_AXIS));
        f.setBackground(new Color(0xF8FAFC));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(8, 12, 8, 12)));

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

        JPanel pnlTitles = new JPanel(new BorderLayout());
        pnlTitles.setOpaque(false);
        JLabel lblTitle = new JLabel(item.getIcon() + "  " + item.getTieuDe());
        lblTitle.setFont(f(Font.BOLD, 22));
        lblTitle.setForeground(C_TEXT_HEAD);
        pnlTitles.add(lblTitle, BorderLayout.CENTER);
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
                new EmptyBorder(4, 10, 4, 10)));
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
            model.setColumnIdentifiers(new String[] { "Thông báo" });
            model.addRow(new Object[] { "Không thể kết nối đến cơ sở dữ liệu. Vui lòng kiểm tra Docker SQL Server." });
            return model;
        }

        try (Connection con = DbUtil.getConnection()) {
            switch (navId) {
                case "NAV_QUAN_LY_DICH_VU" -> {
                    model.setColumnIdentifiers(new String[] { "STT", "Mã DV", "Tên dịch vụ sửa chữa", "Đơn vị tính",
                            "Đơn giá (VNĐ)", "Trạng thái" });
                    String sql = "SELECT maDV, tenDV, donViTinh, donGia, dangCungCap FROM DichVu ORDER BY maDV";
                    try (PreparedStatement ps = con.prepareStatement(sql);
                            ResultSet rs = ps.executeQuery()) {
                        int stt = 1;
                        while (rs.next()) {
                            model.addRow(new Object[] {
                                    stt++, rs.getString("maDV"), rs.getString("tenDV"), rs.getString("donViTinh"),
                                    MONEY_FMT.format(rs.getDouble("donGia")),
                                    rs.getBoolean("dangCungCap") ? "Đang cung cấp" : "Tạm ngưng"
                            });
                        }
                    }
                }
                case "NAV_LINH_KIEN_KHO" -> {
                    model.setColumnIdentifiers(new String[] { "STT", "Mã LK", "Tên linh kiện", "Đơn vị tính",
                            "Đơn giá (VNĐ)", "Tồn kho", "Trạng thái" });
                    String sql = "SELECT maLK, tenLK, donViTinh, donGia, soLuongTon, dangKinhDoanh FROM LinhKien ORDER BY maLK";
                    try (PreparedStatement ps = con.prepareStatement(sql);
                            ResultSet rs = ps.executeQuery()) {
                        int stt = 1;
                        while (rs.next()) {
                            int ton = rs.getInt("soLuongTon");
                            boolean kd = rs.getBoolean("dangKinhDoanh");
                            model.addRow(new Object[] {
                                    stt++, rs.getString("maLK"), rs.getString("tenLK"), rs.getString("donViTinh"),
                                    MONEY_FMT.format(rs.getDouble("donGia")), ton,
                                    kd ? (ton > 0 ? "Còn hàng" : "Hết hàng") : "Ngừng kinh doanh"
                            });
                        }
                    }
                }
                case "NAV_KHACH_HANG_XE" -> {
                    model.setColumnIdentifiers(new String[] { "STT", "Mã KH", "Họ và tên khách hàng", "Số điện thoại",
                            "Ghi chú", "Hạng thành viên" });
                    String sql = "SELECT TOP 50 maKH, hoTen, soDienThoai, ghiChu, thanhVienThanThiet FROM KhachHang ORDER BY maKH";
                    try (PreparedStatement ps = con.prepareStatement(sql);
                            ResultSet rs = ps.executeQuery()) {
                        int stt = 1;
                        while (rs.next()) {
                            String gc = rs.getString("ghiChu");
                            boolean tv = rs.getBoolean("thanhVienThanThiet");
                            model.addRow(new Object[] {
                                    stt++, rs.getString("maKH"), rs.getString("hoTen"), rs.getString("soDienThoai"),
                                    gc != null ? gc : "—",
                                    tv ? "Thân thiết" : "Tiêu chuẩn"
                            });
                        }
                    }
                }
                case "NAV_PHIEU_TIEP_NHAN", "NAV_PHIEU_PHAN_CONG", "NAV_CAP_NHAT_SUA_CHUA" -> {
                    model.setColumnIdentifiers(new String[] { "STT", "Mã phiếu", "Biển số xe", "Thời gian tiếp nhận",
                            "Yêu cầu sửa chữa", "KTV phụ trách", "Trạng thái" });
                    String sql = "SELECT TOP 50 p.maPhieu, x.bienSoXe, p.ngayGioTiepNhan, p.yeuCauBanDau, nv.hoTen AS tenKtv, p.trangThai "
                            +
                            "FROM PhieuTiepNhanSuaChua p " +
                            "LEFT JOIN Xe x ON p.maXe = x.maXe " +
                            "LEFT JOIN NhanVien nv ON p.maNVSuaChua = nv.maNV " +
                            "ORDER BY p.ngayGioTiepNhan DESC";
                    try (PreparedStatement ps = con.prepareStatement(sql);
                            ResultSet rs = ps.executeQuery()) {
                        int stt = 1;
                        while (rs.next()) {
                            String bienSo = rs.getString("bienSoXe");
                            if (bienSo == null)
                                bienSo = "N/A";
                            java.sql.Timestamp tg = rs.getTimestamp("ngayGioTiepNhan");
                            String gio = tg != null ? DATE_FMT.format(tg) : "N/A";
                            String ktv = rs.getString("tenKtv");
                            if (ktv == null)
                                ktv = "Chưa phân công";
                            model.addRow(new Object[] {
                                    stt++, rs.getString("maPhieu"), bienSo, gio, rs.getString("yeuCauBanDau"), ktv,
                                    rs.getString("trangThai")
                            });
                        }
                    }
                }
                case "NAV_HOA_DON", "NAV_THONG_KE" -> {
                    model.setColumnIdentifiers(new String[] { "STT", "Mã HĐ", "Mã phiếu sửa", "Thời gian lập",
                            "Phương thức", "Tổng tiền thanh toán (VNĐ)", "Trạng thái" });
                    String sql = "SELECT TOP 50 h.maHoaDon, h.maPhieu, h.ngayLap, h.phuongThucTT, v.tongThanhToan, h.trangThaiThanhToan "
                            +
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
                            model.addRow(new Object[] {
                                    stt++, rs.getString("maHoaDon"), rs.getString("maPhieu"), ngayStr,
                                    pt != null ? pt : "TIEN_MAT",
                                    MONEY_FMT.format(rs.getDouble("tongThanhToan")), rs.getString("trangThaiThanhToan")
                            });
                        }
                    }
                }
                case "NAV_NHAN_VIEN", "NAV_TAI_KHOAN" -> {
                    model.setColumnIdentifiers(new String[] { "STT", "Mã NV", "Họ và tên", "Số điện thoại", "Vai trò",
                            "Ngày vào làm", "Tài khoản" });
                    String sql = "SELECT maNV, hoTen, soDienThoai, vaiTro, ngayVaoLam FROM NhanVien ORDER BY maNV";
                    try (PreparedStatement ps = con.prepareStatement(sql);
                            ResultSet rs = ps.executeQuery()) {
                        int stt = 1;
                        while (rs.next()) {
                            java.sql.Date nvl = rs.getDate("ngayVaoLam");
                            String ngay = nvl != null ? nvl.toLocalDate().format(LOCAL_DATE_FMT) : "N/A";
                            String vtStr = rs.getString("vaiTro");
                            VaiTro vt = vtStr != null ? VaiTro.valueOf(vtStr) : null;
                            model.addRow(new Object[] {
                                    stt++, rs.getString("maNV"), rs.getString("hoTen"), rs.getString("soDienThoai"),
                                    AuthController.layTenVaiTro(vt), ngay, rs.getString("maNV")
                            });
                        }
                    }
                }
                default -> {
                    model.setColumnIdentifiers(new String[] { "Thông tin phân hệ", "Ghi chú" });
                    model.addRow(new Object[] {
                            "Phân hệ " + navId + " đang kết nối cơ sở dữ liệu thời gian thực.",
                            "Tài khoản đang đăng nhập: " + nhanVien.getHoTen() + " ("
                                    + AuthController.layTenVaiTro(nhanVien.getVaiTro()) + ")"
                    });
                }
            }
        } catch (Exception e) {
            model.setColumnIdentifiers(new String[] { "Lỗi truy vấn CSDL" });
            model.addRow(new Object[] { e.getMessage() });
        }

        if (model.getRowCount() == 0) {
            model.addRow(new Object[] { "Chưa có dữ liệu nào trong cơ sở dữ liệu." });
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
    // ───────────────────────────────────────────────────────────────────────── //
    // ─────────────────────────────────────────────────────────────────────────
    // LOGO COMPONENT (Vẽ ảnh gốc độ nét cao với Bicubic Antialiasing & giữ đúng tỷ
    // lệ)
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
                    g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                            RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
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
        if (rawLogoImage != null)
            return rawLogoImage;
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

    /** Panel bo góc tùy chỉnh: nền + viền vẽ bằng Graphics2D, padding tùy ý. */
    public static class RoundedPanel extends JPanel {
        private final int arc;
        private final Color bgFill;
        private final Color strokeClr;

        public RoundedPanel(Color bgFill, int arc, Color strokeClr, Insets pad) {
            super();
            this.bgFill = bgFill;
            this.arc = arc;
            this.strokeClr = strokeClr;
            setOpaque(false);
            if (pad != null)
                setBorder(new EmptyBorder(pad));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (bgFill != null) {
                g2.setColor(bgFill);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            if (strokeClr != null) {
                g2.setColor(strokeClr);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Cột biểu đồ doanh thu: bề rộng 40px, bo góc trên 6px, cao đặt lại được. */
    public static class CotBieuDo extends JPanel {
        private int chieuCao;
        private Color mau;

        public CotBieuDo(Color mau) {
            this.mau = mau;
            this.chieuCao = 8;
            setOpaque(false);
            setPreferredSize(new Dimension(40, chieuCao));
            setMaximumSize(new Dimension(40, chieuCao));
        }

        public void datChieuCao(int h) {
            this.chieuCao = Math.max(4, h);
            setPreferredSize(new Dimension(40, chieuCao));
            setMaximumSize(new Dimension(40, chieuCao));
            revalidate();
            repaint();
        }

        public void datMau(Color mau) {
            this.mau = mau;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(mau);
            java.awt.geom.RoundRectangle2D top = new java.awt.geom.RoundRectangle2D.Float(
                    0, 0, w, h + 12, 12, 12);
            java.awt.geom.Rectangle2D bottom = new java.awt.geom.Rectangle2D.Double(0, 6, w, h);
            java.awt.geom.Area shape = new java.awt.geom.Area(top);
            shape.intersect(new java.awt.geom.Area(bottom));
            g2.fill(shape);
            g2.dispose();
        }
    }

    /**
     * Thanh cơ cấu thanh toán 9px bo tròn 5px: chia theo tỉ lệ chuyển khoản / tiền
     * mặt.
     */
    public static class ThanhCoCauPanel extends JPanel {
        private double tiLeChuyenKhoan = 0.5;

        public ThanhCoCauPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(200, 9));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 9));
        }

        public void datTiLe(double tiLe) {
            this.tiLeChuyenKhoan = Math.max(0, Math.min(1, tiLe));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int gap = 3;
            int wCk = (int) Math.round((w - gap) * tiLeChuyenKhoan);

            Shape clip = new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, h, h);
            g2.setClip(clip);
            g2.setColor(C_PRIMARY);
            g2.fillRect(0, 0, wCk, h);
            g2.setColor(F_TEAL);
            g2.fillRect(wCk + gap, 0, w - wCk - gap, h);
            g2.dispose();
        }
    }

    // ── Icon vector vẽ tay (thay cho SVG của Figma) ──────────────────────────

    /** Mũi tên chéo lên phải (arrow-up-right) – dùng trong các liên kết ↗. */
    public static class ArrowUpRightIcon implements Icon {
        private final Color color;
        private final int size;

        public ArrowUpRightIcon(Color color, int size) {
            this.color = color;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float m = size * 0.28f;
            g2.drawLine(Math.round(m), size - Math.round(m), size - Math.round(m), Math.round(m));
            g2.drawLine(size - Math.round(m), Math.round(m), size - Math.round(m) - size / 3, Math.round(m));
            g2.drawLine(size - Math.round(m), Math.round(m), size - Math.round(m), Math.round(m) + size / 3);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    /** Mũi tên phải (chevron-right) – mũi tên cuối dòng bảng. */
    public static class ChevronRightIcon implements Icon {
        private final Color color;
        private final int size;

        public ChevronRightIcon(Color color, int size) {
            this.color = color;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(size / 3, size / 3, (size * 2) / 3, size / 2);
            g2.drawLine((size * 2) / 3, size / 2, size / 3, (size * 2) / 3);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    /** Tròn hỏi Help – vòng tròn + dấu chấm hỏi. */
    public static class HelpIcon implements Icon {
        private final Color color;
        private final int size;

        public HelpIcon(Color color, int size) {
            this.color = color;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(1, 1, size - 3, size - 3);
            Path2D question = new Path2D.Double();
            question.moveTo(size * 0.36, size * 0.38);
            question.curveTo(size * 0.36, size * 0.18, size * 0.67, size * 0.18, size * 0.67, size * 0.39);
            question.curveTo(size * 0.67, size * 0.55, size * 0.50, size * 0.55, size * 0.50, size * 0.70);
            g2.draw(question);
            int d = Math.max(2, size / 8);
            g2.fillOval(size / 2 - d / 2, size * 3 / 4 - d / 2, d, d);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    /** Icon info – vòng tròn + chữ i. */
    public static class InfoIcon implements Icon {
        private final Color color;
        private final int size;

        public InfoIcon(Color color, int size) {
            this.color = color;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(1, 1, size - 3, size - 3);
            int d = Math.max(2, size / 8);
            g2.fillOval(size / 2 - d / 2, size / 4 - d / 2, d, d);
            g2.drawLine(size / 2, size / 2, size / 2, size * 2 / 3);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    /** Icon đăng xuất: cửa ra + mũi tên hướng ra ngoài. */
    public static class LogoutIcon implements Icon {
        private final Color color;
        private final int size;

        public LogoutIcon(Color color, int size) {
            this.color = color;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Khung cửa (nửa trái)
            g2.drawArc(1, 2, size - 4, size - 4, 90, 180);
            g2.drawLine(size / 2 - 1, 2, size / 2 - 1, size - 2);
            // Mũi tên ra ngoài phải
            g2.drawLine(size / 2, size / 2, size - 1, size / 2);
            g2.drawLine(size - size / 3, size / 2 - size / 5, size - 1, size / 2);
            g2.drawLine(size - size / 3, size / 2 + size / 5, size - 1, size / 2);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    /** Icon lịch (calendar-days). */
    public static class CalendarIcon implements Icon {
        private final Color color;
        private final int size;

        public CalendarIcon(Color color, int size) {
            this.color = color;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect(1, 3, size - 3, size - 4, 4, 4);
            g2.drawLine(4, 1, 4, 6);
            g2.drawLine(size - 5, 1, size - 5, 6);
            g2.drawLine(1, 7, size - 2, 7);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    /** Chấm tròn nhỏ làm marker trong legend. */
    public static class DotIcon implements Icon {
        private final Color color;
        private final int size;

        public DotIcon(Color color, int size) {
            this.color = color;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x, y, size, size);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

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

    private record HeaderSuggestion(String icon, String title, String subtitle, String navId,
            String query, boolean linhKien, boolean navigationOnly) { }

    private void hienThiGoiYTimKiem(String query) {
        String text = query == null ? "" : query.trim();
        String key = chuanHoaTimKiem(text);
        if (key.isBlank()) {
            searchBox.showSuggestions(List.of());
            return;
        }
        List<HeaderSuggestion> pageMatches = new ArrayList<>();
        for (NavigationItem item : authController.layDanhSachNavigationTheoRole(nhanVien.getVaiTro())) {
            String title = chuanHoaTimKiem(item.getTieuDe());
            if (title.contains(key) || key.contains(title)) {
                pageMatches.add(new HeaderSuggestion(item.getIcon(), item.getTieuDe(), "Mở màn hình",
                        item.getId(), "", false, true));
            }
        }
        new SwingWorker<List<HeaderSuggestion>, Void>() {
            @Override protected List<HeaderSuggestion> doInBackground() {
                List<HeaderSuggestion> results = new ArrayList<>();
                results.addAll(pageMatches);
                java.text.NumberFormat money = java.text.NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));
                try {
                    for (DichVu dv : traCuuController.timDichVu(text, "Tất cả").stream().limit(5).toList()) {
                        results.add(new HeaderSuggestion("🔧", dv.getMaDV() + " · " + dv.getTenDV(),
                                "Dịch vụ · " + money.format(dv.getDonGia()) + " VNĐ / " + dv.getDonViTinh(),
                                nhanVien.getVaiTro() == VaiTro.CHU_CUA_HANG ? "NAV_QUAN_LY_DICH_VU" : "NAV_TRA_CUU",
                                dv.getMaDV(), false, false));
                    }
                } catch (RuntimeException ignored) { }
                if (nhanVien.getVaiTro() != VaiTro.TIEP_NHAN_VIEN) {
                    try {
                        for (LinhKien lk : traCuuController.timLinhKien(text, "Tất cả").stream().limit(5).toList()) {
                            results.add(new HeaderSuggestion("📦", lk.getMaLK() + " · " + lk.getTenLK(),
                                    "Linh kiện · " + money.format(lk.getDonGia()) + " VNĐ / " + lk.getDonViTinh(),
                                    nhanVien.getVaiTro() == VaiTro.CHU_CUA_HANG ? "NAV_LINH_KIEN_KHO" : "NAV_TRA_CUU",
                                    lk.getMaLK(), true, false));
                        }
                    } catch (RuntimeException ignored) { }
                }
                return results;
            }

            @Override protected void done() {
                if (!text.equals(searchBox.getQuery())) return;
                try { searchBox.showSuggestions(get()); }
                catch (Exception ex) { searchBox.showSuggestions(pageMatches); }
            }
        }.execute();
    }

    private void chonGoiYTimKiem(HeaderSuggestion suggestion) {
        if (suggestion == null) return;
        moPageTheoNavigation(suggestion.navId());
        if (suggestion.navigationOnly()) return;
        if (nhanVien.getVaiTro() == VaiTro.CHU_CUA_HANG) {
            if (suggestion.linhKien() && quanLyLinhKienCCHUI != null)
                quanLyLinhKienCCHUI.timKiemTuHeader(suggestion.query());
            else if (!suggestion.linhKien() && quanLyDichVuCCHUI != null)
                quanLyDichVuCCHUI.timKiemTuHeader(suggestion.query());
        } else if (traCuuUI != null) {
            traCuuUI.timKiemTuHeader(suggestion.linhKien(), suggestion.query());
        }
    }

    private static String chuanHoaTimKiem(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
        return normalized.replace('đ', 'd');
    }

    public static class HeaderSearchBox extends JPanel {
        private final JTextField txtInput;
        private final JPopupMenu popup = new JPopupMenu();
        private final DefaultListModel<HeaderSuggestion> suggestionModel = new DefaultListModel<>();
        private final JList<HeaderSuggestion> suggestionList = new JList<>(suggestionModel);
        private Consumer<String> onSearch;
        private Consumer<HeaderSuggestion> onSelect;

        public HeaderSearchBox(String placeholder) {
            setLayout(new BorderLayout(8, 0));
            setOpaque(false);
            setPreferredSize(new Dimension(244, 36));
            setMinimumSize(new Dimension(140, 36));
            setMaximumSize(new Dimension(300, 36));
            setBorder(new EmptyBorder(2, 14, 2, 14));

            JLabel lblSearchIcon = new JLabel(new SearchIcon(F_DESC, 16));
            lblSearchIcon.setPreferredSize(new Dimension(16, 16));
            lblSearchIcon.setHorizontalAlignment(SwingConstants.CENTER);
            lblSearchIcon.setVerticalAlignment(SwingConstants.CENTER);
            add(lblSearchIcon, BorderLayout.WEST);

            txtInput = new JTextField();
            txtInput.putClientProperty("JTextField.placeholderText", placeholder);
            txtInput.setFont(f(Font.PLAIN, 12));
            txtInput.setForeground(F_TITLE);
            txtInput.setBorder(null);
            txtInput.setOpaque(false);
            add(txtInput, BorderLayout.CENTER);

            suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            suggestionList.setFixedCellHeight(48);
            suggestionList.setFont(f(Font.PLAIN, 12));
            suggestionList.setCellRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                        boolean selected, boolean focus) {
                    JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                    if (value instanceof HeaderSuggestion item) {
                        label.setText("<html>" + item.icon() + " &nbsp; <b>" + item.title()
                                + "</b><br><span style='color:#7A889C'>" + item.subtitle() + "</span></html>");
                    }
                    label.setBorder(new EmptyBorder(3, 10, 3, 10));
                    return label;
                }
            });
            JScrollPane listScroll = new JScrollPane(suggestionList);
            listScroll.setBorder(BorderFactory.createLineBorder(F_LINE));
            popup.setLayout(new BorderLayout());
            popup.add(listScroll, BorderLayout.CENTER);
            suggestionList.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 1) chonGoiY();
                }
            });
            txtInput.addActionListener(e -> {
                if (popup.isVisible()) chonGoiY();
                else if (onSearch != null) onSearch.accept(txtInput.getText());
            });
            txtInput.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override public void keyPressed(java.awt.event.KeyEvent e) {
                    if (!popup.isVisible()) return;
                    if (suggestionModel.isEmpty()) {
                        if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) popup.setVisible(false);
                        return;
                    }
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN) {
                        suggestionList.setSelectedIndex(Math.min(suggestionList.getSelectedIndex() + 1, suggestionModel.size() - 1));
                        suggestionList.ensureIndexIsVisible(suggestionList.getSelectedIndex()); e.consume();
                    } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_UP) {
                        suggestionList.setSelectedIndex(Math.max(0, suggestionList.getSelectedIndex() - 1));
                        suggestionList.ensureIndexIsVisible(suggestionList.getSelectedIndex()); e.consume();
                    } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                        popup.setVisible(false); e.consume();
                    }
                }
            });
        }

        public void setHandlers(Consumer<String> onSearch, Consumer<HeaderSuggestion> onSelect) {
            this.onSearch = onSearch;
            this.onSelect = onSelect;
        }

        public String getQuery() { return txtInput.getText().trim(); }

        public void showSuggestions(List<HeaderSuggestion> matches) {
            suggestionModel.clear();
            for (HeaderSuggestion item : matches) suggestionModel.addElement(item);
            if (matches.isEmpty()) {
                popup.removeAll();
                JLabel empty = new JLabel(txtInput.getText().isBlank()
                        ? "Nhập từ khóa để tìm màn hình" : "Không tìm thấy màn hình phù hợp");
                empty.setBorder(new EmptyBorder(10, 12, 10, 12));
                popup.add(empty, BorderLayout.CENTER);
                popup.setPreferredSize(new Dimension(Math.max(getWidth(), 260), 42));
            } else {
                if (popup.getComponentCount() == 0 || !(popup.getComponent(0) instanceof JScrollPane)) {
                    popup.removeAll();
                    JScrollPane scroll = new JScrollPane(suggestionList);
                    scroll.setBorder(BorderFactory.createLineBorder(F_LINE));
                    popup.add(scroll, BorderLayout.CENTER);
                }
                suggestionList.setSelectedIndex(0);
                int height = Math.min(48 * matches.size(), 288) + 2;
                popup.setPreferredSize(new Dimension(Math.max(getWidth(), 260), height));
            }
            popup.show(this, 0, getHeight());
            txtInput.requestFocusInWindow();
        }

        private void chonGoiY() {
            HeaderSuggestion selected = suggestionList.getSelectedValue();
            if (selected != null && onSelect != null) {
                popup.setVisible(false);
                onSelect.accept(selected);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Nền #F4F7FB bo góc 10px, không viền (Figma 83:7725)
            g2.setColor(F_BG);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Kính lúp 16px; JLabel căn giữa Icon theo chiều dọc trong header. */
    public static class SearchIcon implements Icon {
        private final Color color;
        private final int size;

        public SearchIcon(Color color, int size) {
            this.color = color;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(2, 2, 9, 9);
            g2.drawLine(10, 10, size - 1, size - 1);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
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
