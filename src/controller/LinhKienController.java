package controller;

import dao.LinhKienDAO;
import entity.LinhKien;
import enums.VaiTro;
import session.UserSession;

import java.util.List;

/** Điều phối tra cứu và cập nhật danh mục linh kiện, chỉ dành cho Chủ cửa hàng. */
public class LinhKienController {
    private final LinhKienDAO dao;

    public LinhKienController() { this(new LinhKienDAO()); }
    public LinhKienController(LinhKienDAO dao) { this.dao = dao; }

    public List<LinhKien> timKiem(String tuKhoa, Boolean dangKinhDoanh, Boolean conTon) {
        yeuCauChuCuaHang();
        return dao.timKiem(tuKhoa == null ? null : tuKhoa.trim(), dangKinhDoanh, conTon);
    }

    public LinhKien timTheoMa(String maLK) {
        yeuCauChuCuaHang();
        if (maLK == null || !maLK.matches("^LK\\d{5,}$")) throw new IllegalArgumentException("Mã linh kiện không hợp lệ.");
        LinhKien result = dao.timTheoMa(maLK);
        if (result == null) throw new IllegalArgumentException("Không tìm thấy linh kiện " + maLK + ".");
        return result;
    }

    public String layMaTiepTheo() {
        yeuCauChuCuaHang();
        return dao.layMaTiepTheo();
    }

    public LinhKien them(String tenLK, double donGia, String donViTinh, int soLuongTon, boolean dangKinhDoanh) {
        yeuCauChuCuaHang();
        LinhKien linhKien = taoDuLieuHopLe(null, tenLK, donGia, donViTinh, dangKinhDoanh);
        linhKien.setSoLuongTon(soLuongTon);
        return dao.them(linhKien);
    }

    public LinhKien capNhat(String maLK, String tenLK, double donGia, String donViTinh, boolean dangKinhDoanh) {
        yeuCauChuCuaHang();
        LinhKien linhKien = taoDuLieuHopLe(maLK, tenLK, donGia, donViTinh, dangKinhDoanh);
        if (!dao.capNhat(linhKien)) throw new IllegalArgumentException("Linh kiện không còn tồn tại trong cơ sở dữ liệu.");
        return dao.timTheoMa(maLK);
    }

    public void doiTrangThai(String maLK, boolean dangKinhDoanh) {
        yeuCauChuCuaHang(); timTheoMa(maLK);
        if (!dao.capNhatTrangThai(maLK, dangKinhDoanh)) throw new IllegalArgumentException("Linh kiện không còn tồn tại.");
    }

    public void xoa(String maLK) {
        yeuCauChuCuaHang(); timTheoMa(maLK);
        if (!dao.xoa(maLK)) throw new IllegalArgumentException("Linh kiện không còn tồn tại.");
    }

    private static LinhKien taoDuLieuHopLe(String maLK, String tenLK, double donGia, String donViTinh,
                                          boolean dangKinhDoanh) {
        LinhKien linhKien = new LinhKien();
        if (maLK != null) linhKien.setMaLK(maLK);
        linhKien.setTenLK(tenLK == null ? null : tenLK.trim());
        linhKien.setDonGia(donGia);
        linhKien.setDonViTinh(donViTinh == null ? null : donViTinh.trim());
        linhKien.setDangKinhDoanh(dangKinhDoanh);
        return linhKien;
    }

    private static void yeuCauChuCuaHang() {
        if (UserSession.getInstance().getVaiTro() != VaiTro.CHU_CUA_HANG) {
            throw new SecurityException("Chỉ Chủ cửa hàng được quản lý linh kiện.");
        }
    }
}
