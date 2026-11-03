package controller;

import dao.DichVuDAO;
import dao.LinhKienDAO;
import dao.PhieuTiepNhanSuaChuaDAO;
import dao.PhieuTiepNhanSuaChuaDAO.ChiTietDichVuItem;
import dao.PhieuTiepNhanSuaChuaDAO.ChiTietLinhKienItem;
import dao.PhieuTiepNhanSuaChuaDAO.PhieuSuaChuaItem;
import dao.PhieuTiepNhanSuaChuaDAO.ThongKeKTVData;
import entity.DichVu;
import entity.LinhKien;
import enums.VaiTro;
import session.UserSession;

import java.util.Date;
import java.util.List;

/**
 * Controller điều phối nghiệp vụ Phiếu tiếp nhận và sửa chữa:
 * - Cập nhật trạng thái phiếu sửa chữa cho Kỹ thuật viên (Chờ sửa → Đang sửa ⇄ Chờ linh kiện → Hoàn tất).
 * - Quản lý chi tiết dịch vụ thi công và chi tiết linh kiện thay thế.
 * - Thống kê năng suất và sản lượng sửa chữa cho Kỹ thuật viên.
 * - Bắt lỗi trigger SQL Server và dịch sang thông báo tiếng Việt dễ hiểu.
 */
public class PhieuTiepNhanSuaChuaController {

    private final PhieuTiepNhanSuaChuaDAO phieuDao;
    private final DichVuDAO dichVuDao;
    private final LinhKienDAO linhKienDao;

    public PhieuTiepNhanSuaChuaController() {
        this(new PhieuTiepNhanSuaChuaDAO(), new DichVuDAO(), new LinhKienDAO());
    }

    public PhieuTiepNhanSuaChuaController(PhieuTiepNhanSuaChuaDAO phieuDao, DichVuDAO dichVuDao, LinhKienDAO linhKienDao) {
        this.phieuDao = phieuDao;
        this.dichVuDao = dichVuDao;
        this.linhKienDao = linhKienDao;
    }

    /**
     * Tìm danh sách phiếu được phân công cho Kỹ thuật viên hiện tại.
     */
    public List<PhieuSuaChuaItem> timPhieuKTV(String tuKhoa, String trangThai, Date tuNgay, Date denNgay) {
        String maKTV = null;
        if (UserSession.getInstance().getVaiTro() == VaiTro.KY_THUAT_VIEN) {
            maKTV = UserSession.getInstance().getNhanVien().getMaNV();
        }
        return phieuDao.timPhieuKTV(maKTV, tuKhoa, trangThai, tuNgay, denNgay);
    }

    /**
     * Lấy thông tin chi tiết một phiếu.
     */
    public PhieuSuaChuaItem layThongTinPhieu(String maPhieu) {
        if (maPhieu == null || maPhieu.isBlank()) {
            throw new IllegalArgumentException("Mã phiếu không được để trống.");
        }
        return phieuDao.layTheoMa(maPhieu.trim());
    }

    /**
     * Chuyển trạng thái phiếu sửa chữa.
     */
    public void chuyenTrangThai(String maPhieu, String trangThaiMoi) {
        kiemTraQuyenKTV();
        if (maPhieu == null || maPhieu.isBlank()) {
            throw new IllegalArgumentException("Mã phiếu không được để trống.");
        }
        String maKTV = UserSession.getInstance().getNhanVien().getMaNV();
        try {
            boolean ok = phieuDao.chuyenTrangThai(maPhieu.trim(), trangThaiMoi, maKTV);
            if (!ok) {
                throw new IllegalStateException("Không tìm thấy phiếu hoặc trạng thái không thay đổi.");
            }
        } catch (Exception e) {
            throw new RuntimeException(dichLoiDatabase(e.getMessage()), e);
        }
    }

    /**
     * Lấy danh sách dịch vụ đã gán vào phiếu.
     */
    public List<ChiTietDichVuItem> layChiTietDichVu(String maPhieu) {
        return phieuDao.layChiTietDichVu(maPhieu);
    }

    /**
     * Thêm dịch vụ vào phiếu sửa chữa.
     */
    public void themDichVu(String maPhieu, String maDV, int soLuong, String ghiChu) {
        kiemTraQuyenKTV();
        if (maPhieu == null || maPhieu.isBlank()) throw new IllegalArgumentException("Mã phiếu không hợp lệ.");
        if (maDV == null || maDV.isBlank()) throw new IllegalArgumentException("Vui lòng chọn dịch vụ.");
        if (soLuong <= 0) throw new IllegalArgumentException("Số lượng dịch vụ phải lớn hơn 0.");

        DichVu dv = dichVuDao.timTheoMa(maDV);
        if (dv == null) throw new IllegalArgumentException("Dịch vụ không tồn tại trong hệ thống.");
        if (!dv.isDangCungCap()) throw new IllegalStateException("Dịch vụ này đã ngừng cung cấp.");

        try {
            phieuDao.themChiTietDichVu(maPhieu.trim(), maDV.trim(), soLuong, dv.getDonGia(), ghiChu);
        } catch (Exception e) {
            throw new RuntimeException(dichLoiDatabase(e.getMessage()), e);
        }
    }

