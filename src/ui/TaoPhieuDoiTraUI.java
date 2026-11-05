package ui;

import controller.TaoPhieuDoiTraController;
import entity.*;
import enums.HuongXuLyDoiTra;
import session.UserSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static ui.MainFrame.*;

/**
 * Màn hình Quản lý phiếu đổi trả – bảo hành (UC012 - Tiếp nhận viên).
 * Thiết kế chuẩn Figma & Antigravity Design System:
 * - Màn hình chính: Danh sách các phiếu đổi trả đã lập, tìm kiếm đa năng, bộ lọc hướng xử lý, xem chi tiết và tạo mới.
 * - Modal tạo phiếu đổi trả: Tuân thủ chặt chẽ nghiệp vụ đổi trả trong 7 ngày, chọn dịch vụ/linh kiện từ hóa đơn gốc.
 * - Modal xem chi tiết phiếu đổi trả: Hiển thị đầy đủ thông tin khách hàng, xe, lý do, chi tiết hạng mục kèm badge trạng thái.
 */
public class TaoPhieuDoiTraUI extends JPanel {

    private final TaoPhieuDoiTraController controller = new TaoPhieuDoiTraController();
    private final DecimalFormat moneyFmt = new DecimalFormat("#,### đ");
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    // Dữ liệu bảng chính
    private List<PhieuDoiTra> dsPhieuDoiTra = new ArrayList<>();
    private DefaultTableModel modelPhieu;
    private JTable tblPhieu;

    // Components Toolbar
    private JTextField txtTimKiem;
    private JComboBox<String> cboLocHuongXuLy;
    private JButton btnTim;
    private JButton btnLamMoi;
    private JButton btnXemChiTiet;
    private JButton btnThemPhieu;
    private JLabel lblSoLuong;

