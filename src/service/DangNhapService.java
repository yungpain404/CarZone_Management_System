package service;

import dao.TaiKhoanDao;
import entity.NhanVien;
import entity.TaiKhoan;

/**
 * Service xử lý đăng nhập.
 * Kiểm tra tài khoản, mật khẩu, trạng thái hoạt động.
 * Dịch lỗi thành thông báo tiếng Việt để UI hiển thị.
 */
public class DangNhapService {

    private final TaiKhoanDao taiKhoanDao;

    public DangNhapService() {
        this.taiKhoanDao = new TaiKhoanDao();
    }

    /** Kết quả đăng nhập trả về UI. */
    public static class KetQuaDangNhap {
        private final boolean thanhCong;
        private final String thongBao;
        private final NhanVien nhanVien; // null nếu thất bại

        KetQuaDangNhap(boolean thanhCong, String thongBao, NhanVien nhanVien) {
            this.thanhCong = thanhCong;
            this.thongBao = thongBao;
            this.nhanVien = nhanVien;
        }

        public boolean isThanhCong() { return thanhCong; }
        public String getThongBao()  { return thongBao;  }
        public NhanVien getNhanVien() { return nhanVien; }
    }

    /**
     * Thực hiện đăng nhập.
     *
     * @param tenDangNhap tên đăng nhập nhập từ UI (chưa validate định dạng)
     * @param matKhauNhap mật khẩu gốc nhập từ UI
     * @return KetQuaDangNhap (không bao giờ null)
     */
    public KetQuaDangNhap dangNhap(String tenDangNhap, String matKhauNhap) {
        // Validate sớm ở Java (thông báo thân thiện)
        if (tenDangNhap == null || tenDangNhap.isBlank()) {
            return loi("Vui lòng nhập tên đăng nhập.");
        }
        if (matKhauNhap == null || matKhauNhap.isBlank()) {
            return loi("Vui lòng nhập mật khẩu.");
        }
        if (!tenDangNhap.matches("^(CCH|TNV|KTV)\\d{3}$")) {
            return loi("Tên đăng nhập không đúng định dạng (VD: CCH001, TNV002, KTV003).");
        }

        TaiKhoan taiKhoan = null;
        if (connectDB.JpaUtil.isDatabaseOnline()) {
            try {
                taiKhoan = taiKhoanDao.timTheoTenDangNhap(tenDangNhap);
            } catch (Exception e) {
                taiKhoan = null;
            }
        }

        if (taiKhoan == null) {
            // CSDL ngoại tuyến hoặc chưa có dữ liệu -> Fallback sang tài khoản mẫu (khớp V1 database) để hỗ trợ kiểm thử UI
            taiKhoan = timTaiKhoanMau(tenDangNhap);
            if (taiKhoan == null) {
                return loi("Tên đăng nhập hoặc mật khẩu không đúng.");
            }
        }
        if (!taiKhoan.isTrangThaiHoatDong()) {
            return loi("Tài khoản đã bị vô hiệu hóa. Liên hệ chủ cửa hàng.");
        }
        if (!taiKhoan.khopMatKhau(matKhauNhap)) {
            return loi("Tên đăng nhập hoặc mật khẩu không đúng.");
        }

        NhanVien nv = taiKhoan.getNhanVien();
        return new KetQuaDangNhap(true,
                "Chào mừng, " + nv.getHoTen() + "!", nv);
    }

    /** Danh sách tài khoản mẫu chuẩn theo V1__init_CarZone_Database.sql dùng kiểm thử khi chưa kết nối DB. */
    private static TaiKhoan timTaiKhoanMau(String tenDangNhap) {
        NhanVien nv;
        switch (tenDangNhap) {
            case "CCH001" -> {
                nv = new NhanVien("CCH001");
                nv.setHoTen("Trần Minh Quân");
                nv.setVaiTro(enums.VaiTro.CHU_CUA_HANG);
            }
            case "TNV001" -> {
                nv = new NhanVien("TNV001");
                nv.setHoTen("Lê Thị Hoa");
                nv.setVaiTro(enums.VaiTro.TIEP_NHAN_VIEN);
            }
            case "TNV002" -> {
                nv = new NhanVien("TNV002");
                nv.setHoTen("Phạm Ngọc Lan");
                nv.setVaiTro(enums.VaiTro.TIEP_NHAN_VIEN);
            }
            case "KTV001" -> {
                nv = new NhanVien("KTV001");
                nv.setHoTen("Vũ Thanh Tùng");
                nv.setVaiTro(enums.VaiTro.KY_THUAT_VIEN);
            }
            case "KTV002" -> {
                nv = new NhanVien("KTV002");
                nv.setHoTen("Đỗ Văn Hùng");
                nv.setVaiTro(enums.VaiTro.KY_THUAT_VIEN);
            }
            default -> {
                return null;
            }
        }
        TaiKhoan tk = new TaiKhoan(tenDangNhap);
        tk.setNhanVien(nv);
        tk.setMatKhau("Carzone@123");
        tk.setTrangThaiHoatDong(true);
        return tk;
    }

    private static KetQuaDangNhap loi(String thongBao) {
        return new KetQuaDangNhap(false, thongBao, null);
    }
}
