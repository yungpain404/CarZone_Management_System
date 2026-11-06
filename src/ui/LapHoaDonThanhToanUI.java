package ui;

import controller.LapHoaDonThanhToanController;
import entity.*;
import enums.PhuongThucThanhToan;
import enums.TrangThaiThanhToan;
import enums.VaiTro;
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
import java.util.List;

import static ui.MainFrame.*;

/**
 * Màn hình Quản lý Hóa đơn và Thanh toán (UC014, UC015 - Tiếp nhận viên).
 * Mô hình 2 Tab danh sách:
 * - Tab 1: Danh sách các xe hoàn tất sửa chữa đang chờ thanh toán.
 * - Tab 2: Lịch sử hóa đơn đã thanh toán.
 * Thao tác thanh toán thực hiện qua Modal Dialog bám sát 100% Figma Lap_hoa_don_Thanh_Toan.jpg.
 */
public class LapHoaDonThanhToanUI extends JPanel {

    private final LapHoaDonThanhToanController controller = new LapHoaDonThanhToanController();
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private final DecimalFormat moneyFmt = new DecimalFormat("#,### đ");

    public static String docSoTienBangChu(long amount) {
        if (amount == 0) return "Không đồng";
        if (amount < 0) return "Âm " + docSoTienBangChu(-amount);

        final String[] chuSo = {"không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"};
        final String[] donVi = {"", "nghìn", "triệu", "tỷ", "nghìn tỷ", "triệu tỷ"};

        long temp = amount;
        List<Integer> blocks = new ArrayList<>();
        while (temp > 0) {
            blocks.add((int) (temp % 1000));
            temp /= 1000;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = blocks.size() - 1; i >= 0; i--) {
            int b = blocks.get(i);
            if (b == 0) continue;

            int tram = b / 100;
            int chuc = (b % 100) / 10;
            int donViNum = b % 10;

            StringBuilder bText = new StringBuilder();
            if (tram > 0 || (i < blocks.size() - 1 && (chuc > 0 || donViNum > 0))) {
                bText.append(chuSo[tram]).append(" trăm ");
            }

            if (chuc > 1) {
                bText.append(chuSo[chuc]).append(" mươi ");
                if (donViNum == 1) {
                    bText.append("mốt ");
                } else if (donViNum == 4) {
                    bText.append("tư ");
                } else if (donViNum == 5) {
                    bText.append("lăm ");
                } else if (donViNum > 0) {
                    bText.append(chuSo[donViNum]).append(" ");
                }
            } else if (chuc == 1) {
                bText.append("mười ");
                if (donViNum == 5) {
                    bText.append("lăm ");
                } else if (donViNum > 0) {
                    bText.append(chuSo[donViNum]).append(" ");
                }
            } else if (chuc == 0 && donViNum > 0) {
                if (tram > 0 || i < blocks.size() - 1) {
                    bText.append("lẻ ");
                }
                bText.append(chuSo[donViNum]).append(" ");
            }

            if (!bText.isEmpty()) {
                sb.append(bText.toString().trim()).append(" ").append(donVi[i]).append(" ");
            }
        }

        String res = sb.toString().trim().replaceAll("\\s+", " ");
        if (res.isEmpty()) return "Không đồng";
        return Character.toUpperCase(res.charAt(0)) + res.substring(1) + " đồng chẵn.";
    }

    private JTabbedPane tabbedPane;

    // ── TAB 1: XE HOÀN TẤT CHỜ THANH TOÁN ──
    private List<PhieuTiepNhanSuaChua> dsChoThanhToan = new ArrayList<>();
    private DefaultTableModel modelChoTT;
    private JTable tblChoTT;
    private JTextField txtTimKiemChoTT;
    private JButton btnThanhToan;
    private JButton btnLamMoiChoTT;
    private JLabel lblSoLuongChoTT;

    // ── TAB 2: LỊCH SỬ HÓA ĐƠN ĐÃ THANH TOÁN ──
    private List<HoaDon> dsLichSuHD = new ArrayList<>();
    private DefaultTableModel modelLichSu;
    private JTable tblLichSu;
    private JTextField txtTimKiemLichSu;
    private JComboBox<String> cboLocPhuongThuc;
    private JButton btnXemChiTietHD;
    private JButton btnLamMoiLichSu;
    private JLabel lblSoLuongLichSu;
    private boolean dangDatLaiBoLocLichSu;

