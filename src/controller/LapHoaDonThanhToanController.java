package controller;

import dao.HoaDonDao;
import dao.KhuyenMaiDao;
import dao.PhieuTiepNhanSuaChuaDao;
import dao.ChiTietDichVuDao;
import dao.ChiTietLinhKienDao;
import dao.impl.HoaDonDaoImpl;
import dao.impl.KhuyenMaiDaoImpl;
import dao.impl.PhieuTiepNhanSuaChuaDaoImpl;
import dao.impl.ChiTietDichVuDaoImpl;
import dao.impl.ChiTietLinhKienDaoImpl;
import entity.ChiTietDichVu;
import entity.ChiTietLinhKien;
import entity.HoaDon;
import entity.KhuyenMai;
import entity.PhieuTiepNhanSuaChua;
import enums.PhuongThucThanhToan;
import enums.TrangThaiThanhToan;
import enums.VaiTro;
import session.UserSession;

import java.util.List;

/**
 * Controller điều phối nghiệp vụ Lập hóa đơn và Thanh toán (UC014, UC015 - Tiếp nhận viên).
 * Tuân thủ kiến trúc: UI → Controller → DAO, không có SQL trong Controller.
 */
public class LapHoaDonThanhToanController {

    private final HoaDonDao hoaDonDao;
    private final KhuyenMaiDao khuyenMaiDao;
    private final PhieuTiepNhanSuaChuaDao phieuDao;
    private final ChiTietDichVuDao chiTietDichVuDao;
    private final ChiTietLinhKienDao chiTietLinhKienDao;

    public LapHoaDonThanhToanController() {
        this.hoaDonDao = new HoaDonDaoImpl();
        this.khuyenMaiDao = new KhuyenMaiDaoImpl();
        this.phieuDao = new PhieuTiepNhanSuaChuaDaoImpl();
        this.chiTietDichVuDao = new ChiTietDichVuDaoImpl();
        this.chiTietLinhKienDao = new ChiTietLinhKienDaoImpl();
    }

    private void kiemTraQuyen() {
        VaiTro role = UserSession.getInstance().getVaiTro();
        if (role != VaiTro.TIEP_NHAN_VIEN && role != VaiTro.CHU_CUA_HANG) {
            throw new SecurityException("Bạn không có quyền lập hóa đơn và thanh toán.");
        }
    }

    /**
     * Lấy danh sách phiếu sửa chữa HOAN_TAT và chưa có hóa đơn (đủ điều kiện lập HĐ).
     */
    public List<PhieuTiepNhanSuaChua> layDanhSachPhieuDuDieuKien() {
        return phieuDao.layDanhSachDuDieuKienLapHoaDon();
    }

    /**
     * Lấy danh sách khuyến mãi còn hiệu lực để hiển thị cho nhân viên chọn.
     */
    public List<KhuyenMai> layKhuyenMaiHieuLuc() {
        return khuyenMaiDao.layKhuyenMaiHieuLuc();
    }

    /**
     * Lấy tất cả khuyến mãi (để hiển thị combo chọn, kể cả hết hạn).
     */
    public List<KhuyenMai> layTatCaKhuyenMai() {
        return khuyenMaiDao.layTatCa();
    }

    /**
     * Tìm hóa đơn theo mã hóa đơn.
     */
    public HoaDon timHoaDonTheoMa(String maHoaDon) {
        if (maHoaDon == null || maHoaDon.isBlank()) return null;
        return hoaDonDao.timTheoMa(maHoaDon.trim());
    }

    /**
     * Tìm hóa đơn theo mã phiếu sửa chữa.
     */
    public HoaDon timHoaDonTheoPhieu(String maPhieu) {
        if (maPhieu == null || maPhieu.isBlank()) return null;
        return hoaDonDao.timTheoPhieu(maPhieu.trim());
    }

    /**
     * Lấy chi tiết dịch vụ của phiếu sửa chữa.
     */
    public List<ChiTietDichVu> layChiTietDichVu(String maPhieu) {
        if (maPhieu == null || maPhieu.isBlank()) return List.of();
        return chiTietDichVuDao.layTheoPhieu(maPhieu.trim());
    }

