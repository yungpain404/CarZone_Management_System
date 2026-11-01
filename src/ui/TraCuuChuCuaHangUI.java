package ui;

import controller.TraCuuController;
import dao.TraCuuDAO;
import entity.LinhKien;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Tra cứu chỉ đọc của Chủ cửa hàng: linh kiện, khách hàng, lịch sử sửa chữa. */
public class TraCuuChuCuaHangUI extends JPanel {
    private static final Color BG = new Color(0xF4F7FB), WHITE = Color.WHITE, BORDER = new Color(0xE3E9F2);
    private static final Color PRIMARY = new Color(0x2563EB), TEXT = new Color(0x14233D), BODY = new Color(0x40516A), SUBTLE = new Color(0x7A889C);
    private static final NumberFormat MONEY = NumberFormat.getIntegerInstance(new Locale("vi", "VN"));
    private static final SimpleDateFormat DATE_TIME = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private final TraCuuController controller = new TraCuuController();
    private final JLabel lblStatus = label("Chọn loại tra cứu và nhập từ khóa để bắt đầu.", 12, SUBTLE, Font.PLAIN);
    private final JTabbedPane tabs = new JTabbedPane();
    private boolean dangXuLy;
    private final PartPanel partPanel = new PartPanel();
    private final CustomerPanel customerPanel = new CustomerPanel();
    private final HistoryPanel historyPanel = new HistoryPanel();

