package ui;

import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;
import controller.PhieuTiepNhanSuaChuaController;
import controller.PhieuYeuCauNhapLinhKienController;
import dao.PhieuTiepNhanSuaChuaDAO.PhieuSuaChuaItem;
import dao.PhieuTiepNhanSuaChuaDAO.ThongKeKTVData;
import enums.TrangThaiYeuCau;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Màn hình Thống kê sửa chữa cho Kỹ thuật viên (Chuẩn Figma frame 172:5247).
 * Tái hiện 100% bố cục và thứ tự thành phần theo thiết kế Figma:
 * - 4 Thẻ KPI chính (Thẻ 1 nền xanh Primary, 3 thẻ nền trắng).
 * - 2 Thẻ phụ: Tỷ lệ hoàn tất (65.6%) và Thời gian xử lý trung bình (4.8h).
 * - Biểu đồ cột hiệu suất 7 ngày & Biểu đồ Donut phân bố trạng thái.
 * - Bảng Chi tiết phiếu sửa chữa & Khối Yêu cầu linh kiện của tôi.
 */
public class ThongKeSuaChuaUI extends JPanel {

    private static final Color BG = new Color(0xF4F7FB);
    private static final Color WHITE = Color.WHITE;
    private static final Color BORDER = new Color(0xE3E9F2);
    private static final Color PRIMARY = new Color(0x2563EB);
    private static final Color PRIMARY_HV = new Color(0x1D4ED8);
    private static final Color TEXT = new Color(0x14233D);
    private static final Color BODY = new Color(0x40516A);
    private static final Color SUBTLE = new Color(0x7A889C);
    private static final Color SUCCESS = new Color(0x059669);
    private static final Color WARN = new Color(0xD97706);

    private static final NumberFormat MONEY = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    private final PhieuTiepNhanSuaChuaController controller = new PhieuTiepNhanSuaChuaController();
    private final PhieuYeuCauNhapLinhKienController ycController = new PhieuYeuCauNhapLinhKienController();
    private Runnable onMoYeuCauLinhKien;

    /**
     * Callback để chuyển sang trang Yêu cầu linh kiện. Gọi từ MainFrame sau khi
     * tạo.
     */
    public void setOnMoYeuCauLinhKien(Runnable r) {
        this.onMoYeuCauLinhKien = r;
    }

    // Filters
    private final JComboBox<String> cboKyThongKe = new JComboBox<>(
            new String[] { "7 ngày gần nhất", "Tháng này", "Tùy chọn" });
    private final JDateChooser dcTuNgay = createDateChooser(170);
    private final JDateChooser dcDenNgay = createDateChooser(170);
    private final JComboBox<String> cboTrangThai = new JComboBox<>(
            new String[] { "Tất cả", "Hoàn tất", "Đang sửa", "Chờ linh kiện", "Mới phân công" });
    private final JButton btnXem = button("Xem thống kê", true, 140);
    private final JButton btnLamMoi = button("Làm mới", false, 100);
    private final JButton btnXuatCsv = button("Xuất báo cáo", false, 130);
    private final JLabel lblCountInPeriod = label("0 phiếu trong kỳ", 12, SUBTLE, Font.PLAIN);

    // KPI 4 thẻ lớn
    private final JLabel lblKpi1Number = label("0", 32, WHITE, Font.BOLD);
    private final JLabel lblKpi1Title = label("Tổng phiếu được phân công", 13, WHITE, Font.BOLD);
    private final JLabel lblKpi1Sub = label("0 mới · 0 đang sửa · 0 chờ linh kiện", 11, new Color(0xDFEAFE),
            Font.PLAIN);

    private final JLabel lblKpi2Number = label("0", 32, TEXT, Font.BOLD);
    private final JLabel lblKpi2Title = label("Đang sửa", 13, TEXT, Font.BOLD);
    private final JLabel lblKpi2Sub = label("0% tổng phiếu", 11, SUBTLE, Font.PLAIN);

    private final JLabel lblKpi3Number = label("0", 32, TEXT, Font.BOLD);
    private final JLabel lblKpi3Title = label("Hoàn tất", 13, TEXT, Font.BOLD);
    private final JLabel lblKpi3Sub = label("0% tổng phiếu", 11, SUBTLE, Font.PLAIN);

    private final JLabel lblKpi4Number = label("0", 32, TEXT, Font.BOLD);
    private final JLabel lblKpi4Title = label("Chờ linh kiện", 13, TEXT, Font.BOLD);
    private final JLabel lblKpi4Sub = label("0% tổng phiếu", 11, SUBTLE, Font.PLAIN);

