package dao.impl;

import connectDB.DbUtil;
import dao.DichVuDao;
import entity.DichVu;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Triển khai DichVuDao bằng JDBC thuần.
 */
public class DichVuDaoImpl implements DichVuDao {

    private static final String SELECT_BASE =
            "SELECT maDV, tenDV, donGia, donViTinh, moTa, dangCungCap FROM DichVu ";

    @Override
    public DichVu timTheoMa(String maDV) {
        String sql = SELECT_BASE + "WHERE maDV = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maDV);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm dịch vụ: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DichVu> layTatCaDangCungCap() {
        String sql = SELECT_BASE + "WHERE dangCungCap = 1 ORDER BY tenDV ASC";
        List<DichVu> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách dịch vụ đang cung cấp: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<DichVu> layTatCa() {
        String sql = SELECT_BASE + "ORDER BY maDV ASC";
        List<DichVu> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy tất cả dịch vụ: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<DichVu> timKiem(String tuKhoa, Boolean dangCungCap) {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (tuKhoa != null && !tuKhoa.isBlank()) {
            sql.append("AND (maDV LIKE ? OR tenDV LIKE ?) ");
            String p = "%" + tuKhoa.trim() + "%";
            params.add(p);
            params.add(p);
        }
        if (dangCungCap != null) {
            sql.append("AND dangCungCap = ? ");
            params.add(dangCungCap ? 1 : 0);
        }
        sql.append("ORDER BY maDV ASC");

        List<DichVu> list = new ArrayList<>();
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
            throw new RuntimeException("Lỗi tìm kiếm dịch vụ: " + e.getMessage(), e);
        }
        return list;
    }

    private DichVu mapRow(ResultSet rs) throws SQLException {
        DichVu dv = new DichVu(rs.getString("maDV"));
        dv.setTenDV(rs.getString("tenDV"));
        dv.setDonGia(rs.getDouble("donGia"));
        dv.setDonViTinh(rs.getString("donViTinh"));
        dv.setMoTa(rs.getString("moTa"));
        dv.setDangCungCap(rs.getBoolean("dangCungCap"));
        return dv;
    }
}
