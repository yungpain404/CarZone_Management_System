package dao;

import connectDB.DbUtil;
import entity.NhanVien;
import enums.VaiTro;

import java.sql.Connection;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho bảng Nhân viên (`NhanVien`) - JDBC thuần (DriverManager),
 * chỉ đọc/lưu dữ liệu, không chứa luật nghiệp vụ.
 */
public class NhanVienDAO {

    /** Xem mã kế tiếp theo vai trò để hiển thị trước trên form; mã chính thức vẫn do stored procedure cấp khi lưu. */
    public String xemMaTiepTheo(VaiTro vaiTro) {
        String prefix = switch (vaiTro) {
            case CHU_CUA_HANG -> "CCH";
            case TIEP_NHAN_VIEN -> "TNV";
            case KY_THUAT_VIEN -> "KTV";
        };
        String sql = "SELECT ISNULL(MAX(TRY_CONVERT(INT, RIGHT(maNV, 3))), 0) + 1 FROM NhanVien WHERE LEFT(maNV, 3) = ?";
        try (Connection con = DbUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, prefix);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Không lấy được mã nhân viên tiếp theo.");
                int next = rs.getInt(1);
                if (next > 999) throw new SQLException("Đã vượt quá 999 nhân viên trong vai trò này.");
                return prefix + String.format(java.util.Locale.ROOT, "%03d", next);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Không thể lấy mã nhân viên dự kiến: " + e.getMessage(), e);
        }
    }

    /**
     * Tìm nhân viên theo mã nhân viên.
     *
     * @param maNV mã nhân viên (NVxxxxx)
     * @return NhanVien hoặc null nếu không tìm thấy
     */
    public NhanVien timTheoMa(String maNV) {
        String sql = selectNhanVienSql() + " WHERE nv.maNV = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maNV);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm nhân viên theo mã: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy danh sách tất cả nhân viên.
     *
     * @return danh sách nhân viên
     */
    public List<NhanVien> layTatCa() {
        String sql = selectNhanVienSql() + " ORDER BY nv.maNV";
        List<NhanVien> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách nhân viên: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Tìm danh sách nhân viên theo vai trò.
     *
     * @param vaiTro vai trò nhân viên
     * @return danh sách nhân viên thuộc vai trò
     */
    public List<NhanVien> timTheoVaiTro(VaiTro vaiTro) {
        return timKiem(null, vaiTro);
    }

    /**
     * Tìm theo mã, họ tên hoặc số điện thoại; có thể lọc thêm theo vai trò.
     * Từ khóa và vai trò đều rỗng nghĩa là lấy toàn bộ hồ sơ sau khi người dùng chủ động tìm kiếm.
     */
    public List<NhanVien> timKiem(String tuKhoa, VaiTro vaiTro) {
        String sql = selectNhanVienSql()
                + " WHERE (? IS NULL OR nv.maNV LIKE ? OR nv.hoTen LIKE ? OR nv.soDienThoai LIKE ?)"
                + " AND (? IS NULL OR nv.vaiTro = ?) ORDER BY nv.maNV";
        List<NhanVien> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String pattern = tuKhoa == null || tuKhoa.isBlank() ? null : "%" + tuKhoa.trim() + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            ps.setString(5, vaiTro == null ? null : vaiTro.name());
            ps.setString(6, vaiTro == null ? null : vaiTro.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm kiếm nhân viên: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Thêm mới nhân viên vào CSDL.
     *
     * @param nv nhân viên cần thêm
     * @return true nếu thêm thành công; mã do stored procedure gán vào entity
     */
    public boolean them(NhanVien nv) {
        String sql = "{call sp_ThemNhanVien(?, ?, ?, ?, ?, ?, ?, ?, ?)}";
        try (Connection con = DbUtil.getConnection();
             CallableStatement cs = con.prepareCall(sql)) {
            cs.setNString(1, nv.getHoTen());
            cs.setDate(2, Date.valueOf(nv.getNgaySinh()));
            cs.setString(3, nv.getSoDienThoai());
            cs.setString(4, nv.getCCCD());
            if (nv.getDiaChi() == null) cs.setNull(5, Types.NVARCHAR); else cs.setNString(5, nv.getDiaChi());
            cs.setDate(6, Date.valueOf(nv.getNgayVaoLam()));
            cs.setString(7, nv.getVaiTro().name());
            cs.setString(8, nv.getEmail());
            cs.registerOutParameter(9, Types.VARCHAR);
            cs.execute();
            nv.setMaNVRaw(cs.getString(9));
            return nv.getMaNV() != null;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi thêm nhân viên: " + e.getMessage(), e);
        }
    }

    /**
     * Cập nhật thông tin nhân viên.
     *
     * @param nv nhân viên cần cập nhật
     * @return true nếu cập nhật thành công
     */
    public boolean capNhat(NhanVien nv) {
        String sql = "UPDATE NhanVien SET hoTen = ?, ngaySinh = ?, soDienThoai = ?, email = ?, CCCD = ?, " +
                     "diaChi = ?, ngayVaoLam = ? WHERE maNV = ?";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nv.getHoTen());
            ps.setDate(2, nv.getNgaySinh() != null ? Date.valueOf(nv.getNgaySinh()) : null);
            ps.setString(3, nv.getSoDienThoai());
            ps.setString(4, nv.getEmail());
            ps.setString(5, nv.getCCCD());
            ps.setString(6, nv.getDiaChi());
            ps.setDate(7, nv.getNgayVaoLam() != null ? Date.valueOf(nv.getNgayVaoLam()) : null);
            ps.setString(8, nv.getMaNV());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật nhân viên: " + e.getMessage(), e);
        }
    }

    private static String selectNhanVienSql() {
        return "SELECT nv.maNV, nv.hoTen, nv.ngaySinh, nv.soDienThoai, nv.email, nv.CCCD, nv.diaChi, nv.ngayVaoLam, nv.vaiTro, "
                + "tk.tenDangNhap, tk.trangThaiHoatDong "
                + "FROM NhanVien nv LEFT JOIN TaiKhoan tk ON tk.maNV = nv.maNV";
    }

    private NhanVien mapRow(ResultSet rs) throws SQLException {
        NhanVien nv = new NhanVien();
        nv.setMaNV(rs.getString("maNV"));
        nv.setHoTenRaw(rs.getString("hoTen"));
        Date ngaySinhSql = rs.getDate("ngaySinh");
        if (ngaySinhSql != null) nv.setNgaySinhRaw(ngaySinhSql.toLocalDate());
        nv.setSoDienThoaiRaw(rs.getString("soDienThoai"));
        nv.setEmailRaw(rs.getString("email"));
        nv.setCCCDRaw(rs.getString("CCCD"));
        nv.setDiaChi(rs.getString("diaChi"));
        Date ngayVaoLamSql = rs.getDate("ngayVaoLam");
        if (ngayVaoLamSql != null) nv.setNgayVaoLamRaw(ngayVaoLamSql.toLocalDate());
        nv.setVaiTroRaw(VaiTro.valueOf(rs.getString("vaiTro")));
        String tenDangNhap = rs.getString("tenDangNhap");
        if (tenDangNhap != null) {
            entity.TaiKhoan tk = new entity.TaiKhoan();
            tk.setNhanVienRaw(nv);
            tk.setTenDangNhapRaw(tenDangNhap);
            tk.setTrangThaiHoatDong(rs.getBoolean("trangThaiHoatDong"));
            nv.setTaiKhoan(tk);
        }
        return nv;
    }
}
