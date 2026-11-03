package dao;

import connectDB.DbUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Lớp DAO JDBC thuần cho Phiếu tiếp nhận sửa chữa (PhieuTiepNhanSuaChua)
 * và các bảng chi tiết dịch vụ (ChiTietDichVu), chi tiết linh kiện (ChiTietLinhKien).
 * Luôn đóng tài nguyên qua try-with-resources.
 */
public class PhieuTiepNhanSuaChuaDAO {

    public record PhieuSuaChuaItem(
            String maPhieu,
            String bienSoXe,
            String hangModel,
            String maKH,
            String tenKhachHang,
            String soDienThoai,
            Timestamp ngayTiepNhan,
            String yeuCauBanDau,
            String trangThai,
            String maNVKyThuat,
            String tenKyThuatVien,
            String maNVTiepNhan,
            String tenTiepNhanVien,
            double tongTienDichVu,
            double tongTienLinhKien,
            double tongTien,
            Timestamp ngayHoanTat
    ) {}

    public record ChiTietDichVuItem(
            String maPhieu,
            String maDV,
            String tenDV,
            int soLuong,
            double donGiaThucHien,
            double thanhTien,
            String ghiChu
    ) {}

    public record ChiTietLinhKienItem(
            String maPhieu,
            String maLK,
            String tenLK,
            String donViTinh,
            int soLuong,
            double donGiaXuat,
            double thanhTien,
            String ghiChu
    ) {}

    public record ThongKeKTVData(
            int tongPhieu,
            int phieuHoanTat,
            int phieuDangSua,
            int phieuChoLinhKien,
            int tongHangMuc,
            double tongDoanhSo
    ) {}

