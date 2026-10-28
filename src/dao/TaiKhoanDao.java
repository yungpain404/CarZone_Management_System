package dao;

import entity.TaiKhoan;

/**
 * Interface DAO cho bảng TaiKhoan – chỉ đọc/lưu dữ liệu, không chứa luật nghiệp vụ.
 */
public interface TaiKhoanDao {

    /**
     * Tìm TaiKhoan kèm NhanVien theo tên đăng nhập.
     *
     * @param tenDangNhap tên đăng nhập
     * @return TaiKhoan hoặc {@code null} nếu không tìm thấy
     */
    TaiKhoan timTheoTenDangNhap(String tenDangNhap);

    /**
     * Cập nhật mật khẩu đã băm (SHA-256) cho tài khoản.
     *
     * @param tenDangNhap tên đăng nhập
     * @param matKhauMoiHash chuỗi băm hex SHA-256 in hoa
     * @return true nếu cập nhật thành công
     */
    boolean capNhatMatKhau(String tenDangNhap, String matKhauMoiHash);

    /**
     * Cập nhật trạng thái hoạt động tài khoản.
     *
     * @param tenDangNhap tên đăng nhập
     * @param trangThai true nếu đang hoạt động, false nếu bị khóa
     * @return true nếu cập nhật thành công
     */
    boolean capNhatTrangThai(String tenDangNhap, boolean trangThai);
}
