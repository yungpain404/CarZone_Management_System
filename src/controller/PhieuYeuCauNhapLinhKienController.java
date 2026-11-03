package controller;

import dao.PhieuYeuCauNhapLinhKienDAO;
import entity.NhanVien;
import entity.PhieuYeuCauNhapLinhKien;
import enums.TrangThaiYeuCau;
import enums.VaiTro;
import session.UserSession;

import java.util.Date;
import java.util.List;

/** Điều phối tra cứu và xử lý yêu cầu nhập linh kiện của Chủ cửa hàng. */
public class PhieuYeuCauNhapLinhKienController {
    private final PhieuYeuCauNhapLinhKienDAO dao;

    public PhieuYeuCauNhapLinhKienController() { this(new PhieuYeuCauNhapLinhKienDAO()); }
    public PhieuYeuCauNhapLinhKienController(PhieuYeuCauNhapLinhKienDAO dao) { this.dao = dao; }

    public List<PhieuYeuCauNhapLinhKien> timKiem(String tuKhoa, TrangThaiYeuCau trangThai) {
        yeuCauChuCuaHang();
        return dao.timKiem(tuKhoa == null ? null : tuKhoa.trim(), trangThai);
    }

    public PhieuYeuCauNhapLinhKien timTheoMa(String maYeuCau) {
        yeuCauChuCuaHang();
        if (maYeuCau == null || !maYeuCau.matches("^PYC\\d{8}[AP]\\d{2}$")) {
            throw new IllegalArgumentException("Mã yêu cầu nhập linh kiện không hợp lệ.");
        }
        PhieuYeuCauNhapLinhKien item = dao.timTheoMa(maYeuCau);
        if (item == null) throw new IllegalArgumentException("Không tìm thấy yêu cầu " + maYeuCau + ".");
        return item;
    }

    public List<PhieuYeuCauNhapLinhKien> timYeuCauDaDuyetTheoMaLinhKien(String maLK) {
        yeuCauChuCuaHang();
        if (maLK == null || !maLK.matches("^LK\\d{5,}$")) throw new IllegalArgumentException("Mã linh kiện không hợp lệ.");
        return dao.timYeuCauDaDuyetTheoMaLinhKien(maLK);
    }

    public void duyet(String maYeuCau, String ghiChu) {
        chuyenTrangThai(maYeuCau, TrangThaiYeuCau.DA_DUYET, ghiChu, false, null);
    }

    public void tuChoi(String maYeuCau, String lyDo) {
        if (lyDo == null || lyDo.isBlank()) throw new IllegalArgumentException("Cần nhập lý do từ chối yêu cầu.");
        chuyenTrangThai(maYeuCau, TrangThaiYeuCau.TU_CHOI, lyDo, true, null);
    }

