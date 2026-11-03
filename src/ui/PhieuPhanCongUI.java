package ui;

import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;
import controller.PhieuTiepNhanSuaChuaController;
import dao.PhieuTiepNhanSuaChuaDAO.PhieuSuaChuaItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * Màn hình Phiếu được phân công cho Kỹ thuật viên (Figma frame 50:701).
 * - Danh sách xe và phiếu sửa chữa được bàn giao.
 * - Bộ lọc mã phiếu / biển số, trạng thái công việc, khoảng ngày.
 * - Khối Chi tiết phiếu đang chọn · Chỉ đọc bên dưới.
 * - Chuyển nhanh sang màn hình Cập nhật phiếu sửa chữa.
 */
public class PhieuPhanCongUI extends JPanel {

    private static final Color BG = new Color(0xF4F7FB);
    private static final Color WHITE = Color.WHITE;
    private static final Color BORDER = new Color(0xE3E9F2);
    private static final Color PRIMARY = new Color(0x2563EB);
    private static final Color PRIMARY_HV = new Color(0x1D4ED8);
    private static final Color TEXT = new Color(0x14233D);
    private static final Color BODY = new Color(0x40516A);
    private static final Color SUBTLE = new Color(0x7A889C);

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat DATE_TIME = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private final PhieuTiepNhanSuaChuaController controller = new PhieuTiepNhanSuaChuaController();
    private final Consumer<String> onMoCapNhat;

    // Filters
    private final JTextField txtTimKiem = new JTextField();
    private final JComboBox<String> cboTrangThai = new JComboBox<>(
            new String[] { "Tất cả", "Chờ sửa", "Đang sửa", "Chờ linh kiện", "Hoàn tất" });
    private final JDateChooser dcTuNgay = createDateChooser(170);
    private final JDateChooser dcDenNgay = createDateChooser(170);
    private final JButton btnTimKiem = button("Tìm kiếm", true, 110);
    private final JButton btnLamMoi = button("Làm mới", false, 100);

    // Actions
    private final JButton btnXemChiTiet = button("Xem chi tiết", false, 130);
    private final JButton btnCapNhatPhieu = button("Cập nhật phiếu sửa chữa", false, 220);

