package ui;

import controller.TaoPhieuDoiTraController;
import entity.*;
import enums.HuongXuLyDoiTra;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static ui.MainFrame.*;

/**
 * Màn hình Tạo phiếu đổi trả (UC012 - Tiếp nhận viên).
 * Bám sát Figma: Tao_phieu_doi_tra.jpg
 * Chỉ áp dụng cho hóa đơn ĐÃ THANH TOÁN và trong vòng 7 NGÀY.
 */
public class TaoPhieuDoiTraUI extends JPanel {

    private final TaoPhieuDoiTraController controller = new TaoPhieuDoiTraController();
    private final DecimalFormat moneyFmt = new DecimalFormat("#,### đ");
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private List<HoaDon> dsHoaDonDuDieuKien = new ArrayList<>();
    private HoaDon hoaDonDangChon = null;

    // Danh sách dịch vụ và linh kiện của hóa đơn để chọn đổi trả
    private List<ChiTietDichVu> dsDVHoaDon = new ArrayList<>();
    private List<ChiTietLinhKien> dsLKHoaDon = new ArrayList<>();

    // Danh sách các chi tiết đổi trả người dùng thêm vào
    private final List<ChiTietDoiTra> dsChiTietDoiTra = new ArrayList<>();

    // Components khối 1: Chọn hóa đơn
    private JComboBox<String> cboHoaDon;
    private JTextField txtMaPhieuDT;
    private JTextField txtKhachHang;
    private JTextField txtBienSo;
    private JTextField txtNgayThanhToan;
    private JTextField txtTongTienGoc;
    private JLabel lblDieuKienBadge;

    // Components khối 2: Chọn hạng mục
    private JComboBox<String> cboHangMuc;
    private JSpinner spnSoLuongDT;
    private JComboBox<String> cboHuongXuLy;
    private DefaultTableModel modelBangDT;
    private JTable tblBangDT;
    private JLabel lblTongHoanTraTamTinh;

    // Components khối 3: Lý do
    private JTextArea txtLyDo;

    // Action buttons & status
    private JButton btnLuu;
    private JButton btnLamMoi;
    private JLabel lblStatus;

