package controller;

import dao.*;
import dao.impl.*;
import entity.*;
import enums.TrangThaiPhieu;
import enums.VaiTro;
import session.UserSession;

import java.util.Date;
import java.util.List;

/**
 * Controller điều phối nghiệp vụ Quản lý & Lập phiếu tiếp nhận – sửa chữa (UC010, UC011 - Tiếp nhận viên).
 */
public class LapPhieuTiepNhanController {

    private final KhachHangDao khachHangDao;
    private final XeDao xeDao;
    private final DichVuDao dichVuDao;
    private final NhanVienDao nhanVienDao;
    private final PhieuTiepNhanSuaChuaDao phieuDao;
    private final ChiTietDichVuDao chiTietDichVuDao;
    private final ChiTietLinhKienDao chiTietLinhKienDao;

    public LapPhieuTiepNhanController() {
        this.khachHangDao = new KhachHangDaoImpl();
        this.xeDao = new XeDaoImpl();
        this.dichVuDao = new DichVuDaoImpl();
        this.nhanVienDao = new NhanVienDaoImpl();
        this.phieuDao = new PhieuTiepNhanSuaChuaDaoImpl();
        this.chiTietDichVuDao = new ChiTietDichVuDaoImpl();
        this.chiTietLinhKienDao = new ChiTietLinhKienDaoImpl();
    }

    private void kiemTraQuyen() {
        VaiTro role = UserSession.getInstance().getVaiTro();
        if (role != VaiTro.TIEP_NHAN_VIEN && role != VaiTro.CHU_CUA_HANG) {
            throw new SecurityException("Chỉ tiếp nhận viên mới có quyền quản lý và lập phiếu tiếp nhận sửa chữa.");
        }
    }

    public List<PhieuTiepNhanSuaChua> layDanhSachPhieu() {
        return phieuDao.layTatCa();
    }

    public List<PhieuTiepNhanSuaChua> timKiemPhieu(String tuKhoa, TrangThaiPhieu trangThai) {
        return phieuDao.timKiem(tuKhoa, null, trangThai);
    }

    public PhieuTiepNhanSuaChua layPhieuTheoMa(String maPhieu) {
        return phieuDao.timTheoMa(maPhieu);
    }

    public List<ChiTietDichVu> layChiTietDichVu(String maPhieu) {
        if (maPhieu == null || maPhieu.isBlank()) return List.of();
        return chiTietDichVuDao.layTheoPhieu(maPhieu);
    }

    public List<ChiTietLinhKien> layChiTietLinhKien(String maPhieu) {
        if (maPhieu == null || maPhieu.isBlank()) return List.of();
        return chiTietLinhKienDao.layTheoPhieu(maPhieu);
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

    public String lapPhieu(String bienSoXe, String maKTV, String yeuCauKhachHang, String tinhTrangXe, String ghiChuTiepNhan, List<ChiTietDichVu> dsDichVu) {
        if (yeuCauKhachHang == null || yeuCauKhachHang.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập nội dung yêu cầu của khách hàng.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(yeuCauKhachHang.trim());
        if (tinhTrangXe != null && !tinhTrangXe.isBlank()) {
            sb.append(" | [Hiện trạng]: ").append(tinhTrangXe.trim());
        }
        if (ghiChuTiepNhan != null && !ghiChuTiepNhan.isBlank()) {
            sb.append(" | [Ghi chú]: ").append(ghiChuTiepNhan.trim());
        }

        String yeuCauBanDau = sb.toString();
        if (yeuCauBanDau.length() > 500) {
            yeuCauBanDau = yeuCauBanDau.substring(0, 500);
        }

        return lapPhieu(bienSoXe, maKTV, yeuCauBanDau, dsDichVu);
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

    public boolean capNhatPhieu(String maPhieu, String bienSoXe, String maKTV, String yeuCauKhachHang, String tinhTrangXe, String ghiChuTiepNhan, List<ChiTietDichVu> dsDichVu) {
        if (yeuCauKhachHang == null || yeuCauKhachHang.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập nội dung yêu cầu của khách hàng.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(yeuCauKhachHang.trim());
        if (tinhTrangXe != null && !tinhTrangXe.isBlank()) {
            sb.append(" | [Hiện trạng]: ").append(tinhTrangXe.trim());
        }
        if (ghiChuTiepNhan != null && !ghiChuTiepNhan.isBlank()) {
            sb.append(" | [Ghi chú]: ").append(ghiChuTiepNhan.trim());
        }

        String yeuCauBanDau = sb.toString();
        if (yeuCauBanDau.length() > 500) {
            yeuCauBanDau = yeuCauBanDau.substring(0, 500);
        }

        return capNhatPhieu(maPhieu, bienSoXe, maKTV, yeuCauBanDau, dsDichVu);
    }

    public boolean capNhatPhieu(String maPhieu, String bienSoXe, String maKTV, String yeuCauBanDau, List<ChiTietDichVu> dsDichVu) {
        kiemTraQuyen();

        if (maPhieu == null || maPhieu.isBlank()) {
            throw new IllegalArgumentException("Mã phiếu không hợp lệ.");
        }
        if (bienSoXe == null || bienSoXe.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn xe của khách hàng cần tiếp nhận.");
        }
        if (yeuCauBanDau == null || yeuCauBanDau.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập nội dung yêu cầu của khách hàng.");
        }

        PhieuTiepNhanSuaChua phieuCu = phieuDao.timTheoMa(maPhieu.trim());
        if (phieuCu == null) {
            throw new IllegalArgumentException("Không tìm thấy phiếu " + maPhieu);
        }
        if (phieuCu.getTrangThai() != TrangThaiPhieu.CHO_SUA) {
            throw new IllegalStateException("Chỉ được phép sửa phiếu khi ở trạng thái Chờ sửa chữa (CHO_SUA).");
        }

        Xe xe = xeDao.timTheoBienSo(bienSoXe.trim());
        if (xe == null) {
            throw new IllegalArgumentException("Không tìm thấy thông tin xe " + bienSoXe + " trong hệ thống.");
        }

        PhieuTiepNhanSuaChua phieu = new PhieuTiepNhanSuaChua(maPhieu.trim());
        phieu.setXeRaw(xe);
        phieu.setYeuCauBanDauRaw(yeuCauBanDau.trim());
        phieu.setTrangThaiRaw(TrangThaiPhieu.CHO_SUA);

        if (maKTV != null && !maKTV.isBlank()) {
            NhanVien ktv = nhanVienDao.timTheoMa(maKTV.trim());
            if (ktv != null) {
                if (phieuCu.getNhanVienLap() != null && ktv.getMaNV().equals(phieuCu.getNhanVienLap().getMaNV())) {
                    throw new IllegalArgumentException("Kỹ thuật viên chính phải khác với nhân viên tiếp nhận.");
                }
                phieu.setNhanVienSuaChuaRaw(ktv);
            }
        }

        return phieuDao.capNhatPhieu(phieu, dsDichVu);
    }
}
