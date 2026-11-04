package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * TroGiupUI – Trang "Trợ giúp" hiển thị hướng dẫn theo vai trò và FAQ.
 * Bám sát thiết kế Figma: màn hình Help.
 * Tích hợp vào ContentPanel của MainFrame qua card "PAGE_TRO_GIUP".
 */
public class TroGiupUI extends JPanel {

    // ── Bảng màu (lấy từ MainFrame) ─────────────────────────────────────────
    private static final Color C_WHITE        = Color.WHITE;
    private static final Color C_BORDER       = new Color(0xE2E8F0);
    private static final Color C_INPUT_STROKE = new Color(0xCBD5E1);
    private static final Color C_TEXT_HEAD    = new Color(0x0F172A);
    private static final Color C_TEXT_MAIN    = new Color(0x334155);
    private static final Color C_TEXT_SUBTLE  = new Color(0x64748B);
    private static final Color C_PRIMARY      = new Color(0x2563EB);
    private static final Color C_PRIMARY_HV   = new Color(0x1D4ED8);
    private static final Color C_PRIMARY_BG   = new Color(0xEFF6FF);

    // Nội dung FAQ: {câu hỏi, trả lời}
    private static final String[][] FAQ = {
        {
            "Vì sao tôi không thấy một số chức năng?",
            "Hệ thống phân quyền theo vai trò: Chủ cửa hàng thấy toàn bộ chức năng, "
            + "Tiếp nhận viên chỉ thấy các nghiệp vụ tiếp nhận và thanh toán, "
            + "Kỹ thuật viên chỉ thấy phiếu sửa chữa và yêu cầu linh kiện. "
            + "Liên hệ Chủ cửa hàng nếu bạn cần quyền truy cập bổ sung."
        },
        {
            "Làm thế nào để cập nhật tiến độ sửa chữa?",
            "Vào mục 'Cập nhật sửa chữa' trên thanh điều hướng, chọn phiếu sửa chữa "
            + "được giao cho bạn, rồi bấm nút 'Cập nhật trạng thái'. "
            + "Trạng thái hợp lệ: Đang sửa → Chờ linh kiện → Hoàn tất."
        },
        {
            "Tôi có thể sửa hóa đơn đã thanh toán không?",
            "Không. Hóa đơn đã ở trạng thái 'Đã thanh toán' không thể chỉnh sửa hoặc xóa. "
            + "Nếu cần điều chỉnh, hãy thực hiện Đổi trả trong vòng 7 ngày kể từ ngày thanh toán. "
            + "Liên hệ Chủ cửa hàng để được hỗ trợ."
        }
    };

    // Trạng thái mở/đóng từng FAQ item
    private final boolean[] faqExpanded = new boolean[FAQ.length];
    private final JPanel[] faqAnswerPanels = new JPanel[FAQ.length];
    private final JLabel[] faqArrows = new JLabel[FAQ.length];

