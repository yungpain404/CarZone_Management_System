package controller;

import dao.DichVuDao;
import dao.KhachHangDao;
import dao.NhanVienDao;
import dao.PhieuTiepNhanSuaChuaDao;
import dao.XeDao;
import dao.impl.DichVuDaoImpl;
import dao.impl.KhachHangDaoImpl;
import dao.impl.NhanVienDaoImpl;
import dao.impl.PhieuTiepNhanSuaChuaDaoImpl;
import dao.impl.XeDaoImpl;
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

    private final KhachHangDao khachHangDao;
    private final XeDao xeDao;
    private final DichVuDao dichVuDao;
    private final NhanVienDao nhanVienDao;
    private final PhieuTiepNhanSuaChuaDao phieuDao;

    public LapPhieuTiepNhanController() {
        this.khachHangDao = new KhachHangDaoImpl();
        this.xeDao = new XeDaoImpl();
        this.dichVuDao = new DichVuDaoImpl();
        this.nhanVienDao = new NhanVienDaoImpl();
        this.phieuDao = new PhieuTiepNhanSuaChuaDaoImpl();
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
