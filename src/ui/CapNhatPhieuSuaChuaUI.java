package ui;

import controller.PhieuTiepNhanSuaChuaController;
import dao.PhieuTiepNhanSuaChuaDAO.ChiTietDichVuItem;
import dao.PhieuTiepNhanSuaChuaDAO.ChiTietLinhKienItem;
import dao.PhieuTiepNhanSuaChuaDAO.PhieuSuaChuaItem;
import entity.DichVu;
import entity.LinhKien;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Màn hình Cập nhật phiếu sửa chữa cho Kỹ thuật viên (Chuẩn Figma frame 50:787).
 * Tái hiện 100% bố cục và thứ tự thành phần theo thiết kế Figma.
 */
public class CapNhatPhieuSuaChuaUI extends JPanel {

    private static final Color BG = new Color(0xF4F7FB);
    private static final Color WHITE = Color.WHITE;
    private static final Color BORDER = new Color(0xE3E9F2);
    private static final Color PRIMARY = new Color(0x2563EB);
    private static final Color PRIMARY_HV = new Color(0x1D4ED8);
    private static final Color TEXT = new Color(0x14233D);
    private static final Color BODY = new Color(0x40516A);
    private static final Color SUBTLE = new Color(0x7A889C);

    private final PhieuTiepNhanSuaChuaController controller = new PhieuTiepNhanSuaChuaController();
    private final Consumer<Void> onQuayLaiDanhSach;

    // 1. Dropdown chọn phiếu
    private final JComboBox<String> cboChonPhieu = new JComboBox<>();
    private List<PhieuSuaChuaItem> listPhieu = new ArrayList<>();
    private PhieuSuaChuaItem phieuDangChon = null;

    // 2. Các ô xám chỉ đọc
    private final JTextField txtMaPhieu = createReadOnlyField("—");
    private final JTextField txtKhachHang = createReadOnlyField("—");
    private final JTextField txtBienSoXe = createReadOnlyField("—");
    private final JTextField txtNoiDungTiepNhan = createReadOnlyField("—");
    private final JTextField txtKyThuatVien = createReadOnlyField("—");
    private final JTextField txtTrangThaiHienTai = createReadOnlyField("—");

