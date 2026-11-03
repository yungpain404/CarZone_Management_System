package dao.impl;

import connectDB.DbUtil;
import dao.KhuyenMaiDao;
import entity.KhuyenMai;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Triển khai KhuyenMaiDao bằng JDBC thuần.
 */
public class KhuyenMaiDaoImpl implements KhuyenMaiDao {

    private static final String SELECT_BASE =
            "SELECT maKM, tenChuongTrinh, mucGiamPhanTram, ngayBatDau, ngayKetThuc, ghiChu " +
            "FROM KhuyenMai ";

    @Override
    public KhuyenMai timTheoMa(String maKM) {
        String sql = SELECT_BASE + "WHERE maKM = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maKM);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm khuyến mãi theo mã: " + e.getMessage(), e);
        }
    }

    @Override
    public List<KhuyenMai> layKhuyenMaiHieuLuc() {
        String sql = SELECT_BASE +
                     "WHERE CAST(SYSDATETIME() AS DATE) BETWEEN ngayBatDau AND ngayKetThuc " +
                     "ORDER BY mucGiamPhanTram DESC";
        List<KhuyenMai> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy khuyến mãi hiệu lực: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<KhuyenMai> layTatCa() {
        String sql = SELECT_BASE + "ORDER BY ngayBatDau DESC";
        List<KhuyenMai> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy tất cả khuyến mãi: " + e.getMessage(), e);
        }
        return list;
    }

    private KhuyenMai mapRow(ResultSet rs) throws SQLException {
        KhuyenMai km = new KhuyenMai(rs.getString("maKM"));
        km.setTenChuongTrinh(rs.getString("tenChuongTrinh"));
        km.setMucGiamPhanTram(rs.getDouble("mucGiamPhanTram"));
        Date d1 = rs.getDate("ngayBatDau");
        if (d1 != null) km.setNgayBatDau(new java.util.Date(d1.getTime()));
        Date d2 = rs.getDate("ngayKetThuc");
        if (d2 != null) km.setNgayKetThuc(new java.util.Date(d2.getTime()));
        km.setGhiChu(rs.getString("ghiChu"));
        return km;
    }
}
