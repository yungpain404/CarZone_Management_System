package ui;

import controller.PhieuYeuCauNhapLinhKienController;
import entity.PhieuYeuCauNhapLinhKien;
import enums.TrangThaiYeuCau;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Tra cứu và xử lý yêu cầu nhập linh kiện cho Chủ cửa hàng. */
public class XuLyYeuCauLinhKienUI extends JPanel {
    private static final Color BG = new Color(0xF8FAFC);
    private static final Color WHITE = Color.WHITE;
    private static final Color BORDER = new Color(0xDCE4EF);
    private static final Color PRIMARY = new Color(0x2563EB);
    private static final Color PRIMARY_BG = new Color(0xEFF6FF);
    private static final Color TEXT = new Color(0x17233A);
    private static final Color BODY = new Color(0x3B4B65);
    private static final Color SUBTLE = new Color(0x6B7D98);
    private static final Color RED = new Color(0xDC2626);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    private final PhieuYeuCauNhapLinhKienController controller = new PhieuYeuCauNhapLinhKienController();
    private final JComboBox<String> cboTrangThai = new JComboBox<>(new String[]{"Tất cả", "Chờ duyệt", "Đã duyệt", "Từ chối", "Đã nhập kho"});
    private final JTextField txtTimKiem = new JTextField();
    private final JButton btnTim = button("Tìm kiếm", true);
    private final JButton btnLamMoi = button("Làm mới", false);
    private final JButton btnDuyet = button("Duyệt", true);
    private final JButton btnTuChoi = button("Từ chối", false);
    private final JButton btnNhapKho = button("Xác nhận nhập kho", false);
    private final JLabel lblError = label("", 12, RED, Font.PLAIN);
    private final JLabel lblStatus = label("", 12, SUBTLE, Font.PLAIN);
    private final JPanel pnlResults = new JPanel(new BorderLayout());
    private final JPanel pnlDetails = new JPanel(new BorderLayout());
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{
            "Mã yêu cầu", "Ngày tạo", "KTV lập", "Linh kiện", "SL yêu cầu", "Tồn hiện tại", "Trạng thái"
    }, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private List<PhieuYeuCauNhapLinhKien> results = new ArrayList<>();
    private PhieuYeuCauNhapLinhKien selected;
    private boolean searched;
    private boolean busy;
    private boolean resetting;

