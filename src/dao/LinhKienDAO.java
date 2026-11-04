package dao;

import connectDB.DbUtil;
import entity.LinhKien;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** JDBC DAO tra cứu và quản lý danh mục linh kiện. */
public class LinhKienDAO {

    public List<LinhKien> layTatCaDangKinhDoanh() { return timKiem(null, true, null); }

    public List<LinhKien> layTatCa() { return timKiem(null, null, null); }

    public List<LinhKien> timKiem(String tuKhoa, Boolean dangKinhDoanh) {
        return timKiem(tuKhoa, dangKinhDoanh, null);
    }

    /** Tìm theo mã/tên; hai bộ lọc null lần lượt nghĩa là mọi trạng thái kinh doanh/tồn kho. */
    public List<LinhKien> timKiem(String tuKhoa, Boolean dangKinhDoanh, Boolean conTon) {
        String sql = "SELECT maLK, tenLK, donGia, donViTinh, soLuongTon, dangKinhDoanh FROM LinhKien "
                + "WHERE (? IS NULL OR maLK LIKE ? OR tenLK LIKE ?) "
                + "AND (? = -1 OR CAST(dangKinhDoanh AS INT) = ?) "
                + "AND (? = -1 OR CASE WHEN soLuongTon > 0 THEN 1 ELSE 0 END = ?) "
                + "ORDER BY tenLK, maLK";
        String pattern = tuKhoa == null || tuKhoa.isBlank() ? null : "%" + tuKhoa.trim() + "%";
        int business = dangKinhDoanh == null ? -1 : (dangKinhDoanh ? 1 : 0);
        int stock = conTon == null ? -1 : (conTon ? 1 : 0);
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pattern); ps.setString(2, pattern); ps.setString(3, pattern);
            ps.setInt(4, business); ps.setInt(5, business);
            ps.setInt(6, stock); ps.setInt(7, stock);
            try (ResultSet rs = ps.executeQuery()) {
                List<LinhKien> result = new ArrayList<>();
                while (rs.next()) result.add(mapRow(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm kiếm linh kiện: " + e.getMessage(), e);
        }
    }

    /** Đọc giá trị hiện tại từ DB để nạp vào form Sửa. */
    public LinhKien timTheoMa(String maLK) {
        String sql = "SELECT maLK, tenLK, donGia, donViTinh, soLuongTon, dangKinhDoanh FROM LinhKien WHERE maLK=?";
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maLK);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapRow(rs) : null; }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi đọc linh kiện: " + e.getMessage(), e);
        }
    }

    /** Lấy mã kế tiếp do SQL Server tính để hiển thị chỉ đọc trên form Thêm. */
    public String layMaTiepTheo() {
        String sql = "SELECT 'LK' + CASE WHEN n < 100000 THEN RIGHT('00000' + CAST(n AS VARCHAR(8)),5) "
                + "ELSE CAST(n AS VARCHAR(8)) END FROM (SELECT ISNULL(MAX(TRY_CONVERT(INT,SUBSTRING(maLK,3,8))),0)+1 n FROM LinhKien) d";
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new IllegalStateException("Không thể cấp mã linh kiện.");
            return rs.getString(1);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy mã linh kiện kế tiếp: " + e.getMessage(), e);
        }
    }

    /** Tạo linh kiện, mã do stored procedure sinh; tồn ban đầu chỉ ghi trong bước tạo. */
    public LinhKien them(LinhKien linhKien) {
        String sql = "{call sp_ThemLinhKien(?, ?, ?, ?, ?, ?)}";
        try (Connection con = DbUtil.getConnection(); CallableStatement cs = con.prepareCall(sql)) {
            cs.setNString(1, linhKien.getTenLK());
            cs.setDouble(2, linhKien.getDonGia());
            cs.setNString(3, linhKien.getDonViTinh());
            cs.setInt(4, linhKien.getSoLuongTon());
            cs.setBoolean(5, linhKien.isDangKinhDoanh());
            cs.registerOutParameter(6, Types.VARCHAR);
            cs.execute();
            String maLK = cs.getString(6);
            if (maLK == null) throw new IllegalStateException("Cơ sở dữ liệu không trả mã linh kiện.");
            return timTheoMa(maLK);
        } catch (SQLException e) {
            throw loiGhi("thêm", e);
        }
    }

    /** Cập nhật thông tin danh mục, không chỉnh tồn kho. */
    public boolean capNhat(LinhKien linhKien) {
        String sql = "UPDATE LinhKien SET tenLK=?, donGia=?, donViTinh=?, dangKinhDoanh=? WHERE maLK=?";
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setNString(1, linhKien.getTenLK()); ps.setDouble(2, linhKien.getDonGia());
            ps.setNString(3, linhKien.getDonViTinh()); ps.setBoolean(4, linhKien.isDangKinhDoanh());
            ps.setString(5, linhKien.getMaLK());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw loiGhi("cập nhật", e);
        }
    }

    public boolean capNhatTrangThai(String maLK, boolean dangKinhDoanh) {
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(
                "UPDATE LinhKien SET dangKinhDoanh=? WHERE maLK=?")) {
            ps.setBoolean(1, dangKinhDoanh); ps.setString(2, maLK);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi đổi trạng thái linh kiện: " + e.getMessage(), e);
        }
    }

    /** Xóa cứng chỉ khi chưa được tham chiếu; FK bảo vệ phiếu và khuyến mãi có lịch sử. */
    public boolean xoa(String maLK) {
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement("DELETE FROM LinhKien WHERE maLK=?")) {
            ps.setString(1, maLK);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 547) {
                throw new IllegalStateException("Linh kiện đã được dùng trong phiếu hoặc chương trình khuyến mãi. Hãy chọn Ngừng kinh doanh.", e);
            }
            throw new RuntimeException("Lỗi xóa linh kiện: " + e.getMessage(), e);
        }
    }

    private static LinhKien mapRow(ResultSet rs) throws SQLException {
        LinhKien lk = new LinhKien();
        lk.setMaLKRaw(rs.getString("maLK")); lk.setTenLKRaw(rs.getString("tenLK"));
        lk.setDonGiaRaw(rs.getDouble("donGia")); lk.setDonViTinhRaw(rs.getString("donViTinh"));
        lk.setSoLuongTonRaw(rs.getInt("soLuongTon")); lk.setDangKinhDoanhRaw(rs.getBoolean("dangKinhDoanh"));
        return lk;
    }

    private static RuntimeException loiGhi(String hanhDong, SQLException e) {
        if (e.getErrorCode() == 2627 || e.getErrorCode() == 2601) return new IllegalArgumentException("Tên linh kiện đã tồn tại.", e);
        return new RuntimeException("Lỗi " + hanhDong + " linh kiện: " + e.getMessage(), e);
    }
}
