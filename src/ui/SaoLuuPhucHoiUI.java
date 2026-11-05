package ui;

import audit.AuditLogger;
import backup.*;
import session.UserSession;
import ui.MainFrame.AlertTriangleIcon;
import ui.MainFrame.BackupIcon;
import ui.MainFrame.CalendarIcon;
import ui.MainFrame.DotIcon;
import ui.MainFrame.InfoIcon;
import ui.MainFrame.RefreshIcon;
import ui.MainFrame.RestoreIcon;
import ui.MainFrame.RoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Giao diện Quản lý Sao lưu & Phục hồi cơ sở dữ liệu (Dành riêng cho Chủ cửa
 * hàng).
 * Thiết kế chuẩn FlatLaf sáng (Light Theme) theo phong cách CarZone:
 * - Hệ thống vector icon thống nhất từ MainFrame (chống lỗi ô vuông emoji).
 * - Layout chống tràn ngang, chống lệch dòng và co giãn linh hoạt.
 * - Hỗ trợ chuỗi phục hồi tự động thông minh cho cả Full Backup, Differential
 * và Transaction Log.
 */
public class SaoLuuPhucHoiUI extends JPanel {

    // Bảng màu chuẩn giao diện sáng CarZone
    private static final Color C_BG = new Color(0xF4F7FB);
    private static final Color C_WHITE = Color.WHITE;
    private static final Color C_LINE = new Color(0xE3E9F2);
    private static final Color C_TITLE = new Color(0x14233D);
    private static final Color C_DESC = new Color(0x7A889C);
    private static final Color C_PRIMARY = new Color(0x2563EB);
    private static final Color C_DANGER = new Color(0xDC2626);
    private static final Color C_DANGER_BG = new Color(0xFEF2F2);
    private static final Color C_SUCCESS = new Color(0x16A34A);
    private static final Color C_SUCCESS_BG = new Color(0xECFDF5);
    private static final Color C_WARN = new Color(0xD97706);
    private static final Color C_WARN_BG = new Color(0xFFFBEB);

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    private final DefaultTableModel modelHistory;
    private final JTable tblHistory;
    private final JComboBox<BackupType> cboType;
    private final JTextField txtGhiChu;
    private final JButton btnSaoLuu;
    private final JButton btnLamMoi;
    private final JButton btnPhucHoi;

    // Các thành phần trạng thái tự động Quartz
    private final JPanel pnlStatusQuartzBox;
    private final JLabel lblStatusQuartz;
    private final JLabel lblStatusDot;
    private final JLabel lblLastFull;
    private final JLabel lblLastDiff;
    private final JLabel lblLastLog;

    private List<BanSaoLuuItem> danhSachHienTai = new ArrayList<>();