    // KPI 2 thẻ phụ
    private final JLabel lblTyLeHoanTat = label("0%", 24, TEXT, Font.BOLD);
    private final JLabel lblSubTyLe = label("0/0 phiếu đã hoàn tất trong kỳ", 12, SUBTLE, Font.PLAIN);
    private final JLabel lblThoiGianTB = label("4.8h", 24, TEXT, Font.BOLD);

    // Biểu đồ
    private final CotBieuDo7NgayPanel pnlBarChart = new CotBieuDo7NgayPanel();
    private final DonutTrangThaiPanel pnlDonutChart = new DonutTrangThaiPanel();

    // Table
    private final DefaultTableModel model = new DefaultTableModel(new Object[] {
            "Mã phiếu", "Biển số xe", "Ngày phân công", "Trạng thái", "Ngày hoàn tất", "Thời gian xử lý"
    }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel lblTableCount = label("0 bản ghi · 0 phiếu trong kỳ", 12, SUBTLE, Font.PLAIN);
    private List<PhieuSuaChuaItem> listPhieu = List.of();

    // Footer Yêu cầu linh kiện
    private final JLabel lblYcChoDuyet = label("0", 22, TEXT, Font.BOLD);
    private final JLabel lblYcDaDuyet = label("0", 22, TEXT, Font.BOLD);
    private final JLabel lblYcTuChoi = label("0", 22, TEXT, Font.BOLD);
    private final JLabel lblYcSummary = label(
            "0 yêu cầu chờ phê duyệt chủ cửa hàng / 0 yêu cầu linh kiện đã gửi trong kỳ", 12, SUBTLE, Font.PLAIN);

    public ThongKeSuaChuaUI() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(22, 26, 22, 26));

        JPanel root = new MainFrame.ScrollableContentPanel();
        root.setOpaque(false);

        // Heading
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleRow.setOpaque(false);
        titleRow.add(label("🛠️", 25, TEXT, Font.PLAIN));
        titleRow.add(label("Thống kê sửa chữa", 24, TEXT, Font.BOLD));
        heading.add(titleRow);
        heading.add(Box.createVerticalStrut(4));
        addFull(root, heading);
        root.add(Box.createVerticalStrut(18));

        // Filter Card
        addFull(root, buildFilterCard());
        root.add(Box.createVerticalStrut(16));

        // 4 Thẻ KPI lớn
        JPanel kpi4 = new JPanel(new GridLayout(1, 4, 14, 0));
        kpi4.setOpaque(false);
        kpi4.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Thẻ 1 xanh dương đậm
        JPanel card1 = new TraCuuUI.RoundedPanel(PRIMARY, 12, PRIMARY, new Insets(16, 18, 16, 18));
        card1.setLayout(new BoxLayout(card1, BoxLayout.Y_AXIS));
        card1.add(lblKpi1Number);
        card1.add(Box.createVerticalStrut(6));
        card1.add(lblKpi1Title);
        card1.add(Box.createVerticalStrut(4));
        card1.add(lblKpi1Sub);
        kpi4.add(card1);

        kpi4.add(buildWhiteKpiCard(lblKpi2Number, lblKpi2Title, lblKpi2Sub));
        kpi4.add(buildWhiteKpiCard(lblKpi3Number, lblKpi3Title, lblKpi3Sub));
        kpi4.add(buildWhiteKpiCard(lblKpi4Number, lblKpi4Title, lblKpi4Sub));
        addFull(root, kpi4);
        root.add(Box.createVerticalStrut(12));

        // 2 Thẻ KPI phụ
        JPanel kpiSubRow = new JPanel(new GridLayout(1, 2, 14, 0));
        kpiSubRow.setOpaque(false);
        kpiSubRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel sub1 = new TraCuuUI.RoundedPanel(WHITE, 10, BORDER, new Insets(12, 16, 12, 16));
        sub1.setLayout(new BorderLayout());
        JPanel left1 = new JPanel();
        left1.setOpaque(false);
        left1.setLayout(new BoxLayout(left1, BoxLayout.Y_AXIS));
        left1.add(label("Tỷ lệ hoàn tất", 13, TEXT, Font.BOLD));
        left1.add(Box.createVerticalStrut(2));
        left1.add(lblSubTyLe);
        sub1.add(left1, BorderLayout.WEST);
        sub1.add(lblTyLeHoanTat, BorderLayout.EAST);
        kpiSubRow.add(sub1);

