package controller;

import dao.DichVuDao;
import dao.KhachHangDao;
import dao.LinhKienDao;
import dao.PhieuTiepNhanSuaChuaDao;
import dao.XeDao;
import dao.impl.DichVuDaoImpl;
import dao.impl.KhachHangDaoImpl;
import dao.impl.LinhKienDaoImpl;
import dao.impl.PhieuTiepNhanSuaChuaDaoImpl;
import dao.impl.XeDaoImpl;
import entity.DichVu;
import entity.KhachHang;
import entity.LinhKien;
import entity.PhieuTiepNhanSuaChua;
import entity.Xe;
import enums.TrangThaiPhieu;
import enums.VaiTro;
import session.UserSession;

import java.util.List;

/**
 * Controller điều phối nghiệp vụ Tra cứu (UC007 - Tiếp nhận viên).
 * TNV chỉ xem phiếu do mình lập (maNVTiepNhan = session.maNV).
 * Đây là chức năng READ-ONLY, không ghi dữ liệu.
 */
public class TraCuuController {

    private final DichVuDao dichVuDao;
    private final LinhKienDao linhKienDao;
    private final KhachHangDao khachHangDao;
    private final XeDao xeDao;
    private final PhieuTiepNhanSuaChuaDao phieuDao;

    public TraCuuController() {
        this.dichVuDao = new DichVuDaoImpl();
        this.linhKienDao = new LinhKienDaoImpl();
        this.khachHangDao = new KhachHangDaoImpl();
        this.xeDao = new XeDaoImpl();
        this.phieuDao = new PhieuTiepNhanSuaChuaDaoImpl();
    }

    // ── Tra cứu Dịch vụ ──────────────────────────────────────────────────────

    /**
     * Lấy toàn bộ danh sách dịch vụ (read-only).
     */
    public List<DichVu> layTatCaDichVu() {
        return dichVuDao.layTatCa();
    }

    /**
     * Tìm kiếm dịch vụ theo từ khóa (mã, tên).
     */
    public List<DichVu> timDichVu(String tuKhoa) {
        if (tuKhoa == null || tuKhoa.isBlank()) return dichVuDao.layTatCa();
        return dichVuDao.timKiem(tuKhoa.trim(), null);
    }

    // ── Tra cứu Linh kiện ─────────────────────────────────────────────────────

    /**
     * Lấy toàn bộ danh sách linh kiện (read-only).
     */
    public List<LinhKien> layTatCaLinhKien() {
        return linhKienDao.layTatCa();
    }

    /**
     * Tìm kiếm linh kiện theo từ khóa (mã, tên).
     */
    public List<LinhKien> timLinhKien(String tuKhoa) {
        if (tuKhoa == null || tuKhoa.isBlank()) return linhKienDao.layTatCa();
        return linhKienDao.timKiem(tuKhoa.trim(), null);
    }

    // ── Tra cứu Khách hàng ────────────────────────────────────────────────────

    /**
     * Lấy toàn bộ khách hàng (read-only).
     */
    public List<KhachHang> layTatCaKhachHang() {
        return khachHangDao.layTatCa();
    }

    /**
     * Tìm kiếm khách hàng theo từ khóa (mã, họ tên, số điện thoại, CCCD).
     */
    public List<KhachHang> timKhachHang(String tuKhoa) {
        if (tuKhoa == null || tuKhoa.isBlank()) return khachHangDao.layTatCa();
        return khachHangDao.timKiem(tuKhoa.trim());
    }

    /**
     * Lấy danh sách xe của một khách hàng.
     */
    public List<Xe> layXeTheoKhachHang(String maKH) {
        if (maKH == null || maKH.isBlank()) return List.of();
        return xeDao.layTheoKhachHang(maKH.trim());
    }

    /**
     * Tìm kiếm xe theo biển số hoặc thông tin nhận diện.
     */
    public List<Xe> timXe(String tuKhoa) {
        if (tuKhoa == null || tuKhoa.isBlank()) return xeDao.layTatCa();
        return xeDao.timKiem(tuKhoa.trim());
    }

    // ── Tra cứu Lịch sử tiếp nhận (TNV chỉ xem phiếu của mình) ──────────────

    /**
     * Lấy tất cả phiếu tiếp nhận của tiếp nhận viên hiện tại.
     */
    public List<PhieuTiepNhanSuaChua> layLichSuTiepNhan() {
        VaiTro role = UserSession.getInstance().getVaiTro();
        if (role == VaiTro.TIEP_NHAN_VIEN) {
            String maNV = UserSession.getInstance().getNhanVien().getMaNV();
            return phieuDao.layTheoTiepNhanVien(maNV);
        }
        // CHU_CUA_HANG xem tất cả
        return phieuDao.layTatCa();
    }

    /**
     * Tìm kiếm phiếu tiếp nhận theo từ khóa và trạng thái.
     * TNV bị giới hạn trong phạm vi của mình.
     */
    public List<PhieuTiepNhanSuaChua> timLichSuTiepNhan(String tuKhoa, TrangThaiPhieu trangThai) {
        String maNVFilter = null;
        VaiTro role = UserSession.getInstance().getVaiTro();
        if (role == VaiTro.TIEP_NHAN_VIEN) {
            maNVFilter = UserSession.getInstance().getNhanVien().getMaNV();
        }
        return phieuDao.timKiem(tuKhoa, maNVFilter, trangThai);
    }

    /**
     * Xem chi tiết một phiếu tiếp nhận theo mã.
     */
    public PhieuTiepNhanSuaChua layChiTietPhieu(String maPhieu) {
        if (maPhieu == null || maPhieu.isBlank()) return null;
        return phieuDao.timTheoMa(maPhieu.trim());
    }
}