    public TraCuuChuCuaHangUI() {
        setLayout(new BorderLayout()); setBackground(BG); setBorder(new EmptyBorder(24, 24, 22, 24));
        JPanel root = new MainFrame.ScrollableContentPanel(); root.setOpaque(false);
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0)); heading.setOpaque(false);
        heading.add(label("🔎", 25, TEXT, Font.PLAIN)); heading.add(label("Tra cứu", 24, TEXT, Font.BOLD));
        addFull(root, heading); root.add(Box.createVerticalStrut(18));
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13)); tabs.setBackground(BG); tabs.setFocusable(false);
        tabs.addTab("Linh kiện", partPanel.build()); tabs.addTab("Khách hàng", customerPanel.build()); tabs.addTab("Lịch sử sửa chữa", historyPanel.build());
        addFull(root, tabs); root.add(Box.createVerticalStrut(8)); addFull(root, lblStatus);
        JScrollPane scroll = new JScrollPane(root); scroll.setBorder(null); scroll.setOpaque(false); scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);
        tabs.addChangeListener(e -> refreshButtonStates());
    }

    private abstract class SearchPanel<T> {
        final JTextField query = new JTextField();
        final JButton search = button("Tìm kiếm", true, 98), clear = button("Làm mới", false, 92);
        final DefaultTableModel model;
        final JTable table;
        final JPanel detail = new JPanel(new BorderLayout());
        List<T> rows = new ArrayList<>();
        boolean searched;

        SearchPanel(String... columns) {
            model = new DefaultTableModel(columns, 0) { @Override public boolean isCellEditable(int row, int col) { return false; } };
            table = new JTable(model); styleTable(table);
            table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) selectionChanged(); });
            table.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) { if (e.getClickCount() == 2) selectionChanged(); }
            });
            query.setFont(new Font("SansSerif", Font.PLAIN, 13)); query.setBorder(new EmptyBorder(0, 11, 0, 8));
            search.addActionListener(e -> executeSearch()); query.addActionListener(e -> executeSearch());
            clear.addActionListener(e -> reset());
        }

        JComponent base(String placeholder, String filterLabel, JComponent filter) {
            JPanel panel = new JPanel(); panel.setOpaque(false); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(new EmptyBorder(18, 0, 0, 0)); panel.setAlignmentX(Component.LEFT_ALIGNMENT);
            JPanel filters = new JPanel(new GridBagLayout()); filters.setOpaque(false); filters.setAlignmentX(Component.LEFT_ALIGNMENT);
            JPanel searchField = field("Từ khóa tra cứu", query); query.putClientProperty("JTextField.placeholderText", placeholder);
            GridBagConstraints g = new GridBagConstraints(); g.gridy = 0; g.anchor = GridBagConstraints.WEST; g.insets = new Insets(0, 0, 0, 12);
            g.gridx = 0; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL; filters.add(searchField, g);
            if (filter != null) {
                JPanel filterPanel = field(filterLabel, filter); g.gridx = 1; g.weightx = 0; g.fill = GridBagConstraints.NONE; filters.add(filterPanel, g);
            }
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 22)); buttons.setOpaque(false); buttons.add(search); buttons.add(clear);
            g.gridx = 2; g.weightx = 0; g.insets = new Insets(0, 0, 0, 0); filters.add(buttons, g);
            addFull(panel, filters); panel.add(Box.createVerticalStrut(16));
            JPanel statusCard = card(new BorderLayout()); statusCard.setBorder(new EmptyBorder(14, 16, 14, 16));
            statusCard.add(label("Trạng thái tra cứu", 15, TEXT, Font.BOLD), BorderLayout.CENTER); addFull(panel, statusCard);
            panel.add(Box.createVerticalStrut(16));
            JScrollPane resultScroll = new JScrollPane(table); resultScroll.setBorder(BorderFactory.createLineBorder(BORDER)); resultScroll.getViewport().setBackground(WHITE);
            resultScroll.setPreferredSize(new Dimension(980, 300)); JPanel resultCard = card(new BorderLayout()); resultCard.add(resultScroll);
            addFull(panel, resultCard); panel.add(Box.createVerticalStrut(16));
            detail.setOpaque(true); detail.setBackground(WHITE); detail.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), new EmptyBorder(16, 16, 16, 16)));
            detail.add(label("Chi tiết kết quả", 15, TEXT, Font.BOLD), BorderLayout.NORTH);
            detail.add(label("Chưa có kết quả để hiển thị.", 13, SUBTLE, Font.PLAIN), BorderLayout.CENTER);
            detail.setPreferredSize(new Dimension(980, 150)); addFull(panel, detail);
            return panel;
        }

        abstract List<T> searchRows(String text);
        abstract void fillRows(List<T> values);
        abstract void selectionChanged();
        void beforeSearch() { }
        void resetFilters() { }
        boolean canSearchWithoutKeyword() { return false; }
        void executeSearch() {
            String text = query.getText().trim();
            if (!UIInputRules.isSearch(text)) { message("Từ khóa tối đa 150 ký tự; chỉ nhập chữ, số và dấu câu.", JOptionPane.WARNING_MESSAGE); query.requestFocusInWindow(); return; }
            if (text.isEmpty() && !canSearchWithoutKeyword()) { message("Nhập từ khóa tra cứu trước khi tìm kiếm.", JOptionPane.INFORMATION_MESSAGE); query.requestFocusInWindow(); return; }
            beforeSearch();
            setBusy(true); lblStatus.setText("Đang tìm kiếm…");
            runAsync(() -> searchRows(text), values -> {
                rows = values; searched = true; fillRows(values); lblStatus.setText(values.isEmpty() ? "Không tìm thấy kết quả phù hợp." : "Tìm thấy " + values.size() + " kết quả.");
            });
        }
        void reset() {
            query.setText(""); rows.clear(); searched = false; model.setRowCount(0);
            table.clearSelection(); resetFilters();
            setDetail(label("Chưa có kết quả để hiển thị.", 13, SUBTLE, Font.PLAIN));
            lblStatus.setText("Chọn loại tra cứu và nhập từ khóa để bắt đầu."); refreshButtonStates(); query.requestFocusInWindow();
        }
        T selected() {
            int view = table.getSelectedRow(); if (view < 0) return null; int row = table.convertRowIndexToModel(view);
            return row >= 0 && row < rows.size() ? rows.get(row) : null;
        }
        void selectFirst() { if (model.getRowCount() > 0) table.setRowSelectionInterval(0, 0); }
        void setDetail(JComponent content) {
            detail.removeAll(); detail.add(content, BorderLayout.CENTER);
            int height = Math.max(150, content.getPreferredSize().height + 34);
            detail.setPreferredSize(new Dimension(980, height)); detail.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
            detail.revalidate(); detail.repaint();
        }
    }

    private final class PartPanel extends SearchPanel<LinhKien> {
        private final JComboBox<String> status = new JComboBox<>(new String[]{"Tất cả", "Đang kinh doanh", "Ngừng kinh doanh", "Còn hàng", "Hết hàng"});
        private String filterForSearch = "Tất cả";
        PartPanel() { super("Mã linh kiện", "Tên linh kiện", "Đơn vị", "Giá bán (VNĐ)", "Tồn kho", "Trạng thái"); }
        JComponent build() { status.setPreferredSize(new Dimension(190, 42)); return base("Nhập mã hoặc tên linh kiện…", "Trạng thái", status); }
        @Override boolean canSearchWithoutKeyword() { return true; }
        @Override void resetFilters() { status.setSelectedIndex(0); filterForSearch = "Tất cả"; }
        @Override void beforeSearch() { filterForSearch = (String) status.getSelectedItem(); }
        @Override List<LinhKien> searchRows(String text) { return controller.timLinhKien(text, filterForSearch); }
        @Override void fillRows(List<LinhKien> values) {
            model.setRowCount(0); for (LinhKien lk : values) model.addRow(new Object[]{lk.getMaLK(), lk.getTenLK(), lk.getDonViTinh(), money(lk.getDonGia()), lk.getSoLuongTon(), lk.isDangKinhDoanh() ? "Đang kinh doanh" : "Ngừng kinh doanh"});
            selectFirst();
        }
        @Override void selectionChanged() {
            LinhKien lk = selected(); if (lk == null) return;
            JPanel content = detailGrid("Linh kiện — " + lk.getMaLK(),
                    new String[][]{{"Mã linh kiện", lk.getMaLK()}, {"Tên linh kiện", lk.getTenLK()}, {"Trạng thái kinh doanh", lk.isDangKinhDoanh() ? "Đang kinh doanh" : "Ngừng kinh doanh"},
                            {"Đơn vị tính", lk.getDonViTinh()}, {"Giá bán", money(lk.getDonGia()) + " VNĐ"}, {"Tồn kho", lk.getSoLuongTon() + " " + lk.getDonViTinh()}});
            setDetail(content);
        }
    }

    private final class CustomerPanel extends SearchPanel<TraCuuDAO.KhachHangResult> {
        CustomerPanel() { super("Mã khách hàng", "Họ tên", "Số điện thoại", "CCCD", "Số xe", "Số phiếu"); }
        JComponent build() { return base("Nhập mã, tên, số điện thoại hoặc CCCD…", null, null); }
        @Override List<TraCuuDAO.KhachHangResult> searchRows(String text) { return controller.timKhachHang(text); }
        @Override void fillRows(List<TraCuuDAO.KhachHangResult> values) {
            model.setRowCount(0); for (var kh : values) model.addRow(new Object[]{kh.maKH(), kh.hoTen(), kh.soDienThoai(), blankDash(kh.cccd()), kh.soXe(), kh.soPhieu()}); selectFirst();
        }
        @Override void selectionChanged() {
            var kh = selected(); if (kh == null) return;
            setDetail(label("Đang tải thông tin khách hàng…", 13, SUBTLE, Font.PLAIN));
            runAsync(() -> controller.layXeKhachHang(kh.maKH()), vehicles -> {
                JPanel content = new JPanel(new BorderLayout(0, 12)); content.setOpaque(false);
                JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false); top.add(label("Khách hàng — " + kh.maKH(), 15, TEXT, Font.BOLD), BorderLayout.NORTH);
                JPanel info = new JPanel(new GridLayout(2, 3, 10, 8)); info.setOpaque(false); info.setBorder(new EmptyBorder(12, 0, 0, 0));
                info.add(infoCell("Họ tên", kh.hoTen())); info.add(infoCell("Số điện thoại", kh.soDienThoai())); info.add(infoCell("CCCD", blankDash(kh.cccd())));
                info.add(infoCell("Thành viên thân thiết", kh.thanhVienThanThiet() ? "Có" : "Không")); info.add(infoCell("Điểm tích lũy", number(kh.diemTichLuy()))); info.add(infoCell("Số phiếu sửa chữa", String.valueOf(kh.soPhieu())));
                top.add(info, BorderLayout.CENTER); content.add(top, BorderLayout.NORTH);
                StringBuilder text = new StringBuilder("Xe đang sở hữu (" + vehicles.size() + ")\n");
                if (vehicles.isEmpty()) text.append("Chưa có xe được ghi nhận.");
                else for (var xe : vehicles) text.append("• ").append(xe.bienSo()).append(" — ").append(xe.hangXe()).append(" ")
                        .append(blankDash(xe.model())).append(" · ").append(blankDash(xe.mauSac())).append(" · ")
                        .append(xe.namSanXuat() == null ? "N/A" : xe.namSanXuat()).append(" · ").append(number(xe.soKm())).append(" km\n");
                JTextArea vehiclesText = new JTextArea(text.toString()); vehiclesText.setEditable(false); vehiclesText.setLineWrap(true); vehiclesText.setWrapStyleWord(true);
                vehiclesText.setFont(new Font("SansSerif", Font.PLAIN, 13)); vehiclesText.setForeground(BODY); vehiclesText.setBackground(new Color(0xF7F9FC));
                vehiclesText.setBorder(new EmptyBorder(10, 12, 10, 12)); content.add(vehiclesText, BorderLayout.CENTER); setDetail(content);
            });
        }
    }

    private final class HistoryPanel extends SearchPanel<TraCuuDAO.LichSuResult> {
        HistoryPanel() { super("Mã phiếu", "Ngày tiếp nhận", "Biển số xe", "Khách hàng", "Số điện thoại", "Trạng thái", "Tổng tiền"); }
        JComponent build() { return base("Nhập mã phiếu, biển số, mã/tên khách hoặc điện thoại…", null, null); }
        @Override List<TraCuuDAO.LichSuResult> searchRows(String text) { return controller.timLichSu(text); }
        @Override void fillRows(List<TraCuuDAO.LichSuResult> values) {
            model.setRowCount(0); for (var p : values) model.addRow(new Object[]{p.maPhieu(), DATE_TIME.format(p.ngayTiepNhan()), p.bienSo(), p.tenKhachHang(), p.soDienThoai(), tenTrangThai(p.trangThai()), money(p.tongTien())}); selectFirst();
        }
        @Override void selectionChanged() {
            var p = selected(); if (p == null) return;
            setDetail(label("Đang tải chi tiết phiếu sửa chữa…", 13, SUBTLE, Font.PLAIN));
            runAsync(() -> controller.layHangMucPhieu(p.maPhieu()), items -> {
                JPanel content = new JPanel(); content.setOpaque(false); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
                addFull(content, label("Lịch sử sửa chữa — " + p.maPhieu(), 15, TEXT, Font.BOLD)); content.add(Box.createVerticalStrut(12));
                JPanel info = new JPanel(new GridLayout(2, 4, 10, 8)); info.setOpaque(false);
                info.add(infoCell("Khách hàng", p.tenKhachHang() + " · " + p.maKH())); info.add(infoCell("Điện thoại", p.soDienThoai()));
                info.add(infoCell("Xe", p.bienSo() + " · " + p.xe())); info.add(infoCell("Ngày tiếp nhận", DATE_TIME.format(p.ngayTiepNhan())));
                info.add(infoCell("Trạng thái", tenTrangThai(p.trangThai()))); info.add(infoCell("Tiếp nhận viên", blankDash(p.tiepNhanVien())));
                info.add(infoCell("Kỹ thuật viên", blankDash(p.kyThuatVien()))); info.add(infoCell("Yêu cầu ban đầu", p.yeuCau())); addFull(content, info);
                content.add(Box.createVerticalStrut(12));
                DefaultTableModel itemModel = new DefaultTableModel(new Object[]{"Loại", "Mã", "Tên hạng mục", "Số lượng", "Đơn giá", "Thành tiền"}, 0) {
                    @Override public boolean isCellEditable(int row, int col) { return false; }
                };
                for (var item : items) itemModel.addRow(new Object[]{item.loai(), item.ma(), item.ten(), item.soLuong(), money(item.donGia()), money(item.thanhTien())});
                JTable itemTable = new JTable(itemModel); styleTable(itemTable); JScrollPane itemScroll = new JScrollPane(itemTable); itemScroll.setBorder(BorderFactory.createLineBorder(BORDER)); itemScroll.setPreferredSize(new Dimension(900, Math.min(180, 42 + 34 * Math.max(1, items.size()))));
                addFull(content, itemScroll); content.add(Box.createVerticalStrut(10));
                JPanel totals = new JPanel(new GridLayout(1, 3, 10, 0)); totals.setOpaque(false);
                totals.add(infoCell("Tiền dịch vụ", money(p.tienDichVu()) + " VNĐ")); totals.add(infoCell("Tiền linh kiện", money(p.tienLinhKien()) + " VNĐ")); totals.add(infoCell("Tổng phiếu", money(p.tongTien()) + " VNĐ")); addFull(content, totals);
                setDetail(content);
            });
        }
    }

    private Component partPanelComponent() { return tabs.getComponentAt(0); }
    private Component customerPanelComponent() { return tabs.getComponentAt(1); }
    private void setBusy(boolean value) { dangXuLy = value; refreshButtonStates(); }
    private void refreshButtonStates() {
        for (SearchPanel<?> panel : List.of(partPanel, customerPanel, historyPanel)) {
            boolean active = tabs.getSelectedComponent() == panelComponent(panel);
            panel.query.setEnabled(!dangXuLy && active); panel.search.setEnabled(!dangXuLy && active); panel.clear.setEnabled(!dangXuLy && active);
        }
    }
    private Component panelComponent(SearchPanel<?> panel) {
        if (panel == partPanel) return partPanelComponent(); if (panel == customerPanel) return customerPanelComponent(); return tabs.getComponentAt(2);
    }
    private <T> void runAsync(Supplier<T> task, Consumer<T> success) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() { return task.get(); }
            @Override protected void done() {
                setBusy(false);
                try { success.accept(get()); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); fail("Tác vụ bị gián đoạn."); }
                catch (ExecutionException ex) { Throwable cause = ex.getCause(); fail(cause == null ? ex.getMessage() : cause.getMessage()); }
            }
        }.execute();
    }
    private void fail(String text) { lblStatus.setText("Tra cứu chưa hoàn tất."); message(text == null ? "Không thể tra cứu dữ liệu." : text, JOptionPane.ERROR_MESSAGE); }
    private void message(String text, int type) { JOptionPane.showMessageDialog(this, text, "Tra cứu", type); }

    private static JPanel field(String title, JComponent input) {
        JPanel panel = new JPanel(new BorderLayout(0, 6)); panel.setOpaque(false); panel.add(label(title, 13, BODY, Font.PLAIN), BorderLayout.NORTH);
        if (input instanceof JTextField tf) { tf.setFont(new Font("SansSerif", Font.PLAIN, 13)); tf.setPreferredSize(new Dimension(350, 44)); JPanel box = new JPanel(new BorderLayout()); box.setBackground(WHITE); box.setBorder(new RoundedLineBorder(new Color(0xC9D5E5), 10)); box.add(tf); panel.add(box); }
        else { input.setFont(new Font("SansSerif", Font.PLAIN, 13)); panel.add(input); }
        panel.setPreferredSize(new Dimension(input instanceof JTextField ? 480 : 190, 68)); return panel;
    }
    private static JPanel card(LayoutManager layout) { JPanel panel = new JPanel(layout); panel.setBackground(WHITE); panel.setBorder(BorderFactory.createLineBorder(BORDER)); return panel; }
    private static JPanel detailGrid(String title, String[][] values) {
        JPanel content = new JPanel(); content.setOpaque(false); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        addFull(content, label(title, 15, TEXT, Font.BOLD)); content.add(Box.createVerticalStrut(12));
        int cols = 3; for (int start = 0; start < values.length; start += cols) {
            JPanel row = new JPanel(new GridLayout(1, cols, 10, 0)); row.setOpaque(false);
            for (int i = start; i < Math.min(start + cols, values.length); i++) row.add(infoCell(values[i][0], values[i][1]));
            while (row.getComponentCount() < cols) row.add(new JLabel()); addFull(content, row); content.add(Box.createVerticalStrut(8));
        }
        return content;
    }
    private static JPanel infoCell(String title, String value) {
        JPanel cell = new JPanel(new BorderLayout(0, 5)); cell.setBackground(new Color(0xF7F9FC)); cell.setBorder(new EmptyBorder(9, 11, 9, 11));
        cell.add(label(title, 11, SUBTLE, Font.PLAIN), BorderLayout.NORTH); cell.add(label(value == null || value.isBlank() ? "—" : value, 13, TEXT, Font.PLAIN), BorderLayout.CENTER); return cell;
    }
    private static void styleTable(JTable table) {
        table.setFont(new Font("SansSerif", Font.PLAIN, 13)); table.setForeground(BODY); table.setRowHeight(42); table.setShowVerticalLines(false);
        table.setGridColor(BORDER); table.setSelectionBackground(new Color(0xEDF4FF)); table.setSelectionForeground(PRIMARY);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12)); table.getTableHeader().setForeground(BODY);
        table.getTableHeader().setBackground(new Color(0xF7F9FC)); table.getTableHeader().setPreferredSize(new Dimension(0, 42)); table.setAutoCreateRowSorter(true);
    }
    private static String tenTrangThai(String status) {
        return switch (status == null ? "" : status) { case "CHO_SUA" -> "Chờ sửa"; case "DANG_SUA" -> "Đang sửa"; case "CHO_LINH_KIEN" -> "Chờ linh kiện"; case "HOAN_TAT" -> "Hoàn tất"; default -> status; };
    }
    private static String money(double value) { return MONEY.format(Math.round(value)); }
    private static String number(double value) { return String.format(Locale.forLanguageTag("vi-VN"), "%,.0f", value); }
    private static String blankDash(String value) { return value == null || value.isBlank() ? "—" : value; }
    private static JLabel label(String text, int size, Color color, int style) { JLabel l = new JLabel(text); l.setFont(new Font("SansSerif", style, size)); l.setForeground(color); return l; }
    private static void addFull(JPanel parent, JComponent child) { child.setAlignmentX(Component.LEFT_ALIGNMENT); child.setMaximumSize(new Dimension(Integer.MAX_VALUE, child.getPreferredSize().height)); parent.add(child); }
    private static JButton button(String text, boolean primary, int width) {
        JButton b = new MainFrame.RoundedButton(text, 10, primary ? PRIMARY : WHITE, primary ? new Color(0x1D4ED8) : new Color(0xF1F5F9), primary ? PRIMARY : new Color(0xC9D5E5));
        b.setFont(new Font("SansSerif", Font.BOLD, 13)); b.setForeground(primary ? WHITE : BODY); b.setPreferredSize(new Dimension(width, 40)); b.setFocusPainted(false); return b;
    }
    private static class RoundedLineBorder extends javax.swing.border.AbstractBorder {
        private final Color color; private final int radius;
        RoundedLineBorder(Color color, int radius) { this.color = color; this.radius = radius; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(color); g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius); g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(8, 10, 8, 10); }
    }
}
