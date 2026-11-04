package ui;

import controller.KhuyenMaiController;
import dao.KhuyenMaiDAO;
import entity.DichVu;
import entity.KhuyenMai;
import entity.LinhKien;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** Tra cứu và quản lý chương trình khuyến mãi của Chủ cửa hàng. */
public class QuanLyKhuyenMaiUI extends JPanel {
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

    private final KhuyenMaiController controller = new KhuyenMaiController();
    private final JTextField txtQuery = new JTextField();
    private final JDateChooser spnTuNgay = dateSpinner(Date.from(LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
    private final JDateChooser spnDenNgay = dateSpinner(new Date());
    private final JButton btnSearch = button("Tìm kiếm", true);
    private final JButton btnReset = button("Làm mới", false);
    private final JButton btnAdd = button("Thêm chương trình", true);
    private final JButton btnEdit = button("Sửa", false);
    private final JLabel lblError = label("", 12, RED, Font.PLAIN);
    private final JLabel lblStatus = label("", 12, SUBTLE, Font.PLAIN);
    private final JPanel pnlResults = new JPanel(new BorderLayout());
    private final JPanel pnlDetails = new JPanel(new BorderLayout());
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{
            "Mã", "Tên chương trình", "Giảm (%)", "Bắt đầu", "Kết thúc", "Phạm vi", "Hiệu lực"
    }, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private List<KhuyenMaiDAO.KhuyenMaiTongQuan> results = new ArrayList<>();
    private KhuyenMai selectedDetails;
    private boolean searched;
    private boolean busy;

    public QuanLyKhuyenMaiUI() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(24, 24, 22, 24));
        add(buildContent(), BorderLayout.CENTER);
        bindActions();
        showPrompt();
        updateActions();
        SwingUtilities.invokeLater(() -> txtQuery.requestFocusInWindow());
    }