    public void xacNhanNhapKho(String maYeuCau, int soLuongNhapThucTe, String ghiChu) {
        if (soLuongNhapThucTe <= 0) throw new IllegalArgumentException("Số lượng thực nhận phải lớn hơn 0.");
        chuyenTrangThai(maYeuCau, TrangThaiYeuCau.DA_NHAP_KHO, ghiChu, false, soLuongNhapThucTe);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NGHIỆP VỤ CỦA KỸ THUẬT VIÊN
    // ─────────────────────────────────────────────────────────────────────────

    /** Dự đoán trước mã yêu cầu tiếp theo sẽ được sinh. */
    public String layMaYeuCauTiepTheo() {
        yeuCauKyThuatVien();
        return dao.sinhMaYeuCauTiepTheo();
    }

    /** Tạo yêu cầu nhập linh kiện mới (dành riêng cho Kỹ thuật viên). */
    public String taoYeuCau(String maLK, int soLuong, String ghiChu) {
        yeuCauKyThuatVien();
        if (maLK == null || maLK.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn linh kiện cần yêu cầu.");
        }
        if (soLuong <= 0) {
            throw new IllegalArgumentException("Số lượng yêu cầu phải lớn hơn 0.");
        }
        if (ghiChu == null || ghiChu.isBlank()) {
            throw new IllegalArgumentException("Lý do/ghi chú yêu cầu không được để trống.");
        }
        if (ghiChu.trim().length() > 300) {
            throw new IllegalArgumentException("Lý do yêu cầu tối đa 300 ký tự.");
        }
        String maKTV = UserSession.getInstance().getMaNV();
        return dao.taoYeuCau(maLK.trim(), maKTV, soLuong, ghiChu.trim());
    }

    /** Danh sách yêu cầu do chính Kỹ thuật viên đăng nhập lập. */
    public List<PhieuYeuCauNhapLinhKien> timKiemCuaKTV(String tuKhoa, TrangThaiYeuCau trangThai) {
        return timKiemCuaKTV(tuKhoa, trangThai, null, null);
    }

    /** Danh sách yêu cầu do chính Kỹ thuật viên đăng nhập lập có lọc khoảng ngày. */
    public List<PhieuYeuCauNhapLinhKien> timKiemCuaKTV(String tuKhoa, TrangThaiYeuCau trangThai, Date tuNgay, Date denNgay) {
        yeuCauKyThuatVien();
        String maKTV = UserSession.getInstance().getMaNV();
        return dao.timTheoKTV(maKTV, tuKhoa, trangThai, tuNgay, denNgay);
    }

    /** Đếm số lượng yêu cầu theo từng trạng thái của KTV đăng nhập. */
    public int demYeuCauKTV(TrangThaiYeuCau trangThai, Date tuNgay, Date denNgay) {
        yeuCauKyThuatVien();
        String maKTV = UserSession.getInstance().getMaNV();
        return dao.demYeuCauKTV(maKTV, trangThai, tuNgay, denNgay);
    }

    /** Hủy yêu cầu còn đang chờ duyệt của Kỹ thuật viên. */
    public boolean huyYeuCauCuaKTV(String maYeuCau) {
        yeuCauKyThuatVien();
        if (maYeuCau == null || maYeuCau.isBlank()) return false;
        String maKTV = UserSession.getInstance().getMaNV();
        return dao.huyYeuCau(maYeuCau.trim(), maKTV);
    }

    private static void yeuCauKyThuatVien() {
        if (UserSession.getInstance().getVaiTro() != VaiTro.KY_THUAT_VIEN) {
            throw new SecurityException("Chức năng chỉ dành cho Kỹ thuật viên.");
        }
    }

    private void chuyenTrangThai(String maYeuCau, TrangThaiYeuCau trangThaiMoi, String ghiChu,
                                 boolean batBuocGhiChu, Integer soLuongNhapThucTe) {
        yeuCauChuCuaHang();
        if (ghiChu != null && ghiChu.length() > 300) throw new IllegalArgumentException("Ghi chú xử lý tối đa 300 ký tự.");
        if (batBuocGhiChu && ghiChu != null && ghiChu.trim().length() > 300) {
            throw new IllegalArgumentException("Lý do từ chối tối đa 300 ký tự.");
        }
        PhieuYeuCauNhapLinhKien item = timTheoMa(maYeuCau);
        TrangThaiYeuCau trangThaiCu = item.getTrangThai();
        item.setTrangThai(trangThaiMoi);
        item.setNhanVienXuLy(nhanVienDangNhap());
        item.setNgayXuLy(new Date());
        item.setGhiChuXuLy(ghiChu == null || ghiChu.isBlank() ? null : ghiChu.trim());
        if (trangThaiMoi == TrangThaiYeuCau.DA_NHAP_KHO) item.setSoLuongNhapThucTe(soLuongNhapThucTe);
        dao.capNhatTrangThai(maYeuCau, trangThaiCu, trangThaiMoi, UserSession.getInstance().getMaNV(), ghiChu, soLuongNhapThucTe);
    }

    private static NhanVien nhanVienDangNhap() {
        NhanVien nv = UserSession.getInstance().getNhanVien();
        if (nv == null || nv.getVaiTro() != VaiTro.CHU_CUA_HANG) {
            throw new SecurityException("Chỉ Chủ cửa hàng được xử lý yêu cầu nhập linh kiện.");
        }
        return nv;
    }

    private static void yeuCauChuCuaHang() {
        if (UserSession.getInstance().getVaiTro() != VaiTro.CHU_CUA_HANG) {
            throw new SecurityException("Chỉ Chủ cửa hàng được xử lý yêu cầu nhập linh kiện.");
        }
    }
}
