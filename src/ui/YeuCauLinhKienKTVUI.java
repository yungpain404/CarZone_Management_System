package ui;

import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;
import controller.PhieuYeuCauNhapLinhKienController;
import dao.LinhKienDAO;
import entity.LinhKien;
import entity.PhieuYeuCauNhapLinhKien;
import enums.TrangThaiYeuCau;
import session.UserSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Màn hình Yêu cầu cập nhật linh kiện cho Kỹ thuật viên (Chuẩn Figma frame
 * 50:922).
 * Tái hiện 100% bố cục và thứ tự thành phần theo thiết kế Figma.
 */
public class YeuCauLinhKienKTVUI extends JPanel {

    private static final Color BG = new Color(0xF4F7FB);
    private static final Color WHITE = Color.WHITE;
    private static final Color BORDER = new Color(0xE3E9F2);
    private static final Color PRIMARY = new Color(0x2563EB);
    private static final Color PRIMARY_HV = new Color(0x1D4ED8);
    private static final Color TEXT = new Color(0x14233D);
    private static final Color BODY = new Color(0x40516A);
    private static final Color SUBTLE = new Color(0x7A889C);

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private final PhieuYeuCauNhapLinhKienController controller = new PhieuYeuCauNhapLinhKienController();
    private final LinhKienDAO linhKienDao = new LinhKienDAO();

    // Section 1: Lịch sử yêu cầu của tôi
    private final JTextField txtTimKiem = new JTextField();
    private final JComboBox<String> cboTrangThai = new JComboBox<>(
            new String[] { "Tất cả", "Chờ duyệt", "Đã duyệt", "Từ chối", "Đã nhập kho" });
    private final JDateChooser dcTuNgay = createDateChooser(170);
    private final JDateChooser dcDenNgay = createDateChooser(170);
    private final JButton btnTimKiem = button("Tìm kiếm", true, 110);
    private final JButton btnTaoMoiTop = button("Tạo mới", false, 100);

    // Filter Chips
    private final JButton btnChipChoDuyet = chipButton("Chờ duyệt");
    private final JButton btnChipDaDuyet = chipButton("Đã duyệt");
    private final JButton btnChipTuChoi = chipButton("Từ chối");
    private final JButton btnChipDaNhapKho = chipButton("Đã nhập kho");

    private final DefaultTableModel model = new DefaultTableModel(new Object[] {
            "Mã yêu cầu", "Linh kiện", "Số lượng", "Ngày gửi", "Trạng thái", "Phản hồi xử lý"
    }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel lblFooterTableLeft = label("Chưa có dữ liệu · Chưa chọn bản ghi", 12, SUBTLE, Font.PLAIN);
    private final JLabel lblFooterTableRight = label("Trạng thái chờ phê duyệt và phản hồi sẽ hiển thị khi có dữ liệu.",
            12, SUBTLE, Font.PLAIN);
    private final JButton btnXemChiTiet = button("Xem chi tiết yêu cầu", false, 180);
    private final JButton btnLamMoi = button("Làm mới", false, 100);

    // Section 2: Tạo yêu cầu mới (Ẩn mặc định, chỉ hiện khi nhấn Tạo mới)
    private JPanel pnlSectionTaoMoi;
    private final JTextField txtMaYeuCauMoi = createReadOnlyField("Chưa được cấp mã");
    private final JTextField txtNguoiYeuCau = createReadOnlyField(
            UserSession.getInstance().getNhanVien() != null ? UserSession.getInstance().getNhanVien().getHoTen()
                    : "Kỹ thuật viên");
    private final JComboBox<LinhKien> cboLinhKienChon = new JComboBox<>();
    private final JTextField txtSoLuongYeuCau = new JTextField();
    private final JTextField txtMaLKMoi = createReadOnlyField("—");
    private final JTextField txtTenLKMoi = createReadOnlyField("—");
    private final JTextField txtDonViMoi = createReadOnlyField("—");
    private final JTextField txtTonHienTaiMoi = createReadOnlyField("—");
    private final JTextField txtMoTaYeuCau = new JTextField();
    private final JLabel lblFormHint = label("Chưa thể gửi: chưa chọn linh kiện, nhập số lượng và mô tả bắt buộc.", 12,
            SUBTLE, Font.PLAIN);
    private final JButton btnGuiYeuCau = button("Gửi yêu cầu", true, 130);
    private final JButton btnHuyForm = button("Hủy", false, 90);

    private List<PhieuYeuCauNhapLinhKien> listData = new ArrayList<>();
    private PhieuYeuCauNhapLinhKien selectedItem = null;

    public YeuCauLinhKienKTVUI() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(22, 26, 22, 26));

