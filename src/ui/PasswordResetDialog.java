package ui;

import com.formdev.flatlaf.FlatClientProperties;
import controller.PasswordResetController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Password recovery dialog following the CarZone Figma recovery card. */
public final class PasswordResetDialog extends JDialog {
    private static final Color BG = new Color(0xF8FAFC), WHITE = Color.WHITE,
            LINE = new Color(0xE2E8F0), INPUT = new Color(0xCBD5E1), BLUE = new Color(0x2563EB),
            TEXT = new Color(0x202A36), MUTED = new Color(0x64748B), ERROR = new Color(0xB42318), SUCCESS = new Color(0x15803D);
    private final PasswordResetController controller = new PasswordResetController();
    private final String tenDangNhap;
    private final JTextField code = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JPasswordField confirm = new JPasswordField();
    private final JButton confirmCode = button("Xác nhận", true);
    private final JButton reset = button("Đặt lại mật khẩu", true);
    private final JPanel statusPanel = new JPanel();
    private final JLabel emailDestination = new JLabel("Đang xác định Gmail khôi phục…");
    private final JLabel heading = new JLabel("Quên mật khẩu");
    private final JLabel intro = new JLabel("Mã xác thực sẽ được gửi tự động tới Gmail khôi phục của tài khoản.");
    private final JButton resend = new JButton("Gửi lại mã");
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pages = new JPanel(pageLayout);
    private boolean codeSent;

    public PasswordResetDialog(Window owner, String tenDangNhap) {
        super(owner, "Quên mật khẩu", ModalityType.APPLICATION_MODAL);
        this.tenDangNhap = tenDangNhap == null ? "" : tenDangNhap.strip();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(buildContent());
        chiChoPhepNhapSo(code, 6);
        setSize(680, 660);
        setMinimumSize(new Dimension(620, 610));
        setLocationRelativeTo(owner);
        bindActions();
        SwingUtilities.invokeLater(this::guiMa);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new GridBagLayout()); root.setBackground(BG); root.setBorder(new EmptyBorder(24, 24, 24, 24));
        JPanel card = new MainFrame.RoundedPanel(WHITE, 20, LINE, new Insets(28, 28, 24, 28));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel logoTitle = new JLabel("CARZONE"); logoTitle.setFont(new Font("SansSerif", Font.BOLD, 24)); logoTitle.setForeground(BLUE); center(logoTitle);
        JLabel tagline = new JLabel("HỆ THỐNG QUẢN LÝ GARA Ô TÔ"); tagline.setFont(new Font("SansSerif", Font.PLAIN, 12)); tagline.setForeground(MUTED); center(tagline);
        card.add(logoTitle); card.add(Box.createVerticalStrut(8)); card.add(tagline); card.add(Box.createVerticalStrut(24));
        heading.setFont(new Font("SansSerif", Font.BOLD, 19)); heading.setForeground(TEXT); center(heading);
        card.add(heading); card.add(Box.createVerticalStrut(8));
        intro.setFont(new Font("SansSerif", Font.PLAIN, 13)); intro.setForeground(MUTED); intro.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(intro); card.add(Box.createVerticalStrut(22));
        pages.setOpaque(false);
        pages.add(buildOtpPage(), "OTP");
        pages.add(buildPasswordPage(), "PASSWORD");
        pages.setAlignmentX(Component.CENTER_ALIGNMENT);
        pages.setPreferredSize(new Dimension(500, 300));
        pages.setMaximumSize(new Dimension(500, 300));
        card.add(pages);

