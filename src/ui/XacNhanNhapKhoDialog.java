package ui;

import controller.PhieuYeuCauNhapLinhKienController;
import entity.PhieuYeuCauNhapLinhKien;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.text.NumberFormat;
import java.util.concurrent.ExecutionException;

/** Form xác nhận số lượng thực nhận; phần tăng tồn kho do trigger CSDL thực hiện. */
public class XacNhanNhapKhoDialog extends JDialog {
    private static final Color BG = new Color(0xF8FAFC);
    private static final Color WHITE = Color.WHITE;
    private static final Color BORDER = new Color(0xDCE4EF);
    private static final Color PRIMARY = new Color(0x2563EB);
    private static final Color TEXT = new Color(0x17233A);
    private static final Color BODY = new Color(0x3B4B65);
    private static final Color SUBTLE = new Color(0x6B7D98);
    private static final Color RED = new Color(0xDC2626);

    private final PhieuYeuCauNhapLinhKienController controller;
    private final PhieuYeuCauNhapLinhKien request;
    private final JSpinner spnReceived;
    private final JTextField txtAfter = readOnlyField();
    private final JTextField txtDelta = readOnlyField();
    private final JTextArea txtNote = new JTextArea(4, 40);
    private final JLabel lblError = label("", 12, RED, Font.PLAIN);
    private final JButton btnConfirm = button("Xác nhận", true);
    private final JButton btnCancel = button("Hủy", false);
    private boolean confirmed;
    private boolean saving;

    public XacNhanNhapKhoDialog(Window owner, PhieuYeuCauNhapLinhKien request,
                                PhieuYeuCauNhapLinhKienController controller) {
        super(owner, "CarZone — Xác nhận nhập kho", ModalityType.APPLICATION_MODAL);
        this.request = request;
        this.controller = controller;
        this.spnReceived = new JSpinner(new SpinnerNumberModel(request.getSoLuongYeuCau(), 1, Integer.MAX_VALUE, 1));
        UIInputRules.restrictIntegerSpinner(this.spnReceived);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(buildContent());
        pack();
        setSize(660, 660);
        setMinimumSize(new Dimension(620, 620));
        setLocationRelativeTo(owner);
        bindActions();
        updateStockPreview();
        SwingUtilities.invokeLater(() -> spnReceived.requestFocusInWindow());
    }

    public boolean isConfirmed() { return confirmed; }

    private JComponent buildContent() {
        JPanel base = new JPanel(new BorderLayout());
        base.setBackground(BG);
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(22, 24, 16, 24));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JLabel title = label("📦  Xác nhận nhập kho", 20, TEXT, Font.BOLD);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(title);
        content.add(gap(18));
        addField(content, "Yêu cầu đã duyệt", readOnlyField(request.getMaYeuCau() + " — Đã duyệt"));
        content.add(gap(14));
        addField(content, "Linh kiện", readOnlyField(request.getLinhKien().getMaLK() + " — " + request.getLinhKien().getTenLK()));
        content.add(gap(14));

        JPanel qtyRow = new JPanel(new GridLayout(1, 2, 16, 0));
        qtyRow.setOpaque(false);
        addCell(qtyRow, "Số lượng yêu cầu", readOnlyField(request.getSoLuongYeuCau() + " " + request.getLinhKien().getDonViTinh()));
        addCell(qtyRow, "Số lượng nhập thực tế * (> 0)", spnReceived);
        qtyRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(qtyRow);
        content.add(gap(14));

        JPanel stockRow = new JPanel(new GridLayout(1, 3, 14, 0));
        stockRow.setOpaque(false);
        addCell(stockRow, "Tồn trước", readOnlyField(String.valueOf(request.getLinhKien().getSoLuongTon())));
        addCell(stockRow, "Nhập thực tế", txtDelta);
        addCell(stockRow, "Tồn sau", txtAfter);
        stockRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(stockRow);
        content.add(gap(14));

