package dao.impl;

import connectDB.DbUtil;
import dao.XeDao;
import entity.KhachHang;
import entity.Xe;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Triển khai XeDao bằng JDBC thuần.
 */
public class XeDaoImpl implements XeDao {

    private static final String SELECT_BASE =
            "SELECT x.bienSoXe, x.hangXe, x.model, x.mauSac, x.namSanXuat, x.soKmHienTai, x.maKH, " +
            "       kh.hoTen, kh.soDienThoai, kh.ghiChu, kh.CCCD, kh.thanhVienThanThiet, kh.diemTichLuy " +
            "FROM Xe x " +
            "LEFT JOIN KhachHang kh ON x.maKH = kh.maKH ";

    @Override
    public Xe timTheoBienSo(String bienSoXe) {
        String sql = SELECT_BASE + "WHERE x.bienSoXe = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, bienSoXe);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm xe theo biển số: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Xe> layTheoKhachHang(String maKH) {
        String sql = SELECT_BASE + "WHERE x.maKH = ? ORDER BY x.bienSoXe ASC";
        List<Xe> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maKH);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy xe theo khách hàng: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Xe> timKiem(String tuKhoa) {
        if (tuKhoa == null || tuKhoa.isBlank()) {
            return layTatCa();
        }
        String sql = SELECT_BASE +
                     "WHERE x.bienSoXe LIKE ? OR x.hangXe LIKE ? OR x.model LIKE ? OR kh.hoTen LIKE ? OR kh.soDienThoai LIKE ? " +
                     "ORDER BY x.bienSoXe ASC";
        String pattern = "%" + tuKhoa.trim() + "%";
        List<Xe> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) {
                ps.setString(i, pattern);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm kiếm xe: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Xe> layTatCa() {
        String sql = SELECT_BASE + "ORDER BY x.bienSoXe ASC";
        List<Xe> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy tất cả xe: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean them(Xe xe) {
        String sql = "INSERT INTO Xe (bienSoXe, hangXe, model, mauSac, namSanXuat, soKmHienTai, maKH) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, xe.getBienSoXe());
            ps.setString(2, xe.getHangXe());
            ps.setString(3, xe.getModel());
            ps.setString(4, xe.getMauSac());
            ps.setObject(5, xe.getNamSanXuat() > 0 ? xe.getNamSanXuat() : null, Types.INTEGER);
            ps.setInt(6, xe.getSoKmHienTai());
            ps.setString(7, xe.getChuXe() != null ? xe.getChuXe().getMaKH() : null);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("PK_Xe")) {
                throw new RuntimeException("Biển số xe này đã tồn tại trong hệ thống.");
            }
            throw new RuntimeException("Lỗi thêm thông tin xe: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean capNhat(Xe xe) {
        String sql = "UPDATE Xe SET hangXe = ?, model = ?, mauSac = ?, namSanXuat = ?, " +
                     "soKmHienTai = ?, maKH = ? WHERE bienSoXe = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, xe.getHangXe());
            ps.setString(2, xe.getModel());
            ps.setString(3, xe.getMauSac());
            ps.setObject(4, xe.getNamSanXuat() > 0 ? xe.getNamSanXuat() : null, Types.INTEGER);
            ps.setInt(5, xe.getSoKmHienTai());
            ps.setString(6, xe.getChuXe() != null ? xe.getChuXe().getMaKH() : null);
            ps.setString(7, xe.getBienSoXe());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật xe: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean kiemTraCoPhieu(String bienSoXe) {
        String sql = "SELECT 1 FROM PhieuTiepNhanSuaChua WHERE bienSoXe = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, bienSoXe);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra phiếu theo biển số xe: " + e.getMessage(), e);
        }
    }

    private Xe mapRow(ResultSet rs) throws SQLException {
        Xe xe = new Xe();
        xe.setBienSoXeRaw(rs.getString("bienSoXe"));
        xe.setHangXeRaw(rs.getString("hangXe"));
        xe.setModelRaw(rs.getString("model"));
        xe.setMauSacRaw(rs.getString("mauSac"));
        xe.setNamSanXuatRaw(rs.getInt("namSanXuat"));
        xe.setSoKmHienTaiRaw(rs.getInt("soKmHienTai"));

        String maKH = rs.getString("maKH");
        if (maKH != null) {
            KhachHang kh = new KhachHang();
            kh.setMaKHRaw(maKH);
            kh.setHoTenRaw(rs.getString("hoTen"));
            kh.setSoDienThoaiRaw(rs.getString("soDienThoai"));
            kh.setGhiChuRaw(rs.getString("ghiChu"));
            kh.setCCCDRaw(rs.getString("CCCD"));
            kh.setThanhVienThanThietRaw(rs.getBoolean("thanhVienThanThiet"));
            kh.setDiemTichLuyRaw(rs.getDouble("diemTichLuy"));
            xe.setChuXeRaw(kh);
        }
        return xe;
    }
}