    /**
     * Xóa dịch vụ khỏi phiếu.
     */
    public void xoaDichVu(String maPhieu, String maDV) {
        kiemTraQuyenKTV();
        try {
            phieuDao.xoaChiTietDichVu(maPhieu, maDV);
        } catch (Exception e) {
            throw new RuntimeException(dichLoiDatabase(e.getMessage()), e);
        }
    }

    /**
     * Lấy danh sách linh kiện đã dùng trong phiếu.
     */
    public List<ChiTietLinhKienItem> layChiTietLinhKien(String maPhieu) {
        return phieuDao.layChiTietLinhKien(maPhieu);
    }

    /**
     * Thêm linh kiện vào phiếu sửa chữa (kho tự động trừ tồn qua trigger).
     */
    public void themLinhKien(String maPhieu, String maLK, int soLuong, String ghiChu) {
        kiemTraQuyenKTV();
        if (maPhieu == null || maPhieu.isBlank()) throw new IllegalArgumentException("Mã phiếu không hợp lệ.");
        if (maLK == null || maLK.isBlank()) throw new IllegalArgumentException("Vui lòng chọn linh kiện.");
        if (soLuong <= 0) throw new IllegalArgumentException("Số lượng linh kiện phải lớn hơn 0.");

        LinhKien lk = linhKienDao.timTheoMa(maLK);
        if (lk == null) throw new IllegalArgumentException("Linh kiện không tồn tại trong hệ thống.");
        if (!lk.isDangKinhDoanh()) throw new IllegalStateException("Linh kiện này đã ngừng kinh doanh.");
        if (lk.getSoLuongTon() < soLuong) {
            throw new IllegalStateException("Tồn kho không đủ (chỉ còn " + lk.getSoLuongTon() + " " + lk.getDonViTinh() + "). Vui lòng lập yêu cầu nhập linh kiện.");
        }

        try {
            phieuDao.themChiTietLinhKien(maPhieu.trim(), maLK.trim(), soLuong, lk.getDonGia(), ghiChu);
        } catch (Exception e) {
            throw new RuntimeException(dichLoiDatabase(e.getMessage()), e);
        }
    }

    /**
     * Xóa linh kiện khỏi phiếu (kho tự động hoàn tồn qua trigger).
     */
    public void xoaLinhKien(String maPhieu, String maLK) {
        kiemTraQuyenKTV();
        try {
            phieuDao.xoaChiTietLinhKien(maPhieu, maLK);
        } catch (Exception e) {
            throw new RuntimeException(dichLoiDatabase(e.getMessage()), e);
        }
    }

    /**
     * Lấy dữ liệu tổng hợp cho màn hình Thống kê sửa chữa KTV.
     */
    public ThongKeKTVData layThongKeKTV(Date tuNgay, Date denNgay) {
        String maKTV = UserSession.getInstance().getNhanVien().getMaNV();
        return phieuDao.layThongKeKTV(maKTV, tuNgay, denNgay);
    }

    /**
     * Danh sách dịch vụ đang kinh doanh để nạp vào combobox chọn.
     */
    public List<DichVu> layDanhSachDichVuHoatDong() {
        return dichVuDao.timKiem(null, true);
    }

    /**
     * Danh sách linh kiện đang kinh doanh còn hàng để nạp vào combobox chọn.
     */
    public List<LinhKien> layDanhSachLinhKienHoatDong() {
        return linhKienDao.timKiem(null, true, true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DỊCH LỖI VÀ KIỂM TRA QUYỀN
    // ─────────────────────────────────────────────────────────────────────────

    private static void kiemTraQuyenKTV() {
        VaiTro vt = UserSession.getInstance().getVaiTro();
        if (vt != VaiTro.KY_THUAT_VIEN && vt != VaiTro.CHU_CUA_HANG) {
            throw new SecurityException("Chỉ Kỹ thuật viên hoặc Chủ cửa hàng mới được thực hiện thao tác này.");
        }
    }

    private static String dichLoiDatabase(String dbMessage) {
        if (dbMessage == null) return "Thao tác cơ sở dữ liệu không thành công.";
        if (dbMessage.contains("50103")) return "Chuyển trạng thái phiếu không hợp lệ theo quy trình kỹ thuật.";
        if (dbMessage.contains("50104")) return "Phiếu đã ở trạng thái Hoàn tất, không được chỉnh sửa.";
        if (dbMessage.contains("50111")) return "Phiếu đã Hoàn tất, không được thêm/sửa/xóa dịch vụ.";
        if (dbMessage.contains("50112")) return "Dịch vụ đã ngừng cung cấp, không thể thêm vào phiếu.";
        if (dbMessage.contains("50121")) return "Phiếu đã Hoàn tất, không được thêm/sửa/xóa linh kiện.";
        if (dbMessage.contains("50122")) return "Linh kiện đã ngừng kinh doanh, không thể thêm vào phiếu.";
        if (dbMessage.contains("PRIMARY KEY") || dbMessage.contains("PK_ChiTiet")) {
            return "Mục này đã tồn tại trong phiếu sửa chữa.";
        }
        return dbMessage;
    }
}

