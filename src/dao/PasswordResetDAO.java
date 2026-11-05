package dao;

import connectDB.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** JDBC access for password recovery by employee code and the employee's Gmail address. */
public class PasswordResetDAO {
    public record RecoveryTarget(String maNV, String email) {}

    /** Resolves the active account and its saved recovery email using the employee code. */
    public RecoveryTarget timDichDenTheoMaNV(String maNV) {
        String sql = "SELECT nv.maNV, nv.email FROM NhanVien nv "
                + "JOIN TaiKhoan tk ON tk.maNV=nv.maNV "
                + "WHERE nv.maNV=? AND tk.trangThaiHoatDong=1";
        try (Connection con=DbUtil.getConnection(); PreparedStatement ps=con.prepareStatement(sql)) {
            ps.setString(1,maNV);
            try (ResultSet rs=ps.executeQuery()) {
                return rs.next() ? new RecoveryTarget(rs.getString("maNV"),rs.getString("email")) : null;
            }
        } catch (SQLException e) { throw new RuntimeException("Không thể tra cứu thông tin khôi phục mật khẩu.",e); }
    }

    /** Saves the already validated SHA-256 hash for an active account. */
    public boolean datLaiMatKhau(String maNV,String matKhauMoiHash) {
        String sql="UPDATE TaiKhoan SET matKhau=? WHERE maNV=? AND trangThaiHoatDong=1";
        try (Connection con=DbUtil.getConnection(); PreparedStatement ps=con.prepareStatement(sql)) {
            ps.setString(1,matKhauMoiHash); ps.setString(2,maNV); return ps.executeUpdate()>0;
        } catch(SQLException e) { throw new RuntimeException("Không thể cập nhật mật khẩu.",e); }
    }
}
