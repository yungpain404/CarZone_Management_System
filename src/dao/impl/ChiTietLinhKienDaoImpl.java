package dao.impl;

import connectDB.DbUtil;
import dao.ChiTietLinhKienDao;
import entity.ChiTietLinhKien;
import entity.LinhKien;
import entity.PhieuTiepNhanSuaChua;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Triển khai ChiTietLinhKienDao bằng JDBC thuần.
 */
public class ChiTietLinhKienDaoImpl implements ChiTietLinhKienDao {

    @Override
    public List<ChiTietLinhKien> layTheoPhieu(String maPhieu) {
        String sql = "SELECT c.maPhieu, c.maLK, c.soLuong, c.donGiaXuat, c.ghiChu, " +
                     "       l.tenLK, l.donViTinh " +
                     "FROM ChiTietLinhKien c " +
                     "JOIN LinhKien l ON c.maLK = l.maLK " +
                     "WHERE c.maPhieu = ?";
        List<ChiTietLinhKien> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChiTietLinhKien ct = new ChiTietLinhKien();
                    ct.setPhieuSuaChua(new PhieuTiepNhanSuaChua(rs.getString("maPhieu")));

                    LinhKien lk = new LinhKien(rs.getString("maLK"));
                    lk.setTenLK(rs.getString("tenLK"));
                    lk.setDonViTinh(rs.getString("donViTinh"));
                    lk.setDonGia(rs.getDouble("donGiaXuat"));
                    ct.setLinhKien(lk);

                    ct.setSoLuong(rs.getInt("soLuong"));
                    ct.setDonGiaXuat(rs.getDouble("donGiaXuat"));
                    ct.setGhiChu(rs.getString("ghiChu"));
                    list.add(ct);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy chi tiết linh kiện: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean them(ChiTietLinhKien ctlk) {
        try (Connection con = DbUtil.getConnection()) {
            return them(ctlk, con);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi thêm chi tiết linh kiện: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean them(ChiTietLinhKien ctlk, Connection con) {
        String sql = "INSERT INTO ChiTietLinhKien (maPhieu, maLK, soLuong, donGiaXuat, ghiChu) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ctlk.getPhieuSuaChua().getMaPhieu());
            ps.setString(2, ctlk.getLinhKien().getMaLK());
            ps.setInt(3, ctlk.getSoLuong());
            ps.setDouble(4, ctlk.getDonGiaXuat());
            ps.setString(5, ctlk.getGhiChu());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lưu chi tiết linh kiện: " + e.getMessage(), e);
        }
    }
}