    public XuLyYeuCauLinhKienUI() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(24, 24, 22, 24));
        add(buildContent(), BorderLayout.CENTER);
        bindActions();
        showPrompt();
        updateActions();
        SwingUtilities.invokeLater(() -> txtTimKiem.requestFocusInWindow());
    }

    private JComponent buildContent() {
        JPanel root = new MainFrame.ScrollableContentPanel();
        root.setOpaque(false);

        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        heading.setOpaque(false);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(label("📋", 25, TEXT, Font.PLAIN));
        heading.add(label("Xử lý yêu cầu cập nhật linh kiện", 24, TEXT, Font.BOLD));
        root.add(heading);
        root.add(gap(18));
        root.add(buildSearchRow());
        root.add(gap(18));

        pnlResults.setOpaque(false);
        pnlResults.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlResults.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        root.add(pnlResults);
        root.add(gap(18));

        pnlDetails.setOpaque(false);
        pnlDetails.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlDetails.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        root.add(pnlDetails);
        root.add(gap(8));

        JPanel messages = new JPanel(new BorderLayout(8, 0));
        messages.setOpaque(false);
        messages.setAlignmentX(Component.LEFT_ALIGNMENT);
        messages.add(lblError, BorderLayout.WEST);
        messages.add(lblStatus, BorderLayout.EAST);
        root.add(messages);

        JScrollPane scroll = new JScrollPane(root);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    /** Bộ lọc và hai nút hành động cùng nằm trên một hàng như thiết kế. */
    private JComponent buildSearchRow() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 0, 12);

        addSearchCell(row, g, "Trạng thái yêu cầu", cboTrangThai, 0, 0.22);
        addSearchCell(row, g, "Tìm mã yêu cầu / linh kiện", txtTimKiem, 1, 0.38);

        JPanel actions = new JPanel(new BorderLayout(0, 6));
        actions.setOpaque(false);
        actions.add(label(" ", 12, BODY, Font.PLAIN), BorderLayout.NORTH);
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(btnTim);
        buttons.add(Box.createRigidArea(new Dimension(10, 0)));
        buttons.add(btnLamMoi);
        actions.add(buttons, BorderLayout.CENTER);
        g.gridx = 2;
        g.weightx = 0;
        g.insets = new Insets(0, 0, 0, 0);
        row.add(actions, g);
        return row;
    }

    private void addSearchCell(JPanel row, GridBagConstraints base, String title, JComponent field, int x, double weight) {
        JPanel cell = new JPanel(new BorderLayout(0, 6));
        cell.setOpaque(false);
        cell.setMinimumSize(new Dimension(170, 66));
        cell.add(label(title, 13, BODY, Font.PLAIN), BorderLayout.NORTH);
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setPreferredSize(new Dimension(x == 0 ? 220 : 330, 44));
        field.setBackground(WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(0xC9D5E5), 10), new EmptyBorder(0, 11, 0, 10)));
        if (field instanceof JTextField input) input.putClientProperty("JTextField.placeholderText", "Nhập mã hoặc tên…");
        GridBagConstraints g = (GridBagConstraints) base.clone();
        g.gridx = x;
        g.weightx = weight;
        row.add(cell, g);
        cell.add(field, BorderLayout.CENTER);
    }

    private void bindActions() {
        btnTim.addActionListener(e -> search());
        txtTimKiem.addActionListener(e -> search());
        btnLamMoi.addActionListener(e -> reset());
        btnDuyet.addActionListener(e -> approve());
        btnTuChoi.addActionListener(e -> reject());
        btnNhapKho.addActionListener(e -> receive());
        cboTrangThai.addActionListener(e -> {
            if (!resetting && searched) search();
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSelected();
        });
        txtTimKiem.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE && !txtTimKiem.getText().isBlank()) reset();
            }
        });
    }

    private void search() {
        String keyword = txtTimKiem.getText().trim();
        if (!UIInputRules.isSearch(keyword)) { showError("Từ khóa tìm kiếm tối đa 150 ký tự; chỉ nhập chữ, số và dấu câu."); txtTimKiem.requestFocusInWindow(); return; }
        TrangThaiYeuCau status = statusFromLabel(String.valueOf(cboTrangThai.getSelectedItem()));
        setBusy(true);
        clearError();
        new SwingWorker<List<PhieuYeuCauNhapLinhKien>, Void>() {
            @Override protected List<PhieuYeuCauNhapLinhKien> doInBackground() { return controller.timKiem(keyword, status); }
            @Override protected void done() {
                try {
                    results = get();
                    searched = true;
                    renderTable();
                    lblStatus.setText(results.size() + " yêu cầu phù hợp");
                    if (!results.isEmpty()) table.setRowSelectionInterval(0, 0);
                } catch (Exception ex) {
                    results = new ArrayList<>();
                    selected = null;
                    showPrompt();
                    pnlDetails.removeAll();
                    pnlDetails.revalidate();
                    pnlDetails.repaint();
                    showWorkerError("Không thể tìm yêu cầu", ex);
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    /** Được gọi từ KPI Dashboard; chủ động thực hiện truy vấn với trạng thái tương ứng. */
    public void locTuDashboard(String statusLabel) {
        resetting = true;
        cboTrangThai.setSelectedItem(statusLabel);
        resetting = false;
        txtTimKiem.setText("");
        search();
    }

    private void reset() {
        resetting = true;
        txtTimKiem.setText("");
        cboTrangThai.setSelectedIndex(0);
        resetting = false;
        searched = false;
        results = new ArrayList<>();
        selected = null;
        model.setRowCount(0);
        table.clearSelection();
        clearError();
        lblStatus.setText("");
        showPrompt();
        pnlDetails.removeAll();
        pnlDetails.revalidate();
        pnlDetails.repaint();
        updateActions();
        txtTimKiem.requestFocusInWindow();
    }

    private void renderTable() {
        pnlResults.removeAll();
        pnlDetails.removeAll();
        selected = null;
        model.setRowCount(0);
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout());
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setForeground(BODY);
        table.setRowHeight(44);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        table.setSelectionBackground(PRIMARY_BG);
        table.setSelectionForeground(PRIMARY);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setForeground(BODY);
        table.getTableHeader().setBackground(new Color(0xF3F6FA));
        table.getTableHeader().setPreferredSize(new Dimension(0, 42));
        DefaultTableCellRenderer striped = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selectedRow,
                                                                       boolean focus, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(t, value, selectedRow, focus, row, column);
                cell.setForeground(BODY);
                cell.setBackground(selectedRow ? PRIMARY_BG : (row % 2 == 0 ? new Color(0xEEF5FF) : WHITE));
                cell.setBorder(new EmptyBorder(0, 10, 0, 10));
                return cell;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) table.getColumnModel().getColumn(i).setCellRenderer(striped);
        table.getColumnModel().getColumn(4).setCellRenderer(rightAlignedRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(rightAlignedRenderer());
        table.getColumnModel().getColumn(6).setCellRenderer(statusRenderer());

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        tableScroll.getViewport().setBackground(WHITE);
        card.add(tableScroll, BorderLayout.CENTER);
        pnlResults.add(card, BorderLayout.CENTER);
        for (PhieuYeuCauNhapLinhKien item : results) {
            model.addRow(new Object[]{item.getMaYeuCau(), DATE_FORMAT.format(item.getNgayTao()),
                    item.getNhanVienLap().getHoTen(), item.getLinhKien().getMaLK() + " · " + item.getLinhKien().getTenLK(),
                    item.getSoLuongYeuCau(), item.getLinhKien().getSoLuongTon(), statusText(item.getTrangThai())});
        }
        if (results.isEmpty()) {
            JLabel empty = label("Không tìm thấy yêu cầu phù hợp với điều kiện tìm kiếm.", 13, SUBTLE, Font.PLAIN);
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(WHITE);
            panel.setPreferredSize(new Dimension(400, 100));
            panel.add(empty);
            card.removeAll();
            card.add(panel, BorderLayout.CENTER);
        }
        pnlResults.revalidate();
        pnlResults.repaint();
        pnlDetails.revalidate();
        pnlDetails.repaint();
    }

    private void showPrompt() {
        pnlResults.removeAll();
        JPanel prompt = new JPanel(new BorderLayout());
        prompt.setOpaque(false);
        prompt.add(label("Chưa có kết quả tra cứu.", 13, SUBTLE, Font.PLAIN), BorderLayout.WEST);
        pnlResults.add(prompt, BorderLayout.NORTH);
        pnlResults.revalidate();
        pnlResults.repaint();
    }

    private void showSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= results.size()) {
            selected = null;
            pnlDetails.removeAll();
            pnlDetails.revalidate();
            pnlDetails.repaint();
            updateActions();
            return;
        }
        selected = results.get(table.convertRowIndexToModel(row));
        renderDetails(selected);
        updateActions();
    }

    private void renderDetails(PhieuYeuCauNhapLinhKien item) {
        pnlDetails.removeAll();
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout(0, 14));
        card.add(label("Chi tiết yêu cầu — " + item.getMaYeuCau(), 15, TEXT, Font.BOLD), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        g.insets = new Insets(0, 0, 10, 10);
        addDetail(body, g, 0, 0, "Linh kiện", item.getLinhKien().getTenLK() + " · " + item.getLinhKien().getMaLK());
        addDetail(body, g, 1, 0, "Số lượng yêu cầu", item.getSoLuongYeuCau() + " " + item.getLinhKien().getDonViTinh());
        addDetail(body, g, 2, 0, "Tồn hiện tại", item.getLinhKien().getSoLuongTon() + " " + item.getLinhKien().getDonViTinh());
        addDetail(body, g, 0, 1, "Người lập", item.getNhanVienLap().getHoTen());
        addDetail(body, g, 1, 1, "Ngày tạo", DATE_FORMAT.format(item.getNgayTao()));
        addDetail(body, g, 2, 1, "Người xử lý", item.getNhanVienXuLy() == null ? "Chưa xử lý" : item.getNhanVienXuLy().getHoTen());
        addDetail(body, g, 0, 2, "Lý do / ghi chú", item.getGhiChu(), 2);
        addDetail(body, g, 2, 2, "Ghi chú xử lý", item.getGhiChuXuLy() == null || item.getGhiChuXuLy().isBlank() ? "Chưa có ghi chú xử lý" : item.getGhiChuXuLy());
        addDetail(body, g, 0, 3, "Điều kiện xử lý", actionGuidance(item.getTrangThai()), 3);
        card.add(body, BorderLayout.CENTER);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));
        actions.add(btnDuyet);
        actions.add(Box.createRigidArea(new Dimension(8, 0)));
        actions.add(btnTuChoi);
        actions.add(Box.createRigidArea(new Dimension(8, 0)));
        actions.add(btnNhapKho);
        card.add(actions, BorderLayout.SOUTH);
        pnlDetails.add(card, BorderLayout.CENTER);
        pnlDetails.revalidate();
        pnlDetails.repaint();
    }

    private void addDetail(JPanel panel, GridBagConstraints base, int x, int y, String title, String value) {
        addDetail(panel, base, x, y, title, value, 1);
    }

    private void addDetail(JPanel panel, GridBagConstraints base, int x, int y, String title, String value, int span) {
        JPanel cell = new JPanel(new BorderLayout(0, 4));
        cell.setBackground(new Color(0xF6F8FB));
        cell.setBorder(new EmptyBorder(10, 12, 10, 12));
        cell.add(label(title, 11, SUBTLE, Font.PLAIN), BorderLayout.NORTH);
        JLabel content = label("<html>" + escapeHtml(value) + "</html>", 13, BODY, Font.PLAIN);
        cell.add(content, BorderLayout.CENTER);
        GridBagConstraints g = (GridBagConstraints) base.clone();
        g.gridx = x;
        g.gridy = y;
        g.gridwidth = span;
        g.weightx = span;
        panel.add(cell, g);
    }

    private void approve() {
        if (selected == null) return;
        runAction("Đã duyệt yêu cầu " + selected.getMaYeuCau(), () -> controller.duyet(selected.getMaYeuCau(), null));
    }

    private void reject() {
        if (selected == null) return;
        JTextArea note = new JTextArea(4, 36);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setFont(new Font("SansSerif", Font.PLAIN, 13));
        int result = JOptionPane.showConfirmDialog(this, new Object[]{"Nhập lý do từ chối yêu cầu:", new JScrollPane(note)},
                "Từ chối yêu cầu", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String reason = note.getText().trim();
            runAction("Đã từ chối yêu cầu " + selected.getMaYeuCau(), () -> controller.tuChoi(selected.getMaYeuCau(), reason));
        }
    }

    private void receive() {
        if (selected == null) return;
        XacNhanNhapKhoDialog dialog = new XacNhanNhapKhoDialog(
                SwingUtilities.getWindowAncestor(this), selected, controller);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) search();
    }

    private void runAction(String successMessage, Runnable action) {
        String requestCode = selected.getMaYeuCau();
        setBusy(true);
        clearError();
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() { action.run(); return null; }
            @Override protected void done() {
                try {
                    get();
                    lblStatus.setText(successMessage);
                    search();
                } catch (Exception ex) {
                    Throwable cause = ex instanceof ExecutionException && ex.getCause() != null ? ex.getCause() : ex;
                    showError(cause.getMessage() == null ? "Không thể cập nhật yêu cầu." : cause.getMessage());
                    lblStatus.setText("Yêu cầu " + requestCode + " chưa được cập nhật");
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void setBusy(boolean value) {
        busy = value;
        cboTrangThai.setEnabled(!value);
        txtTimKiem.setEnabled(!value);
        btnTim.setEnabled(!value);
        btnLamMoi.setEnabled(!value);
        updateActions();
        if (value) lblStatus.setText("Đang xử lý…");
    }

    private void updateActions() {
        TrangThaiYeuCau status = selected == null ? null : selected.getTrangThai();
        btnDuyet.setEnabled(!busy && status == TrangThaiYeuCau.CHO_DUYET);
        btnTuChoi.setEnabled(!busy && status == TrangThaiYeuCau.CHO_DUYET);
        btnNhapKho.setEnabled(!busy && status == TrangThaiYeuCau.DA_DUYET);
    }

    private void showWorkerError(String title, Exception ex) {
        Throwable cause = ex instanceof ExecutionException && ex.getCause() != null ? ex.getCause() : ex;
        showError(cause.getMessage() == null ? "Hãy thử lại hoặc kiểm tra kết nối cơ sở dữ liệu." : cause.getMessage());
        lblStatus.setText(title);
    }

    private void showError(String message) { lblError.setText(message == null ? "Có lỗi khi xử lý yêu cầu." : message); }
    private void clearError() { lblError.setText(""); }

    private static TrangThaiYeuCau statusFromLabel(String label) {
        return switch (label) {
            case "Chờ duyệt" -> TrangThaiYeuCau.CHO_DUYET;
            case "Đã duyệt" -> TrangThaiYeuCau.DA_DUYET;
            case "Từ chối" -> TrangThaiYeuCau.TU_CHOI;
            case "Đã nhập kho" -> TrangThaiYeuCau.DA_NHAP_KHO;
            default -> null;
        };
    }

    private static String statusText(TrangThaiYeuCau status) {
        return switch (status) {
            case CHO_DUYET -> "Chờ duyệt";
            case DA_DUYET -> "Đã duyệt";
            case TU_CHOI -> "Từ chối";
            case DA_NHAP_KHO -> "Đã nhập kho";
        };
    }

    private static String actionGuidance(TrangThaiYeuCau status) {
        return switch (status) {
            case CHO_DUYET -> "Duyệt yêu cầu hợp lệ; từ chối khi số lượng không phù hợp và ghi rõ lý do. Sau khi duyệt, xác nhận nhập kho khi đã nhận hàng.";
            case DA_DUYET -> "Chỉ xác nhận nhập kho sau khi đã nhận hàng. Cơ sở dữ liệu sẽ tự cộng số lượng vào tồn.";
            case TU_CHOI -> "Yêu cầu đã từ chối và không thể xử lý tiếp.";
            case DA_NHAP_KHO -> "Đã nhập kho; số lượng tồn được cập nhật bởi cơ sở dữ liệu.";
        };
    }

    private static DefaultTableCellRenderer rightAlignedRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selectedRow,
                                                                       boolean focus, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(t, value, selectedRow, focus, row, column);
                cell.setHorizontalAlignment(SwingConstants.RIGHT);
                cell.setForeground(BODY);
                cell.setBackground(selectedRow ? PRIMARY_BG : (row % 2 == 0 ? new Color(0xEEF5FF) : WHITE));
                cell.setBorder(new EmptyBorder(0, 10, 0, 10));
                return cell;
            }
        };
    }

    private static DefaultTableCellRenderer statusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selectedRow,
                                                                       boolean focus, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(t, value, selectedRow, focus, row, column);
                cell.setHorizontalAlignment(SwingConstants.CENTER);
                cell.setFont(new Font("SansSerif", Font.BOLD, 11));
                String status = String.valueOf(value);
                cell.setForeground(switch (status) {
                    case "Đã duyệt", "Đã nhập kho" -> new Color(0x2563EB);
                    case "Từ chối" -> RED;
                    default -> new Color(0x1D4ED8);
                });
                cell.setBackground(switch (status) {
                    case "Đã duyệt" -> new Color(0xECFDF5);
                    case "Đã nhập kho" -> new Color(0xEFF6FF);
                    case "Từ chối" -> new Color(0xFEF2F2);
                    default -> new Color(0xDBEAFE);
                });
                cell.setBorder(new EmptyBorder(0, 4, 0, 4));
                return cell;
            }
        };
    }

    private static JPanel cardPanel() {
        return new MainFrame.RoundedPanel(WHITE, 12, BORDER, new Insets(16, 16, 16, 16));
    }

    private static JButton button(String text, boolean primary) {
        JButton button = new MainFrame.RoundedButton(text, 10, primary ? PRIMARY : WHITE,
                primary ? new Color(0x1D4ED8) : new Color(0xF1F5F9), primary ? PRIMARY : BORDER);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setForeground(primary ? WHITE : (text.equals("Từ chối") ? RED : BODY));
        int width = switch (text) {
            case "Tìm kiếm" -> 94;
            case "Làm mới" -> 92;
            case "Duyệt" -> 72;
            case "Từ chối" -> 84;
            default -> 160;
        };
        int height = text.equals("Tìm kiếm") || text.equals("Làm mới") ? 44 : 42;
        button.setPreferredSize(new Dimension(width, height));
        if (height == 44) {
            Dimension size = new Dimension(width, height);
            button.setMinimumSize(size);
            button.setMaximumSize(size);
        }
        return button;
    }

    private static JLabel label(String text, int size, Color color, int style) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", style, size));
        label.setForeground(color);
        return label;
    }

    private static Component gap(int height) {
        JComponent gap = (JComponent) Box.createRigidArea(new Dimension(0, height));
        gap.setAlignmentX(Component.LEFT_ALIGNMENT);
        return gap;
    }

    private static String escapeHtml(String value) {
        if (value == null) return "—";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>");
    }

    private static class RoundedBorder extends javax.swing.border.AbstractBorder {
        private final Color color;
        private final int arc;
        RoundedBorder(Color color, int arc) { this.color = color; this.arc = arc; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, arc, arc);
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
    }
}
