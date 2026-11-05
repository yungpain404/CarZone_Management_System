package ui;

import controller.QuanLyTaiKhoanController;
import controller.PhanQuyenController;
import dao.QuyenTaiKhoanDAO;
import entity.NhanVien;
import entity.TaiKhoan;
import enums.CheDoTruyCap;
import enums.VaiTro;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Quản lý tài khoản Chủ cửa hàng; không truy vấn danh sách cho đến khi có tìm kiếm. */
public class QuanLyTaiKhoanUI extends JPanel {
    private static final Color BG = new Color(0xF4F7FB), WHITE = Color.WHITE;
    private static final Color BORDER = new Color(0xE3E9F2), PRIMARY = new Color(0x2563EB);
    private static final Color TEXT = new Color(0x14233D), BODY = new Color(0x40516A), SUBTLE = new Color(0x7A889C);

    private final QuanLyTaiKhoanController controller = new QuanLyTaiKhoanController();
    private final PhanQuyenController phanQuyenController = new PhanQuyenController();
    private final JTextField txtTimKiem = new JTextField();
    private final JButton btnTim = button("Tìm kiếm", true, 98), btnLamMoi = button("Làm mới", false, 92),
            btnThem = button("Thêm tài khoản", true, 136),
            btnSua = button("Sửa", false, 64), btnXoa = button("Xóa", false, 64),
            btnPhanQuyen = button("Phân quyền", false, 112);
    private final JLabel lblStatus = label("", 12, SUBTLE, Font.PLAIN);
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{
            "Tên đăng nhập", "Mã nhân viên", "Nhân viên liên kết", "Vai trò", "Đã có tài khoản", "Trạng thái"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private List<NhanVien> ketQua = new ArrayList<>();
    private boolean daTimKiem, dangXuLy;

    public QuanLyTaiKhoanUI() {
        setLayout(new BorderLayout()); setBackground(BG); setBorder(new EmptyBorder(24, 24, 22, 24));
        add(buildContent(), BorderLayout.CENTER);
        btnTim.addActionListener(e -> timKiem()); txtTimKiem.addActionListener(e -> timKiem());
        btnLamMoi.addActionListener(e -> lamMoi());
        btnThem.addActionListener(e -> moFormThem()); btnSua.addActionListener(e -> moFormSua()); btnXoa.addActionListener(e -> xoa());
        btnPhanQuyen.addActionListener(e -> {
            NhanVien nv = selected();
            if (nv != null && nv.getTaiKhoan() != null)
                new PermissionDialog(SwingUtilities.getWindowAncestor(this), nv).setVisible(true);
        });
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
        actions.add(btnThem); actions.add(btnSua); actions.add(btnXoa); actions.add(btnPhanQuyen); addFull(root, actions); root.add(gap(12));
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
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 22)); buttons.setOpaque(false); buttons.add(btnTim); buttons.add(btnLamMoi);
        g.gridx = 1; g.weightx = 0; g.fill = GridBagConstraints.NONE; g.insets = new Insets(0, 0, 0, 0); row.add(buttons, g);
        return row;
    }

    private void styleTable() {
        table.setFont(new Font("SansSerif", Font.PLAIN, 13)); table.setForeground(BODY); table.setRowHeight(44);
        table.setShowVerticalLines(false); table.setGridColor(BORDER); table.setSelectionBackground(new Color(0xEDF4FF));
        table.setSelectionForeground(PRIMARY); table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setForeground(BODY); table.getTableHeader().setBackground(new Color(0xF7F9FC));
        table.getTableHeader().setPreferredSize(new Dimension(0, 42)); table.setAutoCreateRowSorter(true);
        int[] widths = {130, 130, 250, 170, 145, 140};
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

    /** Tải trực tiếp hồ sơ chưa có tài khoản khi điều hướng từ KPI Dashboard. */
    public void locHoSoChuaCoTaiKhoanTuDashboard() {
        txtTimKiem.setText("");
        dangXuLy = true; capNhatNut(); lblStatus.setText("Đang tải hồ sơ chưa có tài khoản…");
        chayNen(controller::layNhanVienChuaCoTaiKhoan, rows -> {
            ketQua = rows; daTimKiem = true; hienThi(rows);
            lblStatus.setText(rows.isEmpty() ? "Không có hồ sơ chưa được cấp tài khoản." : rows.size() + " hồ sơ chưa có tài khoản.");
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
                    tk == null ? "Chưa có" : "Đã có", tk == null ? "—" : tk.isTrangThaiHoatDong() ? "Hoạt động" : "Vô hiệu hóa"});
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
        btnTim.setEnabled(!dangXuLy); btnLamMoi.setEnabled(!dangXuLy);
        btnThem.setEnabled(!dangXuLy); btnSua.setEnabled(!dangXuLy && selected && nv.getTaiKhoan() != null);
        btnXoa.setEnabled(!dangXuLy && selected && nv.getTaiKhoan() != null);
        btnPhanQuyen.setEnabled(!dangXuLy && selected && nv.getTaiKhoan() != null && nv.getVaiTro() != VaiTro.CHU_CUA_HANG);
    }

    private NhanVien selected() {
        int view = table.getSelectedRow(); if (view < 0) return null;
        int row = table.convertRowIndexToModel(view); return row >= 0 && row < ketQua.size() ? ketQua.get(row) : null;
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

    private final class PermissionDialog extends JDialog {
        private final NhanVien employee;
        private final JComboBox<CheDoTruyCap> cboMode = new JComboBox<>(CheDoTruyCap.values());
        private final JPanel pnlPermissions = new JPanel();
        private final JLabel lblStatus = label("Đang tải cấu hình tài khoản…", 12, SUBTLE, Font.PLAIN);
        private final JButton btnSavePermissions = button("Lưu phân quyền", true, 148);
        private final java.util.Map<String, JCheckBox> checks = new java.util.LinkedHashMap<>();
        private boolean busy;
        private boolean loading;

        PermissionDialog(Window owner, NhanVien employee) {
            super(owner, "CarZone — Phân quyền tài khoản", ModalityType.APPLICATION_MODAL);
            this.employee = employee;
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setSize(760, 680); setMinimumSize(new Dimension(680, 580)); setLocationRelativeTo(owner);

            JPanel root = new JPanel(new BorderLayout(0, 14)); root.setBackground(BG);
            root.setBorder(new EmptyBorder(22, 24, 18, 24));
            JPanel content = new JPanel(); content.setOpaque(false);
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            JLabel title = label("Phân quyền tài khoản", 22, PRIMARY, Font.BOLD); title.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel identity = label(employee.getMaNV() + " · " + employee.getHoTen() + " · Hồ sơ: " + tenVaiTro(employee.getVaiTro()),
                    13, BODY, Font.BOLD); identity.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel description = label("Chọn các màn hình tài khoản này được phép mở. Quyền chéo vai trò chỉ cho phép xem; thao tác ghi vẫn theo vai trò hồ sơ.",
                    12, SUBTLE, Font.PLAIN); description.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(title); content.add(gap(8)); content.add(identity); content.add(gap(6)); content.add(description); content.add(gap(16));

            JPanel modeRow = new JPanel(new BorderLayout(12, 0)); modeRow.setOpaque(false);
            modeRow.add(label("Tập quyền truy cập", 13, BODY, Font.BOLD), BorderLayout.WEST);
            cboMode.setFont(new Font("SansSerif", Font.PLAIN, 13));
            cboMode.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                        boolean selected, boolean focus) {
                    super.getListCellRendererComponent(list, value, index, selected, focus);
                    if (value instanceof CheDoTruyCap mode) setText(tenCheDo(mode));
                    return this;
                }
            });
            modeRow.add(cboMode, BorderLayout.CENTER); modeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            modeRow.setAlignmentX(Component.LEFT_ALIGNMENT); content.add(modeRow); content.add(gap(12));

            pnlPermissions.setLayout(new BoxLayout(pnlPermissions, BoxLayout.Y_AXIS));
            pnlPermissions.setBackground(WHITE); pnlPermissions.setBorder(new EmptyBorder(10, 14, 10, 14));
            pnlPermissions.add(label("TIẾP NHẬN VIÊN", 12, PRIMARY, Font.BOLD));
            for (PhanQuyenController.ChucNang item : phanQuyenController.danhSachChucNang()) {
                if (item.nhomVaiTro() == VaiTro.TIEP_NHAN_VIEN) themCheck(item);
            }
            pnlPermissions.add(gap(8));
            pnlPermissions.add(label("KỸ THUẬT VIÊN", 12, PRIMARY, Font.BOLD));
            for (PhanQuyenController.ChucNang item : phanQuyenController.danhSachChucNang()) {
                if (item.nhomVaiTro() == VaiTro.KY_THUAT_VIEN) themCheck(item);
            }
            pnlPermissions.add(gap(8));
            pnlPermissions.add(label("DÙNG CHUNG", 12, PRIMARY, Font.BOLD));
            for (PhanQuyenController.ChucNang item : phanQuyenController.danhSachChucNang()) {
                if (item.nhomVaiTro() == null) themCheck(item);
            }
            JScrollPane scroll = new JScrollPane(pnlPermissions); scroll.setBorder(BorderFactory.createLineBorder(BORDER));
            scroll.setPreferredSize(new Dimension(700, 390)); scroll.setMinimumSize(new Dimension(520, 280));
            scroll.setAlignmentX(Component.LEFT_ALIGNMENT); content.add(scroll); content.add(gap(10));
            lblStatus.setAlignmentX(Component.LEFT_ALIGNMENT); content.add(lblStatus);
            root.add(content, BorderLayout.CENTER);

            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); footer.setOpaque(false);
            JButton close = button("Đóng", false, 78); footer.add(btnSavePermissions); footer.add(close);
            root.add(footer, BorderLayout.SOUTH); setContentPane(root);
            cboMode.addActionListener(e -> { if (!loading) apDungCheDoDangChon(); });
            btnSavePermissions.addActionListener(e -> luuQuyen()); close.addActionListener(e -> dispose());
            taiCauHinh();
        }

        private void themCheck(PhanQuyenController.ChucNang item) {
            JCheckBox check = new JCheckBox(item.ten());
            check.setFont(new Font("SansSerif", Font.PLAIN, 13)); check.setForeground(BODY);
            check.setBackground(WHITE); check.setBorder(new EmptyBorder(5, 4, 5, 4));
            check.setAlignmentX(Component.LEFT_ALIGNMENT); check.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            checks.put(item.ma(), check); pnlPermissions.add(check);
        }

        private void taiCauHinh() {
            busy = true; setControlsEnabled(false); lblStatus.setText("Đang tải cấu hình tài khoản…");
            new SwingWorker<QuyenTaiKhoanDAO.CauHinh, Void>() {
                @Override protected QuyenTaiKhoanDAO.CauHinh doInBackground() {
                    return phanQuyenController.layCauHinh(employee.getMaNV());
                }
                @Override protected void done() {
                    try {
                        QuyenTaiKhoanDAO.CauHinh config = get();
                        CheDoTruyCap mode = config == null ? CheDoTruyCap.THEO_VAI_TRO : config.cheDo();
                        cboMode.setSelectedItem(mode);
                        Set<String> functions = config == null
                                ? PhanQuyenController.chucNangTheoVaiTro(employee.getVaiTro()) : config.maChucNang();
                        if (config != null && mode != CheDoTruyCap.TUY_CHINH)
                            functions = PhanQuyenController.chucNangTheoProfil(mode, employee.getVaiTro(), Set.of());
                        for (var entry : checks.entrySet()) entry.getValue().setSelected(functions.contains(entry.getKey()));
                        apDungCheDoDangChon();
                        lblStatus.setText("Tập quyền chỉ quyết định màn hình được mở; vai trò hồ sơ vẫn là " + tenVaiTro(employee.getVaiTro()) + ".");
                    } catch (Exception ex) {
                        lblStatus.setText("Không tải được quyền cá nhân. Hãy kiểm tra migration V7. " + loiTuWorker(ex));
                        btnSavePermissions.setEnabled(false);
                    } finally {
                        busy = false; cboMode.setEnabled(true); apDungCheDoDangChon();
                    }
                }
            }.execute();
        }

        private void apDungCheDoDangChon() {
            CheDoTruyCap mode = (CheDoTruyCap) cboMode.getSelectedItem();
            boolean editable = mode == CheDoTruyCap.TUY_CHINH && !busy;
            checks.values().forEach(check -> check.setEnabled(editable));
            btnSavePermissions.setEnabled(!busy);
            if (mode != null && mode != CheDoTruyCap.TUY_CHINH) {
                Set<String> preset = mode == CheDoTruyCap.THEO_VAI_TRO
                        ? PhanQuyenController.chucNangTheoVaiTro(employee.getVaiTro())
                        : PhanQuyenController.chucNangTheoProfil(mode, employee.getVaiTro(), Set.of());
                checks.forEach((key, check) -> check.setSelected(preset.contains(key)));
            }
        }

        private void luuQuyen() {
            if (busy) return;
            CheDoTruyCap mode = (CheDoTruyCap) cboMode.getSelectedItem();
            Set<String> functions = new java.util.LinkedHashSet<>();
            checks.forEach((key, check) -> { if (check.isSelected()) functions.add(key); });
            busy = true; setControlsEnabled(false); lblStatus.setText("Đang lưu quyền…");
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() { phanQuyenController.luu(employee.getMaNV(), mode, functions); return null; }
                @Override protected void done() {
                    try { get(); lblStatus.setText("Đã lưu tập quyền cho " + employee.getMaNV() + ". Có hiệu lực khi tài khoản đăng nhập lại."); }
                    catch (Exception ex) { lblStatus.setText("Không lưu được quyền: " + loiTuWorker(ex)); }
                    finally { busy = false; setControlsEnabled(true); apDungCheDoDangChon(); }
                }
            }.execute();
        }

        private void setControlsEnabled(boolean enabled) {
            cboMode.setEnabled(enabled); checks.values().forEach(check -> check.setEnabled(enabled));
            btnSavePermissions.setEnabled(enabled);
        }

        private String tenCheDo(CheDoTruyCap mode) {
            return switch (mode) {
                case THEO_VAI_TRO -> "Theo vai trò hồ sơ hiện tại";
                case TOAN_BO_TNV -> "Toàn bộ chức năng Tiếp nhận viên";
                case TOAN_BO_KTV -> "Toàn bộ chức năng Kỹ thuật viên";
                case TUY_CHINH -> "Tự chọn chức năng được xem";
            };
        }
    }

    private static String loiTuWorker(Exception exception) {
        Throwable cause = exception instanceof ExecutionException && exception.getCause() != null
                ? exception.getCause() : exception;
        return cause.getMessage() == null ? "Có lỗi trong quá trình xử lý." : cause.getMessage();
    }

    private final class AccountDialog extends JDialog {
        private final JComboBox<NhanVien> cboNhanVien = new JComboBox<>();
        private final JTextField txtMaNhanVien = new JTextField();
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
            NhanVien initial = edit ? selectedEmployee : (NhanVien) cboNhanVien.getSelectedItem();
            txtMaNhanVien.setEditable(false); txtMaNhanVien.setFocusable(false);
            txtMaNhanVien.setBackground(new Color(0xF4F7FB));
            txtMaNhanVien.setText(initial == null ? "—" : initial.getMaNV());
            if (!edit) addField(form, "Mã nhân viên", txtMaNhanVien);
            txtUsername.setEditable(false); txtUsername.setText(edit ? account.getTenDangNhap() : "—");
            txtRole.setEditable(false);
            txtRole.setText(initial == null ? "" : tenVaiTro(initial.getVaiTro()));
            if (!edit) cboNhanVien.addActionListener(e -> {
                NhanVien picked = (NhanVien) cboNhanVien.getSelectedItem();
                txtMaNhanVien.setText(picked == null ? "—" : picked.getMaNV());
                txtRole.setText(picked == null ? "" : tenVaiTro(picked.getVaiTro()));
            });
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