    private JComponent buildContent() {
        JPanel root = new MainFrame.ScrollableContentPanel();
        root.setOpaque(false);
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        heading.setOpaque(false);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(label("🎁", 25, TEXT, Font.PLAIN));
        heading.add(label("Quản lý chương trình khuyến mãi", 24, TEXT, Font.BOLD));
        root.add(heading);
        root.add(gap(18));
        root.add(buildSearchRow());
        root.add(gap(12));

        JPanel commands = new JPanel();
        commands.setOpaque(false);
        commands.setAlignmentX(Component.LEFT_ALIGNMENT);
        commands.setLayout(new BoxLayout(commands, BoxLayout.X_AXIS));
        fixCommandButtonHeight(btnAdd);
        fixCommandButtonHeight(btnEdit);
        commands.add(btnAdd);
        commands.add(Box.createRigidArea(new Dimension(8, 0)));
        commands.add(btnEdit);
        root.add(commands);
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

    private JComponent buildSearchRow() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 0, 12);
        addSearchField(row, g, "Tìm mã / tên", txtQuery, 0, 0.34, 255);
        addSearchField(row, g, "Từ ngày", spnTuNgay, 1, 0.22, 165);
        addSearchField(row, g, "Đến ngày", spnDenNgay, 2, 0.22, 165);
        JPanel buttons = new JPanel(new BorderLayout(0, 6));
        buttons.setOpaque(false);
        buttons.add(label(" ", 12, BODY, Font.PLAIN), BorderLayout.NORTH);
        JPanel buttonRow = new JPanel();
        buttonRow.setOpaque(false);
        buttonRow.setLayout(new BoxLayout(buttonRow, BoxLayout.X_AXIS));
        buttonRow.add(btnSearch);
        buttonRow.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonRow.add(btnReset);
        buttons.add(buttonRow, BorderLayout.CENTER);
        g.gridx = 3;
        g.weightx = 0;
        g.insets = new Insets(0, 0, 0, 0);
        row.add(buttons, g);
        return row;
    }

    private void addSearchField(JPanel row, GridBagConstraints base, String title, JComponent field,
                                int x, double weight, int width) {
        JPanel cell = new JPanel(new BorderLayout(0, 6));
        cell.setOpaque(false);
        cell.setMinimumSize(new Dimension(140, 66));
        cell.add(label(title, 13, BODY, Font.PLAIN), BorderLayout.NORTH);
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setPreferredSize(new Dimension(width, 44));
        field.setBackground(WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(new Color(0xC9D5E5), 10), new EmptyBorder(0, 11, 0, 10)));
        if (field instanceof JTextField input) input.putClientProperty("JTextField.placeholderText", "Nhập mã hoặc tên…");
        if (field instanceof JSpinner spinner && spinner.getEditor() instanceof JSpinner.DateEditor editor) {
            editor.getTextField().setBorder(new EmptyBorder(0, 0, 0, 0));
        }
        GridBagConstraints g = (GridBagConstraints) base.clone();
        g.gridx = x;
        g.weightx = weight;
        row.add(cell, g);
        cell.add(field, BorderLayout.CENTER);
    }

    private void bindActions() {
        btnSearch.addActionListener(e -> search());
        txtQuery.addActionListener(e -> search());
        btnReset.addActionListener(e -> reset());
        btnAdd.addActionListener(e -> openForm(null));
        btnEdit.addActionListener(e -> openEdit());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelectedDetails();
        });
        txtQuery.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { clearError(); }
            @Override public void removeUpdate(DocumentEvent e) { clearError(); }
            @Override public void changedUpdate(DocumentEvent e) { clearError(); }
        });
        txtQuery.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { if (e.getKeyCode() == KeyEvent.VK_ESCAPE && !txtQuery.getText().isBlank()) reset(); }
        });
    }

    private void search() {
        String keyword = txtQuery.getText().trim();
        if (!UIInputRules.isSearch(keyword)) { showError("Từ khóa tìm kiếm tối đa 150 ký tự; chỉ nhập chữ, số và dấu câu."); txtQuery.requestFocusInWindow(); return; }
        if (!UIInputRules.isSearch(keyword)) { showError("Từ khóa tìm kiếm tối đa 150 ký tự; chỉ nhập chữ, số và dấu câu."); txtQuery.requestFocusInWindow(); return; }
        LocalDate fromDate;
        LocalDate throughDate;
        try {
            fromDate = UIInputRules.readDate(spnTuNgay);
            throughDate = UIInputRules.readDate(spnDenNgay);
        } catch (DateTimeParseException ex) {
            showError("Ngày tìm kiếm phải đúng định dạng dd/MM/yyyy và là ngày hợp lệ.");
            return;
        }
        Date from = Date.from(fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date through = Date.from(throughDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        if (through.before(from)) {
            showError("Ngày kết thúc tìm kiếm phải sau hoặc bằng ngày bắt đầu.");
            return;
        }
        setBusy(true);
        clearError();
        new SwingWorker<List<KhuyenMaiDAO.KhuyenMaiTongQuan>, Void>() {
            @Override protected List<KhuyenMaiDAO.KhuyenMaiTongQuan> doInBackground() { return controller.timKiem(keyword, from, through); }
            @Override protected void done() {
                try {
                    results = get();
                    searched = true;
                    selectedDetails = null;
                    renderTable();
                    lblStatus.setText(results.size() + " chương trình phù hợp");
                    if (!results.isEmpty()) table.setRowSelectionInterval(0, 0);
                } catch (Exception ex) {
                    results = new ArrayList<>();
                    selectedDetails = null;
                    showPrompt();
                    pnlDetails.removeAll();
                    pnlDetails.revalidate();
                    pnlDetails.repaint();
                    showWorkerError("Không thể tìm chương trình", ex);
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void reset() {
        txtQuery.setText("");
        spnTuNgay.setDate(Date.from(LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        spnDenNgay.setDate(normalizedDate(new Date()));
        results = new ArrayList<>();
        selectedDetails = null;
        searched = false;
        model.setRowCount(0);
        table.clearSelection();
        clearError();
        lblStatus.setText("");
        showPrompt();
        pnlDetails.removeAll();
        pnlDetails.revalidate();
        pnlDetails.repaint();
        updateActions();
        txtQuery.requestFocusInWindow();
    }

    private void renderTable() {
        pnlResults.removeAll();
        pnlDetails.removeAll();
        model.setRowCount(0);
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout());
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setForeground(BODY);
        table.setRowHeight(38);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        table.setSelectionBackground(PRIMARY_BG);
        table.setSelectionForeground(PRIMARY);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setForeground(BODY);
        table.getTableHeader().setBackground(new Color(0xF3F6FA));
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        for (int i = 0; i < table.getColumnCount(); i++) table.getColumnModel().getColumn(i).setCellRenderer(stripedRenderer());
        table.getColumnModel().getColumn(2).setCellRenderer(rightAlignedRenderer());
        table.getColumnModel().getColumn(6).setCellRenderer(statusRenderer());
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        tableScroll.getViewport().setBackground(WHITE);
        card.add(tableScroll, BorderLayout.CENTER);
        pnlResults.add(card, BorderLayout.CENTER);
        Date today = normalizedDate(new Date());
        DecimalFormat percent = new DecimalFormat("0.##");
        for (KhuyenMaiDAO.KhuyenMaiTongQuan summary : results) {
            KhuyenMai item = summary.khuyenMai();
            String validity = today.before(normalizedDate(item.getNgayBatDau())) ? "Chưa bắt đầu"
                    : today.after(normalizedDate(item.getNgayKetThuc())) ? "Đã kết thúc" : "Đang hiệu lực";
            model.addRow(new Object[]{item.getMaKM(), item.getTenChuongTrinh(), percent.format(item.getMucGiamPhanTram()),
                    DATE_FORMAT.format(item.getNgayBatDau()), DATE_FORMAT.format(item.getNgayKetThuc()), summary.phamVi(), validity});
        }
        if (results.isEmpty()) {
            JPanel empty = new JPanel(new GridBagLayout());
            empty.setBackground(WHITE);
            empty.setPreferredSize(new Dimension(400, 110));
            empty.add(label("Không tìm thấy chương trình phù hợp.", 13, SUBTLE, Font.PLAIN));
            card.removeAll();
            card.add(empty, BorderLayout.CENTER);
        }
        pnlResults.revalidate();
        pnlResults.repaint();
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

    private void loadSelectedDetails() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= results.size()) {
            selectedDetails = null;
            pnlDetails.removeAll();
            pnlDetails.revalidate();
            pnlDetails.repaint();
            updateActions();
            return;
        }
        String maKM = results.get(table.convertRowIndexToModel(row)).khuyenMai().getMaKM();
        setBusy(true);
        new SwingWorker<KhuyenMai, Void>() {
            @Override protected KhuyenMai doInBackground() { return controller.timTheoMa(maKM); }
            @Override protected void done() {
                try {
                    selectedDetails = get();
                    renderDetails(selectedDetails);
                } catch (Exception ex) {
                    showWorkerError("Không thể đọc đối tượng áp dụng", ex);
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void renderDetails(KhuyenMai item) {
        pnlDetails.removeAll();
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout(0, 12));
        card.add(label("Đối tượng áp dụng — " + item.getTenChuongTrinh(), 15, TEXT, Font.BOLD), BorderLayout.NORTH);
        DefaultTableModel detailModel = new DefaultTableModel(new Object[]{"Loại", "Mã đối tượng", "Tên đối tượng"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        for (DichVu service : item.getDichVuApDung()) detailModel.addRow(new Object[]{"Dịch vụ", service.getMaDV(), service.getTenDV()});
        for (LinhKien part : item.getLinhKienApDung()) detailModel.addRow(new Object[]{"Linh kiện", part.getMaLK(), part.getTenLK()});
        JTable detailTable = new JTable(detailModel);
        detailTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        detailTable.setRowHeight(32);
        detailTable.setShowVerticalLines(false);
        detailTable.setGridColor(BORDER);
        detailTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        detailTable.getTableHeader().setForeground(BODY);
        detailTable.getTableHeader().setBackground(new Color(0xF3F6FA));
        detailTable.getTableHeader().setPreferredSize(new Dimension(0, 34));
        for (int i = 0; i < detailTable.getColumnCount(); i++) detailTable.getColumnModel().getColumn(i).setCellRenderer(stripedRenderer());
        JScrollPane detailScroll = new JScrollPane(detailTable);
        detailScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        detailScroll.setPreferredSize(new Dimension(600, Math.min(180, Math.max(66, (detailModel.getRowCount() + 1) * 32 + 2))));
        card.add(detailScroll, BorderLayout.CENTER);
        String note = item.getGhiChu() == null || item.getGhiChu().isBlank() ? "Không có ghi chú." : item.getGhiChu();
        String scopeNote = item.apDungTatCa()
                ? "Ghi chú: Áp dụng cho tất cả dịch vụ và linh kiện; mỗi hóa đơn tối đa 1 chương trình."
                : "Ghi chú: Chỉ áp dụng cho các hạng mục trong danh sách; mỗi hóa đơn tối đa 1 chương trình.";
        JPanel footer = new JPanel(new BorderLayout(0, 5));
        footer.setOpaque(false);
        footer.add(label("Ghi chú: " + note, 12, BODY, Font.PLAIN), BorderLayout.NORTH);
        footer.add(label(scopeNote, 12, SUBTLE, Font.PLAIN), BorderLayout.SOUTH);
        card.add(footer, BorderLayout.SOUTH);
        pnlDetails.add(card, BorderLayout.CENTER);
        pnlDetails.revalidate();
        pnlDetails.repaint();
        updateActions();
    }

    private void openEdit() {
        if (selectedDetails == null) return;
        boolean used = results.stream().filter(r -> r.khuyenMai().getMaKM().equals(selectedDetails.getMaKM()))
                .findFirst().map(KhuyenMaiDAO.KhuyenMaiTongQuan::daApDungHoaDon).orElse(false);
        openForm(selectedDetails, used);
    }

    private void openForm(KhuyenMai existing) { openForm(existing, false); }

    private void openForm(KhuyenMai existing, boolean used) {
        KhuyenMaiFormDialog dialog = new KhuyenMaiFormDialog(SwingUtilities.getWindowAncestor(this), controller, existing, used);
        dialog.setVisible(true);
        if (dialog.saved != null) {
            txtQuery.setText(dialog.saved.getMaKM());
            spnTuNgay.setDate(normalizedDate(dialog.saved.getNgayBatDau()));
            spnDenNgay.setDate(normalizedDate(dialog.saved.getNgayKetThuc()));
            search();
        }
    }

    private void updateActions() {
        btnEdit.setEnabled(!busy && selectedDetails != null && searched);
        btnAdd.setEnabled(!busy);
        btnSearch.setEnabled(!busy);
        btnReset.setEnabled(!busy);
    }

    private void setBusy(boolean value) {
        busy = value;
        txtQuery.setEnabled(!value);
        spnTuNgay.setEnabled(!value);
        spnDenNgay.setEnabled(!value);
        table.setEnabled(!value);
        updateActions();
        if (value) lblStatus.setText("Đang xử lý…");
    }

    private void showWorkerError(String title, Exception ex) {
        Throwable cause = ex instanceof ExecutionException && ex.getCause() != null ? ex.getCause() : ex;
        showError(cause.getMessage() == null ? "Hãy thử lại hoặc kiểm tra kết nối cơ sở dữ liệu." : cause.getMessage());
        lblStatus.setText(title);
    }

    private void showError(String message) { lblError.setText(message == null ? "Có lỗi khi xử lý khuyến mãi." : message); }
    private void clearError() { lblError.setText(""); }

    private static DefaultTableCellRenderer stripedRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                                       boolean focus, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                cell.setForeground(BODY);
                cell.setBackground(selected ? PRIMARY_BG : (row % 2 == 0 ? new Color(0xEEF5FF) : WHITE));
                cell.setBorder(new EmptyBorder(0, 10, 0, 10));
                return cell;
            }
        };
    }

    private static DefaultTableCellRenderer rightAlignedRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                                       boolean focus, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                cell.setHorizontalAlignment(SwingConstants.RIGHT);
                cell.setForeground(BODY);
                cell.setBackground(selected ? PRIMARY_BG : (row % 2 == 0 ? new Color(0xEEF5FF) : WHITE));
                cell.setBorder(new EmptyBorder(0, 10, 0, 10));
                return cell;
            }
        };
    }

    private static DefaultTableCellRenderer statusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                                       boolean focus, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                cell.setHorizontalAlignment(SwingConstants.CENTER);
                cell.setFont(new Font("SansSerif", Font.BOLD, 11));
                String status = String.valueOf(value);
                boolean active = "Đang hiệu lực".equals(status);
                cell.setForeground(active ? new Color(0x047857) : SUBTLE);
                cell.setBackground(active ? new Color(0xECFDF5) : new Color(0xF1F5F9));
                cell.setBorder(new EmptyBorder(0, 4, 0, 4));
                return cell;
            }
        };
    }

    private static JDateChooser createDateChooser(Date value, int width) {
        JDateChooser chooser = new JDateChooser();
        chooser.setDateFormatString("dd/MM/yyyy");
        chooser.setDate(normalizedDate(value));
        chooser.setPreferredSize(new Dimension(width, 44));
        chooser.setBackground(WHITE);
        if (chooser.getDateEditor() instanceof JTextFieldDateEditor editor) {
            editor.setEditable(false);
            editor.setBackground(WHITE);
            editor.setFont(new Font("SansSerif", Font.PLAIN, 13));
            editor.setBorder(new EmptyBorder(0, 10, 0, 2));
        }
        chooser.getCalendarButton().setFocusable(false);
        chooser.getCalendarButton().setPreferredSize(new Dimension(32, 34));
        chooser.setBorder(new RoundedBorder(new Color(0xC9D5E5), 10));
        return chooser;
    }

    private static JDateChooser dateSpinner(Date value) {
        return createDateChooser(value, 165);
    }

    private static Date normalizedDate(Date date) {
        LocalDate local = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return Date.from(local.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static JPanel cardPanel() { return new MainFrame.RoundedPanel(WHITE, 12, BORDER, new Insets(14, 16, 14, 16)); }
    private static JButton button(String text, boolean primary) {
        JButton button = new MainFrame.RoundedButton(text, 10, primary ? PRIMARY : WHITE,
                primary ? new Color(0x1D4ED8) : new Color(0xF1F5F9), primary ? PRIMARY : BORDER);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setForeground(primary ? WHITE : BODY);
        int width = switch (text) { case "Tìm kiếm" -> 94; case "Làm mới" -> 92; case "Sửa" -> 72; default -> 162; };
        Dimension searchButtonSize = new Dimension(width, 44);
        button.setPreferredSize(searchButtonSize);
        if (text.equals("Tìm kiếm") || text.equals("Làm mới")) {
            button.setMinimumSize(searchButtonSize);
            button.setMaximumSize(searchButtonSize);
        }
        return button;
    }
    private static void fixCommandButtonHeight(JButton button) {
        Dimension size = button.getPreferredSize();
        size.height = 44;
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
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

    private class KhuyenMaiFormDialog extends JDialog {
        private final KhuyenMaiController formController;
        private final KhuyenMai existing;
        private final boolean readOnlyRules;
        private final JTextField txtName = new JTextField();
        private final JSpinner spnPercent = new JSpinner(new SpinnerNumberModel(10.0, 0.01, 100.0, 1.0));
        private final JDateChooser spnStart = dateSpinner(new Date());
        private final JDateChooser spnEnd = dateSpinner(new Date());
        private final JTextArea txtNote = new JTextArea(3, 30);
        private final JLabel lblError = label("", 12, RED, Font.PLAIN);
        private final JLabel lblRule = label("", 12, SUBTLE, Font.PLAIN);
        private final JButton btnSave = button("Lưu", true);
        private final JButton btnCancel = button("Hủy", false);
        private final Map<String, DichVu> selectedServices = new LinkedHashMap<>();
        private final Map<String, LinhKien> selectedParts = new LinkedHashMap<>();
        private final CatalogPanel servicePanel;
        private final CatalogPanel partPanel;
        private KhuyenMai saved;
        private boolean saving;

        KhuyenMaiFormDialog(Window owner, KhuyenMaiController controller, KhuyenMai existing, boolean used) {
            super(owner, existing == null ? "CarZone — Thêm chương trình khuyến mãi" : "CarZone — Sửa chương trình khuyến mãi", ModalityType.APPLICATION_MODAL);
            this.formController = controller;
            this.existing = existing;
            this.readOnlyRules = used;
            servicePanel = new CatalogPanel(true);
            partPanel = new CatalogPanel(false);
            UIInputRules.restrictDecimalSpinner(spnPercent, 2);
            if (existing != null) {
                for (DichVu dv : existing.getDichVuApDung()) selectedServices.put(dv.getMaDV(), dv);
                for (LinhKien lk : existing.getLinhKienApDung()) selectedParts.put(lk.getMaLK(), lk);
            }
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setContentPane(buildForm());
            pack();
            setSize(820, 700);
            setMinimumSize(new Dimension(760, 630));
            setLocationRelativeTo(owner);
            bindActions();
            SwingUtilities.invokeLater(() -> txtName.requestFocusInWindow());
        }

        private JComponent buildForm() {
            JPanel base = new JPanel(new BorderLayout());
            base.setBackground(BG);
            JPanel content = new JPanel();
            content.setOpaque(false);
            content.setBorder(new EmptyBorder(20, 24, 14, 24));
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            JLabel title = label(existing == null ? "🎁  Thêm chương trình khuyến mãi" : "🎁  Sửa chương trình khuyến mãi", 20, PRIMARY, Font.BOLD);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(title);
            content.add(gap(14));
            addFormField(content, "Tên chương trình *", txtName);
            content.add(gap(12));
            JPanel numbers = new JPanel(new GridLayout(1, 3, 12, 0));
            numbers.setOpaque(false);
            addFieldCell(numbers, "Mức giảm * (%)", spnPercent);
            addFieldCell(numbers, "Ngày bắt đầu *", spnStart);
            addFieldCell(numbers, "Ngày kết thúc *", spnEnd);
            numbers.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(numbers);
            content.add(gap(14));

            JTabbedPane tabs = new JTabbedPane();
            tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));
            tabs.addTab("Dịch vụ (" + selectedServices.size() + " đã chọn)", servicePanel);
            tabs.addTab("Linh kiện (" + selectedParts.size() + " đã chọn)", partPanel);
            tabs.setAlignmentX(Component.LEFT_ALIGNMENT);
            tabs.setPreferredSize(new Dimension(760, 270));
            content.add(tabs);
            content.add(gap(12));
            addFormField(content, "Ghi chú (tối đa 500 ký tự)", new JScrollPane(txtNote));
            content.add(lblRule);
            content.add(lblError);
            if (readOnlyRules) {
                lblRule.setText("Chương trình đã gắn với hóa đơn: chỉ được đổi tên và ghi chú.");
                spnPercent.setEnabled(false);
                spnStart.setEnabled(false);
                spnEnd.setEnabled(false);
                servicePanel.setEnabledAll(false);
                partPanel.setEnabledAll(false);
            } else if (existing != null) {
                lblRule.setText("Nếu không chọn dịch vụ và linh kiện, chương trình áp dụng cho tất cả.");
            } else {
                lblRule.setText("Không chọn dịch vụ và linh kiện nghĩa là áp dụng cho tất cả.");
            }
            if (existing != null) populate();
            JScrollPane contentScroll = new JScrollPane(content);
            contentScroll.setBorder(null);
            contentScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            base.add(contentScroll, BorderLayout.CENTER);
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
            footer.setBackground(WHITE);
            footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));
            footer.add(btnSave);
            footer.add(btnCancel);
            base.add(footer, BorderLayout.SOUTH);
            return base;
        }

        private void addFormField(JPanel parent, String title, JComponent field) {
            JPanel wrapper = new JPanel(new BorderLayout(0, 6));
            wrapper.setOpaque(false);
            wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
            wrapper.add(label(title, 12, BODY, Font.PLAIN), BorderLayout.NORTH);
            field.setFont(new Font("SansSerif", Font.PLAIN, 13));
            field.setPreferredSize(new Dimension(700, field instanceof JScrollPane ? 74 : 42));
            if (!(field instanceof JScrollPane)) field.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(new Color(0xC9D5E5), 10), new EmptyBorder(0, 10, 0, 10)));
            wrapper.add(field, BorderLayout.CENTER);
            parent.add(wrapper);
        }

        private void addFieldCell(JPanel parent, String title, JComponent field) {
            JPanel cell = new JPanel(new BorderLayout(0, 6));
            cell.setOpaque(false);
            cell.add(label(title, 12, BODY, Font.PLAIN), BorderLayout.NORTH);
            field.setFont(new Font("SansSerif", Font.PLAIN, 13));
            field.setPreferredSize(new Dimension(210, 42));
            field.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(new Color(0xC9D5E5), 10), new EmptyBorder(0, 10, 0, 10)));
            if (field instanceof JDateChooser chooser && chooser.getDateEditor() instanceof JTextFieldDateEditor editor) editor.setBorder(new EmptyBorder(0, 10, 0, 2));
            cell.add(field, BorderLayout.CENTER);
            parent.add(cell);
        }

        private void populate() {
            txtName.setText(existing.getTenChuongTrinh());
            spnPercent.setValue(existing.getMucGiamPhanTram());
            spnStart.setDate(normalizedDate(existing.getNgayBatDau()));
            spnEnd.setDate(normalizedDate(existing.getNgayKetThuc()));
            txtNote.setText(existing.getGhiChu() == null ? "" : existing.getGhiChu());
            servicePanel.renderSelected();
            partPanel.renderSelected();
        }

        private void bindActions() {
            btnSave.addActionListener(e -> save());
            btnCancel.addActionListener(e -> dispose());
            txtName.addActionListener(e -> spnPercent.requestFocusInWindow());
            txtNote.setLineWrap(true);
            txtNote.setWrapStyleWord(true);
            txtNote.setFont(new Font("SansSerif", Font.PLAIN, 13));
            txtNote.setBorder(new EmptyBorder(8, 10, 8, 10));
            txtName.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { lblError.setText(""); }
                @Override public void removeUpdate(DocumentEvent e) { lblError.setText(""); }
                @Override public void changedUpdate(DocumentEvent e) { lblError.setText(""); }
            });
            servicePanel.bind();
            partPanel.bind();
        }

        private void save() {
            if (saving) return;
            try {
                spnPercent.commitEdit();
            } catch (java.text.ParseException e) {
                showFormError("Mức giảm hoặc ngày không hợp lệ.");
                return;
            }
            String name = txtName.getText().trim();
            if (name.isBlank()) { showFormError("Tên chương trình không được để trống."); txtName.requestFocusInWindow(); return; }
            if (!UIInputRules.isPromotionName(name)) { showFormError("Tên chương trình chỉ gồm chữ, số, khoảng trắng và dấu câu thông dụng."); txtName.requestFocusInWindow(); return; }
            if (!UIInputRules.isMultilineText(txtNote.getText())) { showFormError("Ghi chú chứa ký tự không hợp lệ."); txtNote.requestFocusInWindow(); return; }
            double percent = ((Number) spnPercent.getValue()).doubleValue();
            LocalDate startDate, endDate;
            try { startDate = UIInputRules.readDate(spnStart); endDate = UIInputRules.readDate(spnEnd); }
            catch (DateTimeParseException ex) { showFormError("Ngày bắt đầu và kết thúc phải đúng định dạng dd/MM/yyyy."); return; }
            Date start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date end = Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            if (percent <= 0 || percent > 100) { showFormError("Mức giảm phải trong khoảng (0, 100]%."); return; }
            if (end.before(start)) { showFormError("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu."); return; }
            String note = txtNote.getText().trim();
            if (note.length() > 500) { showFormError("Ghi chú tối đa 500 ký tự."); return; }
            setSaving(true);
            new SwingWorker<KhuyenMai, Void>() {
                @Override protected KhuyenMai doInBackground() {
                    if (existing == null) return formController.them(name, percent, start, end, note,
                            new java.util.LinkedHashSet<>(selectedServices.values()), new java.util.LinkedHashSet<>(selectedParts.values()));
                    return formController.capNhat(existing.getMaKM(), name, percent, start, end, note,
                            new java.util.LinkedHashSet<>(selectedServices.values()), new java.util.LinkedHashSet<>(selectedParts.values()));
                }
                @Override protected void done() {
                    try { saved = get(); dispose(); }
                    catch (Exception ex) {
                        Throwable cause = ex instanceof ExecutionException && ex.getCause() != null ? ex.getCause() : ex;
                        setSaving(false);
                        showFormError(cause.getMessage());
                    }
                }
            }.execute();
        }

        private void setSaving(boolean value) {
            saving = value;
            btnSave.setEnabled(!value);
            btnCancel.setEnabled(!value);
            txtName.setEnabled(!value);
            spnPercent.setEnabled(!value && !readOnlyRules);
            spnStart.setEnabled(!value && !readOnlyRules);
            spnEnd.setEnabled(!value && !readOnlyRules);
            txtNote.setEnabled(!value);
            servicePanel.setEnabledAll(!value && !readOnlyRules);
            partPanel.setEnabledAll(!value && !readOnlyRules);
            btnSave.setText(value ? "Đang lưu…" : "Lưu");
        }

        private void showFormError(String message) { lblError.setText(message == null ? "Không thể lưu chương trình khuyến mãi." : message); }

        private class CatalogPanel extends JPanel {
            private final boolean services;
            private final JTextField txtCode = new JTextField();
            private final JButton btnFind = button("Tìm mã", true);
            private final JButton btnAddSelected = button("Thêm mục", false);
            private final JButton btnRemove = button("Bỏ chọn", false);
            private final DefaultTableModel resultModel = new DefaultTableModel(new Object[]{"Mã", "Tên"}, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            private final DefaultTableModel selectedModel = new DefaultTableModel(new Object[]{"Mã đã chọn", "Tên"}, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            private final JTable resultTable = new JTable(resultModel);
            private final JTable selectedTable = new JTable(selectedModel);
            private Object found;
            private JLabel hint;

            CatalogPanel(boolean services) {
                super(new BorderLayout(0, 8));
                this.services = services;
                setOpaque(false);
                build();
            }

            private void build() {
                JPanel search = new JPanel(new BorderLayout(8, 0));
                search.setOpaque(false);
                JPanel inputCell = new JPanel(new BorderLayout(0, 5));
                inputCell.setOpaque(false);
                inputCell.add(label(services ? "Tìm theo mã dịch vụ" : "Tìm theo mã linh kiện", 12, BODY, Font.PLAIN), BorderLayout.NORTH);
                txtCode.setFont(new Font("SansSerif", Font.PLAIN, 13));
                txtCode.setPreferredSize(new Dimension(280, 38));
                txtCode.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(new Color(0xC9D5E5), 9), new EmptyBorder(0, 10, 0, 8)));
                txtCode.putClientProperty("JTextField.placeholderText", services ? "Ví dụ: DV00001" : "Ví dụ: LK00001");
                inputCell.add(txtCode, BorderLayout.CENTER);
                search.add(inputCell, BorderLayout.CENTER);
                JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 18));
                controls.setOpaque(false);
                controls.add(btnFind);
                controls.add(btnAddSelected);
                search.add(controls, BorderLayout.EAST);
                add(search, BorderLayout.NORTH);

                JPanel tables = new JPanel(new GridLayout(1, 2, 10, 0));
                tables.setOpaque(false);
                tables.add(makeCatalogTable("Kết quả tìm kiếm", resultTable));
                tables.add(makeCatalogTable("Đang áp dụng", selectedTable));
                add(tables, BorderLayout.CENTER);

                JPanel footer = new JPanel(new BorderLayout(8, 0));
                footer.setOpaque(false);
                hint = label("Chưa tải danh mục. Nhập mã để tìm.", 11, SUBTLE, Font.PLAIN);
                footer.add(hint, BorderLayout.CENTER);
                footer.add(btnRemove, BorderLayout.EAST);
                add(footer, BorderLayout.SOUTH);
                renderSelected();
            }

            private JPanel makeCatalogTable(String title, JTable value) {
                JPanel panel = new JPanel(new BorderLayout(0, 4));
                panel.setOpaque(false);
                panel.add(label(title, 12, BODY, Font.BOLD), BorderLayout.NORTH);
                value.setFont(new Font("SansSerif", Font.PLAIN, 12));
                value.setRowHeight(28);
                value.setShowVerticalLines(false);
                value.setGridColor(BORDER);
                value.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
                value.getTableHeader().setForeground(BODY);
                value.getTableHeader().setBackground(new Color(0xF3F6FA));
                value.getTableHeader().setPreferredSize(new Dimension(0, 28));
                for (int i = 0; i < value.getColumnCount(); i++) value.getColumnModel().getColumn(i).setCellRenderer(stripedRenderer());
                JScrollPane scroll = new JScrollPane(value);
                scroll.setBorder(BorderFactory.createLineBorder(BORDER));
                scroll.setPreferredSize(new Dimension(300, 116));
                panel.add(scroll, BorderLayout.CENTER);
                return panel;
            }

            private void bind() {
                btnFind.addActionListener(e -> findCode());
                txtCode.addActionListener(e -> findCode());
                btnAddSelected.addActionListener(e -> addFound());
                btnRemove.addActionListener(e -> removeSelected());
                txtCode.getDocument().addDocumentListener(new DocumentListener() {
                    @Override public void insertUpdate(DocumentEvent e) { clearError(); }
                    @Override public void removeUpdate(DocumentEvent e) { clearError(); }
                    @Override public void changedUpdate(DocumentEvent e) { clearError(); }
                });
            }

            private void findCode() {
                String code = txtCode.getText().trim();
                if (code.isBlank()) { showFormError("Nhập mã " + (services ? "dịch vụ" : "linh kiện") + " để tìm."); return; }
                String codePattern = services ? "^DV\\d{5,}$" : "^LK\\d{5,}$";
                if (!code.toUpperCase(java.util.Locale.ROOT).matches(codePattern)) { showFormError("Mã " + (services ? "dịch vụ" : "linh kiện") + " không đúng định dạng."); txtCode.requestFocusInWindow(); return; }
                btnFind.setEnabled(false);
                hint.setText("Đang tìm…");
                new SwingWorker<Object, Void>() {
                    @Override protected Object doInBackground() {
                        return services ? formController.timDichVuTheoMa(code) : formController.timLinhKienTheoMa(code);
                    }
                    @Override protected void done() {
                        try {
                            found = get();
                            resultModel.setRowCount(0);
                            if (found instanceof DichVu dv) resultModel.addRow(new Object[]{dv.getMaDV(), dv.getTenDV()});
                            if (found instanceof LinhKien lk) resultModel.addRow(new Object[]{lk.getMaLK(), lk.getTenLK()});
                            if (resultModel.getRowCount() > 0) resultTable.setRowSelectionInterval(0, 0);
                            hint.setText("Đã tìm thấy 1 mục. Chọn Thêm mục để áp dụng.");
                        } catch (Exception ex) {
                            found = null;
                            resultModel.setRowCount(0);
                            Throwable cause = ex instanceof ExecutionException && ex.getCause() != null ? ex.getCause() : ex;
                            showFormError(cause.getMessage());
                            hint.setText("Không tìm thấy mục phù hợp.");
                        } finally { btnFind.setEnabled(true); }
                    }
                }.execute();
            }

            private void addFound() {
                if (found == null) return;
                if (found instanceof DichVu dv) selectedServices.put(dv.getMaDV(), dv);
                if (found instanceof LinhKien lk) selectedParts.put(lk.getMaLK(), lk);
                renderSelected();
                updateTabCounts();
            }

            private void removeSelected() {
                int row = selectedTable.getSelectedRow();
                if (row < 0) return;
                String code = String.valueOf(selectedModel.getValueAt(selectedTable.convertRowIndexToModel(row), 0));
                if (services) selectedServices.remove(code); else selectedParts.remove(code);
                renderSelected();
                updateTabCounts();
            }

            private void renderSelected() {
                selectedModel.setRowCount(0);
                if (services) selectedServices.values().forEach(dv -> selectedModel.addRow(new Object[]{dv.getMaDV(), dv.getTenDV()}));
                else selectedParts.values().forEach(lk -> selectedModel.addRow(new Object[]{lk.getMaLK(), lk.getTenLK()}));
                if (hint != null) hint.setText(selectedModel.getRowCount() + " mục đang áp dụng; danh mục khác chưa được tải.");
            }

            private void setEnabledAll(boolean enabled) {
                txtCode.setEnabled(enabled);
                btnFind.setEnabled(enabled);
                btnAddSelected.setEnabled(enabled);
                btnRemove.setEnabled(enabled);
                resultTable.setEnabled(enabled);
                selectedTable.setEnabled(enabled);
            }
        }

        private void updateTabCounts() {
            Container parent = servicePanel.getParent();
            if (parent instanceof JTabbedPane tabs) {
                tabs.setTitleAt(0, "Dịch vụ (" + selectedServices.size() + " đã chọn)");
                tabs.setTitleAt(1, "Linh kiện (" + selectedParts.size() + " đã chọn)");
            }
        }
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