        txtNote.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtNote.setLineWrap(true);
        txtNote.setWrapStyleWord(true);
        txtNote.setBorder(new EmptyBorder(8, 10, 8, 10));
        txtNote.setText("Đã kiểm đếm, thực nhận " + request.getSoLuongYeuCau() + " " + request.getLinhKien().getTenLK() + ".");
        addField(content, "Ghi chú (tối đa 300 ký tự)", new JScrollPane(txtNote));
        content.add(lblError);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        base.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        footer.setBackground(WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));
        footer.add(btnConfirm);
        footer.add(btnCancel);
        base.add(footer, BorderLayout.SOUTH);
        return base;
    }

    private void bindActions() {
        btnConfirm.addActionListener(e -> confirm());
        btnCancel.addActionListener(e -> dispose());
        ChangeListener quantityListener = e -> updateStockPreview();
        spnReceived.addChangeListener(quantityListener);
        JComponent editor = spnReceived.getEditor();
        if (editor instanceof JSpinner.NumberEditor numberEditor) {
            numberEditor.getTextField().addActionListener(e -> txtNote.requestFocusInWindow());
        }
        txtNote.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { lblError.setText(""); }
            @Override public void removeUpdate(DocumentEvent e) { lblError.setText(""); }
            @Override public void changedUpdate(DocumentEvent e) { lblError.setText(""); }
        });
    }

    private void updateStockPreview() {
        int actual = ((Number) spnReceived.getValue()).intValue();
        long after = (long) request.getLinhKien().getSoLuongTon() + actual;
        txtDelta.setText("+ " + actual);
        txtAfter.setText(NumberFormat.getIntegerInstance().format(after));
        if (after > Integer.MAX_VALUE) {
            lblError.setText("Tồn kho sau nhập vượt giới hạn dữ liệu.");
            btnConfirm.setEnabled(false);
        } else {
            if (lblError.getText().startsWith("Tồn kho sau nhập")) lblError.setText("");
            btnConfirm.setEnabled(!saving);
        }
    }

    private void confirm() {
        if (saving) return;
        int actual;
        try {
            spnReceived.commitEdit();
            actual = ((Number) spnReceived.getValue()).intValue();
        } catch (java.text.ParseException e) {
            lblError.setText("Số lượng nhập thực tế không hợp lệ.");
            return;
        }
        if (actual <= 0) { lblError.setText("Số lượng nhập thực tế phải lớn hơn 0."); return; }
        if ((long) request.getLinhKien().getSoLuongTon() + actual > Integer.MAX_VALUE) {
            lblError.setText("Tồn kho sau nhập vượt giới hạn dữ liệu.");
            return;
        }
        String note = txtNote.getText().trim();
        if (note.length() > 300) { lblError.setText("Ghi chú xử lý tối đa 300 ký tự."); return; }
        if (!UIInputRules.isMultilineText(note)) { lblError.setText("Ghi chú chứa ký tự không hợp lệ."); txtNote.requestFocusInWindow(); return; }
        setSaving(true);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                controller.xacNhanNhapKho(request.getMaYeuCau(), actual, note);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    confirmed = true;
                    dispose();
                } catch (Exception ex) {
                    Throwable cause = ex instanceof ExecutionException && ex.getCause() != null ? ex.getCause() : ex;
                    setSaving(false);
                    lblError.setText(cause.getMessage() == null ? "Không thể xác nhận nhập kho." : cause.getMessage());
                }
            }
        }.execute();
    }

    private void setSaving(boolean value) {
        saving = value;
        spnReceived.setEnabled(!value);
        txtNote.setEnabled(!value);
        btnCancel.setEnabled(!value);
        btnConfirm.setEnabled(!value);
        btnConfirm.setText(value ? "Đang xử lý…" : "Xác nhận");
    }

    private static void addField(JPanel parent, String caption, JComponent field) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 6));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(label(caption, 12, BODY, Font.PLAIN), BorderLayout.NORTH);
        field.setPreferredSize(new Dimension(570, field instanceof JScrollPane ? 96 : 44));
        wrapper.add(field, BorderLayout.CENTER);
        parent.add(wrapper);
    }

    private static void addCell(JPanel parent, String caption, JComponent field) {
        JPanel cell = new JPanel(new BorderLayout(0, 6));
        cell.setOpaque(false);
        cell.add(label(caption, 12, BODY, Font.PLAIN), BorderLayout.NORTH);
        field.setPreferredSize(new Dimension(175, 44));
        cell.add(field, BorderLayout.CENTER);
        parent.add(cell);
    }

    private static JTextField readOnlyField() { return readOnlyField(""); }
    private static JTextField readOnlyField(String value) {
        JTextField field = new JTextField(value);
        field.setEditable(false);
        field.setForeground(SUBTLE);
        field.setBackground(new Color(0xF4F7FB));
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(new Color(0xDCE4EF), 10), new EmptyBorder(0, 10, 0, 10)));
        return field;
    }

    private static JButton button(String text, boolean primary) {
        JButton button = new MainFrame.RoundedButton(text, 10, primary ? PRIMARY : WHITE,
                primary ? new Color(0x1D4ED8) : new Color(0xF1F5F9), primary ? PRIMARY : BORDER);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setForeground(primary ? WHITE : BODY);
        button.setPreferredSize(new Dimension(primary ? 96 : 62, 42));
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
