package ui;

import controller.NhanVienController;
import entity.NhanVien;
import entity.TaiKhoan;
import enums.VaiTro;
import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;

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
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Màn hình quản lý hồ sơ nhân viên của Chủ cửa hàng; dữ liệu chỉ tải sau khi tìm kiếm. */
public class QuanLyNhanVienUI extends JPanel {
    private static final Color BG = new Color(0xF4F7FB);
    private static final Color WHITE = Color.WHITE;
    private static final Color BORDER = new Color(0xE3E9F2);
    private static final Color PRIMARY = new Color(0x2563EB);
    private static final Color PRIMARY_BG = new Color(0xEDF4FF);
    private static final Color TEXT = new Color(0x14233D);
    private static final Color BODY = new Color(0x40516A);
    private static final Color SUBTLE = new Color(0x7A889C);
    private static final Color RED = new Color(0xCA4850);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final NhanVienController controller = new NhanVienController();
    private final JTextField txtTimKiem = new JTextField();
    private final JComboBox<String> cboVaiTro = new JComboBox<>(new String[]{
            "Tất cả", "Chủ cửa hàng", "Tiếp nhận viên", "Kỹ thuật viên"});
    private final JButton btnTim = button("Tìm kiếm", true, 94);
    private final JButton btnLamMoi = button("Làm mới", false, 92);
    private final JButton btnThem = button("Thêm hồ sơ", true, 114);
    private final JButton btnSua = button("Sửa", false, 62);
    private final JLabel lblError = label("", 12, RED, Font.PLAIN);
    private final JLabel lblStatus = label("", 12, SUBTLE, Font.PLAIN);
    private final JPanel pnlResults = new JPanel();
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{
            "Mã nhân viên", "Họ tên", "Vai trò", "Số điện thoại", "Email", "Ngày vào làm", "Tài khoản"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private List<NhanVien> ketQua = new ArrayList<>();
    private boolean daTimKiem;
    private boolean dangXuLy;

    public QuanLyNhanVienUI() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(24, 24, 22, 24));
        add(buildContent(), BorderLayout.CENTER);
        bindActions();
        hienTrangThaiBanDau();
        capNhatNut();
        SwingUtilities.invokeLater(() -> txtTimKiem.requestFocusInWindow());
    }

    private JComponent buildContent() {
        JPanel root = new MainFrame.ScrollableContentPanel();
        root.setOpaque(false);
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        heading.setOpaque(false);
        heading.add(label("👥", 25, TEXT, Font.PLAIN));
        heading.add(label("Quản lý nhân viên", 24, TEXT, Font.BOLD));
        addFull(root, heading);
        root.add(gap(18));
        addFull(root, buildSearchRow());
        root.add(gap(14));
        addFull(root, buildActionRow());
        root.add(gap(12));

        pnlResults.setOpaque(false);
        pnlResults.setLayout(new BoxLayout(pnlResults, BoxLayout.Y_AXIS));
        pnlResults.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlResults.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        addFull(root, pnlResults);
        root.add(gap(8));

        JPanel status = new JPanel(new BorderLayout(8, 0));
        status.setOpaque(false);
        status.add(lblError, BorderLayout.WEST);
        status.add(lblStatus, BorderLayout.EAST);
        addFull(root, status);

        JScrollPane scroll = new JScrollPane(root);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    private JComponent buildSearchRow() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(0, 0, 0, 12);

        JPanel search = new JPanel(new BorderLayout(0, 6));
        search.setOpaque(false);
        search.add(label("Tìm mã / họ tên / điện thoại", 13, BODY, Font.PLAIN), BorderLayout.NORTH);
        JPanel input = new JPanel(new BorderLayout());
        input.setBackground(WHITE);
        input.setBorder(new RoundedLineBorder(new Color(0xC9D5E5), 10));
        txtTimKiem.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtTimKiem.putClientProperty("JTextField.placeholderText", "Nhập thông tin tìm kiếm…");
        txtTimKiem.setBorder(new EmptyBorder(0, 11, 0, 8));
        txtTimKiem.setOpaque(false);
        input.add(txtTimKiem, BorderLayout.CENTER);
        input.setPreferredSize(new Dimension(350, 44));
        search.add(input, BorderLayout.CENTER);
        g.gridx = 0; g.weightx = .45; g.fill = GridBagConstraints.HORIZONTAL;
        row.add(search, g);

        JPanel role = new JPanel(new BorderLayout(0, 6));
        role.setOpaque(false);
        role.add(label("Vai trò", 13, BODY, Font.PLAIN), BorderLayout.NORTH);
        cboVaiTro.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cboVaiTro.setBackground(WHITE);
        cboVaiTro.setPreferredSize(new Dimension(220, 44));
        cboVaiTro.setBorder(BorderFactory.createLineBorder(new Color(0xC9D5E5)));
        role.add(cboVaiTro, BorderLayout.CENTER);
        g.gridx = 1; g.weightx = .3;
        row.add(role, g);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 22));
        buttons.setOpaque(false);
        buttons.add(btnTim); buttons.add(btnLamMoi);
        g.gridx = 2; g.weightx = .25; g.insets = new Insets(0, 0, 0, 0);
        row.add(buttons, g);
        return row;
    }

    private JComponent buildActionRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(btnThem); row.add(btnSua);
        return row;
    }

    private void bindActions() {
        btnTim.addActionListener(e -> timKiem());
        txtTimKiem.addActionListener(e -> timKiem());
        btnLamMoi.addActionListener(e -> lamMoi());
        btnThem.addActionListener(e -> moForm(null));
        btnSua.addActionListener(e -> moFormSua());
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

    private VaiTro vaiTroDaChon() {
        return switch (String.valueOf(cboVaiTro.getSelectedItem())) {
            case "Chủ cửa hàng" -> VaiTro.CHU_CUA_HANG;
            case "Tiếp nhận viên" -> VaiTro.TIEP_NHAN_VIEN;
            case "Kỹ thuật viên" -> VaiTro.KY_THUAT_VIEN;
            default -> null;
        };
    }

    private void timKiem() {
        String keyword = txtTimKiem.getText().trim();
        if (!UIInputRules.isSearch(keyword)) { showError("Từ khóa tìm kiếm tối đa 150 ký tự; chỉ nhập chữ, số và dấu câu."); txtTimKiem.requestFocusInWindow(); return; }
        VaiTro role = vaiTroDaChon();
        setBusy(true); clearError();
        new SwingWorker<List<NhanVien>, Void>() {
            @Override protected List<NhanVien> doInBackground() { return controller.timKiem(keyword, role); }
            @Override protected void done() {
                try {
                    ketQua = get(); daTimKiem = true; hienDanhSach();
                    lblStatus.setText(ketQua.size() + " hồ sơ");
                } catch (Exception ex) {
                    ketQua = new ArrayList<>(); hienDanhSach(); showWorkerError("Không thể tìm nhân viên", ex);
                } finally { setBusy(false); }
            }
        }.execute();
    }

    private void lamMoi() {
        txtTimKiem.setText(""); cboVaiTro.setSelectedIndex(0); ketQua.clear(); daTimKiem = false;
        clearError(); hienTrangThaiBanDau(); capNhatNut();
    }

    private void hienTrangThaiBanDau() {
        pnlResults.removeAll();
        model.setRowCount(0);
        table.clearSelection();
        JPanel empty = card();
        empty.setLayout(new BorderLayout());
        empty.setPreferredSize(new Dimension(600, 112));
        empty.add(label("Danh sách nhân viên chỉ hiển thị sau khi bạn tìm kiếm.", 13, SUBTLE, Font.PLAIN), BorderLayout.CENTER);
        addFull(pnlResults, empty);
        pnlResults.revalidate(); pnlResults.repaint();
    }

    private void hienDanhSach() {
        pnlResults.removeAll();
        JPanel stack = new JPanel(); stack.setOpaque(false); stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setAlignmentX(Component.LEFT_ALIGNMENT); stack.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        JPanel listCard = card(); listCard.setLayout(new BorderLayout());
        listCard.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        table.setFont(new Font("SansSerif", Font.PLAIN, 12)); table.setForeground(BODY); table.setRowHeight(44);
        table.setShowVerticalLines(false); table.setGridColor(BORDER); table.setSelectionBackground(PRIMARY_BG);
        table.setSelectionForeground(PRIMARY); table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setForeground(BODY); table.getTableHeader().setBackground(new Color(0xF4F7FB));
        table.getTableHeader().setPreferredSize(new Dimension(0, 42));
        DefaultTableCellRenderer accountRenderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                                      boolean focus, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                boolean active = "Đã cấp".equals(value);
                cell.setForeground(selected ? PRIMARY : (active ? new Color(0x059669) : BODY));
                return cell;
            }
        };
        table.getColumnModel().getColumn(6).setCellRenderer(accountRenderer);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(BORDER)); tableScroll.getViewport().setBackground(WHITE);
        listCard.add(tableScroll, BorderLayout.CENTER);
        if (ketQua.isEmpty()) {
            JPanel noData = new JPanel(new GridBagLayout()); noData.setBackground(WHITE); noData.setPreferredSize(new Dimension(500, 110));
            noData.add(label("Không tìm thấy hồ sơ phù hợp.", 13, SUBTLE, Font.PLAIN));
            listCard.add(noData, BorderLayout.SOUTH);
        }
        stack.add(listCard); stack.add(gap(18)); stack.add(buildSummary()); stack.add(gap(18));
        stack.add(buildDetailCard());
        addFull(pnlResults, stack);
        model.setRowCount(0);
        for (NhanVien nv : ketQua) {
            model.addRow(new Object[]{nv.getMaNV(), nv.getHoTen(), tenVaiTro(nv.getVaiTro()), nv.getSoDienThoai(),
                    nv.getEmail(), ngay(nv.getNgayVaoLam()), nv.getTaiKhoan() == null ? "Chưa cấp" : "Đã cấp"});
        }
        if (!ketQua.isEmpty()) table.setRowSelectionInterval(0, 0);
        pnlResults.revalidate(); pnlResults.repaint(); capNhatChiTiet();
    }

    private JComponent buildSummary() {
        JPanel wrapper = card(); wrapper.setLayout(new BorderLayout(0, 10));
        wrapper.add(label("Tổng hợp trong kết quả tìm kiếm", 15, TEXT, Font.BOLD), BorderLayout.NORTH);
        int granted = (int) ketQua.stream().filter(nv -> nv.getTaiKhoan() != null).count();
        JPanel metrics = new JPanel(new GridLayout(1, 3, 12, 0)); metrics.setOpaque(false);
        metrics.add(metric("Tổng hồ sơ", ketQua.size() + " hồ sơ", false));
        metrics.add(metric("Đã cấp tài khoản", granted + " hồ sơ", false));
        metrics.add(metric("Chưa cấp tài khoản", (ketQua.size() - granted) + " hồ sơ", true));
        wrapper.add(metrics, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel metric(String title, String value, boolean accent) {
        JPanel item = new JPanel(); item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));
        item.setBackground(accent ? PRIMARY_BG : new Color(0xF7F9FC));
        item.setBorder(new EmptyBorder(12, 14, 10, 14)); item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.add(label(title, 11, accent ? PRIMARY : SUBTLE, Font.PLAIN)); item.add(Box.createVerticalStrut(5));
        item.add(label(value, 17, accent ? PRIMARY : TEXT, Font.BOLD)); return item;
    }

    private JComponent buildDetailCard() {
        JPanel detail = card(); detail.setLayout(new BorderLayout(12, 10));
        JPanel head = new JPanel(new BorderLayout()); head.setOpaque(false);
        head.add(label("Hồ sơ nhân viên", 15, TEXT, Font.BOLD), BorderLayout.WEST);
        head.add(label("Chọn một hồ sơ để xem thông tin chi tiết.", 12, SUBTLE, Font.PLAIN), BorderLayout.EAST);
        detail.add(head, BorderLayout.NORTH);
        detail.setName("employeeDetailCard");
        return detail;
    }

    private void capNhatChiTiet() {
        if (pnlResults.getComponentCount() == 0 || !(pnlResults.getComponent(0) instanceof JPanel stack)
                || stack.getComponentCount() < 5) return;
        NhanVien selected = selectedEmployee();
        JPanel detail = (JPanel) stack.getComponent(4);
        detail.removeAll(); detail.setLayout(new BorderLayout(12, 10));
        if (selected == null) {
            detail.add(label("Chọn một hồ sơ để xem thông tin chi tiết.", 13, SUBTLE, Font.PLAIN), BorderLayout.CENTER);
        } else {
            JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
            header.add(label("Hồ sơ nhân viên — " + selected.getMaNV(), 15, TEXT, Font.BOLD), BorderLayout.WEST);
            String accountStatus = selected.getTaiKhoan() == null ? "Chưa cấp tài khoản" : "Tài khoản: " + selected.getTaiKhoan().getTenDangNhap();
            header.add(label(accountStatus, 12, BODY, Font.PLAIN), BorderLayout.EAST);
            JPanel body = new JPanel(); body.setOpaque(false); body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.add(label(selected.getHoTen() + " · " + tenVaiTro(selected.getVaiTro()), 13, TEXT, Font.BOLD));
            body.add(Box.createVerticalStrut(8));
            body.add(label("Ngày sinh: " + ngay(selected.getNgaySinh()) + " · Tuổi: " + selected.getTuoi()
                    + " · SĐT: " + selected.getSoDienThoai(), 12, BODY, Font.PLAIN));
            body.add(Box.createVerticalStrut(5));
            body.add(label("CCCD: " + selected.getCCCD() + " · Ngày vào làm: " + ngay(selected.getNgayVaoLam()), 12, BODY, Font.PLAIN));
            body.add(Box.createVerticalStrut(5));
            body.add(label("Địa chỉ: " + (selected.getDiaChi() == null ? "Chưa cập nhật" : selected.getDiaChi()), 12, BODY, Font.PLAIN));
            body.add(Box.createVerticalStrut(8));
            body.add(label(selected.getTaiKhoan() == null
                    ? "Hồ sơ này chưa được liên kết tài khoản đăng nhập."
                    : "Tài khoản liên kết đang hoạt động: " + (selected.getTaiKhoan().isTrangThaiHoatDong() ? "Có" : "Không"), 12, SUBTLE, Font.PLAIN));
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); actions.setOpaque(false);
            JButton edit = button("Sửa hồ sơ", false, 102);
            edit.addActionListener(e -> moFormSua());
            actions.add(edit);
            JPanel content = new JPanel(new BorderLayout(0, 12)); content.setOpaque(false);
            content.add(body, BorderLayout.CENTER); content.add(actions, BorderLayout.SOUTH);
            detail.add(header, BorderLayout.NORTH); detail.add(content, BorderLayout.CENTER);
        }
        detail.revalidate(); detail.repaint(); capNhatNut();
    }

    private NhanVien selectedEmployee() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= ketQua.size()) return null;
        return ketQua.get(table.convertRowIndexToModel(row));
    }

    private void moFormSua() {
        NhanVien selected = selectedEmployee(); if (selected == null || dangXuLy) return;
        runTask("Không thể tải hồ sơ", () -> controller.timTheoMa(selected.getMaNV()), this::moForm);
    }

    private void moForm(NhanVien existing) {
        NhanVienFormDialog dialog = new NhanVienFormDialog(SwingUtilities.getWindowAncestor(this), controller, existing);
        dialog.setVisible(true);
        if (dialog.savedEmployee != null) {
            txtTimKiem.setText(dialog.savedEmployee.getMaNV());
            cboVaiTro.setSelectedIndex(0);
            timKiem();
        }
    }

    private <T> void runTask(String title, Task<T> task, java.util.function.Consumer<T> success) {
        if (dangXuLy) return;
        setBusy(true); clearError();
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return task.run(); }
            @Override protected void done() {
                try { T value = get(); setBusy(false); success.accept(value); }
                catch (Exception ex) { setBusy(false); showWorkerError(title, ex); }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        dangXuLy = busy;
        txtTimKiem.setEnabled(!busy); cboVaiTro.setEnabled(!busy); btnTim.setEnabled(!busy); btnLamMoi.setEnabled(!busy);
        btnThem.setEnabled(!busy); capNhatNut();
        lblStatus.setText(busy ? "Đang xử lý…" : (daTimKiem ? ketQua.size() + " hồ sơ" : ""));
    }

    private void capNhatNut() {
        boolean selected = selectedEmployee() != null && !dangXuLy;
        btnSua.setEnabled(selected);
    }

    private void showWorkerError(String title, Exception exception) {
        Throwable cause = exception instanceof ExecutionException && exception.getCause() != null ? exception.getCause() : exception;
        showError(cause.getMessage() == null ? "Hãy kiểm tra dữ liệu và kết nối CSDL." : cause.getMessage());
        lblStatus.setText(title);
    }

    private void showError(String message) { lblError.setText(message); }
    private void clearError() { lblError.setText(""); }

    private static String tenVaiTro(VaiTro role) {
        if (role == null) return "—";
        return switch (role) {
            case CHU_CUA_HANG -> "Chủ cửa hàng";
            case TIEP_NHAN_VIEN -> "Tiếp nhận viên";
            case KY_THUAT_VIEN -> "Kỹ thuật viên";
        };
    }

    private static String ngay(LocalDate date) { return date == null ? "—" : DATE_FORMAT.format(date); }
    private static void addFull(JPanel parent, JComponent child) {
        child.setAlignmentX(Component.LEFT_ALIGNMENT);
        child.setMaximumSize(new Dimension(Integer.MAX_VALUE, child.getMaximumSize().height));
        parent.add(child);
    }
    private static Component gap(int height) {
        JComponent gap = (JComponent) Box.createRigidArea(new Dimension(0, height));
        gap.setAlignmentX(Component.LEFT_ALIGNMENT); return gap;
    }
    private static JPanel card() {
        JPanel panel = new MainFrame.RoundedPanel(WHITE, 12, BORDER, new Insets(16, 16, 16, 16));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT); return panel;
    }
    private static JLabel label(String text, int size, Color color, int style) {
        JLabel label = new JLabel(text); label.setFont(new Font("SansSerif", style, size)); label.setForeground(color); return label;
    }
    private static JButton button(String text, boolean primary, int width) {
        JButton b = new MainFrame.RoundedButton(text, 10, primary ? PRIMARY : WHITE,
                primary ? new Color(0x1D4ED8) : new Color(0xF1F5F9), primary ? PRIMARY : BORDER);
        b.setFont(new Font("SansSerif", Font.BOLD, 13)); b.setForeground(primary ? WHITE : BODY);
        b.setPreferredSize(new Dimension(width, 42)); return b;
    }
    private static class RoundedLineBorder extends javax.swing.border.AbstractBorder {
        private final Color color; private final int arc;
        RoundedLineBorder(Color color, int arc) { this.color = color; this.arc = arc; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color); g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc); g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
    }
    @FunctionalInterface private interface Task<T> { T run() throws Exception; }

    private class NhanVienFormDialog extends JDialog {
        private final boolean adding;
        private final NhanVien existing;
        private final NhanVienController formController;
        private final JTextField txtMa = new JTextField();
        private final JComboBox<VaiTro> cboRole = new JComboBox<>(VaiTro.values());
        private final JTextField txtTen = new JTextField();
        private final JDateChooser spnNgaySinh = dateChooser(LocalDate.now().minusYears(25));
        private final JTextField txtTuoi = new JTextField();
        private final JTextField txtSdt = new JTextField();
        private final JTextField txtEmail = new JTextField();
        private final JTextField txtCccd = new JTextField();
        private final JTextArea txtDiaChi = new JTextArea(3, 30);
        private final JDateChooser spnNgayVaoLam = dateChooser(LocalDate.now());
        private final JLabel lblError = label("", 12, RED, Font.PLAIN);
        private final JButton btnSave = button("Lưu", true, 66);
        private final JButton btnCancel = button("Hủy", false, 66);
        private NhanVien savedEmployee;
        private boolean saving;

        NhanVienFormDialog(Window owner, NhanVienController controller, NhanVien existing) {
            super(owner, existing == null ? "CarZone — Thêm hồ sơ nhân viên" : "CarZone — Sửa hồ sơ nhân viên", ModalityType.APPLICATION_MODAL);
            this.adding = existing == null; this.existing = existing; this.formController = controller;
            setDefaultCloseOperation(DISPOSE_ON_CLOSE); setContentPane(buildForm());
            pack(); setSize(720, 720); setMinimumSize(new Dimension(680, 660)); setLocationRelativeTo(owner);
            bindFormActions(); SwingUtilities.invokeLater(() -> txtTen.requestFocusInWindow());
        }

        private JComponent buildForm() {
            JPanel base = new JPanel(new BorderLayout()); base.setBackground(BG);
            JPanel center = new JPanel(new BorderLayout(0, 14)); center.setOpaque(false); center.setBorder(new EmptyBorder(22, 24, 16, 24));
            center.add(label(adding ? "👥  Thêm hồ sơ nhân viên" : "👥  Sửa hồ sơ nhân viên", 21, PRIMARY, Font.BOLD), BorderLayout.NORTH);
            JPanel fields = new JPanel(new GridBagLayout()); fields.setOpaque(false);
            GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(0, 0, 12, 12); g.anchor = GridBagConstraints.WEST; g.fill = GridBagConstraints.HORIZONTAL;
            txtMa.setEditable(false); txtMa.setFocusable(false); txtMa.setForeground(SUBTLE); txtMa.setBackground(new Color(0xF4F7FB));
            txtMa.setText(adding ? "Đang tải…" : existing.getMaNV());
            addField(fields, g, "Mã nhân viên", txtMa, 0, 0, 1, 1);
            txtMa.setBackground(new Color(0xF4F7FB));
            cboRole.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                    return super.getListCellRendererComponent(list, value == null ? "" : tenVaiTro((VaiTro) value), index, selected, focus);
                }
            });
            cboRole.setBackground(WHITE); cboRole.setEnabled(adding); cboRole.setPreferredSize(new Dimension(220, 42));
            cboRole.setBorder(BorderFactory.createLineBorder(new Color(0xD7E1EF)));
            addField(fields, g, adding ? "Vai trò *" : "Vai trò — cố định theo mã nhân viên", cboRole, 1, 0, 1, 1);
            addField(fields, g, "Họ và tên *", txtTen, 0, 1, 2, 1);
            txtTuoi.setEditable(false); txtTuoi.setFocusable(false); txtTuoi.setBackground(new Color(0xF4F7FB)); txtTuoi.setForeground(SUBTLE);
            JPanel age = fieldPanel("Tuổi", txtTuoi);
            JPanel birthday = fieldPanel("Ngày sinh *", spnNgaySinh);
            g.gridx = 0; g.gridy = 2; g.gridwidth = 1; fields.add(birthday, g);
            g.gridx = 1; fields.add(age, g);
            addField(fields, g, "Số điện thoại *", txtSdt, 0, 3, 1, 1);
            addField(fields, g, "Gmail khôi phục mật khẩu *", txtEmail, 1, 3, 1, 1);
            addField(fields, g, "CCCD *", txtCccd, 0, 4, 1, 1);
            JScrollPane addressScroll = new JScrollPane(txtDiaChi); addressScroll.setPreferredSize(new Dimension(300, 78));
            addressScroll.setBorder(BorderFactory.createLineBorder(new Color(0xD7E1EF))); txtDiaChi.setFont(new Font("SansSerif", Font.PLAIN, 13));
            txtDiaChi.setLineWrap(true); txtDiaChi.setWrapStyleWord(true); txtDiaChi.setBorder(new EmptyBorder(8, 10, 8, 10));
            addField(fields, g, "Địa chỉ *", addressScroll, 0, 5, 2, 1);
            addField(fields, g, "Ngày vào làm *", spnNgayVaoLam, 0, 6, 1, 1);

            g.gridx = 1; g.gridy = 6; g.weightx = 1; fields.add(Box.createHorizontalGlue(), g);
            JPanel body = new JPanel(new BorderLayout(0, 8)); body.setOpaque(false); body.add(fields, BorderLayout.CENTER); body.add(lblError, BorderLayout.SOUTH);
            center.add(body, BorderLayout.CENTER); base.add(center, BorderLayout.CENTER);
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12)); footer.setBackground(WHITE);
            footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER)); footer.add(btnSave); footer.add(btnCancel);
            base.add(footer, BorderLayout.SOUTH);
            installFilters();
            if (!adding) fillCurrentValues(); else {
                cboRole.setSelectedItem(VaiTro.KY_THUAT_VIEN);
                cboRole.addActionListener(e -> capNhatMaDuKien());
                capNhatTuoi(); capNhatMaDuKien();
            }
            return base;
        }

        private void capNhatMaDuKien() {
            VaiTro role = (VaiTro) cboRole.getSelectedItem();
            if (role == null) return;
            txtMa.setText("Đang tải…");
            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() { return formController.xemMaNhanVienTiepTheo(role); }
                @Override protected void done() {
                    if (!isDisplayable() || role != cboRole.getSelectedItem()) return;
                    try { txtMa.setText(get()); }
                    catch (Exception ex) { txtMa.setText("—"); }
                }
            }.execute();
        }

        private void fillCurrentValues() {
            txtTen.setText(existing.getHoTen()); cboRole.setSelectedItem(existing.getVaiTro());
            setDate(spnNgaySinh, existing.getNgaySinh()); setDate(spnNgayVaoLam, existing.getNgayVaoLam());
            txtSdt.setText(existing.getSoDienThoai()); txtEmail.setText(existing.getEmail() == null ? "" : existing.getEmail()); txtCccd.setText(existing.getCCCD());
            txtDiaChi.setText(existing.getDiaChi() == null ? "" : existing.getDiaChi()); capNhatTuoi();
        }

        private void installFilters() {
            applyFilter(txtTen, 100, false); applyFilter(txtSdt, 11, true); applyFilter(txtEmail, 254, false); applyFilter(txtCccd, 12, true); applyFilter(txtDiaChi, 200, false);
            spnNgaySinh.addPropertyChangeListener("date", e -> capNhatTuoi());
            spnNgayVaoLam.addPropertyChangeListener("date", e -> capNhatTuoi());
            txtTen.addActionListener(e -> spnNgaySinh.requestFocusInWindow());
            txtSdt.addActionListener(e -> txtEmail.requestFocusInWindow());
            txtEmail.addActionListener(e -> txtCccd.requestFocusInWindow());
            txtCccd.addActionListener(e -> txtDiaChi.requestFocusInWindow());
        }

        private void capNhatTuoi() {
            LocalDate birth = spnNgaySinh.getDate() == null ? null : spnNgaySinh.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            txtTuoi.setText(birth == null ? "" : Integer.toString(Period.between(birth, LocalDate.now()).getYears()));
        }

        private void bindFormActions() {
            btnCancel.addActionListener(e -> dispose()); btnSave.addActionListener(e -> save());
            getRootPane().setDefaultButton(btnSave);
            getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
            javax.swing.event.DocumentListener listener = new javax.swing.event.DocumentListener() {
                @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { clearFormError(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { clearFormError(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { clearFormError(); }
            };
            txtTen.getDocument().addDocumentListener(listener); txtSdt.getDocument().addDocumentListener(listener); txtEmail.getDocument().addDocumentListener(listener);
            txtCccd.getDocument().addDocumentListener(listener); txtDiaChi.getDocument().addDocumentListener(listener);
        }

        private void save() {
            if (saving) return;
            try {
                String hoTen = txtTen.getText().trim();
                String sdt = txtSdt.getText().trim();
                String email = txtEmail.getText().trim();
                String cccd = txtCccd.getText().trim();
                String diaChi = txtDiaChi.getText().trim();
                VaiTro role = (VaiTro) cboRole.getSelectedItem();
                if (!UIInputRules.isPersonName(hoTen)) { showFormError("Họ tên chỉ gồm chữ cái và dấu cách, dấu nháy hoặc gạch nối."); txtTen.requestFocusInWindow(); return; }
                if (!UIInputRules.isPhone(sdt)) { showFormError("Số điện thoại phải bắt đầu bằng 0 và gồm 10 hoặc 11 chữ số."); txtSdt.requestFocusInWindow(); return; }
                if (!UIInputRules.isEmail(email)) { showFormError("Vui lòng nhập địa chỉ Gmail hợp lệ để khôi phục mật khẩu."); txtEmail.requestFocusInWindow(); return; }
                if (!UIInputRules.isCccd(cccd)) { showFormError("CCCD phải gồm đúng 12 chữ số."); txtCccd.requestFocusInWindow(); return; }
                if (!UIInputRules.isAddress(diaChi)) { showFormError("Địa chỉ không hợp lệ hoặc vượt quá 200 ký tự."); txtDiaChi.requestFocusInWindow(); return; }
                LocalDate birth = UIInputRules.readDate(spnNgaySinh), hireDate = UIInputRules.readDate(spnNgayVaoLam);
                if (birth.isAfter(LocalDate.now())) { showFormError("Ngày sinh không được ở tương lai."); spnNgaySinh.requestFocusInWindow(); return; }
                if (hireDate.isAfter(LocalDate.now())) { showFormError("Ngày vào làm không được ở tương lai."); spnNgayVaoLam.requestFocusInWindow(); return; }
                if (birth.isAfter(hireDate.minusYears(18))) { showFormError("Nhân viên phải đủ 18 tuổi tại ngày vào làm."); spnNgaySinh.requestFocusInWindow(); return; }
                NhanVien existingSnapshot = existing;
                setSaving(true); clearFormError();
                new SwingWorker<NhanVien, Void>() {
                    @Override protected NhanVien doInBackground() {
                        if (adding) return formController.them(hoTen, birth, sdt, email, cccd, diaChi, hireDate, role);
                        return formController.capNhat(existingSnapshot.getMaNV(), hoTen, birth, sdt, email, cccd, diaChi, hireDate);
                    }
                    @Override protected void done() {
                        try { savedEmployee = get(); JOptionPane.showMessageDialog(NhanVienFormDialog.this,
                                adding ? "Đã thêm hồ sơ " + savedEmployee.getMaNV() + "." : "Đã cập nhật hồ sơ nhân viên.",
                                "Thành công", JOptionPane.INFORMATION_MESSAGE); dispose(); }
                        catch (Exception ex) { showFormError(causeMessage(ex)); setSaving(false); }
                    }
                }.execute();
            } catch (DateTimeParseException ex) { showFormError("Ngày sinh và ngày vào làm phải đúng định dạng dd/MM/yyyy và là ngày hợp lệ."); }
            catch (Exception ex) { showFormError(ex.getMessage()); }
        }

        private void setSaving(boolean value) {
            saving = value; btnSave.setEnabled(!value); btnCancel.setEnabled(!value);
            btnSave.setText(value ? "Đang lưu…" : "Lưu");
            setTreeEnabled(getContentPane(), !value);
            btnSave.setEnabled(!value); btnCancel.setEnabled(!value);
            cboRole.setEnabled(!value && adding);
        }
        private void showFormError(String message) { lblError.setText(message == null ? "Không thể lưu hồ sơ." : message); }
        private void clearFormError() { if (!saving) lblError.setText(""); }
    }

    private static JPanel fieldPanel(String title, JComponent input) {
        JPanel panel = new JPanel(new BorderLayout(0, 6)); panel.setOpaque(false);
        panel.add(label(title, 12, BODY, Font.PLAIN), BorderLayout.NORTH); panel.add(input, BorderLayout.CENTER); return panel;
    }
    private static void addField(JPanel parent, GridBagConstraints g, String title, JComponent input,
                                 int x, int y, int width, int height) {
        if (input instanceof JTextField tf) {
            tf.setFont(new Font("SansSerif", Font.PLAIN, 13)); tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xD7E1EF)), new EmptyBorder(0, 10, 0, 10)));
            tf.setPreferredSize(new Dimension(220, 42)); tf.setBackground(WHITE);
        }
        JPanel field = fieldPanel(title, input);
        g.gridx = x; g.gridy = y; g.gridwidth = width; g.gridheight = height; g.weightx = 1;
        g.insets = new Insets(0, 0, 12, 12); parent.add(field, g); g.gridwidth = 1; g.gridheight = 1;
    }
    private static JDateChooser dateChooser(LocalDate initial) {
        Date value = Date.from(initial.atStartOfDay(ZoneId.systemDefault()).toInstant());
        JDateChooser chooser = new JDateChooser();
        chooser.setDateFormatString("dd/MM/yyyy"); chooser.setDate(value); chooser.setMaxSelectableDate(new Date());
        chooser.setPreferredSize(new Dimension(220, 42)); chooser.setMinimumSize(new Dimension(170, 42));
        if (chooser.getDateEditor() instanceof JTextFieldDateEditor editor) {
            editor.setEditable(false); editor.setBorder(new EmptyBorder(0, 10, 0, 2));
            editor.setMargin(new Insets(0, 0, 0, 0)); editor.setFont(new Font("SansSerif", Font.PLAIN, 13));
        }
        return chooser;
    }
    private static void setDate(JDateChooser chooser, LocalDate date) {
        chooser.setDate(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }
    private static void applyFilter(javax.swing.text.JTextComponent field, int maxLength, boolean digitsOnly) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                replace(fb, offset, 0, string, attr);
            }
            @Override public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                String value = text == null ? "" : text;
                if (digitsOnly && !value.matches("\\d*")) return;
                int next = fb.getDocument().getLength() - length + value.length();
                if (next <= maxLength) fb.replace(offset, length, value, attrs);
            }
        });
    }
    private static String causeMessage(Exception e) {
        Throwable cause = e instanceof ExecutionException && e.getCause() != null ? e.getCause() : e;
        return cause.getMessage();
    }
    private static void setTreeEnabled(Component component, boolean enabled) {
        component.setEnabled(enabled);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) setTreeEnabled(child, enabled);
        }
    }
}