    // 3. Khối Cập nhật dịch vụ
    private final JComboBox<DichVu> cboDichVu = new JComboBox<>();
    private final JTextField txtSoLuongDV = new JTextField();
    private final JButton btnThemDV = button("Thêm / cập nhật", false, 145);
    private final JButton btnBoDongDV = button("Bỏ dòng", false, 100);
    private final DefaultTableModel modelDV = new DefaultTableModel(new Object[]{
            "Mã dịch vụ", "Tên dịch vụ", "Đơn vị", "Số lượng", "Nội dung thực hiện"
    }, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblDV = new JTable(modelDV);
    private final JLabel lblFooterDV = label("Chưa có dữ liệu · Chưa chọn bản ghi", 12, SUBTLE, Font.PLAIN);

    // 4. Khối Cập nhật linh kiện sử dụng
    private final JComboBox<LinhKien> cboLinhKien = new JComboBox<>();
    private final JTextField txtSoLuongLK = new JTextField();
    private final JButton btnThemLK = button("Thêm / cập nhật", false, 145);
    private final JButton btnBoDongLK = button("Bỏ dòng", false, 100);
    private final DefaultTableModel modelLK = new DefaultTableModel(new Object[]{
            "Mã linh kiện", "Tên linh kiện", "Đơn vị", "Số lượng sử dụng", "Ghi chú"
    }, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblLK = new JTable(modelLK);
    private final JLabel lblFooterLK = label("Chưa có dữ liệu · Chưa chọn bản ghi", 12, SUBTLE, Font.PLAIN);

    // 5. Trạng thái công việc & Nội dung thực hiện
    private final JComboBox<String> cboTrangThaiCongViec = new JComboBox<>(new String[]{"—", "CHO_SUA", "DANG_SUA", "CHO_LINH_KIEN", "HOAN_TAT"});
    private final JTextField txtNoiDungThucHien = new JTextField();

    // 6. Action buttons
    private final JButton btnLuu = button("Lưu", false, 95);
    private final JButton btnHuy = button("Hủy", false, 95);
    private final JButton btnQuayLai = button("Quay lại danh sách", false, 180);
    private final JLabel lblFooterHint = label("Chưa thể lưu: chưa chọn phiếu được phân công và chưa xác định quyền cập nhật.", 12, SUBTLE, Font.PLAIN);

    public CapNhatPhieuSuaChuaUI() {
        this(null);
    }

    public CapNhatPhieuSuaChuaUI(Consumer<Void> onQuayLaiDanhSach) {
        this.onQuayLaiDanhSach = onQuayLaiDanhSach;

        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(22, 26, 22, 26));

        JPanel root = new MainFrame.ScrollableContentPanel();
        root.setOpaque(false);

        // Heading
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        heading.setOpaque(false);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(label("🛠️", 25, TEXT, Font.PLAIN));
        heading.add(label("Cập nhật phiếu sửa chữa", 24, TEXT, Font.BOLD));
        addFull(root, heading);
        root.add(Box.createVerticalStrut(16));

        // Form Card lớn
        JPanel formCard = new TraCuuUI.RoundedPanel(WHITE, 12, BORDER, new Insets(20, 24, 20, 24));
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 1. Chọn phiếu sửa chữa được phân công
        formCard.add(createLabeledGroup("Chọn phiếu sửa chữa được phân công", cboChonPhieu));
        formCard.add(Box.createVerticalStrut(10));

        // 2. Các ô xám chỉ đọc
        formCard.add(createLabeledGroup("Mã phiếu sửa chữa", txtMaPhieu));
        formCard.add(Box.createVerticalStrut(10));
        formCard.add(createLabeledGroup("Khách hàng", txtKhachHang));
        formCard.add(Box.createVerticalStrut(10));
        formCard.add(createLabeledGroup("Biển số / xe", txtBienSoXe));
        formCard.add(Box.createVerticalStrut(10));
        formCard.add(createLabeledGroup("Nội dung tiếp nhận", txtNoiDungTiepNhan));
        formCard.add(Box.createVerticalStrut(10));
        formCard.add(createLabeledGroup("Kỹ thuật viên được phân công", txtKyThuatVien));
        formCard.add(Box.createVerticalStrut(10));
        formCard.add(createLabeledGroup("Trạng thái hiện tại", txtTrangThaiHienTai));
        formCard.add(Box.createVerticalStrut(16));

        // 3. Khối Cập nhật dịch vụ
        formCard.add(buildKhoiDichVu());
        formCard.add(Box.createVerticalStrut(16));

        // 4. Khối Cập nhật linh kiện sử dụng
        formCard.add(buildKhoiLinhKien());
        formCard.add(Box.createVerticalStrut(16));

        // 5. Trạng thái công việc *
        cboTrangThaiCongViec.setPreferredSize(new Dimension(Integer.MAX_VALUE, 40));
        cboTrangThaiCongViec.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String s) {
                    setText(switch (s) {
                        case "CHO_SUA" -> "Chờ sửa";
                        case "DANG_SUA" -> "Đang sửa";
                        case "CHO_LINH_KIEN" -> "Chờ linh kiện";
                        case "HOAN_TAT" -> "Hoàn tất";
                        default -> s;
                    });
                }
                return this;
            }
        });
        formCard.add(createLabeledGroup("Trạng thái công việc *", cboTrangThaiCongViec));
        formCard.add(Box.createVerticalStrut(10));

        // 6. Nội dung thực hiện / ghi chú
        txtNoiDungThucHien.setPreferredSize(new Dimension(Integer.MAX_VALUE, 40));
        txtNoiDungThucHien.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtNoiDungThucHien.setBorder(new EmptyBorder(0, 10, 0, 10));
        JPanel boxGhiChu = new JPanel(new BorderLayout());
        boxGhiChu.setBackground(WHITE);
        boxGhiChu.setBorder(new TraCuuUI.RoundedLineBorder(new Color(0xC9D5E5), 10));
        boxGhiChu.add(txtNoiDungThucHien);
        formCard.add(createLabeledGroup("Nội dung thực hiện / ghi chú", boxGhiChu));
        formCard.add(Box.createVerticalStrut(16));

        // 7. Hàng nút bấm
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.add(btnLuu);
        btnRow.add(btnHuy);
        btnRow.add(btnQuayLai);
        formCard.add(btnRow);
        formCard.add(Box.createVerticalStrut(8));
        addFull(formCard, lblFooterHint);

        addFull(root, formCard);

        JScrollPane scrollMain = new JScrollPane(root);
        scrollMain.setBorder(null);
        scrollMain.setOpaque(false);
        scrollMain.getViewport().setOpaque(false);
        scrollMain.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollMain.getVerticalScrollBar().setUnitIncrement(18);
        add(scrollMain, BorderLayout.CENTER);

        bindEvents();
        napDanhSachPhieu();
        napDanhSachDanhMuc();
    }

    private JPanel buildKhoiDichVu() {
        JPanel card = new TraCuuUI.RoundedPanel(WHITE, 10, BORDER, new Insets(14, 16, 14, 16));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTitle = label("Cập nhật dịch vụ", 14, TEXT, Font.BOLD);
        addFull(card, lblTitle);
        card.add(Box.createVerticalStrut(10));

        JPanel rowInput = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        rowInput.setOpaque(false);
        rowInput.setAlignmentX(Component.LEFT_ALIGNMENT);

        cboDichVu.setPreferredSize(new Dimension(380, 38));
        cboDichVu.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DichVu dv) {
                    setText(dv.getMaDV() + " — " + dv.getTenDV());
                } else if (value == null) {
                    setText("Chọn phiếu trước");
                }
                return this;
            }
        });

        txtSoLuongDV.setPreferredSize(new Dimension(140, 38));
        txtSoLuongDV.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtSoLuongDV.putClientProperty("JTextField.placeholderText", "Nhập số lượng");
        chiChoPhepNhapSo(txtSoLuongDV);
        JPanel boxSL = new JPanel(new BorderLayout());
        boxSL.setBackground(WHITE);
        boxSL.setBorder(new TraCuuUI.RoundedLineBorder(new Color(0xC9D5E5), 8));
        txtSoLuongDV.setBorder(new EmptyBorder(0, 8, 0, 8));
        boxSL.add(txtSoLuongDV);

        rowInput.add(createMiniField("Dịch vụ được phép cập nhật", cboDichVu));
        rowInput.add(createMiniField("Số lượng", boxSL));
        JPanel btnGrp = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btnGrp.setOpaque(false);
        btnGrp.add(btnThemDV);
        btnGrp.add(btnBoDongDV);
        rowInput.add(createMiniField(" ", btnGrp));
        addFull(card, rowInput);
        card.add(Box.createVerticalStrut(10));

        // Table
        styleSubTable(tblDV);
        JScrollPane scroll = new JScrollPane(tblDV);
        scroll.setBorder(new TraCuuUI.RoundedLineBorder(BORDER, 8));
        scroll.getViewport().setBackground(WHITE);
        scroll.setPreferredSize(new Dimension(900, 140));
        addFull(card, scroll);
        card.add(Box.createVerticalStrut(6));
        addFull(card, lblFooterDV);

        return card;
    }

    private JPanel buildKhoiLinhKien() {
        JPanel card = new TraCuuUI.RoundedPanel(WHITE, 10, BORDER, new Insets(14, 16, 14, 16));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTitle = label("Cập nhật linh kiện sử dụng", 14, TEXT, Font.BOLD);
        addFull(card, lblTitle);
        card.add(Box.createVerticalStrut(10));

        JPanel rowInput = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        rowInput.setOpaque(false);
        rowInput.setAlignmentX(Component.LEFT_ALIGNMENT);

        cboLinhKien.setPreferredSize(new Dimension(380, 38));
        cboLinhKien.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof LinhKien lk) {
                    setText(lk.getMaLK() + " — " + lk.getTenLK() + " (Tồn: " + lk.getSoLuongTon() + " " + lk.getDonViTinh() + ")");
                } else if (value == null) {
                    setText("Chọn phiếu trước");
                }
                return this;
            }
        });

        txtSoLuongLK.setPreferredSize(new Dimension(140, 38));
        txtSoLuongLK.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtSoLuongLK.putClientProperty("JTextField.placeholderText", "Nhập số lượng");
        chiChoPhepNhapSo(txtSoLuongLK);
        JPanel boxSL = new JPanel(new BorderLayout());
        boxSL.setBackground(WHITE);
        boxSL.setBorder(new TraCuuUI.RoundedLineBorder(new Color(0xC9D5E5), 8));
        txtSoLuongLK.setBorder(new EmptyBorder(0, 8, 0, 8));
        boxSL.add(txtSoLuongLK);

        rowInput.add(createMiniField("Linh kiện được phép sử dụng", cboLinhKien));
        rowInput.add(createMiniField("Số lượng sử dụng", boxSL));
        JPanel btnGrp = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btnGrp.setOpaque(false);
        btnGrp.add(btnThemLK);
        btnGrp.add(btnBoDongLK);
        rowInput.add(createMiniField(" ", btnGrp));
        addFull(card, rowInput);
        card.add(Box.createVerticalStrut(10));

        // Table
        styleSubTable(tblLK);
        JScrollPane scroll = new JScrollPane(tblLK);
        scroll.setBorder(new TraCuuUI.RoundedLineBorder(BORDER, 8));
        scroll.getViewport().setBackground(WHITE);
        scroll.setPreferredSize(new Dimension(900, 140));
        addFull(card, scroll);
        card.add(Box.createVerticalStrut(6));
        addFull(card, lblFooterLK);

        return card;
    }

    private JPanel createLabeledGroup(String title, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.add(label(title, 12, BODY, Font.PLAIN), BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height + 24));
        return p;
    }

    private JPanel createMiniField(String title, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.add(label(title, 12, BODY, Font.PLAIN), BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private static JTextField createReadOnlyField(String initial) {
        JTextField tf = new JTextField(initial);
        tf.setEditable(false);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tf.setForeground(TEXT);
        tf.setBackground(new Color(0xF8FAFC));
        tf.setBorder(new TraCuuUI.RoundedLineBorder(new Color(0xE2E8F0), 10));
        tf.setPreferredSize(new Dimension(Integer.MAX_VALUE, 40));
        return tf;
    }

    private void bindEvents() {
        cboChonPhieu.addActionListener(e -> {
            int idx = cboChonPhieu.getSelectedIndex();
            if (idx > 0 && idx - 1 < listPhieu.size()) {
                phieuDangChon = listPhieu.get(idx - 1);
                chonPhieu(phieuDangChon);
            } else {
                phieuDangChon = null;
                xoaTrangForm();
            }
        });

        btnThemDV.addActionListener(e -> xuLyThemDichVu());
        btnBoDongDV.addActionListener(e -> xuLyXoaDichVu());
        btnThemLK.addActionListener(e -> xuLyThemLinhKien());
        btnBoDongLK.addActionListener(e -> xuLyXoaLinhKien());

        btnLuu.addActionListener(e -> xuLyLuu());
        btnHuy.addActionListener(e -> xoaTrangForm());
        btnQuayLai.addActionListener(e -> {
            if (onQuayLaiDanhSach != null) onQuayLaiDanhSach.accept(null);
        });
    }

    private String maPhieuChoSua = null;

    public void datMaPhieuCanSua(String maPhieu) {
        if (maPhieu == null) return;
        this.maPhieuChoSua = maPhieu;
        if (listPhieu != null && !listPhieu.isEmpty()) {
            for (int i = 0; i < listPhieu.size(); i++) {
                if (maPhieu.equalsIgnoreCase(listPhieu.get(i).maPhieu())) {
                    cboChonPhieu.setSelectedIndex(i + 1);
                    this.maPhieuChoSua = null;
                    break;
                }
            }
        }
    }

    private void napDanhSachPhieu() {
        new SwingWorker<List<PhieuSuaChuaItem>, Void>() {
            @Override protected List<PhieuSuaChuaItem> doInBackground() {
                return controller.timPhieuKTV(null, null, null, null);
            }
            @Override protected void done() {
                try {
                    List<PhieuSuaChuaItem> all = get();
                    listPhieu = new ArrayList<>();
                    cboChonPhieu.removeAllItems();
                    cboChonPhieu.addItem("Chưa chọn phiếu");
                    for (PhieuSuaChuaItem p : all) {
                        if (!"HOAN_TAT".equalsIgnoreCase(p.trangThai())) {
                            listPhieu.add(p);
                            cboChonPhieu.addItem(p.maPhieu() + " — " + p.bienSoXe() + " (" + p.hangModel() + ") · " + tenTrangThai(p.trangThai()));
                        }
                    }
                    if (maPhieuChoSua != null) {
                        for (int i = 0; i < listPhieu.size(); i++) {
                            if (maPhieuChoSua.equalsIgnoreCase(listPhieu.get(i).maPhieu())) {
                                cboChonPhieu.setSelectedIndex(i + 1);
                                maPhieuChoSua = null;
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void napDanhSachDanhMuc() {
        new SwingWorker<Void, Void>() {
            List<DichVu> dList;
            List<LinhKien> lList;
            @Override protected Void doInBackground() {
                dList = controller.layDanhSachDichVuHoatDong();
                lList = controller.layDanhSachLinhKienHoatDong();
                return null;
            }
            @Override protected void done() {
                cboDichVu.removeAllItems();
                for (DichVu d : dList) cboDichVu.addItem(d);
                cboLinhKien.removeAllItems();
                for (LinhKien l : lList) cboLinhKien.addItem(l);
            }
        }.execute();
    }

    private void chonPhieu(PhieuSuaChuaItem p) {
        if (p == null) return;
        txtMaPhieu.setText(p.maPhieu());
        txtKhachHang.setText(p.tenKhachHang() + " — " + p.soDienThoai());
        txtBienSoXe.setText(p.bienSoXe() + " · " + p.hangModel());
        txtNoiDungTiepNhan.setText(p.yeuCauBanDau());
        txtKyThuatVien.setText(p.tenKyThuatVien() == null ? "Chưa chỉ định (KTV tiếp nhận việc)" : p.tenKyThuatVien());
        txtTrangThaiHienTai.setText(tenTrangThai(p.trangThai()));

        cboTrangThaiCongViec.setSelectedItem(p.trangThai());

        boolean isDone = "HOAN_TAT".equals(p.trangThai());
        btnLuu.setEnabled(!isDone);
        btnThemDV.setEnabled(!isDone);
        btnBoDongDV.setEnabled(!isDone);
        btnThemLK.setEnabled(!isDone);
        btnBoDongLK.setEnabled(!isDone);
        cboTrangThaiCongViec.setEnabled(!isDone);

        lblFooterHint.setText(isDone ? "Phiếu đã ở trạng thái Hoàn tất và bị khóa chỉnh sửa." : "Có thể cập nhật dịch vụ, linh kiện và chuyển trạng thái công việc.");

        napBangDichVu(p.maPhieu());
        napBangLinhKien(p.maPhieu());
    }

    private void napBangDichVu(String maPhieu) {
        new SwingWorker<List<ChiTietDichVuItem>, Void>() {
            @Override protected List<ChiTietDichVuItem> doInBackground() {
                return controller.layChiTietDichVu(maPhieu);
            }
            @Override protected void done() {
                try {
                    List<ChiTietDichVuItem> list = get();
                    modelDV.setRowCount(0);
                    for (ChiTietDichVuItem it : list) {
                        modelDV.addRow(new Object[]{it.maDV(), it.tenDV(), "Lần", it.soLuong(), it.ghiChu() == null ? "—" : it.ghiChu()});
                    }
                    lblFooterDV.setText(list.isEmpty() ? "Chưa có dịch vụ thực hiện" : list.size() + " dịch vụ đã ghi nhận");
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void napBangLinhKien(String maPhieu) {
        new SwingWorker<List<ChiTietLinhKienItem>, Void>() {
            @Override protected List<ChiTietLinhKienItem> doInBackground() {
                return controller.layChiTietLinhKien(maPhieu);
            }
            @Override protected void done() {
                try {
                    List<ChiTietLinhKienItem> list = get();
                    modelLK.setRowCount(0);
                    for (ChiTietLinhKienItem it : list) {
                        modelLK.addRow(new Object[]{it.maLK(), it.tenLK(), it.donViTinh(), it.soLuong(), it.ghiChu() == null ? "—" : it.ghiChu()});
                    }
                    lblFooterLK.setText(list.isEmpty() ? "Chưa có linh kiện sử dụng" : list.size() + " linh kiện đã thay thế");
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void xuLyThemDichVu() {
        if (phieuDangChon == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn phiếu sửa chữa trước.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        DichVu dv = (DichVu) cboDichVu.getSelectedItem();
        if (dv == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn dịch vụ.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String rawSl = txtSoLuongDV.getText().trim();
        if (rawSl.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập số lượng dịch vụ.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            txtSoLuongDV.requestFocus();
            return;
        }
        int sl;
        try {
            sl = Integer.parseInt(rawSl);
            if (sl <= 0) {
                JOptionPane.showMessageDialog(this, "Số lượng dịch vụ phải là số nguyên dương (> 0).", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                txtSoLuongDV.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Số lượng dịch vụ không hợp lệ! Vui lòng chỉ nhập số nguyên dương.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            txtSoLuongDV.requestFocus();
            return;
        }

        try {
            controller.themDichVu(phieuDangChon.maPhieu(), dv.getMaDV(), sl, "Cập nhật từ form KTV");
            napBangDichVu(phieuDangChon.maPhieu());
            txtSoLuongDV.setText("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void xuLyXoaDichVu() {
        if (phieuDangChon == null) return;
        int r = tblDV.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Chọn một dòng dịch vụ trong bảng để bỏ dòng.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String maDV = (String) modelDV.getValueAt(r, 0);
        try {
            controller.xoaDichVu(phieuDangChon.maPhieu(), maDV);
            napBangDichVu(phieuDangChon.maPhieu());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void xuLyThemLinhKien() {
        if (phieuDangChon == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn phiếu sửa chữa trước.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        LinhKien lk = (LinhKien) cboLinhKien.getSelectedItem();
        if (lk == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn linh kiện.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String rawSl = txtSoLuongLK.getText().trim();
        if (rawSl.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập số lượng linh kiện.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            txtSoLuongLK.requestFocus();
            return;
        }
        int sl;
        try {
            sl = Integer.parseInt(rawSl);
            if (sl <= 0) {
                JOptionPane.showMessageDialog(this, "Số lượng linh kiện phải là số nguyên dương (> 0).", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                txtSoLuongLK.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Số lượng linh kiện không hợp lệ! Vui lòng chỉ nhập số nguyên dương.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            txtSoLuongLK.requestFocus();
            return;
        }

        try {
            controller.themLinhKien(phieuDangChon.maPhieu(), lk.getMaLK(), sl, "Cập nhật từ form KTV");
            napBangLinhKien(phieuDangChon.maPhieu());
            txtSoLuongLK.setText("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void xuLyXoaLinhKien() {
        if (phieuDangChon == null) return;
        int r = tblLK.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Chọn một dòng linh kiện trong bảng để bỏ dòng.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String maLK = (String) modelLK.getValueAt(r, 0);
        try {
            controller.xoaLinhKien(phieuDangChon.maPhieu(), maLK);
            napBangLinhKien(phieuDangChon.maPhieu());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void xuLyLuu() {
        if (phieuDangChon == null) return;
        String stMoi = (String) cboTrangThaiCongViec.getSelectedItem();
        if ("—".equals(stMoi) || stMoi == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn trạng thái công việc cần cập nhật.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!stMoi.equals(phieuDangChon.trangThai())) {
            try {
                controller.chuyenTrangThai(phieuDangChon.maPhieu(), stMoi);
                JOptionPane.showMessageDialog(this, "Cập nhật trạng thái phiếu thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                napDanhSachPhieu();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Đã lưu thông tin phiếu sửa chữa thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void xoaTrangForm() {
        txtMaPhieu.setText("—");
        txtKhachHang.setText("—");
        txtBienSoXe.setText("—");
        txtNoiDungTiepNhan.setText("—");
        txtKyThuatVien.setText("—");
        txtTrangThaiHienTai.setText("—");
        cboTrangThaiCongViec.setSelectedIndex(0);
        txtNoiDungThucHien.setText("");
        modelDV.setRowCount(0);
        modelLK.setRowCount(0);
        lblFooterDV.setText("Chưa có dữ liệu · Chưa chọn bản ghi");
        lblFooterLK.setText("Chưa có dữ liệu · Chưa chọn bản ghi");
        btnLuu.setEnabled(false);
        lblFooterHint.setText("Chưa thể lưu: chưa chọn phiếu được phân công và chưa xác định quyền cập nhật.");
    }

    private static void styleSubTable(JTable table) {
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setForeground(BODY);
        table.setRowHeight(36);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        table.setSelectionBackground(new Color(0xEDF4FF));
        table.setSelectionForeground(PRIMARY);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setForeground(BODY);
        table.getTableHeader().setBackground(new Color(0xF7F9FC));
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        if (table.getColumnCount() >= 4) {
            table.getColumnModel().getColumn(0).setCellRenderer(center);
            table.getColumnModel().getColumn(2).setCellRenderer(center);
            table.getColumnModel().getColumn(3).setCellRenderer(center);
        }
    }

    private static String tenTrangThai(String status) {
        return switch (status == null ? "" : status) {
            case "CHO_SUA" -> "Chờ sửa";
            case "DANG_SUA" -> "Đang sửa";
            case "CHO_LINH_KIEN" -> "Chờ linh kiện";
            case "HOAN_TAT" -> "Hoàn tất";
            default -> status;
        };
    }

    private static JLabel label(String text, int size, Color color, int style) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", style, size));
        l.setForeground(color);
        return l;
    }

    private static void addFull(JPanel parent, JComponent child) {
        child.setAlignmentX(Component.LEFT_ALIGNMENT);
        child.setMaximumSize(new Dimension(Integer.MAX_VALUE, child.getPreferredSize().height));
        parent.add(child);
    }

    private static void chiChoPhepNhapSo(JTextField field) {
        ((javax.swing.text.AbstractDocument) field.getDocument()).setDocumentFilter(new javax.swing.text.DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, javax.swing.text.AttributeSet attr)
                    throws javax.swing.text.BadLocationException {
                if (string != null && string.matches("\\d+")) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, javax.swing.text.AttributeSet attrs)
                    throws javax.swing.text.BadLocationException {
                if (text == null || text.isEmpty() || text.matches("\\d+")) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
    }

    private static JButton button(String text, boolean primary, int width) {
        JButton b = new MainFrame.RoundedButton(text, 10,
                primary ? PRIMARY : WHITE,
                primary ? PRIMARY_HV : new Color(0xF1F5F9),
                primary ? PRIMARY : new Color(0xC9D5E5));
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setForeground(primary ? WHITE : BODY);
        b.setPreferredSize(new Dimension(width, 38));
        b.setFocusPainted(false);
        return b;
    }
}