    // Table
    private final DefaultTableModel model = new DefaultTableModel(new Object[] {
            "Mã phiếu sửa chữa", "Biển số", "Hãng / dòng xe", "Ngày phân công", "Nội dung phân công", "Trạng thái"
    }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel lblFooterLeft = label("Chưa có dữ liệu · Chưa chọn bản ghi", 12, SUBTLE, Font.PLAIN);
    private final JLabel lblFooterRight = label("Đã kết nối nguồn dữ liệu", 12, SUBTLE, Font.PLAIN);

    // Chi tiết chỉ đọc bên dưới
    private final JLabel lblSubDetail = label("Chưa chọn phiếu. Thông tin phân công chưa khả dụng.", 13, SUBTLE,
            Font.PLAIN);
    private final JLabel lblDetailMaPhieu = label("—", 13, TEXT, Font.BOLD);
    private final JLabel lblDetailXe = label("—", 13, TEXT, Font.BOLD);
    private final JLabel lblDetailTrangThai = label("—", 13, TEXT, Font.BOLD);
    private final JLabel lblDetailNgayPhanCong = label("—", 13, TEXT, Font.BOLD);
    private final JLabel lblDetailNoiDung = label("—", 13, TEXT, Font.BOLD);

    private List<PhieuSuaChuaItem> listPhieu = new ArrayList<>();
    private PhieuSuaChuaItem phieuDangChon = null;

    public PhieuPhanCongUI(Consumer<String> onMoCapNhat) {
        this.onMoCapNhat = onMoCapNhat;

        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(22, 26, 22, 26));

        JPanel root = new MainFrame.ScrollableContentPanel();
        root.setOpaque(false);

        // Heading
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        heading.setOpaque(false);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(label("📋", 25, TEXT, Font.PLAIN));
        heading.add(label("Phiếu được phân công", 24, TEXT, Font.BOLD));
        addFull(root, heading);
        root.add(Box.createVerticalStrut(16));

        // Filter Bar (Figma 50:701)
        JPanel filterCard = new TraCuuUI.RoundedPanel(WHITE, 12, BORDER, new Insets(14, 18, 14, 18));
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 0));
        filterCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        filterCard.add(createField("Mã phiếu / biển số", txtTimKiem, 220, "Nhập mã hoặc biển số…"));
        filterCard.add(createField("Trạng thái công việc", cboTrangThai, 160, null));
        filterCard.add(createField("Từ ngày", dcTuNgay, 160, null));
        filterCard.add(createField("Đến ngày", dcDenNgay, 160, null));

        // Bọc button trong panel có label ẩn để canh hàng với các field (label chiếm
        // cùng độ cao)
        JPanel btnWrapper = new JPanel(new BorderLayout(0, 4));
        btnWrapper.setOpaque(false);
        JLabel lblPlaceholder = label(" ", 12, BODY, Font.PLAIN); // label ẩn canh hàng
        btnWrapper.add(lblPlaceholder, BorderLayout.NORTH);
        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnGroup.setOpaque(false);
        btnGroup.add(btnTimKiem);
        btnGroup.add(btnLamMoi);
        btnWrapper.add(btnGroup, BorderLayout.CENTER);
        filterCard.add(btnWrapper);

        addFull(root, filterCard);
        root.add(Box.createVerticalStrut(14));

        // Toolbar nút tác vụ trên bảng
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionRow.setOpaque(false);
        actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionRow.add(btnXemChiTiet);
        actionRow.add(btnCapNhatPhieu);
        addFull(root, actionRow);
        root.add(Box.createVerticalStrut(10));

        // Table
        styleTable(table);
        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(null);
        scrollTable.getViewport().setBackground(WHITE);
        scrollTable.setPreferredSize(new Dimension(980, 280));

        JPanel tableCard = new TraCuuUI.RoundedPanel(WHITE, 12, BORDER, new Insets(8, 8, 8, 8));
        tableCard.setLayout(new BorderLayout());
        tableCard.add(scrollTable, BorderLayout.CENTER);

        JPanel tableFooter = new JPanel(new BorderLayout());
        tableFooter.setOpaque(false);
        tableFooter.setBorder(new EmptyBorder(8, 8, 4, 8));
        tableFooter.add(lblFooterLeft, BorderLayout.WEST);
        tableFooter.add(lblFooterRight, BorderLayout.EAST);
        tableCard.add(tableFooter, BorderLayout.SOUTH);

        addFull(root, tableCard);
        root.add(Box.createVerticalStrut(16));

        // Section: Chi tiết phiếu đang chọn · Chỉ đọc (Figma 50:701)
        JPanel detailCard = new TraCuuUI.RoundedPanel(WHITE, 12, BORDER, new Insets(18, 20, 18, 20));
        detailCard.setLayout(new BoxLayout(detailCard, BoxLayout.Y_AXIS));
        detailCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        detailCard.add(label("Chi tiết phiếu đang chọn", 15, TEXT, Font.BOLD));
        detailCard.add(Box.createVerticalStrut(4));
        detailCard.add(lblSubDetail);
        detailCard.add(Box.createVerticalStrut(14));

        JPanel row1 = new JPanel(new GridLayout(1, 3, 12, 0));
        row1.setOpaque(false);
        row1.add(createInfoBox("Mã phiếu", lblDetailMaPhieu));
        row1.add(createInfoBox("Xe", lblDetailXe));
        row1.add(createInfoBox("Trạng thái", lblDetailTrangThai));
        addFull(detailCard, row1);
        detailCard.add(Box.createVerticalStrut(10));

        JPanel row2 = new JPanel(new GridLayout(1, 2, 12, 0));
        row2.setOpaque(false);
        row2.add(createInfoBox("Ngày phân công", lblDetailNgayPhanCong));
        row2.add(createInfoBox("Nội dung phân công", lblDetailNoiDung));
        addFull(detailCard, row2);

        addFull(root, detailCard);

        JScrollPane scrollMain = new JScrollPane(root);
        scrollMain.setBorder(null);
        scrollMain.setOpaque(false);
        scrollMain.getViewport().setOpaque(false);
        scrollMain.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollMain.getVerticalScrollBar().setUnitIncrement(18);
        add(scrollMain, BorderLayout.CENTER);

        bindEvents();
        taiDuLieu();
    }

    private JPanel createField(String title, JComponent comp, int width, String placeholder) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.add(label(title, 12, BODY, Font.PLAIN), BorderLayout.NORTH);
        comp.setFont(new Font("SansSerif", Font.PLAIN, 13));

        if (comp instanceof JTextField tf) {
            if (placeholder != null)
                tf.putClientProperty("JTextField.placeholderText", placeholder);
            JPanel box = new JPanel(new BorderLayout());
            box.setBackground(WHITE);
            box.setBorder(new TraCuuUI.RoundedLineBorder(new Color(0xC9D5E5), 10));
            tf.setBorder(new EmptyBorder(0, 10, 0, 10));
            tf.setPreferredSize(new Dimension(width, 38));
            box.add(tf);
            p.add(box, BorderLayout.CENTER);
        } else {
            comp.setPreferredSize(new Dimension(width, 38));
            p.add(comp, BorderLayout.CENTER);
        }
        return p;
    }

    private static JDateChooser createDateChooser(int width) {
        JDateChooser chooser = new JDateChooser();
        chooser.setDateFormatString("dd/MM/yyyy");
        chooser.setFont(new Font("SansSerif", Font.PLAIN, 13));
        chooser.setPreferredSize(new Dimension(width, 38));
        chooser.setBackground(WHITE);
        if (chooser.getDateEditor() instanceof JTextFieldDateEditor editor) {
            editor.setEditable(false);
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

    private JPanel createInfoBox(String labelText, JLabel lblVal) {
        JPanel box = new TraCuuUI.RoundedPanel(new Color(0xF8FAFC), 10, new Color(0xEDF2F7),
                new Insets(10, 14, 10, 14));
        box.setLayout(new BorderLayout(0, 4));
        box.add(label(labelText, 11, SUBTLE, Font.PLAIN), BorderLayout.NORTH);
        box.add(lblVal, BorderLayout.CENTER);
        return box;
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

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && phieuDangChon != null && onMoCapNhat != null) {
                    onMoCapNhat.accept(phieuDangChon.maPhieu());
                }
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int viewRow = table.getSelectedRow();
                if (viewRow >= 0 && viewRow < listPhieu.size()) {
                    int modelRow = table.convertRowIndexToModel(viewRow);
                    phieuDangChon = listPhieu.get(modelRow);
                    hienThiChiTiet(phieuDangChon);
                }
            }
        });

        btnXemChiTiet.addActionListener(e -> {
            if (phieuDangChon != null) {
                hienThiChiTiet(phieuDangChon);
                JOptionPane.showMessageDialog(this,
                        "Mã phiếu: " + phieuDangChon.maPhieu() + "\n"
                                + "Xe: " + phieuDangChon.bienSoXe() + " - " + phieuDangChon.hangModel() + "\n"
                                + "Khách hàng: " + phieuDangChon.tenKhachHang() + " (" + phieuDangChon.soDienThoai()
                                + ")\n"
                                + "Yêu cầu: " + phieuDangChon.yeuCauBanDau() + "\n"
                                + "Trạng thái: " + tenTrangThai(phieuDangChon.trangThai()),
                        "Chi tiết phiếu sửa chữa", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        btnCapNhatPhieu.addActionListener(e -> {
            if (phieuDangChon != null && onMoCapNhat != null) {
                onMoCapNhat.accept(phieuDangChon.maPhieu());
            }
        });
    }

    private void taiDuLieu() {
        String tuKhoa = txtTimKiem.getText().trim();
        String stName = (String) cboTrangThai.getSelectedItem();
        String st = switch (stName == null ? "" : stName) {
            case "Chờ sửa" -> "CHO_SUA";
            case "Đang sửa" -> "DANG_SUA";
            case "Chờ linh kiện" -> "CHO_LINH_KIEN";
            case "Hoàn tất" -> "HOAN_TAT";
            default -> null;
        };

        Date tuNgay = dcTuNgay.getDate();
        Date denNgay = dcDenNgay.getDate();

        new SwingWorker<List<PhieuSuaChuaItem>, Void>() {
            @Override
            protected List<PhieuSuaChuaItem> doInBackground() {
                return controller.timPhieuKTV(tuKhoa, st, tuNgay, denNgay);
            }

            @Override
            protected void done() {
                try {
                    listPhieu = get();
                    model.setRowCount(0);
                    for (PhieuSuaChuaItem p : listPhieu) {
                        model.addRow(new Object[] {
                                p.maPhieu(),
                                p.bienSoXe(),
                                p.hangModel(),
                                DATE_FORMAT.format(p.ngayTiepNhan()),
                                p.yeuCauBanDau(),
                                tenTrangThai(p.trangThai())
                        });
                    }

                    if (!listPhieu.isEmpty()) {
                        table.setRowSelectionInterval(0, 0);
                        lblFooterLeft.setText(listPhieu.size() + " phiếu được phân công");
                        btnXemChiTiet.setEnabled(true);
                        btnCapNhatPhieu.setEnabled(true);
                    } else {
                        phieuDangChon = null;
                        xoaTrangChiTiet();
                        lblFooterLeft.setText("Chưa có dữ liệu phiếu được phân công");
                        btnXemChiTiet.setEnabled(false);
                        btnCapNhatPhieu.setEnabled(false);
                    }
                } catch (Exception ex) {
                    lblFooterLeft.setText("Lỗi nạp dữ liệu");
                }
            }
        }.execute();
    }

    private void hienThiChiTiet(PhieuSuaChuaItem p) {
        if (p == null)
            return;
        lblSubDetail.setText("Thông tin phân công chi tiết của phiếu " + p.maPhieu() + ".");
        lblDetailMaPhieu.setText(p.maPhieu());
        lblDetailXe.setText(p.bienSoXe() + " · " + p.hangModel());
        lblDetailTrangThai.setText(tenTrangThai(p.trangThai()));
        lblDetailNgayPhanCong.setText(DATE_TIME.format(p.ngayTiepNhan()));
        lblDetailNoiDung.setText(p.yeuCauBanDau() == null ? "—" : p.yeuCauBanDau());
        btnXemChiTiet.setEnabled(true);
        btnCapNhatPhieu.setEnabled(true);
    }

    private void xoaTrangChiTiet() {
        lblSubDetail.setText("Chưa chọn phiếu. Thông tin phân công chưa khả dụng.");
        lblDetailMaPhieu.setText("—");
        lblDetailXe.setText("—");
        lblDetailTrangThai.setText("—");
        lblDetailNgayPhanCong.setText("—");
        lblDetailNoiDung.setText("—");
    }

    private static Date parseDate(String text) {
        if (text == null || text.isBlank())
            return null;
        try {
            return DATE_FORMAT.parse(text.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static void styleTable(JTable table) {
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setForeground(BODY);
        table.setRowHeight(40);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        table.setSelectionBackground(new Color(0xEDF4FF));
        table.setSelectionForeground(PRIMARY);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setForeground(BODY);
        table.getTableHeader().setBackground(new Color(0xF7F9FC));
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.setAutoCreateRowSorter(true);

        // Can giua / can le dep
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);
        table.getColumnModel().getColumn(1).setCellRenderer(center);
        table.getColumnModel().getColumn(3).setCellRenderer(center);
        table.getColumnModel().getColumn(5).setCellRenderer(new StatusBadgeRenderer());
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
            if (s.contains("Hoàn tất") || s.contains("Hoàn thành")) {
                bg = new Color(0xECFDF5);
                fg = new Color(0x059669);
            } else if (s.contains("Đang sửa")) {
                bg = new Color(0xEFF6FF);
                fg = new Color(0x2563EB);
            } else if (s.contains("Chờ linh kiện")) {
                bg = new Color(0xFFFBEB);
                fg = new Color(0xD97706);
            } else if (s.contains("Chờ sửa") || s.contains("Mới")) {
                bg = new Color(0xF1F5F9);
                fg = new Color(0x64748B);
            }
            if (!isSelected) {
                lbl.setBackground(bg);
                lbl.setForeground(fg);
                lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            }
            return lbl;
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

    private static JButton button(String text, boolean primary, int width) {
        JButton b = new MainFrame.RoundedButton(text, 12,
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
