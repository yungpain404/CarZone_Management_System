package session;

import entity.NhanVien;
import entity.TaiKhoan;
import enums.VaiTro;

import java.util.Date;

/**
 * Quản lý phiên làm việc (Session) của người dùng hiện tại sau khi xác thực thành công.
 * Áp dụng mô hình Singleton để toàn bộ ứng dụng (Header, Navigation, ContentPanel, Controller)
 * có thể truy xuất thông tin nhân viên và vai trò một cách nhất quán.
 */
public class UserSession {

    private static volatile UserSession instance;

    private TaiKhoan taiKhoan;
    private NhanVien nhanVien;
    private Date thoiDiemDangNhap;

    private UserSession() {
    }

    public static UserSession getInstance() {
        if (instance == null) {
            synchronized (UserSession.class) {
                if (instance == null) {
                    instance = new UserSession();
                }
            }
        }
        return instance;
    }

    /**
     * Khởi tạo phiên làm việc sau khi xác thực thành công từ cơ sở dữ liệu.
     *
     * @param taiKhoan thực thể TaiKhoan lấy trực tiếp từ database
     * @param nhanVien thực thể NhanVien tương ứng lấy từ database
     */
    public synchronized void batDauPhien(TaiKhoan taiKhoan, NhanVien nhanVien) {
        this.taiKhoan = taiKhoan;
        this.nhanVien = nhanVien;
        this.thoiDiemDangNhap = new Date();
    }

    /**
     * Hủy phiên làm việc hiện tại khi người dùng đăng xuất.
     */
    public synchronized void huyPhien() {
        this.taiKhoan = null;
        this.nhanVien = null;
        this.thoiDiemDangNhap = null;
    }

    /** Kiểm tra xem phiên có đang hoạt động không. */
    public boolean daDangNhap() {
        return this.taiKhoan != null && this.nhanVien != null;
    }

    public TaiKhoan getTaiKhoan() {
        return taiKhoan;
    }

    public NhanVien getNhanVien() {
        return nhanVien;
    }

    public VaiTro getVaiTro() {
        return nhanVien != null ? nhanVien.getVaiTro() : null;
    }

    public String getHoTen() {
        return nhanVien != null ? nhanVien.getHoTen() : "Khách";
    }

    public String getMaNV() {
        return nhanVien != null ? nhanVien.getMaNV() : "";
    }

    public String getTenDangNhap() {
        return taiKhoan != null ? taiKhoan.getTenDangNhap() : "";
    }

    public Date getThoiDiemDangNhap() {
        return thoiDiemDangNhap;
    }

    /**
     * Kiểm tra người dùng hiện tại có vai trò tương ứng không.
     */
    public boolean coVaiTro(VaiTro vaiTroKiemTra) {
        return getVaiTro() == vaiTroKiemTra;
    }
}
