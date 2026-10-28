package dao.impl;

import connectDB.DbUtil;
import dao.HoaDonDao;
import entity.HoaDon;
import entity.KhuyenMai;
import entity.NhanVien;
import entity.PhieuTiepNhanSuaChua;
import enums.PhuongThucThanhToan;
import enums.TrangThaiThanhToan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Triển khai {@link HoaDonDao} bằng JDBC thuần (DriverManager).
 */
public class HoaDonDaoImpl implements HoaDonDao {

    private static final String SELECT_BASE =
            "SELECT h.maHoaDon, h.maPhieu, h.maNV, h.maKM, h.ngayLap, h.phuongThucTT, " +
            "       h.trangThaiThanhToan, h.thoiGianThanhToan, h.maGiaoDich, " +
            "       v.tongTienDichVu, v.tongTienLinhKien, v.tienGiam, v.thue, v.tongThanhToan " +
            "FROM HoaDon h " +
            "LEFT JOIN vw_HoaDon_TongTien v ON h.maHoaDon = v.maHoaDon ";

    @Override
    public HoaDon timTheoMa(String maHoaDon) {
        String sql = SELECT_BASE + "WHERE h.maHoaDon = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maHoaDon);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm hóa đơn theo mã: " + e.getMessage(), e);
        }
    }

    @Override
    public List<HoaDon> layTatCa() {
        String sql = SELECT_BASE + "ORDER BY h.ngayLap DESC";
        List<HoaDon> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách hóa đơn: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<HoaDon> timTheoTrangThai(TrangThaiThanhToan trangThai) {
        String sql = SELECT_BASE + "WHERE h.trangThaiThanhToan = ? ORDER BY h.ngayLap DESC";
        List<HoaDon> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, trangThai.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm hóa đơn theo trạng thái: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean them(HoaDon hd) {
        String sql = "INSERT INTO HoaDon (maHoaDon, maPhieu, maNV, maKM, ngayLap, phuongThucTT, trangThaiThanhToan, thoiGianThanhToan, maGiaoDich) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, hd.getMaHoaDon());
            ps.setString(2, hd.getPhieuSuaChua() != null ? hd.getPhieuSuaChua().getMaPhieu() : null);
            ps.setString(3, hd.getNhanVien() != null ? hd.getNhanVien().getMaNV() : null);
            ps.setString(4, hd.getKhuyenMai() != null ? hd.getKhuyenMai().getMaKM() : null);
            ps.setTimestamp(5, hd.getNgayLap() != null ? new Timestamp(hd.getNgayLap().getTime()) : new Timestamp(System.currentTimeMillis()));
            ps.setString(6, hd.getPhuongThucTT() != null ? hd.getPhuongThucTT().name() : null);
            ps.setString(7, hd.getTrangThaiThanhToan() != null ? hd.getTrangThaiThanhToan().name() : TrangThaiThanhToan.CHUA_THANH_TOAN.name());
            ps.setTimestamp(8, hd.getThoiGianThanhToan() != null ? new Timestamp(hd.getThoiGianThanhToan().getTime()) : null);
            ps.setString(9, hd.getMaGiaoDich());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi thêm hóa đơn: " + e.getMessage(), e);
        }
    }

    private HoaDon mapRow(ResultSet rs) throws SQLException {
        HoaDon hd = new HoaDon(rs.getString("maHoaDon"));

        String maPhieu = rs.getString("maPhieu");
        if (maPhieu != null) hd.setPhieuSuaChua(new PhieuTiepNhanSuaChua(maPhieu));

        String maNV = rs.getString("maNV");
        if (maNV != null) hd.setNhanVien(new NhanVien(maNV));

        String maKM = rs.getString("maKM");
        if (maKM != null) hd.setKhuyenMai(new KhuyenMai(maKM));

        Timestamp ngayLap = rs.getTimestamp("ngayLap");
        if (ngayLap != null) hd.setNgayLap(new java.util.Date(ngayLap.getTime()));

        String pt = rs.getString("phuongThucTT");
        if (pt != null) hd.setPhuongThucTT(PhuongThucThanhToan.valueOf(pt));

        String tt = rs.getString("trangThaiThanhToan");
        if (tt != null) hd.setTrangThaiThanhToan(TrangThaiThanhToan.valueOf(tt));

        Timestamp thoiGianTT = rs.getTimestamp("thoiGianThanhToan");
        if (thoiGianTT != null) hd.setThoiGianThanhToan(new java.util.Date(thoiGianTT.getTime()));

        String maGD = rs.getString("maGiaoDich");
        if (maGD != null) hd.setMaGiaoDich(maGD);

        // Computed values from view vw_HoaDon_TongTien
        hd.setTongTienDichVuRaw(rs.getDouble("tongTienDichVu"));
        hd.setTongTienLinhKienRaw(rs.getDouble("tongTienLinhKien"));
        hd.setTienGiamRaw(rs.getDouble("tienGiam"));
        hd.setThueRaw(rs.getDouble("thue"));
        hd.setTongThanhToanRaw(rs.getDouble("tongThanhToan"));

        return hd;
    }
}
