package ui;

import controller.KhachHangVaXeController;
import entity.KhachHang;
import entity.Xe;
import enums.VaiTro;
import session.UserSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.util.List;

import static ui.MainFrame.*;

/**
 * Màn hình Quản lý hồ sơ Khách hàng và Xe (UC016 - Tiếp nhận viên).
 * Thiết kế 2 cột: Danh sách khách hàng (trái) và Danh sách xe (phải).
 * Form Thêm/Sửa Khách hàng và Xe sử dụng Modal Dialog chuẩn Figma (image45 & Thêm xe mới).
 */
public class QuanLyKhachHangVaXeUI extends JPanel {

    private final KhachHangVaXeController controller = new KhachHangVaXeController();
    private static final DecimalFormat INT_FMT = new DecimalFormat("#,###");

    // ── Tìm kiếm ─────────────────────────────────────────────────────────────
    private JTextField txtSearch;

    // ── Bảng khách hàng (Cột trái) ───────────────────────────────────────────
    private DefaultTableModel modelKH;
    private JTable tblKhachHang;
    private List<KhachHang> dsKhachHang = List.of();
    private KhachHang khachHangDangChon = null;
    private JLabel lblSoLuongKH;
    private JButton btnThemKH;
    private JButton btnSuaKH;

    // ── Bảng xe (Cột phải) ───────────────────────────────────────────────────
    private DefaultTableModel modelXe;
    private JTable tblXe;
    private List<Xe> dsXe = List.of();
    private Xe xeDangChon = null;
    private JLabel lblXeHeaderTitle;
    private JLabel lblSoLuongXe;
    private JButton btnThemXe;
    private JButton btnSuaXe;
    private JButton btnTaoPhieuTiepNhan;