    public LapHoaDonThanhToanUI() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);

        caiDatPhimTat();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. HEADER
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel pnl = new JPanel(new BorderLayout(12, 0));
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(16, 24, 12, 24));

        JPanel titleBlock = new JPanel(new BorderLayout(0, 4));
        titleBlock.setOpaque(false);

        JLabel lblBreadcrumb = new JLabel("Dashboard  /  Hóa đơn và thanh toán");
        lblBreadcrumb.setFont(f(Font.PLAIN, 12));
        lblBreadcrumb.setForeground(C_TEXT_SUBTLE);

        JLabel lblTitle = new JLabel("Quản lý hóa đơn và thanh toán");
        lblTitle.setFont(f(Font.BOLD, 20));
        lblTitle.setForeground(C_TEXT_HEAD);

        JLabel lblSub = new JLabel("Thu ngân, lập hóa đơn cho xe đã hoàn tất sửa chữa và quản lý lịch sử thu tiền của gara.");
        lblSub.setFont(f(Font.PLAIN, 12));
        lblSub.setForeground(C_TEXT_SUBTLE);

        titleBlock.add(lblBreadcrumb, BorderLayout.NORTH);
        titleBlock.add(lblTitle, BorderLayout.CENTER);
        titleBlock.add(lblSub, BorderLayout.SOUTH);
        pnl.add(titleBlock, BorderLayout.WEST);

        return pnl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. TABS ĐIỀU HƯỚNG
    // ─────────────────────────────────────────────────────────────────────────
    private JComponent buildTabs() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(f(Font.BOLD, 13));
        tabbedPane.setBorder(new EmptyBorder(0, 24, 20, 24));

        tabbedPane.addTab("  🚗 Xe hoàn tất chờ thanh toán  ", buildTab1ChoThanhToan());
        tabbedPane.addTab("  🧾 Lịch sử hóa đơn đã thanh toán  ", buildTab2LichSuHoaDon());

        return tabbedPane;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. TAB 1: DANH SÁCH XE CHỜ THANH TOÁN
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTab1ChoThanhToan() {
        JPanel pnl = new JPanel(new BorderLayout(0, 10));
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(12, 0, 0, 0));

        // Toolbar
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(C_WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(10, 14, 10, 14)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        txtTimKiemChoTT = new JTextField();
        txtTimKiemChoTT.setFont(f(Font.PLAIN, 13));
        txtTimKiemChoTT.setPreferredSize(new Dimension(320, 36));
        txtTimKiemChoTT.putClientProperty("JTextField.placeholderText", "Tìm mã phiếu, biển số xe, khách hàng, SĐT...");
        txtTimKiemChoTT.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(4, 10, 4, 10)));
        txtTimKiemChoTT.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    locTab1();
                }
            }
        });
        left.add(txtTimKiemChoTT);

        JButton btnTim = new RoundedButton("Tìm kiếm", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnTim.setFont(f(Font.PLAIN, 12));
        btnTim.setForeground(C_TEXT_MAIN);
        btnTim.setPreferredSize(new Dimension(90, 36));
        btnTim.addActionListener(e -> locTab1());
        left.add(btnTim);

        btnLamMoiChoTT = new RoundedButton("↺ Làm mới", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnLamMoiChoTT.setFont(f(Font.PLAIN, 12));
        btnLamMoiChoTT.setForeground(C_TEXT_MAIN);
        btnLamMoiChoTT.setPreferredSize(new Dimension(95, 36));
        btnLamMoiChoTT.addActionListener(e -> {
            txtTimKiemChoTT.setText("");
            xoaKetQuaTab1();
        });
        left.add(btnLamMoiChoTT);

        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        btnThanhToan = new RoundedButton("💳 Lập hóa đơn & Thanh toán (F2)", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
        btnThanhToan.setFont(f(Font.BOLD, 12));
        btnThanhToan.setForeground(C_WHITE);
        btnThanhToan.setPreferredSize(new Dimension(240, 36));
        btnThanhToan.setEnabled(false);
        btnThanhToan.setVisible(UserSession.getInstance().getVaiTro() == VaiTro.TIEP_NHAN_VIEN);
        btnThanhToan.addActionListener(e -> onMoThanhToan());
        right.add(btnThanhToan);

        bar.add(right, BorderLayout.EAST);
        pnl.add(bar, BorderLayout.NORTH);

        // Bảng dữ liệu
        JPanel cardTable = new JPanel(new BorderLayout(0, 8));
        cardTable.setBackground(C_WHITE);
        cardTable.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel lblT1 = new JLabel("Danh sách các xe đã hoàn tất sửa chữa cần thanh toán");
        lblT1.setFont(f(Font.BOLD, 14));
        lblT1.setForeground(C_TEXT_HEAD);
        head.add(lblT1, BorderLayout.WEST);

        lblSoLuongChoTT = new JLabel("Nhập từ khóa rồi bấm Tìm kiếm");
        lblSoLuongChoTT.setFont(f(Font.PLAIN, 12));
        lblSoLuongChoTT.setForeground(C_TEXT_SUBTLE);
        head.add(lblSoLuongChoTT, BorderLayout.EAST);
        cardTable.add(head, BorderLayout.NORTH);

        String[] cols = {"STT", "Mã phiếu", "Thời gian tiếp nhận", "Biển số xe", "Hãng & Dòng xe", "Khách hàng", "Số điện thoại", "Kỹ thuật viên chính", "Trạng thái phiếu"};
        modelChoTT = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblChoTT = new JTable(modelChoTT);
        tblChoTT.setFont(f(Font.PLAIN, 13));
        tblChoTT.setRowHeight(34);
        tblChoTT.setShowGrid(false);
        tblChoTT.getTableHeader().setFont(f(Font.BOLD, 12));
        tblChoTT.getTableHeader().setBackground(new Color(0xF8FAFC));
        tblChoTT.getTableHeader().setPreferredSize(new Dimension(0, 36));
        tblChoTT.setSelectionBackground(C_PRIMARY_BG);
        tblChoTT.setSelectionForeground(C_TEXT_HEAD);

        tblChoTT.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblChoTT.getColumnModel().getColumn(1).setPreferredWidth(130);
        tblChoTT.getColumnModel().getColumn(2).setPreferredWidth(130);
        tblChoTT.getColumnModel().getColumn(3).setPreferredWidth(100);
        tblChoTT.getColumnModel().getColumn(4).setPreferredWidth(140);
        tblChoTT.getColumnModel().getColumn(5).setPreferredWidth(140);
        tblChoTT.getColumnModel().getColumn(6).setPreferredWidth(100);
        tblChoTT.getColumnModel().getColumn(7).setPreferredWidth(130);
        tblChoTT.getColumnModel().getColumn(8).setPreferredWidth(160);

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        tblChoTT.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        // Renderer cho cột trạng thái
        tblChoTT.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = new JLabel("Hoàn tất · Chờ thu tiền", SwingConstants.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
                lbl.setOpaque(true);
                lbl.setBackground(new Color(0xD1FAE5));
                lbl.setForeground(new Color(0x065F46));
                lbl.setBorder(BorderFactory.createCompoundBorder(
                        new EmptyBorder(3, 8, 3, 8),
                        BorderFactory.createLineBorder(new Color(0xA7F3D0), 1)
                ));

                JPanel wrapper = new JPanel(new GridBagLayout());
                wrapper.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                wrapper.add(lbl);
                return wrapper;
            }
        });

        tblChoTT.getSelectionModel().addListSelectionListener(e -> {
            boolean coChon = tblChoTT.getSelectedRow() >= 0;
            btnThanhToan.setEnabled(coChon);
        });

        tblChoTT.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tblChoTT.getSelectedRow() >= 0) {
                    onMoThanhToan();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tblChoTT);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        cardTable.add(scroll, BorderLayout.CENTER);

        pnl.add(cardTable, BorderLayout.CENTER);
        return pnl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. TAB 2: LỊCH SỬ HÓA ĐƠN ĐÃ THANH TOÁN
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTab2LichSuHoaDon() {
        JPanel pnl = new JPanel(new BorderLayout(0, 10));
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(12, 0, 0, 0));

        // Toolbar
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(C_WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(10, 14, 10, 14)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        txtTimKiemLichSu = new JTextField();
        txtTimKiemLichSu.setFont(f(Font.PLAIN, 13));
        txtTimKiemLichSu.setPreferredSize(new Dimension(280, 36));
        txtTimKiemLichSu.putClientProperty("JTextField.placeholderText", "Tìm mã hóa đơn, mã phiếu, biển số, khách hàng...");
        txtTimKiemLichSu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(4, 10, 4, 10)));
        txtTimKiemLichSu.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    locTab2();
                }
            }
        });
        left.add(txtTimKiemLichSu);

        cboLocPhuongThuc = new JComboBox<>(new String[]{"-- Tất cả phương thức --", "Tiền mặt (TIEN_MAT)", "Chuyển khoản (CHUYEN_KHOAN)"});
        cboLocPhuongThuc.setFont(f(Font.PLAIN, 13));
        cboLocPhuongThuc.setPreferredSize(new Dimension(200, 36));
        cboLocPhuongThuc.addActionListener(e -> {
            if (!dangDatLaiBoLocLichSu) locTab2();
        });
        left.add(cboLocPhuongThuc);

        JButton btnTim = new RoundedButton("Tìm kiếm", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnTim.setFont(f(Font.PLAIN, 12));
        btnTim.setForeground(C_TEXT_MAIN);
        btnTim.setPreferredSize(new Dimension(90, 36));
        btnTim.addActionListener(e -> locTab2());
        left.add(btnTim);

        btnLamMoiLichSu = new RoundedButton("↺ Làm mới", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnLamMoiLichSu.setFont(f(Font.PLAIN, 12));
        btnLamMoiLichSu.setForeground(C_TEXT_MAIN);
        btnLamMoiLichSu.setPreferredSize(new Dimension(95, 36));
        btnLamMoiLichSu.addActionListener(e -> {
            dangDatLaiBoLocLichSu = true;
            txtTimKiemLichSu.setText("");
            cboLocPhuongThuc.setSelectedIndex(0);
            dangDatLaiBoLocLichSu = false;
            xoaKetQuaTab2();
        });
        left.add(btnLamMoiLichSu);

        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        btnXemChiTietHD = new RoundedButton("📄 Xem chi tiết hóa đơn", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnXemChiTietHD.setFont(f(Font.PLAIN, 12));
        btnXemChiTietHD.setForeground(C_TEXT_MAIN);
        btnXemChiTietHD.setPreferredSize(new Dimension(170, 36));
        btnXemChiTietHD.setEnabled(false);
        btnXemChiTietHD.addActionListener(e -> onXemChiTietHD());
        right.add(btnXemChiTietHD);

        bar.add(right, BorderLayout.EAST);
        pnl.add(bar, BorderLayout.NORTH);

        // Bảng dữ liệu
        JPanel cardTable = new JPanel(new BorderLayout(0, 8));
        cardTable.setBackground(C_WHITE);
        cardTable.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel lblT2 = new JLabel("Lịch sử các hóa đơn đã thanh toán thành công");
        lblT2.setFont(f(Font.BOLD, 14));
        lblT2.setForeground(C_TEXT_HEAD);
        head.add(lblT2, BorderLayout.WEST);

        lblSoLuongLichSu = new JLabel("Nhập từ khóa hoặc chọn phương thức để xem lịch sử");
        lblSoLuongLichSu.setFont(f(Font.PLAIN, 12));
        lblSoLuongLichSu.setForeground(C_TEXT_SUBTLE);
        head.add(lblSoLuongLichSu, BorderLayout.EAST);
        cardTable.add(head, BorderLayout.NORTH);

        String[] cols = {"STT", "Mã HĐ", "Mã phiếu", "Thời gian thanh toán", "Khách hàng", "Biển số xe", "Tiền dịch vụ", "Tiền linh kiện", "Giảm giá", "Thuế VAT", "Tổng thanh toán", "Phương thức"};
        modelLichSu = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblLichSu = new JTable(modelLichSu);
        tblLichSu.setFont(f(Font.PLAIN, 13));
        tblLichSu.setRowHeight(34);
        tblLichSu.setShowGrid(false);
        tblLichSu.getTableHeader().setFont(f(Font.BOLD, 12));
        tblLichSu.getTableHeader().setBackground(new Color(0xF8FAFC));
        tblLichSu.getTableHeader().setPreferredSize(new Dimension(0, 36));
        tblLichSu.setSelectionBackground(C_PRIMARY_BG);
        tblLichSu.setSelectionForeground(C_TEXT_HEAD);

        tblLichSu.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblLichSu.getColumnModel().getColumn(1).setPreferredWidth(120);
        tblLichSu.getColumnModel().getColumn(2).setPreferredWidth(120);
        tblLichSu.getColumnModel().getColumn(3).setPreferredWidth(130);
        tblLichSu.getColumnModel().getColumn(4).setPreferredWidth(130);
        tblLichSu.getColumnModel().getColumn(5).setPreferredWidth(95);
        tblLichSu.getColumnModel().getColumn(6).setPreferredWidth(100);
        tblLichSu.getColumnModel().getColumn(7).setPreferredWidth(100);
        tblLichSu.getColumnModel().getColumn(8).setPreferredWidth(85);
        tblLichSu.getColumnModel().getColumn(9).setPreferredWidth(85);
        tblLichSu.getColumnModel().getColumn(10).setPreferredWidth(120);
        tblLichSu.getColumnModel().getColumn(11).setPreferredWidth(110);

        DefaultTableCellRenderer centerRender2 = new DefaultTableCellRenderer();
        centerRender2.setHorizontalAlignment(SwingConstants.CENTER);
        tblLichSu.getColumnModel().getColumn(0).setCellRenderer(centerRender2);

        // Canh phải cho tiền
        DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
        rightRender.setHorizontalAlignment(SwingConstants.RIGHT);
        tblLichSu.getColumnModel().getColumn(6).setCellRenderer(rightRender);
        tblLichSu.getColumnModel().getColumn(7).setCellRenderer(rightRender);
        tblLichSu.getColumnModel().getColumn(8).setCellRenderer(rightRender);
        tblLichSu.getColumnModel().getColumn(9).setCellRenderer(rightRender);

        // Renderer cho Tổng thanh toán (in đậm, màu primary)
        tblLichSu.getColumnModel().getColumn(10).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setFont(new Font("Segoe UI", Font.BOLD, 13));
                l.setForeground(isSelected ? table.getSelectionForeground() : C_PRIMARY);
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                return l;
            }
        });

        // Renderer phương thức thanh toán
        tblLichSu.getColumnModel().getColumn(11).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                String val = value != null ? value.toString() : "";
                boolean isTM = val.contains("Tiền mặt") || val.contains("TIEN_MAT");

                JLabel lbl = new JLabel(isTM ? "💵 Tiền mặt" : "🏦 Chuyển khoản", SwingConstants.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
                lbl.setOpaque(true);
                lbl.setBackground(isTM ? new Color(0xD1FAE5) : new Color(0xDBEAFE));
                lbl.setForeground(isTM ? new Color(0x065F46) : new Color(0x1E40AF));
                lbl.setBorder(BorderFactory.createCompoundBorder(
                        new EmptyBorder(3, 8, 3, 8),
                        BorderFactory.createLineBorder(isTM ? new Color(0xA7F3D0) : new Color(0xBFDBFE), 1)
                ));

                JPanel wrapper = new JPanel(new GridBagLayout());
                wrapper.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                wrapper.add(lbl);
                return wrapper;
            }
        });

        tblLichSu.getSelectionModel().addListSelectionListener(e -> {
            boolean coChon = tblLichSu.getSelectedRow() >= 0;
            btnXemChiTietHD.setEnabled(coChon);
        });

        tblLichSu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tblLichSu.getSelectedRow() >= 0) {
                    onXemChiTietHD();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tblLichSu);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        cardTable.add(scroll, BorderLayout.CENTER);

        pnl.add(cardTable, BorderLayout.CENTER);
        return pnl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. TẢI DỮ LIỆU & LỌC
    // ─────────────────────────────────────────────────────────────────────────
    public void taiDuLieuTab1() {
        lblSoLuongChoTT.setText("Đang tải...");
        new SwingWorker<List<PhieuTiepNhanSuaChua>, Void>() {
            @Override
            protected List<PhieuTiepNhanSuaChua> doInBackground() {
                return controller.layDanhSachPhieuDuDieuKien();
            }

            @Override
            protected void done() {
                try {
                    dsChoThanhToan = get();
                    capNhatBangTab1(dsChoThanhToan);
                } catch (Exception ex) {
                    lblSoLuongChoTT.setText("Lỗi: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void locTab1() {
        String kw = txtTimKiemChoTT.getText().trim().toLowerCase();
        lblSoLuongChoTT.setText("Đang tìm kiếm...");
        new SwingWorker<List<PhieuTiepNhanSuaChua>, Void>() {
            @Override
            protected List<PhieuTiepNhanSuaChua> doInBackground() {
                return controller.layDanhSachPhieuDuDieuKien();
            }

            @Override
            protected void done() {
                try {
                    List<PhieuTiepNhanSuaChua> ketQua = get();
                    dsChoThanhToan = ketQua;
                    if (!kw.isEmpty()) {
                        ketQua = ketQua.stream().filter(p -> {
                            Xe xe = p.getXe();
                            KhachHang kh = xe != null ? xe.getChuXe() : null;
                            String maPhieu = p.getMaPhieu() != null ? p.getMaPhieu().toLowerCase() : "";
                            String bienSo = xe != null && xe.getBienSoXe() != null ? xe.getBienSoXe().toLowerCase() : "";
                            String tenKh = kh != null && kh.getHoTen() != null ? kh.getHoTen().toLowerCase() : "";
                            String sdt = kh != null && kh.getSoDienThoai() != null ? kh.getSoDienThoai() : "";
                            return maPhieu.contains(kw) || bienSo.contains(kw) || tenKh.contains(kw) || sdt.contains(kw);
                        }).toList();
                    }
                    capNhatBangTab1(ketQua);
                } catch (Exception ex) {
                    lblSoLuongChoTT.setText("Lỗi tìm kiếm: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void xoaKetQuaTab1() {
        dsChoThanhToan = List.of();
        modelChoTT.setRowCount(0);
        lblSoLuongChoTT.setText("Nhập từ khóa rồi bấm Tìm kiếm");
        btnThanhToan.setEnabled(false);
    }

    private void capNhatBangTab1(List<PhieuTiepNhanSuaChua> list) {
        modelChoTT.setRowCount(0);
        int stt = 1;
        for (PhieuTiepNhanSuaChua p : list) {
            Xe xe = p.getXe();
            KhachHang kh = xe != null ? xe.getChuXe() : null;
            String xeInfo = (xe != null && xe.getHangXe() != null ? xe.getHangXe() + " " : "") +
                            (xe != null && xe.getModel() != null ? xe.getModel() : "--");

            modelChoTT.addRow(new Object[]{
                    stt++,
                    p.getMaPhieu(),
                    p.getNgayGioTiepNhan() != null ? timeFmt.format(p.getNgayGioTiepNhan()) : "--",
                    xe != null ? xe.getBienSoXe() : "--",
                    xeInfo,
                    kh != null ? kh.getHoTen() : "--",
                    kh != null ? kh.getSoDienThoai() : "--",
                    p.getNhanVienSuaChua() != null ? p.getNhanVienSuaChua().getHoTen() : "-- Chưa phân công --",
                    "HOAN_TAT"
            });
        }
        lblSoLuongChoTT.setText("Có " + list.size() + " xe hoàn tất chờ thanh toán");
        btnThanhToan.setEnabled(false);
    }

    public void taiDuLieuTab2() {
        lblSoLuongLichSu.setText("Đang tải...");
        new SwingWorker<List<HoaDon>, Void>() {
            @Override
            protected List<HoaDon> doInBackground() {
                return controller.layDanhSachHoaDonDaThanhToan();
            }

            @Override
            protected void done() {
                try {
                    dsLichSuHD = get();
                    capNhatBangTab2(dsLichSuHD);
                } catch (Exception ex) {
                    lblSoLuongLichSu.setText("Lỗi: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void locTab2() {
        String kw = txtTimKiemLichSu.getText().trim();
        int idxPt = cboLocPhuongThuc.getSelectedIndex();
        PhuongThucThanhToan pt = switch (idxPt) {
            case 1 -> PhuongThucThanhToan.TIEN_MAT;
            case 2 -> PhuongThucThanhToan.CHUYEN_KHOAN;
            default -> null;
        };

        lblSoLuongLichSu.setText("Đang lọc...");
        new SwingWorker<List<HoaDon>, Void>() {
            @Override
            protected List<HoaDon> doInBackground() {
                return controller.timKiemHoaDon(kw, TrangThaiThanhToan.DA_THANH_TOAN);
            }

            @Override
            protected void done() {
                try {
                    List<HoaDon> list = get();
                    if (pt != null) {
                        list = list.stream().filter(h -> h.getPhuongThucTT() == pt).toList();
                    }
                    dsLichSuHD = list;
                    capNhatBangTab2(list);
                } catch (Exception ex) {
                    lblSoLuongLichSu.setText("Lỗi: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void capNhatBangTab2(List<HoaDon> list) {
        modelLichSu.setRowCount(0);
        int stt = 1;
        for (HoaDon h : list) {
            PhieuTiepNhanSuaChua p = h.getPhieuSuaChua();
            Xe xe = p != null ? p.getXe() : null;
            KhachHang kh = xe != null ? xe.getChuXe() : null;

            modelLichSu.addRow(new Object[]{
                    stt++,
                    h.getMaHoaDon(),
                    p != null ? p.getMaPhieu() : "--",
                    h.getThoiGianThanhToan() != null ? timeFmt.format(h.getThoiGianThanhToan()) : (h.getNgayLap() != null ? timeFmt.format(h.getNgayLap()) : "--"),
                    kh != null ? kh.getHoTen() : "--",
                    xe != null ? xe.getBienSoXe() : "--",
                    moneyFmt.format(h.getTongTienDichVu()),
                    moneyFmt.format(h.getTongTienLinhKien()),
                    moneyFmt.format(h.getTienGiam()),
                    moneyFmt.format(h.getThue()),
                    moneyFmt.format(h.getTongThanhToan()),
                    h.getPhuongThucTT() != null ? h.getPhuongThucTT().name() : "TIEN_MAT"
            });
        }
        lblSoLuongLichSu.setText("Tổng cộng: " + list.size() + " hóa đơn đã thanh toán");
        btnXemChiTietHD.setEnabled(false);
    }

    private void xoaKetQuaTab2() {
        dsLichSuHD = List.of();
        modelLichSu.setRowCount(0);
        lblSoLuongLichSu.setText("Nhập từ khóa hoặc chọn phương thức để xem lịch sử");
        btnXemChiTietHD.setEnabled(false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. PHÍM TẮT & SỰ KIỆN MỞ MODAL
    // ─────────────────────────────────────────────────────────────────────────
    private void caiDatPhimTat() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "thanhToanF2");
        am.put("thanhToanF2", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (tabbedPane.getSelectedIndex() == 0 && tblChoTT.getSelectedRow() >= 0) {
                    onMoThanhToan();
                }
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "lamMoiF5");
        am.put("lamMoiF5", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (tabbedPane.getSelectedIndex() == 0) {
                    if (txtTimKiemChoTT.getText().isBlank()) xoaKetQuaTab1();
                    else locTab1();
                } else if (txtTimKiemLichSu.getText().isBlank() && cboLocPhuongThuc.getSelectedIndex() == 0) {
                    xoaKetQuaTab2();
                } else {
                    locTab2();
                }
            }
        });
    }

    private void onMoThanhToan() {
        if (UserSession.getInstance().getVaiTro() != VaiTro.TIEP_NHAN_VIEN) return;
        int row = tblChoTT.getSelectedRow();
        if (row < 0 || row >= dsChoThanhToan.size()) return;
        PhieuTiepNhanSuaChua phieu = dsChoThanhToan.get(row);

        Window parent = SwingUtilities.getWindowAncestor(this);
        ThanhToanHoaDonModalDialog dlg = new ThanhToanHoaDonModalDialog(parent, phieu);
        dlg.setVisible(true);

        if (dlg.isDaThanhToanThanhCong()) {
            locTab1();
            tabbedPane.setSelectedIndex(1); // Chuyển sang Tab lịch sử
            if (dlg.getMaHoaDonThanhCong() != null) {
                txtTimKiemLichSu.setText(dlg.getMaHoaDonThanhCong());
                locTab2();
                ChiTietHoaDonModalDialog dlgHD = new ChiTietHoaDonModalDialog(parent, dlg.getMaHoaDonThanhCong());
                dlgHD.setVisible(true);
            }
        }
    }

    private void onXemChiTietHD() {
        int row = tblLichSu.getSelectedRow();
        if (row < 0 || row >= dsLichSuHD.size()) return;
        HoaDon hd = dsLichSuHD.get(row);

        Window parent = SwingUtilities.getWindowAncestor(this);
        ChiTietHoaDonModalDialog dlg = new ChiTietHoaDonModalDialog(parent, hd.getMaHoaDon());
        dlg.setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. MODAL DIALOG: THANH TOÁN HÓA ĐƠN (BÁM SÁT 100% FIGMA Lap_hoa_don_Thanh_Toan.jpg)
    // ─────────────────────────────────────────────────────────────────────────
    private class ThanhToanHoaDonModalDialog extends JDialog {
        private final PhieuTiepNhanSuaChua phieu;
        private boolean daThanhToanThanhCong = false;
        private String maHoaDonThanhCong = null;

        public String getMaHoaDonThanhCong() {
            return maHoaDonThanhCong;
        }

        private List<KhuyenMai> dsKmHieuLuc = new ArrayList<>();
        private List<ChiTietDichVu> dsDV = new ArrayList<>();
        private List<ChiTietLinhKien> dsLK = new ArrayList<>();

        // Components
        private JTextField txtMaHD;
        private JTextField txtMaPhieu;
        private JTextField txtTrangThai;
        private JTextField txtKhachHang;
        private JTextField txtSdt;
        private JTextField txtBienSo;

        private DefaultTableModel modelChiTiet;
        private JTable tblChiTiet;

        private JComboBox<String> cboKhuyenMai;
        private JLabel lblTongDV;
        private JLabel lblTongLK;
        private JLabel lblGiamGia;
        private JLabel lblThue;
        private JLabel lblTongThanhToan;

        private JComboBox<String> cboPhuongThuc;
        private JTextField txtMaGiaoDich;
        private JLabel lblCanhBao;
        private JButton btnXacNhanThanhToan;

        // Số liệu tính toán
        private double tongTienDV = 0;
        private double tongTienLK = 0;
        private double tienGiam = 0;
        private double tienThue = 0;
        private double tongPhaiThu = 0;

        public ThanhToanHoaDonModalDialog(Window parent, PhieuTiepNhanSuaChua phieu) {
            super(parent, "Lập hóa đơn và thanh toán – " + phieu.getMaPhieu(), ModalityType.APPLICATION_MODAL);
            this.phieu = phieu;

            setLayout(new BorderLayout());
            getContentPane().setBackground(C_WHITE);
            setResizable(true);

            add(buildModalHeader(), BorderLayout.NORTH);
            add(buildModalBody(), BorderLayout.CENTER);
            add(buildModalFooter(), BorderLayout.SOUTH);

            // Mở rộng toàn màn hình làm việc (trừ Taskbar)
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Rectangle bounds = ge.getMaximumWindowBounds();
            setBounds(bounds.x + 10, bounds.y + 10, bounds.width - 20, bounds.height - 20);

            napDuLieuChiTiet();
        }

        public boolean isDaThanhToanThanhCong() { return daThanhToanThanhCong; }

        private JPanel buildModalHeader() {
            JPanel pnl = new JPanel(new BorderLayout());
            pnl.setBackground(C_WHITE);
            pnl.setBorder(new EmptyBorder(16, 24, 12, 24));

            JLabel lblTitle = new JLabel("Lập hóa đơn thanh toán");
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

            // KHỐI 1: THÔNG TIN CHUNG (Theo Figma)
            JPanel card1 = taoCardNhom("Thông tin hóa đơn & Khách hàng");
            JPanel pnlC1 = new JPanel();
            pnlC1.setLayout(new BoxLayout(pnlC1, BoxLayout.Y_AXIS));
            pnlC1.setOpaque(false);

            // Hàng 1: Mã hóa đơn | Mã phiếu | Trạng thái
            JPanel row1 = new JPanel(new GridLayout(1, 3, 10, 0));
            row1.setOpaque(false);
            row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            row1.setAlignmentX(0.0f);

            txtMaHD = taoFieldReadOnly("Chưa được cấp mã · Chưa lưu");
            row1.add(wrapFieldModal("Mã hóa đơn — tự sinh · Chỉ đọc", txtMaHD));

            txtMaPhieu = taoFieldReadOnly(phieu.getMaPhieu());
            row1.add(wrapFieldModal("Mã phiếu sửa chữa · Chỉ đọc", txtMaPhieu));

            txtTrangThai = taoFieldReadOnly("Chờ thanh toán");
            row1.add(wrapFieldModal("Trạng thái thanh toán · Chỉ đọc", txtTrangThai));

            pnlC1.add(row1);
            pnlC1.add(Box.createVerticalStrut(10));

            // Hàng 2: Khách hàng | Số điện thoại | Biển số xe
            JPanel row2 = new JPanel(new GridLayout(1, 3, 10, 0));
            row2.setOpaque(false);
            row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            row2.setAlignmentX(0.0f);

            Xe xe = phieu.getXe();
            KhachHang kh = xe != null ? xe.getChuXe() : null;

            txtKhachHang = taoFieldReadOnly(kh != null ? kh.getHoTen() : "--");
            row2.add(wrapFieldModal("Khách hàng · Chỉ đọc", txtKhachHang));

            txtSdt = taoFieldReadOnly(kh != null ? kh.getSoDienThoai() : "--");
            row2.add(wrapFieldModal("Số điện thoại · Chỉ đọc", txtSdt));

            String bienSoInfo = (xe != null ? xe.getBienSoXe() : "--") + " (" + (xe != null && xe.getHangXe() != null ? xe.getHangXe() + " " + xe.getModel() : "") + ")";
            txtBienSo = taoFieldReadOnly(bienSoInfo);
            row2.add(wrapFieldModal("Biển số / xe · Chỉ đọc", txtBienSo));

            pnlC1.add(row2);
            card1.add(pnlC1, BorderLayout.CENTER);
            pnl.add(card1);
            pnl.add(Box.createVerticalStrut(12));

            // KHỐI 2: BẢNG CHI TIẾT DỊCH VỤ & LINH KIỆN
            JPanel card2 = taoCardNhom("Chi tiết dịch vụ đã thực hiện & linh kiện thay thế");
            JPanel pnlC2 = new JPanel(new BorderLayout(0, 6));
            pnlC2.setOpaque(false);

            String[] cols = {"Loại", "Mã", "Tên", "Đơn vị", "Số lượng", "Đơn giá", "Thành tiền"};
            modelChiTiet = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            tblChiTiet = new JTable(modelChiTiet);
            tblChiTiet.setFont(f(Font.PLAIN, 13));
            tblChiTiet.setRowHeight(28);
            tblChiTiet.setShowGrid(false);
            tblChiTiet.getTableHeader().setFont(f(Font.BOLD, 12));
            tblChiTiet.getTableHeader().setBackground(new Color(0xF8FAFC));

            // Canh phải cho đơn giá & thành tiền
            DefaultTableCellRenderer rRight = new DefaultTableCellRenderer();
            rRight.setHorizontalAlignment(SwingConstants.RIGHT);
            tblChiTiet.getColumnModel().getColumn(5).setCellRenderer(rRight);
            tblChiTiet.getColumnModel().getColumn(6).setCellRenderer(rRight);

            JScrollPane scrollChiTiet = new JScrollPane(tblChiTiet);
            scrollChiTiet.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
            scrollChiTiet.setPreferredSize(new Dimension(0, 160));
            pnlC2.add(scrollChiTiet, BorderLayout.CENTER);

            card2.add(pnlC2, BorderLayout.CENTER);
            pnl.add(card2);
            pnl.add(Box.createVerticalStrut(12));

            // KHỐI 3: KHUYẾN MÃI & TÍNH TOÁN TỔNG TIỀN (Chuẩn Figma)
            JPanel card3 = taoCardNhom("Khuyến mãi & Tổng thanh toán");
            JPanel pnlC3 = new JPanel(new GridLayout(1, 2, 24, 0));
            pnlC3.setOpaque(false);

            // Cột trái: Chọn khuyến mãi
            JPanel pnlKmWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
            pnlKmWrap.setOpaque(false);
            JLabel lblKmTitle = new JLabel("Chương trình khuyến mãi áp dụng (tối đa 1):");
            lblKmTitle.setFont(f(Font.BOLD, 12));
            lblKmTitle.setForeground(C_TEXT_MAIN);
            pnlKmWrap.add(lblKmTitle);

            cboKhuyenMai = new JComboBox<>();
            cboKhuyenMai.setFont(f(Font.PLAIN, 13));
            cboKhuyenMai.setPreferredSize(new Dimension(360, 36));
            cboKhuyenMai.addActionListener(e -> tinhToanLaiTien());
            pnlKmWrap.add(cboKhuyenMai);
            pnlC3.add(pnlKmWrap);

            // Cột phải: Bảng số liệu tiền (Tổng DV, Tổng LK, Giảm giá, Thuế, Tổng thanh toán)
            JPanel pnlTien = new JPanel(new GridLayout(5, 2, 10, 6));
            pnlTien.setOpaque(false);

            pnlTien.add(taoLabelTien("Tổng tiền dịch vụ:"));
            lblTongDV = taoValTien("0 đ");
            pnlTien.add(lblTongDV);

            pnlTien.add(taoLabelTien("Tổng tiền linh kiện:"));
            lblTongLK = taoValTien("0 đ");
            pnlTien.add(lblTongLK);

            pnlTien.add(taoLabelTien("Giảm giá khuyến mãi:"));
            lblGiamGia = taoValTien("0 đ");
            lblGiamGia.setForeground(C_SUCCESS);
            pnlTien.add(lblGiamGia);

            pnlTien.add(taoLabelTien("Thuế VAT:"));
            lblThue = taoValTien("0 đ");
            pnlTien.add(lblThue);

            JLabel lblTongPhaiThuHead = new JLabel("TỔNG THANH TOÁN:");
            lblTongPhaiThuHead.setFont(f(Font.BOLD, 14));
            lblTongPhaiThuHead.setForeground(C_TEXT_HEAD);
            pnlTien.add(lblTongPhaiThuHead);

            lblTongThanhToan = new JLabel("0 đ");
            lblTongThanhToan.setFont(f(Font.BOLD, 18));
            lblTongThanhToan.setForeground(C_PRIMARY);
            pnlTien.add(lblTongThanhToan);

            pnlC3.add(pnlTien);
            card3.add(pnlC3, BorderLayout.CENTER);
            pnl.add(card3);
            pnl.add(Box.createVerticalStrut(12));

            // KHỐI 4: PHƯƠNG THỨC THANH TOÁN (Tiền mặt / Chuyển khoản)
            JPanel card4 = taoCardNhom("Hình thức thu ngân");
            JPanel pnlC4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
            pnlC4.setOpaque(false);

            pnlC4.add(new JLabel("Phương thức thanh toán:"));
            cboPhuongThuc = new JComboBox<>(new String[]{"💵 Tiền mặt", "🏦 Chuyển khoản ngân hàng"});
            cboPhuongThuc.setFont(f(Font.BOLD, 13));
            cboPhuongThuc.setPreferredSize(new Dimension(220, 36));
            pnlC4.add(cboPhuongThuc);

            JLabel lblMgd = new JLabel("Mã giao dịch ngân hàng (bắt buộc nếu CK):");
            pnlC4.add(lblMgd);

            txtMaGiaoDich = new JTextField();
            txtMaGiaoDich.setFont(f(Font.PLAIN, 13));
            txtMaGiaoDich.setPreferredSize(new Dimension(240, 36));
            txtMaGiaoDich.setEnabled(false);
            txtMaGiaoDich.putClientProperty("JTextField.placeholderText", "VD: VCB123456789");
            txtMaGiaoDich.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER, 1),
                    new EmptyBorder(4, 8, 4, 8)));
            pnlC4.add(txtMaGiaoDich);

            cboPhuongThuc.addActionListener(e -> {
                boolean isCK = cboPhuongThuc.getSelectedIndex() == 1;
                txtMaGiaoDich.setEnabled(isCK);
                if (isCK) {
                    txtMaGiaoDich.requestFocusInWindow();
                } else {
                    txtMaGiaoDich.setText("");
                }
            });

            card4.add(pnlC4, BorderLayout.CENTER);
            pnl.add(card4);

            JScrollPane mainScroll = new JScrollPane(pnl);
            mainScroll.setBorder(null);
            mainScroll.getVerticalScrollBar().setUnitIncrement(16);
            return mainScroll;
        }

        private JPanel buildModalFooter() {
            JPanel pnl = new JPanel(new BorderLayout(10, 0));
            pnl.setBackground(new Color(0xF8FAFC));
            pnl.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER),
                    new EmptyBorder(12, 24, 12, 24)));

            lblCanhBao = new JLabel("Kiểm tra đầy đủ số tiền và phương thức trước khi xác nhận thu tiền.");
            lblCanhBao.setFont(f(Font.ITALIC, 12));
            lblCanhBao.setForeground(C_TEXT_SUBTLE);
            pnl.add(lblCanhBao, BorderLayout.WEST);

            JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            btnGroup.setOpaque(false);

            JButton btnHuy = new RoundedButton("Hủy", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
            btnHuy.setFont(f(Font.PLAIN, 12));
            btnHuy.setForeground(C_TEXT_MAIN);
            btnHuy.setPreferredSize(new Dimension(85, 34));
            btnHuy.addActionListener(e -> dispose());
            btnGroup.add(btnHuy);

            btnXacNhanThanhToan = new RoundedButton("✓ Xác nhận thanh toán", 8, C_SUCCESS, new Color(0x047857), C_SUCCESS);
            btnXacNhanThanhToan.setFont(f(Font.BOLD, 13));
            btnXacNhanThanhToan.setForeground(C_WHITE);
            btnXacNhanThanhToan.setPreferredSize(new Dimension(190, 36));
            btnXacNhanThanhToan.addActionListener(e -> thucHienThanhToan());
            btnGroup.add(btnXacNhanThanhToan);

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

        private JTextField taoFieldReadOnly(String text) {
            JTextField tf = new JTextField(text);
            tf.setFont(f(Font.PLAIN, 13));
            tf.setEditable(false);
            tf.setBackground(new Color(0xF8FAFC));
            tf.setForeground(C_TEXT_MAIN);
            tf.setPreferredSize(new Dimension(0, 34));
            tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER, 1),
                    new EmptyBorder(4, 8, 4, 8)));
            return tf;
        }

        private JLabel taoLabelTien(String text) {
            JLabel l = new JLabel(text);
            l.setFont(f(Font.PLAIN, 13));
            l.setForeground(C_TEXT_MAIN);
            return l;
        }

        private JLabel taoValTien(String text) {
            JLabel l = new JLabel(text);
            l.setFont(f(Font.BOLD, 13));
            l.setForeground(C_TEXT_HEAD);
            l.setHorizontalAlignment(SwingConstants.RIGHT);
            return l;
        }

        private void napDuLieuChiTiet() {
            new SwingWorker<Void, Void>() {
                List<ChiTietDichVu> dvs;
                List<ChiTietLinhKien> lks;
                List<KhuyenMai> kms;
                HoaDon hdCu;

                @Override
                protected Void doInBackground() {
                    dvs = controller.layChiTietDichVu(phieu.getMaPhieu());
                    lks = controller.layChiTietLinhKien(phieu.getMaPhieu());
                    kms = controller.layKhuyenMaiHieuLuc();
                    hdCu = controller.timHoaDonTheoPhieu(phieu.getMaPhieu());
                    return null;
                }

                @Override
                protected void done() {
                    dsDV = dvs != null ? dvs : List.of();
                    dsLK = lks != null ? lks : List.of();
                    dsKmHieuLuc = kms != null ? kms : List.of();

                    if (hdCu != null) {
                        txtMaHD.setText(hdCu.getMaHoaDon());
                    }

                    cboKhuyenMai.removeAllItems();
                    cboKhuyenMai.addItem("-- Không áp dụng khuyến mãi --");
                    for (KhuyenMai km : dsKmHieuLuc) {
                        cboKhuyenMai.addItem(km.getMaKM() + " - " + km.getTenChuongTrinh() + " (Giảm " + (int)km.getMucGiamPhanTram() + "%)");
                    }

                    // Đổ dữ liệu bảng
                    modelChiTiet.setRowCount(0);
                    tongTienDV = 0;
                    tongTienLK = 0;

                    for (ChiTietDichVu ctdv : dsDV) {
                        DichVu dv = ctdv.getDichVu();
                        double tt = ctdv.getDonGia() * ctdv.getSoLuong();
                        tongTienDV += tt;
                        modelChiTiet.addRow(new Object[]{
                                "Dịch vụ",
                                dv != null ? dv.getMaDV() : "--",
                                dv != null ? dv.getTenDV() : "--",
                                dv != null && dv.getDonViTinh() != null ? dv.getDonViTinh() : "Lần",
                                ctdv.getSoLuong(),
                                moneyFmt.format(ctdv.getDonGia()),
                                moneyFmt.format(tt)
                        });
                    }

                    for (ChiTietLinhKien ctlk : dsLK) {
                        LinhKien lk = ctlk.getLinhKien();
                        double tt = ctlk.getDonGia() * ctlk.getSoLuong();
                        tongTienLK += tt;
                        modelChiTiet.addRow(new Object[]{
                                "Linh kiện",
                                lk != null ? lk.getMaLK() : "--",
                                lk != null ? lk.getTenLK() : "--",
                                lk != null && lk.getDonViTinh() != null ? lk.getDonViTinh() : "Cái",
                                ctlk.getSoLuong(),
                                moneyFmt.format(ctlk.getDonGia()),
                                moneyFmt.format(tt)
                        });
                    }

                    lblTongDV.setText(moneyFmt.format(tongTienDV));
                    lblTongLK.setText(moneyFmt.format(tongTienLK));

                    tinhToanLaiTien();
                }
            }.execute();
        }

        private void tinhToanLaiTien() {
            // Tính thuế VAT theo luật CarZone:
            // - Chỉ linh kiện: 1.5% tongTienLinhKien
            // - Chỉ dịch vụ: 7% tongTienDichVu
            // - Cả hai: 4.5% (tongTienDichVu + tongTienLinhKien)
            if (tongTienDV > 0 && tongTienLK > 0) {
                tienThue = Math.round((tongTienDV + tongTienLK) * 0.045);
            } else if (tongTienDV > 0) {
                tienThue = Math.round(tongTienDV * 0.07);
            } else if (tongTienLK > 0) {
                tienThue = Math.round(tongTienLK * 0.015);
            } else {
                tienThue = 0;
            }

            // Tính tiền giảm khuyến mãi
            int idxKm = cboKhuyenMai.getSelectedIndex();
            tienGiam = 0;
            if (idxKm > 0 && idxKm - 1 < dsKmHieuLuc.size()) {
                KhuyenMai km = dsKmHieuLuc.get(idxKm - 1);
                double tongTienApDung = 0;
                if (km.apDungTatCa()) {
                    tongTienApDung = tongTienDV + tongTienLK;
                } else {
                    for (ChiTietDichVu ctdv : dsDV) {
                        DichVu dv = ctdv.getDichVu();
                        if (dv != null && km.getDichVuApDung().stream()
                                .anyMatch(dvApDung -> dv.getMaDV().equals(dvApDung.getMaDV()))) {
                            tongTienApDung += ctdv.getDonGia() * ctdv.getSoLuong();
                        }
                    }
                    for (ChiTietLinhKien ctlk : dsLK) {
                        LinhKien lk = ctlk.getLinhKien();
                        if (lk != null && km.getLinhKienApDung().stream()
                                .anyMatch(lkApDung -> lk.getMaLK().equals(lkApDung.getMaLK()))) {
                            tongTienApDung += ctlk.getDonGia() * ctlk.getSoLuong();
                        }
                    }
                }
                tienGiam = Math.round(tongTienApDung * (km.getMucGiamPhanTram() / 100.0));
            }

            tongPhaiThu = tongTienDV + tongTienLK - tienGiam + tienThue;
            if (tongPhaiThu < 0) tongPhaiThu = 0;

            lblGiamGia.setText(moneyFmt.format(tienGiam));
            lblThue.setText(moneyFmt.format(tienThue));
            lblTongThanhToan.setText(moneyFmt.format(tongPhaiThu));
        }

        private void thucHienThanhToan() {
            boolean isCK = cboPhuongThuc.getSelectedIndex() == 1;
            String maGd = txtMaGiaoDich.getText().trim();
            if (isCK && maGd.isEmpty()) {
                lblCanhBao.setText("Lỗi: Thanh toán chuyển khoản bắt buộc phải nhập mã giao dịch ngân hàng.");
                lblCanhBao.setForeground(C_DANGER);
                txtMaGiaoDich.requestFocusInWindow();
                return;
            }

            String maKmChon = null;
            int idxKm = cboKhuyenMai.getSelectedIndex();
            if (idxKm > 0 && idxKm - 1 < dsKmHieuLuc.size()) {
                maKmChon = dsKmHieuLuc.get(idxKm - 1).getMaKM();
            }

            final String finalMaKM = maKmChon;
            final PhuongThucThanhToan finalPT = isCK ? PhuongThucThanhToan.CHUYEN_KHOAN : PhuongThucThanhToan.TIEN_MAT;
            final String finalMaGD = maGd;

            btnXacNhanThanhToan.setEnabled(false);
            btnXacNhanThanhToan.setText("Đang xử lý...");

            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() {
                    // Bước 1: Kiểm tra xem đã có hóa đơn chưa, nếu chưa thì gọi sp_LapHoaDon
                    HoaDon hd = controller.timHoaDonTheoPhieu(phieu.getMaPhieu());
                    String maHD;
                    if (hd == null) {
                        maHD = controller.lapHoaDon(phieu.getMaPhieu(), finalMaKM);
                    } else {
                        maHD = hd.getMaHoaDon();
                        controller.capNhatKhuyenMai(maHD, finalMaKM);
                    }

                    // Bước 2: Xác nhận thanh toán hóa đơn sp_ThanhToanHoaDon
                    boolean ok = controller.thanhToanHoaDon(maHD, finalPT, finalMaGD);
                    if (!ok) {
                        throw new RuntimeException("Xác nhận thanh toán thất bại.");
                    }
                    return maHD;
                }

                @Override
                protected void done() {
                    btnXacNhanThanhToan.setEnabled(true);
                    btnXacNhanThanhToan.setText("✓ Xác nhận thanh toán");
                    try {
                        String maHDThanhCong = get();
                        daThanhToanThanhCong = true;
                        ThanhToanHoaDonModalDialog.this.maHoaDonThanhCong = maHDThanhCong;
                        dispose();
                    } catch (Exception ex) {
                        String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                        lblCanhBao.setText("Lỗi: " + msg);
                        lblCanhBao.setForeground(C_DANGER);
                        JOptionPane.showMessageDialog(ThanhToanHoaDonModalDialog.this, msg, "Lỗi thanh toán", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. MODAL DIALOG: XEM CHI TIẾT HÓA ĐƠN ĐÃ THANH TOÁN
    // ─────────────────────────────────────────────────────────────────────────
    private class ChiTietHoaDonModalDialog extends JDialog {
        private final String maHoaDon;

        public ChiTietHoaDonModalDialog(Window parent, String maHoaDon) {
            super(parent, "Chi tiết hóa đơn – " + maHoaDon, ModalityType.APPLICATION_MODAL);
            this.maHoaDon = maHoaDon;

            setLayout(new BorderLayout());
            getContentPane().setBackground(C_WHITE);
            setResizable(true);

            add(buildHeader(), BorderLayout.NORTH);
            add(buildContent(), BorderLayout.CENTER);
            add(buildFooter(), BorderLayout.SOUTH);

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Rectangle bounds = ge.getMaximumWindowBounds();
            int w = Math.min(1000, bounds.width - 60);
            int h = Math.min(750, bounds.height - 60);
            setBounds(bounds.x + (bounds.width - w) / 2, bounds.y + (bounds.height - h) / 2, w, h);
        }

        private JPanel buildHeader() {
            JPanel pnl = new JPanel(new BorderLayout());
            pnl.setBackground(C_WHITE);
            pnl.setBorder(new EmptyBorder(16, 24, 12, 24));

            JLabel lblTitle = new JLabel("Chi tiết hóa đơn thanh toán – " + maHoaDon);
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

            HoaDon hd = controller.timHoaDonTheoMa(maHoaDon);
            if (hd == null) {
                pnl.add(new JLabel("Không tìm thấy thông tin hóa đơn " + maHoaDon));
                return new JScrollPane(pnl);
            }

            PhieuTiepNhanSuaChua phieu = hd.getPhieuSuaChua();
            Xe xe = phieu != null ? phieu.getXe() : null;
            KhachHang kh = xe != null ? xe.getChuXe() : null;

            // Khối thông tin chung
            JPanel cardInfo = new JPanel(new GridLayout(4, 2, 12, 8));
            cardInfo.setBackground(new Color(0xF8FAFC));
            cardInfo.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER, 1),
                    new EmptyBorder(12, 16, 12, 16)));
            cardInfo.setAlignmentX(0.0f);

            cardInfo.add(taoDongInfo("Mã hóa đơn:", hd.getMaHoaDon()));
            cardInfo.add(taoDongInfo("Trạng thái:", "✓ Đã thanh toán"));
            cardInfo.add(taoDongInfo("Mã phiếu sửa chữa:", phieu != null ? phieu.getMaPhieu() : "--"));
            cardInfo.add(taoDongInfo("Thời gian thanh toán:", hd.getThoiGianThanhToan() != null ? timeFmt.format(hd.getThoiGianThanhToan()) : "--"));
            cardInfo.add(taoDongInfo("Khách hàng:", (kh != null ? kh.getHoTen() : "--") + " (" + (kh != null ? kh.getSoDienThoai() : "") + ")"));
            cardInfo.add(taoDongInfo("Biển số xe:", (xe != null ? xe.getBienSoXe() : "--") + " - " + (xe != null && xe.getHangXe() != null ? xe.getHangXe() + " " + xe.getModel() : "")));
            cardInfo.add(taoDongInfo("Phương thức:", hd.getPhuongThucTT() == PhuongThucThanhToan.CHUYEN_KHOAN ? "Chuyển khoản (MGD: " + hd.getMaGiaoDich() + ")" : "Tiền mặt"));
            cardInfo.add(taoDongInfo("Khuyến mãi áp dụng:", hd.getKhuyenMai() != null ? hd.getKhuyenMai().getTenChuongTrinh() : "Không áp dụng"));

            pnl.add(cardInfo);
            pnl.add(Box.createVerticalStrut(12));

            // Bảng chi tiết dịch vụ & linh kiện
            List<ChiTietDichVu> dsDv = phieu != null ? controller.layChiTietDichVu(phieu.getMaPhieu()) : List.of();
            List<ChiTietLinhKien> dsLk = phieu != null ? controller.layChiTietLinhKien(phieu.getMaPhieu()) : List.of();

            JPanel cardBang = new JPanel(new BorderLayout(0, 6));
            cardBang.setBackground(C_WHITE);
            cardBang.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER, 1),
                    new EmptyBorder(10, 14, 10, 14)));
            cardBang.setAlignmentX(0.0f);

            JLabel lblBangTitle = new JLabel("Các dịch vụ và linh kiện tính tiền:");
            lblBangTitle.setFont(f(Font.BOLD, 12));
            cardBang.add(lblBangTitle, BorderLayout.NORTH);

            String[] cols = {"Loại", "Mã", "Tên", "ĐVT", "Số lượng", "Đơn giá", "Thành tiền"};
            DefaultTableModel model = new DefaultTableModel(cols, 0);
            for (ChiTietDichVu ctdv : dsDv) {
                DichVu d = ctdv.getDichVu();
                double tt = ctdv.getDonGia() * ctdv.getSoLuong();
                model.addRow(new Object[]{"Dịch vụ", d != null ? d.getMaDV() : "", d != null ? d.getTenDV() : "", d != null ? d.getDonViTinh() : "Lần", ctdv.getSoLuong(), moneyFmt.format(ctdv.getDonGia()), moneyFmt.format(tt)});
            }
            for (ChiTietLinhKien ctlk : dsLk) {
                LinhKien l = ctlk.getLinhKien();
                double tt = ctlk.getDonGia() * ctlk.getSoLuong();
                model.addRow(new Object[]{"Linh kiện", l != null ? l.getMaLK() : "", l != null ? l.getTenLK() : "", l != null ? l.getDonViTinh() : "Cái", ctlk.getSoLuong(), moneyFmt.format(ctlk.getDonGia()), moneyFmt.format(tt)});
            }

            JTable tbl = new JTable(model);
            tbl.setFont(f(Font.PLAIN, 12));
            tbl.setRowHeight(26);
            tbl.setShowGrid(false);
            tbl.getTableHeader().setFont(f(Font.BOLD, 11));

            DefaultTableCellRenderer rightR = new DefaultTableCellRenderer();
            rightR.setHorizontalAlignment(SwingConstants.RIGHT);
            tbl.getColumnModel().getColumn(5).setCellRenderer(rightR);
            tbl.getColumnModel().getColumn(6).setCellRenderer(rightR);

            JScrollPane spTbl = new JScrollPane(tbl);
            spTbl.setPreferredSize(new Dimension(0, 160));
            cardBang.add(spTbl, BorderLayout.CENTER);
            pnl.add(cardBang);
            pnl.add(Box.createVerticalStrut(12));

            // Tổng kết tiền
            JPanel cardTien = new JPanel(new GridLayout(6, 2, 10, 4));
            cardTien.setBackground(new Color(0xF8FAFC));
            cardTien.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER, 1),
                    new EmptyBorder(10, 16, 10, 16)));
            cardTien.setAlignmentX(0.0f);

            cardTien.add(new JLabel("Tổng tiền dịch vụ:"));
            JLabel l1 = new JLabel(moneyFmt.format(hd.getTongTienDichVu()), SwingConstants.RIGHT);
            cardTien.add(l1);

            cardTien.add(new JLabel("Tổng tiền linh kiện:"));
            JLabel l2 = new JLabel(moneyFmt.format(hd.getTongTienLinhKien()), SwingConstants.RIGHT);
            cardTien.add(l2);

            cardTien.add(new JLabel("Giảm giá khuyến mãi:"));
            JLabel l3 = new JLabel("-" + moneyFmt.format(hd.getTienGiam()), SwingConstants.RIGHT);
            l3.setForeground(C_SUCCESS);
            cardTien.add(l3);

            cardTien.add(new JLabel("Thuế VAT:"));
            JLabel l4 = new JLabel("+" + moneyFmt.format(hd.getThue()), SwingConstants.RIGHT);
            cardTien.add(l4);

            JLabel lTongH = new JLabel("TỔNG TIỀN ĐÃ THANH TOÁN:");
            lTongH.setFont(f(Font.BOLD, 13));
            cardTien.add(lTongH);
            JLabel lTongV = new JLabel(moneyFmt.format(hd.getTongThanhToan()), SwingConstants.RIGHT);
            lTongV.setFont(f(Font.BOLD, 16));
            lTongV.setForeground(C_PRIMARY);
            cardTien.add(lTongV);

            JLabel lBangChuH = new JLabel("Bằng chữ:");
            lBangChuH.setFont(f(Font.ITALIC, 12));
            lBangChuH.setForeground(C_TEXT_SUBTLE);
            cardTien.add(lBangChuH);
            JLabel lBangChuV = new JLabel(docSoTienBangChu((long)hd.getTongThanhToan()), SwingConstants.RIGHT);
            lBangChuV.setFont(f(Font.BOLD | Font.ITALIC, 12));
            lBangChuV.setForeground(new Color(0x047857));
            cardTien.add(lBangChuV);

            pnl.add(cardTien);

            JScrollPane scroll = new JScrollPane(pnl);
            scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            return scroll;
        }

        private JPanel buildFooter() {
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
            pnl.setBackground(new Color(0xF8FAFC));
            pnl.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));

            JButton btnIn = new RoundedButton("🖨 In hóa đơn", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
            btnIn.setFont(f(Font.PLAIN, 12));
            btnIn.setForeground(C_TEXT_MAIN);
            btnIn.setPreferredSize(new Dimension(110, 34));
            btnIn.addActionListener(e -> new BanInHoaDonDialog(this, maHoaDon).setVisible(true));
            pnl.add(btnIn);

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
    // 9. MODAL DIALOG: XEM TRƯỚC BẢN IN & IN HÓA ĐƠN (PRINT PREVIEW)
    // ─────────────────────────────────────────────────────────────────────────
    private class BanInHoaDonDialog extends JDialog {
        private final String maHoaDon;

        public BanInHoaDonDialog(Window parent, String maHoaDon) {
            super(parent, "Xem trước hóa đơn thanh toán – " + maHoaDon, ModalityType.APPLICATION_MODAL);
            this.maHoaDon = maHoaDon;

            setLayout(new BorderLayout());
            getContentPane().setBackground(new Color(0xE2E8F0));
            setResizable(true);

            add(buildToolbar(), BorderLayout.NORTH);
            add(buildPrintPreviewContent(), BorderLayout.CENTER);

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Rectangle bounds = ge.getMaximumWindowBounds();
            int w = Math.min(840, bounds.width - 40);
            int h = Math.min(920, bounds.height - 40);
            setBounds(bounds.x + (bounds.width - w) / 2, bounds.y + (bounds.height - h) / 2, w, h);
        }

        private JPanel buildToolbar() {
            JPanel pnl = new JPanel(new BorderLayout());
            pnl.setBackground(C_WHITE);
            pnl.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER),
                    new EmptyBorder(10, 20, 10, 20)));

            JLabel lblTitle = new JLabel("Xem trước hóa đơn thanh toán");
            lblTitle.setFont(f(Font.BOLD, 15));
            lblTitle.setForeground(C_TEXT_HEAD);
            pnl.add(lblTitle, BorderLayout.WEST);

            JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            btnGroup.setOpaque(false);

            JButton btnXacNhanIn = new RoundedButton("In hóa đơn", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
            btnXacNhanIn.setFont(f(Font.BOLD, 12));
            btnXacNhanIn.setForeground(C_WHITE);
            btnXacNhanIn.setPreferredSize(new Dimension(130, 34));
            btnXacNhanIn.addActionListener(e -> JOptionPane.showMessageDialog(
                    this,
                    "Hóa đơn " + maHoaDon + " đã in thành công.",
                    "In hóa đơn thành công",
                    JOptionPane.INFORMATION_MESSAGE));
            btnGroup.add(btnXacNhanIn);

            JButton btnDong = new RoundedButton("Đóng", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
            btnDong.setFont(f(Font.PLAIN, 12));
            btnDong.setForeground(C_TEXT_MAIN);
            btnDong.setPreferredSize(new Dimension(80, 34));
            btnDong.addActionListener(e -> dispose());
            btnGroup.add(btnDong);

            pnl.add(btnGroup, BorderLayout.EAST);
            return pnl;
        }

        private JScrollPane buildPrintPreviewContent() {
            HoaDon hd = controller.timHoaDonTheoMa(maHoaDon);
            if (hd == null) {
                JPanel errPnl = new JPanel();
                errPnl.add(new JLabel("Không tìm thấy thông tin hóa đơn " + maHoaDon));
                return new JScrollPane(errPnl);
            }

            PhieuTiepNhanSuaChua phieu = hd.getPhieuSuaChua();
            Xe xe = phieu != null ? phieu.getXe() : null;
            KhachHang kh = xe != null ? xe.getChuXe() : null;
            List<ChiTietDichVu> dsDv = phieu != null ? controller.layChiTietDichVu(phieu.getMaPhieu()) : List.of();
            List<ChiTietLinhKien> dsLk = phieu != null ? controller.layChiTietLinhKien(phieu.getMaPhieu()) : List.of();

            // Khung chứa tờ in màu trắng ở giữa nền xám
            JPanel pnlContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
            pnlContainer.setBackground(new Color(0xE2E8F0));

            // TỜ GIẤY IN A4
            JPanel pnlToIn = new JPanel();
            pnlToIn.setLayout(new BoxLayout(pnlToIn, BoxLayout.Y_AXIS));
            pnlToIn.setBackground(C_WHITE);
            pnlToIn.setPreferredSize(new Dimension(740, 950));
            pnlToIn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xCBD5E1), 1),
                    new EmptyBorder(32, 40, 32, 40)));

            // 1. HEADER GARA
            JLabel lblGaraName = new JLabel("CARZONE - TRUNG TÂM BẢO DƯỠNG & SỬA CHỮA Ô TÔ CHUYÊN NGHIỆP");
            lblGaraName.setFont(f(Font.BOLD, 14));
            lblGaraName.setForeground(new Color(0x0F172A));
            lblGaraName.setAlignmentX(0.5f);
            pnlToIn.add(lblGaraName);

            JLabel lblGaraAddr = new JLabel("Địa chỉ: 12 Nguyễn Văn Bảo, Phường 4, Quận Gò Vấp, TP. Hồ Chí Minh");
            lblGaraAddr.setFont(f(Font.PLAIN, 11));
            lblGaraAddr.setForeground(C_TEXT_SUBTLE);
            lblGaraAddr.setAlignmentX(0.5f);
            pnlToIn.add(lblGaraAddr);

            JLabel lblGaraContact = new JLabel("Hotline: 1900 6868 - 0988 888 999 | Email: contact@carzone.vn | Website: www.carzone.vn");
            lblGaraContact.setFont(f(Font.PLAIN, 11));
            lblGaraContact.setForeground(C_TEXT_SUBTLE);
            lblGaraContact.setAlignmentX(0.5f);
            pnlToIn.add(lblGaraContact);

            pnlToIn.add(Box.createVerticalStrut(10));
            JSeparator sepHeader = new JSeparator();
            sepHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
            sepHeader.setForeground(new Color(0x334155));
            pnlToIn.add(sepHeader);
            pnlToIn.add(Box.createVerticalStrut(14));

            // 2. TIÊU ĐỀ HÓA ĐƠN
            JLabel lblInvoiceTitle = new JLabel("HÓA ĐƠN BÁN HÀNG & DỊCH VỤ SỬA CHỮA");
            lblInvoiceTitle.setFont(f(Font.BOLD, 17));
            lblInvoiceTitle.setForeground(new Color(0x0F172A));
            lblInvoiceTitle.setAlignmentX(0.5f);
            pnlToIn.add(lblInvoiceTitle);

            JLabel lblInvoiceSub = new JLabel("(Liên 2: Giao cho khách hàng)");
            lblInvoiceSub.setFont(f(Font.ITALIC, 11));
            lblInvoiceSub.setForeground(C_TEXT_SUBTLE);
            lblInvoiceSub.setAlignmentX(0.5f);
            pnlToIn.add(lblInvoiceSub);

            pnlToIn.add(Box.createVerticalStrut(16));

            // 3. THÔNG TIN HÓA ĐƠN & KHÁCH HÀNG (2 Cột)
            JPanel pnlMeta = new JPanel(new GridLayout(4, 2, 20, 6));
            pnlMeta.setOpaque(false);
            pnlMeta.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
            pnlMeta.setAlignmentX(0.5f);

            pnlMeta.add(taoRowIn("Số hóa đơn:", hd.getMaHoaDon()));
            pnlMeta.add(taoRowIn("Ngày thanh toán:", hd.getThoiGianThanhToan() != null ? timeFmt.format(hd.getThoiGianThanhToan()) : timeFmt.format(new java.util.Date())));

            pnlMeta.add(taoRowIn("Mã phiếu sửa chữa:", phieu != null ? phieu.getMaPhieu() : "--"));
            String tenThuNgan = hd.getNhanVien() != null ? (hd.getNhanVien().getHoTen() != null ? hd.getNhanVien().getHoTen() : hd.getNhanVien().getMaNV()) : "--";
            pnlMeta.add(taoRowIn("Nhân viên thu ngân:", tenThuNgan));

            pnlMeta.add(taoRowIn("Khách hàng:", (kh != null ? kh.getHoTen() : "--") + " - " + (kh != null ? kh.getSoDienThoai() : "")));
            pnlMeta.add(taoRowIn("Phương thức:", hd.getPhuongThucTT() == PhuongThucThanhToan.CHUYEN_KHOAN ? "Chuyển khoản (MGD: " + hd.getMaGiaoDich() + ")" : "Tiền mặt"));

            pnlMeta.add(taoRowIn("Xe & Biển số:", (xe != null ? xe.getBienSoXe() : "--") + " (" + (xe != null && xe.getHangXe() != null ? xe.getHangXe() + " " + xe.getModel() : "") + ")"));
            pnlMeta.add(taoRowIn("Khuyến mãi áp dụng:", hd.getKhuyenMai() != null ? hd.getKhuyenMai().getTenChuongTrinh() : "Không áp dụng"));

            pnlToIn.add(pnlMeta);
            pnlToIn.add(Box.createVerticalStrut(14));

            // 4. BẢNG KÊ CHI TIẾT DỊCH VỤ & PHỤ TÙNG
            String[] cols = {"STT", "Nội dung thực hiện / Phụ tùng", "Loại", "ĐVT", "SL", "Đơn giá (đ)", "Thành tiền (đ)"};
            DefaultTableModel model = new DefaultTableModel(cols, 0);

            int stt = 1;
            for (ChiTietDichVu ctdv : dsDv) {
                DichVu d = ctdv.getDichVu();
                double tt = ctdv.getDonGia() * ctdv.getSoLuong();
                model.addRow(new Object[]{
                        stt++,
                        d != null ? d.getTenDV() : "Dịch vụ",
                        "Dịch vụ",
                        d != null && d.getDonViTinh() != null ? d.getDonViTinh() : "Lần",
                        ctdv.getSoLuong(),
                        moneyFmt.format(ctdv.getDonGia()),
                        moneyFmt.format(tt)
                });
            }
            for (ChiTietLinhKien ctlk : dsLk) {
                LinhKien l = ctlk.getLinhKien();
                double tt = ctlk.getDonGia() * ctlk.getSoLuong();
                model.addRow(new Object[]{
                        stt++,
                        l != null ? l.getTenLK() : "Phụ tùng",
                        "Linh kiện",
                        l != null && l.getDonViTinh() != null ? l.getDonViTinh() : "Cái",
                        ctlk.getSoLuong(),
                        moneyFmt.format(ctlk.getDonGia()),
                        moneyFmt.format(tt)
                });
            }

            JTable tblPrint = new JTable(model);
            tblPrint.setFont(f(Font.PLAIN, 11));
            tblPrint.setRowHeight(24);
            tblPrint.getTableHeader().setFont(f(Font.BOLD, 11));
            tblPrint.setShowGrid(true);
            tblPrint.setGridColor(new Color(0xE2E8F0));

            DefaultTableCellRenderer centerR = new DefaultTableCellRenderer();
            centerR.setHorizontalAlignment(SwingConstants.CENTER);
            tblPrint.getColumnModel().getColumn(0).setCellRenderer(centerR);
            tblPrint.getColumnModel().getColumn(0).setMaxWidth(40);
            tblPrint.getColumnModel().getColumn(2).setCellRenderer(centerR);
            tblPrint.getColumnModel().getColumn(3).setCellRenderer(centerR);
            tblPrint.getColumnModel().getColumn(4).setCellRenderer(centerR);

            DefaultTableCellRenderer rightR = new DefaultTableCellRenderer();
            rightR.setHorizontalAlignment(SwingConstants.RIGHT);
            tblPrint.getColumnModel().getColumn(5).setCellRenderer(rightR);
            tblPrint.getColumnModel().getColumn(6).setCellRenderer(rightR);

            JScrollPane spTable = new JScrollPane(tblPrint);
            spTable.setBorder(BorderFactory.createLineBorder(new Color(0xCBD5E1), 1));
            spTable.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
            spTable.setAlignmentX(0.5f);
            pnlToIn.add(spTable);

            pnlToIn.add(Box.createVerticalStrut(12));

            // 5. TỔNG KẾT TÀI CHÍNH
            JPanel pnlTienPrint = new JPanel(new GridLayout(6, 2, 10, 4));
            pnlTienPrint.setOpaque(false);
            pnlTienPrint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
            pnlTienPrint.setAlignmentX(0.5f);

            pnlTienPrint.add(new JLabel("Cộng tiền dịch vụ:"));
            pnlTienPrint.add(new JLabel(moneyFmt.format(hd.getTongTienDichVu()), SwingConstants.RIGHT));

            pnlTienPrint.add(new JLabel("Cộng tiền phụ tùng linh kiện:"));
            pnlTienPrint.add(new JLabel(moneyFmt.format(hd.getTongTienLinhKien()), SwingConstants.RIGHT));

            pnlTienPrint.add(new JLabel("Giảm trừ chiết khấu (Khuyến mãi):"));
            JLabel lGiam = new JLabel("-" + moneyFmt.format(hd.getTienGiam()), SwingConstants.RIGHT);
            lGiam.setForeground(C_SUCCESS);
            pnlTienPrint.add(lGiam);

            pnlTienPrint.add(new JLabel("Thuế giá trị gia tăng (VAT):"));
            pnlTienPrint.add(new JLabel("+" + moneyFmt.format(hd.getThue()), SwingConstants.RIGHT));

            JLabel lTongCongL = new JLabel("TỔNG CỘNG THANH TOÁN:");
            lTongCongL.setFont(f(Font.BOLD, 14));
            pnlTienPrint.add(lTongCongL);

            JLabel lTongCongV = new JLabel(moneyFmt.format(hd.getTongThanhToan()), SwingConstants.RIGHT);
            lTongCongV.setFont(f(Font.BOLD, 15));
            lTongCongV.setForeground(C_PRIMARY);
            pnlTienPrint.add(lTongCongV);

            JLabel lDocChuL = new JLabel("Số tiền bằng chữ:");
            lDocChuL.setFont(f(Font.ITALIC, 11));
            pnlTienPrint.add(lDocChuL);

            JLabel lDocChuV = new JLabel(docSoTienBangChu((long)hd.getTongThanhToan()), SwingConstants.RIGHT);
            lDocChuV.setFont(f(Font.BOLD | Font.ITALIC, 11));
            lDocChuV.setForeground(new Color(0x047857));
            pnlTienPrint.add(lDocChuV);

            pnlToIn.add(pnlTienPrint);
            pnlToIn.add(Box.createVerticalStrut(20));

            // 6. KHU VỰC KÝ TÊN XÁC NHẬN
            JPanel pnlKyTen = new JPanel(new GridLayout(1, 2, 20, 0));
            pnlKyTen.setOpaque(false);
            pnlKyTen.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
            pnlKyTen.setAlignmentX(0.5f);

            JPanel pnlKyKhach = new JPanel();
            pnlKyKhach.setLayout(new BoxLayout(pnlKyKhach, BoxLayout.Y_AXIS));
            pnlKyKhach.setOpaque(false);
            JLabel lblKHKy = new JLabel("Khách hàng thanh toán");
            lblKHKy.setFont(f(Font.BOLD, 12));
            lblKHKy.setAlignmentX(0.5f);
            JLabel lblKHSub = new JLabel("(Ký và ghi rõ họ tên)");
            lblKHSub.setFont(f(Font.ITALIC, 10));
            lblKHSub.setAlignmentX(0.5f);
            pnlKyKhach.add(lblKHKy);
            pnlKyKhach.add(lblKHSub);
            pnlKyKhach.add(Box.createVerticalStrut(45));
            JLabel lblKHTen = new JLabel(kh != null ? kh.getHoTen() : "");
            lblKHTen.setFont(f(Font.BOLD, 11));
            lblKHTen.setAlignmentX(0.5f);
            pnlKyKhach.add(lblKHTen);

            JPanel pnlKyNV = new JPanel();
            pnlKyNV.setLayout(new BoxLayout(pnlKyNV, BoxLayout.Y_AXIS));
            pnlKyNV.setOpaque(false);
            JLabel lblNVKy = new JLabel("Người lập hóa đơn (Thu ngân)");
            lblNVKy.setFont(f(Font.BOLD, 12));
            lblNVKy.setAlignmentX(0.5f);
            JLabel lblNVSub = new JLabel("(Ký và ghi rõ họ tên)");
            lblNVSub.setFont(f(Font.ITALIC, 10));
            lblNVSub.setAlignmentX(0.5f);
            pnlKyNV.add(lblNVKy);
            pnlKyNV.add(lblNVSub);
            pnlKyNV.add(Box.createVerticalStrut(45));
            JLabel lblNVTen = new JLabel(tenThuNgan);
            lblNVTen.setFont(f(Font.BOLD, 11));
            lblNVTen.setAlignmentX(0.5f);
            pnlKyNV.add(lblNVTen);

            pnlKyTen.add(pnlKyKhach);
            pnlKyTen.add(pnlKyNV);
            pnlToIn.add(pnlKyTen);

            pnlToIn.add(Box.createVerticalStrut(16));

            // 7. LỜI CẢM ƠN
            JLabel lblCamOn = new JLabel("CarZone xin chân thành cảm ơn Quý khách! Chúc Quý khách vạn dặm bình an.");
            lblCamOn.setFont(f(Font.ITALIC, 11));
            lblCamOn.setForeground(C_TEXT_SUBTLE);
            lblCamOn.setAlignmentX(0.5f);
            pnlToIn.add(lblCamOn);

            pnlContainer.add(pnlToIn);

            JScrollPane scroll = new JScrollPane(pnlContainer);
            scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            return scroll;
        }

        private JPanel taoRowIn(String title, String val) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            p.setOpaque(false);
            JLabel l1 = new JLabel(title);
            l1.setFont(f(Font.BOLD, 11));
            l1.setForeground(C_TEXT_HEAD);
            JLabel l2 = new JLabel(val != null ? val : "--");
            l2.setFont(f(Font.PLAIN, 11));
            l2.setForeground(C_TEXT_MAIN);
            p.add(l1);
            p.add(l2);
            return p;
        }

    }
}
