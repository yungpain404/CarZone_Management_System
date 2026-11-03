package ui;

import controller.KhachHangVaXeController;
import entity.KhachHang;
import entity.Xe;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

import static ui.MainFrame.*;

/**
 * Màn hình Quản lý hồ sơ Khách hàng và Xe (UC016 - Tiếp nhận viên).
 * Bám sát Figma: Quan_ly_ho_so_Khach_hang_va_xe.jpg
 */
public class QuanLyKhachHangVaXeUI extends JPanel {

    private final KhachHangVaXeController controller = new KhachHangVaXeController();

    // ── Tìm kiếm ─────────────────────────────────────────────────────────────
    private JTextField txtSearch;

    // ── Bảng khách hàng ───────────────────────────────────────────────────────
    private DefaultTableModel modelKH;
    private JTable tblKhachHang;
    private List<KhachHang> dsKhachHang = List.of();

    // ── Bảng xe ───────────────────────────────────────────────────────────────
    private DefaultTableModel modelXe;
    private JTable tblXe;
    private List<Xe> dsXe = List.of();

    // ── Form Khách hàng ───────────────────────────────────────────────────────
    private JTextField txtMaKH;
    private JTextField txtHoTen;
    private JTextField txtSdt;
    private JTextField txtCCCD;
    private JTextField txtGhiChuKH;
    private JButton btnLuuKH;
    private JButton btnHuyKH;
    private JButton btnThemKH;
    private JButton btnSuaKH;
    private KhachHang khachHangDangChon = null;

    // ── Form Xe ───────────────────────────────────────────────────────────────
    private JTextField txtBienSo;
    private JTextField txtHangXe;
    private JTextField txtModel;
    private JTextField txtMauSac;
    private JTextField txtNamSX;
    private JTextField txtSoKm;
    private JButton btnLuuXe;
    private JButton btnHuyXe;
    private JButton btnThemXe;
    private JButton btnSuaXe;

    // ── Status bar ────────────────────────────────────────────────────────────
    private JLabel lblStatus;

