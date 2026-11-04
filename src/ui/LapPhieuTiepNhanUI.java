package ui;

import controller.LapPhieuTiepNhanController;
import entity.*;
import enums.TrangThaiPhieu;
import session.UserSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static ui.MainFrame.*;

/**
 * Màn hình Quản lý phiếu tiếp nhận – sửa chữa (UC010, UC011 - Tiếp nhận viên).
 * Danh sách phiếu tiếp nhận với bộ lọc, tìm kiếm, xem chi tiết và modal lập phiếu mới (Figma 100%).
 */
public class LapPhieuTiepNhanUI extends JPanel {

    private final LapPhieuTiepNhanController controller = new LapPhieuTiepNhanController();
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private final DecimalFormat moneyFmt = new DecimalFormat("#,### đ");

    // Dữ liệu bảng
    private List<PhieuTiepNhanSuaChua> dsPhieu = new ArrayList<>();
    private DefaultTableModel modelPhieu;
    private JTable tblPhieu;

    // Components thanh công cụ
    private JTextField txtTimKiem;
    private JComboBox<String> cboLocTrangThai;
    private JButton btnThemPhieu;
    private JButton btnXemChiTiet;
    private JButton btnLamMoi;
    private JLabel lblSoLuong;

    public LapPhieuTiepNhanUI() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);

        caiDatPhimTat();
        taiDanhSachPhieu();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. HEADER (Breadcrumb + Tiêu đề)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel pnl = new JPanel(new BorderLayout(12, 0));
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(16, 24, 12, 24));

        JPanel titleBlock = new JPanel(new BorderLayout(0, 4));
        titleBlock.setOpaque(false);

        JLabel lblBreadcrumb = new JLabel("Dashboard  /  Quản lý phiếu tiếp nhận");
        lblBreadcrumb.setFont(f(Font.PLAIN, 12));
        lblBreadcrumb.setForeground(C_TEXT_SUBTLE);

        JLabel lblTitle = new JLabel("Quản lý phiếu tiếp nhận – sửa chữa");
        lblTitle.setFont(f(Font.BOLD, 20));
        lblTitle.setForeground(C_TEXT_HEAD);

        JLabel lblSub = new JLabel("Theo dõi tình trạng tiếp nhận, tiến độ sửa chữa và quản lý phiếu xe ra vào gara.");
        lblSub.setFont(f(Font.PLAIN, 12));
        lblSub.setForeground(C_TEXT_SUBTLE);

        titleBlock.add(lblBreadcrumb, BorderLayout.NORTH);
        titleBlock.add(lblTitle, BorderLayout.CENTER);
        titleBlock.add(lblSub, BorderLayout.SOUTH);
        pnl.add(titleBlock, BorderLayout.WEST);

        return pnl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. KHỐI NỘI DUNG CHÍNH (Thanh lọc + Bảng dữ liệu)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildMainContent() {
        JPanel pnl = new JPanel(new BorderLayout(0, 12));
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(0, 24, 20, 24));

        pnl.add(buildToolbar(), BorderLayout.NORTH);
        pnl.add(buildTablePanel(), BorderLayout.CENTER);

        return pnl;
    }

    private JPanel buildToolbar() {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(C_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(10, 14, 10, 14)));

        // Trái: Ô tìm kiếm + Lọc trạng thái
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        txtTimKiem = new JTextField();
        txtTimKiem.setFont(f(Font.PLAIN, 13));
        txtTimKiem.setPreferredSize(new Dimension(300, 36));
        txtTimKiem.putClientProperty("JTextField.placeholderText", "Tìm mã phiếu, biển số xe, khách hàng, SĐT...");
        txtTimKiem.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(4, 10, 4, 10)));
        txtTimKiem.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    thucHienLoc();
                }
            }
        });
        left.add(txtTimKiem);

        cboLocTrangThai = new JComboBox<>(new String[]{
                "-- Tất cả trạng thái --",
                "Chờ sửa chữa (CHO_SUA)",
                "Đang sửa chữa (DANG_SUA)",
                "Chờ linh kiện (CHO_LINH_KIEN)",
                "Hoàn tất (HOAN_TAT)"
        });
        cboLocTrangThai.setFont(f(Font.PLAIN, 13));
        cboLocTrangThai.setPreferredSize(new Dimension(220, 36));
        cboLocTrangThai.addActionListener(e -> thucHienLoc());
        left.add(cboLocTrangThai);

        JButton btnTim = new RoundedButton("Tìm kiếm", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnTim.setFont(f(Font.PLAIN, 12));
        btnTim.setForeground(C_TEXT_MAIN);
        btnTim.setPreferredSize(new Dimension(90, 36));
        btnTim.addActionListener(e -> thucHienLoc());
        left.add(btnTim);

        btnLamMoi = new RoundedButton("↺ Làm mới", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnLamMoi.setFont(f(Font.PLAIN, 12));
        btnLamMoi.setForeground(C_TEXT_MAIN);
        btnLamMoi.setPreferredSize(new Dimension(95, 36));
        btnLamMoi.addActionListener(e -> {
            txtTimKiem.setText("");
            cboLocTrangThai.setSelectedIndex(0);
            taiDanhSachPhieu();
        });
        left.add(btnLamMoi);

        card.add(left, BorderLayout.WEST);

        // Phải: Nút tạo phiếu mới + Nút xem chi tiết
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        btnXemChiTiet = new RoundedButton("📄 Xem chi tiết phiếu", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnXemChiTiet.setFont(f(Font.PLAIN, 12));
        btnXemChiTiet.setForeground(C_TEXT_MAIN);
        btnXemChiTiet.setPreferredSize(new Dimension(150, 36));
        btnXemChiTiet.setEnabled(false);
        btnXemChiTiet.addActionListener(e -> onXemChiTietPhieu());
        right.add(btnXemChiTiet);

        btnThemPhieu = new RoundedButton("+ Tạo phiếu tiếp nhận (F2)", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
        btnThemPhieu.setFont(f(Font.BOLD, 12));
        btnThemPhieu.setForeground(C_WHITE);
        btnThemPhieu.setPreferredSize(new Dimension(200, 36));
        btnThemPhieu.addActionListener(e -> moModalLapPhieu(null, null));
        right.add(btnThemPhieu);

        card.add(right, BorderLayout.EAST);
        return card;
    }

    private JPanel buildTablePanel() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(C_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)));

        // Header bảng: Tiêu đề + đếm số lượng
        JPanel pnlHead = new JPanel(new BorderLayout());
        pnlHead.setOpaque(false);

        JLabel lblTitle = new JLabel("Danh sách phiếu tiếp nhận sửa chữa");
        lblTitle.setFont(f(Font.BOLD, 14));
        lblTitle.setForeground(C_TEXT_HEAD);
        pnlHead.add(lblTitle, BorderLayout.WEST);

        lblSoLuong = new JLabel("Đang tải dữ liệu...");
        lblSoLuong.setFont(f(Font.PLAIN, 12));
        lblSoLuong.setForeground(C_TEXT_SUBTLE);
        pnlHead.add(lblSoLuong, BorderLayout.EAST);

        card.add(pnlHead, BorderLayout.NORTH);

        // Bảng danh sách phiếu
        String[] cols = {"STT", "Mã phiếu", "Thời gian tiếp nhận", "Biển số xe", "Hãng & Dòng xe", "Khách hàng", "Số điện thoại", "Kỹ thuật viên chính", "Trạng thái"};
        modelPhieu = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tblPhieu = new JTable(modelPhieu);
        tblPhieu.setFont(f(Font.PLAIN, 13));
        tblPhieu.setRowHeight(34);
        tblPhieu.setShowGrid(false);
        tblPhieu.getTableHeader().setFont(f(Font.BOLD, 12));
        tblPhieu.getTableHeader().setBackground(new Color(0xF8FAFC));
        tblPhieu.getTableHeader().setPreferredSize(new Dimension(0, 36));
        tblPhieu.setSelectionBackground(C_PRIMARY_BG);
        tblPhieu.setSelectionForeground(C_TEXT_HEAD);

        // Canh lề và độ rộng cột
        tblPhieu.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblPhieu.getColumnModel().getColumn(1).setPreferredWidth(130);
        tblPhieu.getColumnModel().getColumn(2).setPreferredWidth(130);
        tblPhieu.getColumnModel().getColumn(3).setPreferredWidth(100);
        tblPhieu.getColumnModel().getColumn(4).setPreferredWidth(130);
        tblPhieu.getColumnModel().getColumn(5).setPreferredWidth(140);
        tblPhieu.getColumnModel().getColumn(6).setPreferredWidth(100);
        tblPhieu.getColumnModel().getColumn(7).setPreferredWidth(130);
        tblPhieu.getColumnModel().getColumn(8).setPreferredWidth(130);

        // Renderer cho STT
        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        tblPhieu.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        // Renderer cho Trạng thái (Badge bo góc)
        tblPhieu.getColumnModel().getColumn(8).setCellRenderer(new TrangThaiBadgeRenderer());

        tblPhieu.getSelectionModel().addListSelectionListener(e -> {
            boolean coChon = tblPhieu.getSelectedRow() >= 0;
            btnXemChiTiet.setEnabled(coChon);
        });

        tblPhieu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tblPhieu.getSelectedRow() >= 0) {
                    onXemChiTietPhieu();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tblPhieu);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. TẢI DỮ LIỆU & LỌC
    // ─────────────────────────────────────────────────────────────────────────
    public void taiDanhSachPhieu() {
        lblSoLuong.setText("Đang tải...");
        new SwingWorker<List<PhieuTiepNhanSuaChua>, Void>() {
            @Override
            protected List<PhieuTiepNhanSuaChua> doInBackground() {
                return controller.layDanhSachPhieu();
            }

            @Override
            protected void done() {
                try {
                    dsPhieu = get();
                    capNhatDuLieuLenBang(dsPhieu);
                } catch (Exception ex) {
                    lblSoLuong.setText("Lỗi: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void thucHienLoc() {
        String tuKhoa = txtTimKiem.getText().trim();
        int idxTt = cboLocTrangThai.getSelectedIndex();
        TrangThaiPhieu tt = switch (idxTt) {
            case 1 -> TrangThaiPhieu.CHO_SUA;
            case 2 -> TrangThaiPhieu.DANG_SUA;
            case 3 -> TrangThaiPhieu.CHO_LINH_KIEN;
            case 4 -> TrangThaiPhieu.HOAN_TAT;
            default -> null;
        };

        lblSoLuong.setText("Đang lọc...");
        new SwingWorker<List<PhieuTiepNhanSuaChua>, Void>() {
            @Override
            protected List<PhieuTiepNhanSuaChua> doInBackground() {
                return controller.timKiemPhieu(tuKhoa, tt);
            }

            @Override
            protected void done() {
                try {
                    dsPhieu = get();
                    capNhatDuLieuLenBang(dsPhieu);
                } catch (Exception ex) {
                    lblSoLuong.setText("Lỗi: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void capNhatDuLieuLenBang(List<PhieuTiepNhanSuaChua> list) {
        modelPhieu.setRowCount(0);
        int stt = 1;
        for (PhieuTiepNhanSuaChua p : list) {
            Xe xe = p.getXe();
            KhachHang kh = xe != null ? xe.getChuXe() : null;
            String xeInfo = (xe != null && xe.getHangXe() != null ? xe.getHangXe() + " " : "") +
                            (xe != null && xe.getModel() != null ? xe.getModel() : "--");

            modelPhieu.addRow(new Object[]{
                    stt++,
                    p.getMaPhieu(),
                    p.getNgayGioTiepNhan() != null ? timeFmt.format(p.getNgayGioTiepNhan()) : "--",
                    xe != null ? xe.getBienSoXe() : "--",
                    xeInfo,
                    kh != null ? kh.getHoTen() : "--",
                    kh != null ? kh.getSoDienThoai() : "--",
                    p.getNhanVienSuaChua() != null ? p.getNhanVienSuaChua().getHoTen() : "-- Chưa phân công --",
                    p.getTrangThai() != null ? p.getTrangThai().name() : "CHO_SUA"
            });
        }
        lblSoLuong.setText("Tổng cộng: " + list.size() + " phiếu tiếp nhận");
        btnXemChiTiet.setEnabled(false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. PHÍM TẮT & ĐIỀU HƯỚNG TỪ XE (F2)
    // ─────────────────────────────────────────────────────────────────────────
    private void caiDatPhimTat() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "themPhieuF2");
        am.put("themPhieuF2", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                moModalLapPhieu(null, null);
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "lamMoiF5");
        am.put("lamMoiF5", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                taiDanhSachPhieu();
            }
        });
    }

    /**
     * Nhận yêu cầu từ trang Xe (nút F2 bên Quản lý khách hàng và xe)
     * Tự động mở Modal Lập phiếu tiếp nhận và điền sẵn Khách hàng + Xe.
     */
    public void thietLapKhachHangVaXe(String maKH, String bienSoXe) {
        moModalLapPhieu(maKH, bienSoXe);
    }

    private void moModalLapPhieu(String maKHMoi, String bienSoXeMoi) {
        Window parent = SwingUtilities.getWindowAncestor(this);
        LapPhieuTiepNhanModalDialog dialog = new LapPhieuTiepNhanModalDialog(parent, maKHMoi, bienSoXeMoi);
        dialog.setVisible(true);
        if (dialog.isThanhCong()) {
            taiDanhSachPhieu();
        }
    }

    private void onXemChiTietPhieu() {
        int row = tblPhieu.getSelectedRow();
        if (row < 0 || row >= dsPhieu.size()) return;
        PhieuTiepNhanSuaChua phieu = dsPhieu.get(row);

        Window parent = SwingUtilities.getWindowAncestor(this);
        ChiTietPhieuModalDialog dialog = new ChiTietPhieuModalDialog(parent, phieu.getMaPhieu());
        dialog.setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. MODAL FORM: LẬP PHIẾU TIẾP NHẬN SỬA CHỮA (BÁM SÁT 100% FIGMA)
    // ─────────────────────────────────────────────────────────────────────────
    private class LapPhieuTiepNhanModalDialog extends JDialog {
        private final String defaultMaKH;
        private final String defaultBienSoXe;
        private boolean thanhCong = false;

        private List<KhachHang> modalDsKH = new ArrayList<>();
        private List<Xe> modalDsXe = new ArrayList<>();
        private List<DichVu> modalDsDV = new ArrayList<>();
        private List<NhanVien> modalDsKtv = new ArrayList<>();
        private final List<ChiTietDichVu> modalDsChiTietDV = new ArrayList<>();

        // Form fields
        private JComboBox<String> cboKhachHang;
        private JComboBox<String> cboXe;
        private JComboBox<String> cboKtv;
        private JTextField txtMaPhieu;
        private JTextField txtThoiGian;
        private JTextField txtNguoiTiepNhan;
        private JTextArea txtYeuCauKhach;
        private JTextArea txtTinhTrangXe;
        private JTextField txtGhiChuTiepNhan;

        // Dịch vụ đề nghị
        private JComboBox<String> cboDichVu;
        private JSpinner spnSoLuongDV;
        private JTextField txtGhiChuDV;
        private DefaultTableModel modelDVModal;
        private JTable tblDVModal;

        private JLabel lblCanhBaoLuu;
        private JButton btnHuy;
        private JButton btnLuu;

        public LapPhieuTiepNhanModalDialog(Window parent, String maKH, String bienSo) {
            super(parent, "Lập phiếu tiếp nhận – sửa chữa", ModalityType.APPLICATION_MODAL);
            this.defaultMaKH = maKH;
            this.defaultBienSoXe = bienSo;

            setLayout(new BorderLayout());
            getContentPane().setBackground(C_WHITE);
            setResizable(false);

            add(buildModalHeader(), BorderLayout.NORTH);
            add(buildModalBody(), BorderLayout.CENTER);
            add(buildModalFooter(), BorderLayout.SOUTH);

            pack();
            setLocationRelativeTo(parent);

            napDuLieuModal();
        }

        public boolean isThanhCong() { return thanhCong; }

        private JPanel buildModalHeader() {
            JPanel pnl = new JPanel(new BorderLayout());
            pnl.setBackground(C_WHITE);
            pnl.setBorder(new EmptyBorder(16, 24, 12, 24));

            JLabel lblTitle = new JLabel("Lập phiếu tiếp nhận – sửa chữa");
            lblTitle.setFont(f(Font.BOLD, 18));
            lblTitle.setForeground(C_TEXT_HEAD);
            pnl.add(lblTitle, BorderLayout.WEST);

            return pnl;
        }

        private JScrollPane buildModalBody() {
            JPanel pnl = new JPanel();
            pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
            pnl.setBackground(C_WHITE);
            pnl.setBorder(new EmptyBorder(0, 24, 14, 24));

            // KHỐI 1: THÔNG TIN TIẾP NHẬN (Theo Figma)
            JPanel card1 = taoCardNhom("Thông tin tiếp nhận");
            JPanel pnlC1 = new JPanel();
            pnlC1.setLayout(new BoxLayout(pnlC1, BoxLayout.Y_AXIS));
            pnlC1.setOpaque(false);

            // Hàng 1: Khách hàng * | Xe của khách hàng * | Nút Hồ sơ
            JPanel row1 = new JPanel(new GridLayout(1, 3, 10, 0));
            row1.setOpaque(false);
            row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            row1.setAlignmentX(0.0f);

            cboKhachHang = new JComboBox<>();
            cboKhachHang.setFont(f(Font.PLAIN, 13));
            cboKhachHang.addActionListener(e -> onChonKhachHangModal());
            row1.add(wrapFieldModal("Khách hàng *", cboKhachHang));

            cboXe = new JComboBox<>();
            cboXe.setFont(f(Font.PLAIN, 13));
            row1.add(wrapFieldModal("Xe của khách hàng *", cboXe));

            cboKtv = new JComboBox<>();
            cboKtv.setFont(f(Font.PLAIN, 13));
            row1.add(wrapFieldModal("Kỹ thuật viên chính phụ trách", cboKtv));

            pnlC1.add(row1);
            pnlC1.add(Box.createVerticalStrut(10));

            // Hàng 2: Mã phiếu (tự sinh) | Thời điểm tiếp nhận | Người tiếp nhận
            JPanel row2 = new JPanel(new GridLayout(1, 3, 10, 0));
            row2.setOpaque(false);
            row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            row2.setAlignmentX(0.0f);

            txtMaPhieu = taoTextFieldModal("Chưa được cấp mã (Hệ thống tự sinh)", false);
            row2.add(wrapFieldModal("Mã phiếu — tự sinh · Chỉ đọc", txtMaPhieu));

            txtThoiGian = taoTextFieldModal(timeFmt.format(new Date()), false);
            row2.add(wrapFieldModal("Thời điểm tiếp nhận *", txtThoiGian));

            String nvName = UserSession.getInstance().daDangNhap() ?
                    UserSession.getInstance().getNhanVien().getHoTen() : "Tiếp nhận viên";
            txtNguoiTiepNhan = taoTextFieldModal(nvName, false);
            row2.add(wrapFieldModal("Người tiếp nhận · Chỉ đọc", txtNguoiTiepNhan));

            pnlC1.add(row2);
            pnlC1.add(Box.createVerticalStrut(10));

            // Hàng 3: 2 ô tách biệt Yêu cầu khách hàng * & Tình trạng xe khi tiếp nhận * (Chuẩn Figma)
            JPanel row3 = new JPanel(new GridLayout(1, 2, 10, 0));
            row3.setOpaque(false);
            row3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));
            row3.setAlignmentX(0.0f);

            txtYeuCauKhach = new JTextArea(3, 20);
            txtYeuCauKhach.setFont(f(Font.PLAIN, 13));
            txtYeuCauKhach.setLineWrap(true);
            txtYeuCauKhach.setWrapStyleWord(true);
            JScrollPane scrollYc = new JScrollPane(txtYeuCauKhach);
            scrollYc.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
            row3.add(wrapFieldModal("Yêu cầu của khách hàng *", scrollYc));

            txtTinhTrangXe = new JTextArea(3, 20);
            txtTinhTrangXe.setFont(f(Font.PLAIN, 13));
            txtTinhTrangXe.setLineWrap(true);
            txtTinhTrangXe.setWrapStyleWord(true);
            JScrollPane scrollTt = new JScrollPane(txtTinhTrangXe);
            scrollTt.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
            row3.add(wrapFieldModal("Tình trạng xe khi tiếp nhận *", scrollTt));

            pnlC1.add(row3);
            pnlC1.add(Box.createVerticalStrut(10));

            // Hàng 4: Ghi chú tiếp nhận
            txtGhiChuTiepNhan = taoTextFieldModal("", true);
            txtGhiChuTiepNhan.putClientProperty("JTextField.placeholderText", "Nhập thông tin bổ sung");
            JPanel row4 = wrapFieldModal("Ghi chú tiếp nhận", txtGhiChuTiepNhan);
            row4.setAlignmentX(0.0f);
            pnlC1.add(row4);

            card1.add(pnlC1, BorderLayout.CENTER);
            pnl.add(card1);
            pnl.add(Box.createVerticalStrut(14));

            // KHỐI 2: DỊCH VỤ ĐỀ NGHỊ (Theo Figma)
            JPanel card2 = taoCardNhom("Dịch vụ đề nghị");
            JPanel pnlC2 = new JPanel(new BorderLayout(0, 8));
            pnlC2.setOpaque(false);

            // Công cụ chọn dịch vụ
            JPanel toolRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            toolRow.setOpaque(false);

            toolRow.add(new JLabel("Chọn dịch vụ:"));
            cboDichVu = new JComboBox<>();
            cboDichVu.setFont(f(Font.PLAIN, 13));
            cboDichVu.setPreferredSize(new Dimension(280, 34));
            toolRow.add(cboDichVu);

            toolRow.add(new JLabel("Số lượng:"));
            spnSoLuongDV = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
            spnSoLuongDV.setFont(f(Font.PLAIN, 13));
            spnSoLuongDV.setPreferredSize(new Dimension(65, 34));
            toolRow.add(spnSoLuongDV);

            toolRow.add(new JLabel("Ghi chú:"));
            txtGhiChuDV = taoTextFieldModal("", true);
            txtGhiChuDV.setPreferredSize(new Dimension(140, 34));
            toolRow.add(txtGhiChuDV);

            JButton btnThemDV = new RoundedButton("+ Thêm", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
            btnThemDV.setFont(f(Font.BOLD, 12));
            btnThemDV.setForeground(C_WHITE);
            btnThemDV.setPreferredSize(new Dimension(80, 34));
            btnThemDV.addActionListener(e -> themDichVuModal());
            toolRow.add(btnThemDV);

            JButton btnBoDV = new RoundedButton("✕ Bỏ", 8, C_WHITE, new Color(0xFEE2E2), C_BORDER);
            btnBoDV.setFont(f(Font.PLAIN, 12));
            btnBoDV.setForeground(C_DANGER);
            btnBoDV.setPreferredSize(new Dimension(70, 34));
            btnBoDV.addActionListener(e -> boDichVuModal());
            toolRow.add(btnBoDV);

            pnlC2.add(toolRow, BorderLayout.NORTH);

            // Bảng dịch vụ đề nghị (cột đúng Figma: Mã DV, Tên DV, ĐVT, Số lượng, Ghi chú)
            String[] dvCols = {"STT", "Mã dịch vụ", "Tên dịch vụ", "Đơn vị tính", "Số lượng", "Ghi chú"};
            modelDVModal = new DefaultTableModel(dvCols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            tblDVModal = new JTable(modelDVModal);
            tblDVModal.setFont(f(Font.PLAIN, 13));
            tblDVModal.setRowHeight(28);
            tblDVModal.setShowGrid(false);
            tblDVModal.getTableHeader().setFont(f(Font.BOLD, 12));
            tblDVModal.getTableHeader().setBackground(new Color(0xF8FAFC));

            JScrollPane scrollDV = new JScrollPane(tblDVModal);
            scrollDV.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
            scrollDV.setPreferredSize(new Dimension(740, 130));
            pnlC2.add(scrollDV, BorderLayout.CENTER);

            card2.add(pnlC2, BorderLayout.CENTER);
            pnl.add(card2);

            JScrollPane mainScroll = new JScrollPane(pnl);
            mainScroll.setBorder(null);
            mainScroll.setPreferredSize(new Dimension(780, 520));
            mainScroll.getVerticalScrollBar().setUnitIncrement(16);
            return mainScroll;
        }

        private JPanel buildModalFooter() {
            JPanel pnl = new JPanel(new BorderLayout(10, 0));
            pnl.setBackground(new Color(0xF8FAFC));
            pnl.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER),
                    new EmptyBorder(12, 24, 12, 24)));

            lblCanhBaoLuu = new JLabel("Chưa thể lưu: vui lòng chọn khách hàng, xe và nhập nội dung yêu cầu.");
            lblCanhBaoLuu.setFont(f(Font.ITALIC, 12));
            lblCanhBaoLuu.setForeground(C_TEXT_SUBTLE);
            pnl.add(lblCanhBaoLuu, BorderLayout.WEST);

            JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            btnGroup.setOpaque(false);

            btnHuy = new RoundedButton("Hủy", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
            btnHuy.setFont(f(Font.PLAIN, 12));
            btnHuy.setForeground(C_TEXT_MAIN);
            btnHuy.setPreferredSize(new Dimension(85, 34));
            btnHuy.addActionListener(e -> dispose());
            btnGroup.add(btnHuy);

            btnLuu = new RoundedButton("💾 Lưu phiếu tiếp nhận", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
            btnLuu.setFont(f(Font.BOLD, 12));
            btnLuu.setForeground(C_WHITE);
            btnLuu.setPreferredSize(new Dimension(170, 34));
            btnLuu.addActionListener(e -> thucHienLuuPhieuModal());
            btnGroup.add(btnLuu);

            pnl.add(btnGroup, BorderLayout.EAST);
            return pnl;
        }

        private JPanel taoCardNhom(String title) {
            JPanel card = new JPanel(new BorderLayout(0, 8));
            card.setBackground(C_WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER, 1),
                    new EmptyBorder(10, 14, 10, 14)));
            card.setAlignmentX(0.0f);

            JLabel lbl = new JLabel(title);
            lbl.setFont(f(Font.BOLD, 13));
            lbl.setForeground(C_TEXT_HEAD);
            card.add(lbl, BorderLayout.NORTH);
            return card;
        }

        private JPanel wrapFieldModal(String labelText, JComponent input) {
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

        private JTextField taoTextFieldModal(String text, boolean editable) {
            JTextField tf = new JTextField(text);
            tf.setFont(f(Font.PLAIN, 13));
            tf.setEditable(editable);
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

        private void napDuLieuModal() {
            new SwingWorker<Void, Void>() {
                List<KhachHang> khs;
                List<DichVu> dvs;
                List<NhanVien> ktvs;

                @Override
                protected Void doInBackground() {
                    khs = controller.layDanhSachKhachHang();
                    dvs = controller.layDanhSachDichVuDangCungCap();
                    ktvs = controller.layDanhSachKyThuatVien();
                    return null;
                }

                @Override
                protected void done() {
                    modalDsKH = khs != null ? khs : List.of();
                    modalDsDV = dvs != null ? dvs : List.of();
                    modalDsKtv = ktvs != null ? ktvs : List.of();

                    cboKhachHang.removeAllItems();
                    cboKhachHang.addItem("-- Chọn khách hàng --");
                    for (KhachHang kh : modalDsKH) {
                        cboKhachHang.addItem(kh.getMaKH() + " - " + kh.getHoTen() + " (" + kh.getSoDienThoai() + ")");
                    }

                    cboKtv.removeAllItems();
                    cboKtv.addItem("-- Chưa phân công KTV --");
                    for (NhanVien ktv : modalDsKtv) {
                        cboKtv.addItem(ktv.getMaNV() + " - " + ktv.getHoTen());
                    }

                    cboDichVu.removeAllItems();
                    cboDichVu.addItem("-- Chọn dịch vụ --");
                    for (DichVu dv : modalDsDV) {
                        cboDichVu.addItem(dv.getMaDV() + " - " + dv.getTenDV() + " (" + moneyFmt.format(dv.getDonGia()) + ")");
                    }

                    // Nếu có truyền sẵn maKH và bienSo (từ F2 Quản lý xe)
                    if (defaultMaKH != null) {
                        for (int i = 0; i < modalDsKH.size(); i++) {
                            if (defaultMaKH.equals(modalDsKH.get(i).getMaKH())) {
                                cboKhachHang.setSelectedIndex(i + 1);
                                break;
                            }
                        }
                    }
                }
            }.execute();
        }

        private void onChonKhachHangModal() {
            int idx = cboKhachHang.getSelectedIndex();
            if (idx <= 0 || idx - 1 >= modalDsKH.size()) {
                cboXe.removeAllItems();
                cboXe.addItem("-- Vui lòng chọn khách hàng trước --");
                modalDsXe = List.of();
                return;
            }

            KhachHang kh = modalDsKH.get(idx - 1);
            new SwingWorker<List<Xe>, Void>() {
                @Override
                protected List<Xe> doInBackground() {
                    return controller.layDanhSachXeTheoKhachHang(kh.getMaKH());
                }

                @Override
                protected void done() {
                    try {
                        modalDsXe = get();
                        cboXe.removeAllItems();
                        if (modalDsXe.isEmpty()) {
                            cboXe.addItem("-- Khách hàng chưa đăng ký xe --");
                        } else {
                            for (Xe xe : modalDsXe) {
                                String info = xe.getBienSoXe() + " (" + (xe.getHangXe() != null ? xe.getHangXe() : "") + " " + (xe.getModel() != null ? xe.getModel() : "") + ")";
                                cboXe.addItem(info);
                            }
                            if (defaultBienSoXe != null) {
                                for (int j = 0; j < modalDsXe.size(); j++) {
                                    if (defaultBienSoXe.equalsIgnoreCase(modalDsXe.get(j).getBienSoXe())) {
                                        cboXe.setSelectedIndex(j);
                                        break;
                                    }
                                }
                            }
                        }
                        lblCanhBaoLuu.setText("Đã chọn khách hàng. Vui lòng nhập nội dung yêu cầu.");
                    } catch (Exception ex) {
                        lblCanhBaoLuu.setText("Lỗi nạp xe: " + ex.getMessage());
                    }
                }
            }.execute();
        }

        private void themDichVuModal() {
            int idx = cboDichVu.getSelectedIndex();
            if (idx <= 0 || idx - 1 >= modalDsDV.size()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn dịch vụ đề nghị.", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            DichVu dv = modalDsDV.get(idx - 1);
            for (ChiTietDichVu ct : modalDsChiTietDV) {
                if (ct.getDichVu() != null && ct.getDichVu().getMaDV().equals(dv.getMaDV())) {
                    JOptionPane.showMessageDialog(this, "Dịch vụ này đã có trong danh sách.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            }

            ChiTietDichVu ctMoi = new ChiTietDichVu();
            ctMoi.setSttRaw(modalDsChiTietDV.size() + 1);
            ctMoi.setDichVuRaw(dv);
            ctMoi.setSoLuongRaw((int) spnSoLuongDV.getValue());
            ctMoi.setDonGiaThucHienRaw(dv.getDonGia());
            String gh = txtGhiChuDV.getText().trim();
            ctMoi.setGhiChuRaw(gh.isEmpty() ? null : gh);
            modalDsChiTietDV.add(ctMoi);

            capNhatBangDVModal();
            txtGhiChuDV.setText("");
            spnSoLuongDV.setValue(1);
        }

        private void boDichVuModal() {
            int row = tblDVModal.getSelectedRow();
            if (row < 0 || row >= modalDsChiTietDV.size()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn dịch vụ cần bỏ.", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            modalDsChiTietDV.remove(row);
            for (int i = 0; i < modalDsChiTietDV.size(); i++) {
                modalDsChiTietDV.get(i).setSttRaw(i + 1);
            }
            capNhatBangDVModal();
        }

        private void capNhatBangDVModal() {
            modelDVModal.setRowCount(0);
            for (int i = 0; i < modalDsChiTietDV.size(); i++) {
                ChiTietDichVu ct = modalDsChiTietDV.get(i);
                DichVu dv = ct.getDichVu();
                modelDVModal.addRow(new Object[]{
                        i + 1,
                        dv.getMaDV(),
                        dv.getTenDV(),
                        dv.getDonViTinh() != null ? dv.getDonViTinh() : "Lần",
                        ct.getSoLuong(),
                        ct.getGhiChu() != null ? ct.getGhiChu() : ""
                });
            }
        }

        private void thucHienLuuPhieuModal() {
            int idxKh = cboKhachHang.getSelectedIndex();
            if (idxKh <= 0) {
                lblCanhBaoLuu.setText("Lỗi: Chưa chọn khách hàng.");
                lblCanhBaoLuu.setForeground(C_DANGER);
                cboKhachHang.requestFocusInWindow();
                return;
            }
            if (modalDsXe.isEmpty() || cboXe.getSelectedIndex() < 0) {
                lblCanhBaoLuu.setText("Lỗi: Khách hàng chưa có xe được chọn.");
                lblCanhBaoLuu.setForeground(C_DANGER);
                return;
            }

            String yc = txtYeuCauKhach.getText().trim();
            if (yc.isEmpty()) {
                lblCanhBaoLuu.setText("Lỗi: Vui lòng nhập nội dung yêu cầu của khách hàng.");
                lblCanhBaoLuu.setForeground(C_DANGER);
                txtYeuCauKhach.requestFocusInWindow();
                return;
            }

            Xe xeChon = modalDsXe.get(cboXe.getSelectedIndex());
            String tinhTrang = txtTinhTrangXe.getText().trim();
            String ghiChu = txtGhiChuTiepNhan.getText().trim();

            String maKtv = null;
            int idxKtv = cboKtv.getSelectedIndex();
            if (idxKtv > 0 && idxKtv - 1 < modalDsKtv.size()) {
                maKtv = modalDsKtv.get(idxKtv - 1).getMaNV();
            }

            final String finalBienSo = xeChon.getBienSoXe();
            final String finalMaKtv = maKtv;
            final String finalYc = yc;
            final String finalTt = tinhTrang;
            final String finalGc = ghiChu;

            btnLuu.setEnabled(false);
            btnLuu.setText("Đang lưu...");

            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() {
                    return controller.lapPhieu(finalBienSo, finalMaKtv, finalYc, finalTt, finalGc, modalDsChiTietDV);
                }

                @Override
                protected void done() {
                    btnLuu.setEnabled(true);
                    btnLuu.setText("💾 Lưu phiếu tiếp nhận");
                    try {
                        String maPhieuMoi = get();
                        thanhCong = true;
                        JOptionPane.showMessageDialog(LapPhieuTiepNhanModalDialog.this,
                                "Lập phiếu tiếp nhận sửa chữa thành công!\nMã phiếu: " + maPhieuMoi + "\nTrạng thái: Chờ sửa chữa",
                                "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } catch (Exception ex) {
                        String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                        lblCanhBaoLuu.setText("Lỗi: " + msg);
                        lblCanhBaoLuu.setForeground(C_DANGER);
                        JOptionPane.showMessageDialog(LapPhieuTiepNhanModalDialog.this, msg, "Lỗi lập phiếu", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. MODAL DIALOG: XEM CHI TIẾT PHIẾU TIẾP NHẬN
    // ─────────────────────────────────────────────────────────────────────────
    private class ChiTietPhieuModalDialog extends JDialog {
        private final String maPhieu;

        public ChiTietPhieuModalDialog(Window parent, String maPhieu) {
            super(parent, "Chi tiết phiếu tiếp nhận " + maPhieu, ModalityType.APPLICATION_MODAL);
            this.maPhieu = maPhieu;

            setLayout(new BorderLayout());
            getContentPane().setBackground(C_WHITE);
            setResizable(false);

            add(buildHeader(), BorderLayout.NORTH);
            add(buildContent(), BorderLayout.CENTER);
            add(buildFooter(), BorderLayout.SOUTH);

            pack();
            setLocationRelativeTo(parent);
        }

        private JPanel buildHeader() {
            JPanel pnl = new JPanel(new BorderLayout());
            pnl.setBackground(C_WHITE);
            pnl.setBorder(new EmptyBorder(16, 24, 12, 24));

            JLabel lblTitle = new JLabel("Chi tiết phiếu tiếp nhận – " + maPhieu);
            lblTitle.setFont(f(Font.BOLD, 18));
            lblTitle.setForeground(C_TEXT_HEAD);
            pnl.add(lblTitle, BorderLayout.WEST);

            return pnl;
        }

        private JScrollPane buildContent() {
            JPanel pnl = new JPanel();
            pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
            pnl.setBackground(C_WHITE);
            pnl.setBorder(new EmptyBorder(0, 24, 14, 24));

            PhieuTiepNhanSuaChua phieu = controller.layPhieuTheoMa(maPhieu);
            if (phieu == null) {
                pnl.add(new JLabel("Không tìm thấy thông tin phiếu " + maPhieu));
                return new JScrollPane(pnl);
            }

            Xe xe = phieu.getXe();
            KhachHang kh = xe != null ? xe.getChuXe() : null;

            // Khối thông tin chung
            JPanel cardInfo = new JPanel(new GridLayout(4, 2, 12, 8));
            cardInfo.setBackground(new Color(0xF8FAFC));
            cardInfo.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER, 1),
                    new EmptyBorder(12, 16, 12, 16)));
            cardInfo.setAlignmentX(0.0f);

            cardInfo.add(taoDongInfo("Mã phiếu:", phieu.getMaPhieu()));
            cardInfo.add(taoDongInfo("Trạng thái:", phieu.getTrangThai() != null ? phieu.getTrangThai().name() : "--"));
            cardInfo.add(taoDongInfo("Khách hàng:", (kh != null ? kh.getHoTen() : "--") + " (" + (kh != null ? kh.getSoDienThoai() : "") + ")"));
            cardInfo.add(taoDongInfo("Biển số xe:", (xe != null ? xe.getBienSoXe() : "--") + " - " + (xe != null ? xe.getHangXe() + " " + xe.getModel() : "")));
            cardInfo.add(taoDongInfo("Thời gian tiếp nhận:", phieu.getNgayGioTiepNhan() != null ? timeFmt.format(phieu.getNgayGioTiepNhan()) : "--"));
            cardInfo.add(taoDongInfo("Người tiếp nhận:", phieu.getNhanVienLap() != null ? phieu.getNhanVienLap().getHoTen() : "--"));
            cardInfo.add(taoDongInfo("Kỹ thuật viên chính:", phieu.getNhanVienSuaChua() != null ? phieu.getNhanVienSuaChua().getHoTen() : "-- Chưa phân công --"));
            cardInfo.add(taoDongInfo("Số km hiện tại:", (xe != null ? xe.getSoKmHienTai() + " km" : "--")));

            pnl.add(cardInfo);
            pnl.add(Box.createVerticalStrut(12));

            // Khối yêu cầu & hiện trạng
            JPanel cardYc = new JPanel(new BorderLayout(0, 4));
            cardYc.setBackground(C_WHITE);
            cardYc.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER, 1),
                    new EmptyBorder(10, 14, 10, 14)));
            cardYc.setAlignmentX(0.0f);

            JLabel lblYcTitle = new JLabel("Nội dung yêu cầu & tình trạng xe:");
            lblYcTitle.setFont(f(Font.BOLD, 12));
            lblYcTitle.setForeground(C_TEXT_HEAD);
            cardYc.add(lblYcTitle, BorderLayout.NORTH);

            JTextArea taYc = new JTextArea(phieu.getYeuCauBanDau() != null ? phieu.getYeuCauBanDau() : "--");
            taYc.setFont(f(Font.PLAIN, 13));
            taYc.setEditable(false);
            taYc.setLineWrap(true);
            taYc.setWrapStyleWord(true);
            taYc.setBackground(C_WHITE);
            cardYc.add(taYc, BorderLayout.CENTER);

            pnl.add(cardYc);
            pnl.add(Box.createVerticalStrut(12));

            // Bảng danh sách dịch vụ đề nghị
            List<ChiTietDichVu> dsDv = controller.layChiTietDichVu(maPhieu);
            JPanel cardDv = new JPanel(new BorderLayout(0, 6));
            cardDv.setBackground(C_WHITE);
            cardDv.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER, 1),
                    new EmptyBorder(10, 14, 10, 14)));
            cardDv.setAlignmentX(0.0f);

            JLabel lblDvTitle = new JLabel("Danh sách dịch vụ đề nghị (" + dsDv.size() + " dịch vụ):");
            lblDvTitle.setFont(f(Font.BOLD, 12));
            cardDv.add(lblDvTitle, BorderLayout.NORTH);

            String[] dvCols = {"Mã DV", "Tên dịch vụ", "ĐVT", "Số lượng", "Đơn giá", "Thành tiền", "Ghi chú"};
            DefaultTableModel modelDv = new DefaultTableModel(dvCols, 0);
            double tongTienDv = 0;
            for (ChiTietDichVu ct : dsDv) {
                DichVu d = ct.getDichVu();
                double tt = ct.getDonGia() * ct.getSoLuong();
                tongTienDv += tt;
                modelDv.addRow(new Object[]{
                        d != null ? d.getMaDV() : "--",
                        d != null ? d.getTenDV() : "--",
                        d != null ? d.getDonViTinh() : "Lần",
                        ct.getSoLuong(),
                        moneyFmt.format(ct.getDonGia()),
                        moneyFmt.format(tt),
                        ct.getGhiChu() != null ? ct.getGhiChu() : ""
                });
            }
            JTable tblDv = new JTable(modelDv);
            tblDv.setFont(f(Font.PLAIN, 12));
            tblDv.setRowHeight(26);
            tblDv.setShowGrid(false);
            tblDv.getTableHeader().setFont(f(Font.BOLD, 11));
            JScrollPane spDv = new JScrollPane(tblDv);
            spDv.setPreferredSize(new Dimension(680, 110));
            cardDv.add(spDv, BorderLayout.CENTER);

            JLabel lblTongTien = new JLabel("Tổng tiền dịch vụ tạm tính: " + moneyFmt.format(tongTienDv));
            lblTongTien.setFont(f(Font.BOLD, 12));
            lblTongTien.setForeground(C_PRIMARY);
            lblTongTien.setHorizontalAlignment(SwingConstants.RIGHT);
            cardDv.add(lblTongTien, BorderLayout.SOUTH);

            pnl.add(cardDv);

            JScrollPane scroll = new JScrollPane(pnl);
            scroll.setBorder(null);
            scroll.setPreferredSize(new Dimension(720, 480));
            return scroll;
        }

        private JPanel buildFooter() {
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
            pnl.setBackground(new Color(0xF8FAFC));
            pnl.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));

            JButton btnDong = new RoundedButton("Đóng", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
            btnDong.setFont(f(Font.BOLD, 12));
            btnDong.setForeground(C_WHITE);
            btnDong.setPreferredSize(new Dimension(90, 34));
            btnDong.addActionListener(e -> dispose());
            pnl.add(btnDong);

            return pnl;
        }

        private JPanel taoDongInfo(String label, String value) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            p.setOpaque(false);
            JLabel lblL = new JLabel(label);
            lblL.setFont(f(Font.BOLD, 12));
            lblL.setForeground(C_TEXT_SUBTLE);
            JLabel lblV = new JLabel(value != null ? value : "--");
            lblV.setFont(f(Font.PLAIN, 13));
            lblV.setForeground(C_TEXT_HEAD);
            p.add(lblL);
            p.add(lblV);
            return p;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. CELL RENDERER TRẠNG THÁI BADGE BO GÓC MÀU SẮC
    // ─────────────────────────────────────────────────────────────────────────
    private static class TrangThaiBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String val = value != null ? value.toString() : "";
            JLabel lbl = new JLabel();
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setOpaque(true);

            String text;
            Color bg;
            Color fg;

            switch (val) {
                case "CHO_SUA" -> {
                    text = "Chờ sửa chữa";
                    bg = new Color(0xFEF3C7);
                    fg = new Color(0x92400E);
                }
                case "DANG_SUA" -> {
                    text = "Đang sửa chữa";
                    bg = new Color(0xDBEAFE);
                    fg = new Color(0x1E40AF);
                }
                case "CHO_LINH_KIEN" -> {
                    text = "Chờ linh kiện";
                    bg = new Color(0xF3E8FF);
                    fg = new Color(0x6B21A8);
                }
                case "HOAN_TAT" -> {
                    text = "Hoàn tất";
                    bg = new Color(0xD1FAE5);
                    fg = new Color(0x065F46);
                }
                default -> {
                    text = val;
                    bg = new Color(0xF1F5F9);
                    fg = new Color(0x475569);
                }
            }

            lbl.setText(text);
            lbl.setBackground(bg);
            lbl.setForeground(fg);
            lbl.setBorder(BorderFactory.createCompoundBorder(
                    new EmptyBorder(3, 8, 3, 8),
                    BorderFactory.createLineBorder(bg, 1)
            ));

            JPanel wrapper = new JPanel(new GridBagLayout());
            if (isSelected) {
                wrapper.setBackground(table.getSelectionBackground());
            } else {
                wrapper.setBackground(table.getBackground());
            }
            wrapper.add(lbl);
            return wrapper;
        }
    }
}
