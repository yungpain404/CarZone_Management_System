package ui;

import controller.DichVuController;
import entity.DichVu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Danh mục dịch vụ Chủ cửa hàng; danh sách chỉ được truy vấn sau khi tìm kiếm. */
public class QuanLyDichVuUI extends JPanel {
    private static final Color BG = new Color(0xF8FAFC);
    private static final Color WHITE = Color.WHITE;
    private static final Color BORDER = new Color(0xDCE4EF);
    private static final Color PRIMARY = new Color(0x2563EB);
    private static final Color PRIMARY_BG = new Color(0xEFF6FF);
    private static final Color TEXT = new Color(0x17233A);
    private static final Color BODY = new Color(0x3B4B65);
    private static final Color SUBTLE = new Color(0x6B7D98);
    private static final Color RED = new Color(0xDC2626);

    private final DichVuController controller = new DichVuController();
    private final JTextField txtTimKiem = new JTextField();
    private final JComboBox<String> cboTrangThai = new JComboBox<>(new String[]{"Tất cả", "Đang cung cấp", "Ngừng cung cấp"});
    private final JButton btnTim = button("Tìm kiếm", true);
    private final JButton btnLamMoi = button("Làm mới", false);
    private final JButton btnThem = button("Thêm dịch vụ", true);
    private final JButton btnSua = button("Sửa", false);
    private final JButton btnXoa = button("Xóa", false);
    private final JButton btnTrangThai = button("Ngừng cung cấp", false);
    private final JLabel lblTrangThai = label("", 12, SUBTLE, Font.PLAIN);
    private final JLabel lblLoi = label("", 12, RED, Font.PLAIN);
    private final JPanel pnlKetQua = new JPanel(new BorderLayout(0, 12));
    private final JPanel pnlChiTiet = cardPanel();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Mã dịch vụ", "Tên dịch vụ", "Đơn vị tính", "Đơn giá (VNĐ)", "Trạng thái"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private List<DichVu> ketQua = new ArrayList<>();
    private boolean daTimKiem;
    private boolean dangXuLy;