    public QuanLyKhachHangVaXeUI() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);

        // Khởi tạo tất cả input fields trước để tránh NullPointer hoặc ClassCast
        khoiTaoCacTruongNhapLieu();

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);

        taiDanhSachKhachHang(null);
    }

    private void khoiTaoCacTruongNhapLieu() {
        txtMaKH = taoTextField(false, "Chưa được cấp mã (Hệ thống tự sinh)");
        txtHoTen = taoTextField(true, "Nhập họ tên khách hàng");
        txtSdt = taoTextField(true, "Số điện thoại 10 chữ số (0xxx)");
        txtCCCD = taoTextField(true, "Không bắt buộc");
        txtGhiChuKH = taoTextField(true, "Ghi chú về khách hàng");

        txtBienSo = taoTextField(true, "VD: 51H-12345");
        txtHangXe = taoTextField(true, "VD: Toyota, Mazda");
        txtModel = taoTextField(true, "VD: Camry, CX-5");
        txtMauSac = taoTextField(true, "VD: Đen, Trắng, Đỏ");
        txtNamSX = taoTextField(true, "VD: 2022");
        txtSoKm = taoTextField(true, "VD: 15000");

        lblStatus = new JLabel(" ");
        lblStatus.setFont(f(Font.PLAIN, 12));
        lblStatus.setForeground(C_DANGER);
    }

    private JTextField taoTextField(boolean editable, String placeholder) {
        JTextField tf = new JTextField();
        tf.setFont(f(Font.PLAIN, 13));
        tf.setEditable(editable);
        if (placeholder != null) {
            tf.putClientProperty("JTextField.placeholderText", placeholder);
        }
        if (!editable) {
            tf.setBackground(new Color(0xF8FAFC));
            tf.setForeground(C_TEXT_SUBTLE);
        }
        tf.setPreferredSize(new Dimension(0, 34));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(4, 8, 4, 8)));
        return tf;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TOP BAR: Tiêu đề + Thanh tìm kiếm
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel pnl = new JPanel(new BorderLayout(12, 0));
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(16, 24, 12, 24));

        // Tiêu đề
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JLabel lblTitle = new JLabel("Quản lý hồ sơ khách hàng và xe");
        lblTitle.setFont(f(Font.BOLD, 20));
        lblTitle.setForeground(C_TEXT_HEAD);
        JLabel lblSub = new JLabel("Thêm mới, chỉnh sửa thông tin khách hàng và xe ô tô");
        lblSub.setFont(f(Font.PLAIN, 12));
        lblSub.setForeground(C_TEXT_SUBTLE);
        titleRow.add(lblTitle, BorderLayout.WEST);
        titleRow.add(lblSub, BorderLayout.EAST);
        pnl.add(titleRow, BorderLayout.NORTH);

        // Thanh tìm kiếm + nút
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        searchRow.setOpaque(false);

        txtSearch = new JTextField(30);
        txtSearch.setFont(f(Font.PLAIN, 13));
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm theo mã KH, họ tên, SĐT, biển số xe...");
        txtSearch.setPreferredSize(new Dimension(340, 36));
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(4, 10, 4, 10)));
        searchRow.add(txtSearch);

        JButton btnTimKiem = new RoundedButton("🔍  Tìm kiếm", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
        btnTimKiem.setForeground(C_WHITE);
        btnTimKiem.setFont(f(Font.BOLD, 13));
        btnTimKiem.setPreferredSize(new Dimension(120, 36));
        btnTimKiem.addActionListener(e -> taiDanhSachKhachHang(txtSearch.getText().trim()));
        searchRow.add(btnTimKiem);

        JButton btnLamMoi = new RoundedButton("↺  Làm mới", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnLamMoi.setFont(f(Font.PLAIN, 13));
        btnLamMoi.setForeground(C_TEXT_MAIN);
        btnLamMoi.setPreferredSize(new Dimension(110, 36));
        btnLamMoi.addActionListener(e -> {
            txtSearch.setText("");
            taiDanhSachKhachHang(null);
        });
        searchRow.add(btnLamMoi);

        txtSearch.addActionListener(e -> taiDanhSachKhachHang(txtSearch.getText().trim()));

        pnl.add(searchRow, BorderLayout.SOUTH);
        return pnl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAIN CONTENT: 2 cột (KH + Xe)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildMainContent() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(0, 24, 16, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 0, 12);

        // Cột trái: Danh sách KH + Form KH
        gbc.gridx = 0;
        gbc.weightx = 0.5;
        pnl.add(buildPanelKhachHang(), gbc);

        // Cột phải: Danh sách xe + Form xe
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        gbc.insets = new Insets(0, 0, 0, 0);
        pnl.add(buildPanelXe(), gbc);

        return pnl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PANEL KHÁCH HÀNG (cột trái)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildPanelKhachHang() {
        JPanel pnl = new JPanel(new BorderLayout(0, 10));
        pnl.setOpaque(false);

        // --- Card Danh sách KH ---
        JPanel cardKH = new JPanel(new BorderLayout(0, 8));
        cardKH.setBackground(C_WHITE);
        cardKH.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(12, 14, 10, 14)));

        JLabel lblTitleKH = new JLabel("Danh sách khách hàng");
        lblTitleKH.setFont(f(Font.BOLD, 14));
        lblTitleKH.setForeground(C_TEXT_HEAD);
        cardKH.add(lblTitleKH, BorderLayout.NORTH);

        modelKH = new DefaultTableModel(new String[]{"Mã KH", "Họ tên", "Số điện thoại", "Thành viên"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblKhachHang = new JTable(modelKH);
        tblKhachHang.setFont(f(Font.PLAIN, 13));
        tblKhachHang.setRowHeight(30);
        tblKhachHang.setShowGrid(false);
        tblKhachHang.getTableHeader().setFont(f(Font.BOLD, 12));
        tblKhachHang.getTableHeader().setBackground(new Color(0xF8FAFC));
        tblKhachHang.setSelectionBackground(C_PRIMARY_BG);
        tblKhachHang.setSelectionForeground(C_TEXT_HEAD);

        tblKhachHang.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onChonKhachHang();
        });

        JScrollPane scrollKH = new JScrollPane(tblKhachHang);
        scrollKH.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        scrollKH.setPreferredSize(new Dimension(0, 220));
        cardKH.add(scrollKH, BorderLayout.CENTER);

        // Hàng nút thao tác KH
        JPanel btnRowKH = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRowKH.setOpaque(false);

        btnThemKH = new RoundedButton("+ Thêm khách hàng", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
        btnThemKH.setForeground(C_WHITE);
        btnThemKH.setFont(f(Font.BOLD, 12));
        btnThemKH.setPreferredSize(new Dimension(160, 32));
        btnThemKH.addActionListener(e -> moFormThemKhachHang());

        btnSuaKH = new RoundedButton("✏  Sửa", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnSuaKH.setFont(f(Font.PLAIN, 12));
        btnSuaKH.setForeground(C_TEXT_MAIN);
        btnSuaKH.setPreferredSize(new Dimension(80, 32));
        btnSuaKH.setEnabled(false);
        btnSuaKH.addActionListener(e -> moFormSuaKhachHang());

        btnRowKH.add(btnThemKH);
        btnRowKH.add(btnSuaKH);
        cardKH.add(btnRowKH, BorderLayout.SOUTH);

        pnl.add(cardKH, BorderLayout.CENTER);

        // --- Card Form KH ---
        pnl.add(buildFormKhachHang(), BorderLayout.SOUTH);

        return pnl;
    }

    private JPanel buildFormKhachHang() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(C_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)));

        JLabel lblTitle = new JLabel("Thông tin khách hàng");
        lblTitle.setFont(f(Font.BOLD, 14));
        lblTitle.setForeground(C_TEXT_HEAD);
        lblTitle.setAlignmentX(0.0f);
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(10));

        // Mã KH
        card.add(wrapField("Mã khách hàng", txtMaKH));
        card.add(Box.createVerticalStrut(6));

        // Họ tên + SĐT
        JPanel row1 = new JPanel(new GridLayout(1, 2, 8, 0));
        row1.setOpaque(false);
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        row1.setAlignmentX(0.0f);
        row1.add(wrapField("Họ tên *", txtHoTen));
        row1.add(wrapField("Số điện thoại *", txtSdt));
        card.add(row1);
        card.add(Box.createVerticalStrut(6));

        // CCCD
        card.add(wrapField("CCCD / Căn cước", txtCCCD));
        card.add(Box.createVerticalStrut(6));

        // Ghi chú
        card.add(wrapField("Ghi chú", txtGhiChuKH));
        card.add(Box.createVerticalStrut(6));

        // Status báo lỗi
        lblStatus.setAlignmentX(0.0f);
        card.add(lblStatus);
        card.add(Box.createVerticalStrut(6));

        // Hàng nút Lưu & Hủy
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnRow.setAlignmentX(0.0f);

        btnHuyKH = new RoundedButton("Hủy", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnHuyKH.setFont(f(Font.PLAIN, 12));
        btnHuyKH.setForeground(C_TEXT_MAIN);
        btnHuyKH.setPreferredSize(new Dimension(80, 32));
        btnHuyKH.addActionListener(e -> xoaFormKhachHang());

        btnLuuKH = new RoundedButton("Lưu khách hàng", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
        btnLuuKH.setForeground(C_WHITE);
        btnLuuKH.setFont(f(Font.BOLD, 12));
        btnLuuKH.setPreferredSize(new Dimension(140, 32));
        btnLuuKH.addActionListener(e -> luuKhachHang());

        btnRow.add(btnHuyKH);
        btnRow.add(btnLuuKH);
        card.add(btnRow);

        // DocumentListener để xóa lỗi khi người dùng gõ phím
        DocumentListener clearErr = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { lblStatus.setText(" "); }
            @Override public void removeUpdate(DocumentEvent e) { lblStatus.setText(" "); }
            @Override public void changedUpdate(DocumentEvent e) {}
        };
        txtHoTen.getDocument().addDocumentListener(clearErr);
        txtSdt.getDocument().addDocumentListener(clearErr);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PANEL XE (cột phải)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildPanelXe() {
        JPanel pnl = new JPanel(new BorderLayout(0, 10));
        pnl.setOpaque(false);

        // Card Bảng xe
        JPanel cardXe = new JPanel(new BorderLayout(0, 8));
        cardXe.setBackground(C_WHITE);
        cardXe.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(12, 14, 10, 14)));

        JLabel lblXeTitle = new JLabel("Xe của khách hàng");
        lblXeTitle.setFont(f(Font.BOLD, 14));
        lblXeTitle.setForeground(C_TEXT_HEAD);
        cardXe.add(lblXeTitle, BorderLayout.NORTH);

        modelXe = new DefaultTableModel(new String[]{"Biển số", "Hãng xe", "Model", "Màu sắc", "Năm SX"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblXe = new JTable(modelXe);
        tblXe.setFont(f(Font.PLAIN, 13));
        tblXe.setRowHeight(30);
        tblXe.setShowGrid(false);
        tblXe.getTableHeader().setFont(f(Font.BOLD, 12));
        tblXe.getTableHeader().setBackground(new Color(0xF8FAFC));
        tblXe.setSelectionBackground(C_PRIMARY_BG);
        tblXe.setSelectionForeground(C_TEXT_HEAD);

        JScrollPane scrollXe = new JScrollPane(tblXe);
        scrollXe.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        scrollXe.setPreferredSize(new Dimension(0, 220));
        cardXe.add(scrollXe, BorderLayout.CENTER);

        JPanel btnRowXe = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRowXe.setOpaque(false);

        btnThemXe = new RoundedButton("+ Thêm xe", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
        btnThemXe.setForeground(C_WHITE);
        btnThemXe.setFont(f(Font.BOLD, 12));
        btnThemXe.setPreferredSize(new Dimension(120, 32));
        btnThemXe.setEnabled(false); // Chờ chọn KH
        btnThemXe.addActionListener(e -> moFormThemXe());

        btnSuaXe = new RoundedButton("✏  Sửa xe", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnSuaXe.setFont(f(Font.PLAIN, 12));
        btnSuaXe.setForeground(C_TEXT_MAIN);
        btnSuaXe.setPreferredSize(new Dimension(90, 32));
        btnSuaXe.setEnabled(false);
        btnSuaXe.addActionListener(e -> moFormSuaXe());

        btnRowXe.add(btnThemXe);
        btnRowXe.add(btnSuaXe);
        cardXe.add(btnRowXe, BorderLayout.SOUTH);

        pnl.add(cardXe, BorderLayout.CENTER);

        // Card Form xe
        pnl.add(buildFormXe(), BorderLayout.SOUTH);

        return pnl;
    }

    private JPanel buildFormXe() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(C_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)));

        JLabel lblTitle = new JLabel("Thông tin xe");
        lblTitle.setFont(f(Font.BOLD, 14));
        lblTitle.setForeground(C_TEXT_HEAD);
        lblTitle.setAlignmentX(0.0f);
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(10));

        // Biển số + Hãng xe
        JPanel row1 = new JPanel(new GridLayout(1, 2, 8, 0));
        row1.setOpaque(false);
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        row1.setAlignmentX(0.0f);
        row1.add(wrapField("Biển số xe *", txtBienSo));
        row1.add(wrapField("Hãng xe *", txtHangXe));
        card.add(row1);
        card.add(Box.createVerticalStrut(6));

        // Model + Màu sắc
        JPanel row2 = new JPanel(new GridLayout(1, 2, 8, 0));
        row2.setOpaque(false);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        row2.setAlignmentX(0.0f);
        row2.add(wrapField("Dòng xe / Model *", txtModel));
        row2.add(wrapField("Màu sắc", txtMauSac));
        card.add(row2);
        card.add(Box.createVerticalStrut(6));

        // Năm SX + Số km
        JPanel row3 = new JPanel(new GridLayout(1, 2, 8, 0));
        row3.setOpaque(false);
        row3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        row3.setAlignmentX(0.0f);
        row3.add(wrapField("Năm sản xuất", txtNamSX));
        row3.add(wrapField("Số km hiện tại", txtSoKm));
        card.add(row3);
        card.add(Box.createVerticalStrut(10));

        // Nút Lưu xe & Hủy
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnRow.setAlignmentX(0.0f);

        btnHuyXe = new RoundedButton("Hủy", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnHuyXe.setFont(f(Font.PLAIN, 12));
        btnHuyXe.setForeground(C_TEXT_MAIN);
        btnHuyXe.setPreferredSize(new Dimension(80, 32));
        btnHuyXe.addActionListener(e -> xoaFormXe());

        btnLuuXe = new RoundedButton("Lưu xe", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
        btnLuuXe.setForeground(C_WHITE);
        btnLuuXe.setFont(f(Font.BOLD, 12));
        btnLuuXe.setPreferredSize(new Dimension(100, 32));
        btnLuuXe.addActionListener(e -> luuXe());

        btnRow.add(btnHuyXe);
        btnRow.add(btnLuuXe);
        card.add(btnRow);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper: Wrap JLabel + JTextField trong một JPanel chuẩn
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel wrapField(String labelText, JTextField input) {
        JPanel pnl = new JPanel(new BorderLayout(0, 3));
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

    // ─────────────────────────────────────────────────────────────────────────
    // BUSINESS LOGIC (qua SwingWorker)
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
                                kh.isThanhVienThanThiet() ? "★ Thân thiết" : ""
                        });
                    }
                    // Reset bảng xe
                    modelXe.setRowCount(0);
                    dsXe = List.of();
                    khachHangDangChon = null;
                    btnThemXe.setEnabled(false);
                    btnSuaXe.setEnabled(false);
                    btnSuaKH.setEnabled(false);
                } catch (Exception ex) {
                    if (lblStatus != null)
                        lblStatus.setText("Lỗi tải dữ liệu: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void onChonKhachHang() {
        int row = tblKhachHang.getSelectedRow();
        if (row < 0 || row >= dsKhachHang.size()) {
            khachHangDangChon = null;
            btnThemXe.setEnabled(false);
            btnSuaKH.setEnabled(false);
            modelXe.setRowCount(0);
            return;
        }
        khachHangDangChon = dsKhachHang.get(row);
        btnThemXe.setEnabled(true);
        btnSuaKH.setEnabled(true);

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
                                xe.getNamSanXuat() > 0 ? xe.getNamSanXuat() : ""
                        });
                    }
                } catch (Exception ex) {
                    if (lblStatus != null)
                        lblStatus.setText("Lỗi tải dữ liệu xe: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void moFormThemKhachHang() {
        txtMaKH.setText("Chưa được cấp mã (Hệ thống tự sinh)");
        txtHoTen.setText("");
        txtSdt.setText("");
        txtCCCD.setText("");
        txtGhiChuKH.setText("");
        lblStatus.setText(" ");
        txtHoTen.requestFocusInWindow();
    }

    private void moFormSuaKhachHang() {
        if (khachHangDangChon == null) return;
        txtMaKH.setText(khachHangDangChon.getMaKH());
        txtHoTen.setText(khachHangDangChon.getHoTen() != null ? khachHangDangChon.getHoTen() : "");
        txtSdt.setText(khachHangDangChon.getSoDienThoai() != null ? khachHangDangChon.getSoDienThoai() : "");
        txtCCCD.setText(khachHangDangChon.getCCCD() != null ? khachHangDangChon.getCCCD() : "");
        txtGhiChuKH.setText(khachHangDangChon.getGhiChu() != null ? khachHangDangChon.getGhiChu() : "");
        lblStatus.setText(" ");
        txtHoTen.requestFocusInWindow();
    }

    private void xoaFormKhachHang() {
        txtMaKH.setText("Chưa được cấp mã (Hệ thống tự sinh)");
        txtHoTen.setText("");
        txtSdt.setText("");
        txtCCCD.setText("");
        txtGhiChuKH.setText("");
        lblStatus.setText(" ");
    }

    private void luuKhachHang() {
        String hoTen = txtHoTen.getText().trim();
        String sdt = txtSdt.getText().trim();
        String cccd = txtCCCD.getText().trim();
        String ghiChu = txtGhiChuKH.getText().trim();
        String maKH = txtMaKH.getText().trim();
        if (maKH.startsWith("Chưa được cấp mã")) maKH = "";

        if (hoTen.isEmpty()) {
            lblStatus.setText("Vui lòng nhập họ tên khách hàng.");
            txtHoTen.requestFocusInWindow();
            return;
        }
        if (!sdt.matches("0\\d{9}")) {
            lblStatus.setText("Số điện thoại phải gồm 10 chữ số, bắt đầu số 0.");
            txtSdt.requestFocusInWindow();
            return;
        }

        KhachHang kh = new KhachHang();
        if (!maKH.isEmpty()) kh.setMaKHRaw(maKH);
        kh.setHoTenRaw(hoTen);
        kh.setSoDienThoaiRaw(sdt);
        if (!cccd.isEmpty()) kh.setCCCDRaw(cccd);
        if (!ghiChu.isEmpty()) kh.setGhiChuRaw(ghiChu);

        btnLuuKH.setEnabled(false);
        btnLuuKH.setText("Đang lưu...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return controller.luuKhachHang(kh);
            }

            @Override
            protected void done() {
                btnLuuKH.setEnabled(true);
                btnLuuKH.setText("Lưu khách hàng");
                try {
                    String maKHMoi = get();
                    lblStatus.setText("✓ Đã lưu khách hàng " + maKHMoi + " thành công.");
                    lblStatus.setForeground(C_SUCCESS);
                    xoaFormKhachHang();
                    taiDanhSachKhachHang(null);
                } catch (Exception ex) {
                    lblStatus.setText(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    lblStatus.setForeground(C_DANGER);
                }
            }
        }.execute();
    }

    private void moFormThemXe() {
        if (khachHangDangChon == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn khách hàng trước khi thêm xe.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        txtBienSo.setText("");
        txtBienSo.setEditable(true);
        txtHangXe.setText("");
        txtModel.setText("");
        txtMauSac.setText("");
        txtNamSX.setText("");
        txtSoKm.setText("0");
        txtBienSo.requestFocusInWindow();
    }

    private void moFormSuaXe() {
        int row = tblXe.getSelectedRow();
        if (row < 0 || row >= dsXe.size()) return;
        Xe xe = dsXe.get(row);
        txtBienSo.setText(xe.getBienSoXe());
        txtBienSo.setEditable(false); // Biển số là khóa chính
        txtHangXe.setText(xe.getHangXe() != null ? xe.getHangXe() : "");
        txtModel.setText(xe.getModel() != null ? xe.getModel() : "");
        txtMauSac.setText(xe.getMauSac() != null ? xe.getMauSac() : "");
        txtNamSX.setText(xe.getNamSanXuat() > 0 ? String.valueOf(xe.getNamSanXuat()) : "");
        txtSoKm.setText(String.valueOf(xe.getSoKmHienTai()));
        txtHangXe.requestFocusInWindow();
    }

    private void xoaFormXe() {
        txtBienSo.setText("");
        txtBienSo.setEditable(true);
        txtHangXe.setText("");
        txtModel.setText("");
        txtMauSac.setText("");
        txtNamSX.setText("");
        txtSoKm.setText("");
    }

    private void luuXe() {
        if (khachHangDangChon == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn khách hàng trước.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String bienSo = txtBienSo.getText().trim().toUpperCase();
        String hangXe = txtHangXe.getText().trim();
        String model = txtModel.getText().trim();
        String mauSac = txtMauSac.getText().trim();

        if (bienSo.length() < 6 || bienSo.length() > 15) {
            JOptionPane.showMessageDialog(this, "Biển số xe phải từ 6 đến 15 ký tự.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            txtBienSo.requestFocusInWindow();
            return;
        }
        if (hangXe.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập hãng xe.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            txtHangXe.requestFocusInWindow();
            return;
        }
        if (model.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập dòng xe / model.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            txtModel.requestFocusInWindow();
            return;
        }

        int namSX = 0;
        try { if (!txtNamSX.getText().isBlank()) namSX = Integer.parseInt(txtNamSX.getText().trim()); }
        catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Năm sản xuất không hợp lệ.", "Lỗi", JOptionPane.WARNING_MESSAGE); return; }

        int soKm = 0;
        try { if (!txtSoKm.getText().isBlank()) soKm = Integer.parseInt(txtSoKm.getText().trim()); }
        catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Số km không hợp lệ.", "Lỗi", JOptionPane.WARNING_MESSAGE); return; }

        Xe xe = new Xe();
        xe.setBienSoXeRaw(bienSo);
        xe.setHangXeRaw(hangXe);
        xe.setModelRaw(model);
        if (!mauSac.isEmpty()) xe.setMauSacRaw(mauSac);
        if (namSX > 0) xe.setNamSanXuatRaw(namSX);
        xe.setSoKmHienTaiRaw(soKm);
        xe.setChuXeRaw(khachHangDangChon);

        btnLuuXe.setEnabled(false);
        btnLuuXe.setText("Đang lưu...");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return controller.luuXe(xe);
            }

            @Override
            protected void done() {
                btnLuuXe.setEnabled(true);
                btnLuuXe.setText("Lưu xe");
                try {
                    boolean ok = get();
                    if (ok) {
                        JOptionPane.showMessageDialog(QuanLyKhachHangVaXeUI.this,
                                "Đã lưu xe " + bienSo + " thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        xoaFormXe();
                        onChonKhachHang(); // Reload xe của KH
                    } else {
                        JOptionPane.showMessageDialog(QuanLyKhachHangVaXeUI.this, "Lưu xe thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(QuanLyKhachHangVaXeUI.this,
                            ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