    /**
     * Lấy chi tiết linh kiện của phiếu sửa chữa.
     */
    public List<ChiTietLinhKien> layChiTietLinhKien(String maPhieu) {
        if (maPhieu == null || maPhieu.isBlank()) return List.of();
        return chiTietLinhKienDao.layTheoPhieu(maPhieu.trim());
    }

    /**
     * Lập hóa đơn mới từ phiếu sửa chữa, áp dụng khuyến mãi nếu có.
     * Gọi stored procedure sp_LapHoaDon qua DAO.
     *
     * @param maPhieu mã phiếu sửa chữa HOAN_TAT
     * @param maKM    mã khuyến mãi (null hoặc rỗng nếu không áp dụng)
     * @return mã hóa đơn vừa sinh
     */
    public String lapHoaDon(String maPhieu, String maKM) {
        kiemTraQuyen();
        if (maPhieu == null || maPhieu.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn phiếu sửa chữa.");
        }

        String maNV = UserSession.getInstance().getNhanVien().getMaNV();
        String maKMFinal = (maKM == null || maKM.isBlank()) ? null : maKM.trim();

        String maHD = hoaDonDao.lapHoaDon(maPhieu.trim(), maNV, maKMFinal);
        if (maHD == null || maHD.isBlank()) {
            throw new RuntimeException("Lập hóa đơn thất bại. Vui lòng kiểm tra lại phiếu sửa chữa và khuyến mãi.");
        }
        return maHD;
    }

    /**
     * Xác nhận thanh toán hóa đơn.
     *
     * @param maHoaDon      mã hóa đơn cần thanh toán
     * @param phuongThucTT  phương thức thanh toán (TIEN_MAT hoặc CHUYEN_KHOAN)
     * @param maGiaoDich    mã giao dịch (bắt buộc nếu là CHUYEN_KHOAN)
     */
    public boolean thanhToanHoaDon(String maHoaDon, PhuongThucThanhToan phuongThucTT, String maGiaoDich) {
        kiemTraQuyen();
        if (maHoaDon == null || maHoaDon.isBlank()) {
            throw new IllegalArgumentException("Mã hóa đơn không hợp lệ.");
        }
        if (phuongThucTT == null) {
            throw new IllegalArgumentException("Vui lòng chọn phương thức thanh toán.");
        }
        if (phuongThucTT == PhuongThucThanhToan.CHUYEN_KHOAN) {
            if (maGiaoDich == null || maGiaoDich.isBlank()) {
                throw new IllegalArgumentException("Thanh toán chuyển khoản bắt buộc phải nhập mã giao dịch.");
            }
        }

        // Kiểm tra hóa đơn tồn tại và chưa thanh toán
        HoaDon hd = hoaDonDao.timTheoMa(maHoaDon.trim());
        if (hd == null) {
            throw new IllegalArgumentException("Không tìm thấy hóa đơn \"" + maHoaDon + "\".");
        }
        if (hd.getTrangThaiThanhToan() == TrangThaiThanhToan.DA_THANH_TOAN) {
            throw new IllegalStateException("Hóa đơn này đã được thanh toán trước đó.");
        }

        String mgd = (maGiaoDich == null || maGiaoDich.isBlank()) ? null : maGiaoDich.trim();
        return hoaDonDao.thanhToanHoaDon(maHoaDon.trim(), phuongThucTT, mgd);
    }

    /**
     * Tìm kiếm hóa đơn theo từ khóa và trạng thái.
     */
    public List<HoaDon> timKiemHoaDon(String tuKhoa, TrangThaiThanhToan trangThai) {
        return hoaDonDao.timKiem(tuKhoa, trangThai);
    }

    /**
     * Lấy toàn bộ hóa đơn chưa thanh toán (để hiển thị danh sách cần thu tiền).
     */
    public List<HoaDon> layDanhSachChuaThanhToan() {
        return hoaDonDao.timTheoTrangThai(TrangThaiThanhToan.CHUA_THANH_TOAN);
    }

    /**
     * Lấy toàn bộ hóa đơn.
     */
    public List<HoaDon> layTatCaHoaDon() {
        return hoaDonDao.layTatCa();
    }
}
