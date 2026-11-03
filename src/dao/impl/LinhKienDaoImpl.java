package dao.impl;

import connectDB.DbUtil;
import dao.LinhKienDao;
import entity.LinhKien;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Triển khai LinhKienDao bằng JDBC thuần.
 */
public class LinhKienDaoImpl implements LinhKienDao {

    private static final String SELECT_BASE =
            "SELECT maLK, tenLK, donGia, donViTinh, soLuongTon, dangKinhDoanh FROM LinhKien ";

    @Override
    public LinhKien timTheoMa(String maLK) {
        String sql = SELECT_BASE + "WHERE maLK = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maLK);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm linh kiện: " + e.getMessage(), e);
        }
    }

    @Override
    public List<LinhKien> layTatCaDangKinhDoanh() {
        String sql = SELECT_BASE + "WHERE dangKinhDoanh = 1 ORDER BY tenLK ASC";
        List<LinhKien> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách linh kiện đang kinh doanh: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<LinhKien> layTatCa() {
        String sql = SELECT_BASE + "ORDER BY maLK ASC";
        List<LinhKien> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy tất cả linh kiện: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<LinhKien> timKiem(String tuKhoa, Boolean dangKinhDoanh) {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (tuKhoa != null && !tuKhoa.isBlank()) {
            sql.append("AND (maLK LIKE ? OR tenLK LIKE ?) ");
            String p = "%" + tuKhoa.trim() + "%";
            params.add(p);
            params.add(p);
        }
        if (dangKinhDoanh != null) {
            sql.append("AND dangKinhDoanh = ? ");
            params.add(dangKinhDoanh ? 1 : 0);
        }
        sql.append("ORDER BY maLK ASC");

        List<LinhKien> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm kiếm linh kiện: " + e.getMessage(), e);
        }
        return list;
    }

    private LinhKien mapRow(ResultSet rs) throws SQLException {
        LinhKien lk = new LinhKien(rs.getString("maLK"));
        lk.setTenLK(rs.getString("tenLK"));
        lk.setDonGia(rs.getDouble("donGia"));
        lk.setDonViTinh(rs.getString("donViTinh"));
        lk.setSoLuongTon(rs.getInt("soLuongTon"));
        lk.setDangKinhDoanh(rs.getBoolean("dangKinhDoanh"));
        return lk;
    }
}
