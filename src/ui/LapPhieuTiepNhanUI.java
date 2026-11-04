package ui;

import controller.LapPhieuTiepNhanController;
import entity.ChiTietDichVu;
import entity.DichVu;
import entity.KhachHang;
import entity.NhanVien;
import entity.Xe;
import session.UserSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static ui.MainFrame.*;

/**
 * Màn hình Lập phiếu tiếp nhận – sửa chữa (UC010 - Tiếp nhận viên).
 * Bám sát thiết kế Figma: Lap_pheu_tiep_nhan_sua_chua.jpg
 */
public class LapPhieuTiepNhanUI extends JPanel {

    private final LapPhieuTiepNhanController controller = new LapPhieuTiepNhanController();
    private final DecimalFormat moneyFmt = new DecimalFormat("#,### đ");
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    // Dữ liệu nguồn
    private List<KhachHang> dsKhachHang = new ArrayList<>();
    private List<Xe> dsXe = new ArrayList<>();
    private List<DichVu> dsDichVu = new ArrayList<>();
    private List<NhanVien> dsKtv = new ArrayList<>();

    // Danh sách dịch vụ được chọn vào phiếu
    private final List<ChiTietDichVu> dsChiTietChon = new ArrayList<>();

    // Components thông tin tiếp nhận
    private JComboBox<String> cboKhachHang;
    private JComboBox<String> cboXe;
    private JComboBox<String> cboKtv;
    private JTextField txtMaPhieu;
    private JTextField txtThoiGian;
    private JTextField txtNguoiTiepNhan;
    private JTextArea txtYeuCauBanDau;
    private JTextField txtGhiChuTiepNhan;

    // Components dịch vụ
    private JComboBox<String> cboDichVu;
    private JSpinner spnSoLuongDV;
    private JTextField txtGhiChuDV;
    private DefaultTableModel modelDichVu;
    private JTable tblDichVu;
    private JLabel lblTongTienDuKien;

    // Nút và status
    private JButton btnLuu;
    private JButton btnLamMoi;
    private JLabel lblStatus;

