package ui;

import controller.ThongKeTiepNhanController;
import dao.ThongKeTiepNhanDao.PhieuRow;
import dao.ThongKeTiepNhanDao.PhieuTonDongRow;
import dao.ThongKeTiepNhanDao.SoSanhNamData;
import dao.ThongKeTiepNhanDao.SoSanhThangData;
import dao.ThongKeTiepNhanDao.ThongKeData;
import dao.ThongKeTiepNhanDao.TinhHinhXuLyData;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static ui.MainFrame.*;

/**
 * Màn hình Thống kê tiếp nhận sửa chữa (CarZone).
 * Bố cục 3 phần hoàn chỉnh:
 * 1. THỐNG KÊ TỔNG QUAN (Bộ lọc chọn ngày, KPI, Biểu đồ xu hướng, Biểu đồ trạng thái, Bảng chi tiết)
 * 2. SO SÁNH TÌNH HÌNH TIẾP NHẬN (Dashboard A: Tháng này vs Tháng trước, Dashboard B: Năm nay vs Năm trước)
 * 3. TÌNH HÌNH XỬ LÝ PHIẾU TIẾP NHẬN (4 Card trạng thái, Thanh phân bố tỷ trọng, Nhận xét, Bảng tồn đọng lâu)
 */
public class ThongKeTiepNhanUI extends JPanel {

    private final ThongKeTiepNhanController controller = new ThongKeTiepNhanController();
    private static final DecimalFormat PCT_FMT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat INT_FMT = new DecimalFormat("#,###");

    // ─────────────────────────────────────────────────────────────────────────
    // CONTROLS PHẦN 1: BỘ LỌC & TỔNG QUAN
    // ─────────────────────────────────────────────────────────────────────────
    private JComboBox<String> cboKyThongKe;
    private DatePickerField dpTuNgay;
    private DatePickerField dpDenNgay;
    private JComboBox<String> cboTrangThai;

    private JLabel lblTongPhieuVal;
    private JLabel lblSoXeVal;
    private JLabel lblKhachMoiVal;
    private JLabel lblChoXuLyVal;

    private ChartBarPanel pnlBarChart;
    private ChartDonutPanel pnlDonutChart;
    private DefaultTableModel modelTable;
    private JTable tblChiTiet;

    // ─────────────────────────────────────────────────────────────────────────
    // CONTROLS PHẦN 2: SO SÁNH THEO THỜI GIAN
    // ─────────────────────────────────────────────────────────────────────────
    // Dashboard A: Tháng này vs Tháng trước
    private JLabel lblThangNayVal;
    private JLabel lblThangTruocVal;
    private JLabel lblChenhLechThangVal;
    private JLabel lblTyLeThangVal;
    private JLabel lblLuotKhachThangVal;
    private JLabel lblSoXeThangVal;
    private MonthCompareBarChart pnlMonthChart;
    private JLabel lblNhanXetThang;

    // Dashboard B: Năm nay vs Năm trước
    private JLabel lblNamNayVal;
    private JLabel lblNamTruocVal;
    private JLabel lblChenhLechNamVal;
    private JLabel lblTyLeNamVal;
    private JLabel lblLuotKhachNamVal;
    private JLabel lblSoXeNamVal;
    private YearCompareChart pnlYearChart;
    private JLabel lblNhanXetNam;

    // ─────────────────────────────────────────────────────────────────────────
    // CONTROLS PHẦN 3: TÌNH HÌNH XỬ LÝ PHIẾU
    // ─────────────────────────────────────────────────────────────────────────
    private JLabel lblChoSuaVal, lblChoSuaPct;
    private JLabel lblDangSuaVal, lblDangSuaPct;
    private JLabel lblChoLinhKienVal, lblChoLinhKienPct;
    private JLabel lblHoanTatVal, lblHoanTatPct;

    private DistributionProgressBar pnlProgressBar; // Unused - kept for reference
    private JLabel lblNhanXetTonDong;
    private DefaultTableModel modelTonDong;
    private JTable tblTonDong;

