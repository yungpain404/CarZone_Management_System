package controller;

import dao.TaiKhoanDAO;
import entity.NhanVien;
import entity.TaiKhoan;
import enums.VaiTro;
import session.UserSession;

import java.util.List;

/** Điều phối tra cứu và quản lý tài khoản, chỉ dành cho Chủ cửa hàng. */
public class QuanLyTaiKhoanController {
    private final TaiKhoanDAO dao;

    public QuanLyTaiKhoanController() { this(new TaiKhoanDAO()); }
    public QuanLyTaiKhoanController(TaiKhoanDAO dao) { this.dao = dao; }

    public List<NhanVien> timKiemHoSo(String tuKhoa) {
        yeuCauChuCuaHang();
        if (tuKhoa == null || tuKhoa.isBlank()) return List.of();
        return dao.timKiemHoSo(tuKhoa.trim());
    }

    public List<NhanVien> layNhanVienChuaCoTaiKhoan() {
        yeuCauChuCuaHang();
        return dao.layNhanVienChuaCoTaiKhoan();
    }

    public String taoTaiKhoan(String maNV, String matKhau) {
        yeuCauChuCuaHang();
        if (maNV == null || maNV.isBlank()) throw new IllegalArgumentException("Vui lòng chọn hồ sơ nhân viên.");
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setMatKhau(matKhau);
        return dao.taoTaiKhoan(maNV, taiKhoan.getMatKhau());
    }

    public void capNhatMatKhau(String tenDangNhap, String matKhau) {
        yeuCauChuCuaHang();
        if (tenDangNhap == null || tenDangNhap.isBlank()) throw new IllegalArgumentException("Tài khoản không hợp lệ.");
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setMatKhau(matKhau);
        if (!dao.capNhatMatKhau(tenDangNhap, taiKhoan.getMatKhau()))
            throw new IllegalStateException("Tài khoản không còn tồn tại.");
    }

    public void xoaTaiKhoan(String tenDangNhap) {
        yeuCauChuCuaHang();
        if (tenDangNhap == null || tenDangNhap.isBlank() || !dao.xoaTaiKhoan(tenDangNhap))
            throw new IllegalStateException("Tài khoản không còn tồn tại.");
    }

    private static void yeuCauChuCuaHang() {
        if (UserSession.getInstance().getVaiTro() != VaiTro.CHU_CUA_HANG)
            throw new SecurityException("Chỉ Chủ cửa hàng được quản lý tài khoản.");
    }
}