    public LapPhieuTiepNhanUI() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildScrollBody(), BorderLayout.CENTER);

        napDuLieuBanDau();
    }

    private JPanel buildHeader() {
        JPanel pnl = new JPanel(new BorderLayout(12, 0));
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(16, 24, 12, 24));

        JPanel titleBlock = new JPanel(new BorderLayout(0, 2));
        titleBlock.setOpaque(false);

        JLabel lblTitle = new JLabel("Lập phiếu tiếp nhận – sửa chữa");
        lblTitle.setFont(f(Font.BOLD, 20));
        lblTitle.setForeground(C_TEXT_HEAD);

        JLabel lblSub = new JLabel("Tiếp nhận xe ô tô, ghi nhận yêu cầu khách hàng và đề xuất các dịch vụ thực hiện");
        lblSub.setFont(f(Font.PLAIN, 12));
        lblSub.setForeground(C_TEXT_SUBTLE);

        titleBlock.add(lblTitle, BorderLayout.NORTH);
        titleBlock.add(lblSub, BorderLayout.SOUTH);
        pnl.add(titleBlock, BorderLayout.WEST);

        return pnl;
    }

    private JScrollPane buildScrollBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 24, 24, 24));

        body.add(buildCardThongTinTiepNhan());
        body.add(Box.createVerticalStrut(14));
        body.add(buildCardDichVuDeNghi());
        body.add(Box.createVerticalStrut(14));
        body.add(buildBottomActionBar());

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // KHỐI 1: THÔNG TIN TIẾP NHẬN
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildCardThongTinTiepNhan() {
        JPanel card = createWhiteCard("1. Thông tin tiếp nhận xe");
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Hàng 1: Khách hàng & Xe & Nút mở hồ sơ
        JPanel row1 = new JPanel(new GridLayout(1, 3, 12, 0));
        row1.setOpaque(false);
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

        cboKhachHang = new JComboBox<>();
        cboKhachHang.setFont(f(Font.PLAIN, 13));
        cboKhachHang.addActionListener(e -> onChonKhachHang());
        row1.add(createFieldWrapper("Khách hàng *", cboKhachHang));

        cboXe = new JComboBox<>();
        cboXe.setFont(f(Font.PLAIN, 13));
        row1.add(createFieldWrapper("Xe tiếp nhận (Biển số) *", cboXe));

        cboKtv = new JComboBox<>();
        cboKtv.setFont(f(Font.PLAIN, 13));
        row1.add(createFieldWrapper("Kỹ thuật viên chính phụ trách", cboKtv));

        content.add(row1);
        content.add(Box.createVerticalStrut(8));

        // Hàng 2: Mã phiếu, Thời gian, Người tiếp nhận
        JPanel row2 = new JPanel(new GridLayout(1, 3, 12, 0));
        row2.setOpaque(false);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

        txtMaPhieu = new JTextField("Chưa được cấp mã (Hệ thống tự sinh)");
        txtMaPhieu.setEditable(false);
        txtMaPhieu.setFont(f(Font.PLAIN, 13));
        txtMaPhieu.setBackground(new Color(0xF8FAFC));
        txtMaPhieu.setForeground(C_TEXT_SUBTLE);
        row2.add(createFieldWrapper("Mã phiếu", txtMaPhieu));

        txtThoiGian = new JTextField(timeFmt.format(new Date()));
        txtThoiGian.setEditable(false);
        txtThoiGian.setFont(f(Font.PLAIN, 13));
        txtThoiGian.setBackground(new Color(0xF8FAFC));
        row2.add(createFieldWrapper("Thời điểm tiếp nhận", txtThoiGian));

        String tenNv = UserSession.getInstance().daDangNhap() ?
                UserSession.getInstance().getNhanVien().getHoTen() : "Tiếp nhận viên";
        txtNguoiTiepNhan = new JTextField(tenNv);
        txtNguoiTiepNhan.setEditable(false);
        txtNguoiTiepNhan.setFont(f(Font.PLAIN, 13));
        txtNguoiTiepNhan.setBackground(new Color(0xF8FAFC));
        row2.add(createFieldWrapper("Người tiếp nhận", txtNguoiTiepNhan));

        content.add(row2);
        content.add(Box.createVerticalStrut(8));

        // Hàng 3: Yêu cầu của khách hàng
        JPanel pnlYeuCau = new JPanel(new BorderLayout(0, 4));
        pnlYeuCau.setOpaque(false);
        JLabel lblYc = new JLabel("Yêu cầu của khách hàng & tình trạng xe ban đầu *");
        lblYc.setFont(f(Font.PLAIN, 12));
        lblYc.setForeground(C_TEXT_MAIN);
        pnlYeuCau.add(lblYc, BorderLayout.NORTH);

        txtYeuCauBanDau = new JTextArea(3, 20);
        txtYeuCauBanDau.setFont(f(Font.PLAIN, 13));
        txtYeuCauBanDau.setLineWrap(true);
        txtYeuCauBanDau.setWrapStyleWord(true);
        JScrollPane scrollYc = new JScrollPane(txtYeuCauBanDau);
        scrollYc.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        pnlYeuCau.add(scrollYc, BorderLayout.CENTER);
        content.add(pnlYeuCau);
        content.add(Box.createVerticalStrut(8));

        // Hàng 4: Ghi chú tiếp nhận
        txtGhiChuTiepNhan = new JTextField();
        txtGhiChuTiepNhan.setFont(f(Font.PLAIN, 13));
        content.add(createFieldWrapper("Ghi chú tiếp nhận thêm", txtGhiChuTiepNhan));

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // KHỐI 2: DỊCH VỤ ĐỀ NGHỊ
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildCardDichVuDeNghi() {
        JPanel card = createWhiteCard("2. Dịch vụ đề nghị thực hiện");
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setOpaque(false);

        // Hàng công cụ thêm dịch vụ
        JPanel toolRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolRow.setOpaque(false);

        toolRow.add(new JLabel("Chọn dịch vụ:"));
        cboDichVu = new JComboBox<>();
        cboDichVu.setPreferredSize(new Dimension(280, 34));
        cboDichVu.setFont(f(Font.PLAIN, 13));
        toolRow.add(cboDichVu);

        toolRow.add(new JLabel("Số lượng:"));
        spnSoLuongDV = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        spnSoLuongDV.setPreferredSize(new Dimension(65, 34));
        spnSoLuongDV.setFont(f(Font.PLAIN, 13));
        toolRow.add(spnSoLuongDV);

        toolRow.add(new JLabel("Ghi chú:"));
        txtGhiChuDV = new JTextField();
        txtGhiChuDV.setPreferredSize(new Dimension(160, 34));
        txtGhiChuDV.setFont(f(Font.PLAIN, 13));
        toolRow.add(txtGhiChuDV);

        JButton btnThemDV = new RoundedButton("+ Thêm dịch vụ", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
        btnThemDV.setForeground(C_WHITE);
        btnThemDV.setFont(f(Font.BOLD, 12));
        btnThemDV.setPreferredSize(new Dimension(130, 34));
        btnThemDV.addActionListener(e -> themDichVuVaoDanhSach());
        toolRow.add(btnThemDV);

        JButton btnXoaDV = new RoundedButton("✕ Xóa dòng", 8, C_WHITE, new Color(0xFEE2E2), C_BORDER);
        btnXoaDV.setForeground(C_DANGER);
        btnXoaDV.setFont(f(Font.PLAIN, 12));
        btnXoaDV.setPreferredSize(new Dimension(100, 34));
        btnXoaDV.addActionListener(e -> xoaDichVuKhoiDanhSach());
        toolRow.add(btnXoaDV);

        content.add(toolRow, BorderLayout.NORTH);

        // Bảng dịch vụ đề nghị
        modelDichVu = new DefaultTableModel(new String[]{"STT", "Mã DV", "Tên dịch vụ", "Đơn vị tính", "Đơn giá", "Số lượng", "Thành tiền", "Ghi chú"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblDichVu = new JTable(modelDichVu);
        tblDichVu.setFont(f(Font.PLAIN, 13));
        tblDichVu.setRowHeight(30);
        tblDichVu.setShowGrid(false);
        tblDichVu.getTableHeader().setFont(f(Font.BOLD, 12));
        tblDichVu.getTableHeader().setBackground(new Color(0xF8FAFC));
        tblDichVu.setSelectionBackground(C_PRIMARY_BG);
        tblDichVu.setSelectionForeground(C_TEXT_HEAD);

        JScrollPane scrollTable = new JScrollPane(tblDichVu);
        scrollTable.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        scrollTable.setPreferredSize(new Dimension(0, 160));
        content.add(scrollTable, BorderLayout.CENTER);

        // Footer tổng tiền tạm tính
        JPanel footerTien = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 4));
        footerTien.setOpaque(false);
        JLabel lblTitleTien = new JLabel("Tổng tiền dịch vụ tạm tính:");
        lblTitleTien.setFont(f(Font.BOLD, 13));
        lblTitleTien.setForeground(C_TEXT_MAIN);
        lblTongTienDuKien = new JLabel("0 đ");
        lblTongTienDuKien.setFont(f(Font.BOLD, 15));
        lblTongTienDuKien.setForeground(C_PRIMARY);
        footerTien.add(lblTitleTien);
        footerTien.add(lblTongTienDuKien);
        content.add(footerTien, BorderLayout.SOUTH);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // KHỐI 3: THANH THAO TÁC ACTION BAR DƯỚI CÙNG
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildBottomActionBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);

        lblStatus = new JLabel("Vui lòng chọn khách hàng và xe tiếp nhận.");
        lblStatus.setFont(f(Font.ITALIC, 13));
        lblStatus.setForeground(C_TEXT_SUBTLE);
        bar.add(lblStatus, BorderLayout.WEST);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnGroup.setOpaque(false);

        btnLamMoi = new RoundedButton("↺ Làm mới", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnLamMoi.setFont(f(Font.PLAIN, 13));
        btnLamMoi.setForeground(C_TEXT_MAIN);
        btnLamMoi.setPreferredSize(new Dimension(110, 36));
        btnLamMoi.addActionListener(e -> resetForm());
        btnGroup.add(btnLamMoi);

        btnLuu = new RoundedButton("💾 Lưu phiếu tiếp nhận", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
        btnLuu.setFont(f(Font.BOLD, 13));
        btnLuu.setForeground(C_WHITE);
        btnLuu.setPreferredSize(new Dimension(190, 36));
        btnLuu.addActionListener(e -> thucHienLuuPhieu());
        btnGroup.add(btnLuu);

        bar.add(btnGroup, BorderLayout.EAST);
        return bar;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TẢI DỮ LIỆU & XỬ LÝ SỰ KIỆN
    // ─────────────────────────────────────────────────────────────────────────
    private void napDuLieuBanDau() {
        new SwingWorker<Void, Void>() {
            List<KhachHang> khs;
            List<DichVu> dvs;
            List<NhanVien> ktvs;

            @Override
            protected Void doInBackground() {
                khs = controller.layDanhSachKhachHang();
                dvs = controller.layDanhSachDichVuDangCungCap();
                ktvs = controller.layDanhSachKyThuatVien();
                return null;
            }

            @Override
            protected void done() {
                dsKhachHang = khs != null ? khs : List.of();
                dsDichVu = dvs != null ? dvs : List.of();
                dsKtv = ktvs != null ? ktvs : List.of();

                cboKhachHang.removeAllItems();
                cboKhachHang.addItem("-- Chọn khách hàng --");
                for (KhachHang kh : dsKhachHang) {
                    cboKhachHang.addItem(kh.getMaKH() + " - " + kh.getHoTen() + " (" + kh.getSoDienThoai() + ")");
                }

                cboKtv.removeAllItems();
                cboKtv.addItem("-- Chưa phân công KTV --");
                for (NhanVien ktv : dsKtv) {
                    cboKtv.addItem(ktv.getMaNV() + " - " + ktv.getHoTen());
                }

                cboDichVu.removeAllItems();
                cboDichVu.addItem("-- Chọn dịch vụ --");
                for (DichVu dv : dsDichVu) {
                    cboDichVu.addItem(dv.getMaDV() + " - " + dv.getTenDV() + " (" + moneyFmt.format(dv.getDonGia()) + ")");
                }
            }
        }.execute();
    }

    private void onChonKhachHang() {
        int idx = cboKhachHang.getSelectedIndex();
        if (idx <= 0 || idx - 1 >= dsKhachHang.size()) {
            cboXe.removeAllItems();
            cboXe.addItem("-- Vui lòng chọn khách hàng trước --");
            dsXe = List.of();
            return;
        }

        KhachHang kh = dsKhachHang.get(idx - 1);
        new SwingWorker<List<Xe>, Void>() {
            @Override
            protected List<Xe> doInBackground() {
                return controller.layDanhSachXeTheoKhachHang(kh.getMaKH());
            }

            @Override
            protected void done() {
                try {
                    dsXe = get();
                    cboXe.removeAllItems();
                    if (dsXe.isEmpty()) {
                        cboXe.addItem("-- Khách hàng chưa đăng ký xe --");
                    } else {
                        for (Xe xe : dsXe) {
                            String info = xe.getBienSoXe() + " (" + (xe.getHangXe() != null ? xe.getHangXe() : "") + " " + (xe.getModel() != null ? xe.getModel() : "") + ")";
                            cboXe.addItem(info);
                        }
                    }
                } catch (Exception ex) {
                    lblStatus.setText("Lỗi nạp danh sách xe: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void themDichVuVaoDanhSach() {
        int idx = cboDichVu.getSelectedIndex();
        if (idx <= 0 || idx - 1 >= dsDichVu.size()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một dịch vụ từ danh sách.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DichVu dv = dsDichVu.get(idx - 1);
        int sl = (int) spnSoLuongDV.getValue();
        String ghiChu = txtGhiChuDV.getText().trim();

        // Kiểm tra đã có trong danh sách chưa
        for (ChiTietDichVu ct : dsChiTietChon) {
            if (ct.getDichVu() != null && ct.getDichVu().getMaDV().equals(dv.getMaDV())) {
                JOptionPane.showMessageDialog(this, "Dịch vụ \"" + dv.getTenDV() + "\" đã có trong danh sách.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        }

        ChiTietDichVu ctMoi = new ChiTietDichVu();
        ctMoi.setSttRaw(dsChiTietChon.size() + 1);
        ctMoi.setDichVuRaw(dv);
        ctMoi.setSoLuongRaw(sl);
        ctMoi.setDonGiaThucHienRaw(dv.getDonGia());
        ctMoi.setGhiChuRaw(ghiChu.isEmpty() ? null : ghiChu);
        dsChiTietChon.add(ctMoi);

        capNhatBangDichVu();
        txtGhiChuDV.setText("");
        spnSoLuongDV.setValue(1);
    }

    private void xoaDichVuKhoiDanhSach() {
        int row = tblDichVu.getSelectedRow();
        if (row < 0 || row >= dsChiTietChon.size()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn dòng dịch vụ cần xóa khỏi danh sách.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        dsChiTietChon.remove(row);
        // Đánh lại STT
        for (int i = 0; i < dsChiTietChon.size(); i++) {
            dsChiTietChon.get(i).setSttRaw(i + 1);
        }
        capNhatBangDichVu();
    }

    private void capNhatBangDichVu() {
        modelDichVu.setRowCount(0);
        double tongTien = 0;
        for (int i = 0; i < dsChiTietChon.size(); i++) {
            ChiTietDichVu ct = dsChiTietChon.get(i);
            DichVu dv = ct.getDichVu();
            double thanhTien = ct.getDonGia() * ct.getSoLuong();
            tongTien += thanhTien;
            modelDichVu.addRow(new Object[]{
                    i + 1,
                    dv.getMaDV(),
                    dv.getTenDV(),
                    dv.getDonViTinh() != null ? dv.getDonViTinh() : "Lần",
                    moneyFmt.format(ct.getDonGia()),
                    ct.getSoLuong(),
                    moneyFmt.format(thanhTien),
                    ct.getGhiChu() != null ? ct.getGhiChu() : ""
            });
        }
        lblTongTienDuKien.setText(moneyFmt.format(tongTien));
    }

    private void thucHienLuuPhieu() {
        // Validate dữ liệu
        int idxKh = cboKhachHang.getSelectedIndex();
        if (idxKh <= 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn khách hàng.", "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
            cboKhachHang.requestFocusInWindow();
            return;
        }
        if (dsXe.isEmpty() || cboXe.getSelectedIndex() < 0) {
            JOptionPane.showMessageDialog(this, "Khách hàng này chưa có xe được chọn. Vui lòng thêm xe trước.", "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Xe xeChon = dsXe.get(cboXe.getSelectedIndex());
        String yeuCau = txtYeuCauBanDau.getText().trim();
        if (yeuCau.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập nội dung yêu cầu của khách hàng.", "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
            txtYeuCauBanDau.requestFocusInWindow();
            return;
        }

        String maKtv = null;
        int idxKtv = cboKtv.getSelectedIndex();
        if (idxKtv > 0 && idxKtv - 1 < dsKtv.size()) {
            maKtv = dsKtv.get(idxKtv - 1).getMaNV();
        }

        final String finalBienSo = xeChon.getBienSoXe();
        final String finalMaKtv = maKtv;
        final String finalYeuCau = yeuCau;

        btnLuu.setEnabled(false);
        btnLuu.setText("Đang lưu phiếu...");
        lblStatus.setText("Đang ghi nhận phiếu vào hệ thống...");
        lblStatus.setForeground(C_PRIMARY);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return controller.lapPhieu(finalBienSo, finalMaKtv, finalYeuCau, dsChiTietChon);
            }

            @Override
            protected void done() {
                btnLuu.setEnabled(true);
                btnLuu.setText("💾 Lưu phiếu tiếp nhận");
                try {
                    String maPhieuMoi = get();
                    txtMaPhieu.setText(maPhieuMoi);
                    lblStatus.setText("✓ Đã lập phiếu " + maPhieuMoi + " thành công!");
                    lblStatus.setForeground(C_SUCCESS);

                    JOptionPane.showMessageDialog(LapPhieuTiepNhanUI.this,
                            "Lập phiếu tiếp nhận sửa chữa thành công!\nMã phiếu: " + maPhieuMoi + "\nTrạng thái: Chờ sửa chữa",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);

                    // Reset form để sẵn sàng lập phiếu tiếp theo
                    resetForm();
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    lblStatus.setText("Lỗi: " + msg);
                    lblStatus.setForeground(C_DANGER);
                    JOptionPane.showMessageDialog(LapPhieuTiepNhanUI.this, msg, "Lỗi lập phiếu", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void resetForm() {
        cboKhachHang.setSelectedIndex(0);
        cboXe.removeAllItems();
        cboKtv.setSelectedIndex(0);
        txtMaPhieu.setText("Chưa được cấp mã (Hệ thống tự sinh)");
        txtThoiGian.setText(timeFmt.format(new Date()));
        txtYeuCauBanDau.setText("");
        txtGhiChuTiepNhan.setText("");
        dsChiTietChon.clear();
        capNhatBangDichVu();
        lblStatus.setText("Sẵn sàng lập phiếu mới.");
        lblStatus.setForeground(C_TEXT_SUBTLE);
    }

    public void thietLapKhachHangVaXe(String maKH, String bienSoXe) {
        if (maKH == null || dsKhachHang == null) return;
        for (int i = 0; i < dsKhachHang.size(); i++) {
            if (maKH.equals(dsKhachHang.get(i).getMaKH())) {
                cboKhachHang.setSelectedIndex(i + 1);
                final String targetBienSo = bienSoXe;
                Timer timer = new Timer(350, ev -> {
                    ((Timer) ev.getSource()).stop();
                    if (targetBienSo != null && dsXe != null) {
                        for (int j = 0; j < dsXe.size(); j++) {
                            if (targetBienSo.equalsIgnoreCase(dsXe.get(j).getBienSoXe())) {
                                cboXe.setSelectedIndex(j);
                                break;
                            }
                        }
                    }
                    txtYeuCauBanDau.requestFocusInWindow();
                });
                timer.setRepeats(false);
                timer.start();
                break;
            }
        }
    }

    // Helper tạo Card trắng bo viền
    private JPanel createWhiteCard(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(C_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(14, 16, 14, 16)));

        JLabel lbl = new JLabel(title);
        lbl.setFont(f(Font.BOLD, 14));
        lbl.setForeground(C_TEXT_HEAD);
        card.add(lbl, BorderLayout.NORTH);
        return card;
    }

    private JPanel createFieldWrapper(String labelText, JComponent input) {
        JPanel pnl = new JPanel(new BorderLayout(0, 4));
        pnl.setOpaque(false);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(f(Font.PLAIN, 12));
        lbl.setForeground(C_TEXT_MAIN);
        pnl.add(lbl, BorderLayout.NORTH);

        if (input instanceof JTextField) {
            input.setPreferredSize(new Dimension(0, 34));
            ((JTextField) input).setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_BORDER, 1),
                    new EmptyBorder(4, 8, 4, 8)));
        }
        pnl.add(input, BorderLayout.CENTER);
        return pnl;
    }
}
