package dao;

import connectDB.DbUtil;
import entity.NhanVien;
import entity.TaiKhoan;
import enums.VaiTro;

import java.sql.Connection;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho bảng Tài khoản (`TaiKhoan`) - JDBC thuần (DriverManager),
 * chỉ đọc/lưu dữ liệu, không chứa luật nghiệp vụ.
 * Mỗi phương thức tự mở/đóng Connection trong try-with-resources.
 */
public class TaiKhoanDAO {

    /** Tìm hồ sơ nhân viên và trạng thái cấp tài khoản theo từ khóa. */
    public List<NhanVien> timKiemHoSo(String tuKhoa) {
        if (tuKhoa == null || tuKhoa.isBlank()) return List.of();
        String sql = selectHoSoSql()
                + " WHERE nv.maNV LIKE ? OR nv.hoTen LIKE ? OR nv.soDienThoai LIKE ? OR tk.tenDangNhap LIKE ?"
                + " ORDER BY nv.maNV";
        String pattern = "%" + tuKhoa.trim() + "%";
        List<NhanVien> result = new ArrayList<>();
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) ps.setString(i, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapHoSo(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm kiếm hồ sơ tài khoản: " + e.getMessage(), e);
        }
        return result;
    }

    /** Lấy nhân viên chưa có tài khoản để người dùng chọn khi bấm Thêm tài khoản. */
    public List<NhanVien> layNhanVienChuaCoTaiKhoan() {
        String sql = selectHoSoSql() + " WHERE tk.maNV IS NULL ORDER BY nv.maNV";
        List<NhanVien> result = new ArrayList<>();
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapHoSo(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy hồ sơ chưa có tài khoản: " + e.getMessage(), e);
        }
        return result;
    }

    /** Tạo tài khoản bằng stored procedure để username được sinh nhất quán theo vai trò. */
    public String taoTaiKhoan(String maNV, String matKhauHash) {
        String sql = "{call sp_TaoTaiKhoan(?, ?, ?)}";
        try (Connection con = DbUtil.getConnection(); CallableStatement cs = con.prepareCall(sql)) {
            cs.setString(1, maNV);
            cs.setString(2, matKhauHash);
            cs.registerOutParameter(3, Types.VARCHAR);
            cs.execute();
            return cs.getString(3);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tạo tài khoản: " + e.getMessage(), e);
        }
    }

    /** Xóa tài khoản đăng nhập, giữ nguyên hồ sơ nhân viên. */
    public boolean xoaTaiKhoan(String tenDangNhap) {
        String sql = "DELETE FROM TaiKhoan WHERE tenDangNhap = ?";
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tenDangNhap);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi xóa tài khoản: " + e.getMessage(), e);
        }
    }

    private static String selectHoSoSql() {
        return "SELECT nv.maNV, nv.hoTen, nv.ngaySinh, nv.soDienThoai, nv.CCCD, nv.diaChi, nv.ngayVaoLam, nv.vaiTro, "
                + "tk.tenDangNhap, tk.trangThaiHoatDong "
                + "FROM NhanVien nv LEFT JOIN TaiKhoan tk ON tk.maNV = nv.maNV";
    }

    private static NhanVien mapHoSo(ResultSet rs) throws SQLException {
        NhanVien nv = new NhanVien();
        nv.setMaNVRaw(rs.getString("maNV"));
        nv.setHoTenRaw(rs.getString("hoTen"));
        Date ngaySinh = rs.getDate("ngaySinh");
        if (ngaySinh != null) nv.setNgaySinhRaw(ngaySinh.toLocalDate());
        nv.setSoDienThoaiRaw(rs.getString("soDienThoai"));
        nv.setCCCDRaw(rs.getString("CCCD"));
        nv.setDiaChi(rs.getString("diaChi"));
        Date ngayVaoLam = rs.getDate("ngayVaoLam");
        if (ngayVaoLam != null) nv.setNgayVaoLamRaw(ngayVaoLam.toLocalDate());
        nv.setVaiTroRaw(VaiTro.valueOf(rs.getString("vaiTro")));
        String tenDangNhap = rs.getString("tenDangNhap");
        if (tenDangNhap != null) {
            TaiKhoan tk = new TaiKhoan();
            tk.setNhanVienRaw(nv);
            tk.setTenDangNhapRaw(tenDangNhap);
            tk.setTrangThaiHoatDong(rs.getBoolean("trangThaiHoatDong"));
            nv.setTaiKhoan(tk);
        }
        return nv;
    }

    /**
     * Tìm TaiKhoan kèm NhanVien theo tên đăng nhập.
     *
     * @param tenDangNhap tên đăng nhập
     * @return TaiKhoan hoặc {@code null} nếu không tìm thấy
     */
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

    /**
     * Cập nhật mật khẩu đã băm (SHA-256) cho tài khoản.
     *
     * @param tenDangNhap tên đăng nhập
     * @param matKhauMoiHash chuỗi băm hex SHA-256 in hoa
     * @return true nếu cập nhật thành công
     */
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

    /**
     * Cập nhật trạng thái hoạt động tài khoản.
     *
     * @param tenDangNhap tên đăng nhập
     * @param trangThai true nếu đang hoạt động, false nếu bị khóa
     * @return true nếu cập nhật thành công
     */
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
