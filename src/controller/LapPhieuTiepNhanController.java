package controller;

import dao.DichVuDAO;
import dao.KhachHangDAO;
import dao.NhanVienDAO;
import dao.PhieuTiepNhanSuaChuaDAO;
import dao.XeDAO;
import entity.*;
import enums.TrangThaiPhieu;
import enums.VaiTro;
import session.UserSession;

import java.util.Date;
import java.util.List;

/**
 * Controller điều phối nghiệp vụ Lập phiếu tiếp nhận – sửa chữa (UC010 - Tiếp nhận viên).
 */
public class LapPhieuTiepNhanController {

    private final KhachHangDAO khachHangDao;
    private final XeDAO xeDao;
    private final DichVuDAO dichVuDao;
    private final NhanVienDAO nhanVienDao;
    private final PhieuTiepNhanSuaChuaDAO phieuDao;

    public LapPhieuTiepNhanController() {
        this.khachHangDao = new KhachHangDAO();
        this.xeDao = new XeDAO();
        this.dichVuDao = new DichVuDAO();
        this.nhanVienDao = new NhanVienDAO();
        this.phieuDao = new PhieuTiepNhanSuaChuaDAO();
    }

    private void kiemTraQuyen() {
        VaiTro role = UserSession.getInstance().getVaiTro();
        if (role != VaiTro.TIEP_NHAN_VIEN && role != VaiTro.CHU_CUA_HANG) {
            throw new SecurityException("Chỉ tiếp nhận viên mới có quyền lập phiếu tiếp nhận sửa chữa.");
        }
    }

    public List<KhachHang> layDanhSachKhachHang() {
        return khachHangDao.layTatCa();
    }

    public List<Xe> layDanhSachXeTheoKhachHang(String maKH) {
        if (maKH == null || maKH.isBlank()) return List.of();
        return xeDao.layTheoKhachHang(maKH);
    }

    public List<DichVu> layDanhSachDichVuDangCungCap() {
        return dichVuDao.layTatCaDangCungCap();
    }

    public List<NhanVien> layDanhSachKyThuatVien() {
        return nhanVienDao.timTheoVaiTro(VaiTro.KY_THUAT_VIEN);
    }

    public String lapPhieu(String bienSoXe, String maKTV, String yeuCauBanDau, List<ChiTietDichVu> dsDichVu) {
        kiemTraQuyen();

        if (bienSoXe == null || bienSoXe.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn xe của khách hàng cần tiếp nhận.");
        }
        if (yeuCauBanDau == null || yeuCauBanDau.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập nội dung yêu cầu của khách hàng.");
        }

        Xe xe = xeDao.timTheoBienSo(bienSoXe);
        if (xe == null) {
            throw new IllegalArgumentException("Không tìm thấy thông tin xe " + bienSoXe + " trong hệ thống.");
        }

        NhanVien nvTiepNhan = UserSession.getInstance().getNhanVien();
        if (nvTiepNhan == null || nvTiepNhan.getMaNV() == null) {
            throw new IllegalStateException("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        }

        PhieuTiepNhanSuaChua phieu = new PhieuTiepNhanSuaChua();
        phieu.setXeRaw(xe);
        phieu.setNhanVienLapRaw(nvTiepNhan);
        phieu.setNgayGioTiepNhanRaw(new Date());
        phieu.setYeuCauBanDauRaw(yeuCauBanDau.trim());
        phieu.setTrangThaiRaw(TrangThaiPhieu.CHO_SUA);

        if (maKTV != null && !maKTV.isBlank()) {
            NhanVien ktv = nhanVienDao.timTheoMa(maKTV);
            if (ktv != null) {
                if (ktv.getMaNV().equals(nvTiepNhan.getMaNV())) {
                    throw new IllegalArgumentException("Kỹ thuật viên chính phải khác với nhân viên tiếp nhận.");
                }
                phieu.setNhanVienSuaChuaRaw(ktv);
            }
        }

        // Gọi DAO gọi stored procedure sinh mã và lưu
        return phieuDao.lapPhieuTiepNhan(phieu, dsDichVu);
    }
}
