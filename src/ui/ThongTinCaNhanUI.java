package ui;

import connectDB.DbUtil;
import controller.AuthController;
import controller.AuthController.NavigationItem;
import dao.TaiKhoanDAO;
import entity.NhanVien;
import entity.TaiKhoan;
import enums.VaiTro;
import session.UserSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Màn hình Thông tin cá nhân dùng chung cho cả 3 vai trò (Figma frame 57:454).
 * - Hồ sơ nhân sự trích xuất trực tiếp từ UserSession.
 * - Tab 1: Hồ sơ cá nhân (thẻ bo góc, avatar tròn hoàn hảo, các ô thông tin mềm mại).
 * - Tab 2: Thông tin vai trò & phân quyền tác vụ trong hệ thống.
 * - Hỗ trợ Đổi mật khẩu tài khoản trực tiếp.
 */
public class ThongTinCaNhanUI extends JPanel {

    private static final Color BG = new Color(0xF4F7FB);
    private static final Color WHITE = Color.WHITE;
    private static final Color BORDER = new Color(0xE3E9F2);
    private static final Color PRIMARY = new Color(0x2563EB);
    private static final Color PRIMARY_HV = new Color(0x1D4ED8);
    private static final Color PRIMARY_BG = new Color(0xEFF6FF);
    private static final Color TEXT = new Color(0x14233D);
    private static final Color BODY = new Color(0x40516A);
    private static final Color SUBTLE = new Color(0x7A889C);

    private static final DateTimeFormatter LOCAL_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AuthController authController = new AuthController();
    private final TaiKhoanDAO taiKhoanDao = new TaiKhoanDAO();

