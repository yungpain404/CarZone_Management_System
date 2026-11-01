package controller;

import dao.LinhKienDAO;
import dao.TraCuuDAO;
import entity.LinhKien;
import enums.VaiTro;
import session.UserSession;

import java.util.List;

/** Điều phối tra cứu linh kiện, khách hàng và lịch sử, chỉ dành cho Chủ cửa hàng. */
public class TraCuuController {
    private final LinhKienDAO linhKienDao;
    private final TraCuuDAO traCuuDao;

    public TraCuuController() { this(new LinhKienDAO(), new TraCuuDAO()); }
    public TraCuuController(LinhKienDAO linhKienDao, TraCuuDAO traCuuDao) {
        this.linhKienDao = linhKienDao; this.traCuuDao = traCuuDao;
    }

    public List<LinhKien> timLinhKien(String tuKhoa, String trangThai) {
        yeuCauChuCuaHang();
        Boolean dangKinhDoanh = switch (trangThai) {
            case "Đang kinh doanh" -> true; case "Ngừng kinh doanh" -> false; default -> null;
        };
        Boolean conTon = switch (trangThai) {
            case "Còn hàng" -> true; case "Hết hàng" -> false; default -> null;
        };
        return linhKienDao.timKiem(tuKhoa == null ? null : tuKhoa.trim(), dangKinhDoanh, conTon);
    }

    public List<TraCuuDAO.KhachHangResult> timKhachHang(String tuKhoa) {
        yeuCauChuCuaHang(); yeuCauTuKhoa(tuKhoa); return traCuuDao.timKhachHang(tuKhoa.trim());
    }

    public List<TraCuuDAO.XeResult> layXeKhachHang(String maKH) {
        yeuCauChuCuaHang(); return traCuuDao.layXeKhachHang(maKH);
    }

    public List<TraCuuDAO.LichSuResult> timLichSu(String tuKhoa) {
        yeuCauChuCuaHang(); yeuCauTuKhoa(tuKhoa); return traCuuDao.timLichSu(tuKhoa.trim());
    }

    public List<TraCuuDAO.HangMucResult> layHangMucPhieu(String maPhieu) {
        yeuCauChuCuaHang(); return traCuuDao.layHangMucPhieu(maPhieu);
    }

    private static void yeuCauTuKhoa(String tuKhoa) {
        if (tuKhoa == null || tuKhoa.isBlank()) throw new IllegalArgumentException("Vui lòng nhập từ khóa tra cứu.");
    }
    private static void yeuCauChuCuaHang() {
        if (UserSession.getInstance().getVaiTro() != VaiTro.CHU_CUA_HANG)
            throw new SecurityException("Chỉ Chủ cửa hàng được sử dụng chức năng tra cứu này.");
    }
}