        JPanel sub2 = new TraCuuUI.RoundedPanel(WHITE, 10, BORDER, new Insets(12, 16, 12, 16));
        sub2.setLayout(new BorderLayout());
        JPanel left2 = new JPanel();
        left2.setOpaque(false);
        left2.setLayout(new BoxLayout(left2, BoxLayout.Y_AXIS));
        left2.add(label("Thời gian xử lý trung bình", 13, TEXT, Font.BOLD));
        left2.add(Box.createVerticalStrut(2));
        left2.add(label("Trung bình từ nhận việc đến bàn giao", 12, SUBTLE, Font.PLAIN));
        sub2.add(left2, BorderLayout.WEST);
        sub2.add(lblThoiGianTB, BorderLayout.EAST);
        kpiSubRow.add(sub2);

        addFull(root, kpiSubRow);
        root.add(Box.createVerticalStrut(16));

        // 2 Biểu đồ hàng ngang
        JPanel chartsRow = new JPanel(new GridLayout(1, 2, 14, 0));
        chartsRow.setOpaque(false);
        chartsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        chartsRow.add(pnlBarChart);
        chartsRow.add(pnlDonutChart);
        addFull(root, chartsRow);
        root.add(Box.createVerticalStrut(16));

        // Bảng Chi tiết phiếu sửa chữa
        addFull(root, buildTableCard());
        root.add(Box.createVerticalStrut(16));

        // Khối Footer: Yêu cầu linh kiện của tôi
        addFull(root, buildFooterYeuCauCard());

        JScrollPane scrollMain = new JScrollPane(root);
        scrollMain.setBorder(null);
        scrollMain.setOpaque(false);
        scrollMain.getViewport().setOpaque(false);
        scrollMain.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollMain.getVerticalScrollBar().setUnitIncrement(18);
        add(scrollMain, BorderLayout.CENTER);

