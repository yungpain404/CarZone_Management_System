package dao;

import connectDB.DbUtil;
import enums.VaiTro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/** DAO đọc và lưu cấu hình giới hạn chức năng theo vai trò. */
public class QuyenChucNangDAO {

    public Map<String, Boolean> layQuyen(VaiTro vaiTro) {
        String sql = "SELECT maChucNang, duocPhep FROM QuyenChucNang WHERE vaiTro = ?";
        Map<String, Boolean> result = new LinkedHashMap<>();
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, vaiTro.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.put(rs.getString("maChucNang"), rs.getBoolean("duocPhep"));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Không thể tải quyền chức năng: " + e.getMessage(), e);
        }
    }

    public void capNhat(VaiTro vaiTro, Map<String, Boolean> quyen) {
        String updateSql = "UPDATE QuyenChucNang SET duocPhep = ?, ngayCapNhat = SYSDATETIME() "
                + "WHERE vaiTro = ? AND maChucNang = ?";
        try (Connection con = DbUtil.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                for (Map.Entry<String, Boolean> entry : quyen.entrySet()) {
                    ps.setBoolean(1, Boolean.TRUE.equals(entry.getValue()));
                    ps.setString(2, vaiTro.name());
                    ps.setString(3, entry.getKey());
                    if (ps.executeUpdate() != 1) {
                        throw new IllegalStateException("Không tìm thấy quyền " + entry.getKey() + " cho vai trò đã chọn.");
                    }
                }
                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể lưu quyền chức năng: " + e.getMessage(), e);
        }
    }
}