        JPanel root = new MainFrame.ScrollableContentPanel();
        root.setOpaque(false);

        // Heading
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel headingText = new JPanel();
        headingText.setOpaque(false);
        headingText.setLayout(new BoxLayout(headingText, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleRow.setOpaque(false);
        titleRow.add(label("📖", 25, TEXT, Font.PLAIN));
        titleRow.add(label("Yêu cầu cập nhật linh kiện", 24, TEXT, Font.BOLD));
        headingText.add(titleRow);
        headingText.add(Box.createVerticalStrut(4));

        heading.add(headingText, BorderLayout.WEST);
        addFull(root, heading);
        root.add(Box.createVerticalStrut(18));

        // ── SECTION 1: Lịch sử yêu cầu của tôi ────────────────────────────────
        addFull(root, buildSectionLichSu());
        root.add(Box.createVerticalStrut(16));

        // ── SECTION 2: Tạo yêu cầu mới (Mặc định ẩn) ─────────────────────────
        pnlSectionTaoMoi = buildSectionTaoMoi();
        pnlSectionTaoMoi.setVisible(false); // Chỉ hiện khi bấm nút "Tạo mới"
        addFull(root, pnlSectionTaoMoi);

        // SECTION 3 (buildSectionPhanHoi) đã được loại bỏ theo yêu cầu người dùng

        JScrollPane scrollMain = new JScrollPane(root);
        scrollMain.setBorder(null);
        scrollMain.setOpaque(false);
        scrollMain.getViewport().setOpaque(false);
        scrollMain.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollMain.getVerticalScrollBar().setUnitIncrement(18);
        add(scrollMain, BorderLayout.CENTER);

        bindEvents();
        taiDanhMucLinhKien();
        taiDuLieu();
    }

    private JPanel buildSectionLichSu() {
        JPanel card = new TraCuuUI.RoundedPanel(WHITE, 12, BORDER, new Insets(18, 20, 18, 20));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Top bar
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JPanel topTitles = new JPanel();
        topTitles.setOpaque(false);
        topTitles.setLayout(new BoxLayout(topTitles, BoxLayout.Y_AXIS));
        topTitles.add(label("Lịch sử yêu cầu của tôi", 15, TEXT, Font.BOLD));
        topTitles.add(Box.createVerticalStrut(2));
        topTitles.add(label("Theo dõi yêu cầu đã gửi và trạng thái xử lý", 12, SUBTLE, Font.PLAIN));
        top.add(topTitles, BorderLayout.WEST);
        top.add(btnTaoMoiTop, BorderLayout.EAST);
        addFull(card, top);
        card.add(Box.createVerticalStrut(12));

        // Filters row
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterRow.setOpaque(false);
        filterRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtTimKiem.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtTimKiem.putClientProperty("JTextField.placeholderText", "Nhập mã hoặc tên…");
        JPanel boxSearch = new JPanel(new BorderLayout());
        boxSearch.setBackground(WHITE);
        boxSearch.setBorder(new TraCuuUI.RoundedLineBorder(new Color(0xC9D5E5), 10));
        txtTimKiem.setBorder(new EmptyBorder(0, 10, 0, 10));
        txtTimKiem.setPreferredSize(new Dimension(200, 38));
        boxSearch.add(txtTimKiem);

        filterRow.add(createMiniField("Tìm mã yêu cầu / linh kiện", boxSearch));
        cboTrangThai.setPreferredSize(new Dimension(140, 38));
        filterRow.add(createMiniField("Trạng thái yêu cầu", cboTrangThai));
        filterRow.add(createMiniField("Từ ngày", dcTuNgay));
        filterRow.add(createMiniField("Đến ngày", dcDenNgay));

        // Bọc button trong panel có label ẩn để canh hàng với các field có label phía
        // trên
        JPanel btnTimRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnTimRow.setOpaque(false);
        btnTimRow.add(btnTimKiem);
        filterRow.add(createMiniField(" ", btnTimRow));
        addFull(card, filterRow);
        card.add(Box.createVerticalStrut(10));
        // 

        // Chips row
        JPanel chipsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chipsRow.setOpaque(false);
        chipsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        chipsRow.add(btnChipChoDuyet);
        chipsRow.add(btnChipDaDuyet);
        chipsRow.add(btnChipTuChoi);
        chipsRow.add(btnChipDaNhapKho);
        addFull(card, chipsRow);
        card.add(Box.createVerticalStrut(12));

        // Table
        styleTable(table);
        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(new TraCuuUI.RoundedLineBorder(BORDER, 8));
        scrollTable.getViewport().setBackground(WHITE);
        scrollTable.setPreferredSize(new Dimension(940, 200));
        addFull(card, scrollTable);
        card.add(Box.createVerticalStrut(6));

        JPanel tableFooter = new JPanel(new BorderLayout());
        tableFooter.setOpaque(false);
        tableFooter.add(lblFooterTableLeft, BorderLayout.WEST);
        tableFooter.add(lblFooterTableRight, BorderLayout.EAST);
        addFull(card, tableFooter);
        card.add(Box.createVerticalStrut(10));

        // Bottom actions
        JPanel bottomActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bottomActions.setOpaque(false);
        bottomActions.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottomActions.add(btnXemChiTiet);
        bottomActions.add(btnLamMoi);
        addFull(card, bottomActions);

        return card;
    }

    private JPanel buildSectionTaoMoi() {
        JPanel card = new TraCuuUI.RoundedPanel(WHITE, 12, BORDER, new Insets(18, 20, 18, 20));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        addFull(card, label("Tạo yêu cầu mới", 15, TEXT, Font.BOLD));
        card.add(Box.createVerticalStrut(2));
        addFull(card,
                label("Gửi yêu cầu cập nhật linh kiện cho phiếu sửa chữa được phân công.", 12, SUBTLE, Font.PLAIN));
        card.add(Box.createVerticalStrut(14));

        card.add(createLabeledGroup("Mã yêu cầu", txtMaYeuCauMoi));
        card.add(Box.createVerticalStrut(10));
        card.add(createLabeledGroup("Người yêu cầu", txtNguoiYeuCau));
        card.add(Box.createVerticalStrut(10));

        cboLinhKienChon.setPreferredSize(new Dimension(Integer.MAX_VALUE, 40));
        cboLinhKienChon.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof LinhKien lk) {
                    setText(lk.getMaLK() + " — " + lk.getTenLK() + " (Tồn: " + lk.getSoLuongTon() + " "
                            + lk.getDonViTinh() + ")");
                } else {
                    setText("Tìm và chọn linh kiện");
                }
                return this;
            }
        });
        card.add(createLabeledGroup("Linh kiện liên quan *", cboLinhKienChon));
        card.add(Box.createVerticalStrut(10));

        txtSoLuongYeuCau.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtSoLuongYeuCau.putClientProperty("JTextField.placeholderText", "Nhập số lượng");
        chiChoPhepNhapSo(txtSoLuongYeuCau);
        JPanel boxSL = new JPanel(new BorderLayout());
        boxSL.setBackground(WHITE);
        boxSL.setBorder(new TraCuuUI.RoundedLineBorder(new Color(0xC9D5E5), 10));
        txtSoLuongYeuCau.setBorder(new EmptyBorder(0, 10, 0, 10));
        txtSoLuongYeuCau.setPreferredSize(new Dimension(Integer.MAX_VALUE, 40));
        boxSL.add(txtSoLuongYeuCau);
        card.add(createLabeledGroup("Số lượng yêu cầu *", boxSL));
        card.add(Box.createVerticalStrut(10));

        // 4 Ô chỉ đọc linh kiện
        card.add(createLabeledGroup("Mã linh kiện", txtMaLKMoi));
        card.add(Box.createVerticalStrut(10));
        card.add(createLabeledGroup("Tên linh kiện", txtTenLKMoi));
        card.add(Box.createVerticalStrut(10));
        card.add(createLabeledGroup("Đơn vị tính", txtDonViMoi));
        card.add(Box.createVerticalStrut(10));
        card.add(createLabeledGroup("Tồn hiện tại", txtTonHienTaiMoi));
        card.add(Box.createVerticalStrut(10));

        // Mô tả yêu cầu *
        txtMoTaYeuCau.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtMoTaYeuCau.putClientProperty("JTextField.placeholderText",
                "Nêu nội dung và lý do đề nghị cập nhật linh kiện");
        JPanel boxMoTa = new JPanel(new BorderLayout());
        boxMoTa.setBackground(WHITE);
        boxMoTa.setBorder(new TraCuuUI.RoundedLineBorder(new Color(0xC9D5E5), 10));
        txtMoTaYeuCau.setBorder(new EmptyBorder(0, 10, 0, 10));
        txtMoTaYeuCau.setPreferredSize(new Dimension(Integer.MAX_VALUE, 40));
        boxMoTa.add(txtMoTaYeuCau);
        card.add(createLabeledGroup("Mô tả yêu cầu *", boxMoTa));
        card.add(Box.createVerticalStrut(12));

        addFull(card, lblFormHint);
        card.add(Box.createVerticalStrut(8));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.add(btnGuiYeuCau);
        btnRow.add(btnHuyForm);
        addFull(card, btnRow);

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

    private static JDateChooser createDateChooser(int width) {
        JDateChooser chooser = new JDateChooser();
        chooser.setDateFormatString("dd/MM/yyyy");
        chooser.setFont(new Font("SansSerif", Font.PLAIN, 13));
        chooser.setPreferredSize(new Dimension(width, 38));
        chooser.setBackground(WHITE);
        if (chooser.getDateEditor() instanceof JTextFieldDateEditor editor) {
            editor.setEditable(false); // Tuyệt đối không cho gõ phím, bắt buộc chọn từ lịch
            editor.setBackground(WHITE);
            editor.setFont(new Font("SansSerif", Font.PLAIN, 13));
            editor.setBorder(new EmptyBorder(0, 10, 0, 2));
            editor.setMargin(new Insets(0, 0, 0, 0));
        }
        JButton calBtn = chooser.getCalendarButton();
        if (calBtn != null) {
            calBtn.setPreferredSize(new Dimension(28, 28));
            calBtn.setMargin(new Insets(0, 0, 0, 0));
            calBtn.setFocusable(false);
            calBtn.setContentAreaFilled(false);
            calBtn.setBorder(new EmptyBorder(0, 0, 0, 4));
        }
        chooser.setBorder(new TraCuuUI.RoundedLineBorder(new Color(0xC9D5E5), 10));
        return chooser;
    }

    private static JTextField createReadOnlyField(String text) {
        JTextField tf = new JTextField(text);
        tf.setEditable(false);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tf.setForeground(TEXT);
        tf.setBackground(new Color(0xF8FAFC));
        tf.setBorder(new TraCuuUI.RoundedLineBorder(new Color(0xE2E8F0), 10));
        tf.setPreferredSize(new Dimension(Integer.MAX_VALUE, 40));
        return tf;
    }

    private static JButton chipButton(String text) {
        JButton b = button(text, false, 110);
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        b.setPreferredSize(new Dimension(110, 32));
        return b;
    }

    private void bindEvents() {
        btnTimKiem.addActionListener(e -> taiDuLieu());
        txtTimKiem.addActionListener(e -> taiDuLieu());
        btnLamMoi.addActionListener(e -> {
            txtTimKiem.setText("");
            cboTrangThai.setSelectedIndex(0);
            dcTuNgay.setDate(null);
            dcDenNgay.setDate(null);
            taiDuLieu();
        });

        // Chips click
        btnChipChoDuyet.addActionListener(e -> {
            cboTrangThai.setSelectedItem("Chờ duyệt");
            taiDuLieu();
        });
        btnChipDaDuyet.addActionListener(e -> {
            cboTrangThai.setSelectedItem("Đã duyệt");
            taiDuLieu();
        });
        btnChipTuChoi.addActionListener(e -> {
            cboTrangThai.setSelectedItem("Từ chối");
            taiDuLieu();
        });
        btnChipDaNhapKho.addActionListener(e -> {
            cboTrangThai.setSelectedItem("Đã nhập kho");
            taiDuLieu();
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int viewRow = table.getSelectedRow();
                if (viewRow >= 0 && viewRow < listData.size()) {
                    int modelRow = table.convertRowIndexToModel(viewRow);
                    selectedItem = listData.get(modelRow);
                }
            }
        });

        // Nút Xem chi tiết yêu cầu
        btnXemChiTiet.addActionListener(e -> hienThiDialogChiTiet());

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    hienThiDialogChiTiet();
                }
            }
        });

        cboLinhKienChon.addActionListener(e -> {
            LinhKien lk = (LinhKien) cboLinhKienChon.getSelectedItem();
            if (lk != null) {
                txtMaLKMoi.setText(lk.getMaLK());
                txtTenLKMoi.setText(lk.getTenLK());
                txtDonViMoi.setText(lk.getDonViTinh());
                txtTonHienTaiMoi.setText(String.valueOf(lk.getSoLuongTon()));
            } else {
                txtMaLKMoi.setText("—");
                txtTenLKMoi.setText("—");
                txtDonViMoi.setText("—");
                txtTonHienTaiMoi.setText("—");
            }
        });

        // Bấm Tạo mới: hiện section Tạo mới và sinh mã dự kiến
        btnTaoMoiTop.addActionListener(e -> {
            pnlSectionTaoMoi.setVisible(true);
            capNhatMaYeuCauMoi();
            revalidate();
            repaint();
            txtSoLuongYeuCau.requestFocusInWindow();
        });

        btnGuiYeuCau.addActionListener(e -> xuLyGuiYeuCau());

        // Bấm Hủy form: ẩn section Tạo mới
        btnHuyForm.addActionListener(e -> {
            xoaTrangFormTaoMoi();
            pnlSectionTaoMoi.setVisible(false);
            revalidate();
            repaint();
        });
    }

    private void capNhatMaYeuCauMoi() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    return controller.layMaYeuCauTiepTheo();
                } catch (Exception ex) {
                    return "PYC-TỰ-SINH";
                }
            }

            @Override
            protected void done() {
                try {
                    txtMaYeuCauMoi.setText(get());
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void taiDanhMucLinhKien() {
        new SwingWorker<List<LinhKien>, Void>() {
            @Override
            protected List<LinhKien> doInBackground() {
                return linhKienDao.timKiem(null, true, null);
            }

            @Override
            protected void done() {
                try {
                    List<LinhKien> list = get();
                    cboLinhKienChon.removeAllItems();
                    for (LinhKien lk : list)
                        cboLinhKienChon.addItem(lk);
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void taiDuLieu() {
        String kw = txtTimKiem.getText().trim();
        String stName = (String) cboTrangThai.getSelectedItem();
        TrangThaiYeuCau st = switch (stName == null ? "" : stName) {
            case "Chờ duyệt" -> TrangThaiYeuCau.CHO_DUYET;
            case "Đã duyệt" -> TrangThaiYeuCau.DA_DUYET;
            case "Từ chối" -> TrangThaiYeuCau.TU_CHOI;
            case "Đã nhập kho" -> TrangThaiYeuCau.DA_NHAP_KHO;
            default -> null;
        };

        Date tuNgay = dcTuNgay.getDate();
        Date denNgay = dcDenNgay.getDate();

        new SwingWorker<List<PhieuYeuCauNhapLinhKien>, Void>() {
            @Override
            protected List<PhieuYeuCauNhapLinhKien> doInBackground() {
                return controller.timKiemCuaKTV(kw, st, tuNgay, denNgay);
            }

            @Override
            protected void done() {
                try {
                    listData = get();
                    model.setRowCount(0);
                    for (PhieuYeuCauNhapLinhKien p : listData) {
                        model.addRow(new Object[] {
                                p.getMaYeuCau(),
                                p.getLinhKien().getTenLK(),
                                p.getSoLuongYeuCau(),
                                p.getNgayTao() == null ? "" : DATE_FORMAT.format(p.getNgayTao()),
                                tenTrangThai(p.getTrangThai()),
                                p.getGhiChuXuLy() == null ? "—" : p.getGhiChuXuLy()
                        });
                    }

                    if (!listData.isEmpty()) {
                        table.setRowSelectionInterval(0, 0);
                        selectedItem = listData.get(0);
                        lblFooterTableLeft.setText(listData.size() + " yêu cầu");
                    } else {
                        selectedItem = null;
                        lblFooterTableLeft.setText("Chưa có dữ liệu yêu cầu");
                    }
                } catch (Exception ex) {
                    lblFooterTableLeft.setText("Lỗi nạp dữ liệu: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void hienThiDialogChiTiet() {
        if (selectedItem == null) {
            int row = table.getSelectedRow();
            if (row >= 0 && row < listData.size()) {
                selectedItem = listData.get(table.convertRowIndexToModel(row));
            }
        }
        if (selectedItem == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một yêu cầu trong bảng để xem chi tiết.", "Thông báo",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chi tiết yêu cầu nhập linh kiện",
                true);
        dlg.setLayout(new BorderLayout());
        dlg.setSize(520, 480);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel pnl = new JPanel();
        pnl.setBackground(WHITE);
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        pnl.setBorder(new EmptyBorder(20, 24, 20, 24));

        pnl.add(label("Chi tiết yêu cầu " + selectedItem.getMaYeuCau(), 16, TEXT, Font.BOLD));
        pnl.add(Box.createVerticalStrut(14));

        pnl.add(rowDetail("Mã yêu cầu:", selectedItem.getMaYeuCau()));
        pnl.add(rowDetail("Linh kiện:",
                selectedItem.getLinhKien() != null
                        ? selectedItem.getLinhKien().getMaLK() + " - " + selectedItem.getLinhKien().getTenLK()
                        : "—"));
        pnl.add(rowDetail("Số lượng yêu cầu:", selectedItem.getSoLuongYeuCau()
                + (selectedItem.getLinhKien() != null ? " " + selectedItem.getLinhKien().getDonViTinh() : "")));
        if (selectedItem.getSoLuongNhapThucTe() != null) {
            pnl.add(rowDetail("Số lượng nhập thực tế:", selectedItem.getSoLuongNhapThucTe() + ""));
        }
        pnl.add(rowDetail("Ngày gửi:",
                selectedItem.getNgayTao() != null ? DATE_FORMAT.format(selectedItem.getNgayTao()) : "—"));
        pnl.add(rowDetail("Người gửi:",
                selectedItem.getNhanVienLap() != null ? selectedItem.getNhanVienLap().getHoTen() : "Kỹ thuật viên"));
        pnl.add(rowDetail("Trạng thái:", tenTrangThai(selectedItem.getTrangThai())));
        pnl.add(rowDetail("Lý do / Mô tả:", selectedItem.getGhiChu() != null ? selectedItem.getGhiChu() : "—"));
        pnl.add(Box.createVerticalStrut(8));

        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        pnl.add(sep);
        pnl.add(Box.createVerticalStrut(8));

        pnl.add(label("Phản hồi từ Chủ cửa hàng", 13, PRIMARY, Font.BOLD));
        pnl.add(Box.createVerticalStrut(6));
        pnl.add(rowDetail("Người xử lý:",
                selectedItem.getNhanVienXuLy() != null ? selectedItem.getNhanVienXuLy().getHoTen() : "Chưa xử lý"));
        pnl.add(rowDetail("Thời gian xử lý:",
                selectedItem.getNgayXuLy() != null ? DATE_FORMAT.format(selectedItem.getNgayXuLy()) : "—"));
        pnl.add(rowDetail("Ý kiến phản hồi:",
                selectedItem.getGhiChuXuLy() != null && !selectedItem.getGhiChuXuLy().isBlank()
                        ? selectedItem.getGhiChuXuLy()
                        : "Chưa có phản hồi"));

        pnl.add(Box.createVerticalGlue());

        JButton btnDong = button("Đóng", true, 100);
        btnDong.addActionListener(ev -> dlg.dispose());
        JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pnlBottom.setOpaque(false);
        pnlBottom.add(btnDong);
        pnl.add(pnlBottom);

        dlg.add(pnl, BorderLayout.CENTER);
        dlg.setVisible(true);
    }

    private JPanel rowDetail(String title, String val) {
        JPanel r = new JPanel(new BorderLayout(8, 0));
        r.setOpaque(false);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        JLabel lblT = label(title, 12, SUBTLE, Font.BOLD);
        lblT.setPreferredSize(new Dimension(140, 24));
        JLabel lblV = label(val, 12, TEXT, Font.PLAIN);
        r.add(lblT, BorderLayout.WEST);
        r.add(lblV, BorderLayout.CENTER);
        return r;
    }

    private void xuLyGuiYeuCau() {
        LinhKien lk = (LinhKien) cboLinhKienChon.getSelectedItem();
        if (lk == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn linh kiện.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String rawSl = txtSoLuongYeuCau.getText().trim();
        if (rawSl.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập số lượng yêu cầu.", "Cảnh báo",
                    JOptionPane.WARNING_MESSAGE);
            txtSoLuongYeuCau.requestFocus();
            return;
        }

        int sl;
        try {
            sl = Integer.parseInt(rawSl);
            if (sl <= 0) {
                JOptionPane.showMessageDialog(this, "Số lượng yêu cầu phải là số nguyên dương (> 0).", "Cảnh báo",
                        JOptionPane.WARNING_MESSAGE);
                txtSoLuongYeuCau.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Số lượng yêu cầu không hợp lệ! Vui lòng chỉ nhập số nguyên.",
                    "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            txtSoLuongYeuCau.requestFocus();
            return;
        }

        String moTa = txtMoTaYeuCau.getText().trim();
        if (moTa.isBlank()) {
            JOptionPane.showMessageDialog(this, "Mô tả / lý do yêu cầu là bắt buộc.", "Cảnh báo",
                    JOptionPane.WARNING_MESSAGE);
            txtMoTaYeuCau.requestFocus();
            return;
        }

        try {
            String maMoi = controller.taoYeuCau(lk.getMaLK(), sl, moTa);
            JOptionPane.showMessageDialog(this, "Tạo yêu cầu " + maMoi + " thành công!", "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            xoaTrangFormTaoMoi();
            pnlSectionTaoMoi.setVisible(false);
            revalidate();
            repaint();
            taiDuLieu();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void xoaTrangFormTaoMoi() {
        txtSoLuongYeuCau.setText("");
        txtMoTaYeuCau.setText("");
        txtMaYeuCauMoi.setText("Chưa được cấp mã");
    }

    private static void styleTable(JTable table) {
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setForeground(BODY);
        table.setRowHeight(38);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        table.setSelectionBackground(new Color(0xEDF4FF));
        table.setSelectionForeground(PRIMARY);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setForeground(BODY);
        table.getTableHeader().setBackground(new Color(0xF7F9FC));
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.setAutoCreateRowSorter(true);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);
        table.getColumnModel().getColumn(2).setCellRenderer(center);
        table.getColumnModel().getColumn(3).setCellRenderer(center);
        table.getColumnModel().getColumn(4).setCellRenderer(new StatusBadgeRenderer());
    }

    public static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int col) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            if (value == null)
                return lbl;
            String s = value.toString().trim();
            Color bg = new Color(0xF1F5F9);
            Color fg = new Color(0x64748B);
            if (s.contains("Đã duyệt")) {
                bg = new Color(0xECFDF5);
                fg = new Color(0x059669);
            } else if (s.contains("Đã nhập kho")) {
                bg = new Color(0xEFF6FF);
                fg = new Color(0x2563EB);
            } else if (s.contains("Chờ duyệt")) {
                bg = new Color(0xFFFBEB);
                fg = new Color(0xD97706);
            } else if (s.contains("Từ chối")) {
                bg = new Color(0xFEF2F2);
                fg = new Color(0xDC2626);
            }
            if (!isSelected) {
                lbl.setBackground(bg);
                lbl.setForeground(fg);
                lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            }
            return lbl;
        }
    }

    private static String tenTrangThai(TrangThaiYeuCau status) {
        if (status == null)
            return "";
        return switch (status) {
            case CHO_DUYET -> "Chờ duyệt";
            case DA_DUYET -> "Đã duyệt";
            case TU_CHOI -> "Từ chối";
            case DA_NHAP_KHO -> "Đã nhập kho";
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
