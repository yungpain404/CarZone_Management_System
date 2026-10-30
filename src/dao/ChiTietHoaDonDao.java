package dao;

import connectDB.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho chi tiết các khoản mục trên hóa đơn (linh kiện & dịch vụ) -
 * JDBC thuần truy vấn từ ChiTietDichVu và ChiTietLinhKien theo mã hóa đơn.
 */
public class ChiTietHoaDonDao {

    /**
     * DTO đại diện cho một dòng chi tiết hóa đơn (dịch vụ hoặc linh kiện).
     */
    public static class ChiTietItem {
        private String maHoaDon;
        private String loai; // "DICH_VU" hoặc "LINH_KIEN"
        private String maKhoanMuc;
        private String tenKhoanMuc;
        private int soLuong;
        private double donGia;
        private double thanhTien;

        public ChiTietItem(String maHoaDon, String loai, String maKhoanMuc, String tenKhoanMuc, int soLuong, double donGia, double thanhTien) {
            this.maHoaDon = maHoaDon;
            this.loai = loai;
            this.maKhoanMuc = maKhoanMuc;
            this.tenKhoanMuc = tenKhoanMuc;
            this.soLuong = soLuong;
            this.donGia = donGia;
            this.thanhTien = thanhTien;
        }

        public String getMaHoaDon() { return maHoaDon; }
        public String getLoai() { return loai; }
        public String getMaKhoanMuc() { return maKhoanMuc; }
        public String getTenKhoanMuc() { return tenKhoanMuc; }
        public int getSoLuong() { return soLuong; }
        public double getDonGia() { return donGia; }
        public double getThanhTien() { return thanhTien; }
    }

    /**
     * Lấy toàn bộ các dòng chi tiết của một hóa đơn theo mã hóa đơn.
     *
     * @param maHoaDon mã hóa đơn
     * @return danh sách các khoản mục chi tiết
     */
    public List<ChiTietItem> layChiTietTheoHoaDon(String maHoaDon) {
        List<ChiTietItem> list = new ArrayList<>();

        // 1. Chi tiết dịch vụ theo phiếu của hóa đơn
        String sqlDv =
                "SELECT h.maHoaDon, 'DICH_VU' AS loai, dv.maDV AS maKhoanMuc, dv.tenDV AS tenKhoanMuc, " +
                "       ct.soLuong, dv.donGia, (ct.soLuong * dv.donGia) AS thanhTien " +
                "FROM HoaDon h " +
                "INNER JOIN ChiTietDichVu ct ON ct.maPhieu = h.maPhieu " +
                "INNER JOIN DichVu dv ON dv.maDV = ct.maDV " +
                "WHERE h.maHoaDon = ?";

        // 2. Chi tiết linh kiện theo phiếu của hóa đơn
        String sqlLk =
                "SELECT h.maHoaDon, 'LINH_KIEN' AS loai, lk.maLK AS maKhoanMuc, lk.tenLK AS tenKhoanMuc, " +
                "       ct.soLuong, lk.donGia, (ct.soLuong * lk.donGia) AS thanhTien " +
                "FROM HoaDon h " +
                "INNER JOIN ChiTietLinhKien ct ON ct.maPhieu = h.maPhieu " +
                "INNER JOIN LinhKien lk ON lk.maLK = ct.maLK " +
                "WHERE h.maHoaDon = ?";

        try (Connection con = DbUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(sqlDv)) {
                ps.setString(1, maHoaDon);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new ChiTietItem(
                                rs.getString("maHoaDon"),
                                rs.getString("loai"),
                                rs.getString("maKhoanMuc"),
                                rs.getString("tenKhoanMuc"),
                                rs.getInt("soLuong"),
                                rs.getDouble("donGia"),
                                rs.getDouble("thanhTien")
                        ));
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(sqlLk)) {
                ps.setString(1, maHoaDon);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new ChiTietItem(
                                rs.getString("maHoaDon"),
                                rs.getString("loai"),
                                rs.getString("maKhoanMuc"),
                                rs.getString("tenKhoanMuc"),
                                rs.getInt("soLuong"),
                                rs.getDouble("donGia"),
                                rs.getDouble("thanhTien")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy chi tiết hóa đơn: " + e.getMessage(), e);
        }

        return list;
    }
}
