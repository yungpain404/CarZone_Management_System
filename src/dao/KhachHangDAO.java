package dao;

import connectDB.DbUtil;
import entity.KhachHang;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Triển khai KhachHangDao bằng JDBC thuần.
 */
public class KhachHangDAO {

    private static final String SELECT_BASE =
            "SELECT maKH, hoTen, soDienThoai, ghiChu, CCCD, thanhVienThanThiet, diemTichLuy " +
            "FROM KhachHang ";
    public KhachHang timTheoMa(String maKH) {
        String sql = SELECT_BASE + "WHERE maKH = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maKH);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm khách hàng theo mã: " + e.getMessage(), e);
        }
    }
    public KhachHang timTheoSoDienThoai(String soDienThoai) {
        String sql = SELECT_BASE + "WHERE soDienThoai = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, soDienThoai);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm khách hàng theo số điện thoại: " + e.getMessage(), e);
        }
    }
    public List<KhachHang> timKiem(String tuKhoa) {
        if (tuKhoa == null || tuKhoa.isBlank()) {
            return layTatCa();
        }
        String sql = SELECT_BASE +
                     "WHERE hoTen LIKE ? OR soDienThoai LIKE ? OR CCCD LIKE ? " +
                     "ORDER BY hoTen ASC";
        String pattern = "%" + tuKhoa.trim() + "%";
        List<KhachHang> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm kiếm khách hàng: " + e.getMessage(), e);
        }
        return list;
    }
    public List<KhachHang> layTatCa() {
        String sql = SELECT_BASE + "ORDER BY maKH DESC";
        List<KhachHang> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách khách hàng: " + e.getMessage(), e);
        }
        return list;
    }
    public String them(KhachHang kh) {
        String sql = "INSERT INTO KhachHang (hoTen, soDienThoai, ghiChu, CCCD, thanhVienThanThiet, diemTichLuy) " +
                     "OUTPUT INSERTED.maKH " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, kh.getHoTen());
            ps.setString(2, kh.getSoDienThoai());
            ps.setString(3, kh.getGhiChu());
            ps.setString(4, kh.getCCCD());
            ps.setBoolean(5, kh.isThanhVienThanThiet());
            ps.setDouble(6, kh.getDiemTichLuy());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String maKH = rs.getString(1);
                    kh.setMaKHRaw(maKH);
                    return maKH;
                }
            }
            throw new RuntimeException("Không nhận được mã khách hàng được sinh.");
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UQ_KhachHang_soDienThoai")) {
                throw new RuntimeException("Số điện thoại này đã được đăng ký cho khách hàng khác.");
            }
            if (e.getMessage() != null && e.getMessage().contains("UX_KhachHang_CCCD")) {
                throw new RuntimeException("Số CCCD này đã tồn tại trong hệ thống.");
            }
            throw new RuntimeException("Lỗi thêm khách hàng: " + e.getMessage(), e);
        }
    }
    public boolean capNhat(KhachHang kh) {
        String sql = "UPDATE KhachHang SET hoTen = ?, soDienThoai = ?, ghiChu = ?, CCCD = ?, " +
                     "thanhVienThanThiet = ?, diemTichLuy = ? WHERE maKH = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, kh.getHoTen());
            ps.setString(2, kh.getSoDienThoai());
            ps.setString(3, kh.getGhiChu());
            ps.setString(4, kh.getCCCD());
            ps.setBoolean(5, kh.isThanhVienThanThiet());
            ps.setDouble(6, kh.getDiemTichLuy());
            ps.setString(7, kh.getMaKH());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UQ_KhachHang_soDienThoai")) {
                throw new RuntimeException("Số điện thoại này đã được đăng ký cho khách hàng khác.");
            }
            if (e.getMessage() != null && e.getMessage().contains("UX_KhachHang_CCCD")) {
                throw new RuntimeException("Số CCCD này đã tồn tại trong hệ thống.");
            }
            throw new RuntimeException("Lỗi cập nhật khách hàng: " + e.getMessage(), e);
        }
    }
    public boolean kiemTraCoXeHoacPhieu(String maKH) {
        String sql = "SELECT 1 WHERE EXISTS (SELECT 1 FROM Xe WHERE maKH = ?) " +
                     "OR EXISTS (SELECT 1 FROM PhieuTiepNhanSuaChua p JOIN Xe x ON p.bienSoXe = x.bienSoXe WHERE x.maKH = ?)";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maKH);
            ps.setString(2, maKH);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra quan hệ khách hàng: " + e.getMessage(), e);
        }
    }

    private KhachHang mapRow(ResultSet rs) throws SQLException {
        KhachHang kh = new KhachHang();
        kh.setMaKHRaw(rs.getString("maKH"));
        kh.setHoTenRaw(rs.getString("hoTen"));
        kh.setSoDienThoaiRaw(rs.getString("soDienThoai"));
        kh.setGhiChuRaw(rs.getString("ghiChu"));
        kh.setCCCDRaw(rs.getString("CCCD"));
        kh.setThanhVienThanThietRaw(rs.getBoolean("thanhVienThanThiet"));
        kh.setDiemTichLuyRaw(rs.getDouble("diemTichLuy"));
        return kh;
    }
}

