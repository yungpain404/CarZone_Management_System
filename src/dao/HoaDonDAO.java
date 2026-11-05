package dao;

import connectDB.DbUtil;
import entity.HoaDon;
import entity.KhuyenMai;
import entity.NhanVien;
import entity.PhieuTiepNhanSuaChua;
import enums.PhuongThucThanhToan;
import enums.TrangThaiThanhToan;

import java.sql.Connection;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho bảng Hóa đơn (`HoaDon`) - JDBC thuần (DriverManager),
 * mỗi phương thức tự mở/đóng Connection trong try-with-resources.
 */
public class HoaDonDAO {

    private static final String SELECT_BASE =
            "SELECT h.maHoaDon, h.maPhieu, h.maNV, h.maKM, h.ngayLap, h.phuongThucTT, " +
            "       h.trangThaiThanhToan, h.thoiGianThanhToan, h.maGiaoDich, " +
            "       v.tongTienDichVu, v.tongTienLinhKien, v.tienGiam, v.thue, v.tongThanhToan " +
            "FROM HoaDon h " +
            "LEFT JOIN vw_HoaDon_TongTien v ON h.maHoaDon = v.maHoaDon ";

    /**
     * Tìm hóa đơn theo mã hóa đơn.
     */
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

    /**
     * Tìm hóa đơn theo mã phiếu sửa chữa.
     */
    public HoaDon timTheoPhieu(String maPhieu) {
        String sql = SELECT_BASE + "WHERE h.maPhieu = ?";
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapRow(rs) : null; }
        } catch (SQLException e) { throw new RuntimeException("Lỗi tìm hóa đơn theo phiếu: " + e.getMessage(), e); }
    }

    /**
     * Lấy danh sách tất cả hóa đơn.
     */
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

    /**
     * Lấy danh sách hóa đơn theo trạng thái thanh toán.
     */
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

    /**
     * Lấy danh sách các hóa đơn đã thanh toán và còn trong thời hạn đổi trả 7 ngày.
     */
    public List<HoaDon> layHoaDonDuDieuKienDoiTra() {
        String sql = SELECT_BASE + "WHERE h.trangThaiThanhToan = 'DA_THANH_TOAN' "
                + "AND DATEDIFF(day, h.ngayLap, SYSDATETIME()) BETWEEN 0 AND 7 ORDER BY h.ngayLap DESC";
        return truyVanDanhSach(sql, List.of());
    }

    /**
     * Lập hóa đơn mới - gọi stored procedure sp_LapHoaDon sinh mã HDddMMyyyyAXXX.
     * @return mã hóa đơn vừa sinh
     */
    public String lapHoaDon(String maPhieu, String maNV, String maKM) {
        String sql = "{call sp_LapHoaDon(?, ?, ?, ?)}";
        try (Connection con = DbUtil.getConnection(); CallableStatement cs = con.prepareCall(sql)) {
            cs.setString(1, maPhieu); cs.setString(2, maNV);
            if (maKM == null || maKM.isBlank()) cs.setNull(3, Types.VARCHAR); else cs.setString(3, maKM);
            cs.registerOutParameter(4, Types.CHAR); cs.execute();
            String code = cs.getString(4);
            return code == null ? null : code.trim();
        } catch (SQLException e) { throw new RuntimeException("Lỗi lập hóa đơn: " + e.getMessage(), e); }
    }

    /**
     * Thanh toán hóa đơn - gọi stored procedure sp_ThanhToanHoaDon.
     */
    public boolean thanhToanHoaDon(String maHoaDon, PhuongThucThanhToan phuongThucTT, String maGiaoDich) {
        if (phuongThucTT == PhuongThucThanhToan.CHUYEN_KHOAN && (maGiaoDich == null || maGiaoDich.isBlank()))
            throw new IllegalArgumentException("Thanh toán chuyển khoản bắt buộc phải có mã giao dịch.");
        String sql = "{call sp_ThanhToanHoaDon(?, ?, ?)}";
        try (Connection con = DbUtil.getConnection(); CallableStatement cs = con.prepareCall(sql)) {
            cs.setString(1, maHoaDon); cs.setString(2, phuongThucTT.name());
            if (phuongThucTT == PhuongThucThanhToan.CHUYEN_KHOAN) cs.setString(3, maGiaoDich.trim());
            else cs.setNull(3, Types.VARCHAR);
            cs.execute(); return true;
        } catch (SQLException e) { throw new RuntimeException("Lỗi thanh toán hóa đơn: " + e.getMessage(), e); }
    }

    /**
     * Tìm kiếm hóa đơn theo từ khóa (mã HD, mã phiếu, biển số xe, tên khách hàng).
     */
    public List<HoaDon> timKiem(String tuKhoa, TrangThaiThanhToan trangThai) {
        StringBuilder sql = new StringBuilder(SELECT_BASE)
                .append("LEFT JOIN PhieuTiepNhanSuaChua p ON p.maPhieu = h.maPhieu ")
                .append("LEFT JOIN Xe x ON x.bienSoXe = p.bienSoXe ")
                .append("LEFT JOIN KhachHang kh ON kh.maKH = x.maKH WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (trangThai != null) { sql.append("AND h.trangThaiThanhToan = ? "); params.add(trangThai.name()); }
        if (tuKhoa != null && !tuKhoa.isBlank()) {
            sql.append("AND (h.maHoaDon LIKE ? OR h.maPhieu LIKE ? OR x.bienSoXe LIKE ? OR kh.hoTen LIKE ? OR kh.soDienThoai LIKE ?) ");
            String pattern = "%" + tuKhoa.trim() + "%";
            for (int i = 0; i < 5; i++) params.add(pattern);
        }
        sql.append("ORDER BY h.ngayLap DESC");
        return truyVanDanhSach(sql.toString(), params);
    }

    private List<HoaDon> truyVanDanhSach(String sql, List<?> params) {
        List<HoaDon> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapRow(rs)); }
        } catch (SQLException e) { throw new RuntimeException("Lỗi truy vấn hóa đơn: " + e.getMessage(), e); }
        return list;
    }

    /**
     * Thêm mới hóa đơn vào CSDL.
     */
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

    public boolean capNhatKhuyenMai(String maHoaDon, String maKM) {
        String sql = "UPDATE HoaDon SET maKM=? WHERE maHoaDon=? AND trangThaiThanhToan='CHUA_THANH_TOAN'";
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (maKM == null || maKM.isBlank()) ps.setNull(1, Types.VARCHAR); else ps.setString(1, maKM.trim());
            ps.setString(2, maHoaDon.trim());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException("Lỗi cập nhật khuyến mãi: " + e.getMessage(), e); }
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
