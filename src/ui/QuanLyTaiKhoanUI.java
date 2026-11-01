package ui;

import controller.QuanLyTaiKhoanController;
import entity.NhanVien;
import entity.TaiKhoan;
import enums.VaiTro;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Quản lý tài khoản Chủ cửa hàng; không truy vấn danh sách cho đến khi có tìm kiếm. */
public class QuanLyTaiKhoanUI extends JPanel {
    private static final Color BG = new Color(0xF4F7FB), WHITE = Color.WHITE;
    private static final Color BORDER = new Color(0xE3E9F2), PRIMARY = new Color(0x2563EB);
    private static final Color TEXT = new Color(0x14233D), BODY = new Color(0x40516A), SUBTLE = new Color(0x7A889C);

    private final QuanLyTaiKhoanController controller = new QuanLyTaiKhoanController();
    private final JTextField txtTimKiem = new JTextField();
    private final JButton btnTim = button("Tìm kiếm", true, 98), btnLamMoi = button("Làm mới", false, 92),
            btnQuayLai = button("Quay lại", false, 88), btnThem = button("Thêm tài khoản", true, 136),
            btnSua = button("Sửa", false, 64), btnXoa = button("Xóa", false, 64);
    private final JLabel lblStatus = label("", 12, SUBTLE, Font.PLAIN);
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{
            "Tên đăng nhập", "Mã nhân viên", "Nhân viên liên kết", "Vai trò", "Đã có tài khoản", "Trạng thái", "Thao tác"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private List<NhanVien> ketQua = new ArrayList<>();
    private boolean daTimKiem, dangXuLy;

    public QuanLyTaiKhoanUI() {
        setLayout(new BorderLayout()); setBackground(BG); setBorder(new EmptyBorder(24, 24, 22, 24));
        add(buildContent(), BorderLayout.CENTER);
        btnTim.addActionListener(e -> timKiem()); txtTimKiem.addActionListener(e -> timKiem());
        btnLamMoi.addActionListener(e -> lamMoi()); btnQuayLai.addActionListener(e -> moDashboard());
        btnThem.addActionListener(e -> moFormThem()); btnSua.addActionListener(e -> moFormSua()); btnXoa.addActionListener(e -> xoa());
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) capNhatNut(); });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    NhanVien nv = selected(); if (nv != null && nv.getTaiKhoan() == null) moFormThem(); else moFormSua();
                }
            }
        });
        capNhatNut(); SwingUtilities.invokeLater(txtTimKiem::requestFocusInWindow);
    }

    private JComponent buildContent() {
        JPanel root = new MainFrame.ScrollableContentPanel(); root.setOpaque(false);
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0)); heading.setOpaque(false);
        heading.add(label("🛡️", 25, TEXT, Font.PLAIN)); heading.add(label("Quản lý tài khoản và phân quyền", 24, TEXT, Font.BOLD));
        addFull(root, heading); root.add(gap(18)); addFull(root, searchRow()); root.add(gap(14));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); actions.setOpaque(false); actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.add(btnThem); actions.add(btnSua); actions.add(btnXoa); addFull(root, actions); root.add(gap(12));
        styleTable(); JScrollPane tableScroll = new JScrollPane(table); tableScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        tableScroll.getViewport().setBackground(WHITE); tableScroll.setPreferredSize(new Dimension(1000, 390));
        JPanel card = new JPanel(new BorderLayout()); card.setBackground(WHITE); card.setBorder(BorderFactory.createLineBorder(BORDER));
        card.add(tableScroll, BorderLayout.CENTER); card.setAlignmentX(Component.LEFT_ALIGNMENT); addFull(root, card);
        root.add(gap(8)); lblStatus.setAlignmentX(Component.LEFT_ALIGNMENT); addFull(root, lblStatus);
        JScrollPane outer = new JScrollPane(root); outer.setBorder(null); outer.setOpaque(false); outer.getViewport().setOpaque(false);
        outer.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); outer.getVerticalScrollBar().setUnitIncrement(18);
        return outer;
    }

    private JComponent searchRow() {
        JPanel row = new JPanel(new GridBagLayout()); row.setOpaque(false); row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel field = new JPanel(new BorderLayout(0, 6)); field.setOpaque(false); field.add(label("Tìm mã / họ tên / điện thoại / tên đăng nhập", 13, BODY, Font.PLAIN), BorderLayout.NORTH);
        JPanel input = new JPanel(new BorderLayout()); input.setBackground(WHITE); input.setBorder(new RoundedLineBorder(new Color(0xC9D5E5), 10));
        txtTimKiem.setFont(new Font("SansSerif", Font.PLAIN, 13)); txtTimKiem.putClientProperty("JTextField.placeholderText", "Nhập thông tin tìm kiếm…");
        txtTimKiem.setBorder(new EmptyBorder(0, 11, 0, 8)); txtTimKiem.setOpaque(false); input.add(txtTimKiem);
        field.add(input); field.setPreferredSize(new Dimension(480, 68));
        GridBagConstraints g = new GridBagConstraints(); g.gridy = 0; g.anchor = GridBagConstraints.WEST; g.insets = new Insets(0, 0, 0, 12);
        g.gridx = 0; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL; row.add(field, g);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 22)); buttons.setOpaque(false); buttons.add(btnTim); buttons.add(btnLamMoi); buttons.add(btnQuayLai);
        g.gridx = 1; g.weightx = 0; g.fill = GridBagConstraints.NONE; g.insets = new Insets(0, 0, 0, 0); row.add(buttons, g);
        return row;
    }

    private void styleTable() {
        table.setFont(new Font("SansSerif", Font.PLAIN, 13)); table.setForeground(BODY); table.setRowHeight(44);
        table.setShowVerticalLines(false); table.setGridColor(BORDER); table.setSelectionBackground(new Color(0xEDF4FF));
        table.setSelectionForeground(PRIMARY); table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setForeground(BODY); table.getTableHeader().setBackground(new Color(0xF7F9FC));
        table.getTableHeader().setPreferredSize(new Dimension(0, 42)); table.setAutoCreateRowSorter(true);
        int[] widths = {120, 120, 210, 150, 145, 120, 100};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
    }

    private void timKiem() {
        String keyword = txtTimKiem.getText().trim();
        if (!UIInputRules.isSearch(keyword)) {
            String message = "Từ khóa tìm kiếm tối đa 150 ký tự; chỉ nhập chữ, số và dấu câu.";
            lblStatus.setText(message);
            JOptionPane.showMessageDialog(this, message, "Sai định dạng tìm kiếm", JOptionPane.WARNING_MESSAGE);
            txtTimKiem.requestFocusInWindow(); return;
        }
        if (keyword.isEmpty()) { JOptionPane.showMessageDialog(this, "Nhập thông tin cần tìm trước khi tìm kiếm.", "Tìm kiếm", JOptionPane.INFORMATION_MESSAGE); txtTimKiem.requestFocusInWindow(); return; }
        dangXuLy = true; capNhatNut(); lblStatus.setText("Đang tìm kiếm…");
        chayNen(() -> controller.timKiemHoSo(keyword), rows -> {
            ketQua = rows; daTimKiem = true; hienThi(rows);
            lblStatus.setText(rows.isEmpty() ? "Không tìm thấy hồ sơ phù hợp." : "Tìm thấy " + rows.size() + " hồ sơ.");
        });
    }

    private void lamMoi() {
        txtTimKiem.setText(""); ketQua.clear(); daTimKiem = false; model.setRowCount(0);
        lblStatus.setText(""); capNhatNut(); txtTimKiem.requestFocusInWindow();
    }

    private void hienThi(List<NhanVien> rows) {
        model.setRowCount(0);
        for (NhanVien nv : rows) {
            TaiKhoan tk = nv.getTaiKhoan();
            model.addRow(new Object[]{tk == null ? "—" : tk.getTenDangNhap(), nv.getMaNV(), nv.getHoTen(), tenVaiTro(nv.getVaiTro()),
                    tk == null ? "Chưa có" : "Đã có", tk == null ? "—" : tk.isTrangThaiHoatDong() ? "Hoạt động" : "Vô hiệu hóa",
                    tk == null ? "Tạo" : "Sửa"});
        }
        if (model.getRowCount() > 0) table.setRowSelectionInterval(0, 0); capNhatNut();
    }

    private void moFormThem() {
        NhanVien current = selected();
        dangXuLy = true; capNhatNut(); lblStatus.setText("Đang tải hồ sơ chưa được cấp tài khoản…");
        chayNen(controller::layNhanVienChuaCoTaiKhoan, employees -> {
            dangXuLy = false; capNhatNut();
            if (employees.isEmpty()) { JOptionPane.showMessageDialog(this, "Không còn hồ sơ nhân viên chưa có tài khoản.", "Tạo tài khoản", JOptionPane.INFORMATION_MESSAGE); return; }
            NhanVien preselect = current != null && current.getTaiKhoan() == null ? current : null;
            AccountDialog dialog = new AccountDialog(SwingUtilities.getWindowAncestor(this), employees, preselect, null);
            dialog.setVisible(true);
            if (dialog.saved) { lblStatus.setText("Đã tạo tài khoản " + dialog.resultUsername + "."); if (daTimKiem) timKiem(); }
        });
    }

    private void moFormSua() {
        NhanVien nv = selected();
        if (nv == null || nv.getTaiKhoan() == null) return;
        AccountDialog dialog = new AccountDialog(SwingUtilities.getWindowAncestor(this), List.of(), nv, nv.getTaiKhoan());
        dialog.setVisible(true);
        if (dialog.saved && daTimKiem) timKiem();
    }

    private void xoa() {
        NhanVien nv = selected(); if (nv == null || nv.getTaiKhoan() == null) return;
        int confirm = JOptionPane.showConfirmDialog(this,
                "Xóa tài khoản " + nv.getTaiKhoan().getTenDangNhap() + "? Hồ sơ nhân viên sẽ được giữ lại.",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        String username = nv.getTaiKhoan().getTenDangNhap(); dangXuLy = true; capNhatNut();
        chayNen(() -> { controller.xoaTaiKhoan(username); return username; }, deleted -> {
            lblStatus.setText("Đã xóa tài khoản " + deleted + ". Hồ sơ nhân viên vẫn được giữ lại."); timKiem();
        });
    }

    private void capNhatNut() {
        NhanVien nv = selected(); boolean selected = nv != null;
        btnTim.setEnabled(!dangXuLy); btnLamMoi.setEnabled(!dangXuLy); btnQuayLai.setEnabled(!dangXuLy);
        btnThem.setEnabled(!dangXuLy); btnSua.setEnabled(!dangXuLy && selected && nv.getTaiKhoan() != null);
        btnXoa.setEnabled(!dangXuLy && selected && nv.getTaiKhoan() != null);
    }

    private NhanVien selected() {
        int view = table.getSelectedRow(); if (view < 0) return null;
        int row = table.convertRowIndexToModel(view); return row >= 0 && row < ketQua.size() ? ketQua.get(row) : null;
    }

    private void moDashboard() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        if (owner instanceof MainFrame frame) frame.moPageTheoNavigation("NAV_DASHBOARD");
    }

    private <T> void chayNen(Supplier<T> task, Consumer<T> success) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() { return task.get(); }
            @Override protected void done() {
                dangXuLy = false; capNhatNut();
                try { success.accept(get()); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); loi("Tác vụ bị gián đoạn."); }
                catch (ExecutionException ex) { Throwable cause = ex.getCause(); loi(cause == null ? ex.getMessage() : cause.getMessage()); }
            }
        }.execute();
    }

    private void loi(String message) {
        JOptionPane.showMessageDialog(this, message == null ? "Có lỗi khi xử lý tài khoản." : message,
                "Không thể xử lý", JOptionPane.ERROR_MESSAGE);
        lblStatus.setText("Thao tác chưa hoàn tất.");
    }

    private final class AccountDialog extends JDialog {
        private final JComboBox<NhanVien> cboNhanVien = new JComboBox<>();
        private final JTextField txtUsername = new JTextField();
        private final JTextField txtRole = new JTextField();
        private final JPasswordField txtPassword = new JPasswordField(), txtConfirm = new JPasswordField();
        private final JLabel lblError = label("", 12, new Color(0xCA4850), Font.PLAIN);
        private final JButton btnSave = button("Lưu", true, 64);
        private boolean saved; private String resultUsername;

        AccountDialog(Window owner, List<NhanVien> employees, NhanVien selectedEmployee, TaiKhoan account) {
            super(owner, account == null ? "CarZone — Tạo tài khoản" : "CarZone — Sửa tài khoản", ModalityType.APPLICATION_MODAL);
            boolean edit = account != null; setDefaultCloseOperation(DISPOSE_ON_CLOSE); setSize(700, edit ? 510 : 580); setMinimumSize(new Dimension(640, edit ? 490 : 550)); setLocationRelativeTo(owner);
            JPanel root = new JPanel(new BorderLayout()); root.setBackground(BG);
            JPanel form = new JPanel(); form.setOpaque(false); form.setBorder(new EmptyBorder(24, 24, 20, 24)); form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
            JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); title.setOpaque(false); title.setAlignmentX(Component.LEFT_ALIGNMENT);
            title.add(label("🛡️", 20, TEXT, Font.PLAIN)); title.add(label(edit ? "Sửa tài khoản" : "Tạo tài khoản", 21, PRIMARY, Font.BOLD));
            form.add(title); form.add(Box.createVerticalStrut(20));
            if (!edit) {
                for (NhanVien employee : employees) cboNhanVien.addItem(employee);
                cboNhanVien.setRenderer(new DefaultListCellRenderer() {
                    @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                        super.getListCellRendererComponent(list, value, index, selected, focus);
                        if (value instanceof NhanVien nv) setText(nv.getMaNV() + " — " + nv.getHoTen()); return this;
                    }
                });
                addField(form, "Nhân viên chưa được cấp tài khoản *", cboNhanVien);
            }
            txtUsername.setEditable(false); txtUsername.setText(edit ? account.getTenDangNhap() : "—");
            txtRole.setEditable(false); NhanVien initial = edit ? selectedEmployee : (NhanVien) cboNhanVien.getSelectedItem();
            txtRole.setText(initial == null ? "" : tenVaiTro(initial.getVaiTro()));
            if (!edit) cboNhanVien.addActionListener(e -> { NhanVien picked = (NhanVien) cboNhanVien.getSelectedItem(); txtRole.setText(picked == null ? "" : tenVaiTro(picked.getVaiTro())); });
            JPanel details = new JPanel(new GridLayout(1, 2, 12, 0)); details.setOpaque(false);
            details.add(fieldPanel("Tên đăng nhập", txtUsername)); details.add(fieldPanel("Vai trò", txtRole)); addFull(form, details); form.add(Box.createVerticalStrut(14));
            addField(form, "Mật khẩu mới *", txtPassword); addField(form, "Xác nhận mật khẩu *", txtConfirm);
            lblError.setAlignmentX(Component.LEFT_ALIGNMENT); form.add(lblError); form.add(Box.createVerticalGlue());
            root.add(form, BorderLayout.CENTER);
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12)); footer.setBackground(WHITE); footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));
            JButton cancel = button("Hủy", false, 64); footer.add(btnSave); footer.add(cancel); root.add(footer, BorderLayout.SOUTH); setContentPane(root);
            btnSave.addActionListener(e -> luu(edit)); cancel.addActionListener(e -> dispose());
            txtPassword.addActionListener(e -> txtConfirm.requestFocusInWindow()); txtConfirm.addActionListener(e -> luu(edit));
            SwingUtilities.invokeLater(() -> (edit ? txtPassword : cboNhanVien).requestFocusInWindow());
        }

        private void luu(boolean edit) {
            String password = new String(txtPassword.getPassword()), confirm = new String(txtConfirm.getPassword());
            lblError.setText(" ");
            if (password.isBlank()) { lblError.setText("Vui lòng nhập mật khẩu mới."); txtPassword.requestFocusInWindow(); return; }
            if (!password.equals(confirm)) { lblError.setText("Mật khẩu xác nhận không khớp."); txtConfirm.requestFocusInWindow(); return; }
            NhanVien employee = edit ? null : (NhanVien) cboNhanVien.getSelectedItem();
            if (!edit && employee == null) { lblError.setText("Vui lòng chọn hồ sơ nhân viên."); return; }
            btnSave.setEnabled(false); btnSave.setText("Đang lưu…"); txtPassword.setEnabled(false); txtConfirm.setEnabled(false);
            String username = edit ? txtUsername.getText() : null, maNV = employee == null ? null : employee.getMaNV();
            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() {
                    if (edit) { controller.capNhatMatKhau(username, password); return username; }
                    return controller.taoTaiKhoan(maNV, password);
                }
                @Override protected void done() {
                    btnSave.setEnabled(true); btnSave.setText("Lưu"); txtPassword.setEnabled(true); txtConfirm.setEnabled(true);
                    try { saved = true; resultUsername = get(); dispose(); }
                    catch (InterruptedException ex) { Thread.currentThread().interrupt(); lblError.setText("Tác vụ bị gián đoạn."); }
                    catch (ExecutionException ex) {
                        Throwable cause = ex.getCause(); lblError.setText(cause == null || cause.getMessage() == null
                                ? "Không thể lưu tài khoản." : cause.getMessage());
                    }
                }
            }.execute();
        }
    }

    private static JPanel fieldPanel(String title, JComponent input) {
        JPanel panel = new JPanel(new BorderLayout(0, 6)); panel.setOpaque(false); panel.add(label(title, 13, BODY, Font.PLAIN), BorderLayout.NORTH);
        if (input instanceof JTextField field) { field.setFont(new Font("SansSerif", Font.PLAIN, 13)); field.setBorder(new RoundedLineBorder(new Color(0xC9D5E5), 10)); field.setPreferredSize(new Dimension(200, 44)); }
        else { input.setFont(new Font("SansSerif", Font.PLAIN, 13)); input.setPreferredSize(new Dimension(200, 44)); }
        panel.add(input, BorderLayout.CENTER); return panel;
    }
    private static void addField(JPanel parent, String title, JComponent input) {
        JPanel panel = fieldPanel(title, input); panel.setAlignmentX(Component.LEFT_ALIGNMENT); panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66)); parent.add(panel); parent.add(Box.createVerticalStrut(13));
    }
    private static String tenVaiTro(VaiTro role) {
        if (role == null) return "";
        return switch (role) { case CHU_CUA_HANG -> "Chủ cửa hàng"; case TIEP_NHAN_VIEN -> "Tiếp nhận viên"; case KY_THUAT_VIEN -> "Kỹ thuật viên"; };
    }
    private static void addFull(JPanel parent, JComponent child) { child.setAlignmentX(Component.LEFT_ALIGNMENT); child.setMaximumSize(new Dimension(Integer.MAX_VALUE, child.getPreferredSize().height)); parent.add(child); }
    private static Component gap(int h) { return Box.createVerticalStrut(h); }
    private static JLabel label(String text, int size, Color color, int style) { JLabel l = new JLabel(text); l.setFont(new Font("SansSerif", style, size)); l.setForeground(color); return l; }
    private static JButton button(String text, boolean primary, int width) {
        JButton b = new MainFrame.RoundedButton(text, 10, primary ? PRIMARY : WHITE,
                primary ? new Color(0x1D4ED8) : new Color(0xF1F5F9), primary ? PRIMARY : new Color(0xC9D5E5));
        b.setFont(new Font("SansSerif", Font.BOLD, 13)); b.setForeground(primary ? WHITE : BODY); b.setPreferredSize(new Dimension(width, 40)); b.setFocusPainted(false); return b;
    }
    private static class RoundedLineBorder extends javax.swing.border.AbstractBorder {
        private final Color color; private final int radius;
        RoundedLineBorder(Color color, int radius) { this.color = color; this.radius = radius; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color); g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius); g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(9, 11, 9, 11); }
    }
}
