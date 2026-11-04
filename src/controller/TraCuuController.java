package controller;

import dao.DichVuDAO;
import dao.LinhKienDAO;
import dao.TraCuuDAO;
import entity.DichVu;
import entity.LinhKien;
import enums.VaiTro;
import session.UserSession;

import java.util.List;

/**
 * Điều phối tra cứu dùng chung cho cả 3 vai trò với phân quyền:
 * - Chủ cửa hàng: Tra cứu đủ 5 chức năng (Dịch vụ, Linh kiện, Khách hàng, Lịch sử sửa chữa, Lịch sử tiếp nhận).
 * - Tiếp nhận viên: Không tra cứu được linh kiện và lịch sử sửa chữa (tra cứu: Dịch vụ, Khách hàng, Lịch sử tiếp nhận).
 * - Kỹ thuật viên: Không xem được khách hàng và lịch sử tiếp nhận (tra cứu: Dịch vụ, Linh kiện, Lịch sử sửa chữa).
 */
public class TraCuuController {
    private final DichVuDAO dichVuDao;
    private final LinhKienDAO linhKienDao;
    private final TraCuuDAO traCuuDao;

    public TraCuuController() {
        this(new DichVuDAO(), new LinhKienDAO(), new TraCuuDAO());
    }

    public TraCuuController(DichVuDAO dichVuDao, LinhKienDAO linhKienDao, TraCuuDAO traCuuDao) {
        this.dichVuDao = dichVuDao;
        this.linhKienDao = linhKienDao;
        this.traCuuDao = traCuuDao;
    }

    /** Tra cứu dịch vụ: dùng chung cho cả 3 vai trò (CCH, TNV, KTV). */
    public List<DichVu> timDichVu(String tuKhoa, String trangThai) {
        Boolean dangCungCap = switch (trangThai == null ? "Tất cả" : trangThai) {
            case "Đang cung cấp" -> true;
            case "Ngừng cung cấp" -> false;
            default -> null;
        };
        return dichVuDao.timKiem(tuKhoa == null ? null : tuKhoa.trim(), dangCungCap);
    }

    /** Tra cứu linh kiện: CHU_CUA_HANG và KY_THUAT_VIEN (TIEP_NHAN_VIEN không được truy cập). */
    public List<LinhKien> timLinhKien(String tuKhoa, String trangThai) {
        kiemTraQuyenLinhKien();
        Boolean dangKinhDoanh = switch (trangThai == null ? "Tất cả" : trangThai) {
            case "Đang kinh doanh" -> true;
            case "Ngừng kinh doanh" -> false;
            default -> null;
        };
        Boolean conTon = switch (trangThai == null ? "Tất cả" : trangThai) {
            case "Còn hàng" -> true;
            case "Hết hàng" -> false;
            default -> null;
        };
        return linhKienDao.timKiem(tuKhoa == null ? null : tuKhoa.trim(), dangKinhDoanh, conTon);
    }

    /** Tra cứu khách hàng: CHU_CUA_HANG và TIEP_NHAN_VIEN (KY_THUAT_VIEN không được truy cập). */
    public List<TraCuuDAO.KhachHangResult> timKhachHang(String tuKhoa) {
        kiemTraQuyenKhachHang();
        yeuCauTuKhoa(tuKhoa);
        return traCuuDao.timKhachHang(tuKhoa.trim());
    }

    /** Đọc xe của khách hàng: CHU_CUA_HANG và TIEP_NHAN_VIEN. */
    public List<TraCuuDAO.XeResult> layXeKhachHang(String maKH) {
        kiemTraQuyenKhachHang();
        return traCuuDao.layXeKhachHang(maKH);
    }

    /** Tra cứu lịch sử sửa chữa: CHU_CUA_HANG và KY_THUAT_VIEN (TIEP_NHAN_VIEN không được truy cập). */
    public List<TraCuuDAO.LichSuResult> timLichSu(String tuKhoa) {
        kiemTraQuyenLichSuSuaChua();
        yeuCauTuKhoa(tuKhoa);
        return traCuuDao.timLichSu(tuKhoa.trim());
    }

    /** Đọc hạng mục phiếu sửa chữa: CHU_CUA_HANG và KY_THUAT_VIEN. */
    public List<TraCuuDAO.HangMucResult> layHangMucPhieu(String maPhieu) {
        kiemTraQuyenLichSuSuaChua();
        return traCuuDao.layHangMucPhieu(maPhieu);
    }

    /** Tra cứu lịch sử tiếp nhận: CHU_CUA_HANG và TIEP_NHAN_VIEN (KY_THUAT_VIEN không được truy cập). */
    public List<TraCuuDAO.LichSuTiepNhanResult> timLichSuTiepNhan(String tuKhoa) {
        kiemTraQuyenLichSuTiepNhan();
        yeuCauTuKhoa(tuKhoa);
        String maNVTiepNhan = UserSession.getInstance().getVaiTro() == VaiTro.TIEP_NHAN_VIEN
                ? UserSession.getInstance().getNhanVien().getMaNV() : null;
        return traCuuDao.timLichSuTiepNhan(tuKhoa.trim(), maNVTiepNhan);
    }

    // ── Kiểm tra phân quyền nghiệp vụ ─────────────────────────────────────────

    private static void yeuCauTuKhoa(String tuKhoa) {
        if (tuKhoa == null || tuKhoa.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập từ khóa tra cứu.");
        }
    }

    private static void kiemTraQuyenLinhKien() {
        VaiTro vt = UserSession.getInstance().getVaiTro();
        if (vt == VaiTro.TIEP_NHAN_VIEN) {
            throw new SecurityException("Tiếp nhận viên không có quyền tra cứu linh kiện và tồn kho.");
        }
    }

    private static void kiemTraQuyenKhachHang() {
        VaiTro vt = UserSession.getInstance().getVaiTro();
        if (vt == VaiTro.KY_THUAT_VIEN) {
            throw new SecurityException("Kỹ thuật viên không có quyền tra cứu hồ sơ khách hàng.");
        }
    }

    private static void kiemTraQuyenLichSuSuaChua() {
        VaiTro vt = UserSession.getInstance().getVaiTro();
        if (vt == VaiTro.TIEP_NHAN_VIEN) {
            throw new SecurityException("Tiếp nhận viên không có quyền tra cứu chi tiết lịch sử sửa chữa kỹ thuật.");
        }
    }

    private static void kiemTraQuyenLichSuTiepNhan() {
        VaiTro vt = UserSession.getInstance().getVaiTro();
        if (vt == VaiTro.KY_THUAT_VIEN) {
            throw new SecurityException("Kỹ thuật viên không có quyền tra cứu lịch sử tiếp nhận.");
        }
    }
}
