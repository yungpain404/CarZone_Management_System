package dao;

import connectDB.DbUtil;
import entity.NhanVien;
import enums.VaiTro;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho bảng Nhân viên (`NhanVien`) - JDBC thuần (DriverManager),
 * chỉ đọc/lưu dữ liệu, không chứa luật nghiệp vụ.
 */
public class NhanVienDao {

    /**
     * Tìm nhân viên theo mã nhân viên.
     *
     * @param maNV mã nhân viên (NVxxxxx)
     * @return NhanVien hoặc null nếu không tìm thấy
     */
    public NhanVien timTheoMa(String maNV) {
        String sql = "SELECT maNV, hoTen, ngaySinh, soDienThoai, CCCD, diaChi, ngayVaoLam, vaiTro " +
                     "FROM NhanVien WHERE maNV = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maNV);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm nhân viên theo mã: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy danh sách tất cả nhân viên.
     *
     * @return danh sách nhân viên
     */
    public List<NhanVien> layTatCa() {
        String sql = "SELECT maNV, hoTen, ngaySinh, soDienThoai, CCCD, diaChi, ngayVaoLam, vaiTro " +
                     "FROM NhanVien ORDER BY maNV";
        List<NhanVien> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách nhân viên: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Tìm danh sách nhân viên theo vai trò.
     *
     * @param vaiTro vai trò nhân viên
     * @return danh sách nhân viên thuộc vai trò
     */
    public List<NhanVien> timTheoVaiTro(VaiTro vaiTro) {
        String sql = "SELECT maNV, hoTen, ngaySinh, soDienThoai, CCCD, diaChi, ngayVaoLam, vaiTro " +
                     "FROM NhanVien WHERE vaiTro = ? ORDER BY maNV";
        List<NhanVien> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, vaiTro.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm nhân viên theo vai trò: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Thêm mới nhân viên vào CSDL.
     *
     * @param nv nhân viên cần thêm
     * @return true nếu thêm thành công
     */
    public boolean them(NhanVien nv) {
        String sql = "INSERT INTO NhanVien (maNV, hoTen, ngaySinh, soDienThoai, CCCD, diaChi, ngayVaoLam, vaiTro) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nv.getMaNV());
            ps.setString(2, nv.getHoTen());
            ps.setDate(3, nv.getNgaySinh() != null ? Date.valueOf(nv.getNgaySinh()) : null);
            ps.setString(4, nv.getSoDienThoai());
            ps.setString(5, nv.getCCCD());
            ps.setString(6, nv.getDiaChi());
            ps.setDate(7, nv.getNgayVaoLam() != null ? Date.valueOf(nv.getNgayVaoLam()) : null);
            ps.setString(8, nv.getVaiTro() != null ? nv.getVaiTro().name() : null);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi thêm nhân viên: " + e.getMessage(), e);
        }
    }

    /**
     * Cập nhật thông tin nhân viên.
     *
     * @param nv nhân viên cần cập nhật
     * @return true nếu cập nhật thành công
     */
    public boolean capNhat(NhanVien nv) {
        String sql = "UPDATE NhanVien SET hoTen = ?, ngaySinh = ?, soDienThoai = ?, CCCD = ?, " +
                     "diaChi = ?, ngayVaoLam = ?, vaiTro = ? WHERE maNV = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nv.getHoTen());
            ps.setDate(2, nv.getNgaySinh() != null ? Date.valueOf(nv.getNgaySinh()) : null);
            ps.setString(3, nv.getSoDienThoai());
            ps.setString(4, nv.getCCCD());
            ps.setString(5, nv.getDiaChi());
            ps.setDate(6, nv.getNgayVaoLam() != null ? Date.valueOf(nv.getNgayVaoLam()) : null);
            ps.setString(7, nv.getVaiTro() != null ? nv.getVaiTro().name() : null);
            ps.setString(8, nv.getMaNV());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật nhân viên: " + e.getMessage(), e);
        }
    }

    private NhanVien mapRow(ResultSet rs) throws SQLException {
        NhanVien nv = new NhanVien();
        nv.setMaNV(rs.getString("maNV"));
        nv.setHoTenRaw(rs.getString("hoTen"));
        Date ngaySinhSql = rs.getDate("ngaySinh");
        if (ngaySinhSql != null) nv.setNgaySinhRaw(ngaySinhSql.toLocalDate());
        nv.setSoDienThoaiRaw(rs.getString("soDienThoai"));
        nv.setCCCDRaw(rs.getString("CCCD"));
        nv.setDiaChi(rs.getString("diaChi"));
        Date ngayVaoLamSql = rs.getDate("ngayVaoLam");
        if (ngayVaoLamSql != null) nv.setNgayVaoLamRaw(ngayVaoLamSql.toLocalDate());
        nv.setVaiTroRaw(VaiTro.valueOf(rs.getString("vaiTro")));
        return nv;
    }
}
