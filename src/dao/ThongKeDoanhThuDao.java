package dao;

import connectDB.DbUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO đọc dữ liệu thống kê doanh thu (chỉ đọc, không ghi) phục vụ màn hình
 * Báo cáo doanh thu của Chủ cửa hàng (Figma 50:1634) - JDBC thuần (DriverManager qua {@link DbUtil}).
 * Toàn bộ là SELECT chỉ đọc trên view {@code vw_HoaDon_TongTien} và {@code vw_PhieuDoiTra_HoanTien};
 * không tính lại thuế/khuyến mãi ở Java.
 */
public class ThongKeDoanhThuDao {

    /** Tổng hợp doanh thu kỳ: [soHoaDon, tongThu, tienMat, chuyenKhoan]. */
    public List<Object[]> layTongHopDoanhThu(LocalDate tuNgay, LocalDate denNgay) {
        String sql = "SELECT COUNT(*), COALESCE(SUM(v.tongThanhToan),0), "
                + "COALESCE(SUM(CASE WHEN h.phuongThucTT='TIEN_MAT' THEN v.tongThanhToan ELSE 0 END),0), "
                + "COALESCE(SUM(CASE WHEN h.phuongThucTT='CHUYEN_KHOAN' THEN v.tongThanhToan ELSE 0 END),0) "
                + "FROM HoaDon h JOIN vw_HoaDon_TongTien v ON v.maHoaDon=h.maHoaDon "
                + "WHERE h.trangThaiThanhToan='DA_THANH_TOAN' AND h.thoiGianThanhToan>=? "
                + "AND h.thoiGianThanhToan<DATEADD(DAY,1,?)";
        return query(sql, ngay(tuNgay), ngay(denNgay));
    }

    /** Số tiền hoàn tiền theo 4 khung giờ: [khungGio(0..3), soTien]. */
    public List<Object[]> layHoanTienTheoKhungGio(LocalDate tuNgay, LocalDate denNgay) {
        String sql = "SELECT CASE WHEN DATEPART(HOUR,r.ngayLap)<10 THEN 0 "
                + "WHEN DATEPART(HOUR,r.ngayLap)<12 THEN 1 WHEN DATEPART(HOUR,r.ngayLap)<15 THEN 2 ELSE 3 END, "
                + "COALESCE(SUM(r.soTienHoanTra),0) FROM vw_PhieuDoiTra_HoanTien r "
                + "WHERE r.ngayLap>=? AND r.ngayLap<DATEADD(DAY,1,?) "
                + "AND ((DATEPART(HOUR,r.ngayLap)>=8 AND DATEPART(HOUR,r.ngayLap)<12) "
                + "OR (DATEPART(HOUR,r.ngayLap)>=13 AND DATEPART(HOUR,r.ngayLap)<18)) GROUP BY CASE "
                + "WHEN DATEPART(HOUR,r.ngayLap)<10 THEN 0 WHEN DATEPART(HOUR,r.ngayLap)<12 THEN 1 "
                + "WHEN DATEPART(HOUR,r.ngayLap)<15 THEN 2 ELSE 3 END";
        return query(sql, ngay(tuNgay), ngay(denNgay));
    }

    /** Tổng số tiền hoàn tiền của kỳ (giá trị đơn vị tiền). */
    public Object layTongHoanTien(LocalDate tuNgay, LocalDate denNgay) {
        String sql = "SELECT COALESCE(SUM(r.soTienHoanTra),0) FROM vw_PhieuDoiTra_HoanTien r "
                + "WHERE r.ngayLap>=? AND r.ngayLap<DATEADD(DAY,1,?)";
        List<Object[]> rows = query(sql, ngay(tuNgay), ngay(denNgay));
        if (rows.isEmpty() || rows.get(0).length == 0 || rows.get(0)[0] == null) return 0;
        return rows.get(0)[0];
    }

    /** Doanh thu theo khung giờ: [khungGio(0..3), soHoaDon, tienMat, chuyenKhoan]. */
    public List<Object[]> layDoanhThuTheoKhungGio(LocalDate tuNgay, LocalDate denNgay) {
        String sql = "SELECT CASE WHEN DATEPART(HOUR,h.thoiGianThanhToan)<10 THEN 0 "
                + "WHEN DATEPART(HOUR,h.thoiGianThanhToan)<12 THEN 1 WHEN DATEPART(HOUR,h.thoiGianThanhToan)<15 THEN 2 ELSE 3 END, "
                + "COUNT(*), COALESCE(SUM(CASE WHEN h.phuongThucTT='TIEN_MAT' THEN v.tongThanhToan ELSE 0 END),0), "
                + "COALESCE(SUM(CASE WHEN h.phuongThucTT='CHUYEN_KHOAN' THEN v.tongThanhToan ELSE 0 END),0) "
                + "FROM HoaDon h JOIN vw_HoaDon_TongTien v ON v.maHoaDon=h.maHoaDon "
                + "WHERE h.trangThaiThanhToan='DA_THANH_TOAN' AND h.thoiGianThanhToan>=? "
                + "AND h.thoiGianThanhToan<DATEADD(DAY,1,?) AND DATEPART(HOUR,h.thoiGianThanhToan)>=8 "
                + "AND ((DATEPART(HOUR,h.thoiGianThanhToan)>=8 AND DATEPART(HOUR,h.thoiGianThanhToan)<12) "
                + "OR (DATEPART(HOUR,h.thoiGianThanhToan)>=13 AND DATEPART(HOUR,h.thoiGianThanhToan)<18)) GROUP BY CASE "
                + "WHEN DATEPART(HOUR,h.thoiGianThanhToan)<10 THEN 0 WHEN DATEPART(HOUR,h.thoiGianThanhToan)<12 THEN 1 "
                + "WHEN DATEPART(HOUR,h.thoiGianThanhToan)<15 THEN 2 ELSE 3 END";
        return query(sql, ngay(tuNgay), ngay(denNgay));
    }