    public ThongKeTiepNhanUI() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildScrollBody(), BorderLayout.CENTER);

        // Thiết lập giá trị mặc định cho bộ lọc (7 ngày gần nhất) và tải dữ liệu ban đầu
        datBoLocMacDinh();
        taiToanBoDuLieuThongKe();
    }

    private JPanel buildHeader() {
        JPanel pnl = new JPanel(new BorderLayout(12, 0));
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(16, 24, 12, 24));

        JPanel titleBlock = new JPanel(new BorderLayout(0, 2));
        titleBlock.setOpaque(false);

        JLabel lblTitle = new JLabel("Thống kê tiếp nhận sửa chữa");
        lblTitle.setFont(f(Font.BOLD, 22));
        lblTitle.setForeground(C_TEXT_HEAD);

        titleBlock.add(lblTitle, BorderLayout.NORTH);
        pnl.add(titleBlock, BorderLayout.WEST);

        return pnl;
    }

    private JScrollPane buildScrollBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 24, 30, 24));

        // ═════════════════════════════════════════════════════════════════════
        // PHẦN 1: THỐNG KÊ TỔNG QUAN
        // ═════════════════════════════════════════════════════════════════════
        body.add(buildSectionTitle("THỐNG KÊ TỔNG QUAN", "Theo dõi số lượng tiếp nhận và chi tiết phiếu trong kỳ lọc"));
        body.add(Box.createVerticalStrut(10));
        body.add(buildFilterBar());
        body.add(Box.createVerticalStrut(14));
        body.add(buildKpiCards());
        body.add(Box.createVerticalStrut(14));
        body.add(buildChartsRow());
        body.add(Box.createVerticalStrut(14));
        body.add(buildTableChiTiet());

        // ═════════════════════════════════════════════════════════════════════
        // PHẦN 2: SO SÁNH TÌNH HÌNH TIẾP NHẬN THEO THỜI GIAN
        // ═════════════════════════════════════════════════════════════════════
        body.add(Box.createVerticalStrut(28));
        body.add(buildSectionTitle("SO SÁNH TÌNH HÌNH TIẾP NHẬN", "Đối chiếu sự tăng trưởng hoặc suy giảm số lượng tiếp nhận giữa các mốc thời gian"));
        body.add(Box.createVerticalStrut(12));
        body.add(buildPhanSoSanhThoiGian());

        // ═════════════════════════════════════════════════════════════════════
        // PHẦN 3: TÌNH HÌNH XỬ LÝ PHIẾU TIẾP NHẬN
        // ═════════════════════════════════════════════════════════════════════
        body.add(Box.createVerticalStrut(28));
        body.add(buildSectionTitle("TÌNH HÌNH XỬ LÝ PHIẾU TIẾP NHẬN", "Nắm bắt trạng thái xử lý tức thời và danh sách các phiếu đang tồn đọng cần xử lý"));
        body.add(Box.createVerticalStrut(12));
        body.add(buildPhanTinhHinhXuLy());

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    private JPanel buildSectionTitle(String title, String subtitle) {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        pnl.setOpaque(false);
        pnl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(title);
        lbl.setFont(f(Font.BOLD, 16));
        lbl.setForeground(C_PRIMARY);

        pnl.add(lbl);
        return pnl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1.1. THANH BỘ LỌC (FILTER BAR) - CÓ DATE PICKER & CHỈ LỌC KHI BẤM NÚT
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildFilterBar() {
        JPanel card = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        card.setBackground(C_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(6, 12, 6, 12)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Kỳ thống kê
        JLabel lblKy = new JLabel("Kỳ thống kê:");
        lblKy.setFont(f(Font.BOLD, 12));
        card.add(lblKy);

        cboKyThongKe = new JComboBox<>(new String[]{"7 ngày gần nhất", "Hôm nay", "Tháng này", "Tùy chọn"});
        cboKyThongKe.setFont(f(Font.PLAIN, 13));
        cboKyThongKe.setPreferredSize(new Dimension(140, 32));
        // Đổi kỳ -> chỉ cập nhật Từ ngày, Đến ngày trên thanh lọc, KHÔNG tự động lọc
        cboKyThongKe.addActionListener(e -> capNhatNgayTheoKy(cboKyThongKe.getSelectedIndex()));
        card.add(cboKyThongKe);

        // Từ ngày (DatePicker)
        JLabel lblTu = new JLabel("Từ ngày:");
        lblTu.setFont(f(Font.BOLD, 12));
        card.add(lblTu);

        dpTuNgay = new DatePickerField();
        dpTuNgay.setPreferredSize(new Dimension(135, 32));
        card.add(dpTuNgay);

        // Đến ngày (DatePicker)
        JLabel lblDen = new JLabel("Đến ngày:");
        lblDen.setFont(f(Font.BOLD, 12));
        card.add(lblDen);

        dpDenNgay = new DatePickerField();
        dpDenNgay.setPreferredSize(new Dimension(135, 32));
        card.add(dpDenNgay);

        // Trạng thái
        JLabel lblTrangThai = new JLabel("Trạng thái:");
        lblTrangThai.setFont(f(Font.BOLD, 12));
        card.add(lblTrangThai);

        cboTrangThai = new JComboBox<>(new String[]{
                "-- Tất cả --",
                "Chờ sửa chữa",
                "Đang sửa chữa",
                "Chờ linh kiện",
                "Hoàn tất"
        });
        cboTrangThai.setFont(f(Font.PLAIN, 13));
        cboTrangThai.setPreferredSize(new Dimension(140, 32));
        card.add(cboTrangThai);

        // Nút Xem thống kê (Bấm vào mới lọc)
        JButton btnXem = new RoundedButton("📊 Xem thống kê", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
        btnXem.setFont(f(Font.BOLD, 12));
        btnXem.setForeground(C_WHITE);
        btnXem.setPreferredSize(new Dimension(135, 32));
        btnXem.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnXem.addActionListener(e -> taiDuLieuPhan1());
        card.add(btnXem);

        // Nút Làm mới (Reset lại các giá trị chuẩn)
        JButton btnLamMoi = new RoundedButton("↺ Làm mới", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnLamMoi.setFont(f(Font.PLAIN, 12));
        btnLamMoi.setForeground(C_TEXT_MAIN);
        btnLamMoi.setPreferredSize(new Dimension(95, 32));
        btnLamMoi.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLamMoi.addActionListener(e -> {
            datBoLocMacDinh();
            taiDuLieuPhan1();
        });
        card.add(btnLamMoi);

        return card;
    }

    /**
     * Cập nhật ngày theo kỳ mà KHÔNG kích hoạt lọc tự động.
     */
    private void capNhatNgayTheoKy(int index) {
        switch (index) {
            case 0 -> { // 7 ngày gần nhất
                Date[] r = controller.layKhoang7NgayGanNhat();
                dpTuNgay.setDate(r[0]);
                dpDenNgay.setDate(r[1]);
            }
            case 1 -> { // Hôm nay
                Date[] r = controller.layKhoangHomNay();
                dpTuNgay.setDate(r[0]);
                dpDenNgay.setDate(r[1]);
            }
            case 2 -> { // Tháng này
                Date[] r = controller.layKhoangThangNay();
                dpTuNgay.setDate(r[0]);
                dpDenNgay.setDate(r[1]);
            }
            default -> {} // Tùy chọn giữ nguyên
        }
    }

    /**
     * Reset lại các giá trị trên thanh lọc:
     * - Kỳ thống kê: 7 ngày gần nhất
     * - Từ ngày: Ngày hiện tại - 6 ngày
     * - Đến ngày: Ngày hiện tại
     * - Trạng thái: -- Tất cả --
     */
    private void datBoLocMacDinh() {
        Date[] range7Ngay = controller.layKhoang7NgayGanNhat();
        dpTuNgay.setDate(range7Ngay[0]);
        dpDenNgay.setDate(range7Ngay[1]);
        cboKyThongKe.setSelectedIndex(0); // 7 ngày gần nhất
        cboTrangThai.setSelectedIndex(0);  // -- Tất cả --
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1.2. KPI CARDS PHẦN 1
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildKpiCards() {
        JPanel pnl = new JPanel(new GridLayout(1, 4, 12, 0));
        pnl.setOpaque(false);
        pnl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));
        pnl.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblTongPhieuVal = new JLabel("0");
        lblSoXeVal = new JLabel("0");
        lblKhachMoiVal = new JLabel("0");
        lblChoXuLyVal = new JLabel("0");

        pnl.add(createKpiCard("TỔNG PHIẾU TIẾP NHẬN", lblTongPhieuVal, "Phiếu đã lập trong kỳ", true));
        pnl.add(createKpiCard("XE ĐƯỢC TIẾP NHẬN", lblSoXeVal, "Biển số xe khác nhau", false));
        pnl.add(createKpiCard("KHÁCH HÀNG MỚI", lblKhachMoiVal, "Tổng khách hàng đăng ký", false));
        pnl.add(createKpiCard("PHIẾU CHỜ XỬ LÝ", lblChoXuLyVal, "Trạng thái Chờ sửa chữa", false));

        return pnl;
    }

    private JPanel createKpiCard(String title, JLabel lblVal, String sub, boolean isPrimary) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(isPrimary ? C_PRIMARY : C_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isPrimary ? C_PRIMARY : C_BORDER, 1),
                new EmptyBorder(10, 14, 10, 14)));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(f(Font.BOLD, 11));
        lblTitle.setForeground(isPrimary ? new Color(0xDBEAFE) : C_TEXT_SUBTLE);
        card.add(lblTitle, BorderLayout.NORTH);

        JPanel pnlVal = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlVal.setOpaque(false);
        lblVal.setFont(f(Font.BOLD, 24));
        lblVal.setForeground(isPrimary ? C_WHITE : C_TEXT_HEAD);
        pnlVal.add(lblVal);
        card.add(pnlVal, BorderLayout.CENTER);

        JLabel lblSub = new JLabel(sub);
        lblSub.setFont(f(Font.PLAIN, 11));
        lblSub.setForeground(isPrimary ? new Color(0xBFDBFE) : C_TEXT_MUTED);
        card.add(lblSub, BorderLayout.SOUTH);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1.3. CHARTS ROW PHẦN 1
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildChartsRow() {
        JPanel pnl = new JPanel(new GridLayout(1, 2, 14, 0));
        pnl.setOpaque(false);
        pnl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        pnl.setAlignmentX(Component.LEFT_ALIGNMENT);

        pnlBarChart = new ChartBarPanel("Xu hướng tiếp nhận theo ngày");
        pnl.add(pnlBarChart);

        pnlDonutChart = new ChartDonutPanel("Phân bố trạng thái phiếu");
        pnl.add(pnlDonutChart);

        return pnl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1.4. BẢNG CHI TIẾT PHẦN 1
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTableChiTiet() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(C_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel("Chi tiết các phiếu tiếp nhận trong kỳ");
        lbl.setFont(f(Font.BOLD, 14));
        lbl.setForeground(C_TEXT_HEAD);
        card.add(lbl, BorderLayout.NORTH);

        modelTable = new DefaultTableModel(new String[]{"Mã phiếu", "Ngày tiếp nhận", "Khách hàng", "Biển số xe", "KTV chính", "Trạng thái"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblChiTiet = new JTable(modelTable);
        tblChiTiet.setFont(f(Font.PLAIN, 13));
        tblChiTiet.setRowHeight(30);
        tblChiTiet.setShowGrid(false);
        tblChiTiet.getTableHeader().setFont(f(Font.BOLD, 12));
        tblChiTiet.getTableHeader().setBackground(new Color(0xF8FAFC));

        // Center cột mã phiếu và ngày
        DefaultTableCellRenderer centerRend = new DefaultTableCellRenderer();
        centerRend.setHorizontalAlignment(SwingConstants.CENTER);
        tblChiTiet.getColumnModel().getColumn(0).setCellRenderer(centerRend);
        tblChiTiet.getColumnModel().getColumn(1).setCellRenderer(centerRend);

        JScrollPane scroll = new JScrollPane(tblChiTiet);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        scroll.setPreferredSize(new Dimension(0, 200));
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. PHẦN 2: SO SÁNH TÌNH HÌNH TIẾP NHẬN THEO THỜI GIAN
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildPhanSoSanhThoiGian() {
        JPanel pnl = new JPanel(new GridLayout(1, 2, 14, 0));
        pnl.setOpaque(false);
        pnl.setAlignmentX(Component.LEFT_ALIGNMENT);

        pnl.add(buildDashboardA());
        pnl.add(buildDashboardB());

        return pnl;
    }

    /**
     * Dashboard A: So sánh Tháng này với Tháng trước
     */
    private JPanel buildDashboardA() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(C_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(14, 16, 14, 16)));

        // Header Dashboard A
        JPanel pnlHead = new JPanel(new BorderLayout(0, 2));
        pnlHead.setOpaque(false);
        JLabel lblTitle = new JLabel("DASHBOARD A: THÁNG NÀY VS THÁNG TRƯỚC");
        lblTitle.setFont(f(Font.BOLD, 14));
        lblTitle.setForeground(C_TEXT_HEAD);
        JLabel lblDesc = new JLabel("Theo dõi biến động số lượng phiếu, lượt khách và xe giữa 2 tháng");
        lblDesc.setFont(f(Font.PLAIN, 11));
        lblDesc.setForeground(C_TEXT_SUBTLE);
        pnlHead.add(lblTitle, BorderLayout.NORTH);
        pnlHead.add(lblDesc, BorderLayout.SOUTH);
        card.add(pnlHead, BorderLayout.NORTH);

        // Body: Thẻ chỉ số + Biểu đồ + Nhận xét
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        // Hàng thẻ số liệu 4 cột
        JPanel pnlCards = new JPanel(new GridLayout(2, 2, 8, 8));
        pnlCards.setOpaque(false);
        pnlCards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        lblThangNayVal = new JLabel("0", SwingConstants.CENTER);
        lblThangTruocVal = new JLabel("0", SwingConstants.CENTER);
        lblChenhLechThangVal = new JLabel("0", SwingConstants.CENTER);
        lblTyLeThangVal = new JLabel("0%", SwingConstants.CENTER);
        lblLuotKhachThangVal = new JLabel("0 | 0", SwingConstants.CENTER);
        lblSoXeThangVal = new JLabel("0 | 0", SwingConstants.CENTER);

        pnlCards.add(createMiniCard("THÁNG NÀY", lblThangNayVal, C_PRIMARY_BG, C_PRIMARY));
        pnlCards.add(createMiniCard("THÁNG TRƯỚC", lblThangTruocVal, new Color(0xF1F5F9), C_TEXT_HEAD));
        pnlCards.add(createMiniCard("CHÊNH LỆCH & TĂNG TRƯỞNG", lblChenhLechThangVal, lblTyLeThangVal));
        pnlCards.add(createMiniCard("KHÁCH & XE (NÀY | TRƯỚC)", lblLuotKhachThangVal, new Color(0xF8FAFC), C_TEXT_MAIN));

        body.add(pnlCards);
        body.add(Box.createVerticalStrut(10));

        // Biểu đồ cột so sánh Tháng trước và Tháng này
        pnlMonthChart = new MonthCompareBarChart();
        pnlMonthChart.setPreferredSize(new Dimension(0, 180));
        body.add(pnlMonthChart);
        body.add(Box.createVerticalStrut(10));

        // Đoạn nhận xét bằng chữ
        JPanel pnlNhanXet = new JPanel(new BorderLayout());
        pnlNhanXet.setBackground(new Color(0xF8FAFC));
        pnlNhanXet.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(8, 10, 8, 10)));

        lblNhanXetThang = new JLabel("<html>Đang tải dữ liệu so sánh tháng...</html>");
        lblNhanXetThang.setFont(f(Font.PLAIN, 12));
        lblNhanXetThang.setForeground(C_TEXT_MAIN);
        pnlNhanXet.add(lblNhanXetThang, BorderLayout.CENTER);
        body.add(pnlNhanXet);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    /**
     * Dashboard B: So sánh Năm nay với Năm trước
     */
    private JPanel buildDashboardB() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(C_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(14, 16, 14, 16)));

        // Header Dashboard B
        JPanel pnlHead = new JPanel(new BorderLayout(0, 2));
        pnlHead.setOpaque(false);
        JLabel lblTitle = new JLabel("DASHBOARD B: NĂM NAY VS NĂM TRƯỚC");
        lblTitle.setFont(f(Font.BOLD, 14));
        lblTitle.setForeground(C_TEXT_HEAD);
        JLabel lblDesc = new JLabel("Phân tích xu hướng 12 tháng và tỷ lệ tăng trưởng so với cùng kỳ");
        lblDesc.setFont(f(Font.PLAIN, 11));
        lblDesc.setForeground(C_TEXT_SUBTLE);
        pnlHead.add(lblTitle, BorderLayout.NORTH);
        pnlHead.add(lblDesc, BorderLayout.SOUTH);
        card.add(pnlHead, BorderLayout.NORTH);

        // Body: Thẻ chỉ số + Biểu đồ 12 tháng + Nhận xét
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        // Hàng thẻ số liệu
        JPanel pnlCards = new JPanel(new GridLayout(2, 2, 8, 8));
        pnlCards.setOpaque(false);
        pnlCards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        lblNamNayVal = new JLabel("0", SwingConstants.CENTER);
        lblNamTruocVal = new JLabel("0", SwingConstants.CENTER);
        lblChenhLechNamVal = new JLabel("0", SwingConstants.CENTER);
        lblTyLeNamVal = new JLabel("0%", SwingConstants.CENTER);
        lblLuotKhachNamVal = new JLabel("0 | 0", SwingConstants.CENTER);
        lblSoXeNamVal = new JLabel("0 | 0", SwingConstants.CENTER);

        pnlCards.add(createMiniCard("NĂM NAY", lblNamNayVal, C_PRIMARY_BG, C_PRIMARY));
        pnlCards.add(createMiniCard("NĂM TRƯỚC", lblNamTruocVal, new Color(0xF1F5F9), C_TEXT_HEAD));
        pnlCards.add(createMiniCard("CHÊNH LỆCH & TĂNG TRƯỞNG", lblChenhLechNamVal, lblTyLeNamVal));
        pnlCards.add(createMiniCard("KHÁCH & XE (NAY | TRƯỚC)", lblLuotKhachNamVal, new Color(0xF8FAFC), C_TEXT_MAIN));

        body.add(pnlCards);
        body.add(Box.createVerticalStrut(10));

        // Biểu đồ cột nhóm so sánh 12 tháng giữa 2 năm
        pnlYearChart = new YearCompareChart();
        pnlYearChart.setPreferredSize(new Dimension(0, 180));
        body.add(pnlYearChart);
        body.add(Box.createVerticalStrut(10));

        // Đoạn nhận xét bằng chữ
        JPanel pnlNhanXet = new JPanel(new BorderLayout());
        pnlNhanXet.setBackground(new Color(0xF8FAFC));
        pnlNhanXet.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(8, 10, 8, 10)));

        lblNhanXetNam = new JLabel("<html>Đang tải dữ liệu so sánh năm...</html>");
        lblNhanXetNam.setFont(f(Font.PLAIN, 12));
        lblNhanXetNam.setForeground(C_TEXT_MAIN);
        pnlNhanXet.add(lblNhanXetNam, BorderLayout.CENTER);
        body.add(pnlNhanXet);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel createMiniCard(String title, JLabel lblVal, Color bg, Color textCol) {
        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(6, 8, 6, 8)));

        JLabel lblT = new JLabel(title, SwingConstants.CENTER);
        lblT.setFont(f(Font.BOLD, 10));
        lblT.setForeground(C_TEXT_SUBTLE);
        card.add(lblT, BorderLayout.NORTH);

        lblVal.setFont(f(Font.BOLD, 16));
        lblVal.setForeground(textCol);
        card.add(lblVal, BorderLayout.CENTER);

        return card;
    }

    private JPanel createMiniCard(String title, JLabel lblVal, JLabel lblSubVal) {
        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setBackground(C_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(6, 8, 6, 8)));

        JLabel lblT = new JLabel(title, SwingConstants.CENTER);
        lblT.setFont(f(Font.BOLD, 10));
        lblT.setForeground(C_TEXT_SUBTLE);
        card.add(lblT, BorderLayout.NORTH);

        JPanel pnlVal = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        pnlVal.setOpaque(false);
        lblVal.setFont(f(Font.BOLD, 16));
        lblSubVal.setFont(f(Font.BOLD, 13));
        pnlVal.add(lblVal);
        pnlVal.add(lblSubVal);
        card.add(pnlVal, BorderLayout.CENTER);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. PHẦN 3: TÌNH HÌNH XỬ LÝ VÀ TỒN ĐỌNG PHIẾU
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildPhanTinhHinhXuLy() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 3.1. 4 Card trạng thái lấp đầy hàng ngang (đồng bộ với Phần 1)
        JPanel pnl4Cards = new JPanel(new GridLayout(1, 4, 12, 0));
        pnl4Cards.setOpaque(false);
        pnl4Cards.setAlignmentX(0.0f);
        pnl4Cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));

        lblChoSuaVal = new JLabel("0");
        lblChoSuaPct = new JLabel("0%");
        lblDangSuaVal = new JLabel("0");
        lblDangSuaPct = new JLabel("0%");
        lblChoLinhKienVal = new JLabel("0");
        lblChoLinhKienPct = new JLabel("0%");
        lblHoanTatVal = new JLabel("0");
        lblHoanTatPct = new JLabel("0%");

        pnl4Cards.add(createKpiStatusCard("CHỜ SỬA CHỮA", lblChoSuaVal, lblChoSuaPct, "Chưa bắt đầu sửa chữa"));
        pnl4Cards.add(createKpiStatusCard("ĐANG SỬA CHỮA", lblDangSuaVal, lblDangSuaPct, "Kỹ thuật viên đang xử lý"));
        pnl4Cards.add(createKpiStatusCard("CHỜ LINH KIỆN", lblChoLinhKienVal, lblChoLinhKienPct, "Tạm dừng chờ phụ tùng"));
        pnl4Cards.add(createKpiStatusCard("HOÀN TẤT", lblHoanTatVal, lblHoanTatPct, "Đã hoàn thành sửa chữa"));
        card.add(pnl4Cards);
        card.add(Box.createVerticalStrut(14));

        // 3.2. Nhận xét tổng quan tình hình xử lý
        JPanel pnlNhanXet = new JPanel(new BorderLayout());
        pnlNhanXet.setBackground(new Color(0xF8FAFC));
        pnlNhanXet.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(8, 12, 8, 12)));
        pnlNhanXet.setAlignmentX(0.0f);
        pnlNhanXet.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        lblNhanXetTonDong = new JLabel("<html>Đang tổng hợp tình hình tồn đọng...</html>");
        lblNhanXetTonDong.setFont(f(Font.PLAIN, 12));
        lblNhanXetTonDong.setForeground(C_TEXT_MAIN);
        pnlNhanXet.add(lblNhanXetTonDong, BorderLayout.CENTER);

        card.add(pnlNhanXet);
        card.add(Box.createVerticalStrut(12));

        // 3.3. Bảng danh sách các phiếu tồn đọng lâu
        JPanel pnlTableCard = new JPanel(new BorderLayout(0, 8));
        pnlTableCard.setBackground(C_WHITE);
        pnlTableCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)));
        pnlTableCard.setAlignmentX(0.0f);
        pnlTableCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel lblTonDongTitle = new JLabel("Danh sách các phiếu đang chờ xử lý & tồn đọng lâu (Ưu tiên theo thời gian)");
        lblTonDongTitle.setFont(f(Font.BOLD, 13));
        lblTonDongTitle.setForeground(C_TEXT_HEAD);
        pnlTableCard.add(lblTonDongTitle, BorderLayout.NORTH);

        modelTonDong = new DefaultTableModel(new String[]{
                "Mã phiếu", "Thời gian tiếp nhận", "Thời gian chờ", "Biển số xe", "Khách hàng", "KTV chính", "Trạng thái"
        }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblTonDong = new JTable(modelTonDong);
        tblTonDong.setFont(f(Font.PLAIN, 13));
        tblTonDong.setRowHeight(30);
        tblTonDong.setShowGrid(false);
        tblTonDong.getTableHeader().setFont(f(Font.BOLD, 12));
        tblTonDong.getTableHeader().setBackground(new Color(0xF8FAFC));

        // Format cột trạng thái với màu cảnh báo
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tblTonDong.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tblTonDong.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);

        // Cột thời gian chờ tô đậm
        DefaultTableCellRenderer waitRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setHorizontalAlignment(SwingConstants.CENTER);
                c.setFont(f(Font.BOLD, 12));
                String text = value != null ? value.toString() : "";
                if (text.contains("ngày")) {
                    c.setForeground(new Color(0xDC2626)); // Đỏ nếu trên 1 ngày
                } else {
                    c.setForeground(new Color(0xD97706)); // Vàng cam nếu tính theo giờ
                }
                return c;
            }
        };
        tblTonDong.getColumnModel().getColumn(2).setCellRenderer(waitRenderer);

        JScrollPane scrollTonDong = new JScrollPane(tblTonDong);
        scrollTonDong.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        scrollTonDong.setPreferredSize(new Dimension(0, 180));
        pnlTableCard.add(scrollTonDong, BorderLayout.CENTER);

        card.add(pnlTableCard);
        return card;
    }

    private JPanel createKpiStatusCard(String title, JLabel lblVal, JLabel lblPct, String sub) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(C_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(10, 14, 10, 14)));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(f(Font.BOLD, 11));
        lblTitle.setForeground(C_TEXT_SUBTLE);
        card.add(lblTitle, BorderLayout.NORTH);

        JPanel pnlVal = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pnlVal.setOpaque(false);

        lblVal.setFont(f(Font.BOLD, 24));
        lblVal.setForeground(C_TEXT_HEAD);
        pnlVal.add(lblVal);

        lblPct.setFont(f(Font.BOLD, 12));
        lblPct.setForeground(C_TEXT_SUBTLE);
        pnlVal.add(lblPct);

        card.add(pnlVal, BorderLayout.CENTER);

        JLabel lblSub = new JLabel(sub);
        lblSub.setFont(f(Font.PLAIN, 11));
        lblSub.setForeground(C_TEXT_MUTED);
        card.add(lblSub, BorderLayout.SOUTH);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DỮ LIỆU & SWINGWORKER
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Tải dữ liệu của cả 3 phần.
     */
    private void taiToanBoDuLieuThongKe() {
        taiDuLieuPhan1();
        taiDuLieuPhan2();
        taiDuLieuPhan3();
    }

    /**
     * Tải dữ liệu Phần 1 theo bộ lọc hiện tại.
     */
    private void taiDuLieuPhan1() {
        Date tuNgay = dpTuNgay.getDate();
        Date denNgay = dpDenNgay.getDate();
        String tt = cboTrangThai.getSelectedIndex() > 0 ? (String) cboTrangThai.getSelectedItem() : null;

        new SwingWorker<ThongKeData, Void>() {
            @Override
            protected ThongKeData doInBackground() {
                return controller.layThongKe(tuNgay, denNgay, tt);
            }

            @Override
            protected void done() {
                try {
                    ThongKeData data = get();
                    if (data == null) return;

                    lblTongPhieuVal.setText(INT_FMT.format(data.tongPhieu));
                    lblSoXeVal.setText(INT_FMT.format(data.soXe));
                    lblKhachMoiVal.setText(INT_FMT.format(data.khachHangMoi));
                    lblChoXuLyVal.setText(INT_FMT.format(data.choXuLy));

                    pnlBarChart.setData(data.xuHuongNgay);
                    pnlDonutChart.setData(data.phanBoTrangThai);

                    modelTable.setRowCount(0);
                    if (data.danhSachPhieu != null) {
                        for (PhieuRow r : data.danhSachPhieu) {
                            modelTable.addRow(new Object[]{
                                    r.maPhieu,
                                    r.ngayTiepNhan,
                                    r.khachHang,
                                    r.bienSo,
                                    r.ktv,
                                    r.trangThai
                            });
                        }
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    /**
     * Tải dữ liệu Phần 2: So sánh Tháng này vs Tháng trước & Năm nay vs Năm trước.
     */
    private void taiDuLieuPhan2() {
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() {
                SoSanhThangData dataThang = controller.laySoSanhThang();
                SoSanhNamData dataNam = controller.laySoSanhNam();
                return new Object[]{dataThang, dataNam};
            }

            @Override
            protected void done() {
                try {
                    Object[] res = get();
                    SoSanhThangData dataThang = (SoSanhThangData) res[0];
                    SoSanhNamData dataNam = (SoSanhNamData) res[1];

                    // 1. Cập nhật Dashboard A
                    if (dataThang != null) {
                        lblThangNayVal.setText(INT_FMT.format(dataThang.phieuThangNay));
                        lblThangTruocVal.setText(INT_FMT.format(dataThang.phieuThangTruoc));

                        String dau = dataThang.chenhLechPhieu > 0 ? "+" : "";
                        lblChenhLechThangVal.setText(dau + INT_FMT.format(dataThang.chenhLechPhieu));

                        Color colThang;
                        if (dataThang.chenhLechPhieu > 0) colThang = new Color(0x059669); // Xanh lá
                        else if (dataThang.chenhLechPhieu < 0) colThang = new Color(0xDC2626); // Đỏ
                        else colThang = C_TEXT_SUBTLE; // Xám

                        lblChenhLechThangVal.setForeground(colThang);

                        if (dataThang.coDuLieuKyTruoc) {
                            lblTyLeThangVal.setText("(" + dau + PCT_FMT.format(dataThang.tyLeTangTruong) + "%)");
                            lblTyLeThangVal.setForeground(colThang);
                        } else {
                            lblTyLeThangVal.setText("(Kỳ trước: 0)");
                            lblTyLeThangVal.setForeground(C_TEXT_SUBTLE);
                        }

                        lblLuotKhachThangVal.setText(INT_FMT.format(dataThang.khachThangNay) + " | " + INT_FMT.format(dataThang.khachThangTruoc));
                        lblSoXeThangVal.setText(INT_FMT.format(dataThang.xeThangNay) + " | " + INT_FMT.format(dataThang.xeThangTruoc));

                        pnlMonthChart.setData(dataThang);

                        // Diễn giải chữ Dashboard A
                        StringBuilder sbThang = new StringBuilder("<html>");
                        sbThang.append("• Tháng này (T").append(dataThang.thangNay).append(") tiếp nhận <b>").append(dataThang.phieuThangNay).append("</b> phiếu.<br>");
                        sbThang.append("• Tháng trước (T").append(dataThang.thangTruoc).append(") tiếp nhận <b>").append(dataThang.phieuThangTruoc).append("</b> phiếu.<br>");
                        if (dataThang.chenhLechPhieu > 0) {
                            sbThang.append("• <font color='#059669'><b>Tăng ").append(dataThang.chenhLechPhieu).append(" phiếu</b>, tương đương ")
                                    .append(PCT_FMT.format(dataThang.tyLeTangTruong)).append("% so với tháng trước.</font>");
                        } else if (dataThang.chenhLechPhieu < 0) {
                            sbThang.append("• <font color='#DC2626'><b>Giảm ").append(Math.abs(dataThang.chenhLechPhieu)).append(" phiếu</b>, tương đương ")
                                    .append(PCT_FMT.format(Math.abs(dataThang.tyLeTangTruong))).append("% so với tháng trước.</font>");
                        } else {
                            sbThang.append("• <font color='#64748B'><b>Không thay đổi</b> số lượng phiếu so với tháng trước.</font>");
                        }
                        sbThang.append("</html>");
                        lblNhanXetThang.setText(sbThang.toString());
                    }

                    // 2. Cập nhật Dashboard B
                    if (dataNam != null) {
                        lblNamNayVal.setText(INT_FMT.format(dataNam.phieuNamNay));
                        lblNamTruocVal.setText(INT_FMT.format(dataNam.phieuNamTruoc));

                        String dau = dataNam.chenhLechPhieu > 0 ? "+" : "";
                        lblChenhLechNamVal.setText(dau + INT_FMT.format(dataNam.chenhLechPhieu));

                        Color colNam;
                        if (dataNam.chenhLechPhieu > 0) colNam = new Color(0x059669);
                        else if (dataNam.chenhLechPhieu < 0) colNam = new Color(0xDC2626);
                        else colNam = C_TEXT_SUBTLE;

                        lblChenhLechNamVal.setForeground(colNam);

                        if (dataNam.coDuLieuKyTruoc) {
                            lblTyLeNamVal.setText("(" + dau + PCT_FMT.format(dataNam.tyLeTangTruong) + "%)");
                            lblTyLeNamVal.setForeground(colNam);
                        } else {
                            lblTyLeNamVal.setText("(Kỳ trước: 0)");
                            lblTyLeNamVal.setForeground(C_TEXT_SUBTLE);
                        }

                        lblLuotKhachNamVal.setText(INT_FMT.format(dataNam.khachNamNay) + " | " + INT_FMT.format(dataNam.khachNamTruoc));
                        lblSoXeNamVal.setText(INT_FMT.format(dataNam.xeNamNay) + " | " + INT_FMT.format(dataNam.xeNamTruoc));

                        pnlYearChart.setData(dataNam);

                        // Diễn giải chữ Dashboard B
                        StringBuilder sbNam = new StringBuilder("<html>");
                        sbNam.append("Năm nay (").append(dataNam.namNay).append(") cửa hàng đã tiếp nhận <b>").append(dataNam.phieuNamNay).append("</b> phiếu");
                        if (dataNam.coDuLieuKyTruoc) {
                            if (dataNam.chenhLechPhieu > 0) {
                                sbNam.append(", <font color='#059669'><b>tăng ").append(dataNam.chenhLechPhieu).append(" phiếu (")
                                        .append(PCT_FMT.format(dataNam.tyLeTangTruong)).append("%)</b></font> so với cùng kỳ năm trước.");
                            } else if (dataNam.chenhLechPhieu < 0) {
                                sbNam.append(", <font color='#DC2626'><b>giảm ").append(Math.abs(dataNam.chenhLechPhieu)).append(" phiếu (")
                                        .append(PCT_FMT.format(Math.abs(dataNam.tyLeTangTruong))).append("%)</b></font> so với cùng kỳ năm trước.");
                            } else {
                                sbNam.append(", tương đương cùng kỳ năm trước.");
                            }
                        } else {
                            sbNam.append(" (Năm trước chưa có dữ liệu đối chiếu).");
                        }
                        sbNam.append("</html>");
                        lblNhanXetNam.setText(sbNam.toString());
                    }

                } catch (Exception ignored) {}
            }
        }.execute();
    }

    /**
     * Tải dữ liệu Phần 3: Tình hình xử lý và tồn đọng phiếu.
     */
    private void taiDuLieuPhan3() {
        new SwingWorker<TinhHinhXuLyData, Void>() {
            @Override
            protected TinhHinhXuLyData doInBackground() {
                return controller.layTinhHinhXuLy();
            }

            @Override
            protected void done() {
                try {
                    TinhHinhXuLyData data = get();
                    if (data == null) return;

                    int tong = data.tongCong > 0 ? data.tongCong : 1;
                    double pChoSua = (double) data.choSua * 100.0 / tong;
                    double pDangSua = (double) data.dangSua * 100.0 / tong;
                    double pChoLinhKien = (double) data.choLinhKien * 100.0 / tong;
                    double pHoanTat = (double) data.hoanTat * 100.0 / tong;

                    lblChoSuaVal.setText(INT_FMT.format(data.choSua));
                    lblChoSuaPct.setText("(" + (int) Math.round(pChoSua) + "%)");

                    lblDangSuaVal.setText(INT_FMT.format(data.dangSua));
                    lblDangSuaPct.setText("(" + (int) Math.round(pDangSua) + "%)");

                    lblChoLinhKienVal.setText(INT_FMT.format(data.choLinhKien));
                    lblChoLinhKienPct.setText("(" + (int) Math.round(pChoLinhKien) + "%)");

                    lblHoanTatVal.setText(INT_FMT.format(data.hoanTat));
                    lblHoanTatPct.setText("(" + (int) Math.round(pHoanTat) + "%)");

                    int choXuLy = data.choSua + data.dangSua + data.choLinhKien;
                    String nhanXet = String.format(
                            "<html>Hiện tại gara có <b>%d</b> phiếu đang chờ xử lý, trong đó <b>%d</b> phiếu đang sửa chữa, " +
                            "<b>%d</b> phiếu chờ linh kiện và <b>%d</b> phiếu chờ sửa chữa. Tổng cộng có <b>%d</b> phiếu đã hoàn tất.</html>",
                            choXuLy, data.dangSua, data.choLinhKien, data.choSua, data.hoanTat
                    );
                    lblNhanXetTonDong.setText(nhanXet);

                    // Điền danh sách tồn đọng
                    modelTonDong.setRowCount(0);
                    if (data.danhSachTonDong != null) {
                        for (PhieuTonDongRow r : data.danhSachTonDong) {
                            modelTonDong.addRow(new Object[]{
                                    r.maPhieu,
                                    r.ngayTiepNhan,
                                    r.thoiGianTonDong,
                                    r.bienSo,
                                    r.khachHang,
                                    r.ktv,
                                    r.trangThai
                            });
                        }
                    }

                } catch (Exception ignored) {}
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CUSTOM GRAPHICS 1: BAR CHART PHẦN 1
    // ─────────────────────────────────────────────────────────────────────────
    private static class ChartBarPanel extends JPanel {
        private final String title;
        private Map<String, Integer> data;

        public ChartBarPanel(String title) {
            this.title = title;
            setBackground(C_WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER, 1),
                    new EmptyBorder(10, 14, 10, 14)));
        }

        public void setData(Map<String, Integer> data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setFont(f(Font.BOLD, 13));
            g2.setColor(C_TEXT_HEAD);
            g2.drawString(title, 14, 20);

            if (data == null || data.isEmpty()) {
                g2.setFont(f(Font.ITALIC, 12));
                g2.setColor(C_TEXT_MUTED);
                g2.drawString("Không có dữ liệu trong khoảng thời gian này", 14, 80);
                g2.dispose();
                return;
            }

            int w = getWidth() - 40;
            int h = getHeight() - 60;
            int x0 = 30;
            int y0 = getHeight() - 25;

            int maxVal = 1;
            for (int v : data.values()) if (v > maxVal) maxVal = v;

            int n = data.size();
            int barWidth = Math.min(40, Math.max(16, w / (n * 2)));
            int gap = (w - (n * barWidth)) / Math.max(1, n);

            int i = 0;
            for (Map.Entry<String, Integer> e : data.entrySet()) {
                int bx = x0 + i * (barWidth + gap);
                int bh = (int) ((double) e.getValue() / maxVal * (h - 20));
                int by = y0 - bh;

                g2.setColor(C_PRIMARY);
                g2.fillRoundRect(bx, by, barWidth, bh, 6, 6);

                g2.setFont(f(Font.BOLD, 11));
                g2.setColor(C_TEXT_MAIN);
                g2.drawString(String.valueOf(e.getValue()), bx + 4, by - 4);

                g2.setFont(f(Font.PLAIN, 10));
                g2.setColor(C_TEXT_SUBTLE);
                g2.drawString(e.getKey(), bx, y0 + 14);

                i++;
            }

            g2.dispose();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CUSTOM GRAPHICS 2: DONUT CHART PHẦN 1
    // ─────────────────────────────────────────────────────────────────────────
    private static class ChartDonutPanel extends JPanel {
        private final String title;
        private Map<String, Integer> data;
        private final Color[] colors = new Color[]{
                new Color(0xEA580C), // Chờ sửa (Cam)
                new Color(0x2563EB), // Đang sửa (Xanh dương)
                new Color(0xD97706), // Chờ linh kiện (Vàng)
                new Color(0x059669), // Hoàn tất (Xanh lá)
                new Color(0x8B5CF6)  // Khác (Tím)
        };

        public ChartDonutPanel(String title) {
            this.title = title;
            setBackground(C_WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER, 1),
                    new EmptyBorder(10, 14, 10, 14)));
        }

        public void setData(Map<String, Integer> data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setFont(f(Font.BOLD, 13));
            g2.setColor(C_TEXT_HEAD);
            g2.drawString(title, 14, 20);

            if (data == null || data.isEmpty()) {
                g2.setFont(f(Font.ITALIC, 12));
                g2.setColor(C_TEXT_MUTED);
                g2.drawString("Không có dữ liệu", 14, 80);
                g2.dispose();
                return;
            }

            int total = 0;
            for (int v : data.values()) total += v;

            if (total == 0) {
                g2.setFont(f(Font.ITALIC, 12));
                g2.setColor(C_TEXT_MUTED);
                g2.drawString("0 phiếu tiếp nhận", 14, 80);
                g2.dispose();
                return;
            }

            int size = Math.min(getWidth() / 2 - 20, getHeight() - 60);
            int cx = 20;
            int cy = 40;

            double startAngle = 90;
            int idx = 0;

            int legX = cx + size + 20;
            int legY = 60;

            for (Map.Entry<String, Integer> e : data.entrySet()) {
                double arcAngle = 360.0 * e.getValue() / total;
                Color col = colors[idx % colors.length];

                g2.setColor(col);
                g2.fill(new Arc2D.Double(cx, cy, size, size, startAngle, arcAngle, Arc2D.PIE));

                g2.fillRect(legX, legY - 10, 12, 12);
                g2.setColor(C_TEXT_MAIN);
                g2.setFont(f(Font.PLAIN, 12));
                g2.drawString(e.getKey() + ": " + e.getValue() + " (" + (int)(e.getValue() * 100.0 / total) + "%)", legX + 18, legY);

                legY += 22;
                startAngle += arcAngle;
                idx++;
            }

            int hole = size / 2;
            g2.setColor(C_WHITE);
            g2.fillOval(cx + (size - hole) / 2, cy + (size - hole) / 2, hole, hole);

            g2.dispose();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CUSTOM GRAPHICS 3: BIỂU ĐỒ CỘT SO SÁNH 2 THÁNG (DASHBOARD A)
    // ─────────────────────────────────────────────────────────────────────────
    private static class MonthCompareBarChart extends JPanel {
        private SoSanhThangData data;
        private String tooltipText = null;

        public MonthCompareBarChart() {
            setBackground(C_WHITE);
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    if (data == null) return;
                    setToolTipText(String.format("Tháng %d: %d phiếu | Tháng %d: %d phiếu",
                            data.thangNay, data.phieuThangNay, data.thangTruoc, data.phieuThangTruoc));
                }
            });
        }

        public void setData(SoSanhThangData data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Legend ở góc trên
            g2.setFont(f(Font.PLAIN, 11));
            // Cột Tháng trước: Xám nhạt
            g2.setColor(new Color(0x94A3B8));
            g2.fillRect(w - 220, 8, 12, 12);
            g2.setColor(C_TEXT_MAIN);
            g2.drawString("Tháng trước (T" + data.thangTruoc + ")", w - 204, 18);

            // Cột Tháng này: Xanh dương
            g2.setColor(C_PRIMARY);
            g2.fillRect(w - 100, 8, 12, 12);
            g2.setColor(C_TEXT_MAIN);
            g2.drawString("Tháng này (T" + data.thangNay + ")", w - 84, 18);

            // 3 Tiêu chí so sánh: [Phiếu tiếp nhận], [Số xe], [Lượt khách]
            String[] categories = {"Phiếu tiếp nhận", "Số xe ô tô", "Lượt khách hàng"};
            int[] valsTruoc = {data.phieuThangTruoc, data.xeThangTruoc, data.khachThangTruoc};
            int[] valsNay = {data.phieuThangNay, data.xeThangNay, data.khachThangNay};

            int maxVal = 1;
            for (int v : valsTruoc) if (v > maxVal) maxVal = v;
            for (int v : valsNay) if (v > maxVal) maxVal = v;

            int chartH = h - 60;
            int yBase = h - 25;
            int catW = w / 3;
            int barW = Math.min(32, catW / 3);

            for (int i = 0; i < 3; i++) {
                int cx = i * catW + catW / 2;

                // Cột Tháng trước (trái)
                int xTruoc = cx - barW - 3;
                int hTruoc = (int) ((double) valsTruoc[i] / maxVal * (chartH - 25));
                int yTruoc = yBase - hTruoc;
                g2.setColor(new Color(0x94A3B8));
                g2.fillRoundRect(xTruoc, yTruoc, barW, hTruoc, 6, 6);

                // Số lượng trên đỉnh cột trước
                g2.setFont(f(Font.BOLD, 10));
                g2.setColor(C_TEXT_SUBTLE);
                g2.drawString(String.valueOf(valsTruoc[i]), xTruoc + 6, yTruoc - 3);

                // Cột Tháng này (phải)
                int xNay = cx + 3;
                int hNay = (int) ((double) valsNay[i] / maxVal * (chartH - 25));
                int yNay = yBase - hNay;
                g2.setColor(C_PRIMARY);
                g2.fillRoundRect(xNay, yNay, barW, hNay, 6, 6);

                // Số lượng trên đỉnh cột này
                g2.setColor(C_PRIMARY);
                g2.drawString(String.valueOf(valsNay[i]), xNay + 6, yNay - 3);

                // Nhãn danh mục ở dưới
                g2.setFont(f(Font.PLAIN, 11));
                g2.setColor(C_TEXT_MAIN);
                int strW = g2.getFontMetrics().stringWidth(categories[i]);
                g2.drawString(categories[i], cx - strW / 2, yBase + 16);
            }

            g2.dispose();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CUSTOM GRAPHICS 4: BIỂU ĐỒ 12 THÁNG SO SÁNH 2 NĂM (DASHBOARD B)
    // ─────────────────────────────────────────────────────────────────────────
    private static class YearCompareChart extends JPanel {
        private SoSanhNamData data;

        public YearCompareChart() {
            setBackground(C_WHITE);
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    if (data == null) return;
                    int w = getWidth() - 40;
                    int colW = w / 12;
                    int idx = (e.getX() - 30) / Math.max(1, colW);
                    if (idx >= 0 && idx < 12) {
                        setToolTipText(String.format("Tháng %d | Năm %d: %d phiếu | Năm %d: %d phiếu",
                                idx + 1, data.namNay, data.phieuTheoThangNamNay[idx],
                                data.namTruoc, data.phieuTheoThangNamTruoc[idx]));
                    }
                }
            });
        }

        public void setData(SoSanhNamData data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Legend ở góc trên
            g2.setFont(f(Font.PLAIN, 11));
            // Năm trước: Cam vàng
            g2.setColor(new Color(0xF59E0B));
            g2.fillRect(w - 200, 8, 12, 12);
            g2.setColor(C_TEXT_MAIN);
            g2.drawString("Năm trước (" + data.namTruoc + ")", w - 184, 18);

            // Năm nay: Xanh dương
            g2.setColor(C_PRIMARY);
            g2.fillRect(w - 90, 8, 12, 12);
            g2.setColor(C_TEXT_MAIN);
            g2.drawString("Năm nay (" + data.namNay + ")", w - 74, 18);

            int maxVal = 1;
            for (int v : data.phieuTheoThangNamNay) if (v > maxVal) maxVal = v;
            for (int v : data.phieuTheoThangNamTruoc) if (v > maxVal) maxVal = v;

            int chartH = h - 55;
            int yBase = h - 22;
            int padX = 25;
            int netW = w - padX * 2;
            int stepX = netW / 12;
            int barW = Math.max(6, (stepX - 8) / 2);

            for (int m = 0; m < 12; m++) {
                int cx = padX + m * stepX + stepX / 2;

                // Cột Năm trước
                int vTruoc = data.phieuTheoThangNamTruoc[m];
                int hTruoc = (int) ((double) vTruoc / maxVal * (chartH - 20));
                int xTruoc = cx - barW - 1;
                int yTruoc = yBase - hTruoc;
                g2.setColor(new Color(0xFBBF24));
                g2.fillRoundRect(xTruoc, yTruoc, barW, hTruoc, 4, 4);

                // Cột Năm nay
                int vNay = data.phieuTheoThangNamNay[m];
                int hNay = (int) ((double) vNay / maxVal * (chartH - 20));
                int xNay = cx + 1;
                int yNay = yBase - hNay;
                g2.setColor(C_PRIMARY);
                g2.fillRoundRect(xNay, yNay, barW, hNay, 4, 4);

                // Nhãn T1..T12 dưới chân
                g2.setFont(f(Font.PLAIN, 10));
                g2.setColor(C_TEXT_SUBTLE);
                String tLabel = "T" + (m + 1);
                int lw = g2.getFontMetrics().stringWidth(tLabel);
                g2.drawString(tLabel, cx - lw / 2, yBase + 14);
            }

            g2.dispose();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CUSTOM GRAPHICS 5: THANH PHÂN BỐ TỶ TRỌNG TRẠNG THÁI (PHẦN 3)
    // ─────────────────────────────────────────────────────────────────────────
    private static class DistributionProgressBar extends JPanel {
        private int choSua = 0;
        private int dangSua = 0;
        private int choLinhKien = 0;
        private int hoanTat = 0;

        public DistributionProgressBar() {
            setOpaque(false);
        }

        public void setData(int choSua, int dangSua, int choLinhKien, int hoanTat) {
            this.choSua = choSua;
            this.dangSua = dangSua;
            this.choLinhKien = choLinhKien;
            this.hoanTat = hoanTat;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int total = choSua + dangSua + choLinhKien + hoanTat;

            if (total == 0) {
                g2.setColor(new Color(0xE2E8F0));
                g2.fillRoundRect(0, 0, w, h, 8, 8);
                g2.dispose();
                return;
            }

            int wChoSua = (int) Math.round((double) choSua * w / total);
            int wDangSua = (int) Math.round((double) dangSua * w / total);
            int wChoLK = (int) Math.round((double) choLinhKien * w / total);
            int wHoanTat = w - (wChoSua + wDangSua + wChoLK);

            // Clip bo góc cho toàn bộ thanh progress bar
            Shape clip = new RoundRectangle2D.Double(0, 0, w, h, 8, 8);
            g2.setClip(clip);

            int curX = 0;

            // 1. Chờ sửa (Cam)
            if (wChoSua > 0) {
                g2.setColor(new Color(0xEA580C));
                g2.fillRect(curX, 0, wChoSua, h);
                curX += wChoSua;
            }

            // 2. Đang sửa (Xanh dương)
            if (wDangSua > 0) {
                g2.setColor(new Color(0x2563EB));
                g2.fillRect(curX, 0, wDangSua, h);
                curX += wDangSua;
            }

            // 3. Chờ linh kiện (Vàng hổ phách)
            if (wChoLK > 0) {
                g2.setColor(new Color(0xD97706));
                g2.fillRect(curX, 0, wChoLK, h);
                curX += wChoLK;
            }

            // 4. Hoàn tất (Xanh lá)
            if (wHoanTat > 0) {
                g2.setColor(new Color(0x059669));
                g2.fillRect(curX, 0, wHoanTat, h);
            }

            g2.dispose();
        }
    }
}
