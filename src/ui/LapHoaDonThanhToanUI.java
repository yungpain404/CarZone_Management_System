package ui;

import controller.LapHoaDonThanhToanController;
import entity.*;
import enums.PhuongThucThanhToan;
import enums.TrangThaiThanhToan;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static ui.MainFrame.*;

/**
 * Màn hình Lập hóa đơn và Thanh toán (UC014, UC015 - Tiếp nhận viên).
 * Bám sát Figma: Lap_hoa_don_Thanh_Toan.jpg
 */
public class LapHoaDonThanhToanUI extends JPanel {

    private final LapHoaDonThanhToanController controller = new LapHoaDonThanhToanController();
    private final DecimalFormat moneyFmt = new DecimalFormat("#,### đ");

    private List<PhieuTiepNhanSuaChua> dsPhieuDuDieuKien = new ArrayList<>();
    private List<KhuyenMai> dsKhuyenMai = new ArrayList<>();

    private PhieuTiepNhanSuaChua phieuDangChon = null;
    private HoaDon hoaDonHienTai = null;

    // Components chọn phiếu & khuyến mãi
    private JComboBox<String> cboPhieu;
    private JComboBox<String> cboKhuyenMai;

    // Components thông tin tóm tắt
    private JTextField txtMaHoaDon;
    private JTextField txtMaPhieu;
    private JTextField txtTrangThaiTT;
    private JTextField txtKhachHang;
    private JTextField txtSdt;
    private JTextField txtBienSo;

    // Bảng chi tiết
    private DefaultTableModel modelChiTiet;
    private JTable tblChiTiet;

    // Tổng tiền
    private JLabel lblTongDichVu;
    private JLabel lblTongLinhKien;
    private JLabel lblGiamGia;
    private JLabel lblThue;
    private JLabel lblTongThanhToan;

    // Action buttons & status
    private JButton btnLapHoaDon;
    private JButton btnXacNhanThanhToan;
    private JButton btnLamMoi;
    private JLabel lblStatus;

