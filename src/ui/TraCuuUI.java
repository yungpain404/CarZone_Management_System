package ui;

import controller.TraCuuController;
import entity.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import static ui.MainFrame.*;

/**
 * Màn hình Tra cứu tổng hợp dành cho Tiếp nhận viên (UC007 - Tiếp nhận viên).
 * Bám sát Figma: Tra_cuu.jpg
 * Tabs: Dịch vụ | Linh kiện | Khách hàng | Lịch sử tiếp nhận
 * Toàn bộ là READ-ONLY, truy vấn CSDL qua SwingWorker.
 */
public class TraCuuUI extends JPanel {

    private final TraCuuController controller = new TraCuuController();
    private final DecimalFormat moneyFmt = new DecimalFormat("#,### đ");
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public TraCuuUI() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel pnl = new JPanel(new BorderLayout(12, 0));
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(16, 24, 12, 24));

        JPanel titleBlock = new JPanel(new BorderLayout(0, 2));
        titleBlock.setOpaque(false);

        JLabel lblTitle = new JLabel("Tra cứu thông tin");
        lblTitle.setFont(f(Font.BOLD, 20));
        lblTitle.setForeground(C_TEXT_HEAD);

        JLabel lblSub = new JLabel("Tra cứu danh mục dịch vụ, linh kiện, thông tin khách hàng và lịch sử phiếu tiếp nhận");
        lblSub.setFont(f(Font.PLAIN, 12));
        lblSub.setForeground(C_TEXT_SUBTLE);

        titleBlock.add(lblTitle, BorderLayout.NORTH);
        titleBlock.add(lblSub, BorderLayout.SOUTH);
        pnl.add(titleBlock, BorderLayout.WEST);

        return pnl;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(f(Font.BOLD, 13));
        tabs.setBorder(new EmptyBorder(0, 24, 20, 24));

        tabs.addTab("🔧 Dịch vụ", buildTabDichVu());
        tabs.addTab("⚙️ Linh kiện", buildTabLinhKien());
        tabs.addTab("👥 Khách hàng", buildTabKhachHang());
        tabs.addTab("📋 Lịch sử tiếp nhận", buildTabLichSuPhieu());

        return tabs;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 1: DỊCH VỤ
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTabDichVu() {
        JPanel tab = new JPanel(new BorderLayout(0, 10));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 0, 0, 0));

        // Toolbar tìm kiếm
        JTextField txtSearch = new JTextField(25);
        txtSearch.setPreferredSize(new Dimension(280, 34));
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm theo mã DV hoặc tên dịch vụ...");

        DefaultTableModel model = new DefaultTableModel(new String[]{"Mã DV", "Tên dịch vụ", "Đơn giá", "Đơn vị tính", "Trạng thái"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = createStandardTable(model);

        Runnable loadData = () -> new SwingWorker<List<DichVu>, Void>() {
            @Override protected List<DichVu> doInBackground() {
                return controller.timDichVu(txtSearch.getText().trim());
            }
            @Override protected void done() {
                try {
                    List<DichVu> list = get();
                    model.setRowCount(0);
                    for (DichVu dv : list) {
                        model.addRow(new Object[]{
                                dv.getMaDV(),
                                dv.getTenDV(),
                                moneyFmt.format(dv.getDonGia()),
                                dv.getDonViTinh() != null ? dv.getDonViTinh() : "Lần",
                                dv.isDangCungCap() ? "Đang cung cấp" : "Tạm ngưng"
                        });
                    }
                } catch (Exception ignored) {}
            }
        }.execute();

        JPanel toolRow = buildSearchToolbar(txtSearch, loadData);
        tab.add(toolRow, BorderLayout.NORTH);
        tab.add(new JScrollPane(table), BorderLayout.CENTER);

        loadData.run();
        return tab;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 2: LINH KIỆN
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTabLinhKien() {
        JPanel tab = new JPanel(new BorderLayout(0, 10));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 0, 0, 0));

        JTextField txtSearch = new JTextField(25);
        txtSearch.setPreferredSize(new Dimension(280, 34));
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm theo mã LK hoặc tên linh kiện...");

        DefaultTableModel model = new DefaultTableModel(new String[]{"Mã LK", "Tên linh kiện", "Đơn giá", "Đơn vị tính", "Tồn kho", "Trạng thái"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = createStandardTable(model);

        Runnable loadData = () -> new SwingWorker<List<LinhKien>, Void>() {
            @Override protected List<LinhKien> doInBackground() {
                return controller.timLinhKien(txtSearch.getText().trim());
            }
            @Override protected void done() {
                try {
                    List<LinhKien> list = get();
                    model.setRowCount(0);
                    for (LinhKien lk : list) {
                        model.addRow(new Object[]{
                                lk.getMaLK(),
                                lk.getTenLK(),
                                moneyFmt.format(lk.getDonGia()),
                                lk.getDonViTinh() != null ? lk.getDonViTinh() : "Cái",
                                lk.getSoLuongTon(),
                                lk.isDangKinhDoanh() ? "Đang kinh doanh" : "Ngừng kinh doanh"
                        });
                    }
                } catch (Exception ignored) {}
            }
        }.execute();

        JPanel toolRow = buildSearchToolbar(txtSearch, loadData);
        tab.add(toolRow, BorderLayout.NORTH);
        tab.add(new JScrollPane(table), BorderLayout.CENTER);

        loadData.run();
        return tab;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 3: KHÁCH HÀNG
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTabKhachHang() {
        JPanel tab = new JPanel(new BorderLayout(0, 10));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 0, 0, 0));

        JTextField txtSearch = new JTextField(25);
        txtSearch.setPreferredSize(new Dimension(280, 34));
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm theo mã, họ tên, số điện thoại, CCCD...");

        DefaultTableModel model = new DefaultTableModel(new String[]{"Mã KH", "Họ tên", "Số điện thoại", "CCCD", "Điểm tích lũy", "Hội viên"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = createStandardTable(model);

        Runnable loadData = () -> new SwingWorker<List<KhachHang>, Void>() {
            @Override protected List<KhachHang> doInBackground() {
                return controller.timKhachHang(txtSearch.getText().trim());
            }
            @Override protected void done() {
                try {
                    List<KhachHang> list = get();
                    model.setRowCount(0);
                    for (KhachHang kh : list) {
                        model.addRow(new Object[]{
                                kh.getMaKH(),
                                kh.getHoTen(),
                                kh.getSoDienThoai(),
                                kh.getCCCD() != null ? kh.getCCCD() : "",
                                kh.getDiemTichLuy(),
                                kh.isThanhVienThanThiet() ? "★ Thân thiết" : "Tiêu chuẩn"
                        });
                    }
                } catch (Exception ignored) {}
            }
        }.execute();

        JPanel toolRow = buildSearchToolbar(txtSearch, loadData);
        tab.add(toolRow, BorderLayout.NORTH);
        tab.add(new JScrollPane(table), BorderLayout.CENTER);

        loadData.run();
        return tab;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAB 4: LỊCH SỬ TIẾP NHẬN SỬA CHỮA
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTabLichSuPhieu() {
        JPanel tab = new JPanel(new BorderLayout(0, 10));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 0, 0, 0));

        JTextField txtSearch = new JTextField(25);
        txtSearch.setPreferredSize(new Dimension(280, 34));
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm theo mã phiếu, biển số xe...");

        DefaultTableModel model = new DefaultTableModel(new String[]{"Mã phiếu", "Biển số xe", "Thời điểm tiếp nhận", "Yêu cầu của khách", "KTV chính", "Trạng thái"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = createStandardTable(model);

        Runnable loadData = () -> new SwingWorker<List<PhieuTiepNhanSuaChua>, Void>() {
            @Override protected List<PhieuTiepNhanSuaChua> doInBackground() {
                return controller.timLichSuTiepNhan(txtSearch.getText().trim(), null);
            }
            @Override protected void done() {
                try {
                    List<PhieuTiepNhanSuaChua> list = get();
                    model.setRowCount(0);
                    for (PhieuTiepNhanSuaChua p : list) {
                        String bienSo = p.getXe() != null ? p.getXe().getBienSoXe() : "Chưa rõ";
                        String ngay = p.getNgayGioTiepNhan() != null ? dateFmt.format(p.getNgayGioTiepNhan()) : "";
                        String ktv = p.getNhanVienSuaChua() != null ? p.getNhanVienSuaChua().getHoTen() : "Chưa phân công";
                        model.addRow(new Object[]{
                                p.getMaPhieu(),
                                bienSo,
                                ngay,
                                p.getYeuCauBanDau(),
                                ktv,
                                p.getTrangThai() != null ? p.getTrangThai().name() : ""
                        });
                    }
                } catch (Exception ignored) {}
            }
        }.execute();

        JPanel toolRow = buildSearchToolbar(txtSearch, loadData);
        tab.add(toolRow, BorderLayout.NORTH);
        tab.add(new JScrollPane(table), BorderLayout.CENTER);

        loadData.run();
        return tab;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS UI
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildSearchToolbar(JTextField txtSearch, Runnable onSearch) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);

        bar.add(txtSearch);

        JButton btnSearch = new RoundedButton("🔍 Tìm kiếm", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
        btnSearch.setForeground(C_WHITE);
        btnSearch.setFont(f(Font.BOLD, 12));
        btnSearch.setPreferredSize(new Dimension(100, 34));
        btnSearch.addActionListener(e -> onSearch.run());
        bar.add(btnSearch);

        JButton btnRefresh = new RoundedButton("↺ Làm mới", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnRefresh.setFont(f(Font.PLAIN, 12));
        btnRefresh.setPreferredSize(new Dimension(90, 34));
        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            onSearch.run();
        });
        bar.add(btnRefresh);

        txtSearch.addActionListener(e -> onSearch.run());

        return bar;
    }

    private JTable createStandardTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(f(Font.PLAIN, 13));
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.getTableHeader().setFont(f(Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(0xF8FAFC));
        table.setSelectionBackground(C_PRIMARY_BG);
        table.setSelectionForeground(C_TEXT_HEAD);
        return table;
    }
}
