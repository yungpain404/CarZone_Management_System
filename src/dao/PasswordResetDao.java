package dao;

import connectDB.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO phụ trách chức năng quên mật khẩu / đặt lại mật khẩu - JDBC thuần.
 */
public class PasswordResetDao {

    /**
     * Xác thực thông tin người dùng khi yêu cầu đặt lại mật khẩu bằng CCCD.
     *
     * @param tenDangNhap tên đăng nhập
     * @param cccd số căn cước công dân của nhân viên
     * @return true nếu thông tin khớp chính xác
     */
    public boolean xacThucThongTin(String tenDangNhap, String cccd) {
        String sql =
            "SELECT 1 FROM TaiKhoan tk " +
            "INNER JOIN NhanVien nv ON nv.maNV = tk.maNV " +
            "WHERE tk.tenDangNhap = ? AND nv.CCCD = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tenDangNhap);
            ps.setString(2, cccd);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi xác thực thông tin đặt lại mật khẩu: " + e.getMessage(), e);
        }
    }

    /**
     * Xác thực thông tin người dùng khi yêu cầu đặt lại mật khẩu bằng số điện thoại.
     *
     * @param tenDangNhap tên đăng nhập
     * @param sdt số điện thoại của nhân viên
     * @return true nếu thông tin khớp chính xác
     */
    public boolean xacThucTheoSDT(String tenDangNhap, String sdt) {
        String sql =
            "SELECT 1 FROM TaiKhoan tk " +
            "INNER JOIN NhanVien nv ON nv.maNV = tk.maNV " +
            "WHERE tk.tenDangNhap = ? AND nv.soDienThoai = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tenDangNhap);
            ps.setString(2, sdt);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi xác thực số điện thoại đặt lại mật khẩu: " + e.getMessage(), e);
        }
    }

    /**
     * Cập nhật mật khẩu mới (chuỗi hash SHA-256 in hoa).
     *
     * @param tenDangNhap tên đăng nhập
     * @param matKhauMoiHash mật khẩu mới đã băm
     * @return true nếu cập nhật thành công
     */
    public boolean datLaiMatKhau(String tenDangNhap, String matKhauMoiHash) {
        String sql = "UPDATE TaiKhoan SET matKhau = ? WHERE tenDangNhap = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, matKhauMoiHash);
            ps.setString(2, tenDangNhap);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật mật khẩu mới: " + e.getMessage(), e);
        }
    }
}