        bindEvents();
        taiDuLieu();
    }

    private JPanel buildFilterCard() {
        JPanel card = new TraCuuUI.RoundedPanel(WHITE, 12, BORDER, new Insets(14, 18, 14, 18));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Row 1 inputs
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row1.setOpaque(false);
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);

        Calendar cal = Calendar.getInstance();
        dcDenNgay.setDate(cal.getTime());
        cal.add(Calendar.DAY_OF_MONTH, -7);
        dcTuNgay.setDate(cal.getTime());

        cboKyThongKe.setPreferredSize(new Dimension(160, 38));
        row1.add(createMiniField("Kỳ thống kê", cboKyThongKe));
        row1.add(createMiniField("Từ ngày", dcTuNgay));
        row1.add(createMiniField("Đến ngày", dcDenNgay));
        cboTrangThai.setPreferredSize(new Dimension(150, 38));
        row1.add(createMiniField("Trạng thái", cboTrangThai));
        addFull(card, row1);
        card.add(Box.createVerticalStrut(12));

        // Row 2 buttons
        JPanel row2 = new JPanel(new BorderLayout());
        row2.setOpaque(false);
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnGroup.setOpaque(false);
        btnGroup.add(btnXem);
        btnGroup.add(btnLamMoi);
        btnGroup.add(btnXuatCsv);
        row2.add(btnGroup, BorderLayout.WEST);
        row2.add(lblCountInPeriod, BorderLayout.EAST);
        addFull(card, row2);

        return card;
    }

    private JPanel buildTableCard() {
        JPanel card = new TraCuuUI.RoundedPanel(WHITE, 12, BORDER, new Insets(16, 18, 16, 18));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(label("Chi tiết phiếu sửa chữa", 15, TEXT, Font.BOLD), BorderLayout.WEST);
        top.add(lblTableCount, BorderLayout.EAST);
        addFull(card, top);
        card.add(Box.createVerticalStrut(10));

        styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new TraCuuUI.RoundedLineBorder(BORDER, 8));
        scroll.getViewport().setBackground(WHITE);
        scroll.setPreferredSize(new Dimension(940, 210));
        addFull(card, scroll);
        card.add(Box.createVerticalStrut(8));

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JButton btnXemChiTiet = button("Xem chi tiết", false, 130);
        btnXemChiTiet.addActionListener(e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0 || viewRow >= listPhieu.size()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một phiếu trong bảng để xem chi tiết.", "Thông báo",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            int modelRow = table.convertRowIndexToModel(viewRow);
            PhieuSuaChuaItem p = listPhieu.get(modelRow);
            JOptionPane.showMessageDialog(this,
                    "Mã phiếu: " + p.maPhieu() + "\n"
                            + "Biển số xe: " + p.bienSoXe() + " (" + p.hangModel() + ")\n"
                            + "Khách hàng: " + p.tenKhachHang() + " (" + p.soDienThoai() + ")\n"
                            + "Yêu cầu ban đầu: " + p.yeuCauBanDau() + "\n"
                            + "Ngày phân công: " + DATE_FORMAT.format(p.ngayTiepNhan()) + "\n"
                            + "Kỹ thuật viên: " + (p.tenKyThuatVien() != null ? p.tenKyThuatVien() : "—") + "\n"
                            + "Trạng thái: " + tenTrangThai(p.trangThai()),
                    "Chi tiết phiếu sửa chữa", JOptionPane.INFORMATION_MESSAGE);
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2)
                    btnXemChiTiet.doClick();
            }
        });
        footer.add(btnXemChiTiet, BorderLayout.EAST);
        addFull(card, footer);

        return card;
    }

    private JPanel buildFooterYeuCauCard() {
        JPanel card = new TraCuuUI.RoundedPanel(WHITE, 12, BORDER, new Insets(16, 18, 16, 18));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(label("📖  Yêu cầu linh kiện của tôi", 15, TEXT, Font.BOLD), BorderLayout.WEST);
        JLabel lblLink = label("Mở yêu cầu linh kiện ↗", 12, PRIMARY, Font.BOLD);
        lblLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblLink.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (onMoYeuCauLinhKien != null)
                    onMoYeuCauLinhKien.run();
            }
        });
        top.add(lblLink, BorderLayout.EAST);
        addFull(card, top);
        card.add(Box.createVerticalStrut(12));

        JPanel cols = new JPanel(new GridLayout(1, 3, 14, 0));
        cols.setOpaque(false);
        cols.add(createYcCol("Chờ phê duyệt", lblYcChoDuyet));
        cols.add(createYcCol("Đã duyệt", lblYcDaDuyet));
        cols.add(createYcCol("Cần bổ sung / Từ chối", lblYcTuChoi));
        addFull(card, cols);
        card.add(Box.createVerticalStrut(10));

        addFull(card, lblYcSummary);
        return card;
    }

    private JPanel createYcCol(String title, JLabel lblVal) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(label(title, 12, SUBTLE, Font.PLAIN));
        p.add(Box.createVerticalStrut(4));
        p.add(lblVal);
        return p;
    }

    private JPanel buildWhiteKpiCard(JLabel lblNum, JLabel lblTit, JLabel lblSub) {
        JPanel card = new TraCuuUI.RoundedPanel(WHITE, 12, BORDER, new Insets(16, 18, 16, 18));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(lblNum);
        card.add(Box.createVerticalStrut(6));
        card.add(lblTit);
        card.add(Box.createVerticalStrut(4));
        card.add(lblSub);
        return card;
    }

    private JPanel createMiniField(String title, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.add(label(title, 12, BODY, Font.PLAIN), BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private static JDateChooser createDateChooser(int width) {
        JDateChooser chooser = new JDateChooser();
        chooser.setDateFormatString("dd/MM/yyyy");
        chooser.setFont(new Font("SansSerif", Font.PLAIN, 13));
        chooser.setPreferredSize(new Dimension(width, 38));
        chooser.setBackground(WHITE);
        if (chooser.getDateEditor() instanceof JTextFieldDateEditor editor) {
            editor.setEditable(false); // Bắt buộc chọn từ lịch, không cho gõ bàn phím
            editor.setBackground(WHITE);
            editor.setFont(new Font("SansSerif", Font.PLAIN, 13));
            editor.setBorder(new EmptyBorder(0, 10, 0, 2));
            editor.setMargin(new Insets(0, 0, 0, 0));
        }
        JButton calBtn = chooser.getCalendarButton();
        if (calBtn != null) {
            calBtn.setPreferredSize(new Dimension(28, 28));
            calBtn.setMargin(new Insets(0, 0, 0, 0));
            calBtn.setFocusable(false);
            calBtn.setContentAreaFilled(false);
            calBtn.setBorder(new EmptyBorder(0, 0, 0, 4));
        }
        chooser.setBorder(new TraCuuUI.RoundedLineBorder(new Color(0xC9D5E5), 10));
        return chooser;
    }

    private void bindEvents() {
        btnXem.addActionListener(e -> taiDuLieu());
        btnLamMoi.addActionListener(e -> {
            cboKyThongKe.setSelectedIndex(0);
            cboTrangThai.setSelectedIndex(0);
            Calendar cal = Calendar.getInstance();
            dcDenNgay.setDate(cal.getTime());
            cal.add(Calendar.DAY_OF_MONTH, -7);
            dcTuNgay.setDate(cal.getTime());
            taiDuLieu();
        });
        btnXuatCsv.addActionListener(e -> xuatBaoCaoCsv());

        cboKyThongKe.addActionListener(e -> {
            int idx = cboKyThongKe.getSelectedIndex();
            Calendar cal = Calendar.getInstance();
            dcDenNgay.setDate(cal.getTime());
            if (idx == 0)
                cal.add(Calendar.DAY_OF_MONTH, -7);
            else if (idx == 1)
                cal.set(Calendar.DAY_OF_MONTH, 1);
            dcTuNgay.setDate(cal.getTime());
        });
    }

    private void taiDuLieu() {
        Date tuNgay = dcTuNgay.getDate();
        Date denNgay = dcDenNgay.getDate();
        String stName = (String) cboTrangThai.getSelectedItem();
        String st = switch (stName == null ? "" : stName) {
            case "Hoàn tất" -> "HOAN_TAT";
            case "Đang sửa" -> "DANG_SUA";
            case "Chờ linh kiện" -> "CHO_LINH_KIEN";
            case "Mới phân công" -> "CHO_SUA";
            default -> null;
        };

        new SwingWorker<ThongKeKTVData, Void>() {
            @Override
            protected ThongKeKTVData doInBackground() {
                return controller.layThongKeKTV(tuNgay, denNgay);
            }

            @Override
            protected void done() {
                try {
                    ThongKeKTVData kpi = get();
                    int tong = kpi.tongPhieu();
                    int hoanTat = kpi.phieuHoanTat();
                    int dangSua = kpi.phieuDangSua();
                    int choLK = kpi.phieuChoLinhKien();
                    int moi = Math.max(0, tong - hoanTat - dangSua - choLK);

                    lblKpi1Number.setText(String.valueOf(tong));
                    lblKpi1Title.setText("Tổng phiếu được phân công · " + tong + " phiếu");
                    lblKpi1Sub.setText(moi + " mới · " + dangSua + " đang sửa · " + choLK + " chờ linh kiện");

                    lblKpi2Number.setText(String.valueOf(dangSua));
                    lblKpi2Title.setText("Đang sửa · " + dangSua + " phiếu");
                    lblKpi2Sub.setText(pct(dangSua, tong) + " tổng phiếu");

                    lblKpi3Number.setText(String.valueOf(hoanTat));
                    lblKpi3Title.setText("Hoàn tất · " + hoanTat + " phiếu");
                    lblKpi3Sub.setText(pct(hoanTat, tong) + " tổng phiếu");

                    lblKpi4Number.setText(String.valueOf(choLK));
                    lblKpi4Title.setText("Chờ linh kiện · " + choLK + " phiếu");
                    lblKpi4Sub.setText(pct(choLK, tong) + " tổng phiếu");

                    String pctHoanTat = pct(hoanTat, tong);
                    lblTyLeHoanTat.setText(pctHoanTat);
                    lblSubTyLe.setText(hoanTat + "/" + tong + " phiếu đã hoàn tất trong kỳ");

                    lblCountInPeriod.setText(tong + " phiếu trong kỳ");
                    pnlDonutChart.capNhat(moi, dangSua, hoanTat, choLK);
                } catch (Exception ignored) {
                }
            }
        }.execute();

        new SwingWorker<List<PhieuSuaChuaItem>, Void>() {
            @Override
            protected List<PhieuSuaChuaItem> doInBackground() {
                return controller.timPhieuKTV(null, st, tuNgay, denNgay);
            }

            @Override
            protected void done() {
                try {
                    listPhieu = get();
                    model.setRowCount(0);
                    double tongGioXuLy = 0;
                    int countHoanTat = 0;
                    for (PhieuSuaChuaItem p : listPhieu) {
                        boolean isDone = "HOAN_TAT".equalsIgnoreCase(p.trangThai());
                        String strNgayHoanTat = "—";
                        String strThoiGian = "—";
                        if (isDone) {
                            java.sql.Timestamp ngHT = p.ngayHoanTat() != null ? p.ngayHoanTat() : p.ngayTiepNhan();
                            strNgayHoanTat = DATE_FORMAT.format(ngHT);
                            double hours = 1.0;
                            if (p.ngayHoanTat() != null && p.ngayTiepNhan() != null) {
                                double diff = (p.ngayHoanTat().getTime() - p.ngayTiepNhan().getTime()) / 3600000.0;
                                hours = Math.max(0.5, Math.round(diff * 10.0) / 10.0);
                            }
                            strThoiGian = String.format(java.util.Locale.US, "%.1fh", hours);
                            tongGioXuLy += hours;
                            countHoanTat++;
                        }
                        model.addRow(new Object[] {
                                p.maPhieu(),
                                p.bienSoXe(),
                                DATE_FORMAT.format(p.ngayTiepNhan()),
                                tenTrangThai(p.trangThai()),
                                strNgayHoanTat,
                                strThoiGian
                        });
                    }
                    if (countHoanTat > 0) {
                        double avg = tongGioXuLy / countHoanTat;
                        lblThoiGianTB.setText(String.format(java.util.Locale.US, "%.1fh", avg));
                    } else {
                        lblThoiGianTB.setText("—");
                    }
                    lblTableCount.setText(listPhieu.size() + " bản ghi · " + listPhieu.size() + " phiếu trong kỳ");
                    pnlBarChart.capNhatTheoList(listPhieu);
                } catch (Exception ignored) {
                }
            }
        }.execute();

        // Load YC linh kiện của tôi (từ DB thực tế)
        new SwingWorker<int[], Void>() {
            @Override
            protected int[] doInBackground() {
                try {
                    int choDuyet = ycController.demYeuCauKTV(TrangThaiYeuCau.CHO_DUYET, tuNgay, denNgay);
                    int daDuyet = ycController.demYeuCauKTV(TrangThaiYeuCau.DA_DUYET, tuNgay, denNgay)
                            + ycController.demYeuCauKTV(TrangThaiYeuCau.DA_NHAP_KHO, tuNgay, denNgay);
                    int tuChoi = ycController.demYeuCauKTV(TrangThaiYeuCau.TU_CHOI, tuNgay, denNgay);
                    return new int[] { choDuyet, daDuyet, tuChoi };
                } catch (Exception e) {
                    return new int[] { 0, 0, 0 };
                }
            }

            @Override
            protected void done() {
                try {
                    int[] counts = get();
                    lblYcChoDuyet.setText(String.valueOf(counts[0]));
                    lblYcDaDuyet.setText(String.valueOf(counts[1]));
                    lblYcTuChoi.setText(String.valueOf(counts[2]));
                    int total = counts[0] + counts[1] + counts[2];
                    lblYcSummary.setText(counts[0] + " yêu cầu chờ phê duyệt chủ cửa hàng / " + total
                            + " yêu cầu linh kiện đã gửi trong kỳ");
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private static String pct(int part, int total) {
        if (total <= 0)
            return "0%";
        return String.format(Locale.US, "%.1f%%", (part * 100.0) / total);
    }

    private void xuatBaoCaoCsv() {
        if (listPhieu.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có dữ liệu để xuất.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("ThongKe_SuaChua_KTV.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(fc.getSelectedFile(), StandardCharsets.UTF_8)) {
                pw.write('\ufeff');
                pw.println("MaPhieu,BienSoXe,NgayPhanCong,TrangThai,NgayHoanTat,ThoiGianXuLy");
                for (PhieuSuaChuaItem p : listPhieu) {
                    pw.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                            p.maPhieu(), p.bienSoXe(), DATE_FORMAT.format(p.ngayTiepNhan()),
                            tenTrangThai(p.trangThai()),
                            "HOAN_TAT".equals(p.trangThai()) ? DATE_FORMAT.format(p.ngayTiepNhan()) : "—", "4.2h");
                }
                JOptionPane.showMessageDialog(this, "Xuất file thành công!", "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static Date parseDate(String text) {
        if (text == null || text.isBlank())
            return null;
        try {
            return DATE_FORMAT.parse(text.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static void styleTable(JTable table) {
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setForeground(BODY);
        table.setRowHeight(38);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        table.setSelectionBackground(new Color(0xEDF4FF));
        table.setSelectionForeground(PRIMARY);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setForeground(BODY);
        table.getTableHeader().setBackground(new Color(0xF7F9FC));
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.setAutoCreateRowSorter(true);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);
        table.getColumnModel().getColumn(1).setCellRenderer(center);
        table.getColumnModel().getColumn(2).setCellRenderer(center);
        table.getColumnModel().getColumn(3).setCellRenderer(new StatusBadgeRenderer());
        table.getColumnModel().getColumn(4).setCellRenderer(center);
        table.getColumnModel().getColumn(5).setCellRenderer(center);
    }

    public static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int col) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            if (value == null)
                return lbl;
            String s = value.toString().trim();
            Color bg = new Color(0xF1F5F9);
            Color fg = new Color(0x64748B);
            if (s.contains("Hoàn tất") || s.contains("Hoàn thành")) {
                bg = new Color(0xECFDF5);
                fg = new Color(0x059669);
            } else if (s.contains("Đang sửa")) {
                bg = new Color(0xEFF6FF);
                fg = new Color(0x2563EB);
            } else if (s.contains("Chờ linh kiện")) {
                bg = new Color(0xFFFBEB);
                fg = new Color(0xD97706);
            } else if (s.contains("Mới")) {
                bg = new Color(0xF1F5F9);
                fg = new Color(0x64748B);
            }
            if (!isSelected) {
                lbl.setBackground(bg);
                lbl.setForeground(fg);
                lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            }
            return lbl;
        }
    }

    private static String tenTrangThai(String status) {
        return switch (status == null ? "" : status) {
            case "CHO_SUA" -> "Mới phân công";
            case "DANG_SUA" -> "Đang sửa";
            case "CHO_LINH_KIEN" -> "Chờ linh kiện";
            case "HOAN_TAT" -> "Hoàn tất";
            default -> status;
        };
    }

    private static JLabel label(String text, int size, Color color, int style) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", style, size));
        l.setForeground(color);
        return l;
    }

    private static void addFull(JPanel parent, JComponent child) {
        child.setAlignmentX(Component.LEFT_ALIGNMENT);
        child.setMaximumSize(new Dimension(Integer.MAX_VALUE, child.getPreferredSize().height));
        parent.add(child);
    }

    private static JButton button(String text, boolean primary, int width) {
        JButton b = new MainFrame.RoundedButton(text, 10,
                primary ? PRIMARY : WHITE,
                primary ? PRIMARY_HV : new Color(0xF1F5F9),
                primary ? PRIMARY : new Color(0xC9D5E5));
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setForeground(primary ? WHITE : BODY);
        b.setPreferredSize(new Dimension(width, 38));
        b.setFocusPainted(false);
        return b;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BIỂU ĐỒ CỘT 7 NGÀY (Figma 172:5247)
    // ─────────────────────────────────────────────────────────────────────────
    private static class CotBieuDo7NgayPanel extends JPanel {
        private int[] data = new int[0];
        private String[] days = new String[0];
        private String legendText = "Chưa có dữ liệu";

        public CotBieuDo7NgayPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(480, 260));
        }

        public void capNhatTheoList(List<PhieuSuaChuaItem> list) {
            // Nhóm phiếu theo ngày tiếp nhận (dd/MM/yyyy), lấy 7 ngày gần nhất
            java.util.TreeMap<String, Integer> byDay = new java.util.TreeMap<>();
            java.text.SimpleDateFormat keyFmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
            java.text.SimpleDateFormat labelFmt = new java.text.SimpleDateFormat("dd/MM");
            for (PhieuSuaChuaItem p : list) {
                if (p.ngayTiepNhan() != null) {
                    String key = keyFmt.format(p.ngayTiepNhan());
                    byDay.merge(key, 1, Integer::sum);
                }
            }
            java.util.List<java.util.Map.Entry<String, Integer>> entries =
                    new java.util.ArrayList<>(byDay.entrySet());
            int n = Math.min(7, entries.size());
            if (n == 0) {
                data = new int[0];
                days = new String[0];
                legendText = "Chưa có dữ liệu";
                repaint();
                return;
            }
            data = new int[n];
            days = new String[n];
            int total = 0;
            java.text.SimpleDateFormat parseKey = new java.text.SimpleDateFormat("yyyy-MM-dd");
            for (int i = 0; i < n; i++) {
                java.util.Map.Entry<String, Integer> e = entries.get(entries.size() - n + i);
                data[i] = e.getValue();
                total += data[i];
                try {
                    days[i] = labelFmt.format(parseKey.parse(e.getKey()));
                } catch (Exception ex) {
                    days[i] = e.getKey().substring(5).replace("-", "/");
                }
            }
            int avg = n > 0 ? (total / n) : 0;
            legendText = "• Phiếu tiếp nhận: " + total + " phiếu / " + n + " ngày · Trung bình " + avg + " phiếu/ngày";
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Nền trắng bo góc
            g2.setColor(WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

            // Title
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.setColor(TEXT);
            g2.drawString("Hiệu suất hoàn tất theo ngày", 18, 28);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.setColor(SUBTLE);
            g2.drawString("Đơn vị: phiếu", getWidth() - 90, 28);

            // Nếu không có dữ liệu
            if (data.length == 0) {
                g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
                g2.setColor(SUBTLE);
                String msg = "Chưa có dữ liệu";
                int msgW = g2.getFontMetrics().stringWidth(msg);
                g2.drawString(msg, (getWidth() - msgW) / 2, getHeight() / 2);
                g2.setColor(SUBTLE);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                g2.drawString(legendText, 18, getHeight() - 12);
                g2.dispose();
                return;
            }

            int n = data.length;
            int paddingX = 40;
            int availW = getWidth() - paddingX * 2;
            int barW = Math.min(32, Math.max(14, availW / n - 8));
            int gap = n > 1 ? (availW - barW * n) / (n - 1) : 0;
            int baselineY = getHeight() - 48;
            int maxH = 130;

            int maxVal = 1;
            for (int v : data)
                if (v > maxVal)
                    maxVal = v;

            for (int i = 0; i < n; i++) {
                int x = paddingX + i * (barW + gap);
                int h = (int) (((double) data[i] / maxVal) * maxH);
                if (h < 4 && data[i] > 0) h = 4;
                int y = baselineY - h;

                // Value above bar
                g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                g2.setColor(BODY);
                String valStr = String.valueOf(data[i]);
                int valW = g2.getFontMetrics().stringWidth(valStr);
                g2.drawString(valStr, x + (barW - valW) / 2, y - 4);

                // Bar
                g2.setColor(PRIMARY);
                g2.fillRoundRect(x, y, barW, h, 6, 6);

                // Day label below
                g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                g2.setColor(SUBTLE);
                int dayW = g2.getFontMetrics().stringWidth(days[i]);
                g2.drawString(days[i], x + (barW - dayW) / 2, baselineY + 18);
            }

            // Legend below
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.setColor(SUBTLE);
            g2.drawString(legendText, 18, getHeight() - 12);

            g2.dispose();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BIỂU ĐỒ DONUT PHÂN BỐ TRẠNG THÁI (Figma 172:5247)
    // ─────────────────────────────────────────────────────────────────────────
    private static class DonutTrangThaiPanel extends JPanel {
        private int moi = 12, dangSua = 12, hoanTat = 84, choLK = 20;

        public DonutTrangThaiPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(440, 260));
        }

        public void capNhat(int moi, int dangSua, int hoanTat, int choLK) {
            this.moi = moi;
            this.dangSua = dangSua;
            this.hoanTat = hoanTat;
            this.choLK = choLK;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.setColor(TEXT);
            g2.drawString("Phân bố trạng thái", 18, 28);

            int total = moi + dangSua + hoanTat + choLK;
            if (total == 0)
                total = 1;

            int cx = 95, cy = 135, r = 62, thickness = 22;

            double aHoanTat = (hoanTat * 360.0) / total;
            double aDangSua = (dangSua * 360.0) / total;
            double aMoi = (moi * 360.0) / total;
            double aChoLK = 360.0 - aHoanTat - aDangSua - aMoi;

            g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));

            double start = 90;
            // Hoan tat: xanh la
            g2.setColor(SUCCESS);
            g2.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, start, -aHoanTat, Arc2D.OPEN));
            start -= aHoanTat;

            // Dang sua: xanh duong
            g2.setColor(PRIMARY);
            g2.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, start, -aDangSua, Arc2D.OPEN));
            start -= aDangSua;

            // Cho LK: vang cam
            g2.setColor(WARN);
            g2.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, start, -aChoLK, Arc2D.OPEN));
            start -= aChoLK;

            // Moi: xam
            g2.setColor(new Color(0x94A3B8));
            g2.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, start, -aMoi, Arc2D.OPEN));

            // Tam Donut
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            g2.setColor(TEXT);
            String totStr = String.valueOf(total);
            int totW = g2.getFontMetrics().stringWidth(totStr);
            g2.drawString(totStr, cx - totW / 2, cy + 2);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.setColor(SUBTLE);
            g2.drawString("Tổng phiếu", cx - 25, cy + 16);

            // Legend bên phải
            int lx = 205, ly = 80;
            drawLegendItem(g2, lx, ly, new Color(0x94A3B8), "Mới phân công", moi);
            drawLegendItem(g2, lx, ly + 28, PRIMARY, "Đang sửa", dangSua);
            drawLegendItem(g2, lx, ly + 56, SUCCESS, "Hoàn tất", hoanTat);
            drawLegendItem(g2, lx, ly + 84, WARN, "Chờ linh kiện", choLK);

            // Footer
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.setColor(SUBTLE);
            g2.drawString("Hoàn tất: " + pct(hoanTat, total) + "  |  Chưa hoàn tất: " + pct(total - hoanTat, total), 18,
                    getHeight() - 14);

            g2.dispose();
        }

        private void drawLegendItem(Graphics2D g2, int x, int y, Color dotColor, String label, int count) {
            g2.setColor(dotColor);
            g2.fillOval(x, y - 8, 9, 9);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.setColor(BODY);
            g2.drawString(label, x + 16, y);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.setColor(TEXT);
            g2.drawString(String.valueOf(count), x + 140, y);
        }
    }
}
