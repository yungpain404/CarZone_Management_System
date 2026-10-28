package dao.impl;

import connectDB.DbUtil;
import dao.TaiKhoanDao;
import entity.NhanVien;
import entity.TaiKhoan;
import enums.VaiTro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Triển khai {@link TaiKhoanDao} bằng JDBC thuần (DriverManager).
 * Mỗi phương thức tự mở/đóng Connection trong try-with-resources.
 */
public class TaiKhoanDaoImpl implements TaiKhoanDao {

    @Override
    public TaiKhoan timTheoTenDangNhap(String tenDangNhap) {
        String sql =
            "SELECT tk.maNV, tk.tenDangNhap, tk.matKhau, tk.trangThaiHoatDong, " +
            "       nv.hoTen, nv.ngaySinh, nv.soDienThoai, nv.CCCD, nv.diaChi, " +
            "       nv.ngayVaoLam, nv.vaiTro " +
            "FROM TaiKhoan tk " +
            "INNER JOIN NhanVien nv ON nv.maNV = tk.maNV " +
            "WHERE tk.tenDangNhap = ?";

        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, tenDangNhap);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn TaiKhoan: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean capNhatMatKhau(String tenDangNhap, String matKhauMoiHash) {
        String sql = "UPDATE TaiKhoan SET matKhau = ? WHERE tenDangNhap = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, matKhauMoiHash);
            ps.setString(2, tenDangNhap);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật mật khẩu: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean capNhatTrangThai(String tenDangNhap, boolean trangThai) {
        String sql = "UPDATE TaiKhoan SET trangThaiHoatDong = ? WHERE tenDangNhap = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, trangThai);
            ps.setString(2, tenDangNhap);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật trạng thái tài khoản: " + e.getMessage(), e);
        }
    }

    // ── Ánh xạ ResultSet → entity ────────────────────────────────────────────

    private TaiKhoan mapRow(ResultSet rs) throws SQLException {
        NhanVien nv = new NhanVien();
        nv.setMaNV(rs.getString("maNV"));
        nv.setHoTenRaw(rs.getString("hoTen"));
        java.sql.Date ngaySinhSql = rs.getDate("ngaySinh");
        if (ngaySinhSql != null) nv.setNgaySinhRaw(ngaySinhSql.toLocalDate());
        nv.setSoDienThoaiRaw(rs.getString("soDienThoai"));
        nv.setCCCDRaw(rs.getString("CCCD"));
        nv.setDiaChi(rs.getString("diaChi"));
        java.sql.Date ngayVaoLamSql = rs.getDate("ngayVaoLam");
        if (ngayVaoLamSql != null) nv.setNgayVaoLamRaw(ngayVaoLamSql.toLocalDate());
        nv.setVaiTroRaw(VaiTro.valueOf(rs.getString("vaiTro")));

        TaiKhoan tk = new TaiKhoan();
        tk.setNhanVienRaw(nv);
        tk.setTenDangNhapRaw(rs.getString("tenDangNhap"));
        tk.setMatKhauHash(rs.getString("matKhau"));
        tk.setTrangThaiHoatDong(rs.getBoolean("trangThaiHoatDong"));
        return tk;
    }
}