    /** Tổng hợp phiếu tiếp nhận: [soXe, tongPhieu, hoanTat, dangSua, choLinhKien, choSua]. */
    public List<Object[]> layTongHopPhieu(LocalDate tuNgay, LocalDate denNgay) {
        String sql = "SELECT COUNT(DISTINCT bienSoXe),COUNT(*), "
                + "SUM(CASE WHEN trangThai='HOAN_TAT' THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN trangThai='DANG_SUA' THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN trangThai='CHO_LINH_KIEN' THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN trangThai='CHO_SUA' THEN 1 ELSE 0 END) FROM PhieuTiepNhanSuaChua "
                + "WHERE ngayGioTiepNhan>=? AND ngayGioTiepNhan<DATEADD(DAY,1,?)";
        return query(sql, ngay(tuNgay), ngay(denNgay));
    }

    /** Số phiếu theo trạng thái: [trangThai, soLuong, soXe]. */
    public List<Object[]> layPhieuTheoTrangThai(LocalDate tuNgay, LocalDate denNgay) {
        String sql = "SELECT trangThai,COUNT(*),COUNT(DISTINCT bienSoXe) FROM PhieuTiepNhanSuaChua "
                + "WHERE ngayGioTiepNhan>=? AND ngayGioTiepNhan<DATEADD(DAY,1,?) "
                + "GROUP BY trangThai ORDER BY trangThai";
        return query(sql, ngay(tuNgay), ngay(denNgay));
    }

    /** Top 20 dịch vụ/linh kiện sử dụng nhiều: [loai, ma, ten, soPhieu, soLuong, thanhTien]. */
    public List<Object[]> layDichVuVaLinhKien(LocalDate tuNgay, LocalDate denNgay) {
        // 2 tham số cho nhánh Dịch vụ + 2 tham số cho nhánh Linh kiện (4 tham số tổng cộng)
        String sql = "SELECT TOP 20 loai,ma,ten,COUNT(DISTINCT maPhieu),SUM(soLuong),SUM(thanhTien) FROM ("
                + "SELECT N'Dịch vụ' loai,d.maDV ma,d.tenDV ten,c.maPhieu,c.soLuong,c.thanhTien "
                + "FROM ChiTietDichVu c JOIN DichVu d ON d.maDV=c.maDV JOIN PhieuTiepNhanSuaChua p ON p.maPhieu=c.maPhieu "
                + "WHERE p.ngayGioTiepNhan>=? AND p.ngayGioTiepNhan<DATEADD(DAY,1,?) UNION ALL "
                + "SELECT N'Linh kiện',l.maLK,l.tenLK,c.maPhieu,c.soLuong,c.thanhTien FROM ChiTietLinhKien c "
                + "JOIN LinhKien l ON l.maLK=c.maLK JOIN PhieuTiepNhanSuaChua p ON p.maPhieu=c.maPhieu "
                + "WHERE p.ngayGioTiepNhan>=? AND p.ngayGioTiepNhan<DATEADD(DAY,1,?)) x "
                + "GROUP BY loai,ma,ten ORDER BY SUM(thanhTien) DESC";
        return query(sql, ngay(tuNgay), ngay(denNgay), ngay(tuNgay), ngay(denNgay));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /** Chuyển {@link LocalDate} sang {@link Date} để binding an toàn với cột DATE/DATETIME2. */
    private static Date ngay(LocalDate ngay) {
        return Date.valueOf(ngay);
    }

    /**
     * Chạy truy vấn SELECT chỉ đọc với các tham số positional (?),
     * mỗi dòng ResultSet trả về một {@code Object[]} (giá trị cột theo thứ tự).
     */
    private List<Object[]> query(String sql, Object... params) {
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Object[]> rows = new ArrayList<>();
                int cot = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Object[] dong = new Object[cot];
                    for (int i = 1; i <= cot; i++) {
                        dong[i - 1] = rs.getObject(i);
                    }
                    rows.add(dong);
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn thống kê doanh thu: " + e.getMessage(), e);
        }
    }
}