    public TaoPhieuDoiTraUI() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildScrollBody(), BorderLayout.CENTER);

        napDanhSachHoaDon();
    }

    private JPanel buildHeader() {
        JPanel pnl = new JPanel(new BorderLayout(12, 0));
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(16, 24, 12, 24));

        JPanel titleBlock = new JPanel(new BorderLayout(0, 2));
        titleBlock.setOpaque(false);

        JLabel lblTitle = new JLabel("Tạo phiếu đổi trả");
        lblTitle.setFont(f(Font.BOLD, 20));
        lblTitle.setForeground(C_TEXT_HEAD);

        JLabel lblSub = new JLabel("Tiếp nhận yêu cầu bảo hành, đổi trả linh kiện và dịch vụ trong thời hạn 7 ngày sau thanh toán");
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

        body.add(buildCardChonHoaDon());
        body.add(Box.createVerticalStrut(14));
        body.add(buildCardChonHangMuc());
        body.add(Box.createVerticalStrut(14));
        body.add(buildCardLyDo());
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
    // 1. GIAO DỊCH LIÊN QUAN (HÓA ĐƠN GỐC)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildCardChonHoaDon() {
        JPanel card = createWhiteCard("1. Hóa đơn gốc liên quan (Đã thanh toán, trong 7 ngày)");
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Hàng chọn HĐ
        JPanel rowSelect = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        rowSelect.setOpaque(false);
        rowSelect.add(new JLabel("Chọn hóa đơn *:"));

        cboHoaDon = new JComboBox<>();
        cboHoaDon.setPreferredSize(new Dimension(380, 34));
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

        // Hàng thông tin chi tiết hóa đơn
        JPanel rowInfo = new JPanel(new GridLayout(2, 3, 12, 6));
        rowInfo.setOpaque(false);

        txtMaPhieuDT = createReadOnlyField("Chưa cấp mã");
        rowInfo.add(createFieldWrapper("Mã phiếu đổi trả", txtMaPhieuDT));

        txtKhachHang = createReadOnlyField("--");
        rowInfo.add(createFieldWrapper("Khách hàng", txtKhachHang));

        txtBienSo = createReadOnlyField("--");
        rowInfo.add(createFieldWrapper("Biển số xe", txtBienSo));

        txtNgayThanhToan = createReadOnlyField("--");
        rowInfo.add(createFieldWrapper("Thời điểm thanh toán", txtNgayThanhToan));

        txtTongTienGoc = createReadOnlyField("--");
        rowInfo.add(createFieldWrapper("Tổng tiền hóa đơn gốc", txtTongTienGoc));

        content.add(rowInfo);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. HẠNG MỤC ĐỀ NGHỊ ĐỔI TRẢ
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildCardChonHangMuc() {
        JPanel card = createWhiteCard("2. Hạng mục đổi trả");
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setOpaque(false);

        // Thanh công cụ thêm hạng mục
        JPanel toolRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolRow.setOpaque(false);

        toolRow.add(new JLabel("Hạng mục từ hóa đơn:"));
        cboHangMuc = new JComboBox<>();
        cboHangMuc.setPreferredSize(new Dimension(280, 34));
        cboHangMuc.setFont(f(Font.PLAIN, 13));
        toolRow.add(cboHangMuc);

        toolRow.add(new JLabel("Số lượng:"));
        spnSoLuongDT = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        spnSoLuongDT.setPreferredSize(new Dimension(60, 34));
        spnSoLuongDT.setFont(f(Font.PLAIN, 13));
        toolRow.add(spnSoLuongDT);

        toolRow.add(new JLabel("Hướng xử lý:"));
        cboHuongXuLy = new JComboBox<>(new String[]{"Hoàn tiền", "Đổi mới linh kiện", "Sửa chữa lại"});
        cboHuongXuLy.setPreferredSize(new Dimension(140, 34));
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

        // Bảng
        modelBangDT = new DefaultTableModel(new String[]{"STT", "Phân loại", "Mã", "Tên hạng mục", "Số lượng", "Đơn giá", "Thành tiền", "Hướng xử lý"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblBangDT = new JTable(modelBangDT);
        tblBangDT.setFont(f(Font.PLAIN, 13));
        tblBangDT.setRowHeight(30);
        tblBangDT.setShowGrid(false);
        tblBangDT.getTableHeader().setFont(f(Font.BOLD, 12));
        tblBangDT.getTableHeader().setBackground(new Color(0xF8FAFC));

        JScrollPane scrollTable = new JScrollPane(tblBangDT);
        scrollTable.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        scrollTable.setPreferredSize(new Dimension(0, 160));
        content.add(scrollTable, BorderLayout.CENTER);

        // Footer tiền hoàn tạm tính
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 4));
        footer.setOpaque(false);
        footer.add(new JLabel("Ước tính số tiền hoàn trả:"));
        lblTongHoanTraTamTinh = new JLabel("0 đ");
        lblTongHoanTraTamTinh.setFont(f(Font.BOLD, 16));
        lblTongHoanTraTamTinh.setForeground(C_DANGER);
        footer.add(lblTongHoanTraTamTinh);
        content.add(footer, BorderLayout.SOUTH);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. LÝ DO ĐỔI TRẢ
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildCardLyDo() {
        JPanel card = createWhiteCard("3. Nội dung & Lý do đề nghị đổi trả *");
        JPanel content = new JPanel(new BorderLayout(0, 4));
        content.setOpaque(false);

        txtLyDo = new JTextArea(3, 20);
        txtLyDo.setFont(f(Font.PLAIN, 13));
        txtLyDo.setLineWrap(true);
        txtLyDo.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(txtLyDo);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));
        content.add(scroll, BorderLayout.CENTER);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTION BAR
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildBottomActionBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);

        lblStatus = new JLabel("Vui lòng chọn hóa đơn gốc cần đổi trả.");
        lblStatus.setFont(f(Font.ITALIC, 13));
        lblStatus.setForeground(C_TEXT_SUBTLE);
        bar.add(lblStatus, BorderLayout.WEST);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnGroup.setOpaque(false);

        btnLamMoi = new RoundedButton("↺ Làm mới", 8, C_WHITE, new Color(0xF1F5F9), C_BORDER);
        btnLamMoi.setFont(f(Font.PLAIN, 13));
        btnLamMoi.setForeground(C_TEXT_MAIN);
        btnLamMoi.setPreferredSize(new Dimension(100, 36));
        btnLamMoi.addActionListener(e -> resetForm());
        btnGroup.add(btnLamMoi);

        btnLuu = new RoundedButton("💾 Lập phiếu đổi trả", 8, C_PRIMARY, C_PRIMARY_HV, C_PRIMARY);
        btnLuu.setFont(f(Font.BOLD, 13));
        btnLuu.setForeground(C_WHITE);
        btnLuu.setPreferredSize(new Dimension(180, 36));
        btnLuu.addActionListener(e -> thucHienLuuPhieuDoiTra());
        btnGroup.add(btnLuu);

        bar.add(btnGroup, BorderLayout.EAST);
        return bar;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DỮ LIỆU & BINDING
    // ─────────────────────────────────────────────────────────────────────────
    private void napDanhSachHoaDon() {
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
                    cboHoaDon.addItem("-- Chọn hóa đơn đủ điều kiện đổi trả --");
                    for (HoaDon hd : dsHoaDonDuDieuKien) {
                        cboHoaDon.addItem(hd.getMaHoaDon() + " - " + moneyFmt.format(hd.getTongThanhToan()));
                    }
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
        if (hoaDonDangChon.getThoiGianThanhToan() != null) {
            txtNgayThanhToan.setText(dateFmt.format(hoaDonDangChon.getThoiGianThanhToan()));
        } else if (hoaDonDangChon.getNgayLap() != null) {
            txtNgayThanhToan.setText(dateFmt.format(hoaDonDangChon.getNgayLap()));
        }
        txtTongTienGoc.setText(moneyFmt.format(hoaDonDangChon.getTongThanhToan()));
        lblDieuKienBadge.setText("✓ Đủ điều kiện đổi trả (trong 7 ngày)");
        lblDieuKienBadge.setForeground(C_SUCCESS);

        // Nạp các hạng mục từ hóa đơn
        if (phieu != null) {
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
                    cboHangMuc.addItem("-- Chọn hạng mục cần đổi trả --");
                    if (dsDVHoaDon != null) {
                        for (ChiTietDichVu ctdv : dsDVHoaDon) {
                            DichVu dv = ctdv.getDichVu();
                            if (dv != null) {
                                cboHangMuc.addItem("[Dịch vụ] " + dv.getMaDV() + " - " + dv.getTenDV() + " (SL: " + ctdv.getSoLuong() + ")");
                            }
                        }
                    }
                    if (dsLKHoaDon != null) {
                        for (ChiTietLinhKien ctlk : dsLKHoaDon) {
                            LinhKien lk = ctlk.getLinhKien();
                            if (lk != null) {
                                cboHangMuc.addItem("[Linh kiện] " + lk.getMaLK() + " - " + lk.getTenLK() + " (SL: " + ctlk.getSoLuong() + ")");
                            }
                        }
                    }
                }
            }.execute();
        }
    }

    private void themHangMucVaoDanhSach() {
        int idx = cboHangMuc.getSelectedIndex();
        if (idx <= 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một hạng mục từ danh sách.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int sl = (int) spnSoLuongDT.getValue();
        HuongXuLyDoiTra huongXuLy = switch (cboHuongXuLy.getSelectedIndex()) {
            case 1 -> HuongXuLyDoiTra.DOI_MOI;
            case 2 -> HuongXuLyDoiTra.SUA_LAI_MIEN_PHI;
            default -> HuongXuLyDoiTra.HOAN_TIEN;
        };

        // Xác định là DV hay LK
        int countDV = dsDVHoaDon != null ? dsDVHoaDon.size() : 0;
        ChiTietDoiTra ctMoi = new ChiTietDoiTra();
        ctMoi.setSttRaw(dsChiTietDoiTra.size() + 1);
        ctMoi.setSoLuongRaw(sl);
        ctMoi.setHuongXuLyRaw(huongXuLy);

        if (idx - 1 < countDV) {
            // Là dịch vụ
            ChiTietDichVu ctdv = dsDVHoaDon.get(idx - 1);
            if (sl > ctdv.getSoLuong()) {
                JOptionPane.showMessageDialog(this, "Số lượng đổi trả không được lớn hơn số lượng trong hóa đơn (" + ctdv.getSoLuong() + ").", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            ctMoi.setMaDVRaw(ctdv.getDichVu().getMaDV());
            ctMoi.setDonGiaRaw(ctdv.getDonGiaThucHien());
        } else {
            // Là linh kiện
            int idxLK = idx - 1 - countDV;
            ChiTietLinhKien ctlk = dsLKHoaDon.get(idxLK);
            if (sl > ctlk.getSoLuong()) {
                JOptionPane.showMessageDialog(this, "Số lượng đổi trả không được lớn hơn số lượng trong hóa đơn (" + ctlk.getSoLuong() + ").", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            ctMoi.setMaLKRaw(ctlk.getLinhKien().getMaLK());
            ctMoi.setDonGiaRaw(ctlk.getDonGiaXuat());
        }

        dsChiTietDoiTra.add(ctMoi);
        capNhatBangDoiTra();
    }

    private void xoaHangMucKhoiDanhSach() {
        int row = tblBangDT.getSelectedRow();
        if (row < 0 || row >= dsChiTietDoiTra.size()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn dòng cần xóa.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        dsChiTietDoiTra.remove(row);
        for (int i = 0; i < dsChiTietDoiTra.size(); i++) {
            dsChiTietDoiTra.get(i).setSttRaw(i + 1);
        }
        capNhatBangDoiTra();
    }

    private void capNhatBangDoiTra() {
        modelBangDT.setRowCount(0);
        double tong = 0;
        for (int i = 0; i < dsChiTietDoiTra.size(); i++) {
            ChiTietDoiTra ct = dsChiTietDoiTra.get(i);
            double tt = ct.getDonGia() * ct.getSoLuong();
            tong += tt;
            String loai = ct.getMaDV() != null ? "Dịch vụ" : "Linh kiện";
            String ma = ct.getMaDV() != null ? ct.getMaDV() : ct.getMaLK();
            modelBangDT.addRow(new Object[]{
                    i + 1,
                    loai,
                    ma,
                    ma, // tên có thể lookup thêm
                    ct.getSoLuong(),
                    moneyFmt.format(ct.getDonGia()),
                    moneyFmt.format(tt),
                    ct.getHuongXuLy()
            });
        }
        lblTongHoanTraTamTinh.setText(moneyFmt.format(tong));
    }

    private void thucHienLuuPhieuDoiTra() {
        if (hoaDonDangChon == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn hóa đơn cần đổi trả.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (dsChiTietDoiTra.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng thêm ít nhất một hạng mục đổi trả.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String lyDo = txtLyDo.getText().trim();
        if (lyDo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập lý do đề nghị đổi trả.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            txtLyDo.requestFocusInWindow();
            return;
        }

        btnLuu.setEnabled(false);
        btnLuu.setText("Đang lập...");
        lblStatus.setText("Đang ghi nhận phiếu đổi trả...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return controller.lapPhieuDoiTra(hoaDonDangChon, lyDo, dsChiTietDoiTra);
            }

            @Override
            protected void done() {
                btnLuu.setEnabled(true);
                btnLuu.setText("💾 Lập phiếu đổi trả");
                try {
                    String maPhieuMoi = get();
                    txtMaPhieuDT.setText(maPhieuMoi);
                    lblStatus.setText("✓ Đã tạo phiếu đổi trả " + maPhieuMoi + " thành công!");
                    lblStatus.setForeground(C_SUCCESS);

                    JOptionPane.showMessageDialog(TaoPhieuDoiTraUI.this,
                            "Tạo phiếu đổi trả thành công!\nMã phiếu: " + maPhieuMoi,
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);

                    resetForm();
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    lblStatus.setText("Lỗi: " + msg);
                    lblStatus.setForeground(C_DANGER);
                    JOptionPane.showMessageDialog(TaoPhieuDoiTraUI.this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void resetFormChiTiet() {
        txtMaPhieuDT.setText("Chưa cấp mã");
        txtKhachHang.setText("--");
        txtBienSo.setText("--");
        txtNgayThanhToan.setText("--");
        txtTongTienGoc.setText("--");
        lblDieuKienBadge.setText(" ");
        cboHangMuc.removeAllItems();
        dsChiTietDoiTra.clear();
        capNhatBangDoiTra();
    }

    private void resetForm() {
        napDanhSachHoaDon();
        resetFormChiTiet();
        txtLyDo.setText("");
        lblStatus.setText("Vui lòng chọn hóa đơn gốc cần đổi trả.");
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
}