    public QuanLyKhachHangVaXeUI() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);

        // Đăng ký phím tắt F2: Tạo phiếu tiếp nhận cho xe đang chọn
        dangKyPhimTatF2();

        // Tải dữ liệu ban đầu
        taiDanhSachKhachHang(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TOP BAR: Tiêu đề + Thanh tìm kiếm đa năng
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel pnl = new JPanel(new BorderLayout(12, 0));
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(16, 24, 12, 24));

        // Tiêu đề
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JLabel lblTitle = new JLabel("Hồ sơ khách hàng và xe");
        lblTitle.setFont(f(Font.BOLD, 22));
        lblTitle.setForeground(C_TEXT_HEAD);
        titleRow.add(lblTitle, BorderLayout.WEST);
        pnl.add(titleRow, BorderLayout.NORTH);

        // Thanh tìm kiếm đa năng (SĐT, Biển số xe, Mã KH, Họ tên)
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        searchRow.setOpaque(false);

        txtSearch = new JTextField(32);
        txtSearch.setFont(f(Font.PLAIN, 13));
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm theo số điện thoại, biển số xe, mã khách hàng, họ tên...");
        txtSearch.setPreferredSize(new Dimension(420, 36));
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(4, 10, 4, 10)));
        txtSearch.addActionListener(e -> taiDanhSachKhachHang(txtSearch.getText().trim()));
        searchRow.add(txtSearch);

        JButton btnTimKiem = new RoundedButton("🔍  Tìm kiếm", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
        btnTimKiem.setForeground(C_WHITE);
        btnTimKiem.setFont(f(Font.BOLD, 13));
        btnTimKiem.setPreferredSize(new Dimension(120, 36));
        btnTimKiem.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnTimKiem.addActionListener(e -> taiDanhSachKhachHang(txtSearch.getText().trim()));
        searchRow.add(btnTimKiem);

        JButton btnLamMoi = new RoundedButton("↺  Làm mới", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnLamMoi.setFont(f(Font.PLAIN, 13));
        btnLamMoi.setForeground(C_TEXT_MAIN);
        btnLamMoi.setPreferredSize(new Dimension(110, 36));
        btnLamMoi.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLamMoi.addActionListener(e -> {
            txtSearch.setText("");
            taiDanhSachKhachHang(null);
        });
        searchRow.add(btnLamMoi);

        pnl.add(searchRow, BorderLayout.SOUTH);
        return pnl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAIN CONTENT: 2 cột song song chiếm toàn bộ chiều cao (Master - Detail)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildMainContent() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(0, 24, 20, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 0, 14);

        // Cột trái (Khách hàng)
        gbc.gridx = 0;
        gbc.weightx = 0.48;
        pnl.add(buildPanelKhachHang(), gbc);

        // Cột phải (Xe của khách hàng)
        gbc.gridx = 1;
        gbc.weightx = 0.52;
        gbc.insets = new Insets(0, 0, 0, 0);
        pnl.add(buildPanelXe(), gbc);

        return pnl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CỘT TRÁI: DANH SÁCH KHÁCH HÀNG
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildPanelKhachHang() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(C_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(14, 16, 14, 16)));

        // Header cột khách hàng
        JPanel pnlHead = new JPanel(new BorderLayout());
        pnlHead.setOpaque(false);

        JLabel lblTitleKH = new JLabel("Danh sách khách hàng");
        lblTitleKH.setFont(f(Font.BOLD, 15));
        lblTitleKH.setForeground(C_TEXT_HEAD);
        pnlHead.add(lblTitleKH, BorderLayout.WEST);

        lblSoLuongKH = new JLabel("0 khách hàng");
        lblSoLuongKH.setFont(f(Font.PLAIN, 12));
        lblSoLuongKH.setForeground(C_TEXT_SUBTLE);
        pnlHead.add(lblSoLuongKH, BorderLayout.EAST);
        card.add(pnlHead, BorderLayout.NORTH);

        // Bảng khách hàng
        modelKH = new DefaultTableModel(new String[]{"Mã KH", "Họ tên", "Số điện thoại", "Thành viên"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblKhachHang = new JTable(modelKH);
        tblKhachHang.setFont(f(Font.PLAIN, 13));
        tblKhachHang.setRowHeight(32);
        tblKhachHang.setShowGrid(false);
        tblKhachHang.getTableHeader().setFont(f(Font.BOLD, 12));
        tblKhachHang.getTableHeader().setBackground(new Color(0xF8FAFC));
        tblKhachHang.setSelectionBackground(C_PRIMARY_BG);
        tblKhachHang.setSelectionForeground(C_TEXT_HEAD);
        tblKhachHang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Canh giữa cột Mã KH và SĐT
        DefaultTableCellRenderer centerRend = new DefaultTableCellRenderer();
        centerRend.setHorizontalAlignment(SwingConstants.CENTER);
        tblKhachHang.getColumnModel().getColumn(0).setCellRenderer(centerRend);
        tblKhachHang.getColumnModel().getColumn(2).setCellRenderer(centerRend);

        // Cột thành viên hiển thị sao vàng nếu thân thiết
        DefaultTableCellRenderer tvRend = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setHorizontalAlignment(SwingConstants.CENTER);
                String txt = value != null ? value.toString() : "";
                if (txt.contains("Thân thiết")) {
                    c.setForeground(new Color(0xD97706)); // Vàng cam
                    c.setFont(f(Font.BOLD, 12));
                } else {
                    c.setForeground(C_TEXT_MUTED);
                    c.setFont(f(Font.PLAIN, 12));
                }
                return c;
            }
        };
        tblKhachHang.getColumnModel().getColumn(3).setCellRenderer(tvRend);

        // Sự kiện click chọn khách hàng -> nạp xe bên cột phải
        tblKhachHang.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onChonKhachHang();
            }
        });

        JScrollPane scrollKH = new JScrollPane(tblKhachHang);
        scrollKH.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        card.add(scrollKH, BorderLayout.CENTER);

        // Footer nút thao tác Khách hàng
        JPanel btnRowKH = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRowKH.setOpaque(false);

        btnThemKH = new RoundedButton("+ Thêm khách hàng", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
        btnThemKH.setForeground(C_WHITE);
        btnThemKH.setFont(f(Font.BOLD, 12));
        btnThemKH.setPreferredSize(new Dimension(160, 34));
        btnThemKH.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnThemKH.setEnabled(UserSession.getInstance().getVaiTro() == VaiTro.TIEP_NHAN_VIEN);
        btnThemKH.addActionListener(e -> moModalThemKhachHang());

        btnSuaKH = new RoundedButton("✏  Sửa thông tin", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnSuaKH.setFont(f(Font.PLAIN, 12));
        btnSuaKH.setForeground(C_TEXT_MAIN);
        btnSuaKH.setPreferredSize(new Dimension(130, 34));
        btnSuaKH.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSuaKH.setEnabled(false);
        btnSuaKH.addActionListener(e -> moModalSuaKhachHang());

        btnRowKH.add(btnThemKH);
        btnRowKH.add(btnSuaKH);
        card.add(btnRowKH, BorderLayout.SOUTH);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CỘT PHẢI: DANH SÁCH XE CỦA KHÁCH HÀNG
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildPanelXe() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(C_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(14, 16, 14, 16)));

        // Header cột xe
        JPanel pnlHead = new JPanel(new BorderLayout());
        pnlHead.setOpaque(false);

        lblXeHeaderTitle = new JLabel("Danh sách xe của khách hàng");
        lblXeHeaderTitle.setFont(f(Font.BOLD, 15));
        lblXeHeaderTitle.setForeground(C_TEXT_HEAD);
        pnlHead.add(lblXeHeaderTitle, BorderLayout.WEST);

        lblSoLuongXe = new JLabel("0 xe");
        lblSoLuongXe.setFont(f(Font.PLAIN, 12));
        lblSoLuongXe.setForeground(C_TEXT_SUBTLE);
        pnlHead.add(lblSoLuongXe, BorderLayout.EAST);
        card.add(pnlHead, BorderLayout.NORTH);

        // Bảng xe (bổ sung cột Số Km ODO)
        modelXe = new DefaultTableModel(new String[]{"Biển số xe", "Hãng xe", "Dòng xe / Model", "Màu sắc", "Năm SX", "Số Km (ODO)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblXe = new JTable(modelXe);
        tblXe.setFont(f(Font.PLAIN, 13));
        tblXe.setRowHeight(32);
        tblXe.setShowGrid(false);
        tblXe.getTableHeader().setFont(f(Font.BOLD, 12));
        tblXe.getTableHeader().setBackground(new Color(0xF8FAFC));
        tblXe.setSelectionBackground(C_PRIMARY_BG);
        tblXe.setSelectionForeground(C_TEXT_HEAD);
        tblXe.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Canh giữa Biển số, Năm SX, Số Km
        DefaultTableCellRenderer centerRend = new DefaultTableCellRenderer();
        centerRend.setHorizontalAlignment(SwingConstants.CENTER);
        tblXe.getColumnModel().getColumn(0).setCellRenderer(centerRend);
        tblXe.getColumnModel().getColumn(4).setCellRenderer(centerRend);

        DefaultTableCellRenderer rightRend = new DefaultTableCellRenderer();
        rightRend.setHorizontalAlignment(SwingConstants.RIGHT);
        tblXe.getColumnModel().getColumn(5).setCellRenderer(rightRend);

        // FIX BUG: GẮN LISTSELECTIONLISTENER CHO TBLXE ĐỂ KÍCH HOẠT NÚT SỬA VÀ TẠO PHIẾU
        tblXe.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onChonXe();
            }
        });

        JScrollPane scrollXe = new JScrollPane(tblXe);
        scrollXe.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        card.add(scrollXe, BorderLayout.CENTER);

        // Footer các nút thao tác Xe
        JPanel btnRowXe = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRowXe.setOpaque(false);

        btnThemXe = new RoundedButton("+ Thêm xe", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
        btnThemXe.setForeground(C_WHITE);
        btnThemXe.setFont(f(Font.BOLD, 12));
        btnThemXe.setPreferredSize(new Dimension(120, 34));
        btnThemXe.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnThemXe.setEnabled(UserSession.getInstance().getVaiTro() == VaiTro.TIEP_NHAN_VIEN);
        btnThemXe.addActionListener(e -> moModalThemXe());

        btnSuaXe = new RoundedButton("✏  Sửa xe", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnSuaXe.setFont(f(Font.PLAIN, 12));
        btnSuaXe.setForeground(C_TEXT_MAIN);
        btnSuaXe.setPreferredSize(new Dimension(100, 34));
        btnSuaXe.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSuaXe.setEnabled(false);
        btnSuaXe.addActionListener(e -> moModalSuaXe());

        // Nút Tạo phiếu tiếp nhận (F2) màu xanh lá hoặc xanh dương nổi bật
        btnTaoPhieuTiepNhan = new RoundedButton("🛠  Tạo phiếu tiếp nhận (F2)", 8, new Color(0x059669), new Color(0x047857), new Color(0x059669));
        btnTaoPhieuTiepNhan.setForeground(C_WHITE);
        btnTaoPhieuTiepNhan.setFont(f(Font.BOLD, 12));
        btnTaoPhieuTiepNhan.setPreferredSize(new Dimension(190, 34));
        btnTaoPhieuTiepNhan.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnTaoPhieuTiepNhan.setVisible(UserSession.getInstance().getVaiTro() == VaiTro.TIEP_NHAN_VIEN);
        btnTaoPhieuTiepNhan.setEnabled(false);
        btnTaoPhieuTiepNhan.addActionListener(e -> chuyenSangLapPhieuTiepNhan());

        btnRowXe.add(btnThemXe);
        btnRowXe.add(btnSuaXe);
        btnRowXe.add(btnTaoPhieuTiepNhan);
        card.add(btnRowXe, BorderLayout.SOUTH);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SỰ KIỆN CHỌN KHÁCH HÀNG & CHỌN XE
    // ─────────────────────────────────────────────────────────────────────────
    private void onChonKhachHang() {
        int row = tblKhachHang.getSelectedRow();
        if (row < 0 || row >= dsKhachHang.size()) {
            khachHangDangChon = null;
            btnSuaKH.setEnabled(false);
            btnThemXe.setEnabled(false);
            btnSuaXe.setEnabled(false);
            btnTaoPhieuTiepNhan.setEnabled(false);
            modelXe.setRowCount(0);
            lblXeHeaderTitle.setText("Danh sách xe của khách hàng");
            lblSoLuongXe.setText("0 xe");
            return;
        }

        khachHangDangChon = dsKhachHang.get(row);
        boolean duocGhi = UserSession.getInstance().getVaiTro() == VaiTro.TIEP_NHAN_VIEN;
        btnSuaKH.setEnabled(duocGhi);
        btnThemXe.setEnabled(duocGhi);
        lblXeHeaderTitle.setText("Xe của: " + khachHangDangChon.getHoTen() + " (" + khachHangDangChon.getSoDienThoai() + ")");

        // Nạp danh sách xe của khách hàng đang chọn qua SwingWorker
        new SwingWorker<List<Xe>, Void>() {
            @Override
            protected List<Xe> doInBackground() {
                return controller.layDanhSachXeTheoKhachHang(khachHangDangChon.getMaKH());
            }

            @Override
            protected void done() {
                try {
                    dsXe = get();
                    modelXe.setRowCount(0);
                    for (Xe xe : dsXe) {
                        modelXe.addRow(new Object[]{
                                xe.getBienSoXe(),
                                xe.getHangXe(),
                                xe.getModel(),
                                xe.getMauSac() != null ? xe.getMauSac() : "",
                                xe.getNamSanXuat() > 0 ? xe.getNamSanXuat() : "",
                                INT_FMT.format(xe.getSoKmHienTai()) + " km"
                        });
                    }
                    lblSoLuongXe.setText(dsXe.size() + " xe");
                    xeDangChon = null;
                    btnSuaXe.setEnabled(false);
                    btnTaoPhieuTiepNhan.setEnabled(false);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(QuanLyKhachHangVaXeUI.this,
                            "Lỗi tải danh sách xe: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void onChonXe() {
        int row = tblXe.getSelectedRow();
        if (row >= 0 && row < dsXe.size()) {
            xeDangChon = dsXe.get(row);
                    boolean duocGhi = UserSession.getInstance().getVaiTro() == VaiTro.TIEP_NHAN_VIEN;
                    btnSuaXe.setEnabled(duocGhi);
                    btnTaoPhieuTiepNhan.setEnabled(duocGhi);
        } else {
            xeDangChon = null;
            btnSuaXe.setEnabled(false);
            btnTaoPhieuTiepNhan.setEnabled(false);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHUYỂN SANG LẬP PHIẾU TIẾP NHẬN (F2)
    // ─────────────────────────────────────────────────────────────────────────
    private void dangKyPhimTatF2() {
        KeyStroke f2Key = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(f2Key, "actionF2");
        getActionMap().put("actionF2", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (btnTaoPhieuTiepNhan.isEnabled()) {
                    chuyenSangLapPhieuTiepNhan();
                }
            }
        });
    }

    private void chuyenSangLapPhieuTiepNhan() {
        if (UserSession.getInstance().getVaiTro() != VaiTro.TIEP_NHAN_VIEN) return;
        if (khachHangDangChon == null || xeDangChon == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một xe trong danh sách trước khi tạo phiếu tiếp nhận.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Window win = SwingUtilities.getWindowAncestor(this);
        if (win instanceof MainFrame mf) {
            mf.moLapPhieuTiepNhanVoiXe(khachHangDangChon.getMaKH(), xeDangChon.getBienSoXe());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TẢI DANH SÁCH KHÁCH HÀNG
    // ─────────────────────────────────────────────────────────────────────────
    private void taiDanhSachKhachHang(String tuKhoa) {
        new SwingWorker<List<KhachHang>, Void>() {
            @Override
            protected List<KhachHang> doInBackground() {
                if (tuKhoa == null || tuKhoa.isBlank())
                    return controller.layDanhSachKhachHang();
                return controller.timKiemKhachHang(tuKhoa);
            }

            @Override
            protected void done() {
                try {
                    dsKhachHang = get();
                    modelKH.setRowCount(0);
                    for (KhachHang kh : dsKhachHang) {
                        modelKH.addRow(new Object[]{
                                kh.getMaKH(),
                                kh.getHoTen(),
                                kh.getSoDienThoai(),
                                kh.isThanhVienThanThiet() ? "★ Khách hàng thân thiết" : "--"
                        });
                    }
                    lblSoLuongKH.setText(dsKhachHang.size() + " khách hàng");

                    // Reset bảng xe
                    modelXe.setRowCount(0);
                    dsXe = List.of();
                    khachHangDangChon = null;
                    xeDangChon = null;
                    lblXeHeaderTitle.setText("Danh sách xe của khách hàng");
                    lblSoLuongXe.setText("0 xe");
                    btnThemXe.setEnabled(false);
                    btnSuaXe.setEnabled(false);
                    btnSuaKH.setEnabled(false);
                    btnTaoPhieuTiepNhan.setEnabled(false);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(QuanLyKhachHangVaXeUI.this,
                            "Lỗi tải danh sách khách hàng: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODAL FORM: THÊM / SỬA KHÁCH HÀNG
    // ─────────────────────────────────────────────────────────────────────────
    private void moModalThemKhachHang() {
        Window parent = SwingUtilities.getWindowAncestor(this);
        KhachHangModalDialog dlg = new KhachHangModalDialog(parent, null);
        dlg.setVisible(true);
        if (dlg.isThanhCong()) {
            taiDanhSachKhachHang(null);
        }
    }

    private void moModalSuaKhachHang() {
        if (khachHangDangChon == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn khách hàng cần chỉnh sửa.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Window parent = SwingUtilities.getWindowAncestor(this);
        KhachHangModalDialog dlg = new KhachHangModalDialog(parent, khachHangDangChon);
        dlg.setVisible(true);
        if (dlg.isThanhCong()) {
            taiDanhSachKhachHang(null);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODAL FORM: THÊM / SỬA XE (Bám sát thiết kế Figma)
    // ─────────────────────────────────────────────────────────────────────────
    private void moModalThemXe() {
        if (khachHangDangChon == null) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một khách hàng từ danh sách bên trái trước khi thêm xe.",
                    "Yêu cầu chọn khách hàng", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Window parent = SwingUtilities.getWindowAncestor(this);
        XeModalDialog dlg = new XeModalDialog(parent, khachHangDangChon, null);
        dlg.setVisible(true);
        if (dlg.isThanhCong()) {
            onChonKhachHang(); // Reload xe của khách hàng
        }
    }

    private void moModalSuaXe() {
        if (khachHangDangChon == null || xeDangChon == null) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một xe trong bảng cần chỉnh sửa.",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Window parent = SwingUtilities.getWindowAncestor(this);
        XeModalDialog dlg = new XeModalDialog(parent, khachHangDangChon, xeDangChon);
        dlg.setVisible(true);
        if (dlg.isThanhCong()) {
            onChonKhachHang(); // Reload xe của khách hàng
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DIALOG MODAL: THÔNG TIN KHÁCH HÀNG (THÊM / SỬA)
    // ─────────────────────────────────────────────────────────────────────────
    private class KhachHangModalDialog extends JDialog {
        private final KhachHang khachHangCu;
        private boolean thanhCong = false;

        private JTextField txtMaKH;
        private JTextField txtHoTen;
        private JTextField txtSdt;
        private JTextField txtCCCD;
        private JTextField txtGhiChu;
        private JLabel lblErr;
        private JButton btnLuu;

        public KhachHangModalDialog(Window parent, KhachHang kh) {
            super(parent, kh == null ? "Thêm khách hàng mới" : "Cập nhật thông tin khách hàng", ModalityType.APPLICATION_MODAL);
            this.khachHangCu = kh;

            setLayout(new BorderLayout());
            getContentPane().setBackground(C_WHITE);
            setResizable(false);

            add(buildHeader(), BorderLayout.NORTH);
            add(buildForm(), BorderLayout.CENTER);
            add(buildFooter(), BorderLayout.SOUTH);

            pack();
            setLocationRelativeTo(parent);
        }

        public boolean isThanhCong() { return thanhCong; }

        private JPanel buildHeader() {
            JPanel pnl = new JPanel(new BorderLayout());
            pnl.setBackground(C_WHITE);
            pnl.setBorder(new EmptyBorder(18, 24, 14, 24));

            JLabel lblTitle = new JLabel(khachHangCu == null ? "Thêm khách hàng mới" : "Cập nhật thông tin khách hàng");
            lblTitle.setFont(f(Font.BOLD, 18));
            lblTitle.setForeground(C_TEXT_HEAD);

            pnl.add(lblTitle, BorderLayout.WEST);
            return pnl;
        }

        private JPanel buildForm() {
            JPanel pnl = new JPanel();
            pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
            pnl.setBackground(C_WHITE);
            pnl.setBorder(new EmptyBorder(0, 24, 16, 24));

            txtMaKH = new JTextField();
            txtMaKH.setFont(f(Font.PLAIN, 13));
            txtMaKH.setEditable(false);
            txtMaKH.setBackground(new Color(0xF8FAFC));
            txtMaKH.setPreferredSize(new Dimension(460, 34));
            txtMaKH.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(C_BORDER, 1), new EmptyBorder(4, 8, 4, 8)));
            txtMaKH.setText(khachHangCu != null ? khachHangCu.getMaKH() : "Chưa được cấp mã (Hệ thống tự sinh)");

            txtHoTen = taoField(khachHangCu != null ? khachHangCu.getHoTen() : "", "Nhập họ tên khách hàng");
            txtSdt = taoField(khachHangCu != null ? khachHangCu.getSoDienThoai() : "", "Số điện thoại 10 chữ số (0xxx)");
            txtCCCD = taoField(khachHangCu != null && khachHangCu.getCCCD() != null ? khachHangCu.getCCCD() : "", "Căn cước công dân (12 chữ số)");
            txtGhiChu = taoField(khachHangCu != null && khachHangCu.getGhiChu() != null ? khachHangCu.getGhiChu() : "", "Ghi chú thêm về khách hàng");

            pnl.add(wrapField("Mã khách hàng", txtMaKH));
            pnl.add(Box.createVerticalStrut(10));

            // Họ tên + SĐT
            JPanel row1 = new JPanel(new GridLayout(1, 2, 12, 0));
            row1.setOpaque(false);
            row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            row1.setAlignmentX(0.0f);
            row1.add(wrapField("Họ và tên *", txtHoTen));
            row1.add(wrapField("Số điện thoại *", txtSdt));
            pnl.add(row1);
            pnl.add(Box.createVerticalStrut(10));

            // CCCD + Ghi chú
            JPanel row2 = new JPanel(new GridLayout(1, 2, 12, 0));
            row2.setOpaque(false);
            row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            row2.setAlignmentX(0.0f);
            row2.add(wrapField("CCCD / Căn cước", txtCCCD));
            row2.add(wrapField("Ghi chú", txtGhiChu));
            pnl.add(row2);
            pnl.add(Box.createVerticalStrut(10));

            lblErr = new JLabel(" ");
            lblErr.setFont(f(Font.PLAIN, 12));
            lblErr.setForeground(C_DANGER);
            lblErr.setAlignmentX(0.0f);
            pnl.add(lblErr);

            return pnl;
        }

        private JPanel buildFooter() {
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
            pnl.setBackground(new Color(0xF8FAFC));
            pnl.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));

            JButton btnHuy = new RoundedButton("Hủy", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
            btnHuy.setFont(f(Font.PLAIN, 12));
            btnHuy.setForeground(C_TEXT_MAIN);
            btnHuy.setPreferredSize(new Dimension(85, 34));
            btnHuy.addActionListener(e -> dispose());
            pnl.add(btnHuy);

            btnLuu = new RoundedButton("Lưu khách hàng", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
            btnLuu.setFont(f(Font.BOLD, 12));
            btnLuu.setForeground(C_WHITE);
            btnLuu.setPreferredSize(new Dimension(140, 34));
            btnLuu.addActionListener(e -> xuLyLuuKhachHang());
            pnl.add(btnLuu);

            return pnl;
        }

        private JTextField taoField(String text, String placeholder) {
            JTextField tf = new JTextField(text);
            tf.setFont(f(Font.PLAIN, 13));
            tf.putClientProperty("JTextField.placeholderText", placeholder);
            tf.setPreferredSize(new Dimension(220, 34));
            tf.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(C_BORDER, 1), new EmptyBorder(4, 8, 4, 8)));
            return tf;
        }

        private void xuLyLuuKhachHang() {
            String hoTen = txtHoTen.getText().trim();
            String sdt = txtSdt.getText().trim();
            String cccd = txtCCCD.getText().trim();
            String ghiChu = txtGhiChu.getText().trim();

            if (hoTen.isEmpty()) {
                lblErr.setText("Vui lòng nhập họ tên khách hàng.");
                txtHoTen.requestFocusInWindow();
                return;
            }
            if (!sdt.matches("0\\d{9}")) {
                lblErr.setText("Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0.");
                txtSdt.requestFocusInWindow();
                return;
            }
            if (!cccd.isEmpty() && !cccd.matches("\\d{12}")) {
                lblErr.setText("CCCD phải gồm đúng 12 chữ số.");
                txtCCCD.requestFocusInWindow();
                return;
            }

            KhachHang kh = khachHangCu != null ? khachHangCu : new KhachHang();
            kh.setHoTenRaw(hoTen);
            kh.setSoDienThoaiRaw(sdt);
            kh.setCCCDRaw(cccd.isEmpty() ? null : cccd);
            kh.setGhiChuRaw(ghiChu.isEmpty() ? null : ghiChu);
            if (khachHangCu != null) {
                kh.setThanhVienThanThietRaw(khachHangCu.isThanhVienThanThiet());
                kh.setDiemTichLuyRaw(khachHangCu.getDiemTichLuy());
            } else {
                kh.setThanhVienThanThietRaw(false);
                kh.setDiemTichLuyRaw(0.0);
            }

            btnLuu.setEnabled(false);
            btnLuu.setText("Đang lưu...");

            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() {
                    return controller.luuKhachHang(kh);
                }

                @Override
                protected void done() {
                    btnLuu.setEnabled(true);
                    btnLuu.setText("Lưu khách hàng");
                    try {
                        get();
                        thanhCong = true;
                        dispose();
                    } catch (Exception ex) {
                        lblErr.setText(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    }
                }
            }.execute();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DIALOG MODAL: THÔNG TIN XE (THÊM / SỬA) - CHUẨN FIGMA
    // ─────────────────────────────────────────────────────────────────────────
    private class XeModalDialog extends JDialog {
        private final KhachHang chuXe;
        private final Xe xeCu;
        private boolean thanhCong = false;

        private JTextField txtBienSo;
        private JTextField txtHangXe;
        private JTextField txtModel;
        private JTextField txtMauSac;
        private JTextField txtNamSX;
        private JTextField txtSoKm;
        private JLabel lblErr;
        private JButton btnLuu;

        public XeModalDialog(Window parent, KhachHang chuXe, Xe xe) {
            super(parent, xe == null ? "Thêm xe mới" : "Cập nhật thông tin xe", ModalityType.APPLICATION_MODAL);
            this.chuXe = chuXe;
            this.xeCu = xe;

            setLayout(new BorderLayout());
            getContentPane().setBackground(C_WHITE);
            setResizable(false);

            add(buildHeader(), BorderLayout.NORTH);
            add(buildForm(), BorderLayout.CENTER);
            add(buildFooter(), BorderLayout.SOUTH);

            pack();
            setLocationRelativeTo(parent);
        }

        public boolean isThanhCong() { return thanhCong; }

        private JPanel buildHeader() {
            JPanel pnl = new JPanel(new BorderLayout());
            pnl.setBackground(C_WHITE);
            pnl.setBorder(new EmptyBorder(18, 24, 14, 24));

            JLabel lblTitle = new JLabel(xeCu == null ? "Thêm xe mới" : "Cập nhật thông tin xe");
            lblTitle.setFont(f(Font.BOLD, 18));
            lblTitle.setForeground(C_TEXT_HEAD);

            pnl.add(lblTitle, BorderLayout.WEST);
            return pnl;
        }

        private JPanel buildForm() {
            JPanel pnl = new JPanel();
            pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
            pnl.setBackground(C_WHITE);
            pnl.setBorder(new EmptyBorder(0, 24, 16, 24));

            // THẺ KHÁCH HÀNG SỞ HỮU XE (Theo đúng Figma)
            JPanel cardOwner = new JPanel(new BorderLayout(8, 4));
            cardOwner.setBackground(new Color(0xF8FAFC));
            cardOwner.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER, 1),
                    new EmptyBorder(10, 14, 10, 14)));
            cardOwner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
            cardOwner.setAlignmentX(0.0f);

            JLabel lblOwnerIcon = new JLabel("👤  Khách hàng sở hữu xe");
            lblOwnerIcon.setFont(f(Font.BOLD, 12));
            lblOwnerIcon.setForeground(C_PRIMARY);
            cardOwner.add(lblOwnerIcon, BorderLayout.NORTH);

            JLabel lblOwnerName = new JLabel(chuXe.getHoTen());
            lblOwnerName.setFont(f(Font.BOLD, 14));
            lblOwnerName.setForeground(C_TEXT_HEAD);

            JLabel lblOwnerInfo = new JLabel("Mã KH: " + chuXe.getMaKH() + " · SĐT: " + chuXe.getSoDienThoai());
            lblOwnerInfo.setFont(f(Font.PLAIN, 12));
            lblOwnerInfo.setForeground(C_TEXT_SUBTLE);

            JPanel ownerCenter = new JPanel(new BorderLayout(0, 2));
            ownerCenter.setOpaque(false);
            ownerCenter.add(lblOwnerName, BorderLayout.NORTH);
            ownerCenter.add(lblOwnerInfo, BorderLayout.SOUTH);
            cardOwner.add(ownerCenter, BorderLayout.CENTER);

            pnl.add(cardOwner);
            pnl.add(Box.createVerticalStrut(14));

            // Hàng 1: Biển số xe * | Hãng xe *
            txtBienSo = taoField(xeCu != null ? xeCu.getBienSoXe() : "", "VD: 51H-12345");
            if (xeCu != null) {
                txtBienSo.setEditable(false);
                txtBienSo.setBackground(new Color(0xF8FAFC));
            }
            txtHangXe = taoField(xeCu != null ? xeCu.getHangXe() : "", "VD: Toyota, Mazda");

            JPanel row1 = new JPanel(new GridLayout(1, 2, 12, 0));
            row1.setOpaque(false);
            row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            row1.setAlignmentX(0.0f);
            row1.add(wrapField("Biển số xe *", txtBienSo));
            row1.add(wrapField("Hãng xe *", txtHangXe));
            pnl.add(row1);
            pnl.add(Box.createVerticalStrut(10));

            // Hàng 2: Dòng xe / Model * | Màu sắc
            txtModel = taoField(xeCu != null ? xeCu.getModel() : "", "VD: Camry, CX-5");
            txtMauSac = taoField(xeCu != null && xeCu.getMauSac() != null ? xeCu.getMauSac() : "", "VD: Đen, Trắng");

            JPanel row2 = new JPanel(new GridLayout(1, 2, 12, 0));
            row2.setOpaque(false);
            row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            row2.setAlignmentX(0.0f);
            row2.add(wrapField("Dòng xe / Model *", txtModel));
            row2.add(wrapField("Màu sắc", txtMauSac));
            pnl.add(row2);
            pnl.add(Box.createVerticalStrut(10));

            // Hàng 3: Năm sản xuất | Số km hiện tại
            txtNamSX = taoField(xeCu != null && xeCu.getNamSanXuat() > 0 ? String.valueOf(xeCu.getNamSanXuat()) : "", "VD: 2022");
            txtSoKm = taoField(xeCu != null ? String.valueOf(xeCu.getSoKmHienTai()) : "0", "0");

            JPanel row3 = new JPanel(new GridLayout(1, 2, 12, 0));
            row3.setOpaque(false);
            row3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            row3.setAlignmentX(0.0f);
            row3.add(wrapField("Năm sản xuất", txtNamSX));
            row3.add(wrapField("Số km hiện tại", txtSoKm));
            pnl.add(row3);
            pnl.add(Box.createVerticalStrut(8));

            lblErr = new JLabel(" ");
            lblErr.setFont(f(Font.PLAIN, 12));
            lblErr.setForeground(C_DANGER);
            lblErr.setAlignmentX(0.0f);
            pnl.add(lblErr);

            return pnl;
        }

        private JPanel buildFooter() {
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
            pnl.setBackground(new Color(0xF8FAFC));
            pnl.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));

            JButton btnHuy = new RoundedButton("Hủy", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
            btnHuy.setFont(f(Font.PLAIN, 12));
            btnHuy.setForeground(C_TEXT_MAIN);
            btnHuy.setPreferredSize(new Dimension(85, 34));
            btnHuy.addActionListener(e -> dispose());
            pnl.add(btnHuy);

            btnLuu = new RoundedButton("Lưu xe", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
            btnLuu.setFont(f(Font.BOLD, 12));
            btnLuu.setForeground(C_WHITE);
            btnLuu.setPreferredSize(new Dimension(110, 34));
            btnLuu.addActionListener(e -> xuLyLuuXe());
            pnl.add(btnLuu);

            return pnl;
        }

        private JTextField taoField(String text, String placeholder) {
            JTextField tf = new JTextField(text);
            tf.setFont(f(Font.PLAIN, 13));
            tf.putClientProperty("JTextField.placeholderText", placeholder);
            tf.setPreferredSize(new Dimension(220, 34));
            tf.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(C_BORDER, 1), new EmptyBorder(4, 8, 4, 8)));
            return tf;
        }

        private void xuLyLuuXe() {
            String bienSo = txtBienSo.getText().trim().toUpperCase();
            String hangXe = txtHangXe.getText().trim();
            String model = txtModel.getText().trim();
            String mauSac = txtMauSac.getText().trim();

            if (bienSo.length() < 6 || bienSo.length() > 15) {
                lblErr.setText("Biển số xe phải từ 6 đến 15 ký tự.");
                txtBienSo.requestFocusInWindow();
                return;
            }
            if (hangXe.isEmpty()) {
                lblErr.setText("Vui lòng nhập hãng sản xuất xe.");
                txtHangXe.requestFocusInWindow();
                return;
            }
            if (model.isEmpty()) {
                lblErr.setText("Vui lòng nhập dòng xe / model.");
                txtModel.requestFocusInWindow();
                return;
            }

            int namSX = 0;
            if (!txtNamSX.getText().isBlank()) {
                try { namSX = Integer.parseInt(txtNamSX.getText().trim()); }
                catch (NumberFormatException ex) { lblErr.setText("Năm sản xuất không hợp lệ."); return; }
            }

            int soKm = 0;
            if (!txtSoKm.getText().isBlank()) {
                try { soKm = Integer.parseInt(txtSoKm.getText().trim()); }
                catch (NumberFormatException ex) { lblErr.setText("Số km không hợp lệ."); return; }
            }

            Xe xe = xeCu != null ? xeCu : new Xe();
            xe.setBienSoXeRaw(bienSo);
            xe.setHangXeRaw(hangXe);
            xe.setModelRaw(model);
            xe.setMauSacRaw(mauSac.isEmpty() ? null : mauSac);
            xe.setNamSanXuatRaw(namSX);
            xe.setSoKmHienTaiRaw(soKm);
            xe.setChuXeRaw(chuXe);

            btnLuu.setEnabled(false);
            btnLuu.setText("Đang lưu...");

            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return controller.luuXe(xe);
                }

                @Override
                protected void done() {
                    btnLuu.setEnabled(true);
                    btnLuu.setText("Lưu xe");
                    try {
                        boolean ok = get();
                        if (ok) {
                            thanhCong = true;
                            dispose();
                        } else {
                            lblErr.setText("Lưu xe thất bại.");
                        }
                    } catch (Exception ex) {
                        lblErr.setText(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    }
                }
            }.execute();
        }
    }

    private static JPanel wrapField(String labelText, JComponent input) {
        JPanel pnl = new JPanel(new BorderLayout(0, 4));
        pnl.setOpaque(false);
        pnl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        pnl.setAlignmentX(0.0f);

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(f(Font.PLAIN, 12));
        lbl.setForeground(C_TEXT_MAIN);
        pnl.add(lbl, BorderLayout.NORTH);
        pnl.add(input, BorderLayout.CENTER);

        return pnl;
    }
}