    public ThongTinCaNhanUI() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(22, 26, 22, 26));

        JPanel root = new MainFrame.ScrollableContentPanel();
        root.setOpaque(false);

        // Heading
        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        heading.setOpaque(false);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(label("👤", 25, TEXT, Font.PLAIN));
        heading.add(label("Thông tin cá nhân", 24, TEXT, Font.BOLD));
        addFull(root, heading);
        root.add(Box.createVerticalStrut(18));

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabs.setFocusable(false);
        tabs.setAlignmentX(Component.LEFT_ALIGNMENT);

        tabs.addTab("Hồ sơ cá nhân", buildPersonalProfileTab());
        tabs.addTab("Thông tin vai trò", buildRoleInformationTab());

        addFull(root, tabs);

        JScrollPane scroll = new JScrollPane(root);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);
    }

    private JComponent buildPersonalProfileTab() {
        NhanVien nv = UserSession.getInstance().getNhanVien();
        if (nv == null) {
            nv = new NhanVien("NV001");
            nv.setHoTen("Người dùng CarZone");
            nv.setVaiTro(VaiTro.CHU_CUA_HANG);
        }

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(16, 0, 0, 0));
        content.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Khung Profile lớn bo tròn 14px
        JPanel profileCard = new TraCuuUI.RoundedPanel(WHITE, 14, BORDER, new Insets(24, 26, 24, 26));
        profileCard.setLayout(new BorderLayout(24, 0));
        profileCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Cột bên trái: Avatar tròn và chức danh
        String fullName = nv.getHoTen() == null ? "Người dùng" : nv.getHoTen();
        String initial = fullName.isBlank() ? "U" : fullName.trim().substring(0, 1).toUpperCase();

        JLabel avatar = new JLabel(initial, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PRIMARY_BG);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(new Color(0xBFDBFE));
                g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setFont(new Font("SansSerif", Font.BOLD, 34));
        avatar.setForeground(PRIMARY);
        avatar.setPreferredSize(new Dimension(84, 84));
        avatar.setMaximumSize(new Dimension(84, 84));
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        identity.add(avatar);
        identity.add(Box.createVerticalStrut(12));

        JLabel lblName = label(fullName, 17, TEXT, Font.BOLD);
        lblName.setAlignmentX(Component.CENTER_ALIGNMENT);
        identity.add(lblName);
        identity.add(Box.createVerticalStrut(4));

        String roleStr = AuthController.layTenVaiTro(nv.getVaiTro()) + " · " + (nv.getMaNV() == null ? "" : nv.getMaNV());
        JLabel lblRole = label(roleStr, 12, SUBTLE, Font.PLAIN);
        lblRole.setAlignmentX(Component.CENTER_ALIGNMENT);
        identity.add(lblRole);
        identity.add(Box.createVerticalStrut(16));

        JButton btnDoiMatKhau = button("Đổi mật khẩu", false, 140);
        btnDoiMatKhau.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnDoiMatKhau.addActionListener(e -> moDialogDoiMatKhau());
        identity.add(btnDoiMatKhau);

        profileCard.add(identity, BorderLayout.WEST);

        // Lưới thông tin bên phải (3 cột x 3 dòng)
        JPanel fields = new JPanel(new GridLayout(4, 3, 12, 12));
        fields.setOpaque(false);

        String username = UserSession.getInstance().getTenDangNhap();
        if (username == null || username.isBlank()) username = "Chưa có tài khoản";

        fields.add(createInfoField("Họ và tên", fullName));
        fields.add(createInfoField("Mã nhân viên", nv.getMaNV()));
        fields.add(createInfoField("Tên đăng nhập", username));
        fields.add(createInfoField("Vai trò hệ thống", AuthController.layTenVaiTro(nv.getVaiTro())));
        fields.add(createInfoField("Số điện thoại", nv.getSoDienThoai()));
        fields.add(createInfoField("CCCD", nv.getCCCD()));
        fields.add(createInfoField("Ngày sinh", nv.getNgaySinh() == null ? null : nv.getNgaySinh().format(LOCAL_DATE_FMT)));
        fields.add(createInfoField("Ngày vào làm", nv.getNgayVaoLam() == null ? null : nv.getNgayVaoLam().format(LOCAL_DATE_FMT)));
        fields.add(createInfoField("Tuổi", String.valueOf(nv.getTuoi())));
        fields.add(createInfoField("Địa chỉ", nv.getDiaChi()));

        profileCard.add(fields, BorderLayout.CENTER);
        addFull(content, profileCard);

        return content;
    }

    private JComponent buildRoleInformationTab() {
        NhanVien nv = UserSession.getInstance().getNhanVien();
        VaiTro vt = nv == null ? VaiTro.CHU_CUA_HANG : nv.getVaiTro();

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(16, 0, 0, 0));

        JPanel intro = new TraCuuUI.RoundedPanel(WHITE, 12, BORDER, new Insets(14, 18, 14, 18));
        intro.setLayout(new BorderLayout());
        JLabel lblIntro = label("Vai trò hiện tại: " + AuthController.layTenVaiTro(vt), 15, TEXT, Font.BOLD);
        intro.add(lblIntro, BorderLayout.CENTER);
        content.add(intro, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Chức năng phân hệ", "Phạm vi tác vụ được phép"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        for (NavigationItem nav : authController.layDanhSachNavigationTheoRole(vt)) {
            model.addRow(new Object[]{nav.getTieuDe(), nav.getBreadcrumb()});
        }

        JTable table = new JTable(model);
        styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new TraCuuUI.RoundedLineBorder(BORDER, 10));
        scroll.getViewport().setBackground(WHITE);
        scroll.setPreferredSize(new Dimension(980, 320));

        content.add(scroll, BorderLayout.CENTER);
        return content;
    }

    private JPanel createInfoField(String labelText, String valueText) {
        JPanel card = new TraCuuUI.RoundedPanel(new Color(0xF8FAFC), 10, new Color(0xEDF2F7), new Insets(8, 12, 8, 12));
        card.setLayout(new BorderLayout(0, 4));
        card.add(label(labelText, 11, SUBTLE, Font.PLAIN), BorderLayout.NORTH);
        card.add(label(valueText == null || valueText.isBlank() ? "—" : valueText, 13, TEXT, Font.BOLD), BorderLayout.CENTER);
        return card;
    }

    private void moDialogDoiMatKhau() {
        String username = UserSession.getInstance().getTenDangNhap();
        if (username == null || username.isBlank() || "Chưa có tài khoản".equals(username)) {
            JOptionPane.showMessageDialog(this, "Tài khoản hiện tại chưa được cấp quyền đăng nhập.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JPasswordField pfOld = new JPasswordField();
        JPasswordField pfNew = new JPasswordField();
        JPasswordField pfConfirm = new JPasswordField();

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 10));
        form.add(new JLabel("Mật khẩu hiện tại:")); form.add(pfOld);
        form.add(new JLabel("Mật khẩu mới (≥ 6 ký tự):")); form.add(pfNew);
        form.add(new JLabel("Xác nhận mật khẩu mới:")); form.add(pfConfirm);

        int opt = JOptionPane.showConfirmDialog(this, form, "Đổi mật khẩu tài khoản", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            String oldPass = new String(pfOld.getPassword()).trim();
            String newPass = new String(pfNew.getPassword()).trim();
            String confirmPass = new String(pfConfirm.getPassword()).trim();

            if (oldPass.isEmpty() || newPass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ mật khẩu.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (newPass.length() < 6) {
                JOptionPane.showMessageDialog(this, "Mật khẩu mới phải có ít nhất 6 ký tự.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!newPass.equals(confirmPass)) {
                JOptionPane.showMessageDialog(this, "Xác nhận mật khẩu mới không khớp.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                TaiKhoan tk = taiKhoanDao.timTheoTenDangNhap(username);
                if (tk == null || !tk.khopMatKhau(oldPass)) {
                    JOptionPane.showMessageDialog(this, "Mật khẩu hiện tại không chính xác.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Cập nhật mật khẩu băm SHA-256
                tk.setMatKhau(newPass);
                String sql = "UPDATE TaiKhoan SET matKhau = ? WHERE tenDangNhap = ?";
                try (Connection con = DbUtil.getConnection();
                     PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, tk.getMatKhau());
                    ps.setString(2, username);
                    ps.executeUpdate();
                }

                JOptionPane.showMessageDialog(this, "Đổi mật khẩu thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi cập nhật mật khẩu: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
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
        b.setPreferredSize(new Dimension(width, 36));
        b.setFocusPainted(false);
        return b;
    }
}

