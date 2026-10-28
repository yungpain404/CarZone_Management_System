package controller;

import controller.AuthController.KetQuaXacThuc;
import entity.NhanVien;

/**
 * Controller xử lý đăng nhập.
 * Chuyển tiếp các yêu cầu xác thực tới AuthController.
 * Dữ liệu truy vấn trực tiếp từ Database, tuyệt đối không mockup dữ liệu.
 */
public class DangNhapController {

    private final AuthController authController;

    public DangNhapController() {
        this.authController = new AuthController();
    }

    /** Kết quả đăng nhập trả về UI (tương thích cho màn hình DangNhapUI). */
    public static class KetQuaDangNhap {
        private final boolean thanhCong;
        private final String thongBao;
        private final NhanVien nhanVien; // null nếu thất bại

        public KetQuaDangNhap(boolean thanhCong, String thongBao, NhanVien nhanVien) {
            this.thanhCong = thanhCong;
            this.thongBao = thongBao;
            this.nhanVien = nhanVien;
        }

        public boolean isThanhCong() { return thanhCong; }
        public String getThongBao()  { return thongBao;  }
        public NhanVien getNhanVien() { return nhanVien; }
    }

    /**
     * Thực hiện đăng nhập trực tiếp từ cơ sở dữ liệu qua AuthController.
     *
     * @param tenDangNhap tên đăng nhập nhập từ UI
     * @param matKhauNhap mật khẩu gốc nhập từ UI
     * @return KetQuaDangNhap
     */
    public KetQuaDangNhap dangNhap(String tenDangNhap, String matKhauNhap) {
        KetQuaXacThuc kq = authController.dangNhap(tenDangNhap, matKhauNhap);
        return new KetQuaDangNhap(kq.isThanhCong(), kq.getThongBao(), kq.getNhanVien());
    }

    public AuthController getAuthController() {
        return authController;
    }
}