    public TaoPhieuDoiTraUI() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);

        caiDatPhimTat();
        taiDanhSachPhieuDoiTra();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. HEADER (Breadcrumb + Tiêu đề)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel pnl = new JPanel(new BorderLayout(12, 0));
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(16, 24, 12, 24));

        JPanel titleBlock = new JPanel(new BorderLayout(0, 4));
        titleBlock.setOpaque(false);

        JLabel lblBreadcrumb = new JLabel("Dashboard  /  Quản lý phiếu đổi trả");
        lblBreadcrumb.setFont(f(Font.PLAIN, 12));
        lblBreadcrumb.setForeground(C_TEXT_SUBTLE);

        JLabel lblTitle = new JLabel("Quản lý phiếu đổi trả – bảo hành");
        lblTitle.setFont(f(Font.BOLD, 20));
        lblTitle.setForeground(C_TEXT_HEAD);

        JLabel lblSub = new JLabel("Theo dõi, tra cứu và lập phiếu bảo hành linh kiện, sửa chữa lại dịch vụ hoặc hoàn tiền trong hạn 7 ngày sau thanh toán.");
        lblSub.setFont(f(Font.PLAIN, 12));
        lblSub.setForeground(C_TEXT_SUBTLE);

        titleBlock.add(lblBreadcrumb, BorderLayout.NORTH);
        titleBlock.add(lblTitle, BorderLayout.CENTER);
        titleBlock.add(lblSub, BorderLayout.SOUTH);
        pnl.add(titleBlock, BorderLayout.WEST);

        return pnl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. KHỐI NỘI DUNG CHÍNH (Toolbar + Bảng danh sách)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildMainContent() {
        JPanel pnl = new JPanel(new BorderLayout(0, 12));
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(0, 24, 20, 24));

        pnl.add(buildToolbar(), BorderLayout.NORTH);
        pnl.add(buildTablePanel(), BorderLayout.CENTER);

        return pnl;
    }

    private JPanel buildToolbar() {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(C_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(10, 14, 10, 14)));

        // Trái: Ô tìm kiếm + Lọc hướng xử lý
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        txtTimKiem = new JTextField();
        txtTimKiem.setFont(f(Font.PLAIN, 13));
        txtTimKiem.setPreferredSize(new Dimension(320, 36));
        txtTimKiem.putClientProperty("JTextField.placeholderText", "Tìm mã phiếu ĐT, mã HĐ gốc, biển số, khách hàng...");
        txtTimKiem.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(4, 10, 4, 10)));
        txtTimKiem.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    thucHienLoc();
                }
            }
        });
        left.add(txtTimKiem);

        btnTim = new RoundedButton("Tìm kiếm", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnTim.setFont(f(Font.PLAIN, 12));
        btnTim.setForeground(C_TEXT_MAIN);
        btnTim.setPreferredSize(new Dimension(90, 36));
        btnTim.addActionListener(e -> thucHienLoc());
        left.add(btnTim);

        btnLamMoi = new RoundedButton("↺ Làm mới", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnLamMoi.setFont(f(Font.PLAIN, 12));
        btnLamMoi.setForeground(C_TEXT_MAIN);
        btnLamMoi.setPreferredSize(new Dimension(95, 36));
        btnLamMoi.addActionListener(e -> {
            txtTimKiem.setText("");
            taiDanhSachPhieuDoiTra();
        });
        left.add(btnLamMoi);

        card.add(left, BorderLayout.WEST);

        // Phải: Nút xem chi tiết + Nút tạo phiếu mới
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        btnXemChiTiet = new RoundedButton("📄 Xem chi tiết phiếu", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnXemChiTiet.setFont(f(Font.PLAIN, 12));
        btnXemChiTiet.setForeground(C_TEXT_MAIN);
        btnXemChiTiet.setPreferredSize(new Dimension(150, 36));
        btnXemChiTiet.setEnabled(false);
        btnXemChiTiet.addActionListener(e -> onXemChiTietPhieu());
        right.add(btnXemChiTiet);

        btnThemPhieu = new RoundedButton("+ Tạo phiếu đổi trả (F2)", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
        btnThemPhieu.setFont(f(Font.BOLD, 12));
        btnThemPhieu.setForeground(C_WHITE);
        btnThemPhieu.setPreferredSize(new Dimension(200, 36));
        btnThemPhieu.addActionListener(e -> moModalTaoPhieu());
        right.add(btnThemPhieu);

        card.add(right, BorderLayout.EAST);
        return card;
    }

    private JPanel buildTablePanel() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(C_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)));

        // Header bảng: Tiêu đề + đếm số lượng
        JPanel pnlHead = new JPanel(new BorderLayout());
        pnlHead.setOpaque(false);

        JLabel lblTitle = new JLabel("Danh sách phiếu đổi trả – bảo hành");
        lblTitle.setFont(f(Font.BOLD, 14));
        lblTitle.setForeground(C_TEXT_HEAD);
        pnlHead.add(lblTitle, BorderLayout.WEST);

        lblSoLuong = new JLabel("Hiển thị 0 phiếu đổi trả");
        lblSoLuong.setFont(f(Font.PLAIN, 12));
        lblSoLuong.setForeground(C_TEXT_SUBTLE);
        pnlHead.add(lblSoLuong, BorderLayout.EAST);

        card.add(pnlHead, BorderLayout.NORTH);

        // Bảng dữ liệu
        String[] cols = {
                "STT", "Mã phiếu ĐT", "Ngày lập", "Mã HĐ gốc", "Khách hàng",
                "Biển số xe", "Tiếp nhận viên", "Tiền hoàn trả", "Lý do đổi trả"
        };
        modelPhieu = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tblPhieu = new JTable(modelPhieu);
        tblPhieu.setFont(f(Font.PLAIN, 13));
        tblPhieu.setRowHeight(34);
        tblPhieu.setShowGrid(false);
        tblPhieu.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblPhieu.getTableHeader().setFont(f(Font.BOLD, 12));
        tblPhieu.getTableHeader().setBackground(new Color(0xF8FAFC));
        tblPhieu.getTableHeader().setForeground(C_TEXT_HEAD);
        tblPhieu.getTableHeader().setPreferredSize(new Dimension(0, 38));

        // Column widths
        tblPhieu.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblPhieu.getColumnModel().getColumn(0).setMaxWidth(60);
        tblPhieu.getColumnModel().getColumn(1).setPreferredWidth(140);
        tblPhieu.getColumnModel().getColumn(2).setPreferredWidth(130);
        tblPhieu.getColumnModel().getColumn(3).setPreferredWidth(130);
        tblPhieu.getColumnModel().getColumn(4).setPreferredWidth(160);
        tblPhieu.getColumnModel().getColumn(5).setPreferredWidth(100);
        tblPhieu.getColumnModel().getColumn(6).setPreferredWidth(130);
        tblPhieu.getColumnModel().getColumn(7).setPreferredWidth(125);
        tblPhieu.getColumnModel().getColumn(8).setPreferredWidth(230);

        // Renderers
        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        tblPhieu.getColumnModel().getColumn(0).setCellRenderer(centerRender);
        tblPhieu.getColumnModel().getColumn(1).setCellRenderer(centerRender);
        tblPhieu.getColumnModel().getColumn(2).setCellRenderer(centerRender);
        tblPhieu.getColumnModel().getColumn(3).setCellRenderer(centerRender);
        tblPhieu.getColumnModel().getColumn(5).setCellRenderer(centerRender);

        // Renderer cho tiền hoàn trả (căn phải, màu đỏ nổi bật)
        tblPhieu.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                c.setHorizontalAlignment(SwingConstants.RIGHT);
                c.setFont(f(Font.BOLD, 13));
                if (!isSelected) {
                    c.setForeground(C_DANGER);
                }
                return c;
            }
        });

        tblPhieu.getSelectionModel().addListSelectionListener(e -> {
            boolean coChon = tblPhieu.getSelectedRow() >= 0;
            btnXemChiTiet.setEnabled(coChon);
        });

        tblPhieu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tblPhieu.getSelectedRow() >= 0) {
                    onXemChiTietPhieu();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tblPhieu);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. TẢI DỮ LIỆU & BỘ LỌC
    // ─────────────────────────────────────────────────────────────────────────
    public void taiDanhSachPhieuDoiTra() {
        lblSoLuong.setText("Đang tải dữ liệu...");
        new SwingWorker<List<PhieuDoiTra>, Void>() {
            @Override
            protected List<PhieuDoiTra> doInBackground() {
                return controller.layTatCaPhieuDoiTra();
            }

            @Override
            protected void done() {
                try {
                    dsPhieuDoiTra = get();
                    capNhatBangPhieu(dsPhieuDoiTra);
                } catch (Exception ex) {
                    lblSoLuong.setText("Lỗi tải dữ liệu: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void thucHienLoc() {
        String tuKhoa = txtTimKiem.getText().trim();
        lblSoLuong.setText("Đang tìm kiếm...");
        new SwingWorker<List<PhieuDoiTra>, Void>() {
            @Override
            protected List<PhieuDoiTra> doInBackground() {
                return controller.timKiemTatCaPhieuDoiTra(tuKhoa);
            }

            @Override
            protected void done() {
                try {
                    dsPhieuDoiTra = get();
                    capNhatBangPhieu(dsPhieuDoiTra);
                } catch (Exception ex) {
                    lblSoLuong.setText("Lỗi tìm kiếm: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void capNhatBangPhieu(List<PhieuDoiTra> list) {
        modelPhieu.setRowCount(0);
        if (list == null || list.isEmpty()) {
            lblSoLuong.setText("Không tìm thấy phiếu đổi trả nào phù hợp.");
            btnXemChiTiet.setEnabled(false);
            return;
        }

        int stt = 1;
        for (PhieuDoiTra p : list) {
            String ngayLapStr = p.getNgayLap() != null ? dateFmt.format(p.getNgayLap()) : "--";
            String maHD = p.getHoaDon() != null ? p.getHoaDon().getMaHoaDon() : "--";
            String tenKh = "--";
            String bienSo = "--";
            if (p.getHoaDon() != null && p.getHoaDon().getPhieuSuaChua() != null) {
                Xe xe = p.getHoaDon().getPhieuSuaChua().getXe();
                if (xe != null) {
                    bienSo = xe.getBienSoXe();
                    if (xe.getChuXe() != null && xe.getChuXe().getHoTen() != null) {
                        tenKh = xe.getChuXe().getHoTen();
                    }
                }
            }

            String tenNV = p.getNhanVien() != null ? p.getNhanVien().getHoTen() : "--";
            String tienHoanStr = moneyFmt.format(p.getSoTienHoanTra());
            String lyDo = p.getLyDoDoiTra() != null ? p.getLyDoDoiTra() : "--";

            modelPhieu.addRow(new Object[]{
                    stt++,
                    p.getMaPhieuDT(),
                    ngayLapStr,
                    maHD,
                    tenKh,
                    bienSo,
                    tenNV,
                    tienHoanStr,
                    lyDo
            });
        }

        lblSoLuong.setText("Hiển thị " + list.size() + " phiếu đổi trả");
        btnXemChiTiet.setEnabled(false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. PHÍM TẮT & ĐIỀU HƯỚNG MODAL
    // ─────────────────────────────────────────────────────────────────────────
    private void caiDatPhimTat() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "taoPhieuF2");
        am.put("taoPhieuF2", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                moModalTaoPhieu();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "lamMoiF5");
        am.put("lamMoiF5", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                txtTimKiem.setText("");
                taiDanhSachPhieuDoiTra();
            }
        });
    }

    private void moModalTaoPhieu() {
        Window parent = SwingUtilities.getWindowAncestor(this);
        TaoPhieuDoiTraModalDialog dialog = new TaoPhieuDoiTraModalDialog(parent);
        dialog.setVisible(true);
        if (dialog.isThanhCong()) {
            taiDanhSachPhieuDoiTra();
        }
    }

    private void onXemChiTietPhieu() {
        int row = tblPhieu.getSelectedRow();
        if (row < 0 || row >= dsPhieuDoiTra.size()) return;
        PhieuDoiTra pdt = dsPhieuDoiTra.get(row);

        Window parent = SwingUtilities.getWindowAncestor(this);
        ChiTietPhieuDoiTraModalDialog dialog = new ChiTietPhieuDoiTraModalDialog(parent, pdt);
        dialog.setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. MODAL 1: XEM CHI TIẾT PHIẾU ĐỔI TRẢ
    // ─────────────────────────────────────────────────────────────────────────
    public static class ChiTietPhieuDoiTraModalDialog extends JDialog {

        private final PhieuDoiTra phieuDT;
        private final TaoPhieuDoiTraController controller = new TaoPhieuDoiTraController();
        private final DecimalFormat moneyFmt = new DecimalFormat("#,### đ");
        private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        private DefaultTableModel modelChiTiet;
        private JTable tblChiTiet;
        private JLabel lblTongHoanTra;
        private JLabel lblBangChu;

        public ChiTietPhieuDoiTraModalDialog(Window owner, PhieuDoiTra pdt) {
            super(owner, "Chi tiết phiếu đổi trả – " + (pdt != null ? pdt.getMaPhieuDT() : ""), ModalityType.APPLICATION_MODAL);
            this.phieuDT = pdt;

            setLayout(new BorderLayout(0, 0));
            getContentPane().setBackground(C_WHITE);

            add(buildHeader(), BorderLayout.NORTH);
            add(buildBody(), BorderLayout.CENTER);
            add(buildFooter(), BorderLayout.SOUTH);

            // Mở toàn màn hình làm việc (trừ Taskbar)
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Rectangle bounds = ge.getMaximumWindowBounds();
            setBounds(bounds.x + 10, bounds.y + 10, bounds.width - 20, bounds.height - 20);
            setResizable(true);

            taiDuLieuChiTiet();
        }

        private JPanel buildHeader() {
            JPanel pnl = new JPanel(new BorderLayout());
            pnl.setBackground(C_WHITE);
            pnl.setBorder(new EmptyBorder(18, 24, 14, 24));

            JPanel left = new JPanel(new BorderLayout(0, 4));
            left.setOpaque(false);

            String maP = phieuDT != null ? phieuDT.getMaPhieuDT() : "--";
            JLabel lblTitle = new JLabel("Phiếu đổi trả: " + maP);
            lblTitle.setFont(f(Font.BOLD, 18));
            lblTitle.setForeground(C_PRIMARY);

            String ngay = phieuDT != null && phieuDT.getNgayLap() != null ? dateFmt.format(phieuDT.getNgayLap()) : "--";
            String nv = phieuDT != null && phieuDT.getNhanVien() != null ? phieuDT.getNhanVien().getHoTen() : "--";
            JLabel lblSub = new JLabel("Thời điểm lập: " + ngay + "   •   Tiếp nhận viên: " + nv);
            lblSub.setFont(f(Font.PLAIN, 12));
            lblSub.setForeground(C_TEXT_SUBTLE);

            left.add(lblTitle, BorderLayout.NORTH);
            left.add(lblSub, BorderLayout.SOUTH);
            pnl.add(left, BorderLayout.WEST);

            return pnl;
        }

        private JScrollPane buildBody() {
            JPanel body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setBackground(C_WHITE);
            body.setBorder(new EmptyBorder(0, 24, 16, 24));

            // Khối 1: Thông tin hóa đơn & Khách hàng
            body.add(buildCardThongTinChung());
            body.add(Box.createVerticalStrut(14));

            // Khối 2: Lý do đổi trả
            body.add(buildCardLyDo());
            body.add(Box.createVerticalStrut(14));

            // Khối 3: Bảng chi tiết các hạng mục đổi trả
            body.add(buildCardBangChiTiet());

            JScrollPane scroll = new JScrollPane(body);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(C_WHITE);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            return scroll;
        }

        private JPanel buildCardThongTinChung() {
            JPanel card = taoCardTrang("Thông tin giao dịch & khách hàng liên quan");
            JPanel content = new JPanel(new GridLayout(2, 3, 16, 10));
            content.setOpaque(false);

            String maHD = phieuDT != null && phieuDT.getHoaDon() != null ? phieuDT.getHoaDon().getMaHoaDon() : "--";
            String tenKh = "--";
            String sdt = "--";
            String bienSo = "--";
            String maPhieuSC = "--";

            if (phieuDT != null && phieuDT.getHoaDon() != null && phieuDT.getHoaDon().getPhieuSuaChua() != null) {
                PhieuTiepNhanSuaChua pt = phieuDT.getHoaDon().getPhieuSuaChua();
                maPhieuSC = pt.getMaPhieu();
                if (pt.getXe() != null) {
                    bienSo = pt.getXe().getBienSoXe();
                    if (pt.getXe().getChuXe() != null) {
                        tenKh = pt.getXe().getChuXe().getHoTen();
                        if (pt.getXe().getChuXe().getSoDienThoai() != null) {
                            sdt = pt.getXe().getChuXe().getSoDienThoai();
                        }
                    }
                }
            }

            content.add(taoFieldInfo("Mã hóa đơn gốc", maHD));
            content.add(taoFieldInfo("Khách hàng", tenKh));
            content.add(taoFieldInfo("Biển số xe", bienSo));
            content.add(taoFieldInfo("Mã phiếu sửa chữa", maPhieuSC));
            content.add(taoFieldInfo("Số điện thoại liên hệ", sdt));
            content.add(taoFieldInfo("Trạng thái", "Đã ghi nhận đổi trả"));

            card.add(content, BorderLayout.CENTER);
            return card;
        }

        private JPanel buildCardLyDo() {
            JPanel card = taoCardTrang("Lý do đề nghị đổi trả");
            JPanel content = new JPanel(new BorderLayout());
            content.setOpaque(false);

            JTextArea txtLyDo = new JTextArea(phieuDT != null ? phieuDT.getLyDoDoiTra() : "--");
            txtLyDo.setEditable(false);
            txtLyDo.setFont(f(Font.PLAIN, 13));
            txtLyDo.setForeground(C_TEXT_MAIN);
            txtLyDo.setBackground(new Color(0xF8FAFC));
            txtLyDo.setLineWrap(true);
            txtLyDo.setWrapStyleWord(true);
            txtLyDo.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER, 1),
                    new EmptyBorder(8, 10, 8, 10)));

            content.add(txtLyDo, BorderLayout.CENTER);
            card.add(content, BorderLayout.CENTER);
            return card;
        }

        private JPanel buildCardBangChiTiet() {
            JPanel card = taoCardTrang("Danh sách hạng mục đổi trả");
            JPanel content = new JPanel(new BorderLayout(0, 8));
            content.setOpaque(false);

            String[] cols = {
                    "STT", "Phân loại", "Mã", "Tên hạng mục", "ĐVT", "Số lượng", "Đơn giá", "Hướng xử lý", "Thành tiền hoàn"
            };
            modelChiTiet = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };

            tblChiTiet = new JTable(modelChiTiet);
            tblChiTiet.setFont(f(Font.PLAIN, 13));
            tblChiTiet.setRowHeight(32);
            tblChiTiet.setShowGrid(false);
            tblChiTiet.getTableHeader().setFont(f(Font.BOLD, 12));
            tblChiTiet.getTableHeader().setBackground(new Color(0xF8FAFC));
            tblChiTiet.getTableHeader().setPreferredSize(new Dimension(0, 36));

            tblChiTiet.getColumnModel().getColumn(0).setPreferredWidth(45);
            tblChiTiet.getColumnModel().getColumn(1).setPreferredWidth(90);
            tblChiTiet.getColumnModel().getColumn(2).setPreferredWidth(90);
            tblChiTiet.getColumnModel().getColumn(3).setPreferredWidth(210);
            tblChiTiet.getColumnModel().getColumn(4).setPreferredWidth(60);
            tblChiTiet.getColumnModel().getColumn(5).setPreferredWidth(70);
            tblChiTiet.getColumnModel().getColumn(6).setPreferredWidth(100);
            tblChiTiet.getColumnModel().getColumn(7).setPreferredWidth(130);
            tblChiTiet.getColumnModel().getColumn(8).setPreferredWidth(110);

            DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
            centerRender.setHorizontalAlignment(SwingConstants.CENTER);
            tblChiTiet.getColumnModel().getColumn(0).setCellRenderer(centerRender);
            tblChiTiet.getColumnModel().getColumn(1).setCellRenderer(centerRender);
            tblChiTiet.getColumnModel().getColumn(2).setCellRenderer(centerRender);
            tblChiTiet.getColumnModel().getColumn(4).setCellRenderer(centerRender);
            tblChiTiet.getColumnModel().getColumn(5).setCellRenderer(centerRender);

            DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
            rightRender.setHorizontalAlignment(SwingConstants.RIGHT);
            tblChiTiet.getColumnModel().getColumn(6).setCellRenderer(rightRender);
            tblChiTiet.getColumnModel().getColumn(8).setCellRenderer(rightRender);

            tblChiTiet.getColumnModel().getColumn(7).setCellRenderer(new HuongXuLyBadgeRenderer());

            JScrollPane scrollTable = new JScrollPane(tblChiTiet);
            scrollTable.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
            scrollTable.setPreferredSize(new Dimension(0, 180));
            content.add(scrollTable, BorderLayout.CENTER);

            // Footer tính tổng tiền hoàn
            JPanel pnlTong = new JPanel(new BorderLayout(10, 4));
            pnlTong.setOpaque(false);
            pnlTong.setBorder(new EmptyBorder(8, 0, 0, 0));

            JPanel rightTong = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            rightTong.setOpaque(false);
            JLabel lblTitleTong = new JLabel("Tổng số tiền hoàn trả cho khách:");
            lblTitleTong.setFont(f(Font.BOLD, 13));
            lblTitleTong.setForeground(C_TEXT_MAIN);

            lblTongHoanTra = new JLabel("0 đ");
            lblTongHoanTra.setFont(f(Font.BOLD, 18));
            lblTongHoanTra.setForeground(C_DANGER);
            rightTong.add(lblTitleTong);
            rightTong.add(lblTongHoanTra);

            lblBangChu = new JLabel("Bằng chữ: Không đồng.");
            lblBangChu.setFont(f(Font.ITALIC, 12));
            lblBangChu.setForeground(C_TEXT_SUBTLE);

            pnlTong.add(lblBangChu, BorderLayout.WEST);
            pnlTong.add(rightTong, BorderLayout.EAST);
            content.add(pnlTong, BorderLayout.SOUTH);

            card.add(content, BorderLayout.CENTER);
            return card;
        }

        private JPanel buildFooter() {
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 12));
            pnl.setBackground(C_WHITE);
            pnl.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));

            JButton btnDong = new RoundedButton("Đóng", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
            btnDong.setPreferredSize(new Dimension(100, 36));
            btnDong.setFont(f(Font.PLAIN, 13));
            btnDong.addActionListener(e -> dispose());
            pnl.add(btnDong);

            return pnl;
        }

        private void taiDuLieuChiTiet() {
            if (phieuDT == null || phieuDT.getMaPhieuDT() == null) return;
            new SwingWorker<List<ChiTietDoiTra>, Void>() {
                @Override
                protected List<ChiTietDoiTra> doInBackground() {
                    return controller.layChiTietPhieuDoiTra(phieuDT.getMaPhieuDT());
                }

                @Override
                protected void done() {
                    try {
                        List<ChiTietDoiTra> ds = get();
                        modelChiTiet.setRowCount(0);
                        double tongHoan = 0;
                        int stt = 1;
                        for (ChiTietDoiTra ct : ds) {
                            String phanLoai = ct.getMaDV() != null ? "Dịch vụ" : "Linh kiện";
                            String ma = ct.getMaDV() != null ? ct.getMaDV() : ct.getMaLK();
                            String ten = ct.getTenHangMuc() != null ? ct.getTenHangMuc() : ma;
                            String dvt = ct.getDonViTinh() != null ? ct.getDonViTinh() : "--";
                            double thanhTienHoan = ct.getHuongXuLy() == HuongXuLyDoiTra.HOAN_TIEN ? (ct.getSoLuong() * ct.getDonGia()) : 0.0;
                            tongHoan += thanhTienHoan;

                            modelChiTiet.addRow(new Object[]{
                                    stt++,
                                    phanLoai,
                                    ma,
                                    ten,
                                    dvt,
                                    ct.getSoLuong(),
                                    moneyFmt.format(ct.getDonGia()),
                                    ct.getHuongXuLy(),
                                    moneyFmt.format(thanhTienHoan)
                            });
                        }
                        lblTongHoanTra.setText(moneyFmt.format(tongHoan));
                        lblBangChu.setText("Bằng chữ: " + LapHoaDonThanhToanUI.docSoTienBangChu((long) tongHoan));
                    } catch (Exception ex) {
                        lblBangChu.setText("Lỗi tải chi tiết: " + ex.getMessage());
                    }
                }
            }.execute();
        }

        private JPanel taoCardTrang(String title) {
            JPanel card = new JPanel(new BorderLayout(0, 8));
            card.setBackground(C_WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER, 1),
                    new EmptyBorder(12, 16, 12, 16)));
            JLabel lbl = new JLabel(title);
            lbl.setFont(f(Font.BOLD, 13));
            lbl.setForeground(C_TEXT_HEAD);
            card.add(lbl, BorderLayout.NORTH);
            return card;
        }

        private JPanel taoFieldInfo(String label, String value) {
            JPanel p = new JPanel(new BorderLayout(0, 3));
            p.setOpaque(false);
            JLabel lblL = new JLabel(label);
            lblL.setFont(f(Font.PLAIN, 12));
            lblL.setForeground(C_TEXT_SUBTLE);

            JLabel lblV = new JLabel(value != null && !value.isBlank() ? value : "--");
            lblV.setFont(f(Font.BOLD, 13));
            lblV.setForeground(C_TEXT_HEAD);

            p.add(lblL, BorderLayout.NORTH);
            p.add(lblV, BorderLayout.CENTER);
            return p;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. MODAL 2: TẠO PHIẾU ĐỔI TRẢ MỚI (BÁM SÁT NGHIỆP VỤ & FIGMA)
    // ─────────────────────────────────────────────────────────────────────────
    public static class TaoPhieuDoiTraModalDialog extends JDialog {

        private final TaoPhieuDoiTraController controller = new TaoPhieuDoiTraController();
        private final DecimalFormat moneyFmt = new DecimalFormat("#,### đ");
        private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        private boolean thanhCong = false;

        private List<HoaDon> dsHoaDonDuDieuKien = new ArrayList<>();
        private HoaDon hoaDonDangChon = null;

        private List<ChiTietDichVu> dsDVHoaDon = new ArrayList<>();
        private List<ChiTietLinhKien> dsLKHoaDon = new ArrayList<>();
        private final List<HangMucItem> dsHangMucCombo = new ArrayList<>();

        // Danh sách các chi tiết đổi trả người dùng thêm vào
        private final List<ChiTietDoiTra> dsChiTietDoiTra = new ArrayList<>();

        // Components Khối 1: Chọn HĐ
        private JComboBox<String> cboHoaDon;
        private JTextField txtKhachHang;
        private JTextField txtBienSo;
        private JTextField txtNgayThanhToan;
        private JTextField txtTongTienGoc;
        private JTextField txtThoiHan;
        private JLabel lblDieuKienBadge;

        // Components Khối 2: Chọn hạng mục
        private JComboBox<String> cboHangMuc;
        private JSpinner spnSoLuongDT;
        private JComboBox<String> cboHuongXuLy;
        private DefaultTableModel modelBangDT;
        private JTable tblBangDT;
        private JLabel lblTongHoanTraTamTinh;

        // Components Khối 3: Lý do
        private JTextArea txtLyDo;

        // Actions & Status
        private JButton btnLuu;
        private JButton btnHuy;
        private JLabel lblStatus;

        public TaoPhieuDoiTraModalDialog(Window owner) {
            super(owner, "Lập phiếu đổi trả – bảo hành mới (UC012)", ModalityType.APPLICATION_MODAL);

            setLayout(new BorderLayout(0, 0));
            getContentPane().setBackground(C_WHITE);

            add(buildModalHeader(), BorderLayout.NORTH);
            add(buildModalBody(), BorderLayout.CENTER);
            add(buildModalFooter(), BorderLayout.SOUTH);

            // Mở toàn màn hình làm việc (trừ Taskbar)
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Rectangle bounds = ge.getMaximumWindowBounds();
            setBounds(bounds.x + 10, bounds.y + 10, bounds.width - 20, bounds.height - 20);
            setResizable(true);

            napDanhSachHoaDon();
        }

        public boolean isThanhCong() { return thanhCong; }

        private JPanel buildModalHeader() {
            JPanel pnl = new JPanel(new BorderLayout());
            pnl.setBackground(C_WHITE);
            pnl.setBorder(new EmptyBorder(16, 24, 12, 24));

            JPanel titleBlock = new JPanel(new BorderLayout(0, 3));
            titleBlock.setOpaque(false);

            JLabel lblTitle = new JLabel("Lập phiếu đổi trả linh kiện & dịch vụ");
            lblTitle.setFont(f(Font.BOLD, 18));
            lblTitle.setForeground(C_TEXT_HEAD);

            JLabel lblSub = new JLabel("Chỉ áp dụng cho hóa đơn ĐÃ THANH TOÁN và còn trong thời hạn 7 NGÀY kể từ ngày lập.");
            lblSub.setFont(f(Font.PLAIN, 12));
            lblSub.setForeground(C_TEXT_SUBTLE);

            titleBlock.add(lblTitle, BorderLayout.NORTH);
            titleBlock.add(lblSub, BorderLayout.SOUTH);
            pnl.add(titleBlock, BorderLayout.WEST);

            return pnl;
        }

        private JScrollPane buildModalBody() {
            JPanel body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setBackground(C_WHITE);
            body.setBorder(new EmptyBorder(0, 24, 16, 24));

            body.add(buildCardChonHoaDon());
            body.add(Box.createVerticalStrut(14));
            body.add(buildCardChonHangMuc());
            body.add(Box.createVerticalStrut(14));
            body.add(buildCardLyDo());

            JScrollPane scroll = new JScrollPane(body);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(C_WHITE);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            return scroll;
        }

        private JPanel buildCardChonHoaDon() {
            JPanel card = taoCardTrang("1. Chọn hóa đơn gốc (Đã thanh toán, trong hạn 7 ngày) *");
            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setOpaque(false);

            JPanel rowSelect = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
            rowSelect.setOpaque(false);
            rowSelect.add(new JLabel("Hóa đơn gốc *:"));

            cboHoaDon = new JComboBox<>();
            cboHoaDon.setPreferredSize(new Dimension(420, 34));
            cboHoaDon.setFont(f(Font.PLAIN, 13));
            cboHoaDon.addActionListener(e -> onChonHoaDon());
            rowSelect.add(cboHoaDon);

            JButton btnReload = new RoundedButton("↺ Tải lại", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
            btnReload.setPreferredSize(new Dimension(90, 34));
            btnReload.addActionListener(e -> napDanhSachHoaDon());
            rowSelect.add(btnReload);

            lblDieuKienBadge = new JLabel(" ");
            lblDieuKienBadge.setFont(f(Font.BOLD, 12));
            rowSelect.add(lblDieuKienBadge);

            content.add(rowSelect);
            content.add(Box.createVerticalStrut(8));

            JPanel rowInfo = new JPanel(new GridLayout(1, 5, 10, 0));
            rowInfo.setOpaque(false);

            txtKhachHang = taoFieldReadOnly("--");
            rowInfo.add(taoFieldWrapper("Khách hàng", txtKhachHang));

            txtBienSo = taoFieldReadOnly("--");
            rowInfo.add(taoFieldWrapper("Biển số xe", txtBienSo));

            txtNgayThanhToan = taoFieldReadOnly("--");
            rowInfo.add(taoFieldWrapper("Thời điểm thanh toán", txtNgayThanhToan));

            txtTongTienGoc = taoFieldReadOnly("--");
            rowInfo.add(taoFieldWrapper("Tổng tiền hóa đơn", txtTongTienGoc));

            txtThoiHan = taoFieldReadOnly("--");
            rowInfo.add(taoFieldWrapper("Hạn đổi trả còn lại", txtThoiHan));

            content.add(rowInfo);

            card.add(content, BorderLayout.CENTER);
            return card;
        }

        private JPanel buildCardChonHangMuc() {
            JPanel card = taoCardTrang("2. Chọn hạng mục đề nghị đổi trả *");
            JPanel content = new JPanel(new BorderLayout(0, 8));
            content.setOpaque(false);

            JPanel toolRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            toolRow.setOpaque(false);

            toolRow.add(new JLabel("Hạng mục:"));
            cboHangMuc = new JComboBox<>();
            cboHangMuc.setPreferredSize(new Dimension(320, 34));
            cboHangMuc.setFont(f(Font.PLAIN, 13));
            cboHangMuc.addActionListener(e -> onChonHangMuc());
            toolRow.add(cboHangMuc);

            toolRow.add(new JLabel("Số lượng:"));
            spnSoLuongDT = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
            spnSoLuongDT.setPreferredSize(new Dimension(60, 34));
            spnSoLuongDT.setFont(f(Font.PLAIN, 13));
            toolRow.add(spnSoLuongDT);

            toolRow.add(new JLabel("Hướng xử lý:"));
            cboHuongXuLy = new JComboBox<>(new String[]{"Hoàn tiền", "Đổi mới linh kiện"});
            cboHuongXuLy.setPreferredSize(new Dimension(150, 34));
            cboHuongXuLy.setFont(f(Font.PLAIN, 13));
            toolRow.add(cboHuongXuLy);

            JButton btnThem = new RoundedButton("+ Thêm hạng mục", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
            btnThem.setForeground(C_WHITE);
            btnThem.setFont(f(Font.BOLD, 12));
            btnThem.setPreferredSize(new Dimension(140, 34));
            btnThem.addActionListener(e -> themHangMucVaoDanhSach());
            toolRow.add(btnThem);

            JButton btnXoa = new RoundedButton("✕ Bỏ chọn", 8, C_WHITE, new Color(0xFEE2E2), C_BORDER);
            btnXoa.setForeground(C_DANGER);
            btnXoa.setFont(f(Font.PLAIN, 12));
            btnXoa.setPreferredSize(new Dimension(90, 34));
            btnXoa.addActionListener(e -> xoaHangMucKhoiDanhSach());
            toolRow.add(btnXoa);

            content.add(toolRow, BorderLayout.NORTH);

            // Bảng danh sách hạng mục đề nghị đổi trả
            String[] cols = {
                    "STT", "Phân loại", "Mã", "Tên hạng mục", "ĐVT", "Số lượng", "Đơn giá", "Hướng xử lý", "Thành tiền hoàn"
            };
            modelBangDT = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            tblBangDT = new JTable(modelBangDT);
            tblBangDT.setFont(f(Font.PLAIN, 13));
            tblBangDT.setRowHeight(30);
            tblBangDT.setShowGrid(false);
            tblBangDT.getTableHeader().setFont(f(Font.BOLD, 12));
            tblBangDT.getTableHeader().setBackground(new Color(0xF8FAFC));

            tblBangDT.getColumnModel().getColumn(0).setPreferredWidth(45);
            tblBangDT.getColumnModel().getColumn(1).setPreferredWidth(85);
            tblBangDT.getColumnModel().getColumn(2).setPreferredWidth(85);
            tblBangDT.getColumnModel().getColumn(3).setPreferredWidth(210);
            tblBangDT.getColumnModel().getColumn(4).setPreferredWidth(55);
            tblBangDT.getColumnModel().getColumn(5).setPreferredWidth(65);
            tblBangDT.getColumnModel().getColumn(6).setPreferredWidth(100);
            tblBangDT.getColumnModel().getColumn(7).setPreferredWidth(130);
            tblBangDT.getColumnModel().getColumn(8).setPreferredWidth(110);

            DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
            centerRender.setHorizontalAlignment(SwingConstants.CENTER);
            tblBangDT.getColumnModel().getColumn(0).setCellRenderer(centerRender);
            tblBangDT.getColumnModel().getColumn(1).setCellRenderer(centerRender);
            tblBangDT.getColumnModel().getColumn(2).setCellRenderer(centerRender);
            tblBangDT.getColumnModel().getColumn(4).setCellRenderer(centerRender);
            tblBangDT.getColumnModel().getColumn(5).setCellRenderer(centerRender);

            DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
            rightRender.setHorizontalAlignment(SwingConstants.RIGHT);
            tblBangDT.getColumnModel().getColumn(6).setCellRenderer(rightRender);
            tblBangDT.getColumnModel().getColumn(8).setCellRenderer(rightRender);

            tblBangDT.getColumnModel().getColumn(7).setCellRenderer(new HuongXuLyBadgeRenderer());

            JScrollPane scrollTable = new JScrollPane(tblBangDT);
            scrollTable.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
            scrollTable.setPreferredSize(new Dimension(0, 160));
            content.add(scrollTable, BorderLayout.CENTER);

            // Footer ước tính hoàn tiền
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 4));
            footer.setOpaque(false);
            JLabel lblUocTinh = new JLabel("Ước tính số tiền hoàn trả lại cho khách:");
            lblUocTinh.setFont(f(Font.BOLD, 13));
            lblUocTinh.setForeground(C_TEXT_MAIN);
            footer.add(lblUocTinh);

            lblTongHoanTraTamTinh = new JLabel("0 đ");
            lblTongHoanTraTamTinh.setFont(f(Font.BOLD, 17));
            lblTongHoanTraTamTinh.setForeground(C_DANGER);
            footer.add(lblTongHoanTraTamTinh);
            content.add(footer, BorderLayout.SOUTH);

            card.add(content, BorderLayout.CENTER);
            return card;
        }

        private JPanel buildCardLyDo() {
            JPanel card = taoCardTrang("3. Nội dung & Lý do đề nghị đổi trả *");
            JPanel content = new JPanel(new BorderLayout(0, 4));
            content.setOpaque(false);

            txtLyDo = new JTextArea(3, 20);
            txtLyDo.setFont(f(Font.PLAIN, 13));
            txtLyDo.setLineWrap(true);
            txtLyDo.setWrapStyleWord(true);
            txtLyDo.putClientProperty("JTextArea.placeholderText", "Nhập chi tiết tình trạng linh kiện bị lỗi, dịch vụ chưa đạt yêu cầu, hoặc lý do khách hàng khiếu nại (tối đa 500 ký tự)...");
            JScrollPane scroll = new JScrollPane(txtLyDo);
            scroll.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
            content.add(scroll, BorderLayout.CENTER);

            card.add(content, BorderLayout.CENTER);
            return card;
        }

        private JPanel buildModalFooter() {
            JPanel bar = new JPanel(new BorderLayout(12, 0));
            bar.setBackground(C_WHITE);
            bar.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER),
                    new EmptyBorder(12, 24, 12, 24)));

            lblStatus = new JLabel("Vui lòng chọn hóa đơn gốc cần lập phiếu đổi trả.");
            lblStatus.setFont(f(Font.ITALIC, 13));
            lblStatus.setForeground(C_TEXT_SUBTLE);
            bar.add(lblStatus, BorderLayout.WEST);

            JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            btnGroup.setOpaque(false);

            btnHuy = new RoundedButton("✕ Hủy bỏ", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
            btnHuy.setFont(f(Font.PLAIN, 13));
            btnHuy.setForeground(C_TEXT_MAIN);
            btnHuy.setPreferredSize(new Dimension(100, 36));
            btnHuy.addActionListener(e -> dispose());
            btnGroup.add(btnHuy);

            btnLuu = new RoundedButton("💾 Xác nhận lập phiếu đổi trả", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
            btnLuu.setFont(f(Font.BOLD, 13));
            btnLuu.setForeground(C_WHITE);
            btnLuu.setPreferredSize(new Dimension(220, 36));
            btnLuu.addActionListener(e -> thucHienLuuPhieuDoiTra());
            btnGroup.add(btnLuu);

            bar.add(btnGroup, BorderLayout.EAST);
            return bar;
        }

        // ─────────────────────────────────────────────────────────────────────
        // LOGIC BINDING & EVENTS
        // ─────────────────────────────────────────────────────────────────────
        private void napDanhSachHoaDon() {
            lblStatus.setText("Đang tải danh sách hóa đơn đủ điều kiện đổi trả...");
            new SwingWorker<List<HoaDon>, Void>() {
                @Override
                protected List<HoaDon> doInBackground() {
                    return controller.layHoaDonDuDieuKienDoiTra();
                }

                @Override
                protected void done() {
                    try {
                        dsHoaDonDuDieuKien = get();
                        cboHoaDon.removeAllItems();
                        cboHoaDon.addItem("-- Chọn hóa đơn đủ điều kiện đổi trả (trong 7 ngày) --");
                        for (HoaDon hd : dsHoaDonDuDieuKien) {
                            String bienSo = "";
                            String tenKh = "";
                            if (hd.getPhieuSuaChua() != null && hd.getPhieuSuaChua().getXe() != null) {
                                bienSo = hd.getPhieuSuaChua().getXe().getBienSoXe();
                                if (hd.getPhieuSuaChua().getXe().getChuXe() != null) {
                                    tenKh = hd.getPhieuSuaChua().getXe().getChuXe().getHoTen();
                                }
                            }
                            cboHoaDon.addItem(hd.getMaHoaDon() + "  •  " + bienSo + " - " + tenKh + "  •  " + moneyFmt.format(hd.getTongThanhToan()));
                        }
                        lblStatus.setText("Đã nạp " + dsHoaDonDuDieuKien.size() + " hóa đơn đủ điều kiện đổi trả.");
                    } catch (Exception ex) {
                        lblStatus.setText("Lỗi nạp hóa đơn: " + ex.getMessage());
                    }
                }
            }.execute();
        }

        private void onChonHoaDon() {
            int idx = cboHoaDon.getSelectedIndex();
            if (idx <= 0 || idx - 1 >= dsHoaDonDuDieuKien.size()) {
                hoaDonDangChon = null;
                resetFormChiTiet();
                return;
            }

            hoaDonDangChon = dsHoaDonDuDieuKien.get(idx - 1);
            PhieuTiepNhanSuaChua phieu = hoaDonDangChon.getPhieuSuaChua();

            if (phieu != null && phieu.getXe() != null) {
                Xe xe = phieu.getXe();
                txtBienSo.setText(xe.getBienSoXe());
                if (xe.getChuXe() != null) {
                    txtKhachHang.setText(xe.getChuXe().getHoTen());
                }
            }
            Date ngayTT = hoaDonDangChon.getThoiGianThanhToan() != null ? hoaDonDangChon.getThoiGianThanhToan() : hoaDonDangChon.getNgayLap();
            if (ngayTT != null) {
                txtNgayThanhToan.setText(dateFmt.format(ngayTT));

                // Tính thời hạn còn lại (7 ngày)
                Calendar cal = Calendar.getInstance();
                cal.setTime(ngayTT);
                cal.add(Calendar.DAY_OF_YEAR, 7);
                long diffMs = cal.getTime().getTime() - System.currentTimeMillis();
                long soNgayCon = diffMs / (24L * 60 * 60 * 1000);
                long soGioCon = (diffMs / (60L * 60 * 1000)) % 24;
                if (soNgayCon > 0) {
                    txtThoiHan.setText("Còn " + soNgayCon + " ngày " + soGioCon + " giờ");
                } else if (soGioCon >= 0) {
                    txtThoiHan.setText("Còn " + soGioCon + " giờ");
                } else {
                    txtThoiHan.setText("Hết hạn");
                }
            }

            txtTongTienGoc.setText(moneyFmt.format(hoaDonDangChon.getTongThanhToan()));
            lblDieuKienBadge.setText("✓ Đủ điều kiện đổi trả");
            lblDieuKienBadge.setForeground(C_SUCCESS);

            // Nạp hạng mục từ hóa đơn
            if (phieu != null) {
                lblStatus.setText("Đang nạp các hạng mục từ hóa đơn " + hoaDonDangChon.getMaHoaDon() + "...");
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        dsDVHoaDon = controller.layChiTietDichVu(phieu.getMaPhieu());
                        dsLKHoaDon = controller.layChiTietLinhKien(phieu.getMaPhieu());
                        return null;
                    }

                    @Override
                    protected void done() {
                        cboHangMuc.removeAllItems();
                        dsHangMucCombo.clear();
                        cboHangMuc.addItem("-- Chọn hạng mục từ hóa đơn --");

                        if (dsDVHoaDon != null) {
                            for (ChiTietDichVu ctdv : dsDVHoaDon) {
                                DichVu dv = ctdv.getDichVu();
                                if (dv != null) {
                                    String dvt = dv.getDonViTinh() != null ? dv.getDonViTinh() : "lần";
                                    HangMucItem item = new HangMucItem(true, dv.getMaDV(), dv.getTenDV(), dvt, ctdv.getSoLuong(), ctdv.getDonGiaThucHien());
                                    dsHangMucCombo.add(item);
                                    cboHangMuc.addItem(item.toString());
                                }
                            }
                        }
                        if (dsLKHoaDon != null) {
                            for (ChiTietLinhKien ctlk : dsLKHoaDon) {
                                LinhKien lk = ctlk.getLinhKien();
                                if (lk != null) {
                                    String dvt = lk.getDonViTinh() != null ? lk.getDonViTinh() : "cái";
                                    HangMucItem item = new HangMucItem(false, lk.getMaLK(), lk.getTenLK(), dvt, ctlk.getSoLuong(), ctlk.getDonGiaXuat());
                                    dsHangMucCombo.add(item);
                                    cboHangMuc.addItem(item.toString());
                                }
                            }
                        }
                        lblStatus.setText("Đã nạp xong danh sách dịch vụ và linh kiện của hóa đơn.");
                    }
                }.execute();
            }
        }

        private void onChonHangMuc() {
            int idx = cboHangMuc.getSelectedIndex();
            if (idx <= 0 || idx - 1 >= dsHangMucCombo.size()) {
                return;
            }
            HangMucItem item = dsHangMucCombo.get(idx - 1);

            // Cập nhật các lựa chọn hướng xử lý phù hợp với nghiệp vụ CarZone
            cboHuongXuLy.removeAllItems();
            if (item.isDichVu) {
                cboHuongXuLy.addItem("Hoàn tiền");
                cboHuongXuLy.addItem("Sửa lại miễn phí");
            } else {
                cboHuongXuLy.addItem("Hoàn tiền");
                cboHuongXuLy.addItem("Đổi mới linh kiện");
            }

            // Tính số lượng còn lại có thể đổi trả
            int daChon = 0;
            for (ChiTietDoiTra ct : dsChiTietDoiTra) {
                if (item.isDichVu && item.ma.equals(ct.getMaDV())) {
                    daChon += ct.getSoLuong();
                } else if (!item.isDichVu && item.ma.equals(ct.getMaLK())) {
                    daChon += ct.getSoLuong();
                }
            }
            int conLai = item.soLuongGoc - daChon;
            if (conLai > 0) {
                spnSoLuongDT.setModel(new SpinnerNumberModel(1, 1, conLai, 1));
            } else {
                spnSoLuongDT.setModel(new SpinnerNumberModel(0, 0, 0, 1));
            }
        }

        private void themHangMucVaoDanhSach() {
            if (hoaDonDangChon == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn hóa đơn gốc trước.", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int idx = cboHangMuc.getSelectedIndex();
            if (idx <= 0 || idx - 1 >= dsHangMucCombo.size()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một hạng mục cần đổi trả từ danh sách.", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            HangMucItem item = dsHangMucCombo.get(idx - 1);
            int sl = (int) spnSoLuongDT.getValue();
            if (sl <= 0) {
                JOptionPane.showMessageDialog(this, "Số lượng đổi trả phải lớn hơn 0.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Kiểm tra tổng số lượng đã thêm không vượt quá số lượng trong hóa đơn
            int daChon = 0;
            for (ChiTietDoiTra ct : dsChiTietDoiTra) {
                if (item.isDichVu && item.ma.equals(ct.getMaDV())) {
                    daChon += ct.getSoLuong();
                } else if (!item.isDichVu && item.ma.equals(ct.getMaLK())) {
                    daChon += ct.getSoLuong();
                }
            }
            if (daChon + sl > item.soLuongGoc) {
                JOptionPane.showMessageDialog(this,
                        "Tổng số lượng đổi trả (" + (daChon + sl) + ") vượt quá số lượng trên hóa đơn (" + item.soLuongGoc + ").",
                        "Lỗi nghiệp vụ", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Xác định Hướng xử lý
            String chonHX = (String) cboHuongXuLy.getSelectedItem();
            HuongXuLyDoiTra huongXuLy;
            if ("Đổi mới linh kiện".equals(chonHX)) {
                huongXuLy = HuongXuLyDoiTra.DOI_MOI;
            } else if ("Sửa lại miễn phí".equals(chonHX)) {
                huongXuLy = HuongXuLyDoiTra.SUA_LAI_MIEN_PHI;
            } else {
                huongXuLy = HuongXuLyDoiTra.HOAN_TIEN;
            }

            ChiTietDoiTra ctMoi = new ChiTietDoiTra();
            ctMoi.setSttRaw(dsChiTietDoiTra.size() + 1);
            ctMoi.setMaPhieuRaw(hoaDonDangChon.getPhieuSuaChua().getMaPhieu());
            if (item.isDichVu) {
                ctMoi.setMaDVRaw(item.ma);
                ctMoi.setMaLKRaw(null);
            } else {
                ctMoi.setMaLKRaw(item.ma);
                ctMoi.setMaDVRaw(null);
            }
            ctMoi.setTenHangMucRaw(item.ten);
            ctMoi.setDonViTinhRaw(item.donViTinh);
            ctMoi.setSoLuongRaw(sl);
            ctMoi.setDonGiaRaw(item.donGia);
            ctMoi.setHuongXuLyRaw(huongXuLy);

            dsChiTietDoiTra.add(ctMoi);
            capNhatBangDoiTra();
            onChonHangMuc(); // reset lại spinner số lượng
        }

        private void xoaHangMucKhoiDanhSach() {
            int row = tblBangDT.getSelectedRow();
            if (row < 0 || row >= dsChiTietDoiTra.size()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn dòng cần bỏ chọn trong bảng.", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            dsChiTietDoiTra.remove(row);
            for (int i = 0; i < dsChiTietDoiTra.size(); i++) {
                dsChiTietDoiTra.get(i).setSttRaw(i + 1);
            }
            capNhatBangDoiTra();
            onChonHangMuc();
        }

        private void capNhatBangDoiTra() {
            modelBangDT.setRowCount(0);
            double tongHoan = 0;
            for (int i = 0; i < dsChiTietDoiTra.size(); i++) {
                ChiTietDoiTra ct = dsChiTietDoiTra.get(i);
                double tt = ct.getHuongXuLy() == HuongXuLyDoiTra.HOAN_TIEN ? (ct.getDonGia() * ct.getSoLuong()) : 0.0;
                tongHoan += tt;

                String loai = ct.getMaDV() != null ? "Dịch vụ" : "Linh kiện";
                String ma = ct.getMaDV() != null ? ct.getMaDV() : ct.getMaLK();
                String ten = ct.getTenHangMuc() != null ? ct.getTenHangMuc() : ma;
                String dvt = ct.getDonViTinh() != null ? ct.getDonViTinh() : "--";

                modelBangDT.addRow(new Object[]{
                        i + 1,
                        loai,
                        ma,
                        ten,
                        dvt,
                        ct.getSoLuong(),
                        moneyFmt.format(ct.getDonGia()),
                        ct.getHuongXuLy(),
                        moneyFmt.format(tt)
                });
            }
            lblTongHoanTraTamTinh.setText(moneyFmt.format(tongHoan));
        }

        private void thucHienLuuPhieuDoiTra() {
            if (hoaDonDangChon == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn hóa đơn gốc cần đổi trả.", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (dsChiTietDoiTra.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng thêm ít nhất một hạng mục đổi trả.", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String lyDo = txtLyDo.getText().trim();
            if (lyDo.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập lý do đề nghị đổi trả.", "Thông báo", JOptionPane.WARNING_MESSAGE);
                txtLyDo.requestFocusInWindow();
                return;
            }
            if (lyDo.length() > 500) {
                JOptionPane.showMessageDialog(this, "Lý do đổi trả không được vượt quá 500 ký tự.", "Thông báo", JOptionPane.WARNING_MESSAGE);
                txtLyDo.requestFocusInWindow();
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc chắn muốn lập phiếu đổi trả cho hóa đơn " + hoaDonDangChon.getMaHoaDon() + " không?\n" +
                    "Số hạng mục: " + dsChiTietDoiTra.size() + "\n" +
                    "Tổng tiền hoàn trả ước tính: " + lblTongHoanTraTamTinh.getText(),
                    "Xác nhận lập phiếu đổi trả",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            btnLuu.setEnabled(false);
            btnHuy.setEnabled(false);
            lblStatus.setText("Đang ghi nhận phiếu đổi trả vào hệ thống...");

            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() {
                    return controller.lapPhieuDoiTra(hoaDonDangChon, lyDo, dsChiTietDoiTra);
                }

                @Override
                protected void done() {
                    btnLuu.setEnabled(true);
                    btnHuy.setEnabled(true);
                    try {
                        String maPhieuMoi = get();
                        thanhCong = true;
                        JOptionPane.showMessageDialog(TaoPhieuDoiTraModalDialog.this,
                                "Lập phiếu đổi trả thành công!\n" +
                                "Mã phiếu: " + maPhieuMoi + "\n" +
                                "Số tiền hoàn trả: " + lblTongHoanTraTamTinh.getText(),
                                "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } catch (Exception ex) {
                        String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                        lblStatus.setText("Lỗi: " + msg);
                        lblStatus.setForeground(C_DANGER);
                        JOptionPane.showMessageDialog(TaoPhieuDoiTraModalDialog.this, msg, "Lỗi lập phiếu đổi trả", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }

        private void resetFormChiTiet() {
            txtKhachHang.setText("--");
            txtBienSo.setText("--");
            txtNgayThanhToan.setText("--");
            txtTongTienGoc.setText("--");
            txtThoiHan.setText("--");
            lblDieuKienBadge.setText(" ");
            cboHangMuc.removeAllItems();
            dsHangMucCombo.clear();
            dsChiTietDoiTra.clear();
            capNhatBangDoiTra();
        }

        private JPanel taoCardTrang(String title) {
            JPanel card = new JPanel(new BorderLayout(0, 8));
            card.setBackground(C_WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER, 1),
                    new EmptyBorder(12, 16, 12, 16)));
            JLabel lbl = new JLabel(title);
            lbl.setFont(f(Font.BOLD, 13));
            lbl.setForeground(C_TEXT_HEAD);
            card.add(lbl, BorderLayout.NORTH);
            return card;
        }

        private JTextField taoFieldReadOnly(String text) {
            JTextField tf = new JTextField(text);
            tf.setEditable(false);
            tf.setFont(f(Font.PLAIN, 13));
            tf.setBackground(new Color(0xF8FAFC));
            tf.setForeground(C_TEXT_SUBTLE);
            return tf;
        }

        private JPanel taoFieldWrapper(String labelText, JTextField input) {
            JPanel pnl = new JPanel(new BorderLayout(0, 4));
            pnl.setOpaque(false);
            JLabel lbl = new JLabel(labelText);
            lbl.setFont(f(Font.PLAIN, 12));
            lbl.setForeground(C_TEXT_MAIN);
            pnl.add(lbl, BorderLayout.NORTH);
            input.setPreferredSize(new Dimension(0, 32));
            input.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER, 1),
                    new EmptyBorder(4, 8, 4, 8)));
            pnl.add(input, BorderLayout.CENTER);
            return pnl;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. HELPER CLASSES & BADGE RENDERERS
    // ─────────────────────────────────────────────────────────────────────────
    private static class HangMucItem {
        final boolean isDichVu;
        final String ma;
        final String ten;
        final String donViTinh;
        final int soLuongGoc;
        final double donGia;

        HangMucItem(boolean isDichVu, String ma, String ten, String donViTinh, int soLuongGoc, double donGia) {
            this.isDichVu = isDichVu;
            this.ma = ma;
            this.ten = ten;
            this.donViTinh = donViTinh;
            this.soLuongGoc = soLuongGoc;
            this.donGia = donGia;
        }

        @Override
        public String toString() {
            String loai = isDichVu ? "[Dịch vụ]" : "[Linh kiện]";
            return loai + " " + ma + " - " + ten + " (SL: " + soLuongGoc + " " + donViTinh + " - " + String.format("%,d đ", (long) donGia) + ")";
        }
    }

    public static class HuongXuLyBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 3));
            pnl.setOpaque(true);
            pnl.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

            if (value instanceof HuongXuLyDoiTra hx) {
                String text;
                Color bg;
                Color fg;
                Color border;

                switch (hx) {
                    case HOAN_TIEN -> {
                        text = "Hoàn tiền";
                        bg = C_DANGER_BG;
                        fg = C_DANGER;
                        border = new Color(0xFECACA);
                    }
                    case DOI_MOI -> {
                        text = "Đổi mới linh kiện";
                        bg = C_PRIMARY_BG;
                        fg = C_PRIMARY;
                        border = C_PRIMARY_BR;
                    }
                    case SUA_LAI_MIEN_PHI -> {
                        text = "Sửa lại miễn phí";
                        bg = C_SUCCESS_BG;
                        fg = C_SUCCESS;
                        border = new Color(0xA7F3D0);
                    }
                    default -> {
                        text = hx.name();
                        bg = new Color(0xF1F5F9);
                        fg = C_TEXT_MAIN;
                        border = C_BORDER;
                    }
                }

                JLabel badge = new JLabel(text);
                badge.setFont(f(Font.BOLD, 11));
                badge.setForeground(fg);
                badge.setBackground(bg);
                badge.setOpaque(true);
                badge.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(border, 1),
                        new EmptyBorder(2, 8, 2, 8)));
                pnl.add(badge);
            } else {
                JLabel lbl = new JLabel(value != null ? value.toString() : "");
                lbl.setFont(f(Font.PLAIN, 12));
                pnl.add(lbl);
            }
            return pnl;
        }
    }
}
