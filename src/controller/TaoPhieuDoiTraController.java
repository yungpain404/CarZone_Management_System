package controller;

import dao.HoaDonDao;
import dao.PhieuDoiTraDao;
import dao.ChiTietDichVuDao;
import dao.ChiTietLinhKienDao;
import dao.impl.HoaDonDaoImpl;
import dao.impl.PhieuDoiTraDaoImpl;
import dao.impl.ChiTietDichVuDaoImpl;
import dao.impl.ChiTietLinhKienDaoImpl;
import entity.ChiTietDichVu;
import entity.ChiTietDoiTra;
import entity.ChiTietLinhKien;
import entity.HoaDon;
import entity.NhanVien;
import entity.PhieuDoiTra;
import enums.TrangThaiThanhToan;
import enums.VaiTro;
import session.UserSession;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Controller điều phối nghiệp vụ Tạo phiếu đổi trả (UC012 - Tiếp nhận viên).
 * Kiểm tra hóa đơn đã thanh toán và còn trong vòng 7 ngày trước khi cho phép đổi trả.
 */
public class TaoPhieuDoiTraController {

    private final HoaDonDao hoaDonDao;
    private final PhieuDoiTraDao phieuDoiTraDao;
    private final ChiTietDichVuDao chiTietDichVuDao;
    private final ChiTietLinhKienDao chiTietLinhKienDao;

    public TaoPhieuDoiTraController() {
        this.hoaDonDao = new HoaDonDaoImpl();
        this.phieuDoiTraDao = new PhieuDoiTraDaoImpl();
        this.chiTietDichVuDao = new ChiTietDichVuDaoImpl();
        this.chiTietLinhKienDao = new ChiTietLinhKienDaoImpl();
    }

    private void kiemTraQuyen() {
        VaiTro role = UserSession.getInstance().getVaiTro();
        if (role != VaiTro.TIEP_NHAN_VIEN && role != VaiTro.CHU_CUA_HANG) {
            throw new SecurityException("Bạn không có quyền tạo phiếu đổi trả.");
        }
    }

    /**
     * Tìm hóa đơn theo mã và kiểm tra điều kiện đổi trả:
     * - Phải là hóa đơn DA_THANH_TOAN
     * - Phải còn trong vòng 7 ngày kể từ ngày lập hóa đơn
     *
     * @return HoaDon nếu hợp lệ, ném exception nếu không đủ điều kiện
     */
    public HoaDon timHoaDonDuDieuKienDoiTra(String maHoaDon) {
        if (maHoaDon == null || maHoaDon.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mã hóa đơn cần đổi trả.");
        }

        HoaDon hd = hoaDonDao.timTheoMa(maHoaDon.trim());
        if (hd == null) {
            throw new IllegalArgumentException("Không tìm thấy hóa đơn \"" + maHoaDon + "\".");
        }
        if (hd.getTrangThaiThanhToan() != TrangThaiThanhToan.DA_THANH_TOAN) {
            throw new IllegalStateException("Hóa đơn chưa được thanh toán, không thể tạo phiếu đổi trả.");
        }

        // Kiểm tra 7 ngày
        if (hd.getNgayLap() != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(hd.getNgayLap());
            cal.add(Calendar.DAY_OF_YEAR, 7);
            Date hanDoiTra = cal.getTime();
            if (new Date().after(hanDoiTra)) {
                throw new IllegalStateException("Hóa đơn đã quá thời hạn đổi trả (tối đa 7 ngày từ ngày lập hóa đơn).");
            }
        }

        return hd;
    }

    /**
     * Lấy danh sách hóa đơn đủ điều kiện đổi trả (DA_THANH_TOAN và còn trong 7 ngày).
     */
    public List<HoaDon> layHoaDonDuDieuKienDoiTra() {
        return hoaDonDao.layHoaDonDuDieuKienDoiTra();
    }

    /**
     * Lấy chi tiết dịch vụ của hóa đơn (để chọn hạng mục đổi trả).
     */
    public List<ChiTietDichVu> layChiTietDichVu(String maPhieu) {
        if (maPhieu == null || maPhieu.isBlank()) return List.of();
        return chiTietDichVuDao.layTheoPhieu(maPhieu.trim());
    }

    /**
     * Lấy chi tiết linh kiện của hóa đơn (để chọn hạng mục đổi trả).
     */
    public List<ChiTietLinhKien> layChiTietLinhKien(String maPhieu) {
        if (maPhieu == null || maPhieu.isBlank()) return List.of();
        return chiTietLinhKienDao.layTheoPhieu(maPhieu.trim());
    }

    /**
     * Lập phiếu đổi trả mới.
     * Validate dữ liệu trước khi gọi DAO.
     *
     * @param hoaDon       hóa đơn gốc
     * @param lyDo         lý do đổi trả (bắt buộc)
     * @param dsChiTiet    danh sách chi tiết đổi trả (ít nhất 1 hạng mục)
     * @return mã phiếu đổi trả vừa sinh
     */
    public String lapPhieuDoiTra(HoaDon hoaDon, String lyDo, List<ChiTietDoiTra> dsChiTiet) {
        kiemTraQuyen();

        if (hoaDon == null || hoaDon.getMaHoaDon() == null) {
            throw new IllegalArgumentException("Vui lòng chọn hóa đơn cần đổi trả.");
        }
        if (lyDo == null || lyDo.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do đổi trả.");
        }
        if (dsChiTiet == null || dsChiTiet.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một hạng mục đổi trả.");
        }

        // Tái kiểm tra điều kiện đổi trả
        timHoaDonDuDieuKienDoiTra(hoaDon.getMaHoaDon());

        NhanVien nhanVien = UserSession.getInstance().getNhanVien();

        PhieuDoiTra pdt = new PhieuDoiTra();
        pdt.setHoaDonRaw(hoaDon);
        pdt.setNhanVienRaw(nhanVien);
        pdt.setNgayLapRaw(new Date());
        pdt.setLyDoDoiTraRaw(lyDo.trim());
        // soTienHoanTra được DB tính lại qua stored procedure
        pdt.setSoTienHoanTraRaw(0.0);

        String maPhieuDT = phieuDoiTraDao.lapPhieuDoiTra(pdt, dsChiTiet);
        if (maPhieuDT == null || maPhieuDT.isBlank()) {
            throw new RuntimeException("Lập phiếu đổi trả thất bại. Vui lòng thử lại.");
        }
        return maPhieuDT;
    }

    /**
     * Lấy danh sách phiếu đổi trả của tiếp nhận viên hiện tại.
     */
    public List<PhieuDoiTra> layDanhSachPhieuDoiTra() {
        String maNV = UserSession.getInstance().getNhanVien().getMaNV();
        return phieuDoiTraDao.layTheoTiepNhanVien(maNV);
    }

    /**
     * Tìm kiếm phiếu đổi trả theo từ khóa.
     */
    public List<PhieuDoiTra> timKiemPhieuDoiTra(String tuKhoa) {
        String maNV = UserSession.getInstance().getNhanVien().getMaNV();
        return phieuDoiTraDao.timKiem(tuKhoa, maNV);
    }

    /**
     * Lấy chi tiết phiếu đổi trả.
     */
    public List<ChiTietDoiTra> layChiTietPhieuDoiTra(String maPhieuDT) {
        if (maPhieuDT == null || maPhieuDT.isBlank()) return new ArrayList<>();
        return phieuDoiTraDao.layChiTiet(maPhieuDT.trim());
    }
}
