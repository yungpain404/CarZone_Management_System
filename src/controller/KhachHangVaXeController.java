package controller;

import dao.KhachHangDAO;
import dao.XeDAO;
import dao.KhachHangDAO;
import dao.XeDAO;
import entity.KhachHang;
import entity.Xe;
import enums.VaiTro;
import session.UserSession;

import java.util.List;

/**
 * Controller điều phối nghiệp vụ Quản lý hồ sơ Khách hàng và Xe (UC016 - Tiếp nhận viên).
 */
public class KhachHangVaXeController {

    private final KhachHangDAO khachHangDao;
    private final XeDAO xeDao;

    public KhachHangVaXeController() {
        this.khachHangDao = new KhachHangDAO();
        this.xeDao = new XeDAO();
    }

    public KhachHangVaXeController(KhachHangDAO khachHangDao, XeDAO xeDao) {
        this.khachHangDao = khachHangDao;
        this.xeDao = xeDao;
    }

    private void kiemTraQuyen() {
        VaiTro role = UserSession.getInstance().getVaiTro();
        if (role != VaiTro.TIEP_NHAN_VIEN && role != VaiTro.CHU_CUA_HANG) {
            throw new SecurityException("Bạn không có quyền thực hiện thao tác quản lý khách hàng và xe.");
        }
    }

    public List<KhachHang> layDanhSachKhachHang() {
        return khachHangDao.layTatCa();
    }

    public List<KhachHang> timKiemKhachHang(String tuKhoa) {
        return khachHangDao.timKiem(tuKhoa);
    }

    public KhachHang timKhachHangTheoMa(String maKH) {
        return khachHangDao.timTheoMa(maKH);
    }

    public KhachHang timKhachHangTheoSoDienThoai(String sdt) {
        return khachHangDao.timTheoSoDienThoai(sdt);
    }

    public List<Xe> layDanhSachXeTheoKhachHang(String maKH) {
        if (maKH == null || maKH.isBlank()) return List.of();
        return xeDao.layTheoKhachHang(maKH);
    }

    public List<Xe> timKiemXe(String tuKhoa) {
        return xeDao.timKiem(tuKhoa);
    }

    public String luuKhachHang(KhachHang kh) {
        kiemTraQuyen();
        if (kh == null) throw new IllegalArgumentException("Dữ liệu khách hàng không được để trống.");

        if (kh.getHoTen() == null || kh.getHoTen().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập họ tên khách hàng.");
        }
        if (kh.getSoDienThoai() == null || !kh.getSoDienThoai().matches("0\\d{9}")) {
            throw new IllegalArgumentException("Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0.");
        }

        if (kh.getMaKH() == null || kh.getMaKH().isBlank()) {
            // Thêm mới
            return khachHangDao.them(kh);
        } else {
            // Cập nhật
            boolean ok = khachHangDao.capNhat(kh);
            if (!ok) throw new RuntimeException("Cập nhật thông tin khách hàng thất bại.");
            return kh.getMaKH();
        }
    }

    public boolean luuXe(Xe xe) {
        kiemTraQuyen();
        if (xe == null) throw new IllegalArgumentException("Dữ liệu xe không được để trống.");

        if (xe.getBienSoXe() == null || xe.getBienSoXe().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập biển số xe.");
        }
        if (xe.getBienSoXe().length() < 6 || xe.getBienSoXe().length() > 15) {
            throw new IllegalArgumentException("Biển số xe phải từ 6 đến 15 ký tự.");
        }
        if (xe.getChuXe() == null || xe.getChuXe().getMaKH() == null) {
            throw new IllegalArgumentException("Xe bắt buộc phải gắn với một khách hàng chủ sở hữu.");
        }
        if (xe.getHangXe() == null || xe.getHangXe().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập hãng sản xuất xe.");
        }

        Xe xeCu = xeDao.timTheoBienSo(xe.getBienSoXe());
        if (xeCu == null) {
            return xeDao.them(xe);
        } else {
            return xeDao.capNhat(xe);
        }
    }
}