    public QuanLyDichVuUI() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(24, 24, 22, 24));
        add(buildContent(), BorderLayout.CENTER);
        bindActions();
        capNhatTrangThaiNut();
        hienTrangThaiRong();
        SwingUtilities.invokeLater(() -> txtTimKiem.requestFocusInWindow());
    }

    private JComponent buildContent() {
        JPanel root = new MainFrame.ScrollableContentPanel();
        root.setOpaque(false);
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        heading.setOpaque(false);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel icon = label("🔧", 25, TEXT, Font.PLAIN);
        heading.add(icon);
        heading.add(label("Quản lý dịch vụ", 24, TEXT, Font.BOLD));
        root.add(heading);
        root.add(leftGap(18));
        root.add(buildSearchPanel());
        root.add(leftGap(14));
        root.add(buildActionRow());
        root.add(leftGap(18));

        pnlKetQua.setOpaque(false);
        pnlKetQua.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlKetQua.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        root.add(pnlKetQua);
        root.add(leftGap(8));

        JPanel status = new JPanel(new BorderLayout(8, 0));
        status.setOpaque(false);
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        status.add(lblLoi, BorderLayout.WEST);
        status.add(lblTrangThai, BorderLayout.EAST);
        root.add(status);

        JScrollPane scroll = new JScrollPane(root);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    private JComponent buildSearchPanel() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(0, 0, 0, 12);

        JPanel search = new JPanel(new BorderLayout(0, 6));
        search.setOpaque(false);
        search.add(label("Tìm mã / tên dịch vụ", 13, BODY, Font.PLAIN), BorderLayout.NORTH);
        JPanel input = new JPanel(new BorderLayout());
        input.setBackground(WHITE);
        input.setBorder(new RoundedLineBorder(new Color(0xC9D5E5), 10));
        txtTimKiem.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtTimKiem.putClientProperty("JTextField.placeholderText", "Nhập mã hoặc tên…");
        txtTimKiem.setBorder(new EmptyBorder(0, 11, 0, 8));
        txtTimKiem.setOpaque(false);
        input.add(txtTimKiem, BorderLayout.CENTER);
        input.setPreferredSize(new Dimension(340, 44));
        search.add(input, BorderLayout.CENTER);
        g.gridx = 0; g.weightx = 0.42; g.fill = GridBagConstraints.HORIZONTAL;
        row.add(search, g);

        JPanel status = new JPanel(new BorderLayout(0, 6));
        status.setOpaque(false);
        status.add(label("Trạng thái cung cấp", 13, BODY, Font.PLAIN), BorderLayout.NORTH);
        cboTrangThai.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cboTrangThai.setBackground(WHITE);
        cboTrangThai.setPreferredSize(new Dimension(230, 44));
        cboTrangThai.setBorder(BorderFactory.createLineBorder(new Color(0xC9D5E5)));
        status.add(cboTrangThai, BorderLayout.CENTER);
        g.gridx = 1; g.weightx = 0.28; g.fill = GridBagConstraints.HORIZONTAL;
        row.add(status, g);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 22));
        actions.setOpaque(false);
        actions.add(btnTim);
        actions.add(btnLamMoi);
        g.gridx = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE; g.insets = new Insets(0, 0, 0, 0);
        row.add(actions, g);
        return row;
    }

    private JComponent buildActionRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(btnThem);
        row.add(btnSua);
        row.add(btnXoa);
        row.add(btnTrangThai);
        return row;
    }

    private void bindActions() {
        btnTim.addActionListener(e -> timKiem());
        txtTimKiem.addActionListener(e -> timKiem());
        btnLamMoi.addActionListener(e -> lamMoi());
        cboTrangThai.addActionListener(e -> {
            if (daTimKiem) timKiem();
        });
        btnThem.addActionListener(e -> moFormThem());
        btnSua.addActionListener(e -> moFormSua());
        btnXoa.addActionListener(e -> xoaDichVu());
        btnTrangThai.addActionListener(e -> doiTrangThai());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) capNhatChiTiet();
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) moFormSua();
            }
        });
        txtTimKiem.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) lamMoi();
            }
        });
    }

    private void lamMoi() {
        txtTimKiem.setText("");
        cboTrangThai.setSelectedIndex(0);
        daTimKiem = false;
        ketQua = new ArrayList<>();
        clearError();
        lblTrangThai.setText("");
        hienTrangThaiRong();
        capNhatTrangThaiNut();
        txtTimKiem.requestFocusInWindow();
    }

    private void timKiem() {
        String tuKhoa = txtTimKiem.getText().trim();
        if (!UIInputRules.isSearch(tuKhoa)) { showError("Từ khóa tìm kiếm tối đa 150 ký tự; chỉ nhập chữ, số và dấu câu."); txtTimKiem.requestFocusInWindow(); return; }
        Boolean status = switch (String.valueOf(cboTrangThai.getSelectedItem())) {
            case "Đang cung cấp" -> Boolean.TRUE;
            case "Ngừng cung cấp" -> Boolean.FALSE;
            default -> null;
        };
        setBusy(true);
        clearError();
        new SwingWorker<List<DichVu>, Void>() {
            @Override protected List<DichVu> doInBackground() { return controller.timKiem(tuKhoa, status); }
            @Override protected void done() {
                try {
                    ketQua = get();
                    daTimKiem = true;
                    hienDanhSach();
                    lblTrangThai.setText(ketQua.size() + " dịch vụ");
                } catch (Exception ex) {
                    ketQua = new ArrayList<>();
                    hienTrangThaiRong();
                    showWorkerError("Không thể tìm dịch vụ", ex);
                }
                finally { setBusy(false); }
            }
        }.execute();
    }

    private void hienDanhSach() {
        pnlKetQua.removeAll();
        JPanel resultStack = new JPanel(); resultStack.setOpaque(false);
        resultStack.setLayout(new BoxLayout(resultStack, BoxLayout.Y_AXIS));
        resultStack.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel listCard = cardPanel();
        listCard.setLayout(new BorderLayout());
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setForeground(BODY);
        table.setRowHeight(44);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        table.setSelectionBackground(PRIMARY_BG);
        table.setSelectionForeground(PRIMARY);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setForeground(BODY);
        table.getTableHeader().setBackground(BG);
        table.getTableHeader().setPreferredSize(new Dimension(0, 42));
        DefaultTableCellRenderer moneyRenderer = new DefaultTableCellRenderer();
        moneyRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(3).setCellRenderer(moneyRenderer);
        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                                      boolean focus, int row, int column) {
                JLabel badge = (JLabel) super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                badge.setHorizontalAlignment(SwingConstants.CENTER);
                badge.setFont(new Font("SansSerif", Font.BOLD, 11));
                badge.setForeground("Đang cung cấp".equals(value) ? PRIMARY : RED);
                badge.setBackground(selected ? PRIMARY_BG : ("Đang cung cấp".equals(value) ? PRIMARY_BG : new Color(0xFEF2F2)));
                badge.setBorder(new EmptyBorder(4, 8, 4, 8));
                return badge;
            }
        };
        table.getColumnModel().getColumn(4).setCellRenderer(statusRenderer);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        tableScroll.getViewport().setBackground(WHITE);
        listCard.add(tableScroll, BorderLayout.CENTER);
        listCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultStack.add(listCard);

        if (ketQua.isEmpty()) {
            JPanel empty = new JPanel(new GridBagLayout());
            empty.setBackground(WHITE);
            empty.setPreferredSize(new Dimension(400, 120));
            empty.add(label("Không tìm thấy dịch vụ phù hợp.", 13, SUBTLE, Font.PLAIN));
            listCard.add(empty, BorderLayout.CENTER);
        }
        fillRows();
        if (!ketQua.isEmpty()) table.setRowSelectionInterval(0, 0);
        else { pnlChiTiet.removeAll(); pnlChiTiet.setVisible(false); }
        if (!ketQua.isEmpty()) {
            pnlChiTiet.setVisible(true);
            resultStack.add(leftGap(16));
            resultStack.add(pnlChiTiet);
        }
        pnlKetQua.add(resultStack, BorderLayout.CENTER);
        pnlKetQua.revalidate();
        pnlKetQua.repaint();
        capNhatTrangThaiNut();
    }

    private void fillRows() {
        model.setRowCount(0);
        DecimalFormat money = new DecimalFormat("#,##0");
        for (DichVu dv : ketQua) {
            model.addRow(new Object[]{dv.getMaDV(), dv.getTenDV(), dv.getDonViTinh(), money.format(dv.getDonGia()),
                    dv.isDangCungCap() ? "Đang cung cấp" : "Ngừng cung cấp"});
        }
    }

    private void hienTrangThaiRong() {
        pnlKetQua.removeAll();
        model.setRowCount(0);
        table.clearSelection();
        pnlChiTiet.removeAll();
        pnlChiTiet.setVisible(false);
        JPanel message = new JPanel(new BorderLayout());
        message.setOpaque(false);
        message.setAlignmentX(Component.LEFT_ALIGNMENT);
        message.add(label("Chưa có kết quả tra cứu.", 13, SUBTLE, Font.PLAIN), BorderLayout.WEST);
        pnlKetQua.add(message, BorderLayout.NORTH);
        pnlKetQua.revalidate();
        pnlKetQua.repaint();
        capNhatTrangThaiNut();
    }

    private void capNhatChiTiet() {
        DichVu dv = selectedService();
        pnlChiTiet.removeAll();
        if (dv != null) {
            pnlChiTiet.setLayout(new BoxLayout(pnlChiTiet, BoxLayout.Y_AXIS));
            JLabel title = label("Chi tiết dịch vụ — " + dv.getMaDV(), 14, TEXT, Font.BOLD);
            title.setAlignmentX(Component.LEFT_ALIGNMENT); pnlChiTiet.add(title);
            pnlChiTiet.add(leftGap(8));
            JPanel first = new JPanel(new BorderLayout(12, 0)); first.setOpaque(false); first.setAlignmentX(Component.LEFT_ALIGNMENT);
            first.add(label(dv.getTenDV(), 13, BODY, Font.BOLD), BorderLayout.WEST);
            first.add(label(new DecimalFormat("#,##0").format(dv.getDonGia()) + " VNĐ / " + dv.getDonViTinh(), 13, BODY, Font.PLAIN), BorderLayout.EAST);
            pnlChiTiet.add(first);
            if (dv.getMoTa() != null && !dv.getMoTa().isBlank()) {
                pnlChiTiet.add(leftGap(6));
                JLabel description = label("Mô tả: " + dv.getMoTa(), 12, BODY, Font.PLAIN);
                description.setAlignmentX(Component.LEFT_ALIGNMENT);
                pnlChiTiet.add(description);
            }
        }
        pnlChiTiet.setVisible(dv != null);
        pnlChiTiet.revalidate(); pnlChiTiet.repaint();
        capNhatTrangThaiNut();
    }

    private DichVu selectedService() {
        int selected = table.getSelectedRow();
        if (selected < 0 || selected >= ketQua.size()) return null;
        return ketQua.get(table.convertRowIndexToModel(selected));
    }

    private void moFormThem() {
        runTask("Không thể chuẩn bị mã dịch vụ", controller::layMaTiepTheo, code -> openForm(null, code));
    }

    private void moFormSua() {
        DichVu selected = selectedService();
        if (selected == null) return;
        runTask("Không thể tải dịch vụ", () -> controller.timTheoMa(selected.getMaDV()), service -> openForm(service, service.getMaDV()));
    }

    private void openForm(DichVu existing, String code) {
        DichVuFormDialog dialog = new DichVuFormDialog(SwingUtilities.getWindowAncestor(this), controller, existing, code);
        dialog.setVisible(true);
        if (dialog.savedService != null) {
            txtTimKiem.setText(dialog.savedService.getMaDV());
            timKiem();
        }
    }

    private void xoaDichVu() {
        DichVu selected = selectedService(); if (selected == null) return;
        int answer = JOptionPane.showConfirmDialog(this,
                "Xóa dịch vụ " + selected.getMaDV() + " — " + selected.getTenDV() + "?\nDịch vụ đã có lịch sử sử dụng chỉ có thể ngừng cung cấp.",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        runTask("Không thể xóa dịch vụ", () -> { controller.xoa(selected.getMaDV()); return selected.getMaDV(); }, code -> timKiem());
    }

    private void doiTrangThai() {
        DichVu selected = selectedService(); if (selected == null) return;
        boolean next = !selected.isDangCungCap();
        runTask("Không thể đổi trạng thái", () -> { controller.doiTrangThai(selected.getMaDV(), next); return selected.getMaDV(); }, code -> timKiem());
    }

    private <T> void runTask(String errorTitle, Task<T> task, java.util.function.Consumer<T> success) {
        if (dangXuLy) return;
        setBusy(true); clearError();
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return task.run(); }
            @Override protected void done() {
                try {
                    T value = get();
                    setBusy(false);
                    success.accept(value);
                } catch (Exception ex) {
                    setBusy(false);
                    showWorkerError(errorTitle, ex);
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        dangXuLy = busy;
        txtTimKiem.setEnabled(!busy); cboTrangThai.setEnabled(!busy); btnTim.setEnabled(!busy); btnLamMoi.setEnabled(!busy);
        btnThem.setEnabled(!busy); capNhatTrangThaiNut();
        lblTrangThai.setText(busy ? "Đang xử lý…" : (daTimKiem ? ketQua.size() + " dịch vụ" : ""));
    }

    private void capNhatTrangThaiNut() {
        boolean hasSelection = selectedService() != null && !dangXuLy;
        btnSua.setEnabled(hasSelection); btnXoa.setEnabled(hasSelection); btnTrangThai.setEnabled(hasSelection);
        DichVu selected = selectedService();
        btnTrangThai.setText(selected != null && !selected.isDangCungCap() ? "Cung cấp lại" : "Ngừng cung cấp");
        btnTrangThai.setForeground(selected != null && !selected.isDangCungCap() ? PRIMARY : RED);
    }

    private void showWorkerError(String title, Exception exception) {
        Throwable cause = exception instanceof ExecutionException && exception.getCause() != null ? exception.getCause() : exception;
        String message = cause.getMessage() == null ? "Hãy thử lại hoặc kiểm tra kết nối cơ sở dữ liệu." : cause.getMessage();
        showError(message);
        lblTrangThai.setText(title);
    }

    private void showError(String text) { lblLoi.setText(text == null ? "Có lỗi khi xử lý dịch vụ." : text); }
    private void clearError() { lblLoi.setText(""); }

    private static JButton button(String text, boolean primary) {
        JButton button = new MainFrame.RoundedButton(text, 10, primary ? PRIMARY : WHITE,
                primary ? new Color(0x1D4ED8) : new Color(0xF1F5F9), primary ? PRIMARY : BORDER);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        boolean danger = text.equals("Xóa") || text.equals("Ngừng cung cấp");
        button.setForeground(primary ? WHITE : (danger ? RED : BODY));
        int width = switch (text) {
            case "Thêm dịch vụ" -> 124;
            case "Ngừng cung cấp" -> 144;
            case "Tìm kiếm" -> 98;
            case "Làm mới" -> 92;
            default -> 72;
        };
        button.setPreferredSize(new Dimension(width, 44));
        return button;
    }

    private static JPanel cardPanel() {
        JPanel card = new MainFrame.RoundedPanel(WHITE, 12, BORDER, new Insets(14, 16, 14, 16));
        card.setAlignmentX(Component.LEFT_ALIGNMENT); return card;
    }
    private static JLabel label(String text, int size, Color color, int style) {
        JLabel label = new JLabel(text); label.setFont(new Font("SansSerif", style, size)); label.setForeground(color); return label;
    }
    private static Component leftGap(int height) {
        JComponent gap = (JComponent) Box.createRigidArea(new Dimension(0, height)); gap.setAlignmentX(Component.LEFT_ALIGNMENT); return gap;
    }

    private static class RoundedLineBorder extends javax.swing.border.AbstractBorder {
        private final Color color;
        private final int arc;
        RoundedLineBorder(Color color, int arc) { this.color = color; this.arc = arc; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color); g2.drawRoundRect(x, y, width - 1, height - 1, arc, arc); g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
    }

    @FunctionalInterface private interface Task<T> { T run() throws Exception; }

    private static class LengthFilter extends DocumentFilter {
        private final int maximum;
        LengthFilter(int maximum) { this.maximum = maximum; }
        @Override public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string != null && fb.getDocument().getLength() + string.length() <= maximum) super.insertString(fb, offset, string, attr);
        }
        @Override public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            int current = fb.getDocument().getLength();
            int next = current - length + (text == null ? 0 : text.length());
            if (next <= maximum || (current > maximum && next < current)) super.replace(fb, offset, length, text, attrs);
        }
    }

    private class DichVuFormDialog extends JDialog {
        private final boolean adding;
        private final String code;
        private final DichVu existing;
        private final DichVuController formController;
        private final JTextField txtTen = new JTextField();
        private final JComboBox<String> cboDonVi = new JComboBox<>(new String[]{"Lần", "Gói", "Chiếc", "Giờ", "Bộ"});
        private final JSpinner spnDonGia = new JSpinner(new SpinnerNumberModel(150000d, 1d, 9999999999d, 1000d));
        private final JTextArea txtMoTa = new JTextArea(3, 30);
        private final JCheckBox chkDangCungCap = new JCheckBox("Đang cung cấp", true);
        private final JLabel lblFormError = label("", 12, RED, Font.PLAIN);
        private final JButton btnLuu = button("Lưu", true);
        private final JButton btnHuy = button("Hủy", false);
        private DichVu savedService;
        private boolean saving;

        DichVuFormDialog(Window owner, DichVuController controller, DichVu existing, String code) {
            super(owner, existing == null ? "CarZone — Thêm dịch vụ" : "CarZone — Sửa dịch vụ", ModalityType.APPLICATION_MODAL);
            this.adding = existing == null; this.existing = existing; this.code = code; this.formController = controller;
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setContentPane(buildForm());
            pack(); setSize(680, 570); setMinimumSize(new Dimension(620, 540)); setLocationRelativeTo(owner);
            bindFormActions();
            SwingUtilities.invokeLater(() -> txtTen.requestFocusInWindow());
        }

        private JComponent buildForm() {
            JPanel base = new JPanel(new BorderLayout()); base.setBackground(BG);
            JPanel center = new JPanel(new BorderLayout(0, 14)); center.setOpaque(false); center.setBorder(new EmptyBorder(22, 24, 16, 24));
            center.add(label(adding ? "🔧  Thêm dịch vụ" : "🔧  Sửa dịch vụ", 21, PRIMARY, Font.BOLD), BorderLayout.NORTH);
            JPanel fields = new JPanel(new GridBagLayout()); fields.setOpaque(false);
            GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(0, 0, 12, 12); g.anchor = GridBagConstraints.WEST; g.fill = GridBagConstraints.HORIZONTAL;
            JTextField txtMa = new JTextField(code); txtMa.setEditable(false); txtMa.setForeground(SUBTLE); txtMa.setBackground(new Color(0xF4F7FB));
            addField(fields, g, "Mã dịch vụ", txtMa, 0, 0, 1, 1);
            addField(fields, g, "Tên dịch vụ * (tối đa 100 ký tự)", txtTen, 0, 1, 2, 1);
            cboDonVi.setEditable(true);
            addField(fields, g, "Đơn vị tính *", cboDonVi, 0, 2, 1, 1);
            ((JSpinner.NumberEditor) spnDonGia.getEditor()).getFormat().setGroupingUsed(true);
            ((JSpinner.NumberEditor) spnDonGia.getEditor()).getFormat().setMaximumFractionDigits(0);
            addField(fields, g, "Đơn giá * (VNĐ)", spnDonGia, 1, 2, 1, 1);
            txtMoTa.setLineWrap(true); txtMoTa.setWrapStyleWord(true); txtMoTa.setFont(new Font("SansSerif", Font.PLAIN, 13));
            JScrollPane description = new JScrollPane(txtMoTa); description.setPreferredSize(new Dimension(400, 78));
            addField(fields, g, "Mô tả (tối đa 255 ký tự)", description, 0, 3, 2, 1);
            chkDangCungCap.setOpaque(false); chkDangCungCap.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.gridx = 0; g.gridy = 4; g.gridwidth = 2; g.weightx = 1; g.insets = new Insets(0, 0, 0, 0); fields.add(chkDangCungCap, g);
            if (!adding) populateExisting();
            setLength(txtTen, 100);
            ((AbstractDocument) txtMoTa.getDocument()).setDocumentFilter(new LengthFilter(255));
            JPanel messages = new JPanel(new BorderLayout()); messages.setOpaque(false); messages.add(lblFormError, BorderLayout.CENTER);
            JPanel content = new JPanel(new BorderLayout(0, 10)); content.setOpaque(false); content.add(fields, BorderLayout.CENTER); content.add(messages, BorderLayout.SOUTH);
            center.add(content, BorderLayout.CENTER); base.add(center, BorderLayout.CENTER);
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12)); footer.setBackground(WHITE);
            footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER)); footer.add(btnLuu); footer.add(btnHuy);
            base.add(footer, BorderLayout.SOUTH); return base;
        }

        private void populateExisting() {
            txtTen.setText(existing.getTenDV());
            cboDonVi.setSelectedItem(existing.getDonViTinh());
            spnDonGia.setValue(existing.getDonGia());
            txtMoTa.setText(existing.getMoTa() == null ? "" : existing.getMoTa());
            chkDangCungCap.setSelected(existing.isDangCungCap());
        }

        private void addField(JPanel panel, GridBagConstraints base, String title, JComponent field,
                              int x, int y, int width, double weight) {
            JPanel wrapper = new JPanel(new BorderLayout(0, 6)); wrapper.setOpaque(false);
            JLabel caption = label(title, 12, BODY, Font.PLAIN); wrapper.add(caption, BorderLayout.NORTH);
            field.setFont(new Font("SansSerif", Font.PLAIN, 13));
            if (!(field instanceof JScrollPane)) {
                field.setPreferredSize(new Dimension(230, 42));
                field.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(new Color(0xC9D5E5), 10), new EmptyBorder(0, 10, 0, 10)));
            }
            wrapper.add(field, BorderLayout.CENTER);
            GridBagConstraints g = (GridBagConstraints) base.clone(); g.gridx=x; g.gridy=y; g.gridwidth=width; g.weightx=weight; g.insets=new Insets(0,0,12,12);
            panel.add(wrapper, g);
        }

        private void bindFormActions() {
            btnLuu.addActionListener(e -> save()); btnHuy.addActionListener(e -> dispose());
            txtTen.addActionListener(e -> cboDonVi.requestFocusInWindow());
            spnDonGia.addKeyListener(new KeyAdapter() { @Override public void keyPressed(KeyEvent e) { if (e.getKeyCode()==KeyEvent.VK_ENTER) txtMoTa.requestFocusInWindow(); } });
            txtTen.getDocument().addDocumentListener(clearListener()); txtMoTa.getDocument().addDocumentListener(clearListener());
            cboDonVi.addActionListener(e -> clearError());
        }

        private javax.swing.event.DocumentListener clearListener() {
            return new javax.swing.event.DocumentListener() {
                @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { clearError(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { clearError(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { clearError(); }
            };
        }

        private void save() {
            if (saving) return;
            String name = txtTen.getText().trim();
            Object unitValue = cboDonVi.getEditor().getItem();
            String unit = unitValue == null ? "" : unitValue.toString().trim();
            String description = txtMoTa.getText().trim();
            double price;
            try { spnDonGia.commitEdit(); price = ((Number) spnDonGia.getValue()).doubleValue(); }
            catch (java.text.ParseException ex) { showFormError("Đơn giá không hợp lệ."); return; }
            if (name.isEmpty()) { showFormError("Tên dịch vụ không được để trống."); txtTen.requestFocusInWindow(); return; }
            if (!UIInputRules.isCatalogName(name)) { showFormError("Tên dịch vụ chỉ gồm chữ, số, khoảng trắng và dấu câu thông dụng (tối đa 100 ký tự)."); txtTen.requestFocusInWindow(); return; }
            if (unit.isEmpty()) { showFormError("Đơn vị tính không được để trống."); cboDonVi.requestFocusInWindow(); return; }
            if (!UIInputRules.isUnit(unit)) { showFormError("Đơn vị tính chỉ gồm chữ cái, tối đa 20 ký tự."); cboDonVi.requestFocusInWindow(); return; }
            if (!UIInputRules.isMultilineText(description)) { showFormError("Mô tả chứa ký tự không hợp lệ."); txtMoTa.requestFocusInWindow(); return; }
            if (price <= 0) { showFormError("Đơn giá phải lớn hơn 0."); spnDonGia.requestFocusInWindow(); return; }
            boolean cungCap = chkDangCungCap.isSelected();
            setSaving(true);
            new SwingWorker<DichVu, Void>() {
                @Override protected DichVu doInBackground() {
                    return adding
                            ? formController.them(name, price, unit, description.isEmpty() ? null : description, cungCap)
                            : formController.capNhat(existing.getMaDV(), name, price, unit, description.isEmpty() ? null : description, cungCap);
                }
                @Override protected void done() {
                    try { savedService = get(); dispose(); }
                    catch (Exception ex) { setSaving(false); Throwable cause = ex.getCause() == null ? ex : ex.getCause(); showFormError(cause.getMessage()); }
                }
            }.execute();
        }

        private void setSaving(boolean value) {
            saving = value; btnLuu.setEnabled(!value); btnHuy.setEnabled(!value);
            txtTen.setEnabled(!value); cboDonVi.setEnabled(!value); spnDonGia.setEnabled(!value); txtMoTa.setEnabled(!value); chkDangCungCap.setEnabled(!value);
            btnLuu.setText(value ? "Đang lưu…" : "Lưu");
        }
        private void showFormError(String text) { lblFormError.setText(text == null ? "Không thể lưu dịch vụ." : text); }
        private void clearError() { lblFormError.setText(""); }
        private void setLength(JTextField field, int maximum) { ((AbstractDocument)field.getDocument()).setDocumentFilter(new LengthFilter(maximum)); }
    }
}
