package dao;

import connectDB.DbUtil;
import entity.DichVu;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** JDBC DAO cho tra cứu và quản lý danh mục dịch vụ sửa chữa. */
public class DichVuDAO {

    /** Tìm dịch vụ theo mã/tên; trạng thái null nghĩa là lấy cả hai trạng thái. */
    public List<DichVu> timKiem(String tuKhoa, Boolean dangCungCap) {
        String sql = "SELECT maDV, tenDV, donGia, donViTinh, moTa, dangCungCap FROM DichVu "
                + "WHERE (? IS NULL OR maDV LIKE ? OR tenDV LIKE ?) AND (? = -1 OR CAST(dangCungCap AS INT) = ?) "
                + "ORDER BY tenDV, maDV";
        String pattern = tuKhoa == null || tuKhoa.isBlank() ? null : "%" + tuKhoa.trim() + "%";
        int trangThai = dangCungCap == null ? -1 : (dangCungCap ? 1 : 0);
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setInt(4, trangThai);
            ps.setInt(5, trangThai);
            try (ResultSet rs = ps.executeQuery()) {
                List<DichVu> result = new ArrayList<>();
                while (rs.next()) result.add(mapRow(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm kiếm dịch vụ: " + e.getMessage(), e);
        }
    }

    /** Đọc một dịch vụ hiện tại từ DB để nạp vào form Sửa. */
    public DichVu timTheoMa(String maDV) {
        String sql = "SELECT maDV, tenDV, donGia, donViTinh, moTa, dangCungCap FROM DichVu WHERE maDV = ?";
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maDV);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi đọc dịch vụ: " + e.getMessage(), e);
        }
    }

    /** Lấy mã kế tiếp do SQL Server tính để hiển thị dạng chỉ đọc trên form Thêm. */
    public String layMaTiepTheo() {
        String sql = "SELECT 'DV' + CASE WHEN n < 100000 THEN RIGHT('00000' + CAST(n AS VARCHAR(8)), 5) "
                + "ELSE CAST(n AS VARCHAR(8)) END FROM (SELECT ISNULL(MAX(TRY_CONVERT(INT, SUBSTRING(maDV,3,8))),0)+1 n FROM DichVu) d";
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new IllegalStateException("Không thể cấp mã dịch vụ.");
            return rs.getString(1);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy mã dịch vụ kế tiếp: " + e.getMessage(), e);
        }
    }

    /** Tạo dịch vụ; stored procedure sinh mã trong DB và trả mã qua OUTPUT. */
    public DichVu them(DichVu dichVu) {
        String sql = "{call sp_ThemDichVu(?, ?, ?, ?, ?, ?)}";
        try (Connection con = DbUtil.getConnection(); CallableStatement cs = con.prepareCall(sql)) {
            cs.setNString(1, dichVu.getTenDV());
            cs.setDouble(2, dichVu.getDonGia());
            cs.setNString(3, dichVu.getDonViTinh());
            if (dichVu.getMoTa() == null) cs.setNull(4, Types.NVARCHAR);
            else cs.setNString(4, dichVu.getMoTa());
            cs.setBoolean(5, dichVu.isDangCungCap());
            cs.registerOutParameter(6, Types.VARCHAR);
            cs.execute();
            return timTheoMa(cs.getString(6));
        } catch (SQLException e) {
            throw loiGhi("thêm", e);
        }
    }

    /** Cập nhật thông tin danh mục; giá đã chụp trên phiếu sửa chữa không bị thay đổi. */
    public boolean capNhat(DichVu dichVu) {
        String sql = "UPDATE DichVu SET tenDV=?, donGia=?, donViTinh=?, moTa=?, dangCungCap=? WHERE maDV=?";
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setNString(1, dichVu.getTenDV());
            ps.setDouble(2, dichVu.getDonGia());
            ps.setNString(3, dichVu.getDonViTinh());
            if (dichVu.getMoTa() == null) ps.setNull(4, Types.NVARCHAR);
            else ps.setNString(4, dichVu.getMoTa());
            ps.setBoolean(5, dichVu.isDangCungCap());
            ps.setString(6, dichVu.getMaDV());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw loiGhi("cập nhật", e);
        }
    }

    /** Cập nhật trạng thái cung cấp mà không xóa lịch sử sử dụng. */
    public boolean capNhatTrangThai(String maDV, boolean dangCungCap) {
        String sql = "UPDATE DichVu SET dangCungCap=? WHERE maDV=?";
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, dangCungCap);
            ps.setString(2, maDV);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi đổi trạng thái dịch vụ: " + e.getMessage(), e);
        }
    }

    /** Xóa cứng chỉ khi dịch vụ chưa được tham chiếu; FK bảo vệ dữ liệu lịch sử. */
    public boolean xoa(String maDV) {
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement("DELETE FROM DichVu WHERE maDV=?")) {
            ps.setString(1, maDV);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 547) {
                throw new IllegalStateException("Dịch vụ đã được dùng trong phiếu hoặc chương trình khuyến mãi. Hãy chọn Ngừng cung cấp.", e);
            }
            throw new RuntimeException("Lỗi xóa dịch vụ: " + e.getMessage(), e);
        }
    }

    private static DichVu mapRow(ResultSet rs) throws SQLException {
        DichVu dv = new DichVu();
        dv.setMaDVRaw(rs.getString("maDV"));
        dv.setTenDVRaw(rs.getString("tenDV"));
        dv.setDonGiaRaw(rs.getDouble("donGia"));
        dv.setDonViTinhRaw(rs.getString("donViTinh"));
        dv.setMoTaRaw(rs.getString("moTa"));
        dv.setDangCungCapRaw(rs.getBoolean("dangCungCap"));
        return dv;
    }

    private static RuntimeException loiGhi(String hanhDong, SQLException e) {
        if (e.getErrorCode() == 2627 || e.getErrorCode() == 2601) {
            return new IllegalArgumentException("Tên dịch vụ đã tồn tại.", e);
        }
        return new RuntimeException("Lỗi " + hanhDong + " dịch vụ: " + e.getMessage(), e);
    }
}