    /**
     * Tìm kiếm danh sách phiếu sửa chữa phân công cho Kỹ thuật viên (hoặc phiếu chờ nhận).
     */
    public List<PhieuSuaChuaItem> timPhieuKTV(String maKTV, String tuKhoa, String trangThai, Date tuNgay, Date denNgay) {
        StringBuilder sql = new StringBuilder(
                "SELECT p.maPhieu, p.bienSoXe, x.hangXe + ' ' + ISNULL(x.model, '') AS hangModel, "
                        + "kh.maKH, kh.hoTen AS tenKhachHang, kh.soDienThoai, p.ngayGioTiepNhan, "
                        + "p.yeuCauBanDau, p.trangThai, p.maNVKyThuat, ktv.hoTen AS tenKyThuatVien, "
                        + "p.maNVTiepNhan, tnv.hoTen AS tenTiepNhanVien, "
                        + "ISNULL(t.tongTienDichVu, 0) AS tongTienDichVu, "
                        + "ISNULL(t.tongTienLinhKien, 0) AS tongTienLinhKien, "
                        + "ISNULL(t.tongTien, 0) AS tongTien, "
                        + "hd.ngayLap AS ngayHoanTat "
                        + "FROM PhieuTiepNhanSuaChua p "
                        + "JOIN Xe x ON x.bienSoXe = p.bienSoXe "
                        + "JOIN KhachHang kh ON kh.maKH = x.maKH "
                        + "JOIN NhanVien tnv ON tnv.maNV = p.maNVTiepNhan "
                        + "LEFT JOIN NhanVien ktv ON ktv.maNV = p.maNVKyThuat "
                        + "LEFT JOIN vw_PhieuSuaChua_TongTien t ON t.maPhieu = p.maPhieu "
                        + "LEFT JOIN HoaDon hd ON hd.maPhieu = p.maPhieu "
                        + "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (maKTV != null && !maKTV.isBlank()) {
            sql.append("AND (p.maNVKyThuat = ? OR (p.maNVKyThuat IS NULL AND p.trangThai = 'CHO_SUA')) ");
            params.add(maKTV.trim());
        }

        if (tuKhoa != null && !tuKhoa.isBlank()) {
            String pat = "%" + tuKhoa.trim() + "%";
            sql.append("AND (p.maPhieu LIKE ? OR p.bienSoXe LIKE ? OR kh.hoTen LIKE ? OR kh.soDienThoai LIKE ?) ");
            params.add(pat); params.add(pat); params.add(pat); params.add(pat);
        }

        if (trangThai != null && !trangThai.isBlank() && !"Tất cả".equalsIgnoreCase(trangThai)) {
            sql.append("AND p.trangThai = ? ");
            params.add(trangThai.trim());
        }

        if (tuNgay != null) {
            sql.append("AND p.ngayGioTiepNhan >= ? ");
            params.add(new Timestamp(tuNgay.getTime()));
        }

        if (denNgay != null) {
            sql.append("AND p.ngayGioTiepNhan <= ? ");
            params.add(new Timestamp(denNgay.getTime()));
        }

        sql.append("ORDER BY p.ngayGioTiepNhan DESC, p.maPhieu DESC");

        List<PhieuSuaChuaItem> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPhieu(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm kiếm phiếu sửa chữa: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Lấy thông tin một phiếu sửa chữa theo mã.
     */
    public PhieuSuaChuaItem layTheoMa(String maPhieu) {
        String sql = "SELECT p.maPhieu, p.bienSoXe, x.hangXe + ' ' + ISNULL(x.model, '') AS hangModel, "
                + "kh.maKH, kh.hoTen AS tenKhachHang, kh.soDienThoai, p.ngayGioTiepNhan, "
                + "p.yeuCauBanDau, p.trangThai, p.maNVKyThuat, ktv.hoTen AS tenKyThuatVien, "
                + "p.maNVTiepNhan, tnv.hoTen AS tenTiepNhanVien, "
                + "ISNULL(t.tongTienDichVu, 0) AS tongTienDichVu, "
                + "ISNULL(t.tongTienLinhKien, 0) AS tongTienLinhKien, "
                + "ISNULL(t.tongTien, 0) AS tongTien, "
                + "hd.ngayLap AS ngayHoanTat "
                + "FROM PhieuTiepNhanSuaChua p "
                + "JOIN Xe x ON x.bienSoXe = p.bienSoXe "
                + "JOIN KhachHang kh ON kh.maKH = x.maKH "
                + "JOIN NhanVien tnv ON tnv.maNV = p.maNVTiepNhan "
                + "LEFT JOIN NhanVien ktv ON ktv.maNV = p.maNVKyThuat "
                + "LEFT JOIN vw_PhieuSuaChua_TongTien t ON t.maPhieu = p.maPhieu "
                + "LEFT JOIN HoaDon hd ON hd.maPhieu = p.maPhieu "
                + "WHERE p.maPhieu = ?";

        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPhieu(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi đọc thông tin phiếu sửa chữa: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Chuyển trạng thái phiếu sửa chữa.
     * Kiểm soát theo đúng luật trigger DB trg_PTNSC_KiemTra:
     * CHO_SUA -> DANG_SUA ⇄ CHO_LINH_KIEN -> HOAN_TAT
     */
    public boolean chuyenTrangThai(String maPhieu, String trangThaiMoi, String maKTV) {
        String sql = "UPDATE PhieuTiepNhanSuaChua "
                + "SET trangThai = ?, "
                + "maNVKyThuat = CASE WHEN maNVKyThuat IS NULL THEN ? ELSE maNVKyThuat END "
                + "WHERE maPhieu = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, trangThaiMoi);
            ps.setString(2, maKTV);
            ps.setString(3, maPhieu);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi chuyển trạng thái phiếu: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHI TIẾT DỊCH VỤ
    // ─────────────────────────────────────────────────────────────────────────

    public List<ChiTietDichVuItem> layChiTietDichVu(String maPhieu) {
        String sql = "SELECT c.maPhieu, c.maDV, d.tenDV, c.soLuong, c.donGiaThucHien, c.thanhTien, c.ghiChu "
                + "FROM ChiTietDichVu c "
                + "JOIN DichVu d ON d.maDV = c.maDV "
                + "WHERE c.maPhieu = ? "
                + "ORDER BY d.tenDV";
        List<ChiTietDichVuItem> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ChiTietDichVuItem(
                            rs.getString("maPhieu"),
                            rs.getString("maDV"),
                            rs.getString("tenDV"),
                            rs.getInt("soLuong"),
                            rs.getDouble("donGiaThucHien"),
                            rs.getDouble("thanhTien"),
                            rs.getString("ghiChu")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi đọc chi tiết dịch vụ: " + e.getMessage(), e);
        }
        return list;
    }

    public void themChiTietDichVu(String maPhieu, String maDV, int soLuong, double donGia, String ghiChu) {
        String sql = "INSERT INTO ChiTietDichVu (maPhieu, maDV, soLuong, donGiaThucHien, ghiChu) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            ps.setString(2, maDV);
            ps.setInt(3, soLuong);
            ps.setDouble(4, donGia);
            ps.setString(5, ghiChu);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi thêm dịch vụ vào phiếu: " + e.getMessage(), e);
        }
    }

    public void xoaChiTietDichVu(String maPhieu, String maDV) {
        String sql = "DELETE FROM ChiTietDichVu WHERE maPhieu = ? AND maDV = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            ps.setString(2, maDV);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi xóa dịch vụ khỏi phiếu: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHI TIẾT LINH KIỆN
    // ─────────────────────────────────────────────────────────────────────────

    public List<ChiTietLinhKienItem> layChiTietLinhKien(String maPhieu) {
        String sql = "SELECT c.maPhieu, c.maLK, l.tenLK, l.donViTinh, c.soLuong, c.donGiaXuat, c.thanhTien, c.ghiChu "
                + "FROM ChiTietLinhKien c "
                + "JOIN LinhKien l ON l.maLK = c.maLK "
                + "WHERE c.maPhieu = ? "
                + "ORDER BY l.tenLK";
        List<ChiTietLinhKienItem> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ChiTietLinhKienItem(
                            rs.getString("maPhieu"),
                            rs.getString("maLK"),
                            rs.getString("tenLK"),
                            rs.getString("donViTinh"),
                            rs.getInt("soLuong"),
                            rs.getDouble("donGiaXuat"),
                            rs.getDouble("thanhTien"),
                            rs.getString("ghiChu")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi đọc chi tiết linh kiện: " + e.getMessage(), e);
        }
        return list;
    }

    public void themChiTietLinhKien(String maPhieu, String maLK, int soLuong, double donGia, String ghiChu) {
        String sql = "INSERT INTO ChiTietLinhKien (maPhieu, maLK, soLuong, donGiaXuat, ghiChu) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            ps.setString(2, maLK);
            ps.setInt(3, soLuong);
            ps.setDouble(4, donGia);
            ps.setString(5, ghiChu);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi thêm linh kiện vào phiếu: " + e.getMessage(), e);
        }
    }

    public void xoaChiTietLinhKien(String maPhieu, String maLK) {
        String sql = "DELETE FROM ChiTietLinhKien WHERE maPhieu = ? AND maLK = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            ps.setString(2, maLK);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi xóa linh kiện khỏi phiếu: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // THỐNG KÊ KỸ THUẬT VIÊN
    // ─────────────────────────────────────────────────────────────────────────

    public ThongKeKTVData layThongKeKTV(String maKTV, Date tuNgay, Date denNgay) {
        StringBuilder sql = new StringBuilder(
                "SELECT "
                        + "COUNT(p.maPhieu) AS tongPhieu, "
                        + "SUM(CASE WHEN p.trangThai = 'HOAN_TAT' THEN 1 ELSE 0 END) AS phieuHoanTat, "
                        + "SUM(CASE WHEN p.trangThai = 'DANG_SUA' THEN 1 ELSE 0 END) AS phieuDangSua, "
                        + "SUM(CASE WHEN p.trangThai = 'CHO_LINH_KIEN' THEN 1 ELSE 0 END) AS phieuChoLinhKien, "
                        + "ISNULL(SUM(t.tongTien), 0) AS tongDoanhSo "
                        + "FROM PhieuTiepNhanSuaChua p "
                        + "LEFT JOIN vw_PhieuSuaChua_TongTien t ON t.maPhieu = p.maPhieu "
                        + "WHERE p.maNVKyThuat = ? "
        );

        List<Object> params = new ArrayList<>();
        params.add(maKTV);

        if (tuNgay != null) {
            sql.append("AND p.ngayGioTiepNhan >= ? ");
            params.add(new Timestamp(tuNgay.getTime()));
        }
        if (denNgay != null) {
            sql.append("AND p.ngayGioTiepNhan <= ? ");
            params.add(new Timestamp(denNgay.getTime()));
        }

        int tongHangMuc = 0;
        // Đếm tổng hạng mục dịch vụ + linh kiện
        String sqlCountItems = "SELECT ( "
                + "(SELECT COUNT(*) FROM ChiTietDichVu cd JOIN PhieuTiepNhanSuaChua p ON p.maPhieu = cd.maPhieu WHERE p.maNVKyThuat = ?) "
                + "+ "
                + "(SELECT COUNT(*) FROM ChiTietLinhKien cl JOIN PhieuTiepNhanSuaChua p ON p.maPhieu = cl.maPhieu WHERE p.maNVKyThuat = ?) "
                + ") AS totalItems";

        try (Connection con = DbUtil.getConnection()) {
            try (PreparedStatement ps2 = con.prepareStatement(sqlCountItems)) {
                ps2.setString(1, maKTV);
                ps2.setString(2, maKTV);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    if (rs2.next()) {
                        tongHangMuc = rs2.getInt(1);
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new ThongKeKTVData(
                                rs.getInt("tongPhieu"),
                                rs.getInt("phieuHoanTat"),
                                rs.getInt("phieuDangSua"),
                                rs.getInt("phieuChoLinhKien"),
                                tongHangMuc,
                                rs.getDouble("tongDoanhSo")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tổng hợp thống kê sửa chữa KTV: " + e.getMessage(), e);
        }

        return new ThongKeKTVData(0, 0, 0, 0, 0, 0);
    }

    private static PhieuSuaChuaItem mapPhieu(ResultSet rs) throws SQLException {
        return new PhieuSuaChuaItem(
                rs.getString("maPhieu"),
                rs.getString("bienSoXe"),
                rs.getString("hangModel"),
                rs.getString("maKH"),
                rs.getString("tenKhachHang"),
                rs.getString("soDienThoai"),
                rs.getTimestamp("ngayGioTiepNhan"),
                rs.getString("yeuCauBanDau"),
                rs.getString("trangThai"),
                rs.getString("maNVKyThuat"),
                rs.getString("tenKyThuatVien"),
                rs.getString("maNVTiepNhan"),
                rs.getString("tenTiepNhanVien"),
                rs.getDouble("tongTienDichVu"),
                rs.getDouble("tongTienLinhKien"),
                rs.getDouble("tongTien"),
                rs.getTimestamp("ngayHoanTat")
        );
    }
}

