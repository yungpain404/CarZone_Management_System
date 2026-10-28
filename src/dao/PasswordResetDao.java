package dao;

/**
 * Interface DAO phụ trách chức năng quên mật khẩu / đặt lại mật khẩu.
 */
public interface PasswordResetDao {

    /**
     * Xác thực thông tin người dùng khi yêu cầu đặt lại mật khẩu bằng CCCD.
     *
     * @param tenDangNhap tên đăng nhập
     * @param cccd số căn cước công dân của nhân viên
     * @return true nếu thông tin khớp chính xác
     */
    boolean xacThucThongTin(String tenDangNhap, String cccd);

    /**
     * Xác thực thông tin người dùng khi yêu cầu đặt lại mật khẩu bằng số điện thoại.
     *
     * @param tenDangNhap tên đăng nhập
     * @param sdt số điện thoại của nhân viên
     * @return true nếu thông tin khớp chính xác
     */
    boolean xacThucTheoSDT(String tenDangNhap, String sdt);

    /**
     * Cập nhật mật khẩu mới (chuỗi hash SHA-256 in hoa).
     *
     * @param tenDangNhap tên đăng nhập
     * @param matKhauMoiHash mật khẩu mới đã băm
     * @return true nếu cập nhật thành công
     */
    boolean datLaiMatKhau(String tenDangNhap, String matKhauMoiHash);
}