    public SaoLuuPhucHoiUI() {
        setLayout(new BorderLayout());
        setBackground(C_BG);

        // Khung cuộn toàn trang (chống tràn ngang tuyệt đối)
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(C_BG);
        mainContent.setBorder(new EmptyBorder(20, 24, 24, 24));

        // ─────────────────────────────────────────────────────────────────────
        // 1. TIÊU ĐỀ TRANG + CHIP NGÀY (Chuẩn mẫu MainFrame Dashboard)
        // ─────────────────────────────────────────────────────────────────────
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        pnlHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JPanel pnlTitleBox = new JPanel();
        pnlTitleBox.setLayout(new BoxLayout(pnlTitleBox, BoxLayout.Y_AXIS));
        pnlTitleBox.setOpaque(false);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleRow.setOpaque(false);
        Image navImg = MainFrame.docIconSidebar("nav_sao_luu_phuc_hoi.png");
        if (navImg != null) {
            titleRow.add(new JLabel(new ImageIcon(navImg.getScaledInstance(28, 28, Image.SCALE_SMOOTH))));
        } else {
            titleRow.add(new JLabel(new MainFrame.DatabaseIcon(C_PRIMARY, 26)));
        }
        JLabel lblTitle = new JLabel("Sao lưu & Phục hồi cơ sở dữ liệu");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblTitle.setForeground(C_TITLE);
        titleRow.add(lblTitle);

        pnlTitleBox.add(titleRow);
        pnlHeader.add(pnlTitleBox, BorderLayout.WEST);

        // Chip ngày chuẩn mẫu MainFrame Dashboard (CalendarIcon + dd/MM/yyyy)
        RoundedPanel chipNgay = new RoundedPanel(C_WHITE, 10, C_LINE, new Insets(8, 14, 8, 14));
        chipNgay.setLayout(new BorderLayout(8, 0));
        chipNgay.add(new JLabel(new CalendarIcon(C_DESC, 16)), BorderLayout.WEST);
        JLabel lblNgayHienTai = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lblNgayHienTai.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblNgayHienTai.setForeground(C_TITLE);
        chipNgay.add(lblNgayHienTai, BorderLayout.CENTER);
        pnlHeader.add(chipNgay, BorderLayout.EAST);

        mainContent.add(pnlHeader);
        mainContent.add(Box.createVerticalStrut(18));

        // ─────────────────────────────────────────────────────────────────────
        // 2. KHỐI TRÊN: 2 CARD HÀNG NGANG (Sao lưu thủ công + Giám sát tự động)
        // ─────────────────────────────────────────────────────────────────────
        JPanel pnlTopCards = new JPanel(new GridLayout(1, 2, 16, 0));
        pnlTopCards.setOpaque(false);
        pnlTopCards.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlTopCards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 275));

        // CARD A: Sao lưu thủ công
        RoundedPanel cardManual = new RoundedPanel(C_WHITE, 12, C_LINE, new Insets(16, 18, 16, 18));
        cardManual.setLayout(new BoxLayout(cardManual, BoxLayout.Y_AXIS));

        JLabel lblCard1Title = new JLabel("Thực hiện sao lưu thủ công");
        lblCard1Title.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblCard1Title.setForeground(C_TITLE);
        lblCard1Title.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardManual.add(lblCard1Title);
        cardManual.add(Box.createVerticalStrut(4));

        JLabel lblCard1Desc = new JLabel("Tạo ngay một bản sao lưu dữ liệu tại thời điểm này theo nhu cầu.");
        lblCard1Desc.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblCard1Desc.setForeground(C_DESC);
        lblCard1Desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardManual.add(lblCard1Desc);
        cardManual.add(Box.createVerticalStrut(12));

        // Form nhập liệu linh hoạt (co giãn tự nhiên theo chiều rộng thẻ)
        JPanel formBox = new JPanel();
        formBox.setLayout(new BoxLayout(formBox, BoxLayout.Y_AXIS));
        formBox.setOpaque(false);
        formBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Hàng 1: Loại sao lưu
        JLabel lblTypePrompt = new JLabel("Loại sao lưu:");
        lblTypePrompt.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblTypePrompt.setForeground(C_TITLE);
        lblTypePrompt.setAlignmentX(Component.LEFT_ALIGNMENT);

        cboType = new JComboBox<>(new BackupType[] { BackupType.FULL, BackupType.DIFF, BackupType.LOG });
        cboType.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof BackupType bt) {
                    setText(bt.getTenHienThi() + " (." + bt.getDuoiTep() + ")");
                }
                return this;
            }
        });
        cboType.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        cboType.setPreferredSize(new Dimension(280, 36));
        cboType.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Hàng 2: Ghi chú
        JLabel lblNotePrompt = new JLabel("Ghi chú / Tên đợt sao lưu:");
        lblNotePrompt.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblNotePrompt.setForeground(C_TITLE);
        lblNotePrompt.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtGhiChu = new JTextField();
        txtGhiChu.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        txtGhiChu.setPreferredSize(new Dimension(280, 36));
        txtGhiChu.putClientProperty("JTextField.placeholderText", "Ví dụ: Chốt sổ cuối tuần, nâng cấp...");
        txtGhiChu.setAlignmentX(Component.LEFT_ALIGNMENT);

        formBox.add(lblTypePrompt);
        formBox.add(Box.createVerticalStrut(4));
        formBox.add(cboType);
        formBox.add(Box.createVerticalStrut(8));
        formBox.add(lblNotePrompt);
        formBox.add(Box.createVerticalStrut(4));
        formBox.add(txtGhiChu);
        cardManual.add(formBox);
        cardManual.add(Box.createVerticalStrut(12));

        // Hàng 3: Nút bấm bắt đầu sao lưu
        JPanel pnlBtnSaoLuu = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pnlBtnSaoLuu.setOpaque(false);
        pnlBtnSaoLuu.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlBtnSaoLuu.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        btnSaoLuu = new JButton("Bắt đầu sao lưu ngay", new BackupIcon(Color.WHITE, 15));
        btnSaoLuu.setIconTextGap(8);
        btnSaoLuu.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnSaoLuu.setForeground(Color.WHITE);
        btnSaoLuu.setBackground(C_PRIMARY);
        btnSaoLuu.setPreferredSize(new Dimension(195, 36));
        btnSaoLuu.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSaoLuu.addActionListener(e -> xuLySaoLuuThuCong());
        pnlBtnSaoLuu.add(btnSaoLuu);
        cardManual.add(pnlBtnSaoLuu);

        // CARD B: Giám sát lịch tự động (Quartz)
        RoundedPanel cardAuto = new RoundedPanel(C_WHITE, 12, C_LINE, new Insets(16, 18, 16, 18));
        cardAuto.setLayout(new BoxLayout(cardAuto, BoxLayout.Y_AXIS));

        JPanel pnlAutoHead = new JPanel(new BorderLayout());
        pnlAutoHead.setOpaque(false);
        pnlAutoHead.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlAutoHead.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel lblCard2Title = new JLabel("Lập lịch tự động (Quartz)");
        lblCard2Title.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblCard2Title.setForeground(C_TITLE);

        pnlStatusQuartzBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        pnlStatusQuartzBox.setOpaque(true);
        pnlStatusQuartzBox.setBackground(C_SUCCESS_BG);
        pnlStatusQuartzBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xA7, 0xF3, 0xD0), 1, true),
                new EmptyBorder(3, 8, 3, 8)));

        lblStatusDot = new JLabel(new DotIcon(C_SUCCESS, 6));
        lblStatusQuartz = new JLabel("Đang hoạt động");
        lblStatusQuartz.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblStatusQuartz.setForeground(C_SUCCESS);

        pnlStatusQuartzBox.add(lblStatusDot);
        pnlStatusQuartzBox.add(lblStatusQuartz);

        pnlAutoHead.add(lblCard2Title, BorderLayout.WEST);
        pnlAutoHead.add(pnlStatusQuartzBox, BorderLayout.EAST);
        cardAuto.add(pnlAutoHead);
        cardAuto.add(Box.createVerticalStrut(10));

        // 3 dòng hiển thị lịch trình full-width chống tràn và chống cắt chữ
        JPanel pnlScheduleRows = new JPanel();
        pnlScheduleRows.setLayout(new BoxLayout(pnlScheduleRows, BoxLayout.Y_AXIS));
        pnlScheduleRows.setOpaque(false);
        pnlScheduleRows.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblLastFull = new JLabel("<html><font color='#64748B'>Gần nhất:</font> Đang tải...</html>");
        lblLastFull.setFont(new Font("SansSerif", Font.PLAIN, 11));

        lblLastDiff = new JLabel("<html><font color='#64748B'>Gần nhất:</font> Đang tải...</html>");
        lblLastDiff.setFont(new Font("SansSerif", Font.PLAIN, 11));

        lblLastLog = new JLabel("<html><font color='#64748B'>Gần nhất:</font> Đang tải...</html>");
        lblLastLog.setFont(new Font("SansSerif", Font.PLAIN, 11));

        pnlScheduleRows.add(Box.createVerticalStrut(12));
        pnlScheduleRows.add(taoScheduleRow("FULL", new Color(0xEF, 0xF6, 0xFF), new Color(0x1D, 0x4E, 0xD8),
                "00:00 & 12:00 hàng ngày", lblLastFull));
        pnlScheduleRows.add(Box.createVerticalStrut(6));
        pnlScheduleRows.add(taoScheduleRow("DIFF", new Color(0xFF, 0xFB, 0xEB), new Color(0xD9, 0x77, 0x06),
                "04h, 08h, 16h, 20h hàng ngày", lblLastDiff));
        pnlScheduleRows.add(Box.createVerticalStrut(6));
        pnlScheduleRows.add(taoScheduleRow("LOG", new Color(0xF1, 0xF5, 0xF9), new Color(0x47, 0x55, 0x69),
                "Mỗi 15 phút một lần", lblLastLog));

        cardAuto.add(pnlScheduleRows);
        cardAuto.add(Box.createVerticalStrut(8));

        pnlTopCards.add(cardManual);
        pnlTopCards.add(cardAuto);
        mainContent.add(pnlTopCards);
        mainContent.add(Box.createVerticalStrut(18));

        // ─────────────────────────────────────────────────────────────────────
        // 3. KHỐI DƯỚI: DANH SÁCH LỊCH SỬ BẢN SAO LƯU & THAO TÁC PHỤC HỒI
        // ─────────────────────────────────────────────────────────────────────
        RoundedPanel cardTable = new RoundedPanel(C_WHITE, 12, C_LINE, new Insets(16, 18, 18, 18));
        cardTable.setLayout(new BoxLayout(cardTable, BoxLayout.Y_AXIS));
        cardTable.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardTable.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel pnlTableToolbar = new JPanel(new BorderLayout());
        pnlTableToolbar.setOpaque(false);
        pnlTableToolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlTableToolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel lblTableTitle = new JLabel("Lịch sử bản sao lưu CSDL trên máy chủ");
        lblTableTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblTableTitle.setForeground(C_TITLE);

        JPanel pnlActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlActions.setOpaque(false);

        btnLamMoi = new JButton("Làm mới", new RefreshIcon(C_DESC, 14));
        btnLamMoi.setIconTextGap(6);
        btnLamMoi.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnLamMoi.setPreferredSize(new Dimension(105, 34));
        btnLamMoi.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLamMoi.addActionListener(e -> taiDanhSachLichSu());

        btnPhucHoi = new JButton("Phục hồi từ bản sao lưu...", new RestoreIcon(C_DANGER, 15));
        btnPhucHoi.setIconTextGap(6);
        btnPhucHoi.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnPhucHoi.setForeground(C_DANGER);
        btnPhucHoi.setBackground(C_DANGER_BG);
        btnPhucHoi.setPreferredSize(new Dimension(215, 34));
        btnPhucHoi.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPhucHoi.addActionListener(e -> xuLyPhucHoiDatabase());

        pnlActions.add(btnLamMoi);
        pnlActions.add(btnPhucHoi);

        pnlTableToolbar.add(lblTableTitle, BorderLayout.WEST);
        pnlTableToolbar.add(pnlActions, BorderLayout.EAST);
        cardTable.add(pnlTableToolbar);
        cardTable.add(Box.createVerticalStrut(12));

        // Bảng dữ liệu tinh gọn 6 cột (chống tràn ngang, tự co giãn theo card)
        String[] columns = { "STT", "Loại", "Tên tệp sao lưu", "Thời điểm tạo", "Dung lượng", "Người / Máy thực hiện" };
        modelHistory = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblHistory = new JTable(modelHistory) {
            @Override
            public String getToolTipText(java.awt.event.MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                if (row >= 0 && row < danhSachHienTai.size()) {
                    BanSaoLuuItem item = danhSachHienTai.get(row);
                    return "Đường dẫn máy chủ: " + (item.duongDanTep() != null ? item.duongDanTep() : "N/A");
                }
                return super.getToolTipText(e);
            }
        };

        tblHistory.setRowHeight(36);
        tblHistory.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tblHistory.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblHistory.setShowVerticalLines(false);
        tblHistory.setGridColor(new Color(0xF1F5F9));
        tblHistory.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTableHeader th = tblHistory.getTableHeader();
        th.setFont(new Font("SansSerif", Font.BOLD, 12));
        th.setBackground(new Color(0xF8FAFC));
        th.setForeground(C_TITLE);
        th.setPreferredSize(new Dimension(0, 38));

        // Phân bổ tỷ lệ cột tối ưu
        tblHistory.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblHistory.getColumnModel().getColumn(0).setMaxWidth(55);
        tblHistory.getColumnModel().getColumn(1).setPreferredWidth(85);
        tblHistory.getColumnModel().getColumn(1).setMaxWidth(110);
        tblHistory.getColumnModel().getColumn(2).setPreferredWidth(230);
        tblHistory.getColumnModel().getColumn(3).setPreferredWidth(140);
        tblHistory.getColumnModel().getColumn(4).setPreferredWidth(95);
        tblHistory.getColumnModel().getColumn(5).setPreferredWidth(150);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tblHistory.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tblHistory.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);

        // Renderer cho badge loại backup (Pill mềm mại)
        tblHistory.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
                String v = value != null ? value.toString() : "";
                if (v.contains("Full") || v.contains("Toàn phần")) {
                    lbl.setForeground(new Color(0x1D4ED8));
                    lbl.setBackground(new Color(0xEFF6FF));
                } else if (v.contains("Vi sai") || v.contains("Diff")) {
                    lbl.setForeground(C_WARN);
                    lbl.setBackground(C_WARN_BG);
                } else {
                    lbl.setForeground(new Color(0x475569));
                    lbl.setBackground(new Color(0xF1F5F9));
                }
                lbl.setOpaque(true);
                return lbl;
            }
        });

        JScrollPane scrollTable = new JScrollPane(tblHistory);
        scrollTable.setBorder(BorderFactory.createLineBorder(C_LINE));
        scrollTable.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollTable.setPreferredSize(new Dimension(0, 300));
        scrollTable.setMinimumSize(new Dimension(0, 220));
        scrollTable.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        cardTable.add(scrollTable);
        mainContent.add(cardTable);

        // Khung cuộn trang ngoài cùng: KHÔNG cuộn ngang
        JScrollPane pageScroll = new JScrollPane(mainContent);
        pageScroll.setBorder(null);
        pageScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pageScroll.getVerticalScrollBar().setUnitIncrement(18);
        add(pageScroll, BorderLayout.CENTER);

        // Tải dữ liệu ban đầu
        taiDanhSachLichSu();
    }

    /**
     * Tạo một dòng lịch trình tự động dạng full-width, 2 dòng:
     * Dòng 1: Badge + Lịch định kỳ
     * Dòng 2: Icon lịch CalendarIcon + Thời điểm chạy gần nhất
     */
    private JPanel taoScheduleRow(String badgeText, Color badgeBg, Color badgeFg, String scheduleText,
            JLabel lblLastRun) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(true);
        row.setBackground(new Color(0xF8, 0xFA, 0xFC));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2, 0xE8, 0xF0), 1, true),
                new EmptyBorder(5, 10, 5, 10)));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        JPanel line1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        line1.setOpaque(false);
        line1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblBadge = new JLabel(badgeText);
        lblBadge.setFont(new Font("SansSerif", Font.BOLD, 10));
        lblBadge.setForeground(badgeFg);
        lblBadge.setOpaque(true);
        lblBadge.setBackground(badgeBg);
        lblBadge.setBorder(new EmptyBorder(1, 6, 1, 6));

        JLabel lblSchedule = new JLabel(scheduleText);
        lblSchedule.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblSchedule.setForeground(C_TITLE);

        line1.add(lblBadge);
        line1.add(lblSchedule);

        JPanel line2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        line2.setOpaque(false);
        line2.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblClockIcon = new JLabel(new CalendarIcon(new Color(0x64, 0x74, 0x8B), 13));
        line2.add(lblClockIcon);
        line2.add(lblLastRun);

        row.add(line1);
        row.add(Box.createVerticalStrut(2));
        row.add(line2);

        return row;
    }

    /**
     * Tải danh sách lịch sử sao lưu bất đồng bộ qua SwingWorker.
     */
    private void taiDanhSachLichSu() {
        btnLamMoi.setEnabled(false);
        btnLamMoi.setText("Đang tải...");

        new SwingWorker<List<BanSaoLuuItem>, Void>() {
            @Override
            protected List<BanSaoLuuItem> doInBackground() {
                return BackupService.getInstance().layDanhSachBanSaoLuu();
            }

            @Override
            protected void done() {
                btnLamMoi.setEnabled(true);
                btnLamMoi.setText("Làm mới");
                try {
                    List<BanSaoLuuItem> list = get();
                    danhSachHienTai = list;
                    modelHistory.setRowCount(0);

                    BanSaoLuuItem lastFull = null, lastDiff = null, lastLog = null;

                    int stt = 1;
                    for (BanSaoLuuItem item : list) {
                        if (lastFull == null && item.type() == BackupType.FULL)
                            lastFull = item;
                        if (lastDiff == null && item.type() == BackupType.DIFF)
                            lastDiff = item;
                        if (lastLog == null && item.type() == BackupType.LOG)
                            lastLog = item;

                        modelHistory.addRow(new Object[] {
                                stt++,
                                item.getLoaiHienThi(),
                                item.tenTep() != null ? item.tenTep() : "—",
                                item.thoiDiemTao() != null ? DATE_FMT.format(item.thoiDiemTao()) : "—",
                                AuditLogger.formatDungLuong(item.dungLuongBytes()),
                                item.nguoiThucHien() != null ? item.nguoiThucHien() : "Tự động"
                        });
                    }

                    // Cập nhật trạng thái gần nhất
                    lblLastFull.setText("<html><font color='#64748B'>Gần nhất:</font> <font color='#1D4ED8'><b>"
                            + (lastFull != null ? DATE_FMT.format(lastFull.thoiDiemTao()) : "Chưa có")
                            + "</b></font></html>");
                    lblLastDiff.setText("<html><font color='#64748B'>Gần nhất:</font> <font color='#D97706'><b>"
                            + (lastDiff != null ? DATE_FMT.format(lastDiff.thoiDiemTao()) : "Chưa có")
                            + "</b></font></html>");
                    lblLastLog.setText("<html><font color='#64748B'>Gần nhất:</font> <font color='#475569'><b>"
                            + (lastLog != null ? DATE_FMT.format(lastLog.thoiDiemTao()) : "Chưa có")
                            + "</b></font></html>");

                    boolean schedulerRunning = BackupScheduler.getInstance().dangChay();
                    if (schedulerRunning) {
                        lblStatusDot.setIcon(new DotIcon(C_SUCCESS, 6));
                        lblStatusQuartz.setText("Đang hoạt động");
                        lblStatusQuartz.setForeground(C_SUCCESS);
                        pnlStatusQuartzBox.setBackground(C_SUCCESS_BG);
                        pnlStatusQuartzBox.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(0xA7, 0xF3, 0xD0), 1, true),
                                new EmptyBorder(3, 8, 3, 8)));
                    } else {
                        lblStatusDot.setIcon(new DotIcon(new Color(0x64, 0x74, 0x8B), 6));
                        lblStatusQuartz.setText("Đã tạm dừng");
                        lblStatusQuartz.setForeground(new Color(0x64, 0x74, 0x8B));
                        pnlStatusQuartzBox.setBackground(new Color(0xF1, 0xF5, 0xF9));
                        pnlStatusQuartzBox.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(0xCB, 0xD5, 0xE1), 1, true),
                                new EmptyBorder(3, 8, 3, 8)));
                    }

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(SaoLuuPhucHoiUI.this,
                            "Không thể tải danh sách bản sao lưu: " + ex.getMessage(),
                            "Lỗi truy vấn", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /**
     * Xử lý khi nhấn nút Thực hiện sao lưu thủ công.
     */
    private void xuLySaoLuuThuCong() {
        BackupType type = (BackupType) cboType.getSelectedItem();
        if (type == null)
            type = BackupType.FULL;
        String ghiChu = txtGhiChu.getText() != null ? txtGhiChu.getText().trim() : "";

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn thực hiện sao lưu " + type.getTenHienThi() + " ngay bây giờ không?",
                "Xác nhận sao lưu thủ công",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION)
            return;

        btnSaoLuu.setEnabled(false);
        btnSaoLuu.setText("Đang sao lưu...");

        BackupType finalType = type;
        new SwingWorker<BackupResult, Void>() {
            @Override
            protected BackupResult doInBackground() {
                return BackupService.getInstance().thucHienSaoLuuThuCong(finalType, ghiChu);
            }

            @Override
            protected void done() {
                btnSaoLuu.setEnabled(true);
                btnSaoLuu.setText("Bắt đầu sao lưu ngay");
                try {
                    BackupResult result = get();
                    if (result.thanhCong()) {
                        txtGhiChu.setText("");
                        JOptionPane.showMessageDialog(SaoLuuPhucHoiUI.this,
                                "<html><b>Sao lưu " + finalType.getTenHienThi() + " thành công!</b><br><br>"
                                        + "• Tệp: <code>" + result.fileName() + "</code><br>"
                                        + "• Dung lượng: <b>" + AuditLogger.formatDungLuong(result.sizeBytes())
                                        + "</b><br>"
                                        + "• Thời gian thực thi: <b>" + result.durationMs() + " ms</b></html>",
                                "Thông báo thành công", JOptionPane.INFORMATION_MESSAGE);
                        taiDanhSachLichSu();
                    } else {
                        JOptionPane.showMessageDialog(SaoLuuPhucHoiUI.this,
                                "Sao lưu thất bại: " + result.thongBaoLoi(),
                                "Lỗi sao lưu", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(SaoLuuPhucHoiUI.this,
                            "Lỗi không xác định: " + ex.getMessage(),
                            "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /**
     * Xử lý quy trình phục hồi cơ sở dữ liệu (RESTORE DATABASE / LOG) an toàn.
     * Căn lề chuẩn mực chống lệch (Left-alignment) và hiển thị chi tiết chuỗi phục hồi tự động.
     */
    private void xuLyPhucHoiDatabase() {
        int selectedRow = tblHistory.getSelectedRow();
        String duongDanMacDinh = "";
        BanSaoLuuItem selectedItem = null;

        if (selectedRow >= 0 && selectedRow < danhSachHienTai.size()) {
            selectedItem = danhSachHienTai.get(selectedRow);
            duongDanMacDinh = selectedItem.duongDanTep() != null ? selectedItem.duongDanTep() : "";
        }

        // Tạo Dialog xác nhận chuẩn giao diện CarZone (rộng rãi 600x520, chống lệch, chữ đều đặn)
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Xác nhận phục hồi cơ sở dữ liệu", true);
        dlg.setSize(600, 520);
        dlg.setResizable(false);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        pnl.setBackground(Color.WHITE);
        pnl.setBorder(new EmptyBorder(20, 24, 20, 24));

        // 1. Tiêu đề cảnh báo có icon tam giác đỏ
        JPanel pnlTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlTitle.setOpaque(false);
        pnlTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JLabel lblWarnIcon = new JLabel(new AlertTriangleIcon(new Color(0xDC, 0x26, 0x26), 20));
        JLabel lblWarnTitle = new JLabel("CẢNH BÁO QUAN TRỌNG VỀ PHỤC HỒI DỮ LIỆU");
        lblWarnTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblWarnTitle.setForeground(C_DANGER);
        pnlTitle.add(lblWarnIcon);
        pnlTitle.add(lblWarnTitle);
        pnl.add(pnlTitle);
        pnl.add(Box.createVerticalStrut(12));

        // 2. Khung ghi chú cảnh báo viền đỏ nhạt (dùng RoundedPanel căn lề chuẩn 100%)
        RoundedPanel cardWarn = new RoundedPanel(new Color(0xFE, 0xF2, 0xF2), 10, new Color(0xFE, 0xCA, 0xCA), new Insets(10, 16, 10, 16));
        cardWarn.setLayout(new BoxLayout(cardWarn, BoxLayout.Y_AXIS));
        cardWarn.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardWarn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));

        Color cWarnText = new Color(0x99, 0x1B, 0x1B);
        JLabel lineWarn1 = new JLabel("• Thao tác phục hồi (RESTORE) sẽ ngắt toàn bộ phiên làm việc khác.");
        lineWarn1.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lineWarn1.setForeground(cWarnText);
        lineWarn1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lineWarn2 = new JLabel("• Toàn bộ dữ liệu CSDL sẽ được đưa về chính xác thời điểm bản sao lưu này.");
        lineWarn2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lineWarn2.setForeground(cWarnText);
        lineWarn2.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lineWarn3 = new JLabel("• Các phiếu sửa chữa, hóa đơn phát sinh sau thời điểm này sẽ bị ghi đè.");
        lineWarn3.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lineWarn3.setForeground(cWarnText);
        lineWarn3.setAlignmentX(Component.LEFT_ALIGNMENT);

        cardWarn.add(lineWarn1);
        cardWarn.add(Box.createVerticalStrut(4));
        cardWarn.add(lineWarn2);
        cardWarn.add(Box.createVerticalStrut(4));
        cardWarn.add(lineWarn3);
        pnl.add(cardWarn);
        pnl.add(Box.createVerticalStrut(10));

        // 3. Thông tin chuỗi phục hồi thông minh nếu có bản được chọn
        if (selectedItem != null) {
            RoundedPanel pnlInfoBox = new RoundedPanel(new Color(0xEF, 0xF6, 0xFF), 10, new Color(0xBF, 0xDB, 0xFE), new Insets(10, 16, 10, 16));
            pnlInfoBox.setLayout(new BoxLayout(pnlInfoBox, BoxLayout.Y_AXIS));
            pnlInfoBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            pnlInfoBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

            String loaiText = selectedItem.getLoaiHienThi();
            String timeText = selectedItem.thoiDiemTao() != null ? DATE_FMT.format(selectedItem.thoiDiemTao()) : "";
            JLabel lblItemTitle = new JLabel("Bản sao lưu đích: " + loaiText + " — Mốc tạo: " + timeText);
            lblItemTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
            lblItemTitle.setForeground(new Color(0x1D, 0x4E, 0xD8));
            lblItemTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

            String chuoiDesc;
            if (selectedItem.type() == BackupType.LOG) {
                chuoiDesc = "Hệ thống sẽ tự động phục hồi chuỗi liên hoàn: FULL gốc -> chuỗi các bản LOG đến mốc này.";
            } else if (selectedItem.type() == BackupType.DIFF) {
                chuoiDesc = "Hệ thống sẽ tự động phục hồi bản FULL gốc kết hợp bản vi sai DIFF này.";
            } else {
                chuoiDesc = "Hệ thống sẽ phục hồi toàn bộ cơ sở dữ liệu về thời điểm bản Full Backup này.";
            }
            JLabel lblItemDesc = new JLabel(chuoiDesc);
            lblItemDesc.setFont(new Font("SansSerif", Font.PLAIN, 11));
            lblItemDesc.setForeground(new Color(0x1E, 0x40, 0xAF));
            lblItemDesc.setAlignmentX(Component.LEFT_ALIGNMENT);

            pnlInfoBox.add(lblItemTitle);
            pnlInfoBox.add(Box.createVerticalStrut(4));
            pnlInfoBox.add(lblItemDesc);
            pnl.add(pnlInfoBox);
            pnl.add(Box.createVerticalStrut(12));
        }

        // 4. Ô nhập đường dẫn tệp sao lưu
        JLabel lblPath = new JLabel("Đường dẫn tệp sao lưu trên máy chủ (.bak / .trn):");
        lblPath.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblPath.setForeground(C_TITLE);
        lblPath.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnl.add(lblPath);
        pnl.add(Box.createVerticalStrut(6));

        JTextField txtPath = new JTextField(duongDanMacDinh);
        txtPath.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txtPath.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xCB, 0xD5, 0xE1), 1, true),
                new EmptyBorder(0, 12, 0, 12)));
        txtPath.setPreferredSize(new Dimension(550, 38));
        txtPath.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        txtPath.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnl.add(txtPath);
        pnl.add(Box.createVerticalStrut(12));

        // 5. Ô nhập xác nhận bảo mật
        JLabel lblConfirmPrompt = new JLabel("<html>Để xác nhận, vui lòng nhập chính xác chữ: <font color='#DC2626'><b>PHUCHOI</b></font></html>");
        lblConfirmPrompt.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblConfirmPrompt.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnl.add(lblConfirmPrompt);
        pnl.add(Box.createVerticalStrut(6));

        JTextField txtConfirm = new JTextField();
        txtConfirm.setFont(new Font("SansSerif", Font.BOLD, 13));
        txtConfirm.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xCB, 0xD5, 0xE1), 1, true),
                new EmptyBorder(0, 12, 0, 12)));
        txtConfirm.setPreferredSize(new Dimension(550, 38));
        txtConfirm.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        txtConfirm.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnl.add(txtConfirm);
        pnl.add(Box.createVerticalStrut(18));

        // 6. Nút bấm thao tác (Căn phải mượt mà)
        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlBtns.setOpaque(false);
        pnlBtns.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlBtns.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JButton btnHuy = new JButton("Hủy bỏ");
        btnHuy.setPreferredSize(new Dimension(90, 36));
        btnHuy.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnHuy.addActionListener(e -> dlg.dispose());

        JButton btnDongY = new JButton("Xác nhận phục hồi", new RestoreIcon(Color.WHITE, 15));
        btnDongY.setIconTextGap(6);
        btnDongY.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnDongY.setForeground(Color.WHITE);
        btnDongY.setBackground(C_DANGER);
        btnDongY.setPreferredSize(new Dimension(175, 36));
        btnDongY.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnDongY.addActionListener(e -> {
            String path = txtPath.getText() != null ? txtPath.getText().trim() : "";
            String confirmText = txtConfirm.getText() != null ? txtConfirm.getText().trim() : "";

            if (path.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng chỉ định đường dẫn tệp sao lưu!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!"PHUCHOI".equalsIgnoreCase(confirmText)) {
                JOptionPane.showMessageDialog(dlg, "Mã xác nhận không đúng! Vui lòng gõ chữ PHUCHOI để tiếp tục.", "Mã xác nhận sai", JOptionPane.WARNING_MESSAGE);
                return;
            }

            dlg.dispose();
            tienHanhRestoreThucSu(path);
        });

        pnlBtns.add(btnHuy);
        pnlBtns.add(btnDongY);
        pnl.add(pnlBtns);

        dlg.setContentPane(pnl);
        dlg.setVisible(true);
    }

    /**
     * Thực thi lệnh Restore cơ sở dữ liệu trên luồng nền và hiển thị tiến trình.
     */
    private void tienHanhRestoreThucSu(String duongDanBak) {
        JDialog dlgLoading = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Đang phục hồi hệ thống",
                true);
        dlgLoading.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dlgLoading.setSize(440, 160);
        dlgLoading.setResizable(false);
        dlgLoading.setLocationRelativeTo(this);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel l1 = new JLabel("Đang phục hồi cơ sở dữ liệu...", new RefreshIcon(C_PRIMARY, 16), SwingConstants.CENTER);
        l1.setFont(new Font("SansSerif", Font.BOLD, 13));
        l1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JProgressBar pb = new JProgressBar();
        pb.setIndeterminate(true);
        pb.setPreferredSize(new Dimension(380, 14));
        pb.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel l2 = new JLabel("Hệ thống đang tự động điều phối chuỗi phục hồi, vui lòng không tắt ứng dụng.");
        l2.setFont(new Font("SansSerif", Font.ITALIC, 11));
        l2.setForeground(C_DESC);
        l2.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(l1);
        p.add(Box.createVerticalStrut(10));
        p.add(pb);
        p.add(Box.createVerticalStrut(8));
        p.add(l2);
        dlgLoading.setContentPane(p);

        new SwingWorker<RestoreResult, Void>() {
            @Override
            protected RestoreResult doInBackground() {
                return BackupService.getInstance().thucHienPhucHoi(duongDanBak);
            }

            @Override
            protected void done() {
                dlgLoading.dispose();
                try {
                    RestoreResult r = get();
                    if (r.success()) {
                        JOptionPane.showMessageDialog(SaoLuuPhucHoiUI.this,
                                "<html><b>Phục hồi cơ sở dữ liệu thành công!</b><br><br>"
                                        + "• Tệp nguồn: <code>" + r.filePath() + "</code><br>"
                                        + "• Thời gian hoàn tất: <b>" + r.durationMs() + " ms</b><br><br>"
                                        + "<font color='#16A34A'>Cơ sở dữ liệu đã sẵn sàng hoạt động trở lại.</font><br>"
                                        + "Khuyến nghị: Bạn có thể khởi động lại ứng dụng để làm mới toàn bộ bộ nhớ tạm.</html>",
                                "Phục hồi thành công", JOptionPane.INFORMATION_MESSAGE);
                        taiDanhSachLichSu();
                    } else {
                        JOptionPane.showMessageDialog(SaoLuuPhucHoiUI.this,
                                "Phục hồi thất bại: " + r.errorMessage(),
                                "Lỗi phục hồi CSDL", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(SaoLuuPhucHoiUI.this,
                            "Lỗi không xác định: " + ex.getMessage(),
                            "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();

        dlgLoading.setVisible(true);
    }
}
