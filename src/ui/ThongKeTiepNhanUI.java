package ui;

import controller.ThongKeTiepNhanController;
import dao.ThongKeTiepNhanDao.PhieuRow;
import dao.ThongKeTiepNhanDao.ThongKeData;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static ui.MainFrame.*;

/**
 * Màn hình Thống kê tiếp nhận (UC013 - Tiếp nhận viên).
 * Bám sát Figma: Thong_Ke_Tiep_Nhan.jpg
 * Hiển thị KPI, Biểu đồ xu hướng (vẽ bằng Graphics2D), Biểu đồ tròn trạng thái và bảng chi tiết.
 */
public class ThongKeTiepNhanUI extends JPanel {

    private final ThongKeTiepNhanController controller = new ThongKeTiepNhanController();
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy");

    // Filter controls
    private JComboBox<String> cboKyThongKe;
    private JTextField txtTuNgay;
    private JTextField txtDenNgay;
    private JComboBox<String> cboTrangThai;

    // KPI values
    private JLabel lblTongPhieuVal;
    private JLabel lblSoXeVal;
    private JLabel lblKhachMoiVal;
    private JLabel lblChoXuLyVal;

    // Chart panels
    private ChartBarPanel pnlBarChart;
    private ChartDonutPanel pnlDonutChart;

    // Table
    private DefaultTableModel modelTable;
    private JTable tblChiTiet;