    public LapHoaDonThanhToanUI() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildScrollBody(), BorderLayout.CENTER);

        napDanhSachPhieuVaKhuyenMai();
    }

    private JPanel buildHeader() {
        JPanel pnl = new JPanel(new BorderLayout(12, 0));
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(16, 24, 12, 24));

        JPanel titleBlock = new JPanel(new BorderLayout(0, 2));
        titleBlock.setOpaque(false);

        JLabel lblTitle = new JLabel("Lập hóa đơn và Thanh toán");
        lblTitle.setFont(f(Font.BOLD, 20));
        lblTitle.setForeground(C_TEXT_HEAD);

        JLabel lblSub = new JLabel("Lập hóa đơn cho các phiếu sửa chữa đã hoàn tất, áp dụng khuyến mãi và thu ngân");
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

        body.add(buildCardChonPhieu());
        body.add(Box.createVerticalStrut(14));
        body.add(buildCardThongTinChung());
        body.add(Box.createVerticalStrut(14));
        body.add(buildCardBangChiTiet());
        body.add(Box.createVerticalStrut(14));
        body.add(buildCardTongKetTien());
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
    // 1. CHỌN PHIẾU ĐỦ ĐIỀU KIỆN
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildCardChonPhieu() {
        JPanel card = createWhiteCard("1. Chọn phiếu sửa chữa hoàn tất");
        JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        content.setOpaque(false);

        content.add(new JLabel("Phiếu sửa chữa đủ điều kiện (HOÀN TẤT) *:"));
        cboPhieu = new JComboBox<>();
        cboPhieu.setPreferredSize(new Dimension(380, 34));
        cboPhieu.setFont(f(Font.PLAIN, 13));
        cboPhieu.addActionListener(e -> onChonPhieu());
        content.add(cboPhieu);

        JButton btnReload = new RoundedButton("↺ Tải lại danh sách", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnReload.setFont(f(Font.PLAIN, 12));
        btnReload.setPreferredSize(new Dimension(130, 34));
        btnReload.addActionListener(e -> napDanhSachPhieuVaKhuyenMai());
        content.add(btnReload);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. THÔNG TIN CHUNG HÓA ĐƠN & KHÁCH HÀNG
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildCardThongTinChung() {
        JPanel card = createWhiteCard("2. Thông tin chung hóa đơn & khách hàng");
        JPanel content = new JPanel(new GridLayout(2, 3, 12, 8));
        content.setOpaque(false);

        txtMaHoaDon = createReadOnlyField("Chưa lập hóa đơn");
        content.add(createFieldWrapper("Mã hóa đơn", txtMaHoaDon));

        txtMaPhieu = createReadOnlyField("--");
        content.add(createFieldWrapper("Mã phiếu sửa chữa", txtMaPhieu));

        txtTrangThaiTT = createReadOnlyField("--");
        content.add(createFieldWrapper("Trạng thái thanh toán", txtTrangThaiTT));

        txtKhachHang = createReadOnlyField("--");
        content.add(createFieldWrapper("Khách hàng", txtKhachHang));

        txtSdt = createReadOnlyField("--");
        content.add(createFieldWrapper("Số điện thoại", txtSdt));

        txtBienSo = createReadOnlyField("--");
        content.add(createFieldWrapper("Biển số xe", txtBienSo));

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. BẢNG CHI TIẾT DỊCH VỤ & LINH KIỆN
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildCardBangChiTiet() {
        JPanel card = createWhiteCard("3. Chi tiết dịch vụ đã thực hiện & linh kiện thay thế");
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setOpaque(false);

        modelChiTiet = new DefaultTableModel(new String[]{"STT", "Phân loại", "Mã", "Tên hạng mục", "ĐVT", "Số lượng", "Đơn giá", "Thành tiền"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblChiTiet = new JTable(modelChiTiet);
        tblChiTiet.setFont(f(Font.PLAIN, 13));
        tblChiTiet.setRowHeight(30);
        tblChiTiet.setShowGrid(false);
        tblChiTiet.getTableHeader().setFont(f(Font.BOLD, 12));
        tblChiTiet.getTableHeader().setBackground(new Color(0xF8FAFC));

        JScrollPane scrollTable = new JScrollPane(tblChiTiet);
        scrollTable.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        scrollTable.setPreferredSize(new Dimension(0, 180));
        content.add(scrollTable, BorderLayout.CENTER);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. TỔNG KẾT TÀI CHÍNH & KHUYẾN MÃI
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildCardTongKetTien() {
        JPanel card = createWhiteCard("4. Khuyến mãi & Tổng thanh toán");
        JPanel content = new JPanel(new GridLayout(1, 2, 20, 0));
        content.setOpaque(false);

        // Bên trái: Chọn khuyến mãi
        JPanel pnlKm = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        pnlKm.setOpaque(false);
        pnlKm.add(new JLabel("Mã khuyến mãi áp dụng:"));
        cboKhuyenMai = new JComboBox<>();
        cboKhuyenMai.setPreferredSize(new Dimension(300, 34));
        cboKhuyenMai.setFont(f(Font.PLAIN, 13));
        pnlKm.add(cboKhuyenMai);
        content.add(pnlKm);

        // Bên phải: Bảng số liệu tiền
        JPanel pnlTien = new JPanel(new GridLayout(5, 2, 10, 4));
        pnlTien.setOpaque(false);

        pnlTien.add(createLabelTien("Tổng tiền dịch vụ:"));
        lblTongDichVu = createValTien("0 đ");
        pnlTien.add(lblTongDichVu);

        pnlTien.add(createLabelTien("Tổng tiền linh kiện:"));
        lblTongLinhKien = createValTien("0 đ");
        pnlTien.add(lblTongLinhKien);

        pnlTien.add(createLabelTien("Giảm giá khuyến mãi:"));
        lblGiamGia = createValTien("0 đ");
        lblGiamGia.setForeground(C_SUCCESS);
        pnlTien.add(lblGiamGia);

        pnlTien.add(createLabelTien("Thuế VAT:"));
        lblThue = createValTien("0 đ");
        pnlTien.add(lblThue);

        JLabel lblTongHead = new JLabel("TỔNG THANH TOÁN:");
        lblTongHead.setFont(f(Font.BOLD, 14));
        lblTongHead.setForeground(C_TEXT_HEAD);
        pnlTien.add(lblTongHead);

        lblTongThanhToan = new JLabel("0 đ");
        lblTongThanhToan.setFont(f(Font.BOLD, 17));
        lblTongThanhToan.setForeground(C_PRIMARY);
        pnlTien.add(lblTongThanhToan);

        content.add(pnlTien);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. ACTION BAR
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildBottomActionBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);

        lblStatus = new JLabel("Vui lòng chọn phiếu sửa chữa cần lập hóa đơn.");
        lblStatus.setFont(f(Font.ITALIC, 13));
        lblStatus.setForeground(C_TEXT_SUBTLE);
        bar.add(lblStatus, BorderLayout.WEST);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnGroup.setOpaque(false);

        btnLamMoi = new RoundedButton("↺ Làm mới", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnLamMoi.setFont(f(Font.PLAIN, 13));
        btnLamMoi.setForeground(C_TEXT_MAIN);
        btnLamMoi.setPreferredSize(new Dimension(100, 36));
        btnLamMoi.addActionListener(e -> resetManHinh());
        btnGroup.add(btnLamMoi);

        btnLapHoaDon = new RoundedButton("📝 1. Lập hóa đơn", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
        btnLapHoaDon.setFont(f(Font.BOLD, 13));
        btnLapHoaDon.setForeground(C_WHITE);
        btnLapHoaDon.setPreferredSize(new Dimension(150, 36));
        btnLapHoaDon.addActionListener(e -> thucHienLapHoaDon());
        btnGroup.add(btnLapHoaDon);

        btnXacNhanThanhToan = new RoundedButton("💳 2. Thanh toán", 8, new Color(0x059669), new Color(0x047857), new Color(0x059669));
        btnXacNhanThanhToan.setFont(f(Font.BOLD, 13));
        btnXacNhanThanhToan.setForeground(C_WHITE);
        btnXacNhanThanhToan.setPreferredSize(new Dimension(150, 36));
        btnXacNhanThanhToan.setEnabled(false); // Chờ có hóa đơn mới cho thanh toán
        btnXacNhanThanhToan.addActionListener(e -> moDialogThanhToan());
        btnGroup.add(btnXacNhanThanhToan);

        bar.add(btnGroup, BorderLayout.EAST);
        return bar;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGIC & DATA BINDING
    // ─────────────────────────────────────────────────────────────────────────
    private void napDanhSachPhieuVaKhuyenMai() {
        new SwingWorker<Void, Void>() {
            List<PhieuTiepNhanSuaChua> phieus;
            List<KhuyenMai> kms;

            @Override
            protected Void doInBackground() {
                phieus = controller.layDanhSachPhieuDuDieuKien();
                kms = controller.layKhuyenMaiHieuLuc();
                return null;
            }

            @Override
            protected void done() {
                dsPhieuDuDieuKien = phieus != null ? phieus : List.of();
                dsKhuyenMai = kms != null ? kms : List.of();

                cboPhieu.removeAllItems();
                cboPhieu.addItem("-- Chọn phiếu sửa chữa hoàn tất --");
                for (PhieuTiepNhanSuaChua p : dsPhieuDuDieuKien) {
                    String bienSo = (p.getXe() != null) ? p.getXe().getBienSoXe() : "Chưa rõ";
                    cboPhieu.addItem(p.getMaPhieu() + " - Biển số: " + bienSo);
                }

                cboKhuyenMai.removeAllItems();
                cboKhuyenMai.addItem("-- Không áp dụng khuyến mãi --");
                for (KhuyenMai km : dsKhuyenMai) {
                    cboKhuyenMai.addItem(km.getMaKM() + " - " + km.getTenChuongTrinh() + " (" + (int)km.getMucGiamPhanTram() + "%)");
                }
            }
        }.execute();
    }

    private void onChonPhieu() {
        int idx = cboPhieu.getSelectedIndex();
        if (idx <= 0 || idx - 1 >= dsPhieuDuDieuKien.size()) {
            phieuDangChon = null;
            hoaDonHienTai = null;
            resetFormChiTiet();
            return;
        }

        phieuDangChon = dsPhieuDuDieuKien.get(idx - 1);
        txtMaPhieu.setText(phieuDangChon.getMaPhieu());

        Xe xe = phieuDangChon.getXe();
        if (xe != null) {
            txtBienSo.setText(xe.getBienSoXe());
            KhachHang kh = xe.getChuXe();
            if (kh != null) {
                txtKhachHang.setText(kh.getHoTen());
                txtSdt.setText(kh.getSoDienThoai());
            }
        }

        // Tải chi tiết dịch vụ & linh kiện của phiếu qua SwingWorker
        new SwingWorker<Void, Void>() {
            List<ChiTietDichVu> dsDV;
            List<ChiTietLinhKien> dsLK;
            HoaDon hd;

            @Override
            protected Void doInBackground() {
                dsDV = controller.layChiTietDichVu(phieuDangChon.getMaPhieu());
                dsLK = controller.layChiTietLinhKien(phieuDangChon.getMaPhieu());
                hd = controller.timHoaDonTheoPhieu(phieuDangChon.getMaPhieu());
                return null;
            }

            @Override
            protected void done() {
                hoaDonHienTai = hd;
                modelChiTiet.setRowCount(0);
                double tongDV = 0;
                double tongLK = 0;
                int stt = 1;

                if (dsDV != null) {
                    for (ChiTietDichVu ctdv : dsDV) {
                        DichVu dv = ctdv.getDichVu();
                        double tt = ctdv.getDonGia() * ctdv.getSoLuong();
                        tongDV += tt;
                        modelChiTiet.addRow(new Object[]{
                                stt++,
                                "Dịch vụ",
                                dv != null ? dv.getMaDV() : "",
                                dv != null ? dv.getTenDV() : "",
                                dv != null && dv.getDonViTinh() != null ? dv.getDonViTinh() : "Lần",
                                ctdv.getSoLuong(),
                                moneyFmt.format(ctdv.getDonGia()),
                                moneyFmt.format(tt)
                        });
                    }
                }

                if (dsLK != null) {
                    for (ChiTietLinhKien ctlk : dsLK) {
                        LinhKien lk = ctlk.getLinhKien();
                        double tt = ctlk.getDonGia() * ctlk.getSoLuong();
                        tongLK += tt;
                        modelChiTiet.addRow(new Object[]{
                                stt++,
                                "Linh kiện",
                                lk != null ? lk.getMaLK() : "",
                                lk != null ? lk.getTenLK() : "",
                                lk != null && lk.getDonViTinh() != null ? lk.getDonViTinh() : "Cái",
                                ctlk.getSoLuong(),
                                moneyFmt.format(ctlk.getDonGia()),
                                moneyFmt.format(tt)
                        });
                    }
                }

                lblTongDichVu.setText(moneyFmt.format(tongDV));
                lblTongLinhKien.setText(moneyFmt.format(tongLK));

                if (hoaDonHienTai != null) {
                    txtMaHoaDon.setText(hoaDonHienTai.getMaHoaDon());
                    txtTrangThaiTT.setText(hoaDonHienTai.getTrangThaiThanhToan() == TrangThaiThanhToan.DA_THANH_TOAN ?
                            "Đã thanh toán" : "Chưa thanh toán");
                    lblGiamGia.setText(moneyFmt.format(hoaDonHienTai.getTienGiam()));
                    lblThue.setText(moneyFmt.format(hoaDonHienTai.getThue()));
                    lblTongThanhToan.setText(moneyFmt.format(hoaDonHienTai.getTongThanhToan()));

                    btnLapHoaDon.setEnabled(false);
                    btnXacNhanThanhToan.setEnabled(hoaDonHienTai.getTrangThaiThanhToan() == TrangThaiThanhToan.CHUA_THANH_TOAN);
                    lblStatus.setText("Phiếu này đã được lập hóa đơn " + hoaDonHienTai.getMaHoaDon());
                } else {
                    txtMaHoaDon.setText("Chưa lập hóa đơn");
                    txtTrangThaiTT.setText("Chờ lập HĐ");
                    // Ước lượng tổng tạm tính chưa KM và thuế
                    double tongTam = tongDV + tongLK;
                    lblTongThanhToan.setText(moneyFmt.format(tongTam));
                    lblGiamGia.setText("0 đ");
                    lblThue.setText("Được tính khi lưu");

                    btnLapHoaDon.setEnabled(true);
                    btnXacNhanThanhToan.setEnabled(false);
                    lblStatus.setText("Sẵn sàng lập hóa đơn cho phiếu " + phieuDangChon.getMaPhieu());
                }
            }
        }.execute();
    }

    private void thucHienLapHoaDon() {
        if (phieuDangChon == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn phiếu sửa chữa cần lập hóa đơn.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String maKM = null;
        int idxKM = cboKhuyenMai.getSelectedIndex();
        if (idxKM > 0 && idxKM - 1 < dsKhuyenMai.size()) {
            maKM = dsKhuyenMai.get(idxKM - 1).getMaKM();
        }

        final String finalMaPhieu = phieuDangChon.getMaPhieu();
        final String finalMaKM = maKM;

        btnLapHoaDon.setEnabled(false);
        btnLapHoaDon.setText("Đang lập...");
        lblStatus.setText("Đang gọi sp_LapHoaDon từ cơ sở dữ liệu...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return controller.lapHoaDon(finalMaPhieu, finalMaKM);
            }

            @Override
            protected void done() {
                btnLapHoaDon.setEnabled(true);
                btnLapHoaDon.setText("📝 1. Lập hóa đơn");
                try {
                    String maHDMoi = get();
                    txtMaHoaDon.setText(maHDMoi);
                    txtTrangThaiTT.setText("Chưa thanh toán");
                    lblStatus.setText("✓ Đã lập hóa đơn " + maHDMoi + " thành công!");
                    lblStatus.setForeground(C_SUCCESS);

                    JOptionPane.showMessageDialog(LapHoaDonThanhToanUI.this,
                            "Lập hóa đơn thành công!\nMã hóa đơn: " + maHDMoi + "\nSẵn sàng thực hiện thanh toán.",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);

                    // Nạp lại thông tin hóa đơn
                    onChonPhieu();
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    lblStatus.setText("Lỗi: " + msg);
                    lblStatus.setForeground(C_DANGER);
                    JOptionPane.showMessageDialog(LapHoaDonThanhToanUI.this, msg, "Lỗi lập hóa đơn", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void moDialogThanhToan() {
        if (hoaDonHienTai == null) {
            JOptionPane.showMessageDialog(this, "Chưa có hóa đơn để thanh toán.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Tạo dialog nhập thông tin thanh toán
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Xác nhận thanh toán hóa đơn " + hoaDonHienTai.getMaHoaDon(), true);
        dlg.setSize(420, 280);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(0, 10));

        JPanel pnlBody = new JPanel(new GridLayout(4, 2, 8, 8));
        pnlBody.setBorder(new EmptyBorder(16, 16, 10, 16));

        pnlBody.add(new JLabel("Mã hóa đơn:"));
        JLabel lblMa = new JLabel(hoaDonHienTai.getMaHoaDon());
        lblMa.setFont(f(Font.BOLD, 13));
        pnlBody.add(lblMa);

        pnlBody.add(new JLabel("Số tiền thanh toán:"));
        JLabel lblTien = new JLabel(moneyFmt.format(hoaDonHienTai.getTongThanhToan()));
        lblTien.setFont(f(Font.BOLD, 15));
        lblTien.setForeground(C_PRIMARY);
        pnlBody.add(lblTien);

        pnlBody.add(new JLabel("Hình thức thanh toán:"));
        JComboBox<String> cboHinhThuc = new JComboBox<>(new String[]{"Tiền mặt", "Chuyển khoản ngân hàng"});
        pnlBody.add(cboHinhThuc);

        pnlBody.add(new JLabel("Mã giao dịch (nếu CK):"));
        JTextField txtMaGD = new JTextField();
        txtMaGD.setEnabled(false);
        pnlBody.add(txtMaGD);

        cboHinhThuc.addActionListener(e -> {
            boolean isCK = cboHinhThuc.getSelectedIndex() == 1;
            txtMaGD.setEnabled(isCK);
            if (isCK) txtMaGD.requestFocusInWindow();
        });

        dlg.add(pnlBody, BorderLayout.CENTER);

        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        JButton btnHuy = new RoundedButton("Hủy", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnHuy.addActionListener(e -> dlg.dispose());
        pnlBtns.add(btnHuy);

        JButton btnXacNhan = new RoundedButton("✓ Xác nhận đã thu tiền", 8, C_SUCCESS, new Color(0x047857), C_SUCCESS);
        btnXacNhan.setForeground(C_WHITE);
        btnXacNhan.setFont(f(Font.BOLD, 12));
        btnXacNhan.addActionListener(e -> {
            PhuongThucThanhToan pt = cboHinhThuc.getSelectedIndex() == 0 ?
                    PhuongThucThanhToan.TIEN_MAT : PhuongThucThanhToan.CHUYEN_KHOAN;
            String maGd = txtMaGD.getText().trim();
            if (pt == PhuongThucThanhToan.CHUYEN_KHOAN && maGd.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Vui lòng nhập mã giao dịch ngân hàng.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }

            dlg.dispose();
            xuLyThanhToan(hoaDonHienTai.getMaHoaDon(), pt, maGd);
        });
        pnlBtns.add(btnXacNhan);
        dlg.add(pnlBtns, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private void xuLyThanhToan(String maHD, PhuongThucThanhToan pt, String maGD) {
        btnXacNhanThanhToan.setEnabled(false);
        lblStatus.setText("Đang ghi nhận thanh toán...");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return controller.thanhToanHoaDon(maHD, pt, maGD);
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        lblStatus.setText("✓ Hóa đơn " + maHD + " đã được thanh toán thành công!");
                        lblStatus.setForeground(C_SUCCESS);
                        JOptionPane.showMessageDialog(LapHoaDonThanhToanUI.this,
                                "Thanh toán thành công hóa đơn " + maHD + "!\nCảm ơn quý khách.",
                                "Thanh toán hoàn tất", JOptionPane.INFORMATION_MESSAGE);
                        napDanhSachPhieuVaKhuyenMai();
                        resetFormChiTiet();
                    } else {
                        lblStatus.setText("Thanh toán thất bại.");
                        lblStatus.setForeground(C_DANGER);
                    }
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    lblStatus.setText("Lỗi thanh toán: " + msg);
                    lblStatus.setForeground(C_DANGER);
                    JOptionPane.showMessageDialog(LapHoaDonThanhToanUI.this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void resetFormChiTiet() {
        txtMaHoaDon.setText("Chưa lập hóa đơn");
        txtMaPhieu.setText("--");
        txtTrangThaiTT.setText("--");
        txtKhachHang.setText("--");
        txtSdt.setText("--");
        txtBienSo.setText("--");
        modelChiTiet.setRowCount(0);
        lblTongDichVu.setText("0 đ");
        lblTongLinhKien.setText("0 đ");
        lblGiamGia.setText("0 đ");
        lblThue.setText("0 đ");
        lblTongThanhToan.setText("0 đ");
        btnLapHoaDon.setEnabled(false);
        btnXacNhanThanhToan.setEnabled(false);
    }

    private void resetManHinh() {
        napDanhSachPhieuVaKhuyenMai();
        resetFormChiTiet();
        lblStatus.setText("Vui lòng chọn phiếu sửa chữa cần lập hóa đơn.");
        lblStatus.setForeground(C_TEXT_SUBTLE);
    }

    // Helpers UI
    private JPanel createWhiteCard(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(C_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(12, 16, 12, 16)));
        JLabel lbl = new JLabel(title);
        lbl.setFont(f(Font.BOLD, 14));
        lbl.setForeground(C_TEXT_HEAD);
        card.add(lbl, BorderLayout.NORTH);
        return card;
    }

    private JTextField createReadOnlyField(String text) {
        JTextField tf = new JTextField(text);
        tf.setEditable(false);
        tf.setFont(f(Font.PLAIN, 13));
        tf.setBackground(new Color(0xF8FAFC));
        tf.setForeground(C_TEXT_SUBTLE);
        return tf;
    }

    private JPanel createFieldWrapper(String labelText, JTextField input) {
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

    private JLabel createLabelTien(String text) {
        JLabel l = new JLabel(text);
        l.setFont(f(Font.PLAIN, 13));
        l.setForeground(C_TEXT_MAIN);
        return l;
    }

    private JLabel createValTien(String val) {
        JLabel l = new JLabel(val);
        l.setFont(f(Font.BOLD, 13));
        l.setForeground(C_TEXT_HEAD);
        return l;
    }
}
