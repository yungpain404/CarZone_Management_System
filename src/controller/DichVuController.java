package controller;

import dao.DichVuDAO;
import entity.DichVu;
import enums.VaiTro;
import session.UserSession;

import java.util.List;

/** Điều phối tra cứu và cập nhật dịch vụ, chỉ dành cho Chủ cửa hàng. */
public class DichVuController {
    private final DichVuDAO dao;

    public DichVuController() {
        this(new DichVuDAO());
    }

    public DichVuController(DichVuDAO dao) {
        this.dao = dao;
    }

    public List<DichVu> timKiem(String tuKhoa, Boolean dangCungCap) {
        yeuCauChuCuaHang();
        return dao.timKiem(tuKhoa == null ? null : tuKhoa.trim(), dangCungCap);
    }

    public DichVu timTheoMa(String maDV) {
        yeuCauChuCuaHang();
        if (maDV == null || !maDV.matches("^DV\\d{5,}$")) {
            throw new IllegalArgumentException("Mã dịch vụ không hợp lệ.");
        }
        DichVu result = dao.timTheoMa(maDV);
        if (result == null) throw new IllegalArgumentException("Không tìm thấy dịch vụ " + maDV + ".");
        return result;
    }

    public String layMaTiepTheo() {
        yeuCauChuCuaHang();
        return dao.layMaTiepTheo();
    }

    public DichVu them(String tenDV, double donGia, String donViTinh, String moTa, boolean dangCungCap) {
        yeuCauChuCuaHang();
        return dao.them(taoDuLieuHopLe(null, tenDV, donGia, donViTinh, moTa, dangCungCap));
    }

    public DichVu capNhat(String maDV, String tenDV, double donGia, String donViTinh, String moTa, boolean dangCungCap) {
        yeuCauChuCuaHang();
        DichVu dichVu = taoDuLieuHopLe(maDV, tenDV, donGia, donViTinh, moTa, dangCungCap);
        if (!dao.capNhat(dichVu)) throw new IllegalArgumentException("Dịch vụ không còn tồn tại trong cơ sở dữ liệu.");
        return dao.timTheoMa(maDV);
    }

    public void doiTrangThai(String maDV, boolean dangCungCap) {
        yeuCauChuCuaHang();
        timTheoMa(maDV);
        if (!dao.capNhatTrangThai(maDV, dangCungCap)) throw new IllegalArgumentException("Dịch vụ không còn tồn tại.");
    }

    public void xoa(String maDV) {
        yeuCauChuCuaHang();
        timTheoMa(maDV);
        if (!dao.xoa(maDV)) throw new IllegalArgumentException("Dịch vụ không còn tồn tại.");
    }

    private static DichVu taoDuLieuHopLe(String maDV, String tenDV, double donGia, String donViTinh,
                                         String moTa, boolean dangCungCap) {
        DichVu dichVu = new DichVu();
        if (maDV != null) dichVu.setMaDV(maDV);
        dichVu.setTenDV(tenDV == null ? null : tenDV.trim());
        dichVu.setDonGia(donGia);
        dichVu.setDonViTinh(donViTinh == null ? null : donViTinh.trim());
        dichVu.setMoTa(moTa == null || moTa.isBlank() ? null : moTa.trim());
        dichVu.setDangCungCap(dangCungCap);
        return dichVu;
    }

    private static void yeuCauChuCuaHang() {
        if (UserSession.getInstance().getVaiTro() != VaiTro.CHU_CUA_HANG) {
            throw new SecurityException("Chỉ Chủ cửa hàng được quản lý dịch vụ.");
        }
    }
}
