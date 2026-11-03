package controller;

import dao.NhanVienDAO;
import entity.NhanVien;
import enums.VaiTro;
import session.UserSession;

import java.time.LocalDate;
import java.util.List;

/** Điều phối tra cứu và cập nhật hồ sơ nhân viên, chỉ dành cho Chủ cửa hàng. */
public class NhanVienController {
    private final NhanVienDAO dao;

    public NhanVienController() {
        this(new NhanVienDAO());
    }

    public NhanVienController(NhanVienDAO dao) {
        this.dao = dao;
    }

    public List<NhanVien> timKiem(String tuKhoa, VaiTro vaiTro) {
        yeuCauChuCuaHang();
        return dao.timKiem(tuKhoa, vaiTro);
    }

    public NhanVien timTheoMa(String maNV) {
        yeuCauChuCuaHang();
        if (maNV == null || !maNV.matches("^(CCH|TNV|KTV)\\d{3}$")) {
            throw new IllegalArgumentException("Mã nhân viên không hợp lệ.");
        }
        NhanVien nhanVien = dao.timTheoMa(maNV);
        if (nhanVien == null) throw new IllegalArgumentException("Không tìm thấy hồ sơ " + maNV + ".");
        return nhanVien;
    }

    public NhanVien them(String hoTen, LocalDate ngaySinh, String soDienThoai, String cccd,
                         String diaChi, LocalDate ngayVaoLam, VaiTro vaiTro) {
        yeuCauChuCuaHang();
        NhanVien nhanVien = taoHoSo(null, hoTen, ngaySinh, soDienThoai, cccd, diaChi, ngayVaoLam, vaiTro);
        if (!dao.them(nhanVien)) throw new IllegalStateException("Không thể tạo hồ sơ nhân viên.");
        return dao.timTheoMa(nhanVien.getMaNV());
    }

    public NhanVien capNhat(String maNV, String hoTen, LocalDate ngaySinh, String soDienThoai, String cccd,
                            String diaChi, LocalDate ngayVaoLam) {
        yeuCauChuCuaHang();
        NhanVien hienTai = timTheoMa(maNV);
        NhanVien capNhat = taoHoSo(maNV, hoTen, ngaySinh, soDienThoai, cccd, diaChi,
                ngayVaoLam, hienTai.getVaiTro());
        if (!dao.capNhat(capNhat)) throw new IllegalStateException("Hồ sơ nhân viên không còn tồn tại.");
        return dao.timTheoMa(maNV);
    }

    private static NhanVien taoHoSo(String maNV, String hoTen, LocalDate ngaySinh, String soDienThoai,
                                    String cccd, String diaChi, LocalDate ngayVaoLam, VaiTro vaiTro) {
        NhanVien nhanVien = new NhanVien();
        if (maNV != null) nhanVien.setMaNV(maNV);
        nhanVien.setVaiTro(vaiTro);
        nhanVien.setHoTen(hoTen == null ? null : hoTen.trim());
        nhanVien.setNgaySinh(ngaySinh);
        nhanVien.setSoDienThoai(soDienThoai == null ? null : soDienThoai.trim());
        nhanVien.setCCCD(cccd == null ? null : cccd.trim());
        nhanVien.setDiaChi(diaChi == null || diaChi.isBlank() ? null : diaChi.trim());
        nhanVien.setNgayVaoLam(ngayVaoLam);
        return nhanVien;
    }

    private static void yeuCauChuCuaHang() {
        if (UserSession.getInstance().getVaiTro() != VaiTro.CHU_CUA_HANG) {
            throw new SecurityException("Chỉ Chủ cửa hàng được quản lý hồ sơ nhân viên.");
        }
    }
}