    public TroGiupUI() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel content = buildContent();
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel();
        root.setOpaque(false);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(28, 32, 32, 32));

        // ── Tiêu đề trang ────────────────────────────────────────────────────
        JPanel pnlTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlTitle.setOpaque(false);
        pnlTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        JLabel iconTitle = new JLabel(new MainFrame.HelpIcon(new Color(0x3B82F6), 28));
        pnlTitle.add(iconTitle);

        JLabel lblTitle = new JLabel("Trợ giúp");
        lblTitle.setFont(MainFrame.f(Font.BOLD, 24));
        lblTitle.setForeground(C_TEXT_HEAD);
        pnlTitle.add(lblTitle);

        root.add(pnlTitle);
        root.add(Box.createVerticalStrut(20));

        // ── Thanh tìm kiếm ────────────────────────────────────────────────────
        root.add(buildSearchBar());
        root.add(Box.createVerticalStrut(28));

        // ── Tiêu đề "Hướng dẫn theo vai trò" ─────────────────────────────────
        root.add(sectionLabel("Hướng dẫn theo vai trò"));
        root.add(Box.createVerticalStrut(12));
        root.add(buildRoleGuideCards());
        root.add(Box.createVerticalStrut(28));

        // ── Tiêu đề "Câu hỏi thường gặp" ─────────────────────────────────────
        root.add(sectionLabel("Câu hỏi thường gặp"));
        root.add(Box.createVerticalStrut(12));
        root.add(buildFaqSection());
        root.add(Box.createVerticalStrut(20));

        return root;
    }

    // ── Thanh tìm kiếm ────────────────────────────────────────────────────────
    private JPanel buildSearchBar() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(C_INPUT_STROKE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        wrapper.setPreferredSize(new Dimension(0, 52));

        // Icon tìm kiếm
        JLabel lblSearchIcon = new JLabel(new SearchIcon(C_TEXT_SUBTLE, 18));
        lblSearchIcon.setBorder(new EmptyBorder(0, 16, 0, 0));
        wrapper.add(lblSearchIcon, BorderLayout.WEST);

        // Ô nhập
        JTextField txtSearch = new JTextField();
        txtSearch.setBorder(new EmptyBorder(0, 10, 0, 10));
        txtSearch.setFont(MainFrame.f(Font.PLAIN, 13));
        txtSearch.setForeground(C_TEXT_MAIN);
        txtSearch.setOpaque(false);

        // Placeholder
        String placeholder = "Tìm hướng dẫn, ví dụ: lập phiếu sửa chữa, nhập kho, phân quyền…";
        txtSearch.setText(placeholder);
        txtSearch.setForeground(new Color(0x94A3B8));
        txtSearch.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (txtSearch.getText().equals(placeholder)) {
                    txtSearch.setText("");
                    txtSearch.setForeground(C_TEXT_MAIN);
                }
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (txtSearch.getText().isBlank()) {
                    txtSearch.setText(placeholder);
                    txtSearch.setForeground(new Color(0x94A3B8));
                }
            }
        });
        wrapper.add(txtSearch, BorderLayout.CENTER);

        // Nút Tìm kiếm
        JButton btnSearch = new MainFrame.RoundedButton("Tìm kiếm", 10,
                C_PRIMARY, C_PRIMARY_HV, null);
        btnSearch.setFont(MainFrame.f(Font.BOLD, 13));
        btnSearch.setForeground(C_WHITE);
        btnSearch.setPreferredSize(new Dimension(108, 52));
        btnSearch.setFocusPainted(false);
        wrapper.add(btnSearch, BorderLayout.EAST);

        return wrapper;
    }

    // ── 3 card hướng dẫn theo vai trò ─────────────────────────────────────────
    private JPanel buildRoleGuideCards() {
        JPanel row = new JPanel(new GridLayout(1, 3, 12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        row.add(buildRoleGuideCard(
                "nav_thong_ke.png", "Chủ cửa hàng", "Theo dõi và quản lý hoạt động gara",
                new String[]{"Xem báo cáo doanh thu", "Quản lý dịch vụ và tồn kho", "Thiết lập tài khoản, phân quyền"}
        ));
        row.add(buildRoleGuideCard(
                "nav_yeu_cau.png", "Tiếp nhận viên", "Tiếp nhận xe và chăm sóc khách hàng",
                new String[]{"Tạo hồ sơ khách hàng và xe", "Lập phiếu tiếp nhận – sửa chữa", "Lập hóa đơn và thanh toán"}
        ));
        row.add(buildRoleGuideCard(
                "nav_dich_vu.png", "Kỹ thuật viên", "Thực hiện và cập nhật công việc sửa chữa",
                new String[]{"Xem phiếu được phân công", "Cập nhật tiến độ sửa chữa", "Gửi yêu cầu cập nhật linh kiện"}
        ));

        return row;
    }

    private JPanel buildRoleGuideCard(String iconFile, String roleName, String roleDesc, String[] items) {
        JPanel card = new RoundedCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // Header: icon + tên vai trò + mô tả
        JPanel pnlHeader = new JPanel(new BorderLayout(10, 0));
        pnlHeader.setOpaque(false);
        pnlHeader.setBorder(new EmptyBorder(0, 0, 12, 0));

        JPanel pnlLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlLeft.setOpaque(false);

        Image rawIcon = VeCarZoneUI.loadImage(iconFile);
        if (rawIcon != null) {
            pnlLeft.add(new JLabel(new ImageIcon(rawIcon.getScaledInstance(22, 22, Image.SCALE_SMOOTH))));
        }

        JPanel pnlText = new JPanel();
        pnlText.setOpaque(false);
        pnlText.setLayout(new BoxLayout(pnlText, BoxLayout.Y_AXIS));

        JLabel lblRole = new JLabel(roleName);
        lblRole.setFont(MainFrame.f(Font.BOLD, 14));
        lblRole.setForeground(C_TEXT_HEAD);
        pnlText.add(lblRole);

        JLabel lblRoleDesc = new JLabel(roleDesc);
        lblRoleDesc.setFont(MainFrame.f(Font.PLAIN, 11));
        lblRoleDesc.setForeground(C_TEXT_SUBTLE);
        pnlText.add(lblRoleDesc);

        pnlLeft.add(pnlText);
        pnlHeader.add(pnlLeft, BorderLayout.WEST);
        card.add(pnlHeader);

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(C_BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        card.add(sep);
        card.add(Box.createVerticalStrut(10));

        // Danh sách mục hướng dẫn
        for (String item : items) {
            card.add(buildGuideItem(item));
            card.add(Box.createVerticalStrut(2));
        }

        return card;
    }

    private JPanel buildGuideItem(String text) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.setBorder(new EmptyBorder(6, 0, 6, 0));

        JLabel lblText = new JLabel(text);
        lblText.setFont(MainFrame.f(Font.PLAIN, 13));
        lblText.setForeground(C_TEXT_MAIN);
        row.add(lblText, BorderLayout.CENTER);

        JLabel lblArrow = new JLabel("›");
        lblArrow.setFont(MainFrame.f(Font.PLAIN, 16));
        lblArrow.setForeground(C_TEXT_SUBTLE);
        row.add(lblArrow, BorderLayout.EAST);

        // Hiệu ứng hover
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                lblText.setForeground(C_PRIMARY);
                lblArrow.setForeground(C_PRIMARY);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                lblText.setForeground(C_TEXT_MAIN);
                lblArrow.setForeground(C_TEXT_SUBTLE);
            }
        });

        return row;
    }

    // ── FAQ accordion ─────────────────────────────────────────────────────────
    private JPanel buildFaqSection() {
        JPanel faqPanel = new JPanel();
        faqPanel.setOpaque(false);
        faqPanel.setLayout(new BoxLayout(faqPanel, BoxLayout.Y_AXIS));
        faqPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        faqPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        for (int i = 0; i < FAQ.length; i++) {
            faqPanel.add(buildFaqItem(i, FAQ[i][0], FAQ[i][1]));
            if (i < FAQ.length - 1) {
                faqPanel.add(Box.createVerticalStrut(4));
            }
        }
        return faqPanel;
    }

    private JPanel buildFaqItem(int index, String question, String answer) {
        JPanel container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Header (câu hỏi + toggle arrow)
        JPanel header = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_WHITE);
                int arc = faqExpanded[index] ? 12 : 12; // bo góc 12 toàn bộ
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, 52));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        header.setBorder(new EmptyBorder(0, 20, 0, 20));
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblQuestion = new JLabel(question);
        lblQuestion.setFont(MainFrame.f(Font.PLAIN, 13));
        lblQuestion.setForeground(C_TEXT_HEAD);
        header.add(lblQuestion, BorderLayout.CENTER);

        JLabel lblArrow = new JLabel("∨");
        lblArrow.setFont(MainFrame.f(Font.BOLD, 14));
        lblArrow.setForeground(C_TEXT_SUBTLE);
        faqArrows[index] = lblArrow;
        header.add(lblArrow, BorderLayout.EAST);

        // Phần trả lời (ẩn mặc định)
        JPanel answerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xF8FAFC));
                // Vẽ phần dưới (chỉ bo góc dưới)
                g2.fillRect(0, 0, getWidth(), 12);           // vùng trên không bo
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12); // bo toàn bộ
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                // Che đường viền trên (ghép liền với header)
                g2.setColor(new Color(0xF8FAFC));
                g2.fillRect(1, 0, getWidth() - 2, 2);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        answerPanel.setOpaque(false);
        answerPanel.setBorder(new EmptyBorder(12, 20, 16, 20));
        answerPanel.setVisible(false);
        answerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        answerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        faqAnswerPanels[index] = answerPanel;

        JLabel lblAnswer = new JLabel("<html><body style='width:800px'>" + answer + "</body></html>");
        lblAnswer.setFont(MainFrame.f(Font.PLAIN, 13));
        lblAnswer.setForeground(C_TEXT_SUBTLE);
        answerPanel.add(lblAnswer, BorderLayout.CENTER);

        // Toggle khi click header
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                faqExpanded[index] = !faqExpanded[index];
                answerPanel.setVisible(faqExpanded[index]);
                lblArrow.setText(faqExpanded[index] ? "∧" : "∨");
                header.repaint();
                container.revalidate();
                container.repaint();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                lblQuestion.setForeground(C_PRIMARY);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                lblQuestion.setForeground(C_TEXT_HEAD);
            }
        });

        container.add(header);
        container.add(answerPanel);
        return container;
    }

    // ── Label tiêu đề section ─────────────────────────────────────────────────
    private JPanel sectionLabel(String text) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel lbl = new JLabel(text);
        lbl.setFont(MainFrame.f(Font.BOLD, 16));
        lbl.setForeground(C_TEXT_HEAD);
        wrapper.add(lbl);
        return wrapper;
    }

    // ── Helper: card nền trắng bo góc ─────────────────────────────────────────
    private static class RoundedCardPanel extends JPanel {
        RoundedCardPanel() {
            setOpaque(false);
            setBorder(new EmptyBorder(16, 18, 16, 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(new Color(0xE2E8F0));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ── Icon tìm kiếm ─────────────────────────────────────────────────────────
    private static class SearchIcon implements javax.swing.Icon {
        private final Color color;
        private final int size;

        SearchIcon(Color color, int size) {
            this.color = color;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f));
            int r = (int)(size * 0.38);
            int cx = x + r + 1, cy = y + r + 1;
            g2.drawOval(cx - r, cy - r, r * 2, r * 2);
            int ex = (int)(cx + r * Math.cos(Math.toRadians(45)));
            int ey = (int)(cy + r * Math.sin(Math.toRadians(45)));
            g2.drawLine(ex, ey, x + size - 2, y + size - 2);
            g2.dispose();
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }
}