        statusPanel.setOpaque(false); statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusPanel.setAlignmentX(Component.CENTER_ALIGNMENT); statusPanel.setBorder(new EmptyBorder(8, 0, 0, 0));
        statusPanel.setPreferredSize(new Dimension(500, 48)); statusPanel.setMaximumSize(new Dimension(500, 48)); card.add(statusPanel);
        GridBagConstraints c = new GridBagConstraints(); c.gridx=0; c.gridy=0; c.fill=GridBagConstraints.HORIZONTAL; c.weightx=1; c.anchor=GridBagConstraints.CENTER; root.add(card,c);
        return root;
    }

    private void bindActions() {
        confirmCode.addActionListener(e -> {
            xacThucMa();
        });
        reset.addActionListener(e -> datLai());
    }

    private JPanel buildOtpPage() {
        JPanel form = createForm();
        addLabel(form, "Gmail khôi phục");
        emailDestination.setFont(new Font("SansSerif", Font.PLAIN, 14)); emailDestination.setForeground(TEXT); center(emailDestination); form.add(emailDestination);
        form.add(Box.createVerticalStrut(22)); addLabel(form, "Mã xác thực"); styleInput(code, "Nhập mã xác thực 6 số"); form.add(code);
        JLabel help = new JLabel("Mã có hiệu lực trong 5 phút."); help.setFont(new Font("SansSerif", Font.PLAIN, 12)); help.setForeground(MUTED); center(help);
        form.add(Box.createVerticalStrut(7)); form.add(help); form.add(Box.createVerticalStrut(16));
        JPanel resendRow = new JPanel(new BorderLayout()); resendRow.setOpaque(false); resendRow.setAlignmentX(Component.CENTER_ALIGNMENT); resendRow.setPreferredSize(new Dimension(500, 28)); resendRow.setMaximumSize(new Dimension(500, 28));
        JLabel noCode = new JLabel("Bạn chưa nhận được mã?"); noCode.setFont(new Font("SansSerif", Font.PLAIN, 13)); noCode.setForeground(MUTED);
        resend.setBorderPainted(false); resend.setContentAreaFilled(false); resend.setForeground(BLUE); resend.setFont(new Font("SansSerif", Font.BOLD, 13)); resend.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); resend.addActionListener(e -> guiMa());
        resendRow.add(noCode, BorderLayout.WEST); resendRow.add(resend, BorderLayout.EAST); form.add(resendRow); form.add(Box.createVerticalStrut(16));
        JPanel actions = createActionsRow();
        actions.add(confirmCode); actions.add(Box.createRigidArea(new Dimension(10, 0)));
        JButton back = button("Quay lại đăng nhập", false); back.addActionListener(e -> dispose()); actions.add(back); form.add(actions);
        code.addActionListener(e -> xacThucMa());
        return form;
    }

    private JPanel buildPasswordPage() {
        JPanel form = createForm();
        form.add(Box.createVerticalStrut(22));
        addLabel(form, "Mật khẩu mới"); styleInput(password, "Nhập mật khẩu mới"); form.add(password); form.add(Box.createVerticalStrut(18));
        addLabel(form, "Xác nhận mật khẩu"); styleInput(confirm, "Nhập lại mật khẩu mới"); form.add(confirm); form.add(Box.createVerticalStrut(12));
        JLabel rules = new JLabel("Tối thiểu 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt."); rules.setFont(new Font("SansSerif", Font.PLAIN, 12)); rules.setForeground(MUTED); center(rules);
        form.add(rules); form.add(Box.createVerticalStrut(22));
        JPanel actions = createActionsRow();
        actions.add(Box.createHorizontalGlue()); actions.add(reset); actions.add(Box.createHorizontalGlue()); form.add(actions);
        password.addActionListener(e -> confirm.requestFocusInWindow()); confirm.addActionListener(e -> datLai());
        return form;
    }

    private JPanel createForm() {
        JPanel form = new JPanel(); form.setOpaque(false); form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setAlignmentX(Component.CENTER_ALIGNMENT); form.setPreferredSize(new Dimension(500, 300)); form.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));
        return form;
    }

    private JPanel createActionsRow() {
        JPanel row = new JPanel(); row.setOpaque(false); row.setAlignmentX(Component.CENTER_ALIGNMENT); row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setPreferredSize(new Dimension(440, 46)); row.setMinimumSize(new Dimension(440, 46)); row.setMaximumSize(new Dimension(440, 46));
        return row;
    }

    private void guiMa() {
        setBusy(true);
        showStatus("Đang gửi mã xác thực…", true);
        new SwingWorker<PasswordResetController.SendOutcome, Void>() {
            @Override protected PasswordResetController.SendOutcome doInBackground() { return controller.guiMaXacThuc(tenDangNhap); }
            @Override protected void done() {
                setBusy(false);
                try {
                    PasswordResetController.SendOutcome outcome=get();
                    if (outcome.emailDaChe()!=null) emailDestination.setText(outcome.emailDaChe());
                    switch (outcome.result()) {
                        case SENT -> { codeSent=true; showStatus("Mã xác thực đã được gửi tới Gmail khôi phục.", true); code.requestFocusInWindow(); }
                        case INVALID_USERNAME -> showStatus("Hãy nhập tên đăng nhập trên màn hình đăng nhập rồi mở lại chức năng này.", false);
                        case MISSING_EMAIL -> showStatus("Tài khoản chưa có Gmail khôi phục. Liên hệ Chủ cửa hàng để cập nhật.", false);
                        case TOO_SOON -> { codeSent=outcome.maHienTaiConDung(); showStatus(codeSent ? "Mã đã được gửi gần đây. Hãy dùng mã đó hoặc chờ 1 phút để gửi lại." : "Yêu cầu gửi mã gần đây chưa thành công. Hãy thử lại sau 1 phút.", false); }
                        case UNAVAILABLE -> showStatus("Chưa cấu hình Gmail gửi thư. Vui lòng liên hệ Chủ cửa hàng.", false);
                        case AUTHENTICATION_FAILED -> showStatus("Gmail từ chối đăng nhập SMTP. Kiểm tra địa chỉ gửi và Google App Password trong cấu hình local.", false);
                        case CONNECTION_FAILED -> showStatus("Không kết nối được máy chủ Gmail SMTP. Kiểm tra mạng hoặc tường lửa rồi thử lại.", false);
                        case DELIVERY_FAILED -> showStatus("Gmail không nhận yêu cầu gửi thư. Kiểm tra cấu hình SMTP và địa chỉ Gmail khôi phục.", false);
                        case FAILED -> showStatus("Không thể gửi mã cho tài khoản này. Kiểm tra lại hồ sơ hoặc thử sau.", false);
                    }
                    confirmCode.setEnabled(codeSent);
                    reset.setEnabled(codeSent);
                } catch (java.util.concurrent.ExecutionException e) {
                    emailDestination.setText("Không thể tra cứu Gmail khôi phục.");
                    showStatus("Không truy vấn được hồ sơ nhân viên. Hãy kiểm tra kết nối cơ sở dữ liệu rồi thử lại.", false);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    emailDestination.setText("Chưa tra cứu được Gmail khôi phục.");
                    showStatus("Yêu cầu tra cứu bị gián đoạn. Vui lòng thử lại.", false);
                }
            }
        }.execute();
    }

    private void xacThucMa() {
        String ma = code.getText().strip();
        if (!codeSent) { showStatus("Vui lòng chờ mã xác thực được gửi.", false); return; }
        if (!ma.matches("\\d{6}")) { showStatus("Mã xác thực phải gồm 6 chữ số.", false); code.requestFocusInWindow(); return; }
        setBusy(true); showStatus("Đang xác thực mã…", true);
        new SwingWorker<PasswordResetController.ResetResult, Void>() {
            @Override protected PasswordResetController.ResetResult doInBackground() { return controller.xacThucMa(tenDangNhap, ma); }
            @Override protected void done() {
                setBusy(false);
                try {
                    switch (get()) {
                        case VALIDATED -> showPasswordPage();
                        case INVALID_CODE -> showStatus("Mã xác thực không đúng. Vui lòng kiểm tra lại.", false);
                        case EXPIRED -> showStatus("Mã đã hết hạn. Hãy gửi lại mã mới.", false);
                        case LOCKED -> showStatus("Mã xác thực đã bị khóa. Hãy yêu cầu mã mới.", false);
                        case FAILED -> showStatus("Không thể xác thực mã. Hãy gửi lại mã rồi thử lại.", false);
                        case SUCCESS -> showStatus("", false);
                    }
                } catch (Exception e) { showStatus("Không thể xác thực mã. Vui lòng thử lại.", false); }
            }
        }.execute();
    }

    private void showPasswordPage() {
        heading.setText("Đặt lại mật khẩu");
        intro.setText("Mã xác thực hợp lệ. Hãy tạo mật khẩu mới cho tài khoản.");
        pageLayout.show(pages, "PASSWORD");
        showStatus("Mã xác thực đã được xác nhận.", true);
        pack(); fitToScreen(); password.requestFocusInWindow();
    }

    private void showOtpPage() {
        heading.setText("Quên mật khẩu");
        intro.setText("Nhập mã xác thực đã gửi tới Gmail khôi phục của tài khoản.");
        pageLayout.show(pages, "OTP");
        showStatus("", true); pack(); fitToScreen(); code.requestFocusInWindow();
    }

    private void fitToScreen() {
        GraphicsConfiguration configuration = getOwner() == null
                ? GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration()
                : getOwner().getGraphicsConfiguration();
        Rectangle bounds = configuration.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        int maxWidth = bounds.width - insets.left - insets.right - 48;
        int maxHeight = bounds.height - insets.top - insets.bottom - 48;
        setSize(Math.min(getWidth(), maxWidth), Math.min(getHeight(), maxHeight));
        setLocationRelativeTo(getOwner());
    }

    private void datLai() {
        String pass=new String(password.getPassword()), confirmValue=new String(confirm.getPassword());
        setBusy(true);
        new SwingWorker<PasswordResetController.ResetResult, Void>() {
            @Override protected PasswordResetController.ResetResult doInBackground() { return controller.datLaiMatKhau(tenDangNhap,pass,confirmValue); }
            @Override protected void done() {
                setBusy(false);
                try {
                    switch(get()) {
                        case VALIDATED -> showStatus("Mã đã được xác nhận. Hãy tiếp tục đặt mật khẩu.", true);
                        case SUCCESS -> { JOptionPane.showMessageDialog(PasswordResetDialog.this,"Mật khẩu đã được cập nhật. Bạn có thể đăng nhập bằng mật khẩu mới.","Đặt lại mật khẩu",JOptionPane.INFORMATION_MESSAGE); dispose(); }
                        case INVALID_CODE -> showStatus("Mã không đúng. Bạn còn tối đa 5 lần nhập.",false);
                        case EXPIRED -> { showOtpPage(); showStatus("Mã đã hết hạn. Vui lòng yêu cầu mã mới.",false); }
                        case LOCKED -> { showOtpPage(); showStatus("Mã bị khóa do nhập sai quá số lần cho phép. Hãy yêu cầu mã mới sau 1 phút.",false); }
                        case FAILED -> showStatus("Không thể đặt lại mật khẩu. Hãy yêu cầu mã mới và thử lại.",false);
                    }
                } catch (Exception e) { Throwable cause=e.getCause(); showStatus(cause instanceof IllegalArgumentException ? cause.getMessage() : "Không thể đặt lại mật khẩu.",false); }
            }
        }.execute();
    }

    private void setBusy(boolean busy) { resend.setEnabled(!busy); confirmCode.setEnabled(!busy && codeSent); reset.setEnabled(!busy && codeSent); setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor()); }
    private void showStatus(String text, boolean success) {
        statusPanel.removeAll();
        statusPanel.add(Box.createVerticalGlue());
        Font font = new Font("SansSerif", Font.PLAIN, 12);
        for (String line : wrapStatus(text == null ? "" : text, font, 480)) {
            JLabel label = new JLabel(line, SwingConstants.CENTER);
            label.setFont(font); label.setForeground(success ? SUCCESS : ERROR);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            label.setPreferredSize(new Dimension(500, 17)); label.setMaximumSize(new Dimension(500, 17));
            statusPanel.add(label);
        }
        statusPanel.add(Box.createVerticalGlue());
        statusPanel.revalidate(); statusPanel.repaint();
    }
    private List<String> wrapStatus(String text, Font font, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        FontMetrics metrics = statusPanel.getFontMetrics(font);
        for (String word : text.strip().split("\\s+")) {
            if (word.isEmpty()) continue;
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && metrics.stringWidth(candidate) > maxWidth) {
                lines.add(line.toString()); line.setLength(0); line.append(word);
            } else {
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }
    private static void center(JComponent c) { c.setAlignmentX(Component.CENTER_ALIGNMENT); }
    private static void addLabel(JPanel p,String text) { JLabel label=new JLabel(text); label.setFont(new Font("SansSerif",Font.PLAIN,13)); label.setForeground(new Color(0x334155)); label.setAlignmentX(Component.CENTER_ALIGNMENT); label.setPreferredSize(new Dimension(500,24)); label.setMaximumSize(new Dimension(500,24)); label.setBorder(new EmptyBorder(0,0,7,0)); p.add(label); }
    private static void styleInput(JTextField field,String placeholder) {
        field.setFont(new Font("SansSerif",Font.PLAIN,14)); field.setPreferredSize(new Dimension(500,46)); field.setMaximumSize(new Dimension(500,46)); field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(INPUT,1,true),new EmptyBorder(0,12,0,12)));
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,placeholder); field.setAlignmentX(Component.CENTER_ALIGNMENT);
    }
    private static void chiChoPhepNhapSo(JTextField field, int maxLength) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            private boolean hopLe(String text) { return text.length() <= maxLength && text.chars().allMatch(Character::isDigit); }
            @Override public void insertString(FilterBypass fb,int offset,String string,AttributeSet attr) throws javax.swing.text.BadLocationException {
                String old=fb.getDocument().getText(0,fb.getDocument().getLength()); String next=old.substring(0,offset)+string+old.substring(offset);
                if(hopLe(next)) super.insertString(fb,offset,string,attr);
            }
            @Override public void replace(FilterBypass fb,int offset,int length,String text,AttributeSet attrs) throws javax.swing.text.BadLocationException {
                String old=fb.getDocument().getText(0,fb.getDocument().getLength()); String next=old.substring(0,offset)+(text==null?"":text)+old.substring(offset+length);
                if(hopLe(next)) super.replace(fb,offset,length,text,attrs);
            }
        });
    }
    private static JButton button(String text,boolean primary) {
        Color bg=primary?BLUE:WHITE, hover=primary?new Color(0x1D4ED8):new Color(0xF1F5F9), stroke=primary?BLUE:LINE;
        JButton b=new MainFrame.RoundedButton(text,12,bg,hover,stroke); b.setFont(new Font("SansSerif",Font.BOLD,13)); b.setForeground(primary?Color.WHITE:new Color(0x334155)); b.setBorder(new EmptyBorder(8,16,8,16)); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); b.setPreferredSize(new Dimension(primary?200:210,46)); b.setMinimumSize(new Dimension(primary?190:190,46)); return b;
    }
}
