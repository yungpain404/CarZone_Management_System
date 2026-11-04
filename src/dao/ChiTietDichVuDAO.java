package dao;

import connectDB.DbUtil;
import entity.ChiTietDichVu;
import entity.DichVu;
import entity.PhieuTiepNhanSuaChua;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Triển khai ChiTietDichVuDao bằng JDBC thuần.
 */
public class ChiTietDichVuDAO {
    public List<ChiTietDichVu> layTheoPhieu(String maPhieu) {
        String sql = "SELECT c.maPhieu, c.maDV, c.soLuong, c.donGiaThucHien, c.ghiChu, " +
                     "       d.tenDV, d.donViTinh " +
                     "FROM ChiTietDichVu c " +
                     "JOIN DichVu d ON c.maDV = d.maDV " +
                     "WHERE c.maPhieu = ?";
        List<ChiTietDichVu> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChiTietDichVu ct = new ChiTietDichVu();
                    ct.setPhieuSuaChua(new PhieuTiepNhanSuaChua(rs.getString("maPhieu")));

                    DichVu dv = new DichVu(rs.getString("maDV"));
                    dv.setTenDV(rs.getString("tenDV"));
                    dv.setDonViTinh(rs.getString("donViTinh"));
                    dv.setDonGia(rs.getDouble("donGiaThucHien"));
                    ct.setDichVu(dv);

                    ct.setSoLuong(rs.getInt("soLuong"));
                    ct.setDonGiaThucHien(rs.getDouble("donGiaThucHien"));
                    ct.setGhiChu(rs.getString("ghiChu"));
                    list.add(ct);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy chi tiết dịch vụ: " + e.getMessage(), e);
        }
        return list;
    }
    public boolean them(ChiTietDichVu ctdv) {
        try (Connection con = DbUtil.getConnection()) {
            return them(ctdv, con);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi thêm chi tiết dịch vụ: " + e.getMessage(), e);
        }
    }
    public boolean them(ChiTietDichVu ctdv, Connection con) {
        String sql = "INSERT INTO ChiTietDichVu (maPhieu, maDV, soLuong, donGiaThucHien, ghiChu) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ctdv.getPhieuSuaChua().getMaPhieu());
            ps.setString(2, ctdv.getDichVu().getMaDV());
            ps.setInt(3, ctdv.getSoLuong());
            ps.setDouble(4, ctdv.getDonGiaThucHien());
            ps.setString(5, ctdv.getGhiChu());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lưu chi tiết dịch vụ: " + e.getMessage(), e);
        }
    }
    public boolean xoaTheoPhieu(String maPhieu) {
        String sql = "DELETE FROM ChiTietDichVu WHERE maPhieu = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            return ps.executeUpdate() >= 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi xóa chi tiết dịch vụ: " + e.getMessage(), e);
        }
    }
}