    public ThongKeTiepNhanUI() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildScrollBody(), BorderLayout.CENTER);

        apDungKyThongKe(1); // Mặc định: 7 ngày gần nhất
    }

    private JPanel buildHeader() {
        JPanel pnl = new JPanel(new BorderLayout(12, 0));
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(16, 24, 12, 24));

        JPanel titleBlock = new JPanel(new BorderLayout(0, 2));
        titleBlock.setOpaque(false);

        JLabel lblTitle = new JLabel("Thống kê tiếp nhận sửa chữa");
        lblTitle.setFont(f(Font.BOLD, 20));
        lblTitle.setForeground(C_TEXT_HEAD);

        JLabel lblSub = new JLabel("Báo cáo số lượng xe, khách hàng và hiệu suất tiếp nhận trong phạm vi công việc của bạn");
        lblSub.setFont(f(Font.PLAIN, 12));
        lblSub.setForeground(C_TEXT_SUBTLE);

        titleBlock.add(lblTitle, BorderLayout.NORTH);
        titleBlock.add(lblSub, BorderLayout.SOUTH);
        pnl.add(titleBlock, BorderLayout.WEST);

        return pnl;
    }

    private JScrollPane buildScrollBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 24, 24, 24));

        body.add(buildFilterBar());
        body.add(Box.createVerticalStrut(14));
        body.add(buildKpiCards());
        body.add(Box.createVerticalStrut(14));
        body.add(buildChartsRow());
        body.add(Box.createVerticalStrut(14));
        body.add(buildTableChiTiet());

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. THANH BỘ LỌC
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildFilterBar() {
        JPanel card = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        card.setBackground(C_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(8, 12, 8, 12)));

        card.add(new JLabel("Kỳ thống kê:"));
        cboKyThongKe = new JComboBox<>(new String[]{"Hôm nay", "7 ngày gần nhất", "Tháng này", "Tùy chọn"});
        cboKyThongKe.setFont(f(Font.PLAIN, 13));
        cboKyThongKe.addActionListener(e -> apDungKyThongKe(cboKyThongKe.getSelectedIndex()));
        card.add(cboKyThongKe);

        card.add(new JLabel("Từ ngày:"));
        txtTuNgay = new JTextField(8);
        txtTuNgay.setFont(f(Font.PLAIN, 13));
        card.add(txtTuNgay);

        card.add(new JLabel("Đến ngày:"));
        txtDenNgay = new JTextField(8);
        txtDenNgay.setFont(f(Font.PLAIN, 13));
        card.add(txtDenNgay);

        card.add(new JLabel("Trạng thái:"));
        cboTrangThai = new JComboBox<>(new String[]{"-- Tất cả --", "CHO_SUA", "DANG_SUA", "CHO_LINH_KIEN", "HOAN_TAT"});
        cboTrangThai.setFont(f(Font.PLAIN, 13));
        card.add(cboTrangThai);

        JButton btnXem = new RoundedButton("📊 Xem thống kê", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
        btnXem.setFont(f(Font.BOLD, 12));
        btnXem.setForeground(C_WHITE);
        btnXem.setPreferredSize(new Dimension(130, 32));
        btnXem.addActionListener(e -> taiDuLieuThongKe());
        card.add(btnXem);

        JButton btnLamMoi = new RoundedButton("↺ Làm mới", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnLamMoi.setFont(f(Font.PLAIN, 12));
        btnLamMoi.setPreferredSize(new Dimension(90, 32));
        btnLamMoi.addActionListener(e -> apDungKyThongKe(1));
        card.add(btnLamMoi);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. KPI CARDS
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildKpiCards() {
        JPanel pnl = new JPanel(new GridLayout(1, 4, 12, 0));
        pnl.setOpaque(false);
        pnl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));

        lblTongPhieuVal = new JLabel("0");
        lblSoXeVal = new JLabel("0");
        lblKhachMoiVal = new JLabel("0");
        lblChoXuLyVal = new JLabel("0");

        pnl.add(createKpiCard("TỔNG PHIẾU TIẾP NHẬN", lblTongPhieuVal, "Phiếu đã lập trong kỳ", true));
        pnl.add(createKpiCard("XE ĐƯỢC TIẾP NHẬN", lblSoXeVal, "Biển số xe khác nhau", false));
        pnl.add(createKpiCard("KHÁCH HÀNG MỚI", lblKhachMoiVal, "Tạo mới trong kỳ", false));
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
    // 3. CHARTS ROW (Graphics2D)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildChartsRow() {
        JPanel pnl = new JPanel(new GridLayout(1, 2, 14, 0));
        pnl.setOpaque(false);
        pnl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        pnlBarChart = new ChartBarPanel("Xu hướng tiếp nhận theo ngày");
        pnl.add(pnlBarChart);

        pnlDonutChart = new ChartDonutPanel("Phân bố trạng thái phiếu");
        pnl.add(pnlDonutChart);

        return pnl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. BẢNG CHI TIẾT
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTableChiTiet() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(C_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)));

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

        JScrollPane scroll = new JScrollPane(tblChiTiet);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        scroll.setPreferredSize(new Dimension(0, 200));
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGIC DỮ LIỆU
    // ─────────────────────────────────────────────────────────────────────────
    private void apDungKyThongKe(int index) {
        Date tuNgay = null;
        Date denNgay = new Date();

        switch (index) {
            case 0 -> { // Hôm nay
                tuNgay = new Date();
            }
            case 1 -> { // 7 ngày gần nhất
                Date[] range = controller.layKhoang7NgayGanNhat();
                tuNgay = range[0];
                denNgay = range[1];
            }
            case 2 -> { // Tháng này
                Date[] range = controller.layKhoangThangNay();
                tuNgay = range[0];
                denNgay = range[1];
            }
            default -> {} // Tùy chọn giữ nguyên text
        }

        if (tuNgay != null) txtTuNgay.setText(dateFmt.format(tuNgay));
        if (denNgay != null) txtDenNgay.setText(dateFmt.format(denNgay));

        taiDuLieuThongKe();
    }

    private void taiDuLieuThongKe() {
        Date tuNgay = null;
        Date denNgay = null;
        try {
            if (!txtTuNgay.getText().isBlank()) tuNgay = dateFmt.parse(txtTuNgay.getText().trim());
            if (!txtDenNgay.getText().isBlank()) denNgay = dateFmt.parse(txtDenNgay.getText().trim());
        } catch (Exception ignored) {}

        String tt = cboTrangThai.getSelectedIndex() > 0 ? (String) cboTrangThai.getSelectedItem() : null;

        final Date finalTuNgay = tuNgay;
        final Date finalDenNgay = denNgay;
        final String finalTrangThai = tt;

        new SwingWorker<ThongKeData, Void>() {
            @Override
            protected ThongKeData doInBackground() {
                return controller.layThongKe(finalTuNgay, finalDenNgay, finalTrangThai);
            }

            @Override
            protected void done() {
                try {
                    ThongKeData data = get();
                    if (data == null) return;

                    lblTongPhieuVal.setText(String.valueOf(data.tongPhieu));
                    lblSoXeVal.setText(String.valueOf(data.soXe));
                    lblKhachMoiVal.setText(String.valueOf(data.khachHangMoi));
                    lblChoXuLyVal.setText(String.valueOf(data.choXuLy));

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
                                    r.ktv != null ? r.ktv : "Chưa phân công",
                                    r.trangThai
                            });
                        }
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CUSTOM GRAPHICS: BAR CHART
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

                // Cột bar xanh
                g2.setColor(C_PRIMARY);
                g2.fillRoundRect(bx, by, barWidth, bh, 6, 6);

                // Số lượng trên đầu cột
                g2.setFont(f(Font.BOLD, 11));
                g2.setColor(C_TEXT_MAIN);
                g2.drawString(String.valueOf(e.getValue()), bx + 4, by - 4);

                // Nhãn ngày dưới chân
                g2.setFont(f(Font.PLAIN, 10));
                g2.setColor(C_TEXT_SUBTLE);
                g2.drawString(e.getKey(), bx, y0 + 14);

                i++;
            }

            g2.dispose();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CUSTOM GRAPHICS: DONUT / PIE CHART
    // ─────────────────────────────────────────────────────────────────────────
    private static class ChartDonutPanel extends JPanel {
        private final String title;
        private Map<String, Integer> data;
        private final Color[] colors = new Color[]{
                new Color(0x3B82F6), // Xanh dương
                new Color(0x10B981), // Xanh lá
                new Color(0xF59E0B), // Vàng cam
                new Color(0xEF4444), // Đỏ
                new Color(0x8B5CF6)  // Tím
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
                g2.dispose();
                return;
            }

            int size = Math.min(getWidth() / 2 - 20, getHeight() - 60);
            int cx = 20;
            int cy = 40;

            double startAngle = 90;
            int idx = 0;

            // Chú thích legend
            int legX = cx + size + 20;
            int legY = 60;

            for (Map.Entry<String, Integer> e : data.entrySet()) {
                double arcAngle = 360.0 * e.getValue() / total;
                Color col = colors[idx % colors.length];

                g2.setColor(col);
                g2.fill(new Arc2D.Double(cx, cy, size, size, startAngle, arcAngle, Arc2D.PIE));

                // Legend
                g2.fillRect(legX, legY - 10, 12, 12);
                g2.setColor(C_TEXT_MAIN);
                g2.setFont(f(Font.PLAIN, 12));
                g2.drawString(e.getKey() + ": " + e.getValue() + " (" + (int)(e.getValue() * 100.0 / total) + "%)", legX + 18, legY);

                legY += 22;
                startAngle += arcAngle;
                idx++;
            }

            // Khoét lỗ giữa tạo donut
            int hole = size / 2;
            g2.setColor(C_WHITE);
            g2.fillOval(cx + (size - hole) / 2, cy + (size - hole) / 2, hole, hole);

            g2.dispose();
        }
    }
}
